import java.io.Serializable;

public class InstructionPacket implements Serializable {
	public static final int JOB_DEFAULT = -1;

	//request
	public static final int JOB_STATUS = 0;
	public static final int JOB_REQUEST = 1;
	public static final int JOB_QUIT = 2;

	//responses
	public static final int JOB_IN_PROGRESS = 3;
	public static final int JOB_COMPLETED = 4;
	public static final int JOB_NOT_FOUND = 5;

	public int request_type = JOB_DEFAULT;
	public String data = null; //store the password or the hash
	
	public InstructionPacket(int type, String data){
		assert(type != JOB_DEFAULT && data != null);
		this.request_type = type;
		this.data = data;
	}

}