import java.io.Serializable;
 /**
 * InstructionalPacket
 * ============
 * 
 * Packet format of the packets exchanged between the Server and the Clients
 * 
 */


/* inline class to describe host/port combo */
class ClientLocation implements Serializable {
	public String  client_host;
	public Integer client_port;
	
	/* constructor */
	public ClientLocation(String host, Integer port) {
		this.client_host = host;
		this.client_port = port;
	}
	
	/* printable output */
	public String toString() {
		return " HOST: " + client_host + " PORT: " + client_port; 
	}
	
}

public class InstructionPacket implements Serializable {

	/* define constants */
	/* for part 1/2/3 */
	public static final int SERVER_NULL    = -1;
	public static final int SERVER_REQUEST = 101; //called when server wants to know position of clients
	public static final int SERVER_REPLY    = 102;//used when server replies to new client with hashmap
	public static final int SERVER_ACCEPT    = 103;//used when server accepts client 
	public static final int SERVER_DONE      = 104;//used to notify client that all running clients' info has been transmitted
	//public static final int ENQUEUE_NEW = 105;//used when server needs old clients to add new client
	
	/* for part 3 */
	public static final int CLIENT_ACTION  = 301; 
	public static final int CLIENT_REGISTER = 302;
	public static final int CLIENT_TOKEN = 303;
	public static final int CLIENT_ACK = 304;

	//different actions
	public static final int CLIENT_FORWARD  = 0x26; 
	public static final int CLIENT_TURN_RIGHT  = 0x27; 
	public static final int CLIENT_TURN_LEFT  = 0x25; 
	public static final int CLIENT_FIRE  = 0x20;
	public static final int CLIENT_BACKWARD = 0X28;
	public static final int CLIENT_DEAD  = 307; 
	public static final int CLIENT_BYE  = 308; //called when client leaves game
	
	//different orientations
	public static final int CLIENT_NORTH  = 0; 
	public static final int CLIENT_EAST  = 1; 
	public static final int CLIENT_SOUTH  = 2; 
	public static final int CLIENT_WEST  = 3; 
	
	/* error codes */
	/* for part 2/3 */
	public static final int ERROR_INVALID_ID   = -101;
	public static final int ERROR_OUT_OF_RANGE     = -102;
	public static final int ERROR_SYMBOL_EXISTS    = -103;
	public static final int ERROR_INVALID_EXCHANGE = -104;
	
	public String client_id; //will contain name of client, among other things

	public int action = -1; // client action movements

	public int request_type = -1; //either action or register
	
	//public Long[2] position; //position of client
	
	public int pos_x = -1;
	
	public int pos_y = -1;	

	public int error_code = -1; 
	
	public int orientation = -1;

	public ClientLocation location; //name and port number of client

	public InstructionPacket(InstructionPacket another) {
		this.client_id = new String(another.client_id);
		this.request_type = another.request_type;
		this.pos_x = another.pos_x;
		this.pos_y = another.pos_y;
		this.action = another.action;
		this.error_code = another.error_code;
		this.location = new ClientLocation(another.location.client_host, another.location.client_port);		 
	}

	public InstructionPacket(){
		
	}
}
