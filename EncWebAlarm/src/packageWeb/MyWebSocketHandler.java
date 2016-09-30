package packageWeb;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

@WebSocket
public class MyWebSocketHandler {
	
	private final static String statusPath = "./status.txt"; /* il file mantiene ggiornato lo stato attuale dell'allarme */
	private static Session sess;
	
    public static Session getSess() {
		return sess;
	}

	@OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        System.out.println("Close: statusCode=" + statusCode + ", reason=" + reason);
    }

    @OnWebSocketError
    public void onError(Throwable t) {
        System.out.println("Error: " + t.getMessage());
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
    	this.sess = session;
    	session.setIdleTimeout(Long.MAX_VALUE);
        System.out.println("Connect: " + session.getRemoteAddress().getAddress());
        try {	
            session.getRemote().sendString("{\"status\": \""+checkStatus()+"\"}");
            new Thread(new Runnable(){
    			public void run(){
    				while(true){
    					try {
    						Thread.currentThread().sleep(180000);
    						session.getRemote().sendString("{\"status\": \""+checkStatus()+"\"}");
    					} catch (InterruptedException | IOException e) {
    						e.printStackTrace();
    					}
    				}
    			}
    		}).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @OnWebSocketMessage
    public void onMessage(String message) {
        //System.out.println("Message: " + message);
    }
    

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
}