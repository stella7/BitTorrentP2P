
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * Handles this peer downloading from other peers.
 */
public class Downloader {

    private Logger logger;

    public Downloader(Logger logger) {
        this.logger = logger;
    }

    public void receiveChoke(Connection connection, PeerInfo peer) {
        connection.getDownloadState().setChoked(true);
        logger.writeLog("receive CHOKE from " + peer.getPeerId());
    }

    public void receiveUnchoke(Connection connection, PeerInfo peer, FileInfo datafile) {
        connection.getDownloadState().setChoked(false);
        logger.writeLog("receive UNCHOKE from peer_" + peer.getPeerId());
        requestFirstAvailPiece(connection, peer, datafile);   // request first available piece
    }

    public void receivePiece(Connection connection,
            PeerInfo peer,
            ConcurrentMap<PeerInfo, Connection> connections,
            FileInfo datafile,
            Piece piece) {
			logger.writeLog(String.format("receive PIECE for pieceIndex:%d from peer_" + peer.getPeerId(), piece.getPieceIndex()));
			datafile.getBitField().setPieceToCompleted(piece.getPieceIndex());                  // 1. update bitfield
			datafile.writePiece(piece.getBlock(), piece.getPieceIndex());                       // 2. write piece
			connection.incrementBytesDownloaded(piece.getBlock().length);                       // 3. update bytes downloaded
			for (Map.Entry<PeerInfo, Connection> peerConnection : connections.entrySet()) {         // 4. broadcast Have new piece to all peers
				if (!peerConnection.getValue().equals(connection)) {
					MessageProcessor.sendHave(peerConnection.getValue(), peer, logger, piece.getPieceIndex());
					logger.writeLog("send HAVE to peer_" + peerConnection.getKey().getPeerId());
				}
			}
			requestFirstAvailPiece(connection, peer, datafile);                                 // 5. request next piece
    }

    public void receiveBitfield(Connection connection, PeerInfo peer, BitField bitfield, FileInfo datafile) {
        logger.writeLog("receive BITFIELD " + bitfield.getBitField() + " from peer_" + peer.getPeerId());
        System.out.println("receive BITFIELD " + bitfield.getBitField() + " from peer_" + peer.getPeerId());
        connection.setBitfield(bitfield);               // set peer's bitfield
        if (!datafile.isCompleted()) {
        	MessageProcessor.sendInterested(connection, peer, logger);
        	//requestFirstAvailPiece(connection, peer, datafile);
        }
        //System.out.println("finish");
    }

    /**
     * Requests from peer the first missing piece that peer has.
     */
    private void requestFirstAvailPiece(Connection connection, PeerInfo peer, FileInfo datafile) {
        if (datafile.isCompleted()) {
            logger.writeLog("datafile is complete! WOOOOOOOOOOOO!");
            return;
        }
        for (int i = 0; i < datafile.getNumPieces(); i++) {
            if (datafile.getBitField().missingPiece(i) && connection.getBitfield().hasPiece(i)) {
            	MessageProcessor.sendRequest(connection, peer, logger, i, datafile.getPieceLength()); // request entire piece
                break;
            }
        }
    }

    public Logger getLogger() {
        return logger;
    }
}
