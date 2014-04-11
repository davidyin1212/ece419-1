import java.net.*;
import java.io.*;
import java.util.*;

public class fileServerThread extends Thread {
	private Socket socket = null;
	private ArrayList<String> dict = null;

	public fileServerThread(Socket socket, ArrayList<String> dict) {
		//super("OnlineBrokerThread");
		this.socket = socket;
		this.dict = dict;
		System.out.println("Created new Thread to handle client");
	}

	public void run() {
		boolean gotByePacket = false;
		
		try {
			/* stream to read from client */
			ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
			Packet packetFromClient;
			
			/* stream to write back to client */
			ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());
			
			//writer to the disk
			//String file = "list";
	
			//while (( packetFromClient = (Packet) fromClient.readObject()) != null) {
				/* create a packet to send reply back to client */	

				packetFromClient = (Packet) fromClient.readObject();	
				Packet packetToClient = new Packet();
				packetToClient.type=Packet.LOOKUP_REPLY;
				packetToClient.data = new ArrayList<String>();

				if(packetFromClient.type == Packet.LOOKUP_REQUEST){
					//called by client
					System.out.println("Request from Client:"+packetFromClient.type);

					packetToClient.type = Packet.LOOKUP_REPLY;
					long start = packetFromClient.start;
					long length	= packetFromClient.length;	
					
					if(start>dict.size()){
						//set the error field, return
						packetToClient.error_code = Packet.ERROR_OUT_OF_RANGE;
					}

					else{
						for(int i=(int)start; i<start+length && i<dict.size();i++)
						{
							packetToClient.data.add(dict.get(i));
						}
					}

					toClient.writeObject(packetToClient);
					//continue;
				}	
				
			//}
			
			/* cleanup when client exits */
			fromClient.close();
			toClient.close();
			socket.close();

			//close the filehandle

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