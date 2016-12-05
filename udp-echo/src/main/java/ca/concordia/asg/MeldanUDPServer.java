package ca.concordia.asg;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Timer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

// this is my second commit
/**
 * Packet types
 * 0 : SYN
 * 1 : SYN-ACK
 * 2 : ACK
 * 3 : Data
 * 4 : NAK
 */

public class MeldanUDPServer {

    private static final Logger logger = LoggerFactory.getLogger(MeldanUDPServer.class);
    private final String dataDirectory = "G:\\My Documents\\DataDirectory";
    
    private static ArrayList<Packet> receiverWindow = new ArrayList<Packet>();
    private static int totalDataPackets; //The total amount of packets that the server is supposed to receive.
    private static boolean haveAllPackets = false; 
    private static SocketAddress routerAdr;
    private static DatagramChannel currentChannel;// the currentChannel
    private static Timer timer;
    static Packet packetWithAddOfClient;
    
    
    /**
	 * Attribute used as a lock to synchronize
	 */
	private final static Object accountLock = new Object();
	
	static void stopTimer()
	{
		timer.stop();
	}

    private void listenAndServe(int port) throws IOException {

        
    	try (DatagramChannel channel = DatagramChannel.open()) {
            channel.bind(new InetSocketAddress(port));
            logger.info("EchoServer is listening at {}", channel.getLocalAddress());
            ByteBuffer buf = ByteBuffer
                    .allocate(Packet.MAX_LEN)
                    .order(ByteOrder.BIG_ENDIAN);
            
            currentChannel = channel;
            //ArrayList<Packet> receiverWindow = new ArrayList<Packet>();
            //boolean haveAllPackets = false; 
            
            ArrayList<Packet> listOfPackets = new ArrayList<Packet>(); 
            
            //Packet packetWithAddOfClient = null; // It is going to be used to store the address of the client. 
            
            while(true)
            {
            	
            	
	            //acceptConnection(channel, buf);  //Accept connection from the client
	            
	            /*
	    		 * This will check the receiver list every 3 seconds (3000 milliseconds)
	    		 * We are passing a lock so that no other threads can access the list while we are using it.
	    		 */
	    		timer = new Timer(4000, new checkingListActionListener(accountLock));
	    		timer.start();
	    		
	         
                SocketAddress router = null;
                int testCount = 0;
	            // Accept the data from the client
	            for ( ; ; ) {
	            	System.out.println();
	            	
	                buf.clear(); //clears the buffer to accept more packets(to accept raw data)
	                router = channel.receive(buf); //accepts raw data
	                setRouterAdr(router);     //Added by Dan
	                System.out.println("BEFO " + (++testCount));
	                Packet packet = null;
	                synchronized(accountLock)
	                {
	                packet = receivePacket(channel, buf, router);
	                packetWithAddOfClient = new Packet(packet);
	                }
	                System.out.println("AFTE " + (testCount));
	                
	                System.out.println("My payload is, without padding: ");
	                String payload = new String(packet.getPayload(), UTF_8).trim();
	                logger.info("Packet: {}", packet);
	                logger.info("Payload: {}", payload);
	                logger.info("Router: {}", router);
	                
	              // listOfPackets.add(packet);
	             //note on type-casting here: the sequence number cannot be bigger than Integer.MAX_VALUE. 
	                synchronized(accountLock)
	                {
	            		if(isInList((int)packet.getSequenceNumber()))
	            		{
	            			System.out.println("Duplicate Packet - already in receiver window.");
	            		}
	            		else
	            			receiverWindow.add(packet);  	                	
	                }
	                
	                
	                /*
	                 * Before it used to be a SYN packet that breaks this loop.   
	                 *  Now, the timer will set a flag that will break this loop when certain conditions are met. 
	                 */ 
	                synchronized(accountLock)
	                {
		                if( haveAllPackets == true /*packet.getType() == 0*/)
		                {
		                	packetWithAddOfClient = new Packet(packet);
		                	break;
		                }
	                }
	
	            }
	            
	            System.out.println();
	            System.out.println("I RECEIVED ALL THE PACKETS FROM THE CLIENT");
	           
	            
	            /**
	             * This gets all the packets that we received from the client and puts them together 
	             */
	            Collections.sort(receiverWindow);
	            Iterator<Packet> iter = receiverWindow.iterator();
	            String dataFromClient = "";
	            while(iter.hasNext())
	            {
	            	// All the packets put together into a String
	            	dataFromClient  += new String(iter.next().getPayload()).trim();//remove white spsaces
	            }
	            
	            System.out.println();
	            System.out.println("This is the data that we got from the client:");
	            System.out.println(dataFromClient);
	            
	            // Reset all the variables to zero. 
	            timer.stop();
	            receiverWindow = new ArrayList<Packet>();
	            totalDataPackets = 0; //The total amount of packets that the server is supposed to receive.
	            haveAllPackets = false; 
	            
	            
	            /**
	             * Write a method/statements to do something with the data that we accepted. Ex: see if it is a get or a post
	             */
	            /*
	            // WRITE HERE:
	            String [] arr = dataFromClient.split(" ");
	            
	            byte[] mybytearray = null;
	            
	            if(arr[0].equals("get"))
	            {
	            	mybytearray = get(arr, packetWithAddOfClient, router, channel, dataDirectory);
	            }
	            else if(arr[0].equals("post"))
	            {
	            	mybytearray = post(arr,dataFromClient, packetWithAddOfClient, router, channel, dataDirectory);
	            }
		          //Write a method/statements to reply to the client 
	    	    	
	  	    	  InetSocketAddress clientAddress = new InetSocketAddress(packetWithAddOfClient.getPeerAddress(), packetWithAddOfClient.getPeerPort() ) ;
	  	    	  
	  	    	  //transforming  mybytearray (which is the data that we got to the file) to an array of packets
	  	    	  Packet[] packets = dataToPackets(mybytearray, clientAddress); 
	  	    	  
		             
		             System.out.println();
	                System.out.println("SENDING RESPONSE");
	                
	                sendingResponseToClient(packets, channel, router);
                
                listOfPackets.clear(); // Clears the list of packets received so that in the next iteration
                                       // we only store the new packets received for that iteration 
	            					   // instead of appending them to old packets from a previous iteration
                System.out.println(); */
                logger.info("EchoServer is listening at {}", channel.getLocalAddress());
                
                
	            
	            
	        }
        }
    	
    	
    	
   }
   
   /*
    * Returns an ordered list of current sequence numbers in the receiver window. 
    */
   public static ArrayList<Integer> currentSequenceNumbers()
   {
	   ArrayList<Integer> tmp = new ArrayList<Integer>();
	   
	   for(int i = 0; i < tmp.size(); i++)
	   {
		   tmp.add((int)receiverWindow.get(i).getSequenceNumber());
	   }
	   
	   Collections.sort(tmp);
	   return tmp;
   }
    
   /*
    * Returns a deep copy of the packet with a certain sequence number if it is in the list,
    * null otherwise.  
    */
   static Packet getPacketWithSequenceNum(int seqNum)
   {
	   Packet p = null;
	   
	   try
   		{
	    	for(int i =0; i< receiverWindow.size(); i++)
	    	{
	    		if((int)receiverWindow.get(i).getSequenceNumber() == seqNum)
	    		{
	    			p = new Packet(receiverWindow.get(i));
	    		}
	    	}
   		}
   		catch(IndexOutOfBoundsException e)
   		{
   			return null;
   		}
   	
		return p;
	   
   }
    
   /**
    * Sets the total amount of packets that the server is supposed to receive
    * @param n
    */
    static void setTotalDataPackets(int n)
   {
	   totalDataPackets = n;
   }
    
   static int getReceiverWindowSize()
   {
	   return receiverWindow.size();
   }
   
   static int getTotalDataPackets()
   {
	   return totalDataPackets;
   }
   
   static void setHaveAllPackets(boolean bool)
   {
	   haveAllPackets = bool;
   }
   
   static void setRouterAdr(SocketAddress router)
   {
	   routerAdr = router;
   }
   
   static SocketAddress getRouterAdr()
   {
	   return routerAdr;
   }
   
   static DatagramChannel getCurrentChannel()
   {
	   return currentChannel;
   }
   
   static boolean getHaveAllPackets()
   {
	   return haveAllPackets;
   }
   
    private byte[] post(String[] arr, String dataFromClient, Packet packetWithAddOfClient,
			SocketAddress router, DatagramChannel channel, String dataDirectory) throws UnsupportedEncodingException, IOException {
    	String fileContent = ""; 
    	System.out.println("Message from Client: " + dataFromClient);
    	
  		   //retrieve file name
  		String fileName= arr[1].substring(arr[1].lastIndexOf('/')+1);
  		Pattern p = Pattern.compile("\"([^\"]*)\"");
  		Matcher m = p.matcher(dataFromClient);
  		while (m.find()) {
  			//Get content in quotations and assign it to String message
  		  fileContent = m.group(1);
  		}
  		System.out.println("file name is: "+ fileName);
  		

  		   PrintWriter printWriter = new PrintWriter (dataDirectory+"\\"+fileName);
		   printWriter.println (fileContent);
  		   printWriter.close (); 
  		   System.out.println("file content is: "+ fileContent);
  		   
  		   return ("success!").getBytes();
	}

	    
    public static Date addDays(Date date, int days)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, days); //minus number would decrement the days
        return cal.getTime();
    }
    
    public static String listFilesInDirectory(final File dir) {
    	String str = "";
        for (final File fileEntry : dir.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesInDirectory(fileEntry);
            } else {
                str += fileEntry.getName() +" <BR>\n";
            }
        }
        
        return str;
    }
    
	private byte[] get(String[] arr, Packet packetWithAddOfClient, SocketAddress router, DatagramChannel channel, String dataDirectory) throws IOException {
		// TODO Auto-generated method stub
		String toReturn = "";
		byte[] mybytearray = null;
		// Responding to client according to what we have received
        Date today = new Date();
        Date expires = addDays(today, 2); // constructor deprecated, change with something else later
        final File dir = new File(dataDirectory);
		
        System.out.println("Arr 1 is: " + arr[1]);
		if(arr[1].equals("localhost/"))
    	{
    		toReturn +=("HTTP/1.0 200 OK\r\n");
            toReturn +=("Date: "+(today)+"\r\n");
            toReturn +=("Server: Meldan Server/0.7.6\r\n");
            toReturn +=("Content-Type: text/html\r\n");
            toReturn +=("Content-Length: 150\r\n");
            toReturn +=("Expires: "+(expires)+"\r\n");
            toReturn +=("Last-modified: "+(today)+"\r\n");
            toReturn +=("\r\n");
            toReturn +=("<TITLE>Lab 2</TITLE>");
            toReturn +=("<P>Listing files in the data directory:</P>");
            toReturn +=("<P>"+listFilesInDirectory(dir)+"</P>");
            mybytearray = toReturn.getBytes();
    		
    	}
    	else if(arr[1].contains("/"))
    	{
    		String fileName = arr[1].substring(arr[1].indexOf('/')+1);
    		System.out.println("my file name is "+fileName);
    		 final File myFile = new File(dir.getPath() + "\\" + fileName);
    		 System.out.println(myFile);
    	      mybytearray = new byte[(int) myFile.length()];
    	      try{
    	    	  BufferedInputStream bis = new BufferedInputStream(new FileInputStream(myFile));
    	    	  bis.read(mybytearray, 0, mybytearray.length);//reading file and saving it to mybytearray
    	    	
    	      }
    	      catch(FileNotFoundException e)
    	      {
    	    	  toReturn +=("HTTP/1.0 200 OK\r\n");
                  toReturn +=("Date: "+(today)+"\r\n");
                  toReturn +=("Server: Meldan Server/0.7.6\r\n");
                  toReturn +=("Content-Type: text/html\r\n");
                  toReturn +=("Content-Length: 150\r\n");
                  toReturn +=("Expires: "+(expires)+"\r\n");
                  toReturn +=("Last-modified: "+(today)+"\r\n");
                  toReturn +=("\r\n");
                  toReturn +=("<TITLE>HTTP ERROR 404</TITLE>");
                  toReturn +=("<P>HTTP ERROR 404</P>");
                  
                  mybytearray = toReturn.getBytes();
                  System.out.println("Client asked for a file that does not exist. HTTP ERROR 404");
                  return mybytearray;
                  
    	      }
    	      
    	      
    	      
    	     
    	      
    	}
		
		return mybytearray;
		
	}

	private void sendingResponseToClient(Packet[] pArray, DatagramChannel channel, SocketAddress routerAddr ) throws IOException {
		
		boolean cont = true;
    	int counter = 0;
    	
		//Sending data packets to client (Type: 3) 
		while(cont)
        {
			System.out.println();
     		System.out.println("Run number " + (counter + 1));
     		
            channel.send(pArray[counter].toBuffer(), routerAddr);

            logger.info("Sending \"{}\" to client at {}", new String(pArray[counter].getPayload()), routerAddr);

        
            counter++;
            if(counter == pArray.length)
            	cont = false;   
    	
        }
		
		for(int i = 0; i < 1000000000; i++)
		{
			
		}
		
		System.out.println();
		System.out.println("I SENT ALL MY RESPONSE PACKETS TO THE CLIENT");
		
		//Send SYN to let the client know I have sent all my packets
		Packet p = new Packet.Builder()
                .setType(0)
                .setSequenceNumber(0)
                .setPortNumber(pArray[0].getPeerPort())
                .setPeerAddress(pArray[0].getPeerAddress())
                .setPayload("CLOSING_SYN".getBytes())
                .create();
    
        channel.send(p.toBuffer(), routerAddr);

        logger.info("Sending \"{}\" to router at {}", new String(p.getPayload(), UTF_8), routerAddr);
		
		
		
	}

	/**
     * Waits for a SYN sent by a client in order to establish a connection (3-way handshake).
     * @throws IOException 
     */
    public static void acceptConnection(DatagramChannel channel, ByteBuffer buf) throws IOException
    {
    	/**
         * Expecting a SYN packet from a client
         */
    	buf.clear(); //clears the buffer to accept more packets(to accept raw data)
        SocketAddress router = channel.receive(buf); //accepts raw data
        Packet packet = receivePacket(channel, buf, router); // Receive a packet
        
        // The server is expecting a SYN (for the 3-way handshake)
        while(packet.getType() != 0)
        {
        	System.out.println("Im here in expecting SYN loop");
        }
        
        System.out.println("GOT A SYN PACKET");
        
        String payload = new String(packet.getPayload(), UTF_8);
        logger.info("Packet: {}", packet);
        logger.info("Payload: {}", payload);
        logger.info("Router: {}", router);
        
        // Gets the SYN sequence number, increase it by one and stores it at the beginning of the payload.
        long synAckSeq = packet.getSequenceNumber()+1;
        payload = String.valueOf(synAckSeq);  
        logger.info("Sending: {}", payload);

       
        /**
         * Creating a SYN-ACK packet. Setting its sequence number to random value.
         */
        Random randGenerator = new Random(); 
    	int pSeq = randGenerator.nextInt(257);
    	
        Packet resp = packet.toBuilder()
        		.setType(1)  // Type 1 is a SYN-ACK
        		.setSequenceNumber(pSeq)
                .setPayload(payload.getBytes()) //Storing the SYN sequence number at the beginning of the payload.
                .create();
        
        // Sending the SYN-ACK to the client. 
        channel.send(resp.toBuffer(), router);
        
        System.out.println("JUST SENT AN SYN-ACK PACKET");
        System.out.println();
        
        /**
         * Expecting a ACK packet from the client
         */
        Boolean ack = false;
        Packet ackPacket = null;
        long ack_aknowledgeNum = 0;
        
        while(true)
        {
	        buf.clear(); //clears the buffer to accept more packets(to accept raw data)
	        router = channel.receive(buf); //accepts raw data
	        ackPacket = receivePacket(channel, buf, router); //Receiving supposed ACK packet from the server
	           
	       //Getting the acknowledgment number from the supposed ACK packet which is at the beginning of the payload   
	        String akwPayload = new String(ackPacket.getPayload(), StandardCharsets.UTF_8);
	        ack_aknowledgeNum = Long.parseLong(akwPayload); // If I get a NumberFormatException is because I am getting a packet that it is not a ACK
	                                                        // We have to fix this later      
	        
	        
	        // The server is expecting a valid ACK from the client to finish establishing a connection
	        if( ackPacket.getType() == 2 && (ackPacket.getSequenceNumber() == synAckSeq +1) && (ack_aknowledgeNum == pSeq+1) )
	        {
	        	System.out.println("GOT AN ACK PACKET. Connection established.");
	        	break;
	        }
	        
        }
        
        
    }
    
    public static Packet receivePacket(DatagramChannel channel, ByteBuffer buf, SocketAddress router) throws IOException
    {
    	//buf.clear(); //clears the buffer to accept more packets(to accept raw data)
        //router = channel.receive(buf); //accepts raw data

        // Parse a packet from the received raw data.
        buf.flip();
        Packet packet = Packet.fromBuffer(buf);
        buf.flip();
        
        System.out.println("Received a packet");
        
        return packet;
    }
    
    /**
     * This method transforms a byte[] into an array of Packets. Useful before sending it to the server through our UDP interface. 
     * @return Array of packets representing the string
     */
    public static Packet[] dataToPackets(byte[] data, InetSocketAddress clientAddress)
    {
    	
    	int a = data.length; // The length of the string
    	int b = Packet.MAX_LEN - Packet.MIN_LEN; // The maximum size of the packet (payload of the packet without counting the 11 extra bytes)
    	
    	int numOfPackets = (a / b) + ((a % b == 0) ? 0 : 1);
    	
    	//System.out.print("Num of packets: "+numOfPackets );
    	
    	Packet[] arrOfPackets = new Packet[numOfPackets];
    
    	byte[] tmpPayload = new byte[b];
    	int firstIndex, lastIndex = 0;
    	
    	for(int i = 0; i < numOfPackets; i++)
    	{
    		lastIndex = (b*(i+1)) - 1;
    		firstIndex = lastIndex - (b-1);
    		
    		if(i == numOfPackets-1)
    		{
    			if(data.length < (lastIndex + 1))//4052)
    			{
    				lastIndex = data.length - 1; //4051 - 1 = 4050
    				int initialFirstIndex = firstIndex;
    				
    				for(int j = 0; j < ((lastIndex - initialFirstIndex)+1); j++)
    	        	{
    	        		tmpPayload[j] = data[firstIndex];
    	        		firstIndex++;
    	        	}
    			}
    		}
    		else
    		{
	    		for(int j = 0; j < b; j++)
	        	{
	        		tmpPayload[j] = data[firstIndex];
	        		firstIndex++;
	        	}
    		}
    		
    		Packet packet = new Packet.Builder()
                    .setType(3) // Type 3 is an DATA packet
                    .setSequenceNumber(i+1)
                    .setPortNumber(clientAddress.getPort())
                    .setPeerAddress(clientAddress.getAddress())
                    .setPayload(tmpPayload)
                    .create();
    		
    		arrOfPackets[i] = new Packet(packet);  // has to be a deep copy or else things won't work
    		
    		tmpPayload = new byte[b];
    	}
    	
    	
    	
    	return arrOfPackets;
    }
    

    public static void main(String[] args) throws IOException {
        OptionParser parser = new OptionParser();
        parser.acceptsAll(asList("port", "p"), "Listening port")
                .withOptionalArg()
                .defaultsTo("8007");

        OptionSet opts = parser.parse(args);
        int port = Integer.parseInt((String) opts.valueOf("port"));
        MeldanUDPServer server = new MeldanUDPServer();
        server.listenAndServe(port);
    }
    
    public static boolean isInList(int sequenceNum)
    {
    	try
    	{
	    	for(int i =0; i< receiverWindow.size(); i++)
	    	{
	    		if((int)receiverWindow.get(i).getSequenceNumber() == sequenceNum)
	    		{
	    			return true;
	    		}
	    	}
    	}
    	catch(IndexOutOfBoundsException e)
    	{
    		return false;
    	}
    	
		return false;
    	
    }
    
    
}



/**
 * Waits for an event in order to check the list (receiver window).
 * This class implements ActionListener. Waits for an event to occur in order to run.
 * In this case it is used with javax.swing.Timer (Timer) class in order to run every 
 * certain period of time.
 */
final class checkingListActionListener implements ActionListener {
	/*
	 * Used for synchronization purposes 
	 */
	private final Object lock;
	
	private static boolean gotPacketZero = false;
	
	 public checkingListActionListener(Object lock)
	 {
		 super();  // Calls the parent(ActionListener) default constructor explicitly. 
		 this.lock = lock; 
	 }
	 
	  public void actionPerformed(ActionEvent e) {
		
		/*
		 * This has to be atomic. That's why we use the lock so that no other threads
		 * can do operations on the list while this method is checking the list. 
		 */
		 
		synchronized(lock)
		{
		    //System.out.println("3 seconds have passed. Checking the receiver window...");
		    
		    // Checking if we have received packet zero
		    //if(gotPacketZero == false)
		    //{
			    if(MeldanUDPServer.isInList(0))
			    {
			    	Packet packetToAnalyze = MeldanUDPServer.getPacketWithSequenceNum(0);
			    	
			    	String sval = new String(packetToAnalyze.getPayload());
			    	int val = Integer.parseInt(sval);
			    	MeldanUDPServer.setTotalDataPackets(val);
			    	
			    	/**
			    	 * IMP PART
			    	 */
			    	if(MeldanUDPServer.getTotalDataPackets() != MeldanUDPServer.getReceiverWindowSize())
			    	{
			    	
				    	String superNakStr = "";
					    Packet pThatContainsAddClient = null;
					    for(int i = 0; i < MeldanUDPServer.getTotalDataPackets(); i++)
					    {
					    	if(!MeldanUDPServer.isInList(i))
					    	{
					    		superNakStr +=  i + " ";
					    	}
					    	else
					    		pThatContainsAddClient = MeldanUDPServer.getPacketWithSequenceNum(i);
					    }
					    
					 // Packet with information about the packets that were not received //SUPER NAK
				    	Packet superNak = new Packet.Builder()
				                .setType(4) // Type 4 is a NAK packet
				                .setSequenceNumber(99999)
				                .setPortNumber(pThatContainsAddClient.getPeerPort())
				                .setPeerAddress(pThatContainsAddClient.getPeerAddress())
				                .setPayload(superNakStr.getBytes())
				                .create();
				    	
				    	
				    	//Sending Super NAK to the client 
				    	try {
							MeldanUDPServer.getCurrentChannel().send(superNak.toBuffer(), MeldanUDPServer.getRouterAdr());
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						System.out.println();
			    	}
			    	else
			    	{
			    		System.out.println("I HAVE ALL PACKETS!!!!!!!!!");
			    		MeldanUDPServer.setHaveAllPackets(true);
			    		
			    		// Packet with information about the packets that were not received //SUPER NAK
				    	Packet superNak = new Packet.Builder()
				                .setType(4) // Type 4 is a NAK packet
				                .setSequenceNumber(99999)
				                .setPortNumber(MeldanUDPServer.getPacketWithSequenceNum(0).getPeerPort())
				                .setPeerAddress(MeldanUDPServer.getPacketWithSequenceNum(0).getPeerAddress())
				                .setPayload("-56".getBytes())
				                .create();
				    	
				    	
				    	//Sending Super NAK to the client 
				    	try {
							MeldanUDPServer.getCurrentChannel().send(superNak.toBuffer(), MeldanUDPServer.getRouterAdr());
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						System.out.println();
			    		
			    		MeldanUDPServer.stopTimer();
			    	}
			    	
			    	/***
			    	 * End of |IMP
			    	 */
			    	// If we got all the packets that we are supposed to get
			    	//sets a flag to true so that the server can stop accepting packets
			    	/*
			    	if(/*MeldanUDPServer.getTotalDataPackets()val == MeldanUDPServer.getReceiverWindowSize())
			    	{
			    		System.out.println("I HAVE ALL PACKETS!!!!!!!!!");
			    		MeldanUDPServer.setHaveAllPackets(true);
			    		MeldanUDPServer.stopTimer();
			    		//return;
			    	}
			    	*/
			    	gotPacketZero = true;
			    	
			    }
			    else
			    {
			    	Packet pThatContainsAddClient = null;
			    	int check = 0;
				    for(int i = 0; i < MeldanUDPServer.getTotalDataPackets(); i++)
				    {
				    	System.out.println("isInList: " + MeldanUDPServer.isInList(i) );
				    	if(!MeldanUDPServer.isInList(i))
				    	{
				    		
				    	}
				    	else
				    	{
				    		check++;
				    		pThatContainsAddClient = MeldanUDPServer.getPacketWithSequenceNum(i);
				    	}
				    }
				    //System.out.println("CHECK IS: "+check);
				    //System.out.println("Testing ABC: "+pThatContainsAddClient);
			    	
				    try{
			    	// Packet with information about the packets that were not received //SUPER NAK
			    	Packet superNak = new Packet.Builder()
			                .setType(4) // Type 4 is a NAK packet
			                .setSequenceNumber(99999)
			                .setPortNumber(MeldanUDPServer.packetWithAddOfClient.getPeerPort())
			                .setPeerAddress(MeldanUDPServer.packetWithAddOfClient.getPeerAddress())
			                .setPayload("0".getBytes())
			                .create();
			    	
			    	//Sending Super NAK to the client 
			    	MeldanUDPServer.getCurrentChannel().send(superNak.toBuffer(), MeldanUDPServer.getRouterAdr());
				    }
				    catch(NullPointerException pt)
				    {
				    	return;
				    }
				    catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
			    	
			    	
					System.out.println();
			    }
		    //}
		    // Find the sequence numbers that we are missing and send a SUPER NAK to the client
		    ArrayList<Integer> seqNumsThatWeHave = MeldanUDPServer.currentSequenceNumbers();
		    // We can implement this later to increase performance. 
		    /*
		    ArrayList<Integer> missingSeqNums = new ArrayList<Integer>(); 
		    for(int i = 0; i < seqNumsThatWeHave.size(); i++)
		    {
		    	for(int j = i; j < seqNumsThatWeHave.get(i); j++)
		    	{
		    		missingSeqNums.add(j);
		    	}
		    }
		    */
		    //if(true/*MeldanUDPServer.getHaveAllPackets() == false*/) //Redundant statement -taken care of above
		    /*{
			    String superNakStr = "GOT EM ALL";
			    Packet pThatContainsAddClient = null;
			    for(int i = 0; i < MeldanUDPServer.getReceiverWindowSize(); i++)
			    {
			    	if(!MeldanUDPServer.isInList(i))
			    	{
			    		superNakStr +=  i + " ";
			    	}
			    	else
			    		pThatContainsAddClient = MeldanUDPServer.getPacketWithSequenceNum(i);
			    }
			    
			 // Packet with information about the packets that were not received //SUPER NAK
		    	Packet superNak = new Packet.Builder()
		                .setType(4) // Type 4 is a NAK packet
		                .setSequenceNumber(99999)
		                .setPortNumber(pThatContainsAddClient.getPeerPort())
		                .setPeerAddress(pThatContainsAddClient.getPeerAddress())
		                .setPayload(superNakStr.getBytes())
		                .create();
		    	
		    	
		    	//Sending Super NAK to the client 
		    	try {
					MeldanUDPServer.getCurrentChannel().send(superNak.toBuffer(), MeldanUDPServer.getRouterAdr());
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				System.out.println();
				*/
		    }
		}

	  }
	  
	  
