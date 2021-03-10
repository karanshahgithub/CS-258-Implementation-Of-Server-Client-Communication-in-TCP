

import java.io.*;
import java.net.*;

public class Client {

	private static final String INITIATE_MESSAGE = "Network";
    private static final int PORT = 5000;
	
	public static void main(String[] args) throws UnknownHostException, IOException, ClassNotFoundException, InterruptedException{
        //get the localhost IP address -- to be changed to the Server IP
        InetAddress host = InetAddress.getLocalHost();
        Socket socket = null;
        DataOutputStream dataOutput = null;
        
        //establish socket connection to server
        socket = new Socket(host.getHostName(), PORT);
        
        //write to socket using ObjectOutputStream
        dataOutput = new DataOutputStream(socket.getOutputStream());

        //Send handshake message to server to establish connection
        System.out.println("Sending request to Server");
        dataOutput.writeUTF(INITIATE_MESSAGE);
       
        serverCommunication(socket, dataOutput);
        
        //close resources
        dataOutput.close();
        Thread.sleep(100);
    }
	
	public static void serverCommunication(Socket socket, DataOutputStream dataOutput) {
		//read the server response message
        DataInputStream dataInput = null;

        try {
			dataInput = new DataInputStream(socket.getInputStream());
	         
			for(int i=0; i<5; i++) {
				//Sending packets as sequence number to server
				dataOutput.writeInt(i);

				//Acknowledged sequences
				String message = dataInput.readUTF();
				System.out.println(message);
			}
			
			//closing the connection with server
			dataOutput.writeInt(-1);
			
	        dataInput.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
        
	}
}
