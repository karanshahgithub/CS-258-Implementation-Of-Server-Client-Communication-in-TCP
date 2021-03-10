import java.io.*;
import java.net.*;

public class Server {
    
    //static ServerSocket variable
    private static ServerSocket server;
    
    //socket server port on which it will listen
    private static final int PORT = 5000;
	private static final String INITIATE_MESSAGE = "Network";
	private static final String SUCCESS_MESSAGE = "Connection Success";
    
    public static void main(String args[]) throws IOException, ClassNotFoundException{
        //create the socket server object
        server = new ServerSocket(PORT);

        //creating socket and waiting for client connection
        System.out.println("Waiting for the client request");
        Socket socket = server.accept();
        
        //Setting connection with client
        //Read from socket to DataInputStream object
        DataInputStream dataInput = new DataInputStream(socket.getInputStream());
        String message = dataInput.readUTF();

        if(message.equals(INITIATE_MESSAGE)) {
        	System.out.println(SUCCESS_MESSAGE);
        	clientCommunication(socket, dataInput);
        }
       
        //close resources
        dataInput.close();
        socket.close();
        
        //close the ServerSocket object
        System.out.println("Shutting down server");
        server.close();
    }
    
    public static void clientCommunication(Socket socket, DataInputStream dataInput) {
        try(DataOutputStream dataOutput = new DataOutputStream(socket.getOutputStream())){
        	while(true) {
        		//Read data from client
        		int acknw = dataInput.readInt();
        		System.out.println("Received : " + acknw);
        		if(acknw < 0)
        			break;
        		
        		//Send acknowledgement to client
        		dataOutput.writeUTF("Acknowledged : " + acknw);
        	}
        	
        } catch (IOException e) {
			e.printStackTrace();
		}
        
    }
    
}
