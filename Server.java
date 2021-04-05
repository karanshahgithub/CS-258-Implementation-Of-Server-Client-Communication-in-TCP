import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

public class Server {

	// static ServerSocket variable
	private static ServerSocket server;

	// socket server port on which it will listen
	private static final int PORT = 5000;
	private static final String INITIATE_MESSAGE = "Network";
	private static final String SUCCESS_MESSAGE = "Connection Success";
	private static final int PACKET_COUNT = 32 ;
	private static final int SEQ_LIMIT = 64;

	public static void main(String args[]) throws IOException, ClassNotFoundException {

		// create the socket server object
		server = new ServerSocket(PORT);

		// creating socket and waiting for client connection
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
		Queue<Integer> receivingBuffer = new LinkedList<Integer>();
		Random rnd = new Random();

		int windowSize = PACKET_COUNT;
		int lastByteRead = 0;
		int lastByteAcknowledged = 0;
		int lastByteRcvd = 0;
		int nextByteExpected = 1;
		int missingPacketCount = 0;
		int totalAcknowledged = 0;
		int receivedPacketCount = 0;

		double averageGoodput= 0;

		boolean continueTranmission = true;

		try {

			while (continueTranmission) {
				process: {
				// Sending window size to the client
				System.out.println("Sending window size : " + windowSize);
				dataOutput.writeInt(windowSize);

				while (true) {
					// Read from the client
					lastByteRead = dataInput.readInt();
					;

					// Client is done sending the window
					if (lastByteRead == -1 || lastByteRead == -2) {
						break;
					} 

					// Storing packets into the receiving buffer
					receivingBuffer.add(lastByteRead);
					receivedPacketCount++;

					//System.out.println("Received : " + receivingBuffer[lastByteRcvd]);

					// Checking for packets dropped at client end
					if (missingPackets.contains(lastByteRead)) {
						// Retranmission of dropped packets in progress
						// Update the missing packets list

						missingPackets.remove(new Integer(lastByteRead));

					} else if (lastByteRead != nextByteExpected) {
						// Byte read from received packet does match the expected byte
						// Add expected bytes to the missing packet list
						// Update expected byte for the next byte read
						while (nextByteExpected != lastByteRead) {
							missingPackets.add(nextByteExpected);
							nextByteExpected = (nextByteExpected % SEQ_LIMIT) + 1;
						}

						nextByteExpected = (lastByteRead) % SEQ_LIMIT + 1;
						missingPacketCount++;

					} else {
						// No packets are missing
						// Update expected byte for next byte read

						nextByteExpected = (lastByteRead) % SEQ_LIMIT + 1;
					}
					lastByteRcvd = (lastByteRcvd + 1) % PACKET_COUNT;


					if (receivedPacketCount % 1000 == 0) { 
						double goodput = (receivedPacketCount * 1.0) / (receivedPacketCount + missingPacketCount);
						averageGoodput += (goodput - averageGoodput) / (receivedPacketCount/1000);
						System.out.println("Goodput after receving " + receivedPacketCount + " : " +  goodput); 
					}

				}


				if(lastByteRead == -2) {
					while(!receivingBuffer.isEmpty()) {
						dataOutput.writeInt(receivingBuffer.poll());
						//System.out.println("Acknowledgement sent : " + receivingBuffer[lastByteAcknowledged] );
						//receivingBuffer[lastByteAcknowledged] = 0;
						lastByteAcknowledged = (lastByteAcknowledged + 1) % PACKET_COUNT;
						totalAcknowledged++;
					}
					dataOutput.writeInt(-1);
					continueTranmission = false;
					break process;
				}else {
					//int availableAcknowledgmentWindow = rnd.nextInt(PACKET_COUNT) + 1;
					
					int availableAcknowledgmentWindow = rnd.nextInt(receivingBuffer.size()) + 1; 
					System.out.println("Available ack window : " +availableAcknowledgmentWindow);
					// Set window size as the number of acknowledgements to be sent
					windowSize = PACKET_COUNT - receivingBuffer.size() + availableAcknowledgmentWindow;
					
					//System.out.println("LastByteAcknowledged : " + lastByteAcknowledged);
					
					while (availableAcknowledgmentWindow != 0) {
						
						// Sending acknowledgements for received packets
						dataOutput.writeInt(receivingBuffer.poll());
						
						//System.out.println("Acknowledgement sent : " +	receivingBuffer[lastByteAcknowledged]);
						lastByteAcknowledged = (lastByteAcknowledged + 1) % PACKET_COUNT;
						availableAcknowledgmentWindow--;
						totalAcknowledged++;
					}
				}

				System.out.println("Total Acknowledged : " + totalAcknowledged);
				// Stop sending acknowledgements to client
				dataOutput.writeInt(-1);
			}

			}

			System.out.println("Retransmission -  ");
			lastByteRcvd = 0;
			while(true) {
				lastByteRead = dataInput.readInt();
				//System.out.println("Received : " +lastByteRead);
				if(lastByteRead == -1)
					break;
				receivingBuffer.add(lastByteRead);
				missingPackets.remove(new Integer(lastByteRead));
				receivedPacketCount++;
				lastByteRcvd++;
			}

			lastByteAcknowledged = 0;
			while(!receivingBuffer.isEmpty()) {
				dataOutput.writeInt(receivingBuffer.poll());
				//System.out.println("Acknowledgement sent : " + receivingBuffer[lastByteAcknowledged] );
				lastByteAcknowledged++;
				totalAcknowledged++;
			}
			dataOutput.writeInt(-1);
			
			System.out.println("Total Acknowledged : " + totalAcknowledged);
			System.out.println("Total Received : " + receivedPacketCount);
			System.out.println("Average goodput : " + averageGoodput);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}