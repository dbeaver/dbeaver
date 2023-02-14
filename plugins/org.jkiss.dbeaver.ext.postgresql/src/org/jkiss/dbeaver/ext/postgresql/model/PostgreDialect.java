/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.postgresql.model.data.PostgreBinaryFormatter;
import org.jkiss.dbeaver.ext.postgresql.sql.PostgreEscapeStringRule;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.sql.SQLDataTypeConverter;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLDialectDDLExtension;
import org.jkiss.dbeaver.model.sql.SQLExpressionFormatter;
import org.jkiss.dbeaver.model.sql.parser.rules.SQLDollarQuoteRule;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.text.parser.TPRule;
import org.jkiss.dbeaver.model.text.parser.TPRuleProvider;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.Types;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * PostgreSQL dialect
 */
public class PostgreDialect extends JDBCSQLDialect implements TPRuleProvider, SQLDataTypeConverter,
    SQLDialectDDLExtension {
    public static final String[] POSTGRE_NON_TRANSACTIONAL_KEYWORDS = ArrayUtils.concatArrays(
        BasicSQLDialect.NON_TRANSACTIONAL_KEYWORDS,
        new String[]{
            "SHOW", "SET"
        }
    );

    private static final String[][] PG_STRING_QUOTES = {
        {"'", "'"}
    };

    // In PgSQL there are no blocks. DO $$ ... $$ queries are processed as strings
    public static final String[][] BLOCK_BOUND_KEYWORDS = {
//        {SQLConstants.BLOCK_BEGIN, SQLConstants.BLOCK_END},
//        {"LOOP", "END LOOP"}
    };

    private static final String[] EXEC_KEYWORDS = {
        "CALL"
    };

    //Function without arguments/parameters #8710
    public static final String AUTO_INCREMENT_KEYWORD = "AUTO_INCREMENT";

    //region KeyWords


    //endregion

    //region FUNCTIONS KW

    //endregion

    private PostgreServerExtension serverExtension;

    public PostgreDialect() {
        super("PostgreSQL", "postgresql");
    }

    public void addExtraKeywords(String... keywords) {
        super.addSQLKeywords(Arrays.asList(keywords));
    }

    public void addExtraFunctions(String... functions) {
        super.addFunctions(Arrays.asList(functions));
    }

    public void initDriverSettings(JDBCSession session, JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(session, dataSource, metaData);

        removeSQLKeyword("LENGTH");

        if (dataSource instanceof PostgreDataSource) {
            serverExtension = ((PostgreDataSource) dataSource).getServerType();
            serverExtension.configureDialect(this);
        }

        // #12723 Redshift driver returns wrong infor about unquoted case
        setUnquotedIdentCase(DBPIdentifierCase.LOWER);
    }

    @Override
    public void addKeywords(Collection<String> set, DBPKeywordType type) {
        super.addKeywords(set, type);
    }

    @NotNull
    @Override
    public String[] getExecuteKeywords() {
        return EXEC_KEYWORDS;
    }

    @Override
    public char getStringEscapeCharacter() {
        if (serverExtension != null && serverExtension.supportsBackslashStringEscape()) {
            return '\\';
        }
        return super.getStringEscapeCharacter();
    }

    @Override
    public int getCatalogUsage() {
        return SQLDialect.USAGE_DML;
    }

    @Override
    public int getSchemaUsage() {
        return SQLDialect.USAGE_ALL;
    }

    @NotNull
    @Override
    public String[] getParametersPrefixes() {
        return new String[]{"$"};
    }

    @NotNull
    @Override
    public MultiValueInsertMode getDefaultMultiValueInsertMode() {
        return MultiValueInsertMode.GROUP_ROWS;
    }

    @Override
    public String[][] getBlockBoundStrings() {
        return BLOCK_BOUND_KEYWORDS;
    }

    @Override
    public String getCastedAttributeName(@NotNull DBSAttributeBase attribute, String attributeName) {
        // This method actually works for special data types like JSON and XML. Because column names in the condition in a table without key must be also cast, as data in getTypeCast method.
        if (attribute instanceof DBSObject && !DBUtils.isPseudoAttribute(attribute)) {
            if (!CommonUtils.equalObjects(attributeName, attribute.getName())) {
                // Must use explicit attribute name
                attributeName = DBUtils.getQuotedIdentifier(((DBSObject) attribute).getDataSource(), attributeName);
            } else {
                attributeName = DBUtils.getObjectFullName(((DBSObject) attribute).getDataSource(), attribute, DBPEvaluationContext.DML);
            }
        }
        return getCastedString(attribute, attributeName, true, true);
    }

    @NotNull
    @Override
    public String getTypeCastClause(@NotNull DBSTypedObject attribute, String expression, boolean isInCondition) {
        // Some data for some types of columns data types must be cast. It can be simple casting only with data type name like "::pg_class" or casting with fully qualified names for user defined types like "::schemaName.testType".
        // Or very special clauses with JSON and XML columns, when we have to cast both column data and column name to text.
        return getCastedString(attribute, expression, isInCondition, false);
    }

    private String getCastedString(@NotNull DBSTypedObject attribute, String string, boolean isInCondition, boolean castColumnName) {
        if (attribute instanceof DBSTypedObjectEx) {
            DBSDataType dataType = ((DBSTypedObjectEx) attribute).getDataType();
            if (dataType instanceof PostgreDataType) {
                String typeCasting = ((PostgreDataType) dataType).getConditionTypeCasting(isInCondition, castColumnName);
                if (CommonUtils.isNotEmpty(typeCasting)) {
                    return string + typeCasting;
                }
            }
        }
        return string;
    }

    @NotNull
    @Override
    public String escapeScriptValue(DBSTypedObject attribute, @NotNull Object value, @NotNull String strValue) {
        if (PostgreUtils.isPGObject(value)
            || PostgreConstants.TYPE_BIT.equals(attribute.getTypeName())
            || PostgreConstants.TYPE_INTERVAL.equals(attribute.getTypeName())
            || attribute.getTypeID() == Types.OTHER
            || attribute.getTypeID() == Types.ARRAY
            || attribute.getTypeID() == Types.STRUCT)
        {
            // TODO: we need to add value handlers for all PG data types.
            // For now we use workaround: represent objects as strings
            return '\'' + escapeString(strValue) + '\'';
        }
        if (CommonUtils.isNaN(value) || CommonUtils.isInfinite(value)) {
            // These special values should be quoted
            // https://www.postgresql.org/docs/current/datatype-numeric.html#DATATYPE-NUMERIC-DECIMAL
            return '\'' + String.valueOf(value) + '\'';
        }
        return super.escapeScriptValue(attribute, value, strValue);
    }

    @NotNull
    @Override
    public String[][] getStringQuoteStrings() {
        return PG_STRING_QUOTES;
    }

    @Override
    public boolean supportsAliasInSelect() {
        return true;
    }

    @Override
    public boolean supportsAliasInConditions() {
        return false;
    }

    @Override
    public boolean supportsTableDropCascade() {
        return true;
    }

    @Override
    public boolean supportsColumnAutoIncrement() {
        return false;
    }

    @Override
    public boolean supportsCommentQuery() {
        return true;
    }

    @Override
    public boolean supportsNestedComments() {
        return true;
    }

    @Nullable
    @Override
    public SQLExpressionFormatter getCaseInsensitiveExpressionFormatter(@NotNull DBCLogicalOperator operator) {
        if (operator == DBCLogicalOperator.LIKE) {
            return (left, right) -> left + " ILIKE " + right;
        }
        return super.getCaseInsensitiveExpressionFormatter(operator);
    }

    @NotNull
    @Override
    public DBDBinaryFormatter getNativeBinaryFormatter() {
        return PostgreBinaryFormatter.INSTANCE;
    }

    @Override
    protected void loadDataTypesFromDatabase(JDBCDataSource dataSource) {
        super.loadDataTypesFromDatabase(dataSource);
        addDataTypes(PostgreConstants.DATA_TYPE_ALIASES.keySet());
    }

    @NotNull
    @Override
    public String[] getNonTransactionKeywords() {
        return POSTGRE_NON_TRANSACTIONAL_KEYWORDS;
    }

    @Override
    protected boolean isStoredProcedureCallIncludesOutParameters() {
        return false;
    }

    @Override
    public void extendRules(@Nullable DBPDataSourceContainer dataSource, @NotNull List<TPRule> rules, @NotNull RulePosition position) {
        if (position == RulePosition.INITIAL || position == RulePosition.PARTITION) {
            boolean ddTagDefault = DBWorkbench.getPlatform().getPreferenceStore().getBoolean(PostgreConstants.PROP_DD_TAG_STRING);
            boolean ddTagIsString = dataSource == null
                ? ddTagDefault
                : CommonUtils.getBoolean(dataSource.getActualConnectionConfiguration().getProviderProperty(PostgreConstants.PROP_DD_TAG_STRING), ddTagDefault);

            boolean ddPlainDefault = DBWorkbench.getPlatform().getPreferenceStore().getBoolean(PostgreConstants.PROP_DD_PLAIN_STRING);
            boolean ddPlainIsString = dataSource == null
                ? ddPlainDefault
                : CommonUtils.getBoolean(dataSource.getActualConnectionConfiguration().getProviderProperty(PostgreConstants.PROP_DD_PLAIN_STRING), ddPlainDefault);

            rules.add(new SQLDollarQuoteRule(position == RulePosition.PARTITION, true, ddTagIsString, ddPlainIsString));
            rules.add(new PostgreEscapeStringRule());
        }
    }

    @Override
    public boolean supportsInsertAllDefaultValuesStatement() {
        return true;
    }

    @Override
    public String convertExternalDataType(@NotNull SQLDialect sourceDialect, @NotNull DBSTypedObject sourceTypedObject, @Nullable DBPDataTypeProvider targetTypeProvider) {
        String externalTypeName = sourceTypedObject.getTypeName().toLowerCase(Locale.ENGLISH);
        String localDataType = null, dataTypeModifies = null;

        switch (externalTypeName) {
            case "xml":
            case "xmltype":
            case "sys.xmltype":
                localDataType = "xml";
                break;
            case "varchar2":
            case "nchar":
            case "nvarchar":
                localDataType = "varchar";
                if (sourceTypedObject.getMaxLength() > 0 &&
                    sourceTypedObject.getMaxLength() != Integer.MAX_VALUE &&
                    sourceTypedObject.getMaxLength() != Long.MAX_VALUE)
                {
                    dataTypeModifies = String.valueOf(sourceTypedObject.getMaxLength());
                }
                break;
            case "json":
            case "jsonb":
                localDataType = "jsonb";
                break;
            case "geometry":
            case "sdo_geometry":
            case "mdsys.sdo_geometry":
                localDataType = "geometry";
                break;
            case "number":
                localDataType = "numeric";
                if (sourceTypedObject.getPrecision() != null) {
                    dataTypeModifies = sourceTypedObject.getPrecision().toString();
                    if (sourceTypedObject.getScale() != null) {
                        dataTypeModifies += "," + sourceTypedObject.getScale();
                    }
                }
                break;
        }
        if (localDataType == null) {
            return null;
        }
        if (targetTypeProvider == null) {
            return localDataType;
        } else {
            DBSDataType dataType = targetTypeProvider.getLocalDataType(localDataType);
            if (dataType == null) {
                return null;
            }
            String targetTypeName = DBUtils.getObjectFullName(dataType, DBPEvaluationContext.DDL);
            if (dataTypeModifies != null) {
                targetTypeName += "(" + dataTypeModifies + ")";
            }
            return targetTypeName;
        }
    }

    @Nullable
    @Override
    public String getAutoIncrementKeyword() {
        return AUTO_INCREMENT_KEYWORD;
    }

    @Override
    public boolean supportsCreateIfExists() {
        return true;
    }

    @NotNull
    @Override
    public String getTimestampDataType() {
        return PostgreConstants.TYPE_TIMESTAMP;
    }

    @NotNull
    @Override
    public String getBigIntegerType() {
        return PostgreConstants.TYPE_BIGINT;
    }

    @NotNull
    @Override
    public String getClobDataType() {
        return PostgreConstants.TYPE_TEXT;
    }
}
