package sonchain.blockchain.vm.program.invoke;

import sonchain.blockchain.core.Repository;
import sonchain.blockchain.db.BlockStore;
import sonchain.blockchain.vm.DataWord;

public interface ProgramInvoke {
	
	 	DataWord getOwnerAddress();

	    DataWord getBalance();

	    DataWord getOriginAddress();

	    DataWord getCallerAddress();

	    DataWord getCallValue();

	    DataWord getDataSize();

	    DataWord getDataValue(DataWord indexData);

	    byte[] getDataCopy(DataWord offsetData, DataWord lengthData);

	    DataWord getPrevHash();

	    DataWord getCoinbase();

	    DataWord getTimestamp();

	    DataWord getNumber();

	    boolean byTransaction();

	    boolean byTestingSuite();

	    int getCallDeep();

	    Repository getRepository();

	    BlockStore getBlockStore();
}
