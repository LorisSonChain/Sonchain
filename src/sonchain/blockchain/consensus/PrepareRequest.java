package sonchain.blockchain.consensus;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.base.Binary;
import sonchain.blockchain.core.Block;
import sonchain.blockchain.core.Transaction;
import sonchain.blockchain.util.Numeric;

public class PrepareRequest extends ConsensusMessage{

	public static final Logger m_logger = Logger.getLogger(PrepareRequest.class);
	public BigInteger m_nonce = BigInteger.ZERO;
	public byte[] m_nextConsensus = null;
	public List<Transaction> m_lstTransaction = new ArrayList<Transaction>();
	public Block m_block = null;
	public byte[] m_signature = null;
	
	public PrepareRequest()
	{
		super(ConsensusMessageType.PrepareRequest);
	}

	@Override
    public void deserialize(Binary reader)
    {
    	m_logger.debug("deserialize start." );
    	try
    	{
    		super.deserialize(reader);
    		m_nonce = new BigInteger(reader.ReadString());
    		m_nextConsensus = Numeric.hexStringToByteArray(reader.ReadString());
    		int size = reader.ReadInt();
    		for(int i = 0; i < size; i++){
    			byte[] rlpEncoded  = Numeric.hexStringToByteArray(reader.ReadString());
    			Transaction trans = new Transaction(rlpEncoded);
    			m_lstTransaction.add(trans);
    		}
    		byte[] rlpEncodedBlock = reader.ReadString().getBytes();
    		m_block = new Block(rlpEncodedBlock);
    		m_signature = reader.ReadString().getBytes();
	    	m_logger.debug("deserialize end.");
    	}catch(Exception ex){
    		m_logger.error("deserialize error:" + ex.getMessage());
    	}
    }

	@Override
    public void serialize(Binary writer)
    {
    	m_logger.debug("serialize start." );
    	try
    	{
    		super.serialize(writer);
    		writer.WriteString(m_nonce.toString(10));
    		writer.WriteString(Hex.toHexString(m_nextConsensus));
    		int size = m_lstTransaction.size();
    		for(int i = 0; i < size; i++){
        		writer.WriteString(Hex.toHexString(m_lstTransaction.get(i).getEncoded()));
    		}
    		writer.WriteString(Hex.toHexString(m_block.getEncoded()));
    		writer.WriteString(Hex.toHexString(m_signature));
        	m_logger.debug("serialize end.");
    	}catch(Exception ex){
    		m_logger.error("serialize error:" + ex.getMessage());
    	}
    }
}
