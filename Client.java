
import java.io.*;
import java.net.*;
import java.util.*;

public class Client {

	private static final String INITIATE_MESSAGE = "Network";
	private static final int PORT = 5000;

	public static void main(String[] args)
			throws UnknownHostException, IOException, ClassNotFoundException, InterruptedException {
		// get the localhost IP address -- to be changed to the Server IP
		InetAddress host = InetAddress.getLocalHost();
		Socket socket = null;
		DataOutputStream dataOutput = null;
		DataInputStream dataInput ;

		// establish socket connection to server
		socket = new Socket(host.getHostName(), PORT);

		// write to socket using ObjectOutputStream
		dataOutput = new DataOutputStream(socket.getOutputStream());

		// read to socket using ObjectInputStream
		dataInput  = new DataInputStream(socket.getInputStream());

		// Send handshake message to server to establish connection
		System.out.println("Sending request to Server");
		dataOutput.writeUTF(INITIATE_MESSAGE);
		String message = dataInput.readUTF();

		if(message!=null){

			System.out.println(message);
		}
		else{
			System.out.println("Connection not established");
		}


		serverCommunication(socket, dataOutput);

		// close resources
		dataOutput.close();
		Thread.sleep(100);
	}

	public static void serverCommunication(Socket socket, DataOutputStream dataOutput) {

		System.out.println("karan");
		// read the server response message
		DataInputStream dataInput = null;

		try {
			dataInput = new DataInputStream(socket.getInputStream());

			// entering sequence numbers
			int[] application_Array = new int[500];

			for(int i=0,j=1;i<500;i++){

				if(j%200==0){
					j=1;
				}

				application_Array[i] = j;
				j++;
			}

			
			int noofBytesAcknowledged = 0;
			int lastByteAcknowledged = 0;
			int lastByteSent = 0;
			int lastByteWritten = 200;
			int[] sendingArray;
			int check2 = 0;
			
			// Sending packets by sliding window
			
			while(lastByteSent<500)
			{
				
				sendingArray = new int[lastByteWritten-lastByteSent];
				dataOutput.writeInt(lastByteWritten-lastByteSent);
				System.out.println(lastByteWritten-lastByteSent);

				

				for(int i=0;i<sendingArray.length;i++){
					
					sendingArray[i] = application_Array[lastByteSent];                                  
					dataOutput.writeInt(sendingArray[i]);

					System.out.println("---filling sendingArray---"+sendingArray[i]);
					lastByteSent++;

					int AcknowledgedByte = dataInput.readInt();
					System.out.println("---AcknowledgeByteAcknowledgedByte---"+AcknowledgedByte);

					/*
					if(AcknowledgedByte==-1){
						lastByteAcknowledged--;
						break;
					}
					*/
					noofBytesAcknowledged++;


				lastByteAcknowledged++;
				}

			
			lastByteWritten = lastByteSent+noofBytesAcknowledged;

			// checking if lastByteWritten exceeds 
			noofBytesAcknowledged = 0;
			
			System.out.println("--- phase 1---lastByteWritten "+lastByteWritten);
			System.out.println("--- phase 1---noofBytesAcknowledged "+noofBytesAcknowledged);
			System.out.println("--- phase 1---lastByteSent "+lastByteSent);

			// checking lastByteWritten
			if(lastByteWritten>500 && check2==0){
				lastByteWritten = 500;
				check2 = -1;
			}

			else if(lastByteWritten>500 && check2==-1){
				break;
			}


			dataOutput.writeInt(0);

		}
		dataOutput.writeInt(-1);
			
	} 

	catch (IOException e) {
			e.printStackTrace();
		}

	}
}
