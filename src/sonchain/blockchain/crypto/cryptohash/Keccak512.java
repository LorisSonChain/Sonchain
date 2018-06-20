package sonchain.blockchain.crypto.cryptohash;

public class Keccak512 extends KeccakCore {

	/**
	 * Create the engine.
	 */
	public Keccak512()
	{
		super("gtc-keccak-512");
	}

	/** @see Digest */
	public Digest Copy()
	{
		return CopyState(new Keccak512());
	}

	/** @see Digest */
	public int EngineGetDigestLength()
	{
		return 64;
	}

	@Override
	protected byte[] engineDigest() {
		return null;
	}

	@Override
	protected void engineUpdate(byte input) {
	}

	@Override
	protected void engineUpdate(byte[] input, int offset, int len) {
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
