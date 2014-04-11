import java.io.*;
import java.net.*;
import java.util.*;

public class BrokerClient {
	public static void main(String[] args) throws IOException,
			ClassNotFoundException {

		Socket brokerSocket = null;
		Socket NSSocket = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;
		ObjectOutputStream outNS = null;
		ObjectInputStream inNS = null;
		String hostname = "localhost";
		int port = 1234;
		boolean connected=false;

		//connecting to naming service
		try {
			
			if(args.length == 2 ) {
				hostname = args[0];
				port = Integer.parseInt(args[1]);
			} else {
				System.err.println("ERROR: Invalid arguments!");
				System.exit(-1);
			}
			NSSocket = new Socket(hostname, port);

			//NAMING SERVICE OUT AND IN
			outNS = new ObjectOutputStream(NSSocket.getOutputStream());
			inNS = new ObjectInputStream(NSSocket.getInputStream());

		} catch (UnknownHostException e) {
			System.err.println("ERROR: Don't know where to connect!!");
			System.exit(1);
		} catch (IOException e) {
			System.err.println("ERROR: Couldn't get I/O for the connection.");
			System.exit(1);
		}

		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		String userInput;
	
		System.out.print("Enter queries or x for exit:\n> ");


		while ((userInput = stdIn.readLine()) != null
				&& userInput.toLowerCase().indexOf("x") == -1) {
			
				StringTokenizer st = new StringTokenizer(userInput);
	
				if(st.countTokens() <2 && connected == false){
					System.out.println("Must connect to server first.");
				}
			
				//only command that has 2 words should be local
				else if(st.countTokens() >= 2){
					String function = st.nextToken();	

					//local command				
					if(function.toLowerCase().equals("local") && connected==false)
					{
						BrokerPacket packetToServer = new BrokerPacket();

						packetToServer.type = BrokerPacket.LOOKUP_REQUEST;
						packetToServer.symbol = st.nextToken();
						outNS.writeObject(packetToServer);

						/* print server reply */
						BrokerPacket packetFromServer;
						packetFromServer = (BrokerPacket) inNS.readObject();
			
						if(packetFromServer.error_code==BrokerPacket.BROKER_NULL)
						{
							hostname = packetFromServer.locations[0].broker_host;
							port = packetFromServer.locations[0].broker_port;
							
							try {
								brokerSocket = new Socket(hostname, port);
								
								out = new ObjectOutputStream(brokerSocket.getOutputStream());
								in = new ObjectInputStream(brokerSocket.getInputStream());
								//System.out.println("created server streams");//debug
							} catch (UnknownHostException e) {
								System.err.println("ERROR: Don't know where to connect!!");
								System.exit(1);
							} catch (IOException e) {
								System.err.println("ERROR: Couldn't get I/O for the connection.");
								System.exit(1);
							}
							connected=true;
						}
	
						else if(packetFromServer.error_code==BrokerPacket.ERROR_SERVER_NOT_REGISTERED){
							System.out.println("Server doesn't exist.");
						}
					}
				
					//local command but already connected to a server, so must diconnect first
					else if(function.toLowerCase().equals("local") && connected==true)
					{
						/* tell server that i'm quitting */
						BrokerPacket packetToServerQuit = new BrokerPacket();
						packetToServerQuit.type = BrokerPacket.BROKER_BYE;

						//used to help the server identify type of client terminating to decide to flush to the database or not
						packetToServerQuit.symbol = new String("CLIENT");
						out.writeObject(packetToServerQuit);

						out.close();
						in.close();
				
						BrokerPacket packetToServer = new BrokerPacket();

						packetToServer.type = BrokerPacket.LOOKUP_REQUEST;
						packetToServer.symbol=st.nextToken();
						
						//System.out.println("about to write to ns"); //debug
						outNS.writeObject(packetToServer);
						//System.out.println("recieved from ns"); //debug
						/* print server reply */
						BrokerPacket packetFromServer = new BrokerPacket();
						packetFromServer = (BrokerPacket) inNS.readObject();
			
						if(packetFromServer.error_code==BrokerPacket.BROKER_NULL)
						{
							hostname = packetFromServer.locations[0].broker_host;
							port = packetFromServer.locations[0].broker_port;

							try {
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
							connected=true;
						}
	
						else if(packetFromServer.error_code==BrokerPacket.ERROR_SERVER_NOT_REGISTERED){
							System.out.println("Server doesn't exist.");
						}
					}


					else{
						System.out.println("Unknown command.");
					}
				}

				else if(st.countTokens() <2 && connected == true){
					/* make a new request packet */
					BrokerPacket packetToServer = new BrokerPacket();
					packetToServer.type = BrokerPacket.BROKER_REQUEST;
					packetToServer.symbol = userInput;
					//System.out.println("about to send to server");//debug
					out.writeObject(packetToServer);
					
					/* print server reply */
					BrokerPacket packetFromServer;
					packetFromServer = (BrokerPacket) in.readObject();
					//System.out.println("received a packet from server");//debug
					if (packetFromServer.type == BrokerPacket.BROKER_QUOTE){
						System.out.println("Quote from broker: " + packetFromServer.quote);
					}
				}

				System.out.print("> ");
			}
			/* tell server that i'm quitting */
			BrokerPacket packetToNS = new BrokerPacket();
			packetToNS.type = BrokerPacket.BROKER_BYE;

			//used to help the server identify type of client terminating to decide to flush to the database or not
			packetToNS.symbol = new String("CLIENT");
			outNS.writeObject(packetToNS);
			
			//System.out.println("im closing outns and inns"); //debug
			outNS.close();
			inNS.close();

			/* tell server that i'm quitting */
			BrokerPacket packetToServer = new BrokerPacket();
			packetToServer.type = BrokerPacket.BROKER_BYE;
			NSSocket.close();

			//used to help the server identify type of client terminating to decide to flush to the database or not
			packetToServer.symbol = new String("CLIENT");
			out.writeObject(packetToServer);

			out.close();
			in.close();
			stdIn.close();
			brokerSocket.close();
	}
}

