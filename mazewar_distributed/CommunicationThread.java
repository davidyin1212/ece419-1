import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Vector;


public class CommunicationThread extends Thread{
	private Queue<InstructionPacket> actions = null;
	private HashMap<String,Client> clients = null;
	private Maze maze = null;
	private ServerSocket clientSocket= null;
	private Vector<Client> players = null;
	private int index = 0;
	private String client_id;
	
	public CommunicationThread(Queue<InstructionPacket> actions, HashMap<String, 
			Client> clients, Vector<Client> players,Maze maze, 
			ServerSocket clientSocket, int index, String client_id) {
		super("CommunicationThread");
		//this.socket = socket;
		this.actions = actions;
		this.clients = clients;
		//this.clients = clients;
		this.players = players; 
		this.maze = maze;
		this.clientSocket= clientSocket;
		this.index= index;
		this.client_id = client_id;
		System.out.println("Created new Thread to deque actions");
	}

	public void run() {
		//boolean gotByePacket = false;
		//create a list of sockets to client using the socket list
		
		/* stream to write back to client */
		//ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());
		//ObjectOutputStream toClient = null;
		//ServerSocket = clientSocket = null;
		Socket recieving = null;
		ObjectInputStream fromClient = null;
		ObjectOutputStream toClient = null;
		boolean listening = true;
		
		while(listening){
			//System.out.println("In deque loop");

			try{

				recieving = clientSocket.accept();
				fromClient = new ObjectInputStream(recieving.getInputStream());
				toClient = new ObjectOutputStream(recieving.getOutputStream());
				
				//System.out.println("Got a connection");
				
				//get the packet from client
				InstructionPacket packet = (InstructionPacket) fromClient.readObject();
				
				//fromClient.close();
				//toClient.close();
				//recieving.close();
				
				if(packet.request_type == InstructionPacket.CLIENT_ACTION){
					//send ack back to sender
					InstructionPacket toSender = new InstructionPacket();
					System.out.println("acking the sender");
					toSender.request_type = InstructionPacket.CLIENT_ACK;
					toClient.writeObject(toSender);
					toClient.close();
					fromClient.close();
					recieving.close();
					
					System.out.println("client id is: "+packet.client_id);
					//just draw it
					switch(packet.action){
						case InstructionPacket.CLIENT_FIRE: 
							clients.get(packet.client_id).fire();
							break;
						case InstructionPacket.CLIENT_FORWARD: 
							clients.get(packet.client_id).forward();
							break;
						case InstructionPacket.CLIENT_TURN_LEFT: 
							clients.get(packet.client_id).turnLeft();
							break;
						case InstructionPacket.CLIENT_TURN_RIGHT: 
							clients.get(packet.client_id).turnRight();
							break;
						case InstructionPacket.CLIENT_BACKWARD: 
							clients.get(packet.client_id).backup();
							break;
							
						//must add more scenarios	
						default:
							break;
						
					}
					
					//actions.add(packet);
					//maze.addClient(client)
				}
				else if(packet.request_type == InstructionPacket.CLIENT_TOKEN)
				{
					//System.out.println("got the token!");
					
					fromClient.close();
					toClient.close();
					recieving.close();
					
					
					//dequeue then broadcast and await acks
					if(!actions.isEmpty())
					{
						InstructionPacket ins = actions.remove();
						System.out.println("Dequeud action: "+ins.action);
						if(ins.request_type == InstructionPacket.CLIENT_ACTION)
						{
							//if there are other clients then broadcast
							if(clients.containsKey(ins.client_id)){
								//create packet to broadcast
								InstructionPacket packetToBroadcast = new InstructionPacket();
								packetToBroadcast.action = ins.action;
								packetToBroadcast.client_id = ins.client_id;
								packetToBroadcast.pos_x = ins.pos_x;
								packetToBroadcast.pos_y = ins.pos_y;
								packetToBroadcast.orientation = ins.orientation;
								packetToBroadcast.location = ins.location;
								
								//System.out.println("about to broadcast action to others");
								Broadcast(clients, packetToBroadcast);
								
							}
							switch(ins.action){
							case InstructionPacket.CLIENT_FIRE:
								clients.get(ins.client_id).fire();
								break;
							case InstructionPacket.CLIENT_FORWARD: 
								clients.get(ins.client_id).forward();
								break;
							case InstructionPacket.CLIENT_TURN_LEFT: 
								clients.get(ins.client_id).turnLeft();
								break;
							case InstructionPacket.CLIENT_TURN_RIGHT: 
								clients.get(ins.client_id).turnRight();
								break;
							case InstructionPacket.CLIENT_BACKWARD: 
								clients.get(ins.client_id).backup();
								break;
							//must add more scenarios	
							default:
								break;
							
							}
							
							
						}
						
						//DYNAMIC JOIN
						else if (ins.request_type == InstructionPacket.CLIENT_REGISTER){
							System.out.println("creating new client: "+ins.client_id); 
							RemoteClient new_client = new RemoteClient(ins.client_id, ins.pos_x, ins.pos_y, ins.orientation);
							clients.put(ins.client_id, new_client);
							maze.addClient(new_client);
						}
					}
					//forward the token lol
					
					InstructionPacket token = new InstructionPacket();
					token.request_type= InstructionPacket.CLIENT_TOKEN;
					
					
					//forward to first player							
					if(players.size()-1==index)
					{
						String hostname= (players.get(0)).getHostname();
						int port = (players.get(0)).getPort();
						//System.out.println("forwarding to first player on port: "+port+" hostname: "+hostname);
						
						Socket socket = new Socket(hostname, port);
						ObjectOutputStream ostream = new ObjectOutputStream(socket.getOutputStream());
						ostream.writeObject(token);
						Thread.sleep(50);
						ostream.close();
						socket.close();
						
					}
					
					//forward to next player
					else
					{
						
						String hostname= (players.get(index+1)).getHostname();
						int port = (players.get(index+1)).getPort();
						//System.out.println("forwarding to next player on port: "+port+" hostname: "+hostname);
						
						Socket socket = new Socket(hostname, port);
						ObjectOutputStream ostream = new ObjectOutputStream(socket.getOutputStream());
						ostream.writeObject(token);
						Thread.sleep(50);
						ostream.close();
						socket.close();
					}
				}
				else{
					//invalid packet
				}
			
			
			
			}
			catch (IOException e){
				e.printStackTrace();
			}
			catch (ClassNotFoundException e){
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
				
			}
	}

	private void Broadcast(HashMap<String, Client> clients, InstructionPacket packetToBroadcast) throws UnknownHostException, IOException, ClassNotFoundException {
		// Function that broadcasts packetToBroadcast to every client in clients
		Set client_set = clients.entrySet();
		Iterator i = client_set.iterator();
		
		InstructionPacket packetToClient=null, packetFromClient=null;
		
		while (i.hasNext()){
			Map.Entry pairs = (Map.Entry)i.next();
			Client client = (Client)pairs.getValue();
			
			
			String hostname = client.getHostname();
			int port = client.getPort();
			System.out.println("hostname is: "+hostname+" port is: "+port);
			ObjectOutputStream ostream = null;
			ObjectInputStream istream = null;
			Socket socket = null;
			System.out.println("comparing getname: "+client.getName()+" with "+this.client_id);
			if(!client.getName().equals(this.client_id)){
				
				socket = new Socket(hostname, port);
				ostream = new ObjectOutputStream(socket.getOutputStream());
				istream = new ObjectInputStream(socket.getInputStream());
				
				packetToClient = new InstructionPacket(packetToBroadcast);
				packetToClient.request_type = InstructionPacket.CLIENT_ACTION;
				ostream.writeObject(packetToClient);
				
				//wait for ACK's
				packetFromClient = (InstructionPacket) istream.readObject();
				if(packetFromClient.request_type!=InstructionPacket.CLIENT_ACK)
				{
					//received ack lol
					System.out.println("Got ack");
				}
				ostream.close();
				istream.close();
				socket.close();
			}

		}
		
	}
}
