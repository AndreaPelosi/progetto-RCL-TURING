import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class ConnectionsServerSide {

	
	//gestisce le scritture sulla socket da parte del client per l'operazione di login
	//restituisce un array di stringhe con username e password del client se il metodo ha successo, null altrimenti
	public static String[] manageLogin(Socket sock) {
		
		DataInputStream dis;
		int unamesize, passsize; //size rispettivamente di username e password
		String[] credentials = new String[2];

		try {			
			dis = new DataInputStream(sock.getInputStream());
			
			unamesize = dis.readInt(); //legge un intero corrispondente alla size dello username	
			byte[] uname = new byte[unamesize];			
			
			int acc1 = 0; //accumulatore di byte letti
			do {
				acc1 = acc1 + dis.read(uname);
			}
			while (acc1 < unamesize); //mentre i byte letti sono minori della size dello username 
			
			credentials[0] = new String(uname); //viene creata una nuova stringa coi byte dello username

			
			passsize = dis.readInt(); //operazione analoga a quella qui sopra, stavolta per la password
			byte[] pass = new byte[passsize];
			
			int acc2 = 0;
			do {
				acc2 = acc2 + dis.read(pass);
			}
			while (acc2 < passsize);
			
			credentials[1] = new String(pass);

			
			return credentials;
			
		} catch (IOException e) {
			System.out.println("Errore di IO durante operazione di manageLogin, fallimento operazione");
		}

		return null;
		
	}
	
	
	
	
	
	
	
	
	//gestisce le scritture sulla socket da parte del client per l'operazione di creazione di un file
	//ritorna una tripla (nome del file, numero sezioni, nome dell'utente creatore) se il metodo ha successo, null altrimenti
	public static Triple<String, Integer, String> manageCreate(Socket sock) {
		
		DataInputStream dis;
		int filenamesize, creatorsize, numSect; //size rispettivamente del nome del file, nome del creatore e numero sezioni
		
		
		try {
			dis = new DataInputStream(sock.getInputStream());
			
			filenamesize = dis.readInt();
			byte[] filename = new byte[filenamesize];
			
			int acc1 = 0; //accumulatore di byte letti
			
			do {
				acc1 = acc1 + dis.read(filename);
			}
			while (acc1 < filenamesize); //mentre i byte letti sono minori della size del filename 

			numSect = dis.readInt(); //numero di sezioni in cui deve essere diviso il documento
			
			//se come numero di sezioni dal client viene passato un numero <= 0,
			//vengono assegnate 5 sezioni di default
			if (numSect <= 0)
				numSect = 5;
			
			//operazione analoga a quella qui sopra, stavolta per il nome del creatore del documento
			creatorsize = dis.readInt();
			byte[] creator = new byte[creatorsize];
			
			int acc2 = 0;
			
			do {
				acc2 = acc2 + dis.read(creator);
			} while (acc2 < creatorsize);
			
			
			return new Triple<String, Integer, String>(new String(filename), numSect, new String(creator));
					
		} catch (IOException e) {
			System.out.println("Errore di IO durante operazione di manageCreate, fallimento operazione");
		}
		return null;
		
	}
	
	
	
	
	
	
	
	
	//gestisce le scritture sulla socket da parte del client per l'operazione di invito alla modifica di un file
	//ritorna una tripla (nome del file, nome utente invitato, nome utente creatore) se il metodo ha successo, null altrimenti
	public static Triple<String,String,String> manageInvite(Socket sock) {
		
		int filenamesize, userInvitedSize, creatorsize;//size rispettivamente del nome del file, nome dell'invitato e nome del creatore		
		
		try {
			DataInputStream dis = new DataInputStream(sock.getInputStream());

			filenamesize = dis.readInt();
			byte[] filename = new byte[filenamesize];
			
			int acc1 = 0; //accumulatore di byte letti
			
			do {
				acc1 = acc1 + dis.read(filename);
			}
			while (acc1 < filenamesize); //mentre i byte letti sono minori della size del nome del file

			
			userInvitedSize = dis.readInt(); //operazione analoga a quella qui sopra, stavolta per lo username dell'utente invitato
			byte[] userInvited = new byte[userInvitedSize];
			
			int acc2 = 0;
			
			do {
				acc2 = acc2 + dis.read(userInvited);
			}
			while (acc2 < userInvitedSize);

			
			creatorsize = dis.readInt(); //operazione analoga a quella qui sopra, stavolta per lo username del creatore del documento
			byte[] creator = new byte[creatorsize];
			
			int acc3 = 0; //accumulatore di byte letti
			
			do {
				acc3 = acc3 + dis.read(creator);
			}
			while (acc3 < creatorsize); //mentre i byte letti sono minori della size del creator 

			return new Triple<String, String, String>(new String(filename), new String(userInvited), new String(creator));
			
		} catch (IOException e) {
			System.out.println("Errore di IO durante operazione di manageInvite, fallimento operazione");
		}
		return null;
	}
	
	
	
	
	
	

	
	
	//gestisce le scritture sulla socket da parte del client per l'operazione di Edit, EndEdit e Show section
	//ritorna una tripla (nome del file, numero di sezione, nome utente richiedente la sezione) se il metodo ha successo, null altrimenti
	public static Triple<String,Integer,String> manageEditEndEditShowSec(Socket sock) {
		
		DataInputStream dis;
		int filenamesize, usernamesize, secno;//size rispettivamente del nome del file, nome del richiedente e numero di sezione		
		
		
		try {
			dis = new DataInputStream(sock.getInputStream());
			
			filenamesize = dis.readInt();
			byte[] filename = new byte[filenamesize];
			
			int acc1 = 0; //accumulatore di byte letti
			
			do {
				acc1 = acc1 + dis.read(filename);
			}
			while (acc1 < filenamesize); //mentre i byte letti sono minori della size del filename 

			secno = dis.readInt(); //numero della sezione che l'utente vuole editare/end-edit/vedere
			
			
			//operazione analoga a quella qui sopra, stavolta per il nome dell'utente che fa richiesta di edit/end-edit/showsec
			usernamesize = dis.readInt();
			byte[] username = new byte[usernamesize];
			
			int acc2 = 0;
			
			do {
				acc2 = acc2 + dis.read(username);
			} while (acc2 < usernamesize);
			
			return new Triple<String,Integer,String>(new String(filename), secno, new String(username));
	
		} catch (IOException e) {
			System.out.println("Errore di IO durante operazione di manageEditEndEditShowSec, fallimento operazione");
		}
		return null;
	}
	
	
	
	
	
	//gestisce le scritture sulla socket da parte del client per l'operazione di List
	//ritorna nome del richiedente della lista se il metodo ha successo, null altrimenti
	public static String manageList(Socket sock) {
		
		DataInputStream dis;
		int usernamesize; //size del nome del richiedente della lista
		
		try {
			dis = new DataInputStream(sock.getInputStream());
			
			usernamesize = dis.readInt();
			byte[] username = new byte[usernamesize];
			
			int acc = 0; //accumulatore di byte letti
			
			do {
				acc = acc + dis.read(username);
			} while (acc < usernamesize);  //mentre i byte letti sono minori della size dello username
			
			return new String(username);
		} catch (IOException e) {
			System.out.println("Errore di IO durante operazione di manageList, fallimento operazione");
		}
		
		return null;
	}
	
	
	
	
	
		
	
	
	
	//gestisce le scritture sulla socket da parte del client per l'operazione di Show di un documento
	//restituisce un array di stringhe con nome del documento richiesto e username richiedente se il metodo ha successo, null altrimenti
	public static String[] manageShowDoc(Socket sock) {
		
		
		DataInputStream dis;
		int filenamesize, usernamesize; //size rispettivamente del nome del file e del nome del richiedente
		String[] couple = new String[2];
		
		try {
			dis = new DataInputStream(sock.getInputStream());
			
			filenamesize = dis.readInt();
			byte[] filename = new byte[filenamesize];
			
			int acc1 = 0; //accumulatore di byte letti
			
			do {
				acc1 = acc1 + dis.read(filename);
			}
			while (acc1 < filenamesize); //mentre i byte letti sono minori della size del filename 
			
			couple[0] = new String(filename);
			
			//operazione analoga a quella qui sopra, stavolta per il nome dell'utente che fa richiesta di showdoc
			usernamesize = dis.readInt();
			byte[] username = new byte[usernamesize];
			
			int acc2 = 0;
			
			do {
				acc2 = acc2 + dis.read(username);
			} while (acc2 < usernamesize);
			
			
			couple[1] = new String(username);
			

			return couple;
			
		} catch (IOException e) {
			System.out.println("Errore di IO durante operazione di manageShowDoc, fallimento operazione");
		}

		return null;	
	}

		
}
