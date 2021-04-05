
import java.io.*;
import java.net.*;
import java.util.*;

public class Client {

	private static final String INITIATE_MESSAGE = "Network";
	private static final int PORT = 5000;
	private static final int PACKET_COUNT = 1000;
	private static final int SEQ_LIMIT = 64;
	private static final int RETRANSMISSION_INTERVAL = 100;
	
	private static int[] sendingArray;
	private static int windowSize = 0;
	private static int totalSentCount = 0;
	

	public static void main(String[] args)
			throws UnknownHostException, IOException, ClassNotFoundException, InterruptedException {
		// get the localhost IP address -- to be changed to the Server IP
		InetAddress host =  InetAddress.getLocalHost(); //InetAddress.getByName("10.0.0.122");
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
		socket.close();
	}

	public static void serverCommunication(DataOutputStream dataOutput, DataInputStream dataInput) {
		Queue<Integer> droppedPackets = new LinkedList<Integer>();
		Random rnd = new Random();
		List<Integer> list = new ArrayList<Integer>();
		

		int retransmissionSequence = RETRANSMISSION_INTERVAL;
		int lastByteSent = 0;
		int lastByteWritten = 0;
		int acknowledgedByte = 0;
		int noOfBytesAcknowledged = 0;
		
		
		try {
			// entering sequence numbers
			int[] application_Array = new int[PACKET_COUNT];

			for (int i = 0; i < PACKET_COUNT; i++) {
				application_Array[i] = (i % SEQ_LIMIT) + 1;
			}

			// Sending packets by sliding window
			while (lastByteWritten < PACKET_COUNT) {
				//Get window size from server
				windowSize = dataInput.readInt();
				System.out.println("Window size : " + windowSize);
				
				//Creating a new sender window
				sendingArray = new int[windowSize];
				noOfBytesAcknowledged = 0;
				acknowledgedByte = 0;
				int i=0;
				
				
				//Retransmission of dropped packets after a fixed interval of sequences
				if(lastByteWritten > retransmissionSequence) {
					i = retransmitDroppedPackets(dataOutput, dataInput, droppedPackets, rnd, i);
					retransmissionSequence = lastByteWritten + RETRANSMISSION_INTERVAL;
				}
				
				
				//Processing for every byte in the window
				for (; i < windowSize && lastByteWritten < PACKET_COUNT; i++) {
					
					//Writing packets to the sending window
					sendingArray[i] = application_Array[lastByteWritten];
					lastByteWritten++;
					
					System.out.println("Current i : " + i);
					System.out.println("Last Byte Written " + lastByteWritten);
					//Generate probability of 1% by randomly sampling one integer from 100 values
					if (rnd.nextInt(100) == 0) {
						System.out.println("Dropped : " + i );
						droppedPackets.add(sendingArray[i]);
						continue;
					}
					
					//Sending the packet to the server
					dataOutput.writeInt(sendingArray[i]);
					totalSentCount++;
					System.out.println("Sent count : " + totalSentCount);
					list.add(sendingArray[i]);
					//System.out.println("Sending : " + sendingArray[i]);
					lastByteSent++;
					System.out.println("Last Byte Sent : " +lastByteSent);

				}
				if(lastByteWritten == PACKET_COUNT) {
					//Message server that client is done sending packets
					dataOutput.writeInt(-2);
				}else {
					dataOutput.writeInt(-1);
				}

				//Acknowledging packets from server till server sends a -1
				while(acknowledgedByte != -1) {
					//Reading acknowledgment from the server
					acknowledgedByte = dataInput.readInt();
					list.remove(new Integer(acknowledgedByte));
					//System.out.println("Acknowledged : " + acknowledgedByte);

					if (acknowledgedByte != -1)
						noOfBytesAcknowledged++;
				}
				
				System.out.println("--- phase 1---noofBytesAcknowledged " + noOfBytesAcknowledged);
				System.out.println("--- phase 1---lastByteWritten " + lastByteWritten);
				System.out.println("--- phase 1---lastByteSent " + lastByteSent);
			}
			
			
			//Retransmit remaining dropped packets in list
			System.out.println("Retransmitting dropped packets  -");
			while(!droppedPackets.isEmpty()) {
				int packet = droppedPackets.poll();
				dataOutput.writeInt(packet);
				totalSentCount++;
				System.out.println("Sent count : " + totalSentCount);
				list.add(packet);
				//System.out.println("Sending : " + packet);
			}
			dataOutput.writeInt(-1);
			
			acknowledgedByte = 0;
			while(acknowledgedByte != -1) {
				acknowledgedByte = dataInput.readInt();
				list.remove(new Integer(acknowledgedByte));
				//System.out.println("Acknowledged : " + acknowledgedByte);
			}

			System.out.println("List size : " + list.size());
			System.out.println("Total sent count : "+ totalSentCount);
		}

		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static int retransmitDroppedPackets(DataOutputStream dataOutput, DataInputStream dataInput, Queue<Integer> droppedPackets, Random rnd, int index) throws IOException {
		//Send all packets in the dropped list to the server
		while(index<windowSize && !droppedPackets.isEmpty() ) {
			int packet = droppedPackets.poll();
			
			//Generate probability of 1% by randomly sampling one integer from 100 values
			if (rnd.nextInt(100) == 0) {
				System.out.println("Dropped : " + packet);
				droppedPackets.add(packet);
				continue;
			}
			sendingArray[index] = packet;
			
			dataOutput.writeInt(sendingArray[index]);
			totalSentCount++;
			
			index++;
		}
		return index;
	}
}