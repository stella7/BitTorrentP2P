

public class DataMessage {
    private MessageID messageID;
    private int pieceIndex;
    private String filename;
    private PeerInfo peer;

    private Request request;
    private Piece piece;
    private BitField bitfield;
    private HandshakeMessage handshakeMessage;

    // CHOKE, UNCHOKE, INTERESTED, NOT_INTERESTED
    public DataMessage(MessageID messageID) {
        this.messageID = messageID;
    }

    // HANDSHAKE
    public DataMessage(MessageID messageID, HandshakeMessage handshakeMessage) {
        this.messageID = messageID;
        this.handshakeMessage = handshakeMessage;
    }

    // HAVE
    public DataMessage(MessageID messageID, int pieceIndex) {
        this.messageID = messageID;
        this.pieceIndex = pieceIndex;
    }

    // BITFIELD
    public DataMessage(MessageID messageID, BitField bitfield) {
        this.messageID = messageID;
        this.bitfield = bitfield;
    }

    // PIECE
    public DataMessage(MessageID messageID, Piece piece) {
        this.messageID = messageID;
        this.piece = piece;
    }

    // REQUEST
    public DataMessage(MessageID messageID, Request request) {
        this.messageID = messageID;
        this.request = request;
    }

    public int getPieceIndex() {
        return pieceIndex;
    }

    public String getFilename() {
        return filename;
    }
    
    public HandshakeMessage getHandshakeMessage(){
    	return handshakeMessage;
    }

    public Request getRequest() {
        return request;
    }

    public Piece getPiece() {
        return piece;
    }

    public BitField getBitfield() {
        return bitfield;
    }

    public MessageID getMessageID() {
        return messageID;
    }

    public PeerInfo getPeer() {
        return peer;
    }

    public enum MessageID {
        INTERESTED_ID, NOT_INTERESTED_ID, HAVE_ID, REQUEST_ID, PIECE_ID, BITFIELD_ID, HANDSHAKE_ID, CHOKE_ID, UNCHOKE_ID
    }
}
