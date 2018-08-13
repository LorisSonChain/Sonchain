package sonchain.blockchain.consensus;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import sonchain.blockchain.service.DataCenter;

public class ConsensusServiceTaskExecutor implements Callable<Integer>{

	public static final Logger m_logger = Logger.getLogger(ConsensusServiceTaskExecutor.class);

	@Override
	public Integer call() {
    	m_logger.debug("Timer start.");
        try {
        	ConsensusService consensusService = DataCenter.getSonChainImpl().getConsensusService();
	        synchronized (consensusService.m_context)
	        {
	            if (consensusService.m_timerHeight != consensusService.m_context.m_blockNumber 
	            		|| consensusService.m_timerView != consensusService.m_context.m_viewNumber) {
		            m_logger.info(String.format("Timer height={%d} context height={%d} Timer view={%d} context view={%d} return",
		            		consensusService.m_timerHeight,
		            		consensusService.m_context.m_blockNumber, 
		            		consensusService.m_timerView, 
		            		consensusService.m_context.m_viewNumber));
	            	return 0;
	            }
	            m_logger.debug(String.format("Timer height={%d} view={%d} state={%d}", consensusService.m_timerHeight,
	            		consensusService.m_timerView, consensusService.m_context.m_state));
	            if ((consensusService.m_context.m_state & ConsensusContext.Primary) != 0
	            		&& (consensusService.m_context.m_state & ConsensusContext.RequestSent) == 0)
	            {
	                m_logger.info(String.format("Timer send perpare request height={%d} view={%d} ", 
	                		consensusService.m_timerHeight, consensusService.m_timerView));
	                consensusService.m_context.m_state |= ConsensusContext.RequestSent;
	                if ((consensusService.m_context.m_state & ConsensusContext.SignatureSent) == 0)
	                {
	            		Calendar now = Calendar.getInstance(Locale.CHINA);
	            		//TODO
	            		//consensusService.m_context.m_timestamp = Math.max(now.getTimeInMillis(), 
	            		//		consensusService.m_blockChain.getBlockByHash(consensusService.m_context.m_preHash).getTimestamp() + 1);
	            		consensusService.m_context.m_signatures[consensusService.m_context.m_myIndex]
	            				//= m_consensusService.m_context.makeHeader().sign(m_consensusService.m_context.m_keyPair);
        						= consensusService.m_context.makeHeader().getEncoded();
	                }
	                consensusService.signAndRelay(consensusService.m_context.makePrepareRequest());
        			int delayTime = DataCenter.getSonChainImpl().SecondsPerBlock << (consensusService.m_timerView + 1);
        			m_logger.debug(String.format("Primary call ConsensusServiceTaskExecutor timeout:{%d}", delayTime));
	                ConsensusService.m_statTimer.schedule(new ConsensusServiceTaskExecutor(), delayTime, TimeUnit.SECONDS);
	            }
	            else if (((consensusService.m_context.m_state & ConsensusContext.Primary) != 0
	            		&& (consensusService.m_context.m_state & ConsensusContext.RequestSent) != 0)
	            		|| (consensusService.m_context.m_state & ConsensusContext.Backup) != 0)
	            {
	            	consensusService.requestChangeView();
	            }
	        }
	    	m_logger.debug("Timer end.");
	        return 1;
        }
        catch(Exception ex){
        	m_logger.error(" Timer error:" + ex.getMessage()); 
        	return 0;
        }		
	}
}
