package sonchain.blockchain.config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import sonchain.blockchain.consensus.SonChainProducerNode;

public class NodeFilter {
	private List<Entry> m_entries = new ArrayList<>();

    public void add(byte[] nodeId, String hostIpPattern) {
    	m_entries.add(new Entry(nodeId, hostIpPattern));
    }

    public boolean accept(SonChainProducerNode node) {
        for (Entry entry : m_entries) {
            if (entry.accept(node)) return true;
        }
        return false;
    }

    public boolean accept(InetAddress nodeAddr) {
        for (Entry entry : m_entries) {
            if (entry.accept(nodeAddr)) return true;
        }
        return false;
    }

    private class Entry {
        private String m_hostIpPattern = "";
        private byte[] m_nodeId = null;
        
        public Entry(byte[] nodeId, String hostIpPattern) {
        	m_nodeId = nodeId;
            if (hostIpPattern != null) {
                int idx = hostIpPattern.indexOf("*");
                if (idx > 0) {
                    hostIpPattern = hostIpPattern.substring(0, idx);
                }
            }
            m_hostIpPattern = hostIpPattern;
        }

        public boolean accept(InetAddress nodeAddr) {
            String ip = nodeAddr.getHostAddress();
            return m_hostIpPattern != null && ip.startsWith(m_hostIpPattern);
        }

        public boolean accept(SonChainProducerNode node) {
            try {
                return (m_nodeId == null || Arrays.equals(node.getId(), m_nodeId))
                        && (m_hostIpPattern == null || accept(InetAddress.getByName(node.getHost())));
            } catch (UnknownHostException e) {
                return false;
            }
        }
    }
}
