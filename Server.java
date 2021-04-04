import java.io.*;
import java.net.*;

public class Server {

	// static ServerSocket variable
	private static ServerSocket server;

	// socket server port on which it will listen
	private static final int PORT = 5000;
	private static final String INITIATE_MESSAGE = "Network";
	private static final String SUCCESS_MESSAGE = "Connection Success";
	private static int[] receivingBuffer = new int[10000000];
	private static int lastReadByte = 0;
	private static int lastByteAcknowledged = 0;

	public static void main(String args[]) throws IOException, ClassNotFoundException {

		// create the socket server object
		server = new ServerSocket(PORT);

		// creating socket and waiting for client connection
		//System.out.println("Waiting for the client request");
		Socket socket = server.accept();
		DataOutputStream dataOutput = new DataOutputStream(socket.getOutputStream());
		

		// Setting connection with client
		// Read from socket to DataInputStream object
		DataInputStream dataInput = new DataInputStream(socket.getInputStream());
		String message = dataInput.readUTF();

		if (message.equals(INITIATE_MESSAGE)) {
			System.out.println(SUCCESS_MESSAGE);
			
			dataOutput.writeUTF(SUCCESS_MESSAGE);
		}


		clientCommunication(socket, dataInput);
		

		//close resources
		dataInput.close();
		socket.close();

		// close the ServerSocket object
		System.out.println("Shutting down server");
		server.close();
	}

	public static void clientCommunication(Socket socket, DataInputStream dataInput) {
		try (DataOutputStream dataOutput = new DataOutputStream(socket.getOutputStream())) {

		int check = 0;	

		while(check==0){

			int count = dataInput.readInt();
			System.out.println(count);
			int[] receivingArray = new int[count];

			// Storing value in array
			for(int i=0;i<count;i++){
				receivingArray[i] = dataInput.readInt();
				receivingBuffer[lastReadByte] = receivingArray[i];
				lastReadByte++;

				System.out.println("---receiving array---"+receivingArray[i]);

				dataOutput.writeInt(receivingBuffer[lastByteAcknowledged]);
				
				lastByteAcknowledged++;

				
			}


			check = dataInput.readInt();
			System.out.println(check);
		}
			
	} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
