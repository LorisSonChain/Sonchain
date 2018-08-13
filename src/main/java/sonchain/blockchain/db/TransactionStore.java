package sonchain.blockchain.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PreDestroy;

import sonchain.blockchain.core.TransactionInfo;
import sonchain.blockchain.datasource.ObjectDataSource;
import sonchain.blockchain.datasource.base.Serializer;
import sonchain.blockchain.datasource.base.Source;
import sonchain.blockchain.util.FastByteComparisons;
import sonchain.blockchain.util.RLP;
import sonchain.blockchain.util.RLPList;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TransactionStore extends ObjectDataSource<List<TransactionInfo>> {

	public static final Logger m_logger = Logger.getLogger(TransactionStore.class);
	private final LRUMap<StringWrapper, Object> m_lastSavedTxHash = new LRUMap<>(5000);
	private final Object m_object = new Object();

	private final static Serializer<List<TransactionInfo>, String> serializer = new Serializer<List<TransactionInfo>, String>() {
		@Override
		public String serialize(List<TransactionInfo> object) {
			try
			{
				ObjectMapper mapper = new ObjectMapper(); 
				ArrayNode arrayNode = mapper.createArrayNode();
				for(int i = 0; i < object.size(); i++){
					TransactionInfo info = object.get(i);
					ObjectNode node = mapper.createObjectNode();
					info.toJson(node);
					arrayNode.add(node);
				}
				String jsonStr =  mapper.writeValueAsString (arrayNode);
				m_logger.debug(" TransactionStore serializer Json String is :" + jsonStr);
				return jsonStr;
			}
			catch(Exception ex){
				m_logger.error(" TransactionStore serializer error:" + ex.getMessage());
				return "";
			}
		}

		@Override
		public List<TransactionInfo> deserialize(String stream) {
			try {
				if (stream == null){
					return null;
				}
				ObjectMapper mapper = new ObjectMapper(); 
				JsonNode node = mapper.readTree(stream); 
				List<TransactionInfo> infos = new ArrayList<TransactionInfo>();
				for(JsonNode childNode : node){
					TransactionInfo info = new TransactionInfo();
					info.jsonParse(childNode);
					infos.add(info);
				}
				return infos;
			} catch (Exception e) {
				// fallback to previous DB version
				return null;
			}
		}
	};

	public TransactionInfo get(byte[] txHash, byte[] blockHash) {
		String strHash = Hex.toHexString(txHash);
		List<TransactionInfo> existingInfos = get(strHash);
		for (TransactionInfo info : existingInfos) {
			if (FastByteComparisons.equal(info.getBlockHash(), blockHash)) {
				return info;
			}
		}
		return null;
	}

	public boolean put(TransactionInfo tx) {
		byte[] txHash =  tx.getReceipt().getTransaction().getTransaction().getHash();
		String strHash = Hex.toHexString(txHash);
		List<TransactionInfo> existingInfos = null;
		synchronized (m_lastSavedTxHash) {
			if (m_lastSavedTxHash.put(new StringWrapper(strHash), m_object) != null || !m_lastSavedTxHash.isFull()) {
				existingInfos = get(strHash);
			}
		}
		// else it is highly unlikely that the transaction was included into
		// another block
		// earlier than 5000 transactions before with regard to regular block
		// import process

		if (existingInfos == null) {
			existingInfos = new ArrayList<>();
		} else {
			for (TransactionInfo info : existingInfos) {
				if (FastByteComparisons.equal(info.getBlockHash(), tx.getBlockHash())) {
					return false;
				}
			}
		}
		existingInfos.add(tx);
		put(strHash, existingInfos);
		return true;
	}

	public TransactionStore(Source<String, String> src) {
		super(src, serializer, 256);
	}

	@PreDestroy
	public void close() {
		// try {
		// logger.info("Closing TransactionStore...");
		// super.close();
		// } catch (Exception e) {
		// logger.warn("Problems closing TransactionStore", e);
		// }
	}

}
