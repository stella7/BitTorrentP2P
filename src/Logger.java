import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;


public class Logger {
	static String logName;
	static BufferedWriter out;
	static String timeStamp;
	public Logger(String lName){
		logName = lName;
		try {
			out = new BufferedWriter(new FileWriter(logName +".log"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	public static String timeHeader(){
		timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
		return timeStamp;
	}
	
	public void writeLog(String message) {
		try {
			out.write(timeHeader() + " : " + logName + " " + message + '\n');
			out.flush();
			//System.out.println(str);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void stop() {
		try {
			
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
