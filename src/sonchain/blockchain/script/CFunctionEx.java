//******************************************************************************************\
//*                                                                                         *
//* CFunctionEx.java -  Indicator functions, types, and definitions.                        *
//*                                                                                         *
//*               Version 1.00  ★★�                                                       *
//*                                                                                         *
//*               Copyright (c) 2016-2016, Piratecat. All rights reserved.                  *
//*               Created by Lord 2016/10/17.                                               *
//*                                                                                         *
//*******************************************************************************************

package sonchain.blockchain.script;

import sonchain.blockchain.service.DataCenter;
import owchart.owlib.Base.*;
import owchart.owlib.Chart.*;
import owchart.owlib.Indicator.CIndicatorEx;

/**
 * 提示方法
 * 
 */
public class CFunctionEx extends CFunction
{
	/**
	 * 创建方法
	 * 
	 * @param indicator 指标
	 * @param id ID
	 * @param name 名称
	 * @inative XML
	 */
	public CFunctionEx(CIndicator indicator, int id, String name, INativeBase inative)
	{
		m_indicator = indicator;
		m_ID = id;
		m_name = name;
		m_native = inative;
	}

	/**
	 * 指标
	 * 
	 */
	public CIndicator m_indicator;

	/**
	 * XML对象
	 * 
	 */
	public INativeBase m_native;

	/**
	 * 所有方法
	 * 
	 */
	private static final String FUNCTIONS = "CREATETHREAD,ISAPPALIVE";

	/**
	 * 开始索引
	 */
	private static final int STARTINDEX = 1000000;

	/**
	 * 计算
	 * 
	 * @param var 变量
	 * @return 结果
	 */
	@Override
	public double OnCalculate(CVariable var)
	{
		switch (var.m_functionID)
		{
		case STARTINDEX:
			return CREATETHREAD(var);
		case STARTINDEX + 1:
			return ISAPPALIVE(var);
		default:
			return 0;
		}
	}

	/**
	 * 创建指标
	 * 
	 * @param script 脚本
	 * @param inative XML
	 * @return 指标
	 */
	public static CIndicator CreateIndicator(String script, INativeBase inative)
	{
		CIndicator indicator = new CIndicatorEx();
		CTable table = new CTableEx();
		indicator.SetDataSource(table);
		CFunctionBase.AddFunctions(indicator);
		CFunctionHttp.AddFunctions(indicator);
		int index = STARTINDEX;
		String[] functions = FUNCTIONS.split("[,]");
		int functionsSize = functions.length;
		for (int i = 0; i < functionsSize; i++)
		{
			indicator.AddFunction(new CFunctionEx(indicator, index + i, functions[i], inative));
		}
		indicator.SetScript(script);
		table.AddColumn(0);
		table.Set(0, 0, 0);
		indicator.OnCalculate(0);
		return indicator;
	}

	/**
	 * 启动线程
	 * 
	 * @param var 变量
	 * @return 状态
	 */
	private double CREATETHREAD(CVariable var)
	{
		Thread thread = new Thread()
		{
			public void run()
			{
				CIndicator indicator = CreateIndicator(DataCenter.GetNodeService().GetScript(), m_native);
				indicator.CallFunction(m_indicator.GetText(var.m_parameters[0]));
				indicator.Dispose();
			}
		};
		thread.start();
		return 0;
	}

	/**
	 * 验证程序是否存活
	 * 
	 * @param var 变量
	 * @return 状态
	 */
	private double ISAPPALIVE(CVariable var)
	{
		return DataCenter.IsAppAlive() == true ? 1 : 0;
	}
}