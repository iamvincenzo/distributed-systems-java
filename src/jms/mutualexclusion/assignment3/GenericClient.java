package jms.mutualexclusion.assignment3;

import javax.jms.Message;
import javax.jms.TextMessage;
import javax.jms.Destination;
import javax.jms.QueueSession;
import javax.jms.JMSException;
import javax.jms.QueueReceiver;
import javax.jms.MessageProducer;
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

	// client's state
	protected State myState;

	// client id used to identify the process and the queue. INCR_ID is used by the coordinator to
	// assign an incremental id to other clients
	protected static int INCR_ID = 0;

	// this is the client id
	protected int CLIENT_ID = 0;

	// number of client firstly connected
	protected static final int N_CONNECTED = 3; 

	protected SendReceiverQueue myQueue;


	public void idAssignment(QueueSession session, QueueReceiver receiver) throws JMSException
	{
		// The server waits for requests by any client
		Message request = receiver.receive();
		
		System.out.println("Message: " + ((TextMessage) request).getText() 
			+ " ReplyTo: " + request.getJMSReplyTo() + "\n");

		if(request.getJMSType().compareTo(GenericClient.ID_REQUEST) == 0)
		{
			System.out.println("ID_REQUEST. Assigned: " + GenericClient.getINCR_ID() + "\n");
			this.myQueue.sendResponse(request, Integer.toString(GenericClient.getINCR_ID()), GenericClient.ID_RESPONSE);
			GenericClient.setINCR_ID();
		}
	}


	protected void convocateElection(QueueSession session) throws JMSException
	{
		int start = Integer.parseInt(this.getCLIENT_ID()) + 1;
		int end = GenericClient.N_CONNECTED;

		if (start <= end)
		{
			System.out.println("I'm the process with ID-" + this.getCLIENT_ID() 
					+ " start: " + start + ", end: " + end);

			for (int i = start; i <= end; i++) 
			{
				System.out.println("I'm the process with ID-" + this.getCLIENT_ID() 
					+ " and I'm convocating an election to client-" + i);

				// Communication settings: the client-broker sends messages to other peers
				Destination peerQueue  = session.createQueue(Integer.toString(i));
				MessageProducer producer = session.createProducer(peerQueue);
				this.myQueue.sendRequest(producer, "Election convocated.", GenericClient.ELECTION_REQ, this.getCLIENT_ID());
			}
		
			int count = end - start;
			int ackReceived = 0;

			while(count >= 0)
			{
				// the client waits for request
				Message reply = this.myQueue.getConsumer().receive(3000);
				
				if(reply == null)
				{
					System.out.println("Client is DEAD!");
				}
				else if(reply.getJMSType().compareTo(GenericClient.ELECTION_ACK) == 0)
				{
					System.out.println("(C-" + this.getCLIENT_ID() + ") ID: " 
						+ ((TextMessage) reply).getText());
					ackReceived++;
				}

				count--;
			}

			// the client is the new coordinator (case: the highest processes are down) 
			if(ackReceived == 0)
			{
				// nessun processo con id più elevato mi ha mandato l'ack
				this.sendCoordinatorMsg();
			}
		}

		else // the client is the new coordinator (case: higher id) 
		{
			System.out.println("My ID is higher!");
			this.sendCoordinatorMsg();
		}
	}


	protected void clientOperations(QueueSession session) throws JMSException, InterruptedException
	{
		this.myQueue = new SendReceiverQueue(session);

		if(this.getMyState() == GenericClient.State.IDLE)
		{			
			// Communication settings: the client sends messages to server
			// Destination serverQueue  = this.myQueue.getServerQueue();
			MessageProducer producer = this.myQueue.getProducer();

			// Communication settings: the client defines an endpoint used by the server to reply
			// Destination tempDest = this.myQueue.getTempDest();
			MessageConsumer consumer = this.myQueue.getConsumer();// setting the role

			// firstly the client get ID
			while(true)
			{
				// The client creates the request
				this.myQueue.sendRequest(producer, "Client ID request message.", 
						GenericClient.ID_REQUEST, this.getCLIENT_ID());

				// the client waits for request
				Message reply = consumer.receive();	

				if(reply.getJMSType().compareTo(GenericClient.ID_RESPONSE) == 0)
				{
					this.setCLIENT_ID(Integer.parseInt(((TextMessage) reply).getText()));
					System.out.println("(C-" + this.getCLIENT_ID() + ") ID: " 
						+ ((TextMessage) reply).getText());
					break;
				}
			}

			// once client has the ID can communicate with other peers
			this.myQueue.createQueue(this.getCLIENT_ID());

			// communication phase
			while(true)
			{
				// The server waits for requests by any client
				Message msg = this.myQueue.getQueueReceiver().receive();
				
				if(msg.getJMSType().compareTo(GenericClient.COORDINATOR) == 0)
				{
					System.out.println("There's a new coordinator with ID: " + msg.getJMSCorrelationID());
				}
				else if(msg.getJMSType().compareTo(GenericClient.ELECTION_REQ) == 0)
				{
					System.out.println("(C-" + this.getCLIENT_ID() + ") ID: " 
						+ ((TextMessage) msg).getText() + ", received by: " + msg.getJMSCorrelationID());

					this.myQueue.sendResponse(msg, "Client-" + this.getCLIENT_ID() 
						+ " ACK", GenericClient.ELECTION_ACK);

					this.convocateElection(session);
				}
			}
		}
	}

	protected void sendCoordinatorMsg() throws JMSException
	{
		this.setMyState(GenericClient.State.COORDINATOR);

		int last = Integer.parseInt(this.getCLIENT_ID()) - 1;

		for (int i = 0; i <= last; i++) 
		{
			System.out.println("I'm the process with ID-" + this.getCLIENT_ID() 
				+ " and I'm sending coordinator maeesage to client-" + i);

			// Communication settings: the client-broker sends messages to other peers
			Destination peerQueue = this.myQueue.getSession().createQueue(Integer.toString(i));
			MessageProducer producer = this.myQueue.getSession().createProducer(peerQueue);
			this.myQueue.sendRequest(producer, "Coordinator elected.", GenericClient.COORDINATOR, this.getCLIENT_ID());
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
}
