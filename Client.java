
import java.io.*;
import java.net.*;
import java.util.*;

public class Client {

	private static final String INITIATE_MESSAGE = "Network";
	private static final int PORT = 5000;
	private static final int PACKET_COUNT = 10000;
	private static final int SEQ_LIMIT = 200;
	private static final int RETRANSMISSION_INTERVAL = 100;
	

	public static void main(String[] args)
			throws UnknownHostException, IOException, ClassNotFoundException, InterruptedException {
		// get the localhost IP address -- to be changed to the Server IP
		InetAddress host = InetAddress.getLocalHost();
		Socket socket = null;
		DataOutputStream dataOutput = null;
		DataInputStream dataInput;

		// establish socket connection to server
		socket = new Socket(host.getHostName(), PORT);

		// write to socket using ObjectOutputStream
		dataOutput = new DataOutputStream(socket.getOutputStream());

		// read to socket using ObjectInputStream
		dataInput = new DataInputStream(socket.getInputStream());

		// Send handshake message to server to establish connection
		System.out.println("Sending request to Server");
		dataOutput.writeUTF(INITIATE_MESSAGE);
		String message = dataInput.readUTF();

		if (message != null) {
			System.out.println(message);
			serverCommunication(dataOutput, dataInput);
		} else {
			System.out.println("Connection not established");
		}

		// close resources
		dataOutput.close();
		dataInput.close();
	}

	public static void serverCommunication(DataOutputStream dataOutput, DataInputStream dataInput) {
		Queue<Integer> droppedPackets = new LinkedList<Integer>();
		Random rnd = new Random();
		int[] sendingArray;
		
		int windowSize = 100;
		int retransmissionSequence = RETRANSMISSION_INTERVAL;
		//int lastByteAcknowledged = 0;
		int lastByteSent = 0;
		int lastByteWritten = 0;
		int acknowledgedByte = 0;
		int noOfBytesAcknowledged = 0;
		//int check2 = 0;
		
		try {
			// entering sequence numbers
			int[] application_Array = new int[PACKET_COUNT];

			for (int i = 0; i < PACKET_COUNT; i++) {
				application_Array[i] = (i % SEQ_LIMIT) + 1;
			}

			// Sending packets by sliding window
			while (lastByteWritten < PACKET_COUNT) {

				//Creating a new sender window
				sendingArray = new int[windowSize];
				noOfBytesAcknowledged = 0;

				//Processing for every byte in the window
				for (int i = 0; i < windowSize && lastByteWritten < PACKET_COUNT; i++) {
					
					//Writing packets to the sending window
					sendingArray[i] = application_Array[lastByteWritten];
					lastByteWritten++;

					//Generate probability of 1% by randomly sampling one integer from 100 values
					if (rnd.nextInt(100) == 0) {
						System.out.println("dropped " + sendingArray[i]);
						droppedPackets.add(sendingArray[i]);
						continue;
					}

					//Sending the packet to the server
					dataOutput.writeInt(sendingArray[i]);
					System.out.println("---filling sendingArray---" + sendingArray[i]);
					lastByteSent++;

					//Reading acknowledgment from the server
					acknowledgedByte = dataInput.readInt();
					System.out.println("---AcknowledgeByte---" + acknowledgedByte);

					if (acknowledgedByte != 0) //
						noOfBytesAcknowledged++;
					//lastByteAcknowledged++;
				}

				// lastByteWritten = lastByteSent+noofBytesAcknowledged;
				//windowSize = noofBytesAcknowledged;
				System.out.println("--- phase 1---lastByteWritten " + lastByteWritten);
				System.out.println("--- phase 1---noofBytesAcknowledged " + noOfBytesAcknowledged);
				System.out.println("--- phase 1---lastByteSent " + lastByteSent);

				//Retransmission of dropped packets after a fixed interval of sequences
				if(lastByteWritten > retransmissionSequence) {
					retransmitDroppedPackets(dataOutput, dataInput, droppedPackets, rnd);
					retransmissionSequence = lastByteWritten + RETRANSMISSION_INTERVAL;
				}

				// checking lastByteWritten
				/*
				 * if(lastByteWritten>=PACKET_COUNT && check2==0){ lastByteWritten =
				 * PACKET_COUNT; check2 = -1; }
				 * 
				 * else if(lastByteWritten>=PACKET_COUNT && check2==-1){ break; }
				 */

				// dataOutput.writeInt(0);
			}
			
			//Retransmit remaining dropped packets in list
			retransmitDroppedPackets(dataOutput, dataInput, droppedPackets, rnd);
			
			//Message server that client is done sending packets
			dataOutput.writeInt(-1);
		}

		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void retransmitDroppedPackets(DataOutputStream dataOutput, DataInputStream dataInput, Queue<Integer> droppedPackets, Random rnd) throws IOException {
		//Send all packets in the dropped list to the server
		while(!droppedPackets.isEmpty()) {
			int packet = droppedPackets.poll();
			
			//Transmission with the same 1% probability as the original transmission
			if(rnd.nextInt(100) == 0) {
				droppedPackets.add(packet);
				continue;
			}
			
			dataOutput.writeInt(packet);
			System.out.println("Retransmitting packet : " + packet);
			int acknowledgedByte = dataInput.readInt();
			System.out.println("Acknowledgement received : " + acknowledgedByte);
		}
	}
}