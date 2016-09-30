package packageBB;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

@WebSocket(maxTextMessageSize = 64 * 1024)
public class BBWSUtil {
	/**
	 * When some movement is detected, it processes data and
	 * send a POST to set the alarm ON (activation of the siren / light) and
	 * contacts the external Web Server.
	 * 
	 * @author Daniele Scattone (matr. 20006361)
	 * @author Alessio Poggio (matr. 20008565)
	 */
	
	private final CountDownLatch closeLatch;
	private Session session;
	private static double timeP1 = Double.MIN_VALUE;
	private static double timeP3 = Double.MAX_VALUE;
	private final static String sec = "3"; // deve essere stringa
	private static int port;
	private static String urlServer;
	private static int portServer;
	private static String urlOsso;

	private final static String statusPath = "./status.txt"; /* il file mantiene ggiornato lo stato attuale dell'allarme */

	public BBWSUtil() {
		this.closeLatch = new CountDownLatch(1);
		setVars();
		System.out.println("BBclient running...port: " + port);
	}
	
	public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
		return this.closeLatch.await(duration, unit);
	}

	@OnWebSocketClose
	public void onClose(int statusCode, String reason) {
		System.out.printf("Connection closed: %d - %s%n", statusCode, reason);
		this.session = null;
		this.closeLatch.countDown(); // trigger latch
	}
	
	@OnWebSocketConnect
	public void onConnect(Session session) {
		System.out.printf("Connected: %s%n", session);
		this.session = session;
		try {
			Future<Void> fut;
			fut = session.getRemote().sendStringByFuture("");
			fut.get(2, TimeUnit.SECONDS); // wait for send to complete.

			fut = session.getRemote().sendStringByFuture("");
			fut.get(2, TimeUnit.SECONDS); // wait for send to complete.
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	/** It receives a JSON message for each activated/deactivated sensor.
	 * @param msg
	 */
	@OnWebSocketMessage
	public void onMessage(String msg) {
		System.out.println(msg);
		String time = parseTime(msg);
		String rule = parseRule(msg);
		if (matchRules(rule, time)) {
			alarm();
		}
	}
	
	/**
	 * Some JSON parsing.
	 * @param msg
	 */
	private String parseRule(String msg) {
		String name = msg.split(",")[2];
		String status = msg.split(",")[3];
		name = name.split(":")[1];
		status = status.split(":")[1];
		status = status.substring(1, 2);
		return name + status;
	}
	
	/** 
	 * Some JSON parsing.
	 * @param msg
	 */
	private String parseTime(String msg) {
		String tmp = msg.split(":")[1];
		tmp = tmp.split(",")[0];
		tmp = tmp.substring(1, 15);
		return tmp;
	}

	/**
	 * This method controls the received data and triggered sensors. Depending on
	 * which configuration is active, invokes the method which triggers the
	 * alarm.
	 * 
	 * @param rule
	 * @param time
	 * @return
	 */
	private synchronized boolean matchRules(String rule, String time) {
		/* controlla match regole in BBserver.statusPath */
		boolean match = false;
		String actual = "";
		try {
			actual = check();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (actual.contains("ON")) {
			if ((rule.contains("Pir1") && rule.contains("\"1")) || (rule.contains("Pir2") && rule.contains("\"1"))
					|| (rule.contains("Pir3") && rule.contains("\"1"))) {
				match = true;
			}
		} else if (actual.contains("S1")) {
			if (rule.contains("Pir3\"1")) {
				match = true;
			}
		} else if (actual.contains("OFF")) {
			match = false;
		} else if (actual.contains("S2")) {
			if (rule.contains("Pir1\"1") || rule.contains("Pir2\"1")) {
				if (rule.contains("Pir1")) {
					timeP1 = Double.parseDouble(time);
				} else {
					timeP3 = Double.parseDouble(time);
				}
				Double timeFinal = Math.abs(timeP1 - timeP3);
				if ((timeP3 < timeP1) && timeFinal < Double
						.parseDouble(sec)) { /*
												 * sensore3 -> sensore1 a
												 * distanza di sec secondi
												 */
					match = true;
				}
			}
		}
		return match;
	}

	/**
	 * @return the alarm status.
	 * @throws IOException
	 */
	private String check() throws IOException { 
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
	 * @return true if the alarm has been activated, and false otherwise.
	 */
	private boolean alarm() {
		/* scatena l'allarme */
		boolean success = true;
		if (sirenON() == true) {
			try {
				Thread.currentThread().sleep(4000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (sirenOFF() == true) {
				success = true;
			}
		}
		if (postOnServer() == false) {
			success = false;
		}
		return success;
	}

	/**
	 * @return true if the siren has been activated, and false otherwise.
	 */
	private boolean sirenON() {
		/* attiva la sirena (o luce) */
		String sentence;
		String responseSentence;
		boolean on = true;
		try {
			Socket clientSocket = new Socket(urlOsso, 2432);
			System.out.println(new Date().toString() + " Alarm! POST request to " + urlOsso + ", port 2432");
			DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			sentence = "POST /cmd/OUT2/ON HTTP/1.1\n\r";
			outToServer.writeBytes(sentence + "\n");
			responseSentence = inFromServer.readLine();
			if (!responseSentence.contains("OK")) {
				on = false;
			}
			clientSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return on;
	}

	/**
	 * @return true if the siren has been deactivated, and false otherwise.
	 */
	private boolean sirenOFF() {
		/* disattiva la sirena (o luce) */
		String sentence;
		String responseSentence;
		boolean off = true;
		try {
			Socket clientSocket = new Socket(urlOsso, 2432);
			System.out.println(new Date().toString() + " Alarm OFF! POST request to " + urlOsso + ", port 2432");
			DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			sentence = "POST /cmd/OUT2/OFF HTTP/1.1\n\r";
			outToServer.writeBytes(sentence + "\n");
			responseSentence = inFromServer.readLine();
			if (!responseSentence.contains("OK")) {
				off = false;
			}
			clientSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return off;
	}

	/**
	 * This method contacts the Web Server when the alarm is triggered.
	 * 
	 * @return true if the method has been successfully executed, and false
	 *         otherwise.
	 */
	private boolean postOnServer() {
		/* contatta il web esterno avvisandolo dell'allarme */
		boolean posted = false;
		String sentence;
		String responseSentence;
		try {
			Socket clientSocket = new Socket(urlServer, portServer);
			DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			sentence = "$HELLO_PIZZA$BEEP$BYE$";
			/* cripto */
			String sentenceEnc = BBAes.encrypt(sentence);
			System.out.println("contacting WebHandler...ALARM!");
			outToServer.writeBytes(sentenceEnc + "\n");
			responseSentence = inFromServer.readLine();
			System.out.println("receiving from WebHandler...");
			/* decripto */
			String responseDec = BBAes.decrypt(responseSentence);
			if (responseDec.substring(0, 4).equals("$HI$")) {
				posted = true;
				System.out.println("Done.");
			}
			clientSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return posted;
	}

	/**
	 * Reads the configuration file BBcfg and sets the parameters for the
	 * connection (ports and urls).
	 */
	private synchronized static void setVars() {
		File file = new File("./BBcfg");
		if (file.exists() && !file.isDirectory()) {
			BufferedReader reader;
			try {
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
				String line = new String();
				while ((line = reader.readLine()) != null) {
					if (!line.startsWith("#") && !line.isEmpty()) {
						if((line.split("\\s+")[0]).contains("portBBc")){
							port = Integer.parseInt(line.split("\\s+")[1]);
						}
						if ((line.split("\\s+")[0]).contains("urlServer")) {
							urlServer = line.split("\\s+")[1];
						}
						if ((line.split("\\s+")[0]).contains("portServer")) {
							portServer = Integer.parseInt(line.split("\\s+")[1]);
						}
						if ((line.split("\\s+")[0]).contains("urlOsso")) {
							urlOsso = line.split("\\s+")[1];
						}
					}

				}
				reader.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}