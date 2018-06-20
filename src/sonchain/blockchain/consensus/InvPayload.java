package sonchain.blockchain.consensus;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.base.Binary;
import sonchain.blockchain.core.Transaction;
import sonchain.blockchain.util.Numeric;

public class InvPayload {

	public static final Logger m_logger = Logger.getLogger(InvPayload.class);
	public InventoryType m_type = null;
	public List<Transaction> m_lstTransaction = new ArrayList<Transaction>();
	
	public int size()
	{
		int size = 1;
		int length = m_lstTransaction.size();
		size += 4;
		for(int i = 0; i < length; i++){
			String hexString = Hex.toHexString(m_lstTransaction.get(i).getEncoded());
			size += hexString.length();
		}
		return size;
	}

    public static InvPayload create(InventoryType type, List<Transaction> lstTransaction)
    {
    	InvPayload invPayload = new InvPayload();
    	invPayload.m_type = type;
    	invPayload.m_lstTransaction = lstTransaction;
    	return invPayload;
    }
	
	void deserialize(Binary reader){
		try{
			m_logger.debug("deserialize start.");
			byte byt = reader.ReadByte();
			m_type = InventoryType.fromByte(byt);
			int size = m_lstTransaction.size();
			for(int i = 0; i < size; i++){
				m_lstTransaction.add(new Transaction(Numeric.hexStringToByteArray(reader.ReadString())));
			}
			m_logger.debug("deserialize end.");
			
		}catch(Exception ex){
			m_logger.error("deserialize error :" + ex.getMessage());
		}
	}
	
	void serialize(Binary writer){
		try{
			m_logger.debug("serialize start.");
			writer.WriteByte(m_type.asByte());
			int size = m_lstTransaction.size();
			writer.WriteInt(size);
			for(int i = 0; i < size; i++){
				writer.WriteString(Hex.toHexString(m_lstTransaction.get(i).getEncoded()));
			}
			m_logger.debug("serialize end.");
			
		}catch(Exception ex){
			m_logger.error("serialize error :" + ex.getMessage());
		}
		
	}
}
