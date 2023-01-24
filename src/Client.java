import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class Client {
    //Client config
    static private int Port = 8314;// change this for port
    static private String serverHost = "localhost"; // Local IP or "localhost"
    static private int TimeoutAttemts=3;//how many times it will re send a message before desconecting due to timeout
    static private int TimeoutAttemtsInterval=1;//seconds between atemts
    static private int TimeoutWrongLimit=6;// after how many wrong messages will it disconect (both wrong time and uninteligable)


    static private Socket socketOfClient ;
    static private BufferedWriter os;
    static private BufferedReader is ;
    static private Encryption ClientEncript;
    static private String MessageOUT="";
    static private String MessageIN="";
    static private boolean ThreadActive=true;
    static private int WrongMessageCounter=0;
    static private int MessageCheckCounter=0;
    static private int MessageChecksMax=10;
    static private int AttemtCounter=0;
    static private boolean AttemtReSend=false;
    

    static private int KeyExchangeStage=0;
    static private boolean KeysExchanged=false;

    public static void main(String[] args) {

        ConnectToServer();
        ClientEncript = new Encryption();
        ClientKeyExchange();


        
    }

    static private void ConnectToServer(){
        try {
            Encryption.GenerateKeys();
            Encryption.InitDecrypCipher();
            System.out.println();
            socketOfClient = new Socket(serverHost, Port);
            os = new BufferedWriter(new OutputStreamWriter(socketOfClient.getOutputStream()));
            is = new BufferedReader(new InputStreamReader(socketOfClient.getInputStream()));

        } catch (UnknownHostException e) {
            System.err.println("&Don't know about host " + serverHost);
            return;
        } catch (IOException e) {
            System.err.println("&Couldn't get I/O for the connection to " + serverHost);
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
                    System.exit(0);
                }
                while(WaitForMessage()){
                    AttemtCounter++;
                    
                    if(AttemtCounter>TimeoutAttemts){
                        System.err.println("&connection timmed out invalid communication");
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
                    System.exit(0);
                }
                while(WaitForMessage()){
                    AttemtCounter++;
                    MessageEncriptedSend(MessageOUT);
                    if(AttemtCounter>TimeoutAttemts){
                        System.err.println("&connection timmed out invalid communication");
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