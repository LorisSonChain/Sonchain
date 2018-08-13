package sonchain.blockchain.plugins.wallet;

import java.io.File;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import owchart.owlib.Base.RefObject;
import sonchain.blockchain.base.CFileA;
import sonchain.blockchain.crypto.ECKeyPair;
import sonchain.blockchain.crypto.Keys;
import sonchain.blockchain.crypto.cryptohash.AESCoder;
import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.util.FastByteComparisons;
import sonchain.blockchain.util.Numeric;

public class Wallet implements  WalletApi{	

	public static final Logger m_logger = Logger.getLogger(Wallet.class);
	public final String WALLET_FILENAME_EXTENSION = ".wallet";

	private byte[] m_cipher_keys = null;
	private String m_walletFileName = "";
	private Map<String, String> m_keys = new HashMap<String, String>();
	private byte[] m_checkSum = ByteUtil.ZERO_BYTE_ARRAY;
	
	public Wallet(){		
	}
	
	public Wallet(byte[] cipher_keys){
		m_cipher_keys = cipher_keys;
	}
	
	public  boolean copyWalletFile(String descFileName){
		m_logger.debug("copyWalletFile start descFileName:" + descFileName);
		String srcPath = getWalletFileName();
		if(!CFileA.IsDirectoryExist(srcPath)){
			return false;
		}
		String descPath = descFileName + WALLET_FILENAME_EXTENSION;
		m_logger.debug("copyWalletFile descPath:" + descPath);
		int suffix = 0;
		while(CFileA.IsDirectoryExist(descPath)){
			++suffix;
			descPath = descFileName + "-" + String.valueOf(suffix) + WALLET_FILENAME_EXTENSION;
			m_logger.debug("copyWalletFile descPath:" + descPath);
		}
		m_logger.debug(String.format("copyWalletFile backing up wallet {%s} to {%s}", srcPath, descPath));
		File file = new File(descPath);
		String parentPath = file.getParent();
		try{
			if(!CFileA.IsDirectoryExist(parentPath)){
				CFileA.CreateDirectory(parentPath);
			}
			CFileA.CopyFile(srcPath, descPath);
			m_logger.debug("copyWalletFile end descFileName:" + descFileName);
			
		}catch(Exception ex){
			m_logger.error("copyWalletFile error :" + ex.getMessage());
			return false;
		}
		return true;
	}
	
	/**
	 * Returns the current wallet filename.
	 * @return
	 */
	public String getWalletFileName(){
		return m_walletFileName;
	}
	
	@Override
	public String getPrivateKey(String pubKey) {
		m_logger.debug("getPrivateKey start pubKey:" + pubKey);
		if(m_keys.containsKey(pubKey)){
			String privateKey = m_keys.get(pubKey);
			m_logger.debug("getPrivateKey privateKey:" + privateKey);
			return privateKey;
		}
		else
		{
			m_logger.warn("Key doesn't exist!");
			return null;
		}
	}
	
	/**
	 *  @param role - active | owner | posting | memo
	 * @param account
	 * @param role
	 * @param password
	 * @return
	 */
	public Pair<String, String> getPrivateKeyFromPassword(String account, String role, String password){
		//TODO
		m_logger.debug(String.format("getPrivateKeyFromPassword start account:{%s} role:{%s} password:{%s}", account,  role, password));

		String seed = account + role + password;
		m_logger.debug(String.format("getPrivateKeyFromPassword seed:{%s}", seed));
		if(seed.length() == 0){
			m_logger.debug("The seed is empty");
			return null;
		};
		return null;		
	}

	/**
	 * Checks whether the wallet has just been created and has not yet had a password set.
	 * @return
	 */
	public boolean isNew(){
		m_logger.debug("isNew start");
		if(m_cipher_keys == null || m_cipher_keys.length == 0){
			m_logger.debug("isNew New");
			return true;
		}
		else
		{
			m_logger.debug("isNew NoNew");
			return false;
		}
	}

	@Override
	public boolean isLocked() {
		m_logger.debug("isLocked start");
		if(m_checkSum == null || FastByteComparisons.equal(m_checkSum, ByteUtil.EMPTY_BYTE_ARRAY)){
			m_logger.debug("isLocked locked");
			return true;
		}
		m_logger.debug("isLocked no locked");
		return false;
	}

	@Override
	public void lock() {
		m_logger.debug("lock start");
		try{
			if(isLocked()){
				m_logger.warn("Unable to lock a locked wallet");
				return;
			}
			encryptKeys();
			m_keys.clear();
			m_checkSum = ByteUtil.EMPTY_BYTE_ARRAY;
		}catch(Exception ex){
			
		}
		m_logger.debug("lock end");		
	}

	@Override
	public void unlock(String password) {
		m_logger.debug("unlock start password:" + password);
		if(password.length() == 0){
			m_logger.debug("unlock the length of password is 0");
			return;
		}
		byte[] hashPassword = Numeric.hexStringToByteArray(password);
		byte[] decrypted = AESCoder.decrypt(m_cipher_keys, hashPassword);
		String jsonStr = Hex.toHexString(decrypted);
		m_logger.debug("unlock cipher jsonStr:" + jsonStr);
		PlainKeys data = new PlainKeys();
		data.jsonParse(jsonStr);
		if(!FastByteComparisons.equal(hashPassword, data.getChecksum())){
			m_logger.debug("unlock failed password notequal");
		}
		m_keys = data.getKeys();
		m_checkSum = data.getChecksum();
		m_logger.debug("unlock end");		
	}

	@Override
	public void checkPassword(String password) {
		m_logger.debug("checkPassword start password:" + password);
		if(password.length() == 0){
			m_logger.debug("the length of password is 0");
			return;
		}
		byte[] hashPassword = Numeric.hexStringToByteArray(password);
		byte[] decrypted = AESCoder.decrypt(m_cipher_keys, hashPassword);
		String jsonStr = Hex.toHexString(decrypted);
		m_logger.debug("checkPassword cipher jsonStr:" + jsonStr);
		PlainKeys data = new PlainKeys();
		data.jsonParse(jsonStr);
		if(!FastByteComparisons.equal(hashPassword, data.getChecksum())){
			m_logger.debug("checkPassword failed Invalid password for wallet walletFileName:" + getWalletFileName());
		}
		m_logger.debug("checkPassword end");		
	}

	@Override
	public void setPassword(String password) {
		m_logger.debug("setPassword start password:" + password);
		if(!isNew()){
			if(isLocked()){
				m_logger.debug("The wallet must be unlocked before the password can be set");
				return;
			}
		}
		m_checkSum = Numeric.hexStringToByteArray(password);
		m_logger.debug("setPassword end password:" + password);		
	}

	@Override
	public Map<String, String> listKeys() {
		m_logger.debug("listKeys start");
		if(isLocked()){
			m_logger.debug("Unable to list public keys of a locked wallet");
			return null;
		}
		return m_keys;
	}

	@Override
	public Set<String> listPubKeys() {
		m_logger.debug("listPubKeys start");
		if(isLocked()){
			m_logger.debug("Unable to list public keys of a locked wallet");
			return null;
		}
		Set<String> publicKeys = new TreeSet<String>();
		for(Map.Entry<String, String> entry : m_keys.entrySet()){
			publicKeys.add(entry.getKey());
		}
		return publicKeys;
	}
	
	/**
	 * Loads a specified Graphene wallet
	 * The current wallet is closed before the new wallet is loaded.
	 * @param walletFileName wallet_filename the filename of the wallet JSON file to load.
       *  If \c wallet_filename is empty, it reloads the existing wallet file
	 * @return if the specified wallet is loaded
	 */
	public boolean loadWalletFile(String walletFileName){
		m_logger.debug("loadWalletFile start walletFileName:" + walletFileName);
		if(walletFileName.length() == 0){
			walletFileName = m_walletFileName;
		}
		if(!CFileA.IsFileExist(walletFileName)){
			m_logger.debug("loadWalletFile fialed file is not existed walletFileName:" + walletFileName);
			return false;
		}
		String fileContent = "";
		RefObject<String> refContent = new RefObject<>(fileContent);
		CFileA.Read(walletFileName, refContent);
		fileContent = refContent.argvalue;
		m_logger.debug("loadWalletFile fileContent:" + fileContent);
		m_cipher_keys = Numeric.hexStringToByteArray(fileContent);
		m_logger.debug("loadWalletFile start walletFileName:" + walletFileName);
		return true;
	}
	
	/**
	 * Saves the current wallet to the given filename.
	 * @param walletFileName the filename of the new wallet JSON file to create
     *  or overwrite.  If \c wallet_filename is empty, save to the current filename.
	 */
	public void saveWalletFile(String walletFileName){
		m_logger.debug("saveWalletFile start walletFileName:" + walletFileName);
		//
	    // Serialize in memory, then save to disk
	    //
	    // This approach lessens the risk of a partially written wallet
	    // if exceptions are thrown in serialization
	    //
		encryptKeys();
		if(walletFileName.length() == 0){
			walletFileName = m_walletFileName;
		}
		m_logger.debug("saveWalletFile walletFileName:" + walletFileName);
		String strCipherKeys = Hex.toHexString(m_cipher_keys);    	
		m_logger.debug("saveWalletFile CipherKeys:" + strCipherKeys);
		try
		{
			CFileA.Write(walletFileName, strCipherKeys);
		}catch(Exception ex){
			m_logger.debug("saveWalletFile error:" + ex.getMessage());
		}
		m_logger.debug("saveWalletFile end walletFileName:" + walletFileName);
	}
	
	/**
	 * Sets the wallet filename used for future writes.
	 * This does not trigger a save, it only changes the default filename
     * that will be used the next time a save is triggered.
	 * @param walletFileName the new filename to use for future saves
	 */
	public void setWalletFileName(String walletFileName){
		m_walletFileName = walletFileName;
	}

	@Override
	public boolean importKey(String wifKey) {
		m_logger.debug("importKey start wifKey:" + wifKey);
		if(isLocked()){
			m_logger.debug("Unable to import key on a locked wallet");
			return false;
		}
		byte[] privateKeyBytes = Numeric.hexStringToByteArray(wifKey);
		ECKeyPair ecKeyPair = ECKeyPair.Create(privateKeyBytes);
		String publicKey = Numeric.toHexStringWithPrefixZeroPadded(ecKeyPair.GetPublicKey(), Keys.PUBLIC_KEY_LENGTH_IN_HEX);
		m_logger.debug("importKey privateKey:" + publicKey);
		if(!m_keys.containsKey(publicKey)){
			m_keys.put(publicKey, wifKey);
			m_logger.debug("importKey privateKey successs.");
			return true;
		}
		else
		{
			m_logger.debug("importKey privateKey failed Key already in wallet.");			
		}
		m_logger.debug("importKey end wifKey:" + wifKey);
		return false;
	}

	@Override
	public boolean removeKey(String key) {
		m_logger.debug("importKey start key:" + key);
		if(isLocked()){
			m_logger.debug("Unable to remove key on a locked wallet");
			return false;
		}
		if(m_keys.containsKey(key)){
			m_keys.remove(key);
			saveWalletFile("");
			return true;
		}
		m_logger.debug("importKey not in wallet key:" + key);
		m_logger.debug("importKey end key:" + key);
		return false;
	}

	@Override
	public String createKey(String keyType) {
		m_logger.debug("createKey start keyType:" + keyType);
		if(isLocked()){
			m_logger.debug("Unable to create key on a locked wallet");
			return "";
		}
		try{
			ECKeyPair ecKeyPair = Keys.CreateEcKeyPair();
	        byte[] privateKeyBytes =
	                Numeric.toBytesPadded(ecKeyPair.GetPrivateKey(), Keys.PRIVATE_KEY_SIZE);
	        String privateKey = Numeric.toHexStringWithPrefix(privateKeyBytes);
			String publicKey = Numeric.toHexStringWithPrefixZeroPadded(ecKeyPair.GetPublicKey(), Keys.PUBLIC_KEY_LENGTH_IN_HEX);
			m_logger.debug("createKey privateKey:" + privateKey);
			m_logger.debug("createKey publicKey:" + publicKey);
			importKey(privateKey);
			return publicKey;
		}
		catch(NoSuchProviderException ex)
		{
			m_logger.error("CreateEcKeyPair error message:" + ex.getMessage());
			return "";
		}
		catch(NoSuchAlgorithmException ex)
		{
			m_logger.error("CreateEcKeyPair error message:" + ex.getMessage());	
			return "";	
		}
		catch(InvalidAlgorithmParameterException ex)
		{
			m_logger.error("CreateEcKeyPair error message:" + ex.getMessage());	
			return "";	
		}
		catch(Exception ex)
		{
			m_logger.error("CreateEcKeyPair error message:" + ex.getMessage());	
			return "";	
		}
		finally
		{
			m_logger.debug("createKey end keyType:" + keyType);
		}
	}	
	
	private void encryptKeys(){
		m_logger.debug("encryptKeys start");
		if(!isLocked()){
			PlainKeys data = new PlainKeys();
			data.setKeys(m_keys);
			data.setChecksum(m_checkSum);
			String plainTxt = data.toJson();
			m_cipher_keys = AESCoder.encrypt(plainTxt.getBytes(), data.getChecksum());
		}
		m_logger.debug("encryptKeys end");
	}

    /** Returns a signature given the digest and public_key, if this wallet can sign via that public key
     */
    public byte[] trySignDigest(byte[] digest, String publicKey)
    {
    	byte[] signDigest = null;
		m_logger.debug("trySignDigest start");
    	String privateKey = m_keys.get(publicKey);
    	if(privateKey == null || publicKey.length() == 0){
    		m_logger.debug("public key not exists publickey is :" + publicKey);
    		return signDigest;
    	}
    	
    	//TODO
		m_logger.debug("trySignDigest end");
		return signDigest;
    }
}
