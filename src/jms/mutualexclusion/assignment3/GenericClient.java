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
			this.myQueue.sendResponse(request, Integer.toString(GenericClient.INCR_ID), GenericClient.ID_RESPONSE);
			GenericClient.setINCR_ID();
		}
	}


	protected void convocateElection(QueueSession session) throws JMSException
	{
		int start = Integer.parseInt(this.getCLIENT_ID()) + 1;
		int end = GenericClient.getINCR_ID();

		System.out.println("I'm the process with ID-" + this.getCLIENT_ID() 
				+ " start: " + start + ", end: " + end);

		for (int i = start; i < end; i++) 
		{
			System.out.println("I'm the process with ID-" + this.getCLIENT_ID() 
				+ " and I'm convocating an election to client-" + i);

			// Communication settings: the client-broker sends messages to other peers
			Destination peerQueue  = session.createQueue(Integer.toString(i));
			MessageProducer producer = session.createProducer(peerQueue);
			this.myQueue.sendRequest(producer, "Election convocated.", GenericClient.ELECTION_REQ, this.getCLIENT_ID());
		}

		int count = end - start;

		while(count > 0)
		{
			// the client waits for request
			Message reply = this.myQueue.getConsumer().receive(2000);
			
			if(reply == null)
			{
				System.out.println("Client is DEAD!");
			}
			else if(reply.getJMSType().compareTo(GenericClient.ELECTION_ACK) == 0)
			{
				System.out.println("(C-" + this.getCLIENT_ID() + ") ID: " 
					+ ((TextMessage) reply).getText());
			}

			count--;
		}

		System.out.println("ESCO!");
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
				this.myQueue.sendRequest(producer, "Client ID request message.", GenericClient.ID_REQUEST, this.getCLIENT_ID());

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
				
				if(msg.getJMSType().compareTo(GenericClient.ELECTION_REQ) == 0)
				{
					System.out.println("(C-" + this.getCLIENT_ID() + ") ID: " 
						+ ((TextMessage) msg).getText());

					this.myQueue.sendResponse(msg, "Client-" + this.getCLIENT_ID() + " ACK", GenericClient.ELECTION_ACK);

					this.convocateElection(session);
				}
			}
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

	/* 	protected void coordinatorOperations(QueueSession session) throws JMSException
	{
		if(this.getMyState() == GenericClient.State.COORDINATOR)
		{
			// Communication settings: the client/broker/coordinator creates a queue used by any client
			Queue queue = session.createQueue(GenericClient.BROKER_QUEUE_NAME);
			QueueReceiver receiver = session.createReceiver(queue);

			while(true)
			{
				// The server waits for requests by any client
				Message request = receiver.receive();
				
				System.out.println("Message: " + ((TextMessage) request).getText() 
					+ " ReplyTo: " + request.getJMSReplyTo() + "\n");
				
				if(request.getJMSType().compareTo(ID_REQUEST) == 0) // this.clientType() == "coordinator" && 
				{
					System.out.println("ID_REQUEST.");

					MessageProducer producer = session.createProducer(null);
					TextMessage reply = session.createTextMessage();
					reply.setText(Integer.toString(INCR_ID));
					reply.setJMSType(ID_RESPONSE);
					reply.setJMSCorrelationID(request.getJMSCorrelationID());
					producer.send(request.getJMSReplyTo(), reply);
					this.setINCR_ID();
				}
				else if(request.getJMSType().compareTo(RESOURCE_REQUEST) == 0)
				{
					// to-do
				}
				else if(request.getJMSType().compareTo(ELECTION) == 0)
				{
					// act as a normal peer
					break;
				}
				else
				{
					System.out.println("Another request.");
					break;
				}
			}
		}
	} */


	/* public void body()
	{
		ActiveMQConnection connection = null;

		try
		{
			// Initialization settings
			ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory(GenericClient.BROKER_URL);
			connection = (ActiveMQConnection) cf.createConnection();
			connection.start();
			QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);


			// GENERIC OPERATIONS
			while(true)
			{
				if(getMyRole() == Role.COORDINATOR)
				{
					// Communication settings: the client/broker/coordinator creates a queue used by any client
					Queue coordQueue = session.createQueue(GenericClient.BROKER_QUEUE_NAME);
					QueueReceiver coordReceiver = session.createReceiver(coordQueue);

					while(true)
					{
						// The coordinator waits for requests by any client
						Message request = coordReceiver.receive();

						System.out.println("Message: " + ((TextMessage) request).getText() 
								+ " ReplyTo: " + request.getJMSReplyTo());

						if(request.getJMSType().compareTo(ID_REQUEST) == 0)
						{
							System.out.println("ID_REQUEST.");

							MessageProducer producer = session.createProducer(null);
							TextMessage reply = session.createTextMessage();
							reply.setText(Integer.toString(INCR_ID));
							reply.setJMSType(ID_RESPONSE);
							reply.setJMSCorrelationID(request.getJMSCorrelationID());
							producer.send(request.getJMSReplyTo(), reply);
							this.setINCR_ID();
						}
						else if(request.getJMSType().compareTo(RESOURCE_REQUEST) == 0)
						{
							// to-do
						}
						else if(request.getJMSType().compareTo(ELECTION) == 0)
						{
							// act as a normal peer
							break;
						}
						else
						{
							System.out.println("Another request.");
							break;
						}
					}
				} // end if - Role.COORDINATOR

				else if(getMyRole() == Role.CLIENT)
				{
					// Communication settings: the client sends messages to server
					Destination serverQueue  = session.createQueue(GenericClient.BROKER_QUEUE_NAME);
					MessageProducer producer = session.createProducer(serverQueue);

					// Communication settings: the client defines an endpoint used by the server to reply
					Destination tempDest = session.createTemporaryQueue();
					MessageConsumer consumer = session.createConsumer(tempDest);

					// firstly the client get ID
					while(true)
					{
						// The client creates the request
						TextMessage request = session.createTextMessage();
						request.setText("Client ID request message.");
						request.setJMSReplyTo(tempDest);
						request.setJMSType(ID_REQUEST);
						request.setJMSCorrelationID("123");	
						producer.send(request);

						// the client waits for request
						Message reply = consumer.receive();	

						if(reply.getJMSType().compareTo(ID_RESPONSE) == 0)
						{
							this.setCLIENT_ID(Integer.parseInt(((TextMessage) reply).getText()));
							System.out.println("(C-" + this.getCLIENT_ID() + ") ID: " + ((TextMessage) reply).getText());
							break;
						}
					}

					// once client has the ID can communicate with other peers
					Queue clientQueue = session.createQueue(Integer.toString(this.CLIENT_ID));
					QueueReceiver clientReceiver = session.createReceiver(clientQueue);

					// communication phase
					while(true)
					{
						System.out.println("CIAONE");
						Thread.sleep(3000);

						// if (this.getMyRole() == )
						// {
						// 	// act as a coordinator
						// }
						// else
						// {
						// 	// act as a peer
						// 	// resource request
						// 	// other operations
						// }				
					}		

				} // end if - Role.CLIENT

			} // while true client generic operations




		}		
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (connection != null)
			{
				try
				{
					connection.close();
				}
				catch (JMSException e)
				{
					e.printStackTrace();
				}
			}
		}		
	}*/

}
