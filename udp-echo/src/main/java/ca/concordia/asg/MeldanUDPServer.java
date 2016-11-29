package ca.concordia.asg;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;













import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;

// this is my second commit
/**
 * Packet types
 * 0 : SYN
 * 1 : SYN-ACK
 * 2 : ACK
 * 3 : Data
 */

public class MeldanUDPServer {

    private static final Logger logger = LoggerFactory.getLogger(MeldanUDPServer.class);

    private void listenAndServe(int port) throws IOException {

        
    	try (DatagramChannel channel = DatagramChannel.open()) {
            channel.bind(new InetSocketAddress(port));
            logger.info("EchoServer is listening at {}", channel.getLocalAddress());
            ByteBuffer buf = ByteBuffer
                    .allocate(Packet.MAX_LEN)
                    .order(ByteOrder.BIG_ENDIAN);
            
            
            ArrayList<Packet> listOfPackets = new ArrayList<Packet>(); 
            
            Packet packetWithAddOfClient = null; // It is going to be used to store the address of the client. 
            
            while(true)
            {
            	
            	
	            acceptConnection(channel, buf);  //Accept connection from the client
	         
                SocketAddress router = null;
	
	            // Accept the data from the client
	            for ( ; ; ) {
	            	System.out.println();
	            	
	                buf.clear(); //clears the buffer to accept more packets(to accept raw data)
	                router = channel.receive(buf); //accepts raw data
	                Packet packet = receivePacket(channel, buf, router);
	                
	                System.out.println("My payload is, without padding: ");
	                String payload = new String(packet.getPayload(), UTF_8).trim();
	                logger.info("Packet: {}", packet);
	                logger.info("Payload: {}", payload);
	                logger.info("Router: {}", router);
	                
	                /*
	                 * If I get a SYN packet when I am expecting a DATA packet, it means  
	                 * I have received all packets. Get out of the for loop and wait for another client request. 
	                 */ 
	                if(packet.getType() == 0)
	                {
	                	packetWithAddOfClient = new Packet(packet);
	                	break;
	                }
	                
	               listOfPackets.add(packet);
	                
	
	            }
	            
	            System.out.println();
	            System.out.println("I RECEIVED ALL THE PACKETS FROM THE CLIENT");
	           
	            
	            /**
	             * This gets all the packets that we received from the client and puts them together 
	             */
	            
	            Iterator<Packet> iter = listOfPackets.iterator();
	            String dataFromClient = "";
	            while(iter.hasNext())
	            {
	            	// All the packets put together into a String
	            	dataFromClient  += new String(iter.next().getPayload()).trim();//remove white spsaces
	            }
	            
	            System.out.println();
	            System.out.println("This is the data that we got from the client:");
	            System.out.println(dataFromClient);
	            
	            /**
	             * Write a method/statements to do something with the data that we accepted. Ex: see if it is a get or a post
	             */
	            
	            // WRITE HERE:
	            String [] arr = dataFromClient.split(" ");
	            
	            byte[] mybytearray = null;
	            
	            if(arr[0].equals("get"))
	            {
	            	mybytearray = get(arr, packetWithAddOfClient, router, channel);
	            }
	            else if(arr[0].equals("post"))
	            {
	            	//post(arr,dataFromClient, packetWithAddOfClient, router, channel);
	            }
		          //Write a method/statements to reply to the client 
	    	    	
	  	    	  InetSocketAddress clientAddress = new InetSocketAddress(packetWithAddOfClient.getPeerAddress(), packetWithAddOfClient.getPeerPort() ) ;
	  	    	  
	  	    	  //transforming  mybytearray (which is the data that we got to the file) to an array of packets
	  	    	  Packet[] packets = dataToPackets(mybytearray, clientAddress); 
	  	    	  
		             
		             System.out.println();
	                System.out.println("SENDING RESPONSE");
	                
	                sendingResponseToClient(packets, channel, router);
                
                System.out.println();
                logger.info("EchoServer is listening at {}", channel.getLocalAddress());
                
                
	            
	            
	        }
        }
    	
    	
    	
   }
   
    private void post(String[] arr, String dataFromClient, Packet packetWithAddOfClient,
			SocketAddress router, DatagramChannel channel) throws FileNotFoundException {
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
  		  
  		   PrintWriter printWriter = new PrintWriter (fileName);
		   printWriter.println (fileContent);
  		   printWriter.close (); 
  		   System.out.println("file content is :"+ fileContent);
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
    
	private byte[] get(String[] arr, Packet packetWithAddOfClient, SocketAddress router, DatagramChannel channel) throws IOException {
		// TODO Auto-generated method stub
		String toReturn = "";
		byte[] mybytearray = null;
		// Responding to client according to what we have received
        Date today = new Date();
        Date expires = addDays(today, 2); // constructor deprecated, change with something else later
        final File dir = new File("C:\\Users\\Mel\\Documents\\fall2016");
		
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
}