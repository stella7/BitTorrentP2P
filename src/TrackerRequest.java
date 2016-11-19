import java.io.*;
import java.nio.charset.StandardCharsets;




public class TrackerRequest {
	private RequestEvent event;
    private PeerInfo peerInfo;
    private String fileName;
    
    public TrackerRequest(RequestEvent pEvent, PeerInfo pInfo, String fName) {
        event = pEvent;
        peerInfo = pInfo;
        fileName = fName;
    }
    
    public static TrackerRequest decodeRequest(InputStream in) throws IOException{    	
    	DataInputStream dis = new DataInputStream(in);
    	RequestEvent event = RequestEvent.values()[dis.readShort()];
    	//read peerId
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
    	
    	
    	//read file
    	int filelen = dis.readInt();
		byte[] fileRaw = new byte[filelen];
		dis.read(fileRaw, 0, filelen);
 		String filename = new String(fileRaw, StandardCharsets.US_ASCII);
 
 		return new TrackerRequest(event, peer, filename);
    }
    
    public void sendRequest(OutputStream out) throws IOException{
    	DataOutputStream dos = new DataOutputStream(out);
        dos.writeShort(event.ordinal());
        //dos.write(peerInfo.encodePeer());
        dos.writeInt(peerInfo.getPeerId().length());
        dos.write(peerInfo.getPeerId().getBytes(StandardCharsets.US_ASCII));
        dos.writeInt(peerInfo.getHost().length());
        dos.write(peerInfo.getHost().getBytes(StandardCharsets.US_ASCII));
        dos.writeInt(peerInfo.getPort());
        dos.writeInt(fileName.length());
        dos.write(fileName.getBytes(StandardCharsets.US_ASCII));
        dos.flush();
    }
    
    public RequestEvent getEvent(){
    	return event;
    }
    
    public PeerInfo getPeer(){
    	return peerInfo;
    }
    
    public String getFileName(){
    	return fileName;
    }
    
	public enum RequestEvent {
        STARTED,
        STOPPED,
        COMPLETED,
    }
}
