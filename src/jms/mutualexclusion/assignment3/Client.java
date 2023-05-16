package jms.mutualexclusion.assignment3;

import java.util.Scanner;

import javax.jms.JMSException;

/**
 * This class is used to run a single 
 * node in the system.
 * 
 * @author Vincenzo Fraello (339641)
 * @author Giorgia Tedaldi (339642)
 */

public class Client extends GenericClient
{
	/**
	 * Main mehtod used to run the simulation.
	 */
	public void body()
	{
		try
		{			
			/* Initialization settings. */
			this.createSession();

			/* Getting the initial state. */
			try (Scanner myObj = new Scanner(System.in)) 
			{
				while(true)
				{
					/* Manual state setting. */
					// System.out.print("Enter initial state (I/C): ");
					// String state = myObj.nextLine(); 

					/* Automatic state setting. */
					String state = "i";

					if(state.toLowerCase().compareTo("i") == 0)
					{
						/* Setting the state */
						this.setMyState(GenericClient.State.IDLE);
						break;
					}
					else if(state.toLowerCase().compareTo("c") == 0)
					{
						/* Setting the state */
						this.setMyState(GenericClient.State.CANDIDATE);
						break;
					}
					else
					{
						System.out.println("Entered state not valid. Retry with (I/C)!");
					}
				}
			}
			
			/* Node behaviour depending on its state. */ 
			this.clientOperations();
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
		new Client().body();
	}
}
