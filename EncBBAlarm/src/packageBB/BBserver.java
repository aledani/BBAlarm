package packageBB;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;



public class BBserver implements Runnable {
	/**
	 * This class receives communications from the Web Server and performs POST
	 * on occupancy sensors in order to activate / deactivate them.
	 * 
	 * @author Alessio Poggio (matr. 20008565)
	 * @author Daniele Scattone (matr. 20006361)
	 */


	private static String urlOsso;
	private static String urlServer;
	private static int portWS;
	private final static String statusPath = "./status.txt"; /* il file mantiene ggiornato lo stato attuale dell'allarme */

	

	/**
	 * 
	 */
	public BBserver() {
		
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
						if ((line.split("\\s+")[0]).contains("urlServer")) {
							urlServer = line.split("\\s+")[1];
						}
						if ((line.split("\\s+")[0]).contains("portWS")) {
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
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		String destUri = "ws://"+urlServer+":"+portWS;
        WebSocketClient client = new WebSocketClient();
        BBWS socket = new BBWS();
        try
        {
            client.start();
            URI echoUri = new URI(destUri);
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            client.connect(socket,echoUri,request); 
            System.out.printf("Connecting to : %s%n",echoUri);
            socket.awaitClose(Integer.MAX_VALUE, TimeUnit.DAYS);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
       finally
       {
            try
            {
                client.stop(); 
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
       }
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

	/**
	 * Starts the BBserver.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String args[]) throws IOException {
		setVars();
		System.out.println("BBserver started");
		new Thread(new BBserver()).start();
		
	}

}
