import java.net.*;
import java.io.*;
import java.util.*;

//initial lookupserver, spawns lookup threads

public class BrokerLookupServer {

private static final String QUOTES = "list";

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        boolean listening = true;
	
	HashMap<String, BrokerLocation> server_list = new HashMap<String, BrokerLocation>();
	
        try {
        	if(args.length == 1) {
        		serverSocket = new ServerSocket(Integer.parseInt(args[0]));
        	} else {
        		System.err.println("ERROR: Invalid arguments!");
        		System.exit(-1);
        	}
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        } catch(NumberFormatException e){
     		System.err.println("ERROR: port name is not a number!");
     		System.exit(-1);
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

        while (listening) {
	        //cache the contents of the nasdaq quotes upon startup
        	   new LookupServerThread(serverSocket.accept(), server_list).start();
        }

        serverSocket.close();
    }
}
