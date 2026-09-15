/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recognizes container DDL statements not supported by the semantic parser.
 */
final class SQLContainerDdlParser {
    private static final String SQL_IDENTIFIER_PATTERN =
        "(?:\"(?:[^\"]|\"\")*\"|`(?:[^`]|``)*`|\\[(?:[^]]|]])*]|(?:(?!--|/\\*)[^\\s.;])+)";
    private static final String SQL_SEPARATOR_PATTERN = "(?:\\s|--[^\\r\\n]*(?:\\R|$)|/\\*.*?\\*/)";
    private static final String QUALIFIED_IDENTIFIER_PATTERN = SQL_IDENTIFIER_PATTERN +
        "(?:" + SQL_SEPARATOR_PATTERN + "*\\." + SQL_SEPARATOR_PATTERN + "*" + SQL_IDENTIFIER_PATTERN + ")*";
    private static final Pattern CONTAINER_DDL_PATTERN = Pattern.compile(
        "^" + SQL_SEPARATOR_PATTERN + "*(CREATE|DROP|ALTER)" + SQL_SEPARATOR_PATTERN + "+" +
            "(?:(OR" + SQL_SEPARATOR_PATTERN + "+REPLACE)" + SQL_SEPARATOR_PATTERN + "+)?" +
            "(SCHEMA|DATABASE|CATALOG)" + SQL_SEPARATOR_PATTERN + "+" +
            "(?:IF" + SQL_SEPARATOR_PATTERN + "+(?:NOT" + SQL_SEPARATOR_PATTERN + "+)?EXISTS" +
            SQL_SEPARATOR_PATTERN + "+)?(" + QUALIFIED_IDENTIFIER_PATTERN + ")(.*)$",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern ALTER_RENAME_PATTERN = Pattern.compile(
        "^" + SQL_SEPARATOR_PATTERN + "*RENAME" + SQL_SEPARATOR_PATTERN + "+TO" + SQL_SEPARATOR_PATTERN + "+" +
            QUALIFIED_IDENTIFIER_PATTERN + "(?:" + SQL_SEPARATOR_PATTERN + "|;)*$",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern QUALIFIED_IDENTIFIER_PART_PATTERN = Pattern.compile(
        "\\G(" + SQL_IDENTIFIER_PATTERN + ")(?:" + SQL_SEPARATOR_PATTERN + "*\\." + SQL_SEPARATOR_PATTERN + "*|$)",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private SQLContainerDdlParser() {
    }

    @Nullable
    static ContainerDdl parse(@Nullable SQLDialect dialect, @NotNull String sql) {
        SQLDialect effectiveDialect = dialect == null ? BasicSQLDialect.INSTANCE : dialect;
        Matcher matcher = CONTAINER_DDL_PATTERN.matcher(SQLUtils.maskDialectLineComments(effectiveDialect, sql));
        if (!matcher.matches()) {
            return null;
        }
        String action = matcher.group(1);
        if (matcher.group(2) != null && !"CREATE".equalsIgnoreCase(action)) {
            return null;
        }
        Matcher identifierMatcher = QUALIFIED_IDENTIFIER_PART_PATTERN.matcher(matcher.group(4));
        List<String> targetNameParts = new ArrayList<>();
        while (identifierMatcher.find()) {
            targetNameParts.add(identifierMatcher.group(1));
        }
        if (targetNameParts.isEmpty()) {
            return null;
        }
        Operation operation = "ALTER".equalsIgnoreCase(action) ?
            ALTER_RENAME_PATTERN.matcher(matcher.group(5)).matches() ? Operation.RENAME : Operation.ALTER :
            Operation.valueOf(action.toUpperCase(Locale.ENGLISH));
        return new ContainerDdl(
            ContainerType.valueOf(matcher.group(3).toUpperCase(Locale.ENGLISH)),
            operation,
            List.copyOf(targetNameParts)
        );
    }

    enum ContainerType {
        SCHEMA,
        DATABASE,
        CATALOG
    }

    enum Operation {
        CREATE,
        DROP,
        ALTER,
        RENAME
    }

    record ContainerDdl(
        @NotNull ContainerType type,
        @NotNull Operation operation,
        @NotNull List<String> targetNameParts
    ) {
    }
}
