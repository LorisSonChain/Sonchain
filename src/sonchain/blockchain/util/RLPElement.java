package sonchain.blockchain.util;

import java.io.Serializable;

public interface RLPElement extends Serializable {

    byte[] getRLPData();
}

