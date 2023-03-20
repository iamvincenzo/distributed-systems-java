package jms.mutualexclusion.assignment3;

import javax.jms.Session;
import javax.jms.JMSException;
import javax.jms.QueueSession;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;


public class Client extends GenericClient
{
	public void send()
	{
		ActiveMQConnection connection = null;

		try
		{			
			// Initialization settings
			ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory(GenericClient.BROKER_URL);
			connection = (ActiveMQConnection) cf.createConnection();
			connection.start();
			QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

			// setting the state
			this.setMyState(GenericClient.State.IDLE);

			/////////////// NOT SURE FOR THE FOLLOWING OPERATIONS ///////////////			
			this.clientOperations(session);
			////////////////////////////////////////////////////////////////////
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

	public static void main(final String[] args) throws InterruptedException
	{
		new Client().send();
	}
}
