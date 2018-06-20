//GAIA_USERMANAGERSERVER_JAVA_S1:ZHOULIN100%
/********************************************************************************************\
*                                                                                           *
* BaseAppender.java -      日志基类               *
*                                                                                           *
*               Version 1.00  ★                                                            *
*                                                                                           *
*               Copyright (c) 2016-2017, Gaia Financial technology. All rights reserved.    *
*               Created by Joel 2017/06/12.                                                  *
*                                                                                           *
*********************************************************************************************/
package sonchain.blockchain.log;

/**
 * 日志基类
 * 
 * @author Joel
 *
 */
public abstract class BaseAppender
{

	/**
	 * 日志基类- > 构造方法
	 */
	public BaseAppender()
	{

	}

	/**
	 * Appender的ID
	 */
	public int m_id = 0;

	/**
	 * Appender名字
	 */
	public String m_name = "";

	/**
	 * 输出调试日志
	 * 
	 * @param message
	 */
	public abstract void Debug(String message);

	/**
	 * 输出错误日志
	 */
	public abstract void Error(String message);

	/**
	 * 输出信息日志
	 * 
	 * @param message
	 */
	public abstract void Info(String message);

	/**
	 * 输出日志
	 * 
	 * @param logType
	 *            日志类型
	 * @param message
	 *            日志内容
	 */
	public abstract void Log(int logType, String message);

	/**
	 * 输出警告日志
	 * 
	 * @param message
	 */
	public abstract void Warn(String message);
}
