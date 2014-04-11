import java.net.*;
import java.io.*;
import java.util.*;

public class BrokerExchange {

private static final String QUOTES = "list";

public static void main(String[] args) throws IOException,
			ClassNotFoundException {

		// arguments to BrokerExchange
		// $1 = hostname of where BrokerLookupServer is located
		// $2 = port # where BrokerLookupServer is listening
		// $3 = name of broker you are connecting to ("nasdaq" or "tse")

		Socket brokerSocket = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;
		ObjectOutputStream outNS = null;
		ObjectInputStream inNS = null;
		Socket NSSocket = null;

		try {
			//hostname should be specified in the terminal, and should be the same
			//as the name of the server this broker will try to connect to
			//then this broker should access name server to find port number of server
			//specified by its own hostname

			/* variables for hostname/port */
			String hostNS = null;
			int portNS = 0;
			if(args.length == 3) {
				hostNS = args[0];
				portNS = Integer.parseInt(args[1]);		

			} else {
				System.err.println("ERROR: Invalid arguments!");
				System.exit(-1);
			}
			NSSocket = new Socket(hostNS, portNS);
			//brokerSocket = new Socket("nasdaq",8000);
			outNS = new ObjectOutputStream(NSSocket.getOutputStream());
			inNS = new ObjectInputStream(NSSocket.getInputStream());

		} catch (UnknownHostException e) {
			System.err.println("ERROR: Don't know where to connect!!");
			System.exit(1);
		} catch (IOException e) {
			System.err.println("ERROR: Couldn't get I/O for the connection.");
			System.exit(1);
		}

		BrokerPacket packetToNS = new BrokerPacket();
		BrokerPacket packetFromNS;
		packetToNS.type=BrokerPacket.LOOKUP_REQUEST;
		packetToNS.symbol= args[2];

		outNS.writeObject(packetToNS);

		packetFromNS= (BrokerPacket) inNS.readObject();

		if(packetFromNS.error_code==BrokerPacket.BROKER_NULL)
		{
			/* variables for hostname/port */
			String host=packetFromNS.locations[0].broker_host;
			int port=packetFromNS.locations[0].broker_port;

			try {
				//hostname should be specified in the terminal, and should be the same
				//as the name of the server this broker will try to connect to
				//then this broker should access name server to find port number of server
				//specified by its own hostname	

				brokerSocket = new Socket(host, port);
				//brokerSocket = new Socket("nasdaq",8000);
				out = new ObjectOutputStream(brokerSocket.getOutputStream());
				in = new ObjectInputStream(brokerSocket.getInputStream());

			} catch (UnknownHostException e) {
				System.err.println("ERROR: Don't know where to connect!!");
				System.exit(1);
			} catch (IOException e) {
				System.err.println("ERROR: Couldn't get I/O for the connection.");
				System.exit(1);
			}
		}

		else if(packetFromNS.error_code==BrokerPacket.ERROR_SERVER_NOT_REGISTERED)
		{
			System.err.println("ERROR: Server doesn't exist!!");
			packetToNS = new BrokerPacket();
			packetToNS.type= BrokerPacket.BROKER_BYE;
			packetToNS.symbol="EXCHANGE";
			outNS.writeObject(packetToNS);
			outNS.close();
			inNS.close();
			NSSocket.close();
			System.exit(1);
		}

		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		String userInput;
		
		System.out.print("Enter command or quit for exit:\n> ");
		while ((userInput = stdIn.readLine()) != null && userInput.toLowerCase().indexOf("quit") == -1) {
			//process the user input
			//split the user input into tokens separated by spaces
			StringTokenizer st = new StringTokenizer(userInput);
			//System.out.println("number of tokens: "+st.countTokens());
			if(st.countTokens() >= 2){
				BrokerPacket packetToServer = new BrokerPacket();
				String function = st.nextToken();
				//System.out.print("Token: "+function);
				if(function.toLowerCase().equals("add")){
					if(st.countTokens() == 1){
						packetToServer.type = BrokerPacket.EXCHANGE_ADD;
						packetToServer.symbol = st.nextToken();
					}	
					else{
						System.out.println("invalid number of arguments");
					}
				}
				else if(function.toLowerCase().equals("update")){
					if(st.countTokens() == 2){
						packetToServer.type = BrokerPacket.EXCHANGE_UPDATE;
						packetToServer.symbol = st.nextToken();
						//try catch this exception if conversion fails
						try{
							packetToServer.quote = Long.parseLong(st.nextToken());
						}
						catch(NumberFormatException e){
							System.out.println("cannot update to this value. it is not a number");
							System.out.print("> ");
							continue;
						}
					}
					else{
						System.out.println("invalid number of arguments");
					}
				}
				else if(function.toLowerCase().equals("remove")){
					if(st.countTokens() == 1){
						packetToServer.type = BrokerPacket.EXCHANGE_REMOVE;
						packetToServer.symbol = st.nextToken();
					}
					else{
						System.out.println("invalid number of arguments");
					}
				}
				else{
					System.out.print("Invalid arguments");
				}
				
				
				//process the reply from server
				if(packetToServer.type != BrokerPacket.BROKER_NULL){
					out.writeObject(packetToServer);
				
					BrokerPacket packetFromServer;
					packetFromServer = (BrokerPacket) in.readObject();
					
					//process the error code reply from server
					if(packetFromServer.error_code != 0){
						//System.out.println("error from server: "+packetFromServer.error_code);
						if(packetFromServer.error_code == BrokerPacket.ERROR_INVALID_SYMBOL){
							System.out.println(packetToServer.symbol+" invalid.");
						}
						else if(packetFromServer.error_code == BrokerPacket.ERROR_OUT_OF_RANGE){
							System.out.println(packetToServer.symbol+" out of range.");
						}
						else if(packetFromServer.error_code == BrokerPacket.ERROR_SYMBOL_EXISTS){
							System.out.println(packetToServer.symbol+" exists.");
						}
						else{
							System.out.println("unknown error code.");
						}
					}
					//successful transaction
					else{
						//check type of return from server
						if(packetToServer.type == BrokerPacket.EXCHANGE_ADD){
							System.out.println(packetToServer.symbol+" added.");
						}
						else if(packetToServer.type == BrokerPacket.EXCHANGE_UPDATE){
							System.out.println(packetToServer.symbol+" updated to "+packetToServer.quote+".");
						}
						else if(packetToServer.type == BrokerPacket.EXCHANGE_REMOVE){
							System.out.println(packetToServer.symbol+" removed.");
						}
					}
				}
			}
			else{
				System.out.println("invalid number of arguments");
			}
			/* re-print console prompt */
			System.out.print("> ");
		}

		/* tell server that i'm quitting */
		packetToNS = new BrokerPacket();
		packetToNS.type = BrokerPacket.BROKER_BYE;

		//used to help the server identify type of client terminating to decide to flush to the database or not
		packetToNS.symbol = new String("EXCHANGE");
		outNS.writeObject(packetToNS);

		outNS.close();
		inNS.close();
		NSSocket.close();

		/* tell server that i'm quitting */
		BrokerPacket packetToServer = new BrokerPacket();
		packetToServer.type = BrokerPacket.BROKER_BYE;

		//used to help the server identify type of client terminating to decide to flush to the database or not
		packetToServer.symbol = new String("EXCHANGE");
		out.writeObject(packetToServer);

		out.close();
		in.close();
		stdIn.close();
		brokerSocket.close();
	}
}
