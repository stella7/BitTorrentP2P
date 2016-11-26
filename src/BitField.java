import java.util.BitSet;

public class BitField {
	public BitSet bitfield;
	int numPieces;
	public BitField(int numPieces){
		bitfield = new BitSet(numPieces);
		this.numPieces = numPieces;
	}
	public BitField(int numPieces, BitSet bitfield){
		this.bitfield = bitfield;
		this.numPieces = numPieces;
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
	
	public BitSet getBitField(){
		return bitfield;
	}
	
	public int getNumPieces(){
		return numPieces;
	}
}
