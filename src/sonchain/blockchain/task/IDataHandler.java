package sonchain.blockchain.task;

public interface IDataHandler {
	public boolean doJob(Object data, String taskId, int taskGroupId, int taskRetryCount);
}
