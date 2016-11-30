package ca.concordia.asg;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.charset.StandardCharsets.UTF_8;

// this is my second commit *MEL WAS HERE. another update THis is a change!
/**
 * Packet types
 * 0 : SYN
 * 1 : SYN-ACK
 * 2 : ACK
 * 3 : Data
 */

public class MeldanUDPClient {

    private static final Logger logger = LoggerFactory.getLogger(MeldanUDPClient.class);

    private static void runClient(SocketAddress routerAddr, InetSocketAddress serverAddr) throws IOException {
        
    	   	
    	boolean cont = true;
    	int counter = 0;
        Scanner kb = new Scanner(System.in);
        
 
    	try(DatagramChannel channel = DatagramChannel.open()){
    		
    		
    		while(true)
    		{
    			
    			establishConnection(channel, routerAddr, serverAddr);
    			
    			System.out.println();
    			System.out.println("THE SERVER RECEIVED MY ACK. SUCCESSFULLY ESTABLISHED CONNECTION.");
    			System.out.println("We can start sending data packets to the server.\n");
    		
    			// GET THE GET OR POST REQUEST IN STRING FORMAT RIGHT HERE
    			// AND TRANSFORM IT INTO PACKETS USING stringToPackets method
    			
    			//"post localhost/worksActuallyRight.txt \"Modifying worksActuallyRight again !\"";
    			//"get localhost/POLI_ESSAY.txt";
    			
    			String requestToSend = "post localhost/worksActuallyRight2.txt \"Creating worksActuallyRight2 ^_^ !\""; //takeUserInput();
    		//	String requestToSend = generateMySampleStr();
    			
    			//takeUserInput(requestToSend, keyboard);
    			
    			
    			
    			System.out.println("Byte length of my string: "+ requestToSend.getBytes().length);
    			
    			// Transforms the String requestToSend into a array of packets. 
    		    Packet[] pArray = dataToPackets(requestToSend.getBytes(), serverAddr);
    		        
    		    System.out.println("I have " + pArray.length + " packets.");
    		    
    			
    			//Sending data packets to server (Type: 3) 
    			while(cont)
		        {
    				System.out.println();
		     		System.out.println("Run number " + (counter + 1));
		     		
		            channel.send(pArray[counter].toBuffer(), routerAddr);
		
		            logger.info("Sending \"{}\" to router at {}", new String(pArray[counter].getPayload()).trim(), routerAddr);
		
		            
		            //receivePacket(channel); 
		        
		            counter++;
		            if(counter == pArray.length)
		            	cont = false;   
		    	
		        }
				
    			for(int i = 0; i < 1000000000; i++)
    			{
    				
    			}
    			
    			System.out.println();
				System.out.println("I SENT ALL MY PACKETS TO THE SERVER");
				
				//Send SYN to let the server know I have sent all my packets
				Packet p = new Packet.Builder()
	                    .setType(0)
	                    .setSequenceNumber(0)
	                    .setPortNumber(serverAddr.getPort())
	                    .setPeerAddress(serverAddr.getAddress())
	                    .setPayload("CLOSING_SYN".getBytes())
	                    .create();
	        
	            channel.send(p.toBuffer(), routerAddr);
	
	            logger.info("Sending \"{}\" to router at {}", new String(p.getPayload(), UTF_8), routerAddr);
				
				// If I am here it means I was able to send my GET or POST request( in the form of packets) to server.
				
				/*
				 * Write a method that receives response (all the packets) from the server
				 */
	            // Note: Here, client is just catching one packet from the server. If the server response is multiple packets 
	            // only the first packet will be caught and the rest will be dropped. To fix this we will have to write a loop. 
	             
	            ArrayList<Packet> listOfPackets = new ArrayList<Packet>(); 
                Packet packetWithAddOfServer = null; // It is going to be used to store the address of the server in case we want to reply. 
	           
                for ( ; ; ) {
	             System.out.println();
	             Packet packetFromServer = receivePacket(channel);
	             //String strPayload = new String(packetFromServer.getPayload(), StandardCharsets.UTF_8).trim();
	             //System.out.println("Content of the packet: " + strPayload);
	            
	             
	             if(packetFromServer.getType() == 0)
	                {
	                	packetWithAddOfServer = new Packet(packetFromServer);
	                	break;
	                }
	                
		            listOfPackets.add(packetFromServer);
	            }
                
                
                System.out.println();
	            System.out.println("I RECEIVED ALL THE PACKETS FROM THE SERVER");
	           
	            
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
	            System.out.println("Response from server:");
	            System.out.println(dataFromClient);
	            
	            
				
				break;
    		}
        }
    	
    	 
        
    }
    
    /**
     * This method does the 3-way handshake to establish a connection with the server.
     * @param routerAddr SocketAddress object that contains the address and the port of the router.
     * @param serverAddr InetSocketAddress object that contains the address and the port of the server.
     * @return true if it was able to establish a connection with the server
     * @throws IOException 
     */
    private static boolean establishConnection(DatagramChannel channel, SocketAddress routerAddr, InetSocketAddress serverAddr) throws IOException
    {
    	logger.info("Establishing connection with server.");
    	
    	Random randGenerator = new Random(); 
    	int pSeq = randGenerator.nextInt(257);
    	
    	/**
    	 * Creating a SYN packet
    	 */
    	
    	Packet synP = new Packet.Builder()
                .setType(0)
                .setSequenceNumber(pSeq)
                .setPortNumber(serverAddr.getPort())
                .setPeerAddress(serverAddr.getAddress())
                .setPayload("SYN".getBytes())
                .create();
    	
    	// Sending SYN packet to the server
    	channel.send(synP.toBuffer(), routerAddr);
        logger.info("Sending \"{}\" to router at {}", new String(synP.getPayload()).trim(), routerAddr);

        
        /**
         * Expecting a SYN-ACK packet from the server
         */
        Boolean synAck = false;
        Packet synAckPacket = null;
        long synAck_aknowledgeNum = 0;
        
        do
        {
        	synAckPacket = receivePacket(channel);
        	// Getting the acknowledgment number   
            String akwPayload = new String(synAckPacket.getPayload(), StandardCharsets.UTF_8).trim(); // If I get a NullPointerException is because I have not received a SYN-ACK (or other packet from the server) after a certain amount of time.
            synAck_aknowledgeNum = Long.parseLong(akwPayload); // If I get a NumberFormatException is because I am getting a packet that it is not a SYN-ACK
            													// We have to fix this later (This error happens when I run the client for a second time using the same instance of the server)
        	if( synAckPacket.getType() == 1 && ( synAck_aknowledgeNum == synP.getSequenceNumber()+1 ) )
        	{
        		synAck = true;
        	}
        	
        	
        }while(synAck == false);
        
        System.out.println("GOT A SYN-ACK PACKET");
        
        /**
    	 * Creating an ACK packet
    	 */
        // Note: If I don't send an ACK server will crash with a NumberFormatException -- We have to fix this
        
        String ack_aknowledgeNum = String.valueOf(synAckPacket.getSequenceNumber()+1);
    	Packet ackPacket = new Packet.Builder()
                .setType(2) // Type 2 is an ACK packet
                .setSequenceNumber(synAck_aknowledgeNum + 1)
                .setPortNumber(serverAddr.getPort())
                .setPeerAddress(serverAddr.getAddress())
                .setPayload(ack_aknowledgeNum.getBytes())
                .create();
    	
    	// Sending ACK packet to the server
    	channel.send(ackPacket.toBuffer(), routerAddr);
    	
    	System.out.println();
    	System.out.println("SENDING AN ACK TO THE SERVER");
        logger.info("Sending \"{}\" to router at {}", "ACK", routerAddr);

    	
    	
    	
		return true;
    	
    }
    
    /**
     * Receives a packet from the underlying channel
     * @param channel 
     * @return Packet that was sent through the Datagram channel, null if there is no packet after timeout 
     * @throws IOException
     */
    private static Packet receivePacket(DatagramChannel channel) throws IOException
    {
    	// Try to receive a packet within timeout.
        channel.configureBlocking(false);
        Selector selector = Selector.open();
        channel.register(selector, OP_READ);
        logger.info("Waiting for the response");
        selector.select(5000);

        Set<SelectionKey> keys = selector.selectedKeys();
        if(keys.isEmpty()){
            logger.error("No response after timeout");
            return null;
        }

        // We just want a single response.
        ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
        SocketAddress router = channel.receive(buf);
        buf.flip();
        Packet resp = Packet.fromBuffer(buf);
        logger.info("Packet: {}", resp);
        logger.info("Router: {}", router);
        String payload = new String(resp.getPayload(), StandardCharsets.UTF_8).trim();
        logger.info("Payload: {}",  payload);

        keys.clear();
        
        return resp;
		
    	
    }
    
    /**
     * This method transforms a byte[] into an array of Packets. Useful before sending it to the server through our UDP interface. 
     * @return Array of packets representing the string
     */
    public static Packet[] dataToPackets(byte[] data, InetSocketAddress serverAddr)
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
                    .setPortNumber(serverAddr.getPort())
                    .setPeerAddress(serverAddr.getAddress())
                    .setPayload(tmpPayload)
                    .create();
    		
    		arrOfPackets[i] = new Packet(packet);  // has to be a deep copy or else things won't work
    		
    		tmpPayload = new byte[b];
    	}
    	
    	
    	
    	return arrOfPackets;
    }
    
    

    public static void main(String[] args) throws IOException {
        OptionParser parser = new OptionParser();
        parser.accepts("router-host", "Router hostname")
                .withOptionalArg()
                .defaultsTo("localhost");

        parser.accepts("router-port", "Router port number")
                .withOptionalArg()
                .defaultsTo("3000");

        parser.accepts("server-host", "EchoServer hostname")
                .withOptionalArg()
                .defaultsTo("localhost");

        parser.accepts("server-port", "EchoServer listening port")
                .withOptionalArg()
                .defaultsTo("8007");

        OptionSet opts = parser.parse(args);

        // Router address
        String routerHost = (String) opts.valueOf("router-host");
        int routerPort = Integer.parseInt((String) opts.valueOf("router-port"));

        // Server address
        String serverHost = (String) opts.valueOf("server-host");
        int serverPort = Integer.parseInt((String) opts.valueOf("server-port"));

        SocketAddress routerAddress = new InetSocketAddress(routerHost, routerPort);
        InetSocketAddress serverAddress = new InetSocketAddress(serverHost, serverPort);

        runClient(routerAddress, serverAddress);
        
        
        // Testing transformation of string to packets.
        /*
        String strTest = generateMySampleStr();
        
        System.out.println("Byte length of my string: "+ strTest.getBytes().length);
        Packet[] pArr = dataToPackets(strTest.getBytes(), serverAddress);
        
        System.out.println("I have " + pArr.length + " packets.");
        System.out.println();
        
        System.out.println("MY STRING IS: ");
        System.out.println(strTest);
        
        for(int i=0; i< pArr.length; i++)
        {
        	System.out.println("The payload of packet " + (i+1) + " is: " + new String(pArr[i].getPayload()));
        	System.out.println();
        }
        
        */
        
    }
    
    public static String generateMySampleStr()
    {
    	String strTest = "Trigger Point Guide\n"+
    			"by DCarrer · Published November 10, 2016 · Updated November 10, 2016\n" +
    			"First, I’m going to explain what trigger points are and how to treat them. Then, it will be easier for you to learn by watching videos. I posted really good videos at the end of this document.\n" +
    			"Basically, what happens is that small balls form along certain muscle fibers due to many different reasons like stress, tension, injuries, lack of sleep, having certain illnesses, etc. Any of these reasons can form small contracted balls of muscle fiber along any muscle. People usually call these balls muscle knots. Physical therapists (physiotherapists) call them trigger points.\n"+
    			"When a person moves muscle fibers are being fired; they contract and de-contract in a fraction of a second. Normally, any part of a muscle in your body contracts and then it goes back to a de-contracted and resting state. For some reason, some parts of the fiber are unable to de-contract and they remain in a contracted state indefinitely. It is usually over-using a body part and stress that cause muscle fibers to remain contracted. If the parts in the fiber or the fiber itself remain contracted for too long they eventually become like a ball or a knot. Trigger point therapy helps the fibers to relax and de-contract by releasing trigger points (this is also known as myofascial release or trigger point massage).\n"+
    			"Muscles are like taut bands or a collection of many strings or fibers. When a ball forms along the fiber, the string, or along the taut band, it makes it way shorter than it normally is, causing it to overstretch when doing certain movements.\n"+
    			"Imagine you have a shoelace or a string, and you stretch it from side to side with your hands. See how far you can stretch it. Now, tie a knot. If you stretch the lace again it is much shorter and you have to force it to reach the same length it was able to reach before you tied the knot. If you tie more knots along the lace and you stretch it again you won’t be able to reach the same length anymore. The same principle applies to muscles. Eventually, if the balls are not released or corrected it could lead the muscle to lose its flexibility. This is one of the reasons why elderly people become tight and lose mobility.\n"+
    			"Inflammation and the Vicious Cycle\n"+
    			"When a ball forms along the muscle it also restricts the normal blood flow and circulation in the affected area. Waste and other by-products of metabolism get stuck in the fiber. It is like everything gets stuck where the ball is. Nutrients cannot reach other parts of the fiber around the knot. This causes starvation of oxygen and nutrients in the surrounding area producing tiredness, tension, pain and discomfort. In the most extreme cases, it can produce extreme pain and sensitivity. The body’s natural response and attempt to heal is to cause inflammation in the area.\n"+
    			"Inflammation is one of the natural mechanisms that the body has in order to heal itself. Basically, the body raises the temperature in the area affected and sends antibodies to kill anything that could be causing the pain, then it patches the affected area with random cells in order to fix it. Inflammation is good for certain cases but it is not the right way to fix trigger points. Also, inflammation often produces more trigger points and more trigger points produce more inflammation ending in a vicious cycle or what we call a feedback loop. We have to cut the cycle by giving a hand to our own body if we want to fix trigger points.\n"+
    			"Referred pain\n"+
    			"Trigger points also cause what is called referred pain which means that you can have a trigger point in an area of your body that causes pain in another area of your body. For example, a trigger point in your gluteus muscle can send pain to your lower back or a trigger point in your shoulder can cause pain in your arm and hand. Even headaches can be caused by trigger points found in other areas of the body. Usually, the trigger point is somewhere else from where you feel the pain. This can make patients and some doctors, who don’t know about trigger points, treat the area where you have pain instead of finding the trigger point that is causing the pain. Some doctors focus on treating the symptoms instead of addressing the underlying cause. They do this by giving painkillers and other drugs to their patients. Painkillers do not fix the problem, they only help you so that you don’t feel pain (they mask the pain). Also, physiotherapists or other doctors, ignoring trigger points, will tell their patients to do certain exercises to improve the condition. These exercises are not effective and do not work because the trigger point is starving the tissues around it. For the exercises to be effective you need nutrients and oxygen constantly reaching cells so that they can regenerate and assimilate the exercise. What has to be done first is to find and fix the trigger points, then exercises will be effective.\n" +
    			"Finding trigger points\n"+
    			"Now, I will get to the point. The secret of trigger point therapy is finding the trigger points that are causing the pain and to deactivate them. After you find them, you deactivate them by performing a massage using the right technique. To find trigger points you have to apply pressure on your muscles, you can apply pressure using your hands or using tools.\n"+
    			"With your fingers or with a tool you press on your muscle and then you move up or down a little bit and you press on your muscle again. If you press on your muscle and you suddenly feel pain, then a trigger point could exist in that part of the muscle that you just pressed. In other words, you can find them by touch; you will feel like a small bump, like a sac or a ball. If you apply pressure on this ball or bump, and you feel pain right away or you feel pain somewhere else in your body, then it is a trigger point.\n"+
    			"Releasing trigger points\n"+
    			"To release the trigger points you have to apply pressure on the point and you have to move the pressure up or down. Just follow one direction. For example, you press the trigger point with your finger and then you push it away while applying constant pressure or you just keep applying pressure on the point. See what works best for you. Then, lift your hand, let the muscle rest a little bit and repeat the same thing again a couple of times. The pressure has to be strong enough so that you feel a 3 or 4 out of 10 in the scale of pain where 1 is the least pain you can feel and 10 is the highest level of pain you can feel. So aim for a 3 or 4 in the level of pain.\n"+
    			"Basically, you have to apply pressure strong enough until you feel pain or discomfort that it is easy to resist. The massage is working if you start feeling less pain when you apply pressure to the point after a couple of repetitions. If the pain decreases it means that the massage is being effective.\n"+
    			"Applying stronger pressure to feel a higher level of pain will not make the treatment more effective and it can even give you a temporal setback by irritating your muscles more.\n"+
    			"People with chronic pain have to do treatment 2 to 3 times per day or at least 3 to 4 times per week. You have to do it until the trigger point has disappeared (for people with chronic problems).\n"+
    			"To do the massage you can use your hands or you can use tools. The good thing about this is that you can treat yourself by doing self-treatment massage techniques. For hard to reach places and for the massage to be effective in some places of your body, you will need to get certain tools. They are easy to find and most of them are cheap.\n"+
    			"You need a tennis ball, you could also use a lacrosse ball or any other ball that is similar to a tennis ball. It has to be firm because you will put your weight on it. Lacrosse balls are much harder so they might not be good for you unless you are already used to the massage with a tennis ball. You also need a Foam Roller, usually, you can find them in athletic gyms or in fitness stores. I would recommend a high-density foam roller because it will last many years. For the most types of massage, that’s all you need. If you can find an Index Knobber, a Thera Cane, a Tiger Tail, or other similar instruments it would be a plus. They can help your hands to avoid overusing them, and some of them are really effective instruments for the back.\n"+
    			"In any case, you can do most of the massages with just a tennis ball, a foam roller, and your hands.";
    	
    			return strTest;
    }
    
   public static String takeUserInput() {
	   
	
		String cmd = new String(); //String used to store user input
		@SuppressWarnings("resource")
		Scanner kb = new Scanner(System.in);
		
	   
	    do
		{
			System.out.println("Enter Meldan commands and press enter. For help enter help, \\h, or ?");
			System.out.println("For Lab 2 commands do the following:");
			System.out.println("1) get localhost/ --> Returns a list of the current files in the data directory");
			System.out.println("2) get localhost/bestWish.txt --> Returns the content of the file bestWish.txt in the data directory");
			System.out.println("3) post localhost/bestWish.txt \"content\" --> Rewrites the file bestWish.txt with the content written by the user");
			cmd = kb.nextLine();
			//System.out.println(cmd);
			String [] arr = cmd.split(" ");
			
			
			// Main commands 
			switch(arr[0]) 
			{
			case "get": return cmd;  
			case "post": return cmd;  
			case "help" :  case "\\h": case "?": help(); break;
			case "exit" : return "exit";
			default : System.out.println("Please enter a valid command.");
			}
			
		
	   }while(!cmd.equals("exit"));
		return "not-valid";
   }
   
   public static void help()
   {
 	  System.out.println("Enter a command in the following format: \n" +
 	  		  "meldan get -v -h a:b -h c:d url/extension \nNote: The url does not contain \"http://\" \n" + 
 			  "Example 1: meldan get -v -h a:b httpbin.org/get?course=networking&assignment=1 \n" +
 	  		  "Example 2: meldan get -v -h Content-Type:application/json -h a:b -h c:d -h z:2 google.ca/?gws_rd=cr&ei=qQr4V6PzGYiF-wHFlqeICg#q=hello \n" +
 			  "Example 3: meldan post -h Content-Type:application/json -d '{\"Assignment\":_1}' httpbin.org/post \n" +
 	  		  "Example 4: meldan post -h a:b -f C:\\Users\\Daniel\\Documents\\fileName.txt httpbin.org/post");

 	  System.out.println("Enter the command 'exit' to end session.");
   }
   
   
}

