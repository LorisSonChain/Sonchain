package sonchain.blockchain.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PreDestroy;

import sonchain.blockchain.core.TransactionInfo;
import sonchain.blockchain.datasource.ObjectDataSource;
import sonchain.blockchain.datasource.Serializer;
import sonchain.blockchain.datasource.Source;
import sonchain.blockchain.util.FastByteComparisons;
import sonchain.blockchain.util.RLP;
import sonchain.blockchain.util.RLPList;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.log4j.Logger;

public class TransactionStore extends ObjectDataSource<List<TransactionInfo>> {

	public static final Logger m_logger = Logger.getLogger(TransactionStore.class);
	private final LRUMap<ByteArrayWrapper, Object> m_lastSavedTxHash = new LRUMap<>(5000);
	private final Object m_object = new Object();

	private final static Serializer<List<TransactionInfo>, byte[]> serializer = new Serializer<List<TransactionInfo>, byte[]>() {
		@Override
		public byte[] serialize(List<TransactionInfo> object) {
			byte[][] txsRlp = new byte[object.size()][];
			for (int i = 0; i < txsRlp.length; i++) {
				txsRlp[i] = object.get(i).getEncoded();
			}
			return RLP.encodeList(txsRlp);
		}

		@Override
		public List<TransactionInfo> deserialize(byte[] stream) {
			try {
				if (stream == null){
					return null;
				}
				RLPList params = RLP.decode2(stream);
				RLPList infoList = (RLPList) params.get(0);
				List<TransactionInfo> ret = new ArrayList<>();
				for (int i = 0; i < infoList.size(); i++) {
					ret.add(new TransactionInfo(infoList.get(i).getRLPData()));
				}
				return ret;
			} catch (Exception e) {
				// fallback to previous DB version
				return Collections.singletonList(new TransactionInfo(stream));
			}
		}
	};

	public TransactionInfo get(byte[] txHash, byte[] blockHash) {
		List<TransactionInfo> existingInfos = get(txHash);
		for (TransactionInfo info : existingInfos) {
			if (FastByteComparisons.equal(info.getBlockHash(), blockHash)) {
				return info;
			}
		}
		return null;
	}

	/**
	 * Adds TransactionInfo to the store. If entries for this transaction
	 * already exist the method adds new entry to the list if no entry for the
	 * same block exists
	 * 
	 * @return true if TransactionInfo was added, false if already exist
	 */
	public boolean put(TransactionInfo tx) {
		byte[] txHash = tx.getReceipt().getTransaction().getHash();

		List<TransactionInfo> existingInfos = null;
		synchronized (m_lastSavedTxHash) {
			if (m_lastSavedTxHash.put(new ByteArrayWrapper(txHash), m_object) != null || !m_lastSavedTxHash.isFull()) {
				existingInfos = get(txHash);
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
		put(txHash, existingInfos);
		return true;
	}

	public TransactionStore(Source<byte[], byte[]> src) {
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
