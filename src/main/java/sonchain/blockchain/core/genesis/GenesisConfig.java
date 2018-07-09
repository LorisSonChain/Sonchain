package sonchain.blockchain.core.genesis;

import java.util.*;

/**
 *
 */
public class GenesisConfig {
	
    private List<GenesisHashValidator> m_headerValidators = new ArrayList<GenesisHashValidator>();

    public List<GenesisHashValidator> getHeaderValidators() {
		return m_headerValidators;
	}

	public void setHeaderValidators(List<GenesisHashValidator> m_headerValidators) {
		this.m_headerValidators = m_headerValidators;
	}

    public boolean isCustomConfig() {
        return false;
    }
}
