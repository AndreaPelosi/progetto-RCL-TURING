import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class Utente {

	
	private String password;
	
	private boolean isOnline; //flag che attesta se l'utente è online o meno
	private boolean isEditing; //flag che attesta se l'utente sta modificando un documento o meno
	private ReentrantLock online_lock;
	private ReentrantLock edit_lock;
	
	private CopyOnWriteArrayList<String> createdDocs; //lista dei nomi dei documenti creati
	private CopyOnWriteArrayList<String> docsInvited; //lista dei nomi dei documenti ai quali sono stato invitato alla modifica
	private CopyOnWriteArrayList<String> notYetAcked; //lista dei documenti al quale l'utente è stato invitato ma di cui non è ancora stato notificato
	
	public Utente(String password) {
		
	
		this.password = password;
		
		isOnline = false;
		isEditing = false;
		
		createdDocs = new CopyOnWriteArrayList<>();
		docsInvited = new CopyOnWriteArrayList<>();
		notYetAcked = new CopyOnWriteArrayList<>();
		
		online_lock = new ReentrantLock();
		edit_lock = new ReentrantLock();
	}
	
	
	public String getPassword() {
		return password; 
	}
	
	public void SetOnline(boolean flag) {
		online_lock.lock();
		
		isOnline = flag;
		
		online_lock.unlock();
	}
	
	public boolean checkOnline() {
		boolean var;
		
		online_lock.lock();	
		var = isOnline;
		online_lock.unlock();
		
		return var;
	}
	
	public void SetEdit(boolean flag) {
		edit_lock.lock();
		
		isEditing = flag;
		
		edit_lock.unlock();
	}
	
	//restituisce true se l'utnte sta editando un documento, false altrimenti
	public boolean checkEdit() {
		boolean var;
		
		edit_lock.lock();
		var = isEditing;
		edit_lock.unlock();
		
		return var;
	}
	
	
	
	//aggiunge filename alla lista dei documenti dei quali l'utente è invitato alla modifica
	public void setInvite(String me, String filename) {
		//prendo le lock cosi lo stato dell'utente (sia online/offline che edit/non-edit) non può cambiare durante l'aggiornamento
		//della lista degli inviti
		online_lock.lock(); 
		edit_lock.lock();
		
		if (!isOnline || isEditing) {
			
			docsInvited.add(filename);
			notYetAcked.add(filename);
			
		} else { //ovvero se l'utente è online (e quindi non offline e non in fase di editing)
			docsInvited.add(filename);
			
			RegistrationImpl.notifyOnlineInvite(me, filename);
			
		}
		
		edit_lock.unlock();
		online_lock.unlock();
	}
	
	
	public Iterator<String> notYetAckedIteratorGet() {
		return notYetAcked.iterator();
	}
	
	public Iterator<String> docCreatedIteratorGet() {		
		return createdDocs.iterator();
	}
	
	public Iterator<String> docInvitedIteratorGet() {
		return docsInvited.iterator();
	}
	
	public void notYetAckedClean() {
		for (String i : notYetAcked) {
			notYetAcked.remove(i);
		}
	}
	
	
	public int notYetAckedSize() {
		return notYetAcked.size();
	}

	public int getCreatedDocSize() {
		return createdDocs.size();
	}
	
	public int getDocsInvitedSize() {
		return docsInvited.size();
	}
	
	public void addCreatedDoc(String filename) {
		this.createdDocs.add(filename);
	}
 	
}
