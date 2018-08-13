package sonchain.blockchain.consensus;

import java.util.List;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.base.Binary;
import sonchain.blockchain.core.Block;
import sonchain.blockchain.core.BlockTimestamp;
import sonchain.blockchain.core.TimePoint;
import sonchain.blockchain.net.SonMessageCodes;
import sonchain.blockchain.service.DataCenter;
import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.util.FastByteComparisons;
import sonchain.blockchain.util.Numeric;

/**
 * 共识消息装载器
 * @author GAIA
 *
 */
public class ConsensusPayload{
	public static final Logger m_logger = Logger.getLogger(ConsensusPayload.class);
	public int m_version = 0;
	public byte[] m_preHash = null;
	public int m_blockNumber = 0;
	public int m_validatorIndex = 0;
	public BlockTimestamp m_timestamp;
	public byte[] m_data = null;
	public Witness m_script = null;
	public SonMessageCodes m_messageType = SonMessageCodes.ChangeView;	
	private byte[] m_hash = null;

	public Witness[] getScripts() {
		Witness[] scripts = new Witness[1];
		scripts[0] = m_script;
		return scripts;
	}

	public void setScripts(Witness[] witness) {
		if(witness == null || witness.length != 1){
			throw new IllegalArgumentException();
		}
		m_script = witness[0];
	}

	public void deserializeUnsigned(Binary reader) {
		try
		{
			m_logger.debug(" deserializeUnsigned start.");
			m_messageType = SonMessageCodes.fromByte(reader.ReadByte());
			m_version = reader.ReadInt();
			String preHashString = reader.ReadString();
			if(!preHashString.equals(""))
			{
				m_preHash = Numeric.hexStringToByteArray(preHashString);
			}
			else
			{
				m_preHash = ByteUtil.EMPTY_BYTE_ARRAY;
			}
			m_blockNumber = reader.ReadInt();
			m_validatorIndex = reader.ReadInt();
			String timeStamp = reader.ReadString();
			TimePoint point = TimePoint.from_iso_string(timeStamp);
			m_timestamp = new BlockTimestamp(point);
			String dataString = reader.ReadString();
			if(!dataString.equals(""))
			{
				m_data = Numeric.hexStringToByteArray(dataString);
			}
			else
			{
				m_data = ByteUtil.EMPTY_BYTE_ARRAY;
			}			
			m_logger.debug(" deserializeUnsigned end.");
		}catch(Exception ex){
			m_logger.error(" deserializeUnsigned error:" + ex.getMessage());
		}		
	}

	public List<byte[]> getScriptHashesForVerifying() {
		//TODO
//	      ECPoint[] validators = Blockchain.Default.GetValidators();
//          if (validators.Length <= ValidatorIndex)
//              throw new InvalidOperationException();
//          return new[] { Contract.CreateSignatureRedeemScript(validators[ValidatorIndex]).ToScriptHash() };
		return null;
	}

	public void serializeUnsigned(Binary writer) {
		try
		{
			m_logger.debug(" serializeUnsigned start.");
			writer.WriteByte(m_messageType.asByte());
			writer.WriteInt(m_version);
			String preHashStr = "";
			if(m_preHash != null)
			{
				preHashStr = Hex.toHexString(m_preHash);
			}
			writer.WriteString(preHashStr);
			writer.WriteInt(m_blockNumber);
			writer.WriteInt(m_validatorIndex);
			writer.WriteString(m_timestamp.toTimePoint().toString());
			String dataStr = "";
			if(m_data != null)
			{
				dataStr = Hex.toHexString(m_data);
			}
			writer.WriteString(dataStr);
			m_logger.debug(" serializeUnsigned end.");
		}catch(Exception ex){
			m_logger.error(" serializeUnsigned error:" + ex.getMessage());
		}	
		
	}

	public int getSize() {
		int size = 1;
		size += 4;
		String preHashStr = "";
		if(m_preHash != null)
		{
			preHashStr = Hex.toHexString(m_preHash);
		}
		size += preHashStr.length();
		size += 4;
		size += 4;
		size += 8;
		String dataStr = "";
		if(m_data != null)
		{
			dataStr = Hex.toHexString(m_data);
		}
		size += dataStr.length();
		size += 4;
		//size += m_script.getSize();		
		return size;
	}

	public void serialize(Binary writer) {
		try
		{
			m_logger.debug(" serialize start.");
			serializeUnsigned(writer);
			writer.WriteInt(1);
			//m_script.serialize(writer);
			m_logger.debug(" serialize end.");
		}catch(Exception ex){
			m_logger.error(" serialize error:" + ex.getMessage());
		}	
	}

	public void deserialize(Binary reader) {
		try
		{
			m_logger.debug(" deserialize start.");
			deserializeUnsigned(reader);
			if(reader.ReadInt() != 1){
				throw new ClassFormatError();
			}
			//m_script = new Witness();
			//m_script.deserialize(reader);	
			m_logger.debug(" deserialize end.");
		}catch(Exception ex){
			m_logger.error(" deserialize error:" + ex.getMessage());
		}			
	}

	public byte[] getMessage() {
		Binary writer = new Binary();
		serializeUnsigned(writer);
		return writer.GetBytes();
	}

	public byte[] getHash() {
		if(m_hash == null){
			Binary writer = new Binary();
			serializeUnsigned(writer);
			m_hash = writer.GetBytes();
			//m_hash = Crypto.Default.Hash256(writer.GetBytes());
		}
		return m_hash;
	}

	public InventoryType getInventoryType() {
		return InventoryType.Consensus;
	}

	public boolean verify() {
		m_logger.debug("verify start");
		Block block = DataCenter.getSonChainImpl().getBlockChain().getBestBlock();
        if (m_blockNumber <= block.getBlockNumber()){
    		m_logger.debug(String.format("verify failed blockNumber={%d}, BestBlockNumber={%d}"
    				, m_blockNumber, block.getBlockNumber()));
            return false;
        }
//        List<byte[]> hashes = getScriptHashesForVerifying();
//        Witness[] witnesses = getScripts();
//        if(hashes == null || hashes.size() != witnesses.length){
//    		m_logger.debug(String.format("verify failed the length is not equal hashlength={%d}, Scriptlength={%d}"
//    				, hashes.size(), witnesses.length));
//        	return false;
//        }
//        for(int i = 0; i < hashes.size(); i++)
//        {
//            byte[] verification = witnesses[i].m_verificationScript;
//            if (verification.length == 0)
//            {
////                using (ScriptBuilder sb = new ScriptBuilder())
////                {
////                    sb.EmitAppCall(hashes[i].ToArray());
////                    verification = sb.ToArray();
////                }
//            }
//            else
//            {
//                if (!FastByteComparisons.equal(hashes.get(i), witnesses[i].getScriptHash())){
//                	return false;
//                }
//            }
////            using (StateReader service = new StateReader())
////            {
////                ApplicationEngine engine = new ApplicationEngine(TriggerType.Verification, verifiable, Blockchain.Default, service, Fixed8.Zero);
////                engine.LoadScript(verification, false);
////                engine.LoadScript(verifiable.Scripts[i].InvocationScript, true);
////                if (!engine.Execute()) return false;
////                if (engine.EvaluationStack.Count != 1 || !engine.EvaluationStack.Pop().GetBoolean()) return false;
////            }
//        }
        boolean result = true;
		m_logger.debug("verify end result:" + result);
		return result;
	}	
}
