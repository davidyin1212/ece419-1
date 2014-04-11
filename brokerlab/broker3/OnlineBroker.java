import java.net.*;
import java.io.*;
import java.util.*;


public class OnlineBroker {
//This is the server class
//the server should set QUOTES to whatever the argument was given in the terminal
//private static String QUOTES = "nasdaq";
//private static final int SERVICE_PORT=1234;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
	   Socket namingSocket = null;
	   ObjectOutputStream out = null;
	   ObjectInputStream in = null;
	   ObjectOutputStream outNS = null;
	   ObjectInputStream inNS = null;
        boolean listening = true;
        
       	//parse the arguments 
        
	  /* # $1 = hostname of BrokerLookupServer
		# $2 = port where BrokerLookupServer is listening
		# $3 = port where I will be listening
		# $4 = my name ("nasdaq" or "tse")*/
	
	
	
	HashMap<String, Long> quotes = new HashMap<String, Long>();
	BufferedReader reader = null;
	String line;
	File file = null;
	String QUOTES = null;
	
	//creating server
     try {
		//System.out.print("arg length "+args.length);
		if(args.length == 4) {
			//create socket for port
			serverSocket = new ServerSocket(Integer.parseInt(args[2]));
			
			//check if the text is a valid text file
			QUOTES = args[3];
			file = new File(QUOTES);
			if(!file.exists()){
     				System.err.println("ERROR: invalid file name!");
     				System.exit(-1);			
			}
			
		} 
		else {
		     System.err.println("ERROR: Invalid arguments!");
		     System.exit(-1);
		}

     } catch (IOException e) {
         System.err.println("ERROR: Could not listen on port!");
         System.exit(-1);
     }
     catch(NumberFormatException e){
     	System.err.println("ERROR: port name is not a number!");
     	System.exit(-1);
     }

	//cache the contents of the quote file inside a HashMap
	try {
		reader = new BufferedReader(new FileReader(file));
		line = reader.readLine();
		while(line != null){
			String[] result = line.split("\\s");
			//check to see if content of file is correct format
			if(result.length == 2){
				quotes.put(result[0], Long.valueOf(Long.parseLong(result[1])));

				//parselong takes in string and spits out long,
				//valueof takes in long and spits out Long object needed as value to the hash.
			}
			line = reader.readLine();
		}
		reader.close();	
	}
	catch (FileNotFoundException e){
		e.printStackTrace();
	}
	catch (IOException e){
		e.printStackTrace();
	}

	//get the set from the quotes hash, and construct an iterator to iterate through the hash
	/*Set set = quotes.entrySet();
	Iterator i = set.iterator();
	
	System.out.println("quotes from file:");
	while(i.hasNext()){
		Map.Entry me = (Map.Entry)i.next();
         	System.out.print(me.getKey() + ":");
         	System.out.println(me.getValue());
	}
	System.out.println();*/
	
	//server has started
	System.out.println("Listening on port: "+serverSocket.getLocalPort());

	//server adds itself to the list of running servers that naming service has
	//by issuing an add command to the naming service

	//connecting to naming service
	try{
		namingSocket = new Socket(args[0],Integer.parseInt(args[1]));
		
		outNS = new ObjectOutputStream(namingSocket.getOutputStream());
		inNS = new ObjectInputStream(namingSocket.getInputStream());
	} catch (UnknownHostException e) {
			System.err.println("ERROR: Invalid Lookup server!!!");
			System.exit(1);
	} catch (IOException e) {
			System.err.println("ERROR: Couldn't get I/O for the connection.");
			System.exit(1);
	}
	
	/* stream to write back to client */
	//ObjectOutputStream toNamingServer = new ObjectOutputStream(socket.getOutputStream());
	//ObjectInputStream fromNamingServer = new ObjectInputStream(socket.getInputStream());
	
	//packet to receive from naming service
	BrokerPacket packetFromNamingService;
	
	//packet to send to naming service
	BrokerPacket packetToNamingService= new BrokerPacket();		
	packetToNamingService.type=BrokerPacket.LOOKUP_REGISTER;
	packetToNamingService.symbol= args[3];
	
	packetToNamingService.locations = new BrokerLocation[1];
	System.out.println("number of locations:"+packetToNamingService.locations.length);
	
	packetToNamingService.locations[0] = new BrokerLocation("localhost",Integer.parseInt(args[2]));

	//sending packet to naming service
	outNS.writeObject(packetToNamingService);

	//waiting for reply from naming service
	try {
		packetFromNamingService = (BrokerPacket) inNS.readObject();
		if(packetFromNamingService.error_code==BrokerPacket.ERROR_SERVER_ALREADY_REGISTERED)
		{
			//terminate gracefully
			packetToNamingService = new BrokerPacket();
			packetToNamingService.type = BrokerPacket.BROKER_BYE;
			outNS.writeObject(packetToNamingService);
			
			inNS.close();
			outNS.close();
			namingSocket.close();
			serverSocket.close();
			System.err.println("ERROR: Server with that same name already exists!");
			System.exit(-1);
		}

     } catch ( ClassNotFoundException e ) {
     	//terminate gracefully
     	packetToNamingService = new BrokerPacket();
		packetToNamingService.type = BrokerPacket.BROKER_BYE;
		outNS.writeObject(packetToNamingService);
     
		inNS.close();
		outNS.close();
		serverSocket.close();
		namingSocket.close();
         	System.err.println("ERROR: Class not found?!");
		System.exit(1);
     }

	//inNS.close();
	//outNS.close();
	//namingSocket.close();
	
    //terminate the connection to the lookup server
   /* packetToNamingService = new BrokerPacket();
    packetToNamingService.type = BrokerPacket.BROKER_BYE;
    outNS.writeObject(packetToNamingService);*/
    
    
	BrokerPacket packetToNS = new BrokerPacket();
	packetToNS.type = BrokerPacket.BROKER_BYE;
	packetToNS.symbol = "CLIENT";
	outNS.writeObject(packetToNS);
     inNS.close();
     outNS.close();
     namingSocket.close();
    
     while (listening) {
     		System.out.println("im listening");
		//cache the contents of the nasdaq quotes upon startup
        	new OnlineBrokerThread(serverSocket.accept(), quotes, QUOTES, args[0], Integer.parseInt(args[1])).start();
     }
    	/* tell server that i'm quitting */
	packetToNS.type = BrokerPacket.BROKER_BYE;

	//used to help the server identify type of client terminating to decide to flush to the database or not
	packetToNS.symbol = new String("CLIENT");
	outNS.writeObject(packetToNS);

	outNS.close();
	inNS.close();
	namingSocket.close();
	serverSocket.close();
     
     //will always terminate ungracefully
    }
}
