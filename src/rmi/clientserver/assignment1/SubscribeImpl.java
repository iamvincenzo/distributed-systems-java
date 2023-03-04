package rmi.clientserver.assignment1;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Set;

public class SubscribeImpl extends UnicastRemoteObject implements Subscribe{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Set<PriceWriterReader> writers;

	public SubscribeImpl(final Set<PriceWriterReader> s) throws RemoteException {
		this.writers = s;
	}

	@Override 
	public void subscribe(PriceWriterReader w) throws RemoteException {
		this.writers.add(w);
		
	}

	@Override
	public void unsubscribe(PriceWriterReader w) throws RemoteException {
		this.writers.remove(w);
		
	}

	@Override
	public boolean clientIsInSet(PriceWriterReader w) throws RemoteException {
		return this.writers.contains(w);
	}
	
	
	

}
