import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    //Client config
    static private int Port = 8314;// change this for port
    static private String serverHost = "localhost"; // Local IP or "localhost"
    static private int TimeoutAttemts=3;//how many times it will re send a message before desconecting due to timeout
    static private int TimeoutWrongLimit=6;// after how many wrong messages will it disconect (both wrong time and uninteligable)
    static private String Domain="SERVER";//used as part of SMTP (the server will play along)

    static private Scanner Scann;
    static private Log ClientLog;
    static private Socket socketOfClient ;
    static private BufferedWriter os;
    static private BufferedReader is ;
    static private Encryption ClientEncript;
    static private String MessageOUT="";
    static private String MessageIN="";
    static private String UserEmai;
    static private String MailRCPT;//temprary storage for recipient
    static private int WrongMessageCounter=0;
    static private int MessageCheckCounter=0;
    static private int MessageChecksMax=10;
    static private int AttemtCounter=0;

    public static void main(String[] args) {
        //init of client
        Log.LOGSINIT();
        ClientLog = new Log("CLIENT");
        Scann=new Scanner(System.in);
        Encryption.GenerateKeys();
        Encryption.InitDecrypCipher();
        ConnectToServer();
        ClientEncript = new Encryption();
        ClientLog.WriteLog("INIT COMPLEAT");

        //interaction with server
        ClientKeyExchange();
        if(LogIn()){//if login was successfull
            while(WaitForMessage()){}
            MessageIN=ReadEncrypted();
            SMTPCodes Reply = new SMTPCodes(MessageIN);
            if(Reply.Code.equals("220")){
                Reply = SMTPSend(SMTP.HELO(Domain));
                if(Reply!=null){//in case of failed communication
                    if(Reply.Code.equals("250")){//successful login
                        ClientLog.WriteLog("SUCCESSFUL HELO: "+Reply.Code + Reply.Message);
                        
                        while(true){
                            String CMDString =Scann.nextLine();
                            if(CMDString.equals("END")){//exit command
                                ClientLog.WriteLog("END COMMAND GIVEN");
                                break;
                            }else if (CMDString.equals("SEND MAIL")){//send mail command
                                Reply = SMTPSend(SMTP.MAIL(UserEmai));//sends MAIL
                                if(Reply.Code.equals("250")){
                                    System.out.println("~where do you want to send it:");
                                    MessageOUT=Scann.nextLine();
                                    Reply = SMTPSend(SMTP.RCPT(MessageOUT));//reads recipient adress and sends RCPT
                                    if(Reply.Code.equals("250")){
                                        MailRCPT=MessageOUT;
                                        Reply = SMTPSend(SMTP.DATA());//sends DATA command
                                        if(Reply.Code.equals("354")){
                                            MessageOUT=Scann.nextLine();
                                            while(true){//loop for sending lines of text
                                                
                                                if(MessageOUT.equals(".")){
                                                    break;
                                                }
                                                MessageEncriptedSend(MessageOUT);
                                                MessageOUT=Scann.nextLine();
                                            }
                                            Reply = SMTPSend(SMTP.Dot());//ends transmition of text
                                            if(Reply.Code.equals("250")){
                                                System.out.println("~MAil send successfuly to:"+MailRCPT);
                                                ClientLog.WriteLog("MAIL SENT SUCCESSFULY TO:"+MailRCPT);
                                            }else{
                                                System.err.println("&failed DOT try again:"+Reply.Code);
                                                ClientLog.WriteLog("DOT COMMAND FAILED:"+Reply.Code);
                                            }
                                        }else{
                                            System.err.println("&failed DATA try again:"+Reply.Code);
                                            ClientLog.WriteLog("DATA COMMAND FAILED:"+Reply.Code);
                                        }
                                    }else{
                                        System.err.println("&failed RCPT try again:"+Reply.Code);
                                        ClientLog.WriteLog("RCPT COMMAND FAILED:"+Reply.Code);
                                    }
                                }else{
                                    System.err.println("&failed mail try again:"+Reply.Code);
                                    ClientLog.WriteLog("MAIL COMMAND FAILED:"+Reply.Code);
                                }

                            }else if (CMDString.equals("HELP")){//send HELP command
                                Reply = SMTPSend(SMTP.HELP(Scann.nextLine()));//takes user input and sends HELP
                                if(Reply.Code.equals("214")){
                                    System.out.println("Help:"+Reply.Message);
                                    ClientLog.WriteLog("HELP COMMAND SUCCESS");
                                }else{
                                    System.err.println("&failed HELP try again:"+Reply.Code);
                                    ClientLog.WriteLog("HELP COMMAND FAILED:"+Reply.Code);
                                }
                            }else if (CMDString.equals("NOOP")){//send NOOP command
                                if(Reply.Code.equals("250")){
                                    System.out.println("~NOOP succeded");
                                    ClientLog.WriteLog("NOOP COMMAND SUCCESS");
                                }else{
                                    System.err.println("&failed NOOP try again:"+Reply.Code);
                                    ClientLog.WriteLog("NOOP COMMAND FAILED:"+Reply.Code);
                                }
                            }
                        }


                        
                        Reply = SMTPSend(SMTP.QUIT());//sends quit to denote end of communication
                    }else{
                        System.err.println("&failed HELO: "+Reply.Code + Reply.Message);
                        ClientLog.WriteLog("FAILED HELO: "+Reply.Code + Reply.Message);
                    }
                }
            }
        }
        //exit
        TerminateConnection();
        ClientLog.TerminateLog();
    }

    static private void ConnectToServer(){
        try {
            
            socketOfClient = new Socket(serverHost, Port);
            os = new BufferedWriter(new OutputStreamWriter(socketOfClient.getOutputStream()));
            is = new BufferedReader(new InputStreamReader(socketOfClient.getInputStream()));
            ClientLog.WriteLog("SUCCESSFUL CONNECTION: ");
        } catch (UnknownHostException e) {
            System.err.println("&Don't know about host " + serverHost);
            ClientLog.WriteLog("FAILED TO CONNECT UNKNOWN HOST "+ e);
            return;
        } catch (IOException e) {
            System.err.println("&Couldn't get I/O for the connection to " + serverHost);
            ClientLog.WriteLog("FAILED TO CONNECT IOEXC: "+ e);
            return;            
        }
    }
    static private void ClientKeyExchange(){
        //public key exchange
        //C:HELLO
        //S:HELLO
        //C:<PUBLIC_KEY>
        //S:<PUBLIC_KEY>
        //C:DONE*encrypted
        //S:DONE*encrypted
        try {
            //sends the first message
            MessageOUT="HELLO";
            MessageSend(MessageOUT);
            while(!(MessageIN.equals("HELLO"))){
                WrongMessageCounter++;
                if(WrongMessageCounter>TimeoutWrongLimit){
                    TerminateConnection();
                    System.err.println("&connection timmed out");
                    ClientLog.WriteLog("TIMEOUT");
                    System.exit(0);
                }
                while(WaitForMessage()){
                    AttemtCounter++;
                    
                    if(AttemtCounter>TimeoutAttemts){
                        System.err.println("&connection timmed out invalid communication");
                        ClientLog.WriteLog("TIMEOUT");
                        TerminateConnection();
                        System.exit(0);
                    }
                }
                AttemtCounter=0;
                MessageIN=is.readLine();
            }

            MessageOUT=Encryption.GetPublicKey();
            MessageSend(MessageOUT);
            while(WaitForMessage()){
                MessageSend(MessageOUT);
                AttemtCounter++;
                if(AttemtCounter>TimeoutAttemts){
                    TerminateConnection();
                    System.err.println("&connection timmed out");
                    ClientLog.WriteLog("TIMEOUT");
                    System.exit(0);
                }
            }
            MessageIN=is.readLine();
            ClientEncript.InitEncryptCipher(MessageIN);
            MessageOUT="DONE";
            MessageEncriptedSend(MessageOUT);
            while(WaitForMessage()){
                AttemtCounter++;
                if(AttemtCounter>TimeoutAttemts){
                    TerminateConnection();
                    System.err.println("&connection timmed out");
                    ClientLog.WriteLog("TIMEOUT");
                    System.exit(0);
                }
            }
            AttemtCounter=0;

            MessageIN=ReadEncrypted();
            while(!(MessageIN.equals("DONE"))){
                WrongMessageCounter++;
                if(WrongMessageCounter>TimeoutWrongLimit){
                    TerminateConnection();
                    System.err.println("&connection timmed out");
                    ClientLog.WriteLog("TIMEOUT");
                    System.exit(0);
                }
                while(WaitForMessage()){
                    AttemtCounter++;
                    MessageEncriptedSend(MessageOUT);
                    if(AttemtCounter>TimeoutAttemts){
                        System.err.println("&connection timmed out invalid communication");
                        ClientLog.WriteLog("TIMEOUT");
                        TerminateConnection();
                        System.exit(0);
                    }
                }
                AttemtCounter=0;
                MessageIN=ReadEncrypted();
            }
            System.out.println("~Transfer compleat");


        } catch (Exception e) {
            System.err.println("&Failed to exchange keys : " + e);
            System.exit(0);
        }
        ClientLog.WriteLog("SUCCESSFUL KEY EXCHANGE");
    }
    static private boolean LogIn(){
        System.out.println("Email:");
        MessageOUT=Scann.nextLine();//gets user iput
        MessageEncriptedSend(MessageOUT);
        while(!MessageOUT.equals("EXIT")){//condition to leave login
            while(WaitForMessage()){}
            MessageIN=ReadEncrypted();
            if(MessageIN.equals("GOODEMAIL")){// server accepted email
                UserEmai=MessageOUT;//saves user path for later transactions like MAIL
                System.out.println("Password:");
                while(true){
                    MessageOUT=Scann.nextLine();
                    MessageEncriptedSend(MessageOUT);
                    while(WaitForMessage()){}
                    MessageIN=ReadEncrypted();
                    if(MessageIN.equals("SUCCESSFUL LOGIN")){
                        ClientLog.WriteLog("SUCCESSFUL LOGGIN");
                        return true;//successful login
                    }
                    else if (MessageIN.equals("PASSWORDWRONG")) {
                        System.out.println("Wrong password:");//wrong pasword
                    }
                    else if(MessageIN.equals("PASSWORDFAIL")){
                        System.out.println("&failed password login (exided atempts)");
                        ClientLog.WriteLog("FAILED LOGGIN WRONG PASSWORD");//failed too many times
                        return false;
                    }
                }
            }else if(MessageIN.equals("WRONGEMAIL")){
                System.out.println("Wrong email if you wish to exit write EXIT:");
            }
            MessageOUT=Scann.nextLine();
            MessageEncriptedSend(MessageOUT);
            
        }
        ClientLog.WriteLog("FAILED LOGGIN NO EMAIL");
        return false;

        
    }

    static private void MessageSend(String Message){
        try{
            
            os.write(Message);
            os.newLine();
            os.flush();
        }catch(Exception e){
            System.err.println("&Message failed to be sent "+e);
        }
    }
    static private void MessageEncriptedSend(String Message){
        try{
            os.write(ClientEncript.Encript(Message));
            os.newLine();
            os.flush();
        }catch(Exception e){
            System.err.println("&Message failed to be sent "+e);
        }
    }
    static private SMTPCodes SMTPSend(String Message){
        System.out.println("~SMTP MSG:"+Message);
        MessageOUT=Message;
        MessageEncriptedSend(MessageOUT);
        return SMTPReplyWait();
    }

    static private String ReadEncrypted(){
        try{
            return Encryption.DeCrypt(is.readLine());
        }catch(Exception e){
            System.err.println("&Message failed to be sent "+e);
            return null;
        }
        
    }
    
    static private Boolean WaitForMessage(){
        try{
            //checks if a message has arived
            while(!is.ready()){try{
                if(MessageCheckCounter>MessageChecksMax){
                    MessageCheckCounter=0;
                    return true;
                }else{
                    MessageCheckCounter++;
                }
                Thread.sleep(100);

                }catch(Exception e){
                    System.err.println("&Thread can't sleep"+e);
                }
            }


        }catch(Exception e){
            System.out.println(e);
        }
        return false;
    }
    static private SMTPCodes SMTPReplyWait(){
        while(WaitForMessage()){
            MessageSend(MessageOUT);
            AttemtCounter++;
            if(AttemtCounter>TimeoutAttemts){
                TerminateConnection();
                System.err.println("&connection timmed out");
                ClientLog.WriteLog("TIMEOUT");
                System.exit(0);
            }
        }
        AttemtCounter=0;
        MessageIN=ReadEncrypted();
        SMTPCodes Reply = new SMTPCodes(MessageIN);
        if(Reply.IsValid(ClientLog,MessageOUT)){
            System.out.println("~SMTP RPL: "+Reply.Code+Reply.Message);
            return Reply;
        }
        return null;
    }

    static private void TerminateConnection(){
        try{
            os.close();
            is.close();
            socketOfClient.close();
        }catch(IOException e){
            System.err.println("&failed to terminate connection"+e);
        }
        
    }
}