package sonchain.blockchain.util;

import java.util.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Pipeline(流水线)执行器
 * @author GAIA
 *
 * @param <In>
 * @param <Out>
 */
public class ExecutorPipeline <In, Out>{

    /**
     * 初始化
     * @param threads 线程数
     * @param queueSize 队列大小
     * @param preserveOrder 
     * @param processor 处理方法
     * @param exceptionHandler 异常处理句柄
     */
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

    /**
     * 异常处理句柄
     */
    private Functional.Consumer<Throwable> m_exceptionHandler = null;
    /**
     * 重入锁
     */
    private ReentrantLock m_lock = new ReentrantLock();
    private ExecutorPipeline <Out, ?> m_next = null;
    private long m_nextOutTaskNumber = 0;
    /**
     * 订单计数器（线程安全）
     */
    private AtomicLong m_orderCounter = new AtomicLong();
    /**
     * 订单字典
     */
    private Map<Long, Out> m_orderMap = new HashMap<>();
    /**
     * 管道序号
     */
    private static AtomicInteger m_pipeNumber = new AtomicInteger(1);
    /**
     * 处理器
     */
    private Functional.Function<In, Out> m_processor = null;
    /**
     * 是否保留顺序
     */
    private boolean m_preserveOrder = false;
    /**
     * 有限队列
     */
    private BlockingQueue<Runnable> m_queue = null;
    /**
     * 线程ID，自增
     */
    private AtomicInteger m_threadNumber = new AtomicInteger(1);
    /**
     * 线程池
     */
    private ThreadPoolExecutor m_threadPoolExecutor = null;
    /**
     * 线程池名称
     */
    private String m_threadPoolName = "";

    /**
     * 添加
     * @param threads
     * @param queueSize
     * @param consumer
     * @return
     */
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
     * 添加
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
     * 获取有限队列
     * @return
     */
    public BlockingQueue<Runnable> GetQueue() {
        return m_queue;
    }

    /**
     * 获取订单列表
     * @return
     */
    public Map<Long, Out> GetOrderMap() {
        return m_orderMap;
    }

    /**
     * 是否关闭
     * @return
     */
    public boolean IsShutdown() {
        return m_threadPoolExecutor.isShutdown();
    }

    /**
     * 推送
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
     * 全部推送
     * @param list
     */
    public void PushAll(final List<In> list) {
        for (In in : list) {
            Push(in);
        }
    }

    /**
     * 推送下一个
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
     * 设置线程池名称
     * @param threadPoolName
     * @return
     */
    public ExecutorPipeline<In, Out> SetThreadPoolName(String threadPoolName) {
    	m_threadPoolName = threadPoolName;
        return this;
    }

    /**
     * 关闭
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
     * 关闭所有的执行并等待所有流水线提交完成任务
     * @throws InterruptedException
     */
    public void Join() throws InterruptedException {
    	m_threadPoolExecutor.shutdown();
    	//该方法调用会被阻塞，直到所有任务执行完毕并且shutdown请求被调用，
    	//或者参数中定义的timeout时间到达或者当前线程被打断，这几种情况任意一个发生了就会导致该方法的执行。
    	m_threadPoolExecutor.awaitTermination(10, TimeUnit.MINUTES);
        if (m_next != null) {
        	m_next.Join();
        }
    }

    /**
     * 阻塞队列
     * @author GAIA
     *
     * @param <E>
     */
    private static class LimitedQueue<E> extends LinkedBlockingQueue<E> {
        public LimitedQueue(int maxSize) {
            super(maxSize);
        }

        /**
         *  将指定的元素插入到此队列的尾部（如果立即可行且不会超过该队列的容量） 
         *  将指定的元素插入此队列的尾部，如果该队列已满， 
         *  则在到达指定的等待时间之前等待可用的空间,该方法可中断 
         */
        @Override
        public boolean offer(E e) {
            try {
            	//将指定的元素插入此队列的尾部，如果该队列已满，则一直等到（阻塞）。 
                put(e);
                return true;
            } catch(InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }
}