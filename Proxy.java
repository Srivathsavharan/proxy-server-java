import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.border.LineBorder;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import javax.imageio.ImageIO;

public class Proxy implements Runnable,ActionListener{
		 
	private ServerSocket serverSocket;

	private volatile boolean running = true;

	static HashMap<String, File> cache;

	static HashMap<String, String> blockedSites;

	static ArrayList<Thread> servicingThreads;
	
	
	static int lim=3,c=0;
	//static ArrayList<String> urlkey;
	
	String com="",command="",s="";
	
//	int limit=3,count=0;
	static Object ob = new Object();
	
	JFrame f1=new JFrame("Proxy");
	JLabel l1,l2,outputLabel;
	JRadioButton rb1,rb2,rb3;
	JButton b1,addMore,ok,HTML_CONTENT;
	JPanel p1,p2,innerpanel,outputPanel;
	JTextArea ta1;
	JScrollPane jp,jp1;
	JLabel l=new JLabel();
	public Proxy(int port) {
		f1.setSize(400,400);
		f1.setLayout(new GridLayout(1,1));
		p1 = new JPanel();
		p2=new JPanel();
		HTML_CONTENT=new JButton("HTML_CONTENT");
		HTML_CONTENT.setBounds(80,250,200,30);
		innerpanel=new JPanel();
		outputPanel=new JPanel();
		outputLabel=new JLabel();
		p1.setLayout(null);
		p2.setLayout(null);
		outputPanel.setLayout(new GridLayout(1,1));
		innerpanel.setLayout(new GridLayout(1,1));
		l1=new JLabel("Select the function");
		innerpanel.setBounds(50, 50, 300, 200);
		ta1=new JTextArea(15,40);
		l2=new JLabel();
		jp= new JScrollPane(l2,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		jp1= new JScrollPane(ta1,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		outputPanel.add(jp1);
		innerpanel.add(jp);
		p2.add(innerpanel);
		addMore=new JButton("ADD MORE");
		addMore.setBounds(100,280,100,30);
		ok=new JButton("OK");
		ok.setBounds(230,280,80,30);
		ok.addActionListener(this);
		addMore.addActionListener(this);
		HTML_CONTENT.addActionListener(this);
		p2.add(addMore);
		p2.add(ok);
		l1.setFont(new Font("Courier", Font.BOLD,18));
		l1.setBounds(70, 50, 210, 20);
		rb1=new JRadioButton("cached"); 
		rb1.setBounds(50,100,100,30);
		rb2=new JRadioButton("blocked"); 
		rb2.setBounds(50,150,100,30);
		rb3=new JRadioButton("close"); 
		rb3.setBounds(50,200,100,30);
		ButtonGroup bg=new ButtonGroup();    
		bg.add(rb1);
		bg.add(rb2);    
		bg.add(rb3);    		
		b1=new JButton("SUBMIT");
		b1.setBounds(210,150,100,30);
		b1.addActionListener(this);
		p1.add(l1);
		p1.add(rb1);
		p1.add(rb2);
		p1.add(rb3);
		p1.add(b1);
		p1.add(HTML_CONTENT);
		f1.add(p1);
		f1.setVisible(true);
		f1.setLocationRelativeTo(null);
        f1.setDefaultCloseOperation(f1.EXIT_ON_CLOSE);
		
		// Load in hash map containing previously cached sites and blocked Sites
		cache = new HashMap<>();
		blockedSites = new HashMap<>();

		// Create array list to hold servicing threads
		servicingThreads = new ArrayList<>();

		// Start dynamic manager on a separate thread.
		new Thread(this).start();	// Starts overriden run() method at bottom

		try{
			// Load in cached sites from file
			File cachedSites = new File("cachedSites.txt");
			if(!cachedSites.exists()){
				System.out.println("No cached sites found - creating new file");
				cachedSites.createNewFile();
			} else {
				FileInputStream fileInputStream = new FileInputStream(cachedSites);
				ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
				cache = (HashMap<String,File>)objectInputStream.readObject();
				fileInputStream.close();
				objectInputStream.close();
			}

			// Load in blocked sites from file
			File blockedSitesTxtFile = new File("blockedSites.txt");
			if(!blockedSitesTxtFile.exists()){
				System.out.println("No blocked sites found - creating new file");
				blockedSitesTxtFile.createNewFile();
			} else {
				FileInputStream fileInputStream = new FileInputStream(blockedSitesTxtFile);
				ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
				blockedSites = (HashMap<String, String>)objectInputStream.readObject();
				fileInputStream.close();
				objectInputStream.close();
			}
		} catch (IOException e) {
			//System.out.println("Error loading previously cached sites file");
			//e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.out.println("Class not found loading in preivously cached sites file");
			e.printStackTrace();
		}

		try {
			// Create the Server Socket for the Proxy 
			serverSocket = new ServerSocket(port);
			
			// Set the timeout
			//serverSocket.setSoTimeout(100000);	// debug
			System.out.println("Waiting for client on port " + serverSocket.getLocalPort() + "..");
			running = true;
		} 

		// Catch exceptions associated with opening socket
		catch (SocketException se) {
			System.out.println("Socket Exception when connecting to client");
			se.printStackTrace();
		}
		catch (SocketTimeoutException ste) {
			System.out.println("Timeout occured while connecting to client");
		} 
		catch (IOException io) {
			System.out.println("IO exception when connecting to client");
		}
	}


	/**
	 * Listens to port and accepts new socket connections. 
	 * Creates a new thread to handle the request and passes it the socket connection and continues listening.
	 */
	public void listen(){

		while(running){
			try {
				// serverSocket.accpet() Blocks until a connection is made
				Socket socket = serverSocket.accept();
				
				// Create new Thread and pass it Runnable RequestHandler
				Thread thread = new Thread(new RequestHandler(socket));
				
				// Key a reference to each thread so they can be joined later if necessary
				synchronized (ob) {
					
					servicingThreads.add(thread);
					//servicingThreads.add(thread);
				}

				
				thread.start();	
			} catch (SocketException e) {
				// Socket exception is triggered by management system to shut down the proxy 
				System.out.println("Server closed");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


	/**
	 * Saves the blocked and cached sites to a file so they can be re loaded at a later time.
	 * Also joins all of the RequestHandler threads currently servicing requests.
	 */
	private void closeServer(){
		System.out.println("\nClosing Server..");
		running = false;
		try{
				// Close all servicing threads
				for(Thread thread : servicingThreads){
					if(thread.isAlive()){
						System.out.print("Waiting on "+  thread.getId()+" to close..");
						thread.join();
						System.out.println(" closed");


					}
									}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			// Close Server Socket
			try{
				System.out.println("Terminating Connection");
				
				serverSocket.close();
				JOptionPane.showMessageDialog(f1, "Server closed Successfully!", "closing status", JOptionPane.PLAIN_MESSAGE);
			} catch (Exception e) {
				System.out.println("Exception closing proxy's server socket");
				e.printStackTrace();
			}
	}


		/**
		 * Looks for File in cache
		 * @param url of requested file 
		 * @return File if file is cached, null otherwise
		 */
		public static File getCachedPage(String url){

			return cache.get(url);
		}


		/**
		 * Adds a new page to the cache
		 * @param urlString URL of webpage to cache 
		 * @param fileToCache File Object pointing to File put in cache
		 */
		 
		 //static int h=0;
		public static void addCachedPage(String urlString, File fileToCache,int c,ArrayList urlkey){
			
			synchronized (ob) {
				
				if(c>lim)
				{
					//System.out.println("URLKEY:"+urlkey.get(0));
					cache.remove(urlkey.get(h));
				    //h++;
				}
				
			
				
				cache.put(urlString, fileToCache);
				//urlkey.add(urlString);
				c++;
								
			}
			
		}

		/**
		 * Check if a URL is blocked by the proxy
		 * @param url URL to check
		 * @return true if URL is blocked, false otherwise
		 */
		public static boolean isBlocked (String url){
			if(blockedSites.get(url) != null){
				return true;
			} else {
				return false;
			}
		}
		
		
		public void actionPerformed(ActionEvent e)
		{
	      	
	      	if(e.getSource()==b1)
	      	{	
				if(rb1.isSelected()){    
					  command="cached";
				}    
				if(rb2.isSelected()){    
					   command="blocked";
				}    
				if(rb3.isSelected()){    
				    command="close";
				}    
			}
			if(e.getSource()==addMore)
	      	{	
				String s3= JOptionPane.showInputDialog("Enter the valid http url to block");
				if(s3!= null && s3.length() >= 1)
					com=s3;
			}
			if(e.getSource()== ok)
	      	{
					f1.remove(p2);
					f1.setContentPane(p1);
					f1.validate();
					f1.repaint();
					l.setText("");
					try{
							FileOutputStream fileOutputStream = new FileOutputStream("cachedSites.txt");
							ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
							objectOutputStream.writeObject(cache);
							objectOutputStream.close();
							fileOutputStream.close();
							//System.out.println("Cached Sites written");
							FileOutputStream fileOutputStream2 = new FileOutputStream("blockedSites.txt");
							ObjectOutputStream objectOutputStream2 = new ObjectOutputStream(fileOutputStream2);
							objectOutputStream2.writeObject(blockedSites);
							objectOutputStream2.close();
							fileOutputStream2.close();
							
					}catch(Exception exp){}
			}
			if(e.getSource()== HTML_CONTENT)
	      	{	
				f1.remove(p1);
				f1.setContentPane(outputPanel);
				f1.validate();
				f1.repaint();
				ta1.setText(s);
			}
		}
	


		/**
		 * Creates a management interface which can dynamically update the proxy configurations
		 * 		blocked : Lists currently blocked sites
		 *  	cached	: Lists currently cached sites
		 *  	close	: Closes the proxy server
		 *  	*		: Adds * to the list of blocked sites
		 */
		 String str="";
		@Override
		
		public void run() {
			
			while(running){

				if(command == "cached" ||command == "blocked" || command == "close" )
				{
				
				if(command.toLowerCase().equals("blocked")){
				
					System.out.println("\nCurrently Blocked Sites");
					for(String key : blockedSites.keySet()){
						System.out.println(key);
						str+="\n"+key;
					}
					
					l.setText("Currently Blocked Sites");
					l.setBounds(70,10,300,20);
					l.setFont(new Font("Baskerville Old Face", Font.BOLD,24));
					
					String[] list = str.split("\n");
					str="";
					String label = "<html>";
					for (int i = 0; i < list.length; i++) {
						System.out.println(list[i]);
						label = label + list[i] + "<br>";
					}
					label = label + "</html>"; 
					
					p2.add(l);
					l2.setText(label);
					l2.setFont(new Font("Courier", Font.PLAIN,16));
					addMore.setVisible(true);
					f1.remove(p1);
					f1.setContentPane(p2);
					f1.validate();
					f1.repaint();
					System.out.println();
					command="";
					
					
				} 

				else if(command.toLowerCase().equals("cached")){
					
					System.out.println("\nCurrently Cached Sites");
					
						for(String key : cache.keySet()){
								//System.out.println(key);
								str+="\n"+key;
						}
						
						System.out.println(str);
					l.setText("Currently cached Sites");
					l.setBounds(70,10,300,20);
					l.setFont(new Font("Baskerville Old Face", Font.BOLD,24));
					
					String[] list = str.split("\n");
					str="";
					String label = "<html>";
					for (int i = 0; i < list.length; i++) {
						System.out.println(list[i]);
						label = label + list[i] + "<br>";
					}
					label = label + "</html>"; 
					addMore.setVisible(false);
					p2.add(l);
					l2.setText(label);
					l2.setFont(new Font("Courier", Font.PLAIN,16));
					f1.remove(p1);
					f1.setContentPane(p2);
					f1.validate();
					f1.repaint();
					
					System.out.println();
					command="";
				}


				else if(command.equals("close")){
					running = false;
					closeServer();
					command = "";
				}
				}
				if(com!=""){
						synchronized(ob) {
							
							blockedSites.put(com, com);
						}
						System.out.println("\n" + com + " blocked successfully \n");
			
				com="";
				}
				
			}
		} 

		public static void main(String[] args) {
		// Create an instance of Proxy and begin listening for connections
		Proxy myProxy = new Proxy(8085);
		myProxy.listen();	
		
	}
	
	

	}
	
	class RequestHandler implements Runnable {

	/**
	 * Socket connected to client passed by Proxy server
	 */
	Socket clientSocket;

	/**
	 * Read data client sends to proxy
	 */
	BufferedReader proxyToClientBr;

	/**
	 * Send data from proxy to client
	 */
	BufferedWriter proxyToClientBw;
	

	/**
	 * Thread that is used to transmit data read from client to server when using HTTPS
	 * Reference to this is required so it can be closed once completed.
	 */
	private Thread httpsClientToServer;


	/**
	 * Creates a ReuqestHandler object capable of servicing HTTP(S) GET requests
	 * @param clientSocket socket connected to the client
	 */
	
	 
	public RequestHandler(Socket clientSocket){
		
		this.clientSocket = clientSocket;
		try{
			this.clientSocket.setSoTimeout(2000);
			proxyToClientBr = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			proxyToClientBw = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
		} 
		catch (IOException e) {
			//e.printStackTrace();
		}
	}


	
	/**
	 * Reads and examines the requestString and calls the appropriate method based 
	 * on the request type. 
	 */
	@Override
	public void run() {
		
		// Get Request from client
		String requestString;
		try{
			requestString = proxyToClientBr.readLine();
		} catch (IOException e) {
			//e.printStackTrace();
			//System.out.println("Error reading request from client");
			return;
		}

		// Parse out URL
		
		System.out.println("Request Received " + requestString);
		// Get the Request type
		String request = requestString.substring(0,requestString.indexOf(' '));

		// remove request type and space
		String urlString = requestString.substring(requestString.indexOf(' ')+1);

		// Remove everything past next space
		urlString = urlString.substring(0, urlString.indexOf(' '));

		// Prepend http:// if necessary to create correct URL
		if(!urlString.substring(0,4).equals("http")){
			String temp = "http://";
			urlString = temp + urlString;
		}


		// Check if site is blocked
		if(Proxy.isBlocked(urlString)){
			System.out.println("Blocked site requested : " + urlString);
			blockedSiteRequested();
			return;
		}


		// Check request type
		if(request.equals("CONNECT")){
				System.out.println("HTTPS Request for : " + urlString + "\n");
			handleHTTPSRequest(urlString);
		} 

		else{
			// Check if we have a cached copy
			File file;
			if((file = Proxy.getCachedPage(urlString)) != null){
				System.out.println("Cached Copy found for : " + urlString + "\n");
				sendCachedPageToClient(file);
			} else {
					System.out.println("HTTP GET for : " + urlString + "\n");
				sendNonCachedToClient(urlString);
			}
		}
	} 


	/**
	 * Sends the specified cached file to the client
	 * @param cachedFile The file to be sent (can be image/text)
	 */
	private void sendCachedPageToClient(File cachedFile){
		// Read from File containing cached web page
		try{
			// If file is an image write data to client using buffered image.
			String fileExtension = cachedFile.getName().substring(cachedFile.getName().lastIndexOf('.'));
			
			// Response that will be sent to the server
			String response;
			if((fileExtension.contains(".png")) || fileExtension.contains(".jpg") ||
					fileExtension.contains(".jpeg") || fileExtension.contains(".gif")){
				// Read in image from storage
				BufferedImage image = ImageIO.read(cachedFile);
				
				if(image == null ){
					System.out.println("Image " + cachedFile.getName() + " was null");
					response = "HTTP/1.0 404 NOT FOUND \n" +
							"Proxy-agent: ProxyServer/1.0\n" +
							"\r\n";
					proxyToClientBw.write(response);
					proxyToClientBw.flush();
				} else {
					response = "HTTP/1.0 200 OK\n" +
							"Proxy-agent: ProxyServer/1.0\n" +
							"\r\n";
					proxyToClientBw.write(response);
					proxyToClientBw.flush();
					ImageIO.write(image, fileExtension.substring(1), clientSocket.getOutputStream());
				}
			} 
			
			// Standard text based file requested
			else {
				BufferedReader cachedFileBufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(cachedFile)));

				response = "HTTP/1.0 200 OK\n" +
						"Proxy-agent: ProxyServer/1.0\n" +
						"\r\n";
				proxyToClientBw.write(response);
				proxyToClientBw.flush();

				String line;
				while((line = cachedFileBufferedReader.readLine()) != null){
					proxyToClientBw.write(line);
					//s=line + "\n";
				}

				proxyToClientBw.flush();
				
				// Close resources
				if(cachedFileBufferedReader != null){
					cachedFileBufferedReader.close();
				}	
			}


			// Close Down Resources
			if(proxyToClientBw != null){
				proxyToClientBw.close();
			}

		} catch (IOException e) {
			//System.out.println("Error Sending Cached file to client");
			//e.printStackTrace();
		}
	}


	/**
	 * Sends the contents of the file specified by the urlString to the client
	 * @param urlString URL ofthe file requested
	 */
	private void sendNonCachedToClient(String urlString){

		try{
			
			// Compute a logical file name as per schema
			// This allows the files on stored on disk to resemble that of the URL it was taken from
			int fileExtensionIndex = urlString.lastIndexOf(".");
			String fileExtension;

			// Get the type of file
			fileExtension = urlString.substring(fileExtensionIndex, urlString.length());

			// Get the initial file name
			String fileName = urlString.substring(0,fileExtensionIndex);


			// Trim off http://www. as no need for it in file name
			fileName = fileName.substring(fileName.indexOf('.')+1);

			// Remove any illegal characters from file name
			fileName = fileName.replace("/", "__");
			fileName = fileName.replace('.','_');
			
			// Trailing / result in index.html of that directory being fetched
			if(fileExtension.contains("/")){
				fileExtension = fileExtension.replace("/", "__");
				fileExtension = fileExtension.replace('.','_');
				fileExtension += ".html";
			}
		
			fileName = fileName + fileExtension;



			// Attempt to create File to cache to
			boolean caching = true;
			File fileToCache = null;
			BufferedWriter fileToCacheBW = null;

			try{
				// Create File to cache 
				fileToCache = new File("cached" + fileName);

				if(!fileToCache.exists()){
					fileToCache.createNewFile();
				}

				// Create Buffered output stream to write to cached copy of file
				fileToCacheBW = new BufferedWriter(new FileWriter(fileToCache));
			}
			catch (IOException e){
				//System.out.println("Couldn't cache: " + fileName);
				//caching = false;
				//e.printStackTrace();
			} catch (NullPointerException e) {
				//System.out.println("NPE opening file");
			}





			// Check if file is an image
			if((fileExtension.contains(".png")) || fileExtension.contains(".jpg") ||
					fileExtension.contains(".jpeg") || fileExtension.contains(".gif")){
				// Create the URL
				URL remoteURL = new URL(urlString);
				BufferedImage image = ImageIO.read(remoteURL);

				if(image != null) {
					// Cache the image to disk
					ImageIO.write(image, fileExtension.substring(1), fileToCache);

					// Send response code to client
					String line = "HTTP/1.0 200 OK\n" +
							"Proxy-agent: ProxyServer/1.0\n" +
							"\r\n";
					proxyToClientBw.write(line);
					proxyToClientBw.flush();

					// Send them the image data
					ImageIO.write(image, fileExtension.substring(1), clientSocket.getOutputStream());

				// No image received from remote server
				} else {
					System.out.println("Sending 404 to client as image wasn't received from server"
							+ fileName);
					String error = "HTTP/1.0 404 NOT FOUND\n" +
							"Proxy-agent: ProxyServer/1.0\n" +
							"\r\n";
					proxyToClientBw.write(error);
					proxyToClientBw.flush();
					return;
				}
			} 

			// File is a text file
			else {
								
				// Create the URL
				URL remoteURL = new URL(urlString);
				// Create a connection to remote server
				HttpURLConnection proxyToServerCon = (HttpURLConnection)remoteURL.openConnection();
				proxyToServerCon.setRequestProperty("Content-Type", 
						"application/x-www-form-urlencoded");
				proxyToServerCon.setRequestProperty("Content-Language", "en-US");  
				proxyToServerCon.setUseCaches(false);
				proxyToServerCon.setDoOutput(true);
			
				// Create Buffered Reader from remote Server
				BufferedReader proxyToServerBR = new BufferedReader(new InputStreamReader(proxyToServerCon.getInputStream()));
				

				// Send success code to client
				String line = "HTTP/1.0 200 OK\n" +
						"Proxy-agent: ProxyServer/1.0\n" +
						"\r\n";
				proxyToClientBw.write(line);
				
				
				// Read from input stream between proxy and remote server
				while((line = proxyToServerBR.readLine()) != null){
					// Send on data to client
					proxyToClientBw.write(line);
					System.out.println(line);
					System.out.println("\n");
					
					/*synchronized(ob)
					{
						 s=line +"\n";
					}*/

					// Write to our cached copy of the file
					if(caching){
						fileToCacheBW.write(line);
					}
				}

				// Ensure all data is sent by this point
				proxyToClientBw.flush();

				// Close Down Resources
				if(proxyToServerBR != null){
					proxyToServerBR.close();
				}
			}
			
			//ArrayList<String> urlkey=new ArrayList<String>();
			if(caching){
				// Ensure data written and add to our cached hash maps
				c++;
				//urlkey.add(urlString);
				fileToCacheBW.flush();                                 
				Proxy.addCachedPage(urlString, fileToCache,c,urlkey);
			}

			// Close down resources
			if(fileToCacheBW != null){
				fileToCacheBW.close();
			}

			if(proxyToClientBw != null){
				proxyToClientBw.close();
			}
		} 

		catch (Exception e){
			//e.printStackTrace();
		}
	}

	
	/**
	 * Handles HTTPS requests between client and remote server
	 * @param urlString desired file to be transmitted over https
	 */
	private void handleHTTPSRequest(String urlString){
		// Extract the URL and port of remote 
		String url = urlString.substring(7);
		String pieces[] = url.split(":");
		url = pieces[0];
		int port  = Integer.valueOf(pieces[1]);

		try{
			// Only first line of HTTPS request has been read at this point (CONNECT *)
			// Read (and throw away) the rest of the initial data on the stream
			for(int i=0;i<5;i++){
				proxyToClientBr.readLine();
			}

			// Get actual IP associated with this URL through DNS
			InetAddress address = InetAddress.getByName(url);
			
			// Open a socket to the remote server 
			Socket proxyToServerSocket = new Socket(address, port);
			proxyToServerSocket.setSoTimeout(5000);

			// Send Connection established to the client
			String line = "HTTP/1.0 200 Connection established\r\n" +
					"Proxy-Agent: ProxyServer/1.0\r\n" +
					"\r\n";
			proxyToClientBw.write(line);
			proxyToClientBw.flush();
			
			
			
			// Client and Remote will both start sending data to proxy at this point
			// Proxy needs to asynchronously read data from each party and send it to the other party


			//Create a Buffered Writer betwen proxy and remote
			BufferedWriter proxyToServerBW = new BufferedWriter(new OutputStreamWriter(proxyToServerSocket.getOutputStream()));

			// Create Buffered Reader from proxy and remote
			BufferedReader proxyToServerBR = new BufferedReader(new InputStreamReader(proxyToServerSocket.getInputStream()));



			// Create a new thread to listen to client and transmit to server
			ClientToServerHttpsTransmit clientToServerHttps = 
					new ClientToServerHttpsTransmit(clientSocket.getInputStream(), proxyToServerSocket.getOutputStream());
			
			httpsClientToServer = new Thread(clientToServerHttps);
			httpsClientToServer.start();
			
			
			// Listen to remote server and relay to client
			try {
				byte[] buffer = new byte[4096];
				int read;
				do {
					read = proxyToServerSocket.getInputStream().read(buffer);
					if (read > 0) {
						clientSocket.getOutputStream().write(buffer, 0, read);
						if (proxyToServerSocket.getInputStream().available() < 1) {
							clientSocket.getOutputStream().flush();
						}
					}
				} while (read >= 0);
			}
			catch (SocketTimeoutException e) {
				
			}
			catch (IOException e) {
				//e.printStackTrace();
			}


			// Close Down Resources
			if(proxyToServerSocket != null){
				proxyToServerSocket.close();
			}

			if(proxyToServerBR != null){
				proxyToServerBR.close();
			}

			if(proxyToServerBW != null){
				proxyToServerBW.close();
			}

			if(proxyToClientBw != null){
				proxyToClientBw.close();
			}
			
			
		} catch (SocketTimeoutException e) {
			String line = "HTTP/1.0 504 Timeout Occured after 10s\n" +
					"User-Agent: ProxyServer/1.0\n" +
					"\r\n";
			try{
				proxyToClientBw.write(line);
				proxyToClientBw.flush();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		} 
		catch (Exception e){
			System.out.println("Error on HTTPS : " + urlString );
			e.printStackTrace();
		}
	}

	


	/**
	 * Listen to data from client and transmits it to server.
	 * This is done on a separate thread as must be done 
	 * asynchronously to reading data from server and transmitting 
	 * that data to the client. 
	 */
	class ClientToServerHttpsTransmit implements Runnable{
		
		InputStream proxyToClientIS;
		OutputStream proxyToServerOS;
		
		/**
		 * Creates Object to Listen to Client and Transmit that data to the server
		 * @param proxyToClientIS Stream that proxy uses to receive data from client
		 * @param proxyToServerOS Stream that proxy uses to transmit data to remote server
		 */
		public ClientToServerHttpsTransmit(InputStream proxyToClientIS, OutputStream proxyToServerOS) {
			this.proxyToClientIS = proxyToClientIS;
			this.proxyToServerOS = proxyToServerOS;
		}

		@Override
		public void run(){
			try {
				// Read byte by byte from client and send directly to server
				byte[] buffer = new byte[4096];
				int read;
				do {
					read = proxyToClientIS.read(buffer);
					if (read > 0) {
						proxyToServerOS.write(buffer, 0, read);
						if (proxyToClientIS.available() < 1) {
							proxyToServerOS.flush();
						}
					}
				} while (read >= 0);
			}
			catch (SocketTimeoutException ste) {
				// TODO: handle exception
			}
			catch (IOException e) {
				//System.out.println("Proxy to client HTTPS read timed out");
				//e.printStackTrace();
			}
		}
	}

	
	/**
	 * This method is called when user requests a page that is blocked by the proxy.
	 * Sends an access forbidden message back to the client
	 */
	/*private void blockedSiteRequested(){
		try {
			BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
			String line = "HTTP/1.0 403 Access Forbidden \n" +
					"User-Agent: ProxyServer/1.0\n" +
					"\r\n";
			bufferedWriter.write(line);
			bufferedWriter.flush();
		} catch (IOException e) {
			System.out.println("Error writing to client when requested a blocked site");
			e.printStackTrace();
		}
	}*/
	
	private void blockedSiteRequested(){
		try {
			BufferedReader cachedFileBufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream("blocked.html")));

				String response = "HTTP/1.0 200 OK\n" +
						"Proxy-agent: ProxyServer/1.0\n" +
						"\r\n";
				proxyToClientBw.write(response);
				proxyToClientBw.flush();

				String line;
				while((line = cachedFileBufferedReader.readLine()) != null){
					proxyToClientBw.write(line);
				}
				proxyToClientBw.flush();
				
				// Close resources
				if(cachedFileBufferedReader != null){
					cachedFileBufferedReader.close();
				}
		} catch (IOException e) {
			System.out.println("Error writing to client when requested a blocked site");
			e.printStackTrace();
		}
	}
	
}






