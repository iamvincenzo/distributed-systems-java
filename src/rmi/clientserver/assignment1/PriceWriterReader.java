package rmi.clientserver.assignment1;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * 
 * Interface {@code PriceWriterReader} of the remote object used by clients and server to communicate their
 * offers and prices.
 *
 **/
public interface PriceWriterReader extends Remote {
	
	/**
	 * 
	 * Method used by the server to send prices to the client.
	 * 
	 * @param p price sent.
	 * @throws RemoteException It handles exceptions that may occur during execution of a remote method call.
	 * 
	 **/
	void putPrice(final int p) throws RemoteException;
	
	/**
	 * 
	 * Method used by the client to send its offer to the server.
	 * 
	 * @param p client's offer 
	 * @throws RemoteException It handles exceptions that may occur during execution of a remote method call.
	 * 
	 **/
	void putOffer(final int p) throws RemoteException; 
	
	/**
	 * 
	 * Method used by the client to read the server's price.
	 * 
	 * @return server's price
	 * @throws RemoteException It handles exceptions that may occur during execution of a remote method call.
	 * 
	 **/
	int getPrice() throws RemoteException;
	
	/**
	 * Method used by the server to read client's offer.
	 * 
	 * @return client's offer
	 * @throws RemoteException It handles exceptions that may occur during execution of a remote method call.
	 * 
	 **/
	int getOffer() throws RemoteException;
	
	/**
	 * 
	 * Method used by the server to increment the number of client's purchases whenever
	 * its offer is valid.
	 * 
	 * @throws RemoteException It handles exceptions that may occur during execution of a remote method call.
	 * 
	 **/
	void clientHasPurchased() throws RemoteException;
	
	/**
	 * 
	 * Method used to check how many purchases the client has successfully completed.
	 * @return number of client's purchases
	 * @throws RemoteException It handles exceptions that may occur during execution of a remote method call.
	 * 
	 **/
	int getPurchases() throws RemoteException;
	
	/**
	 * Method used to check the client's state.
	 * State is 0 when then client wants to make an offer.
	 * State is 1 when the purchase is successful.
	 * State is -1 when the purchase fails.
	 * 
	 * @return client's state
	 * @throws RemoteException It handles exceptions that may occur during execution of a remote method call.
	 * 
	 **/
	int getState()throws RemoteException;
	
	/**
	 * Method used to update the client's state.
	 * 
	 * @param s new client's state
	 * @throws RemoteException It handles exceptions that may occur during execution of a remote method call.
	 * 
	 **/
	void setState(final int s)throws RemoteException;
}
