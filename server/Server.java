package server;

import java.net.*;
import java.nio.ByteBuffer;
import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.Map;

public class Server {
	private static ConcurrentNavigableMap<String, String[]> availableFiles = new ConcurrentSkipListMap<String, String[]>();
    public static void main(String[] args) {
        try(ServerSocket listener = new ServerSocket(10500)) {
            System.out.println("Server running");
            ExecutorService threadPool = Executors.newFixedThreadPool(15);
            while(true) {
                threadPool.execute(new ClientHandler(listener.accept(), availableFiles)); 
            } 
        } catch(Exception e){e.printStackTrace();}
    }


    private static class ClientHandler implements Runnable {
        private Socket socket;
        private InputStream serverIn;            
        private OutputStream serverOut;
        private String hostname;
        private String userName;
        private String speed;
        private ConcurrentNavigableMap<String, String[]> availableFiles;
        private ArrayList<String> clientFiles;


        
        ClientHandler(Socket socket, ConcurrentNavigableMap<String, String[]> fileMap) throws Exception {
            this.socket = socket;
            this.serverIn = socket.getInputStream();
            this.serverOut = socket.getOutputStream();
            this.availableFiles = fileMap;
            this.clientFiles = new ArrayList<String>();
        }
        
        //Once a new thread is spawned, read in information
        //about the client. Then read in it's directory
        //contents, and go to listen() to wait for further
        //instructions
        @Override
        public void run() {
        		String infoString;
				try {
					infoString = this.getMessage();
					String[] attributes = infoString.split(":");
	        		this.userName = attributes[0];
	        		this.hostname = attributes[1];
	        		this.speed = attributes[2];
	        		System.out.println("Client " + this.userName + " has connected.");
	        		this.fetchFiles();	 
	        		this.listen();
				} catch (Exception e) {					
					e.printStackTrace();
				}
        	
        }
        
        //Listen to the client to either request
        //a search of the client pool's directory, 
        //or to quit (TODO)
        private void listen() throws Exception {
        	boolean connected = true;
        	while(connected) {
        		String cmd = this.getMessage();
        		String[] words = cmd.split(" ");
        		
        		switch(words[0]) {
        			case "search":
        				String result = "";
        				String prefix;
        				
        				if(words.length < 2) {
        					prefix = "";
        				} else {
        					prefix = words[1];
        				}
        				
        				//Append each matching search result to result,
        				//and format it to ensure proper spacing.
        	        	for(Map.Entry<String, String[]> entry : this.search(this.availableFiles, prefix).entrySet()) {
        	        		result += String.format("%-35.35s %-15.15s %-10.10s \n ", entry.getKey(), entry.getValue()[1], entry.getValue()[2]);		
        	        	}
        	        	
        	        	this.sendMessage(result);
        	        	break;
        			case "disc":
        				connected = false;
        				this.removeFiles();
        				this.socket.close();
        				System.out.println("Client " + this.userName + " has disconnected.");
        				
        				break;
        			
        			case "reset":
        				this.removeFiles();
        				this.fetchFiles();
        			
        		}
        	}
        }
        
        
        private void removeFiles() {
        	for(String file : this.clientFiles) {
				this.availableFiles.remove(file);
				
			}
        	
        	this.clientFiles.removeAll(this.clientFiles);
        	}
        
        //Returns a submap of the available files based on
        //a search term.
        private ConcurrentNavigableMap<String, String[]> search(ConcurrentNavigableMap<String, String[]> map, String prefix) {
        	if(prefix.length() > 0) {
        		
        		//Return a submap whose keys are in the range
        		//between prefix and the highest unicode character.
                return map.subMap(prefix, prefix + '\uffff');
        	}
        	return map;
        }

        //Read in the contents of the client's directory,
        //and add it to the map of available files.
        private void fetchFiles() throws Exception {
        	String[] userData = {userName, hostname, speed};
        	String files = this.getMessage();
        	String[] fileList = files.split("<SEP>");
        	for(String s : fileList) {
        		this.availableFiles.put(s + "/" + this.userName, userData);
        		this.clientFiles.add(s + "/" + this.userName);
        		
        	}   		
    		
        }
        
        //Read a message from the client, and return
        //it as a string.
        private String getMessage() throws Exception {
        	//First read a 32 bit integer indicating
        	//how many bytes to read for the message.
        	byte[] msgLength = new byte[4];
    		this.serverIn.read(msgLength, 0, 4);
    		int len = ByteBuffer.wrap(msgLength).getInt();
    		
    		//Next read len bytes of data and return it
    		//as a string.
    		byte[] msg = new byte[len];
    		this.serverIn.read(msg, 0, len);
    		return new String(msg);
        }
        
        //Write a message to the client
        private void sendMessage(String send) throws Exception {
        	//In the same manner as getMessage(),
        	//send a 32 bit integer representing
        	//how many bytes to read for the message
    		byte[] msg = send.getBytes();
    		byte[] msgLen = ByteBuffer.allocate(4).putInt(msg.length).array();		
    		this.serverOut.write(msgLen, 0, 4);
    		
    		//Next write the message.
    		this.serverOut.write(msg, 0, msg.length);
    	}
    
    }
}
