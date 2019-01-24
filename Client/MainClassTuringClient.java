import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Scanner;


public class MainClassTuringClient {

	private static int REGISTRY_PORT = 9999; //porta sul quale è esportato il registry
	private static int SERVICE_PORT = 10001; //porta sulla quale è attivo il servizio del server
	public static int INT_SIZE = 4;
	
	//capacità in numero di byte con cui vengono costruiti i datagram packet per la ricezione dei messaggi nella chat
	public static int DATAGRAMPACKET_CAPACITY = 1024;
	
	public static void main(String[] args) {

		Registry registry; //riferimento al registro
		Registration registration; //riferimento all'oggetto remoto
		NotifyInviteInterface callbackObj;
		NotifyInviteInterface stub;
		
		Socket sock = null;
		DataInputStream dis = null;
		DataOutputStream dos = null;
		String username = null;
		String password = null;
		
		Scanner scan = 	new Scanner(System.in);
		String token = null;
		
		boolean isEditing = false;
		boolean isOnline = false;
		
		MulticastSocket ms = null;
		InetAddress chataddr = null;
		Integer chatport = null;
		
		try {
			registry = LocateRegistry.getRegistry(REGISTRY_PORT);
			registration = (Registration)registry.lookup(Registration.SERVICE_NAME);
			
			
			callbackObj = new NotifyInviteImpl();
			stub = (NotifyInviteInterface)UnicastRemoteObject.exportObject(callbackObj, 0);
			
		} catch (RemoteException e) {
			System.out.println("connessione al server remoto fallita, riprovare");
			scan.close();
			return;
		
		} catch (NotBoundException e) {
			System.out.println("servizio inesistente, riprovare");
			scan.close();
			return;
		}
		
		System.out.println("per avere informazioni sul funzionamento di TURING, digitare \"turing --help \" ");
		
		
		
		
		
		while (true) { 
			//come da specifica del client, una volta che il client è avviato, non ne è richiesta
			//la terminazione con un comando
			System.out.println("in attesa di comandi...");
			
			try {
				token = scan.next();
			} catch (NoSuchElementException e) {
				System.out.println("client in chiusura");
				scan.close();
				return;
			}
			
			if (!"turing".equals(token)) {
				System.out.println(token + " non è stato riconosciuto dal programma. Per aiuto, digitare "
						+ "\"turing --help \" ");
				continue;
			}
			
			token = scan.next();
			
			
			
			
			if ("--help".equals(token)) {
				usage();
				continue;
			}	
			
			
			
			
			if ("register".equals(token)) {
				
				String tempUsername = scan.next();
				String tempPassword = scan.next();
				boolean success = false;
				
				
				if (isOnline) {
					System.out.println("Permesso negato, non si può registrare un utente mentre un altro è loggato");
					continue;
				}
				
				if (isEditing) {
					System.out.println("Permesso negato, non si può registrare un utente mentre un altro è in fase di edit");
				}
				try {					
					success = registration.register(tempUsername, tempPassword);

				} catch (RemoteException e) {
					System.out.println("Errore nell'esecuzione del metodo remoto, riprovare");
				}				
				
				if (success)
					System.out.println("ti sei registrato con successo!");
				else
					System.out.println("registrazione fallita, ritentare");
				
				continue;
			}
			
					
			if ("login".equals(token)) {
				String tempUsername = scan.next();
				String tempPassword = scan.next();
				
				
				if (isOnline) {
					System.out.println("Permesso negato, un utente non può accedere mentre un altro è loggato");
					continue;
				}
				
				if (isEditing) {
					System.out.println("Permesso negato, un utente non può accedere mentre un altro è in fase di edit");
					continue;
				}
				
				try {
					sock = new Socket("localhost", SERVICE_PORT);
					dis = new DataInputStream(sock.getInputStream());
					dos = new DataOutputStream(sock.getOutputStream());
				} catch (IOException e) {
					System.out.println("Errore di IO nella costruzione del socket o nella costruzione degli stream di IO");
					try {
						sock.close();
						dis.close();
						dos.close();
						continue;
					} catch (IOException e1) {
						System.out.println("Errore di IO nella chiusura del socket o nella chiusura degli stream di IO");
						System.out.println("riprovare a fare login");
						continue;
					}
					
				}
				
				try {
					dos.writeInt(OpCodes.LOGIN_OP.getValue());
					
					dos.writeInt(tempUsername.length());
					dos.write(tempUsername.getBytes());
				
					dos.writeInt(tempPassword.length());
					dos.write(tempPassword.getBytes());
					
					int x = dis.readInt();
					
					if (x == OpCodes.OP_NOUSER.getValue()) {
						System.out.println("L'utente " + tempUsername + " non è registrato a TURING");
						sock.close();
						continue;
					}
					
					if (x == OpCodes.OP_WRONGPASSWORD.getValue()) {
						System.out.println("Password errata, riprovare");
						sock.close();
						continue;
					}
					
					if (x == OpCodes.OP_OK.getValue()) {
						System.out.println("Utente Loggato con successo!");

						isOnline = true;
						username = tempUsername;
						password = tempPassword;
						
						//controlla se ci sono stati inviti mentre l'utente non era online, in tal caso li stampa a video
						ConnectionsClientSide.receiveOffNotif(sock); 

						registration.registerForCallback(username, stub);
						
						continue;
					}
				} catch (SocketException e1) {
					System.out.println("connessione chiusa dall'host remoto");
					continue;
				} catch (IOException e2) {
					System.out.println("Errore di IO nella scrittura o ricezione dei dati relativi alla login, riprovare");
					continue;
				}
				
			}
			
			
			if ("logout".equals(token)) {
				
				
				if (isEditing) {
					System.out.println("Permesso negato, l'utente è in fase di edit");
					continue;
				}
				
				if (!isOnline) {
					System.out.println("L'utente deve fare la login prima di potersi disconnettere");
					continue;
				}
				
				
				try {
					dos.writeInt(OpCodes.LOGOUT_OP.getValue());
					int x = dis.readInt();
					
					if (x == OpCodes.OP_FAIL.getValue()) {
						System.out.println("C'è stato un errore inaspettato nella logout, il programma uscirà");
						
						registration.unregisterFromCallback(username);
						
						isOnline = false;
						username = null;
						password = null;

						sock.close();
						return;
					}
					
					if (x == OpCodes.OP_OK.getValue()) {
						System.out.println("Logout avvenuto con successo, a presto!");
						
						registration.unregisterFromCallback(username);
						
						isOnline = false;
						username = null;
						password = null;
						
						sock.close();
						continue;
					}
					
				} catch (IOException e) {
					System.out.println("Errore di IO nella scrittura o ricezione dei dati relativi alla logout, riprovare");
					continue;
				}
			}
			
			
			if ("create".equals(token)) {
				
				String docName = scan.next();
				int numSez = Integer.parseInt(scan.next());
				
				if (isEditing) {
					System.out.println("Permesso negato, l'utente è in fase di edit");
					continue;
				}
				
				if (!isOnline) {
					System.out.println("L'utente deve fare la login prima di poter creare un documento");
					continue;
				}
				
				
				try {
					dos.writeInt(OpCodes.CREATE_OP.getValue());
					
					dos.writeInt(docName.length());
					dos.write(docName.getBytes());
					
					dos.writeInt(numSez);
					
					dos.writeInt(username.length());
					dos.write(username.getBytes());
					
					int x = dis.readInt();
					
					if (x == OpCodes.OP_FAIL.getValue()) {
						System.out.println("TURING non è riuscito ad ottenere nome del documento e numero di sezioni; riprovare.");
						continue;
					}
					
					if (x == OpCodes.OP_DOCNAMETAKEN.getValue()) {
						System.out.println("il nome scelto per il documento esiste già; riprovare usando un altro nome");
						continue;
					}
					
					if (x == OpCodes.OP_FAILEDCREATE.getValue()) {
						System.out.println("fallimento nella divisione in sezioni del documento; riprovare");
						continue;
					}
					
					if (x == OpCodes.OP_OK.getValue()) {
						if (numSez <= 0) {
							System.out.println("anche se la richiesta di creazione è stata fatta con un numero di sezioni non accettato,"
									+ "la richiesta di creazione è stata esaudita lo stesso ma il documento è stato creato con un numero"
									+ "di sezioni standard (5 sezioni)");
						} else {
							System.out.println("documento creato con successo!");
						}
						continue;
					}
				
				} catch (SocketException e1) {
					System.out.println("connessione interrotta; per evitare stati inconsistenti si consiglia di riavviare il programma");
					continue;
				} catch (IOException e2) {
					System.out.println("Errore di IO nella scrittura o ricezione dei dati relativi alla create, riprovare");
					continue;
				}
			}
			
			
			
			if ("share".equals(token)) {
			
				String docName = scan.next();
				String userInvited = scan.next();
				
				
				if (isEditing) {
					System.out.println("Permesso negato, l'utente è in fase di edit");
					continue;
				}
				
				if (!isOnline) { 
					System.out.println("L'utente deve fare la login prima di poter condividere un documento");
					continue;
				}
				
				try {
					dos.writeInt(OpCodes.INVITE_OP.getValue());
					
					dos.writeInt(docName.length());
					dos.write(docName.getBytes());
					
					dos.writeInt(userInvited.length());
					dos.write(userInvited.getBytes());
					
					dos.writeInt(username.length());
					dos.write(username.getBytes());
					
					
					int x = dis.readInt();
					
					
					if (x == OpCodes.OP_FAIL.getValue()) {
						System.out.println("TURING non è riuscito ad ottenere nome del documento e dell'invitato; riprovare");
						continue;
					}
					
					if (x == OpCodes.OP_NOUSER.getValue()) {
						System.out.println("L'utente che si vuole invitare non è registrato a TURING");
						continue;
					}
					
					if (x == OpCodes.OP_NOSUCHDOC.getValue()) {
						System.out.println("Il documento che si vuole condividere non esiste");
						continue;
					}
					
					if (x == OpCodes.OP_NOTPERMITTED.getValue()) {
						System.out.println("Non sei il creatore del documento " + docName + ", permesso d'invito negato");
						continue;
					}
					
					if (x == OpCodes.OP_OK.getValue()) {
						System.out.println("Operazione di condivisione avvenuta con successo!");
						continue;
					}
					
				} catch (SocketException e1) {
					System.out.println("connessione interrotta; per evitare stati inconsistenti si consiglia di riavviare il programma");		
					continue;
				} catch (IOException e) {
					System.out.println("Errore di IO nella scrittura o ricezione dei dati relativi alla share, riprovare");
					continue;
				}

			}
			
			
			
			
			
			if ("edit".equals(token)) {
				
				String docName = scan.next();
				int secno = Integer.parseInt(scan.next());
				
				if (isEditing) {
					System.out.println("Permesso negato, l'utente è già in fase di edit");
					continue;
				}
				
				
				if (!isOnline) {
					System.out.println("L'utente deve fare la login prima di poter editare la sezione di un documento");
					continue;
				}

				try {
					dos.writeInt(OpCodes.EDIT_OP.getValue());
					
					dos.writeInt(docName.length());
					dos.write(docName.getBytes());
					
					dos.writeInt(secno);
					
					dos.writeInt(username.length());
					dos.write(username.getBytes());
					
					int x = dis.readInt();
					
					
					if (x == OpCodes.OP_FAIL.getValue()) {
						System.out.println("TURING non è riuscito ad ottenere nome del documento e numero della sezione; riprovare");
						continue;
					}
					
					if (x == OpCodes.OP_EDITSTATE.getValue()) {
						System.out.println("Impossibile ottenere l'edit per il documento " + docName + " perché si è già in stato di edit");
						continue;
					}
					
					if (x == OpCodes.OP_NOSUCHDOC.getValue()) {
						System.out.println("Documento richiesto per l'edit inesistente");
						continue;
					}
					
					if (x == OpCodes.OP_ILLEGALSECTION.getValue()) {
						System.out.println("Sezione richiesta per l'edit inesistente");
						continue;
					}
					
					if (x == OpCodes.OP_NOTPERMITTED.getValue()) {
						System.out.println("Non si dispone delle autorizzazioni necessarie per editare una sezione del documento " +
								docName);
						continue;
					}
					
					if (x == OpCodes.OP_SECTIONTAKEN.getValue()) {
						System.out.println("La sezione richiesta è già in fase di modifica da parte di un altro utente");
						continue;
					}
					
					if (x == OpCodes.OP_OK.getValue()) {
						isEditing = true;
						isOnline = false;
						
						int port = dis.readInt();
						
						
						String[] chatinfo = ConnectionsClientSide.receiveChatInfo(sock);
						sock.close();

						
						if (chatinfo == null) {
							System.out.println("collegamento alla chat relativa al documento " + docName + " fallito");
						} else {
							chataddr = InetAddress.getByName(chatinfo[0]);
							chatport = Integer.parseInt(chatinfo[1]);

							ms = new MulticastSocket(chatport);

							ms.joinGroup(chataddr);

						}

						ConnectionsClientSide.receiveSection(docName, secno, port, true);
						System.out.println("Sezione " + secno + " del documento " + docName + " scaricata con successo!");
						continue;
							
					}
					
				} catch (SocketException e1) {
					System.out.println("connessione interrotta; per evitare stati inconsistenti si consiglia di riavviare il programma");
					continue;
				} catch (IOException e) {
					System.out.println("Errore di IO nella scrittura o ricezione dei dati relativi alla edit, riprovare");
					continue;
				}
				
			}
			
			
			
			
			
			
			if ("end-edit".equals(token)) {
				
				String docName = scan.next();
				int secno = Integer.parseInt(scan.next());
				
				if (!isEditing) {
					System.out.println("Permesso negato");
					continue;
				}
				
				if (isOnline) {
					System.out.println("Operazione non consentita mentre si è online");
					continue;
				}
				

				try {
					sock = new Socket("localhost", SERVICE_PORT);
					dis = new DataInputStream(sock.getInputStream());
					dos = new DataOutputStream(sock.getOutputStream());
				} catch (IOException e) {
					System.out.println("Errore di IO nella costruzione del socket o nella costruzione degli stream di IO");
					try {
						sock.close();
						dis.close();
						dos.close();
						continue;
					} catch (IOException e1) {
						System.out.println("Errore di IO nella chiusura del socket o nella chiusura degli stream di IO");
						System.out.println("riprovare a fare login");
						continue;
					}
					
				}
				
				try {
					dos.writeInt(OpCodes.ENDEDIT_OP.getValue());
					
					dos.writeInt(docName.length());
					dos.write(docName.getBytes());
					
					dos.writeInt(secno);
					
					dos.writeInt(username.length());
					dos.write(username.getBytes());
					
					int x = dis.readInt();

					if (x == OpCodes.OP_FAIL.getValue()) {
						System.out.println("TURING non è riuscito ad ottenere nome del documento e numero della sezione; riprovare");
						continue;
					}
					
	
					if (x == OpCodes.OP_NOSUCHDOC.getValue()) {
						System.out.println("Documento richesto per l'end-edit inesistente");
						continue;
					}

					
					if (x == OpCodes.OP_ILLEGALSECTION.getValue()) {
						System.out.println("Sezione richiesta per l'end-edit inesistente");
						continue;
					}

					
					if (x == OpCodes.OP_NOTPERMITTED.getValue()) {
						System.out.println("Non si dispone delle autorizzazioni necessarie per poter fare l'end-edit di una sezione "
								+ "del documento " + docName);
						continue;
					}
					
					
					if (x == OpCodes.OP_NOTEDITOR.getValue()) {
						System.out.println("Non sei l'editor della sezione per l'operazione di end-edit richiesta");
						continue;
					}
					
					
					if (x == OpCodes.OP_KICKEDEDITOR.getValue()) {
						System.out.println("Hai passato troppo tempo nella modifica, verrai messo offline e le tue"
								+ " modifiche verranno scartate");
						
						isEditing = false;
						username = null;
						password = null;
						
						if (ms != null) {
							ms.leaveGroup(chataddr);
							ms.close();
							ms = null;
						}
						chataddr = null;
						chatport = null;

						continue;
					}
					
					
					if (x == OpCodes.OP_OK.getValue()) {
						
						int port = dis.readInt();
						ConnectionsClientSide.sendSection(docName, secno, port);
						
						isEditing = false;
						
						if (ms != null) {
							ms.leaveGroup(chataddr);
							ms.close();
							ms = null;
						}
						chataddr = null;
						chatport = null;
						
						System.out.println("Sezione " + secno + " del documento " + docName + " aggiornata "
								+ "con successo!");
						
						//se l'operazione di end-edit ha avuto successo, il client si riconnette a TURING
						dos.writeInt(OpCodes.LOGIN_OP.getValue());
						
						dos.writeInt(username.length());
						dos.write(username.getBytes());
					
						dos.writeInt(password.length());
						dos.write(password.getBytes());
						
						int z = dis.readInt();
						
						
						if (z == OpCodes.OP_OK.getValue()) {
							
							isOnline = true;
							//controlla se ci sono stati inviti mentre l'utente non era online, in tal caso li stampa a video
							ConnectionsClientSide.receiveOffNotif(sock); 
							
							registration.registerForCallback(username, stub);
							
							continue;
						}
						
						
					}
					
					continue;
					
					
				} catch (SocketException e1) {
					System.out.println("connessione interrotta; per evitare stati inconsistenti si consiglia di riavviare il programma");
					continue;
				} catch (IOException e) {
					System.out.println("Errore di IO nella scrittura o ricezione dei dati relativi alla end-edit, riprovare");
					continue;
				}								
			}
			
			
			
			
			
			
			if ("list".equals(token)) {
				
				
				if (isEditing) {
					System.out.println("Permesso negato, l'utente è in fase di edit");
					continue;
				}
				
				if (!isOnline) {
					System.out.println("L'utente deve fare la login prima di poter richiedere"
							+ " la lista dei documenti");
					continue;
				}
				
				try {
					dos.writeInt(OpCodes.LIST_OP.getValue());
					
					dos.writeInt(username.length());
					dos.write(username.getBytes());
					
					int x = dis.readInt();
					
					if (x == OpCodes.OP_FAIL.getValue()) { //fallita la ricezione del nome utente
						System.out.println("TURING non è riuscito ad elaborare la richiesta; riprovare");
						continue;
					}
					
					if (x == OpCodes.OP_OK.getValue()) {
						ConnectionsClientSide.receiveList(sock, username);
						continue;
					}
					
				} catch (SocketException e1) {
					System.out.println("connessione interrotta; per evitare stati inconsistenti si consiglia di riavviare il programma");
					continue;
				} catch (IOException e) {
					System.out.println("Errore di IO nella scrittura o ricezione dei dati relativi alla list, riprovare");
					continue;
				}
			}

			
			
			
			
			if ("show".equals(token)) {
				
				String docName = scan.next();
				int secno = Integer.parseInt(scan.next());
				
				
				if (isEditing) {
					System.out.println("Permesso negato, l'utente è in fase di edit");
					continue;
				}
				
				
				if (!isOnline) {
					System.out.println("L'utente deve fare la login prima di poter richiedere un documento");
					continue;
				}

				
				
				if (secno == -1) { //l'utente sta richiedendo la visione dell'intero documento
					
					try {
						dos.writeInt(OpCodes.SHOWDOC_OP.getValue());
						
						
						dos.writeInt(docName.length());
						dos.write(docName.getBytes());
						
						dos.writeInt(username.length());
						dos.write(username.getBytes());
						
						int x = dis.readInt();
						
						
						if (x == OpCodes.OP_FAIL.getValue()) {
							System.out.println("TURING non è riuscito ad ottenere nome del documento; riprovare");
							continue;
						}
						
						if (x == OpCodes.OP_EDITSTATE.getValue()) {
							System.out.println("Impossibile richiedere la show per il documento " + docName + " perché si è in stato di edit");
							continue;
						}
						
						if (x == OpCodes.OP_NOSUCHDOC.getValue()) {
							System.out.println("Documento richiesto per la show inesistente");
							continue;
						}
												
						if (x == OpCodes.OP_NOTPERMITTED.getValue()) {
							System.out.println("Non si dispone delle autorizzazioni necessarie per richiedere la show "
									+ "del documento " + docName);
							continue;
						}
						
						
						
						if (x == OpCodes.OP_OK.getValue()) {
							
							int secNumber = dis.readInt();
							
							for (int i = 0; i < secNumber; i++) {
								
								int port = dis.readInt();
								int inEdit = dis.readInt();
								
								ConnectionsClientSide.receiveSection(docName, i, port, false);
								
								
								System.out.println("Sezione " + i + " del documento " + docName + " scaricata con successo!");

								if (inEdit == 1) {
									System.out.println("Essa è in fase di modifica");
								}
								if (inEdit == 0) {
									System.out.println("Essa non è in fase di modifica");
								}

								
							}
							
							continue;
						}
						
						
					} catch (SocketException e1) {
						System.out.println("connessione interrotta; per evitare stati inconsistenti si consiglia di riavviare il programma");
						continue;
					} catch (IOException e) {
						System.out.println("Errore di IO nella scrittura o ricezione dei dati relativi alla show, riprovare");
						continue;
					}								
					
				} else { //l'utente sta richiedendo la visione solo della sezione secno
					
					try {
						dos.writeInt(OpCodes.SHOWSEC_OP.getValue());
						
						dos.writeInt(docName.length());
						dos.write(docName.getBytes());
						
						dos.writeInt(secno);
						
						dos.writeInt(username.length());
						dos.write(username.getBytes());
						
						int x = dis.readInt();
						
						
						if (x == OpCodes.OP_FAIL.getValue()) {
							System.out.println("TURING non è riuscito ad ottenere nome del documento e numero della sezione; riprovare");
							continue;
						}
						
						if (x == OpCodes.OP_EDITSTATE.getValue()) {
							System.out.println("Impossibile richiedere la show per il documento " + docName + " perché si è in stato di edit");
							continue;
						}
						
						if (x == OpCodes.OP_NOSUCHDOC.getValue()) {
							System.out.println("Documento richiesto per la show inesistente");
							continue;
						}
						
						if (x == OpCodes.OP_ILLEGALSECTION.getValue()) {
							System.out.println("Sezione richiesta per la show inesistente");
							continue;
						}
						
						if (x == OpCodes.OP_NOTPERMITTED.getValue()) {
							System.out.println("Non si dispone delle autorizzazioni necessarie per richiedere la show di una sezione "
									+ "del documento " + docName);
							continue;
						}
						
						
						if (x == OpCodes.OP_OK.getValue()) {
							
							int inEdit = dis.readInt(); //0 se la sezione richiesta non è in fase di edit, 1 altrimenti
							
							int port = dis.readInt();
							ConnectionsClientSide.receiveSection(docName, secno, port, false);
							
							System.out.println("Sezione " + secno + " del documento " + docName + " scaricata con successo!");

							if (inEdit == 1) {
								System.out.println("Essa è in fase di modifica");
							}
							if (inEdit == 0) {
								System.out.println("Essa non è in fase di modifica");
							}
							
							continue;
						}
						
					} catch (SocketException e1) {
						System.out.println("connessione interrotta; per evitare stati inconsistenti si consiglia di riavviare il programma");
						continue;
					} catch (IOException e) {
						System.out.println("Errore di IO nella scrittura o ricezione dei dati relativi alla end-edit, riprovare");
						continue;
					}								
					
				}
			}
			
			
			
			
			if ("send".equals(token)) {
				
				String usermessage = scan.nextLine();
				
				if (!isEditing) {
					System.out.println("Permesso negato");
					continue;
				}
				
				if (isOnline) {
					System.out.println("Operazione non consentita mentre si è online");
					continue;
				}
				
								
				DateFormat df = new SimpleDateFormat("HH:mm:ss");
				Date d = new Date();
				String timestamp = df.format(d);
				
				String message = timestamp + " " + username + ": " + usermessage;
				
				byte[] buffer = message.getBytes();
				

				try {					
					DatagramPacket dp = new DatagramPacket(buffer, buffer.length, chataddr, chatport);
					
					ms.send(dp);

				} catch (IOException e) {
				
					System.out.println("Messaggio non inviato sulla chat a causa di un errore di IO");
					continue;
				}
				
				System.out.println("Messaggio inviato sulla chat");
				continue;
				
			}
			
			
			if ("receive".equals(token)) {
				
				if (!isEditing) {
					System.out.println("Permesso negato");
					continue;
				}
				
				if (isOnline) {
					System.out.println("Operazione non consentita mentre si è online");
					continue;
				}

			
				boolean stop = false;
				byte[] buf = new byte[MainClassTuringClient.DATAGRAMPACKET_CAPACITY];
				int iterations = 0; //conta quante iterazioni fa il while
				DatagramPacket dp = new DatagramPacket(buf, buf.length);

				try {
					ms.setSoTimeout(100); //tempo di attesa di ms sulla receive 
				} catch (SocketException e) {
					System.out.println("Errore di accesso alla socket durante il setting del timeout sulla receive, riprovare");
					continue;
				}
				
				while (!stop) {
					
					
					try {
						ms.receive(dp);
						
						String s = new String(dp.getData(), 0, buf.length);
						System.out.println(s);
						
						dp.setLength(buf.length);
						//si riempie il buffer di zeri in modo che non resti sporco da receive precedenti
						Arrays.fill(buf,(byte)0); 
					
					} catch (SocketTimeoutException e) {
						
						stop = true;
						
						//l'abbandono del gruppo e la chiusura del multicast socket viene fatta dalla end-edit
						
					} catch (IOException e) {
						System.out.println("Errore di IO nella receive");
						break;
					}
				
					iterations++; //un'iterazione è stata compiuta
				}
				
				
				//è stata fatta una sola iterazione, il while è uscito al primo socket timeout;
				//vuol dire che non c'era nessun messaggio da ricevere
				if (iterations == 1) { 
					System.out.println("Nessun messaggio ricevuto");
				}
				
				continue;
			}
			
		}
				
		
	}
	
	
	
	
	private static void usage() {
	
		System.out.println("usage: turing COMMAND [ARGS...]");
		System.out.println();
		
		System.out.println("commands:");
		System.out.println("register <username> <password> registra l'utente");
		System.out.println("login <username> <password> effettua il login");
		System.out.println("logout effettua il logout");
		System.out.println();
		
		System.out.println("create <doc> <numesezioni> crea un documento");
		System.out.println("share <doc> <username> condivide il documento");
		System.out.println("show <doc> <sec> mostra una sezione del documento");
		System.out.println("show <doc> mostra l'intero documento");
		System.out.println("list mostra la lista dei documenti");
		System.out.println();
		
		System.out.println("edit <doc> <sec> modifica una sezione del documento");
		System.out.println("end-edit <doc> <sec> fine modifica della sezione del doc.");
		System.out.println();
		
		System.out.println("send <msg> invia un msg sulla chat");
		System.out.println("receive visualizza i msg ricevuti sulla chat");
	}

}
