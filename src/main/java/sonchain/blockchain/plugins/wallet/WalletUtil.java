package sonchain.blockchain.plugins.wallet;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.Logger;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.generators.SCrypt;
import org.bouncycastle.crypto.params.KeyParameter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import sonchain.blockchain.accounts.keystore.Cipherparams;
import sonchain.blockchain.accounts.keystore.Crypto;
import sonchain.blockchain.accounts.keystore.KdfParams;
import sonchain.blockchain.accounts.keystore.ScryptKdfParams;
import sonchain.blockchain.accounts.keystore.WalletFile;
import sonchain.blockchain.crypto.Bip39Wallet;
import sonchain.blockchain.crypto.CipherException;
import sonchain.blockchain.crypto.Credentials;
import sonchain.blockchain.crypto.ECKeyPair;
import sonchain.blockchain.crypto.HashUtil;
import sonchain.blockchain.crypto.Keys;
import sonchain.blockchain.crypto.MnemonicUtils;
import sonchain.blockchain.crypto.SecureRandomUtils;
import sonchain.blockchain.data.CredentialData;
import sonchain.blockchain.service.DataCenter;
import sonchain.blockchain.util.Numeric;

/**
 *WalletUtil
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

    static final int PRIVATE_KEY_SIZE = 32;
    static final int PUBLIC_KEY_SIZE = 64;

    public static final int ADDRESS_SIZE = 160;
    public static final int ADDRESS_LENGTH_IN_HEX = ADDRESS_SIZE >> 2;
    static final int PUBLIC_KEY_LENGTH_IN_HEX = PUBLIC_KEY_SIZE << 1;
    public static final int PRIVATE_KEY_LENGTH_IN_HEX = PRIVATE_KEY_SIZE << 1;
    
	private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final SecureRandom secureRandom = SecureRandomUtils.secureRandom();
	public static final Logger m_logger = Logger.getLogger(WalletManager.class);

    static {
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    
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
    }  /**
     * generateFullNewWalletFile
     * @param password
     * @param destinationDirectory
     * @return
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws InvalidAlgorithmParameterException
     * @throws CipherException
     * @throws IOException
     */
    public static String generateFullNewWalletFile(String password, File destinationDirectory)
            throws NoSuchAlgorithmException, NoSuchProviderException,
            InvalidAlgorithmParameterException, CipherException, IOException {
        return generateNewWalletFile(password, destinationDirectory, true);
    }
	
	/**
	 * generateFullNewWalletFile
	 * @return
	 * @throws IOException 
	 * @throws CipherException 
	 * @throws NoSuchProviderException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidAlgorithmParameterException 
	 */
	public static WalletFile generateFullNewWalletFile(String password) 
			throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, 
			NoSuchProviderException, CipherException, IOException
	{
        return generateNewWalletFile(password, true);
	}
	
	/**
	 * generateFullNewWallet
	 * @return
	 * @throws IOException 
	 * @throws CipherException 
	 * @throws NoSuchProviderException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidAlgorithmParameterException 
	 */
	public static String generateFullNewWallet(String password) 
			throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, 
			NoSuchProviderException, CipherException, IOException
	{
		m_logger.debug("generateFullNewWallet start password:" + password);
        WalletFile walletFile = generateNewWalletFile(password, true);
		m_logger.debug("generateFullNewWallet end password:" + password);
        return objectMapper.writeValueAsString(walletFile);
	}
	

    /**
     * generateLightNewWalletFile
     * @param password
     * @param destinationDirectory
     * @return
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws InvalidAlgorithmParameterException
     * @throws CipherException
     * @throws IOException
     */
    public static String generateLightNewWalletFile(String password, File destinationDirectory)
            throws NoSuchAlgorithmException, NoSuchProviderException,
            InvalidAlgorithmParameterException, CipherException, IOException {

        return generateNewWalletFile(password, destinationDirectory, false);
    }


    
	public static WalletFile generateLightNewWalletFile(String password) 
			throws InvalidAlgorithmParameterException, NoSuchAlgorithmException,
			NoSuchProviderException, CipherException, IOException
	{
        return generateNewWalletFile(password, false);
	}

    public static String generateNewWalletFile(
            String password, File destinationDirectory, boolean useFullScrypt)
            throws CipherException, IOException, InvalidAlgorithmParameterException,
            NoSuchAlgorithmException, NoSuchProviderException {

        ECKeyPair ecKeyPair = Keys.CreateEcKeyPair();
        return generateWalletFile(password, ecKeyPair, destinationDirectory, useFullScrypt);
    }

    public static WalletFile generateNewWalletFile(
            String password, boolean useFullScrypt)
            throws CipherException, IOException, InvalidAlgorithmParameterException,
            NoSuchAlgorithmException, NoSuchProviderException {
        ECKeyPair ecKeyPair = Keys.CreateEcKeyPair();
        return generateWalletFile(password, ecKeyPair, useFullScrypt);
    }

    public static String generateWalletFile(
            String password, ECKeyPair ecKeyPair, File destinationDirectory, boolean useFullScrypt)
            throws CipherException, IOException {
        WalletFile walletFile;
        if (useFullScrypt) {
            walletFile = WalletUtil.createStandard(password, ecKeyPair);
        } else {
            walletFile = WalletUtil.createLight(password, ecKeyPair);
        }
        String fileName = getWalletFileName(walletFile);
        File destination = new File(destinationDirectory, fileName);
        objectMapper.writeValue(destination, walletFile);     
        return fileName;
    }

    public static WalletFile generateWalletFile(
            String password, ECKeyPair ecKeyPair, boolean useFullScrypt)
            throws CipherException, IOException {
        WalletFile walletFile;
        if (useFullScrypt) {
            walletFile = WalletUtil.createStandard(password, ecKeyPair);
        } else {
            walletFile = WalletUtil.createLight(password, ecKeyPair);
        }
        return walletFile;
    }

    public static Bip39Wallet generateBip39Wallet(String password, File destinationDirectory)
            throws CipherException, IOException {
        byte[] initialEntropy = new byte[16];
        secureRandom.nextBytes(initialEntropy);

        String mnemonic = MnemonicUtils.generateMnemonic(initialEntropy);
        byte[] seed = MnemonicUtils.generateSeed(mnemonic, password);
        ECKeyPair privateKey = ECKeyPair.Create(HashUtil.sha256(seed));
        String walletFile = generateWalletFile(password, privateKey, destinationDirectory, false);
        return new Bip39Wallet(walletFile, mnemonic);
    }

    public static Credentials getCredential(String password, String source)
            throws IOException, CipherException {
    	try
    	{
    		WalletFile walletFile = objectMapper.readValue(source, WalletFile.class);
    		//return LoadCredentials(walletFile.m_crypto.m_ciphertext);
    		Credentials credentials = Credentials.Create(WalletUtil.decrypt(password, walletFile));
    		return credentials;
    	}
    	catch(Exception ex)
    	{
    		m_logger.error(ex);
    		return null;
    	}
    }

    public static String getCredentialDataString(String password, String source)
            throws IOException, CipherException {
    	try
    	{
    		m_logger.debug("getCredentialDataString start source:" + source);
    		WalletFile walletFile = objectMapper.readValue(source, WalletFile.class);
    		//return LoadCredentials(walletFile.m_crypto.m_ciphertext);
    		Credentials credentials = Credentials.Create(WalletUtil.decrypt(password, walletFile));
    		CredentialData data = new CredentialData();
    		data.m_address = credentials.GetAddress();
            byte[] privateKeyBytes =
                    Numeric.toBytesPadded(credentials.GetEcKeyPair().GetPrivateKey(), Keys.PRIVATE_KEY_SIZE);
            data.m_privateKey =  Numeric.toHexStringNoPrefix(privateKeyBytes);
            data.m_balance = DataCenter.getSonChainImpl().getLastRepositorySnapshot().getBalance(
            		Numeric.hexStringToByteArray(data.m_address));
    		m_logger.debug("getCredentialDataString end source:" + source);
            return objectMapper.writeValueAsString(data);
    	}
    	catch(Exception ex)
    	{
    		m_logger.error(ex);
    		return "";
    	}
    }

    public static String getDefaultKeyDirectory() {
        return getDefaultKeyDirectory(System.getProperty("os.name"));
    }

    static String getDefaultKeyDirectory(String osName1) {
        String osName = osName1.toLowerCase();

        if (osName.startsWith("mac")) {
            return String.format(
                    "%s%sLibrary%sEthereum", System.getProperty("user.home"), File.separator,
                    File.separator);
        } else if (osName.startsWith("win")) {
            return String.format("%s%sEthereum", System.getenv("APPDATA"), File.separator);
        } else {
            return String.format("%s%s.ethereum", System.getProperty("user.home"), File.separator);
        }
    }  
    
    public static String getMainnetKeyDirectory() {
        return String.format("%s%skeystore", getDefaultKeyDirectory(), File.separator);
    }
    
    public static String getPrivateKey(String password, String privatejson)
            throws IOException, CipherException {
    	try
    	{
    		m_logger.debug("getPrivateKey start privatejson:" + privatejson);
    		WalletFile walletFile = objectMapper.readValue(privatejson, WalletFile.class);
    		//return LoadCredentials(walletFile.m_crypto.m_ciphertext);
            ECKeyPair keyPair = WalletUtil.decrypt(password, walletFile);
            byte[] privateKeyBytes =
                    Numeric.toBytesPadded(keyPair.GetPrivateKey(), Keys.PRIVATE_KEY_SIZE);
    		m_logger.debug("getPrivateKey end privatejson:" + privatejson);
            return Numeric.toHexStringNoPrefix(privateKeyBytes);
    	}
    	catch(Exception ex)
    	{
    		m_logger.error(ex);
    		return "";
    	}
    }
    
    public static String getTestnetKeyDirectory() {
        return String.format(
                "%s%stestnet%skeystore", getDefaultKeyDirectory(), File.separator, File.separator);
    }
    
    private static String getWalletFileName(WalletFile walletFile) {
        DateTimeFormatter format = DateTimeFormatter.ofPattern(
                "'UTC--'yyyy-MM-dd'T'HH-mm-ss.nVV'--'");
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        return now.format(format) + walletFile.m_address + ".json";
    }

    /**
     * 是否合法的私钥
     * @param privateKey
     * @return
     */
    public static boolean isValidPrivateKey(String privateKey) {
        String cleanPrivateKey = Numeric.cleanHexPrefix(privateKey);
        return cleanPrivateKey.length() == PRIVATE_KEY_LENGTH_IN_HEX;
    }
    
    public static boolean isValidAddress(String input) {
        String cleanInput = Numeric.cleanHexPrefix(input);
        try {
            Numeric.toBigIntNoPrefix(cleanInput);
        } catch (NumberFormatException e) {
    		m_logger.error(e);
            return false;
        }
        return cleanInput.length() == ADDRESS_LENGTH_IN_HEX;
    }
    
    public static Credentials loadBip39Credentials(String password, String mnemonic) {
        byte[] seed = MnemonicUtils.generateSeed(mnemonic, password);
        return Credentials.Create(ECKeyPair.Create(HashUtil.sha256(seed)));
    }
    
    public static Credentials loadCredentials(String ciphertext) {
    	try
    	{
    		if (ciphertext.startsWith("0x")) {
    			ciphertext = ciphertext.substring(2);
    		}
    		BigInteger privateKey = new BigInteger(ciphertext, 16);
    		ECKeyPair ecKeyPair = ECKeyPair.Create(privateKey);
    		Credentials credentials = Credentials.Create(ecKeyPair);
    		return credentials;
    	}
    	catch(Exception ex)
    	{
    		m_logger.error(ex);
    		return null;
    	}
    }
    
    public static String loadCredentialsString(String ciphertext) {
    	try
    	{
    		m_logger.debug("generateFullNewWallet start ciphertext:" + ciphertext);
    		if (ciphertext.startsWith("0x")) {
    			ciphertext = ciphertext.substring(2);
    		}
    		BigInteger privateKey = new BigInteger(ciphertext, 16);
    		ECKeyPair ecKeyPair = ECKeyPair.Create(privateKey);
    		Credentials credentials = Credentials.Create(ecKeyPair);
    		if(credentials == null)
    		{
    			return "1";
    		}
    		CredentialData retData = new CredentialData();
    		retData.m_address = credentials.GetAddress();
            byte[] privateKeyBytes =
                    Numeric.toBytesPadded(credentials.GetEcKeyPair().GetPrivateKey(), Keys.PRIVATE_KEY_SIZE);
            retData.m_privateKey =  Numeric.toHexStringNoPrefix(privateKeyBytes);
    		retData.m_balance = DataCenter.getSonChainImpl().getLastRepositorySnapshot().getBalance(
            		Numeric.hexStringToByteArray(retData.m_address));
    		m_logger.debug("generateFullNewWallet end ciphertext:" + ciphertext);
            return objectMapper.writeValueAsString(retData);
    	}
    	catch(Exception ex)
    	{
    		m_logger.error(ex);
    		return "";
    	}
    }
    
    public static String getBalanceString(String address) {
    	try
    	{
    		m_logger.debug("getBalanceString start address:" + address);
    		CredentialData retData = new CredentialData();
    		retData.m_address = address;
    		retData.m_balance = DataCenter.getSonChainImpl().getLastRepositorySnapshot().getBalance(
            		Numeric.hexStringToByteArray(address));
    		m_logger.debug("getBalanceString end address:" + address);
            return objectMapper.writeValueAsString(retData);
    	}
    	catch(Exception ex)
    	{
    		m_logger.error(ex);
    		return "";
    	}
    }
    
    public static Credentials loadCredentials(String password, String source)
            throws IOException, CipherException {
    	try
    	{
    		WalletFile walletFile = objectMapper.readValue(source, WalletFile.class);
    		//return LoadCredentials(walletFile.m_crypto.m_ciphertext);
            return Credentials.Create(WalletUtil.decrypt(password, walletFile));
    	}
    	catch(Exception ex)
    	{
    		m_logger.error(ex);
    		return null;
    	}
    }
    
    public static Credentials loadCredentials(String password, File source)
            throws IOException, CipherException {
        WalletFile walletFile = objectMapper.readValue(source, WalletFile.class);
        return Credentials.Create(WalletUtil.decrypt(password, walletFile));
    } 
    
}
