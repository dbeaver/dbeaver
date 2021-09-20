/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.hana.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.data.formatters.BinaryFormatterHexString;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.parser.rules.SQLVariableRule;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.text.parser.TPRule;
import org.jkiss.dbeaver.model.text.parser.TPRuleProvider;
import org.jkiss.utils.CommonUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class HANASQLDialect extends GenericSQLDialect implements TPRuleProvider {

    private static final Log log = Log.getLog(HANASQLDialect.class);

    private static final String[][] HANA_BEGIN_END_BLOCK = new String[][]{
        {SQLConstants.BLOCK_BEGIN, SQLConstants.BLOCK_END},
        {"IF", SQLConstants.BLOCK_END},
        {SQLConstants.KEYWORD_CASE, SQLConstants.BLOCK_END},
        {"FOR", SQLConstants.BLOCK_END + " FOR"}
    };

    public HANASQLDialect() {
        super("HANA", "sap_hana");
    }

    @Override
    public String[][] getBlockBoundStrings() {
        return HANA_BEGIN_END_BLOCK;
    }

    public void initDriverSettings(JDBCSession session, JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(session, dataSource, metaData);
        // TODO: check if obsolete
        addSQLKeywords(
                Arrays.asList(
                        "REPLACE_REGEXPR"));
    }

    @Override
    public boolean supportsAliasInSelect() {
        return true;
    }

    @Override
    public boolean validIdentifierStart(char c) {
        return super.validIdentifierStart(c) || c == '_';
    }
    
    /*
     * expression evaluation
     */
    @Override
    public String getDualTableName() {
        return "DUMMY";
    }

    @Override
    public String getColumnTypeModifiers(@NotNull DBPDataSource dataSource, @NotNull DBSTypedObject column,
            @NotNull String typeName, @NotNull DBPDataKind dataKind) {
        String ucTypeName = CommonUtils.notEmpty(typeName).toUpperCase(Locale.ENGLISH);
        if (("ST_POINT".equals(ucTypeName) || "ST_GEOMETRY".equals(ucTypeName))
                && (column instanceof HANATableColumn)) {
            HANATableColumn hanaColumn = (HANATableColumn) column;
            try {
                int srid = hanaColumn.getAttributeGeometrySRID(new VoidProgressMonitor());
                return "(" + Integer.toString(srid) + ")";
            } catch (DBCException e) {
                log.info("Could not determine SRID of column", e);
            }
        }
        return super.getColumnTypeModifiers(dataSource, column, ucTypeName, dataKind);
    }

    @NotNull
    @Override
    public DBDBinaryFormatter getNativeBinaryFormatter() {
        return BinaryFormatterHexString.INSTANCE;
    }

    @NotNull
    @Override
    public String getSearchStringEscape() {
        // https://github.com/dbeaver/dbeaver/issues/9998#issuecomment-805710837
        return "\\";
    }

    @Override
    public void extendRules(@Nullable DBPDataSourceContainer dataSource, @NotNull List<TPRule> rules, @NotNull RulePosition position) {
        if (position == RulePosition.FINAL) {
            rules.add(new SQLVariableRule(this));
        }
    }
}
