package sonchain.blockchain.vm.program.invoke;

import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.core.Block;
import sonchain.blockchain.core.BlockTimestamp;
import sonchain.blockchain.core.Repository;
import sonchain.blockchain.core.Transaction;
import sonchain.blockchain.db.BlockStore;
import sonchain.blockchain.util.ByteUtil;

import static org.apache.commons.lang3.ArrayUtils.nullToEmpty;

import org.apache.log4j.Logger;

public class ProgramInvokeFactoryImpl implements ProgramInvokeFactory {

	public static final Logger m_logger = Logger.getLogger(ProgramInvokeFactoryImpl.class);

    // Invocation by the wire tx
    @Override
    public ProgramInvoke createProgramInvoke(Transaction tx, Block block, Repository repository,
                                             BlockStore blockStore) {

        /***         ADDRESS op       ***/
        // YP: Get address of currently executing account.
        byte[] address = tx.isContractCreation() ? tx.getContractAddress() : tx.getReceiveAddress();

        /***         ORIGIN op       ***/
        // YP: This is the sender of original transaction; it is never a contract.
        byte[] origin = tx.getSenderAddress();

        /***         CALLER op       ***/
        // YP: This is the address of the account that is directly responsible for this execution.
        byte[] caller = tx.getSenderAddress();

        /***         BALANCE op       ***/
        byte[] balance = repository.getBalance(address).toByteArray();

        /***        CALLVALUE op      ***/
        byte[] callValue = nullToEmpty(tx.getValue());

        /***     CALLDATALOAD  op   ***/
        /***     CALLDATACOPY  op   ***/
        /***     CALLDATASIZE  op   ***/
        //TODO
        //byte[] data = tx.isContractCreation() ? ByteUtil.EMPTY_BYTE_ARRAY : nullToEmpty(tx.getData());
        byte[] data = tx.isContractCreation() ? ByteUtil.EMPTY_BYTE_ARRAY : nullToEmpty(tx.getEncoded());

        /***    PREVHASH  op  ***/
        String lastHash = block.getParentHash();

        /***   Producer  op ***/
        String producer = block.getProducer();

        /*** TIMESTAMP  op  ***/
        BlockTimestamp timestamp = block.getTimestamp();

        /*** NUMBER  op  ***/
        long number = block.getBlockNumber();

        if (m_logger.isInfoEnabled()) {
        	m_logger.info(String.format("Top level call: \n" +
                            "address={%s}\n" +
                            "origin={%s}\n" +
                            "caller={%s}\n" +
                            "balance={%d}\n" +
                            "callValue={%d}\n" +
                            "data={%s}\n" +
                            "lastHash={%s}\n" +
                            "minedby={%s}\n" +
                            "timestamp={%d}\n" +
                            "blockNumber={%d}\n",

                    Hex.toHexString(address),
                    Hex.toHexString(origin),
                    Hex.toHexString(caller),
                    ByteUtil.bytesToBigInteger(balance),
                    ByteUtil.bytesToBigInteger(callValue),
                    Hex.toHexString(data),
                    lastHash,
                    producer,
                    timestamp,
                    number));
        }

        return new ProgramInvokeImpl(address, origin, caller, balance, callValue, data,
                lastHash, producer, timestamp, number, repository, blockStore);
    }

//    /**
//     * This invocation created for contract call contract
//     */
//    @Override
//    public ProgramInvoke createProgramInvoke(Program program, DataWord toAddress, DataWord callerAddress,
//                                             DataWord inValue, DataWord inGas,
//                                             BigInteger balanceInt, byte[] dataIn,
//                                             Repository repository, BlockStore blockStore, boolean byTestingSuite) {
//
//        DataWord address = toAddress;
//        DataWord origin = program.getOriginAddress();
//        DataWord caller = callerAddress;
//
//        DataWord balance = new DataWord(balanceInt.toByteArray());
//        DataWord gasPrice = program.getGasPrice();
//        DataWord gas = inGas;
//        DataWord callValue = inValue;
//
//        byte[] data = dataIn;
//        DataWord lastHash = program.getPrevHash();
//        DataWord coinbase = program.getCoinbase();
//        DataWord timestamp = program.getTimestamp();
//        DataWord number = program.getNumber();
//        DataWord difficulty = program.getDifficulty();
//        DataWord gasLimit = program.getGasLimit();
//
//        if (logger.isInfoEnabled()) {
//            logger.info("Internal call: \n" +
//                            "address={}\n" +
//                            "origin={}\n" +
//                            "caller={}\n" +
//                            "balance={}\n" +
//                            "gasPrice={}\n" +
//                            "gas={}\n" +
//                            "callValue={}\n" +
//                            "data={}\n" +
//                            "lastHash={}\n" +
//                            "coinbase={}\n" +
//                            "timestamp={}\n" +
//                            "blockNumber={}\n" +
//                            "difficulty={}\n" +
//                            "gaslimit={}\n",
//                    Hex.toHexString(address.getLast20Bytes()),
//                    Hex.toHexString(origin.getLast20Bytes()),
//                    Hex.toHexString(caller.getLast20Bytes()),
//                    balance.toString(),
//                    gasPrice.longValue(),
//                    gas.longValue(),
//                    Hex.toHexString(callValue.getNoLeadZeroesData()),
//                    data == null ? "" : Hex.toHexString(data),
//                    Hex.toHexString(lastHash.getData()),
//                    Hex.toHexString(coinbase.getLast20Bytes()),
//                    timestamp.longValue(),
//                    number.longValue(),
//                    Hex.toHexString(difficulty.getNoLeadZeroesData()),
//                    gasLimit.bigIntValue());
//        }
//
//        return new ProgramInvokeImpl(address, origin, caller, balance, gasPrice, gas, callValue,
//                data, lastHash, coinbase, timestamp, number, difficulty, gasLimit,
//                repository, program.getCallDeep() + 1, blockStore, byTestingSuite);
//    }
}
