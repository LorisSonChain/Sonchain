package sonchain.blockchain.vm;

public class CallCreate {

    private byte[] m_data;
    private byte[] m_destination;
    private byte[] m_gasLimit;
    private byte[] m_value;

    public CallCreate(byte[] data, byte[] destination, byte[] gasLimit, byte[] value) {
    	m_data = data;
    	m_destination = destination;
    	m_gasLimit = gasLimit;
    	m_value = value;
    }

    public byte[] getData() {
        return m_data;
    }

    public byte[] getDestination() {
        return m_destination;
    }

    public byte[] getGasLimit() {
        return m_gasLimit;
    }

    public byte[] getValue() {
        return m_value;
    }
}
