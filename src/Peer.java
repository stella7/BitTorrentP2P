import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.retry.RetryNTimes;
import org.apache.log4j.BasicConfigurator;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher.Event;

public class Peer implements MessageConstants{
    private static final int NUM_THREADS = 8;
    private static final int BACKLOG = 10;
    private static final String CMD_USAGE = "NORMAL: java Client name port metafile directory\n" +
            "SHARING: java Client name port file trackerIP trackerPort";
    private static final int UNCHOKE_INTERVAL = 5;
    static PeerInfo myPeer;
    static Logger log;
    static final String ZK_CONNECT_STR = "snorkel.uwaterloo.ca:2181";
	static String zkNode = System.getProperty("user.name");
	private static CuratorFramework curClient;
	static ConcurrentMap<PeerInfo, Connection> connectionMap = new ConcurrentHashMap<>();
	static boolean isSeeder = false;
	static BitField myBitField;
	static String fileName = "";
	static String trackerHost = "ecelinux2";
    static int trackerPort = 10123;
    static PeerTracker peerTracker;
	public static ConcurrentHashMap<String,BitField> bitMap = new ConcurrentHashMap<>();
	
    public static void main(String[] args) throws Exception {

    	BasicConfigurator.configure();
    	String peerId = args[0];
    	log = new Logger("peer_" + peerId);
    	int port = Integer.parseInt(args[1]);
    	String hostFull = InetAddress.getLocalHost().getHostName();
        String host = hostFull.substring(0, hostFull.indexOf("."));
        myPeer = new PeerInfo(peerId, host, port);
        
        String path = "";
        long fileLen = 0;
        int pieceLen = 256 * 1024;
    	if (args.length == 4) { // start as leecher
    		fileName = args[2];
    		path = args[3];
    	}else if(args.length == 5){ // start as seeder
    		trackerHost = args[2];
    		trackerPort = Integer.parseInt(args[3]);
    		Path filePath = Paths.get(args[4]);
    		File fileData = filePath.toFile();
    		fileLen = fileData.length();    		
    		fileName = filePath.getFileName().toString();
    		path = filePath.getParent().toString();
    		isSeeder = true;
    	}else{
    		System.out.println(args.length);
            System.out.println(CMD_USAGE);
            System.exit(0);
    	}

		Peer peer = new Peer();
		peer.start();
		
		FileInfo newFile = new FileInfo(isSeeder, fileName, path, fileLen, pieceLen);
		myBitField = newFile.getBitField();
		bitMap.put(fileName, myBitField);
		peerTracker = new PeerTracker(trackerHost,trackerPort,myPeer, newFile);
		//find seeders for the filename and connect to those peers, send HANDSHAKE message
		peer.firstRequest(peerTracker);
		//start listening port to accept connection from newly joined peers
		new Thread(new ListeningThread(myPeer, BACKLOG, connectionMap, log, newFile)).start();
		//set watcher on zknode, if child node changed, peer will send request to tracker
		new Thread(peer.new peerNodeChanged()).start();
    }
    
	void start() throws Exception {
		curClient =
			    CuratorFrameworkFactory.builder()
			    .connectString(ZK_CONNECT_STR)
			    .retryPolicy(new RetryNTimes(10, 1000))
			    .connectionTimeoutMs(1000)
			    .sessionTimeoutMs(10000)
			    .build();
		
		curClient.start();
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
			    curClient.close();
			}
		    });
		
		curClient.create()
		 .creatingParentsIfNeeded()
		 .withMode(CreateMode.EPHEMERAL)
		 .forPath("/" + zkNode + "/" + myPeer.getPeerId(), String.format("%s:%s", myPeer.getHost(), myPeer.getPort()).getBytes());
	}
	
	private class peerNodeChanged implements Runnable{
		public void run() {
			try {
				curClient.checkExists().usingWatcher(new peerWatcher()).forPath("/" + zkNode);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}			
    }
	
	private class peerWatcher implements CuratorWatcher {
    	public void process(WatchedEvent event) {
    		if(event.getType() == Event.EventType.NodeChildrenChanged) {
    			new Thread(new peerNodeChanged()).start();
    			log.writeLog("send REQUEST to tracker for updating peer list");
    			try {
					TrackerResponse resp = peerTracker.getRequest(TrackerRequest.RequestEvent.EMPTY);
					List<PeerInfo> peers = resp.getPeers();
					for (PeerInfo peer : connectionMap.keySet()) {
		                if (!peers.contains(peer)) {
		                    Connection connection = connectionMap.get(peer);
		                    connectionMap.remove(peer);
		                    log.writeLog("removing peer " + peer + " for file " + fileName);
		                    connection.getSocket().close();
		                }
		            }
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		}
    	}
	}
    
    public void firstRequest(PeerTracker peerTracker) throws Exception{
    	TrackerResponse firstResp;
		
		if(isSeeder){
			log.writeLog("send COMPLETED Request");
			firstResp = peerTracker.getRequest(TrackerRequest.RequestEvent.COMPLETED);
			log.writeLog("get peers information from COMPLETED Response");

		}else{
			log.writeLog("send STARTED Request");
			firstResp = peerTracker.getRequest(TrackerRequest.RequestEvent.STARTED);
			log.writeLog("get peers information from STARTED Response");				
		}			
		List<PeerInfo> peers = firstResp.getPeers();
		if(peers.isEmpty()){
			log.writeLog("does NOT find  seeder for file: " + peerTracker.getFile().getFilename());
		}else{
			connectToPeers(peers);
		}
    }
    
    public void connectToPeers(List<PeerInfo> peersToConnect){
    	for (PeerInfo peer : peersToConnect) {
            if (!myPeer.equals(peer)) {
                log.writeLog("Initializing connection to peer: " + peer);
                Socket socket;
				try {
					socket = new Socket(peer.getHost(), peer.getPort());
					Connection connection = Connection.init(peer, socket);
					connectionMap.put(peer, connection);
					
					//To be continued...
					
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                
            }
        }	
    }
    
    public void showPeers(TrackerResponse resp){
    	List<PeerInfo> peers = resp.getPeers();
		for(PeerInfo peer : peers){
			System.out.println(peer.getPeerId());
		}
    }
}
