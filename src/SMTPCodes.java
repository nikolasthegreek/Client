public class SMTPCodes {
    private static String Codes[]={"220","221","214","221","250","251","354"};
    private static String ErrorCodes[]={"421","450","421","450","451","452","500","501","502","503","504","550","551","552","553","554"};
    public String Message;
    public String Code;
    
    SMTPCodes(String Reply){//parsing of message from server
        Code = new String(new char[]{Reply.charAt(0),Reply.charAt(1),Reply.charAt(2)});
        
        char[] _message = new char[Reply.length()-3];
        for (int i = 3; i < Reply.length(); i++) {
            _message[i-3]=Reply.charAt(i);
        }
        Message=new String(_message);
    }   
    public boolean IsValid(Log log,String ReplyOf){// it reqires the log and reply so it can log any error codes
        for (String _code : Codes) {
            if(Code.equals(_code)){
                return true;
            }
        }
        for (String _code : ErrorCodes) {
            if(Code.equals(_code)){
                log.WriteLog("ERROR CODE AFTER:"+ReplyOf+":"+Code+"="+Message);
                return true;
            }
        }
        return false;
    }
}
