import java.net.*;
import java.io.*;
import java.util.*;
//broadcast thread and server thread use the same socket to communicate to client
public class MazewarServerBroadcastThread extends Thread {
	//private Socket socket = null;
	//private HashMap<String, ClientLocation> clients = null;
	private Queue<InstructionPacket> actions = null;
	private HashMap<String,Streams> clients = null;

	public MazewarServerBroadcastThread(/*HashMap<String, ClientLocation> clients,*/ HashMap<String,Streams> clients, Queue<InstructionPacket> actions) {
		super("MazewarServerBroadcastThread");
		//this.socket = socket;
		this.actions = actions;
		//this.clients = clients;
		this.clients = clients;		
		System.out.println("Created new Thread to broadcast client");
	}

	public void run() {
		//boolean gotByePacket = false;
		//create a list of sockets to client using the socket list
		
		try {
			/* stream to write back to client */
			//ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());
			//ObjectOutputStream toClient = null;
			Socket to_player = null;
			ObjectOutputStream out = null;
			while (true) {
				//System.out.println("in loop");
				if(!clients.isEmpty()){
					System.out.println("client not empty");
					while(actions.peek() != null){
						System.out.println("actions not empty");
						InstructionPacket instruction = actions.remove();
						//broadcast the packet to other clients
						Set client_list = clients.entrySet();
						Iterator i = client_list.iterator();
						while(i.hasNext()){
							InstructionPacket packet_to_client = new InstructionPacket(instruction);
							Map.Entry entry = (Map.Entry)i.next();
							Streams stream =  (Streams)entry.getValue();
							System.out.println("broadcasting to "+entry.getKey()+" action: "+packet_to_client.action);
							/*to_player = new Socket(location.client_host, location.client_port);
							out = new ObjectOutputStream(to_player.getOutputStream());*/
							ObjectOutputStream outstream = stream.outputstream;
								
								

							//fill up output stream, then send over socket, then close socket
							outstream.writeObject(packet_to_client);
							System.out.println("Sent instruction"); //debug
					
						}
					}
				}
				try{
					this.sleep(50);
				}
				catch (Exception e){
					
				}
			}
			
			/* cleanup when client exits */
			//fromClient.close();
			//toClient.close();
			//socket.close();

		} catch (IOException e) {
			//if(!gotByePacket){
				e.printStackTrace();
			//}
		} 
	}

}
