package jms.mutualexclusion.assignment3;

import java.util.Scanner;

import javax.jms.JMSException;


/**
 * 
 */
public class Client extends GenericClient
{
	/**
	 * 
	 */
	public void body()
	{
		try
		{			
			/* initialization settings */
			this.createSession();

			/* getting the initial state */
			try (Scanner myObj = new Scanner(System.in)) 
			{
				while(true)
				{
					// System.out.print("Enter initial state (I/C): ");
					// String state = myObj.nextLine(); 

					String state = "i";

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
	 * 
	 * @param args
	 * @throws InterruptedException
	 */
	public static void main(final String[] args) throws InterruptedException
	{
		new Client().body();
	}
}
