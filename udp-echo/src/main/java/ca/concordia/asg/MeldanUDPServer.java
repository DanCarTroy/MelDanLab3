package ca.concordia.asg;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.Scanner;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;

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
            
            Packet packetWithAddOfClient = null;
            
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
	                
	                	
	                String payload = new String(packet.getPayload(), UTF_8);
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
	            	dataFromClient  += new String(iter.next().getPayload());
	            }
	            
	            System.out.println();
	            System.out.println("This is the data that we got from the client:");
	            System.out.println(dataFromClient);
	            
	            /**
	             * Write a method/statements to do something with the data that we accepted. Ex: see if it is a get or a post
	             */
	            
	            // WRITE HERE:
	            
	            /**
	             * Write a method/statements to reply to the client 
	             */
	             
	            System.out.println();
                System.out.println("SENDING RESPONSE");
	            
                String payload = "Hello, I am the server. I received all your packets. Thank you.";
                logger.info("Sending: {}", payload);

                // Send the response to the router not the client.
                // The peer address of the packet is the address of the client already.
                // We can use toBuilder to copy properties of the current packet.
                // This demonstrate how to create a new packet from an existing packet.
                Packet resp = packetWithAddOfClient.toBuilder()
                        .setPayload(payload.getBytes())
                        .create();
                logger.info("Packet: {}", resp);
                logger.info("Sending: {}", payload);
                logger.info("Router: {}", router);
                channel.send(resp.toBuffer(), router);
                
                System.out.println();
                logger.info("EchoServer is listening at {}", channel.getLocalAddress());
                
                
	            
	            
	        }
        }
    	
    	
    	
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