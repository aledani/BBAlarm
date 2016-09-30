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
import java.time.LocalDateTime;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * This class generates an HTML web page which is the user interface for the rules handling.
 * It captures the content of the submit form and handles it by contacting the server side 
 * which is listening on the BB for setting the alarm ON, OFF etc.
 * 
 * @author Daniele Scattone (matr. 20006361)
 * @author Alessio Poggio (matr. 20008565)
 */

public class WebRulesHandler implements HttpHandler {
	
	private final static String statusPath = "./status.txt";
	/**
	 * @return true if the alarm is ON, false if some errors occurred
	 * either in the communication process or in setting the alarm.
	 */
	private boolean setAlarmON() {
		boolean isset = false;
		if(MyWebSocketHandler.getSess().isOpen()){
			RemoteEndpoint remote = MyWebSocketHandler.getSess().getRemote();
			try {
				setStatus("ON");
				remote.sendString("{\"status\": \"ON\"}");
			} catch (IOException e) {
				e.printStackTrace();
			}
			isset = true;
		}
		return isset;
	}
	
	/**
	 * @return true if the alarm is OFF, false if some errors occurred
	 * either in the communication process or in setting the alarm.
	 */
	private boolean setAlarmOFF() {
		boolean isset = false;
		if(MyWebSocketHandler.getSess().isOpen()){
			RemoteEndpoint remote = MyWebSocketHandler.getSess().getRemote();
			try {
				setStatus("OFF");
				remote.sendString("{\"status\": \"OFF\"}");
			} catch (IOException e) {
				e.printStackTrace();
			}
			isset = true;
		}
		return isset;
		
	}


	/**
	 * @return true if the alarm is in the first configuration, false if some errors occurred
	 * either in the communication process or in setting the alarm.
	 */
	private boolean setSensor1() {
		boolean isset = false;
		if(MyWebSocketHandler.getSess().isOpen()){
			RemoteEndpoint remote = MyWebSocketHandler.getSess().getRemote();
			try {
				setStatus("S1");
				remote.sendString("{\"status\": \"S1\"}");
			} catch (IOException e) {
				e.printStackTrace();
			}
			isset = true;
		}
		return isset;
	}

	/**
	 * @return true if the alarm is in the second configuration, false if some errors occurred
	 * either in the communication process or in setting the alarm.
	 */
	private boolean setSensor2() {
		boolean isset = false;
		if(MyWebSocketHandler.getSess().isOpen()){
			RemoteEndpoint remote = MyWebSocketHandler.getSess().getRemote();
			try {
				setStatus("S2");
				remote.sendString("{\"status\": \"S2\"}");
			} catch (IOException e) {
				e.printStackTrace();
			}
			isset = true;
		}
		return isset;
	}

	
	/**
	 * @see com.sun.net.httpserver.HttpHandler#handle(com.sun.net.httpserver.HttpExchange)
	 */
	@Override
	public void handle(HttpExchange he) throws IOException {
		String response = "<html><head><script>function loadXMLDoc(){if (window.XMLHttpRequest){ xmlhttp=new XMLHttpRequest(); }"
				+ "else { xmlhttp=new ActiveXObject(\"Microsoft.XMLHTTP\");}"
				+ "xmlhttp.onreadystatechange=function(){ if (xmlhttp.readyState==XMLHttpRequest.DONE && xmlhttp.status==200) {"
				+ "if(xmlhttp.responseText.indexOf(\"ON\") > -1){ document.getElementById(\"on\").checked = true; document.getElementById(\"piantina\").src=\"https://www.dropbox.com/s/xb1zpuech0cep6y/PiantinaAllarmeVerde.jpg?raw=1\";  }"
				+ "if(xmlhttp.responseText.indexOf(\"OFF\") > -1){ document.getElementById(\"off\").checked = true; document.getElementById(\"piantina\").src=\"https://www.dropbox.com/s/hrxnfy8d6mhfswg/PiantinaAllarmeRosso.jpg?raw=1\"; }"
				+ "if(xmlhttp.responseText.indexOf(\"S1\") > -1){ document.getElementById(\"s1\").checked = true; document.getElementById(\"piantina\").src=\"https://www.dropbox.com/s/0ga9kyi7eak5xns/PiantinaAllarmeVR.jpg?raw=1\"; }"
				+ "if(xmlhttp.responseText.indexOf(\"S2\") > -1){ document.getElementById(\"s2\").checked = true; document.getElementById(\"piantina\").src=\"https://www.dropbox.com/s/j1v3rhx3p350oww/PiantinaAllarmeRV.jpg?raw=1\"; }"
				+ " } };"
				+ "xmlhttp.open(\"GET\", \"/status\", true);"
				+ "xmlhttp.send(null);"
			    + "};</script>"
			    + "<style>"+readFile("./0001.css")+"</style>"
			    + "<link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css\" integrity=\"sha384-1q8mTJOASx8j1Au+a5WDVnPi2lkFfwwEAa8hDDdjZlpLegxhjVME1fgjWPGmkzs7\" crossorigin=\"anonymous\">"
			    + "<script src=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/js/bootstrap.min.js\" integrity=\"sha384-0mSbJDEHialfmuBBQP6A4Qrprq5OVfW37PRR3j5ELqxss1yVqOtnepnHVP9aJ7xS\" crossorigin=\"anonymous\"></script>"
			    + "<script src=\"https://code.jquery.com/jquery-2.2.3.js\" integrity=\"sha256-laXWtGydpwqJ8JA+X9x2miwmaiKhn8tVmOVEigRNtP4=\" crossorigin=\"anonymous\"></script>"
			    + "</head>"
				+ "<body><div class=\"row\">"
				+ "<div class=\"col-md-2\"></div>"
				+ "<div class=\"col-md-8\" style=\"background-color: aliceblue; border-radius: 40px; padding: 20px; top: 5ex; margin-bottom: 10ex\">"
				+ "<h1 style=\"text-align: center\">Regole</h1>"
				+ "<form method=\"post\" style=\"display: inline;\">"
				+ "<div class=\"switch-field\" style=\"font-family: 'Roboto', sans-serif;\">"
				+ "<input type=\"radio\" name=\"rule\" id=\"on\" value=\"ON\"/>"
				+ "<label for=\"on\" style=\"width: 20%; margin-left: 10%;\">ON</label>"
				+ "<input type=\"radio\" name=\"rule\" id=\"off\" value=\"OFF\"/>"
				+ "<label for=\"off\" style=\"width: 20%;\">OFF</label>"
				+ "<input type=\"radio\" name=\"rule\" id=\"s1\" value=\"S1\"/>"
				+ "<label for=\"s1\" style=\"width: 20%;\">INTERNO</label>"
				+ "<input type=\"radio\" name=\"rule\" id=\"s2\" value=\"S2\"/>"
				+ "<label for=\"s2\" style=\"width: 20%;\">ESTERNO</label></div>"
				+ "</form>"
				+ "<br></br>"
				+ "<form action=\"../\" style=\"text-align: right;\"><button class=\"button\" type=\"submit\" value=\"Home\"\">Home</button></form>"
				+ "</div>"
				+ "<div class=\"col-md-2\"></div>"
				+ "</div><div style=\"display: flex;\">"
				+ "<img id=\"piantina\" src=\"\" style=\"margin: auto;\"/>"
						+ "<span style=\"margin: auto;\"><h3>Legenda</h3><br></br><strong>Allarme ON: </strong>attiva tutti i sensori e rileva ogni movimento.<br><br>"
						+ "<strong>Allarme OFF: </strong>allarme spento, nessun movimento rilevato.<br><br>"
						+ "<strong>Interno: </strong>attiva solo il sensore posto all'interno.<br><br>"
						+ "<strong>Esterno: </strong>attiva solo i sensori all'esterno e permette di uscire ma non di entrare.</span>"
						+ "</div>"
						+ "<script>loadXMLDoc(); var myVar = setInterval(loadXMLDoc, 5000);  $(document).ready(function() { $('input[name=rule]').change(function(){ this.form.submit();});});</script>"
						+ "</body>"
						+ "<style>.switch-field input:checked + label {background-color: #2EE59D; color: white; font-weight: bold;} "
						+ "</style></html>";
		he.sendResponseHeaders(200, response.length());
		OutputStream os = he.getResponseBody();
		os.write(response.toString().getBytes());
		InputStreamReader isr = new InputStreamReader(he.getRequestBody(), "utf-8");
		BufferedReader br = new BufferedReader(isr);
		String query = br.readLine();
		os.close();
		if(query.toString().contains("ON")){
			if(setAlarmON()) {
				WebHandler.updateLog(LocalDateTime.now().toString() + " - allarme ON");
			}
			else{
				WebHandler.updateLog(LocalDateTime.now().toString() + " - errore! allarme NON attivato!");
			}
		}
		else if(query.toString().contains("OFF")){
			if(setAlarmOFF()){
				WebHandler.updateLog(LocalDateTime.now().toString() + " - allarme OFF");
			}
			else{
				WebHandler.updateLog(LocalDateTime.now().toString() + " - errore! allarme NON disattivato");
			}
		}
		else if(query.toString().contains("S1")){
			if(setSensor1()){
				WebHandler.updateLog(LocalDateTime.now().toString() + " - interno");
			}
			else{
				WebHandler.updateLog(LocalDateTime.now().toString() + " - errore! impossibile attivare solo interno!");
			}
		}
		else if(query.toString().contains("S2")){
			if(setSensor2()){
				WebHandler.updateLog(LocalDateTime.now().toString() + " - esterno");
			}
			else{
				WebHandler.updateLog(LocalDateTime.now().toString() + " - errore! impossibile attivare solo esterno!");
			}
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
	
	/**
	 * @param string,
	 *            it contains the current status of the alarm to be set on the
	 *            file status.txt.
	 * @return true if the method has been successfully executed, and false
	 *         otherwise.
	 */
	private synchronized static boolean setStatus(String string) {
		/* scrive sul file lo stato */
		PrintWriter pw;
		boolean stat = true;
		try {
			pw = new PrintWriter(new FileOutputStream(new File(statusPath),
					false /* append = false */));
			pw.write(string + "\n");
			pw.close();
		} catch (IOException e) {
			stat = false;
			e.printStackTrace();
		}
		return stat;
	}
}
