package sonchain.blockchain.consensus;

import org.apache.log4j.Logger;

import sonchain.blockchain.base.Binary;

public class Witness{

	public static final Logger m_logger = Logger.getLogger(Witness.class);
	public byte[] m_invocationScript;
	public byte[] m_verificationScript;
	
	private byte[] m_scriptHash;
	public byte[] getScriptHash(){
		if(m_scriptHash == null)
		{
			m_scriptHash = m_verificationScript;
		}
		return m_scriptHash;
	}
	
	public int getSize() {
		int size = 4 + m_invocationScript.length + 4 + m_verificationScript.length;
		m_logger.debug("the size of Witness is:" + size);
		return size;
	}

	public void serialize(Binary writer) {
		try
		{
			m_logger.debug("serialize start");
			int size = m_invocationScript.length;
			writer.WriteInt(size);
			for(byte b : m_invocationScript){
				writer.WriteByte(b);
			}
			size = m_verificationScript.length;
			writer.WriteInt(size);
			for(byte b : m_verificationScript){
				writer.WriteByte(b);
			}
			m_logger.debug("serialize end");
		}
		catch(Exception ex){
			m_logger.debug("serialize error :" + ex.getMessage());
		}		
	}

	public void deserialize(Binary reader) {
		try
		{
			m_logger.debug("deserialize start");
			int size = reader.ReadInt();
			m_invocationScript = new byte[size];
			for(int i = 0; i < size; i++){
				m_invocationScript[i] = reader.ReadByte();
			}
			size = reader.ReadInt();
			m_verificationScript = new byte[size];
			for(int i = 0; i < size; i++){
				m_verificationScript[i] = reader.ReadByte();
			}
			m_logger.debug("deserialize end");
		}
		catch(Exception ex){
			m_logger.debug("deserialize error :" + ex.getMessage());
		}		
	}

}
