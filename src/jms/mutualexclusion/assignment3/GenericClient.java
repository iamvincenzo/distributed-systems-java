package jms.mutualexclusion.assignment3;

import javax.jms.Message;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Session;
import javax.jms.QueueSession;

import java.util.Random;

import javax.jms.JMSException;
import javax.jms.QueueReceiver;
import javax.jms.MessageConsumer;
import jms.mutualexclusion.assignment3.GenericClient;

/**
 * This class provides the main methods required to handle interaction. 
 * The class also defines the actions that need to be performed by the node 
 * based on the role it assumes.
 * 
 * @author Vincenzo Fraello (339641)
 * @author Giorgia Tedaldi (339642)
 */

public class GenericClient
{
	/* Class fields */
	/*******************************************************************************************************************************/
	
	/**
	 * This enum defines all the possible states 
	 * in which a node can be during its lifecycle.
	 */
	protected enum State 
	{
		IDLE,
		CANDIDATE,
		DEAD,
		COORDINATOR
	}

	/**
	 * Class field used to save the current
	 * state of the node.
	 */
	protected State myState;

	/**
	 * H - is the probability to stay active.
	 * K - is the probability to crash.
	 */
	protected static final float H = 0.02f;
	protected static final float K = 0.001f;

	/**
	 * This threshold is used to determine whether 
	 * a node needs to request the resource.
	 */
	protected static final int RES_REQ_TH = 70;

	/**
	 * This threshold is used to determine whether the coordinator 
	 * has to ping the process that holds the resource.
	 */
	protected static final int RES_PING_TH = 3;

	/**
	 * These constant class fields represent different types of messages.
	 */
	protected static final String ID_REQUEST = "ID_REQUEST";
	protected static final String ID_RESPONSE = "ID_RESPONDE";
	protected static final String RESOURCE_REQUEST = "RESOURCE_REQUEST";
	protected static final String ELECTION_REQ = "ELECTION_REQ";
	protected static final String ELECTION_ACK = "ELECTION_ACK"; 
	protected static final String COORDINATOR = "COORDINATOR";
	protected static final String COORD_PING = "COORD_PING";
	protected static final String COORD_ALIVE = "COORD_ALIVE";
	protected static final String RESOURCE_REQ = "RESOURCE_REQ";
	protected static final String RESOURCE_RESP = "RESOURCE_RESP";
	protected static final String FREE_RESOURCE = "FREE_RESOURCE";
	protected static final String USER_ALIVE_CHECK = "USER_ALVIE_CHECK";
	protected static final String USER_ALVIE_ACK = "USER_ALVIE_ACK";

	/**
	 * Client-broker parameters
	 * Client-broker-coordinator queue: used by clients to get id
	 */
	protected static final String BROKER_URL   = "tcp://localhost:61616";
	protected static final String BROKER_PROPS = "persistent=false&useJmx=false";
	public static final String BROKER_QUEUE_NAME   = "0";

	/**
	 * Class field used to save the id of the current coordinator.
	 */	
	public String COORD_ID = "-1";

	/**
	 * This threshold is used in various operations, such as 
	 * receiving acknowledgments after an election, to provide 
	 * a certain degree of tolerance.
	 */
	protected static final int ITER_TH_TOLERANCE = 4;

	/**
	 * This class field is used by the coordinator to 
	 * assign an incremental id to other clients.
	 */
	protected static int INCR_ID = 1;

	/**
	 * This is the client id. The client id 
	 * used to identify the process and the queue.
	 */
	protected int CLIENT_ID = -1;

	/**
	 * This class field represents the totol 
	 * number of nodes connected to the system.
	 */
	protected static final int N_CONNECTED = 3;

	/**
	 * This class field represents the queue used by a 
	 * node to communicate with other nodes in the system.
	 */
	protected SendReceiverQueue myQueue;

	/**
	 * Class fields that represents:
	 * 	- a session for sending and receiving messages to/from a message queue;
	 *  - a connection to an ActiveMQ message broker. 
	 */
	private QueueSession session;
	private ActiveMQConnection connection;


	/**
	 * This class field is used to 
	 * represent the status of the resource.
	 */
	private boolean busy = false;

	/**
	 * This class field is used to represent 
	 * the id of the user that holds the resource.
	 */
	private String resourceUser = null;

	/**
	 * This class field is used to generate random numbers.
	 */
	private Random random = new Random();

	/* Class methods */
	/*******************************************************************************************************************************/

	/**
	 * This class method is used by the ClientBroker to first 
	 * assign IDs to the various nodes connected to the system.
	 * 
	 * @param receiver - QueueReceiver used in to receive messages from a message queue.
	 * @throws JMSException - It is an exception used to handle exceptions that occur during JMS operations.
	 */
	public void idAssignment(QueueReceiver receiver) throws JMSException
	{
		/* The server waits for requests by any client. */
		Message request = receiver.receive();
		
		System.out.println("\nMessage: " + ((TextMessage) request).getText());

		/* The server sends the ID if the type of the request is "ID_REQUEST". */
		if(request.getJMSType().compareTo(GenericClient.ID_REQUEST) == 0)
		{
			System.out.println("\nID_REQUEST. Assigned: " + GenericClient.getINCR_ID() + "\n");
			
			/* The response-message is composed of the request parameters, 
			 * the ID, and the type of response.*/
			this.myQueue.sendIdResponse(request, 
										Integer.toString(GenericClient.getINCR_ID()), 
										GenericClient.ID_RESPONSE);
			
			/* Increment the ID to assign to the next request. */
			GenericClient.setINCR_ID();
		}
	}

	/**
	 * This class method is used by the node that is 
	 * in CANDIDATE state to initiate a coordinator election.
	 * 
	 * @return It returns the number of election messages sent.
	 * @throws JMSException - It is an exception used to handle exceptions that occur during JMS operations.
	 */
	protected int convocateElection() throws JMSException
	{
		/* The node in the CANDIDATE state sends election messages 
		 * to all nodes with IDs greater than its own. */ 
		int start = Integer.parseInt(this.getCLIENT_ID()) + 1;
		int end = GenericClient.N_CONNECTED;
		
		/* Node N does not send messages to anyone. Therefore, the value of 1 
		 * is used to ensure that it sends at least one message and does not 
		 * receive any response. This allows it to become the new coordinator. */
		int count = 1;

		/* To avoid pinging the coordinator when a new election is initiated. */
		this.setCOORD_ID("-1");

		if (start <= end)
		{
			count = end - start + 1;

			for (int i = start; i <= end; i++) 
			{
				System.out.println("\nI'm the process with ID-" + this.getCLIENT_ID() 
					+ " and I'm convocating an election to client-" + i);

				/* The node sends messages of type "ELECTION REQUEST" to other nodes. */
				this.myQueue.sendMessage("Election convocated...",
									     GenericClient.ELECTION_REQ, 
									     this.getCLIENT_ID(), 
									     Integer.toString(i));
			}
		}

		return count;
	}

	/**
	 * This class method is used by the new coordinator to 
	 * inform other nodes that it has assumed the coordinator role.
	 * 
	 * @throws JMSException - It is an exception used to handle exceptions that occur during JMS operations.
	 */
	protected void sendCoordinatorMsg() throws JMSException
	{
		this.setMyState(GenericClient.State.COORDINATOR);

		/* The coordinator is the node with the highest ID. */ 
		int last = Integer.parseInt(this.getCLIENT_ID()) - 1;

		for (int i = 0; i <= last; i++) 
		{
			System.out.println("\nI'm the process with ID-" + this.getCLIENT_ID() 
				+ " and I'm sending coordinator maeesage to client-" + i);

			// Communication settings: the client-broker sends messages to other peers
			this.myQueue.sendMessage("Coordinator elected...", 
									 GenericClient.COORDINATOR, 
									 this.getCLIENT_ID(), 
									 Integer.toString(i));
		}
	}

	/**
	 * This class method is used to generate random numbers.
	 * 
	 * @param MIN - It is the min value of the interval.
	 * @param MAX - It is the max value of the interval.
	 * 
	 * @return It returns the geenrated random number.
	 */
	protected int generateRandomNumber(int MIN, int MAX)
	{		
		return random.nextInt(MAX - MIN) + MIN;
	}

	/**
	 * This class method is used by a node to check 
	 * if it has to change its state from alive to dead.
	 * 
	 * @return It returns true if the probability of staying active 
	 * 			is higher than the probability of crashing.
	 */
	protected boolean aliveOrDead()
	{
		int n1 = generateRandomNumber(0, 100);
		int n2 = generateRandomNumber(0, 100);

		return (n1 * GenericClient.H > n2 * GenericClient.K);
	} 

	/**
	 * This class method is used by a node to 
	 * check whether it can request the resource.
	 * 
	 * @return It returns true if it can request the resource.
	 */
	protected boolean checkResourceRequest()
	{
		int n = generateRandomNumber(0, 100);

		return n > GenericClient.RES_REQ_TH;
	}

	/**
	 * This class method is used by a node 
	 * to request the resource from the coordinator.
	 * 
	 * @throws JMSException - It is an exception used to handle exceptions that occur during JMS operations.
	 */
	protected void resourceRequest() throws JMSException
	{
		System.out.println("\n(C-" + this.getCLIENT_ID() + 
			") I'm requesting the resource to the coordinator...");

		this.myQueue.sendMessage("Resource requets...", 
								 GenericClient.RESOURCE_REQ, 
								 this.getCLIENT_ID(), 
								 "R-" + this.COORD_ID);
	}

	/**
	 * This class method is used by a node to free the resource.
	 * 
	 * @throws JMSException - It is an exception used to handle exceptions that occur during JMS operations.
	 */
	protected void freeResource() throws JMSException
	{
		System.out.println("\n(C-" + this.getCLIENT_ID() + 
			") I'm freeing the resource...");

		this.myQueue.sendMessage("Resource free...", 
								 GenericClient.FREE_RESOURCE, 
								 this.getCLIENT_ID(), 
								 "R-" + this.COORD_ID);
	}

	/**
	 * This method is used by the coordinator to 
	 * keep track of the node that owns the resource.
	 */
	protected void assignResource(String id)
	{
		this.resourceUser = id;
	}

	/**
	 * This class method is used by the coordinator to
	 * check the status of the node that owns the resource.
	 * 
	 * @throws JMSException - It is an exception used to handle exceptions that occur during JMS operations.
	 */
	protected void checkIfAlive() throws JMSException
	{
		System.out.println("\n(C-" + this.getCLIENT_ID() + 
			") I'm the coordinator, checking if the resource-user (C-"+ this.getResourceUser() +") is ALIVE...");

		this.myQueue.sendMessage("Resource check alive...", 
								 GenericClient.USER_ALIVE_CHECK, 
								 this.getCLIENT_ID(), 
								 this.getResourceUser());
	}

	/**
	 * This method is used by each node in the system to operate depending on its role.
	 * 
	 * @throws JMSException - It is an exception used to handle exceptions that occur during JMS operations.
	 * @throws InterruptedException - Thrown when a thread is interrupted while it's waiting, sleeping, or otherwise occupied.
	 */
	protected void clientOperations() throws JMSException, InterruptedException
	{
		/* variable used to keep track of the 
		 * ack messages that a node has to receive. */
		int ackToReceive = 0;
		
		/* variable used to keep track 
		 * of the received ack messages. */
		int ackReceived = 0;
		
		/* variable used to provide a degree of 
		 * tolerance after certain operations. */
		int iterTolerance = 0;
		
		/* variable used to keep track of 
		 * the election operation. */
		boolean elecSent = false;
		
		/* variable used to execute the 
		 * initialization phase only once. */
		boolean times = true;
		
		/* variable used to keep track 
		 * of the ping operation. */
		boolean pinged = false;
		
		/* variable used to keep track 
		 * of the resource request operation. */
		boolean requested = false;
		
		/* variable used to keep track of the 
		 * ping operation (coordinator to resource-user) */
		int pingResourceUser = 0;
		
		/* variable used to keep track of the check alive 
		 * operation exectued by the coordinator node. */
		boolean checkAlive = false; 
		
		/* variable used to provide a degree of 
		 * tolerance after the check alive operation. */
		int checkAliveTh = 0;
		
		/* main-loop */
		while(true)
		{
			/**
			 * All clients except broker-client only once.
			 * 
			 * INITIALIZATION PHASE: firstly the client gets the ID.
			 */		
			if(!this.getCLIENT_ID().equals("0") && times)
			{
				/* Creation of a temporany queue 
				 * used by the node to receive its ID. */
				this.myQueue = new SendReceiverQueue(this.session);

				MessageConsumer consumer = this.myQueue.getConsumer(); 
				
				while(true)
				{
					this.myQueue.sendIdRequest("Client ID request message.", 
											   GenericClient.ID_REQUEST);
					
					/* The client waits for request. */
					Message reply = consumer.receive(); 

					if(reply.getJMSType().compareTo(GenericClient.ID_RESPONSE) == 0)
					{
						this.setCLIENT_ID(Integer.parseInt(((TextMessage) reply).getText()));
						
						System.out.println("\n(C-" + this.getCLIENT_ID() + ") Message ID-response: " 
							+ ((TextMessage) reply).getText());
						
						break;
					}
				}
				
				/* the parameter 0 indicates the queue type. */ 
				this.myQueue.createQueue(this.getCLIENT_ID(), 0);
				random.setSeed(Integer.parseInt(getCLIENT_ID()));
			}

			/* 
			 * The peer checks if he has to change its state.
			 * 
			 * Subcase: 
			 * If the coordinator doesn't exist yet, and the state
			 * is candidate, that peer has to start election.
			 */
			if(this.aliveOrDead() || (this.getCOORD_ID().equals("-1") 
				&& this.getMyState() == GenericClient.State.CANDIDATE)) // ??? GIO
			{
				System.out.println("\nI'm ALIVE...");

				/* If this.aliveOrDead() == true then and i was dead, 
				I'm going back active. */
				if (this.getMyState() == GenericClient.State.DEAD)
				{
					/* 
					 * Everytime a peer change its status from dead
					 * to alive, it has to convocate an election. 
					 */
					// while ((this.myQueue.getQueueReceiver().receiveNoWait()) != null) { // GIO ???
					// 	// scarta il messaggio
					// 	System.out.println("QUEUE CLEANING...");
					// }
					this.setMyState(GenericClient.State.CANDIDATE);
				}
			}
			else // I have to die 
			{
				System.out.println("\nI'm DEAD...");
				this.setMyState(GenericClient.State.DEAD);
				Thread.sleep(5000);
			}

			/* If the status of the node is CANDIDATE then it has to initiate an election. */
			if(this.getMyState() == GenericClient.State.CANDIDATE)
			{
				/* No more execution of init-phase */
				times = false;

				ackReceived = 0; 
				ackToReceive = this.convocateElection();
				elecSent = true;

				this.setMyState(GenericClient.State.IDLE);
			}
			/* If the status of the node is IDLE then the client could receive different type of messages. */
			else if(this.getMyState() == GenericClient.State.IDLE)
			{
				/* No more execution of init-phase */
				times = false;

				/* If the coordinatore exists, Ping the coordinator to 
				 * start the interaction between the node and the coordinator. */
				if(!this.getCOORD_ID().equals("-1"))
				{
					System.out.println("(C-" + this.getCLIENT_ID() + "): " 
						+ " Check if coordinator is alive (COORD_PING)...");

					this.myQueue.sendMessage("Client-" + this.getCLIENT_ID() 
												+ " Check if coordinator is alive (COORD_PING)...", 
											 GenericClient.COORD_PING, 
											 this.getCLIENT_ID(), 
											 this.getCOORD_ID());
					
					/* Used to check if the 
					 * coordinator is still alive.*/
					pinged = true;
				}

				/* Time limit in which to 
				 * receive the coordinator's response. */
				int timeout = 3000;

				/* IDLE state main-loop */ 
				while(true)
				{
					Message msg = this.myQueue.getQueueReceiver().receive(timeout);
					
					/* Case in which the node doesn't 
					 * receive any message from the coordinator. */
					if(msg == null)
					{
						/* Tolerance that has to be satisfied 
						 * before taking one of the following decisions. */
						iterTolerance++;	

						/* The node is the new coordinator (case: the highest processes are down) */
						if(elecSent && ackToReceive > 0 && ackReceived == 0 && 
								iterTolerance >= GenericClient.ITER_TH_TOLERANCE)
						{
							System.out.println("\nI'm C-" + this.getCLIENT_ID() + " the new coordinator...");
							ackToReceive = 0;
							iterTolerance = 0; 
							elecSent = false; 
							
							/* Creation of the queue used for resource requests */
							this.myQueue.createQueue("R-" + this.getCLIENT_ID(), 1);
							
							/* Inform other nodes. */
							this.sendCoordinatorMsg();
							
							break;
						}
						/* The coordinator doesn't respond to ping messages. */
						else if(pinged && iterTolerance >= GenericClient.ITER_TH_TOLERANCE) 
						{
							iterTolerance = 0;
							pinged = false;

							System.out.println("No response from the coordinator (ping-message)...");

							/* The node who notes that the coordinator 
							 * is probably down starts an election. */
							this.setMyState(GenericClient.State.CANDIDATE);
							
							break;
						}
						/* I requested the resource and the 
						 * coordinator doesn't respond to the request. */
						else if(requested)
						{
							// iterTolerance = 0;
							requested = false;
							System.out.println("I'm NOT getting the resource... EXECUTION FAILED...");

							/* The node starts a new election. */
							this.setMyState(GenericClient.State.CANDIDATE);
							
							break;
						}
					}
					/* The message received is COORDINATOR type. It means a new coordinator is elected so the loop is
					 * restarted*/
					else if(msg.getJMSType().compareTo(GenericClient.COORDINATOR) == 0)
					{
						System.out.println("\n(C-" + this.getCLIENT_ID() + ") There's a new coordinator with ID: " 
							+ msg.getJMSCorrelationID());
						
						//Update coordinator's ID
						this.setCOORD_ID(msg.getJMSCorrelationID());
						break; 
					}
					/* The message received is ELECTION_REQ type. It means another node has called a new election*/
					else if(msg.getJMSType().compareTo(GenericClient.ELECTION_REQ) == 0)
					{	
						/* Tolerance parameters are reinitialized. */ 
						iterTolerance = 0;
						pinged = false;

						System.out.println("\n(C-" + this.getCLIENT_ID() + ") Message: " 
							+ ((TextMessage) msg).getText() + " received by: " + msg.getJMSCorrelationID());

						this.myQueue.sendMessage("Client-" + this.getCLIENT_ID() + " ACK", 
												 GenericClient.ELECTION_ACK, 
												 this.getCLIENT_ID(), 
												 msg.getJMSCorrelationID());
						
						/* Every client that receive an election request calls the convocateElection method to all the
						 * clients which have an higher id and expects acknowledgments from them. Clients set his variables.
						 * The election process terminates when a client does not receive any acknowledgment and declares himself 
						 * the new coordinator.*/
						ackReceived = 0;
						ackToReceive = this.convocateElection();
						elecSent = true;
					}
					/* The message received is ELECTION_ACK type. It means another node has sent an acknowledgment over the client
					 * election request*/
					else if(msg.getJMSType().compareTo(GenericClient.ELECTION_ACK) == 0)
					{
						System.out.println("\n (C-" + this.getCLIENT_ID() + ") Peer " + 
							msg.getJMSCorrelationID() + " message: " + ((TextMessage) msg).getText());
						
						ackToReceive--;
						ackReceived++;
					}
					/* The message received is COORD_ALIVE type. It means the node has received response from
					 * the coordinator that which is alive. */
					else if(msg.getJMSType().compareTo(GenericClient.COORD_ALIVE) == 0)
					{
						pinged = false;

						System.out.println("\n (C-" + this.getCLIENT_ID() + ") Peer " + 
							msg.getJMSCorrelationID() + " message: " + ((TextMessage) msg).getText());

						/* Randomly decides whether to request the use of the resource or not*/
						if(this.checkResourceRequest())
						{
							requested = true;
							/* If it requests the use of the resource, then the executor randomly 
							 * sets a timeout and completes the execution if it receives permission 
							 * from the coordinator within the waiting period.
							 */
							timeout = this.generateRandomNumber(6000, 10000);
							this.resourceRequest();
						}
						else
						{
							System.out.println("\n(C-" + this.getCLIENT_ID() + 
								") I'm NOT requesting the resource to the coordinator...");
							
							/* If it doesn't ask for the resource, then it has to 
							 * double check its status to understand if it should stay alive or die, 
							 * then eventually if it stays alive, check if the coordinator is alive to 
							 * request the resource again.
							 */
							int wait = generateRandomNumber(6000, 10000); 
							Thread.sleep(wait);							
							break; 
						}
					}
					/* The message received is RESOURCE_RESP type. The message contains the responde to the request
					 * resource message. */
					else if(msg.getJMSType().compareTo(GenericClient.RESOURCE_RESP) == 0)
					{
						/* The client gets the resource.*/
						if(((TextMessage) msg).getText().equals("Y"))
						{
							System.out.println("BAM I'm getting the resource... Completing execution...");

							// simulating client execution for a random period of time
							int cont = generateRandomNumber(6, 10); 
							while(cont > 0)
							{
								/* While the client is using the resource he has to be prepared to respond to 
								 * coordinator message. The coordinator checks if the resource's user is alive. It is used
								 * a receiveNoWait to check if a message is immediately available otherwise continues its execution.  */
								Message aliveReq = this.myQueue.getQueueReceiver().receiveNoWait();
								
								if(aliveReq != null && aliveReq.getJMSType().compareTo(GenericClient.USER_ALIVE_CHECK) == 0)
								{
									System.out.println("\n(C-" + this.getCLIENT_ID() + ") Message: " 
										+ ((TextMessage) msg).getText() + " received by: " + msg.getJMSCorrelationID());

									this.myQueue.sendMessage("Client-" + this.getCLIENT_ID() 
										+ " USER ALIVE ACK", GenericClient.USER_ALVIE_ACK, 
										this.getCLIENT_ID(), msg.getJMSCorrelationID());
								}

								Thread.sleep(1000);
								cont--;
							}
							
							/* Client frees the resource. */
							this.freeResource();
							break;
						}
						/* The client does not receive the resource because another client is using it. */
						else
						{
							System.out.println("I'm NOT getting the resource because it's BUSY...");
							
							//The client waits a random period of time before deciding if to make another request or not.
							int wait = generateRandomNumber(6000, 10000); 
							Thread.sleep(wait);
							break;
						}
					}
				}
			}
			/* If the status of the node is COORDINATOR then it could receive different types of messages. */
			else if(this.getMyState() == GenericClient.State.COORDINATOR)
			{
				pingResourceUser++;

				/* the coordinator looks for resource request/release. The coordinator has two different type of queues:
				 * one to handle the use of the resource and one to handle the other type of messages. */ 
				Message resReq = this.myQueue.getResourceReceiver().receiveNoWait();

				if (!(resReq == null))
				{
					/* Some node contacted the coordinator to get the resource*/
					if(resReq.getJMSType().compareTo(GenericClient.RESOURCE_REQ) == 0)
					{
						System.out.println("\n(C-" + this.getCLIENT_ID() + ") I'm the coordinator, C-" 
							+ resReq.getJMSCorrelationID() + " contacted me for RESOURCE REQUEST...");
						
						String result = "";
						
						/* The coordinator checks if the resource is free or busy. It responds
						 * to the node assigning him the resource or not. */
						if(!busy)
						{
							result = "Y";
							
							//Set the resource to busy and get the ID of the user that is using it
							this.setBusy(true);
							this.assignResource(resReq.getJMSCorrelationID());
							
							System.out.println("\n(C-" + this.getCLIENT_ID() + ") I'm the coordinator, and I assigned" + 
								" the resource to C-" + resReq.getJMSCorrelationID() + "...");
						}
						else
						{
							result = "N";

							System.out.println("\n(C-" + this.getCLIENT_ID() + ") I'm the coordinator, I'm not assigning" + 
								" the resource to C-" + resReq.getJMSCorrelationID() + "...");
						}

						this.myQueue.sendMessage(result, GenericClient.RESOURCE_RESP, 
							this.getCLIENT_ID(), resReq.getJMSCorrelationID());
					}
					/* Some node contacted the coordinator to free the resource*/
					else if(resReq.getJMSType().compareTo(GenericClient.FREE_RESOURCE) == 0)
					{
						this.setBusy(false);
						this.assignResource(null);

						System.out.println("\n(C-" + this.getCLIENT_ID() + ") I'm the coordinator, C-" 
							+ resReq.getJMSCorrelationID() + " contacted me for RESOURCE FREEING...");
					}
				}
				
				/* The coordinator has to check weather if the resource's user is still alive or not. */
				if(pingResourceUser > GenericClient.RES_PING_TH && this.getBusy())
				{
					//Coordinator starts to ping the resource user and sets the tolerance to 0.
					checkAlive = true;
					pingResourceUser = 0;
					this.checkIfAlive();
				}
					
				Message msg = this.myQueue.getQueueReceiver().receive(3000);	
				
				/* If the coordinator does not receive any message */
				if(msg == null)
				{
					checkAliveTh++; 

					// the coordinator check if the resource-user is still alive
					if(checkAlive && checkAliveTh > GenericClient.RES_PING_TH)
					{
						//The user that owns the resource didn't answer to the coordinator for a number
						//times that is above the threshold so the resource is freed.
						checkAlive = false;
						checkAliveTh = 0;
						this.setBusy(false);
						this.assignResource(null);
					}
				}		
				/* The coordinator received a COORD_PING message. It means that one of the client contacted him to 
				 * check if he's alive. */
				else if(msg.getJMSType().compareTo(GenericClient.COORD_PING) == 0)
				{
					System.out.println("\n(C-" + this.getCLIENT_ID() + ") I'm the coordinator, C-" 
						+ msg.getJMSCorrelationID() + " contacted me for PING...");
						
					this.myQueue.sendMessage("I'm ALIVE...", GenericClient.COORD_ALIVE, 
						this.getCLIENT_ID(), msg.getJMSCorrelationID());
				}
				/* The coordinator received a USER_ALVIE_ACK message. It means that the user which owns the resource
				 * is still alive and has responded to the message. */
				else if(msg.getJMSType().compareTo(GenericClient.USER_ALVIE_ACK) == 0)
				{
					System.out.println("\n(C-" + this.getCLIENT_ID() + ") I'm the coordinator, C-" 
						+ msg.getJMSCorrelationID() + " is still ALIVE...");
					
					checkAlive = false;
					checkAliveTh = 0;
					pingResourceUser = 0;
				}
				/* The coordinator received a ELECTION_REQ or COORDINATOR message. It means that a new election is
				 * convocated or a new coordinator is elected. The coordinator then frees the resource and set its state 
				 * to IDLE */
				else if (msg.getJMSType().compareTo(GenericClient.ELECTION_REQ) == 0 || 
						 msg.getJMSType().compareTo(GenericClient.COORDINATOR) == 0)
				{
					this.setBusy(false);
					this.assignResource(null);

					System.out.println("\n(C-" + this.getCLIENT_ID() + ") (COORD) Election request received by: " 
										+ msg.getJMSCorrelationID());
					this.setMyState(GenericClient.State.IDLE);
				}
			}
			/* If the state of the client is DEAD then the node sleeps for 6 seconds. */
			else if(this.getMyState() == GenericClient.State.DEAD)
			{
				Thread.sleep(6000);
			}
		}
	}

	/**
	 * 
	 * Method used by the client to set its own state.
	 * 
	 * @param state to set
	 */
	protected void setMyState(State state) 
	{
		this.myState = state;		
	}


	/**
	 * 
	 * Method used by the client to get its own state
	 * 
	 * @return current state of the client
	 */
	public State getMyState() 
	{
		return this.myState;		
	}


	/**
	 * Method to increase the variable that is used to set the id of the clients.
	 */
	protected static void setINCR_ID() 
	{
		INCR_ID++;	
	}


	/**
	 * 
	 * Method to get the variable that is used to set the id of the clients.
	 * 
	 * @return value of the last ID assigned
	 */
	public static int getINCR_ID()
	{
		return INCR_ID;
	}


	/**
	 * 
	 * Method used to set the client is-
	 * 
	 * @param CLIENT_ID 
	 */
	protected void setCLIENT_ID(int CLIENT_ID) 
	{
		this.CLIENT_ID = CLIENT_ID;
	}


	/**
	 * 
	 * Method to get the client id.
	 * 
	 * @return string that represent the current client's id
	 */
	public String getCLIENT_ID() 
	{
		return Integer.toString(this.CLIENT_ID);
	}

	/**
	 * 
	 * Method to get the id of the coordinator.
	 * 
	 * @return coordinator id
	 */
	public String getCOORD_ID()
	{
		return this.COORD_ID;
	}


	/**
	 * 
	 * Method to set the id of the coordinator.
	 * 
	 * @param id string that represents the id of the new coordinator
	 */
	protected void setCOORD_ID(String id)
	{
		this.COORD_ID = id;
	}

	/**
	 * 
	 * Method to initialize ActiveMQ connection and to start a new session. 
	 * 
	 * @return the new session (it's a QueueSession)
	 */
	protected QueueSession createSession()
	{
		try 
		{
			ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory(ClientBroker.BROKER_URL);
			ActiveMQConnection connection = (ActiveMQConnection) cf.createConnection();
			connection.start();
			QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			this.session = session; // setSession()
			this.connection = connection; // setConnection()
		} 
		catch (JMSException e) 
		{
			e.printStackTrace();
		}
		
		return this.session;
	}


	/**
	 * 
	 * Method to get the ActiveMQConnection connection.
	 * 
	 * @return ActiveMQConnection connection
	 * 
	 */
	protected ActiveMQConnection getConnection()
	{
		return this.connection;
	}

	/**
	 * 
	 * Method to get the session.
	 * 
	 * @return QueueSession session
	 * 
	 */
	protected QueueSession getSession()
	{
		return this.session;
	}

	/**
	 * 
	 * Method to handle the resource.
	 * 
	 * @param val 
	 * 
	 */
	private void setBusy(boolean val)
	{
		this.busy = val;
	}

	/**
	 * 
	 * Method to get the state of the resource.
	 * 
	 * @return val True if resource is busy, False if it is not.
	 * 
	 */
	private boolean getBusy()
	{
		return this.busy;
	}


	/**
	 * 
	 * Method to get the id of the current resource user.
	 * 
	 * @return string that contains the current resource user.
	 * 
	 */
	private String getResourceUser()
	{
		return this.resourceUser;
	}

}
