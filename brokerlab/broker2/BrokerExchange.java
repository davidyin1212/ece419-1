import java.net.*;
import java.io.*;
import java.util.*;

public class BrokerExchange {
public static void main(String[] args) throws IOException,
			ClassNotFoundException {

		Socket brokerSocket = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;

		try {
			/* variables for hostname/port */
			String hostname = "localhost";
			int port = 4444;
			
			if(args.length == 2 ) {
				hostname = args[0];
				port = Integer.parseInt(args[1]);
			} else {
				System.err.println("ERROR: Invalid arguments!");
				System.exit(-1);
			}
			brokerSocket = new Socket(hostname, port);

			out = new ObjectOutputStream(brokerSocket.getOutputStream());
			in = new ObjectInputStream(brokerSocket.getInputStream());

		} catch (UnknownHostException e) {
			System.err.println("ERROR: Don't know where to connect!!");
			System.exit(1);
		} catch (IOException e) {
			System.err.println("ERROR: Couldn't get I/O for the connection.");
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
		BrokerPacket packetToServer = new BrokerPacket();
		packetToServer.type = BrokerPacket.BROKER_BYE;
		packetToServer.symbol = new String("EXCHANGE");
		out.writeObject(packetToServer);

		out.close();
		in.close();
		stdIn.close();
		brokerSocket.close();
	}
}
