/*
 * 
 * @author : 
 * Karan Shashin Shah  SJSU ID : 014490671
 * Shreya Satish Bhajikhaye SJSU ID : 014522560
 * 
 * 
 */


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
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
	private static final int PACKET_COUNT = 32768;
	private static final int SEQ_LIMIT = 65536;
	private static final int GOODPUT_FREQ = 1000;

	private static List<Integer> windowSizeList = new ArrayList<Integer>();
	private static List<Integer> totalPacketMissed = new ArrayList<Integer>();
	private static List<Integer> totalSequenceReceived = new ArrayList<Integer>();

	public static void main(String args[]) throws IOException, ClassNotFoundException {

		System.out.println("Server IP Address : " + InetAddress.getLocalHost());

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

		File windowFile = new File("C:\\Users\\admin\\Desktop\\window.txt");
		FileWriter windowfw = new FileWriter(windowFile);
		
		// printing window size
		for (int i = 0; i < windowSizeList.size(); i++) {
			windowfw.append(String.valueOf(windowSizeList.get(i)));
			windowfw.append("\n");
		}
		windowfw.close();
		

		System.out.println();
		System.out.println();
		
		File missedFile = new File("C:\\Users\\admin\\Desktop\\missed.txt");
		FileWriter missedfw = new FileWriter(missedFile);
		
		// printing packet missed
		for (int i = 0; i < totalPacketMissed.size(); i++) {
			missedfw.append(String.valueOf(totalPacketMissed.get(i)));
			missedfw.append("\n");
		}
		missedfw.close();

		System.out.println();
		System.out.println();
		
		File recvFile = new File("C:\\Users\\admin\\Desktop\\received.txt");
		FileWriter recvfw = new FileWriter(recvFile);
		// printing sequence numnber
		for (int i = 0; i < totalSequenceReceived.size(); i++) {
			recvfw.append(String.valueOf(totalSequenceReceived.get(i)));
			recvfw.append("\n");
		}
		recvfw.close();
			
		// close resources
		dataInput.close();
		dataOutput.close();
		socket.close();

		// close the ServerSocket object
		System.out.println();
		System.out.println();
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
		int nextByteExpected = 1;
		int totalAcknowledged = 0;

		double averageGoodput = 0;

		boolean continueTranmission = true;

		try {

			while (continueTranmission) {
				process: {

					// Sending window size to the client
					dataOutput.writeInt(windowSize);
					windowSizeList.add(windowSize);

					while (true) {
						// Read from the client
						lastByteRead = dataInput.readInt();

						// Client is done sending the window
						if (lastByteRead == -1 || lastByteRead == -2) {
							break;
						}

						// Storing packets into the receiving buffer
						receivingBuffer.add(lastByteRead);
						totalSequenceReceived.add(lastByteRead);

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
								totalPacketMissed.add(nextByteExpected);

								nextByteExpected = (nextByteExpected % SEQ_LIMIT) + 1;
							}
							nextByteExpected = (lastByteRead) % SEQ_LIMIT + 1;
						} else {
							// No packets are missing
							// Update expected byte for next byte read

							nextByteExpected = (lastByteRead) % SEQ_LIMIT + 1;
						}

						// Calculate the goodput after receiving every 1000 packets
						if (totalSequenceReceived.size() % GOODPUT_FREQ == 0) {
							double goodput = (totalSequenceReceived.size() * 1.0)
									/ (totalSequenceReceived.size() + missingPackets.size());
							averageGoodput += (goodput - averageGoodput)
									/ (totalSequenceReceived.size() / GOODPUT_FREQ);
						}

					}

					// Client is sending the last window ; Acknowledge all packets in the receiving
					// buffer
					if (lastByteRead == -2) {
						while (!receivingBuffer.isEmpty()) {
							dataOutput.writeInt(receivingBuffer.poll());
							lastByteAcknowledged = (lastByteAcknowledged + 1) % PACKET_COUNT;
							totalAcknowledged++;
						}
						dataOutput.writeInt(-1);
						continueTranmission = false;
						break process;
					} else {
						// Create random acknowledgement windows according to the available space in the
						// buffer
						int availableAcknowledgmentWindow = rnd.nextInt(receivingBuffer.size()) + 1;

						// Set window size as the number of acknowledgements to be sent
						windowSize = PACKET_COUNT - receivingBuffer.size() + availableAcknowledgmentWindow;

						while (availableAcknowledgmentWindow != 0) {
							// Sending acknowledgements for received packets
							dataOutput.writeInt(receivingBuffer.poll());

							lastByteAcknowledged = (lastByteAcknowledged + 1) % PACKET_COUNT;
							availableAcknowledgmentWindow--;
							totalAcknowledged++;
						}
					}

					// Stop sending acknowledgements to client
					dataOutput.writeInt(-1);
				}

			}

			// Receiving remaining retransmission from the client
			while (true) {
				lastByteRead = dataInput.readInt();

				if (lastByteRead == -1)
					break;
				receivingBuffer.add(lastByteRead);
				totalSequenceReceived.add(lastByteRead);

				missingPackets.remove(new Integer(lastByteRead));
			}

			// Sending acknowledgements for the re-sent packets
			lastByteAcknowledged = 0;
			while (!receivingBuffer.isEmpty()) {
				dataOutput.writeInt(receivingBuffer.poll());

				lastByteAcknowledged++;
				totalAcknowledged++;
			}
			dataOutput.writeInt(-1);

			System.out.println("Total Acknowledged : " + totalAcknowledged);
			System.out.println("Total Received : " + totalSequenceReceived.size());
			System.out.println("Average goodput : " + averageGoodput);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}