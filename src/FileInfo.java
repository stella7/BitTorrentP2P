
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.nio.file.Paths;


public class FileInfo {
	public static final String MODE = "rws";    // Read and write synchronously for thread-safety
    private final long fileLength;
    private final int pieceLength;
    private final int numPieces;
    private final RandomAccessFile file;
    private final Path dataFolder;
    private final String filename;        // treat as unique identifier for file
    private final BitField bitfield;
    public FileInfo(boolean isSeeder, String fname, String directory, long fileLen, int pieceLen) throws Exception{
    	dataFolder = Paths.get(directory);
        filename = fname;
        fileLength = fileLen;
        pieceLength = pieceLen;
        numPieces = (int) Math.ceil(((float) fileLength) / ((float) pieceLength));    // round up
        bitfield = new BitField(numPieces);
        bitfield.initBitField(isSeeder);
        if (!isSeeder) {        	
            file = new RandomAccessFile(dataFolder.toString() + "/" + filename, "rws");
            System.out.println("set file length to " + fileLength);
            file.setLength(fileLength);

        } else {
            file = new RandomAccessFile(dataFolder.toString() + "/" + filename, "r");
        }
    }
    
    public void writePiece(byte[] data, int pieceIndex) {
        synchronized (file) {
            long pos = pieceIndex * pieceLength;
            try {
                file.seek(pos);
                file.write(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public byte[] readPiece(int pieceIndex) {
        synchronized (file) {
            long pos = pieceIndex * pieceLength;
            int length = Math.min(pieceLength, (int) (fileLength - pos));
            byte[] piece = new byte[length];
            try {
                file.seek(pos);
                file.read(piece, 0, length);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return piece;
        }
    }

    public void close() throws IOException {
        synchronized (file) {
            file.close();
        }
    }

    public String getFilename() {
        return filename;
    }
    
    public long getFileLength() {
        return fileLength;
    }

    public int getPieceLength() {
        return pieceLength;
    }

    public int getNumPieces() {
        return numPieces;
    }
    
    public BitField getBitField(){
    	return bitfield;
    }

}
