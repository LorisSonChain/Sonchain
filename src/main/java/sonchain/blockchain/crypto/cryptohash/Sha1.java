package sonchain.blockchain.crypto.cryptohash;

public class Sha1 extends Sha{

	/**
	 * encode
	 * 
	 * @param strEncode
	 * @param algorithmName
	 * @return
	 */
	public String encode(final String strEncode, final String algorithmName) {
		return super.encode(strEncode, "SHA-1");
	}

	/**
	 * encode
	 * 
	 * @param bytesEncode
	 * @param algorithmName
	 * @return
	 */
	public byte[] encode(final byte[] bytesEncode, final String algorithmName) {
		return super.encode(bytesEncode, "SHA-1");
	}
}
