import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.data.ACL;


import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Tracker{

private static zkWatcher zkwatcher;
private static ZkConnector zkc;
private static ZooKeeper zk;
private static String TRACKER_PATH = "/trackers";
private static String JOB_PATH = "/jobs";
private String primary_path;

protected static final List<ACL> acl = Ids.OPEN_ACL_UNSAFE;

	public Tracker(/*String trackerID,*/ String zkHost, int trackerPort) {
		assert(zkHost != null);
		//assert(trackerID != null);
		try{
			//connect to zookeeper
			zkc = new ZkConnector();
			zkc.connect(zkHost);
			zk = zkc.getZooKeeper();
			zkwatcher = new zkWatcher();
			primary_path = InetAddress.getLocalHost().getHostAddress()+":"+Integer.toString(trackerPort);
			
			//try to establish yourself as the primary/backup server
			Stat stat = zk.exists(TRACKER_PATH, false);
				if(stat == null){
					System.out.println("I am the first tracker, i will try to become primary");
					zk.create(TRACKER_PATH,this.primary_path.getBytes(),acl, CreateMode.EPHEMERAL);
				}
				else{
					System.out.println("I am the secondary tracker, I will keep a watch on the primary");
					//set the watch on the primary
					zk.exists(TRACKER_PATH, zkwatcher);
				}
			
			
			//try to create the /jobs folder
			stat = zk.exists(JOB_PATH, false);
			if(stat == null){
				System.out.println("Creating the jobs folder");
				zk.create(JOB_PATH, null, acl, CreateMode.PERSISTENT);
			}
			
		}
		catch (Exception e){
			System.err.println("Zookeeper connect exception: "+e.getMessage());
		}
	}
	
	class zkWatcher implements Watcher {
		@Override
		public void process(WatchedEvent event){
			Event.EventType etype = event.getType();
			String epath = event.getPath();
			System.out.println("event type: "+etype+" path: "+epath);
			try{
				if(epath.equalsIgnoreCase(TRACKER_PATH)){
					//handle primary tracker failure
					//zk.create(TRACKER_PATH,primary_path.getBytes(),acl,CreateMode.EPHEMERAL);
					//clean up the job folders
					List<String> jobs = zk.getChildren(JOB_PATH, false);
					for (String job : jobs){
						String task_path = JOB_PATH+"/"+job;
						//if the folder is locked, then must have died during creation of folder, recreate contents of folder
						if(zk.getData(task_path,false,null) != null){
							//redo all the shit in the folder
							System.out.println("attempting to recreate tasks for job: "+job);
								if(zk.exists(task_path.concat("/tasks"), null) == null){
									zk.create(task_path.concat("/tasks"),null, acl, CreateMode.PERSISTENT);
								}
								if(zk.exists(task_path.concat("/solution"), null) == null){
									zk.create(task_path.concat("/solution"), "in_progress".getBytes(),acl,CreateMode.PERSISTENT);
								}
							System.out.println("recreating all the tasks");
							String internal_task_path = task_path+"/tasks";
							
							for(int i=0;i<272;i++){
								String path = internal_task_path + "/" + Integer.toString(i*977);
								System.out.println("recreating folder: "+path);
								if(zk.exists(path, null) == null){
									zk.create(path, null,acl, CreateMode.PERSISTENT);
								}
							}
							//System.out.println("unlocking "+task_path);
							zk.setData(task_path, null, -1);
							/*if(zk.getData(task_path,false,null) != null){
								System.out.println("data is still not set to null");
							}*/
							System.out.println("job "+job+" has been repaired");
						}
						
					}
					System.out.println("Taking over for the primary...");
					zk.create(TRACKER_PATH,primary_path.getBytes(),acl, CreateMode.EPHEMERAL);
				}
				else{
					System.out.println("im not supposed to handle this.");
				}
			}
			catch (Exception e){
				System.err.println("Zookeeper handling exception: "+e.getMessage());
			}
			
		}
	
	}
	
	public static class TrackerThread extends Thread {
		private Socket socket = null;
		public TrackerThread (Socket socket){
			this.socket = socket;
		}
		public void run() {
			try{
				ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
				ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());
				InstructionPacket packetFromClient;
				InstructionPacket packetToClient;
				System.out.println("recieved new connection");
				while (( packetFromClient = (InstructionPacket) fromClient.readObject()) != null){
					System.out.println("recieved hash: "+packetFromClient.data);
					if(!packetFromClient.data.equals("")){
						//System.out.println("shit?");
						String HASH_PATH = JOB_PATH+"/"+packetFromClient.data;
						Stat stat = zk.exists(HASH_PATH, false);
						if(packetFromClient.request_type  == InstructionPacket.JOB_REQUEST){
							//attempt to create the job folder
							if(stat != null){
								//job folder already exists, return the answer or job_in progress
								System.out.println("job folder already exists");
								String status = new String(zk.getData(HASH_PATH.concat("/solution"),false,null));
								int state = InstructionPacket.JOB_COMPLETED;
								if(status.equalsIgnoreCase("in_progress")){
									state = InstructionPacket.JOB_IN_PROGRESS;
									System.out.println("IN PROGRESS");
								}
								packetToClient = new InstructionPacket(state,status);
							}
							//create the job folder
							else{
								packetToClient = new InstructionPacket(InstructionPacket.JOB_IN_PROGRESS, "");
								String task_path = JOB_PATH+"/"+packetFromClient.data;
								System.out.println("creating folder: "+task_path);
								zk.create(task_path,"locked".getBytes() ,acl, CreateMode.PERSISTENT);
								System.out.println("creating folder: "+task_path.concat("/tasks"));
								zk.create(task_path.concat("/tasks"),null, acl, CreateMode.PERSISTENT);
								System.out.println("creating folder: "+task_path.concat("/solution"));
								zk.create(task_path.concat("/solution"), "in_progress".getBytes(),acl,CreateMode.PERSISTENT);
								System.out.println("creating all the tasks");
								String internal_task_path = task_path+"/tasks";
							
								for(int i=0;i<272;i++){
									String path = internal_task_path + "/" + Integer.toString(i*977);
									System.out.println("creating folder: "+path);
									zk.create(path, null,acl, CreateMode.PERSISTENT);
								}
								//System.out.println("unlocking folder: "+task_path);
								zk.setData(task_path, null, -1);
								/*if(zk.getData(task_path, false, null) != null){
									System.out.println(task_path+" has not been unlocked");
								}*/
							
							}
							//send reply back to client
							toClient.writeObject(packetToClient);
						}
						else if(packetFromClient.request_type == InstructionPacket.JOB_STATUS){
							if(stat != null){
								String status = new String(zk.getData(HASH_PATH.concat("/solution"),false,null));
								int state = InstructionPacket.JOB_COMPLETED;
								if(status.equalsIgnoreCase("in_progress")){
									state = InstructionPacket.JOB_IN_PROGRESS;
								}
								packetToClient = new InstructionPacket(state, status);
							}
							else{
								packetToClient = new InstructionPacket(InstructionPacket.JOB_NOT_FOUND, "");
							}
							toClient.writeObject(packetToClient);
						}
					}
					else 
					{
						if(packetFromClient.request_type == InstructionPacket.JOB_QUIT){
							//disconnect from the client
							break;
						}
						else{
							System.out.println("I dont know how to handle this packet");
						}
					}
				}
				System.out.println("disconnecting from client");
				fromClient.close();
				toClient.close();
				this.socket.close();
			}
			catch(Exception e){
				System.err.println("found some exception: "+e.getMessage());
			}
		}
	}
	
	public static void main (String[] args){
	
	ServerSocket serverSocket = null;
	boolean listening = true;
	
		if(args.length != 3){
			System.out.println("Usage: Tracker [zooHostname] [zooPortnum] [trackerport]");
			System.exit(1);
		}
		else{
			String trackeraddr = args[0]+":"+args[1];
			try{
				int trackerport = Integer.parseInt(args[2]);
				serverSocket = new ServerSocket(trackerport);
				Tracker tracker = new Tracker (trackeraddr, trackerport);
				while(listening){
					System.out.println("tracker has started");
					new TrackerThread(serverSocket.accept()).start();
				}
				 
			}
			catch(NumberFormatException e){
				System.err.println("Usage: tracker [zooHostname] [zooPortnum] [trackerhost] [trackerport]");
				System.exit(1);
			}
			catch (IOException e) {
			    System.err.println("ERROR: Could not listen on port!");
			    System.exit(1);
			}
		}
	} 
	
	
	
}
