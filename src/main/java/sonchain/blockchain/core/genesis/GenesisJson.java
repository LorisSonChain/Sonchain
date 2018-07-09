
package sonchain.blockchain.core.genesis;

import java.util.HashMap;
import java.util.Map;

/**
 * The Define of GenesisJson
 *  *
 */
public class GenesisJson {

    private Map<String, AllocatedAccount> m_alloc = new HashMap<String, AllocatedAccount>();
    private GenesisConfig m_config = null;
    private String m_extraData = "";
    private String m_minedBy = "";
    private String m_mixHash = "";
    private String m_parentHash = "";
    private String m_timestamp = "";

    /**
     * Constructor
     */
    public GenesisJson() {
    }

    public Map<String, AllocatedAccount> getAlloc() {
        return m_alloc;
    }

    public void setAlloc(Map<String, AllocatedAccount> alloc) {
        m_alloc = alloc;
    }
    
    public GenesisConfig getConfig() {
        return m_config;
    }
    
    public void setConfig(GenesisConfig config) {
        m_config = config;
    }

    public String getExtraData() {
        return m_extraData;
    }

    public void setExtraData(String extraData) {
        m_extraData = extraData;
    }

    public String getMinedBy() {
        return m_minedBy;
    }

    public void setMinedBy(String minedBy) {
        m_minedBy = minedBy;
    }
    
    public String getMixHash() {
		return m_mixHash;
	}

	public void setMixHash(String mixHash) {
		m_mixHash = mixHash;
	}
	
    public String getParentHash() {
        return m_parentHash;
    }

    public void setParentHash(String parentHash) {
        m_parentHash = parentHash;
    }

    public String getTimestamp() {
        return m_timestamp;
    }

    public void setTimestamp(String timestamp) {
        m_timestamp = timestamp;
    }
}
