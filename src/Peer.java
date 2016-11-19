import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.log4j.BasicConfigurator;
import org.apache.zookeeper.CreateMode;

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
	ConcurrentMap<PeerInfo, Connection> connectionMap = new ConcurrentHashMap<>();
	static boolean isSeeder = false;
	static BitField myBitField;
	public static ConcurrentHashMap<String,BitField> bitMap = new ConcurrentHashMap<>();
	
    public static void main(String[] args) throws Exception {

    	BasicConfigurator.configure();
    	String peerId = args[0];
    	log = new Logger("peer_" + peerId);
    	int port = Integer.parseInt(args[1]);
    	String hostFull = InetAddress.getLocalHost().getHostName();
        String host = hostFull.substring(0, hostFull.indexOf("."));
        myPeer = new PeerInfo(peerId, host, port);
        String trackerHost = "ecelinux2";
        int trackerPort = 10123;
        String fileName = "";
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
		peer.firstRequest(trackerHost, trackerPort, fileName);

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
    
    public void firstRequest(String trackerHost, int trackerPort, String fileName) throws Exception{
    	TrackerRequest firstReq;
    	TrackerResponse firstResp;
		try(Socket socket = new Socket(trackerHost, trackerPort)) {
			OutputStream out = socket.getOutputStream();
			InputStream in = socket.getInputStream();
			if(isSeeder){
				firstReq = new TrackerRequest(TrackerRequest.RequestEvent.COMPLETED, myPeer, fileName);
				log.writeLog("send COMPLETED Request");
				firstReq.sendRequest(out);
				firstResp = TrackerResponse.decodeResponse(in);
				log.writeLog("get peers information from COMPLETED Response");

			}else{
				firstReq = new TrackerRequest(TrackerRequest.RequestEvent.STARTED, myPeer, fileName);
				log.writeLog("send STARTED Request");
				firstReq.sendRequest(out);
				firstResp = TrackerResponse.decodeResponse(in);
				log.writeLog("get peers information from STARTED Response");				
			}			
		}
		List<PeerInfo> peers = firstResp.getPeers();
		if(peers.isEmpty()){
			log.writeLog("find NO seeder for file: " + fileName);
		}else{
			connectToPeers(peers);
		}
		
		
    	while(true){
    		
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
