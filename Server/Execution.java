import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Execution implements Runnable {

	
	private Socket sock;
	private ConcurrentHashMap<String, Utente> utenti;
	private ConcurrentHashMap<String, Documento> docSystem;
	private MulticastArr group;

	private boolean logoutFlag;
	
	public Execution(Socket sock, ConcurrentHashMap<String, Utente> utenti,
			ConcurrentHashMap<String, Documento> docSystem, MulticastArr group) {
		
		this.utenti = utenti;
		this.docSystem = docSystem;
		this.sock = sock;
		this.group = group;

		this.logoutFlag = false;
	}
	
	
	@Override
	public void run() {
		
		Utente user = null;
		DataInputStream dis = null;
		DataOutputStream dos = null;
		int x = -2;
		boolean loginSuccess = false;
		
		try {			
			dis = new DataInputStream(sock.getInputStream());
			dos = new DataOutputStream(sock.getOutputStream());
		} catch (IOException e) {
			System.out.println("Errore di IO durante la creazione dei data IO stream");
			return;
		}
		
		
		
		while (!loginSuccess) {
		
			
			
			try {
				x = dis.readInt();
			} catch (SocketException e) {
				System.out.println("Errore nell'accesso della socket, la connessione verrà chiusa");
				try {
					sock.close();
					dis.close();
					dos.close();
				} catch (IOException e1) {
					System.out.println("Errore nella chiusura delle risorse");
					return;
				}
			} catch (IOException e1) {
				System.out.println("Errore di IO durante attesa input da parte di un client");
				return;
			}

//******************************************************************************************************//
			//LOGIN//
			
			if (x == OpCodes.LOGIN_OP.getValue()) {
				
				String [] cred = ConnectionsServerSide.manageLogin(sock); //credenziali di accesso di cui va controllata la validità
				
				if (cred == null) {
					System.out.println("operazione di login fallita");
					return;
				}
				
				user = utenti.get(cred[0]);
				
				if (user == null) { //se nessun utente col nome richiesto è registrato a TURING
					try {
						dos.writeInt(OpCodes.OP_NOUSER.getValue()); //l'utente richiesto non è registrato
						sock.close();
						return;
					} catch (IOException e) {
						System.out.println("Errore di IO durante la gestione di una login");
						return;
					}
				}
				String pass = user.getPassword();
				
				if (!pass.equals(cred[1])) { //se la password digitata dall'utente non è quella corretta
					try {
						dos.writeInt(OpCodes.OP_WRONGPASSWORD.getValue()); //l'utente richiesto non è registrato
						sock.close();
						return;
					} catch (IOException e) {
						System.out.println("Errore di IO durante la gestione di una login");
						return;
					}
				}
				
				
				try {
					user.SetOnline(true); //l'utente viene messo online
					loginSuccess = true;
					
					dos.writeInt(OpCodes.OP_OK.getValue()); //viene scritto il codice di successo dell'operazione
					
					dos.writeInt(user.notYetAckedSize()); //viene scritto il numero di documenti a cui il client è stato invitato alla modifica
					
					String curr;
					Iterator<String> it = user.notYetAckedIteratorGet();
					while (it.hasNext()) { //scrive namesize e nome di ogni documento
						curr = it.next();
						
						dos.writeInt(curr.length());
						dos.write(curr.getBytes());
					}
					
					user.notYetAckedClean();
				
				} catch (SocketException e) {
					user.SetOnline(false);
				} catch (IOException e) {
					System.out.println("Errore di IO durante la gestione di una login");
					return;
				}
				
				
//*****************************************************************************************************//
				//ENDEDIT//
				
			} else if (x == OpCodes.ENDEDIT_OP.getValue()) {
				
				Triple<String, Integer, String> t = ConnectionsServerSide.manageEditEndEditShowSec(sock);
				
				
				if (t == null) {
					try {
						//se il server non è riuscito ad ottenere nome del documento, sezione e richiedente della end-edit
						dos.writeInt(OpCodes.OP_FAIL.getValue());
						sock.close();
						return;
					} catch (IOException e) {
						System.out.println("Errore di IO durante la gestione della end-edit");
						return;
					}
				}
				
				
				String filename = t.getFirst();
				String username = t.getThird();
				int secno = t.getSecond();
				
				Utente u = utenti.get(username);	
				
				
				
				if (!docSystem.containsKey(filename)) { //se il documento di cui terminare la modifica della sezione non esiste
					try {
						dos.writeInt(OpCodes.OP_NOSUCHDOC.getValue());
						sock.close();
						return;
					} catch (IOException e) {
						System.out.println("Errore di IO durante la gestione della end-edit");
						return;
					}

				}
				
				
				if (secno < 0 || secno >= docSystem.get(filename).getSecNumber()) { //se la sezione richiesta non esiste
					try {
						dos.writeInt(OpCodes.OP_ILLEGALSECTION.getValue());
						sock.close();
						return;
					} catch (IOException e) {
						System.out.println("Errore di IO durante la gestione della end-edit");
						return;
					}
				}
				
				boolean doc_ok = false; //prende true se e solo se l'utente è autorizzato alla modifica del documento
				Iterator<String> it1 = u.docCreatedIteratorGet();
				
				while (it1.hasNext()) { 
					String str = it1.next();
					if (str.equals(filename)) { //se c'è un documento col nome filename nella lista dei documenti creati
						doc_ok = true;
						break;
					}
				}
				
				if (doc_ok == false) {
					
					Iterator<String> it2 = u.docInvitedIteratorGet();
					
					while (it2.hasNext()) {
						String str = it2.next();
						
						//se c'è un documento col nome filename nella lista dei documenti al quale l'utente è stato invitato
						if (str.equals(filename)) { 
							doc_ok = true;
							break;
						}
					}
				}
				
				
				if (!doc_ok) { //se l'utente non ha il file né nella lista creati né nella lista invitati
					try {
						dos.writeInt(OpCodes.OP_NOTPERMITTED.getValue());
						sock.close();
						return;
					} catch (IOException e) {
						System.out.println("Errore di IO durante la gestione della end-edit");
						return;
					}
				}

				
				
				Documento d = docSystem.get(filename);
				
				Sezione s = d.getSection(secno); //la sezione richiesta esiste, ho già controllato sopra che secno fosse valido

				
				int res = s.allowedEndEdit(username);
				
				if (res == 0) { //se username è stato kickato
					
					try {
						dos.writeInt(OpCodes.OP_KICKEDEDITOR.getValue());
						sock.close();
					} catch (IOException e) {
						System.out.println("Errore di IO durante la gestione della end-edit");
					}
					return;
					
				} else if (res == 1) { //se non è username a star editando la sezione
					try {
						dos.writeInt(OpCodes.OP_NOTEDITOR.getValue());
						sock.close();
					} catch (IOException e) {
						System.out.println("Errore di IO durante la gestione della end-edit");
					}
					return;

				} else { //username è l'editor e richiede la end-edit
					
					ServerSocketChannel ssc;
					try {
						
						
						
						ssc = ServerSocketChannel.open();
						
						
						//collega ssc ad una porta effimera
						ssc.bind(new InetSocketAddress("localhost", 0));
						
						InetSocketAddress addr = (InetSocketAddress) ssc.getLocalAddress();
						int servsockchanport = addr.getPort(); //ottiene la porta a cui ssc è collegato
						
						ssc.configureBlocking(true);
		
						dos.writeInt(OpCodes.OP_OK.getValue());
						
						dos.writeInt(servsockchanport);//scrive la porta sulla quale è in ascolto ssc
						
						SocketChannel sc = ssc.accept();
		
						s.receiveSection(sc);
						s.resetTimeStamp();
						
						d.activeUsersDecrement();
						
						
						u.SetEdit(false);

						
					} catch (IOException e) {
						System.out.println("Errore di IO durante la gestione della end-edit");
						d.activeUsersDecrement();
						s.resetTimeStamp();
						u.SetEdit(false);
						return;
					}

				}
				

//***************************************************************************************************//				
			//se l'utente ha tentato di eseguire una qualsiasi operazione senza aver prima fatto login
			} else {
				try { 
					dos.writeInt(OpCodes.OP_LOGINFIRST.getValue());
					sock.close();
					dis.close();
					dos.close();
					return;
				} catch (IOException e) {
					System.out.println("Errore di IO durante la scrittura di OP_LOGINFIRST");
					return;
				} 
			}

			
		}
				
	
		
		
		
		while (!logoutFlag) {
		
			try {
				x = dis.readInt();
			
			} catch (SocketException e1) {
				//se la connessione viene resettata dal client
				System.out.println("Il client ha terminato la connessione");

				//se l'utente era online e la connessione viene resettata l'utente viene messo offline
				if (user.checkOnline()) { 
					user.SetOnline(false);
				}
				return;

			} catch (IOException e2) {
			
				System.out.println("errore di IO");
				return;
			}
	
			
			
			switch (x) {
			
			
//******************************************************************************************************//
			//LOGOUT//
			
			case 1: {
				
				if (!user.checkOnline()) { //se l'utente non è online
					try {
						dos.writeInt(OpCodes.OP_FAIL.getValue());
						sock.close();
					} catch (IOException e) {
						System.out.println("Errore di IO durante la gestione della logout");
					}
					return;
				}
				
				
				user.SetOnline(false);
				try {
					dos.writeInt(OpCodes.OP_OK.getValue());
		
					sock.close();
				} catch (IOException e) {
					System.out.println("Errore di IO durante la gestione della logout");
				}
				logoutFlag = true;
			}
				
				break;
				
				
				
//*****************************************************************************************************//		
				
			//CREATE//	
				
			case 2: {
				
				Triple<String, Integer, String> ft = ConnectionsServerSide.manageCreate(sock);
				
				if (ft == null) {
					try {
						//se il server non è riuscito ad ottenere nome del documento e numero di sezioni
						dos.writeInt(OpCodes.OP_FAIL.getValue());
						continue;
					} catch (IOException e) {
						System.out.println("Errore di IO durante la gestione della create");
						continue;
					}
				}
				
				
				String filename = ft.getFirst();
				int numSect = ft.getSecond();
				String creator = ft.getThird();
				boolean filealready = false;
				
				if (docSystem.containsKey(filename)) {
					try {
						dos.writeInt(OpCodes.OP_DOCNAMETAKEN.getValue());
						filealready = true;
						break;  
					} catch (IOException e) {
						System.out.println("Errore di IO durante la gestione della create");
						user.SetOnline(false);
						return;
					}
				}
				
				
				if (!filealready) { //se non c'è già un altro file con lo stesso nome
					
					try {
						Documento doc = new Documento(filename, creator, numSect, group);
						
						if (!doc.errcheck()) { //se non ci sono stati errori nella creazione delle sezioni del file
							docSystem.put(filename, doc);
							
							Utente u = utenti.get(creator);
							u.addCreatedDoc(filename);
							
							dos.writeInt(OpCodes.OP_OK.getValue());
						} else {
							dos.writeInt(OpCodes.OP_FAILEDCREATE.getValue());
						}						

					} catch (IOException e) {
						System.out.println("Errore di IO durante la gestione della create");
						user.SetOnline(false);
						return;
					}
				}
				
								
			}
				
				break;

				
				
				
//*******************************************************************************************************//
		
				//SHARE//				
				
			case 3: {
				
				Triple<String, String, String> t = ConnectionsServerSide.manageInvite(sock);
				
				if (t == null) {
					try {
						//se il server non è riuscito ad ottenere nome del documento, dell'invitato e del creatore del documento
						dos.writeInt(OpCodes.OP_FAIL.getValue());
						continue;
					} catch (IOException e) {
						System.out.println("Errore di IO durante la gestione della share");
						user.SetOnline(false);
						return;
					}
				}
				
						
				String filename = t.getFirst();
				String userInvited = t.getSecond();
				String creator  = t.getThird();
				
				//controllo che l'utente invitato sia registrato
				if (!utenti.containsKey(userInvited)) { //l'utente che il client ha provato ad invitare non è registrato
					try {
						dos.writeInt(OpCodes.OP_NOUSER.getValue());
						continue;
					} catch (IOException e) {
						System.out.println("Errore di IO durante la gestione della share");
						user.SetOnline(false);
					}
				}
				

				if (!docSystem.containsKey(filename)) { //se il documento da condividere non esiste
					try {
						dos.writeInt(OpCodes.OP_NOSUCHDOC.getValue());
						continue;
					} catch (IOException e) {
						System.out.println("Errore di IO durante la gestione della share");
						user.SetOnline(false);
						return;
					}

				}
				
				
				
				//controllo che creator sia il vero creatore del documento
				Utente u1 = utenti.get(creator);
				Iterator<String> it = u1.docCreatedIteratorGet();
				boolean realcreator = false;
				
				while (it.hasNext()) { //itera sulla lista dei file creati da creator
					if (it.next().equals(filename)) {
						realcreator = true; //prende true se e solo se creatore è il vero creatore del documento filename
						break;
					}
				}
				
				if (!realcreator) {
					try {
						dos.writeInt(OpCodes.OP_NOTPERMITTED.getValue());
						continue;
					} catch (IOException e) {
						System.out.println("Errore di IO durante la gestione della share");
						user.SetOnline(false);
						return;
					}
				}
				
				
				
				
				try {
					
					//se tutti i controlli hanno successo
					Utente u2 = utenti.get(userInvited);
					
					//aggiunge filename alla lista dei documenti che u2 può modificare, se l'utente è online gli viene notificato subito che
					//è stato invitato alla modifica di un nuovo documento, se è offline o se sta editando, l'invito gli viene notificato
					//all'accesso successivo
					u2.setInvite(userInvited, filename);
					
					
					//aggiunge nel Documento filename che l'utente userInvited è stato invitato
					Documento d = docSystem.get(filename);
					d.addInvited(userInvited);
										
					dos.writeInt(OpCodes.OP_OK.getValue());
				} catch (IOException e) {
					System.out.println("Errore di IO durante la gestione della share");
					user.SetOnline(false);
					return;
				}
			}
				
				break;
				
				

				
//*******************************************************************************************************//
				
				//EDIT//
				
				
			case 4: {
				
				
				Triple<String, Integer, String> t = ConnectionsServerSide.manageEditEndEditShowSec(sock);
				
				if (t == null) {
					try {
						//se il server non è riuscito ad ottenere nome del documento, sezione e richiedente della edit
						dos.writeInt(OpCodes.OP_FAIL.getValue());
						continue;
					} catch (IOException e) {
						System.out.println("Errore di IO durante la gestione della edit");
						user.SetOnline(false);
						return;
					}
				}
				
				
				String filename = t.getFirst();
				String username = t.getThird();
				int secno = t.getSecond();
				
				Utente u = utenti.get(username);
				
				
				if (u.checkEdit()) { //l'utente richiedente sta già editando un altra sezione
					try {
						dos.writeInt(OpCodes.OP_EDITSTATE.getValue());
						continue;
					} catch (IOException e) {
						System.out.println("Errore di IO durante la gestione della edit");
						user.SetOnline(false);
						return;
					}
				}
				
				
				
				if (!docSystem.containsKey(filename)) { //se il documento di cui modificare la sezione non esiste
					try {
						dos.writeInt(OpCodes.OP_NOSUCHDOC.getValue());
						continue;
					} catch (IOException e) {
						System.out.println("Errore di IO durante la gestione della edit");
						user.SetOnline(false);
						return;
					}

				}
				
				
				if (secno < 0 || secno >= docSystem.get(filename).getSecNumber()) { //se la sezione richiesta non esiste
					try {
						dos.writeInt(OpCodes.OP_ILLEGALSECTION.getValue());
						continue;
					} catch (IOException e) {
						System.out.println("Errore di IO durante la gestione della edit");
						user.SetOnline(false);
						return;
					}
				}
				
				boolean doc_ok = false; //prende true se e solo se l'utente è autorizzato alla modifica del documento
				Iterator<String> it1 = u.docCreatedIteratorGet();
				
				while (it1.hasNext()) { 
					String str = it1.next();
					if (str.equals(filename)) { //se c'è un documento col nome filename nella lista dei documenti creati
						doc_ok = true;
						break;
					}
				}
				
				if (doc_ok == false) {
					
					Iterator<String> it2 = u.docInvitedIteratorGet();
					
					while (it2.hasNext()) {
						String str = it2.next();
						
						//se c'è un documento col nome filename nella lista dei documenti al quale l'utente è stato invitato
						if (str.equals(filename)) { 
							doc_ok = true;
							break;
						}
					}
				}
				
				
				if (!doc_ok) { //se l'utente non ha il file né nella lista creati né nella lista invitati
					try {
						dos.writeInt(OpCodes.OP_NOTPERMITTED.getValue());
						continue;
					} catch (IOException e) {
						System.out.println("Errore di IO durante la gestione della edit");
						user.SetOnline(false);
						return;
					}
				}
				
				
				
				Documento d = docSystem.get(filename);
				
				Sezione s = d.getSection(secno); //la sezione richiesta esiste, ho già controllato sopra che secno fosse valido
				
				if (!s.sezEdit(username)) { //se qualcun altro sta editando la sezione che l'utente ha richiesto
					try {
						dos.writeInt(OpCodes.OP_SECTIONTAKEN.getValue());
						continue;
					} catch (IOException e) {
						System.out.println("Errore di IO durante la gestione della edit");
						user.SetOnline(false);
						return;
					}
				} else { //la sezione è libera di essere editata
					
					try {

						//ottiene l'indirizzo di multicast collegato al documento dal quale 
						//si prende la sezione, su tale indirizzo sarà attiva la chat
						String multicastAddr = d.activeUsersIncrement(); 

						s.setTimeStamp();

						
						ServerSocketChannel ssc = ServerSocketChannel.open();
						
						//collega ssc ad una porta effimera
						ssc.bind(new InetSocketAddress("localhost", 0));
						
						InetSocketAddress addr = (InetSocketAddress) ssc.getLocalAddress();
						int servsockchanport = addr.getPort(); //ottiene la porta a cui ssc è collegato
						
						ssc.configureBlocking(true);
		
						dos.writeInt(OpCodes.OP_OK.getValue());
						
						dos.writeInt(servsockchanport);//scrive la porta sulla quale è in ascolto ssc
						

						dos.writeInt(multicastAddr.length()); //scrive lunghezza dell'indirizzo e i suoi byte
						dos.write(multicastAddr.getBytes());
						
						//scrive la porta sulla quale sarà attiva la chat per il documento d
						dos.writeInt(MainClassTuringServer.CHAT_PORT);
						
						
						sock.close();
						
						SocketChannel sc = ssc.accept();
		
						s.sendSection(sc);
				
						u.SetEdit(true);
						u.SetOnline(false);
						
					} catch (IOException e) {
						System.out.println("Errore di IO durante la gestione della edit");
						d.activeUsersDecrement();
						u.SetEdit(false);
						u.SetOnline(false);
						s.resetTimeStamp();
						s.allowedEndEdit(username); 
						return;
					}
					
				}

				
				
			}
				
				break;
				
				
				
//******************************************************************************************************//
				
				//LIST//
				
				
			case 5: {
				
				String username = ConnectionsServerSide.manageList(sock);
				
				if (username == null) {
					try {
						//se il server non è riuscito ad ottenere nome utente
						dos.writeInt(OpCodes.OP_FAIL.getValue());
						continue;
					} catch (IOException e) {
						System.out.println("Errore di IO durante la gestione della list");
						user.SetOnline(false);
						return;
					}
				}
				
				try {
					dos.writeInt(OpCodes.OP_OK.getValue());
				} catch (IOException e) {
					System.out.println("Errore di IO durante la gestione della list");
					user.SetOnline(false);
					return;
				}
				
				Utente u = utenti.get(username);
				
				Iterator<String> it1 = u.docCreatedIteratorGet();
				
				try {
					dos.writeInt(u.getCreatedDocSize());
				} catch (IOException e1) {
					System.out.println("Errore di IO durante la gestione della list");
					user.SetOnline(false);
					return;
				}
				
				
				//scorro la lista dei documenti creati, per ognuno di essi ottengo dal docSystem il 
				//documento e scrivo sulla socket size del nome e nome e lista degli invitati
				while (it1.hasNext()) {
					String filename = it1.next();
					
					Documento d = docSystem.get(filename);
					
					List<String> list = d.getInvitedList();
					
					try {
						dos.writeInt(filename.length()); //scrive filenamesize e filename
						dos.write(filename.getBytes());
						
						//scrive il numero degli utenti invitati alla modifica del documento d
						dos.writeInt(list.size());
						
						//per ogni invitato scrivo size del nome e nome
						for (String invited : list) {
							dos.writeInt(invited.length());
							dos.write(invited.getBytes());
						}
							
					} catch (IOException e) {
						System.out.println("Errore di IO durante la gestione della list");
						user.SetOnline(false);
						return;
					}
				}
				
				
				
				Iterator<String> it2 = u.docInvitedIteratorGet();
				
				try {
					dos.writeInt(u.getDocsInvitedSize());
				} catch (IOException e1) {
					System.out.println("Errore di IO durante la gestione della list");
					user.SetOnline(false);
					return;
				}

				//scorro la lista dei documenti ai quali sono stato invitato, per ognuno di essi ottengo
				//dal docSystem il documento e scrivo sulla socket size del nome del documento e nome,
				//size del nome del creatore e creatore, lista degli invitati
				while (it2.hasNext()) {
					String filename = it2.next();
					
					Documento d = docSystem.get(filename);
					
					List<String> list = d.getInvitedList();
					
					try {
						String creator = d.getCreator();
						
						//scrive size del nome del documento e nome del documento
						dos.writeInt(filename.length());
						dos.write(filename.getBytes());
						
						//scrive size del nome del creatore e nome del creatore
						dos.writeInt(creator.length()); 
						dos.write(creator.getBytes());
						
						//scrive il numero degli utenti invitati alla modifica del documento d
						dos.writeInt(list.size());
						
						//per ogni invitato scrivo size del nome e nome
						for (String invited : list) {
							dos.writeInt(invited.length());
							dos.write(invited.getBytes());
						}
						
						
					} catch (IOException e) {
						System.out.println("Errore di IO durante la gestione della list");
						user.SetOnline(false);
						return;
					}
				}
				

					
			}
				
				break;
				
				
				
				
//**************************************************************************************************//
				
				//SHOW SECTION//				
				
			case 6: {
				
					Triple<String, Integer, String> t = ConnectionsServerSide.manageEditEndEditShowSec(sock);
					
					if (t == null) {
						try {
							//se il server non è riuscito ad ottenere nome del documento, sezione e richiedente della show
							dos.writeInt(OpCodes.OP_FAIL.getValue());
							continue;
						} catch (IOException e) {
							System.out.println("Errore di IO durante la gestione della show section");
							user.SetOnline(false);
							return;
						}
					}
					
					
					String filename = t.getFirst();
					String username = t.getThird();
					int secno = t.getSecond();
					
					Utente u = utenti.get(username);
					
					
					if (u.checkEdit()) { //l'utente richiedente sta editando una sezione
						try {
							dos.writeInt(OpCodes.OP_EDITSTATE.getValue());
							continue;
						} catch (IOException e) {
							System.out.println("Errore di IO durante la gestione della show section");
							user.SetOnline(false);
							return;
						}
					}
					
					
					
					if (!docSystem.containsKey(filename)) { //se il documento di cui mostrare la sezione non esiste
						try {
							dos.writeInt(OpCodes.OP_NOSUCHDOC.getValue());
							continue;
						} catch (IOException e) {
							System.out.println("Errore di IO durante la gestione della show section");
							user.SetOnline(false);
							return;
						}

					}
					
					
					if (secno < 0 || secno >= docSystem.get(filename).getSecNumber()) { //se la sezione richiesta non esiste
						try {
							dos.writeInt(OpCodes.OP_ILLEGALSECTION.getValue());
							continue;
						} catch (IOException e) {
							System.out.println("Errore di IO durante la gestione della show section");
							user.SetOnline(false);
							return;
						}
					}
					
					boolean doc_ok = false; //prende true se e solo se l'utente è autorizzato alla visione del documento
					Iterator<String> it1 = u.docCreatedIteratorGet();
					
					while (it1.hasNext()) { 
						String str = it1.next();
						if (str.equals(filename)) { //se c'è un documento col nome filename nella lista dei documenti creati
							doc_ok = true;
							break;
						}
					}
					
					if (doc_ok == false) {
						
						Iterator<String> it2 = u.docInvitedIteratorGet();
						
						while (it2.hasNext()) {
							String str = it2.next();
							
							//se c'è un documento col nome filename nella lista dei documenti al quale l'utente è stato invitato
							if (str.equals(filename)) { 
								doc_ok = true;
								break;
							}
						}
					}
					
					
					if (!doc_ok) { //se l'utente non ha il file né nella lista creati né nella lista invitati
						try {
							dos.writeInt(OpCodes.OP_NOTPERMITTED.getValue());
							continue;
						} catch (IOException e) {
							System.out.println("Errore di IO durante la gestione della show section");
							user.SetOnline(false);
							return;
						}
					}
					
					
					
					Documento d = docSystem.get(filename);
					
					Sezione s = d.getSection(secno); //la sezione richiesta esiste, ho già controllato sopra che secno fosse valido
				
					
					//posso inviare la sezione al richiedente
					
					try {
						
						
						ServerSocketChannel ssc = ServerSocketChannel.open();
						
						//collega ssc ad una porta effimera
						ssc.bind(new InetSocketAddress("localhost", 0));
						
						InetSocketAddress addr = (InetSocketAddress) ssc.getLocalAddress();
						int port = addr.getPort(); //ottiene la porta a cui ssc è collegato
						
						ssc.configureBlocking(true);
		
						dos.writeInt(OpCodes.OP_OK.getValue());
						
						
						//controllo se qualcuno sta editando la sezione al momento della show
						if (s.checkEditing()) {
							dos.writeInt(1); //scrivo 1 se qualcuno sta editando
						} else {
							dos.writeInt(0); //scrivo 0 altrimenti
						}
						
						dos.writeInt(port);//scrive la porta su sock
						
						
						SocketChannel sc = ssc.accept();
		
						s.sendSection(sc);
				
					} catch (IOException e) {
						System.out.println("Errore di IO durante la gestione della show section");
						user.SetOnline(false);
						return;
					}

				
			}
				
				break;
				
	
				
				
				
//****************************************************************************************************//
				
				//SHOW DOCUMENT//
				
			case 7: {
				
				String[] couple = ConnectionsServerSide.manageShowDoc(sock);
				
				if (couple == null) {
					try {
						//se il server non è riuscito ad ottenere nome del documento e del richiedente della show
						dos.writeInt(OpCodes.OP_FAIL.getValue());
						continue;
					} catch (IOException e) {
						System.out.println("Errore di IO durante la gestione della show document");
						user.SetOnline(false);
						return;
					}
				}
				
				
				
				String filename = couple[0];
				String username = couple[1];

				
				
				Utente u = utenti.get(username);
				
				
				if (u.checkEdit()) { //l'utente richiedente sta editando una sezione
					try {
						dos.writeInt(OpCodes.OP_EDITSTATE.getValue());
						continue;
					} catch (IOException e) {
						System.out.println("Errore di IO durante la gestione della show document");
						user.SetOnline(false);
						return;
					}
				}
				
				
				
				if (!docSystem.containsKey(filename)) { //se il documento non esiste
					try {
						dos.writeInt(OpCodes.OP_NOSUCHDOC.getValue());
						continue;
					} catch (IOException e) {
						System.out.println("Errore di IO durante la gestione della show document");
						user.SetOnline(false);
						return;
					}

				}
				
				
				
				boolean doc_ok = false; //prende true se e solo se l'utente è autorizzato alla visione del documento
				Iterator<String> it1 = u.docCreatedIteratorGet();
				
				while (it1.hasNext()) { 
					String str = it1.next();
					if (str.equals(filename)) { //se c'è un documento col nome filename nella lista dei documenti creati
						doc_ok = true;
						break;
					}
				}
				
				if (doc_ok == false) {
					
					Iterator<String> it2 = u.docInvitedIteratorGet();
					
					while (it2.hasNext()) {
						String str = it2.next();
						
						//se c'è un documento col nome filename nella lista dei documenti al quale l'utente è stato invitato
						if (str.equals(filename)) { 
							doc_ok = true;
							break;
						}
					}
				}
				
				
				if (!doc_ok) { //se l'utente non ha il file né nella lista creati né nella lista invitati
					try {
						dos.writeInt(OpCodes.OP_NOTPERMITTED.getValue());
						continue;
					} catch (IOException e) {
						System.out.println("Errore di IO durante la gestione della show document");
						user.SetOnline(false);
						return;
					}
				}
			
				
				
				//posso inviare il documento al richiedente
				
				Documento d = docSystem.get(filename);
				int secnumber = d.getSecNumber();

				try {
					dos.writeInt(OpCodes.OP_OK.getValue());
					
					dos.writeInt(secnumber); //scrive il numero di sezioni che ha il documento

				} catch (IOException e1) {
					System.out.println("Errore di IO durante la gestione della show document");
					user.SetOnline(false);
					return;
				}

				for (int i = 0; i < secnumber; i++) {
					
					Sezione s = d.getSection(i);
					
					try {
						
						
						ServerSocketChannel ssc = ServerSocketChannel.open();
						
						//collega ssc ad una porta effimera
						ssc.bind(new InetSocketAddress("localhost", 0));
						
						InetSocketAddress addr = (InetSocketAddress) ssc.getLocalAddress();
						int port = addr.getPort(); //ottiene la porta a cui ssc è collegato
						
						ssc.configureBlocking(true);

						
						dos.writeInt(port);//scrive la porta su sock
						
						
						//controllo se qualcuno sta editando la sezione al momento della show
						if (s.checkEditing()) {
							dos.writeInt(1); //scrivo 1 se qualcuno sta editando
						} else {
							dos.writeInt(0); //scrivo 0 altrimenti
						}
						
						SocketChannel sc = ssc.accept();
						
						s.sendSection(sc);	
				
					} catch (IOException e) {
						System.out.println("Errore di IO durante la gestione della show document");
						user.SetOnline(false);
						return;
					}
	
				}					
				
			}
					
				break;
				
			
				
				
				
//**************************************************************************************************//
				
			default: {
				System.out.println("Operazione non riconosciuta");
				try {
					dos.writeInt(OpCodes.OP_FAIL.getValue());

				} catch (IOException e) {
					System.out.println("Errore di IO");
					user.SetOnline(false);
					return;
				}

			}
				break;
			
			
			
			}	
		
		}
	
	}
	
}
