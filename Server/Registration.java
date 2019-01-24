import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Registration extends Remote{

	public String SERVICE_NAME = "Registration";
	
	/* operazione di registrazione di un client, restituisce il numero della porta su cui è attivo il servizio
	 *  se ha avuto successo, -1 altrimenti */
	public boolean register(String username, String password) throws RemoteException;
	
	public void registerForCallback (String Username, NotifyInviteInterface ClientInterface) throws RemoteException;

	public void unregisterFromCallback(String Username) throws RemoteException;
}
