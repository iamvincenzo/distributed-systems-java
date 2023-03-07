package rmi.clientserver.assignment1;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Set;

/**
 * 
 * Implementation of {@code Subscribe} interface.
 * 
 **/
public class SubscribeImpl extends UnicastRemoteObject implements Subscribe{

	/**
	 * 
	 * writers Set that contains all clients' remote objects.
	 * 
	 **/
	private static final long serialVersionUID = 1L;
	private Set<PriceWriterReader> writers;

	/**
	 * 
	 * Class constructor.
	 * 
	 * @param s set of PriceWriterReader
	 * @throws RemoteException It handles exceptions that may occur during execution of a remote method call.
	 * 
	 **/
	public SubscribeImpl(final Set<PriceWriterReader> s) throws RemoteException {
		this.writers = s;
	}

	/**
	 * 
	 * {@inheritDoc}
	 * 
	 **/
	@Override 
	public void subscribe(PriceWriterReader w) throws RemoteException {
		this.writers.add(w);
		 
	}
	
	/**
	 * 
	 * {@inheritDoc}
	 * 
	 **/
	@Override
	public void unsubscribe(PriceWriterReader w) throws RemoteException {
		this.writers.remove(w);
		
	}
	
	/**
	 * 
	 * {@inheritDoc}
	 * 
	 **/
	@Override
	public boolean clientIsInSet(PriceWriterReader w) throws RemoteException {
		return this.writers.contains(w);
	}
}
