import java.net.*;
import java.io.*;
import java.util.*;

public class OnlineBrokerThread extends Thread {
	private Socket socket = null;
	private HashMap quotes = null;

	public OnlineBrokerThread(Socket socket, HashMap quotes) {
		super("OnlineBrokerThread");
		this.socket = socket;
		this.quotes = quotes;
		System.out.println("Created new Thread to handle client");
	}

	public void run() {
		boolean gotByePacket = false;
		
		try {
			/* stream to read from client */
			ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
			BrokerPacket packetFromClient;
			
			/* stream to write back to client */
			ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());
			

			while (( packetFromClient = (BrokerPacket) fromClient.readObject()) != null) {
				/* create a packet to send reply back to client */		

				BrokerPacket packetToClient = new BrokerPacket();
				packetToClient.type = BrokerPacket.BROKER_QUOTE;
				
				/* process client request */
				if(packetFromClient.type == BrokerPacket.BROKER_REQUEST) {
					System.out.println("Request from Client: " + packetFromClient.symbol);
					Long quote = (Long)quotes.get(packetFromClient.symbol.toLowerCase());
					if(quote != null){

						packetToClient.quote = (long) quote;
					}
					else{
						//cannot find the quote, just send 0
						packetToClient.quote = (long)0;
					}
					/* send reply back to client */
					toClient.writeObject(packetToClient);
					/* wait for next packet */
					continue;
				}
				
				/* Sending an ECHO_NULL || ECHO_BYE means quit */
				if (packetFromClient.type == BrokerPacket.BROKER_NULL || packetFromClient.type == BrokerPacket.BROKER_BYE) {
					gotByePacket = true;
					System.out.println("Client request disconnect");
					/*gotByePacket = true;
					packetToClient = new BrokerPacket();
					packetToClient.type = BrokerPacket.BROKER_BYE;
					packetToClient.message = "Bye!";
					toClient.writeObject(packetToClient);*/
					break;
				}
				
				/* if code comes here, there is an error in the packet */
				System.err.println("ERROR: Unknown BROKER_* packet!!");
				System.exit(-1);
			}
			
			/* cleanup when client exits */
			fromClient.close();
			toClient.close();
			socket.close();

		} catch (IOException e) {
			if(!gotByePacket){
				e.printStackTrace();
			}
		} catch (ClassNotFoundException e) {
			if(!gotByePacket)
				e.printStackTrace();
		}
	}
}
