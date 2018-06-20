
package sonchain.blockchain.core.genesis;

import java.util.HashMap;
import java.util.Map;

/**
 * The Define of GenesisJson
 * @author GAIA
 *
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

    /**
     * 获取账户列表
     * @return
     */
    public Map<String, AllocatedAccount> getAlloc() {
        return m_alloc;
    }

    /**
     * 设置账户列表
     * @param alloc
     */
    public void setAlloc(Map<String, AllocatedAccount> alloc) {
        m_alloc = alloc;
    }
    
    /**
     * 获取创世块配置信息
     * @return
     */
    public GenesisConfig getConfig() {
        return m_config;
    }

    /**
     * 设置 创世块配置信息
     * @param config
     */
    public void setConfig(GenesisConfig config) {
        m_config = config;
    }

    /**
     * 获取此区块相关的附加数据
     * @return
     */
    public String getExtraData() {
        return m_extraData;
    }

    /**
     * 设置此区块相关的附加数据
     * @param extraData
     */
    public void setExtraData(String extraData) {
        m_extraData = extraData;
    }

    /**
     * 获取矿工地址
     * @return
     */
    public String getMinedBy() {
        return m_minedBy;
    }

    /**
     * 设置矿工地址
     * @param minedBy
     */
    public void setMinedBy(String minedBy) {
        m_minedBy = minedBy;
    }
    
    /**
     * 获取区块散列值
     * @return
     */
    public String getMixHash() {
		return m_mixHash;
	}

    /**
     * 设置区块散列值
     * @param mixHash
     */
	public void setMixHash(String mixHash) {
		m_mixHash = mixHash;
	}
    /**
     * 获取父区块头的Hash值
     * @return
     */
    public String getParentHash() {
        return m_parentHash;
    }

    /**
     * 设置父区块头的Hash值
     * @param parentHash
     */
    public void setParentHash(String parentHash) {
        m_parentHash = parentHash;
    }

    /**
     * 获取此区块成立时的unix的时间戳
     * @return
     */
    public String getTimestamp() {
        return m_timestamp;
    }

    /**
     * 设置此区块成立时的unix的时间戳
     * @param timestamp
     */
    public void setTimestamp(String timestamp) {
        m_timestamp = timestamp;
    }
}
