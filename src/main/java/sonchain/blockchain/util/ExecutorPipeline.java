package sonchain.blockchain.util;

import java.util.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class ExecutorPipeline <In, Out>{

    public ExecutorPipeline(int threads, int queueSize, boolean preserveOrder, 
    		Functional.Function<In, Out> processor, Functional.Consumer<Throwable> exceptionHandler) {
    	m_queue = new LimitedQueue<>(queueSize);
    	m_threadPoolExecutor = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS, m_queue, 
    			new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, m_threadPoolName + "-" + m_threadNumber.getAndIncrement());
            }
        });
    	m_preserveOrder = preserveOrder;
    	m_processor = processor;
    	m_exceptionHandler = exceptionHandler;
    	m_threadPoolName = "pipe-" + m_pipeNumber.getAndIncrement();
    }

    private Functional.Consumer<Throwable> m_exceptionHandler = null;
    private ReentrantLock m_lock = new ReentrantLock();
    private ExecutorPipeline <Out, ?> m_next = null;
    private long m_nextOutTaskNumber = 0;
    private AtomicLong m_orderCounter = new AtomicLong();
    private Map<Long, Out> m_orderMap = new HashMap<>();
    private static AtomicInteger m_pipeNumber = new AtomicInteger(1);
    private Functional.Function<In, Out> m_processor = null;
    private boolean m_preserveOrder = false;
    private BlockingQueue<Runnable> m_queue = null;
    private AtomicInteger m_threadNumber = new AtomicInteger(1);
    private ThreadPoolExecutor m_threadPoolExecutor = null;
    private String m_threadPoolName = "";
    
    public ExecutorPipeline<Out, Void> Add(int threads, int queueSize, final Functional.Consumer<Out> consumer) {
        return Add(threads, queueSize, false, new Functional.Function<Out, Void>() {
            @Override
            public Void apply(Out out) {
                consumer.accept(out);
                return null;
            }
        });
    }

    /**
     * Add
     * @param threads
     * @param queueSize
     * @param preserveOrder
     * @param processor
     * @return
     */
    public <NextOut> ExecutorPipeline<Out, NextOut> Add(
    		int threads, int queueSize, boolean preserveOrder, Functional.Function<Out, NextOut> processor) {
        ExecutorPipeline<Out, NextOut> ret = new ExecutorPipeline<>(threads, queueSize, preserveOrder, 
        		processor, m_exceptionHandler);
        m_next = ret;
        return ret;
    }

    /**
     * GetQueue
     * @return
     */
    public BlockingQueue<Runnable> GetQueue() {
        return m_queue;
    }

    /**
     * GetOrderMap
     * @return
     */
    public Map<Long, Out> GetOrderMap() {
        return m_orderMap;
    }

    /**
     * IsShutdown
     * @return
     */
    public boolean IsShutdown() {
        return m_threadPoolExecutor.isShutdown();
    }

    /**
     * Push
     * @param in
     */
    public void Push(final In in) {
        final long orderCounter = m_orderCounter.getAndIncrement();
        m_threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    PushNext(orderCounter, m_processor.apply(in));
                } catch (Throwable e) {
                	m_exceptionHandler.accept(e);
                }
            }
        });
    }

    /**
     * PushAll
     * @param list
     */
    public void PushAll(final List<In> list) {
        for (In in : list) {
            Push(in);
        }
    }

    /**
     * PushNext
     * @param orderCounter
     * @param res
     */
    private void PushNext(long orderCounter, Out res) {
        if (m_next != null) {
            if (!m_preserveOrder) {
            	m_next.Push(res);
            } else {
            	m_lock.lock();
                try {
                    if (orderCounter == m_nextOutTaskNumber) {
                    	m_next.Push(res);
                        while(true) {
                        	m_nextOutTaskNumber++;
                            Out out = m_orderMap.remove(m_nextOutTaskNumber);
                            if (out == null) 
                            	break;
                            m_next.Push(out);
                        }
                    } else {
                    	m_orderMap.put(orderCounter, res);
                    }
                } finally {
                	m_lock.unlock();
                }
            }
        }
    }

    /**
     * SetThreadPoolName
     * @param threadPoolName
     * @return
     */
    public ExecutorPipeline<In, Out> SetThreadPoolName(String threadPoolName) {
    	m_threadPoolName = threadPoolName;
        return this;
    }

    /**
     *Shutdown
     */
    public void Shutdown() {
        try {
        	m_threadPoolExecutor.shutdown();
        } catch (Exception e) {}
        if (m_next != null) {
        	m_threadPoolExecutor.shutdown();
        }
    }

    /**
     * Join
     * @throws InterruptedException
     */
    public void Join() throws InterruptedException {
    	m_threadPoolExecutor.shutdown();
    	m_threadPoolExecutor.awaitTermination(10, TimeUnit.MINUTES);
        if (m_next != null) {
        	m_next.Join();
        }
    }

    /**
     *
     * @param <E>
     */
    private static class LimitedQueue<E> extends LinkedBlockingQueue<E> {
        public LimitedQueue(int maxSize) {
            super(maxSize);
        }

        @Override
        public boolean offer(E e) {
            try { 
                put(e);
                return true;
            } catch(InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }
}