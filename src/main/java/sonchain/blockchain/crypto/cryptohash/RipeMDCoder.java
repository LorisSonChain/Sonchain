package sonchain.blockchain.crypto.cryptohash;

import java.security.MessageDigest;
import java.security.Security;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;

public class RipeMDCoder {

	/**
	 * RipeMD128
	 * @param data 
	 * @return byte[] 
	 */
	public static byte[] encodeRipeMD128(byte[] data) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		MessageDigest md = MessageDigest.getInstance("RipeMD128");
		return md.digest(data);

	}

	/**
	 * RipeMD128Hex
	 * 
	 * @param data
	 * @return String 
	 **/
	public static String encodeRipeMD128Hex(byte[] data) throws Exception {
		byte[] b = encodeRipeMD128(data);
		return new String(Hex.encode(b));
	}

	/**
	 * RipeMD160
	 * 
	 * @param data
	 * @return byte[] 
	 */
	public static byte[] encodeRipeMD160(byte[] data) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		MessageDigest md = MessageDigest.getInstance("RipeMD160");
		return md.digest(data);

	}

	/**
	 * RipeMD160Hex
	 * 
	 * @param data
	 * @return String 
	 **/
	public static String encodeRipeMD160Hex(byte[] data) throws Exception {
		byte[] b = encodeRipeMD160(data);
		return new String(Hex.encode(b));
	}

	/**
	 * RipeMD256
	 * 
	 * @param data
	 * @return byte[] 
	 */
	public static byte[] encodeRipeMD256(byte[] data) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		MessageDigest md = MessageDigest.getInstance("RipeMD256");
		return md.digest(data);

	}

	/**
	 * RipeMD256Hex
	 * 
	 * @param data
	 * @return String 
	 **/
	public static String encodeRipeMD256Hex(byte[] data) throws Exception {
		byte[] b = encodeRipeMD256(data);
		return new String(Hex.encode(b));
	}

	/**
	 * RipeMD320
	 * 
	 * @param data
	 * @return byte[] 
	 */
	public static byte[] encodeRipeMD320(byte[] data) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		MessageDigest md = MessageDigest.getInstance("RipeMD320");
		return md.digest(data);

	}

	/**
	 * RipeMD320Hex
	 * 
	 * @param data
	 * @return String
	 **/
	public static String encodeRipeMD320Hex(byte[] data) throws Exception {
		byte[] b = encodeRipeMD320(data);
		return new String(Hex.encode(b));
	}

	/**
	 * init
	 * 
	 * @return byte[] 
	 */
	public static byte[] initHmacRipeMD128Key() throws Exception {

		Security.addProvider(new BouncyCastleProvider());
		KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacRipeMD128");
		SecretKey secretKey = keyGenerator.generateKey();
		return secretKey.getEncoded();
	}

	/**
	 * HmacRipeMD128
	 * 
	 * @param data
	 * @param key
	 * @return byte[] 
	 */
	public static byte[] encodeHmacRipeMD128(byte[] data, byte[] key) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		SecretKey secretKey = new SecretKeySpec(key, "HmacRipeMD128");
		Mac mac = Mac.getInstance(secretKey.getAlgorithm());
		mac.init(secretKey);
		return mac.doFinal(data);
	}

	/**
	 * HmacRipeMD128Hex
	 * 
	 * @param data
	 * @param String
	 * @return byte[] 
	 */
	public static String encodeHmacRipeMD128Hex(byte[] data, byte[] key) throws Exception {
		byte[] b = encodeHmacRipeMD128(data, key);
		return new String(Hex.encode(b));
	}

	/**
	 * init
	 * 
	 * @return byte[] 
	 */
	public static byte[] initHmacRipeMD160Key() throws Exception {

		Security.addProvider(new BouncyCastleProvider());
		KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacRipeMD160");
		SecretKey secretKey = keyGenerator.generateKey();
		return secretKey.getEncoded();
	}

	/**
	 * HmacRipeMD160
	 * 
	 * @param data
	 * @param key
	 * @return byte[] 
	 */
	public static byte[] encodeHmacRipeMD160(byte[] data, byte[] key) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		SecretKey secretKey = new SecretKeySpec(key, "HmacRipeMD160");
		Mac mac = Mac.getInstance(secretKey.getAlgorithm());
		mac.init(secretKey);
		return mac.doFinal(data);
	}

	/**
	 * HmacRipeMD160Hex
	 * 
	 * @param data
	 * @param String
	 * @return byte[] 
	 */
	public static String encodeHmacRipeMD160Hex(byte[] data, byte[] key) throws Exception {
		byte[] b = encodeHmacRipeMD160(data, key);
		return new String(Hex.encode(b));
	}
}
