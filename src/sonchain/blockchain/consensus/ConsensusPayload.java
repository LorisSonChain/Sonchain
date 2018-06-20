package sonchain.blockchain.consensus;

import java.util.List;

import org.apache.log4j.Logger;

import sonchain.blockchain.base.Binary;
import sonchain.blockchain.core.Block;
import sonchain.blockchain.service.DataCenter;
import sonchain.blockchain.util.FastByteComparisons;

public class ConsensusPayload implements IInventory{
	public static final Logger m_logger = Logger.getLogger(ConsensusPayload.class);
	public int m_version = 0;
	public byte[] m_preHash = null;
	public int m_blockNumber = 0;
	public int m_validatorIndex = 0;
	public long m_timestamp = 0;
	public byte[] m_data = null;
	public Witness m_script = null;
	
	private byte[] m_hash = null;

	@Override
	public Witness[] getScripts() {
		Witness[] scripts = new Witness[1];
		scripts[0] = m_script;
		return null;
	}

	@Override
	public void setScripts(Witness[] witness) {
		if(witness == null || witness.length != 1){
			throw new IllegalArgumentException();
		}
		m_script = witness[0];
	}

	@Override
	public void deserializeUnsigned(Binary reader) {
		try
		{
			m_logger.debug(" deserializeUnsigned start.");
			m_version = reader.ReadInt();
			int size = reader.ReadInt();
			m_preHash = new byte[size];
			for(int i = 0; i < size; i++){
				m_preHash[i] = reader.ReadByte();
			}
			m_blockNumber = reader.ReadInt();
			m_validatorIndex = reader.ReadInt();
			m_timestamp = (long)reader.ReadDouble();
			size = reader.ReadInt();
			m_data = new byte[size];
			for(int i = 0; i < size; i++){
				m_data[i] = reader.ReadByte();
			}
			m_logger.debug(" deserializeUnsigned end.");
		}catch(Exception ex){
			m_logger.error(" deserializeUnsigned error:" + ex.getMessage());
		}		
	}

	@Override
	public List<byte[]> getScriptHashesForVerifying() {
		//TODO
//	      ECPoint[] validators = Blockchain.Default.GetValidators();
//          if (validators.Length <= ValidatorIndex)
//              throw new InvalidOperationException();
//          return new[] { Contract.CreateSignatureRedeemScript(validators[ValidatorIndex]).ToScriptHash() };
		return null;
	}

	@Override
	public void serializeUnsigned(Binary writer) {
		try
		{
			m_logger.debug(" serializeUnsigned start.");
			writer.WriteInt(m_version);
			int size = m_preHash.length;
			writer.WriteInt(size);
			for(int i = 0; i < size; i++){
				writer.WriteByte(m_preHash[i]);
			}
			writer.WriteInt(m_blockNumber);
			writer.WriteInt(m_validatorIndex);
			writer.WriteDouble(m_timestamp);
			size = m_data.length;
			writer.WriteInt(size);
			for(int i = 0; i < size; i++){
				writer.WriteByte(m_data[i]);
			}
			m_logger.debug(" serializeUnsigned end.");
		}catch(Exception ex){
			m_logger.error(" serializeUnsigned error:" + ex.getMessage());
		}	
		
	}

	@Override
	public int getSize() {
		int size = 4;
		size += 4;
		size += m_preHash.length;
		size += 4;
		size += 4;
		size += 8;
		size += 4;
		size += m_data.length;
		size += 1;
		size += m_script.getSize();		
		return size;
	}

	@Override
	public void serialize(Binary writer) {
		try
		{
			m_logger.debug(" serialize start.");
			serializeUnsigned(writer);
			writer.WriteInt(1);
			m_script.serialize(writer);
			m_logger.debug(" serialize end.");
		}catch(Exception ex){
			m_logger.error(" serialize error:" + ex.getMessage());
		}		
			
		}

	@Override
	public void deserialize(Binary reader) {
		try
		{
			m_logger.debug(" deserialize start.");
			deserializeUnsigned(reader);
			if(reader.ReadInt() != 1){
				throw new ClassFormatError();
			}
			m_script = new Witness();
			m_script.deserialize(reader);	
			m_logger.debug(" deserialize end.");
		}catch(Exception ex){
			m_logger.error(" deserialize error:" + ex.getMessage());
		}			
	}

	@Override
	public byte[] getMessage() {
		Binary writer = new Binary();
		serializeUnsigned(writer);
		return writer.GetBytes();
	}

	@Override
	public byte[] getHash() {
		if(m_hash == null){
			Binary writer = new Binary();
			serializeUnsigned(writer);
			m_hash = writer.GetBytes();
			//m_hash = Crypto.Default.Hash256(writer.GetBytes());
		}
		return m_hash;
	}

	@Override
	public InventoryType getInventoryType() {
		return InventoryType.Consensus;
	}

	@Override
	public boolean verify() {
		m_logger.debug("verify start");
		Block block = DataCenter.getSonChainImpl().getBlockChain().getBestBlock();
        if (m_blockNumber <= block.getNumber()){
    		m_logger.debug(String.format("verify failed blockNumber={%d}, BestBlockNumber={%d}"
    				, m_blockNumber, block.getNumber()));
            return false;
        }
        List<byte[]> hashes = getScriptHashesForVerifying();
        Witness[] witnesses = getScripts();
        if(hashes == null || hashes.size() != witnesses.length){
    		m_logger.debug(String.format("verify failed the length is not equal hashlength={%d}, Scriptlength={%d}"
    				, hashes.size(), witnesses.length));
        	return false;
        }
        for(int i = 0; i < hashes.size(); i++)
        {
            byte[] verification = witnesses[i].m_verificationScript;
            if (verification.length == 0)
            {
//                using (ScriptBuilder sb = new ScriptBuilder())
//                {
//                    sb.EmitAppCall(hashes[i].ToArray());
//                    verification = sb.ToArray();
//                }
            }
            else
            {
                if (!FastByteComparisons.equal(hashes.get(i), witnesses[i].getScriptHash())){
                	return false;
                }
            }
//            using (StateReader service = new StateReader())
//            {
//                ApplicationEngine engine = new ApplicationEngine(TriggerType.Verification, verifiable, Blockchain.Default, service, Fixed8.Zero);
//                engine.LoadScript(verification, false);
//                engine.LoadScript(verifiable.Scripts[i].InvocationScript, true);
//                if (!engine.Execute()) return false;
//                if (engine.EvaluationStack.Count != 1 || !engine.EvaluationStack.Pop().GetBoolean()) return false;
//            }
        }
        boolean result = true;
		m_logger.debug("verify end result:" + result);
		return result;
	}	
}
