package sonchain.blockchain.consensus;

import org.apache.log4j.Logger;

import sonchain.blockchain.base.Binary;

public class ChangeView extends ConsensusMessage{

	public static final Logger m_logger = Logger.getLogger(ChangeView.class);
	public int m_newViewNumber = 0;
	public ChangeView()
	{
		super(ConsensusMessageType.ChangeView);
	}
	
	@Override
    public void deserialize(Binary reader)
    {
    	m_logger.debug("deserialize start." );
    	try
    	{
    		super.deserialize(reader);
    		m_newViewNumber = reader.ReadInt();
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
    		writer.WriteInt(m_newViewNumber);
        	m_logger.debug("serialize end.");
    	}catch(Exception ex){
    		m_logger.error("serialize error:" + ex.getMessage());
    	}
    }
}
