//GAIA_USERMANAGERSERVER_JAVA_S1:ZHOULIN100%
/********************************************************************************************\
*                                                                                           *
* LogType.java -      日志级别选项,日志记录的类型               *
*                                                                                           *
*               Version 1.00  ★                                                            *
*                                                                                           *
*               Copyright (c) 2016-2017, Gaia Financial technology. All rights reserved.    *
*               Created by Joel 2017/06/12.                                                  *
*                                                                                           *
*********************************************************************************************/
package sonchain.blockchain.log;

/**
 * 日志级别选项,日志记录的类型
 * 
 * @author GAIA
 *
 */
public enum LogType
{

	/**
	 * 调试
	 */
	Debug(1),

	/**
	 * 错误
	 */
	Error(4),

	/**
	 * 致命错误
	 */
	Fatal(5),

	/**
	 * 信息
	 */
	Info(2),

	/**
	 * 警告
	 */
	Warnning(3);
	LogType(int typea)
	{
		type = typea;
	}

	public int type;
}
