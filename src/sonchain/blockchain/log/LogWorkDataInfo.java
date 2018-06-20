//GAIA_USERMANAGERSERVER_JAVA_S1:ZHOULIN100%
/********************************************************************************************\
*                                                                                           *
* LogWorkDataInfo.java -      日志工作数据信息               *
*                                                                                           *
*               Version 1.00  ★                                                            *
*                                                                                           *
*               Copyright (c) 2016-2017, Gaia Financial technology. All rights reserved.    *
*               Created by Joel 2017/06/12.                                                  *
*                                                                                           *
*********************************************************************************************/
package sonchain.blockchain.log;

/**
 * 日志工作数据信息
 * 
 * @author JOEL
 *
 */
public class LogWorkDataInfo
{
	/**
	 * 日志appender
	 */
	public BaseAppender m_appender;

	/**
	 * KEy
	 */
	public int m_id;

	/**
	 * 日志的内容
	 */
	public String m_logContent = "";

	/**
	 * 日志的类型
	 */
	public int m_logType = 0;

	/**
	 * 执行次数
	 */
	public int m_pos;

	/**
	 * 线程ID
	 */
	public int m_threadID;
}
