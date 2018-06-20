package sonchain.blockchain.core.genesis;

import java.util.*;

/**
 * 创世块配置
 * @author GAIA
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

	/**
     * 是否自定义配置
     * @return
     */
    public boolean isCustomConfig() {
        return false;
    }
}
