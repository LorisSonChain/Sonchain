//GAIA_USERMANAGERSERVER_JAVA_S1:ZHOULIN100%
/********************************************************************************************\
*                                                                                           *
* RollingFileAppender.java -        滚动日志            *
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
import java.io.OutputStreamWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import sonchain.blockchain.base.CFileA;

/**
 * 滚动日志
 * 
 * @author GAIA
 *
 */
public class RollingFileAppender extends FileAppender
{
	/**
	 * 构造函数
	 */
	public RollingFileAppender()
	{
	}

	/**
	 * 构造函数
	 * 
	 * @param fileAppenderConfig
	 */
	public RollingFileAppender(RollingFileAppenderConfig fileAppenderConfig)
	{
		m_fileAppenderConfig = fileAppenderConfig;
		m_baseLogFileName = m_fileAppenderConfig.m_logFile;
		CheckDirectory(m_baseLogFileName);
		CalculateLogFileSize();
		InitExistingLog();
	}

	/**
	 * 构造函数
	 * 
	 * @param configPath
	 */
	public RollingFileAppender(String configPath)
	{
		m_fileAppenderConfig = new RollingFileAppenderConfig();
		m_fileAppenderConfig.LoadConfig(configPath);
		m_baseLogFileName = m_fileAppenderConfig.m_logFile;
		CheckDirectory(m_baseLogFileName);
		CalculateLogFileSize();
		InitExistingLog();
	}

	// 滚动日志的基础名字,也就是第一个日志的名字
	private String m_baseLogFileName = "";

	// 配置文件
	private RollingFileAppenderConfig m_fileAppenderConfig;

	// 当前备份的文件的个数
	private int m_curSizeRollBackups = 0;

	// 单个日志文件大小
	public long m_maxFileSize = 1 * 1024 * 1024;

	/**
	 * 追加之前调整文件
	 * 
	 * @param logFile
	 */
	protected void AdjustFileBeforeAppend(String logFile)
	{
		File file = new File(logFile);
		if (file.exists())
		{
			if (file.length() >= m_maxFileSize)
			{
				RollOverRenameFiles(m_baseLogFileName);
			}
		}
	}

	/**
	 * 向文件中追加内容
	 */
	protected boolean Append(String logFile, String content)
	{
		try
		{
			if (logFile == null || logFile.length() == 0)
			{
				return false;
			}
			AdjustFileBeforeAppend(logFile);
			CFileA.Append(logFile, content);
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	/**
	 * 计算日志的大小,检测用户指定的文件大小是否合法;
	 */
	protected void CalculateLogFileSize()
	{
		if (m_fileAppenderConfig == null || m_fileAppenderConfig.m_maxFileSize == null
				|| m_fileAppenderConfig.m_maxFileSize.length() == 0)
		{
			return;
		}
		int baseLine = 1024;
		String maxFileSize = m_fileAppenderConfig.m_maxFileSize.toUpperCase();
		if (maxFileSize.contains("MB"))
		{
			maxFileSize = maxFileSize.replace("MB", "");
			baseLine = 1024 * 1024;
		}
		else if (maxFileSize.contains("KB"))
		{
			maxFileSize = maxFileSize.replace("KB", "");
			baseLine = 1024;
		}
		else if (maxFileSize.contains("GB"))
		{
			maxFileSize = maxFileSize.replace("GB", "");
			baseLine = 1024 * 1024 * 1024;
		}
		int fileSize = Integer.parseInt(maxFileSize);
		m_maxFileSize = fileSize * baseLine;
	}

	/**
	 * 合并文件的名字. exp: path1 = c\a ; path2 = qq.txt ; result = c\a\qq.txt;
	 * 
	 * @param path1
	 * @param path2
	 * @return
	 */
	private String CombinePath(String path1, String path2)
	{
		File file1 = new File(path1);
		String extension = file1.getName();
		extension = extension.substring(extension.indexOf("."), extension.length());
		if (extension.length() > 0)
		{
			return path1.substring(0, path1.lastIndexOf(".")) + path2 + extension;
		}
		else
		{
			return path1 + path2;
		}
	}

	public void Debug(String message)
	{
		message = MessageFormat.format("{0} {1} {2}\r\n", GetNowDate(m_fileAppenderConfig.m_datePattern), "Debug",
				message);
		Append(m_fileAppenderConfig.m_logFile, message);
	}

	/**
	 * 删除文件
	 * 
	 * @param fileName
	 */
	protected void DeleteFile(String fileName)
	{
		File file = new File(fileName);
		if (file.exists())
		{
			file.delete();
		}
	}

	// 检测当前滚动文件信息
	private void DetermineCurSizeRollBackups()
	{
		m_curSizeRollBackups = 0;
		String fullPath = null;
		String fileName = null;
		File file = new File(m_baseLogFileName);
		fullPath = file.getAbsolutePath();
		fileName = file.getName();
		List<String> arrayFiles = GetExistingFiles(fullPath);
		InitializeRollBackups(fileName, arrayFiles);
	}

	public void Error(String message)
	{
		message = MessageFormat.format("{0} {1} {2}\r\n", GetNowDate(m_fileAppenderConfig.m_datePattern), "Error",
				message);
		Append(m_fileAppenderConfig.m_logFile, message);
	}

	public void Fatal(String message)
	{
		message = MessageFormat.format("{0} {1}{2}", GetNowDate(m_fileAppenderConfig.m_datePattern), "Fatal", message);
		Append(m_fileAppenderConfig.m_logFile, message);
	}

	/**
	 * 获取当前文件是第几个备份 exp:xxxx.2.log
	 * 
	 * @param curFileName
	 * @return
	 */
	private int GetBackUpIndex(String curFileName)
	{
		File file = new File(curFileName);
		String extension = file.getName();// 获取名字
		// 拿到扩展名
		extension = extension.substring(extension.indexOf("."), extension.length());
		if (extension != null && extension.length() > 0)
		{
			curFileName = curFileName.replace(extension, "");// 将扩展名字转换为空
		}
		int backUpIndex = -1;
		String fileName = curFileName;
		int index = fileName.lastIndexOf(".");
		if (index > 0)
		{
			backUpIndex = Integer.parseInt(fileName.substring(index + 1));
		}
		return backUpIndex;
	}

	/**
	 * 获取指定文件夹下面的所有文件,不包括文件夹
	 * 
	 * @param fullPath
	 * @return
	 */
	private List<String> GetExistingFiles(String fullPath)
	{
		List<String> con = new ArrayList<>();
		File file = new File(fullPath);
		File[] files = file.listFiles();
		if (files == null || files.length < 1)
		{
			return con;
		}
		for (File f : files)
		{
			if (f.isFile())
			{
				con.add(f.getName());
			}
		}
		return con;
	}

	public void Info(String message)
	{
		message = MessageFormat.format("{0} {1} {2}\r\n", GetNowDate(m_fileAppenderConfig.m_datePattern), "Info",
				message);
		Append(m_fileAppenderConfig.m_logFile, message);
	}

	/**
	 * 初始化备份级别
	 * 
	 * @param baseFile
	 * @param curFileName
	 */
	private void InitializeFromOneFile(String baseFile, String curFileName)
	{
		File file = new File(baseFile);
		// 文件名字不一致
		if (curFileName.startsWith(file.getName()) == false)
		{
			return;
		}
		// 拿到自己了
		if (curFileName.equals(baseFile))
		{
			return;
		}
		try
		{
			int backup = GetBackUpIndex(curFileName);// 获取备份级别
			if (backup > m_curSizeRollBackups)
			{
				if (0 == m_fileAppenderConfig.m_maxSizeRollBackups)
				{

				}
				else if (-1 == m_fileAppenderConfig.m_maxSizeRollBackups)
				{// 用户填写错误?
					m_curSizeRollBackups = backup;
				}
				else
				{
					if (backup <= m_fileAppenderConfig.m_maxSizeRollBackups)
					{
						m_curSizeRollBackups = backup;
					}
				}
			}
		}
		catch (Exception e)
		{

		}
	}

	/**
	 * 初始化滚动信息
	 * 
	 * @param baseFile
	 * @param arrayFiles
	 */
	private void InitializeRollBackups(String baseFile, List<String> arrayFiles)
	{
		if (null != arrayFiles && arrayFiles.size() > 0)
		{
			String baseFileLower = baseFile.toLowerCase();
			for (String curFileName : arrayFiles)
			{
				InitializeFromOneFile(baseFileLower, curFileName.toLowerCase());
			}
		}
	}

	/**
	 * 初始化当前日志文件
	 */
	protected void InitExistingLog()
	{
		DetermineCurSizeRollBackups();
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
	 * 滚动文件;;;;用fromFile覆盖toFile;
	 * 
	 * @param fromFile
	 * @param toFile
	 */
	protected void RollFile(String fromFile, String toFile)
	{
		File file = new File(fromFile);
		if (file.exists())
		{
			DeleteFile(toFile);
			file.renameTo(new File(toFile));
		}

	}

	/**
	 * 滚动重新命名文件的名称
	 * 
	 * @param baseFileName
	 */
	protected void RollOverRenameFiles(String baseFileName)
	{
		if (m_fileAppenderConfig.m_maxSizeRollBackups != 0)
		{
			// 日志文件个数已经满了
			if (m_curSizeRollBackups >= m_fileAppenderConfig.m_maxSizeRollBackups)
			{
				// 删除文件
				DeleteFile(CombinePath(baseFileName, "." + m_fileAppenderConfig.m_maxSizeRollBackups));
				m_curSizeRollBackups--;
			}
			for (int i = m_curSizeRollBackups; i >= 1; i--)
			{
				RollFile((CombinePath(baseFileName, "." + i)), (CombinePath(baseFileName, "." + (i + 1))));
			}
			m_curSizeRollBackups++;
			RollFile(baseFileName, CombinePath(baseFileName, ".1"));
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
