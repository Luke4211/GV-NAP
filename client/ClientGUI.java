package client;

import java.awt.EventQueue;
import java.awt.Font;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.io.*;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import java.awt.SystemColor;

public class ClientGUI implements ActionListener {

	private JFrame frame;
	private JTextField serverHost;
	private JTextField port;
	private JTextField username;
	private JTextField localHost;
	private JTextField searchField;
	private JButton cntBtn;
	private JButton btnSearch;
	private JButton btnSend;
	private JButton dscBtn;
	private JComboBox<String> speed;
	private Socket sock;
	private OutputStream clientOut;
	private InputStream clientIn;
	private JTextArea searchArea;
	private JTextArea terminal;
	private HashMap<String, String> fileMap;
	private JTextField cmdField;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ClientGUI window = new ClientGUI();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	//Connect to server, send it client information
	//as well as directory contents. 
	private void connect(String serverHost, String clientHost, int port, String username, String speed) {
		try {
			this.sock = new Socket(serverHost, port);
			this.clientOut = sock.getOutputStream();
			this.clientIn = sock.getInputStream();
			fileMap = new HashMap<String, String>();
			
			//Send username, IP, and connection speed to host.
			String hostInfo = username + ":" + clientHost + ":" + speed;
			this.sendMessage(hostInfo, this.clientOut);
			
			//Read filenames into a string separated by
			//<SEP>, and send to host.
			File dir = new File(System.getProperty("user.dir"));
			File[] files = dir.listFiles();
			String fileStr = "";
			
			for (File f : files) {
				fileStr += f.getName() + "<SEP>";
			}
			
			this.sendMessage(fileStr, this.clientOut);
			this.cntBtn.setEnabled(false);
			this.dscBtn.setEnabled(true);
			this.terminal.append(">>Connected.\n");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private String getMessage(InputStream in) throws Exception {
    	byte[] msgLength = new byte[4];
		in.read(msgLength, 0, 4);
		int len = ByteBuffer.wrap(msgLength).getInt();
		byte[] msg = new byte[len];
		in.read(msg, 0, len);
		return new String(msg);
    }
	
	private void getFile(InputStream in, String fname) throws Exception {
		byte[] msgLength = new byte[4];
		in.read(msgLength, 0, 4);
		int len = ByteBuffer.wrap(msgLength).getInt();
		FileOutputStream fout = new FileOutputStream(fname);
		byte[] msg = new byte[len];
		int bCount;
		while( (bCount = in.read(msg) ) > 0) {
			fout.write(msg, 0, bCount);
			
		}
	
		fout.close();
		
	}
	void sendMessage(String send, OutputStream out) throws Exception {
		byte[] msg = send.getBytes();
		byte[] msgLen = ByteBuffer.allocate(4).putInt(msg.length).array();		
		out.write(msgLen, 0, 4);
		out.write(msg, 0, msg.length);
	}
	
	public void actionPerformed(ActionEvent event) {
		if(event.getSource() == this.cntBtn ) {
			
			//Read in connections details from the GUI,
			//and then pass them to connect()
			String ip = String.valueOf(this.serverHost.getText());
			String clientHost = String.valueOf(this.localHost.getText());
			int port = Integer.parseInt(this.port.getText());
			String user = String.valueOf(this.username.getText());
			String speed = String.valueOf(this.speed.getSelectedItem());
			this.connect(ip, clientHost, port, user, speed);
			
		} else if(event.getSource() == this.btnSearch) {
			try {
				
				//Send search command to server
				this.sendMessage("search " + this.searchField.getText(), this.clientOut);
				
				//Read server response, split it on any sequence of whitespace,
				//and put them in the file map
				String response = this.getMessage(this.clientIn);
				String[] components = response.split("\\s+");
				if(components.length > 1) {
					for(int i = 0; i < components.length; i += 3) {
						this.fileMap.put(components[i], components[i+1]);
					}
				}
				
				//Clear the textbox, print labels and the query result.
				String cols = String.format("%-35.35s %-15.15s %-10.10s\n", "Filename/Owner", "Hostname", "Speed");
				this.searchArea.setText("");
				this.searchArea.append(cols);
				this.searchArea.append(response);
			} catch(Exception e) {
				e.printStackTrace();
				
			}
		} else if(event.getSource() == this.btnSend) {
			String[] componentStrings = this.cmdField.getText().split("/");
			String fname = componentStrings[0].split(" ")[1];
			String fnameOwner = this.cmdField.getText().split(" ")[1];
			if(this.fileMap.containsKey(fnameOwner)) {
				try {
					
					System.out.println(fname);
					Socket newSock = new Socket(this.fileMap.get(fnameOwner), 10001);
					BufferedInputStream Inp2p = new BufferedInputStream(newSock.getInputStream());
					OutputStream Outp2p = newSock.getOutputStream();
					
					//Send name of requested file to server.
					this.sendMessage(fname, Outp2p);
					this.getFile(Inp2p, fname);
					this.terminal.append(">>Downloading " + fname + "\n");
					Inp2p.close();
					newSock.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
						
			}
		} else if(event.getSource() == this.dscBtn) {
			try {
				this.sendMessage("disc", this.clientOut);
				this.searchArea.setText("");
				this.terminal.append(">>Disconnected.\n");
				this.cntBtn.setEnabled(true);
				this.dscBtn.setEnabled(false);
				this.sock.close();
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		
	}

	/**
	 * Create the application.
	 */
	public ClientGUI() {
		initialize();
		
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.getContentPane().setBackground(SystemColor.info);
		frame.setBounds(100, 100, 571, 550);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		JPanel panel = new JPanel();
		panel.setBounds(0, 0, 545, 100);
		frame.getContentPane().add(panel);
		panel.setLayout(null);
		
		JLabel lblServerHostname = new JLabel("Server Hostname:");
		lblServerHostname.setBounds(46, 11, 127, 14);
		panel.add(lblServerHostname);
		
		serverHost = new JTextField();
		serverHost.setText("localhost");
		serverHost.setBounds(174, 8, 141, 20);
		panel.add(serverHost);
		serverHost.setColumns(10);
		
		JLabel lblPort = new JLabel("Port:");
		lblPort.setBounds(365, 11, 46, 14);
		panel.add(lblPort);
		
		port = new JTextField();
		port.setText("10500");
		port.setBounds(443, 8, 86, 20);
		panel.add(port);
		port.setColumns(10);
		
		JLabel lblUsername = new JLabel("Username:");
		lblUsername.setBounds(46, 39, 70, 14);
		panel.add(lblUsername);
		
		username = new JTextField();
		username.setText("gorskil");
		username.setBounds(169, 36, 120, 20);
		panel.add(username);
		username.setColumns(10);
		
		JLabel lblHostname = new JLabel("Hostname:");
		lblHostname.setBounds(365, 48, 70, 14);
		panel.add(lblHostname);
		
		localHost = new JTextField();
		localHost.setBounds(443, 39, 86, 20);
		panel.add(localHost);
		localHost.setColumns(10);
		
		JLabel lblSpeed = new JLabel("Speed:");
		lblSpeed.setBounds(365, 74, 46, 14);
		panel.add(lblSpeed);
		
		speed = new JComboBox<String>();
		speed.setModel(new DefaultComboBoxModel<String>(new String[] {"Ethernet", "T1", "T2"}));
		speed.setSelectedIndex(0);
		speed.setBounds(443, 70, 76, 22);
		panel.add(speed);
		
		cntBtn = new JButton("Connect");
		cntBtn.addActionListener(this);
		cntBtn.setBounds(56, 70, 89, 23);
		panel.add(cntBtn);
		
		dscBtn = new JButton("Disconnect");
		dscBtn.setEnabled(false);
		dscBtn.addActionListener(this);
		dscBtn.setBounds(209, 70, 106, 23);
		panel.add(dscBtn);
		
		JPanel panel_1 = new JPanel();
		panel_1.setBounds(0, 111, 545, 218);
		frame.getContentPane().add(panel_1);
		panel_1.setLayout(null);
		
		JLabel label = new JLabel("");
		label.setBounds(217, 5, 0, 0);
		panel_1.add(label);
		
		JLabel lblKeyword = new JLabel("Keyword:");
		lblKeyword.setBounds(10, 11, 70, 14);
		panel_1.add(lblKeyword);
		
		searchField = new JTextField();
		searchField.setBounds(110, 8, 86, 20);
		panel_1.add(searchField);
		searchField.setColumns(10);
		
		btnSearch = new JButton("Search");
		btnSearch.addActionListener(this);
		btnSearch.setBounds(266, 5, 89, 23);
		panel_1.add(btnSearch);
		
		JPanel panel_2 = new JPanel();
		panel_2.setBounds(10, 49, 525, 158);
		panel_1.add(panel_2);
		panel_2.setLayout(null);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(10, 11, 505, 136);
		panel_2.add(scrollPane);
		
		searchArea = new JTextArea();
		searchArea.setFont(new Font("monospaced", Font.PLAIN, 12));
		searchArea.setColumns(3);
		scrollPane.setViewportView(searchArea);
		
		
		
		JPanel panel_3 = new JPanel();
		panel_3.setBounds(0, 340, 545, 160);
		frame.getContentPane().add(panel_3);
		panel_3.setLayout(null);
		
		JLabel lblEnterCommand = new JLabel("Enter Command: ");
		lblEnterCommand.setBounds(10, 135, 83, 14);
		panel_3.add(lblEnterCommand);
		
		btnSend = new JButton("Send");
		btnSend.addActionListener(this);
		btnSend.setBounds(210, 131, 89, 23);
		panel_3.add(btnSend);
		
		cmdField = new JTextField();
		cmdField.setBounds(93, 132, 86, 20);
		panel_3.add(cmdField);
		cmdField.setColumns(10);
		
		JScrollPane scrollPane_1 = new JScrollPane();
		scrollPane_1.setBounds(10, 11, 525, 113);
		panel_3.add(scrollPane_1);
		
		terminal = new JTextArea();
		scrollPane_1.setViewportView(terminal);
	}
}
