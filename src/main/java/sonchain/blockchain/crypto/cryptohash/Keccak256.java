package sonchain.blockchain.crypto.cryptohash;

public class Keccak256 extends KeccakCore {

	/**
	 * Create the engine.
	 */
	public Keccak256()
	{
		super("gtc-keccak-256");
	}

	/** @see org.ethereum.crypto.cryptohash.Digest */
	public Digest Copy()
	{
		return CopyState(new Keccak256());
	}

	/** @see org.ethereum.crypto.cryptohash.Digest */
	public int EngineGetDigestLength()
	{
		return 32;
	}

	@Override
	protected byte[] engineDigest() {
		return null;
	}

	@Override
	protected void engineUpdate(byte arg0) {
	}

	@Override
	protected void engineUpdate(byte[] arg0, int arg1, int arg2) {
	}

	@Override
	public int GetDigestLength() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected void engineReset() {
		// TODO Auto-generated method stub
		
	}
}
