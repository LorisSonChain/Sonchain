package sonchain.blockchain.crypto;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import sonchain.blockchain.util.Numeric;
import sonchain.blockchain.util.Strings;

/**
 * 键值处理
 * @author GAIA
 *
 */
public class Keys {
	public static final int PRIVATE_KEY_SIZE = 32;
	public static final int PUBLIC_KEY_SIZE = 64;

    public static final int ADDRESS_SIZE = 160;
    public static final int ADDRESS_LENGTH_IN_HEX = ADDRESS_SIZE >> 2;
    static final int PUBLIC_KEY_LENGTH_IN_HEX = PUBLIC_KEY_SIZE << 1;
    public static final int PRIVATE_KEY_LENGTH_IN_HEX = PRIVATE_KEY_SIZE << 1;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * 初始化
     */
    private Keys() { }

    /**
     * 用SECP-256k1椭圆算法创建键值对
     *
     * <p>公钥用PKCS8编码
     * <p>私钥用X.509编码 
     */
    static KeyPair CreateSecp256k1KeyPair() throws NoSuchProviderException,
            NoSuchAlgorithmException, InvalidAlgorithmParameterException {

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDSA", "BC");
        ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec("secp256k1");
        keyPairGenerator.initialize(ecGenParameterSpec, SecureRandomUtils.secureRandom());
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * 创建EC键值对
     * @return
     * @throws InvalidAlgorithmParameterException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     */
    public static ECKeyPair CreateEcKeyPair() throws InvalidAlgorithmParameterException,
            NoSuchAlgorithmException, NoSuchProviderException {
        KeyPair keyPair = CreateSecp256k1KeyPair();
        return ECKeyPair.Create(keyPair);
    }

    /**
     * 反序列化
     * @param input
     * @return
     */
    public static ECKeyPair Deserialize(byte[] input) {
        if (input.length != PRIVATE_KEY_SIZE + PUBLIC_KEY_SIZE) {
            throw new RuntimeException("Invalid input key size");
        }

        BigInteger privateKey = Numeric.toBigInt(input, 0, PRIVATE_KEY_SIZE);
        BigInteger publicKey = Numeric.toBigInt(input, PRIVATE_KEY_SIZE, PUBLIC_KEY_SIZE);

        return new ECKeyPair(privateKey, publicKey);
    }

    /**
     * 获取地址
     * @param ecKeyPair
     * @return
     */
    public static String GetAddress(ECKeyPair ecKeyPair) {
        return GetAddress(ecKeyPair.GetPublicKey());
    }

    /**
     * 获取地址
     * @param publicKey
     * @return
     */
    public static String GetAddress(BigInteger publicKey) {
        return GetAddress(
                Numeric.toHexStringWithPrefixZeroPadded(publicKey, PUBLIC_KEY_LENGTH_IN_HEX));
    }

    /**
     * 获取地址
     * @param publicKey
     * @return
     */
    public static String GetAddress(String publicKey) {
        String publicKeyNoPrefix = Numeric.cleanHexPrefix(publicKey);

        if (publicKeyNoPrefix.length() < PUBLIC_KEY_LENGTH_IN_HEX) {
            publicKeyNoPrefix = Strings.zeros(
                    PUBLIC_KEY_LENGTH_IN_HEX - publicKeyNoPrefix.length())
                    + publicKeyNoPrefix;
        }
        String hash = HashUtil.sha3(publicKeyNoPrefix);
        return hash.substring(hash.length() - ADDRESS_LENGTH_IN_HEX);  // right most 160 bits
    }

    /**
     * 获取地址
     * @param publicKey
     * @return
     */
    public static byte[] GetAddress(byte[] publicKey) {
        byte[] hash = HashUtil.sha3(publicKey);
        return Arrays.copyOfRange(hash, hash.length - 20, hash.length);  // right most 160 bits
    }

    /**
     * 校验和地址编码
     *
     * @param address a valid hex encoded address
     * @return hex encoded checksum address
     */
    public static String ToChecksumAddress(String address) {
        String lowercaseAddress = Numeric.cleanHexPrefix(address).toLowerCase();
        String addressHash = Numeric.cleanHexPrefix(HashUtil.sha3String(lowercaseAddress));
        StringBuilder result = new StringBuilder(lowercaseAddress.length() + 2);
        result.append("0x");

        for (int i = 0; i < lowercaseAddress.length(); i++) {
            if (Integer.parseInt(String.valueOf(addressHash.charAt(i)), 16) >= 8) {
                result.append(String.valueOf(lowercaseAddress.charAt(i)).toUpperCase());
            } else {
                result.append(lowercaseAddress.charAt(i));
            }
        }
        return result.toString();
    }

    /**
     * 序列化
     * @param ecKeyPair
     * @return
     */
    public static byte[] Serialize(ECKeyPair ecKeyPair) {
        byte[] privateKey = Numeric.toBytesPadded(ecKeyPair.GetPrivateKey(), PRIVATE_KEY_SIZE);
        byte[] publicKey = Numeric.toBytesPadded(ecKeyPair.GetPublicKey(), PUBLIC_KEY_SIZE);

        byte[] result = Arrays.copyOf(privateKey, PRIVATE_KEY_SIZE + PUBLIC_KEY_SIZE);
        System.arraycopy(publicKey, 0, result, PRIVATE_KEY_SIZE, PUBLIC_KEY_SIZE);
        return result;
    }
}
