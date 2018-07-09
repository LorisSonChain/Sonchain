package sonchain.blockchain.contract;

import java.util.*;
import java.util.regex.Pattern;

import owchart.owlib.Base.CMathLib;
import owchart.owlib.Base.COLOR;
import owchart.owlib.Base.CStr;
import owchart.owlib.Base.RefObject;
import owchart.owlib.Chart.*;
import owchart.owlib.Indicator.LPDATA;

public class CIndicatorEx2 extends CIndicator
{
    public CIndicatorEx2()
    {
        m_mainVariables = new HashMap<String, Integer>();
        m_variables = new ArrayList<CVariable>();
        m_defineParams = new HashMap<String, Double>();
        m_lines = new ArrayList<CVariable>();
        String[] functions = FUNCTIONS.split("[,]");
        String[] fieldFunctions = FUNCTIONS_FIELD.split("[,]");
        int iSize = functions.length;
        int jSize = fieldFunctions.length;
        for (int i = 0; i < iSize; i++)
        {
            int cType = 0;
            for (int j = 0; j < jSize; j++)
            {
                if (functions[i].equals(fieldFunctions[j]))
                {
                    cType = 1;
                    break;
                }
            }
            CFunction function = new CFunction(i, functions[i]);
            function.m_type = cType;
            m_functions.put(function.m_name, function);
        }
        m_systemColors.add(COLOR.ARGB(255, 255, 255));
        m_systemColors.add(COLOR.ARGB(255, 255, 0));
        m_systemColors.add(COLOR.ARGB(255, 0, 255));
        m_systemColors.add(COLOR.ARGB(0, 255, 0));
        m_systemColors.add(COLOR.ARGB(82, 255, 255));
        m_systemColors.add(COLOR.ARGB(255, 82, 82));
		m_varFactory = new CVarFactory();
    }

    protected void finalize() throws Throwable
    {
        Dispose();
    }

    private HashMap<String, Double> m_defineParams;

    private static String FUNCTIONS = "CURRBARSCOUNT,BARSCOUNT,DRAWKLINE,STICKLINE,VALUEWHEN,BARSLAST,DOWNNDAY,DRAWICON,DRAWNULL,FUNCTION,FUNCVAR"
            + ",DRAWTEXT,POLYLINE,BETWEEN,CEILING,EXPMEMA,HHVBARS,INTPART,LLVBARS,DOTIMES,DOWHILE,CONTINUE"
            + ",RETURN,REVERSE,AVEDEV,MINUTE,SQUARE,UPNDAY,DELETE"
            + ",COUNT,CROSS,EVERY,EXIST,EXPMA,FLOOR,MONTH,ROUND,TIME2,WHILE,BREAK,CHUNK"
            + ",ACOS,ASIN,ATAN,DATE,HOUR,LAST,MEMA,NDAY,RAND,SIGN,SQRT,TIME,YEAR"
            + ",ABS,AMA,COS,DAY,DMA,EMA,EXP,HHV,IFF,IFN,LLV,LOG,MAX,MIN"
            + ",MOD,NOT,POW,SIN,SMA,STD,SUM,TAN,REF,SAR,FOR,GET,SET"
            + ",TMA,VAR,WMA,ZIG,IF,MA"
            + ",STR.CONTACT,STR.EQUALS,STR.FIND,STR.FINDLAST,STR.LENGTH,STR.SUBSTR,STR.REPLACE,STR.SPLIT,STR.TOLOWER,STR.TOUPPER,LIST.ADD,LIST.CLEAR,LIST.GET,LIST.INSERT,LIST.REMOVE,LIST.SIZE,MAP.CLEAR,MAP.CONTAINSKEY,MAP.GET,MAP.GETKEYS,MAP.REMOVE,MAP.SET,MAP.SIZE";


    private int m_break;

    private static int FUNCTIONID_FUNCVAR = 10;

    private static int FUNCTIONID_FUNCTION = 9;

    private static int FUNCTIONID_VAR = 82;

    private static String FUNCTIONS_FIELD = "EXPMEMA,EXPMA,MEMA,AMA,DMA,EMA,SMA,SUM,SAR,TMA,WMA,MA";

    private HashMap<String, CFunction> m_functions = new HashMap<String, CFunction>();

    private HashMap<Integer, CFunction> m_functionsMap = new HashMap<Integer, CFunction>();

    private ArrayList<CVariable> m_lines;

    private Random m_random = new Random();

    private double m_result;
    
    private CVar m_resultVar;

    private HashMap<String, CVariable> m_tempFunctions = new HashMap<String, CVariable>();

    private HashMap<String, CVariable> m_tempVariables = new HashMap<String, CVariable>();

    private static String VARIABLE = "~";

    private static String VARIABLE2 = "@";

	private static String VARIABLE3 = "?";

    protected ArrayList<CVariable> m_variables;

    protected AttachVScale m_attachVScale = AttachVScale.Left;

    @Override
    public AttachVScale GetAttachVScale()
    {
        return m_attachVScale;
    }
    @Override
    public void SetAttachVScale(AttachVScale value)
    {
        m_attachVScale = value;
        for (CVariable var : m_variables)
        {
            if (var.m_polylineShape != null)
            {
                var.m_barShape.SetAttachVScale(value);
                var.m_candleShape.SetAttachVScale(value);
                var.m_polylineShape.SetAttachVScale(value);
                var.m_textShape.SetAttachVScale(value);
            }
        }
    }

    protected CTable m_dataSource = null;

    @Override
    public CTable GetDataSource()
    {
        return m_dataSource;
    }
    @Override
    public void SetDataSource(CTable value)
    {
        m_dataSource = value;
    }

    protected CDiv m_div = null;

    @Override
    public CDiv GetDiv()
    {
        return m_div;
    }
    @Override
    public void SetDiv(CDiv value)
    {
        m_div = value;
        m_dataSource = m_div.GetChart().GetDataSource();
    }

    protected boolean m_isDisposed = false;

    @Override
    public boolean IsDisposed()
    {
        return m_isDisposed;
    }

    protected int m_index = -1;

    @Override
    public int GetIndex()
    {
        return m_index;
    }

    protected HashMap<String, Integer> m_mainVariables;

    @Override
    public HashMap<String, Integer> GetMainVariables()
    {
        return m_mainVariables;
    }

    protected String m_name;

    @Override
    public String GetName()
    {
        return m_name;
    }
    @Override
    public void SetName(String value)
    {
        m_name = value;
    }

	@Override
	public double GetResult()
	{
		return m_result;
	}

    @Override
    public void SetScript(String value)
    {
        synchronized(this)
        {
            m_lines.clear();
            m_defineParams.clear();
            ArrayList<String> lines = new ArrayList<String>();
            GetMiddleScript(value, lines);
            int linesCount = lines.size();
            for (int i = 0; i < linesCount; i++)
            {
                String strLine = lines.get(i);
                if (strLine.startsWith("FUNCTION "))
                {
                    String funcName = strLine.substring(9, strLine.indexOf('(')).toUpperCase();
                    AddFunction(new CFunction(FUNCTIONID_FUNCTION, funcName));
                }
                else if (strLine.startsWith("CONST "))
                {
                    String[] consts = strLine.substring(6).split("[:]");
                    m_defineParams.put(consts[0], CStr.ConvertStrToDouble(consts[1]));
                    lines.remove(i);
                    i--;
                    linesCount--;
                }
            }
            linesCount = lines.size();
            for(int i = 0; i < linesCount; i++)
            {
                AnalysisScriptLine(lines.get(i));
            }
            lines.clear();
        }
    }

    protected ArrayList<Long> m_systemColors = new ArrayList<Long>();

    @Override
    public ArrayList<Long> GetSystemColors()
    {
        return m_systemColors;
    }
    @Override
    public void SetSystemColors(ArrayList<Long> value)
    {
        m_systemColors = value;
    }
	
	protected Object m_tag = null;

        @Override
	public Object GetTag()
	{
		return m_tag;
	}
        @Override
	public void SetTag(Object value)
	{
		m_tag = value;
	}

	protected CVarFactory m_varFactory;

	@Override
	public CVarFactory GetVarFactory()
	{
		return m_varFactory;
	}

	@Override
	public void SetVarFactory(CVarFactory value)
	{
		m_varFactory = value;
	}

    @Override
    public void AddFunction(CFunction function)
    {
        m_functions.put(function.m_name, function);
        m_functionsMap.put(function.m_ID, function);
    }

    @Override
    public double CallFunction(String funcName)
    {
        double result = 0;
        synchronized(this)
        {
            ArrayList<String> lines = new ArrayList<String>();
            GetMiddleScript(funcName, lines);
            int linesSize = lines.size();
            m_result = 0;
            for (int i = 0; i < linesSize; i++)
            {
                String str = lines.get(i);
                int cindex = str.indexOf('(');
                String upperName = str.substring(0, cindex).toUpperCase();
                if (m_tempFunctions.containsKey(upperName))
                {
                    CVariable function = m_tempFunctions.get(upperName);
                    int rindex = str.lastIndexOf(')');
                    CVariable topVar = new CVariable(this);
                    if (rindex - cindex > 1)
                    {
                        String pStr = str.substring(cindex + 1, rindex);
                        String[] pList = pStr.split("[" + VARIABLE2 + "]");
                        String[] fieldTexts = function.m_fieldText.split("[" + VARIABLE2 + "]");
                        int pListLen = pList.length;
                        if(!(pListLen == 1 && pList[0].length() == 0)) {
                            topVar.m_parameters = new CVariable[pListLen * 2];
                            for (int j = 0; j < pListLen; j++) {
                                String pName = fieldTexts[j];
                                String pValue = pList[j];
                                CVariable varName = null;
                                if (m_tempVariables.containsKey(pName)) {
                                    varName = m_tempVariables.get(pName);
                                }
                                CVariable varValue = new CVariable(this);
                                varValue.m_expression = pValue;
                                if (pValue.startsWith("\'")) {
                                    varValue.m_type = 1;
                                } else {
                                    varValue.m_type = 3;
                                    varValue.m_value = CStr.ConvertStrToDouble(pValue);
                                }
                                topVar.m_parameters[j * 2] = varName;
                                topVar.m_parameters[j * 2 + 1] = varValue;
                            }
                            FUNCVAR(topVar);
                        }
                    }
                    GetValue(m_tempFunctions.get(upperName));
                    if (topVar.m_parameters != null)
                    {
                        int variablesSize = topVar.m_parameters.length;
                        for (int j = 0; j < variablesSize; j++)
                        {
                            if (j % 2 == 0)
                            {
                                int id = topVar.m_parameters[j].m_field;
                                if (m_tempVars.containsKey(id))
                                {
                                    CVar cVar = m_tempVars.get(id);
                                    if (cVar.m_parent != null)
                                    {
                                        m_tempVars.put(id, cVar.m_parent);
                                    }
                                    else
                                    {
                                        m_tempVars.remove(id);
                                    }
                                    cVar.Dispose();
                                }
                            }
                        }
                    }
                }
            }
            lines.clear();
            result = m_result;
            m_result = 0;
			m_break = 0;
        }
        return result;
    }

    private void AnalysisVariables(RefObject<String> sentence, int line, String funcName, String fieldText, boolean isFunction)
    {
        ArrayList<String> wordsList = new ArrayList<String>();
        String[] splitWords = SplitExpression2(sentence.argvalue);
        int splitWordsSize = splitWords.length;
        for(int s = 0; s < splitWordsSize; s++)
        {
            String wStr = splitWords[s];
            String[] subWStr = wStr.split(VARIABLE2 + "|:");
            int subWStrSize = subWStr.length;
            for (int u = 0; u < subWStrSize; u++)
            {
                if (m_functions.containsKey(subWStr[u]))
                {
                    wordsList.add(subWStr[u]);
                }
            }
        }
        int wordsListSize = wordsList.size();
        for (int f = 0; f < wordsListSize; f++)
        {
            String word = wordsList.get(f);
            CFunction func = m_functions.get(word);
            String fName = func.m_name;
            int funcID = func.m_ID;
            int funcType = func.m_type;
            String function = fName + "(";
            int bIndex = sentence.argvalue.indexOf(function);
            while (bIndex != -1)
            {
                int rightBracket = 0;
                int idx = 0;
                int count = 0;
                char[] charArray  = sentence.argvalue.toCharArray();
                for (char ch : charArray)
                {
                    if (idx >= bIndex)
                    {
                        if (ch == '(')
                        {
                            count++;
                        }
                        else if (ch == ')')
                        {
                            count--;
                            if (count == 0)
                            {
                                rightBracket = idx;
                                break;
                            }
                        }
                    }
                    idx++;
                }
                if (rightBracket == 0)
                {
                    break;
                }
                String body = sentence.argvalue.substring(bIndex, rightBracket + 1);
                CVariable var = new CVariable(this);
                var.m_name = String.format("%s%d", VARIABLE, m_variables.size());
                var.m_expression = body.substring(0, body.indexOf('('));
                var.m_type = 0;
                var.m_functionID = funcID;
                var.m_fieldText = body;
                if (funcType == 1)
                {
                    int field = CTable.GetAutoField();
                    var.m_field = field;
                    m_dataSource.AddColumn(field);
                }
                m_variables.add(var);
                if (bIndex == 0)
                {
                    if (isFunction)
                    {
                        var.m_funcName = funcName;
                        var.m_line = line;
                        var.m_fieldText = fieldText;
                        m_lines.add(var);
                        m_tempFunctions.put(funcName, var);
                        isFunction = false;
                    }
                }
                var.m_splitExpression = SplitExpression(var.m_expression);
                int startIndex = bIndex + function.length();
                String subSentence = sentence.argvalue.substring(startIndex, rightBracket);
                if (funcID == FUNCTIONID_FUNCTION)
                {
                    if (m_tempFunctions.containsKey(fName))
                    {
                        if (m_tempFunctions.get(fName).m_fieldText != null)
                        {
                            String[] fieldTexts = m_tempFunctions.get(fName).m_fieldText.split("["+ VARIABLE2 +"]");
                            String[] transferParams = subSentence.split("["+ VARIABLE2 +"]");
                            subSentence = "";
                            int transferParamsLen = transferParams.length;
                            for (int i = 0; i < transferParamsLen; i++)
                            {
                                if (i == 0)
                                {
                                    subSentence = "FUNCVAR(";
                                }
								subSentence += fieldTexts[i] + VARIABLE2 + transferParams[i];
                                if (i != transferParamsLen - 1)
                                {
                                    subSentence += VARIABLE2;
                                }
                                else
                                {
                                    subSentence += ")";
                                }
                            }
                        }
                    }
                }
                RefObject<String> tempRef_subSentence = new RefObject<String>(subSentence);
                AnalysisVariables(tempRef_subSentence,  0, "", "", false);
                subSentence = tempRef_subSentence.argvalue;
                String[] parameters = subSentence.split("["+ VARIABLE2 +"]");
                if (parameters != null && parameters.length > 0 && parameters[0].length() > 0)
                {
                    var.m_parameters = new CVariable[parameters.length];
                    for (int j = 0; j < parameters.length; j++)
                    {
                        String parameter = parameters[j];
                        parameter = Replace(parameter);
                        CVariable pVar = new CVariable(this);
                        pVar.m_expression = parameter;
                        pVar.m_name = String.format("%s%d", VARIABLE, m_variables.size());
                        pVar.m_type = 1;
                        var.m_parameters[j] = pVar;
                        for (CVariable variable : m_variables)
                        {
                            if (variable.m_type == 2 && variable.m_expression.equals(parameters[j]) && variable.m_field != CTable.NULLFIELD)
                            {
                                pVar.m_type = 2;
                                pVar.m_field = variable.m_field;
                                pVar.m_fieldText = parameters[j];
                                break;
                            }
                        }
                        if(pVar.m_type == 1)
                        {
							String varKey = parameter;
                            if (varKey.indexOf("[REF]") == 0)
                            {
                                varKey = varKey.substring(5);
                            }
                            if(m_tempVariables.containsKey(varKey))
                            {
                                pVar.m_field = m_tempVariables.get(varKey).m_field;
                            }
                            else
                            {
                                pVar.m_field = -m_variables.size();
                                m_tempVariables.put(varKey, pVar);
                            }
                        }
                        m_variables.add(pVar);
                        pVar.m_splitExpression = SplitExpression(parameter);
						if (pVar.m_splitExpression != null && pVar.m_splitExpression.length == 2)
                        {
                            if (pVar.m_splitExpression[0].m_var == pVar)
                            {
                                pVar.m_splitExpression = null;
                            }
                        }
                    }
                }
                sentence.argvalue = sentence.argvalue.substring(0, bIndex) + var.m_name + sentence.argvalue.substring(rightBracket + 1);
                bIndex = sentence.argvalue.indexOf(function, sentence.argvalue.indexOf(var.m_name));
            }
        }
		wordsList.clear();
    }

    private void AnalysisScriptLine(String line)
    {
        CVariable script = new CVariable(this);
        boolean isFunction = false;
        String strLine = line;
        String funcName = null;
        String fieldText = null;
        if (line.startsWith("FUNCTION "))
        {
            int cindex = strLine.indexOf('(');
            funcName = strLine.substring(9, cindex);
            int rindex = strLine.indexOf(')');
            if (rindex - cindex > 1)
            {
                fieldText = strLine.substring(cindex + 1, rindex);
                String[] pList = fieldText.split("["+VARIABLE2 +"]");
                int pListSize = pList.length;
                for (int i = 0; i < pListSize; i++)
                {
					String str = pList[i];
					if(str.indexOf("[REF]") != -1)
					{
						str = str.substring(5);
					}
					String pCmd = "VAR(" + str + VARIABLE2 + "0)";
					RefObject<String> refCmd = new RefObject<String>(pCmd);
					AnalysisVariables(refCmd, 0, "", "", false);
                }
            }
            strLine = strLine.substring(rindex + 1);
            strLine = "CHUNK" + strLine.substring(0, strLine.length() - 1) + ")";
            isFunction = true;
        }
        RefObject<String> refStrLine = new RefObject<String>(strLine);
        AnalysisVariables(refStrLine, m_lines.size(), funcName, fieldText, isFunction);
        strLine = refStrLine.argvalue;
        script.m_line = m_lines.size();
        if (isFunction)
        {
            return;
        }
        script.m_name = "";
        String variable = null;
        String sentence = null;
        String followParameters = "";
        String op = "";
        char[] charArray = strLine.toCharArray();
        for (char ch : charArray)
        {
            if (ch != ':' && ch != '=')
            {
                if (op.length() > 0)
                {
                    break;
                }
            }
            else
            {
                op += (new Character(ch)).toString();
            }
        }
        if (op.equals(":="))
        {
            variable = strLine.substring(0, strLine.indexOf(":="));
            sentence = strLine.substring(strLine.indexOf(":=") + 2);
        }
        else if (op.equals(":"))
        {
            followParameters = "COLORAUTO";
            variable = strLine.substring(0, strLine.indexOf(":"));
            sentence = strLine.substring(strLine.indexOf(":") + 1);
            if (sentence.indexOf(VARIABLE2) != -1)
            {
                followParameters = sentence.substring(sentence.indexOf(VARIABLE2) + 1);
                sentence = sentence.substring(0, sentence.indexOf(VARIABLE2));
            }
        }
        else
        {
            sentence = strLine;
            String[] strs = sentence.split("["+VARIABLE2 +"]");
            if (strs != null && strs.length > 1)
            {
                String strVar = strs[0];
                sentence = strVar;
				int idx = CStr.ConvertStrToInt(strVar.substring(1));
				if(idx < (int)m_variables.size())
				{
					CVariable var = m_variables.get(idx);
					int startIndex = 0;
					if (var.m_parameters == null)
					{
						var.m_parameters = new CVariable[strs.length - 1];
						startIndex = 0;
					}
					else
					{
						CVariable[] newParameters = new CVariable[var.m_parameters.length + strs.length - 1];
						for (int i = 0; i < var.m_parameters.length; i++)
						{
							newParameters[i] = var.m_parameters[i];
						}
						startIndex = var.m_parameters.length;
						var.m_parameters = newParameters;
					}
					for (int i = 1; i < strs.length; i++)
					{
						CVariable newVar = new CVariable(this);
						newVar.m_type = 1;
						newVar.m_expression = strs[i];
						var.m_parameters[startIndex + i - 1] = newVar;
					}
				}
            }
        }
        script.m_expression = Replace(sentence);
	m_variables.add(script);
        m_lines.add(script);
        if (variable != null)
        {
            script.m_type = 1;
            CVariable pfunc = new CVariable(this);
            pfunc.m_type = 2;
            pfunc.m_name = String.format("%s%d", VARIABLE, m_variables.size());
            int field = CTable.NULLFIELD;
            if (sentence.startsWith(VARIABLE))
            {
                boolean isNum =  IsNumeric(sentence.replace(VARIABLE, ""));
                if (isNum)
                {
                    for (CVariable var : m_variables)
                    {
                        if (var.m_name.equals(sentence) && var.m_field != CTable.NULLFIELD)
                        {
                            field = var.m_field;
                            break;
                        }
                    }
                }
            }
            if (field == CTable.NULLFIELD)
            {
                field = CTable.GetAutoField();
                m_dataSource.AddColumn(field);
            }
            else
            {
                script.m_type = 0;
            }
            pfunc.m_field = field;
            pfunc.m_expression = variable;
            pfunc.m_splitExpression = SplitExpression(variable);
            m_variables.add(pfunc);
            m_mainVariables.put(variable, field);
            script.m_field = field;
        }
        if (followParameters != null && followParameters.length() > 0)
        {
            String newLine = null;
            if (followParameters.indexOf("COLORSTICK") != -1)
            {
                newLine = "STICKLINE(1" + VARIABLE2 + variable + VARIABLE2 + "0" + VARIABLE2 + "1" + VARIABLE2 + "2" + VARIABLE2 + "DRAWTITLE)";
            }
            else if (followParameters.indexOf("CIRCLEDOT") != -1)
            {
                newLine = "DRAWICON(1" + VARIABLE2 + variable + VARIABLE2 + "CIRCLEDOT" + VARIABLE2 + "DRAWTITLE)";
            }
            else if (followParameters.indexOf("POINTDOT") != -1)
            {
                newLine = "DRAWICON(1" + VARIABLE2 + variable + VARIABLE2 + "POINTDOT" + VARIABLE2 + "DRAWTITLE)";
            }
            else
            {
                newLine = "POLYLINE(1" + VARIABLE2 + variable + VARIABLE2 + followParameters + VARIABLE2 + "DRAWTITLE)";
            }
            AnalysisScriptLine(newLine);
        }
        script.m_splitExpression = SplitExpression(script.m_expression);
    }

    private double Calculate(CMathElement[] expr)
    {
        CMathElement[] optr = new CMathElement[expr.length];
        int optrLength = 1;
        CMathElement exp = new CMathElement();
        exp.m_type = 3;
        optr[0] = exp;
        CMathElement[] opnd = new CMathElement[expr.length];
        int opndLength = 0;
        int idx = 0;
        CMathElement right = null;
        while (idx < expr.length && (expr[idx].m_type != 3 || optr[optrLength - 1].m_type != 3))
        {
            CMathElement Q2 = expr[idx];
            if (Q2 .m_type != 0 && Q2 .m_type != 3)
            {
                opnd[opndLength] = Q2 ;
                opndLength++;
                idx++;
            }
            else
            {
                CMathElement Q1 = optr[optrLength - 1];
                int precede = -1;
                if (Q2.m_type == 3)
                {
                    if (Q1.m_type == 3)
                    {
                        precede = 3;
                    }
                    else
                    {
                        precede = 4;
                    }
                }
                else
                {
					int q1Value = (int)Q1.m_value;
					int q2Value = (int)Q2.m_value;
                    switch (q2Value)
                    {
                        case 3:
                        case 0:
                        case 13:
                        case 4:
                        case 7:
                        case 1:
                        case 11:
                        case 5:
                        case 8:
                        case 10:
                        case 14:
                            if (Q1.m_type == 3 || (Q1.m_type == 0 && q1Value  == 6))
                            {
                                precede = 7;
                            }
                            else
                            {
                                precede = 4;
                            }
                            break;
                        case 9:
                        case 2:
                            if (Q1.m_type == 0 && (q1Value  == 9 || q1Value  == 2 || q1Value  == 12))
                            {
                                precede = 4;
                            }
                            else
                            {
                                precede = 7;
                            }
                            break;
                        case 6:
                            precede = 7;
                            break;
                        case 12:
                            if (Q1.m_type == 0 && q1Value == 6)
                            {
                                precede = 3;
                            }
                            else
                            {
                                precede = 4;
                            }
                            break;
                    }
                }
                switch (precede)
                {
                    case 7:
                        optr[optrLength] = Q2;
                        optrLength++;
                        idx++;
                        break;
                    case 3:
                        optrLength--;
                        idx++;
                        break;
                    case 4:
                        if (opndLength == 0) return 0;
                        int op = (int)Q1.m_value;
                        optrLength--;
                        double opnd1 = 0, opnd2 = 0;
                        CMathElement left = opnd[opndLength - 1];
                        if (left .m_type == 2)
                        {
                            opnd2 = GetValue(left.m_var);
                        }
                        else
                        {
                            opnd2 = left.m_value;
                        }
                        if (opndLength > 1)
                        {
                            right = opnd[opndLength - 2];
                            if (right .m_type == 2)
                            {
                                opnd1 = GetValue(right.m_var);
                            }
                            else
                            {
                                opnd1 = right.m_value;
                            }
                            opndLength-=2;
                        }
                        else
                        {
                            opndLength--;
                        }
                        double result = 0;
                        switch (op)
                        {
                            case 0: result = opnd1 + opnd2; break;
                            case 13: result = opnd1 - opnd2; break;
                            case 9: result = opnd1 * opnd2; break;
                            case 2:
                            {
                                if (opnd2 == 0)
                                {
                                    result = 0;
                                }
                                else
                                {
                                    result = opnd1 / opnd2;
                                }
                                break;
                            }
                            case 14:
                            {
                                if (opnd2 == 0)
                                {
                                    result = 0;
                                }
                                else
                                {
                                    result = opnd1 % opnd2;
                                }
                                break;
                            }
                            case 5: result = (opnd1 >= opnd2 ? 1 : 0); break;
                            case 8: result = (opnd1 <= opnd2 ? 1 : 0); break;
                            case 10:
                            {
                                if ((left.m_var != null && left.m_var.m_functionID == -2) || (right != null && right.m_var != null && right.m_var.m_functionID == -2))
                                {
                                    if (right != null && left.m_var != null && right.m_var != null)
                                    {
										if (!GetText(left.m_var).equals(GetText(right.m_var)))
										{
											result = 1;
										}
									}
                                }
                                else
                                {
                                    result = (opnd1 != opnd2 ? 1 : 0);
                                }
                                break;
                            }
                        case 3:
                            {

                                if ((left.m_var != null && left.m_var.m_functionID == -2) || (right != null && right.m_var != null && right.m_var.m_functionID == -2))
                                {
                                    if (right != null && left.m_var != null && right.m_var != null)
                                    {
										if (GetText(left.m_var).equals(GetText(right.m_var)))
										{
											result = 1;
										}
									}
                                }
                                else
                                {
                                    result = (opnd1 == opnd2 ? 1 : 0);
                                }
                                break;
                            }
                            case 4: result = (opnd1 > opnd2 ? 1 : 0); break;
                            case 7: result = (opnd1 < opnd2 ? 1 : 0); break;
                            case 1:
                                if (opnd1 == 1 && opnd2 == 1) result = 1;
                                else result = 0;
                                break;
                            case 11:
                                if (opnd1 == 1 || opnd2 == 1) result = 1;
                                else result = 0;
                                break;
                            default: result = 0;
                        }
                        if(m_break > 0)
                        {
                            return result;
                        }
						else
						{
							CMathElement expression = new CMathElement();
							expression.m_type = 1;
							expression.m_value = result;
							opnd[opndLength] = expression;
							opndLength++;
						}
                        break;
                }
            }
        }
        if (opndLength > 0)
        {
            CMathElement rlast = opnd[opndLength - 1];
            if (rlast.m_type == 2)
            {
                return GetValue(rlast.m_var);
            }
            else
            {
                return rlast.m_value;
            }
        }
        return 0;
    }

    private double CallFunction(CVariable var)
    {
        switch (var.m_functionID)
        {
            case 0: return CURRBARSCOUNT(var);
            case 1: return BARSCOUNT(var);
            case 2: return DRAWKLINE(var);
            case 3: return STICKLINE(var);
            case 4: return VALUEWHEN(var);
            case 5: return BARSLAST(var);
            case 6: return DOWNNDAY(var);
            case 7: return DRAWICON(var);
            case 8: return DRAWNULL(var);
            case 9: return FUNCTION(var);
            case 10: return FUNCVAR(var);
            case 11: return DRAWTEXT(var);
            case 12: return POLYLINE(var);
            case 13: return BETWEEN(var);
            case 14: return CEILING(var);
            case 15: return EXPMEMA(var);
            case 16: return HHVBARS(var);
            case 17: return INTPART(var);
            case 18: return LLVBARS(var);
            case 19: return DOTIMES(var);
            case 20: return DOWHILE(var);
            case 21: return CONTINUE(var);
            case 22: return RETURN(var);
            case 23: return REVERSE(var);
            case 24: return AVEDEV(var);
            case 25: return MINUTE(var);
            case 26: return SQUARE(var);
            case 27: return UPNDAY(var);
            case 28: return DELETE(var);
            case 29: return COUNT(var);
            case 30: return CROSS(var);
            case 31: return EVERY(var);
            case 32: return EXIST(var);
            case 33: return EMA(var);
            case 34: return FLOOR(var);
            case 35: return MONTH(var);
            case 36: return ROUND(var);
            case 37: return TIME2(var);
            case 38: return WHILE(var);
            case 39: return BREAK(var);
            case 40: return CHUNK(var);
            case 41: return ACOS(var);
            case 42: return ASIN(var);
            case 43: return ATAN(var);
            case 44: return DATE(var);
            case 45: return HOUR(var);
            case 46: return LAST(var);
            case 47: return MEMA(var);
            case 48: return NDAY(var);
            case 49: return RAND(var);
            case 50: return SIGN(var);
            case 51: return SQRT(var);
            case 52: return TIME(var);
            case 53: return YEAR(var);
            case 54: return ABS(var);
            case 55: return AMA(var);
            case 56: return COS(var);
            case 57: return DAY(var);
            case 58: return DMA(var);
            case 59: return EMA(var);
            case 60: return EXP(var);
            case 61: return HHV(var);
            case 62: return IF(var);
            case 63: return IFN(var);
            case 64: return LLV(var);
            case 65: return LOG(var);
            case 66: return MAX(var);
            case 67: return MIN(var);
            case 68: return MOD(var);
            case 69: return NOT(var);
            case 70: return POW(var);
            case 71: return SIN(var);
            case 72: return SMA(var);
            case 73: return STD(var);
            case 74: return SUM(var);
            case 75: return TAN(var);
            case 76: return REF(var);
            case 77: return SAR(var);
            case 78: return FOR(var);
            case 79: return GET(var);
            case 80: return SET(var);
            case 81: return TMA(var);
            case 82: return VAR(var);
            case 83: return WMA(var);
            case 84: return ZIG(var);
            case 85: return IF(var);
            case 86: return MA(var);
            case 87: return STR_CONTACT(var);
            case 88: return STR_EQUALS(var);
            case 89: return STR_FIND(var);
            case 90: return STR_FINDLAST(var);
            case 91: return STR_LENGTH(var);
            case 92: return STR_SUBSTR(var);
            case 93: return STR_REPLACE(var);
            case 94: return STR_SPLIT(var);
            case 95: return STR_TOLOWER(var);
            case 96: return STR_TOUPPER(var);
            case 97: return LIST_ADD(var);
            case 98: return LIST_CLEAR(var);
            case 99: return LIST_GET(var);
            case 100: return LIST_INSERT(var);
            case 101: return LIST_REMOVE(var);
            case 102: return LIST_SIZE(var);
            case 103: return MAP_CLEAR(var);
            case 104: return MAP_CONTAINSKEY(var);
            case 105: return MAP_GET(var);
            case 106: return MAP_GETKEYS(var);
            case 107: return MAP_REMOVE(var);
            case 108: return MAP_SET(var);
            case 109: return MAP_SIZE(var);
            default:
                if (m_functionsMap.containsKey(var.m_functionID))
                {
                    return m_functionsMap.get(var.m_functionID).OnCalculate(var);
                }
                return 0;
        }
    }

    @Override
    public void Clear()
    {
        synchronized(this)
        {
            if (m_div != null)
            {
                ArrayList<BaseShape> shapes = GetShapes();
                for (BaseShape shape : shapes)
                {
                    m_div.RemoveShape(shape);
                    m_div.GetTitleBar().GetTitles().clear();
                    shape.Dispose();
                }
                if (shapes != null)
                {
                    shapes.clear();
                }
            }
            for (CVariable var : m_variables)
            {
                if (var.m_field >= 10000)
                {
                    m_dataSource.RemoveColumn(var.m_field);
                }
			    if (var.m_tempFields != null)
                {
                    for (int i = 0; i < var.m_tempFields.length; i++)
                    {
                        if (var.m_tempFields[i] >= 10000)
                        {
                            m_dataSource.RemoveColumn(var.m_tempFields[i]);
                        }
                    }
                }
            }
            m_lines.clear();
            m_variables.clear();
            m_mainVariables.clear();
            m_defineParams.clear();
            m_tempFunctions.clear();
            DeleteTempVars();
            m_tempVariables.clear();
        }
    }
    
    public CVar CopyTempVar(CVar var) 
    {
        CVar newVar = new CVar();
        newVar.m_type = var.m_type;
        newVar.m_str = var.m_str;
        newVar.m_num = var.m_num;
        return newVar;
    }

    private void DeleteTempVars()
    {
        while(m_tempVars.size() > 0)
        {
            ArrayList<Integer> removeIDs = new ArrayList<Integer>();
            for(Map.Entry<Integer, CVar> entry : m_tempVars.entrySet())
            {
                removeIDs.add(entry.getKey());
            }
            int removeIDsSize = removeIDs.size();
            for(int i = 0; i < removeIDsSize; i++)
            {
                int removeID = removeIDs.get(i);
                if(m_tempVars.containsKey(removeID))
                {
                    CVar cVar = m_tempVars.get(removeID);
                    if(cVar.m_parent != null)
                    {
                        m_tempVars.put(removeID, cVar.m_parent);
                    }
                    else
                    {
                        m_tempVars.remove(removeID);
                    }
                    cVar.Dispose();
                }
            }
            removeIDs.clear();
        }
    }

    private void DeleteTempVars(CVariable var)
    {
        if (var.m_parameters != null)
        {
            int pLen = var.m_parameters.length;
            if (pLen > 0)
            {
                for (int i = 0; i < pLen; i++)
                {
                    CVariable parameter = var.m_parameters[i];
                    if (parameter.m_splitExpression != null && parameter.m_splitExpression.length > 0)
                    {
                        CVariable subVar = parameter.m_splitExpression[0].m_var;
                        if (subVar != null && (subVar.m_functionID == FUNCTIONID_FUNCVAR || subVar.m_functionID == FUNCTIONID_VAR))
                        {
                            int sunLen = subVar.m_parameters.length;
                            for (int j = 0; j < sunLen; j++)
                            {
                                if (j % 2 == 0)
                                {
                                    CVariable sunVar = subVar.m_parameters[j];
                                    int id = sunVar.m_field;
									if (sunVar.m_expression.indexOf("[REF]") == 0)
                                    {
                                        int variablesSize = m_variables.size();
                                        for (int k = 0; k < variablesSize; k++)
                                        {
                                            CVariable variable = m_variables.get(k);
                                            if (variable.m_expression == sunVar.m_expression)
                                            {
                                                variable.m_field = id;
                                            }
                                        }
                                    }
									else
									{
										if (m_tempVars.containsKey(id))
										{
											CVar cVar = m_tempVars.get(id);
											if (cVar.m_parent != null)
											{
												m_tempVars.put(id, cVar.m_parent);
											}
											else
											{
												m_tempVars.remove(id);
											}
											cVar.Dispose();
										}
									}
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static CIndicator CreateIndicator()
    {
        CIndicator indicator = new CIndicatorEx2();
        return indicator;
    }

    @Override
    public void Dispose()
    {
        if (!m_isDisposed)
        {
            Clear();
            m_functionsMap.clear();
            m_functions.clear();
            m_isDisposed = true;
        }
    }

    private long GetColor(String strColor)
    {
        if (strColor.equals("COLORRED"))
        {
            return COLOR.ARGB(255, 0, 0);
        }
        else if (strColor.equals("COLORGREEN"))
        {
            return COLOR.ARGB(0, 255, 0);
        }
        else if (strColor.equals("COLORBLUE"))
        {
            return COLOR.ARGB(0, 0, 255);
        }
        else if (strColor.equals("COLORMAGENTA"))
        {
            return COLOR.ARGB(255, 0, 255);
        }
        else if (strColor.equals("COLORYELLOW"))
        {
            return COLOR.ARGB(255, 255, 0);
        }
        else if (strColor.equals("COLORLIGHTGREY"))
        {
            return COLOR.ARGB(211, 211, 211);
        }
        else if (strColor.equals("COLORLIGHTRED"))
        {
            return COLOR.ARGB(255, 82, 82);
        }
        else if (strColor.equals("COLORLIGHTGREEN"))
        {
            return COLOR.ARGB(144, 238, 144);
        }
        else if (strColor.equals("COLORLIGHTBLUE"))
        {
            return COLOR.ARGB(173, 216, 230);
        }
        else if (strColor.equals("COLORBLACK"))
        {
            return COLOR.ARGB(0, 0, 0);
        }
        else if (strColor.equals("COLORWHITE"))
        {
            return COLOR.ARGB(255, 255, 255);
        }
        else if (strColor.equals("COLORCYAN"))
        {
            return COLOR.ARGB(0, 255, 255);
        }
        else if (strColor.equals("COLORAUTO"))
        {
            int lineCount = 0;
            long lineColor = COLOR.EMPTY;
            for (BaseShape shape : GetShapes())
            {
                if (shape instanceof PolylineShape)
                {
                    lineCount++;
                }
            }
            int systemColorsSize = m_systemColors.size();
            if (systemColorsSize > 0)
            {
                lineColor = m_systemColors.get((lineCount) % systemColorsSize);
            }
            return lineColor;
        }
        else
        {
            //
            return 0;
        }
    }

    private LPDATA GetDatas(int fieldIndex, int mafieldIndex, int index, int n)
    {
        LPDATA math_struct = new LPDATA();
        math_struct.mode = 1;
        if (index >= 0)
        {
            double value = m_dataSource.Get3(index, mafieldIndex);
            if (!Double.isNaN(value))
            {
                math_struct.lastvalue = value;
                if (index >= n - 1)
                {
                    double nValue = m_dataSource.Get3(index + 1 - n, fieldIndex);
                    if (!Double.isNaN(nValue))
                    {
                        math_struct.first_value = nValue;
                    }
                }
            }
            else
            {
                math_struct.mode = 0;
                ArrayList<Double> list = new ArrayList<Double>();
                int start = index - n + 2;
                if (start < 0)
                {
                    start = 0;
                }
                for (int i = start; i <= index; i++)
                {
                    double lValue = m_dataSource.Get3(i, fieldIndex);
                    if (!Double.isNaN(lValue))
                    {
                        math_struct.sum += lValue;
                    }
                }
            }
        }
        return math_struct;
    }

    @Override
    public ArrayList<CFunction> GetFunctions()
    {
		ArrayList<CFunction> functions = new ArrayList<CFunction>();
		for (Map.Entry<String,CFunction> entry: m_functions.entrySet())
		{
			functions.add(entry.getValue());
		}
        return functions;
    }

    private float GetLineWidth(String strLine)
    {
        float lineWidth = 1;
        if (strLine.length() > 9)
        {
            lineWidth = CStr.ConvertStrToFloat(strLine.substring(9));
        }
        return lineWidth;
    }

    private int GetOperator(String op)
    {
        if (op.equals(">="))
        {
            return 5;
        }
        else if (op.equals("<="))
        {
            return 8;
        }
        else if (op.equals("<>") || op.equals("!"))
        {
            return 10;
        }
        else if (op.equals("+"))
        {
            return 0;
        }
        else if (op.equals(VARIABLE3))
        {
            return 13;
        }
        else if (op.equals("*"))
        {
            return 9;
        }
        else if (op.equals("/"))
        {
            return 2;
        }
        else if (op.equals("("))
        {
            return 6;
        }
        else if (op.equals(")"))
        {
            return 12;
        }
        else if (op.equals("="))
        {
            return 3;
        }
        else if (op.equals(">"))
        {
            return 4;
        }
        else if (op.equals("<"))
        {
            return 7;
        }
        else if (op.equals("&"))
        {
            return 1;
        }
        else if (op.equals("|"))
        {
            return 11;
        }
        else if(op.equals("%"))
        {
            return 14;
        }
        return -1;
    }

    private int GetMiddleScript(String script, ArrayList<String> lines)
    {
        script = script.replace(" AND ", "&").replace(" OR ", "|");
        String line = "";
        boolean isstr = false;
        char lh = '0';
        boolean isComment = false;
        boolean functionBegin = false;
        int kh = 0;
        boolean isReturn = false, isVar = false, isNewParam = false, isSet = false; 
        char[] charArray  = script.toCharArray();
        for (char ch : charArray)
        {
            if((int)ch == 65279)
            {
                continue;
            }
            if (ch == '\'')
            {
                isstr = !isstr;
            }
            if (!isstr)
            {
				if (ch == '{')
                {
                    int lineLength = line.length();
                    if (lineLength == 0)
                    {
                        isComment = true;
                    }
                    else
                    {
                        if(!isComment)
                        {
                            kh++;
                            if (functionBegin && kh == 1)
                            {
                                line += "(";
                            }
                            else
                            {
                                if(line.lastIndexOf(")") == lineLength - 1)
                                {
                                    line = line.substring(0, lineLength - 1) + VARIABLE2 + "CHUNK(";
                                }
                                else if(line.lastIndexOf("))" + VARIABLE2 +"ELSE") == lineLength - 7)
                                {
                                    line = line.substring(0, lineLength - 6) + VARIABLE2 + "CHUNK(";
                                }
                            }
                        }
                    }
                }
                else if (ch == '}')
                {
                    if (isComment)
                    {
                        isComment = false;
                    }
                    else
                    {
                        kh--;
                        if (functionBegin && kh == 0)
                        {
                            int lineLength = line.length();
                            if (lineLength > 0)
                            {
                                if (line.substring(lineLength - 1).equals(VARIABLE2))
                                {
                                    line = line.substring(0, lineLength - 1);
                                }
                            }
                            line += ")";
                            lines.add(line);
                            functionBegin = false;
                            line = "";
                        }
                        else
                        {
                            if (kh == 0)
                            {
                                line += "))";
                                lines.add(line);
								line = "";
                            }
                            else
                            {
                                line += "))" + VARIABLE2;
                            }
                        }
                    }
                }
                else if (ch == ' ')
                {
                    int lineLength = line.length();
                    if (line.equals("CONST"))
                    {
                        line = "CONST ";
                    }
                    else if (line.equals("FUNCTION"))
                    {
                        line = "FUNCTION ";
                        functionBegin = true;
                    }
                    else if (!isReturn && (line.lastIndexOf("RETURN") == lineLength - 6)) 
                    {
                        if (lineLength == 6 || (line.lastIndexOf(")RETURN") == lineLength - 7 
                        || line.lastIndexOf("(RETURN") == lineLength - 7  
                        || line.lastIndexOf(VARIABLE2 + "RETURN") == lineLength - 7))
                        {
                            line += "("; 
                            isReturn = true;
                        }
                    }
                    else if (!isVar && line.lastIndexOf("VAR") == lineLength - 3) 
                    {
                        if (lineLength == 3 || (line.lastIndexOf(")VAR") == lineLength - 4 
                         || line.lastIndexOf("(VAR") == lineLength - 4 
                         || line.lastIndexOf(VARIABLE2 + "VAR") == lineLength - 4))
                        {
                            line += "("; 
                            isVar = true; 
                            isNewParam = true; 
                        }
                    }
                    else if (!isSet && line.lastIndexOf("SET") == lineLength - 3) 
                    {
                        if (lineLength == 3 || (line.lastIndexOf(")SET") == lineLength - 4 
                        || line.lastIndexOf("(SET") == lineLength - 4 
                        || line.lastIndexOf(VARIABLE2 + "SET") == lineLength - 4))
                        {
                            line = line.substring(0, lineLength - 3) + "SET("; 
                            isSet = true; 
                            isNewParam = true; 
                        }
                    }
                    else
                    {
                            continue;
                    }
                }
                else if (ch != '\t' && ch != '\r' && ch != '\n')
                {
                    if (!isComment)
                    {
                        if (ch == '&')
                        {
                            if (lh != '&')
                            {
                                line += ch;
                            }
                        }
                        else if (ch == '|')
                        {
                            if (lh != '|')
                            {
                                line += ch;
                            }
                        }
						else if (ch == '=')
                        {
                            if (isVar && isNewParam) 
                                {
                                    isNewParam = false; 
                                    line += VARIABLE2; 
                                }
                                else if (isSet && isNewParam)
                                {
                                    isNewParam = false; 
                                    line += VARIABLE2; 
                                }
                                else if (lh != '=' && lh != '!')
                            {
                                line += ch;
                            }
                        }
			else if (ch == '-')
                        {
                            String strLh = String.valueOf(lh);
                            if (!strLh.equals(VARIABLE2) && GetOperator(strLh) != -1
                                    && !strLh.equals(")"))
                            {
                                line += ch;
                            }
                            else
                            {
                                line += VARIABLE3;
                                lh = VARIABLE3.charAt(0);
                                continue;
                            }
                        }
                        else if (ch == ',')
                        {
                            isNewParam = true; 
                            line += VARIABLE2;
                        }
						else if (ch == ';')
                        {
                            if (isReturn) 
                            {
                                line += ")"; 
                                isReturn = false; 
                            }
                            else if (isVar)
                            {
                                line += ")"; 
                                isVar = false; 
                            }
                            else if (isSet)
                            {
                                line += ")"; 
                                isSet = false;
                            }
                            else
                            {
                                int lineLength = line.length();
                                if (line.lastIndexOf("BREAK") == lineLength - 5) 
                                {
                                    if (line.lastIndexOf(")BREAK") == lineLength - 6 
                                   || line.lastIndexOf("(BREAK") == lineLength - 6
                                   || line.lastIndexOf(VARIABLE2 + "BREAK") == lineLength - 6) 
                                    {
                                        line += "()"; 
                                    }
                                }
                                else if (line.lastIndexOf("CONTINUE") == lineLength - 8)
                                {
                                    if (line.lastIndexOf(")CONTINUE") == lineLength - 9 
                                    || line.lastIndexOf("(CONTINUE") == lineLength - 9 
                                    || line.lastIndexOf(VARIABLE2 + "CONTINUE") == lineLength - 9) 
                                    {
                                        line += "()"; 
                                    }
                                }
                            }
                            if (kh > 0)
                            {
                                line += VARIABLE2;
                            }
                            else
                            {
                                lines.add(line);
                                line = "";
                            }
                        }
                        else if(ch == '(')
                        {
                            int lineLength = line.length();
                            if (kh > 0 && line.lastIndexOf("))"+ VARIABLE2 +"ELSEIF") == lineLength - 9)
                            {
                                line = line.substring(0, lineLength - 9) + ")" + VARIABLE2;
                            }
                            else
                            {
                                line += "(";
                            }
                        }
                        else
                        {
                            String newStr = String.valueOf(ch).toUpperCase();
                            line += newStr;
                        }
                    }
                }
            }
            else
            {
                line += ch;
            }
            lh = ch;
        }
        return 0;
    }

    @Override
    public ArrayList<BaseShape> GetShapes()
    {
        ArrayList<BaseShape> shapes = new ArrayList<BaseShape>();
        for (CVariable var : m_variables)
        {
            if (var.m_barShape != null)
            {
                shapes.add(var.m_barShape);
            }
            if (var.m_candleShape != null)
            {
                shapes.add(var.m_candleShape);
            }
            if (var.m_polylineShape != null)
            {
                shapes.add(var.m_polylineShape);
            }
            if (var.m_textShape != null)
            {
                shapes.add(var.m_textShape);
            }
        }
        return shapes;
    }

    @Override
    public String GetText(CVariable var)
    {
        if(var.m_expression.length() > 0 && var.m_expression.startsWith("\'"))
        {
            return var.m_expression.substring(1, var.m_expression.length() - 1);
        }
        else
        {
            if (m_tempVars.containsKey(var.m_field))
            {
                CVar cVar = m_tempVars.get(var.m_field);
                return cVar.GetText(this, var);
            }
            else
            {
                return CStr.ConvertDoubleToStr(GetValue(var));
            }
        }
    }

    @Override
    public double GetValue(CVariable var)
    {
        switch (var.m_type) {
            case 0:
                return CallFunction(var);
            case 1:
                if (m_tempVars.containsKey(var.m_field))
                {
                    CVar cVar = m_tempVars.get(var.m_field);
                    return cVar.GetValue(this, var);
                }
                else
                {
                    if(var.m_expression.length() > 0 && var.m_expression.startsWith("\'"))
                    {
                        return CStr.ConvertStrToDouble(var.m_expression.substring(1, var.m_expression.length() - 1));
                    }
                    else
                    {
						if(var.m_splitExpression != null)
						{
							return Calculate(var.m_splitExpression);
						}
						else
						{
							return 0;
						}
                    }
                }
            case 2:
                return m_dataSource.Get3(m_index, var.m_fieldIndex);
            case 3:
                return var.m_value;
            default:
                return 0;
        }
    }

    @Override
    public CVariable GetVariable(String name)
    {
        if (m_tempVariables.containsKey(name))
        {
            return m_tempVariables.get(name);
        }
        else
        {
            return null;
        }
    }

    public static boolean IsNumeric(String str)
    {
        for (int i = 0; i < str.length(); i++)
        {
            if (!Character.isDigit(str.charAt(i)))
            {
                if(str.charAt(i) != '.')
                {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void OnCalculate(int index)
    {
        synchronized(this)
        {
            if (m_lines != null && m_lines.size() > 0)
            {
                for (CVariable sentence : m_lines)
                {
                    if (sentence.m_field != CTable.NULLFIELD)
                    {
                        sentence.m_fieldIndex = m_dataSource.GetColumnIndex(sentence.m_field);
                    }
                }
                for (CVariable var : m_variables)
                {
                    if (var.m_field != CTable.NULLFIELD)
                    {
                        var.m_fieldIndex = m_dataSource.GetColumnIndex(var.m_field);
                    }
                    if (var.m_tempFields != null)
                    {
                        for (int i = 0; i < var.m_tempFields.length; i++)
                        {
                            var.m_tempFieldsIndex[i] = m_dataSource.GetColumnIndex(var.m_tempFields[i]);
                        }
                    }
                }
                for (int i = index; i < m_dataSource.GetRowsCount(); i++)
                {
					m_break = 0;
                    m_index = i;
                    int lineSize = m_lines.size();
                    for (int j = 0; j < lineSize; j++)
                    {
                        CVariable sentence = m_lines.get(j);
                        if (sentence.m_funcName == null || (sentence.m_funcName != null && sentence.m_line != j))
                        {
                            double value = Calculate(sentence.m_splitExpression);
                            if (sentence.m_type == 1 && sentence.m_field != CTable.NULLFIELD)
                            {
                                m_dataSource.Set3(i, sentence.m_fieldIndex, value);
                            }
                        }
						if(m_break == 1)
						{
							m_break = 0;
						}
                    }
                }
            }
        }
    }

    @Override
    public void RemoveFunction(CFunction function)
    {
        m_functions.remove(function.m_name);
        m_functionsMap.remove(function.m_ID);
    }

    private String Replace(String parameter)
    {
        String[] splitParameters = SplitExpression2(parameter);
        for (int p = 0; p < splitParameters.length; p++)
        {
            String str = splitParameters[p];
            if (m_defineParams.containsKey(str))
            {
                splitParameters[p] = m_defineParams.get(str).toString();
            }
            else
            {
                for (CVariable varaible : m_variables)
                {
                    if (varaible.m_type == 2 && varaible.m_expression.equals(str))
                    {
                        splitParameters[p] = varaible.m_name;
                        break;
                    }
                }
            }
        }
        String newParameter = "";
        for (int p = 0; p < splitParameters.length - 1; p++)
        {
            newParameter += splitParameters[p];
        }
        return newParameter;
    }

    @Override
    public void SetSourceField(String key, int value)
    {
        CVariable pfunc = new CVariable(this);
        pfunc.m_type = 2;
        pfunc.m_name = String.format("%s%d", VARIABLE, m_variables.size());
        pfunc.m_expression = key;
        pfunc.m_splitExpression = SplitExpression(key);
        pfunc.m_field = value;
	    int columnIndex = m_dataSource.GetColumnIndex(value);
        if (columnIndex == -1)
        {
            m_dataSource.AddColumn(value);
        }
        m_variables.add(pfunc);
    }

    @Override
    public void SetSourceValue(String key, double value)
    {
    }

    @Override
    public void SetVariable(CVariable variable, CVariable parameter)
    {
        int type = variable.m_type;
        int id = variable.m_field;
        switch (type)
        {
            case 2:
                double value = GetValue(parameter);
                m_dataSource.Set3(m_index, variable.m_fieldIndex, value);
                break;
            default:
                if (m_tempVars.containsKey(id))
                {
                    CVar cVar = m_tempVars.get(id);
                    cVar.SetValue(this, variable, parameter);
                    if (m_resultVar != null) 
                    {
                        cVar.m_str = m_resultVar.m_str; 
                        m_resultVar = null; 
                    }
                }
                else
                {
                    variable.m_value = GetValue(parameter);
                }
                break;
        }
    }

    private CMathElement[] SplitExpression(String expression)
    {
        CMathElement[] exprs = null;
        ArrayList<String> lstItem = new ArrayList<String>();
        int length = expression.length();
        String item = "";
        String ch = "";
		boolean isstr = false;
        while (length != 0)
        {
            ch = expression.substring(expression.length() - length, expression.length() - length + 1);
			if (ch.equals("\'"))
            {
                isstr = !isstr;
            }
            if (isstr || GetOperator(ch) == -1)
            {
                item += ch;
            }
            else
            {
                if (!item.equals(""))
                {
                    lstItem.add(item);
                }
                item = "";
                int nextIndex = expression.length() - length + 1;
                String chNext = "";
                if (nextIndex < expression.length() - 1)
                {
                    chNext = expression.substring(nextIndex, nextIndex + 1);
                }
                String unionText = ch + chNext;
                if (unionText.equals(">=") || unionText.equals("<=") || unionText.equals("<>"))
                {
                    lstItem.add(unionText);
                    length--;
                }
                else
                {
                    lstItem.add(ch);
                }
            }
            length--;
        }
        if (!item.equals(""))
        {
            lstItem.add(item);
        }
        exprs = new CMathElement[lstItem.size() + 1];
        int lstSize = lstItem.size();
        for (int i = 0; i < lstSize; i++)
        {
            CMathElement expr = new CMathElement();
            String strExpr = lstItem.get(i);
            int op = GetOperator(strExpr);
            if (op != -1)
            {
                expr.m_type = 0;
                expr.m_value = op;
            }
            else
            {
                boolean success = IsNumeric(strExpr);
                if (success)
                {
                    double value= Double.parseDouble(strExpr);
                    expr.m_type = 1;
                    expr.m_value = value;
                }
                else
                {
                    for (CVariable var : m_variables)
                    {
                        if (var.m_name.equals(strExpr) || var.m_expression.equals(strExpr))
                        {
                            expr.m_type = 2;
                            expr.m_var = var;
                            break;
                        }
                    }
                }
            }
            exprs[i] = expr;
        }
        CMathElement lExpr = new CMathElement();
        lExpr.m_type = 3;
        exprs[lstItem.size()] = lExpr;
        return exprs;
    }

    private String[] SplitExpression2(String expression)
    {
        String[] exprs = null;
        ArrayList<String> lstItem = new ArrayList<String>();
        int length = expression.length();
        String item = "";
        String ch = "";
		boolean isstr = false;
        while (length != 0)
        {
            ch = expression.substring(expression.length() - length, expression.length() - length + 1);
			if (ch.equals("\'"))
            {
                isstr = !isstr;
            }
            if (isstr || GetOperator(ch) == -1)
            {
                item += ch;
            }
            else
            {
                if (!item.equals(""))
                {
                    lstItem.add(item);
                }
                item = "";
                int nextIndex = expression.length() - length + 1;
                String chNext = "";
                if (nextIndex < expression.length() - 1)
                {
                    chNext = expression.substring(nextIndex, nextIndex + 1);
                }
                String unionText = ch + chNext;
                if (unionText.equals(">=") || unionText.equals("<=") || unionText.equals("<>"))
                {
                    lstItem.add(unionText);
                    length--;
                }
                else
                {
                    lstItem.add(ch);
                }
            }
            length--;
        }
        if (!item.equals(""))
        {
            lstItem.add(item);
        }
        exprs = new String[lstItem.size() + 1];
        for (int i = 0; i < lstItem.size(); i++)
        {
            exprs[i] = lstItem.get(i);
        }
        exprs[lstItem.size()] = "#";
        return exprs;
    }

    private double ABS(CVariable var)
    {
        return Math.abs(GetValue(var.m_parameters[0]));
    }

    private double AMA(CVariable var)
    {
        double close = GetValue(var.m_parameters[0]);
        double lastAma = 0;
        if (m_index > 0)
        {
            lastAma = m_dataSource.Get3(m_index - 1, var.m_fieldIndex);
        }
        double n = GetValue(var.m_parameters[1]);
        double ama = lastAma + n * (close - lastAma);
        m_dataSource.Set3(m_index, var.m_fieldIndex, ama);
        return ama;
    }

    private double ACOS(CVariable var)
    {
        return Math.acos(GetValue(var.m_parameters[0]));
    }

    private double ASIN(CVariable var)
    {
        return Math.asin(GetValue(var.m_parameters[0]));
    }

    private double ATAN(CVariable var)
    {
        return Math.atan(GetValue(var.m_parameters[0]));
    }

    private double AVEDEV(CVariable var)
    {
    	return 0;
    }

    private int BARSCOUNT(CVariable var)
    {
        return m_dataSource.GetRowsCount();
    }

    private int BARSLAST(CVariable var)
    {
        int result = 0;
        int tempIndex = m_index;
        for (int i = m_index; i >= 0; i--)
        {
            m_index = i;
            double value = GetValue(var.m_parameters[0]);
            if (value == 1)
            {
                break;
            }
            else
            {
                if (i == 0)
                {
                    result = 0;
                }
                else
                {
                    result++;
                }
            }
        }
        m_index = tempIndex;
        return result;
    }

    private int BETWEEN(CVariable var)
    {
        double value = GetValue(var.m_parameters[0]);
        double min = GetValue(var.m_parameters[1]);
        double max = GetValue(var.m_parameters[2]);
        int result = 0;
        if (value >= min && value <= max)
        {
            result = 1;
        }
        return result;
    }

    private int BREAK(CVariable var)
    {
        m_break = 2;
        return 0;
    }

    private double CEILING(CVariable var)
    {
        return Math.ceil(GetValue(var.m_parameters[0]));
    }

    private double CHUNK(CVariable var)
    {
        int pLen = var.m_parameters.length;
        if (pLen > 0)
        {
            for (int i = 0; m_break == 0 && i < pLen; i++)
            {
                GetValue(var.m_parameters[i]);
            }
        }
        DeleteTempVars(var);
        return 0;
    }

    private double COS(CVariable var)
    {
        return Math.cos(GetValue(var.m_parameters[0]));
    }

    private int CONTINUE(CVariable var)
    {
        m_break = 3;
        return 0;
    }

    private int COUNT(CVariable var)
    {
        int n = (int)GetValue(var.m_parameters[1]);
        if (n < 0)
        {
            n = m_dataSource.GetRowsCount();
        }
        else if (n > m_index + 1)
        {
            n = m_index + 1;
        }
        int tempIndex = m_index;
        int result = 0;
        for (int i = 0; i < n; i++)
        {
            if (GetValue(var.m_parameters[0]) > 0)
            {
                result++;
            }
            m_index--;
        }
        m_index = tempIndex;
        return result;
    }

    private int CROSS(CVariable var)
    {
        double x = GetValue(var.m_parameters[0]);
        double y = GetValue(var.m_parameters[1]);
        int result = 0;
        int tempIndex = m_index;
        m_index -= 1;
        if (m_index < 0)
        {
            m_index = 0;
        }
        double lastX = GetValue(var.m_parameters[0]);
        double lastY = GetValue(var.m_parameters[1]);
        m_index = tempIndex;
        if (x >= y && lastX < lastY)
        {
            result = 1;
        }
        return result;
    }

    private int CURRBARSCOUNT(CVariable var)
    {
        return m_index + 1;
    }

    private int DATE(CVariable var)
    {
    	return 0;
    }

    private int DAY(CVariable var)
    {
        return CStr.ConvertNumToDate(m_dataSource.GetXValue(m_index)).get(Calendar.DAY_OF_MONTH);
    }

    private int DELETE(CVariable var)
    {
        int pLen = var.m_parameters.length;
        for (int i = 0; i < pLen; i++)
        {
            CVariable name = var.m_parameters[i];
            int id = name.m_field;
            if (m_tempVars.containsKey(id))
            {
                CVar cVar = m_tempVars.get(id);
                if (cVar.m_parent != null)
                {
                    m_tempVars.put(id, cVar.m_parent);
                }
                else
                {
                    m_tempVars.remove(id);
                }
                cVar.Dispose();
            }
        }
        return 0;
    }

    private double DMA(CVariable var)
    {
        double close = GetValue(var.m_parameters[0]);
        double lastDma = 0;
        if (m_index > 0)
        {
            lastDma = m_dataSource.Get3(m_index - 1, var.m_fieldIndex);
        }
        double n = GetValue(var.m_parameters[1]);
        double result = n * close + (1 - n) * lastDma;
        m_dataSource.Set3(m_index, var.m_fieldIndex, result);
        return result;
    }

    private int DOTIMES(CVariable var)
    {
        int n = (int)GetValue(var.m_parameters[0]);
        int pLen = var.m_parameters.length;
        if (pLen > 1)
        {
            for (int i = 0; i < n; i++)
            {
                for (int j = 1; m_break == 0 && j < pLen; j++)
                {
                    GetValue(var.m_parameters[j]);
                }
                if (m_break > 0)
                {
                    if (m_break == 3)
                    {
                        m_break = 0;
                        DeleteTempVars(var);
                        continue;
                    }
                    else
                    {
                        m_break = 0;
                        DeleteTempVars(var);
                        break;
                    }
                }
                else
                {
                    DeleteTempVars(var);
                }
            }
        }
        return 0;
    }


    private int DOWHILE(CVariable var)
    {
        int pLen = var.m_parameters.length;
        if (pLen > 1)
        {
            while (true)
            {
                for (int i = 0; m_break == 0 && i < pLen - 1; i++)
                {
                    GetValue(var.m_parameters[i]);
                }
                if (m_break > 0)
                {
                    if (m_break == 3)
                    {
                        m_break = 0;
                        DeleteTempVars(var);
                        continue;
                    }
                    else
                    {
                        m_break = 0;
                        DeleteTempVars(var);
                        break;
                    }
                }
                double inLoop = GetValue(var.m_parameters[pLen - 1]);
                DeleteTempVars(var);
                if (inLoop <= 0)
                {
                    break;
                }
            }
        }
        return 0;
    }

    private int DOWNNDAY(CVariable var)
    {
        int n = (int)GetValue(var.m_parameters[0]);
        if (n < 0)
        {
            n = m_dataSource.GetRowsCount();
        }
        else if (n > m_index + 1)
        {
            n = m_index + 1;
        }
        int tempIndex = m_index;
        int result = 1;
        for (int i = 0; i < n; i++)
        {
            double right = GetValue(var.m_parameters[0]);
            m_index--;
            double left = m_index >= 0 ? GetValue(var.m_parameters[0]) : 0;
            if (right >= left)
            {
                result = 0;
                break;
            }
        }
        m_index = tempIndex;
        return result;
    }

    private double DRAWICON(CVariable var)
    {
        if (m_div != null)
        {
            CVariable cond = var.m_parameters[0];
            CVariable price = var.m_parameters[1];
            PolylineShape polylineShape = null;
            if (var.m_polylineShape == null)
            {
                String strColor = "COLORAUTO";
                String strStyle = "CIRCLEDOT";
                for (int i = 2; i < var.m_parameters.length; i++)
                {
                    String strParam = var.m_parameters[i].m_expression;
                    if (strParam.startsWith("COLOR"))
                    {
                        strColor = strParam;
                        break;
                    }
                    else if (strParam.equals("CIRCLEDOT"))
                    {
                        strStyle = strParam;
                        break;
                    }
                    else if (strParam.equals("POINTDOT"))
                    {
                        strStyle = strParam;
                        break;
                    }
                }
                if (var.m_expression.equals("DRAWICON"))
                {
                    strStyle = var.m_expression;
                }
                polylineShape = new PolylineShape();
                m_div.AddShape(polylineShape);
                long lineColor = GetColor(strColor);
                polylineShape.SetAttachVScale(m_attachVScale);
                polylineShape.SetFieldText(price.m_fieldText);
                polylineShape.SetColor(lineColor);
                polylineShape.SetStyle(PolylineStyle.Cycle);
                var.CreateTempFields(1);
                var.m_polylineShape = polylineShape;
            }
            else
            {
                polylineShape = var.m_polylineShape;
            }
            if (price.m_expression != null && price.m_expression.length() > 0)
            {
                if (polylineShape.GetFieldName() == CTable.NULLFIELD)
                {
                    if (price.m_field != CTable.NULLFIELD)
                    {
                        polylineShape.SetFieldName(price.m_field);
                    }
                    else
                    {
                        price.CreateTempFields(1);
                        polylineShape.SetFieldName(price.m_tempFields[0]);
                    }
                    for (int i = 2; i < var.m_parameters.length; i++)
                    {
                        String strParam = var.m_parameters[i].m_expression;
                        if (strParam.equals("DRAWTITLE"))
                        {
                            if (polylineShape.GetFieldText() != null)
                            {
                                m_div.GetTitleBar().GetTitles().add(new CTitle(polylineShape.GetFieldName(), polylineShape.GetFieldText(), polylineShape.GetColor(), 2, true));
                            }
                        }
                    }
                }
                if (price.m_tempFields != null)
                {
                    double value = GetValue(price);
                    m_dataSource.Set3(m_index, price.m_tempFieldsIndex[0], value);
                }
            }
            double dCond = 1;
            if (cond.m_expression != null && cond.m_expression.length() > 0 && !cond.m_expression.equals("1"))
            {
                dCond = GetValue(cond);
                if (dCond != 1)
                {
                    m_dataSource.Set3(m_index, var.m_tempFieldsIndex[0], -10000);
                }
                else
                {
                    m_dataSource.Set3(m_index, var.m_tempFieldsIndex[0], 1);
                }
            }
        }
        return 0;
    }

    private double DRAWKLINE(CVariable var)
    {
        if (m_div != null)
        {
            CVariable high = var.m_parameters[0];
            CVariable open = var.m_parameters[1];
            CVariable low = var.m_parameters[2];
            CVariable close = var.m_parameters[3];
            CandleShape candleShape = null;
            if (var.m_candleShape == null)
            {
                candleShape = new CandleShape();
                candleShape.SetHighFieldText(high.m_fieldText);
                candleShape.SetOpenFieldText(open.m_fieldText);
                candleShape.SetLowFieldText(low.m_fieldText);
                candleShape.SetCloseFieldText(close.m_fieldText);
                candleShape.SetAttachVScale(m_attachVScale);
                candleShape.SetStyle(CandleStyle.Rect);
                m_div.AddShape(candleShape);
                var.m_candleShape = candleShape;
            }
            else
            {
                candleShape = var.m_candleShape;
            }
            if (high.m_expression != null && high.m_expression.length() > 0)
            {
                if (candleShape.GetHighField() == CTable.NULLFIELD)
                {
                    if (high.m_field != CTable.NULLFIELD)
                    {
                        candleShape.SetHighField(high.m_field);
                    }
                    else
                    {
                        high.CreateTempFields(1);
                        candleShape.SetHighField(high.m_tempFields[0]);
                    }
                }
                if (high.m_tempFields != null)
                {
                    double value = GetValue(high);
                    m_dataSource.Set3(m_index, high.m_tempFieldsIndex[0], value);
                }
            }
            if (open.m_expression != null && open.m_expression.length() > 0)
            {
                if (open.m_field != CTable.NULLFIELD)
                {
                    candleShape.SetOpenField(open.m_field);
                }
                else
                {
                    open.CreateTempFields(1);
                    candleShape.SetOpenField(open.m_tempFields[0]);
                }
                if (open.m_tempFields != null)
                {
                    double value = GetValue(open);
                    m_dataSource.Set3(m_index, open.m_tempFieldsIndex[0], value);
                }
            }
            if (low.m_expression != null && low.m_expression.length() > 0)
            {
                if (low.m_field != CTable.NULLFIELD)
                {
                    candleShape.SetLowField(low.m_field);
                }
                else
                {
                    low.CreateTempFields(1);
                    candleShape.SetLowField(low.m_tempFields[0]);
                }
                if (low.m_tempFields != null)
                {
                    double value = GetValue(low);
                    m_dataSource.Set3(m_index, low.m_tempFieldsIndex[0], value);
                }
            }
            if (close.m_expression != null && close.m_expression.length() > 0)
            {
                if (close.m_field != CTable.NULLFIELD)
                {
                    candleShape.SetCloseField(close.m_field);
                }
                else
                {
                    close.CreateTempFields(1);
                    candleShape.SetCloseField(close.m_tempFields[0]);
                }
                if (close.m_tempFields != null)
                {
                    double value = GetValue(close);
                    m_dataSource.Set3(m_index, close.m_tempFieldsIndex[0], value);
                }
            }
        }
        return 0;
    }

    private double DRAWNULL(CVariable var)
    {
        return Double.NaN;
    }

    private double DRAWTEXT(CVariable var)
    {
        if (m_div != null)
        {
            CVariable cond = var.m_parameters[0];
            CVariable price = var.m_parameters[1];
            CVariable text = var.m_parameters[2];
            TextShape textShape = null;
            if (var.m_textShape == null)
            {
                textShape = new TextShape();
                textShape.SetAttachVScale(m_attachVScale);
                textShape.SetText(GetText(text));
                var.CreateTempFields(1);
                textShape.SetStyleField(var.m_tempFields[0]);
                String strColor = "COLORAUTO";
                for (int i = 3; i < var.m_parameters.length; i++)
                {
                    String strParam = var.m_parameters[i].m_expression;
                    if (strParam.startsWith("COLOR"))
                    {
                        strColor = strParam;
                        break;
                    }
                }
                if (!strColor.equals("COLORAUTO"))
                {
                    textShape.SetForeColor(GetColor(strColor));
                }
                m_div.AddShape(textShape);
                var.m_textShape = textShape;
            }
            else
            {
                textShape = var.m_textShape;
            }
            if (price.m_expression != null && price.m_expression.length() > 0)
            {
                if (textShape.GetFieldName() == CTable.NULLFIELD)
                {
                    if (price.m_field != CTable.NULLFIELD)
                    {
                        textShape.SetFieldName(price.m_field);
                    }
                    else
                    {
                        price.CreateTempFields(1);
                        textShape.SetFieldName(price.m_tempFields[0]);
                    }
                }
                if (price.m_tempFields != null)
                {
                    double value = GetValue(price);
                    m_dataSource.Set3(m_index, price.m_tempFieldsIndex[0], value);
                }
            }
            double dCond = 1;
            if (cond.m_expression != null && cond.m_expression.length() > 0 && !cond.m_expression.equals("1"))
            {
                dCond = GetValue(cond);
                if (dCond != 1)
                {
                    m_dataSource.Set3(m_index, var.m_tempFieldsIndex[0], -10000);
                }
                else
                {
                    m_dataSource.Set3(m_index, var.m_tempFieldsIndex[0], 0);
                }
            }
        }
        return 0;
    }

    private int EXIST(CVariable var)
    {
        int n = (int)GetValue(var.m_parameters[1]);
        if (n < 0)
        {
            n = m_dataSource.GetRowsCount();
        }
        else if (n > m_index + 1)
        {
            n = m_index + 1;
        }
        int tempIndex = m_index;
        int result = 0;
        for (int i = 0; i < n; i++)
        {
            if (GetValue(var.m_parameters[0]) > 0)
            {
                result = 1;
                break;
            }
            m_index--;
        }
        m_index = tempIndex;
        return result;
    }

    private double EMA(CVariable var)
    {
    	return 0;
    }

    private int EVERY(CVariable var)
    {
        int n = (int)GetValue(var.m_parameters[1]);
        if (n < 0)
        {
            n = m_dataSource.GetRowsCount();
        }
        else if (n > m_index + 1)
        {
            n = m_index + 1;
        }
        int tempIndex = m_index;
        int result = 1;
        for (int i = 0; i < n; i++)
        {
            if (GetValue(var.m_parameters[0]) <= 0)
            {
                result = 0;
                break;
            }
            m_index--;
        }
        m_index = tempIndex;
        return result;
    }

    private double EXPMEMA(CVariable var)
    {
    	return 0;
    }

    private double EXP(CVariable var)
    {
        return Math.exp(GetValue(var.m_parameters[0]));
    }

    private double FLOOR(CVariable var)
    {
        return Math.floor(GetValue(var.m_parameters[0]));
    }

    private int FOR(CVariable var)
    {
        int pLen = var.m_parameters.length;
        if (pLen > 3)
        {
            int start = (int)GetValue(var.m_parameters[0]);
            int end = (int)GetValue(var.m_parameters[1]);
            int step = (int)GetValue(var.m_parameters[2]);
            if (step > 0)
            {
                for (int i = start; i < end; i += step)
                {
                    for (int j = 3; j < pLen; j++)
                    {
                        GetValue(var.m_parameters[j]);
                        if (m_break != 0)
                        {
                            break;
                        }
                    }
                    if (m_break > 0)
                    {
                        if (m_break == 3)
                        {
                            m_break = 0;
                            DeleteTempVars(var);
                            continue;
                        }
                        else
                        {
                            m_break = 0;
                            DeleteTempVars(var);
                            break;
                        }
                    }
                    else
                    {
                        DeleteTempVars(var);
                    }
                }
            }
            else if (step < 0)
            {
                for (int i = start; i > end; i += step)
                {
                    for (int j = 3; j < pLen; j++)
                    {
                        if (m_break != 0)
                        {
                            break;
                        }
                    }
                    if (m_break > 0)
                    {
                        if (m_break == 3)
                        {
                            m_break = 0;
                            DeleteTempVars(var);
                            continue;
                        }
                        else
                        {
                            m_break = 0;
                            DeleteTempVars(var);
                            break;
                        }
                    }
                    else
                    {
                        DeleteTempVars(var);
                    }
                }
            }
        }
        return 0;
    }

    private double FUNCTION(CVariable var)
    {
        m_result = 0;
        if (var.m_parameters != null)
        {
            int pLen = var.m_parameters.length;
            if (pLen > 0)
            {
                for (int i = 0; i < pLen; i++)
                {
                    GetValue(var.m_parameters[i]);
                }
            }
        }
        String name = var.m_expression;
        if (m_tempFunctions.containsKey(name))
        {
            GetValue(m_tempFunctions.get(name));
        }
        if (m_break == 1)
        {
            m_break = 0;
        }
        double result = m_result;
        m_result = 0;
        DeleteTempVars(var);
        return result;
    }

    private double FUNCVAR(CVariable var)
    {
        double result = 0;
        int pLen = var.m_parameters.length;
        HashMap<CVar, Integer> cVars = new HashMap<CVar, Integer>();
        for (int i = 0; i < pLen; i++)
        {
            if (i % 2 == 0)
            {
                CVariable name = var.m_parameters[i];
                CVariable value = var.m_parameters[i + 1];
                int id = name.m_field;
                if (name.m_expression.indexOf("[REF]") == 0)
                    {
                        int variablesSize = m_variables.size();
                        for (int j = 0; j < variablesSize; j++)
                        {
                            CVariable variable = m_variables.get(j);
                            if (variable != name)
                            {
                                if (variable.m_field == id)
                                {
                                    variable.m_field = value.m_field;
                                }
                            }
                        }
                        continue;
                    }
				else
				{
					CVar newCVar = m_varFactory.CreateVar();
                    result = newCVar.OnCreate(this, name, value);
					if(newCVar.m_type == 1)
				    {
					   name.m_functionID = -2;
				    }
					cVars.put(newCVar, id);
				}
            }
        }
        for (Map.Entry<CVar,Integer> entry : cVars.entrySet())
        {
            int id = entry.getValue();
            CVar newCVar = entry.getKey();
            if (m_tempVars.containsKey(id))
            {
                CVar cVar = m_tempVars.get(id);
                newCVar.m_parent = cVar;
            }
            m_tempVars.put(id, newCVar);
        }
        cVars.clear();
        return result;
    }

    private double GET(CVariable var)
    {
        return GetValue(var.m_parameters[0]);
    }

    private double HHV(CVariable var)
    {
    	return 0;
    }

    private double HHVBARS(CVariable var)
    {
        int n = (int)GetValue(var.m_parameters[1]);
        CVariable cParam = var.m_parameters[0];
        int closeField = cParam.m_field;
        int closeFieldIndex = cParam.m_fieldIndex;
        if (closeFieldIndex == -1)
        {
            if (cParam.m_tempFields == null)
            {
                cParam.CreateTempFields(0);
            }
            closeField = cParam.m_tempFields[0];
            closeFieldIndex = cParam.m_tempFieldsIndex[0];
            double close = GetValue(cParam);
            m_dataSource.Set3(m_index, closeFieldIndex, close);
        }
        double[] higharray = m_dataSource.DATA_ARRAY(closeField, m_index, n);
        double result = 0;
        if (higharray.length > 0)
        {
            int mIndex = 0;
            double close = 0;
            for (int i = 0; i < higharray.length; i++)
            {
                if (i == 0)
                {
                    close = higharray[i];
                    mIndex = 0;
                }
                else
                {
                    if (higharray[i] > close)
                    {
                        close = higharray[i];
                        mIndex = i;
                    }
                }
            }
            result = higharray.length - mIndex - 1;
        }
        return result;
    }

    private int HOUR(CVariable var)
    {
        return CStr.ConvertNumToDate(m_dataSource.GetXValue(m_index)).get(Calendar.HOUR_OF_DAY);
    }


    private double IF(CVariable var)
    {
        double result = 0;
        int pLen = var.m_parameters.length;
        for (int i = 0; i < pLen; i++)
        {
            result = GetValue(var.m_parameters[i]);
            if (i % 2 == 0)
            {
                if (result == 0)
                {
                    i++;
                    continue;
                }
            }
            else
            {
                break;
            }
        }
        DeleteTempVars(var);
        return result;
    }

    private double IFN(CVariable var)
    {
        double result = 0;
        int pLen = var.m_parameters.length;
        for (int i = 0; i < pLen; i++)
        {
            result = GetValue(var.m_parameters[i]);
            if (i % 2 == 0)
            {
                if (result != 0)
                {
                    i++;
                    continue;
                }
            }
            else
            {
                break;
            }
        }
        DeleteTempVars(var);
        return result;
    }

    private double INTPART(CVariable var)
    {
        double result = GetValue(var.m_parameters[0]);
        if (result != 0)
        {
            int intResult = (int)result;
            double sub = Math.abs(result - intResult);
            if (sub >= 0.5)
            {
                if (result > 0)
                {
                    result = intResult - 1;
                }
                else
                {
                    result = intResult + 1;
                }
            }
            else
            {
                result = intResult;
            }
        }
        return result;
    }

    private int LAST(CVariable var)
    {
        int n = (int)GetValue(var.m_parameters[1]);
        int m = (int)GetValue(var.m_parameters[2]);
        if (n < 0)
        {
            n = m_dataSource.GetRowsCount();
        }
        else if (n > m_index + 1)
        {
            n = m_index + 1;
        }
        if (m < 0)
        {
            m = m_dataSource.GetRowsCount();
        }
        else if (m > m_index + 1)
        {
            m = m_index + 1;
        }
        int tempIndex = m_index;
        int result = 1;
        for (int i = m; i < n; i++)
        {
            m_index = tempIndex - m;
            if (GetValue(var.m_parameters[0]) <= 0)
            {
                result = 0;
                break;
            }
        }
        m_index = tempIndex;
        return result;
    }

    private double LLV(CVariable var)
    {
    	return 0;
    }

    private double LLVBARS(CVariable var)
    {
        int n = (int)GetValue(var.m_parameters[1]);
        CVariable cParam = var.m_parameters[0];
        int closeField = cParam.m_field;
        int closeFieldIndex = cParam.m_fieldIndex;
        if (closeField == CTable.NULLFIELD)
        {
            if (cParam.m_tempFields == null)
            {
                cParam.CreateTempFields(0);
            }
            closeField = cParam.m_tempFields[0];
            closeFieldIndex = cParam.m_tempFieldsIndex[0];
            double close = GetValue(cParam);
            m_dataSource.Set3(m_index, closeFieldIndex, close);
        }
        double[] lowarray = m_dataSource.DATA_ARRAY(closeField, m_index, n);
        double result = 0;
        if (lowarray.length > 0)
        {
            int mIndex = 0;
            double close = 0;
            for (int i = 0; i < lowarray.length; i++)
            {
                if (i == 0)
                {
                    close = lowarray[i];
                    mIndex = 0;
                }
                else
                {
                    if (lowarray[i] < close)
                    {
                        close = lowarray[i];
                        mIndex = i;
                    }
                }
            }
            result = lowarray.length - mIndex - 1;
        }
        return result;
    }

    private double LOG(CVariable var)
    {
        return Math.log10(GetValue(var.m_parameters[0]));
    }

    private double MA(CVariable var)
    {
    	return 0;
    }

    private double MAX(CVariable var)
    {
        double left = GetValue(var.m_parameters[0]);
        double right = GetValue(var.m_parameters[1]);
        if (left >= right)
        {
            return left;
        }
        else
        {
            return right;
        }
    }

    private double MEMA(CVariable var)
    {
    	return 0;
    }

    private double MIN(CVariable var)
    {
        double left = GetValue(var.m_parameters[0]);
        double right = GetValue(var.m_parameters[1]);
        if (left <= right)
        {
            return left;
        }
        else
        {
            return right;
        }
    }

    private int MINUTE(CVariable var)
    {
        return CStr.ConvertNumToDate(m_dataSource.GetXValue(m_index)).get(Calendar.MINUTE);
    }

    private double MOD(CVariable var)
    {
        double left = GetValue(var.m_parameters[0]);
        double right = GetValue(var.m_parameters[1]);
        if (right != 0)
        {
            return left % right;
        }
        return 0;
    }

    private int MONTH(CVariable var)
    {
        return CStr.ConvertNumToDate(m_dataSource.GetXValue(m_index)).get(Calendar.MONTH) + 1;
    }

    private int NDAY(CVariable var)
    {
        int n = (int)GetValue(var.m_parameters[2]);
        if (n < 0)
        {
            n = m_dataSource.GetRowsCount();
        }
        else if (n > m_index + 1)
        {
            n = m_index + 1;
        }
        int tempIndex = m_index;
        int result = 1;
        for (int i = 0; i < n; i++)
        {
            if (GetValue(var.m_parameters[0]) <= GetValue(var.m_parameters[1]))
            {
                result = 0;
                break;
            }
            m_index--;
        }
        m_index = tempIndex;
        return result;
    }

    private int NOT(CVariable var)
    {
        double value = GetValue(var.m_parameters[0]);
        if (value == 0)
        {
            return 1;
        }
        else
        {
            return 0;
        }
    }

    private double POLYLINE(CVariable var)
    {
        if (m_div != null)
        {
            CVariable cond = var.m_parameters[0];
            CVariable price = var.m_parameters[1];
            PolylineShape polylineShape = null;
            if (var.m_polylineShape == null)
            {
                String strColor = "COLORAUTO";
                String strLineWidth = "LINETHICK";
                boolean dotLine = false;
                for (int i = 2; i < var.m_parameters.length; i++)
                {
                    String strParam = var.m_parameters[i].m_expression;
                    if (strParam.startsWith("COLOR"))
                    {
                        strColor = strParam;
                    }
                    else if (strParam.startsWith("LINETHICK"))
                    {
                        strLineWidth = strParam;
                    }
                    else if (strParam.startsWith("DOTLINE"))
                    {
                        dotLine = true;
                    }
                }
                polylineShape = new PolylineShape();
                m_div.AddShape(polylineShape);
                polylineShape.SetAttachVScale(m_attachVScale);
                polylineShape.SetColor(GetColor(strColor));
                polylineShape.SetWidth(GetLineWidth(strLineWidth));
                var.CreateTempFields(1);
                polylineShape.SetColorField(var.m_tempFields[0]);
                polylineShape.SetFieldText(price.m_fieldText);
                if (dotLine)
                {
                    polylineShape.SetStyle(PolylineStyle.DotLine);
                }
                var.m_polylineShape = polylineShape;
            }
            else
            {
                polylineShape = var.m_polylineShape;
            }
            if (price.m_expression != null && price.m_expression.length() > 0)
            {
                if (polylineShape.GetFieldName() == CTable.NULLFIELD)
                {
                    if (price.m_field != CTable.NULLFIELD)
                    {
                        polylineShape.SetFieldName(price.m_field);
                    }
                    else
                    {
                        price.CreateTempFields(1);
                        polylineShape.SetFieldName(price.m_tempFields[0]);
                    }
                    for (int i = 2; i < var.m_parameters.length; i++)
                    {
                        String strParam = var.m_parameters[i].m_expression;
                        if (strParam.equals("DRAWTITLE"))
                        {
                            if (polylineShape.GetFieldText() != null)
                            {
                                m_div.GetTitleBar().GetTitles().add(new CTitle(polylineShape.GetFieldName(), polylineShape.GetFieldText(), polylineShape.GetColor(), 2, true));
                            }
                        }
                    }
                }
                if (price.m_tempFieldsIndex != null)
                {
                    double value = GetValue(price);
                    m_dataSource.Set3(m_index, price.m_tempFieldsIndex[0], value);
                }
            }
            double dCond = 1;
            if (cond.m_expression != null && cond.m_expression.length() > 0 && !cond.m_expression.equals("1"))
            {
                dCond = GetValue(cond);
                if (dCond != 1)
                {
                    m_dataSource.Set3(m_index, var.m_tempFieldsIndex[0], -10000);
                }
                else
                {
                    m_dataSource.Set3(m_index, var.m_tempFieldsIndex[0], 1);
                }
            }
        }
        return 0;
    }

    private double POW(CVariable var)
    {
        double left = GetValue(var.m_parameters[0]);
        double right = GetValue(var.m_parameters[1]);
        return Math.pow(left, right);
    }

    private int RAND(CVariable var)
    {
        int n = (int)GetValue(var.m_parameters[0]);
        return m_random.nextInt(n + 1);
    }

    private double REF(CVariable var)
    {
        int param = (int)GetValue(var.m_parameters[1]);
        param = m_index - param;
        double result = 0;
        if (param >= 0)
        {
            int tempIndex = m_index;
            m_index = param;
            result = GetValue(var.m_parameters[0]);
            m_index = tempIndex;
        }
        return result;
    }

    private double RETURN(CVariable var)
    {
        m_resultVar = null; 
        m_result = GetValue(var.m_parameters[0]);
        if (m_tempVars.containsKey(var.m_parameters[0].m_field)) 
        {
            m_resultVar = CopyTempVar(m_tempVars.get(var.m_parameters[0].m_field)); 
        }
        else
        {
            if (var.m_parameters[0].m_expression.indexOf('\'') == 0) 
            {
                m_resultVar = new CVar(); 
                m_resultVar.m_type = 1; 
                m_resultVar.m_str = var.m_parameters[0].m_expression; 
            }
        }
        m_break = 1;
        return m_result;
    }

    private double REVERSE(CVariable var)
    {
        return -GetValue(var.m_parameters[0]);
    }

    private double ROUND(CVariable var)
    {
        return Math.round(GetValue(var.m_parameters[0]));
    }

    private double SAR(CVariable var)
    {
    	return 0;
    }

    private double SET(CVariable var)
    {
        int pLen = var.m_parameters.length;
        for (int i = 0; i < pLen; i++)
        {
            if (i % 2 == 0)
            {
                CVariable variable = var.m_parameters[i];
                CVariable parameter = var.m_parameters[i + 1];
                SetVariable(variable, parameter);
            }
        }
        return 0;
    }

    private int SIGN(CVariable var)
    {
        double value = GetValue(var.m_parameters[0]);
        if (value > 0)
        {
            return 1;
        }
        else if (value < 0)
        {
            return -1;
        }
        return 0;
    }

    private double SIN(CVariable var)
    {
        return Math.sin(GetValue(var.m_parameters[0]));
    }

    private double SMA(CVariable var)
    {
    	return 0;
    }

    private double SQRT(CVariable var)
    {
        return Math.sqrt(GetValue(var.m_parameters[0]));
    }

    private double SQUARE(CVariable var)
    {
        double result = GetValue(var.m_parameters[0]);
        result = result * result;
        return result;
    }

    private double STD(CVariable var)
    {
    	return 0;
    }

    private double STICKLINE(CVariable var)
    {
        if (m_div != null)
        {
            CVariable cond = var.m_parameters[0];
            CVariable price1 = var.m_parameters[1];
            CVariable price2 = var.m_parameters[2];
            CVariable width = var.m_parameters[3];
            CVariable empty = var.m_parameters[4];
            BarShape barShape = null;
            if (var.m_barShape == null)
            {
                barShape = new BarShape();
                m_div.AddShape(barShape);
                barShape.SetAttachVScale(m_attachVScale);
                barShape.SetFieldText(price1.m_fieldText);
                barShape.SetFieldText2(price2.m_fieldText);
                CVariable color = null;
                for (int i = 5; i < var.m_parameters.length; i++)
                {
                    String strParam = var.m_parameters[i].m_expression;
                    if (strParam.startsWith("COLOR"))
                    {
                        color = var.m_parameters[i];
                        break;
                    }
                }
                if (color != null)
                {
                    barShape.SetUpColor(GetColor(color.m_expression));
                    barShape.SetDownColor(GetColor(color.m_expression));
                }
                else
                {
                    barShape.SetUpColor(COLOR.ARGB(255, 82, 82));
                    barShape.SetDownColor(COLOR.ARGB(82, 255, 255));
                }
                barShape.SetStyle(BarStyle.Line);
                var.CreateTempFields(1);
                barShape.SetStyleField(var.m_tempFields[0]);
                barShape.SetLineWidth((int)Math.round(CStr.ConvertStrToDouble(width.m_expression)));
                var.m_barShape = barShape;
            }
            else
            {
                barShape = var.m_barShape;
            }
            if (price1.m_expression != null && price1.m_expression.length() > 0)
            {
                if (barShape.GetFieldName() == CTable.NULLFIELD)
                {
                    if (price1.m_field != CTable.NULLFIELD)
                    {
                        barShape.SetFieldName(price1.m_field);
                    }
                    else
                    {
                        price1.CreateTempFields(1);
                        barShape.SetFieldName(price1.m_tempFields[0]);
                    }
                    for (int i = 5; i < var.m_parameters.length; i++)
                    {
                        String strParam = var.m_parameters[i].m_expression;
                        if (strParam.equals("DRAWTITLE"))
                        {
                            if (barShape.GetFieldText() != null)
                            {
                                m_div.GetTitleBar().GetTitles().add(new CTitle(barShape.GetFieldName(), barShape.GetFieldText(), barShape.GetDownColor(), 2, true));
                            }
                            break;
                        }
                    }
                }
                if (price1.m_tempFieldsIndex != null)
                {
                    double value = GetValue(price1);
                    m_dataSource.Set3(m_index, price1.m_tempFieldsIndex[0], value);
                }
            }
            if (price2.m_expression != null && price2.m_expression.length() > 0 && !price2.m_expression.equals("0"))
            {
                if (price2.m_field != CTable.NULLFIELD)
                {
                    barShape.SetFieldName2(price2.m_field);
                }
                else
                {
                    price2.CreateTempFields(1);
                    barShape.SetFieldName2(price2.m_tempFields[0]);
                }
                if (price2.m_tempFieldsIndex != null)
                {
                    double value = GetValue(price2);
                    m_dataSource.Set3(m_index, price2.m_tempFieldsIndex[0], value);
                }
            }
            double dCond = 1;
            if (cond.m_expression != null && cond.m_expression.length() > 0 && !cond.m_expression.equals("1"))
            {
                dCond = GetValue(cond);
                if (dCond != 1)
                {
                    m_dataSource.Set3(m_index, var.m_tempFieldsIndex[0], -10000);
                }
                else
                {
                    int dEmpty = 2;
                    if (empty.m_expression != null && empty.m_expression.length() > 0)
                    {
                        dEmpty = (int)GetValue(empty);
                        m_dataSource.Set3(m_index, var.m_tempFieldsIndex[0], dEmpty);
                    }
                }
            }
        }
        return 0;
    }

    private double SUM(CVariable var)
    {
    	return 0;
    }

    private double TAN(CVariable var)
    {
        return Math.tan(GetValue(var.m_parameters[0]));
    }

    private double TIME(CVariable var)
    {
    	return 0;
    }

    private double TIME2(CVariable var)
    {
    	return 0;
    }

    private double TMA(CVariable var)
    {
        double close = GetValue(var.m_parameters[0]);
        int n = (int)GetValue(var.m_parameters[1]);
        int m = (int)GetValue(var.m_parameters[2]);
        double lastTma = 0;
        if (m_index > 0)
        {
            lastTma = m_dataSource.Get3(m_index - 1, var.m_fieldIndex);
        }
        double result = n * lastTma + m * close;
        m_dataSource.Set3(m_index, var.m_fieldIndex, result);
        return result;
    }

    private int UPNDAY(CVariable var)
    {
        int n = (int)GetValue(var.m_parameters[0]);
        if (n < 0)
        {
            n = m_dataSource.GetRowsCount();
        }
        else if (n > m_index + 1)
        {
            n = m_index + 1;
        }
        int tempIndex = m_index;
        int result = 1;
        for (int i = 0; i < n; i++)
        {
            double right = GetValue(var.m_parameters[0]);
            m_index--;
            double left = m_index >= 0 ? GetValue(var.m_parameters[0]) : 0;
            if (right <= left)
            {
                result = 0;
                break;
            }
        }
        m_index = tempIndex;
        return result;
    }

    private double VALUEWHEN(CVariable var)
    {
        int n = m_dataSource.GetRowsCount();
        int tempIndex = m_index;
        double result = 0;
        for (int i = 0; i < n; i++)
        {
            double value = GetValue(var.m_parameters[0]);
            if (value == 1)
            {
                result = GetValue(var.m_parameters[1]);
                break;
            }
            m_index--;
        }
        m_index = tempIndex;
        return result;
    }

    private double VAR(CVariable var)
    {
        double result = 0;
        int pLen = var.m_parameters.length;
        for (int i = 0; i < pLen; i++)
        {
            if (i % 2 == 0)
            {
                CVariable name = var.m_parameters[i];
                CVariable value = var.m_parameters[i + 1];
                int id = name.m_field;
                CVar newCVar = m_varFactory.CreateVar();
                result = newCVar.OnCreate(this, name, value);
				if(newCVar.m_type == 1)
                {
                   name.m_functionID = -2;
                }
                if (m_tempVars.containsKey(id))
                {
                    CVar cVar = m_tempVars.get(id);
                    newCVar.m_parent = cVar;
                }
                m_tempVars.put(id, newCVar);
            }
        }
        return result;
    }

    private int WHILE(CVariable var)
    {
        int pLen = var.m_parameters.length;
        if (pLen > 1)
        {
            while (true)
            {
                if (GetValue(var.m_parameters[0]) <= 0)
                {
                    break;
                }
                for (int i = 1; m_break == 0 && i < pLen; i++)
                {
                    GetValue(var.m_parameters[i]);
                }
                if (m_break > 0)
                {
                    if (m_break == 3)
                    {
                        m_break = 0;
                        DeleteTempVars(var);
                        continue;
                    }
                    else
                    {
                        m_break = 0;
                        DeleteTempVars(var);
                        break;
                    }
                }
                else
                {
                    DeleteTempVars(var);
                }
            }
        }
        return 0;
    }

    private double WMA(CVariable var)
    {
    	return 0;
    }

    private int YEAR(CVariable var)
    {
        return CStr.ConvertNumToDate(m_dataSource.GetXValue(m_index)).get(Calendar.YEAR);
    }

    private double ZIG(CVariable var)
    {
    	return 0;
    }

    private int STR_CONTACT(CVariable var)
    {
        int pLen = var.m_parameters.length;
        String text = "'";
        for (int i = 0; i < pLen; i++)
        {
            text += GetText(var.m_parameters[i]);
        }
        text += "'";
        m_resultVar = new CVar(); 
        m_resultVar.m_type = 1; 
        m_resultVar.m_str = text; 
        return 0;
    }

    private int STR_FIND(CVariable var)
    {
        return GetText(var.m_parameters[0]).indexOf(GetText(var.m_parameters[1]));
    }

    private int STR_EQUALS(CVariable var)
    {
        int result = 0;
        if (GetText(var.m_parameters[0]).equals(GetText(var.m_parameters[1])))
        {
            result = 1;
        }
        return result;
    }

    private int STR_FINDLAST(CVariable var)
    {
        return GetText(var.m_parameters[0]).lastIndexOf(GetText(var.m_parameters[1]));
    }

    private int STR_LENGTH(CVariable var)
    {
        return GetText(var.m_parameters[0]).length();
    }

    private int STR_SUBSTR(CVariable var)
    {
        int pLen = var.m_parameters.length;
        if (pLen == 2)
        {
            m_resultVar = new CVar(); 
            m_resultVar.m_type = 1; 
            m_resultVar.m_str = "'" + GetText(var.m_parameters[0]).substring((int)GetValue(var.m_parameters[1])) + "'";
        }
        else if (pLen >= 3)
        {
            m_resultVar = new CVar(); 
            m_resultVar.m_type = 1; 
            m_resultVar.m_str = "'" + GetText(var.m_parameters[0]).substring((int)GetValue(var.m_parameters[1]), (int)GetValue(var.m_parameters[1]) + (int)GetValue(var.m_parameters[2])) + "'";
        }
        return 0;
    }

    private int STR_REPLACE(CVariable var)
    {
        m_resultVar = new CVar(); 
        m_resultVar.m_type = 1; 
        m_resultVar.m_str = "'" + GetText(var.m_parameters[0]).replace(GetText(var.m_parameters[1]), GetText(var.m_parameters[2])) + "'";
        return 0;
    }

    private int STR_SPLIT(CVariable var)
    {
        CVariable pName = var.m_parameters[0];
        int id = pName.m_field;
        if (m_tempVars.containsKey(id))
        {
            ArrayList<String> list = m_tempVars.get(id).m_list;
            list.clear();
            String[] strs = GetText(var.m_parameters[1]).split("["+GetText(var.m_parameters[2])+"]");
            int strsSize = strs.length;
            for (int i = 0; i < strsSize; i++)
            {
                if(strs[i].length()>0) {
                    list.add(strs[i]);
                }
            }
            return 1;
        }
        return 0;
    }

    private int STR_TOLOWER(CVariable var)
    {
        m_resultVar = new CVar(); 
        m_resultVar.m_type = 1; 
        m_resultVar.m_str = GetText(var.m_parameters[0]).toLowerCase();
        return 0;
    }

    private int STR_TOUPPER(CVariable var)
    {
        m_resultVar = new CVar(); 
        m_resultVar.m_type = 1; 
        m_resultVar.m_str = GetText(var.m_parameters[0]).toUpperCase();
        return 0;
    }

    private int LIST_ADD(CVariable var)
    {
        CVariable pName = var.m_parameters[0];
        int listName = pName.m_field;
        if (m_tempVars.containsKey(listName))
        {
            ArrayList<String> list = m_tempVars.get(listName).m_list;
            int pLen = var.m_parameters.length;
            for (int i = 1; i < pLen; i++)
            {
                list.add(GetText(var.m_parameters[i]));
            }
            return 1;
        }
        return 0;
    }

    private int LIST_CLEAR(CVariable var)
    {
        CVariable pName = var.m_parameters[0];
        int listName = pName.m_field;
        if (m_tempVars.containsKey(listName))
        {
            m_tempVars.get(listName).m_list.clear();
            return 1;
        }
        return 0;
    }

    private int LIST_GET(CVariable var)
    {
        CVariable pName = var.m_parameters[1];
        int listName = pName.m_field;
        if (m_tempVars.containsKey(listName))
        {
            ArrayList<String> list = m_tempVars.get(listName).m_list;
            int index = (int)GetValue(var.m_parameters[2]);
            if (index < list.size())
            {
                String strValue = list.get(index);
                CVariable variable = var.m_parameters[0];
                int id = variable.m_field;
                int type = variable.m_type;
                switch (type)
                {
                    case 2:
                        double value = CStr.ConvertStrToDouble(strValue);
                        m_dataSource.Set3(m_index, variable.m_fieldIndex, value);
                        break;
                    default:
                        if (m_tempVars.containsKey(id))
                        {
                            CVar otherCVar = m_tempVars.get(id);
                            CVariable newVar = new CVariable(this);
							newVar.m_type = 1;
                            newVar.m_expression = "'" + strValue + "'";
                            otherCVar.SetValue(this, variable, newVar);
                        }
                        break;
                }
            }
            return 1;
        }
        return 0;
    }


    private int LIST_INSERT(CVariable var)
    {
        CVariable pName = var.m_parameters[0];
        int listName = pName.m_field;
        if (m_tempVars.containsKey(listName))
        {
            m_tempVars.get(listName).m_list.add((int)GetValue(var.m_parameters[1]), GetText(var.m_parameters[2]));
            return 1;
        }
        return 0;
    }


    private int LIST_REMOVE(CVariable var)
    {
        CVariable pName = var.m_parameters[0];
        int listName = pName.m_field;
        if (m_tempVars.containsKey(listName))
        {
            m_tempVars.get(listName).m_list.remove((int)GetValue(var.m_parameters[1]));
            return 1;
        }
        return 0;
    }

    private int LIST_SIZE(CVariable var)
    {
        int size = 0;
        CVariable pName = var.m_parameters[0];
        int listName = pName.m_field;
        if (m_tempVars.containsKey(listName))
        {
            size = m_tempVars.get(listName).m_list.size();
        }
        return size;
    }

    private int MAP_CLEAR(CVariable var)
    {
        CVariable pName = var.m_parameters[0];
        int mapName = pName.m_field;
        if (m_tempVars.containsKey(mapName))
        {
            m_tempVars.get(mapName).m_map.clear();
            return 1;
        }
        return 0;
    }

    private int MAP_CONTAINSKEY(CVariable var)
    {
        int result = 0;
        CVariable pName = var.m_parameters[0];
        int mapName = pName.m_field;
        if (m_tempVars.containsKey(mapName))
        {
            if (m_tempVars.get(mapName).m_map.containsKey(GetText(var.m_parameters[1])))
            {
                result = 1;
            }
        }
        return result;
    }
    private int MAP_GET(CVariable var)
    {
        CVariable pName = var.m_parameters[1];
        int mapName = pName.m_field;
        if (m_tempVars.containsKey(mapName))
        {
            HashMap<String, String> map = m_tempVars.get(mapName).m_map;
            String key = GetText(var.m_parameters[2]);
            if (map.containsKey(key))
            {
                String strValue = map.get(key);
                CVariable variable = var.m_parameters[0];
                int id = variable.m_field;
                int type = variable.m_type;
                switch (type)
                {
                    case 2:
                        double value = CStr.ConvertStrToDouble(strValue);
                        m_dataSource.Set3(m_index, variable.m_fieldIndex, value);
                        break;
                    default:
                        if (m_tempVars.containsKey(id))
                        {
                            CVar otherCVar = m_tempVars.get(id);
                            CVariable newVar = new CVariable(this);
							newVar.m_type = 1;
                            newVar.m_expression = "'" + strValue + "'";
                            otherCVar.SetValue(this, variable, newVar);
                        }
                        break;
                }
            }
            return 1;
        }
        return 0;
    }

    private int MAP_GETKEYS(CVariable var)
    {
        CVariable pName = var.m_parameters[1];
        int mapName = pName.m_field;
        if (m_tempVars.containsKey(mapName))
        {
            int listName = var.m_parameters[0].m_field;
            if (m_tempVars.containsKey(listName))
            {
                HashMap<String, String> map = m_tempVars.get(mapName).m_map;
                ArrayList<String> list = m_tempVars.get(listName).m_list;
                list.clear();
                for (Map.Entry<String,String> entry: map.entrySet())
                {
                    list.add(entry.getKey());
                }
                return 1;
            }
        }
        return 0;
    }


    private int MAP_REMOVE(CVariable var)
    {
        CVariable pName = var.m_parameters[0];
        int mapName = pName.m_field;
        if (m_tempVars.containsKey(mapName))
        {
            m_tempVars.get(mapName).m_map.remove(GetText(var.m_parameters[1]));
            return 1;
        }
        return 0;
    }

    private int MAP_SET(CVariable var)
    {
        CVariable pName = var.m_parameters[0];
        int mapName = pName.m_field;
        if (m_tempVars.containsKey(mapName))
        {
            m_tempVars.get(mapName).m_map.put(GetText(var.m_parameters[1]), GetText(var.m_parameters[2]));
        }
        return 0;
    }

    private int MAP_SIZE(CVariable var) {
        int size = 0;
        CVariable pName = var.m_parameters[0];
        int mapName = pName.m_field;
        if (m_tempVars.containsKey(mapName)) {
            size = m_tempVars.get(mapName).m_map.size();
        }
        return size;
    }
}