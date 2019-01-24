import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;

public class NotifyInviteImpl extends RemoteObject implements NotifyInviteInterface{

	private static final long serialVersionUID = 1L;
	
	public NotifyInviteImpl() throws RemoteException{
		super();
	}

	@Override
	public void notifyInvite(String newDocument) throws RemoteException {
		
		System.out.println("L'utente è stato invitato a partecipare alla modifica del file " + newDocument);
	}

}
