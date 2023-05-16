package jms.mutualexclusion.assignment3;

import java.util.Scanner;

import javax.jms.JMSException;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.BrokerFactory;


/**
 * This class is used to run a single 
 * node and a broker in the system.
 * 
 * @author Vincenzo Fraello (339641)
 * @author Giorgia Tedaldi (339642)
 */

public class ClientBroker extends GenericClient
{
	/**
	 * The class ClientBroker is a GenericCLient's sublcass. Initiates a client that during the
	 * entire process has also the Broker task. The broker assigns all the IDs to the other nodes.
	 */
	public void body()
	{
		try
		{
			/* Broker initialization settings */
			BrokerService broker = BrokerFactory.createBroker("broker:(" + BROKER_URL + ")?" + BROKER_PROPS);
			broker.start();

			/* initialization settings */
			this.createSession();

			/* getting the initial state */
			try (Scanner myObj = new Scanner(System.in)) 
			{
				while(true)
				{
					/* Manual state setting. */
					// System.out.print("Enter initial state (I/C): ");
					// String state = myObj.nextLine(); 
					
					/* Automatic state setting. */
					String state = "c";

					if(state.toLowerCase().compareTo("i") == 0)
					{
						/* setting the state */
						this.setMyState(GenericClient.State.IDLE);
						break;
					}
					else if(state.toLowerCase().compareTo("c") == 0)
					{
						/* setting the state */
						this.setMyState(GenericClient.State.CANDIDATE);
						break;
					}
					else
					{
						System.out.println("Entered state not valid. Retry with (I/C)!");
					}
				}
			}

			/* Assumption: the Broker id is 0 and it is self-assigned.*/
			this.setCLIENT_ID(Integer.parseInt(GenericClient.BROKER_QUEUE_NAME));

			/* Broker creates its queue with id 0 */
			this.myQueue = new SendReceiverQueue(this.getSession());
			this.myQueue.createQueue(this.getCLIENT_ID(), 0);

			/* Broker assigns id to other clients in a increasing way. When N clients
			 * are connected the service starts. */
			while(GenericClient.getINCR_ID() <= GenericClient.N_CONNECTED) 
			{
				this.idAssignment(this.myQueue.getQueueReceiver());
			}

			System.out.println("All N-1 ID have been assigned!");
			this.clientOperations();			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (this.getConnection() != null)
			{
				try
				{
					this.getConnection().close();
				}
				catch (JMSException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Main method used to run the simulation.
	 * 
	 * @param args Parameter is used to pass any command-line arguments 
	 * 				to the application during execution.
	 * @throws InterruptedException Thrown when a running thread is interrupted asynchronously
	 */
	public static void main(final String[] args) throws InterruptedException
	{
		new ClientBroker().body();				
	}
}
