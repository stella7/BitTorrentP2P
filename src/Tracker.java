import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;

import java.util.concurrent.ConcurrentHashMap;

//import TrackerRequest.RequestEvent;
import org.apache.zookeeper.*;
import org.apache.zookeeper.Watcher.Event;
import org.apache.zookeeper.data.*;


//import TrackerRequest.RequestEvent;

import org.apache.curator.*;
import org.apache.curator.retry.*;
import org.apache.curator.framework.*;
import org.apache.curator.framework.api.*;

import org.apache.log4j.*;
public class Tracker implements Runnable{
	static Logger log;
	static final String ZK_CONNECT_STR = "snorkel.uwaterloo.ca:2181";
	static String zkNode = System.getProperty("user.name");
	private static CuratorFramework curClient;
	static ServerSocket trackserver;
	
	private static ConcurrentHashMap<String, List<String>> peerLists = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<String, List<PeerInfo>> fileLists = new ConcurrentHashMap<>();
	private static boolean run;
	
	public static void main(String[] args){
		BasicConfigurator.configure();
		if (args.length != 1) {
            System.out.println("java Tracker trackerPort");
            return;
        }
		log = new Logger("tracker");	
		int trackerPort = Integer.parseInt(args[0]);		
		Tracker tracker = new Tracker();		
		tracker.start();

		try {
			trackserver = new ServerSocket(trackerPort);
			trackserver.setSoTimeout(1000);
		}catch (SocketTimeoutException e) {
			
		}catch (Exception e) {
			// TODO Auto-generated catch block
			System.exit(-1);
		}
		tracker.run();
		
	}	
	
	void start() {
		curClient =
			    CuratorFrameworkFactory.builder()
			    .connectString(ZK_CONNECT_STR)
			    .retryPolicy(new RetryNTimes(10, 1000))
			    .connectionTimeoutMs(1000)
			    .sessionTimeoutMs(10000)
			    .build();
		
		curClient.start();
		run = true;
	}
	
	public void run() {		
		while(run){
			try {
				Socket socket = trackserver.accept();
				OutputStream out = socket.getOutputStream();
                InputStream in = socket.getInputStream();
                TrackerRequest request = TrackerRequest.decodeRequest(in);
                TrackerResponse response = processRequest(request);
                if(response != null){
                	response.sendResponse(out);
                }                	
			}catch (SocketTimeoutException e) {
                // used to retest run condition
            }catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		try {
			trackserver.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	private TrackerResponse processRequest(TrackerRequest request){
		TrackerRequest.RequestEvent event = request.getEvent();
		PeerInfo peerRequest = request.getPeer();
		String fileName = request.getFileName();
		List<PeerInfo> peers;
		 switch (event) {
		 	case STARTED:
		 		log.writeLog("accept STARTED Request from peer: " + peerRequest.getPeerId() + ", file: " + fileName);

		 		// starting a new session but file doesn't exist
		 		if (!fileLists.containsKey(fileName)) {
		 			return new TrackerResponse(0, null);
		 		}

		 		// else new session for existing file
		 		// note that this only says it was tracked at SOME point
		 		// may no longer be seeded
		 		if(!peerLists.contains(peerRequest.getPeerId())){
		 			List<String> files = new ArrayList<>();
		 			files.add(fileName);
		 			peerLists.put(peerRequest.getPeerId(), files);
		 			peerDeleted(peerRequest.getPeerId());
		 		}
		 		peers = fileLists.get(fileName);

		 		if (!peers.contains(peerRequest)) {
		 			peers.add(peerRequest);
		 			fileLists.put(fileName, peers);
		 		}
		 		showFiles();
		 		return new TrackerResponse(peers.size(), peers);
		 		
		 	case COMPLETED:
		 		if(!peerLists.contains(peerRequest.getPeerId())){
                	List<String> files = new ArrayList<>();
                	files.add(fileName);
                	peerLists.put(peerRequest.getPeerId(), files);
                	peerDeleted(peerRequest.getPeerId());
                }else{
                	List<String> files = peerLists.get(peerRequest);
                	files.add(fileName);
                	peerLists.put(peerRequest.getPeerId(), files);
                }
		 		
		 		if (!fileLists.containsKey(fileName)) {
                    log.writeLog("accept COMPLETED Request from peer: " + peerRequest.getPeerId() + ". Submitting file: " + fileName);
                    peers = new ArrayList<>();
                    peers.add(peerRequest);
                    fileLists.put(fileName, peers);                
                    showFiles();
                    return new TrackerResponse(1, peers);
                }
		 		return null;
		 	
		 	case STOPPED:
                log.writeLog("peer: " + peerRequest.getPeerId() + "STOPPED");
                // stupid request
                if (!peerLists.containsKey(fileName)) {
                    break;
                }

                // else remove from list
                peers = fileLists.get(fileName);
                if (peers.contains(peerRequest)) {
                    peers.remove(peerRequest);
                    fileLists.put(fileName, peers);                    
                }
                peerLists.remove(peerRequest.getPeerId());
                return null;
                
			default:
				return null;		 
		 }
		 
		 return new TrackerResponse(0, null);
	}
	
	public void showFiles(){
		for(Map.Entry<String, List<PeerInfo>> entry : fileLists.entrySet()){
			String file = entry.getKey();
			for(PeerInfo peer : entry.getValue()){
				System.out.println(file + ":" + peer);
			}
		}
	}
	
	synchronized private void peerDeleted(String peerId){    			
		//for(Map.Entry<String, List<String>> entry : peerLists.entrySet()){
		//	String peerId = entry.getKey();
			try {
				curClient.checkExists().usingWatcher(new peerWatcher(peerId)).forPath("/" + zkNode + "/" + peerId);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		//}				
    }
	
	private class peerWatcher implements CuratorWatcher {
		String peerDeleted;
		public peerWatcher(String pId){
			peerDeleted = pId;
		}
    	public void process(WatchedEvent event) {
    		if(event.getType() == Event.EventType.NodeDeleted) {
    			log.writeLog("peer: " + peerDeleted + " deleted");
    			System.out.println("Removing peer: " + peerDeleted);
    			//String path = event.getPath();
    			//System.out.println(path);
    			//System.out.println(peerDeleted);
    			List<String> files = peerLists.get(peerDeleted);
    			for(String file : files){
    			
					List<PeerInfo> peers = fileLists.get(file);
					//System.out.println(peers.size());
					for(Iterator<PeerInfo>  iterator = peers.iterator(); iterator.hasNext();){
						PeerInfo peer = iterator.next();
						if(peer.getPeerId().equals(peerDeleted)){
							iterator.remove();;
						}
					} 
					fileLists.put(file, peers);
					//System.out.println(peers.size());
    			}
    			peerLists.remove(peerDeleted);
    			//System.out.println(peerLists.size());
    		}
    	}
	}
/*	
	synchronized public void process(WatchedEvent event) {
		if(event.getType() == Event.EventType.NodeChildrenChanged) {
			try {
				peerAdded();
			} catch (Exception e) {
			    
			}	
		}
			
    }
	
	synchronized private void peerAdded() throws Exception {
		List<String> peersNew = 
				curClient.getChildren().usingWatcher(this).forPath(zkNode);
		List<String> peersOld = new ArrayList<String>(peerLists.keySet());
		if(peersNew.size() > peersOld.size()){
			
		}
	}

*/

}
