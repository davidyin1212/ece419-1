import java.net.*;
import java.io.*;
import java.util.*;

public class OnlineBrokerThread extends Thread {
	private Socket socket = null;
	private HashMap<String, Long> quotes = null;

	public OnlineBrokerThread(Socket socket, HashMap<String, Long> quotes) {
		super("OnlineBrokerThread");
		this.socket = socket;
		this.quotes = quotes;
		System.out.println("Created new Thread to handle client");
	}

	public void run() {
		boolean gotByePacket = false;
		
		try {
			/* stream to read from client */
			ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
			BrokerPacket packetFromClient;
			
			/* stream to write back to client */
			ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());
			
			//writer to the disk
			String file = "nasdaq";
			//initialize the writer
			PrintWriter writer = null;
						
			while (( packetFromClient = (BrokerPacket) fromClient.readObject()) != null) {
				/* create a packet to send reply back to client */		
				BrokerPacket packetToClient = new BrokerPacket();

				/* process client request */
				if(packetFromClient.type == BrokerPacket.BROKER_REQUEST) {
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
				else{
					//called by exchange client
					if(writer == null && packetFromClient.type != BrokerPacket.BROKER_BYE){
						System.out.println("need new writer");
						writer = new PrintWriter(file);
					}
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
			if(writer != null){
				System.out.println("closing file");
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
}
