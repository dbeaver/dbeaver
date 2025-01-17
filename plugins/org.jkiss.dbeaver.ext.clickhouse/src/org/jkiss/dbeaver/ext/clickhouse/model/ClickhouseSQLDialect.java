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
package org.jkiss.dbeaver.ext.clickhouse.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.clickhouse.ClickhouseConstants;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.CommonUtils;

import java.util.Arrays;

public class ClickhouseSQLDialect extends GenericSQLDialect {

    private static final String[] CLICKHOUSE_FUNCTIONS = {
        "quantile",
        "quantileExact",
        "uniq",
        "concat",
        "replaceOne",
        "replaceAll",
        "toStartOfFifteenMinutes",
        "toStartOfFiveMinute",
        "toStartOfInterval",
        "toTimezone",
        "formatDateTime",
        "now",
        "multiIf",
        "geoToS2",
        "s2ToGeo",
        "greatCircleDistance",
        "greatCircleAngle",
        "plus",
        "minus",
        "multiply",
        "divide",
        "arrayConcat",
        "hasAll",
        "hasAny",
        "indexOf",
        "mapKeys",
        "mapValues",
        "UUIDNumToString",
        "UUIDStringToNum",
        "visitParamHas",
        "IPv4StringToNum",
        "IPv6StringToNum",
        "randConstant",
        "javaHash",
        "bitmapBuild",
        "bitCount",
        "splitByChar",
        "splitByWhitespace",
        "toLowCardinality",
        "formatRow",
        "formatRow",
        "toDateTime64",
        "toUInt64",
        "toUInt128",
        "toUInt256",
        "toInt32",
        "toInt64",
        "toInt128",
        "sleep",
        "toString",
        "toDate",
        "toDateTime",
        "toDateOrNull",
        "toDateOrDefault",
        "toDateTimeOrZero",
        "toDateTimeOrNull",
        "toDateTimeOrDefault",
        "toDate32",
        "toDate32OrZero",
        "toDate32OrNull",
        "toDate32OrDefault",
        "timeZone",
        "timezoneOf",
        "toStartOfMonth",
        "parseDateTime",
        "parseDateTimeOrZero",
        "parseDateTimeOrNull",
        "parseDateTimeInJodaSyntax",
        "parseDateTimeInJodaSyntaxOrZero",
        "parseDateTimeInJodaSyntaxOrNull",
        "parseDateTimeBestEffort",
        "snowflakeToDateTime",
        "snowflakeToDateTime64",
        "dateTimeToSnowflake",
        "dateTime64ToSnowflake",
        "empty",
        "notEmpty",
        "trimLeft",
        "trimRight",
        "trimBoth",
        "startsWith",
        "endsWith",
        "isNull",
        "isNotNull",
        "ifNull",
        "nullIf",
        "assumeNotNull",
        "toNullable",
        "emptyArrayString",
        "arrayConcat",
        "arrayElement",
        "arrayJoin",
        "hasAll",
        "hasAny",
        "arraySort",
        "arrayReverseSort",
        "arrayMin",
        "arrayMax",
        "arraySum",
        "arrayAvg",
        "arrayStringConcat",
        "notLike",
        "notILike",
        "regexpExtract",
        "divideDecimal",
        "translate",
        "translateUTF8",
        "mapFromArrays",
        "mapAdd",
        "mapContains",
        "mapKeys",
        "mapValues",
        "mapFilter",
        "isValidJSON",
        "JSONHas",
        "JSONLength",
        "JSONExtractString",
        "JSONExtract",
        "JSONExtractKeysAndValues",
        "toJSONString",
        "JSONArrayLength",
        "dictGet",
        "dictGetOrDefault",
        "dictGetOrNull",
        "dictHas",
        "dictGetHierarchy",
        "dictIsIn",
        "dictGetChildren",
        "dictGetDescendant",
        "greatCircleDistance",
        "geoDistance",
        "greatCircleAngle",
        "pointInEllipses",
        "pointInPolygon",
        "geohashEncode",
        "geohashDecode",
        "geohashesInBox",
        "evalMLMethod",
        "stochasticLinearRegression",
        "stochasticLogisticRegression",
        "encrypt",
        "aes_encrypt_mysql",
        "decrypt",
        "tryDecrypt",
        "aes_decrypt_mysql",
        "queryStringAndFragment",
        "extractURLParameter",
        "L1Norm",
        "L2Norm",
        "LinfNorm",
        "LpNorm",
        "L1Distance",
        "L2Distance",
        "LinfDistance",
        "LpDistance",
        "L1Normalize",
        "L2Normalize",
        "LinfNormalize",
        "LpNormalize",
        "cosineDistance"
    };
    private static final String[] CLICKHOUSE_NONKEYWORDS = {
        "DEFAULT",
        "SYSTEM"
    };

    private static final String[] CLICKHOUSE_KEYWORDS = {
        "COMMENT",
        "REPLACE",
        "ENGINE",
        "SHOW"
    };

    public ClickhouseSQLDialect() {
        super("Clickhouse SQL", "clickhouse");
    }

    @Override
    public boolean supportsOrderByIndex() {
        return false;
    }

    public void initDriverSettings(JDBCSession session, JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(session, dataSource, metaData);
        for (String word : CLICKHOUSE_NONKEYWORDS) {
            removeSQLKeyword(word);
        }
        addFunctions(Arrays.asList(CLICKHOUSE_FUNCTIONS));
        addSQLKeywords(Arrays.asList(CLICKHOUSE_KEYWORDS));

        setIdentifierQuoteString(new String[][]{
            { "`", "`" },
            { "\"", "\"" },
        });
    }

    @Override
    public boolean supportsSubqueries() {
        return true;
    }

    @Override
    public boolean supportsAliasInSelect() {
        return true;
    }

    @Override
    public String getColumnTypeModifiers(@NotNull DBPDataSource dataSource, @NotNull DBSTypedObject column, @NotNull String typeName, @NotNull DBPDataKind dataKind) {
        if (typeName.equals("String")) {
            return null;
        }
        return super.getColumnTypeModifiers(dataSource, column, typeName, dataKind);
    }

    @Override
    public boolean supportsNestedComments() {
        return true;
    }

    //We should quote keywords which is not keywords for clickhouse, otherwise JSQLParser can't parse statements
    @Override
    public boolean mustBeQuoted(@NotNull String str, boolean forceCaseSensitive) {
        for (String word : CLICKHOUSE_NONKEYWORDS) {
            if (word.equalsIgnoreCase(str)) {
                return true;
            }
        }
        for (int i = 0; i < str.length(); i++) {
            int c = str.charAt(i);
            if (Character.isLetter(c) && !(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z')) {
                return true;
            }
        }
        return super.mustBeQuoted(str, forceCaseSensitive);
    }

    @Override
    public char getStringEscapeCharacter() {
        return '\\';
    }

    @NotNull
    @Override
    public String getTypeCastClause(@NotNull DBSTypedObject attribute, String expression, boolean isInCondition) {
        String typeName = attribute.getTypeName();
        if (isInCondition && CommonUtils.isNotEmpty(typeName)) {
            String lowerTypeName = typeName.toLowerCase();
            if (ClickhouseConstants.DATA_TYPE_IPV4.equals(lowerTypeName)) {
                return "IPv4StringToNum(" + expression + ")";
            } else if (ClickhouseConstants.DATA_TYPE_IPV6.equals(lowerTypeName)) {
                return "IPv6StringToNum(" + expression + ")";
            }
        }
        return super.getTypeCastClause(attribute, expression, isInCondition);
    }

    @Override
    public boolean isEscapeBackslash() {
        return true;
    }
}
