import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class PeerInfo {
	public String peerId;
	public String peerHost;
	public int peerPort;
	public static final int intByteLength = 4;
	public int state = -1;
	public PeerInfo(String pId, String pHost, int pPort){
		peerId = pId;
		peerHost = pHost;
		peerPort = pPort;
	}
	
	public String getPeerId(){
		return peerId;
	}
	
	public String getHost(){
		return peerHost;
	}
	
	public int getPort(){
		return peerPort;
	}
	
	public boolean equals(PeerInfo p){
		if(peerId.equals(p.getPeerId()) && peerHost.equals(p.getHost()) && peerPort == p.getPort())
			return true;
		else
			return false;
	}
	
	public void setState(int state){
		this.state = state;
	}
	
	public int getState(){
		return state;
	}
	
	public byte[] encodePeer(){
		/*
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			out.write(peer.getPeerId().getBytes(StandardCharsets.US_ASCII));
			out.write(intToByte(peer.getPort()));
			out.write(peer.getHost().getBytes(StandardCharsets.US_ASCII));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		ByteArrayOutputStream out = new ByteArrayOutputStream();        
        try {
        	out.write(intToByte(this.getPeerId().length()));
            out.write(this.getPeerId().getBytes(StandardCharsets.US_ASCII));
            out.write(intToByte(this.getHost().length()));
			out.write(this.getHost().getBytes(StandardCharsets.US_ASCII));
			 out.write(intToByte(this.getPort()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
       
		return out.toByteArray();
	}
	
	public PeerInfo decodePeer(InputStream in) throws IOException{
		DataInputStream dis = new DataInputStream(in);
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
    	return new PeerInfo(pId, pHost, pPort);
	}
	
	public static byte[] intToByte(int i) {
        ByteBuffer b = ByteBuffer.allocate(intByteLength);
        b.order(ByteOrder.BIG_ENDIAN);
        b.putInt(i);
        return b.array();
    }
	
	public static int byteToInt(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getInt();
    }
}
