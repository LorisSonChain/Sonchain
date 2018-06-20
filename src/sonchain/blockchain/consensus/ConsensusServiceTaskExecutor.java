package sonchain.blockchain.consensus;

import java.util.Calendar;
import java.util.Locale;

import org.apache.log4j.Logger;

import sonchain.blockchain.service.DataCenter;

public class ConsensusServiceTaskExecutor implements Runnable{

	public static final Logger m_logger = Logger.getLogger(ConsensusServiceTaskExecutor.class);
	private ConsensusService m_consensusService;

	public ConsensusServiceTaskExecutor(ConsensusService consensusService){
		m_consensusService = consensusService;
	}

	@Override
	public void run() {
    	m_logger.debug("Timer start.");
        synchronized (m_consensusService.m_context)
        {
            if (m_consensusService.m_timerHeight != m_consensusService.m_context.m_blockNumber 
            		|| m_consensusService.m_timerView != m_consensusService.m_context.m_viewNumber) {
            	return;
            }
            m_logger.debug(String.format("Timer height={%d} view={%d} state={%d}", m_consensusService.m_timerHeight,
            		m_consensusService.m_timerView, m_consensusService.m_context.m_state));
            if ((m_consensusService.m_context.m_state & ConsensusContext.Primary) != 0
            		&& (m_consensusService.m_context.m_state & ConsensusContext.RequestSent) == 0)
            {
                m_logger.debug(String.format("Timer send perpare request height={%d} view={%d} ", 
                		m_consensusService.m_timerHeight, m_consensusService.m_timerView));
                m_consensusService.m_context.m_state |= ConsensusContext.RequestSent;
                if ((m_consensusService.m_context.m_state & ConsensusContext.SignatureSent) == 0)
                {
            		Calendar now = Calendar.getInstance(Locale.CHINA);
            		m_consensusService.m_context.m_timestamp = Math.max(now.getTimeInMillis(), 
            				m_consensusService.m_blockChain.getBlockByHash(m_consensusService.m_context.m_preHash).getTimestamp() + 1);
            		//m_consensusService.m_context.m_signatures[m_consensusService.m_context.m_myIndex] = m_consensusService.m_context.makeHeader().sign(m_context.m_keyPair);
                }
                m_consensusService.signAndRelay(m_consensusService.m_context.makePrepareRequest());
                //TODO
                ///timer.Change(TimeSpan.FromSeconds(DataCenter.getSonChainImpl().SecondsPerBlock << (m_consensusService.m_timerView + 1)),
                //		Timeout.InfiniteTimeSpan);
            }
            else if (((m_consensusService.m_context.m_state & ConsensusContext.Primary) != 0
            		&& (m_consensusService.m_context.m_state & ConsensusContext.RequestSent) != 0)
            		|| (m_consensusService.m_context.m_state & ConsensusContext.Backup) != 0)
            {
            	m_consensusService.requestChangeView();
            }
        }
    	m_logger.debug("Timer end.");
		
	}
}
