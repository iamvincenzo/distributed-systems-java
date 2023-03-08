package socket.clientserver.assignment2;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Random;

/**
 *
 * The class {@code ServerThread} manages the interaction
 * with a client of the server.
 *
 **/
public class ServerThread implements Runnable
{
	private static final int MAX = 200;
	private static final int MIN = 10;
	private static final long SLEEPTIME = 3000;

	private Server server;
	private Socket socket;

	/**
	 * Class constructor.
	 *
	 * @param s  the server.
	 * @param c  the client socket.
	 *
	 **/
	public ServerThread(final Server s, final Socket c)
	{
		this.server = s;
		this.socket = c;
	}
	
	
	/**
	 * 
	 * Starts the ServerThread.
	 * ServerThread creates a price and communicates it to the client than wait for its response.
	 * If client sends back 0 then he couldn't afford the object.
	 * If client sends back -1 he reached 10 purchases so connection has to be closed.
	 * Otherwise the server check the purchase request and its current price. If client's
	 * offer is greater than its price he sends an Acknowledgment with a true value (false otherwise).
	 * 
	 **/
	@Override
	public void run()
	{
		ObjectInputStream  is = null;
		ObjectOutputStream os = null;

		try
		{
			is = new ObjectInputStream(new BufferedInputStream(
					this.socket.getInputStream()));
		}
		catch (Exception e)
		{
			e.printStackTrace();

			return;
		}

		String id = String.valueOf(this.hashCode());
		Random r = new Random();

		while (true)
		{
			try
			{
				//Sends price to client
				if (os == null)
				{
					os = new ObjectOutputStream(new BufferedOutputStream(
							this.socket.getOutputStream()));
				}

				Price price = new Price(r.nextInt(MAX - MIN) + MIN);
				System.out.format("thread %s sends: %s to its client%n", id, price.getValue());
				os.writeObject(price);
				os.flush();

				//Waits for response (client sends 0 if can't make an offer and -1 to close connection)
				Object i = is.readObject();

				if (i instanceof Offer)
				{
					Offer offer = (Offer) i;

					System.out.format("thread %s receives: %s from its client%n", id, offer.getValue());
					Thread.sleep(SLEEPTIME);
					
					Acknowledgement ack = new Acknowledgement();
					
					if (offer.getValue() == -1)
					{
						//Client completed its purchases, connection has to be closed
						if (this.server.getPool().getActiveCount() == 1)
						{
							this.server.close();
						}

						this.socket.close();
						return;
					}
					else if (offer.getValue() == 0)
					{
						//Client couldn't make the offer
						System.out.println("Client couldn't make the offer");
					}
					else if (offer.getValue() > price.getValue())
					{
						//Client successfully purchased the object
						ack.setValue(true);
						System.out.println("Object purchased by client");
					}
					else if (offer.getValue() <= price.getValue())
					{
						//Client's offer is no longer valid
						ack.setValue(false);
						System.out.println("This offer is no longer valid for this object");
					}
					os.writeObject(ack);
					os.flush();
				} 
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.exit(0);
			}
		}
	}
}
