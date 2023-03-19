package jms.mutualexclusion.assignment3;

import javax.jms.Queue;
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
		IDLE
	}

	protected enum Role
	{
		COORDINATOR,
		CLIENT
	}

	// client-broker parameters
	protected static final String BROKER_URL   = "tcp://localhost:61616";
	protected static final String BROKER_PROPS = "persistent=false&useJmx=false";

	// client-broker-coordinator queue: used by clients to get id
	protected static final String BROKER_QUEUE_NAME   = "clientBroker";

	// different types of message
	protected static final String ID_REQUEST = "ID_REQUEST";
	protected static final String ID_RESPONSE = "ID_RESPONDE";
	protected static final String RESOURCE_REQUEST = "RESOURCE_REQUEST";
	protected static final String ELECTION = "ELECTION";

	// client's state
	protected State myState;

	// client's role
	protected Role myRole;

	// client id used to identify the process and the queue. INCR_ID is used by the coordinator to
	// assign an incremental id to other clients
	protected int INCR_ID = 0;

	// this is the client id
	protected int CLIENT_ID = 0;

	@SuppressWarnings("unused")
	private void setMyState(State state) 
	{
		this.myState = state;		
	}

	@SuppressWarnings("unused")
	private State getState() 
	{
		return this.myState;		
	}


	public Role getMyRole() 
	{
		return this.myRole;
	}

	public void setMyRole(Role myRole) 
	{
		this.myRole = myRole;
	}



	protected void setINCR_ID() 
	{
		this.INCR_ID++;	
	}

	

	public void setCLIENT_ID(int CLIENT_ID) 
	{
		this.CLIENT_ID = CLIENT_ID;
	}

	public int getCLIENT_ID() 
	{
		return this.CLIENT_ID;
	}

	public void coordinatorOperations(QueueSession session) throws JMSException
	{
		if(this.getMyRole() == GenericClient.Role.COORDINATOR)
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
	}


	public void clientOperations(QueueSession session) throws JMSException, InterruptedException
	{
		if(this.getMyRole() == GenericClient.Role.CLIENT)
		{			
			// Communication settings: the client sends messages to server
			Destination serverQueue  = session.createQueue(GenericClient.BROKER_QUEUE_NAME);
			MessageProducer producer = session.createProducer(serverQueue);

			// Communication settings: the client defines an endpoint used by the server to reply
			Destination tempDest = session.createTemporaryQueue();
			MessageConsumer consumer = session.createConsumer(tempDest);// setting the role
			this.setMyRole(GenericClient.Role.COORDINATOR);

			// firstly the client get ID
			while(true)
			{
				// The client creates the request
				TextMessage request = session.createTextMessage();
				request.setText("Client ID request message.");
				request.setJMSReplyTo(tempDest);
				request.setJMSType(Client.ID_REQUEST);
				request.setJMSCorrelationID("123");	// ????
				producer.send(request);

				// the client waits for request
				Message reply = consumer.receive();	

				if(reply.getJMSType().compareTo(ID_RESPONSE) == 0)
				{
					this.setCLIENT_ID(Integer.parseInt(((TextMessage) reply).getText()));
					System.out.println("(C-" + this.getCLIENT_ID() + ") ID: " 
						+ ((TextMessage) reply).getText());
					break;
				}
			}

			// once client has the ID can communicate with other peers
			Queue queue = session.createQueue(Integer.toString(this.getCLIENT_ID()));
			QueueReceiver receiver = session.createReceiver(queue);

			// communication phase
			while(true)
			{
				System.out.println("Sono attivo: " + getCLIENT_ID());
				Thread.sleep(3000);

				// The server waits for requests by any client
				Message request = receiver.receive(3000);
				
				if(request != null)
				{
					System.out.println("Message: " + ((TextMessage) request).getText() + 
					" ReplyTo: " + request.getJMSReplyTo() + "\n");
				}

				else 
				{
					System.out.println("Elapsed 3 s.\n");
				}
				


				//				if (this.clientType() == "coordinator")
				//				{
				//					// act as a coordinator
				//				}
				//				else
				//				{
				//					// act as a peer
				//					// resource request
				//					// other operations
				//				}				
			}
		}
	}

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
