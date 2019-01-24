import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class PoolManager implements Runnable{

	private ThreadPoolExecutor executor;
	private ArrayBlockingQueue<Socket> queue;
	private ConcurrentHashMap<String, Utente> utenti;
	private ConcurrentHashMap<String, Documento> docSystem;
	private MulticastArr group;
	
	public PoolManager(ArrayBlockingQueue<Socket> queue, ConcurrentHashMap<String, Utente> utenti,
			ConcurrentHashMap<String, Documento> docSystem, MulticastArr group) {
		
		this.utenti = utenti;
		this.docSystem = docSystem;
		this.queue = queue;
		this.group = group;
		
		executor = (ThreadPoolExecutor)Executors.newCachedThreadPool();
	}
	
	
	
	@Override
	public void run() {
		
		while (true) {
			try {
				//estrae un socket dalla coda ed esegue un nuovo task dove vengono gestite le richieste del client
				executor.execute(new Execution(queue.take(), utenti, docSystem, group));
			} catch (InterruptedException e) {
				System.out.println("Errore nell'estrazione di un socket dalla coda");
				continue;
			}
		}
	}

}
