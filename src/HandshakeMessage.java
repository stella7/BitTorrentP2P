
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class HandshakeMessage implements MessageConstants 
{
	// Attributes
	private byte[] header = new byte[HANDSHAKE_HEADER_LEN];
	private byte[] peerInfo;
	private byte[] filenameBytes;
	//private byte[] zeroBits = new byte[HANDSHAKE_ZEROBITS_LEN];
	private String messageHeader;
	private PeerInfo messagePeerInfo;
	private String filename;


	public HandshakeMessage(String Header, PeerInfo Peer, String fileName) {

		try {
			this.messageHeader = Header;
			this.header = Header.getBytes(MSG_CHARSET_NAME);
			if (this.header.length > HANDSHAKE_HEADER_LEN)
				throw new Exception("Header is too large.");

			this.messagePeerInfo = Peer;
			this.peerInfo = Peer.encodePeer();
			this.filename = fileName;
			filenameBytes = fileName.getBytes();

		} catch (Exception e) {
			//peerProcess.showLog(e.toString());
		}

	}
	//HEADER: HEADER PerInfo FileName
    public byte[] encodeHandshake() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] filenameBytes = filename.getBytes(StandardCharsets.UTF_8);
        try {
        	out.write(intToByte(DataMessage.MessageID.HANDSHAKE_ID.ordinal()));
            //out.write(intToByte(DataMessage.MessageID.HANDSHAKE_ID.ordinal()));
            out.write(HANDSHAKE_HEADER.getBytes(StandardCharsets.US_ASCII));
            //out.write(peer.encodePeer());
            out.write(intToByte(messagePeerInfo.getPeerId().length()));
            out.write(messagePeerInfo.getPeerId().getBytes(StandardCharsets.US_ASCII));
            out.write(intToByte(messagePeerInfo.getHost().length()));
			out.write(messagePeerInfo.getHost().getBytes(StandardCharsets.US_ASCII));
			out.write(intToByte(messagePeerInfo.getPort()));
			
            out.write(intToByte(filenameBytes.length));
            out.write(filenameBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }
	
	public static HandshakeMessage decodeHandshake(InputStream input) throws IOException {
/*
		DataInputStream dis = new DataInputStream(input);
		byte[] headerArray = new byte[HANDSHAKE_HEADER_LEN];
		dis.read(headerArray, 0, HANDSHAKE_HEADER_LEN);
		String header = new String(headerArray, StandardCharsets.US_ASCII);
		System.out.println(header);
		//read peer Id
		int idLen = dis.readInt();
    	byte[] idRaw = new byte[idLen];
    	dis.read(idRaw, 0, idLen);
    	String pId = new String(idRaw, StandardCharsets.US_ASCII);
    	//read peer host
    	int hostLen = dis.readInt();
    	byte[] hostRaw = new byte[hostLen];
    	dis.read(hostRaw, 0, hostLen);
    	String pHost = new String(hostRaw, StandardCharsets.US_ASCII);
    	//read peer port
    	int pPort = dis.readInt();
    	PeerInfo peer = new PeerInfo(pId, pHost, pPort);
    	int filelen = dis.readInt();
		byte[] fileRaw = new byte[filelen];
		dis.read(fileRaw, 0, filelen);
 		String filename = new String(fileRaw, StandardCharsets.US_ASCII);
*/
		byte[] headerArray = new byte[HANDSHAKE_HEADER_LEN];
		byte[] peerIdLengthArray = new byte[INT_BYTE_LEN];
		byte[] peerHostLengthArray = new byte[INT_BYTE_LEN];
		byte[] peerPortArray = new byte[INT_BYTE_LEN];
		byte[] filenameLengthArray = new byte[INT_BYTE_LEN];
		input.read(headerArray, 0, HANDSHAKE_HEADER_LEN);
		String header = new String(headerArray, StandardCharsets.UTF_8);
		input.read(peerIdLengthArray, 0, INT_BYTE_LEN);
		int idLength = byteToInt(peerIdLengthArray);
		byte[] idArray = new byte[idLength];
		input.read(idArray, 0, idLength);
		String peerId = new String(idArray, StandardCharsets.UTF_8);
		
		input.read(peerHostLengthArray, 0, INT_BYTE_LEN);
		int hostLength = byteToInt(peerHostLengthArray);
		byte[] hostArray = new byte[hostLength];
		input.read(hostArray, 0, hostLength);
		String peerHost = new String(hostArray, StandardCharsets.UTF_8);
		
		input.read(peerPortArray, 0, INT_BYTE_LEN);
		int peerPort = byteToInt(peerPortArray);
		
		PeerInfo peer = new PeerInfo(peerId, peerHost, peerPort);
		
		input.read(filenameLengthArray, 0, INT_BYTE_LEN);
		int filenameLength = byteToInt(filenameLengthArray);
		byte[] filenameArray = new byte[filenameLength];
		input.read(filenameArray, 0, filenameLength);
		String filename = new String(filenameArray, StandardCharsets.UTF_8);
		

        return new HandshakeMessage(header, peer, filename);
    }
	
	public String getHeaderString() {
		return messageHeader;
	}
	
	public PeerInfo getPeer(){
		return messagePeerInfo;
	}
	
	public String getFileName(){
		return filename;
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
