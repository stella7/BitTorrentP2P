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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.retry.RetryNTimes;
import org.apache.log4j.BasicConfigurator;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher.Event;

public class Peer implements MessageConstants{
    private static final int NUM_THREADS = 16;
    private static final int BACKLOG = 10;
    private static final String CMD_USAGE = "NORMAL: java Client name port metafile directory\n" +
            "SHARING: java Client name port file trackerIP trackerPort";
    private static final int UNCHOKE_INTERVAL = 1;
    static PeerInfo myPeer;
    static Logger log;
    static final String ZK_CONNECT_STR = "snorkel.uwaterloo.ca:2181";
	static String zkNode = "s66he";
	private static CuratorFramework curClient;
	static ConcurrentMap<PeerInfo, Connection> connectionMap = new ConcurrentHashMap<>();
	static boolean isSeeder = false;
	static BitField myBitField;
	static String fileName = "";
	static String trackerHost;
    static int trackerPort;
    static PeerTracker peerTracker;
    static MetaFile metaFile;
    static long fileLen;
    static int pieceLen;
    static FileInfo newFile;
	public static ConcurrentHashMap<String,BitField> bitMap = new ConcurrentHashMap<>();
	public static volatile Queue<DataMessage> messageQ = new LinkedList<DataMessage>();
	static final int PIECE_LEN = 256 * 1024;
	static ScheduledExecutorService executor = Executors.newScheduledThreadPool(NUM_THREADS);
	
	public static synchronized void addToMsgQueue(DataMessage msg)
	{
		messageQ.add(msg);
	}
	
	public static synchronized DataMessage removeFromMsgQueue()
	{
		DataMessage msg = null;
		
		if(!messageQ.isEmpty())
		{
			msg = messageQ.remove();
		}
		
		return msg;
	}
	
    public static void main(String[] args) throws Exception {

    	BasicConfigurator.configure();
    	String peerId = args[0];
    	log = new Logger("peer_" + peerId);
    	int myPort = Integer.parseInt(args[1]);
    	String hostFull = InetAddress.getLocalHost().getHostName();
        String myHost = hostFull.substring(0, hostFull.indexOf("."));
        myPeer = new PeerInfo(peerId, myHost, myPort);
        
        String path = "";
        //long fileLen = 0;
        //int pieceLen = 256 * 1024;
    	if (args.length == 4) { // start as leecher
    		fileName = args[2];
    		path = args[3];
    		myPeer.setState(0);
    	}else if(args.length == 5){ // start as seeder
    		trackerHost = args[2];
    		trackerPort = Integer.parseInt(args[3]);
    		Path filePath = Paths.get(args[4]);
    		File fileData = filePath.toFile();
    		fileLen = fileData.length();  
    		
    		System.out.println(fileLen);
    		
    		fileName = filePath.getFileName().toString();
    		path = filePath.getParent().toString();
    		pieceLen = PIECE_LEN;
    		isSeeder = true;
    		myPeer.setState(1);
    	}else{
    		System.out.println(args.length);
            System.out.println(CMD_USAGE);
            System.exit(0);
    	}
    	
    	//System.out.println(isSeeder);
		Peer peer = new Peer();
		List<String> children = peer.start();
		
		//System.out.println(trackerHost + trackerPort);
		
		ConcurrentHashMap<PeerInfo, Float> unchokedPeers = new ConcurrentHashMap<>();
		
		newFile = new FileInfo(isSeeder, fileName, path, fileLen, pieceLen);
		myBitField = newFile.getBitField();
		//System.out.println(myBitField.getBitField());
		bitMap.put(fileName, myBitField);
		
		peer.initialize(children);
		//peerTracker = new PeerTracker(trackerHost,trackerPort,myPeer, newFile);
		//find seeders for the filename and connect to those peers, send HANDSHAKE message
		//peer.firstRequest(peerTracker, connectionMap);
		//start listening port to accept connection from newly joined peers
		
		//set watcher on zknode, if child node changed, peer will send request to tracker
		new Thread(peer.new peerNodeChanged()).start();

        executor.scheduleAtFixedRate(new ChokeTask(connectionMap, newFile, unchokedPeers, log), 0, UNCHOKE_INTERVAL, TimeUnit.SECONDS);

        new Thread(new ListeningThread(myPeer, BACKLOG, connectionMap, log, newFile)).start();
        //System.out.println("Start respondTask");

        new Thread(new RespondTask(connectionMap, unchokedPeers, newFile, executor, log)).start();
    }
    
	public List<String> start() throws Exception {
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
		 .forPath("/" + zkNode + "/peer/" + myPeer.getPeerId(), String.format("%s:%s", myPeer.getHost(), myPeer.getPort()).getBytes());
		
		if(isSeeder){
			metaFile = new MetaFile(fileName, pieceLen, fileLen, trackerHost, trackerPort);
			curClient.create()
			 .creatingParentsIfNeeded()
			 .withMode(CreateMode.PERSISTENT)
			 .forPath("/" + zkNode + "/file/" + fileName, metaFile.createMetaFile());
		}else{
			byte[] data = curClient.getData().forPath("/" + zkNode + "/file/" + fileName);
			
			String strData = new String(data);
		    String[] primary = strData.split(";");
		    String[] info = primary[0].split(",");
		    String[] announce = primary[1].split(",");
		    pieceLen = Integer.parseInt(info[1]);
		    fileLen = Long.parseLong(info[2]);
		    trackerHost = announce[0];
		    trackerPort = Integer.parseInt(announce[1]);
		    metaFile = new MetaFile(fileName, pieceLen, fileLen, trackerHost, trackerPort);
		}
		
		List<String> children = 
    			curClient.getChildren().forPath("/" + zkNode + "/file/" + fileName);
		return children;
	}
	
	public void initialize(List<String> children) throws Exception{
		
		curClient.create()
		 .creatingParentsIfNeeded()
		 .withMode(CreateMode.EPHEMERAL)
		 .forPath("/" + zkNode + "/file/" + fileName +"/"+ myPeer.getPeerId(), String.format("%s:%s", myPeer.getHost(), myPeer.getPort()).getBytes());
		if(children.isEmpty()) return;	
		
		for(String child : children){
			//System.out.println(child);
			byte[] data = curClient.getData().forPath("/" + zkNode + "/peer/" + child);
			String strData = new String(data);
		    String[] primary = strData.split(":");
		    String peerHost = primary[0];
		    int peerPort = Integer.parseInt(primary[1]);
		    PeerInfo remotePeer = new PeerInfo(child, peerHost, peerPort);
		    connectToRemotePeer(remotePeer, connectionMap);
		}
		//System.out.println("finish init");
		
	}
	
	private class peerNodeChanged implements Runnable{
		public void run() {
			try {
				List<String> peers = curClient.getChildren().usingWatcher(new peerWatcher()).forPath("/" + zkNode + "/file/" + fileName);
				for (PeerInfo peer : connectionMap.keySet()) {
	                if (!peers.contains(peer.getPeerId())) {
	                    Connection connection = connectionMap.get(peer);
	                    connectionMap.remove(peer);
	                    log.writeLog("removing peer_" + peer.getPeerId() + " from file " + fileName + " list");
	                    connection.getSocket().close();
	                }
	            }
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.writeLog("exits");
				System.exit(0);
			}
		}			
    }
	
	private class peerWatcher implements CuratorWatcher {
    	public void process(WatchedEvent event) {
    		if(event.getType() == Event.EventType.NodeChildrenChanged) {
    			
    			//log.writeLog("send EMPTY request to tracker for updating peer list");
    			log.writeLog("get update peer list");
    			//try {
    			new Thread(new peerNodeChanged()).start();
    				/*
					TrackerResponse resp = peerTracker.getRequest(TrackerRequest.RequestEvent.EMPTY);
					List<PeerInfo> peers = resp.getPeers();
					for (PeerInfo peer : connectionMap.keySet()) {
		                if (!peers.contains(peer)) {
		                    Connection connection = connectionMap.get(peer);
		                    connectionMap.remove(peer);
		                    log.writeLog("removing peer_" + peer.getPeerId() + " from file " + fileName + " list");
		                    connection.getSocket().close();
		                }
		            }
					*/
				//} catch (IOException e) {
					// TODO Auto-generated catch block
				//	e.printStackTrace();
				//}
    		}
    	}
	}
    
    public void firstRequest(PeerTracker peerTracker, ConcurrentMap<PeerInfo, Connection> connections) throws Exception{
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
			connectToPeers(peers, connections);
		}
    }
    
    public void connectToPeers(List<PeerInfo> peersToConnect, ConcurrentMap<PeerInfo, Connection> connections){
    	for (PeerInfo remotePeer : peersToConnect) {
            if (!myPeer.equals(remotePeer)) {
                log.writeLog("Initializing connection to peer: " + remotePeer.getPeerId());
                Socket socket;
				try {
					socket = new Socket(remotePeer.getHost(), remotePeer.getPort());
					Connection connection = Connection.init(myPeer, socket);
					connections.put(remotePeer, connection);
					//Thread sendingThread = new Thread(new RespondTask(connections, connection, remotePeer, log));
					//sendingThread.start();
					MessageProcessor.sendHandshake(connection, remotePeer, log, newFile);
					System.out.println("Send bitfield");
					MessageProcessor.sendBitfield(connection, remotePeer, log, myBitField);
					//MessageProcessor.sendInterested(connection, remotePeer, log);
					
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
    
    public void connectToRemotePeer(PeerInfo remotePeer, ConcurrentMap<PeerInfo, Connection> connections){
    	log.writeLog("Initializing connection to peer: " + remotePeer.getPeerId());
        Socket socket;
		try {
			socket = new Socket(remotePeer.getHost(), remotePeer.getPort());
			Connection connection = Connection.init(myPeer, socket);
			connections.put(remotePeer, connection);
			//Thread sendingThread = new Thread(new RespondTask(connections, connection, remotePeer, log));
			//sendingThread.start();
			//System.out.println(connection.getSocket());
			MessageProcessor.sendHandshake(connection, remotePeer, log, newFile);
			//System.out.println("Send bitfield");
			//MessageSender.sendBitfield(connection, remotePeer, log, myBitField);
			//MessageProcessor.sendInterested(connection, remotePeer, log);
			
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
 /*   
    public class ResponderTask implements Runnable{
    	
    	public void run() {
    		
            while (true) {
            	//System.out.println("Responder running");
                for (Map.Entry<PeerInfo, Connection> connection : connectionMap.entrySet()) {
                    try {
                        InputStream input = connection.getValue().getSocket().getInputStream();
                        if (input.available() == 0) {
                        	//System.out.println("input empty");
                            continue;
                        }
                        System.out.println(connection.getKey().getPeerId());
                        DataMessage message = MessageProcessor.parseMessage(input);
                        System.out.println(message.getMessageID());
                        executor.submit(new MessageHandler(connection.getValue(),
                                connection.getKey(),
                                connectionMap,
                                message,
                                newFile,
                                log));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
  */  
    public void showPeers(TrackerResponse resp){
    	List<PeerInfo> peers = resp.getPeers();
		for(PeerInfo peer : peers){
			System.out.println(peer.getPeerId());
		}
    }
    
    public void showList(){
    	for (Map.Entry<PeerInfo, Connection> connection : connectionMap.entrySet()) {
    		System.out.println(connection.getKey().getPeerId());
    	}
    }
}
