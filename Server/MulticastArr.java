import java.util.concurrent.locks.ReentrantLock;

public class MulticastArr {
	
	private MulticastAddress[] group;
	
	public MulticastArr() {
		
		group = new MulticastAddress[MainClassTuringServer.MAX_MULTIADDR_NUMBER];
		
		for (int i = 0; i < MainClassTuringServer.MAX_MULTIADDR_NUMBER; i++) {
			group[i] = new MulticastAddress(i);
		}
	}
	
	
	public String getAddr() {

		for (int i = 0; i < group.length; i ++) {
			
			
			
			boolean flag = group[i].setInUse();
			
			if (flag) {
				String str = group[i].getAddress();
				return str;
			} else
				continue;
		}
		
		return null;
	}
	
	
	
	
	public void freeAddr(String oldaddress) {
		
		for (int i = 0; i < group.length; i++) {
			
			if (oldaddress.equals(group[i].getAddress())) {
				group[i].freeUse();
			}
		}
		
	}

	
	private class MulticastAddress {
		
		private String address;
		private boolean inUse;
		private ReentrantLock lock_use;
		
		public MulticastAddress(Integer i) {
			this.address = "239.0.0." + i.toString();
			this.inUse = false;
			this.lock_use = new ReentrantLock();

		}
		
		
		
		//restituisce false se l'indirizzo è in uso, altrimenti setta l'indirizzo in uso e restituisce true
		public boolean setInUse () {
			
			lock_use.lock();
			
			if (inUse) {
				lock_use.unlock();
				return false;
			}
			
			inUse = true;
			lock_use.unlock();
			
			return true;
		}
		
		//restituisce la stringa che codifica l'indirizzo di multicast
		public String getAddress() {
			return address;
		}
		
		//setta inUse a false
		public void freeUse() {
			lock_use.lock();
			inUse = false;
			lock_use.unlock();
		}
		
	}

	
}
