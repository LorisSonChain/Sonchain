package sonchain.blockchain.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sonchain.blockchain.crypto.HashUtil;
import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.util.Numeric;


/**
 * Transaction
 * 
 * @author GAIA
 *
 */
public class Transaction extends TransactionHeader implements IJson{

	public static final Logger m_logger = Logger.getLogger(Transaction.class);
	public static final int HASH_LENGTH = 32;
	public static final int ADDRESS_LENGTH = 20;
    private static final int LOWER_REAL_V = 27;
    
	/**
	 * Constructor
	 * 
	 */
	public Transaction() {
	}

	/**
	 * Constructor
	 * 
	 * @param nonce
	 * @param receiveAddress
	 * @param value
	 * @param data
	 */
	public Transaction(byte[] nonce, byte[] receiveAddress, byte[] value, byte[] data) {
		this(nonce, receiveAddress, value, data, "");
	}

	/**
	 * Constructor
	 * 
	 * @param nonce
	 * @param receiveAddress
	 * @param value
	 * @param data
	 * @param privateNote
	 */
	public Transaction(byte[] nonce, byte[] receiveAddress, byte[] value, byte[] data, String privateNote) {
		m_nonce = nonce;
		m_receiveAddress = receiveAddress;
		if (ByteUtil.isSingleZero(value)) {
			setValue(ByteUtil.EMPTY_BYTE_ARRAY);
		} else {
			setValue(value);
		}
		if (receiveAddress == null) {
			m_receiveAddress = ByteUtil.EMPTY_BYTE_ARRAY;
		}
		m_privateNote = privateNote;
	}

	/**
	 * Constructor
	 * 
	 * @param nonce
	 * @param receiveAddress
	 * @param senderAddress
	 * @param value
	 * @param data
	 * @param privateNote
	 */
	public Transaction(byte[] nonce, byte[] receiveAddress, byte[] senderAddress, byte[] value, byte[] data, String privateNote) {
		m_nonce = nonce;
		m_receiveAddress = receiveAddress;
		m_senderAddress = senderAddress;
		if (ByteUtil.isSingleZero(value)) {
			setValue(ByteUtil.EMPTY_BYTE_ARRAY);
		} else {
			setValue(value);
		}
		if (receiveAddress == null) {
			m_receiveAddress = ByteUtil.EMPTY_BYTE_ARRAY;
		}
		if (m_senderAddress == null) {
			m_senderAddress = ByteUtil.EMPTY_BYTE_ARRAY;
		}
		m_privateNote = privateNote;
	}
	
	private byte[] m_nonce = ByteUtil.ZERO_BYTE_ARRAY;
	private String m_privateNote = "";
	private byte[] m_receiveAddress = ByteUtil.ZERO_BYTE_ARRAY;
	private byte[] m_senderAddress = ByteUtil.ZERO_BYTE_ARRAY;
	private List<Action> m_actions = new ArrayList<Action>();
	 
    public byte[] getContractAddress() {
        if (!isContractCreation()) 
       {
        	return null;
        }
        return HashUtil.calcNewAddr(getSenderAddress(), getNonce());
    }
    
    @Override
    public byte[] getHash()	 
    {
    	byte[] hash = super.getHash();
        if (!ArrayUtils.isEmpty(hash))
    	{
        	return hash;
    	}
        byte[] plainMsg = getEncodedRaw();
        hash = HashUtil.sha3(plainMsg);
        super.setHash(hash);
        m_logger.debug("getHash hash:" + Hex.toHexString(hash));
        return hash;
    }
    
    public byte[] getNonce() {
        return m_nonce == null ? ByteUtil.ZERO_BYTE_ARRAY : m_nonce;
    }

    protected void setNonce(byte[] nonce) {
        m_nonce = nonce;
    }
    
    public boolean isContractCreation() {
        return m_receiveAddress == null || Arrays.equals(m_receiveAddress, ByteUtil.EMPTY_BYTE_ARRAY);
    }
    
    public boolean isValueTx() {
        return super.getValue() != null;
    }
    
    public byte[] getReceiveAddress() {
        return m_receiveAddress;
    }
    
    protected void setReceiveAddress(byte[] receiveAddress) {
        m_receiveAddress = receiveAddress;
    }

	public byte[] getSenderAddress() {
		return m_senderAddress;
	}

	public void setSenderAddress(byte[] senderAddress) {
		m_senderAddress = senderAddress;
	}

	public List<Action> getActions() {
		return m_actions;
	}

	public void setActions(List<Action> actions) {
		m_actions = actions;
	}

    @Override
    public boolean validate() {
    	if(!super.validate())
    	{
    		return false;
    	}
        if (getNonce().length > HASH_LENGTH) 
        {
			m_logger.error("Nonce is not valid!");
			return false;
        }
        if (m_receiveAddress != null 
        		&& m_receiveAddress.length != 0 
        		&& m_receiveAddress.length != ADDRESS_LENGTH)
        {
			m_logger.error("Receive address is not valid!");
			return false;
        }
        return true;
    }
    
	public synchronized boolean verify() {
		m_logger.error("Transaction verify:  TransactionInfo:" + toString());
		return validate();
	}
    
    @Override
    public String toString() {
        return toString("\n");
    }
    
    public String toString(final String suffix) {

        StringBuilder toStringBuff = new StringBuilder();
        toStringBuff.append("  Transaction[version=").append(getVersion()).append(suffix);
        toStringBuff.append("  hash=").append(ByteUtil.toHexString(getHash())).append(suffix);
        toStringBuff.append("  nonce=").append(ByteUtil.toHexString(m_nonce)).append(suffix);
        toStringBuff.append("  receiveAddress=").append(ByteUtil.toHexString(m_receiveAddress)).append(suffix);
        toStringBuff.append("  sendAddress=").append(ByteUtil.toHexString(m_senderAddress)).append(suffix);
        toStringBuff.append("  value=").append(ByteUtil.toHexString(getValue())).append(suffix);
        toStringBuff.append("  refBlockheight=").append(getRefBlockHeight()).append(suffix);
        toStringBuff.append("  timestamp=").append(getTimeStamp()).append(suffix);
        toStringBuff.append("  privateNote=").append(m_privateNote).append(suffix);
        toStringBuff.append("  expiration=").append(getExpiration()).append(suffix);		
        if (!m_actions.isEmpty()) {
			toStringBuff.append("Actions [\n");
			for (Action action : m_actions) {
				toStringBuff.append(action.toString());
				toStringBuff.append("\n");
			}
			toStringBuff.append("]\n");
		} else {
			toStringBuff.append("Actions []\n");
		}
        toStringBuff.append("  ]");
        return toStringBuff.toString();
    }

    @Override
    public byte[] getEncoded()  {
    	try
    	{
			ObjectMapper mapper = new ObjectMapper();
		    ObjectNode transactionNode = mapper.createObjectNode();
		    toJson(transactionNode);
	    	String content = mapper.writeValueAsString (transactionNode);
	    	m_logger.debug(" Json Content:" + content);
		    return content.getBytes();
    	}catch(JsonProcessingException ex){
    		m_logger.error("getEncoded error:" + ex.getMessage());
    		return null;
    	}	    
    }

    @Override
    public byte[] getEncodedRaw()  {
    	try
    	{
			ObjectMapper mapper = new ObjectMapper();
		    ObjectNode transactionNode = mapper.createObjectNode();
			
	    	String nonce = Hex.toHexString(m_nonce);
	    	String receiveAddress = Hex.toHexString(m_receiveAddress);
	    	String senderAddress = Hex.toHexString(m_senderAddress);
	    	String value = Hex.toHexString(getValue());
	    	
	    	m_logger.debug("getEncoded refBlockHeight\t\t\t: " + getRefBlockHeight());
	    	m_logger.debug("getEncoded nonce\t\t\t: " + nonce);
	    	m_logger.debug("getEncoded privateNote\t\t\t: " + m_privateNote);
	    	m_logger.debug("getEncoded receiveAddress\t\t\t: " + receiveAddress);
	    	m_logger.debug("getEncoded senderAddress\t\t\t: " + senderAddress);
	    	m_logger.debug("getEncoded timeStamp\t\t\t: " + getTimeStamp());
	    	m_logger.debug("getEncoded version\t\t\t: " + getVersion());
	    	m_logger.debug("getEncoded value\t\t\t: " + value);    
	    	m_logger.debug("getEncoded expiration\t\t\t: " + getExpiration());    
	    	
	    	transactionNode.put("refBlockHeight", getRefBlockHeight());
	    	transactionNode.put("nonce", nonce);
	    	transactionNode.put("privateNote", m_privateNote);
	    	transactionNode.put("receiveAddress", receiveAddress);
	    	transactionNode.put("senderAddress", senderAddress);
	    	transactionNode.put("timeStamp", getTimeStamp());
	    	transactionNode.put("version", getVersion());
	    	transactionNode.put("value", value);
	    	transactionNode.put("expiration", getExpiration());
	    	String content = mapper.writeValueAsString (transactionNode);
	    	m_logger.debug(" Json Content:" + content);
		    return content.getBytes();
    	}catch(JsonProcessingException ex){
    		m_logger.error("getEncoded error:" + ex.getMessage());
    		return null;
    	}	    
    }
    
    @Override
    public int hashCode() {
        byte[] hash = getHash();
        int hashCode = 0;

        for (int i = 0; i < hash.length; ++i) {
            hashCode += hash[i] * i;
        }
        return hashCode;
    }
    
    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof Transaction)) {
        	return false;
        }
        Transaction tx = (Transaction) obj;
        return tx.hashCode() == this.hashCode();
    }

	@Override
    public void toJson(ObjectNode transactionNode){    	
		super.toJson(transactionNode);
    	String hash = Hex.toHexString(getHash());
    	String nonce = Hex.toHexString(m_nonce);
    	String receiveAddress = Hex.toHexString(m_receiveAddress);
    	String senderAddress = Hex.toHexString(m_senderAddress);
    	
    	m_logger.debug("toJson hash\t\t\t: " + hash);
    	m_logger.debug("toJson nonce\t\t\t: " + nonce);
    	m_logger.debug("toJson privateNote\t\t\t: " + m_privateNote);
    	m_logger.debug("toJson receiveAddress\t\t\t: " + receiveAddress);
    	m_logger.debug("toJson senderAddress\t\t\t: " + senderAddress);
    	
    	if(m_actions != null){
    		ArrayNode transNodes = transactionNode.arrayNode();
    		int size = m_actions.size();
    		for(int i = 0 ; i < size; i++){
    			ObjectNode transNode = transactionNode.objectNode();
    			m_actions.get(i).toJson(transNode);
    			transNodes.add(transNode);
    		}
    		transactionNode.set("actions", transNodes);
    	}
    	
    	transactionNode.put("hash", hash);
    	transactionNode.put("nonce", nonce);
    	transactionNode.put("privateNote", m_privateNote);
    	transactionNode.put("receiveAddress", receiveAddress);
    	transactionNode.put("senderAddress", senderAddress);
    }

	@Override
	public synchronized void jsonParse(JsonNode transactionNode) {
		try {
			super.jsonParse(transactionNode);		
			String hash = transactionNode.get("hash").asText();
			String nonce = transactionNode.get("nonce").asText();
			m_privateNote = transactionNode.get("privateNote").asText();
			String receiveAddress = transactionNode.get("receiveAddress").asText();
			String senderAddress = transactionNode.get("senderAddress").asText();
	       
	        setHash(Numeric.hexStringToByteArray(hash));
	        m_nonce = Numeric.hexStringToByteArray(nonce);
	        m_receiveAddress = Numeric.hexStringToByteArray(receiveAddress);
	        m_senderAddress = Numeric.hexStringToByteArray(senderAddress);
	        
			if(m_actions == null){
				m_actions = new CopyOnWriteArrayList<>();
			}
			JsonNode trans = transactionNode.get("actions");
			for (JsonNode tran : trans) {  
				Action action = new Action();
				action.jsonParse(tran);
				m_actions.add(action);
			}			
	        m_logger.debug("jsonParse hash\t\t\t: " + hash);
	        m_logger.debug("jsonParse nonce\t\t\t: " + nonce);
	        m_logger.debug("jsonParse privateNote\t\t\t: " + m_privateNote);
	        m_logger.debug("jsonParse receiveAddress\t\t\t: " + receiveAddress);
	        m_logger.debug("jsonParse senderAddress\t\t\t: " + senderAddress);
		}catch (Exception e) {
			 m_logger.error(e);
			 throw new RuntimeException("Error on parsing Json", e);
		}
	}
	
    @Override
	public String toJson(){
		try{
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode blockHeaderNode = mapper.createObjectNode();
			toJson(blockHeaderNode);
			String jsonStr =  mapper.writeValueAsString (blockHeaderNode);
			m_logger.debug(" Transaction Json String is :" + jsonStr);
			return jsonStr;
		}
		catch(Exception ex){
			m_logger.error(" Transaction toJson error:" + ex.getMessage());
			return "";
		}
	}

    @Override
	public synchronized void jsonParse(String json) {
		try{
			ObjectMapper mapper = new ObjectMapper();
			JsonNode blockHeaderNode = mapper.readTree(json); 
			jsonParse(blockHeaderNode);
		}
		catch(IOException ex){
			m_logger.error(" Transaction jsonParse error:" + ex.getMessage());
		}
	}

	public Transaction(byte[] m_nonce, String m_privateNote, byte[] m_receiveAddress, byte[] m_senderAddress,
			List<Action> m_actions) {
		super();
		this.m_nonce = m_nonce;
		this.m_privateNote = m_privateNote;
		this.m_receiveAddress = m_receiveAddress;
		this.m_senderAddress = m_senderAddress;
		this.m_actions = m_actions;
	}
    
	
    
}