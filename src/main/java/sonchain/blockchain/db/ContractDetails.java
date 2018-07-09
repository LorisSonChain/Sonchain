package sonchain.blockchain.db;

import java.util.*;

import javax.annotation.Nullable;

import sonchain.blockchain.vm.DataWord;

public interface ContractDetails {

    byte[] getAddress();
    void setAddress(byte[] address);
    ContractDetails clone();
    void decode(byte[] rlpCode);
    DataWord get(DataWord key);
    byte[] getCode();
    byte[] getCode(byte[] codeHash);
    void setCode(byte[] code);
    byte[] getEncoded();
    Map<DataWord,DataWord> getStorage(@Nullable Collection<DataWord> keys);
    Map<DataWord, DataWord> getStorage();
    void setStorage(List<DataWord> storageKeys, List<DataWord> storageValues);
    void setStorage(Map<DataWord, DataWord> storage);
    ContractDetails getSnapshotTo(byte[] hash);
    byte[] getStorageHash();
    Set<DataWord> getStorageKeys();
    int getStorageSize();
    boolean isDeleted();
    boolean isDirty();
    void put(DataWord key, DataWord value);
    void setDeleted(boolean deleted);
    void setDirty(boolean dirty);
    String toString();
    void syncStorage();
}
