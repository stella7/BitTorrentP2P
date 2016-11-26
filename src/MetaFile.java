
public class MetaFile {
	private String fileName;
    private int pieceLength;
    private long fileLength;
    private String trackerHost;
    private int trackerPort;
    
    public MetaFile(String fName, int pieceLen, long fileLen, String tHost, int tPort){
    	fileName = fName;
    	pieceLength = pieceLen;
    	fileLength = fileLen;
    	trackerHost = tHost;
    	trackerPort = tPort;
    }
    
    public byte[] createMetaFile(){
    	return String.format("%s,%s,%s;%s,%s",fileName,pieceLength,Long.toString(fileLength),trackerHost,trackerPort).getBytes();
    }

}
