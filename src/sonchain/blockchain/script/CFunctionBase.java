/*******************************************************************************************\
*                                                                                           *
* CFunctionBase.java -  Base functions, types, and definitions.                             *
*                                                                                           *
*               Version 1.00  ★★�?                                                          *
*                                                                                           *
*               Copyright (c) 2016-2016, Piratecat. All rights reserved.                    *
*               Created by Lord 2016/10/17.                                                 *
*                                                                                           *
********************************************************************************************/

package sonchain.blockchain.script;

import java.util.Scanner;
import owchart.owlib.Chart.*;

/**
 *
 * @author Todd
 */
public class CFunctionBase extends CFunction
{
	/**
	 * 创建方法
	 * 
	 * @param indicator
	 *            指标
	 * @param id
	 *            ID
	 * @param name
	 *            名称
	 */
	public CFunctionBase(CIndicator indicator, int id, String name)
	{
		m_indicator = indicator;
		m_ID = id;
		m_name = name;
	}

	/**
	 * 指标
	 * 
	 */
	public CIndicator m_indicator;

	/**
	 * 方法
	 */
	private static String FUNCTIONS = "IN,OUT,SLEEP";

	/**
	 * 前缀
	 */
	private static String PREFIX = "";

	/**
	 * 开始索引
	 */
	private static final int STARTINDEX = 1000;

	/**
	 * 添加方法
	 * 
	 * @param indicator
	 *            脚本
	 * @param inative
	 *            XML
	 * @return 指标
	 */
	public static void AddFunctions(CIndicator indicator)
	{
		String[] functions = FUNCTIONS.split("[,]");
		int functionsSize = functions.length;
		for (int i = 0; i < functionsSize; i++)
		{
			indicator.AddFunction(new CFunctionBase(indicator, STARTINDEX + i, PREFIX + functions[i]));
		}
	}

	/**
	 * 计算
	 * 
	 * @param var
	 *            变量
	 * @return 结果
	 */
	@Override
	public double OnCalculate(CVariable var)
	{
		switch (var.m_functionID)
		{
		case STARTINDEX:
			return IN(var);
		case STARTINDEX + 1:
			return OUT(var);
		case STARTINDEX + 2:
			return SLEEP(var);
		default:
			return 0;
		}
	}

	/**
	 * 输入函数
	 * 
	 * @param var
	 *            变量
	 * @return 状态
	 */
	private double IN(CVariable var)
	{
		Scanner sc = new Scanner(System.in);
		CVariable newVar = new CVariable(m_indicator);
		newVar.m_expression = "'" + sc.next() + "'";
		m_indicator.SetVariable(var.m_parameters[0], newVar);
		return 0;
	}

	/**
	 * 输出函数
	 * 
	 * @param var
	 *            变量
	 * @return 状态
	 */
	private double OUT(CVariable var)
	{
		int len = var.m_parameters.length;
		for (int i = 0; i < len; i++)
		{
			String text = m_indicator.GetText(var.m_parameters[i]);
			System.out.print(text);
		}
		System.out.println("");
		return 0;
	}

	/**
	 * 睡眠
	 * 
	 * @param var
	 *            变量
	 * @return 状态
	 */
	private double SLEEP(CVariable var)
	{
		try
		{
			Thread.sleep((int) m_indicator.GetValue(var.m_parameters[0]));
		}
		catch (Exception ex)
		{
		}
		return 1;
	}
}
