package sonchain.blockchain.vm.program.invoke;

import sonchain.blockchain.core.Block;
import sonchain.blockchain.core.Repository;
import sonchain.blockchain.core.Transaction;
import sonchain.blockchain.db.BlockStore;
import sonchain.blockchain.vm.DataWord;

public interface ProgramInvokeFactory {

    ProgramInvoke createProgramInvoke(Transaction tx, Block block,
                                      Repository repository, BlockStore blockStore);

//    ProgramInvoke createProgramInvoke(Program program, DataWord toAddress, DataWord callerAddress,
//                                             DataWord inValue, DataWord inGas,
//                                             BigInteger balanceInt, byte[] dataIn,
//                                             Repository repository, BlockStore blockStore, boolean byTestingSuite);
}
