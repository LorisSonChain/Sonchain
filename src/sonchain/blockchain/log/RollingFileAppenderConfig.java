//GAIA_USERMANAGERSERVER_JAVA_S1:ZHOULIN100%
/********************************************************************************************\
*                                                                                           *
* RollingFileAppenderConfig.java -        滚动文件输出型日志的配置文件        *
*                                                                                           *
*               Version 1.00  ★                                                            *
*                                                                                           *
*               Copyright (c) 2016-2017, Gaia Financial technology. All rights reserved.    *
*               Created by Joel 2017/06/12.                                                  *
*                                                                                           *
*********************************************************************************************/
package sonchain.blockchain.log;

/**
 * 滚动文件输出型日志的配置文件
 * 
 * @author JOEl
 *
 */
public class RollingFileAppenderConfig extends FileAppenderConfig
{
	/**
	 * 滚动文件输出型日志的配置文件
	 */
	public RollingFileAppenderConfig()
	{
		super();
	}

	/**
	 * 单个日志文件的大小,默认1MB
	 */
	public String m_maxFileSize = "2MB";

	/**
	 * log保留个数
	 */
	public int m_maxSizeRollBackups = 100;
}
