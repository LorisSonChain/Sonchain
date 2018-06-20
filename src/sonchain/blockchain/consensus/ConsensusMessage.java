package sonchain.blockchain.consensus;

import org.apache.log4j.Logger;

import sonchain.blockchain.base.Binary;

public class ConsensusMessage implements ISerializable{

	public static final Logger m_logger = Logger.getLogger(ConsensusMessage.class);
	public ConsensusMessageType m_type;
	public int m_viewNumber = 0;

    public ConsensusMessageType getMType() {
		return m_type;
	}

	public void setMType(ConsensusMessageType type) {
		m_type = type;
	}

	public int getMViewNumber() {
		return m_viewNumber;
	}

	public void setMViewNumber(int viewNumber) {
		m_viewNumber = viewNumber;
	}    

	@Override
    public int getSize(){
    	return 1 + 4;
    }
    
    public ConsensusMessage(){
    }

    public ConsensusMessage(ConsensusMessageType type){
    	m_type = type;
    }

    public void deserialize(Binary reader)
    {
    	m_logger.debug("deserialize start." );
    	try
    	{
    		m_type = ConsensusMessageType.fromByte(reader.ReadByte());
	        if (m_type == null){
	        	throw new ClassCastException();
	        }
	        m_viewNumber = reader.ReadInt();
	    	m_logger.debug("deserialize end. Type:" + m_type + " viewNumber:" +  m_viewNumber);
    	}
    	catch(Exception ex){
    		m_logger.error("deserialize error:" + ex.getMessage());
    	}
    }

    public static ConsensusMessage deserializeFrom(byte[] data)
    {
    	m_logger.debug("deserializeFrom start." );
    	try
    	{
	    	Binary reader = new Binary();
	    	reader.ReadBytes(data);
	    	ConsensusMessage message = new ConsensusMessage();
	    	message.deserialize(reader);
	    	m_logger.debug("deserializeFrom end. Type:"
	    			+ message.getMType() + " viewNumber:" +  message.getMViewNumber());
	        return message;
    	}
    	catch(Exception ex){
    		m_logger.error("deserializeFrom error:" + ex.getMessage());
    		return null;
    	}
    }

    public void serialize(Binary writer)
    {
    	m_logger.debug("serialize start." );
    	try
    	{
    		writer.WriteByte(m_type.asByte());
    		writer.WriteInt(m_viewNumber);
        	m_logger.debug("serialize end. Type:" + m_type + " viewNumber:" +  m_viewNumber);
    	}
    	catch(Exception ex){
    		m_logger.error("serialize error:" + ex.getMessage());
    	}
    }
}
