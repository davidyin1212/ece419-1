import java.net.*;
import java.io.*;
import java.util.*;


public class OnlineBroker {

private static final String QUOTES = "nasdaq";

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        boolean listening = true;
	
	HashMap<String, Long> quotes = new HashMap<String, Long>();
	File file = new File(QUOTES);
	BufferedReader reader = null;
	String line;
	
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

        while (listening) {
		//cache the contents of the nasdaq quotes upon startup
        	new OnlineBrokerThread(serverSocket.accept(), quotes).start();
        }

        serverSocket.close();
    }
}
