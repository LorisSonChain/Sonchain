package sonchain.blockchain.task;

public interface ITaskDataRetryPolicy {
	/**
	 * delay time decides the next time of retrying
	 * @param retryCount
	 * @return milliseconds for delay
	 */
	public long delayTime(int retryCount);
	
	/**
	 * max times of retrying
	 * @return
	 */
	public int maxRetry();
}
