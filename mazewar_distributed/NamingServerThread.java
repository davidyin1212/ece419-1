import java.net.*;
import java.io.*;
import java.util.*;

public class NamingServerThread extends Thread{
	private Socket socket = null;
	private HashMap<String, Streams> client_list = null;
	private Vector<Streams> players = null;

	public NamingServerThread(Socket socket, HashMap<String, Streams> client_list, Vector<Streams> playerz) {
		super("NamingServerThread");
		this.socket = socket;
		this.client_list = client_list;
		this.players = playerz;
		System.out.println("Created new Thread to handle client");
	}

	public void run() {
		boolean gotByePacket = false;
		
		try {
			/* stream to read from client */
			ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
			InstructionPacket packetFromClient;
			
			/* stream to write back to client */
			ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());

						
		//	packetFromClient = (InstructionPacket)fromClient.readObject();
			while (( packetFromClient = (InstructionPacket) fromClient.readObject()) != null) {
				/* create a packet to send reply back to client */		
				InstructionPacket packetToClient = new InstructionPacket();
				
				/* check if dup client id */
				if(client_list.get(packetFromClient.client_id) == null)
				{
						
						//NOT A DUPLICATE
						//send list of already existing clients
						packetToClient.request_type = InstructionPacket.SERVER_ACCEPT;
						toClient.writeObject(packetToClient);
						
						//Set client_set = players.entrySet();
						Iterator i = players.iterator();
						
						
						
						//send all the locations of other players to new player
						while (i.hasNext()){
							//Map.Entry pairs = (Map.Entry)i.next();
							Streams stream = (Streams)i.next();
							InstructionPacket location = stream.location_packet;
							packetToClient = new InstructionPacket(location);
							packetToClient.request_type = InstructionPacket.SERVER_REPLY;
							toClient.writeObject(packetToClient);
						}
						
						packetToClient= new InstructionPacket();
						packetToClient.request_type = InstructionPacket.SERVER_DONE;
						toClient.writeObject(packetToClient);
						
						//waiting for position of new client
						packetFromClient = (InstructionPacket) fromClient.readObject();
						
						
						//broadcasting new client location to old clients
						//client_set = client_list.entrySet();
						i = players.iterator();
						
						ObjectOutputStream new_stream = null;
						
						
						while (i.hasNext()){
							//Map.Entry pairs = (Map.Entry)i.next();
							Streams stream = (Streams)i.next();
							new_stream = stream.outputstream;
							packetToClient = new InstructionPacket(packetFromClient);
							packetToClient.request_type = InstructionPacket.CLIENT_REGISTER;
							new_stream.writeObject(packetToClient);
						}
						
						//adding new client to clientlist
						Streams N_client = new Streams(fromClient,toClient, packetFromClient);
						
						//give the first guy the token
						if(client_list.isEmpty()==false){
							packetToClient = new InstructionPacket();
							packetToClient.request_type = InstructionPacket.CLIENT_TOKEN;
							Socket clientSocket = new Socket(packetFromClient.location.client_host, 
							packetFromClient.location.client_port);
							ObjectOutputStream ostream = new ObjectOutputStream(clientSocket.getOutputStream());
							ostream.writeObject(packetToClient);
							//ostream.close();
						}
						client_list.put(packetFromClient.client_id, N_client);
						players.addElement(N_client);
						
	
				}
				/* if code comes here, there is an error in the packet */
				/*System.err.println("ERROR: Unknown BROKER_* packet!!");
				System.exit(-1);*/
				//System.out.println("quitting");
				//break;
			//}
				else{
					packetToClient.error_code = InstructionPacket.ERROR_INVALID_ID;
					toClient.writeObject(packetToClient);
					break;
				}
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
