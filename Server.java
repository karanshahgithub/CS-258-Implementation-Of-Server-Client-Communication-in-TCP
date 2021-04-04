import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {

	// static ServerSocket variable
	private static ServerSocket server;

	// socket server port on which it will listen
	private static final int PORT = 5000;
	private static final String INITIATE_MESSAGE = "Network";
	private static final String SUCCESS_MESSAGE = "Connection Success";
	private static final int PACKET_COUNT = 10000;
	private static final int SEQ_LIMIT = 200;

	public static void main(String args[]) throws IOException, ClassNotFoundException {

		// create the socket server object
		server = new ServerSocket(PORT);

		// creating socket and waiting for client connection
		// System.out.println("Waiting for the client request");
		Socket socket = server.accept();
		DataOutputStream dataOutput = new DataOutputStream(socket.getOutputStream());

		// Setting connection with client
		// Read from socket to DataInputStream object
		DataInputStream dataInput = new DataInputStream(socket.getInputStream());
		String message = dataInput.readUTF();

		if (message.equals(INITIATE_MESSAGE)) {
			System.out.println(SUCCESS_MESSAGE);
			dataOutput.writeUTF(SUCCESS_MESSAGE);
			
			clientCommunication(dataInput, dataOutput);
		}

		// close resources
		dataInput.close();
		dataOutput.close();
		socket.close();

		// close the ServerSocket object
		System.out.println("Shutting down server");
		server.close();
	}

	public static void clientCommunication(DataInputStream dataInput, DataOutputStream dataOutput) {
		List<Integer> missingPackets = new ArrayList<Integer>();
		int[] receivingBuffer = new int[PACKET_COUNT];
		
		int windowSize = 100;
		int lastByteRead = 0;
		int lastByteAcknowledged = 0;
		int lastByteRcvd = 0;
		int nextByteExpected = 1;
		int missingPacketCount = 0;
		
		double goodputAverage = 0;
		
		boolean continueTranmission = true;
		
		try {

			while (continueTranmission) {

				// int count = dataInput.readInt();
				// System.out.println("Window size : " + count);

				int[] receivingArray = new int[windowSize];

				// Storing value in array
				for (int i = 0; i < windowSize; i++) {
					// Reading data into receiving window
					receivingArray[i] = dataInput.readInt();
					lastByteRead = receivingArray[i];

					if (lastByteRead == -1) {
						// Client has stopped sending packets
						continueTranmission = false;
						break;
					}

					// Storing packets into the receiving buffer
					receivingBuffer[lastByteRcvd] = lastByteRead;
					// rcvPacketCount++; // update number of packets received

					System.out.println("---receiving array---" + receivingArray[i]);

					// Checking for packets dropped at client end
					if (missingPackets.contains(receivingBuffer[lastByteRcvd])) {
						// Retranmission of dropped packets in progress
						// Update the missing packets list

						missingPackets.remove(new Integer(receivingBuffer[lastByteRcvd]));

					} else if (receivingBuffer[lastByteRcvd] != nextByteExpected) {
						// Byte read from received packet does match the expected byte
						// Add expected bytes to the missing packet list
						// Update expected byte for the next byte read
						while (nextByteExpected != receivingBuffer[lastByteRcvd]) {
							missingPackets.add(nextByteExpected);
							nextByteExpected = (nextByteExpected % SEQ_LIMIT) + 1;
						}

						nextByteExpected = (receivingBuffer[lastByteRcvd]) % SEQ_LIMIT + 1;
						missingPacketCount++;

					} else {
						// No packets are missing
						// Update expected byte for next byte read

						nextByteExpected = (receivingBuffer[lastByteRcvd]) % SEQ_LIMIT + 1;
					}
					lastByteRcvd++;

					// Sending acknowledgements for received packets
					dataOutput.writeInt(receivingBuffer[lastByteAcknowledged]);
					lastByteAcknowledged++;

					if (lastByteRcvd % 1000 == 0) {
						double goodput = (lastByteRcvd * 1.0) / (lastByteRcvd + missingPacketCount);
						goodputAverage += (goodput - goodputAverage) / (lastByteRcvd / 1000);
						System.out.println("Goodput after receving " + lastByteRcvd + " : " + goodput);
					}

				}
				// check = dataInput.readInt();
				// System.out.println("Check : " +check);
				// Print the goodput = received packets/sent packets after every 1000 packets
				// received
			}

			System.out.println("Average goodput : " + goodputAverage);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}