package sonchain.blockchain.facade;


public class SonChainFactory {

    public static SonChain createEthereum() {
        return new SonChainImpl(null);
    }
}
