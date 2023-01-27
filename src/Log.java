import java.time.format.DateTimeFormatter;  
import java.time.LocalDateTime;
import java.io.File;
import java.io.FileWriter;

public class Log {
    static private DateTimeFormatter DTF;
    private String FileName;
    private File _log;
    private String LogStr;
    private FileWriter Writer;

    public static void LOGSINIT(){
        try{
            File Dir = new File(".\\Logs");
            if(Dir.mkdir()){
                System.out.println("~Logs folder created");
            }
        }catch(Exception  e){
            System.err.println("&failed to create Log directory beacuse : "+e);
        }
        DTF = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");

    }

    static private String Time(){;  
        return DTF.format(LocalDateTime.now());
    }

    Log(){
        try{
            LogFileInit();
            LogStr="";//initialise as empty for us to start the log
            Writer = new FileWriter(_log);
            WriteLog("LOG START");
        }catch(Exception e){
            System.err.println("&failed to start Log :"+e);
        }
    }
    Log(String Name){
        try{
            LogFileInit(Name);
            LogStr="";//initialise as empty for us to start the log
            Writer = new FileWriter(_log);
            WriteLog("LOG "+Name+" START");//squises in the name
        }catch(Exception e){
            System.err.println("&failed to start Log :"+e);
        }
    }

    public void WriteLog(String ToLog){
        LogStr=LogStr+"<"+Time()+">:"+ToLog+"\n";
    }

    public void TerminateLog(){
        try{
            WriteLog("LOG END");
            Writer.write(LogStr);
            Writer.close();
        }catch(Exception e){
            System.err.println("&failed to terminate Log :"+e);
        }
    }

    private void LogFileInit(){
        try{
            FileName = ".\\Logs\\LOG"+Time()+".txt";
            _log= new File(FileName);
            if(_log.exists()){// in case other thread just started another log
                FileName = ".\\Logs\\LOG"+Time()+"_2.txt";
                _log= new File(FileName);
            }
            if(!_log.createNewFile()){
                System.err.println("&failed to create Log file :");
                LogFileInit();//second time is the charm
                //also if it is not throwing an error then it just has to wait for the seccond to change
                //thus unlikely to lead to an infinite loop
            }
        }catch(Exception e){
            System.err.println("&failed to init Log file :"+e);
        }
    }
    private void LogFileInit(String Name){//names the file 
        try{
            FileName = ".\\Logs\\"+Name+"_LOG"+Time()+".txt";
            _log= new File(FileName);
            if(_log.exists()){// in case other thread just started another log
                FileName = ".\\Logs\\"+Name+"_LOG"+Time()+"_2.txt";
                _log= new File(FileName);
            }
            if(!_log.createNewFile()){
                System.err.println("&failed to create Log file :");
                LogFileInit();//second time is the charm
                //also if it is not throwing an error then it just has to wait for the seccond to change
                //thus unlikely to lead to an infinite loop
            }
        }catch(Exception e){
            System.err.println("&failed to init Log file :"+e);
        }
    }
}
