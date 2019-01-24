import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class Clean implements Runnable{

	private ConcurrentHashMap<String, Documento> docSystem;
	private ConcurrentHashMap<String, Utente> utenti;
	private int SLEEPING_TIME;
	private int MAX_EDIT_TIME;
	
	public Clean(ConcurrentHashMap<String, Documento> docSystem, ConcurrentHashMap<String, Utente> utenti) {
		this.docSystem = docSystem;
		this.utenti = utenti;
		
		SLEEPING_TIME = 600000; //in millisecondi, corrispondono a 10 minuti
		MAX_EDIT_TIME = 3600000; //in millisecondi, corrisponde ad un'ora
	}
	
	
	@Override
	public void run() {
		
		while (true) {
		
			try {
				Thread.sleep(SLEEPING_TIME); //dorme per SLEEPING_TIME
			} catch (InterruptedException e) {
				System.out.println("thread cleaner risvegliato durante la sleep");
			}
			

			//valori della hashmap visti come collezione
			Collection<Documento> coll = docSystem.values();
			
			//Quando si sveglia itera sui Documenti di docSystem e per ogni documento controlla se ogni sezione è in fase di 
			//edit da meno di MAX_EDIT_TIME. Se così non è la sezione è liberata dall'edit, l'utente che la sta modificando
			//viene messo offline e le sue modifiche vengono scartate
			for (Iterator<Documento> iterator = coll.iterator(); iterator.hasNext();) {
				
				Documento doc = iterator.next();
				int secNumber = doc.getSecNumber();
				
				for (int i = 0; i < secNumber; i++) {
					Sezione sec = doc.getSection(i);
					
					long lastTimeStamp = sec.getTimeStamp();
					
					//se l'utente sta editando la sezione sec da troppo tempo
					if (System.currentTimeMillis() - lastTimeStamp > MAX_EDIT_TIME) {
						
						String writer = sec.getWriter();
						Utente u = utenti.get(writer);
						
						u.SetEdit(false);
						u.SetOnline(false);
						sec.ForceEndEdit(); //forza la fine dell'edit
						doc.activeUsersDecrement();
						
						System.out.println("L'utente " + writer + " ha tenuto in edit la sezione " + sec.getSecno() +
								" del documento " + doc.getFilename() + " per troppo tempo, pertanto è stato tolto dallo stato" +
							    "di edit e messo offline. Le sue modifiche alla sezione verranno ignorate");
					}
				}
			
			}
		
		}
	
	}

	
	
}
