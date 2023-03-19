package jms.mutualexclusion.assignment3;


import javax.jms.Session;
import javax.jms.QueueSession;
import javax.jms.JMSException;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.ActiveMQConnectionFactory;



/**
 * 
 * This is the first client when the system is launched at the beginning
 *
 **/
public class ClientBroker extends GenericClient
{	
	public void receive()
	{
		ActiveMQConnection connection = null;
		
		try
		{
			// Broker initialization settings
			BrokerService broker = BrokerFactory.createBroker("broker:(" + BROKER_URL + ")?" + BROKER_PROPS);
			broker.start();

			// Initialization settings
			ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory(ClientBroker.BROKER_URL);
			connection = (ActiveMQConnection) cf.createConnection();
			connection.start();
			QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

            // setting the role
            this.setMyRole(GenericClient.Role.COORDINATOR);

            this.coordinatorOperations(session);
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

	public static void main(final String[] args)
	{
		new ClientBroker().receive();
	}
}
