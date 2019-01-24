import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Sezione {

	private Integer secno; //numero della sezione
	private String dirname; //nome della directory in cui si trova il file relativo alla sezione

	private boolean beingEdited; //flag che indica se qualcuno sta modificando la sezione
	private String writer; //username di chi sta modificando la sezione
	private long timeStamp; //timestamp dell'ultima edit attiva sulla sezione, è null se non ci sono edit attive sulla sezione 
	
	private boolean errdetected; //prende true se e solo se fallisce la creazione della sezione
	private String userkicked; //ultimo utente che ha passato più tempo del consentito in edit, è null alla creazione
	
	private ReentrantLock edit_lock; //lock per gestire il flag beingEdited
	private ReentrantReadWriteLock rwlock;
	private Lock readfile; //lock per la lettura del file corrispondente alla sezione
	private Lock writefile;//lock per la scrittura del file corrispondente alla sezione
	
	private SocketChannel sockchan; //canale per l'invio/ricezione del file corrispondente alla sezione
	private Path path;
		
	
	public Sezione(int secno, String dirname) {
		this.secno = secno;
		beingEdited = false;
		writer = null;
		this.dirname = dirname;
		
		errdetected = false;
		userkicked = null;
		
		edit_lock = new ReentrantLock();
		rwlock = new ReentrantReadWriteLock();
		readfile = rwlock.readLock();
		writefile = rwlock.writeLock();
		
		timeStamp = Long.MAX_VALUE;
		
		sockchan = null;
		
		try {
			
			path = Files.createFile(Paths.get(dirname, this.secno.toString()));

			
		} catch (IOException e) {
			errdetected = true; //c'è stato un errore nella creazione del file
			System.out.println("errore nella creazione della sezione " + secno + " del file " + dirname);
		}
		
	}
	
	
	//true se c'è stato un errore nella creazione del file, false altrimenti
	public boolean errcheck() {
		return errdetected;
	}
	
	
	//elimina il file associato alla sezione 
	public void deleteFileSez() {
		try {
			Files.delete(Paths.get(dirname, this.secno.toString()));
		} catch (IOException e) {
			System.out.println("Errore nella distruzione della sezione");
		}
	}
	
	
	
	public String getWriter() {
		return writer;
	}
	
	
	public int getSecno() {
		return secno;
	}
	
	//ritorna true se qualcuno sta editando questa sezione, false altrimenti
	public boolean checkEditing() { 		
		return beingEdited;
	}
	
	
	
	//ritorna false se qualcuno sta editando la sezione, altrimenti setta username
	//all'edit di questa sezione
	public boolean sezEdit(String username) {
		
		edit_lock.lock();
		
		if (this.checkEditing()) { //qualcuno sta editando la sezione
			edit_lock.unlock();
			return false;
		}
		
		writer = username;
		beingEdited = true;
		edit_lock.unlock();
		
		return true;
	}
	
	
	//ritorna false se username non è l'utente che sta editando la sezione; altrimenti
	//ritorna true e compie le operazioni di chiusura edit della sezione (mette il writer a null)
	public int allowedEndEdit(String username) {
		
		edit_lock.lock();
		
		if (username.equals(userkicked)) {
			userkicked = null;
			edit_lock.unlock();
			return 0;
		}
		
		if (writer == null || !username.equals(writer)) {
			edit_lock.unlock();
			return 1;
		}
		
		writer = null;
		beingEdited = false;
		edit_lock.unlock();
		
		return 2;
		
	}
	
	
	
	public void ForceEndEdit() {
		
		edit_lock.lock();
		
		
		userkicked = writer;
		beingEdited = false;
		writer = null;
		timeStamp = Long.MAX_VALUE;
		
	
		edit_lock.unlock();
	}
	
	
	
	public void setTimeStamp() {
		timeStamp = System.currentTimeMillis();
	}
	
	public void resetTimeStamp() {
		timeStamp = Long.MAX_VALUE;
	}
	
	public long getTimeStamp() {
		return timeStamp;
	}
	
	
	
	//invia la sezione su un SocketChannel
	public void sendSection(SocketChannel sc) {
		
		readfile.lock();
		
		try (FileChannel filereader = FileChannel.open(Paths.get(dirname, this.secno.toString()), StandardOpenOption.READ);) 
		{
			//si alloca lo spazio per un intero più un numero di byte pari alla size del file
			ByteBuffer dst = ByteBuffer.allocate( (MainClassTuringServer.INT_SIZE + (int)filereader.size()) );
			
			sockchan = sc;
			
			//nei primi 4 byte di dst viene scritto un intero che rappresenta il numero dei restanti byte del buffer
			dst.putInt((int)filereader.size());
			
			boolean stop = false;
			
			while (!stop) {
				int bytesread = filereader.read(dst);
				
				if (bytesread == -1) 
					stop = true;
				
				dst.flip();
				
				while (dst.hasRemaining()) {
					sockchan.write(dst);
				}
				dst.clear();
			}
			
			sockchan.shutdownOutput(); //chiude la connessione in scrittura senza chiudere il canale
			filereader.close();
			
		} catch (IOException e) {
			readfile.unlock();
			System.out.println("Errore nell'invio della sezione " + secno + " del documento " + dirname);
			return;
		}
		
		System.out.println("Sezione " + secno + " del documento " + dirname + " inviata con successo!");
		readfile.unlock();
		
	}
	
	
	//riceve la sezione su un SocketChannel
	public void receiveSection(SocketChannel sc) {
		
		writefile.lock();
		
		//allocato per leggere un intero corrispondente al numero di byte del file
		ByteBuffer bufreadlen = ByteBuffer.allocate(MainClassTuringServer.INT_SIZE);
		
		try {
			sc.configureBlocking(true);
			sc.read(bufreadlen);

			bufreadlen.flip();
			
			int x = bufreadlen.asIntBuffer().get(0);
			
			Files.delete(Paths.get(dirname, this.secno.toString())); //cancella il vecchio file
			Files.createFile(Paths.get(dirname, this.secno.toString()));  //crea un nuovo file con lo stesso nome

			//se x == 0 vuol dire che il file passato dal client è vuoto, non c'è bisogno di fare altro
			
			if (x > 0) { //se x > 0 il file passato è non vuoto, bisogna scrivere il file
				
				FileChannel filechan = FileChannel.open(path, StandardOpenOption.WRITE);
				
				ByteBuffer buf = ByteBuffer.allocate(x);
				
				while (sc.read(buf) != -1) {
					
					buf.flip();
					
					while (buf.hasRemaining()) {
						filechan.write(buf);
					}
					
					buf.clear();
				}
				
				filechan.close();
			}
		} catch (IOException e) {
			writefile.unlock();
			System.out.println("Errore nella ricezione della sezione " + secno + " del documento " + dirname);
			return;
		}
		
		System.out.println("Sezione " + secno + " del documento " + dirname + " ricevuta con successo!");
		writefile.unlock();
		
	}
	
}
