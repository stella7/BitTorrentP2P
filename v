import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;


public class RespondTask implements Runnable{
	private final Uploader uploader;
    private final Downloader downloader;
    private ConcurrentMap<PeerInfo, Connection> connectionMap;
    private ConcurrentHashMap<PeerInfo, Float> unchokedPeers;
    private FileInfo datafile;
    private ScheduledExecutorService executor;
    private Logger logger;

    public RespondTask(ConcurrentMap<PeerInfo, Connection> connections,
                     ConcurrentHashMap<PeerInfo, Float> unchokedPeers,
                     FileInfo datafile,
                     ScheduledExecutorService executor,
                     Logger logger) {
        this.connectionMap = connections;
        this.unchokedPeers = unchokedPeers;
        this.datafile = datafile;
        this.executor = executor;
        this.logger = logger;
        this.downloader = new Downloader(logger);
        this.uploader = new Uploader(logger, unchokedPeers);
    }
	public void run() {
		
        while (true) {
        	System.out.println("Responder running");
            for (Map.Entry<PeerInfo, Connection> connection : connectionMap.entrySet()) {
                try {
                    InputStream input = connection.getValue().getSocket().getInputStream();
                    if (input.available() == 0) {
                    	//System.out.println("input empty");
                        continue;
                    }
                    
                    System.out.println("message from: " + connection.getKey().getPeerId());
                    DataMessage message = MessageProcessor.parseMessage(input);
                    System.out.println(message.getMessageID());
                    executor.submit(new MessageHandler(connection.getValue(),
                            connection.getKey(),
                            connectionMap,
                            downloader,
                            uploader,
                            message,
                            datafile));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}