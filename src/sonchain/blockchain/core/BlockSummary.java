package sonchain.blockchain.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.util.Functional;
import sonchain.blockchain.util.RLP;
import sonchain.blockchain.util.RLPElement;
import sonchain.blockchain.util.RLPList;

public class BlockSummary {
	
	private Block m_block = null;
	private List<TransactionReceipt> m_receipts = null;
	private List<TransactionExecutionSummary> m_summaries = null;

	/**
	 * Constructor
	 * 
	 * @param rlp
	 */
	public BlockSummary(byte[] rlp) {
		RLPList summary = (RLPList) RLP.decode2(rlp).get(0);
		m_block = new Block(summary.get(0).getRLPData());
		m_summaries = decodeSummaries((RLPList) summary.get(1));
		m_receipts = new ArrayList<>();

		Map<String, TransactionReceipt> receiptByTxHash = decodeReceipts((RLPList) summary.get(2));
		List<Transaction> listTransaction = m_block.getTransactionsList();
		for (Transaction tx : listTransaction) {
			TransactionReceipt receipt = receiptByTxHash.get(ByteUtil.toHexString(tx.getHash()));
			receipt.setTransaction(tx);
			m_receipts.add(receipt);
		}
	}

	/**
	 * Constructor
	 * 
	 * @param block
	 * @param receipts
	 * @param summaries
	 */
	public BlockSummary(Block block, List<TransactionReceipt> receipts, List<TransactionExecutionSummary> summaries) {
		m_block = block;
		m_receipts = receipts;
		m_summaries = summaries;
	}

	public Block getBlock() {
		return m_block;
	}

	public List<TransactionReceipt> getReceipts() {
		return m_receipts;
	}

	public List<TransactionExecutionSummary> getSummaries() {
		return m_summaries;
	}

	public byte[] getEncoded() {
		return RLP.encodeList(m_block.getEncoded(), encodeSummaries(m_summaries), encodeReceipts(m_receipts));
	}

	private static <T> List<T> decodeList(RLPList list, Functional.Function<byte[], T> decoder) {
		List<T> result = new ArrayList<>();
		for (RLPElement item : list) {
			result.add(decoder.apply(item.getRLPData()));
		}
		return result;
	}

	private static <K, V> Map<K, V> decodeMap(RLPList list, Functional.Function<byte[], K> keyDecoder,
			Functional.Function<byte[], V> valueDecoder) {
		Map<K, V> result = new HashMap<>();
		for (RLPElement entry : list) {
			K key = keyDecoder.apply(((RLPList) entry).get(0).getRLPData());
			V value = valueDecoder.apply(((RLPList) entry).get(1).getRLPData());
			result.put(key, value);
		}
		return result;
	}

	private static Map<String, TransactionReceipt> decodeReceipts(RLPList receipts) {
		return decodeMap(receipts, new Functional.Function<byte[], String>() {
			@Override
			public String apply(byte[] bytes) {
				return new String(bytes);
			}
		}, new Functional.Function<byte[], TransactionReceipt>() {
			@Override
			public TransactionReceipt apply(byte[] encoded) {
				return new TransactionReceipt(encoded);
			}
		});
	}

	private static List<TransactionExecutionSummary> decodeSummaries(RLPList summaries) {
		return decodeList(summaries, new Functional.Function<byte[], TransactionExecutionSummary>() {
			@Override
			public TransactionExecutionSummary apply(byte[] encoded) {
				return new TransactionExecutionSummary(encoded);
			}
		});
	}

	private static <T> byte[] encodeList(List<T> entries, Functional.Function<T, byte[]> encoder) {
		byte[][] result = new byte[entries.size()][];
		for (int i = 0; i < entries.size(); i++) {
			result[i] = encoder.apply(entries.get(i));
		}
		return RLP.encodeList(result);
	}

	private static <K, V> byte[] encodeMap(Map<K, V> map, Functional.Function<K, byte[]> keyEncoder,
			Functional.Function<V, byte[]> valueEncoder) {
		byte[][] result = new byte[map.size()][];
		int i = 0;
		for (Map.Entry<K, V> entry : map.entrySet()) {
			byte[] key = keyEncoder.apply(entry.getKey());
			byte[] value = valueEncoder.apply(entry.getValue());
			result[i++] = RLP.encodeList(key, value);
		}
		return RLP.encodeList(result);
	}

	private static byte[] encodeSummaries(final List<TransactionExecutionSummary> summaries) {
		return encodeList(summaries, new Functional.Function<TransactionExecutionSummary, byte[]>() {
			@Override
			public byte[] apply(TransactionExecutionSummary summary) {
				return summary.getEncoded();
			}
		});
	}
	
	private static byte[] encodeReceipts(List<TransactionReceipt> receipts) {
		Map<String, TransactionReceipt> receiptByTxHash = new HashMap<>();
		for (TransactionReceipt receipt : receipts) {
			receiptByTxHash.put(ByteUtil.toHexString(receipt.getTransaction().getHash()), receipt);
		}
		return encodeMap(receiptByTxHash, new Functional.Function<String, byte[]>() {
			@Override
			public byte[] apply(String txHash) {
				return RLP.encodeString(txHash);
			}
		}, new Functional.Function<TransactionReceipt, byte[]>() {
			@Override
			public byte[] apply(TransactionReceipt receipt) {
				return receipt.getEncoded();
			}
		});
	}
}