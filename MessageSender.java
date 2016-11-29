import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MessageSender implements MessageConstants{
	public static void sendHandshake(Connection connection, PeerInfo peer, Logger log, FileInfo datafile) {
		HandshakeMessage hsm = new HandshakeMessage(HANDSHAKE_HEADER, connection.getPeer(), datafile.getFilename());
		byte[] handshakeMessage = hsm.encodeHandshake();
        sendMessage(connection.getSocket(), handshakeMessage);
        log.writeLog("send HANDSHAKE to peer_" + peer.getPeerId());
    }
	
	public static void sendBitfield(Connection connection, PeerInfo peer, Logger log, BitField bitfield) {
		byte[] bitfieldByte = bitfield.getBitField().toByteArray();
		ByteArrayOutputStream message = new ByteArrayOutputStream();
        try {
			message.write(intToByte(DataMessage.MessageID.BITFIELD_ID.ordinal()));
			message.write(intToByte(bitfield.numPieces));
	        message.write(bitfieldByte);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        byte[] bitfieldMessage = message.toByteArray();
        System.out.println(connection.getSocket());
        sendMessage(connection.getSocket(), bitfieldMessage);
        log.writeLog("send BITFIELD " + bitfield.getBitField() + " to peer_" + peer.getPeerId());
    }
	
	public static void sendInterested(Connection connection, PeerInfo peer, Logger log) {
		 byte[] interestedMessage = intToByte(DataMessage.MessageID.INTERESTED_ID.ordinal());
		//byte[] interestedMessage = buildMessage(DataMessage.MessageID.INTERESTED_ID);
        sendMessage(connection.getSocket(), interestedMessage);
        connection.getDownloadState().setInterested(true);
        log.writeLog("send INTERESTED to peer_" + peer.getPeerId());
        System.out.println("send INTERESTED to peer_" + peer.getPeerId());
    }
	
	public static void sendNotInterested(Connection connection, PeerInfo peer, Logger log) {
        byte[] notInterestedMessage = buildMessage(DataMessage.MessageID.NOT_INTERESTED_ID);
        sendMessage(connection.getSocket(), notInterestedMessage);
        connection.getDownloadState().setInterested(false);
        log.writeLog("send NOT_INTERESTED to peer_" + peer.getPeerId());
        System.out.println("send NOT_INTERESTED to peer_" + peer.getPeerId());
    }
	
	public static void sendChoke(Connection connection, PeerInfo peer, Logger log) {
        byte[] chokeMessage = buildMessage(DataMessage.MessageID.CHOKE_ID);
        sendMessage(connection.getSocket(), chokeMessage);
        connection.getUploadState().setChoked(true);
        log.writeLog("send CHOKE to peer_" + peer.getPeerId());
    }
	
	public static void sendUnchoke(Connection connection, PeerInfo peer, Logger log) {
        byte[] UnchokeMessage = buildMessage(DataMessage.MessageID.UNCHOKE_ID);
        sendMessage(connection.getSocket(), UnchokeMessage);
        connection.getUploadState().setChoked(false);
        log.writeLog("send UNCHOKE to peer_" + peer.getPeerId());
    }
	
	public static byte[] buildMessage(DataMessage.MessageID messageID) {
        return intToByte(messageID.ordinal());
    }
	
	public static void sendHave(Connection connection, PeerInfo peer, Logger log, int pieceIndex) {
        ByteArrayOutputStream message = new ByteArrayOutputStream();
        try {
            message.write(intToByte(DataMessage.MessageID.HAVE_ID.ordinal()));
            message.write(intToByte(pieceIndex));
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] haveMessage = message.toByteArray();
        sendMessage(connection.getSocket(), haveMessage);
        log.writeLog(String.format("send HAVE for pieceIndex:%d to peer_" + peer.getPeerId(), pieceIndex));
    }
	
	public static void sendRequest(Connection connection, PeerInfo peer, Logger log, int pieceIndex, int pieceLength) {
        if (!connection.canDownloadFrom()) {
            log.writeLog("ERROR: cannot download from peer_" + peer.getPeerId());
            return;
        }
          
        
        ByteArrayOutputStream message = new ByteArrayOutputStream();
        try {
            message.write(intToByte(DataMessage.MessageID.REQUEST_ID.ordinal()));
            message.write(intToByte(pieceIndex));
            message.write(intToByte(0));
            message.write(intToByte(pieceLength));
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] requestMessage = message.toByteArray();
        
        sendMessage(connection.getSocket(), requestMessage);
        log.writeLog(String.format("send REQUEST for pieceIndex:%d, pieceLength:%d to peer_" + peer.getPeerId(), pieceIndex, pieceLength));
    }
	
	public static void sendPiece(Connection connection, PeerInfo peer, Logger log, int pieceIndex, FileInfo datafile) {
        ByteArrayOutputStream message = new ByteArrayOutputStream();
        try {
            message.write(intToByte(DataMessage.MessageID.PIECE_ID.ordinal()));
            message.write(intToByte(pieceIndex));
            message.write(intToByte(0));
            message.write(intToByte(datafile.readPiece(pieceIndex).length));
            message.write(datafile.readPiece(pieceIndex));
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] pieceMessage = message.toByteArray();
        
        sendMessage(connection.getSocket(), pieceMessage);
        log.writeLog(String.format("send PIECE for pieceIndex:%d to peer_" + peer.getPeerId(), pieceIndex));
    }
	
	
	private static void sendMessage(Socket peerSocket, byte[] message) {
        try {
            peerSocket.getOutputStream().write(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
	public static byte[] intToByte(int i) {
        ByteBuffer b = ByteBuffer.allocate(INT_BYTE_LEN);
        b.order(ByteOrder.BIG_ENDIAN);
        b.putInt(i);
        return b.array();
    }
}
