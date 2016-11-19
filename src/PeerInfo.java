
public class PeerInfo {
	public String peerId;
	public String peerHost;
	public int peerPort;
	
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
}
