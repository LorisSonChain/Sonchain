package sonchain.blockchain.consensus;

import java.math.BigInteger;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.base.Binary;

public class PrepareResponse extends ConsensusMessage{

	public static final Logger m_logger = Logger.getLogger(PrepareResponse.class);
	public byte[] m_signature = null;
	
	public PrepareResponse()
	{
		super(ConsensusMessageType.PrepareResponse);
	}

	@Override
    public void deserialize(Binary reader)
    {
    	m_logger.debug("deserialize start." );
    	try
    	{
    		super.deserialize(reader);
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
    		writer.WriteString(Hex.toHexString(m_signature));
        	m_logger.debug("serialize end.");
    	}catch(Exception ex){
    		m_logger.error("serialize error:" + ex.getMessage());
    	}
    }
}
