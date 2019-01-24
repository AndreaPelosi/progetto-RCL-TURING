import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;

public class Listen implements Runnable{

	private ServerSocket welcome;
	private ArrayBlockingQueue<Socket> queue;
	
	public Listen(ArrayBlockingQueue<Socket> queue) {
		try {
			welcome = new ServerSocket(MainClassTuringServer.SERVICE_PORT);
		} catch (IOException e) {
			System.out.println("Errore nella creazione del server socket, riavviare il server");
			MainClassTuringServer.THREAD_OK = false;
			return;
		}
		this.queue = queue;
	}
	
	
	@Override
	public void run() {
		
		while (true) {
			try { 
				queue.put(welcome.accept());
				
			} catch (IOException | InterruptedException e) {
				System.out.println("Errore nella put di un socket nella coda");
				continue;
			}
			
		}
	}

}
