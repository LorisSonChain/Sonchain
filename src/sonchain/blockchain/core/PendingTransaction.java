package sonchain.blockchain.core;

import sonchain.blockchain.util.ByteUtil;

import java.math.BigInteger;
import java.util.Arrays;

public class PendingTransaction {

    /**
     * number of block that was best at the moment when transaction's been added
     */
    private long m_blockNumber;
	/**
     * transaction
     */
    private Transaction m_transaction;

    public PendingTransaction(byte[] bytes) {
        parse(bytes);
    }

    public PendingTransaction(Transaction transaction) {
        this(transaction, 0);
    }

    public PendingTransaction(Transaction transaction, long blockNumber) {
    	m_transaction = transaction;
    	m_blockNumber = blockNumber;
    }

    /**
     *  Two pending transaction are equal if equal their sender + nonce
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
        	return true;
        }
        if (!(o instanceof PendingTransaction)) {
        	return false;
        }
        PendingTransaction that = (PendingTransaction) o;
        return Arrays.equals(getSender(), that.getSender()) &&
                Arrays.equals(m_transaction.getNonce(), that.getTransaction().getNonce());
    }

    public Transaction getTransaction() {
    	return m_transaction;
    }

    public long getBlockNumber() {
        return m_blockNumber;
    }

    public byte[] getSender() {
        return m_transaction.getSender();
    }

    public byte[] getHash() {
        return m_transaction.getHash();
    }

    public byte[] getBytes() {
        byte[] numberBytes = BigInteger.valueOf(m_blockNumber).toByteArray();
        byte[] txBytes = m_transaction.getEncoded();
        byte[] bytes = new byte[1 + numberBytes.length + txBytes.length];

        bytes[0] = (byte) numberBytes.length;
        System.arraycopy(numberBytes, 0, bytes, 1, numberBytes.length);
        System.arraycopy(txBytes, 0, bytes, 1 + numberBytes.length, txBytes.length);
        return bytes;
    }

    @Override
    public int hashCode() {
        return ByteUtil.byteArrayToInt(getSender()) + ByteUtil.byteArrayToInt(m_transaction.getNonce());
    }
    @Override
    public String toString() {
        return "PendingTransaction [" +
                "  transaction=" + m_transaction +
                ", blockNumber=" + m_blockNumber +
                ']';
    }

    private void parse(byte[] bytes) {
        byte[] numberBytes = new byte[bytes[0]];
        byte[] txBytes = new byte[bytes.length - 1 - numberBytes.length];
        System.arraycopy(bytes, 1, numberBytes, 0, numberBytes.length);
        System.arraycopy(bytes, 1 + numberBytes.length, txBytes, 0, txBytes.length);
        m_blockNumber = new BigInteger(numberBytes).longValue();
        m_transaction = new Transaction(txBytes);
    }

}
