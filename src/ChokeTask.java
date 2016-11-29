import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class ChokeTask implements Runnable {
	private final int UNCHOKE_SLOTS = 4;
	private int i;
	private ConcurrentMap<PeerInfo, Connection> connections;
	private FileInfo datafile;
	private ConcurrentHashMap<PeerInfo, Float> unchokedPeers;
	private Logger logger;
	private Random random = new Random();
	
	public ChokeTask(ConcurrentMap<PeerInfo, Connection> connections, FileInfo datafile, ConcurrentHashMap<PeerInfo, Float> unchokedPeers, Logger logger) {
	    this.connections = connections;
	    this.datafile = datafile;
	    this.unchokedPeers = unchokedPeers;
	    this.logger = logger;
	    this.i = 0;
	}
	
	/**
	 * 1. Calculate data-receiving rates from all peers.
	 * 2. Take top 4 as unchoked.
	 * 3. Every 3 instances of run(), choose 1 peer at random from remaining for optimistic unchoking.
	 */
	@Override
	public void run() {
	    HashMap<PeerInfo, Float> rates = getRates();
	    // Take 4 peers with top rates
	    Map<PeerInfo, Float> sortedRates = rates.entrySet().stream()
	            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
	            .limit(4)
	            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	
	    ConcurrentHashMap<PeerInfo, Float> oldUnchokedPeers = unchokedPeers;
	    unchokedPeers = new ConcurrentHashMap<>(sortedRates);
	    notifyPeers(oldUnchokedPeers, unchokedPeers);
	
	    for (PeerInfo peer1 : unchokedPeers.keySet()) {
	        System.out.println("Unchoked set contains " + peer1.getPeerId());
	    }
	    //        if (i == 0) {       // optimistic unchoking
	    //            unchokedPeers.add(getOptimisticUnchoke());
	    //        }
	    //        i = (i + 1) % 3;
	}
	
	private HashMap<PeerInfo, Float> getRates() {
	    HashMap<PeerInfo, Float> rates = new HashMap<>();
	    long currentTime = System.currentTimeMillis();
	    if (datafile.isCompleted()) {       // use upload rate to determine 4 unchoke slots
	        for (Map.Entry<PeerInfo, Connection> connection : connections.entrySet()) {
	            if (connection.getValue().getUploadState().isInterested()) {
	                rates.put(connection.getKey(), connection.getValue().getUploadRate(currentTime));
	            }
	        }
	    } else {                            // use download rate to determine 4 unchoke slots
	        for (Map.Entry<PeerInfo, Connection> connection : connections.entrySet()) {
	            if (connection.getValue().getUploadState().isInterested()) {
	                rates.put(connection.getKey(), connection.getValue().getDownloadRate(currentTime));
	            }
	        }
	    }
	    return rates;
	}
	
	private void notifyPeers(ConcurrentHashMap<PeerInfo, Float> oldUnchokedPeers, ConcurrentHashMap<PeerInfo, Float> newUnchokedPeers) {
	    // go through old peers, if not in new peers, send CHOKE
	    for (PeerInfo peer : oldUnchokedPeers.keySet()) {
	        System.out.println("Old set contains " + peer.getPeerId());
	        if (!newUnchokedPeers.containsKey(peer)) {
	            MessageProcessor.sendChoke(connections.get(peer), peer, logger);
	        }
	    }
	    // go through new peers, if not in old peers, send UNCHOKE
	    for (PeerInfo peer : newUnchokedPeers.keySet()) {
	        System.out.println("New set contains " + peer.getPeerId());
	        if (!oldUnchokedPeers.containsKey(peer)) {
	        	MessageProcessor.sendUnchoke(connections.get(peer), peer, logger);
	        }
	    }
	}
}
