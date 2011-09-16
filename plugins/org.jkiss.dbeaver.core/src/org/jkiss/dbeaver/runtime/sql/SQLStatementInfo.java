/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.sql;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.IToken;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLSyntaxManager;
import org.jkiss.dbeaver.ui.editors.sql.syntax.tokens.SQLParameterToken;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLStatementInfo
 */
public class SQLStatementInfo {

    static final Log log = LogFactory.getLog(SQLStatementInfo.class);

    private String query;
    private List<SQLStatementParameter> parameters;
    private int offset;
    private int length;
    private Object data;
    private SQLStatementType type;

    public SQLStatementInfo(String query)
    {
        this(query, null);
    }

    public SQLStatementInfo(String query, List<SQLStatementParameter> parameters) {
        this.query = query;
        this.parameters = parameters;
    }

    public String getQuery()
    {
        return query;
    }

    public List<SQLStatementParameter> getParameters() {
        return parameters;
    }

    public int getOffset()
    {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLength()
    {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    /**
     * User defined data object. May be used to identify statements.
     * @return data or null
     */
    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public SQLStatementType getType()
    {
        return type;
    }

    public void setType(SQLStatementType type)
    {
        this.type = type;
    }

    public void parseParameters(IDocument document, SQLSyntaxManager syntaxManager)
    {
        syntaxManager.setRange(document, offset, length);
        for (;;) {
            IToken token = syntaxManager.nextToken();
            int tokenOffset = syntaxManager.getTokenOffset();
            final int tokenLength = syntaxManager.getTokenLength();
            if (token.isEOF() || tokenOffset > offset + length) {
                break;
            }

            if (token instanceof SQLParameterToken && tokenLength > 0) {
                try {
                    if (parameters == null) {
                        parameters = new ArrayList<SQLStatementParameter>();
                    }
                    parameters.add(
                        new SQLStatementParameter(
                            null,
                            null,
                            parameters.size() + 1,
                            document.get(tokenOffset, tokenLength),
                            null));
                } catch (BadLocationException e) {
                    log.warn("Can't extract query parameter", e);
                }
            }
        }
    }

    @Override
    public String toString()
    {
        return query;
    }

}
