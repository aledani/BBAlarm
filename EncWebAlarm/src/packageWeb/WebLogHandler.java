package packageWeb;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * This class generates an HTML web page used for the log service.
 * Users can read a simple log file.
 * 
 * @author Alessio Poggio (matr. 20008565)
 * @author Daniele Scattone (matr. 20006361)
 */

public class WebLogHandler implements HttpHandler {

	private final static String logPath = "./log.txt";
	
	/**
	 * @return
	 */
	public static String getLogPath() {
		return logPath;
	}
	
	/**
	 * @param path the path of the log file
	 * @return the content of the file
	 * @throws IOException
	 */
	private synchronized String readFile(String path) throws IOException {
		File file = new File(path);
		String log = new String();
		if(file.exists() && !file.isDirectory()){
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			String line = new String();
			while ((line = reader.readLine()) != null) {
					log += line + "\n";
			}
			reader.close();
		}
		return log;
	}


	/**
	 * @see com.sun.net.httpserver.HttpHandler#handle(com.sun.net.httpserver.HttpExchange)
	 */
	@Override
	public void handle(HttpExchange he) throws IOException {
		String response = "<html><head><style>"+readFile("./0001.css")+"</style>"
			    + "<link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css\" integrity=\"sha384-1q8mTJOASx8j1Au+a5WDVnPi2lkFfwwEAa8hDDdjZlpLegxhjVME1fgjWPGmkzs7\" crossorigin=\"anonymous\">"
			    + "<script src=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/js/bootstrap.min.js\" integrity=\"sha384-0mSbJDEHialfmuBBQP6A4Qrprq5OVfW37PRR3j5ELqxss1yVqOtnepnHVP9aJ7xS\" crossorigin=\"anonymous\"></script></head>"
			    + "<body><div class=\"row\">"
				+ "<div class=\"col-md-2\"></div>"
				+ "<div class=\"col-md-8\" style=\"background-color: aliceblue; border-radius: 40px; padding: 20px; top: 5ex; margin-bottom: 10ex; text-align: center;\">"
				+ "<h1 style=\"text-align: center; margin-bottom: 2ex;\">Log allarmi</h1>"
				+ "<textarea name=\"log\" rows=\"14\" cols=\"80\" readonly=\"\">" + readFile(logPath) + "</textarea>"
				+ "<br></br>"
				+ "<form action=\"../\" style=\"text-align: right;\"><button class=\"button\" type=\"submit\" value=\"Home\">Home</button></form>"
				+ "</div>"
				+ "<div class=\"col-md-2\"></div>"
				+ "</div></body></html>";
		he.sendResponseHeaders(200, response.length());
		OutputStream os = he.getResponseBody();
		os.write(response.toString().getBytes());
		os.close();
	}
}
