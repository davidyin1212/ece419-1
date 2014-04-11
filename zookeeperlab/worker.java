import org.apache.zookeeper.KeeperException;

import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class worker {
    
    static ZkConnector zkc;
    static ZooKeeper zk;
    public static void main(String[] args) throws IOException, KeeperException, InterruptedException, 
    NumberFormatException, ClassNotFoundException {
        
        if (args.length != 1) {
            System.out.println("Usage: java -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar:. fileServer zkServer:clientPort");
            return;
        }
        
		String line = null, answer=null, hash = null, start = null, task=null;
		List<String> jobs = null, tasks = null;
	
        ZkConnector zkc = new ZkConnector();
        try {
            zkc.connect(args[0]);
        } catch(Exception e) {
            System.out.println("Zookeeper connect "+ e.getMessage());
        }

        zk = zkc.getZooKeeper();
        
        while(true){
	        //get list of jobs
        	
        	if(zk.exists("/jobs", false)!=null)
        		jobs= zk.getChildren("/jobs", false);
        	
	        if(jobs!=null){
	        	for(int j=0; j<jobs.size();j++){
	        		tasks=null;
	        		
	        		////////////////////////////////////////////////////////////
	        		///////////////REMEBER THIS LINE MUST BE REMOVED////////////
	        		////////////////////////////////////////////////////////////
	        		if(zk.getData("/jobs/"+jobs.get(j), false, null)!=null)
	        		{
	        			//locked
	        			continue;
	        		}
	        		tasks=zk.getChildren("/jobs/"+jobs.get(j)+"/tasks", false);
		        	
	        		//start working on a task
		        	if(tasks != null && tasks.size()!=0)
		        	{
		        		//go through tasks until an unlocked one is found
		        		for(int i=0; i<tasks.size(); i++)
		        		{
		        			//task exists
		        			//if(zk.exists("/jobs/"+jobs.get(j)+"/tasks/"+tasks.get(i), false)!=null)
		        			//{
			        			//not locked!

				        			//if(zk.exists("/jobs/"+jobs.get(j)+"/tasks/"+tasks.get(i)+"/LOCKED", false)==null)
				        			//{
				        				//lock node
				        	        	try{
			        						zk.create("/jobs/"+jobs.get(j)+"/tasks/"+tasks.get(i)+"/LOCKED", // Path of znode
					        	                    null,           // Data not needed.
					        	                    Ids.OPEN_ACL_UNSAFE,    // ACL, set to Completely Open.
					        	                    CreateMode.EPHEMERAL   // Znode type, set to Persistent.
					        	                    );
					        	        			task = "/jobs/"+jobs.get(j)+"/tasks/"+tasks.get(i);
					        	        			hash = jobs.get(j);
					        	        			start= tasks.get(i);
					        	        			break;
				        	        	}
				        	        	catch (KeeperException e){
				        	        		System.err.println("woops lol... "+e.getMessage());
				        	        	}

				        			//}

		        			//}
		        		}
		        		
		        		//if no tasks were free or existed
		        		if(start==null)
		        			continue;
		    			//get data
		        		ArrayList<String> data = getData(start);
		        		
		        		if(data!=null){
	        	        	//work on partition
			        		answer = workPartition(data, hash);
		        		}
		        		
		        		else{System.out.println("fileServer resturned an error " +
		        								"OR fileServer not active");}
		        		
		        		if(answer!=null)
		        		{
		        			System.out.println("Found the password!!");
		        			
		        			//add solution
		                    zk.setData("/jobs/"+hash+"/solution", answer.getBytes(), -1);
		                    
		                    //remove lock
		                    try{
			                    zk.delete(task+"/LOCKED", -1);
			        			//delete task
			                    zk.delete(task,-1);
		                    }
		                    catch(KeeperException e){
		                    		System.err.println("lol no folder lol");	                    	
		                    }
		        			//delete all other tasks
		                    deleteOtherTasks(hash);
		                    System.out.println("Deleted other tasks");
		                    start=null;task = null;hash = null;
		                    
		        		}
		        		
		        		else{
		        			System.out.println("Didn't find password in partition: "+start);
		        			//remove lock
		        			try{
			                    zk.delete(task+"/LOCKED", -1);
			        			//delete task
			                    zk.delete(task,-1);
		        			}
		                    catch(KeeperException e){
		                    		System.err.println("lol no folder lol");	                    	
		                    }
		                    start=null;task = null;hash = null;
		        		}
		        	}
		        	
		        	else if(new String(zk.getData("/jobs/"+jobs.get(j)+"/solution", false, null)).equalsIgnoreCase("in_progress"))
		        	{
		        		//solution not found
		        		System.out.println("Solution not found!");
		        		zk.setData("/jobs/"+jobs.get(j)+"/solution", "null".getBytes(), -1);
		        	}
	        	} 		
	        }
	    }
    }
    
    static void deleteOtherTasks(String hash) throws KeeperException, InterruptedException
    {
    	List<String> tasks=zk.getChildren("/jobs/"+hash+"/tasks", false);
    	
    	for(int i=0; i<tasks.size(); i++)
    	{
    		//task isn't locked, otherwise skip
    		//if(zk.getData("/jobs/"+hash+"/tasks/"+tasks.get(i), false, null)==null)
			//{
    			try{
    				zk.delete("/jobs/"+hash+"/tasks/"+tasks.get(i),-1);
    			}
    			catch(KeeperException e){
    				System.err.println("lol not there lol");
    			}
			//}
    	}
    }
    
    static String workPartition(ArrayList<String> data, String hash)
    {
    	for(int i=0; i<data.size();i++){
    		String check = getHash(data.get(i));
    		if(hash.equals(check))
    			return data.get(i);
    	}
    	return null;
    }
    
    public static String getHash(String word) {

        String hash = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            BigInteger hashint = new BigInteger(1, md5.digest(word.getBytes()));
            hash = hashint.toString(16);
            while (hash.length() < 32) hash = "0" + hash;
        } catch (NoSuchAlgorithmException nsae) {
            // ignore
        }
        return hash;
    }
    
    static ArrayList<String> getData(String start) throws KeeperException, InterruptedException, 
    NumberFormatException, UnknownHostException, IOException, ClassNotFoundException{
    	//ask zookeeper for fileserver IP
    	ObjectOutputStream toClient=null;
    	ObjectInputStream fromClient = null;
    	Packet packetFromClient=new Packet(), packetToClient=new Packet();
		boolean successful=false;
		Socket socket = null;
    	
		while(!successful){
	    	String info =null;
	    	if(zk.exists("/fileServer", false)!=null)
	    		info = new String(zk.getData("/fileServer", false, null));
	    	else
	    		return null;
			
			//IP[0]=hostname, IP[1]=port
			String[] IP= info.split(":");
			
			try{
				socket = new Socket(IP[0], Integer.parseInt(IP[1]));
		    	//connect to fileserver
				toClient = new ObjectOutputStream(socket.getOutputStream());
				toClient.flush();
				fromClient = new ObjectInputStream(socket.getInputStream());
		    	
				//send LOOKUP_REQUEST packet
				packetToClient.type=Packet.LOOKUP_REQUEST;
				packetToClient.length=977;
				packetToClient.start=(long)Integer.parseInt(start);
				toClient.writeObject(packetToClient);
				
				packetFromClient=(Packet)fromClient.readObject();
				successful = true;
			}
			catch(Exception e){
	    		System.err.println("fileServer crashed!");
	    		Thread.sleep(5000);
			}
		}
		fromClient.close();
		toClient.close();
		socket.close();
		
		if(packetFromClient.type==Packet.LOOKUP_REPLY)
			return packetFromClient.data;
		
		//error
		else
			return null;
		
    }
    
}