import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class ConnectionsClientSide {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
	
	public static void receiveOffNotif(Socket sock) {
		
		DataInputStream dis;
		
		try {
			dis = new DataInputStream(sock.getInputStream());

			int y = dis.readInt(); //numero di inviti alla modifica non ancora visualizzati

			if (y == 0) {
				System.out.println("Nessun nuovo invito alla modifica ricevuto");
			} else {
				System.out.println("Mentre non eri online, sei stato invitato alla modifica di " + y + " documenti:");
			
				for (int i = 0; i < y; i++) {
					
					int filenamesize = dis.readInt();
					byte[] filename = new byte[filenamesize];
					int acc = 0;
					
					do {
						acc = acc + dis.read(filename);
					} while (acc < filenamesize);
					
					System.out.println(new String(filename));
				}
			}

		
		} catch (IOException e) {
			System.out.println("Errore nella ricezione notifiche, esse potrebbero non venir"
					+ " visualizzate correttamente");
		}
		
		
	}
	
	
	
	public static void receiveList(Socket sock, String username) {
		
		DataInputStream dis;
		int createdDocNumb, invitedDocNumb;
		
		try {
			dis = new DataInputStream(sock.getInputStream());
			
			createdDocNumb = dis.readInt();
			
			for (int i = 0; i < createdDocNumb; i++) {
				
				int filenamesize = dis.readInt();
				byte[] filename = new byte[filenamesize];
				
				int acc = 0;
				
				do {
					acc = acc + dis.read(filename);
				} while (acc < filenamesize);
				
				System.out.println(new String(filename) + ":");
				System.out.println("Creatore: " + username);
				System.out.println("Collaboratori: ");
				
				int length = dis.readInt();
				int size;
				
				
				for (int j = 0; j < length; j++) {
					size = dis.readInt();
					
					byte[] name = new byte[size];
					int acc1 = 0;
					
					do {
						acc1 = acc1 + dis.read(name);
					} while (acc1 < size);
					
					System.out.println(new String(name));
				}
			}
			
			
			invitedDocNumb = dis.readInt();
			
			for (int i = 0; i < invitedDocNumb; i++) {
				
				int filenamesize = dis.readInt();
				byte[] filename = new byte[filenamesize];
				
				int acc = 0;
				
				do {
					acc = acc + dis.read(filename);
				} while (acc < filenamesize);
				
				
				
				int creatornamesize = dis.readInt();
				byte[] creatorname = new byte[creatornamesize];
				
				int acc1 = 0;
				
				do {
					acc1 = acc1 + dis.read(creatorname);
				} while (acc1 < creatornamesize);
				
				
				System.out.println(new String(filename) + ":");
				System.out.println("Creatore: " + new String(creatorname));
				System.out.println("Collaboratori: ");


				int length = dis.readInt();
				int size;
				
				
				for (int j = 0; j < length; j++) {
					size = dis.readInt();
					
					byte[] name = new byte[size];
					int acc2 = 0;
					
					do {
						acc2 = acc2 + dis.read(name);
					} while (acc2 < size);
					
					System.out.println(new String(name));
				}

			}
					
		} catch (IOException e) {
			System.out.println("Errore nella ricezione della lista essa potrebbe non venir"
					+ " visualizzata correttamente");
		}
	}
	
	
	
	
	
	//il flag forEditing è true se la sezione da ricevere deve essere editata, è false se deve essere mostrata
	public static void receiveSection(String DocName, Integer secno, int port, boolean forEditing) {
		
		try {
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			String ts = sdf.format(timestamp);
			
			
			SocketChannel sc = SocketChannel.open();	
			sc.connect(new InetSocketAddress("localhost", port));
			
			ByteBuffer readbuflen = ByteBuffer.allocate(MainClassTuringClient.INT_SIZE);
			

			
			sc.read(readbuflen);
			readbuflen.flip();
			
			int x = readbuflen.asIntBuffer().get(0);
			
			if (x == 0) {
				
				if (forEditing) {
					
					Files.createFile(Paths.get(DocName + secno.toString()));
				} else {
					
					Files.createFile(Paths.get(DocName + secno.toString() + " " + ts));
				}
				
			} else {
				
				FileChannel filedownloader;
				
				if (forEditing) {
				
					filedownloader = FileChannel.open(Paths.get(DocName + secno.toString()), StandardOpenOption.WRITE, StandardOpenOption.CREATE);
				} else {
					
					filedownloader = FileChannel.open(Paths.get(DocName + secno.toString() + " " + ts), StandardOpenOption.WRITE, StandardOpenOption.CREATE);
				}
			
				ByteBuffer buf = ByteBuffer.allocate(x);
				
				while (sc.read(buf) != -1) {
					buf.flip();
					
					while (buf.hasRemaining()) {
						filedownloader.write(buf); //scarica la sezione
					}
					buf.clear();
				}

				filedownloader.close();
			}
			
					
			
		} catch (UnknownHostException e) {
			System.out.println("l'indirizzo al quale si tenta di collegarsi non è stato riconosciuto,"
					+ " la sezione potrebbe non esser stata ricevuta correttamente");
		} catch (IOException e) {
			System.out.println(" Errore nella ricezione della sezione, essa potrebbe non essere stata"
					+ " ricevuta correttamente");
		}
	}
	
	
	
	
	public static void sendSection(String DocName, Integer secno, int port) {
		
		Path path = Paths.get(DocName + secno.toString());
		
		try (FileChannel filereader = FileChannel.open(path, StandardOpenOption.READ);
				SocketChannel sc = SocketChannel.open();
			) {
			
			sc.connect(new InetSocketAddress("localhost", port));

			sc.configureBlocking(true);
			
			//si alloca lo spazio per un intero più un numero di byte pari alla size del file
			ByteBuffer dst = ByteBuffer.allocate( (MainClassTuringClient.INT_SIZE + (int)filereader.size()) );

			//nei primi 4 byte di dst viene scritto un intero che rappresenta il numero dei restanti byte del buffer
			dst.putInt((int)filereader.size());

			
			boolean stop = false;
			
			while (!stop) {
				int bytesread = filereader.read(dst);
				
				if (bytesread == -1) 
					stop = true;
				
				dst.flip();
				
				while (dst.hasRemaining()) {
					sc.write(dst);
				}
				dst.clear();
			}

			filereader.close();
			Files.delete(path);
			
		} catch (IOException e) {
			System.out.println("Errore di IO nell'invio della sezione " + secno + " del documento " + DocName
					+ ", essa potrebbe non essere stata inviata correttamente");
		}	

	}
	
	
	
	
	
	public static String[] receiveChatInfo(Socket sock) {
		
		DataInputStream dis;
		String[] info = new String[2];
		
		try {
			dis = new DataInputStream(sock.getInputStream());
			
			int multicastAddrlen = dis.readInt();
			byte[] multicastAddr = new byte[multicastAddrlen];
			
			int acc = 0;
			
			do {
				acc = acc + dis.read(multicastAddr);
			} while (acc < multicastAddrlen);
			
			
			Integer chatport = dis.readInt();
			
			info[0] = new String(multicastAddr);
			info[1] = Integer.toString(chatport);
			
			return info;
			
			
		} catch (IOException e) {
			System.out.println("Errore di IO nella ricezione delle informazioni, per iscriversi alla chat");
		}
		
		return null;
		
	}

	
}
