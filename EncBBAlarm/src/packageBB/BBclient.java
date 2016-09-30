package packageBB;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

public class BBclient {
	/**
	 * Client that runs on BB. It registers to receive notifications by
	 * occupancy sensors. 
	 * 
	 * @author Daniele Scattone (matr. 20006361)
	 * @author Alessio Poggio (matr. 20008565)
	 */

	/**
	 * This method tells the BB that we want to receive notifications about the
	 * events.
	 */
	private static void register() {
		String destUri = "ws://osso2.local:14994";
		WebSocketClient client = new WebSocketClient();
		BBWSUtil socket = new BBWSUtil();
		try {
			client.start();

			URI echoUri = new URI(destUri);
			ClientUpgradeRequest request = new ClientUpgradeRequest();
			client.connect(socket, echoUri, request);
			System.out.printf("Connecting to : %s%n", echoUri);

			// wait for closed socket connection.
			socket.awaitClose(Integer.MAX_VALUE, TimeUnit.DAYS);
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			try {
				client.stop();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Starts the BBclient.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String args[]) throws IOException {
		register();
	}

}
