import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class MessageHandler implements Runnable{
	private ConcurrentHashMap<PeerInfo, Float> unchokedPeers;
    private Connection connection;
    //private DataMessage message;
    private FileInfo datafile;
  
    private PeerInfo peer;
    private Downloader downloader;
    private Uploader uploader;
    private ConcurrentMap<PeerInfo, Connection> connections; // only used for case PIECE_ID

    public MessageHandler(Connection connection,
                       PeerInfo peer,
                       ConcurrentMap<PeerInfo, Connection> connections,
                       Downloader downloader,
                       Uploader uploader,
                       //DataMessage message,
                       FileInfo datafile) {

        this.peer = peer;
        this.connection = connection;
        this.connections = connections;
        this.downloader = downloader;
        this.uploader = uploader;
        //this.message = message;
        this.datafile = datafile;        
    }
    
    public MessageHandler(){
    	
    }
    

    @Override
    public void run() {
    	//System.out.println("MessageHandler:" + message.getMessageID());
    	DataMessage message;
    	while(true){
    		
    		message  = connection.removeFromMsgQueue();
			while(message == null)
			{
				Thread.currentThread();
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					
					e.printStackTrace();
				}
				message  = Peer.removeFromMsgQueue();
			}
    			
			switch (message.getMessageID()) {
	            case HANDSHAKE_ID:
	                MessageSender.sendBitfield(connection, peer, downloader.getLogger(), datafile.getBitField());
	                connection.getSocket().getInetAddress();
	                        connection.getSocket().getPort();
	                break;
	            case INTERESTED_ID:
	            	uploader.receiveInterested(connection, peer);
	                break;
	            case NOT_INTERESTED_ID:
	            	uploader.receiveUninterested(connection, peer);
	                break;
	            case HAVE_ID:
	                updatePeerBitfield(connection, message.getPieceIndex());
	                break;
	            case REQUEST_ID:
	            	uploader.receiveRequest(connection, peer, datafile, message.getRequest().getPieceIndex());
	                break;
	            case PIECE_ID:
	            	downloader.receivePiece(connection, peer, connections, datafile, message.getPiece());
	                break;
	            case BITFIELD_ID:
	            	downloader.receiveBitfield(connection, peer, message.getBitfield(), datafile);
	            	//System.out.println("finish bitfield");
	                break;
	            case CHOKE_ID:
	            	downloader.receiveChoke(connection, peer);
	                break;
	            case UNCHOKE_ID:
	            	downloader.receiveUnchoke(connection, peer, datafile);
	                break;
    		}
    	}
        
    }
    /*
    public void receiveInterested(Connection connection, PeerInfo peer) {
    	
        logger.writeLog("receive INTERESTED from peer_" + peer.getPeerId());
        State state = connection.getUploadState();
        //        if (!state.isInterested() && state.isChoked()) {
        state.setInterested(true);
        for (PeerInfo peer1 : unchokedPeers.keySet()) {
            System.out.println("Uploaded unchoked set contains " + peer1);
        }
        if (unchokedPeers.containsKey(peer)) {
            MessageSender.sendUnchoke(connection, peer, logger);
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
        MessageSender.sendPiece(connection, peer, logger, pieceIndex, datafile);
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
            	MessageSender.sendHave(peerConnection.getValue(), peer, logger, piece.getPieceIndex());
                logger.writeLog("send HAVE to peer_" + peerConnection.getKey().getPeerId());
            }
        }
        requestFirstAvailPiece(connection, peer, datafile);                                 // 5. request next piece
    }

    public void receiveBitfield(Connection connection, PeerInfo peer, BitField bitfield, FileInfo datafile) {
        logger.writeLog("receive BITFIELD " + bitfield.getBitField() + " from peer_" + peer.getPeerId());
        System.out.println("receive BITFIELD " + bitfield.getBitField() + " from peer_" + peer.getPeerId());
        //System.out.println("my bitfield is:" + datafile.getBitField().getBitField());
        //System.out.println("is completed:" + datafile.isCompleted());
        //System.out.println("next clear bit:" + datafile.getBitField().getBitField().nextClearBit(0) + " total:" + 
        //		datafile.getNumPieces());
        connection.setBitfield(bitfield);               // set peer's bitfield
        
        if (!datafile.isCompleted()) {
        	MessageSender.sendInterested(connection, peer, logger);
        	//requestFirstAvailPiece(connection, peer, datafile);
        }else{
        	MessageSender.sendNotInterested(connection, peer, logger);
        }
        //System.out.println("finish");
    }


    private void requestFirstAvailPiece(Connection connection, PeerInfo peer, FileInfo datafile) {
        if (datafile.isCompleted()) {
            logger.writeLog("datafile is complete! WOOOOOOOOOOOO!");
            return;
        }
        for (int i = 0; i < datafile.getNumPieces(); i++) {
            if (datafile.getBitField().missingPiece(i) && connection.getBitfield().hasPiece(i)) {
            	MessageSender.sendRequest(connection, peer, logger, i, datafile.getPieceLength()); // request entire piece
                break;
            }
        }
    }
*/
    private void updatePeerBitfield(Connection connection, int pieceIndex) {
        connection.getBitfield().setPieceToCompleted(pieceIndex);
    }

}
