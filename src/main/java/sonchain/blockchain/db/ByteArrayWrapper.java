package sonchain.blockchain.db;

import java.io.Serializable;
import java.util.Arrays;

import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.util.FastByteComparisons;

public class ByteArrayWrapper implements Comparable<ByteArrayWrapper>, Serializable {

    private byte[] m_data = null;
    private int m_hashCode = 0;

    public ByteArrayWrapper(byte[] data) {
        if (data == null)
            throw new NullPointerException("Data must not be null");
        m_data = data;
        m_hashCode = Arrays.hashCode(data);
    }

    public byte[] getData() {
        return m_data;
    }

    public boolean equals(Object other) {
        if (!(other instanceof ByteArrayWrapper))
            return false;
        byte[] otherData = ((ByteArrayWrapper) other).getData();
        return FastByteComparisons.compareTo(
        		m_data, 0, m_data.length,
                otherData, 0, otherData.length) == 0;
    }

    @Override
    public int compareTo(ByteArrayWrapper o) {
        return FastByteComparisons.compareTo(
        		m_data, 0, m_data.length,
                o.getData(), 0, o.getData().length);
    }

    @Override
    public int hashCode() {
        return m_hashCode;
    }

    @Override
    public String toString() {
        return Hex.toHexString(m_data);
    }
}
    
