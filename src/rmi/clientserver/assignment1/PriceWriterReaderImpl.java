package rmi.clientserver.assignment1;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class PriceWriterReaderImpl extends UnicastRemoteObject implements PriceWriterReader {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int serverPrice = 0;
	private int clientPrice = 0;
	private int purchases = 0;
	private int state = -1;

	public PriceWriterReaderImpl() throws RemoteException {
	}

	@Override
	public void putPrice(final int p) throws RemoteException {
		this.serverPrice = p;
	}
	
	@Override 
	public void putOffer(final int p) throws RemoteException {
		this.clientPrice = p;
	}
	
	@Override
	public int getPrice() throws RemoteException {
		return this.serverPrice;
	}
	
	@Override
	public int getOffer() throws RemoteException {
		return this.clientPrice;
	}

	@Override
	public void clientHasPurchased() throws RemoteException {
		this.purchases += 1;
		
	}
	
	@Override
	public int getPurchases() throws RemoteException {
		return this.purchases;
		
	}

	@Override
	public int getState() throws RemoteException {
		return this.state;
	}

	@Override
	public void setState(int s) throws RemoteException {
		this.state = s;
	}
	



}
