package tmp.vince;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;

/**
 *
 * Class providing an implementation of a client that accepts and sends requests.
 *
 **/

public class ClientBroker
{
	private static final String BROKER_URL   = "tcp://localhost:61616";
	private static final String BROKER_PROPS = "persistent=false&useJmx=false";
	private static final String QUEUE_NAME   = "clientBroker";
	private static final String ID_REQUEST = "ID_REQUEST";
	private static final String ID_RESPONSE = "ID_RESPONDE";
	private static final String RESOURCE_REQUEST = "RESOURCE_REQUEST";
	private static final String ELECTION = "ELECTION";
	private int INCR_ID = 0;

	/**
	 * 
	 * Receive a request and sends a reply.
	 *
	 **/
	public void receive()
	{
		ActiveMQConnection connection = null;
		
		try
		{
			// Broker initialization settings
			BrokerService broker = BrokerFactory.createBroker("broker:(" + BROKER_URL + ")?" + BROKER_PROPS);
			broker.start();
			/* OPPURE UNICO CLIENT CON GESTIONE DEL BROKER CON UN IF */

			// Initialization settings
			ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory(ClientBroker.BROKER_URL);
			connection = (ActiveMQConnection) cf.createConnection();
			connection.start();
			QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			
			
			// This is the first client when the system is launched at the beginning			
				/* TO-DO State: coordinator. */
				
				/* TO-DO ID: the coordinator assign the ID to other clients. 
				   Each client firstly contacs coordinator by using "clientBroker" queue. */

			// Communication settings: the client/broker/coordinator creates a queue used by any client
			Queue queue = session.createQueue(ClientBroker.QUEUE_NAME);
			QueueReceiver receiver = session.createReceiver(queue);

			while(true)
			{
				// The server waits for requests by any client
				Message request = receiver.receive();
				
				System.out.println("Message: " + ((TextMessage) request).getText() + " ReplyTo: " + request.getJMSReplyTo());
				
				
				if(this.clientType() == "coordinator" && request.getJMSType().compareTo(ID_REQUEST) == 0)
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
				else if(request.getJMSType().compareTo(RESOURCE_REQUEST) == 0)
				{
					// to-do
				}
				else if(request.getJMSType().compareTo(ELECTION) == 0)
				{
					// act as a normal peer
				}
				else
				{
					System.out.println("Another request.");
					break;
				}
			}
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
	}

	private String clientType() {
		// TODO Auto-generated method stub
		return null;
	}

	private void setINCR_ID() 
	{
		this.INCR_ID++;	
	}

	/**
	 * Starts the server.
	 *
	 * @param args
	 *
	 * It does not need arguments.
	 *
	 **/
	public static void main(final String[] args)
	{
		new ClientBroker().receive();
	}
}
