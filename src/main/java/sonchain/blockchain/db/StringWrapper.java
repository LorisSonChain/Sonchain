package sonchain.blockchain.db;

import java.io.Serializable;
import java.util.Arrays;

import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.util.FastByteComparisons;

public class StringWrapper implements Comparable<StringWrapper>, Serializable {

    private String m_data = "";
    private int m_hashCode = 0;

    public StringWrapper(String data) {
        if (data == null)
            throw new NullPointerException("Data must not be null");
        m_data = data;
        m_hashCode = data.hashCode();
    }

    public String getData() {
        return m_data;
    }

    public boolean equals(Object other) {
        if (!(other instanceof StringWrapper))
            return false;
        String otherData = ((StringWrapper) other).getData();
        return m_data.equals(otherData);
    }

    @Override
    public int compareTo(StringWrapper other) {
        return m_data.compareTo(other.getData());
    }

    @Override
    public int hashCode() {
        return m_hashCode;
    }

    @Override
    public String toString() {
        return m_data;
    }
}
    
