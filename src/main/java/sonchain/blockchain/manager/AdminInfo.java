package sonchain.blockchain.manager;

import java.util.LinkedList;
import java.util.List;

public class AdminInfo {
	
	private static final int ExecTimeListLimit = 10000;

    private List<Long> m_blockExecTime = new LinkedList<>();
    private boolean m_consensus = true;
    private long m_startupTimeStamp = 0;

    public void init() {
    	m_startupTimeStamp = System.currentTimeMillis();
    }

    public long getStartupTimeStamp() {
        return m_startupTimeStamp;
    }

    public boolean isConsensus() {
        return m_consensus;
    }

    public void lostConsensus() {
    	m_consensus = false;
    }

    public void addBlockExecTime(long time){
        while (m_blockExecTime.size() > ExecTimeListLimit) {
        	m_blockExecTime.remove(0);
        }
        m_blockExecTime.add(time);
    }

    public List<Long> getBlockExecTime(){
        return m_blockExecTime;
    }

    public Long getExecAvg(){
        if (m_blockExecTime.isEmpty()){
        	return 0L;
        }
        long sum = 0;
        for (int i = 0; i < m_blockExecTime.size(); ++i){
            sum += m_blockExecTime.get(i);
        }
        return sum / m_blockExecTime.size();
    }
}
