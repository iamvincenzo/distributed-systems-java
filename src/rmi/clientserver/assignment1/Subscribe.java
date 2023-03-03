package rmi.clientserver.assignment1;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Subscribe extends Remote {
	void subscribe (final PriceWriterReader w) throws RemoteException;
}
