package sonchain.blockchain.log;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import sonchain.blockchain.base.CFileA;
import sonchain.blockchain.service.DataCenter;

public class SeparateDirAppender extends FileAppender 
{
	/**
	 * 构造函数
	 */
	public SeparateDirAppender()
	{
	}

	/**
	 * 构造函数
	 * 
	 * @param fileAppenderConfig
	 */
	public SeparateDirAppender(RollingFileAppenderConfig fileAppenderConfig)
	{
		m_fileAppenderConfig = fileAppenderConfig;
		m_baseLogFileName = m_fileAppenderConfig.m_logFile;
		CheckDirectory(m_baseLogFileName);
		CalculateLogFileSize();
	}

	/**
	 * 构造函数
	 * 
	 * @param configPath
	 */
	public SeparateDirAppender(String configPath)
	{
		m_fileAppenderConfig = new RollingFileAppenderConfig();
		m_fileAppenderConfig.LoadConfig(configPath);
		m_baseLogFileName = m_fileAppenderConfig.m_logFile;
		CheckDirectory(m_baseLogFileName);
		CalculateLogFileSize();
	}
	
	// 滚动日志的基础名字,也就是第一个日志的名字
	private String m_baseLogFileName = "";

	// 配置文件
	private RollingFileAppenderConfig m_fileAppenderConfig;

	// 单个日志文件大小
	public long m_maxFileSize = 1 * 1024 * 1024;
	

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
	
	/**
	 * 输出日志
	 */
	public void Log(int logType, String message)
	{
		if (CheckOut(logType))
		{
			String type = null;
			if (logType == 1)
			{
				type = "Debug";
			}
			else if (logType == 2)
			{
				type = "Info";
			}
			else if (logType == 3)
			{
				type = "Warn";
			}
			else if (logType == 4)
			{
				type = "Error";
			}
			else if (logType == 5)
			{
				type = "Fatal";
			}
			//yyyyMMdd
			message = MessageFormat.format("{0} {1} {2}\r\n", GetNowDate(m_fileAppenderConfig.m_datePattern), type,
					message);
			
			String logFile = m_fileAppenderConfig.m_logFile;
			if (logFile.length() == 0)
			{
				return;
			}
			String separator = FILESEPARATOR;
			String date = GetNowDate("yyyyMMdd");
			int np = logFile.lastIndexOf(date);
			if(np < 0)
			{
				String logDir = DataCenter.GetAppPath() + "\\log\\" + date;
				CFileA.CreateDirectory(logDir);
				int pos = logFile.lastIndexOf(separator);
				String logFileName = logFile.substring(pos, logFile.length() - 1);
				String newLogFile = logDir + separator + logFileName;
				m_fileAppenderConfig.m_logFile = newLogFile;
				CFileA.Write(logFile, message);
				return;
				
			}
			if(CFileA.IsFileExist(logFile))
			{
				File file = new File(logFile);
				long len = file.length();
				if (len >= m_maxFileSize)
				{
					String logDir = DataCenter.GetAppPath() + "\\log\\" + date;
					ArrayList<String> files = new ArrayList<>();
					CFileA.GetFiles(logDir, files);
					int count = files.size();
					files.clear();
					int pos = logFile.lastIndexOf(separator);
					int pos2 = logFile.lastIndexOf(".");
					String logFileName = logFile.substring(pos, pos2 - 1);
					String newFileName = MessageFormat.format("{0}\\{1}.{2}.log", logDir, logFileName, count);
					File oldFile = new File(logFile);
			        File newFile = new File(newFileName);
			        oldFile.renameTo(newFile);
					CFileA.Write(logFile, message);
					return;
				}
			}
			CFileA.Append(logFile, message);
		}
	}
}
