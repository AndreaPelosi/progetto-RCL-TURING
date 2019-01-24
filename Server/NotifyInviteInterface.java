import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NotifyInviteInterface extends Remote{

	public void notifyInvite(String filename) throws RemoteException;
}
