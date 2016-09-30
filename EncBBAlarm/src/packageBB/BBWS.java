package packageBB;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

/**
 * Basic Echo Client Socket
 */
@WebSocket(maxTextMessageSize = 64 * 1024)
public class BBWS {
	
	private final static String statusPath = "./status.txt"; /* il file mantiene ggiornato lo stato attuale dell'allarme */
	private static String urlOsso;
	
	private final CountDownLatch closeLatch;
	@SuppressWarnings("unused")
	private Session session;

	public BBWS() {
		setVars();
		this.closeLatch = new CountDownLatch(1);
		setStatus("ON");
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
		System.out.printf("Got connect: %s%n", session);
		this.session = session;
		try {
			Future<Void> fut;
			fut = session.getRemote().sendStringByFuture("Hello");
			fut.get(2, TimeUnit.SECONDS); // wait for send to complete.

			fut = session.getRemote().sendStringByFuture("Thanks for the conversation.");
			fut.get(2, TimeUnit.SECONDS); // wait for send to complete.

			// session.close(StatusCode.NORMAL,"I'm done");
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	/**
	 * Some JSON parsing.
	 * 
	 * @param msg
	 */
	private String parseRule(String msg) {
		String status = msg.split(":")[1];
		return status;
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
	 * It turns on the green light, alarm is on.
	 */
	private static void onGreenLight() {
		String sentence;
		String responseSentence;
		try {
			Socket clientSocket = new Socket(urlOsso, 2432);
			System.out.println(new Date().toString() + " POST green light request to " + urlOsso + ", port 2432");
			DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			sentence = "POST /cmd/OUT1/ON HTTP/1.1\n\r";
			outToServer.writeBytes(sentence + "\n");
			responseSentence = inFromServer.readLine();
			clientSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * It turns off the green light, alarm is off.
	 */
	private void offGreenLight() {
		String sentence;
		String responseSentence;
		try {
			Socket clientSocket = new Socket(urlOsso, 2432);
			System.out.println(new Date().toString() + " POST green light off request to " + urlOsso + ", port 2432");
			DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			sentence = "POST /cmd/OUT1/OFF HTTP/1.1\n\r";
			outToServer.writeBytes(sentence + "\n");
			responseSentence = inFromServer.readLine();
			clientSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Reads the configuration file BBcfg and sets the parameters for the
	 * connection.
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
	


	@OnWebSocketMessage
	public void onMessage(String msg) {
		boolean flag = false;
		if (parseRule(msg).contains("ON")) {

			System.out.println("setting alarm ON...");
			/* chiama metodo per settare allarme ON */
			if (setStatus("ON")) {
				onGreenLight();
				flag = true;
			}
		} else if (parseRule(msg).contains("OFF")) {
			System.out.println("setting alarm OFF...");
			/* chiama metodo per settare allarme OFF */
			if (setStatus("OFF")) {
				offGreenLight();
				flag = true;
			}
		} else if (parseRule(msg).contains("S1")) {
			System.out.println("setting interno...");
			/* chiama metodo per settare la regola 1 */
			if (setStatus("S1")) {
				onGreenLight();
				flag = true;
			}
		} else if (parseRule(msg).contains("S2")) {
			System.out.println("setting esterno...");
			/* chiama metodo per settare la regola 2 */
			if (setStatus("S2")) {
				onGreenLight();
				flag = true;
			}
		}
		System.out.printf("Got msg: %s%n", msg);

	}
}