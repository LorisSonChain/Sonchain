//GAIA_USERMANAGERSERVER_JAVA_S1:ZHOULIN100%
/********************************************************************************************\
*                                                                                           *
* FileAppender.java -      文件日志输出             *
*                                                                                           *
*               Version 1.00  ★                                                            *
*                                                                                           *
*               Copyright (c) 2016-2017, Gaia Financial technology. All rights reserved.    *
*               Created by Joel 2017/06/12.                                                  *
*                                                                                           *
*********************************************************************************************/
package sonchain.blockchain.log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import sonchain.blockchain.base.CFileA;

/**
 * 文件日志输出
 * 
 * @author JOEL
 *
 */
public class FileAppender extends BaseAppender
{
	public final static String FILESEPARATOR = System.getProperty("file.separator");
	
	/**
	 * 构造函数
	 */
	public FileAppender()
	{

	}

	/**
	 * 指定一个配置文件FileAppenderConfig
	 * 
	 * @param fileAppenderConfig
	 */
	public FileAppender(FileAppenderConfig fileAppenderConfig)
	{
		m_fileAppenderConfig = fileAppenderConfig;
		CheckDirectory(m_fileAppenderConfig.m_logFile);
	}

	/**
	 * 指定配置文件的位置
	 * 
	 * @param configPath
	 */
	public FileAppender(String configPath)
	{
		m_fileAppenderConfig = new FileAppenderConfig();
		m_fileAppenderConfig.LoadConfig(configPath);
		CheckDirectory(m_fileAppenderConfig.m_logFile);
	}

	// 配置文件类
	private FileAppenderConfig m_fileAppenderConfig;

	/**
	 * 向文件中追加内容
	 * 
	 * @param logFile
	 *            日志文件的名称
	 * @param content
	 *            内容
	 * @return 是否成功
	 */
	protected boolean Append(String logFile, String content)
	{
		try
		{
			if (logFile == null || logFile.length() == 0)
			{
				return false;
			}
			CFileA.Append(logFile, content);
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	/**
	 * 判断日志目录
	 * 
	 * @param fileFullPath
	 *            日志文件的名称,如果指定的地址是一个目录地址的话,如果不存在将不会创建目录
	 */
	protected void CheckDirectory(String fileFullPath)
	{
		if (fileFullPath.contains("."))
		{
			String dir = fileFullPath.substring(0, fileFullPath.lastIndexOf(FILESEPARATOR));
			if (!IsDirectoryExist(dir))
			{
				CreateDirectory(dir);
				File file = new File(fileFullPath);
				try
				{
					file.createNewFile();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
		else
		{
			// CreateDirectory(fileFullPath);
		}
	}

	/**
	 * 判断是否输出
	 * 
	 * @param logType
	 *            日志的级别
	 * @return
	 */
	protected boolean CheckOut(int logType)
	{
		if (m_fileAppenderConfig == null)
		{
			return true;
		}

		if (m_fileAppenderConfig.m_logType <= logType)
		{
			return true;
		}
		return false;
	}

	/**
	 * 创建文件夹 文件夹
	 * 
	 * @param dir
	 */
	protected void CreateDirectory(String dir)
	{
		File f = new File(dir);
		f.mkdirs();
	}

	/**
	 * 输出调试日志
	 */
	public void Debug(String message)
	{
		message = MessageFormat.format("{0} {1} {2}\r\n", GetNowDate(m_fileAppenderConfig.m_datePattern), "Debug",
				message);
		Append(m_fileAppenderConfig.m_logFile, message);
	}

	/**
	 * 输出错误日志
	 */
	public void Error(String message)
	{
		message = MessageFormat.format("{0} {1} {2}\r\n", GetNowDate(m_fileAppenderConfig.m_datePattern), "Error",
				message);
		Append(m_fileAppenderConfig.m_logFile, message);
	}

	/**
	 * 输出致命错误日志
	 */
	public void Fatal(String message)
	{
		message = MessageFormat.format("{0} {1}{2}", GetNowDate(m_fileAppenderConfig.m_datePattern), "Fatal", message);
		Append(m_fileAppenderConfig.m_logFile, message);
	}

	/**
	 * 获取当前的时间
	 * 
	 * @param datePattern
	 * @return
	 */
	protected String GetNowDate(String datePattern)
	{
		SimpleDateFormat sdf = new SimpleDateFormat(datePattern);
		return sdf.format(new Date()).toString();
	}

	/**
	 * 输出信息日志
	 */
	public void Info(String message)
	{
		message = MessageFormat.format("{0} {1} {2}\r\n", GetNowDate(m_fileAppenderConfig.m_datePattern), "Info",
				message);
		Append(m_fileAppenderConfig.m_logFile, message);
	}

	/**
	 * 判断文件夹是否存在
	 * 
	 * @param dir
	 * @return
	 */
	protected boolean IsDirectoryExist(String dir)
	{
		File file = new File(dir);
		boolean aa = file.exists();
		return aa;
	}

	/**
	 * 输出日志
	 */
	public void Log(int logType, String message)
	{
		if (CheckOut(logType))
		{
			if (logType == 1)
			{
				Debug(message);
			}
			else if (logType == 2)
			{
				Info(message);
			}
			else if (logType == 3)
			{
				Warn(message);
			}
			else if (logType == 4)
			{
				Error(message);
			}
			else if (logType == 5)
			{
				Fatal(message);
			}
		}
	}

	/**
	 * 输出警告日志
	 */
	public void Warn(String message)
	{
		message = MessageFormat.format("{0} {1} {2}\r\n", GetNowDate(m_fileAppenderConfig.m_datePattern), "Warn",
				message);
		Append(m_fileAppenderConfig.m_logFile, message);
	}
}
