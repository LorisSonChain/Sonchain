package sonchain.blockchain.task;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

public class SimpleTimer extends Thread {
	private final static Logger logger = Logger.getLogger(SimpleTimer.class);
	
	protected final long m_period;
	protected volatile AtomicBoolean m_stopFlg = new AtomicBoolean(false);
	protected final String m_timerName;
	protected final ISimpleTimerTask m_timerTask;
	
	public SimpleTimer(String timerName, long period, ISimpleTimerTask iTimerTask) {
		m_timerName = timerName;
		m_period = period;
		m_timerTask = iTimerTask;
	}

	@Override
	public void run() {
		while(!m_stopFlg.get()) {
			int didSleepTime = 0;
			try {
				didSleepTime = m_timerTask.doJob();
			} catch (InterruptedException e) {
				logger.info("SimpleTimer(" + m_timerName + ") Interrupted");
				break;
			} catch(Throwable e) {
				logger.error(null, e);
			}
			
			try {
				if(didSleepTime < m_period) 
				{
					Thread.sleep(m_period);
				}
			} catch (InterruptedException e) {
				logger.info("SimpleTimer(" + m_timerName + ") Interrupted");
				break;
			}
		}
	}
	
	public void shutdown() {
		m_stopFlg.set(true);		
		try {
			this.interrupt();
		} catch(Throwable e) {
			logger.error(null, e);
		}
	}
} 
