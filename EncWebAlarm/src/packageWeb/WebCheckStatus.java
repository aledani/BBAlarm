package packageWeb;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * This class contains the current status of the alarm which is read by the WebRulesHandler class.
 * 
 * @author Alessio Poggio (matr. 20008565)
 * @author Daniele Scattone (matr. 20006361)
 * @see WebRulesHandler
 */


public class WebCheckStatus implements HttpHandler {
	
	private final static String statusPath = "./status.txt"; /* il file mantiene ggiornato lo stato attuale dell'allarme */
	
	/**
	 * @return the current status of the alarm.
	 * @throws IOException
	 *             if some error occur while processing the status file.
	 */
	private synchronized String checkStatus() throws IOException {
		/* controlla lo stato leggendo il file */
		File file = new File(statusPath);
		String status = "";
		if (file.exists() && !file.isDirectory()) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			String line = new String();
			while ((line = reader.readLine()) != null) {
				status += line;
			}
			reader.close();
		}
		return status;
	}

	/**
	 * @see com.sun.net.httpserver.HttpHandler#handle(com.sun.net.httpserver.HttpExchange)
	 */
	@Override
	public void handle(HttpExchange he) throws IOException {
		String response = "<html>" + checkStatus() + "</html>";
		he.sendResponseHeaders(200, response.length());
		OutputStream os = he.getResponseBody();
		os.write(response.toString().getBytes());
		os.close();
	}
	
}
