package jms.mutualexclusion.assignment3;

import java.util.Scanner;

import javax.jms.JMSException;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.BrokerFactory;


/**
 * 
 * This is the first client when the system is launched at the beginning
 *
 **/
public class ClientBroker extends GenericClient
{
	/**
	 * 
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
					// System.out.print("Enter initial state (I/C): ");
					// String state = myObj.nextLine(); 
					 
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

			/* Phase 1: ids assignment */
			this.setCLIENT_ID(Integer.parseInt(GenericClient.BROKER_QUEUE_NAME));

			/* creation of queue with id 0 */
			this.myQueue = new SendReceiverQueue(this.getSession());
			this.myQueue.createQueue(this.getCLIENT_ID(), 0);

			/* then assign id to other clients */
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
	 * 
	 * @param args
	 * @throws InterruptedException
	 */
	public static void main(final String[] args) throws InterruptedException
	{
		new ClientBroker().body();				
	}
}

// convocate an election
			/*this.convocateElection(session);

			int start = Integer.parseInt(this.getCLIENT_ID()) + 1;
			int end = GenericClient.N_CONNECTED;
			int count = end - start;
			@SuppressWarnings("unused")
			int ackReceived = 0;

			while(count >= 0)
			{
				// The server waits for requests by any client
				Message reply = this.myQueue.getQueueReceiver().receive(); //3000

				if(reply == null)
				{
					System.out.println("Client is DEAD!");
				}
				else if(reply.getJMSType().compareTo(GenericClient.ELECTION_ACK) == 0)
				{
					System.out.println("reply to (C-" + this.getCLIENT_ID() + ") ID: " 
						+ ((TextMessage) reply).getText());
					ackReceived++;
				}

				count--;
			}

			while(true)
			{
				// System.out.println("Ho inviato tutti i messaggi di elezione.");
				Thread.sleep(10000);
			}*/
