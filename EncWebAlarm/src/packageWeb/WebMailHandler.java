package packageWeb;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * This class generates an HTML web page used for the e-mail notification service.
 * Users can register/unregister to the service by entering their e-mail address.
 * 
 * @author Daniele Scattone (matr. 20006361)
 * @author Alessio Poggio (matr. 20008565)
 */

public class WebMailHandler implements HttpHandler {

	private final static String usersPath = "./users.txt";
	
	/**
	 * @return the path of the file name that contains the e-mail addresses of the users.
	 */
	public static String getUsersPath() {
		return usersPath;
	}
	
	/**
	 * @param email address
	 * @return true is the address is already in the file, false otherwise.
	 */
	private synchronized boolean exists(String email){
		boolean found = false;
		File file = new File(usersPath);
		try {
			if(file.exists() && !file.isDirectory()){
				BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
				String line = new String();
				while ((line = reader.readLine()) != null) {
					if(line.equals(email)){
						found = true;
					}
				}	
				reader.close();
			}
		} catch (IOException e) {
				e.printStackTrace();
			}		
		return found;
		
	}
	
	/**
	 * @param emailParser which parses the address so it can be added to the file.
	 */
	private synchronized void addOnFile(String emailParser) {
		PrintWriter pw;
		try {
			pw = new PrintWriter(new FileOutputStream(new File(usersPath), true /* append = true */));
			pw.append(emailParser+"\n");
			WebHandler.updateLog(LocalDateTime.now().toString() + " - " + emailParser + " registrato");
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param emailParser which parses the address so it can be removed from the file.
	 */
	private synchronized void remFromFile(String emailParser) {
		File file = new File(usersPath);
		File temp;
		try {
			if(file.exists() && !file.isDirectory()){
				temp = File.createTempFile("tmp", ".txt", file.getParentFile());
				String delete = emailParser;
				BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
				PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(temp)));
				String line = new String();
				while ((line = reader.readLine()) != null) {
					line = line.replace(delete, "");
					if(!line.equals("")){
						writer.println(line);
					}
				}
				reader.close();
				writer.close();
				file.delete();
				WebHandler.updateLog(LocalDateTime.now().toString() + " - " + emailParser + " eliminato");
				temp.renameTo(file);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @param email to be parsed
	 * @return the parsed address
	 */
	private String emailParser(String email) {
		String tmp = email.split("=")[1];
		return tmp.replace("%40", "@");
	}
	
	/**
	 * @param in input to be parsed
	 * @param what e-mail address or button name?
	 * @return input parsed
	 */
	private String inputParser(String in, String what) {
		String ret = new String();
		if(what.equals("mail")){
			ret = in.split("&")[0];
		}
		else if(what.equals("iname")){
			ret = in.split("&")[1];
		}
		return ret;
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
				+ "<div class=\"col-md-8\" style=\"background-color: aliceblue; border-radius: 40px; padding: 20px; top: 5ex; margin-bottom: 10ex\">"
				+ "<h1 style=\"text-align: center; margin-bottom: 4ex;\">Notifiche mail</h1>"
				+ "<form method=\"post\" style=\"text-align: center;\">Indirizzo e-mail: <input type=\"email\" name=\"mail\" style=\"margin-right: 2%;\">"
				+ "<button style=\"margin-left: 2%; margin-right:2%;\" class=\"button\" type=\"submit\" name=\"reg\" value=\"Registrati\">Registrati</button>"
				+ "<button style=\"margin-left: 2%; margin-right:2%;\" class=\"button\" type=\"submit\" name=\"del\" value=\"Cancellati\">Cancellati</button></form>"
				+ "<br></br>"
				+ "<form action=\"../\" style=\"text-align: right;\"><button class=\"button\" type=\"submit\" value=\"Home\">Home</button></form>"
				+ "<div class=\"col-md-2\"></div><body></html>";
		he.sendResponseHeaders(200, response.length());
		OutputStream os = he.getResponseBody();
		os.write(response.toString().getBytes());
		InputStreamReader isr = new InputStreamReader(he.getRequestBody(), "utf-8");
		BufferedReader br = new BufferedReader(isr);
		String query = br.readLine();
		String email = emailParser(inputParser(query,"mail"));
		if(inputParser(query,"iname").contains("reg")){
			if(!exists(email)){
				addOnFile(email);
			}
		}
		else if(inputParser(query,"iname").contains("del")){
			remFromFile(email);
		}
		os.close();
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
