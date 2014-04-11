import java.net.*;
import java.io.*;
import java.util.*;

public class MazewarServerThread extends Thread {
	private Socket socket = null;
	private Queue<InstructionPacket> actions = null;
	private HashMap<String, Streams> clients = null;
	//private HashMap<String, InstructionPacket> locations = null;

	public MazewarServerThread(Socket socket, HashMap<String, Streams> clients, Queue<InstructionPacket> actions) {
		super("MazewarServerThread");
		this.socket = socket;
		this.actions = actions;
		//this.clients = clients;
		this.clients = clients;	
		//this.locations = locations;
		System.out.println("Created new Thread to handle client");
	}

	public void run() {
		boolean gotByePacket = false;
		
		try {
			/* stream to read from client */
			ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());
			ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
			InstructionPacket packetFromClient;
			Streams streams = new Streams(fromClient, toClient, null);
			/* stream to write back to client */
			
			
			//writer to the disk
			//String file = "actions";
			//PrintWriter writer = new PrintWriter(file);
						
	
			while (( packetFromClient = (InstructionPacket) fromClient.readObject()) != null) {

				System.out.println("the request type from client is: "+packetFromClient.request_type);
				if(packetFromClient.request_type == InstructionPacket.CLIENT_ACTION) {
					System.out.println("Request from Client: ACTION"+packetFromClient.action);//debug
					//process request by placing action in instruction queue
					actions.add(packetFromClient);

					/* wait for next packet */
					continue;
				}

				else if(packetFromClient.request_type == InstructionPacket.CLIENT_REGISTER) {
					System.out.println("Request from Client: REGISTER");//debug

					//lookup the client name in the clients hash, terminate if already found, else add to hash
					if(clients.get(packetFromClient.client_id) == null){
						
						//first packet
						InstructionPacket confirm= new InstructionPacket();
						confirm.request_type = InstructionPacket.SERVER_ACCEPT;
						toClient.writeObject(confirm);	
						
						//waiting until queue is empty
						while(actions.isEmpty()==false){;}
						
						Set client_list = clients.entrySet();
						Iterator i = client_list.iterator();
						
						//iterating through list of clients
						//then return positions to new client
						/*while(i.hasNext()){
							;
						}*/
						
						//send all the locations of other players to new player
						while (i.hasNext()){
							Map.Entry pairs = (Map.Entry)i.next();
							Streams stream = (Streams)pairs.getValue();
							InstructionPacket location = stream.location_packet;
							confirm = new InstructionPacket(location);
							confirm.request_type = InstructionPacket.SERVER_REPLY;
							toClient.writeObject(confirm);
						}

						//final packet
						confirm= new InstructionPacket();
						confirm.request_type = InstructionPacket.SERVER_DONE;
						toClient.writeObject(confirm);	
						
						//should contain position of new client
						confirm = (InstructionPacket)fromClient.readObject();
						
						//attach the locations to confirm 
						streams.location_packet = confirm;
						clients.put(packetFromClient.client_id, streams);
						System.out.println(packetFromClient.client_id+" added ");
						System.out.println("size of hashmap now: "+clients.size());

						//now broadcast position of client to the rest of the other players
						client_list = clients.entrySet();
						i = client_list.iterator();
						//System.out.println("size of list: "+i.size());
						while(i.hasNext()){
							Map.Entry pairs = (Map.Entry)i.next();
							String player = (String)pairs.getKey();
							System.out.println("creating player on: "+player);
							Streams send = (Streams)pairs.getValue();
							ObjectOutputStream ostream = send.outputstream;
							if(!player.equals(packetFromClient.client_id)){
								InstructionPacket new_player = new InstructionPacket(packetFromClient);
								new_player.request_type = InstructionPacket.CLIENT_REGISTER;
								ostream.writeObject(new_player);
							}
						}
					}
					else{
						System.out.println("username "+packetFromClient.client_id+" already exists! try reconnecting with different username");
						InstructionPacket response = new InstructionPacket();
						response.error_code = InstructionPacket.ERROR_INVALID_ID;
						toClient.writeObject(response);
						break;
					}
					/* wait for next packet */
					continue;
				}
				/* if code comes here, there is an error in the packet */
				System.err.println("ERROR: Unknown SERVER_* packet!!");
				System.exit(-1);
			}
			
			/* cleanup when client exits */
			fromClient.close();
			//toClient.close();
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
