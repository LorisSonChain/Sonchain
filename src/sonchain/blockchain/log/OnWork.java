//GAIA_USERMANAGERSERVER_JAVA_S1:ZHOULIN100%
/********************************************************************************************\
*                                                                                           *
* OnWork.java -        日志线程工作类             *
*                                                                                           *
*               Version 1.00  ★                                                            *
*                                                                                           *
*               Copyright (c) 2016-2017, Gaia Financial technology. All rights reserved.    *
*               Created by Joel 2017/06/12.                                                  *
*                                                                                           *
*********************************************************************************************/
package sonchain.blockchain.log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 日志线程工作类
 * 
 * @author JOEL
 *
 */
public class OnWork implements Runnable
{

	// 执行ID
	private int id;

	// 消息
	private List<LogWorkDataInfo> m_messages;

	//
	private Map<Integer, List<LogWorkDataInfo>> m_dataInfos;

	// 停止信息
	private List<LogWorkDataInfo> m_stopDatas;

	public OnWork()
	{

	}

	/**
	 *
	 * @param id
	 * @param m_messages
	 * @param m_dataInfos
	 * @param m_stopDatas
	 */
	public OnWork(int id, List<LogWorkDataInfo> m_messages, Map<Integer, List<LogWorkDataInfo>> m_dataInfos,
			List<LogWorkDataInfo> m_stopDatas)
	{
		super();
		this.id = id;
		this.m_messages = m_messages;
		this.m_dataInfos = m_dataInfos;
		this.m_stopDatas = m_stopDatas;
	}

	/**
	 * 线程入口
	 */
	public void run()
	{
		while (true)
		{
			int count = exc();
			if (count == 0)
			{
				try
				{
					Thread.sleep(1);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 线程实际方法
	 */
	public int exc()
	{
		int count = 0;
		synchronized (m_messages)
		{
			int messagesSize = m_messages.size();
			if (messagesSize > 0)
			{
				for (int i = 0; i < messagesSize;)
				{
					LogWorkDataInfo message = m_messages.get(i);
					OnWorkStart(message);
					m_dataInfos.get(id).add(message);
					m_messages.remove(i);
					break;
				}
			}

		}
		// 执行方法
		List<LogWorkDataInfo> dataInfos = m_dataInfos.get(id);
		int dataInfosSize = dataInfos.size();
		// 检测停止
		List<LogWorkDataInfo> stopDatas = new ArrayList<LogWorkDataInfo>();
		int stopDatasSize = stopDatas.size();
		if (stopDatasSize > 0)
		{
			synchronized (m_stopDatas)
			{
				stopDatasSize = m_stopDatas.size();
				for (int i = 0; i < stopDatasSize; i++)
				{
					stopDatas.add(m_stopDatas.get(i));
				}
			}
		}
		for (int i = 0; i < dataInfosSize; i++)
		{
			LogWorkDataInfo pushDataInfo = dataInfos.get(i);
			pushDataInfo.m_threadID = id;
			int state = 0;
			if (stopDatasSize > 0)
			{
				for (int j = 0; j < stopDatasSize; j++)
				{
					LogWorkDataInfo reqDataInfo = stopDatas.get(j);
					if (reqDataInfo.m_id == pushDataInfo.m_id)
					{
						synchronized (m_stopDatas)
						{
							int spdSize = m_stopDatas.size();
							for (int s = 0; s < spdSize; s++)
							{
								if (m_stopDatas.get(s).m_id == reqDataInfo.m_id)
								{
									m_stopDatas.remove(s);
									break;
								}
							}
						}
						state = -1;
					}
				}
			}
			if (state == 0)
			{
				state = OnWorking(pushDataInfo);
				count++;
			}
			if (state <= 0)
			{
				QuitWork(pushDataInfo);
				dataInfos.remove(i);
				i--;
				dataInfosSize--;
			}
		}
		return count;
	}

	/**
	 * 开始工作
	 * 
	 * @param dataInfo
	 */
	private void OnWorkStart(LogWorkDataInfo dataInfo)
	{
	}

	/**
	 * 工作中
	 * 
	 * @param dataInfo
	 * @return
	 */
	private int OnWorking(LogWorkDataInfo dataInfo)
	{
		dataInfo.m_appender.Log(dataInfo.m_logType, dataInfo.m_logContent);
		return 0;
	}

	/**
	 * 结束工作 退出
	 * 
	 * @param reqInfo
	 * @return
	 */
	private int QuitWork(LogWorkDataInfo reqInfo)
	{
		synchronized (m_stopDatas)
		{
			m_stopDatas.add(reqInfo);
		}
		return 0;
	}
}
