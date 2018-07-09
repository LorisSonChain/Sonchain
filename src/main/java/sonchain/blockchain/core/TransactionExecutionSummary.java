package sonchain.blockchain.core;

import java.math.BigInteger;
import java.util.*;

import org.apache.commons.lang3.ArrayUtils;

import sonchain.blockchain.util.BIUtil;
import sonchain.blockchain.util.RLP;
import sonchain.blockchain.util.RLPElement;
import sonchain.blockchain.util.RLPList;
import sonchain.blockchain.vm.DataWord;
import sonchain.blockchain.vm.LogInfo;

/**
 *
 */
public class TransactionExecutionSummary {
    private List<DataWord> m_deletedAccounts = Collections.emptyList();
    private boolean m_failed = false;
    private List<InternalTransaction> m_internalTransactions = Collections.emptyList();
    private List<LogInfo> m_logs = null;
    private boolean m_parsed = false;
    private byte[] m_result = null;
    private byte[] m_rlpEncoded = null;
    private Map<DataWord, DataWord> m_storageDiff = Collections.emptyMap();
	private Transaction m_transaction = null;
    private BigInteger m_value = BigInteger.ZERO;
   
    /**
     * constructor
     * @param transaction
     */
    public TransactionExecutionSummary(Transaction transaction) {
    	m_transaction = transaction;
    	m_value = BIUtil.toBI(transaction.getValue());
    }

    /**
     * constructor
     * @param rlpEncoded
     */
    public TransactionExecutionSummary(byte[] rlpEncoded) {
    	m_rlpEncoded = rlpEncoded;
    	m_parsed = false;
    }
    
    private static BigInteger decodeBigInteger(byte[] encoded) {
        return ArrayUtils.isEmpty(encoded) ? BigInteger.ZERO : new BigInteger(1, encoded);
    }

    private static List<DataWord> decodeDeletedAccounts(RLPList deletedAccounts) {
        List<DataWord> result = new ArrayList<>();
        for (RLPElement deletedAccount : deletedAccounts) {
            result.add(new DataWord(deletedAccount.getRLPData()));
        }
        return result;
    }

    private static List<InternalTransaction> decodeInternalTransactions(RLPList internalTransactions) {
        List<InternalTransaction> result = new ArrayList<>();
        for (RLPElement internalTransaction : internalTransactions) {
            result.add(new InternalTransaction(internalTransaction.getRLPData()));
        }
        return result;
    }

    private static List<LogInfo> decodeLogs(RLPList logs) {
        ArrayList<LogInfo> result = new ArrayList<>();
        for (RLPElement log : logs) {
            result.add(new LogInfo(log.getRLPData()));
        }
        return result;
    }
    
    private static Map<DataWord, DataWord> decodeStorageDiff(RLPList storageDiff) {
        Map<DataWord, DataWord> result = new HashMap<>();
        for (RLPElement entry : storageDiff) {
            DataWord key = new DataWord(((RLPList) entry).get(0).getRLPData());
            DataWord value = new DataWord(((RLPList) entry).get(1).getRLPData());
            result.put(key, value);
        }
        return result;
    }

    private static byte[] encodeDeletedAccounts(List<DataWord> deletedAccounts) {
        byte[][] result = new byte[deletedAccounts.size()][];
        for (int i = 0; i < deletedAccounts.size(); i++) {
            DataWord deletedAccount = deletedAccounts.get(i);
            result[i] = RLP.encodeElement(deletedAccount.getData());
        }
        return RLP.encodeList(result);
    }
    
    private static byte[] encodeInternalTransactions(List<InternalTransaction> internalTransactions) {
        byte[][] result = new byte[internalTransactions.size()][];
        for (int i = 0; i < internalTransactions.size(); i++) {
            InternalTransaction transaction = internalTransactions.get(i);
            result[i] = transaction.getEncoded();
        }

        return RLP.encodeList(result);
    }
    
    private static byte[] encodeLogs(List<LogInfo> logs) {
        byte[][] result = new byte[logs.size()][];
        for (int i = 0; i < logs.size(); i++) {
            LogInfo log = logs.get(i);
            result[i] = log.getEncoded();
        }

        return RLP.encodeList(result);
    }

    private static byte[] encodeStorageDiff(Map<DataWord, DataWord> storageDiff) {
        byte[][] result = new byte[storageDiff.size()][];
        int i = 0;
        for (Map.Entry<DataWord, DataWord> entry : storageDiff.entrySet()) {
            byte[] key = RLP.encodeElement(entry.getKey().getData());
            byte[] value = RLP.encodeElement(entry.getValue().getData());
            result[i++] = RLP.encodeList(key, value);
        }
        return RLP.encodeList(result);
    }
    
    public byte[] getEncoded() {
        if (m_rlpEncoded != null){
        	return m_rlpEncoded;
        }
        m_rlpEncoded = RLP.encodeList(
        		m_transaction.getEncoded(),
                RLP.encodeBigInteger(m_value),
                encodeDeletedAccounts(m_deletedAccounts),
                encodeInternalTransactions(m_internalTransactions),
                RLP.encodeElement(m_result),
                encodeLogs(m_logs),
                RLP.encodeInt(m_failed ? 1 : 0)
        );
        return m_rlpEncoded;
    }
    
    public Transaction getTransaction() {
        if (!m_parsed) {
        	rlpParse();
        }
        return m_transaction;
    }

    public byte[] getTransactionHash() {
        return getTransaction().getHash();
    }

    public BigInteger getValue() {
        if (!m_parsed) {
        	rlpParse();
        }
        return m_value;
    }

    public List<DataWord> getDeletedAccounts() {
        if (!m_parsed){
        	rlpParse();
        }
        return m_deletedAccounts;
    }
    
    public List<InternalTransaction> getInternalTransactions() {
        if (!m_parsed) {
        	rlpParse();
        }
        return m_internalTransactions;
    }

    @Deprecated
    /* Use getTouchedStorage().getAll() instead */
    public Map<DataWord, DataWord> getStorageDiff() {
        if (!m_parsed) {
        	rlpParse();
        }
        return m_storageDiff;
    }
    
    public boolean isFailed() {
        if (!m_parsed) {
        	rlpParse();
        }
        return m_failed;
    }

    public byte[] getResult() {
        if (!m_parsed){
        	rlpParse();
        }
        return m_result;
    }

    public List<LogInfo> getLogs() {
        if (!m_parsed) {
        	rlpParse();
        }
        return m_logs;
    }

    public static Builder builderFor(Transaction transaction) {
        return new Builder(transaction);
    }

    public void rlpParse() {
        if (m_parsed) {
        	return;
        }
        RLPList decodedTxList = RLP.decode2(m_rlpEncoded);
        RLPList summary = (RLPList) decodedTxList.get(0);
        m_transaction = new Transaction(summary.get(0).getRLPData());
        m_value = decodeBigInteger(summary.get(1).getRLPData());
        m_deletedAccounts = decodeDeletedAccounts((RLPList) summary.get(2));
        m_internalTransactions = decodeInternalTransactions((RLPList) summary.get(3));
        m_result = summary.get(4).getRLPData();
        m_logs = decodeLogs((RLPList) summary.get(5));
        byte[] failed = summary.get(6).getRLPData();
        m_failed = ArrayUtils.isNotEmpty(failed) && RLP.decodeInt(failed, 0) == 1;
    }

    public static class Builder {

        private final TransactionExecutionSummary m_summary;
        Builder(Transaction transaction) {
            //Assert.notNull(transaction, "Cannot build TransactionExecutionSummary for null transaction.");
        	m_summary = new TransactionExecutionSummary(transaction);
        }

        public Builder internalTransactions(List<InternalTransaction> internalTransactions) {
        	m_summary.m_internalTransactions = Collections.unmodifiableList(internalTransactions);
            return this;
        }

        public Builder deletedAccounts(Set<DataWord> deletedAccounts) {
        	m_summary.m_deletedAccounts = new ArrayList<>();
            for (DataWord account : deletedAccounts) {
            	m_summary.m_deletedAccounts.add(account);
            }
            return this;
        }

        public Builder storageDiff(Map<DataWord, DataWord> storageDiff) {
        	m_summary.m_storageDiff = Collections.unmodifiableMap(storageDiff);
            return this;
        }

        public Builder markAsFailed() {
        	m_summary.m_failed = true;
            return this;
        }

        public Builder logs(List<LogInfo> logs) {
        	m_summary.m_logs = logs;
            return this;
        }

        public Builder result(byte[] result) {
        	m_summary.m_result = result;
            return this;
        }

        public TransactionExecutionSummary build() {
        	m_summary.m_parsed = true;
            if (m_summary.m_failed) {
                for (InternalTransaction transaction : m_summary.m_internalTransactions) {
                    transaction.reject();
                }
            }
            return m_summary;
        }
    }
}
