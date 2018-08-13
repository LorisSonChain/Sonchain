package sonchain.blockchain.plugins.wallet;

import sonchain.blockchain.base.CFileA;
import sonchain.blockchain.core.SignedTransaction;
import sonchain.blockchain.crypto.ECKey;
import sonchain.blockchain.crypto.ECKeyPair;
import sonchain.blockchain.crypto.Keys;
import sonchain.blockchain.util.Numeric;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

/**
 * WalletManager
 *
 */
public class WalletManager {
	
	public static final Logger m_logger = Logger.getLogger(WalletManager.class);
	
	public final String FILE_EXT = ".wallet";
	public final String PASSWORD_PREFIX = "PW";
  
    private String m_dir = "";
    
    /**
     * Set the path for location of wallet files.
     * @param dir
     */
    public void setDir(String dir){
    	m_dir = dir;
    }
    
    private Date m_timeOutTime = Calendar.getInstance(Locale.CHINA).getTime();
    private int m_timeout = 0;
    
    /**
     * Set the timeout for locking all wallets.
     * If set then after t seconds of inactivity then lock_all().
     * Activity is defined as any wallet_manager method call below.
     * @param timeout
     */
    public void setTimeout(int timeoutSecs){
		m_logger.debug("setTimeout start. " + timeoutSecs);
    	m_timeout = timeoutSecs;
    	Calendar now = Calendar.getInstance(Locale.CHINA);
    	Calendar timeOut = now;
    	timeOut.add(Calendar.MILLISECOND, timeoutSecs);
    	m_timeOutTime = timeOut.getTime();    	
    	if(m_timeOutTime.compareTo(now.getTime()) <= 0){
    		m_logger.debug(String.format("Overflow on timeout_time, specified ${%d}, now ${%s}, timeout_time ${%s}",
    				timeoutSecs, now.toString(), m_timeOutTime.toString()));
    	}
		m_logger.debug("setTimeout end. " + timeoutSecs);
    }
    
    public void checkTimeout(){
    	Calendar now = Calendar.getInstance(Locale.CHINA);
    	if(now.getTime().compareTo(m_timeOutTime) >= 0){
    		lockAll();
    	}
    	now.add(Calendar.MILLISECOND, m_timeout);
    	m_timeOutTime = now.getTime();
    }
    
    private Map<String, WalletApi> m_wallets = new HashMap<String, WalletApi>();
    
    /**
     * generate the password
     * @return
     */
    public String genPassword(){
    	try{
    		m_logger.debug("genPassword start.");
    		ECKeyPair ecKeyPair = Keys.CreateEcKeyPair();
    		byte[] privateKeyBytes =
    				Numeric.toBytesPadded(ecKeyPair.GetPrivateKey(), Keys.PRIVATE_KEY_SIZE);
    		String privateKey = Numeric.toHexStringNoPrefix(privateKeyBytes);
    		return PASSWORD_PREFIX + privateKey;
    	}
    	catch(Exception ex){
    		m_logger.debug("genPassword is failed " + ex.getMessage());
    		return "";
    	}
    	finally
    	{
    		m_logger.debug("genPassword end.");
    	}
    }
    
    public boolean validFileName(String name){
    	try{
    		m_logger.debug("validFileName start.");
    		if(name.length() == 0){
        		m_logger.debug("validFileName the length of name is 0");
        		return false;
    		}
    		return true;
    	}
    	catch(Exception ex){
    		m_logger.debug("validFileName is failed " + ex.getMessage());
    		return false;
    	}
    	finally
    	{
    		m_logger.debug("validFileName end.");
    	}
    }
    
    /**
     * Sign transaction with the private keys specified via their public keys.
     * @param txn     the transaction to sign.
     * @param keys    the public keys of the corresponding private keys to sign the transaction with
     * @param chainId the chain_id to sign transaction with.
     * @return
     */
    public SignedTransaction signTransaction(SignedTransaction txn, Set<String> keys, String chainId){
		m_logger.debug("signTransaction start.");
    	SignedTransaction sTxn = new SignedTransaction();
    	//TODO
    	for(String key : keys)
    	{
	    	boolean found = false;
	    	for(Map.Entry<String, WalletApi> wallet : m_wallets.entrySet()){
	    		if(!wallet.getValue().isLocked()){
	    			ECKey ecKey = ECKey.fromPublicOnly(Numeric.hexStringToByteArray(key));
	    			//wallet.getValue().trySignDigest(digest, publicKey)
	    		}
	    	}
    	}
		m_logger.debug("signTransaction end.");
    	return sTxn;
    }
    
    /**
     * Sign digest with the private keys specified via their public keys.
     * @param digest      the digest to sign.
     * @param publicKey   the public key of the corresponding private key to sign the digest with
     * @return  signature over the digest
     */
    public byte[] signDigest(byte[] digest, String publicKey){
		m_logger.debug("signDigest start.");
    	checkTimeout();
    	try
    	{
    		byte[] signDigest = null;
    		for(Map.Entry<String, WalletApi> wallet : m_wallets.entrySet()){
    			if(!wallet.getValue().isLocked()){
    				signDigest = wallet.getValue().trySignDigest(digest, publicKey);
    				if(signDigest != null){
    					return signDigest;
    				}
    			}
    		}
    		m_logger.debug("signDigest Public key not found in unlocked wallets " + publicKey);
        	return null;
    	}
    	catch(Exception ex){
    		m_logger.error("signDigest error :" + ex.getMessage());
    	}
		m_logger.debug("signDigest end.");
    	return null;
    }
    
    /**
     * Create a new wallet.
     * A new wallet is created in file dir/{name}.wallet see set_dir.
     * The new wallet is unlocked after creation.
     * @param name name of the wallet and name of the file without ext .wallet.
     * @return  Plaintext password that is needed to unlock wallet. Caller is responsible for saving password otherwise
     *  they will not be able to unlock their wallet. Note user supplied passwords are not supported.
     */
    public String create(String name){
		m_logger.debug("create start.");
    	checkTimeout();
    	if(!validFileName(name)){
    		m_logger.debug("create Invalid filename, path not allowed in wallet name " + name);
    		return "";
    	}
    	String walletFileName = m_dir = "\\" + name + FILE_EXT;
    	if(CFileA.IsFileExist(walletFileName)){
    		m_logger.debug("create Wallet already exists filePath: " + walletFileName);
    		return "";
    	}
    	String password = genPassword();
    	Wallet wallet = new Wallet();
    	wallet.setPassword(password);
    	wallet.setWalletFileName(walletFileName);
    	wallet.unlock(password);
    	wallet.lock();
    	wallet.unlock(password);
    	// Explicitly save the wallet file here, to ensure it now exists.
    	wallet.saveWalletFile(walletFileName);
    	m_wallets.put(name, wallet);
		m_logger.debug("create end.");
    	return password;
    }
    
    /**
     * Open an existing wallet file dir/{name}.wallet.
     * @param name name of the wallet file (minus ext .wallet) to open.
     */
    public void open(String name){
		m_logger.debug("open start.");
    	checkTimeout();
    	if(!validFileName(name)){
    		m_logger.debug("open Invalid filename, path not allowed in wallet name " + name);
    		return;
    	}
    	String walletFileName = m_dir = "\\" + name + FILE_EXT;
    	if(!CFileA.IsFileExist(walletFileName)){
    		m_logger.debug("open Wallet is not exists not open filePath: " + walletFileName);
    		return;
    	}
    	Wallet wallet = new Wallet();
    	wallet.setWalletFileName(walletFileName);
    	if(!wallet.loadWalletFile(walletFileName)){
    		m_logger.debug("open Unable to open file: " + walletFileName);
    		return;
    	}
    	m_wallets.put(name, wallet);  
		m_logger.debug("open end.");  	
    }
    
    /**
     * return A list of wallet names with " *" appended if the wallet is unlocked.
     * @return
     */
    public List<String> listWallet(){
		m_logger.debug("listWallet start.");
    	checkTimeout();
    	List<String> result = new ArrayList<String>();
    	for(Map.Entry<String, WalletApi> wallet : m_wallets.entrySet()){
    		if(wallet.getValue().isLocked()){
    			result.add(wallet.getKey());
    		}else{
    			result.add(wallet.getKey() + " *");
    		}
    	}
		m_logger.debug("listWallet end.");
    	return result;
    }
    
    /**
     * return A list of private keys from a wallet provided password is correct to said wallet
     * @param name
     * @param pw
     * @return
     */
    public Map<String,String> listKeys(String name, String pw){
		m_logger.debug("listKeys start.");
    	checkTimeout();
    	if(m_wallets.size() == 0){
    		m_logger.debug("listKeys Wallet not found: " + name);
    		return null;
    	}
    	WalletApi wallet = m_wallets.get(name);
    	if(wallet.isLocked()){
    		m_logger.debug("listKeys Wallet is locked: " + name);
    	}
    	wallet.checkPassword(pw);
		m_logger.debug("listKeys end.");
    	return wallet.listKeys();
    }
    
    /**
     * return A set of public keys from all unlocked wallets, use with chain_controller::get_required_keys.
     * @return
     */
    public Set<String> getPublicKeys()
    {
		m_logger.debug("getPublicKeys start.");
    	checkTimeout();
    	if(m_wallets.size() == 0){
    		m_logger.debug("getPublicKeys You don't have any wallet!");
    		return null;
    	}
    	Set<String> result = new TreeSet<String>();
    	boolean is_all_wallet_locked = true;
    	for(Map.Entry<String, WalletApi> wallet : m_wallets.entrySet()){
    		if(!wallet.getValue().isLocked()){
    			result.addAll(wallet.getValue().listPubKeys());
    		}
    		is_all_wallet_locked = wallet.getValue().isLocked();
    	}
    	if(is_all_wallet_locked){
    		m_logger.debug("getPublicKeys You don't have any unlocked wallet!");
    		return null;
    	}
		m_logger.debug("getPublicKeys end.");
    	return result;
    }
    
    /**
     * Locks all the unlocked wallets.
     */
    public void lockAll(){
		m_logger.debug("lockAll start.");
    	for(Map.Entry<String, WalletApi> wallet : m_wallets.entrySet()){
    		if(!wallet.getValue().isLocked()){
    			wallet.getValue().lock();
    		}
    	}
		m_logger.debug("lockAll end.");
    }
    
    /**
     * Lock the specified wallet.
     * @param name the name of the wallet to lock.
     */
    public void lock(String name){
		m_logger.debug("lock start.");
    	checkTimeout();
    	if(m_wallets.size() == 0){
    		m_logger.debug("lock Wallet not found:" + name);
    		return;
    	}
    	if(m_wallets.containsKey(name)){
        	WalletApi wallet = m_wallets.get(name);
        	if(wallet.isLocked()){
        		return;
        	}
        	wallet.lock();
    	}
		m_logger.debug("lock end.");    	
    }
    
    /**
     * Unlock the specified wallet.
     * The wallet remains unlocked until ::lock is called or program exit.
     * @param name     the name of the wallet to lock.
     * @param password the plaintext password returned from ::create.
     */
    public void unlock(String name, String password){
		m_logger.debug("unlock start.");
    	checkTimeout();
    	if(m_wallets.size() == 0){
    		open(name);
    	}
    	if(m_wallets.containsKey(name)){
        	WalletApi wallet = m_wallets.get(name);
        	if(!wallet.isLocked()){
        		m_logger.debug("unlock Wallet is already unlocked: " + name);
        		return;
        	}
        	wallet.unlock(password);
    	}
		m_logger.debug("unlock end.");    	
    	
    }
    
    /**
     * Import private key into specified wallet.
	 * Imports a WIF Private Key into specified wallet.
	 * Wallet must be opened and unlocked.
     * @param name  the name of the wallet to import into.
     * @param key   the Private Key to import, 
     */
    public void importKey(String name, String key){
		m_logger.debug("importKey start.");
    	checkTimeout();
    	if(m_wallets.size() == 0){
    		m_logger.debug("importKey Wallet not found:" + name);
    		return;
    	}
    	if(m_wallets.containsKey(name)){
        	WalletApi wallet = m_wallets.get(name);
        	if(wallet.isLocked()){
        		m_logger.debug("importKey Wallet is Locked:" + name);
        		return;
        	}
        	wallet.importKey(key);
    	}
		m_logger.debug("importKey end.");    	
    	
    }
    
    /**
     * Removes a key from the specified wallet.  Wallet must be opened and unlocked.
     * @param name		the name of the wallet to remove the key from.
     * @param password  the plaintext password returned from ::create.
     * @param key		the Public Key to remove
     */
    public void removeKey(String name, String password, String key){
		m_logger.debug("removeKey start.");
    	checkTimeout();
    	if(m_wallets.size() == 0){
    		m_logger.debug("removeKey Wallet not found:" + name);
    		return;
    	}
    	if(m_wallets.containsKey(name)){
        	WalletApi wallet = m_wallets.get(name);
        	if(wallet.isLocked()){
        		m_logger.debug("removeKey Wallet is Locked:" + name);
        		return;
        	}
        	wallet.checkPassword(password);
        	wallet.removeKey(key);
    	}
		m_logger.debug("removeKey end.");    	
    	
    }
    
    /**
     * Creates a key within the specified wallet.
     * Wallet must be opened and unlocked
     * @param name of the wallet to create key in
     * @param keyType type of key to create
     * @return The public key of the created key
     */
    public String createKey(String name, String keyType){
		m_logger.debug("createKey start.");
    	checkTimeout();
    	if(m_wallets.size() == 0){
    		m_logger.debug("createKey Wallet not found:" + name);
    		return "";
    	}
    	if(m_wallets.containsKey(name)){
        	WalletApi wallet = m_wallets.get(name);
        	if(wallet.isLocked()){
        		m_logger.debug("createKey Wallet is Locked:" + name);
        		return "";
        	}
        	String upperKeyType = keyType.toUpperCase();
        	return wallet.createKey(upperKeyType);
    	}
		m_logger.debug("createKey end.");    	
    	return "";
    }
    
    /**
     * Takes ownership of a wallet to use
     * @param name
     * @param wallet
     */
    public void ownAndUseWallet(String name, WalletApi wallet){
		m_logger.debug("ownAndUseWallet start.");
		if(!m_wallets.containsKey(name)){
    		m_logger.debug("ownAndUseWallet tried to use wallet name the already existed:" + name);			
		}
		m_wallets.put(name, wallet);
		m_logger.debug("ownAndUseWallet end.");     	
    }
}
