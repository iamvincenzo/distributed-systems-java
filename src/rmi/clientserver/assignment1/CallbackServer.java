/**
 * 
 * @author giorgia.tedaldi@studenti.unipr.it 339642
 * @author vincenzo.fraello@studenti.unipr.it 339641
 *
 **/

package rmi.clientserver.assignment1;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 
 * The class {@code CallbackServer} manages the interaction between client and server.
 *
 **/
public class CallbackServer {

	/**
	 * 
	 * Class fields'.
	 * 
	 * PORT port's number on which server is listening.
	 * MAX is the max number that can be generated as offer.
	 * MIN is the min number that can be generated as offer.
	 * CONNECTEDCLIENTS number of clients which has to be connected to start the purchase.
	 * 
	 **/
	private static final int PORT = 1099;
	private static final int MAX = 200;
	private static final int MIN = 10;
	private static final int CONNECTEDCLIENTS = 3;

	/**
	 * 
	 * Starts the server.
	 * The server creates an empty Set of @see PriceWriterReaderImpl where the clients can subscribe their
	 * remote objects to communicate with server.
	 * The server waits until 3 clients are connected, then he creates a random offer between MIN and MAX and sends it 
	 * to all clients and periodically updates it.
	 * The server sells the object to the client if its offer is greater than the current price. 
	 * When all the clients have purchased at least 10 objects the server terminates.
	 *
	 * @param args  The method does not requires arguments.
	 * @throws Exception It indicates if a problem occurred.
	 */
	public static void main(String[] args) throws Exception {
		Random random = new Random();
		Registry registry = LocateRegistry.createRegistry(PORT);
		Set<PriceWriterReader> clientList = new CopyOnWriteArraySet<>();
		Subscribe service = new SubscribeImpl(clientList);
		registry.rebind("subscribe", service);
		System.out.println("Server is listening on PORT: " + PORT);
		boolean isStarted = false;
		
		while (true)
		{
			if (clientList.size() >= CONNECTEDCLIENTS || isStarted)
			{
				/**
				 * 
				 * isStarted variable handles the case in which there are less
				 * connected clients (that have to complete their purchases) than 3.
				 * 
				 **/
				isStarted = true;
				int currentPrice = random.nextInt(MAX - MIN) + MIN;
				
				try 
				{	
					for (PriceWriterReader w: clientList)
					{
						if(w.getPurchases() >= 10)
						{
							service.unsubscribe(w);
						}
					}

					System.out.println("clientList size " + clientList.size());


					if(clientList.size() == 0)
					{
						break;
					}

					for (PriceWriterReader w: clientList) 
					{
						w.putPrice(currentPrice);	
					}

					Thread.sleep(3000);

					for (PriceWriterReader w: clientList) 
					{
						int clientPrice = w.getOffer();
						
						if (w.getOffer() > currentPrice && w.getState() == 0) //Purchase happened successfully.
						{
							w.clientHasPurchased();
							w.setState(1);
							System.out.println("You get the object: the current price is " + currentPrice + " and you offered " + clientPrice);
							System.out.println("Total purchases: " + w.getPurchases());

						}
						else if (w.getOffer() <= currentPrice && w.getState() == 0) //Purchase failed.
						{
							w.setState(-1);
							System.out.println("Your offer is no longer valid for this object: the current price is: " + currentPrice + " and you offered " + clientPrice);
						}						

					}

				}
				catch (Exception e) 
				{
					continue;
				} 

			}			
		}

		System.exit(0);

	}

}
