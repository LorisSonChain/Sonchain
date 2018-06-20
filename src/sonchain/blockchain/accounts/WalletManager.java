package sonchain.blockchain.accounts;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import sonchain.blockchain.service.BlockWalletService;
import sonchain.blockchain.service.DataCenter;
import sonchain.blockchain.util.Numeric;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.log4j.Logger;
import org.bouncycastle.crypto.generators.SCrypt;

/**
 * WalletManager
 * @author GAIA
 *
 */
public class WalletManager {

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

    /**
     * Generates a BIP-39 compatible Ethereum wallet. The private key for the wallet can
     * be calculated using following algorithm:
     * <pre>
     *     Key = SHA-256(BIP_39_SEED(mnemonic, password))
     * </pre>
     *
     * @param password Will be used for both wallet encryption and passphrase for BIP-39 seed
     * @param destinationDirectory The directory containing the wallet
     * @return A BIP-39 compatible Ethereum wallet
     * @throws CipherException if the underlying cipher is not available
     * @throws IOException if the destination cannot be written to
     */
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

    /**
     * 加载BIP39证明
     * BIP32, BIP39, BIP44 共同定义了目前被广泛使用的 HD Wallet，包含其设计动机和理念、实作方式、实例等。
     * BIP39：将 seed 用方便记忆和书写的单字表示。一般由 12 个单字组成，称为 mnemonic code(phrase)，中文称为助记词或助记码
     * @param password
     * @param mnemonic
     * @return
     */
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
