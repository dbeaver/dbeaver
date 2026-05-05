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
package org.jkiss.dbeaver.model.lsm;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record LSMAnalyzerParameters (
        @NotNull Map<String, String> knownIdentifierQuotes,
        boolean isSqlParametersEnabled,
        boolean isAnonymousSqlParametersEnabled,
        char anonymousParameterMark,
        @NotNull List<Map.Entry<Integer, Set<String>>> namedParameterPrefixes,
        boolean variablesEnabled
) {

    public static LSMAnalyzerParameters forDialect(SQLDialect dialect, SQLSyntaxManager syntaxManager) {
        Map<String, String> identifierQuotPairs = Stream.of(Objects.requireNonNull(dialect.getIdentifierQuoteStrings()))
            .collect(Collectors.toUnmodifiableMap(q -> q[0], q -> q[1]));

        List<Map.Entry<Integer, Set<String>>> namedParameterPrefixes = Stream.of(Objects.requireNonNull(syntaxManager.getNamedParameterPrefixes()))
            .collect(Collectors.groupingBy(String::length, Collectors.toSet())).entrySet().stream()
            .sorted(Comparator.comparingInt(Map.Entry::getKey))
            .collect(Collectors.toList());

        return new LSMAnalyzerParameters(
            identifierQuotPairs,
            syntaxManager.isParametersEnabled(),
            syntaxManager.isAnonymousParametersEnabled(),
            syntaxManager.getAnonymousParameterMark(),
            namedParameterPrefixes,
            syntaxManager.isVariablesEnabled()
        );
    }
}
