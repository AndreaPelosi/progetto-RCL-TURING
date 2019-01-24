import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;

public class RegistrationImpl extends UnicastRemoteObject implements Registration{
	
	
	private static final long serialVersionUID = 1L;
	private ConcurrentHashMap<String, Utente> utenti;
	private static ConcurrentHashMap<String, NotifyInviteInterface> clients;
	
	protected RegistrationImpl(ConcurrentHashMap<String, Utente> utenti) throws RemoteException {
		super();
		this.utenti = utenti;
		RegistrationImpl.clients = new ConcurrentHashMap<>();
		
	}

	@Override
	public boolean register(String username, String password) throws RemoteException {

		synchronized(utenti) {

			if (username.equals("") || password.equals(""))
				return false;
			
			if (utenti.containsKey(username)) //un utente con lo stesso username è già registrato al servizio
				return false;
			
			
			utenti.put(username, new Utente(password)); //l'utente viene inserito nella Map con successo
			System.out.println("L'utente " + username + " è stato registrato con successo");
			return true;
		}
	}

	@Override
	public void registerForCallback(String Username, NotifyInviteInterface ClientInterface) throws RemoteException {

		clients.put(Username, ClientInterface);
		System.out.println("utente " + Username + " registrato al meccanismo di callback");

	}
	
	public void unregisterFromCallback(String Username) throws RemoteException{
		
		if (clients.containsKey(Username)) {
			clients.remove(Username);
			System.out.println("utente " + Username + " deregistrato dal meccanismo di callback");
		}
	}

	
	public static void notifyOnlineInvite(String userInvited, String newDocument) {
		doCallback(userInvited, newDocument);
	}
	
	
	private synchronized static void doCallback(String userInvited, String newDocument) {
		
		NotifyInviteInterface client = (NotifyInviteInterface)clients.get(userInvited);
		
		try {
			client.notifyInvite(newDocument);
		} catch (RemoteException e) {
			System.out.println("Errore nella notifica di un invito all'utente" + userInvited);
		}
	}

}
