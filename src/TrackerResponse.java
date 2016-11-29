import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class TrackerResponse {
	int seederNum;
	List<PeerInfo> seedPeers;
	String zkPath;
	public TrackerResponse(int sNum, List<PeerInfo> sPeers){
		seederNum = sNum;
		seedPeers = sPeers;
	}
	/*
	public TrackerResponse(int sNum, List<PeerInfo> sPeers, String zkPath){
		seederNum = sNum;
		seedPeers = sPeers;
		this.zkPath = zkPath;
	}
	*/
	
	public static TrackerResponse decodeResponse(InputStream in) throws IOException{
		DataInputStream dis = new DataInputStream(in);
		int sNum = dis.readInt();
		List<PeerInfo> peers = new ArrayList<>();
        //byte[] raw = new byte[4];
        for (int i = 0; i < sNum; i++) {
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
            peers.add(new PeerInfo(pId, pHost, pPort));
        }
        /*
        int pathLen = dis.readInt();
    	byte[] pathRaw = new byte[pathLen];
    	dis.read(pathRaw, 0, pathLen);
    	String zkPath = new String(pathRaw);
        */
        return new TrackerResponse(sNum, peers);
	}
	
	public void sendResponse(OutputStream out) throws IOException{
		 DataOutputStream dos = new DataOutputStream(out);
		 dos.writeInt(seederNum);

		 if (seedPeers == null)
			 return;
		 for (PeerInfo peer : seedPeers) {
			 dos.writeInt(peer.getPeerId().length());
			 dos.write(peer.getPeerId().getBytes(StandardCharsets.US_ASCII));
			 dos.writeInt(peer.getHost().length());
			 dos.write(peer.getHost().getBytes(StandardCharsets.US_ASCII));
			 dos.writeInt(peer.getPort());
		 }
		 
		// dos.writeInt(zkPath.length());
		// dos.write(zkPath.getBytes(StandardCharsets.US_ASCII));
	}
	
	public List<PeerInfo> getPeers(){
		return seedPeers;
	}
}
