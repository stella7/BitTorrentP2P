
import java.util.concurrent.ConcurrentHashMap;

/**
 * Uploader protocol functions.
 */
public class Uploader {

    private Logger logger;
    private ConcurrentHashMap<PeerInfo, Float> unchokedPeers;

    public Uploader(Logger logger, ConcurrentHashMap<PeerInfo, Float> unchokedPeers) {
        this.logger = logger;
        this.unchokedPeers = unchokedPeers;
    }

public void receiveInterested(Connection connection, PeerInfo peer) {
    	
        logger.writeLog("receive INTERESTED from peer_" + peer.getPeerId());
        State state = connection.getUploadState();
        //        if (!state.isInterested() && state.isChoked()) {
        state.setInterested(true);
        for (PeerInfo peer1 : unchokedPeers.keySet()) {
            System.out.println("Uploaded unchoked set contains " + peer1);
        }
        if (unchokedPeers.containsKey(peer)) {
            MessageProcessor.sendUnchoke(connection, peer, logger);
        }
        //        }
    }

    public void receiveUninterested(Connection connection, PeerInfo peer) {
        logger.writeLog("receive UNINTERESTED from peer_" + peer.getPeerId());
        State state = connection.getUploadState();
        //        if (state.isInterested() && state.isChoked()) {
        state.setInterested(false);
        //        }
    }

    public void receiveRequest(Connection connection, PeerInfo peer, FileInfo datafile, int pieceIndex) {
        logger.writeLog(String.format("receive REQUEST for pieceIndex:%d from peer_" + peer.getPeerId(), pieceIndex));
        if (!connection.canUploadTo()) {
            logger.writeLog("ERROR: cannot upload to " + peer);
            return;
        }
        MessageProcessor.sendPiece(connection, peer, logger, pieceIndex, datafile);
    }

}
