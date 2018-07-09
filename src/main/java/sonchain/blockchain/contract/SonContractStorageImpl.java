package sonchain.blockchain.contract;

import sonchain.blockchain.service.DataCenter;
import sonchain.blockchain.vm.DataWord;

public class SonContractStorageImpl implements SonContractStorage{
	  private byte[] m_contractAddr;

      public SonContractStorageImpl(byte[] contractAddr) {
    	  m_contractAddr = contractAddr;
      }

      @Override
      public byte[] getStorageSlot(long slot) {
          return getStorageSlot(new DataWord(slot).getData());
      }

      @Override
      public byte[] getStorageSlot(byte[] slot) {
          DataWord ret = DataCenter.getSonChainImpl().getBlockChain().getRepository()
        		  .getContractDetails(m_contractAddr).get(new DataWord(slot));
          return ret.getData();
      }
}
