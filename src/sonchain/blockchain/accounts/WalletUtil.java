package sonchain.blockchain.accounts;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.generators.SCrypt;
import org.bouncycastle.crypto.params.KeyParameter;

import sonchain.blockchain.accounts.keystore.Cipherparams;
import sonchain.blockchain.accounts.keystore.Crypto;
import sonchain.blockchain.accounts.keystore.KdfParams;
import sonchain.blockchain.accounts.keystore.ScryptKdfParams;
import sonchain.blockchain.accounts.keystore.WalletFile;
import sonchain.blockchain.crypto.CipherException;
import sonchain.blockchain.crypto.ECKeyPair;
import sonchain.blockchain.crypto.HashUtil;
import sonchain.blockchain.crypto.Keys;
import sonchain.blockchain.crypto.SecureRandomUtils;
import sonchain.blockchain.util.Numeric;

/**
 * @author GAIA
 *
 */
public class WalletUtil {
	private static final int N_LIGHT = 1 << 12;
    private static final int P_LIGHT = 6;
    
    private static final int N_STANDARD = 1 << 18;
    private static final int P_STANDARD = 1;

    private static final int R = 8;
    private static final int DKLEN = 32;

    private static final int CURRENT_VERSION = 3;

    private static final String CIPHER = "aes-128-ctr";
    static final String AES_128_CTR = "pbkdf2";
    static final String SCRYPT = "scrypt";
    
    /**
     * create
     * @param password
     * @param ecKeyPair
     * @param n
     * @param p
     * @return
     * @throws CipherException
     */
    public static WalletFile create(String password, ECKeyPair ecKeyPair, int n, int p)
            throws CipherException {

        byte[] salt = generateRandomBytes(32);
        byte[] derivedKey = generateDerivedScryptKey(password.getBytes(UTF_8), salt, n, R, p, DKLEN);
        byte[] encryptKey = Arrays.copyOfRange(derivedKey, 0, 16);
        byte[] iv = generateRandomBytes(16);
        byte[] privateKeyBytes =
                Numeric.toBytesPadded(ecKeyPair.GetPrivateKey(), Keys.PRIVATE_KEY_SIZE);
        String privateKey = Numeric.toHexStringNoPrefix(privateKeyBytes);
        System.out.println("privateKey:" + privateKey);
        byte[] cipherText = performCipherOperation(
                    Cipher.ENCRYPT_MODE, iv, encryptKey, privateKeyBytes);
        byte[] mac = generateMac(derivedKey, cipherText);
        return createWalletFile(ecKeyPair, cipherText, iv, salt, mac, n, p);
    }

    /**
     * createLight
     * @param password
     * @param ecKeyPair
     * @return
     * @throws CipherException
     */
    public static WalletFile createLight(String password, ECKeyPair ecKeyPair)
            throws CipherException {
        return create(password, ecKeyPair, N_LIGHT, P_LIGHT);
    }

    /**
     * createStandard
     * @param password
     * @param ecKeyPair
     * @return
     * @throws CipherException
     */
    public static WalletFile createStandard(String password, ECKeyPair ecKeyPair)
            throws CipherException {
        return create(password, ecKeyPair, N_STANDARD, P_STANDARD);
    }

    /**
     * createWalletFile
     * @param ecKeyPair
     * @param cipherText
     * @param iv
     * @param salt
     * @param mac
     * @param n
     * @param p
     * @return
     */
    private static WalletFile createWalletFile(
            ECKeyPair ecKeyPair, byte[] cipherText, byte[] iv, byte[] salt, byte[] mac, int n, int p) {

    	WalletFile walletFile = new WalletFile();
        walletFile.m_address = Keys.GetAddress(ecKeyPair);

        walletFile.m_crypto = new Crypto();
        walletFile.m_crypto.m_cipher = CIPHER;
        walletFile.m_crypto.m_ciphertext = Numeric.toHexStringNoPrefix(cipherText);

        walletFile.m_crypto.m_cipherparams = new Cipherparams();
        walletFile.m_crypto.m_cipherparams.m_iv = Numeric.toHexStringNoPrefix(iv);

        walletFile.m_crypto.m_kdf = SCRYPT;
        walletFile.m_crypto.m_kdfParams = new ScryptKdfParams();
        walletFile.m_crypto.m_kdfParams.m_dklen = DKLEN;
        walletFile.m_crypto.m_kdfParams.m_n = n;
        walletFile.m_crypto.m_kdfParams.m_p = p;
        walletFile.m_crypto.m_kdfParams.m_r = R;
        walletFile.m_crypto.m_kdfParams.m_salt = Numeric.toHexStringNoPrefix(salt);

        walletFile.m_crypto.m_mac = Numeric.toHexStringNoPrefix(mac);
        walletFile.m_id = UUID.randomUUID().toString();
        walletFile.m_version = CURRENT_VERSION;

        return walletFile;
    }

    /**
     * decrypt
     * @param password
     * @param walletFile
     * @return
     * @throws CipherException
     */
    public static ECKeyPair decrypt(String password, WalletFile walletFile)
            throws CipherException {
        validate(walletFile);

        Crypto crypto = walletFile.m_crypto;

        byte[] mac = Numeric.hexStringToByteArray(crypto.m_mac);
        byte[] iv = Numeric.hexStringToByteArray(crypto.m_cipherparams.m_iv);
        byte[] cipherText = Numeric.hexStringToByteArray(crypto.m_ciphertext);
        byte[] derivedKey;
        KdfParams kdfParams = crypto.m_kdfParams;
        if (kdfParams instanceof ScryptKdfParams) {
        	ScryptKdfParams scryptKdfParams = (ScryptKdfParams) crypto.m_kdfParams;
            int dklen = scryptKdfParams.m_dklen;
            int n = scryptKdfParams.m_n;
            int p = scryptKdfParams.m_p;
            int r = scryptKdfParams.m_r;
            byte[] salt = Numeric.hexStringToByteArray(scryptKdfParams.m_salt);
            derivedKey = generateDerivedScryptKey(password.getBytes(UTF_8), salt, n, r, p, dklen);
        } else {
            throw new CipherException("Unable to deserialize params: " + crypto.m_kdf);
        }

        byte[] derivedMac = generateMac(derivedKey, cipherText);

        if (!Arrays.equals(derivedMac, mac)) {
            throw new CipherException("Invalid password provided");
        }

        byte[] encryptKey = Arrays.copyOfRange(derivedKey, 0, 16);
        byte[] privateKey = performCipherOperation(Cipher.DECRYPT_MODE, iv, encryptKey, cipherText);
        return ECKeyPair.Create(privateKey);
    }

    /**
     * generateAes128CtrDerivedKey
     * @param password
     * @param salt
     * @param c
     * @param prf
     * @return
     * @throws CipherException
     */
    private static byte[] generateAes128CtrDerivedKey(
            byte[] password, byte[] salt, int c, String prf) throws CipherException {

        if (!prf.equals("hmac-sha256")) {
            throw new CipherException("Unsupported prf:" + prf);
        }

        // Java 8 supports this, but you have to convert the password to a character array, see
        // http://stackoverflow.com/a/27928435/3211687

        PKCS5S2ParametersGenerator gen = new PKCS5S2ParametersGenerator(new SHA256Digest());
        gen.init(password, salt, c);
        return ((KeyParameter) gen.generateDerivedParameters(256)).getKey();
    }

    /**
     * generateDerivedScryptKey
     * @param password
     * @param salt
     * @param n
     * @param r
     * @param p
     * @param dkLen
     * @return
     * @throws CipherException
     */
    private static byte[] generateDerivedScryptKey(
            byte[] password, byte[] salt, int n, int r, int p, int dkLen) throws CipherException {
        return SCrypt.generate(password, salt, n, r, p, dkLen);
    }

    /**
     * generateMac
     * @param derivedKey
     * @param cipherText
     * @return
     */
    private static byte[] generateMac(byte[] derivedKey, byte[] cipherText) {
        byte[] result = new byte[16 + cipherText.length];

        System.arraycopy(derivedKey, 16, result, 0, 16);
        System.arraycopy(cipherText, 0, result, 16, cipherText.length);

        return HashUtil.sha3(result);
    }

    /**
     * generateRandomBytes
     * @param size
     * @return
     */
    static byte[] generateRandomBytes(int size) {
        byte[] bytes = new byte[size];
        SecureRandomUtils.secureRandom().nextBytes(bytes);
        return bytes;
    }

    /**
     * performCipherOperation
     * @param mode
     * @param iv
     * @param encryptKey
     * @param text
     * @return
     * @throws CipherException
     */
    private static byte[] performCipherOperation(
            int mode, byte[] iv, byte[] encryptKey, byte[] text) throws CipherException {

        try {
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");

            SecretKeySpec secretKeySpec = new SecretKeySpec(encryptKey, "AES");
            cipher.init(mode, secretKeySpec, ivParameterSpec);
            return cipher.doFinal(text);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException
                | InvalidAlgorithmParameterException | InvalidKeyException
                | BadPaddingException | IllegalBlockSizeException e) {
            throw new CipherException("Error performing cipher operation", e);
        }
    }

    /**
     * validate
     * @param walletFile
     * @throws CipherException
     */
    public static void validate(WalletFile walletFile) throws CipherException {
        Crypto crypto = walletFile.m_crypto;

        if (walletFile.m_version != CURRENT_VERSION) {
            throw new CipherException("Wallet version is not supported");
        }

        if (!crypto.m_cipher.equals(CIPHER)) {
            throw new CipherException("Wallet cipher is not supported");
        }

        if (!crypto.m_kdf.equals(AES_128_CTR) && !crypto.m_kdf.equals(SCRYPT)) {
            throw new CipherException("KDF type is not supported");
        }
    }
}
