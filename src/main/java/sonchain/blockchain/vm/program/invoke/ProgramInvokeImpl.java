package sonchain.blockchain.vm.program.invoke;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;

import sonchain.blockchain.core.BlockTimestamp;
import sonchain.blockchain.core.Repository;
import sonchain.blockchain.db.BlockStore;
import sonchain.blockchain.vm.DataWord;

public class ProgramInvokeImpl implements ProgramInvoke {

    private BlockStore m_blockStore;
    /**
     * TRANSACTION  env **
     */
    private final DataWord m_address;
    private final DataWord m_origin, m_caller, m_balance, m_callValue;

    private byte[] m_msgData;

    /**
     * BLOCK  env **
     */
    private final DataWord m_prevHash, m_coinbase, m_number;
    
    private final BlockTimestamp m_timestamp;

    private Map<DataWord, DataWord> m_storage;

    private final Repository m_repository;
    private boolean m_byTransaction = true;
    private boolean m_byTestingSuite = false;
    private int m_callDeep = 0;


    public ProgramInvokeImpl(DataWord address, DataWord origin, DataWord caller, DataWord balance,
                             DataWord callValue, byte[] msgData, DataWord lastHash, DataWord coinbase, 
                             BlockTimestamp timestamp, DataWord number, Repository repository, int callDeep, 
                             BlockStore blockStore, boolean byTestingSuite) {

        // Transaction env
    	m_address = address;
    	m_origin = origin;
    	m_caller = caller;
    	m_balance = balance;
    	m_callValue = callValue;
    	m_msgData = msgData;

        // last Block env
    	m_prevHash = lastHash;
    	m_coinbase = coinbase;
    	m_timestamp = timestamp;
    	m_number = number;

    	m_repository = repository;
    	m_byTransaction = false;
    	m_callDeep = callDeep;
    	m_blockStore = blockStore;
    	m_byTestingSuite = byTestingSuite;
    }

    public ProgramInvokeImpl(byte[] address, byte[] origin, byte[] caller, byte[] balance,
                             byte[] callValue, byte[] msgData,
                             String lastHash, String coinbase, BlockTimestamp timestamp, long number,
                             Repository repository, BlockStore blockStore, boolean byTestingSuite) {
        this(address, origin, caller, balance, callValue, msgData, lastHash, coinbase,
                timestamp, number, repository, blockStore);
        m_byTestingSuite = byTestingSuite;
    }


    public ProgramInvokeImpl(byte[] address, byte[] origin, byte[] caller, byte[] balance,
                             byte[] callValue, byte[] msgData,
                             String lastHash, String coinbase, BlockTimestamp timestamp, long number,
                             Repository repository, BlockStore blockStore) {

        // Transaction env
    	m_address = new DataWord(address);
    	m_origin = new DataWord(origin);
    	m_caller = new DataWord(caller);
    	m_balance = new DataWord(balance);
    	m_callValue = new DataWord(callValue);
    	m_msgData = msgData;

        // last Block env
    	m_prevHash = new DataWord(lastHash);
    	m_coinbase = new DataWord(coinbase);
    	m_timestamp = timestamp;
    	m_number = new DataWord(number);
        
    	m_repository = repository;
    	m_blockStore = blockStore;
    }

    /*           ADDRESS op         */
    public DataWord getOwnerAddress() {
        return m_address;
    }

    /*           BALANCE op         */
    public DataWord getBalance() {
        return m_balance;
    }

    /*           ORIGIN op         */
    public DataWord getOriginAddress() {
        return m_origin;
    }

    /*           CALLER op         */
    public DataWord getCallerAddress() {
        return m_caller;
    }

    /*          CALLVALUE op    */
    public DataWord getCallValue() {
        return m_callValue;
    }

    /*****************/
    /***  msg data ***/
    /*****************/
    /* NOTE: In the protocol there is no restriction on the maximum message data,
     * However msgData here is a byte[] and this can't hold more than 2^32-1
     */
    private static BigInteger MAX_MSG_DATA = BigInteger.valueOf(Integer.MAX_VALUE);

    /*     CALLDATALOAD  op   */
    public DataWord getDataValue(DataWord indexData) {

        BigInteger tempIndex = indexData.value();
        int index = tempIndex.intValue(); // possible overflow is caught below
        int size = 32; // maximum datavalue size

        if (m_msgData == null || index >= m_msgData.length
                || tempIndex.compareTo(MAX_MSG_DATA) == 1)
            return new DataWord();
        if (index + size > m_msgData.length)
            size = m_msgData.length - index;

        byte[] data = new byte[32];
        System.arraycopy(m_msgData, index, data, 0, size);
        return new DataWord(data);
    }

    /*  CALLDATASIZE */
    public DataWord getDataSize() {

        if (m_msgData == null || m_msgData.length == 0) {
        	return DataWord.ZERO;
        }
        int size = m_msgData.length;
        return new DataWord(size);
    }

    /*  CALLDATACOPY */
    public byte[] getDataCopy(DataWord offsetData, DataWord lengthData) {

        int offset = offsetData.intValueSafe();
        int length = lengthData.intValueSafe();
        byte[] data = new byte[length];
        if (m_msgData == null) {
        	return data;
        }
        if (offset > m_msgData.length) {
        	return data;
        }
        if (offset + length > m_msgData.length) {
        	length = m_msgData.length - offset;
        }

        System.arraycopy(m_msgData, offset, data, 0, length);

        return data;
    }


    /*     PREVHASH op    */
    public DataWord getPrevHash() {
        return m_prevHash;
    }

    /*     COINBASE op    */
    public DataWord getCoinbase() {
        return m_coinbase;
    }

    /*     TIMESTAMP op    */
    public DataWord getTimestamp() {
        return null;
    }

    /*     NUMBER op    */
    public DataWord getNumber() {
        return m_number;
    }
    /*  Storage */
    public Map<DataWord, DataWord> getStorage() {
        return m_storage;
    }

    public Repository getRepository() {
        return m_repository;
    }

    @Override
    public BlockStore getBlockStore() {
        return m_blockStore;
    }

    @Override
    public boolean byTransaction() {
        return m_byTransaction;
    }

    @Override
    public boolean byTestingSuite() {
        return m_byTestingSuite;
    }

    @Override
    public int getCallDeep() {
        return m_callDeep;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) {
        	return false;
        }
        ProgramInvokeImpl that = (ProgramInvokeImpl) o;
        if (m_byTestingSuite != that.m_byTestingSuite) {
        	return false;
        }
        if (m_byTransaction != that.m_byTransaction) {
        	return false;
        }
        if (m_address != null ? !m_address.equals(that.m_address) : that.m_address != null) {
        	return false;
        }
        if (m_balance != null ? !m_balance.equals(that.m_balance) : that.m_balance != null){
        	return false;
        }
        if (m_callValue != null ? !m_callValue.equals(that.m_callValue) : that.m_callValue != null) {
        	return false;
        }
        if (m_caller != null ? !m_caller.equals(that.m_caller) : that.m_caller != null){
        	return false;
        }
        if (m_coinbase != null ? !m_coinbase.equals(that.m_coinbase) : that.m_coinbase != null) {
        	return false;
        }
        if (!Arrays.equals(m_msgData, that.m_msgData)) {
        	return false;
        }
        if (m_number != null ? !m_number.equals(that.m_number) : that.m_number != null){
        	return false;
        }
        if (m_origin != null ? !m_origin.equals(that.m_origin) : that.m_origin != null) {
        	return false;
        }
        if (m_prevHash != null ? !m_prevHash.equals(that.m_prevHash) : that.m_prevHash != null) {
        	return false;
        }
        if (m_repository != null ? !m_repository.equals(that.m_repository) : that.m_repository != null) {
        	return false;
        }
        if (m_storage != null ? !m_storage.equals(that.m_storage) : that.m_storage != null) {
        	return false;
        }
        if (m_timestamp != null ? !m_timestamp.equals(that.m_timestamp) : that.m_timestamp != null) {
        	return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "ProgramInvokeImpl{" +
                "address=" + m_address +
                ", origin=" + m_origin +
                ", caller=" + m_caller +
                ", balance=" + m_balance +
                ", callValue=" + m_callValue +
                ", msgData=" + Arrays.toString(m_msgData) +
                ", prevHash=" + m_prevHash +
                ", coinbase=" + m_coinbase +
                ", timestamp=" + m_timestamp +
                ", number=" + m_number +
                ", storage=" + m_storage +
                ", repository=" + m_repository +
                ", byTransaction=" + m_byTransaction +
                ", byTestingSuite=" + m_byTestingSuite +
                ", callDeep=" + m_callDeep +
                '}';
    }
}