package sonchain.blockchain.accounts;

import java.math.BigInteger;

import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.crypto.HashUtil;
import sonchain.blockchain.service.DataCenter;
import sonchain.blockchain.util.FastByteComparisons;
import sonchain.blockchain.util.RLP;
import sonchain.blockchain.util.RLPList;

/**
 * AccountState
 *
 */
public class AccountState {
	
    private BigInteger m_balance = BigInteger.ZERO;
    private byte[] m_codeHash = null;
    private BigInteger m_nonce = BigInteger.ZERO;	
    private byte[] m_rlpEncoded = null;
    private byte[] m_stateRoot = null;
    
    public AccountState() {
        this(DataCenter.m_config.getCommonConstants().getInitialNonce(), BigInteger.ZERO);
    }
    
    /**
     * AccountState
     * @param nonce
     * @param balance
     */
    public AccountState(BigInteger nonce, BigInteger balance) {
        this(nonce, balance, HashUtil.EMPTY_TRIE_HASH, HashUtil.EMPTY_DATA_HASH);
    }

    /**
     * AccountState
     * @param nonce
     * @param balance
     * @param stateRoot
     * @param codeHash
     */
    public AccountState(BigInteger nonce, BigInteger balance, byte[] stateRoot, byte[] codeHash) {
    	m_nonce = nonce;
    	m_balance = balance;
    	m_stateRoot = stateRoot == HashUtil.EMPTY_TRIE_HASH 
        		|| FastByteComparisons.equal(stateRoot, HashUtil.EMPTY_TRIE_HASH) ?
        				HashUtil.EMPTY_TRIE_HASH : stateRoot;
    	m_codeHash = codeHash == HashUtil.EMPTY_DATA_HASH 
        		|| FastByteComparisons.equal(codeHash, HashUtil.EMPTY_DATA_HASH) ?
        				HashUtil.EMPTY_DATA_HASH : codeHash;
    }

    /**
     * AccountState
     * @param rlpData
     */
    public AccountState(byte[] rlpData) {
    	m_rlpEncoded = rlpData;

        RLPList items = (RLPList) RLP.decode2(m_rlpEncoded).get(0);
        m_nonce = items.get(0).getRLPData() == null ? BigInteger.ZERO
                : new BigInteger(1, items.get(0).getRLPData());
        m_balance = items.get(1).getRLPData() == null ? BigInteger.ZERO
                : new BigInteger(1, items.get(1).getRLPData());
        m_stateRoot = items.get(2).getRLPData();
        m_codeHash = items.get(3).getRLPData();
    }

    public BigInteger getBalance() {
        return m_balance;
    }

    public byte[] getCodeHash() {
        return m_codeHash;
    }

    public byte[] getEncoded() {
        if (m_rlpEncoded == null) {
            byte[] nonce = RLP.encodeBigInteger(m_nonce);
            byte[] balance = RLP.encodeBigInteger(m_balance);
            byte[] stateRoot = RLP.encodeElement(m_stateRoot);
            byte[] codeHash = RLP.encodeElement(m_codeHash);
            m_rlpEncoded = RLP.encodeList(nonce, balance, stateRoot, codeHash);
        }
        return m_rlpEncoded;
    }

    public BigInteger getNonce() {
        return m_nonce;
    }

    public byte[] getStateRoot() {
        return m_stateRoot;
    }

    public boolean isEmpty() {
        return FastByteComparisons.equal(m_codeHash, HashUtil.EMPTY_DATA_HASH) &&
                BigInteger.ZERO.equals(m_balance) &&
                BigInteger.ZERO.equals(m_nonce);
    }

    public String toString() {
        String ret = "  Nonce: " + getNonce().toString() + "\n" +
                "  Balance: " + getBalance() + "\n" +
                "  State Root: " + Hex.toHexString(getStateRoot()) + "\n" +
                "  Code Hash: " + Hex.toHexString(getCodeHash());
        return ret;
    }

    public AccountState withBalanceIncrement(BigInteger value) {
        return new AccountState(m_nonce, m_balance.add(value), m_stateRoot, m_codeHash);
    }

    public AccountState withCodeHash(byte[] codeHash) {
        return new AccountState(m_nonce, m_balance, m_stateRoot, codeHash);
    }

    public AccountState withIncrementedNonce() {
        return new AccountState(m_nonce.add(BigInteger.ONE), m_balance, m_stateRoot, m_codeHash);
    }

    public AccountState withNonce(BigInteger nonce) {
        return new AccountState(nonce, m_balance, m_stateRoot, m_codeHash);
    }

    public AccountState withStateRoot(byte[] stateRoot) {
        return new AccountState(m_nonce, m_balance, stateRoot, m_codeHash);
    }
}
