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

			/**
			 * 
			 * Phase 1: ids assignment
			 * 
			 */

			// the broker assign itself the first ID
			// setting the state
			this.setMyState(GenericClient.State.CANDIDATE);
			this.setCLIENT_ID(Integer.parseInt(GenericClient.BROKER_QUEUE_NAME));
			GenericClient.setINCR_ID();

			// Communication settings: the client-broker creates a queue used by 
			// any client to send an ID assignment request

			this.myQueue = new SendReceiverQueue(session);
			this.myQueue.createQueue(this.getCLIENT_ID());

			// then assign id to other clients
			while(GenericClient.getINCR_ID() <= GenericClient.N_CONNECTED) 
			{
				this.idAssignment(session, this.myQueue.getQueueReceiver());
			}

			System.out.println("All N-1 id have been assigned!");

			// TO-DO : delete queue with name BROKER_QUEUE_NAME ???

			// convocate an election
			this.convocateElection(session);

			while(true)
			{
				System.out.println("Ho inviato tutti i messaggi di elezione.");
				Thread.sleep(10000);
			}


			/////////////// NOT SURE FOR THE FOLLOWING OPERATIONS ///////////////			
            // this.setMyRole(GenericClient.Role.COORDINATOR);
            // this.coordinatorOperations(session);
			///////////////////////////////////////////////////////////////////
			
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

	public static void main(final String[] args) throws InterruptedException
	{
		new ClientBroker().receive();				
	}
}
