package sonchain.blockchain.vm;

public abstract class PrecompiledContract {
    public abstract long getGasForData(byte[] data);
    public abstract byte[] execute(byte[] data);
}
