/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.runtime.sql;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQLQueryInfo
 */
public class SQLQueryInfo
{
    public static class TableRef
    {
        final String path;
        final String alias;

        public TableRef(String path, String alias)
        {
            this.path = path;
            this.alias = alias;
        }

        public String getPath()
        {
            return path;
        }

        public String getAlias()
        {
            return alias;
        }
    }

    public static final String ALIAS_PATTERN = "FROM\\s+(([\\w\\.\\\"]+)\\s+[as\\s+]?(\\w+)[\\s\\,]+)+WHERE?";
    //public static final String ALIAS_PATTERN_2 = "(\\w+)\\s+(\\w+)[\\s\\,]+";
    public Pattern aliasPattern = Pattern.compile(ALIAS_PATTERN, Pattern.CASE_INSENSITIVE);

    private List<TableRef> tableRefs = new ArrayList<TableRef>();

    public SQLQueryInfo(String query)
    {
        parseDTT(query);
        List<TableRef> tableRefs = new ArrayList<TableRef>();
        Matcher matcher = aliasPattern.matcher(query);
        int pos = 0;
        while (matcher.find(pos)) {
            TableRef newRef = new TableRef(matcher.group(2), matcher.group(3));
            tableRefs.add(newRef);
            pos = matcher.end();
        }
        this.tableRefs = tableRefs;
    }

    public List<TableRef> getTableRefs()
    {
        return tableRefs;
    }

    private void parseDTT(String query)
    {
/*
        PostParseProcessor dataTypeResolver = new DataTypeResolver();

        // ordering is important for post parse processing! first we need to fill

        // in the database information for table references and column types

        List postParseProcessors = new ArrayList();
        postParseProcessors.add(1, dataTypeResolver);

        // get the SQL source format options and set at least the current schema

        // that is omited but implicit for any unqualified table references

        // important for later resolving of table references!

        SQLQuerySourceFormat sourceFormat = SQLQuerySourceFormat.copyDefaultFormat();

        //sourceFormat.setOmitSchema(currentSchemaName);


        SQLQueryParserManager parserManager = SQLQueryParserManagerProvider.getInstance()
            .getParserManager("sa", "");

        parserManager.configParser(sourceFormat, postParseProcessors);

        // parse the SQL statement

        try {

            SQLQueryParseResult parseResult = parserManager.parseQuery(query);
            List semanticErrors = parseResult.getErrorList();
            Iterator itr = semanticErrors.iterator();

            while (itr.hasNext()) {

                SQLParseErrorInfo errorInfo = (SQLParseErrorInfo) itr.next();
                // the error message
                String errorMessage = errorInfo.getParserErrorMessage();
                // the line numbers of error

                int errorLine = errorInfo.getLineNumberStart();
                int errorColumn = errorInfo.getColumnNumberStart();

                // Error processing for specific errors
                String errorCode = errorInfo.getErrorCode();
                if (TableReferenceResolver.ERROR_CODE_TABLE_UNRESOLVED.equals(errorCode)) {
                    // table not found
                } else if (TableReferenceResolver.ERROR_CODE_COLUMN_UNRESOLVED.equals(errorCode)
                    || TableReferenceResolver.ERROR_CODE_NONEXISTENT_COLUMN.equals(errorCode))
                {
                    // column not found
                }

            }


        } catch (SQLParserException spe) {
            // handle the syntax exception
            System.out.println(spe.getMessage());

        } catch (SQLParserInternalException spie) {
            // handle the exception
            System.out.println(spie.getMessage());
        }

*/
    }
}
