package org.jkiss.dbeaver.antlr.model.internal;

import java.util.Map;

import javax.xml.xpath.XPathExpression;

public class LiteralTypeInfo {
    public final String ruleName;
    public final Class<?> type;
    public final XPathExpression stringExpr;
    public final Map<Object, XPathExpression> exprByValue;
    public final Map<String, Object> valuesByName;
    public final boolean isCaseSensitive;
    
    public LiteralTypeInfo(String ruleName, Class<?> type, XPathExpression stringExpr, Map<Object, XPathExpression> exprByValue, Map<String, Object> valuesByName, boolean isCaseSensitive) {
        this.ruleName = ruleName;
        this.type = type;
        this.stringExpr = stringExpr;
        this.exprByValue = exprByValue;
        this.valuesByName = valuesByName;
        this.isCaseSensitive = isCaseSensitive;
    }        
}
