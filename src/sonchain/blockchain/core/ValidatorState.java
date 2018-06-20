package sonchain.blockchain.core;

import java.math.BigInteger;

import org.apache.log4j.Logger;

import sonchain.blockchain.base.Binary;

public class ValidatorState extends StateBase {

	public static final Logger m_logger = Logger.getLogger(ValidatorState.class);
	private String m_publicKey = "";
	private boolean m_registered = false;
	private BigInteger m_vote = BigInteger.ZERO;
	
	public int size(){
		int size = super.size();
		size += m_publicKey.length();
		size += 1;
		String hexString = m_vote.toString(10);
		size += hexString.length();
		m_logger.debug("the size of ValidatorState is:" + size);
		return size;
	}
	
	public ValidatorState(){
		
	}
	
	public ValidatorState(String publicKey){
		m_publicKey = publicKey;
		m_registered = false;
		m_vote = BigInteger.ZERO;
	}

	public ValidatorState clone(){
		m_logger.debug("clone start.");
		ValidatorState state =  new ValidatorState();
		state.m_vote = m_vote;
		state.m_publicKey = m_publicKey;
		state.m_registered = m_registered;
		m_logger.debug("clone end.");
		return state;
	}
	
	public void deserialize(Binary reader){
		m_logger.debug("deserialize start.");
		try
		{
			super.deserialize(reader);
			m_publicKey = reader.ReadString();
			m_registered = reader.ReadBool();
			String hexString = reader.ReadString();
			m_vote = new BigInteger(hexString);
			m_logger.debug(String.format("the m_publicKey :{%s} m_registered：{%d}, vote：{%s}",
					m_publicKey, m_registered, m_vote.toString(10)));
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
			writer.WriteString(m_publicKey);
			writer.WriteBool(m_registered);
			writer.WriteString(m_vote.toString(10));
			m_logger.debug("serialize end.");
		}catch(Exception ex){
			m_logger.error(" serialize error:" + ex);
		}
	}
}
