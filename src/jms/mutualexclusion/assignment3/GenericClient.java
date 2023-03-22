package jms.mutualexclusion.assignment3;

import javax.jms.Message;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Session;
import javax.jms.QueueSession;
import javax.jms.JMSException;
import javax.jms.QueueReceiver;
import javax.jms.MessageConsumer;
import jms.mutualexclusion.assignment3.GenericClient;


public class GenericClient
{
	protected enum State 
	{
		IDLE,
		CANDIDATE,
		DEAD,
		COORDINATOR,
		REQUESTER,
		WAITER
	}

	// client-broker parameters
	protected static final String BROKER_URL   = "tcp://localhost:61616";
	protected static final String BROKER_PROPS = "persistent=false&useJmx=false";

	// client-broker-coordinator queue: used by clients to get id
	public static final String BROKER_QUEUE_NAME   = "0";

	// different types of message
	protected static final String ID_REQUEST = "ID_REQUEST";
	protected static final String ID_RESPONSE = "ID_RESPONDE";
	protected static final String RESOURCE_REQUEST = "RESOURCE_REQUEST";
	protected static final String ELECTION_REQ = "ELECTION_REQ";
	protected static final String ELECTION_ACK = "ELECTION_ACK"; 
	protected static final String COORDINATOR = "COORDINATOR";

	protected static final int ITER_TH_TOLERANCE = 2;

	// client's state
	protected State myState;

	// client id used to identify the process and the queue. INCR_ID is used by the coordinator to
	// assign an incremental id to other clients
	protected static int INCR_ID = 1;

	// this is the client id
	protected int CLIENT_ID = -1;

	// number of client firstly connected
	protected static final int N_CONNECTED = 3; 

	protected SendReceiverQueue myQueue;

	private QueueSession session;
	private ActiveMQConnection connection;


	protected void clientOperations() throws JMSException, InterruptedException
	{
		int ackToReceive = 0;
		int iterTolerance = 0;
		boolean elecSent = false;
		boolean times = true;

		while(true)
		{
			/* all clients except broker-client only once */
			if(!this.getCLIENT_ID().equals("0") && times)
			{
				this.myQueue = new SendReceiverQueue(this.session);

				/**
				 * INITIALIZATION PHASE: firstly the client gets the ID   
				 */			
				MessageConsumer consumer = this.myQueue.getConsumer(); 
				while(true)
				{
					this.myQueue.sendIdRequest("Client ID request message.", GenericClient.ID_REQUEST);
					Message reply = consumer.receive(); // the client waits for request	

					if(reply.getJMSType().compareTo(GenericClient.ID_RESPONSE) == 0)
					{
						this.setCLIENT_ID(Integer.parseInt(((TextMessage) reply).getText()));
						System.out.println("\n(C-" + this.getCLIENT_ID() + ") Message ID-response: " 
							+ ((TextMessage) reply).getText());
						break;
					}
				}

				this.myQueue.createQueue(this.getCLIENT_ID());
			}


			// gestione se sei candidato indici l'elezione
			if(this.getMyState() == GenericClient.State.CANDIDATE)
			{
				times = false;
				ackToReceive = this.convocateElection();
				elecSent = true;

				this.setMyState(GenericClient.State.IDLE);
			}
			else if(this.getMyState() == GenericClient.State.IDLE)
			{
				times = false;

				// if(!this.getCLIENT_ID().equals("0"))
				// {
				// 	this.myQueue = new SendReceiverQueue(session);

				// 	/**
				// 	 * INITIALIZATION PHASE: firstly the client gets the ID   
				// 	 */			
				// 	MessageConsumer consumer = this.myQueue.getConsumer(); 
				// 	while(true)
				// 	{
				// 		this.myQueue.sendIdRequest("Client ID request message.", GenericClient.ID_REQUEST);
				// 		Message reply = consumer.receive(); // the client waits for request	

				// 		if(reply.getJMSType().compareTo(GenericClient.ID_RESPONSE) == 0)
				// 		{
				// 			this.setCLIENT_ID(Integer.parseInt(((TextMessage) reply).getText()));
				// 			System.out.println("\n(C-" + this.getCLIENT_ID() + ") Message ID-response: " 
				// 				+ ((TextMessage) reply).getText());
				// 			break;
				// 		}
				// 	}

				// 	this.myQueue.createQueue(this.getCLIENT_ID());
				// }
				
				/**
				 * COMMUNICATION PHASE: once client has the ID can communicate with other peers
				 */
				// this.myQueue.createQueue(this.getCLIENT_ID());
				// int ackToReceive = 0; // not sure to cancel ???
				// int iterTolerance = 0; // not sure to cancel ???
				// boolean elecSent = false; // not sure to cancel ???

				while(true)
				{
					Message msg = this.myQueue.getQueueReceiver().receive(3000);	
					
					if(msg == null)
					{
						iterTolerance++;

						/* the client is the new coordinator (case: the highest processes are down) */
						if(elecSent && ackToReceive > 0 && iterTolerance >= GenericClient.ITER_TH_TOLERANCE)
						{
							System.out.println("\nI'm C-" + this.getCLIENT_ID() + " the new coordinator...");
							ackToReceive = 0; // not sure ???
							iterTolerance = 0; // not sure ???
							elecSent = false; // not sure ???
							
							this.sendCoordinatorMsg();
						}
					}				
					else if(msg.getJMSType().compareTo(GenericClient.COORDINATOR) == 0)
					{
						System.out.println("\n(C-" + this.getCLIENT_ID() + ") There's a new coordinator with ID: " 
							+ msg.getJMSCorrelationID());
					}
					else if(msg.getJMSType().compareTo(GenericClient.ELECTION_REQ) == 0)
					{
						System.out.println("\n(C-" + this.getCLIENT_ID() + ") Message: " 
							+ ((TextMessage) msg).getText() + " received by: " + msg.getJMSCorrelationID());

						this.myQueue.sendMessage("Client-" + this.getCLIENT_ID() 
							+ " ACK", GenericClient.ELECTION_ACK, this.getCLIENT_ID(), msg.getJMSCorrelationID());

						ackToReceive = this.convocateElection();
						elecSent = true;
					}
					else if(msg.getJMSType().compareTo(GenericClient.ELECTION_ACK) == 0)
					{
						System.out.println("\nreply to (C-" + this.getCLIENT_ID() + ") Message: " 
							+ ((TextMessage) msg).getText());
						
						ackToReceive--;
					}
				}
			}
		}
	}

	public void idAssignment(QueueReceiver receiver) throws JMSException
	{
		// The server waits for requests by any client
		Message request = receiver.receive();
		
		System.out.println("\nMessage: " + ((TextMessage) request).getText());

		if(request.getJMSType().compareTo(GenericClient.ID_REQUEST) == 0)
		{
			System.out.println("\nID_REQUEST. Assigned: " + GenericClient.getINCR_ID() + "\n");
			this.myQueue.sendIdResponse(request, Integer.toString(GenericClient.getINCR_ID()), GenericClient.ID_RESPONSE);
			GenericClient.setINCR_ID();
		}
	}


	protected int convocateElection() throws JMSException
	{
		int start = Integer.parseInt(this.getCLIENT_ID()) + 1;
		int end = GenericClient.N_CONNECTED;
		int count = 1; // positivo così nel caso in cui start > end allora il client con id più grande è automaticamente il coordinatore

		if (start <= end)
		{
			count = end - start;

			for (int i = start; i <= end; i++) 
			{
				System.out.println("\nI'm the process with ID-" + this.getCLIENT_ID() 
					+ " and I'm convocating an election to client-" + i);

				// Communication settings: the client-broker sends messages to other peers
				this.myQueue.sendMessage("Election convocated...", GenericClient.ELECTION_REQ, 
					this.getCLIENT_ID(), Integer.toString(i));
			}
		}

		return count;
	}


	protected void sendCoordinatorMsg() throws JMSException
	{
		this.setMyState(GenericClient.State.COORDINATOR);

		int last = Integer.parseInt(this.getCLIENT_ID()) - 1;

		for (int i = 0; i <= last; i++) 
		{
			System.out.println("\nI'm the process with ID-" + this.getCLIENT_ID() 
				+ " and I'm sending coordinator maeesage to client-" + i);

			// Communication settings: the client-broker sends messages to other peers
			this.myQueue.sendMessage("Coordinator elected...", GenericClient.COORDINATOR, 
				this.getCLIENT_ID(), Integer.toString(i));
		}
	}

	protected void setMyState(State state) 
	{
		this.myState = state;		
	}

	public State getMyState() 
	{
		return this.myState;		
	}


	protected static void setINCR_ID() 
	{
		INCR_ID++;	
	}

	public static int getINCR_ID()
	{
		return INCR_ID;
	}

	protected void setCLIENT_ID(int CLIENT_ID) 
	{
		this.CLIENT_ID = CLIENT_ID;
	}

	public String getCLIENT_ID() 
	{
		return Integer.toString(this.CLIENT_ID);
	}

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

	protected ActiveMQConnection getConnection()
	{
		return this.connection;
	}
}

			// int ackReceived = 0;
			
			// while(count >= 0)
			// {				
			// 	System.out.println("PRIMA DELLA RECEIVE");
			// 	// The server waits for requests by any client
			// 	Message reply = this.myQueue.getQueueReceiver().receive(); //3000
			// 	System.out.println("DOPO DELLA RECEIVE");

			// 	if(reply == null)
			// 	{
			// 		System.out.println("Client is DEAD!");
			// 	}
			// 	else if(reply.getJMSType().compareTo(GenericClient.ELECTION_ACK) == 0)
			// 	{
			// 		System.out.println("reply to (C-" + this.getCLIENT_ID() + ") ID: " 
			// 			+ ((TextMessage) reply).getText());
			// 		ackReceived++;
			// 	}

			// 	count--;
			// }

			/*// the client is the new coordinator (case: the highest processes are down) 
			if(ackReceived == 0)
			{
				// nessun processo con id più elevato mi ha mandato l'ack
				this.sendCoordinatorMsg();
			}*/

		// else // the client is the new coordinator (case: higher id) 
		// {
		// 	System.out.println("My ID is higher!");
		// 	// this.sendCoordinatorMsg();
		// }