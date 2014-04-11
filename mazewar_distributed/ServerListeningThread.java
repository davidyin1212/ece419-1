import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Vector;


public class ServerListeningThread extends Thread{
	private Queue<InstructionPacket> actions = null;
	private HashMap<String,Client> clients = null;
	private Vector<Client> players = null;
	private ObjectInputStream in = null;
	private String my_ID = null;
	private Maze maze = null;
	
	public ServerListeningThread(ObjectInputStream in, Queue<InstructionPacket> actions, String my_ID, Maze maze, HashMap<String, Client> cl,Vector<Client> playerz) {
		super("ServerListeningThread");
		//this.socket = socket;
		this.actions = actions;
		this.in = in;
		this.my_ID = my_ID;
		this.maze = maze;
		this.clients = cl;
		this.players = playerz;
		System.out.println("Created new Thread to enqueue actions");
	}

	public void run() {
		//boolean gotByePacket = false;
		//create a list of sockets to client using the socket list
		
		try {
			/* stream to write back to client */
			//ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());
			//ObjectOutputStream toClient = null;
			ObjectInputStream input = this.in;
			InstructionPacket ins = null;
			
			while((ins = (InstructionPacket)input.readObject()) != null ){
				System.out.println("recieved request: "+ins.request_type);
				if (ins.request_type == InstructionPacket.CLIENT_ACTION){
					//System.out.println("recieved action: "+ins.action);
					actions.add(ins);
				}
				
				else if( ins.request_type == InstructionPacket.CLIENT_REGISTER)
				{
					System.out.println("recieved action: "+ins.action);
					if(!ins.client_id.equals(this.my_ID)){
						System.out.println("creating new client: "+ins.client_id); 
						RemoteClient new_client = new RemoteClient(ins.client_id, ins.pos_x, ins.pos_y, ins.orientation, ins.location.client_host, ins.location.client_port);
						clients.put(ins.client_id, new_client);
						players.addElement(new_client);
						maze.addClient(new_client);
					}	
					
				}
				
				else{
					System.out.println("unknown packet");
					break;
				}
			}
			
			
			/* cleanup when client exits */
			//fromClient.close();
			//toClient.close();
			//socket.close();

		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
}
