/**
 * 
 * @author giorgia.tedaldi@studenti.unipr.it 339642
 * @author vincenzo.fraello@studenti.unipr.it 339641
 *
 **/
package socket.clientserver.assignment2;


import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Random;

/**
 *
 * The class {@code Client} defines a client that sends an object
 * to a server and receives its answer.
 *
 **/

public class Client
{
	private static final int SPORT = 4445;
	private static final String SHOST = "localhost";
	private static final int MAX = 200;
	private static final int MIN = 10;
	private int purchases = 0;

	/**
	 * 
	 * Runs the client's code.
	 * The client wait for a @see Price from server and generates a random @see Offer.
	 * If offer is greater than price he can make a purchase request, than he waits for
	 * server's response (successfully purchased the object or not).
	 * If the client cannot make the purchase request he sends an offer with a 0 value.
	 * When the client reaches 10 purchases he sends an offer with a -1 value to the server.
	 *
	 **/
	public void run()
	{
		try
		{
			Socket client = new Socket(SHOST, SPORT);

			ObjectOutputStream os = new ObjectOutputStream(client.getOutputStream());
			ObjectInputStream  is = null;
			Random r = new Random();

			while (true)
			{
				
				if (is == null)
				{
					is = new ObjectInputStream(new BufferedInputStream(client.getInputStream()));
				}
				
				//Client reads server price
				Object o = is.readObject();
				
				if (o instanceof Price)
				{
					//If client purchases are greater than 10 he will close connection
					if (getPurchases() >= 10) 
					{  
						//Sends -1 to warn serverThred that he is closing the connection
						Offer endOffer = new Offer(-1);
						os.writeObject(endOffer);
						break;
					}
					
					Price serverPrice = (Price) o;
					System.out.format("Client received %s from Server%n", serverPrice.getValue());

					//Sets its offer
					Offer offer = new Offer(r.nextInt(MAX-MIN)+MIN);
					
					if (offer.getValue() > serverPrice.getValue())
					{
						System.out.format("Client is making its offer %s to Server\n", offer.getValue());
						os.writeObject(offer);
						os.flush();  

						//Waits for server's response to know if purchase is successfully done
						Object a = is.readObject();

						if (a instanceof Acknowledgement)
						{
							Acknowledgement ack = (Acknowledgement) a;
							if(ack.getValue())
							{
								incrementPurchases();
								System.out.println("Client has purchased " + getPurchases() + " objects");
							}
							else
							{
								System.out.println("Couldn't purchase the object");
							}
						}
					}
					else 
					{
						//Cannot afford the object
						Offer noOffer = new Offer(0);
						os.writeObject(noOffer);
						System.out.println("Client can't make an offer.");
					}
				}
			}
			client.close();
		} 
		catch (IOException | ClassNotFoundException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * Method to increment client's purchases.
	 * 
	 **/
	private void incrementPurchases()
	{
		this.purchases += 1;
	}

	/**
	 * 
	 * Method to get client's purchases.
	 * 
	 * @return purchases
	 * 
	 **/
	private int getPurchases()
	{
		return this.purchases;
	}

	/**
	 * 
	 * Starts the demo.
	 *
	 * @param args  the method does not requires arguments.
	 *
	 **/
	public static void main(final String[] args)
	{
		new Client().run();
	}
}