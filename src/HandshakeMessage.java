
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
    public static byte[] encodeHandshake(String filename, PeerInfo peer) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] filenameBytes = filename.getBytes(StandardCharsets.UTF_8);
        try {
            //out.write(intToByte(DataMessage.MessageID.HANDSHAKE_ID.ordinal()));
            out.write(HANDSHAKE_HEADER.getBytes());
            out.write(peer.encodePeer());
            out.write(intToByte(filenameBytes.length));
            out.write(filenameBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }
	
	public static HandshakeMessage decodeHandshake(InputStream input) throws IOException {
		DataInputStream dis = new DataInputStream(input);
		byte[] headerArray = new byte[HANDSHAKE_HEADER_LEN];
		dis.read(headerArray, 0, HANDSHAKE_HEADER_LEN);
		String header = new String(headerArray, StandardCharsets.US_ASCII);
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

        return new HandshakeMessage(header, peer, filename);
    }
	
	public String getHeaderString() {
		return messageHeader;
	}
	
	public PeerInfo getPeer(){
		return messagePeerInfo;
	}
	
	public static byte[] intToByte(int i) {
        ByteBuffer b = ByteBuffer.allocate(INT_BYTE_LEN);
        b.order(ByteOrder.BIG_ENDIAN);
        b.putInt(i);
        return b.array();
    }


}
