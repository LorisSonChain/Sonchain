package sonchain.blockchain.core;

import java.math.BigInteger;

import org.apache.log4j.Logger;

import sonchain.blockchain.base.Binary;
import sonchain.blockchain.service.DataCenter;

public class ValidatorsCountState extends StateBase{

	public static final Logger m_logger = Logger.getLogger(ValidatorsCountState.class);
	public BigInteger[] m_votes = null;
	
	public int size(){
		int size = super.size();
		int length = m_votes.length;
		for(int i = 0; i < length; i++){
			String hexString = m_votes[i].toString(10);
			size += hexString.length();
		}
		m_logger.debug("the size of ValidatorsCountState is:" + size);
		return size;
	}
	
	public ValidatorsCountState(){
		m_votes = new BigInteger[DataCenter.getSonChainImpl().MaxValidators];
	}
	
	public ValidatorsCountState(BigInteger[] votes){
		m_votes = votes;
	}
	
	public ValidatorsCountState clone(){
		m_logger.debug("Clone start.");
		return new ValidatorsCountState(m_votes);
	}
	
	public void deserialize(Binary reader){
		m_logger.debug("deserialize start.");
		try
		{
			super.deserialize(reader);
			int size = reader.ReadInt();
			m_logger.debug("the size of votes is:" + size);
			for(int i = 0; i < size; i++){
				String hexString = reader.ReadString();
				m_votes[i] = new BigInteger(hexString);
				m_logger.debug(String.format("the index {%d} of votes is:{%s}", i, m_votes[i].toString(10)));
			}
			m_logger.debug("deserialize end.");
		}catch(Exception ex){
			m_logger.error(" deserialize error:" + ex);
		}
	}
	
	public void serialize(Binary writer){
		m_logger.debug("serialize start.");
		try
		{
			super.serialize(writer);
			int size = m_votes.length;
			writer.WriteInt(size);
			m_logger.debug("the size of votes is:" + size);
			for(int i = 0; i < size; i++){
				String hexString = m_votes[i].toString(10);
				writer.WriteString(hexString);
				m_logger.debug(String.format("the index {%d} of votes is:{%s}", i, hexString));
			}
			m_logger.debug("serialize end.");
		}catch(Exception ex){
			m_logger.error(" serialize error:" + ex);
		}
	}
}
