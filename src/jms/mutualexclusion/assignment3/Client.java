package tmp.vince;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

/**
 *
 * Class providing an implementation of a client that sends a request
 * and waits for an answer.
 *
 **/

public class Client
{
	private static final String BROKER_URL = "tcp://localhost:61616";
	private static final String QUEUE_NAME = "clientBroker";
	private static final String ID_REQUEST = "ID_REQUEST";
	private static final String ID_RESPONSE = "ID_RESPONDE";
	private int CLIENT_ID = 0;

	/**
	 * Sends a sequence of messages.
	 *
	 * @param n  the number of messages.
	 *
	 **/
	@SuppressWarnings("unused")
	public void send(final int n)
	{
		ActiveMQConnection connection = null;

		try
		{			
			// Initialization settings
			ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory(Client.BROKER_URL);
			connection = (ActiveMQConnection) cf.createConnection();
			connection.start();
			QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			
			// Communication settings: the client sends messages to server
			Destination serverQueue  = session.createQueue(QUEUE_NAME);
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
			Queue queue = session.createQueue(Integer.toString(this.CLIENT_ID));
			QueueReceiver receiver = session.createReceiver(queue);
			
			// communication phase
			while(true)
			{
				if (this.clientType() == "coordinator")
				{
					// act as a coordinator
				}
				else
				{
					// act as a peer
					// resource request
					// other operations
				}
			}
		}
		catch (JMSException e)
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
	}


	
	private String clientType() {
		// TODO Auto-generated method stub
		return null;
	}



	/**
	 * Starts the client.
	 *
	 * @param args
	 *
	 * It does not need arguments.
	 *
	 **/
	public static void main(final String[] args)
	{
		final int n = 3;

		new Client().send(n);
	}

	/***
	 * 
	 * This method is used to get the client ID that is also the queue name.
	 *
	 */
	public int getCLIENT_ID() 
	{
		return this.CLIENT_ID;
	}

	/***
	 * 
	 * This method is used to set the client ID that is also the queue name.
	 * 
	 * @param id The ID to set
	 *
	 */
	public void setCLIENT_ID(int cLIENT_ID) 
	{
		this.CLIENT_ID = cLIENT_ID;
	}
}
