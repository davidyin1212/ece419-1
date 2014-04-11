import java.net.*;
import java.io.*;
import java.util.*;


public class MazewarServer {

//private static final String QUOTES = "nasdaq";

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        boolean listening = true;
	
	//HashMap<String, ClientLocation> clients = new HashMap<String, ClientLocation>();
	Queue<InstructionPacket> actions = new LinkedList<InstructionPacket>();
	HashMap<String,Streams> clients = new HashMap<String, Streams>();
	//HashMap<String,InstructionPacket> locations = new HashMap<String, InstructionPacket>();

        try {
		//System.out.print("arg length "+args.length);
        	if(args.length == 1) {
        		serverSocket = new ServerSocket(Integer.parseInt(args[0]));
        	} else {
        		System.err.println("ERROR: Invalid arguments!");
        		System.exit(-1);
        	}
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }
	
		//server has started
		System.out.println("Listening on port: "+serverSocket.getLocalPort());

		//create a thread to dequeue actions
		new MazewarServerBroadcastThread(clients, actions).start();
		while (listening) {
			//created a thread to handle client 
		  	new MazewarServerThread(serverSocket.accept(), clients, actions).start();
			System.out.println("Created thread to handle client");//debug
		}
		serverSocket.close();
    }
}
