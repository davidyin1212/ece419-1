import java.net.*;
import java.io.*;
import java.util.*;

public class LookupServerThread extends Thread {
	private Socket socket = null;
	private HashMap<String, BrokerLocation> quotes = null;

	public LookupServerThread(Socket socket, HashMap<String, BrokerLocation> quotes) {
		super("LookupServerThread");
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
			
			//writer to the disk
			/*String file = "list";
			PrintWriter writer = new PrintWriter(file);*/
						
	
			while (( packetFromClient = (BrokerPacket) fromClient.readObject()) != null) {
				System.out.println("new iteration");
				/* create a packet to send reply back to client */		
				BrokerPacket packetToClient = new BrokerPacket();
				
				packetToClient.type=BrokerPacket.LOOKUP_REPLY;

				/* process client request */
				if(packetFromClient.type == BrokerPacket.LOOKUP_REGISTER) {
					System.out.println("Request from Client: REGISTER");
					//symbol contains name of server client is looking for
					BrokerLocation location = quotes.get(packetFromClient.symbol.toLowerCase());
					if(location != null){
						//server with that name already exists
						packetToClient.error_code=BrokerPacket.ERROR_SERVER_ALREADY_REGISTERED;
					}
					else{
						//adding server to hashmap
						quotes.put(packetFromClient.symbol.toLowerCase(), packetFromClient.locations[0]);
						System.out.println(packetFromClient.symbol.toLowerCase()+" added");
						packetToClient.error_code = 0;
					}
					packetToClient.type = BrokerPacket.LOOKUP_REPLY;
					/* send reply back to client */
					//System.out.println("writing to client register");//debug
					toClient.writeObject(packetToClient);
					/* wait for next packet */
					continue;
				}

				else if(packetFromClient.type == BrokerPacket.LOOKUP_REQUEST){
					//called by client
					packetToClient.type = BrokerPacket.LOOKUP_REPLY;
					packetToClient.locations = new BrokerLocation[1];
					System.out.println("Request from Client: port # for " +packetFromClient.symbol);
					String broker = packetFromClient.symbol.toLowerCase();
					if(quotes.containsKey(broker)){
						//set the error field, return
						//System.out.println("server found"); //debug
						int port = (quotes.get(packetFromClient.symbol)).broker_port;
						String host = (quotes.get(packetFromClient.symbol)).broker_host;
						packetToClient.error_code = 0;
						packetToClient.locations[0]= new BrokerLocation(host, port);
					}
					else{
						//System.out.println(" server not registered"); //debug
						//server doesnt exist
						packetToClient.error_code = BrokerPacket.ERROR_SERVER_NOT_REGISTERED;					
					}
					//System.out.println("writing to client request");//debug
					toClient.writeObject(packetToClient);
					//System.out.println("wrote to client request");//debug
					continue;
				}	
					
				/* Sending an ECHO_NULL || ECHO_BYE means quit */
				else if (packetFromClient.type == BrokerPacket.BROKER_NULL || packetFromClient.type == BrokerPacket.BROKER_BYE) {
					gotByePacket = true;
					System.out.println("terminating client");
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

			//close the filehandle
			//writer.close();

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
