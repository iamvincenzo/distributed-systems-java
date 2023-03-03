package rmi.clientserver.assignment1;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class CallbackServer {
	private static final int PORT = 1099;
	private static final int MAX = 200;
	private static final int MIN = 10;

	public static void main(String[] args) throws Exception {
		Random random = new Random();
		Registry registry = LocateRegistry.createRegistry(PORT);
		Set<PriceWriterReader> clientList = new CopyOnWriteArraySet<>();
		Subscribe service = new SubscribeImpl(clientList);
		registry.rebind("subscribe", service);
		System.out.println("Server is listening on PORT: " + PORT);
		while (true)
		{
			if (clientList.size() >= 1)
			{
				int currentPrice = random.nextInt(MAX - MIN) + MIN;
				try 
				{
					for (PriceWriterReader w: clientList) 
					{
						w.putPrice(currentPrice);	
					}
					
					Thread.sleep(3000);
					
					for (PriceWriterReader w: clientList) 
					{
						int clientPrice = w.getOffer();
						if (w.getOffer() > currentPrice && w.getState() == 0)
						{
							w.clientHasPurchased();
							System.out.println("You get the object: the current price is " + currentPrice + " and you offered " + clientPrice);
							System.out.println("Total purchases: " + w.getPurchases());
							w.setState(1);
						}
						else if (w.getOffer() <= currentPrice && w.getState() == 0)
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

	}

}
