import org.apache.zookeeper.KeeperException;

import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.BufferedReader;
import java.net.*;
import java.io.*;
import java.util.*;
public class fileServer {
    
    static String myPath = "/fileServer";
    static String dict = "lowercase.rand";
    static String port;
    static Watcher watcher;
    static ZkConnector zkc;
    static ZooKeeper zk;
    public static void main(String[] args) throws IOException {
        
        if (args.length != 2) {
            System.out.println("Usage: java -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar:. fileServer zkServer:clientPort listenPort");
            return;
        }
        port=args[1];
        
        ServerSocket serverSocket = null;
        boolean listening = true;
		String line = null;

        ZkConnector zkc = new ZkConnector();
        try {
            zkc.connect(args[0]);
        } catch(Exception e) {
            System.out.println("Zookeeper connect "+ e.getMessage());
        }

        zk = zkc.getZooKeeper();

        watcher = new Watcher() { // Anonymous Watcher
            @Override
            public void process(WatchedEvent event) {
                try {
					handleEvent(event);
				} catch (KeeperException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        
            } 
        };
        
        try {
            System.out.println("Sleeping...");
            Thread.sleep(2000);
	           
            Stat s = zk.exists(myPath, watcher);
	        if (s == null) {
	        	System.out.println("Creating " + myPath);
	        	String primary_path = InetAddress.getLocalHost().getHostAddress()+":"+args[1];
	        	zk.create(
	        			myPath,         // Path of znode
	                    primary_path.getBytes(),           // Data not needed.
	                    Ids.OPEN_ACL_UNSAFE,    // ACL, set to Completely Open.
	                    CreateMode.EPHEMERAL   // Znode type, set to Persistent.
	                    );
	        }
	        else{
	        	System.out.println(s.toString());
	        }           

        } catch(KeeperException e) {
            System.out.println(e.code());
        } catch(Exception e) {
            System.out.println("Make node:" + e.getMessage());
        }
        
        
        //trying to connect to specified port
        try {
        	serverSocket = new ServerSocket(Integer.parseInt(args[1]));
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }

		//server has started
		System.out.println("Listening on port: "+serverSocket.getLocalPort());
		ArrayList<String> dictionary = new ArrayList<String>();

		//moving dictionary to physical memory
		BufferedReader reader = new BufferedReader(new FileReader(dict));
		while ((line = reader.readLine()) != null) {
			dictionary.add(line);
		}
		
        while (listening) {
	        //cache the contents of the nasdaq quotes upon startup
        	   new fileServerThread(serverSocket.accept(), dictionary).start();
        }

        serverSocket.close();

    }
    
    private static void checkpath() throws KeeperException, InterruptedException, UnknownHostException {
        Stat stat = zk.exists(myPath, watcher);
        if (stat == null) {              // znode doesn't exist; let's try creating it
            System.out.println("Creating " + myPath);
            String primary_path = InetAddress.getLocalHost().getHostAddress()+":"+port;
            String ret = zk.create(
                        myPath,         // Path of znode
                        primary_path.getBytes(),  
                        Ids.OPEN_ACL_UNSAFE,// Data not needed.
                        CreateMode.EPHEMERAL   // Znode type, set to EPHEMERAL.
                        );
            
            System.out.println("Now designated Primary fileServer!");
        } 
    }
    
    private static void handleEvent(WatchedEvent event) throws KeeperException, InterruptedException, UnknownHostException {
        String path = event.getPath();
        System.out.println(path);
        EventType type = event.getType();
        if(path.equalsIgnoreCase(myPath)) {
            if (type == EventType.NodeDeleted) {
                System.out.println(myPath + " deleted! Let's go!");       
                checkpath(); // try to become the boss
            }
            if (type == EventType.NodeCreated) {
                System.out.println(myPath + " created!");       
                try{ Thread.sleep(5000); } catch (Exception e) {}
                checkpath(); // re-enable the watch
            }
        }
    }
}
