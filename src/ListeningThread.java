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
		byte []handshakeBuff = new byte[32];
		byte []dataBuffWithoutPayload = new byte[DATA_MSG_LEN + DATA_MSG_TYPE];
		byte[] msgLength;
		byte[] msgType;
	    try {
	        ServerSocket socket = new ServerSocket(peer.getPort(), backlog);
	        while (true) {
	            // Accept peer connections
	            try {
	                Socket peerSocket = socket.accept();
	                log.writeLog("accepted new connection from " + peerSocket.getInetAddress() + " at port " + peerSocket.getPort());
	                InputStream in = peerSocket.getInputStream();
	                OutputStream out = peerSocket.getOutputStream();
	                in.read(handshakeBuff);
	                HandshakeMessage handshakeMessage = HandshakeMessage.decodeMessage(handshakeBuff);
	                if(handshakeMessage.getHeaderString().equals(MessageConstants.HANDSHAKE_HEADER))
					{
						
						String remotePeerId = handshakeMessage.getPeerIDString();
						
						log.writeLog(" makes a connection to Peer " + remotePeerId);
						
						log.writeLog(" Received a HANDSHAKE message from Peer " + remotePeerId);
						
						//populate peerID to socket mapping
						peerProcess.peerIDToSocketMap.put(remotePeerId, this.peerSocket);
						break;
					}
					else
					{
						continue;
					}
	
	                Peer peer = new Peer(message.getPeerIp(), message.getPeerPort());
	                connections.put(peer, Connection.getInitialState(peerSocket));
	
	                MessageSender.sendBitfield(connections.get(peer), peer, logger, datafile.getBitfield());
	                //                    byte[] bitfieldMessage = MessageBuilder.buildBitfield(datafile.getBitfield().getByteArray());
	                //                    MessageSender.sendMessage(peerSocket, bitfieldMessage);
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
	        }
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
}
