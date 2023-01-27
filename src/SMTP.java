public class SMTP {
    static public String HELO(String domain){
        return "HELO "+domain;
    }
    static public String QUIT(){
        return "QUIT";
    }
    static public String NOOP(){
        return "NOOP";
    }
    static public String MAIL(String UserEmail){
        return "MAIL FROM:"+UserEmail;
    }
    static public String RCPT(String TargetMail){
        return "RCPT TO:"+TargetMail;
    }
    static public String DATA(){
        return "DATA";
    }
    static public String Dot(){
        return ".";
    }
}
