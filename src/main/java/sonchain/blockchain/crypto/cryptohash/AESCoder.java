package sonchain.blockchain.crypto.cryptohash;

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.Logger;

public class AESCoder {
	public static final Logger m_logger = Logger.getLogger(AESCoder.class);
    private static final String defaultCharset = "UTF-8";
    public static final String KEY_ALGORITHM = "AES";    
    public static final String CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";
    /**
     * encrypt
     *
     * @param data 
     * @param key
     * @return
     */
    public static String encrypt(String data, String key) {
        return doAES(data, key, Cipher.ENCRYPT_MODE);
    }
    
    /**
     * encrypt
     *
     * @param bytesData 
     * @param bytesKey
     * @return
     */
    public static byte[] encrypt(byte[] bytesData, byte[] bytesKey) {
        return doAES(bytesData, bytesKey, Cipher.ENCRYPT_MODE);
    }
 
    /**
     * decrypt
     *
     * @param data
     * @param key
     * @return
     */
    public static String decrypt(String data, String key) {
        return doAES(data, key, Cipher.DECRYPT_MODE);
    }
 
    /**
     * decrypt
     *
     * @param bytesData
     * @param bytesKey
     * @return
     */
    public static byte[] decrypt(byte[] bytesData, byte[] bytesKey) {
        return doAES(bytesData, bytesKey, Cipher.DECRYPT_MODE);
    }
 
    /**
     * doAES
     *
     * @param bytesData 
     * @param bytesKey 
     * @param mode 
     * @return
     */
    private static byte[] doAES(byte[] bytesData, byte[] bytesKey, int mode) {
        try {
            KeyGenerator kgen = KeyGenerator.getInstance(KEY_ALGORITHM);
            kgen.init(128, new SecureRandom(bytesKey));
            SecretKey secretKey = kgen.generateKey();
            byte[] enCodeFormat = secretKey.getEncoded();
            SecretKeySpec keySpec = new SecretKeySpec(enCodeFormat, KEY_ALGORITHM);
            Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);
            cipher.init(mode, keySpec);
            return cipher.doFinal(bytesData);
        } catch (Exception e) {
        	m_logger.error("doAES error :" + e.getMessage());
        }
        return null;
    }
 
    /**
     * doAES
     *
     * @param data 
     * @param key 
     * @param mode 
     * @return
     */
    private static String doAES(String data, String key, int mode) {
        try {
            boolean encrypt = mode == Cipher.ENCRYPT_MODE;
            byte[] content = null;
            if (encrypt) {
                content = data.getBytes(defaultCharset);
            } else {
                content = parseHexStr2Byte(data);
            }
            KeyGenerator kgen = KeyGenerator.getInstance(KEY_ALGORITHM);
            kgen.init(128, new SecureRandom(key.getBytes()));
            SecretKey secretKey = kgen.generateKey();
            byte[] enCodeFormat = secretKey.getEncoded();
            SecretKeySpec keySpec = new SecretKeySpec(enCodeFormat, KEY_ALGORITHM);
            Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);
            cipher.init(mode, keySpec);
            byte[] result = cipher.doFinal(content);
            if (encrypt) {
                return parseByte2HexStr(result);
            } else {
                return new String(result, defaultCharset);
            }
        } catch (Exception e) {
        	m_logger.error("doAES error :" + e.getMessage());
        }
        return null;
    }
    
    /**
     * parseByte2HexStr
     *
     * @param buf
     * @return
     */
    public static String parseByte2HexStr(byte buf[]) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < buf.length; i++) {
            String hex = Integer.toHexString(buf[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex.toUpperCase());
        }
        return sb.toString();
    }
    
    /**
     * parseHexStr2Byte
     *
     * @param hexStr
     * @return
     */
    public static byte[] parseHexStr2Byte(String hexStr) {
        if (hexStr.length() < 1) {
            return null;
        }
        byte[] result = new byte[hexStr.length() / 2];
        for (int i = 0; i < hexStr.length() / 2; i++) {
            int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
            int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2), 16);
            result[i] = (byte) (high * 16 + low);
        }
        return result;
    }   
}
