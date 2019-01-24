import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class Documento {

	private String filename; //nome del documento
	private String creator;
	private CopyOnWriteArrayList<String> invited; //lista dei nomi degli user invitati alla modifica del documento
	private int secNumber; //numero di sezioni in cui è diviso il documento
	private ArrayList<Sezione> sezioni;
	
	private MulticastArr group; //riferimento all'elenco di indirizzi di multicast
	private String multicastAddr; //indirizzo di multicast associato al documento, è null quando nessuno sta editando il documento

	private int activeUsersNumb; //indica il numero di utenti che stanno editando una sezione del documento corrente
	
	private boolean errordetected; //prende true se e solo se fallisce la creazione di una o più sezioni del documento

	private Path dir; 
	
	public Documento (String filename, String creator, int secNumber, MulticastArr group) {
		
		this.filename = filename;
		this.creator = creator;
		this.secNumber  = secNumber;
		invited = new CopyOnWriteArrayList<>();
		
		this.group = group;
		multicastAddr = null;
		this.activeUsersNumb = 0;
		
		errordetected = false;
		dir = Paths.get(filename); //converte il nome del documento in un Path
		
		if (!Files.exists(dir)) {
			try {
				Files.createDirectory(dir); //crea una directory denominata come il documento
			} catch (IOException e) {
				System.out.println("Errore nella creazione della cartella col nome del documento");
				errordetected = true;
			}
		}
		
		sezioni = new ArrayList<>(secNumber);
		
		for (int i = 0; i < secNumber; i++) {
			sezioni.add(new Sezione(i, dir.toString()));
		
			if (sezioni.get(i).errcheck()) { //se è fallita la creazione del file relativo a sez
				
				errordetected = true;
				
				for (int j = 0; j < i; j++) { //per ogni sezione finora creata rimuove il file relativo alla sezione
					Sezione sez1 = sezioni.get(j);
					
					sez1.deleteFileSez();
					
				}
				break; //poi esce dal ciclo
			}
		}
		
		
	}
	
	
	//ritorna il nome del documento
	public String getFilename() {
		return this.filename;
	}
	
	//ritorna il creatore del documento
	public String getCreator() {
		return this.creator;
	}
	
	//ritorna il numero di sezioni in cui è diviso il documento
	public int getSecNumber () {
		return this.secNumber;
	}
	
	
	//ritorna la lista degli invitati alla modifica del documento
	public CopyOnWriteArrayList<String> getInvitedList() {
		return invited;
	}
	
	
	//ritorna l'i-esima sezione del documento, oppure null se il documento non ha una sezione i-esima
	public Sezione getSection(int i) {
		
		if (i >= 0 && i < sezioni.size()) {
			return sezioni.get(i);
		}
		
		return null;
		
	}
	
	//ritorna false se la creazione delle sezioni è avvenuta con successo, true altrimenti. In tal caso, la cartella dove erano 
	//precedentemente contenute le sezioni viene eliminata
	public boolean errcheck() {
		
		if (!this.errordetected) //se la creazione delle sezioni è avvenuta con successo
			return false;	
		
		try {
			Files.delete(this.dir);
		} catch (IOException e) {
			System.out.println("fallimento file delete, rimuovere il file " + dir + " manualmente");
		}
		
		return true;
		
	}
	
	
	//aggiunge un utente alla lista degli invitati alla modifica del documento
	public void addInvited(String userInvited) {
		this.invited.add(userInvited);
	}
	
	
	//incrementa di uno il numero degli utenti in edit sul documento; se tale numero diventa 1,
	//multicastAddr prende un indirizzo di multicast e si restituisce quello, altrimenti viene
	//restiuito il precendente multicastAddr
	public String activeUsersIncrement() {
		this.activeUsersNumb++;
		
		if (activeUsersNumb == 1) {
			 
			multicastAddr = group.getAddr();
			return multicastAddr;
		}
		
		return multicastAddr;
		
	}
	
	
	//decrementa di uno il numero degli utenti in edit sul documento; se tale numero diventa 0,
	//libera l'indirizzo di multicast preso e assegna null a multicastAddr
	public void activeUsersDecrement() {
		this.activeUsersNumb--;
		
		if (activeUsersNumb == 0) {
			group.freeAddr(multicastAddr);
			multicastAddr = null;
		}
	}
	
}
