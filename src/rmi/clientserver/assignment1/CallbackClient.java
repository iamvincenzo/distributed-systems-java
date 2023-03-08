/**
 * 
 * @author giorgia.tedaldi@studenti.unipr.it 339642
 * @author vincenzo.fraello@studenti.unipr.it 339641
 *
 **/

package rmi.clientserver.assignment1;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;

/**
*
* The class {@code CallbackClient} manages the interaction of a client with the
* server.
*
**/
public class CallbackClient 
{
	
	/**
	 * 
	 * Class fields'.
	 * 
	 * MAX is the max number that can be generated as offer.
	 * MIN is the min number that can be generated as offer.
	 * 
	 **/
	private static final int MAX = 200;
	private static final int MIN = 10;
	
	  /**
	   * 
	   * Starts the client.
	   * The client creates a new instance of @see PriceWriterReaderImpl through which he will
	   * communicate with the server.
	   * The client creates a random offer between MIN and MAX and if the offer
	   * is greater than the server's price he sends the purchase request to the server.
	   * The client waits for the server response. When he has purchased at least 10 objects
	   * he will terminate.
	   *
	   * @param args  The method does not requires arguments.
	   * @throws Exception It indicates if a problem occurred.
	   *
	  **/
	public static void main(String[] args) throws Exception 
	{
		Random random = new Random();
		Registry registry = LocateRegistry.getRegistry();
		PriceWriterReader w = new PriceWriterReaderImpl();
		Subscribe service = (Subscribe) registry.lookup("subscribe");
		service.subscribe(w);
		
		while(w.getPurchases() < 10)
		{
			int offer = random.nextInt(MAX - MIN) + MIN;
			
			if (offer > w.getPrice() && w.getPrice() > 0)
			{
				System.out.println("Client is making an offer  " + offer + " for serverPrice " + w.getPrice());
				w.setState(0);
				w.putOffer(offer); 
				 
				/*Synchronization between client and server. If conditions are matched
				client sends its offer to the server and waits for its response.*/
				boolean busyWaiting = true;
				while(busyWaiting)
				{
					int state = w.getState();
					if (state > 0)
					{
						System.out.println("Client has bought " + w.getPurchases() + " objects");
						break;
					} 
					else if (state < 0)
					{
						System.out.println("Couldn't buy the object");
						break;
					}
				}
			}
			else if (w.getPrice() > 0)
			{
				System.out.println("Client can't make an offer. serverPrice is " + w.getPrice() + " and offer is " + offer);
			}		
		}
		
		service.unsubscribe(w);
		UnicastRemoteObject.unexportObject(w, true);	 
	}
}