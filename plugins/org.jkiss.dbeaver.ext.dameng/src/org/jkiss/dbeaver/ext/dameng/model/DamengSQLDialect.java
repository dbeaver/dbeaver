/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jkiss.dbeaver.ext.dameng.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.sql.SQLConstants;

import java.util.Arrays;

/**
 * @author Shengkai Bai
 */
public class DamengSQLDialect extends GenericSQLDialect {

    private static final String[] EXEC_KEYWORDS = new String[]{"CALL"};

    private static final String[][] DM_BEGIN_END_BLOCK = new String[][]{
            {SQLConstants.BLOCK_BEGIN, SQLConstants.BLOCK_END},
            {"IF", SQLConstants.BLOCK_END + " IF"},
            {"LOOP", SQLConstants.BLOCK_END + " LOOP"},
            {SQLConstants.KEYWORD_CASE, SQLConstants.BLOCK_END},
    };

    public DamengSQLDialect() {
        super("Dameng", "dameng");
    }

    @Override
    public String[][] getBlockBoundStrings() {
        return DM_BEGIN_END_BLOCK;
    }

    @Override
    public void initDriverSettings(JDBCSession session, JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(session, dataSource, metaData);
        addFunctions(Arrays.asList(
                //Number Functions:
                "ACOS", "ASIN", "ATAN", "ATAN2", "COS", "COSH", "COT", "DEGREES", "GREATEST", "GREAT", "LEAST", "LOG",
                "LOG10", "PI", "POWER2", "RADIANS", "RAND", "ROUND", "SIGN", "SIN", "SINH", "TAN", "TANH", "TO_NUMBER",
                "TRUNC", "TO_CHAR", "BITAND", "NANVL", "REMAINDER", "TO_BINARY_FLOAT", "TO_BINARY_DOUBLE", "BIN_TO_NUM",
                // String function:
                "ASCII", "ASCIISTR", "BIT_LENGTH", "CHAR", "CHAR_LENGTH", "CHARACTER_LENGTH", "CHR", "NCHR", "CONCAT", "DIFFERENCE",
                "INITCAP", "INS", "INSERT", "INSSTR", "INSTR", "INSTRB", "LCASE", "LEFTSTR", "LEN", "LENGTHC", "LENGTH2", "LENGTH4",
                "OCTET_LENGTH", "LOCATE", "LPAD", "LTRIM", "REPEAT", "REPLACE", "REPLICATE", "REVERSE", "RIGHT", "RPAD", "RTRIM",
                "SOUNDEX", "STRPOSDEC", "STRPOSINC", "STUFF", "SUBSTR", "SUBSTRB", "UCASE", "NLS_UPPER", "NLS_LOWER", "REGEXP",
                "TEXT_EQUAL", "BLOB_EQUAL", "NLSSORT", "GREATEST", "GREAT", "to_single_byte", "to_multi_byte", "EMPTY_CLOB", "EMPTY_BLOB",
                "UNISTR", "CONCAT_WS", "SUBSTRING_INDEX", "COMPOSE", "FIND_IN_SET", "TRUNC",

                // Null Function
                "COALESCE", "IFNULL", "ISNULL", "NULLIF", "NVL", "NULL_EQU"
        ));
    }

    @Override
    public boolean supportsAliasInSelect() {
        return true;
    }

    @NotNull
    @Override
    public String[] getExecuteKeywords() {
        return EXEC_KEYWORDS;
    }

    @Override
    public String getDualTableName() {
        return "DUAL";
    }
}
