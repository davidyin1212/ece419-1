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


public class ServerListeningThread extends Thread{
	private Queue<InstructionPacket> actions = null;
	private HashMap<String,Client> clients = null;
	private ObjectInputStream in = null;
	private String my_ID = null;
	
	public ServerListeningThread(ObjectInputStream in, Queue<InstructionPacket> actions, String my_ID) {
		super("ServerListeningThread");
		//this.socket = socket;
		this.actions = actions;
		this.in = in;
		this.my_ID = my_ID;
		//this.clients = clients;		
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
				if (ins.request_type == InstructionPacket.CLIENT_ACTION){
					System.out.println("recieved action: "+ins.action);
					actions.add(ins);
				}
				
				else if( ins.request_type == InstructionPacket.CLIENT_REGISTER)
				{
					System.out.println("recieved action: "+ins.action);
					System.out.println("my client id is: "+this.my_ID);
					if(!ins.client_id.equals(this.my_ID)){
						System.out.println("adding shit to actions");
						actions.add(ins);
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
