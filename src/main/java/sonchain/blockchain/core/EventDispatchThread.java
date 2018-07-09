package sonchain.blockchain.core;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

public class EventDispatchThread {
	public static final Logger m_logger = Logger.getLogger(EventDispatchThread.class);
	
    private static EventDispatchThread m_eventDispatchThread = null;

    private static final int[] queueSizeWarnLevels = new int[]{0, 10_000, 50_000, 100_000, 
    		250_000, 500_000, 1_000_000, 10_000_000};

    private final BlockingQueue<Runnable> m_executorQueue = new LinkedBlockingQueue<Runnable>();
    /**
     * ThreadPoolExecutor(int corePoolSize,
                        int maximumPoolSize,
                        long keepAliveTime,
                        TimeUnit unit,
                        BlockingQueue<Runnable> workQueue,
                        ThreadFactory threadFactory)
     */
    private final ExecutorService m_executor = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS, m_executorQueue, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
        	m_logger.debug("newThread start");
            return new Thread(r, "EDT");
        }
    });

    private int m_counter = 0;
    private int m_lastQueueSizeWarnLevel = 0;
    private Runnable m_lastTask = null;
    private long m_taskStart = 0;

    /**
     * Returns the default instance for initialization of Autowired instances
     * to be used in tests
     */
    public static EventDispatchThread getDefault() {
        if (m_eventDispatchThread == null) {
        	m_eventDispatchThread = new EventDispatchThread() {
                @Override
                public void invokeLater(Runnable r) {
                    r.run();
                }
            };
        }
        return m_eventDispatchThread;
    }

    public void invokeLater(final Runnable r) {
    	m_logger.debug("invokeLater start");
        if (m_executor.isShutdown()) {
        	return;
        }
        if (m_counter++ % 1000 == 0) {
        	logStatus();
        }
        m_executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                	m_logger.debug("EDT task start");
                	m_lastTask = r;
                    m_taskStart = System.nanoTime();
                    r.run();
                    long t = (System.nanoTime() - m_taskStart) / 1_000_000;
                    m_taskStart = 0;
                    if (t > 1000) {
                        m_logger.warn("EDT task executed in more than 1 sec: " + t + "ms, " +
                        "Executor queue size: " + m_executorQueue.size());

                    }
                } catch (Exception e) {
                	m_logger.error("EDT task exception", e);
                }
                finally
                {
                	m_logger.debug("EDT task end");
                }
            }
        });
    }

    // monitors EDT queue size and prints warning if exceeds thresholds
    private void logStatus() {
    	m_logger.debug("logStatus start");
    	try
    	{
	        int curLevel = getSizeWarnLevel(m_executorQueue.size());
	        if (m_lastQueueSizeWarnLevel == curLevel) {
	        	return;
	        }
	        synchronized (this) {
	            if (curLevel > m_lastQueueSizeWarnLevel) {
	                long t = m_taskStart == 0 ? 0 : (System.nanoTime() - m_taskStart) / 1_000_000;
	                String msg = "EDT size grown up to " + m_executorQueue.size() 
	                	+ " (last task executing for " + t + " ms: " + m_lastTask;
	                if (curLevel < 3) {
	                    m_logger.info(msg);
	                } else {
	                    m_logger.warn(msg);
	                }
	            } else if (curLevel < m_lastQueueSizeWarnLevel) {
	            	m_logger.info("EDT size shrunk down to " + m_executorQueue.size());
	            }
	            m_lastQueueSizeWarnLevel = curLevel;
	        }
    	}finally
    	{
        	m_logger.debug("logStatus end");
    	}
    }

    private static int getSizeWarnLevel(int size) {
    	m_logger.debug("getSizeWarnLevel start");
        int idx = Arrays.binarySearch(queueSizeWarnLevels, size);
        return idx >= 0 ? idx : -(idx + 1) - 1;
    }

    public void shutdown() {
    	m_logger.debug("shutdown start");
    	m_executor.shutdownNow();
        try {
        	m_executor.awaitTermination(10L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        	m_logger.warn(String.format("shutdown: executor interrupted: {%s}", e.getMessage()));
        }
        finally
        {
        	m_logger.debug("shutdown end");
        }
    }
}
