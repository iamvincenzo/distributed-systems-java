package rmi.clientserver.assignment1;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;

public class CallbackClient {
	private static final int MAX = 200;
	private static final int MIN = 10;
	
	public static void main(String[] args) throws Exception {
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
				
				boolean busyWaiting = true;
				while(busyWaiting)
				{
					int state = w.getState();
					//System.out.println("state " + w.getState());
					if (state > 0)
					{
						System.out.println("Client has bought " + w.getPurchases() + " objects");
						//busyWaiting = false;
						break;
					} 
					else if (state < 0)
					{
						System.out.println("Couldn't buy the object");
						//busyWaiting = false;
						break;
					}
				}
			}
			else if (w.getPrice() > 0)
			{
				System.out.println("Client can't make an offer. serverPrice is " + w.getPrice() + " and offer is " + offer);
			}		
		}
		
//		try
//		{
//			service.clientIsInSet(w);
//		}
//		catch (Exception e)
//		{
//			UnicastRemoteObject.unexportObject(w, false);	
//		}

		UnicastRemoteObject.unexportObject(w, false);
		
	}

}
