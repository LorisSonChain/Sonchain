package sonchain.blockchain.crypto.cryptohash;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Sha {

	/**
	 * encode
	 * 
	 * @param strEncode
	 * @param algorithmName
	 * @return
	 */
	public String encode(final String strEncode, final String algorithmName) {
		String strResult = "";
		if (strEncode != null && strEncode.length() > 0) {
			try {
				MessageDigest messageDigest = MessageDigest.getInstance(algorithmName);
				messageDigest.update(strEncode.getBytes());
				byte byteBuffer[] = messageDigest.digest();
				StringBuffer strHexString = new StringBuffer();
				for (int i = 0; i < byteBuffer.length; i++) {
					String hex = Integer.toHexString(0xff & byteBuffer[i]);
					if (hex.length() == 1) {
						strHexString.append('0');
					}
					strHexString.append(hex);
				}
				strResult = strHexString.toString();
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		}
		return strResult;
	}

	/**
	 * encode
	 * 
	 * @param bytesEncode
	 * @param algorithmName
	 * @return
	 */
	public byte[] encode(final byte[] bytesEncode, final String algorithmName) {
		if (bytesEncode != null && bytesEncode.length > 0) {
			try {
				MessageDigest messageDigest = MessageDigest.getInstance(algorithmName);
				messageDigest.update(bytesEncode);
				return  messageDigest.digest();
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
}
