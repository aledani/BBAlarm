package packageWeb;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * This class handles the communications received from the BB device. It updates the log file
 * when an alarm occurs and it sends e-mails to registered users.
 * 
 * @author Daniele Scattone (matr. 20006361)
 * @author Alessio Poggio (matr. 20008565)
 */


public class WebHandler implements Runnable {
	/* 
	 * Riceve comunicazioni dalla BB device: aggiorna registro allarmi
	 * e manda e-mail a chi è registrato al servizio di notifica.
	 */
	
	private static int port;
	private static Socket connectionSocket;
	private static String username = ""; //indirizzo gmail che manda le mail di avviso (esempio: example@gmail.com)
	private static String password = ""; //password dell' account
	
	/**
	 * @param connectionSocket
	 */
	public WebHandler(Socket connectionSocket) {
		this.connectionSocket = connectionSocket;
	}
	
	/**
	 * @param action to be written in the log file
	 * @return true if the log file has been updated, false otherwise.
	 */
	public synchronized static boolean updateLog(String action) {
		/* aggiorna registro allarmi posto in WebLogHandler.logPath */
		boolean updated = false;
		PrintWriter pw;
		try {
			pw = new PrintWriter(new FileOutputStream(new File(WebLogHandler.getLogPath()), true /* append = true */));
			pw.append(action+"\n");
			pw.close();
			updated = true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}			
		return updated;
	}
	
	/**
	 * @return true if the users file is not empty and it sends the e-mails or if the users file is empty, false otherwise.
	 */
	private synchronized boolean alert() {
		/* manda mail a tutti gli utenti registrati in WebMailHandler.usersPath */
		boolean done = false;
		File file = new File(WebMailHandler.getUsersPath());
		if(file.exists() && !file.isDirectory()){
			BufferedReader reader;
			try {
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
				String line = new String();
				while ((line = reader.readLine()) != null) {
					sendMail(line);
				}
				reader.close();
				done = true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return done;
	}
	
	private boolean sendMail(String daddress) {
		/* manda mail a un utente registrato */
		boolean sent = false;
		
		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");

		Session session = Session.getInstance(props,new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);}});
		try {
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(username));
			message.setRecipients(Message.RecipientType.TO,InternetAddress.parse(daddress)); 
			message.setSubject("Attenzione - Allarme!");
			message.setText("Allarme attivato il " + LocalDateTime.now().toString() + ".\n");
			Transport.send(message);
			System.out.println("mail sent to " + daddress);
			updateLog(LocalDateTime.now().toString() + " - mail sent to " + daddress);
			sent = true;
		} catch (MessagingException e) {
			updateLog(LocalDateTime.now().toString() + " - can't connect to mail server");
			updateLog(LocalDateTime.now().toString() + " - ALARM!");
			throw new RuntimeException(e);
		}
		return sent;
	}
 
	/**
	 * Listens for communications from the BB device. If an alarm is received it sends e-mails
	 * and updates the log file. 
	/* @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		/* quando scatta l'allarme richiama metodi alert() e updateLog() */
		String clientSentence;
		String responseSentence = "";
		BufferedReader inFromClient;
		try {
			inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
			DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
			clientSentence = inFromClient.readLine();
			/*decripto*/
			String responseDec = WebAes.decrypt(clientSentence);
			if(responseDec.substring(0, 13).equals("$HELLO_PIZZA$")){
				if(responseDec.contains("BEEP")){
					/* allarme ricevuto */
					System.out.println("ALARM! sending mails...");
					if(alert()){
						System.out.println("updating log...");
						if(updateLog(LocalDateTime.now().toString() + " - ALLARME!")){
							System.out.println("Done.");
							responseSentence = "$HI$OK$BYE$";						
						}
						else{
							/* responseSentence NOLOG, eventualmente gestibile da BBclient */
							System.out.println("Error updating log file!");
							responseSentence = "$HI$NOLOG$BYE$";
						}
					}
					else{
						System.out.println("Error sending mails!");
						responseSentence = "$HI$OUCH$BYE$";
					}
				}
				/*cripto*/
				String sentenceEnc = WebAes.encrypt(responseSentence);
				outToClient.writeBytes(sentenceEnc + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	/**
	 * Reads the configuration file Webcfg and sets the parameters for the connection.
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
						if((line.split("\\s+")[0]).contains("portHandler")){
							port = Integer.parseInt(line.split("\\s+")[1]);
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
	 * Starts the WebHandler and creates a new Thread for each connection.
	 * @param args
	 * @throws IOException
	 */
	public static void main(String args[]) throws IOException {
		setVars();
		ServerSocket welcomeSocket = new ServerSocket(port);
		System.out.println("WebHandler started at port: " + port);
		PrintWriter pw = new PrintWriter(new FileOutputStream(new File(WebMailHandler.getUsersPath()), true /* append = true */));
		pw.close();
		while (true) {
			Socket connectionSocket = welcomeSocket.accept();
			new Thread(new WebHandler(connectionSocket)).start();
		}
	}
	
}
