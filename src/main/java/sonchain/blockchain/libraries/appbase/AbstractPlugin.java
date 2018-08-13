package sonchain.blockchain.libraries.appbase;

/**
 * 抽象插件类
 * @author GAIA
 *
 */
public abstract class AbstractPlugin {
	/**
	 * 获取插件的状态
	 * @return
	 */
	protected abstract PluginState getState();
	/**
	 * 获取插件名称
	 * @return
	 */
	protected abstract String getName();	
	/**
	 * 初始化
	 */
	protected abstract void initialize(VariablesMap options);	
	/**
	 * 设置程序启动参数
	 */
	protected abstract void setProgramOptions(OptionsDescription cli, OptionsDescription cfg);	
	/**
	 * 启动
	 */
	protected abstract void startup();	
	/**
	 * 关闭
	 */
	protected abstract void shutdown();
	
	protected abstract void pluginInitialize(VariablesMap options);
	protected abstract void pluginStartup();
	protected abstract void pluginShutdown();
}
