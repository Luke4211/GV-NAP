package server;

import java.net.*;
import java.nio.ByteBuffer;
import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.HashMap;
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

        
        
        ClientHandler(Socket socket, ConcurrentNavigableMap<String, String[]> fileMap) throws Exception {
            this.socket = socket;
            this.serverIn = socket.getInputStream();
            this.serverOut = socket.getOutputStream();
            this.availableFiles = fileMap;
        }
        
        private ConcurrentNavigableMap<String, String[]> search(ConcurrentNavigableMap<String, String[]> map, String prefix) {
        	if(prefix.length() > 0) {
                return map.subMap(prefix, prefix + '\uffff');
        	}
        	return map;
        }

        
        private void fetchFiles() throws Exception {
        	String[] userData = {userName, hostname, speed};
        	String files = this.getMessage();
        	String[] fileList = files.split("<SEP>");
        	for(String s : fileList) {
        		this.availableFiles.put(s, userData);
        	}
    		
    		
        }
        
        private String getMessage() throws Exception {
        	byte[] msgLength = new byte[4];
    		this.serverIn.read(msgLength, 0, 4);
    		int len = ByteBuffer.wrap(msgLength).getInt();
    		byte[] msg = new byte[len];
    		this.serverIn.read(msg, 0, len);
    		return new String(msg);
        }
        
        void sendMessage(String send) throws Exception {
    		byte[] msg = send.getBytes();
    		byte[] msgLen = ByteBuffer.allocate(4).putInt(msg.length).array();		
    		this.serverOut.write(msgLen, 0, 4);
    		this.serverOut.write(msg, 0, msg.length);
    	}

        @Override
        public void run() {
        		String infoString;
				try {
					infoString = this.getMessage();
					String[] attributes = infoString.split(":");
	        		this.userName = attributes[0];
	        		this.hostname = attributes[1];
	        		this.speed = attributes[2];
	        		this.fetchFiles();	
	        		this.listen();
				} catch (Exception e) {					
					e.printStackTrace();
				}
        	
        }
        
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
        				
        	        	for(Map.Entry<String, String[]> entry : this.search(this.availableFiles, prefix).entrySet()) {
        	        		result += String.format("%-25.25s %-20.20s %-10.10s\n", entry.getKey(), entry.getValue()[1], entry.getValue()[2]);		
        	        	}
        	        	
        	        	this.sendMessage(result);
        	        	break;
        			
        		}
        	}
        } 
    
    }
}
