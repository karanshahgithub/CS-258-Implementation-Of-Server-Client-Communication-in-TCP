
import java.io.*;
import java.net.*;
import java.util.*;

public class Client {

	private static final String INITIATE_MESSAGE = "Network";
	private static final int PORT = 5000;
	private static final int PACKET_COUNT = 1000;
	private static final int SEQ_LIMIT = 64;
	private static final int RETRANSMISSION_INTERVAL = 10;

	private static int[] sendingArray;
	private static int windowSize = 0;
	private static int totalSentCount = 0;
	private static Map<Integer, List<Integer>> retransmissionSequenceMap = new HashMap<Integer, List<Integer>>();
	private static int globalRetransMissionCount = 0;

	public static void main(String[] args)
			throws UnknownHostException, IOException, ClassNotFoundException, InterruptedException {
		// get the localhost IP address -- to be changed to the Server IP
		InetAddress host = InetAddress.getLocalHost(); // InetAddress.getByName("10.0.0.122");
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
		System.out.println("Client IP Address : " + InetAddress.getLocalHost());
		System.out.println("Sending request to Server at : " + host);
		dataOutput.writeUTF(INITIATE_MESSAGE);
		String message = dataInput.readUTF();

		if (message != null) {
			System.out.println(message);
			serverCommunication(dataOutput, dataInput);

		} else {
			System.out.println("Connection not established");
		}

		System.out.println("Total packets sent : " + totalSentCount);

		// printing sequences in each retransmission
		for (Integer i : retransmissionSequenceMap.keySet()) {
			System.out.println("-----" + i + "-----");

			for (Integer j : retransmissionSequenceMap.get(i)) {

				System.out.println(j);
			}
		}

		// waiting for input
		// int temp = dataInput.readInt();

		// close resources
		dataOutput.close();
		dataInput.close();
		socket.close();
	}

	public static void serverCommunication(DataOutputStream dataOutput, DataInputStream dataInput) {
		Queue<Integer> droppedPackets = new LinkedList<Integer>();
		Random rnd = new Random();

		int retransmissionSequence = RETRANSMISSION_INTERVAL;
		int lastByteWritten = 0;
		int acknowledgedByte = 0;

		try {
			// entering sequence numbers
			int[] application_Array = new int[PACKET_COUNT];

			for (int i = 0; i < PACKET_COUNT; i++) {
				application_Array[i] = (i % SEQ_LIMIT) + 1;
			}

			// Sending packets by sliding window
			while (lastByteWritten < PACKET_COUNT) {
				// Get window size from server
				windowSize = dataInput.readInt();

				// Creating a new sender window
				sendingArray = new int[windowSize];
				acknowledgedByte = 0;
				int i = 0;

				// Retransmission of dropped packets after a fixed interval of sequences
				if (lastByteWritten > retransmissionSequence) {
					i = retransmitDroppedPackets(dataOutput, dataInput, droppedPackets, rnd, i);
					retransmissionSequence = lastByteWritten + RETRANSMISSION_INTERVAL;
				}

				// Processing for every byte in the window
				for (; i < windowSize && lastByteWritten < PACKET_COUNT; i++) {

					// Writing packets to the sending window
					sendingArray[i] = application_Array[lastByteWritten];
					lastByteWritten++;

					// Generate probability of 1% by randomly sampling one integer from 100 values
					if (rnd.nextInt(100) == 0) {
						droppedPackets.add(sendingArray[i]);
						continue;
					}

					// Sending the packet to the server
					dataOutput.writeInt(sendingArray[i]);
					totalSentCount++;
				}
				if (lastByteWritten == PACKET_COUNT) {
					// Message server that client is done sending packets
					dataOutput.writeInt(-2);
				} else {
					dataOutput.writeInt(-1);
				}

				// Acknowledging packets from server till server sends a -1
				while (acknowledgedByte != -1) {
					// Reading acknowledgment from the server
					acknowledgedByte = dataInput.readInt();
				}
			}

			// Retransmit remaining dropped packets in list
			List<Integer> transmittingSequence = new ArrayList<Integer>();

			if (!droppedPackets.isEmpty())
				globalRetransMissionCount++;

			while (!droppedPackets.isEmpty()) {
				int packet = droppedPackets.poll();
				dataOutput.writeInt(packet);
				transmittingSequence.add(packet);
				totalSentCount++;
			}

			retransmissionSequenceMap.put(globalRetransMissionCount, transmittingSequence);
			dataOutput.writeInt(-1);

			//Receive acknowledgement for the retransmitted packets
			acknowledgedByte = 0;
			while (acknowledgedByte != -1) {
				acknowledgedByte = dataInput.readInt();
			}
		}

		catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static int retransmitDroppedPackets(DataOutputStream dataOutput, DataInputStream dataInput,
			Queue<Integer> droppedPackets, Random rnd, int index) throws IOException {

		List<Integer> transmittingSequence = new ArrayList<Integer>();
		globalRetransMissionCount++;

		// Send all packets in the dropped list to the server
		while (index < windowSize && !droppedPackets.isEmpty()) {
			int packet = droppedPackets.poll();

			// Generate probability of 1% by randomly sampling one integer from 100 values
			if (rnd.nextInt(100) == 0) {
				droppedPackets.add(packet);
				continue;
			}
			sendingArray[index] = packet;
			dataOutput.writeInt(sendingArray[index]);
			transmittingSequence.add(sendingArray[index]);
			
			totalSentCount++;
			index++;
		}

		retransmissionSequenceMap.put(globalRetransMissionCount, transmittingSequence);
		return index;
	}
}