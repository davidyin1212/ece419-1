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


public class ClientDequeThread extends Thread{
	private Queue<InstructionPacket> actions = null;
	private HashMap<String,Client> clients = null;
	private Maze maze = null;
	
	public ClientDequeThread(Queue<InstructionPacket> actions, HashMap<String, Client> clients, Maze maze) {
		super("ClientDequeThread");
		//this.socket = socket;
		this.actions = actions;
		this.clients = clients;
		//this.clients = clients;
		this.maze = maze;
		System.out.println("Created new Thread to deque actions");
	}

	public void run() {
		//boolean gotByePacket = false;
		//create a list of sockets to client using the socket list
		
		/* stream to write back to client */
		//ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());
		//ObjectOutputStream toClient = null;
		while(true){
			//System.out.println("In deque loop");
			if(!actions.isEmpty()){
				InstructionPacket ins = actions.remove();
				System.out.println("Dequeud action: "+ins.action);
				if(ins.request_type == InstructionPacket.CLIENT_ACTION){
					if(clients.containsKey(ins.client_id)){
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
				}
				else if (ins.request_type == InstructionPacket.CLIENT_REGISTER){
					System.out.println("creating new client: "+ins.client_id); 
					RemoteClient new_client = new RemoteClient(ins.client_id, ins.pos_x, ins.pos_y, ins.orientation);
					clients.put(ins.client_id, new_client);
					maze.addClient(new_client);
				}
				
			}
			try{
				this.sleep(50);
			}
			catch(Exception e){

			}
		} 
	}
}
