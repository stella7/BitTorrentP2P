import java.util.BitSet;

public class BitField {
	public BitSet bitfield;
	int numPieces;
	public BitField(int numPieces){
		bitfield = new BitSet(numPieces);
	}
	
	public void initBitField(boolean isSeeder){
		if(isSeeder){
			bitfield.set(0,numPieces,true);
		}else{
			bitfield.clear();
		}
	}
	
	public boolean isCompleted(){
		return bitfield.nextClearBit(0) > numPieces;		
	}
}
