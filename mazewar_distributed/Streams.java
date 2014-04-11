import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Streams {
	public ObjectInputStream inputstream;
	public ObjectOutputStream outputstream;
	public InstructionPacket location_packet;

	public Streams(ObjectInputStream in, ObjectOutputStream out, InstructionPacket l_packet){
		inputstream = in;
		outputstream = out;
		location_packet = l_packet;
	}
}
