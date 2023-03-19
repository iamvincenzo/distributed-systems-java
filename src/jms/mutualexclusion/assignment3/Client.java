package jms.mutualexclusion.assignment3;

import javax.jms.Session;
import javax.jms.JMSException;
import javax.jms.QueueSession;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;


public class Client extends GenericClient
{
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

           	// setting the role
		   	this.setMyRole(GenericClient.Role.CLIENT);

		   	this.clientOperations(session);
		}
		catch (JMSException e)
		{
			e.printStackTrace();
		} 
		catch (InterruptedException e) 
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
		final int n = 3;

		new Client().send(n);
	}
}
