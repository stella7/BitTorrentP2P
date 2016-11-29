
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.BitSet;

public class MessageProcessor implements MessageConstants{
	
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
	
	public static DataMessage parseMessage(InputStream inputStream) {
		DataMessage message = null;
        int messageIdInt = readIntFromStream(inputStream);
        //System.out.println(messageIdInt);
        DataMessage.MessageID messageId = DataMessage.MessageID.values()[messageIdInt];
        try {
            switch (messageId) {
                case CHOKE_ID:
                    message = new DataMessage(messageId);
                    break;
                case UNCHOKE_ID:
                    message = new DataMessage(messageId);
                    break;
                case INTERESTED_ID:
                    message = new DataMessage(messageId);
                    break;
                case NOT_INTERESTED_ID:
                    message = new DataMessage(messageId);
                    break;
                case HAVE_ID:
                    message = parseHave(inputStream);
                    break;
                case REQUEST_ID:
                    message = parseRequest(inputStream);
                    break;
                case PIECE_ID:
                    message = parsePiece(inputStream);
                    break;
                case BITFIELD_ID:
                    message = parseBitfield(inputStream);
                    break;
                case HANDSHAKE_ID:
                    message = parseHandshake(inputStream);
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return message;
    }
	public static DataMessage parseHandshake(InputStream input) throws IOException {
		HandshakeMessage handshakeMessage = HandshakeMessage.decodeHandshake(input);
		//String filename = handshakeMessage.getFileName();
		//PeerInfo peer = handshakeMessage.getPeer();
		return new DataMessage(DataMessage.MessageID.HANDSHAKE_ID, handshakeMessage);
	}
	
	public static DataMessage parseBitfield(InputStream input) throws IOException {
        byte[] bitfieldLengthArray = new byte[INT_BYTE_LEN];
        input.read(bitfieldLengthArray, 0, INT_BYTE_LEN);
        int bitfieldLength = byteToInt(bitfieldLengthArray);

        byte[] bitfieldArray = new byte[bitfieldLength];
        input.read(bitfieldArray, 0, bitfieldLength);
        BitField bitfield = new BitField(bitfieldLength, BitSet.valueOf(bitfieldArray));
        return new DataMessage(DataMessage.MessageID.BITFIELD_ID, bitfield);
    }
	
	public static DataMessage parseHave(InputStream inputStream) throws IOException {
        int pieceIndex = readIntFromStream(inputStream);
        return new DataMessage(DataMessage.MessageID.HAVE_ID, pieceIndex);
    }
	
	public static DataMessage parseRequest(InputStream input) throws IOException {
        byte[] pieceIndexArray = new byte[INT_BYTE_LEN];
        byte[] beginArray = new byte[INT_BYTE_LEN];
        byte[] lengthArray = new byte[INT_BYTE_LEN];

        input.read(pieceIndexArray, 0, INT_BYTE_LEN);
        input.read(beginArray, 0, INT_BYTE_LEN);
        input.read(lengthArray, 0, INT_BYTE_LEN);

        int pieceIndex = byteToInt(pieceIndexArray);
        int begin = byteToInt(beginArray);
        int length = byteToInt(lengthArray);
        Request request = new Request(pieceIndex, begin, length);
        return new DataMessage(DataMessage.MessageID.REQUEST_ID, request);
    }
	
	public static DataMessage parsePiece(InputStream input) throws IOException {
        byte[] pieceIndexArray = new byte[INT_BYTE_LEN];
        byte[] beginArray = new byte[INT_BYTE_LEN];
        byte[] blockLengthArray = new byte[INT_BYTE_LEN];

        input.read(pieceIndexArray, 0, INT_BYTE_LEN);
        input.read(beginArray, 0, INT_BYTE_LEN);
        input.read(blockLengthArray, 0, INT_BYTE_LEN);

        int pieceIndex = byteToInt(pieceIndexArray);
        int begin = byteToInt(beginArray);
        int blockLength = byteToInt(blockLengthArray);
        byte[] block = new byte[blockLength];
        int read = 0;
        while (read < blockLength){
            read += input.read(block, read, blockLength - read);
        }
        Piece piece = new Piece(pieceIndex, begin, block);
        return new DataMessage(DataMessage.MessageID.PIECE_ID, piece);
    }
	
	
	public static int readIntFromStream(InputStream inputStream) {
        byte[] i = new byte[INT_BYTE_LEN];
        try {
            int j = inputStream.read(i);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return byteToInt(i);
    }
	
	public static byte[] intToByte(int i) {
        ByteBuffer b = ByteBuffer.allocate(INT_BYTE_LEN);
        b.order(ByteOrder.BIG_ENDIAN);
        b.putInt(i);
        return b.array();
    }
	
	public static int byteToInt(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getInt();
    }

}
