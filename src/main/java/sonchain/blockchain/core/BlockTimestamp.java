package sonchain.blockchain.core;

import org.apache.log4j.Logger;

import sonchain.blockchain.service.DataCenter;

public class BlockTimestamp {

	public static final Logger m_logger = Logger.getLogger(BlockTimestamp.class);

	private int m_intervalMs = DataCenter.m_config.m_blockIntervalMs;
	private long m_epochMs = DataCenter.m_config.m_blockTimeStampEpoch;
	private long m_slot = 0;

	public int getIntervalMs() {
		return m_intervalMs;
	}
	public void setIntervalMs(int intervalMs) {
		m_intervalMs = intervalMs;
	}

	public long getEpochMs() {
		return m_epochMs;
	}
	public void setEpochMs(int epochMs) {
		m_epochMs = epochMs;
	}

	public long getSlot() {
		return m_slot;
	}
	public void setSlot(long slot) {
		m_slot = slot;
	}

	public BlockTimestamp(){		
	}

	public BlockTimestamp(int intervalMs, long epochMs, long slot){
		m_intervalMs = intervalMs;
		m_epochMs = epochMs;
		m_slot = slot;
	}

	public BlockTimestamp(long slot){
		m_slot = slot;
	}

	public BlockTimestamp(int intervalMs, long epochMs){
		m_intervalMs = intervalMs;
		m_epochMs = epochMs;
	}

	public BlockTimestamp(TimePoint t){
		set_time_point(t);
	}

	public BlockTimestamp next(){
		if(Integer.MAX_VALUE - m_slot < 1){
			m_logger.debug("block timestamp overflow now slot is:" + m_slot);
		}
		//EOS_ASSERT( std::numeric_limits<uint32_t>::max() - slot >= 1, fc::overflow_exception, "block timestamp overflow" );
		BlockTimestamp result = new BlockTimestamp(m_intervalMs, m_epochMs, m_slot);
		result.setSlot(m_slot + 1);
		return result;
	}

	public static BlockTimestamp maximum(){	
		return new BlockTimestamp(Integer.MAX_VALUE);
	}

	public BlockTimestamp(TimePointSec t){	
		set_time_point(t);	
	}

	public void set_time_point(TimePoint t) {
		Microseconds micro_since_epoch = t.time_since_epoch();
		long msec_since_epoch  = (micro_since_epoch.getCount() / 1000);
		m_slot = (msec_since_epoch - m_epochMs) / m_intervalMs;
	}

	public void set_time_point(TimePointSec t) {
		long  sec_since_epoch = t.sec_since_epoch();
		m_slot = (sec_since_epoch * 1000 - m_epochMs) / m_intervalMs;
	}

	public TimePoint toTimePoint(){
		long msec = m_slot * m_intervalMs;
		msec += m_epochMs;
		return new TimePoint(Microseconds.milliseconds(msec));
	}

	public boolean equals(BlockTimestamp t){
		return m_slot == t.getSlot();
	}

	public boolean gr(BlockTimestamp t){
		return m_slot > t.getSlot();
	}

	public boolean ge(BlockTimestamp t){
		return m_slot >= t.getSlot();
	}

	public boolean lr(BlockTimestamp t){
		return m_slot < t.getSlot();
	}

	public boolean le(BlockTimestamp t){
		return m_slot <= t.getSlot();
	}
}
