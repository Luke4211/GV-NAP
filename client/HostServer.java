package client;

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class HostServer {
    private static int locSoc;
    public static void main(String[] args) {
        if(args.length != 1){
            try{
                locSoc = 10000;
                ServerSocket ss = new ServerSocket(locSoc);
                ServerSocket listener = ss;
                System.out.println("Centralized server running on default: " + ss.toString());
                ExecutorService threadPool = Executors.newFixedThreadPool(15);
                while(true) {
                    threadPool.execute(new ClientHandler(listener.accept()));
                }
            } catch(Exception e){e.printStackTrace();}
        }
        else{
            try{
                locSoc = Integer.parseInt(args[0]);
                ServerSocket ss = new ServerSocket(locSoc);
                ServerSocket listener = ss;
                System.out.println("Centralized server running on: " + ss.toString());
                ExecutorService threadPool = Executors.newFixedThreadPool(15);
                while(true) {
                    threadPool.execute(new ClientHandler(listener.accept()));
                }
            } catch(Exception e){e.printStackTrace();}
        }
    }


    //Runnable class which handles each file request.
    private static class ClientHandler implements Runnable {
        private Socket socket;
        private InputStream serverIn;            
        private BufferedOutputStream serverOut;
        private Path path= FileSystems.getDefault().getPath(".");
        private File curDir = path.toFile();

        ClientHandler(Socket socket) throws Exception {
            this.socket = socket;
            this.serverIn = socket.getInputStream();
            this.serverOut = new BufferedOutputStream(socket.getOutputStream());
        }
        
        //Read a message from the client, and return
        //it as a string. See Server.java for specifics
        private String getMessage() throws Exception {
        	byte[] msgLength = new byte[4];
    		this.serverIn.read(msgLength, 0, 4);
    		int len = ByteBuffer.wrap(msgLength).getInt();
    		byte[] msg = new byte[len];
    		this.serverIn.read(msg, 0, len);
    		return new String(msg);
        }
        //Send a message to the client. See Server.java for
        //specifics. 
        private void sendMessage(byte[] msg) throws Exception {
    		byte[] msgLen = ByteBuffer.allocate(4).putInt(msg.length).array();		
    		this.serverOut.write(msgLen, 0, 4);
    		this.serverOut.write(msg, 0, msg.length);
    	}
        
        //Start function for new threads. Read
        //in the requested filename, then convert it to
        //a byte array and write it to the client.
        @Override
        public void run() {
        		String request;
				try {
					request = this.getMessage();
					byte[] fbytes = Files.readAllBytes(Paths.get(request));
					this.sendMessage(fbytes);
					this.socket.close();
				} catch (Exception e) {					
					e.printStackTrace();
				}
        	
        }
        private File getCurDir(){
            //search current
            String name = "ftp_server.java";
            File[] fileList = curDir.listFiles();
            for(File f:fileList){
                if(f.isDirectory()){
                    File[] dir = f.listFiles();
                    for (File target:dir){
                        if (target.isFile() && target.getName().equalsIgnoreCase(name)) {
//                        System.out.println("curDir: " + f); //testing
                            return f;
                        }
                    }
                }
            }
            return curDir;
        }
    }
}
