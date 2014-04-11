import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.Watcher.Event.EventType;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;


public class Client {

	private String hosts = null;
	private ZkConnector zkc = null;
	private String TrackerPath = "/trackers";
	private ZooKeeper zk = null;
	private Watcher watcher = null; 

	private Socket to_tracker = null;
	private ObjectInputStream instream = null;
	private ObjectOutputStream outstream = null;
	private boolean locked = true;

	//private CountDownLatch latch = null;

	public Client(String hosts) {
		assert(hosts != null);
		try{
			zkc = new ZkConnector();
			zkc.connect(hosts);
		}
		catch(Exception e){
			System.err.println("Zookeeper connect exception: "+e.getMessage());
		}
		
		zk = zkc.getZooKeeper();
		watcher = new Watcher() {
			@Override
			public void process(WatchedEvent event){
				handleEvent(event);
			}
		};
		//getting the status of the tracker
		Stat stat = null;
		//init the latch
		//latch = new CountDownLatch(1);
		
		try{
			stat = zk.exists(TrackerPath, watcher);
		}
		catch (Exception e){
			System.err.println("trouble accessing zookeeper: "+e.getMessage());
			System.exit(1);
		}
		if (stat == null){
			System.out.println("tracker not online yet, please retry later");
			System.exit(1);
		}
		else{
			//retrieve info about the tracker and establish connection
			//tracker addresss in the form of hostname:port			
			try{
				String tracker_addr = new String(zk.getData(TrackerPath, false, null));
				String[] info = tracker_addr.split(":");
				if(info.length != 2){
					System.err.println("tracker address is of illegal format");
					System.exit(1);
				}
				to_tracker = new Socket(info[0], Integer.parseInt(info[1]));
				//to_tracker.setSoTimeout(20000);
				outstream = new ObjectOutputStream(to_tracker.getOutputStream());
				outstream.flush();
				instream = new ObjectInputStream(to_tracker.getInputStream());	
				
			}
			catch(UnknownHostException e){
				System.err.println("ERROR: Don't know where to connect");
				System.exit(1);
			}
			catch(IOException e){
				System.err.println("ERROR: Couldnt get IO for connection.");
				System.exit(1);
			}
			catch(NumberFormatException e){
				System.err.println("ERROR: Port number cant be converted to integer.");
				System.exit(1);
			}
			catch (Exception e){
				System.err.println("trouble accessing zookeeper: "+e.getMessage()); 
			}
		}
	}

	private void handleEvent(WatchedEvent event){
		System.out.println("handleevent activated: "+event.toString());
		Stat stat = zkc.exists(TrackerPath, this.watcher);
		EventType type = event.getType();
		String epath = event.getPath();
		if(epath.equalsIgnoreCase(TrackerPath)){
			try{
				if(type == EventType.NodeCreated){
					//reconnect to the primary, the failover must have happened already
					String tracker_data  = new String(zk.getData(TrackerPath, false, null));
						String[] info = tracker_data.split(":");
						if(info.length != 2){
							System.err.println("tracker address is of illegal format");
							System.exit(1);
						}
						this.to_tracker = new Socket(info[0], Integer.parseInt(info[1]));
						//this.to_tracker.setSoTimeout(20000);
						this.outstream = new ObjectOutputStream(to_tracker.getOutputStream());	
						this.instream = new ObjectInputStream(to_tracker.getInputStream());
						
						//set the watch again
						//System.out.println("setting the watch after create");
						//System.out.println("counting down latch");
						//latch.countDown();//let the handlejob proceed;

						//release the lock of the connector
						locked = false;

						//System.out.println("hella");
				}
				if(type == EventType.NodeDeleted){
					//close the sockets to the old tracker
					this.to_tracker.close();
					this.outstream.close();
					this.instream.close();
					//set the watch
					//System.out.println("setting the watch after delete");
					//Stat stat = zkc.exists(TrackerPath, this.watcher);
				}
			}
			catch(UnknownHostException e){
				System.err.println("ERROR: Don't know where to connect");
				System.exit(1);
			}
			catch(IOException e){
				System.err.println("ERROR: Couldnt get IO for connection.");
				System.exit(1);
			}
			catch(NumberFormatException e){
				System.err.println("ERROR: Port number cant be converted to integer.");
				System.exit(1);
			}
			catch (Exception e){
				System.err.println("zookeeper exists: "+ e.getMessage());
				System.exit(1);
			}
		}
		else{
			System.out.println("got an event thats not for me");
		}
	}

	public void handle_job(int request, String hash){
		//send to the tracker
		assert(request!=InstructionPacket.JOB_DEFAULT);
		assert(hash!=null);
		try{		
			InstructionPacket packet_to_tracker = new InstructionPacket(request, hash);
			this.outstream.writeObject(packet_to_tracker);

			//wait for packet from tracker, may trigger exception when tracker crashes
			InstructionPacket packet_from_tracker = (InstructionPacket) this.instream.readObject();
			if(packet_from_tracker.request_type == InstructionPacket.JOB_IN_PROGRESS){
				System.out.println("current job is in progress");
			}
			else if (packet_from_tracker.request_type == InstructionPacket.JOB_NOT_FOUND){
				System.out.println("unable to find job");
			}
			else if (packet_from_tracker.request_type == InstructionPacket.JOB_COMPLETED){
				if(packet_from_tracker.data.equals("null")){
					System.out.println("Sorry. No results found");
				}
				else{
					System.out.println("job completed: "+packet_from_tracker.data);
				}
			}
			else{
				System.out.println("unknown packet from sender with id :"+packet_from_tracker.request_type);
			}
		}
		catch (IOException e){
			//System.err.println("Error: "+e.getMessage());
			System.err.println("disconnected from Job Tracker. Attempting to reconnect....");
			//set the watch first
			/*try{
				zk.exists(TrackerPath,watcher);

				//wait for socket to be reestablished
				latch.await();
			}*/
			
				try{
					zk.exists(TrackerPath,watcher);
					while(locked){
						System.out.println("sleeping");
						Thread.sleep(50);
					}
				}
				catch (KeeperException f){
					System.err.println("KeeperException "+e.getMessage());
				}
				catch(InterruptedException f){
					System.err.println("latch not smart enough to count. you are fucked");
					System.exit(1);
				}
				


				
		}
		//}
		catch (ClassNotFoundException e){
			System.err.println(e.getMessage());
		}
		
	}
	
	public void disconnect() throws IOException {
		//send bye packet
		InstructionPacket packet = new InstructionPacket(InstructionPacket.JOB_QUIT, "");
		outstream.writeObject(packet);
		this.outstream.close();
		this.instream.close();
		this.to_tracker.close();
		
	}
	
    
	    public static void main(String[] args) {
		
		if (args.length != 2) {
		   System.out.println("Usage: client [zkhost] [zkport]");
		   System.exit(1);
		    return;
		}

		String hosts = args[0]+":"+String.valueOf(args[1]);
		//initialize the client	
		Client client = new Client(hosts);
		//user input
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		String userInput = null;
		System.out.print("client:> ");
		try{
			while((userInput = stdIn.readLine()) != null && userInput.toLowerCase().indexOf("quit") == -1){
				StringTokenizer st = new StringTokenizer(userInput);
				if(st.countTokens() > 2 || st.countTokens() < 1){
					System.out.println("Usage: {{job | status} [hash]} | quit");
				}
				else{
					String task = st.nextToken();
					if(task.toLowerCase().equals("job")){
						if(st.hasMoreTokens()){
							String hash = st.nextToken();
							client.handle_job(InstructionPacket.JOB_REQUEST, hash);
						}
						else{
							System.out.println("Usage: {{job | status} [hash]} | quit");
						}
					}
					else if(task.toLowerCase().equals("status")){
						if(st.hasMoreTokens()){
							String hash = st.nextToken();
							client.handle_job(InstructionPacket.JOB_STATUS, hash);
						}
						else{
							System.out.println("Usage: {{job | status} [hash]} | quit");
						}
					}
					else{
						System.out.println("Usage: {{job | status} [hash]} | quit");
					}
				}
				System.out.print("client:> ");
			}
			//disconnect from tracker
			System.out.println("disconnecting from tracker. good bye");
			client.disconnect();
			
		}
		catch (IOException e){
			System.err.println("IOException uh-oh "+e.getMessage());
		} 

	}
}
