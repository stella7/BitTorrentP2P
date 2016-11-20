import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;



public class PeerTracker {
	PeerInfo myPeer;
	String trackerHost;
	int trackerPort;
	FileInfo file;
	public PeerTracker(String trackerHost, int trackerPort, PeerInfo peer, FileInfo file){
		this.myPeer = peer;
		this.trackerHost = trackerHost;
		this.trackerPort = trackerPort;
		this.file = file;
	}
	
	public TrackerResponse getRequest(TrackerRequest.RequestEvent event) throws IOException {

        try (Socket socket = new Socket(trackerHost, trackerPort)) {
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();
            TrackerRequest req = new TrackerRequest(event, myPeer, file.getFilename());
            req.sendRequest(out);
            return TrackerResponse.decodeResponse(in);
        }
    }
	
	public FileInfo getFile(){
		return file;
	}
}
