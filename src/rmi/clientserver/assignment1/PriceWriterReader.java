package rmi.clientserver.assignment1;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PriceWriterReader extends Remote {
	void putPrice(final int p) throws RemoteException;
	void putOffer(final int p) throws RemoteException; 
	int getPrice() throws RemoteException;
	int getOffer() throws RemoteException;
	void clientHasPurchased() throws RemoteException;
	int getPurchases() throws RemoteException;
	int getState()throws RemoteException;
	void setState(final int s)throws RemoteException;
}
