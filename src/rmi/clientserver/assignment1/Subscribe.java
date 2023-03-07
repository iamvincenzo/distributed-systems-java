package rmi.clientserver.assignment1;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * 
 * Interface {@code Subscribe} to allow the client to subscribe its remote object.
 *
 **/
public interface Subscribe extends Remote 
{
	/**
	 * 
	 * Method used by the client to subscribe its remote object.
	 * 
	 * @param w PriceWriterReader object to add
	 * @throws RemoteException It handles exceptions that may occur during execution of a remote method call.
	 * 
	 **/
	void subscribe (final PriceWriterReader w) throws RemoteException;
	
	/**
	 * 
	 * Method used by the server to unsubscribe client's remote object.
	 *
	 * @param w PriceWriterReader object to remove
	 * @throws RemoteException It handles exceptions that may occur during execution of a remote method call.
	 * 
	 **/
	void unsubscribe (final PriceWriterReader w) throws RemoteException;
}
