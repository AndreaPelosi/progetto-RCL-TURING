import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class MainClassTuringServer {

	private static int NUMBER_USERS_ESTIMATED = 100; //numero stimato di utenti registrati 
	private static int ACTIVE_USERS_ESTIMATED = 20;
	private static int NUMBER_DOCUMENTS_ESTIMATED = 30;
	
	private static int REGISTRY_PORT = 9999; //porta sul quale è verrà esportato il registry
	public static int SERVICE_PORT = 10001; //porta sulla quale è attivo TURING
	public static int CHAT_PORT = 12000; //porta sulla quale è attivo il servizio di chat sui vari indirizzi IP di multicast
	public static int MAX_MULTIADDR_NUMBER = 256; //numero massimo scelto di indirizzi di multicast
	public static int INT_SIZE = 4;
	public static boolean THREAD_OK = true; //prende false se c'è un errore nella configurazione dei thread
	
	public static void main(String[] args) {		
		
		/**********************************/
		 //INIZIALIZZAZIONE STRUTTURE DATI
		/**********************************/
			
		//hashmap per la memorizzazione degli utenti come coppia <username, oggetto Utente>
		ConcurrentHashMap<String, Utente> utenti = new ConcurrentHashMap<>(NUMBER_USERS_ESTIMATED * (4/3) + 1);
			
		//coda di socket; thread listener stabilisce connessioni con i client e pusha sulla coda, i thread del
		//threadpool poppano e comunicano con un client sulla socket estratta, true indica che la coda è fair per
		//i consumatori
		ArrayBlockingQueue<Socket> queue = new ArrayBlockingQueue<>(ACTIVE_USERS_ESTIMATED, true);
			
		
		//struttura dati che gestisce l'assegnamento degli indirizzi di multicast ai documenti
		MulticastArr group = new MulticastArr();
		
		
		//hashmap dei documenti
		ConcurrentHashMap<String, Documento> docSystem = new ConcurrentHashMap<>(NUMBER_DOCUMENTS_ESTIMATED * (4/3) + 1);
		
		
		
		/********** CONFIGURAZIONE DELLA REGISTRAZIONE **********/
		try {	
			
			Registration registration = new RegistrationImpl(utenti);

			
			LocateRegistry.createRegistry(REGISTRY_PORT);
			Registry registry = LocateRegistry.getRegistry(REGISTRY_PORT);
			registry.rebind(Registration.SERVICE_NAME, registration);
			
		} catch (RemoteException e) {
			System.out.println("Errore nella configurazione del servizio di registrazione, riavviare il server");
			return;
		}
		
		
		/********** INIZIALIZZAZIONE THREAD **********/
		Thread listener = new Thread(new Listen(queue));
		listener.start();
		
		Thread manager = new Thread(new PoolManager(queue, utenti, docSystem, group));
		manager.start();
		
		Thread cleaner = new Thread(new Clean(docSystem, utenti));
		cleaner.start();
		
		
		if (!MainClassTuringServer.THREAD_OK) {
			System.out.println("C'è stato un errore nell'avvio dei thread, riavviare Turing");
			return;
		}
		
		
		System.out.println("TURING is ready and waiting for users...");
		
	}

}
