package packageWeb;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.time.LocalDateTime; 

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;

/**
 * This class is the core component of the Web Server: it creates a new context for each web page visited.
 * The web pages are also protected by username and password.
 * 
 * @author Alessio Poggio (matr. 20008565)
 * @author Daniele Scattone (matr. 20006361)
 */

public class WebServer {
	/* 
	 * Fornisce i servizi di registrazione per notifiche e-mail, consultazione del
	 * registro degli allarmi scattati e possibilità di attivare o disattivare l'allarme.
	 * Modifica le regole.
	 */
	private final static String statusPath = "./status.txt"; /* il file mantiene ggiornato lo stato attuale dell'allarme */
	private static int port;
	private static int portWS;

	private static final String username = "admin";
	private static final String password = "nonteladico";
	
	private static int init = 1;
	
	/**
	 * @throws IOException if some error occur while processing the log file.
	 */
	public WebServer() throws IOException {
		setVars();
		System.out.println("starting server...");
		HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
		System.out.println("server started at port: " + port);
		WebHandler.updateLog(LocalDateTime.now().toString() + " - server started at port: " + port);
		HttpContext hc1 = server.createContext("/", new WebRootHandler());
		/* new context per ogni servizio fornito */
		HttpContext hc2 = server.createContext("/rules", new WebRulesHandler());
		HttpContext hc3 = server.createContext("/mail", new WebMailHandler());
		HttpContext hc4 = server.createContext("/log", new WebLogHandler());
		server.createContext("/status", new WebCheckStatus());
		hc1.setAuthenticator(new BasicAuthenticator("Inserisci username e password.") {
	        @Override
	        public boolean checkCredentials(String user, String pwd) {
	            return user.equals(username) && pwd.equals(password);
	        }
	    });
		hc2.setAuthenticator(new BasicAuthenticator("Inserisci username e password.") {
	        @Override
	        public boolean checkCredentials(String user, String pwd) {
	            return user.equals(username) && pwd.equals(password);
	        }
	    });
		hc3.setAuthenticator(new BasicAuthenticator("Inserisci username e password.") {
	        @Override
	        public boolean checkCredentials(String user, String pwd) {
	            return user.equals(username) && pwd.equals(password);
	        }
	    });
		hc4.setAuthenticator(new BasicAuthenticator("Inserisci username e password.") {
	        @Override
	        public boolean checkCredentials(String user, String pwd) {
	            return user.equals(username) && pwd.equals(password);
	        }
	    });
		server.setExecutor(null);
		server.start();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				System.out.println("server closed.");
				WebHandler.updateLog(LocalDateTime.now().toString() + " - server closed.");
			}
		});
	}	
	
	/**
	 * @return the value of init
	 */
	public static int getInit() {
		return init;
	}

	/**
	 * @param init 
	 */
	public static void setInit(int init) {
		WebServer.init = init;
	}
	
	/**
	 * Reads the configuration file Webcfg and sets the parameters for the connection (ports and urls).
	 */
	private synchronized static void setVars(){
		File file = new File("./Webcfg");
		if(file.exists() && !file.isDirectory()){
			BufferedReader reader;
			try {
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
				String line = new String();
				while ((line = reader.readLine()) != null) {
					if (!line.startsWith("#") && !line.isEmpty()){
						if((line.split("\\s+")[0]).contains("portServer")){
							port = Integer.parseInt(line.split("\\s+")[1]);
						}
						if((line.split("\\s+")[0]).contains("portWS")){
							portWS = Integer.parseInt(line.split("\\s+")[1]);
						}
					}
				}
				reader.close();
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
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
	
	/**
	 * Starts the WebServer and resets the value of init every 5 minutes for logging purpose.
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		new WebServer();
		setStatus("ON");
		new Thread(new Runnable(){
			public void run(){
				Server server = new Server(portWS);
		        WebSocketHandler wsHandler = new WebSocketHandler() {
		            @Override
		            public void configure(WebSocketServletFactory factory) {
		                factory.register(MyWebSocketHandler.class);
		            }
		        };
		        server.setHandler(wsHandler);
		        try {
					server.start();
					server.join();
				} catch (Exception e) {
					e.printStackTrace();
				}  
			}
		}).start();
		new Thread(new Runnable(){
			public void run(){
				while(true){
					init = 1;
					try {
						Thread.currentThread().sleep(300000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}
	
}
