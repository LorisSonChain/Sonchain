package sonchain.blockchain.vm;

/**
 */
public class MessageCall {

	private final MsgType m_type;
	private final DataWord m_gas;
	private final DataWord m_codeAddress;
	private final DataWord m_endowment;
	private final DataWord m_inDataOffs;
	private final DataWord m_inDataSize;
	private DataWord m_outDataOffs;
	private DataWord m_outDataSize;

	public MessageCall(MsgType type, DataWord gas, DataWord codeAddress, DataWord endowment, DataWord inDataOffs,
			DataWord inDataSize) {
		m_type = type;
		m_gas = gas;
		m_codeAddress = codeAddress;
		m_endowment = endowment;
		m_inDataOffs = inDataOffs;
		m_inDataSize = inDataSize;
	}

	public MessageCall(MsgType type, DataWord gas, DataWord codeAddress, DataWord endowment, DataWord inDataOffs,
			DataWord inDataSize, DataWord outDataOffs, DataWord outDataSize) {
		this(type, gas, codeAddress, endowment, inDataOffs, inDataSize);
		m_outDataOffs = outDataOffs;
		m_outDataSize = outDataSize;
	}

	public MsgType getType() {
		return m_type;
	}

	public DataWord getGas() {
		return m_gas;
	}

	public DataWord getCodeAddress() {
		return m_codeAddress;
	}

	public DataWord getEndowment() {
		return m_endowment;
	}

	public DataWord getInDataOffs() {
		return m_inDataOffs;
	}

	public DataWord getInDataSize() {
		return m_inDataSize;
	}

	public DataWord getOutDataOffs() {
		return m_outDataOffs;
	}

	public DataWord getOutDataSize() {
		return m_outDataSize;
	}
}
