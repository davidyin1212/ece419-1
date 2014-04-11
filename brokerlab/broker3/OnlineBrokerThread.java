import java.net.*;
import java.io.*;
import java.util.*;

public class OnlineBrokerThread extends Thread {
	private Socket socket = null;
	private HashMap<String, Long> quotes = null;
	private String file = null;
	private String NShost = null;
	private int NSport = 0;

	public OnlineBrokerThread(Socket socket, HashMap<String, Long> quotes, String file,  String NShost, int NSport) {
		super("OnlineBrokerThread");
		this.socket = socket;
		this.quotes = quotes;
		this.file = file;
		this.NShost = NShost;
		this.NSport = NSport;
		System.out.println("Created new Thread to handle client");
	}

	public void run() {
		boolean gotByePacket = false;
		
		try {
			/* stream to read from client */
			ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
			ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());

			BrokerPacket packetFromClient;

			/*Socket namingSocket = new Socket(NShost, NSport);

			ObjectOutputStream outNS = new ObjectOutputStream(namingSocket.getOutputStream());
			ObjectInputStream   inNS = new ObjectInputStream(namingSocket.getInputStream());*/
			
			//writer to the disk
			System.out.println("initializing writer to null: ");
			//PrintWriter writer = new PrintWriter(file);
			PrintWriter writer = null;	
	
			while (( packetFromClient = (BrokerPacket) fromClient.readObject()) != null) {
				/* create a packet to send reply back to client */		
				BrokerPacket packetToClient = new BrokerPacket();

				/* process client request */
				System.out.println("request type: "+packetFromClient.type);
				if(packetFromClient.type == BrokerPacket.BROKER_REQUEST) {
					System.out.println("Request from Client: " + packetFromClient.symbol);
					Long quote = (Long)quotes.get(packetFromClient.symbol.toLowerCase());
					if(quote != null){

						packetToClient.quote = (long) quote;
					}
					else{
						//cannot find the quote, ask other server
						BrokerPacket packetToNS = new BrokerPacket();
						BrokerPacket packetFromNS;
						packetToNS.type = BrokerPacket.LOOKUP_REQUEST;
						if(file.equals("nasdaq")){
							packetToNS.symbol = "tse";}
						else{
							packetToNS.symbol = "nasdaq";}
						Socket namingSocket = new Socket(NShost, NSport);

						ObjectOutputStream outNS = new ObjectOutputStream(namingSocket.getOutputStream());
						ObjectInputStream   inNS = new ObjectInputStream(namingSocket.getInputStream());
						outNS.writeObject(packetToNS);
						packetFromNS = (BrokerPacket) inNS.readObject();

						if(packetFromNS.error_code==BrokerPacket.BROKER_NULL){
							int OSport = packetFromNS.locations[0].broker_port;
							String OShost = packetFromNS.locations[0].broker_host;
							//System.out.println("about to enter query function"); //debug
							packetToClient.quote = query(OShost, OSport, packetFromClient.symbol);
							//System.out.println("exited query function"); //debug
						}
						
						else {
							//cannot find the quote, just send 0
							packetToClient.quote = (long)0;
						}

						packetToNS = new BrokerPacket();
						packetToNS.type = BrokerPacket.BROKER_BYE;
						packetToNS.symbol = "CLIENT";
						outNS.writeObject(packetToNS);
						outNS.close();
						inNS.close();
					}
					packetToClient.type = BrokerPacket.BROKER_QUOTE;
					/* send reply back to client */
					toClient.writeObject(packetToClient);
					/* wait for next packet */
					continue;
				}

				else if(packetFromClient.type == BrokerPacket.BROKER_FORWARD){
					System.out.println("Request from Client: " + packetFromClient.symbol);
					Long quote = (Long)quotes.get(packetFromClient.symbol.toLowerCase());
					if(quote != null){

						packetToClient.quote = (long) quote;
					}
					else{
						//cannot find the quote, just send 0
						packetToClient.quote = (long)0;
					}
					packetToClient.type = BrokerPacket.BROKER_QUOTE;
					/* send reply back to client */
					toClient.writeObject(packetToClient);
					/* wait for next packet */
					continue;
				}
				else if(packetFromClient.type!=BrokerPacket.BROKER_BYE){
					if(writer == null){
						System.out.println("creating writer");
						writer = new PrintWriter(file);
					}
					//called by exchange client
					System.out.println("serviced by a broker, preparing to modify file");
					//writer = new PrintWriter(file);
					packetToClient.type = BrokerPacket.EXCHANGE_REPLY;
					if(packetFromClient.type == BrokerPacket.EXCHANGE_ADD){
						System.out.println("Request from Client: add " + packetFromClient.symbol);
						String company = packetFromClient.symbol.toLowerCase();
						if(quotes.containsKey(company)){
							//set the error field, return
							packetToClient.error_code = BrokerPacket.ERROR_SYMBOL_EXISTS;
						}
						else{
							//add to the hashmap
							quotes.put(company,Long.valueOf((long) 0));
							packetToClient.error_code = 0;					
						}
						toClient.writeObject(packetToClient);
						continue;
					}
					else if(packetFromClient.type == BrokerPacket.EXCHANGE_UPDATE){
						System.out.println("Request from Client: update " + packetFromClient.symbol);
						String company = packetFromClient.symbol.toLowerCase();
						if(quotes.containsKey(company)){
							//check the bounds, then update
							//you must convert the 300 to an object and compare with quote which is a long object
							if(packetFromClient.quote.compareTo(Long.valueOf((long)300)) > 0 || packetFromClient.quote.compareTo(Long.valueOf((long) 1)) < 0){
								//set the error
								packetToClient.error_code = BrokerPacket.ERROR_OUT_OF_RANGE;
							}
							else{
								//update
								quotes.put(company, Long.valueOf(packetFromClient.quote));
								packetToClient.error_code = 0;
								
							}
						}
						else{
							//no key found
							packetToClient.error_code = BrokerPacket.ERROR_INVALID_SYMBOL;
							
						}
						toClient.writeObject(packetToClient);
						continue;
					}
					else if(packetFromClient.type == BrokerPacket.EXCHANGE_REMOVE){
						System.out.println("Request from Client: remove " + packetFromClient.symbol);
						String company = packetFromClient.symbol.toLowerCase();
						if(quotes.containsKey(company)){
							//delete the keyvalue pair
							quotes.remove(company);
							packetToClient.error_code = 0;
						}
						else{
							packetToClient.error_code = BrokerPacket.ERROR_INVALID_SYMBOL;
							
						}
						toClient.writeObject(packetToClient);
						continue;
					}
				}
				/* Sending an ECHO_NULL || ECHO_BYE means quit */
				if (packetFromClient.type == BrokerPacket.BROKER_NULL || packetFromClient.type == BrokerPacket.BROKER_BYE) {
					gotByePacket = true;
					System.out.println("terminating client");
					if(packetFromClient.symbol.equals("EXCHANGE") && writer != null){
					//flush the contents of the hashmap to the disk
						System.out.println("flushing to disk");
						Set set = quotes.entrySet();
						Iterator i = set.iterator();
						while(i.hasNext()){
							Map.Entry current = (Map.Entry)i.next();
							writer.print(current.getKey()+" "+current.getValue()+"\n");
						}
						writer.flush();
					}
					break;
				}
				/* if code comes here, there is an error in the packet */
				System.err.println("ERROR: Unknown BROKER_* packet!!");
				System.exit(-1);
			}
			
			/* cleanup when client exits */
			fromClient.close();
			toClient.close();
			socket.close();

			//close the filehandle
			//writer.close();
			if(writer != null){
				//System.out.println("closeing writer");//debug
				writer.close();
			}

		} catch (IOException e) {
			if(!gotByePacket){
				e.printStackTrace();
			}
		} catch (ClassNotFoundException e) {
			if(!gotByePacket)
				e.printStackTrace();
		}
	}

	public Long query(String OShost, int OSport, String company){
		ObjectOutputStream outOS = null;
	     ObjectInputStream inOS = null;
		Socket OSSocket = null;
		BrokerPacket packetFromOS = null;
		BrokerPacket packetToOS = null;
		System.out.println("connecting to: "+OShost+" on port "+OSport);
		try{
			//connecting to other server
			try{
				//System.out.println("create other server socket");//debug
				OSSocket = new Socket(OShost,OSport);
		
				outOS = new ObjectOutputStream(OSSocket.getOutputStream());
				inOS  = new ObjectInputStream(OSSocket.getInputStream());
			} catch (UnknownHostException e) {
					System.err.println("ERROR: Invalid Lookup server!!!");
					System.exit(1);
			} catch (IOException e) {
					System.err.println("ERROR: Couldn't get I/O for the connection.");
					System.exit(1);
			}
		
			/* make a new request packet */
			packetToOS = new BrokerPacket();
			packetToOS.type = BrokerPacket.BROKER_FORWARD;
			packetToOS.symbol = company;
			outOS.writeObject(packetToOS);
	
			/* print server reply */
			packetFromOS = (BrokerPacket) inOS.readObject();

			if (packetFromOS.type == BrokerPacket.BROKER_QUOTE){
							System.out.println("Quote from other broker: " + packetFromOS.quote);
			}

			packetToOS = new BrokerPacket();
			packetToOS.type = BrokerPacket.BROKER_BYE;
			packetToOS.symbol = "CLIENT";

			//System.out.println("closing other server socket");//debug
			outOS.writeObject(packetToOS);
			outOS.close();
			inOS.close();

		} catch(IOException e){
			System.err.println("ERROR: Couldn't get I/O for the connection.");
			System.exit(1);
		} catch(ClassNotFoundException e){
			//terminate gracefully
			packetToOS = new BrokerPacket();
			packetToOS.type = BrokerPacket.BROKER_BYE;

			try{
				outOS.writeObject(packetToOS);
				
				//System.out.println("closing other server socket");//debug
				inOS.close();
				outOS.close();
				OSSocket.close();
			} catch (IOException g){
				System.err.println("ERROR: Couldn't get I/O for the connection.");
				System.exit(1);
			}
			System.err.println("ERROR: Class not found?");
			System.exit(1);
		}
		
		return packetFromOS.quote;

	}
}
