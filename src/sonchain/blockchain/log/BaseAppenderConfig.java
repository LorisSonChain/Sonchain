//GAIA_USERMANAGERSERVER_JAVA_S1:ZHOULIN100%
/********************************************************************************************\
*                                                                                           *
* BaseAppenderConfig.java -      配置Appender的类             *
*                                                                                           *
*               Version 1.00  ★                                                            *
*                                                                                           *
*               Copyright (c) 2016-2017, Gaia Financial technology. All rights reserved.    *
*               Created by Joel 2017/06/12.                                                  *
*                                                                                           *
*********************************************************************************************/
package sonchain.blockchain.log;

/**
 * 
 * 配置Appender的类
 * 
 * @author Joel
 */
public abstract class BaseAppenderConfig
{
	/**
	 * 日志输出模式
	 */
	public String m_logPatten = "";

	/**
	 * 1：调试模式 2：信息模式 3：警告模式 4：错误模式 5：致命错误
	 */
	public int m_logType = 0;

	/**
	 * 日志的名称
	 */
	public String m_name = "";

	/**
	 * 日志配置类型
	 */
	public String m_type = "";

	/**
	 * 格式化
	 */
	public BaseAppenderConfig()
	{
	}

	/**
	 * 加载配置文件
	 * 
	 * @param configPath
	 *            文件路径
	 * @return 是否加载成功
	 */
	public abstract boolean LoadConfig(String configPath);
}
