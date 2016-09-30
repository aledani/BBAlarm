package packageWeb;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.time.LocalDateTime;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * This class generates an HTML web page which is the home page of the user interface for the alarm management.
 * 
 * @author Alessio Poggio (matr. 20008565)
 * @author Daniele Scattone (matr. 20006361)
 */

public class WebRootHandler implements HttpHandler {

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
				+ "<div class=\"col-md-8\" style=\"background-color: aliceblue; border-radius: 40px; padding: 20px; top: 5ex; margin-bottom: 10ex\">"
				+ "<h1 style=\"text-align: center; margin-bottom: 4ex;\">Benvenuto nel pannello di configurazione</h1>"
				+ "<div style=\"margin: 0 auto; text-align: center;\"><form style=\"width: 140px; display: inline; margin-right: 5%;\" action=\"../rules\"><button class=\"button\" type=\"submit\" value=\"Regole\">Regole</button></form>"
				+ "<form style=\"width: 140px; display: inline; margin-right: 5%;\" action=\"../mail\"><button class=\"button\" type=\"submit\" value=\"Notifiche mail\">e-mail</button></form>"
				+ "<form style=\"width: 140px; display: inline;\" action=\"../log\"><button class=\"button\" type=\"submit\" value=\"Log\">Log</button></form></div>"
				+ "<br></br><br></br>"
				+ "</div>"
				+ "<div class=\"col-md-2\"></div>"
				+ "</div>"
				+ "<body></html>";
		he.sendResponseHeaders(200, response.length());
		OutputStream os = he.getResponseBody();
		os.write(response.toString().getBytes());
		os.close();
		
		if(WebServer.getInit() == 1){
			WebHandler.updateLog(LocalDateTime.now().toString() + " - " + he.getPrincipal().getUsername() + ": accesso effettuato. IP: " + he.getRemoteAddress().getAddress().toString());
			WebServer.setInit(0);
			}
		}
	
	private synchronized static String readFile(String name){
		File file = new File(name);
		String content = "";
		if(file.exists() && !file.isDirectory()){
			BufferedReader reader;
			try {
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
				String line = new String();
				while ((line = reader.readLine()) != null) {
					content += line;
				}
			}
			catch(Exception e){}
		}
		return content;
	}
}
