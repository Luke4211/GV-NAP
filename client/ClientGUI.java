package client;

import java.awt.EventQueue;
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JToggleButton;


import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.image.ComponentSampleModel;
import java.awt.event.ActionEvent;
import javax.swing.JTable;
import javax.swing.JTextPane;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.io.*;
import javax.swing.JTextArea;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

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
	private HashMap<String, String> fileMap = new HashMap<String, String>();
	private JTextField cmdField;
	private String user;

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
	
	void connect(String serverHost, String clientHost, int port, String username, String speed) {
		try {
			this.sock = new Socket(serverHost, port);
			this.clientOut = sock.getOutputStream();
			this.clientIn = sock.getInputStream();
			this.user = username;
			String hostInfo = username + ":" + clientHost + ":" + speed;
			
			this.sendMessage(hostInfo, this.clientOut);
			File dir = new File(System.getProperty("user.dir"));
			File[] files = dir.listFiles();
			String fileStr = "";
			
			for (File f : files) {
				fileStr += f.getName() + "<SEP>";
			}
			
			this.sendMessage(fileStr, this.clientOut);
			this.cntBtn.setEnabled(false);
			this.dscBtn.setEnabled(true);
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
	void sendMessage(String send, OutputStream out) throws Exception {
		byte[] msg = send.getBytes();
		byte[] msgLen = ByteBuffer.allocate(4).putInt(msg.length).array();		
		out.write(msgLen, 0, 4);
		out.write(msg, 0, msg.length);
	}
	
	public void actionPerformed(ActionEvent event) {
		if(event.getSource() == this.cntBtn ) {
			String ip = String.valueOf(this.serverHost.getText());
			String clientHost = String.valueOf(this.localHost.getText());
			int port = Integer.parseInt(this.port.getText());
			String user = String.valueOf(this.username.getText());
			String speed = String.valueOf(this.speed.getSelectedItem());
			this.connect(ip, clientHost, port, user, speed);
		} else if(event.getSource() == this.btnSearch) {
			try {
				this.sendMessage("search " + this.searchField.getText(), this.clientOut);
				String response = this.getMessage(this.clientIn);
				String[] components = response.split("\\s+");
				for(int i = 0; i < components.length; i += 3) {
					//System.out.println(components[i] + '\n' + components[i+1].split("/")[0]);
					this.fileMap.put(components[i], components[i+1]);
				}
				
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
			System.out.println(fname);
			if(this.fileMap.containsKey(fname + "/" + this.user)) {
				try {
					
					Socket newSock = new Socket(this.fileMap.get(this.cmdField.getText().split(" ")[1]), 10000);
					InputStream Inp2p = newSock.getInputStream();
					OutputStream Outp2p = newSock.getOutputStream();
					
					
					//String[] componentStrings = this.cmdField.getText().split("/");
					String send = componentStrings[0];
					this.sendMessage(fname, Outp2p);
					byte[] fbytes = this.getMessage(Inp2p).getBytes();
					
					FileOutputStream fout = new FileOutputStream(fname + "2");
					fout.write(fbytes, 0, fbytes.length);
					fout.close();
					newSock.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				
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
		localHost.setText("192.168.1.1");
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
		
		JTextPane textPane = new JTextPane();
		textPane.setBounds(10, 11, 473, 109);
		panel_3.add(textPane);
		
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
	}
}
