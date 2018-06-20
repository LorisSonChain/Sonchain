//GAIA_USERMANAGERSERVER_JAVA_S1:ZHOULIN100%
/********************************************************************************************\
*                                                                                           *
* FileAppenderConfig.java -     文件输出型日志的配置文件           *
*                                                                                           *
*               Version 1.00  ★                                                            *
*                                                                                           *
*               Copyright (c) 2016-2017, Gaia Financial technology. All rights reserved.    *
*               Created by Joel 2017/06/12.                                                  *
*                                                                                           *
*********************************************************************************************/
package sonchain.blockchain.log;

/**
 * 文件输出型日志的配置文件
 * 
 * @author JOEL
 *
 */
public class FileAppenderConfig extends BaseAppenderConfig
{

	/**
	 * 文件输出型日志的配置文件
	 */
	public FileAppenderConfig()
	{

	}

	/**
	 * 日志文件的名字
	 */
	public String m_logFile = "";

	/**
	 * 时间格式
	 */
	public String m_datePattern = "yyyy-MM-dd HH:mm:ss";

	@Override
	public boolean LoadConfig(String configPath)
	{
		return true;
	}
}
