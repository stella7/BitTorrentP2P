import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentMap;


public class ListeningThread implements Runnable, MessageConstants{
	private PeerInfo peer;
	private int backlog;
	private Logger log;
	private FileInfo datafile;
	private ConcurrentMap<PeerInfo, Connection> connections;
	 
	public ListeningThread(PeerInfo peer, int backlog, ConcurrentMap<PeerInfo, Connection> connections, Logger log, FileInfo datafile){
		this.peer = peer;
        this.backlog = backlog;
        this.connections = connections;
        this.log = log;
        this.datafile = datafile;
	}

	public void run() {

	    try {
	        ServerSocket socket = new ServerSocket(peer.getPort(), backlog);
	        while (true) {
	            // Accept peer connections
	            try {
	                Socket peerSocket = socket.accept();
	                log.writeLog("accepted new connection from " + peerSocket.getInetAddress() + " at port " + peerSocket.getPort());
	                InputStream in = peerSocket.getInputStream();
	                //OutputStream out = peerSocket.getOutputStream();

	                HandshakeMessage handshakeMessage = HandshakeMessage.decodeHandshake(in);
	                if(handshakeMessage.getHeaderString().equals(MessageConstants.HANDSHAKE_HEADER))
					{
						
						PeerInfo remotePeer = handshakeMessage.getPeer();
																
						log.writeLog(" Received a HANDSHAKE message from Peer " + remotePeer.getPeerId());
						log.writeLog(" makes a connection to Peer " + remotePeer.getPeerId());
						//populate peerID to socket mapping
						connections.put(remotePeer, Connection.init(remotePeer, peerSocket));
						//MessageSender.sendBitfield(connections.get(peer), peer, logger, datafile.getBitfield());
						
					}
					else
					{
						log.writeLog("HANDSHAKE header does NOT match, reject connection from peer " + handshakeMessage.getPeer().getPeerId());
						continue;
					}
	
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
	        }
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
}
