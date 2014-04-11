import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Streams {
	public ObjectInputStream inputstream;
	public ObjectOutputStream outputstream;
	public InstructionPacket location_packet;
	public Streams(ObjectInputStream in, ObjectOutputStream out){
		inputstream = in;
		outputstream = out;
		location_packet = null;;
	}
}
