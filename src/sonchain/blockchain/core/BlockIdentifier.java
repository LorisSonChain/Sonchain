package sonchain.blockchain.core;

import java.math.BigInteger;

import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.util.RLP;
import sonchain.blockchain.util.RLPList;

public class BlockIdentifier {
	/**
     * Block hash
     */
    private byte[] m_hash;

    /**
     * Block number
     */
    private long m_number;

    public BlockIdentifier(RLPList rlp) {
    	m_hash = rlp.get(0).getRLPData();
    	m_number = ByteUtil.byteArrayToLong(rlp.get(1).getRLPData());
    }

    public BlockIdentifier(byte[] hash, long number) {
    	m_hash = hash;
    	m_number = number;
    }

    public byte[] getEncoded() {
        byte[] hash = RLP.encodeElement(m_hash);
        byte[] number = RLP.encodeBigInteger(BigInteger.valueOf(m_number));
        return RLP.encodeList(hash, number);
    }

    public byte[] getHash() {
        return m_hash;
    }

    public long getNumber() {
        return m_number;
    }

    @Override
    public String toString() {
        return "BlockIdentifier {" +
                "hash=" + Hex.toHexString(m_hash) +
                ", number=" + m_number +
                '}';
    }
}
