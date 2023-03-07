package rmi.clientserver.assignment1;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * 
 * Implementation of {@code PriceWriterReader} interface.
 * 
 **/
public class PriceWriterReaderImpl extends UnicastRemoteObject implements PriceWriterReader {

	/**
	 * 
	 * serverPrice is the price that the server sends to the client.
	 * clientPrice is the offer from the client.
	 * purchases number of purchases of the client.
	 * state is a variable that indicates client's state.
	 * 
	 **/
	private static final long serialVersionUID = 1L;
	private int serverPrice = 0;
	private int clientPrice = 0;
	private int purchases = 0;
	private int state = -1;
	
	
	/**
	 * 
	 * Class' constructor.
	 * 
	 * @throws RemoteException It handles exceptions that may occur during execution of a remote method call.
	 * 
	 **/
	public PriceWriterReaderImpl() throws RemoteException {
	}

	/**
	 * 
	 * {@inheritDoc}
	 * 
	 **/
	@Override
	public void putPrice(final int p) throws RemoteException {
		this.serverPrice = p;
	}
	
	/**
	 * 
	 * {@inheritDoc}
	 * 
	 **/
	@Override 
	public void putOffer(final int p) throws RemoteException {
		this.clientPrice = p;
	}
	
	/**
	 * 
	 * {@inheritDoc}
	 * 
	 **/
	@Override
	public int getPrice() throws RemoteException {
		return this.serverPrice;
	}
	
	/**
	 * 
	 * {@inheritDoc}
	 * 
	 **/
	@Override
	public int getOffer() throws RemoteException {
		return this.clientPrice;
	}

	/**
	 * 
	 * {@inheritDoc}
	 * 
	 **/
	@Override
	public void clientHasPurchased() throws RemoteException {
		this.purchases += 1;
		
	}
	
	/**
	 * 
	 * {@inheritDoc}
	 * 
	 **/
	@Override
	public int getPurchases() throws RemoteException {
		return this.purchases;
		
	}

	/**
	 * 
	 * {@inheritDoc}
	 * 
	 **/
	@Override
	public int getState() throws RemoteException {
		return this.state;
	}

	/**
	 * 
	 * {@inheritDoc}
	 * 
	 **/
	@Override
	public void setState(int s) throws RemoteException {
		this.state = s;
	}
}
