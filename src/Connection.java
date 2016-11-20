import java.net.Socket;


public class Connection {
	private PeerInfo myPeer; 
	private Socket myPeerSocket;

	private boolean isSeeder;
	private boolean isConnected;
    private BitField bitfield;            // bitfield of the peer
    private State downloadState;
    private State uploadState;
    
    private float myUploadRate;
	private float myDownloadRate;
    private long bytesDownloaded;
    private long bytesUploaded;

    private long initTime;
    
    public Connection(PeerInfo peer, Socket socket, boolean isConnected, State downloadState, State uploadState) {
    	myPeer = peer;
    	myPeerSocket = socket;
        this.isConnected = isConnected;
        this.downloadState = downloadState;
        this.uploadState = uploadState;
    }
    
    public static Connection init(PeerInfo peer, Socket socket){
    	State download = new State(true, false);
    	State upload = new State(true, false);
    	return new Connection(peer, socket, true, download, upload);
    }
    
    public Socket getSocket() {
        return myPeerSocket;
    }

}
