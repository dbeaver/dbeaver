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
package org.jkiss.dbeaver.model.lsm;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class LSMAnalyzerParameters {
    private final Map<String, String> knownIdentifierQuotes;
    private final boolean isSqlParametersEnabled;
    private final boolean isAnonymousSqlParametersEnabled;
    private final char anonymousParameterMark;
    private final List<Map.Entry<Integer, Set<String>>> namedParameterPrefixes;
    private final boolean variablesEnabled;
    private final boolean hashLineCommentsEnabled;
    private final SQLDialect.ProceduralSyntaxFamily proceduralSyntaxFamily;
    private final SQLDialect.TrailingSelectIntoSupport trailingSelectIntoSupport;
    private final Set<String> typeModifierKeywords;
    private final Set<String> signatureModifierKeywords;
    private final Set<String> knownTypeNames;

    private LSMAnalyzerParameters(
        @NotNull Map<String, String> knownIdentifierQuotes,
        boolean isSqlParametersEnabled,
        boolean isAnonymousSqlParametersEnabled,
        char anonymousParameterMark,
        @NotNull List<Map.Entry<Integer, Set<String>>> namedParameterPrefixes,
        boolean variablesEnabled,
        boolean hashLineCommentsEnabled,
        @NotNull SQLDialect.ProceduralSyntaxFamily proceduralSyntaxFamily,
        @NotNull SQLDialect.TrailingSelectIntoSupport trailingSelectIntoSupport,
        @NotNull Set<String> typeModifierKeywords,
        @NotNull Set<String> signatureModifierKeywords,
        @NotNull Set<String> knownTypeNames
    ) {
        this.knownIdentifierQuotes = knownIdentifierQuotes;
        this.isSqlParametersEnabled = isSqlParametersEnabled;
        this.isAnonymousSqlParametersEnabled = isAnonymousSqlParametersEnabled;
        this.anonymousParameterMark = anonymousParameterMark;
        this.namedParameterPrefixes = namedParameterPrefixes;
        this.variablesEnabled = variablesEnabled;
        this.hashLineCommentsEnabled = hashLineCommentsEnabled;
        this.proceduralSyntaxFamily = proceduralSyntaxFamily;
        this.trailingSelectIntoSupport = trailingSelectIntoSupport;
        this.typeModifierKeywords = typeModifierKeywords;
        this.signatureModifierKeywords = signatureModifierKeywords;
        this.knownTypeNames = knownTypeNames;
    }

    @NotNull
    public Map<String, String> knownIdentifierQuotes() {
        return this.knownIdentifierQuotes;
    }

    public boolean isSqlParametersEnabled() {
        return this.isSqlParametersEnabled;
    }

    public boolean isAnonymousSqlParametersEnabled() {
        return this.isAnonymousSqlParametersEnabled;
    }

    public char anonymousParameterMark() {
        return this.anonymousParameterMark;
    }

    @NotNull
    public List<Map.Entry<Integer, Set<String>>> namedParameterPrefixes() {
        return this.namedParameterPrefixes;
    }

    public boolean variablesEnabled() {
        return this.variablesEnabled;
    }

    public boolean hashLineCommentsEnabled() {
        return this.hashLineCommentsEnabled;
    }

    @NotNull
    public SQLDialect.ProceduralSyntaxFamily proceduralSyntaxFamily() {
        return this.proceduralSyntaxFamily;
    }

    @NotNull
    public SQLDialect.TrailingSelectIntoSupport trailingSelectIntoSupport() {
        return this.trailingSelectIntoSupport;
    }

    @NotNull
    public Set<String> typeModifierKeywords() {
        return this.typeModifierKeywords;
    }

    @NotNull
    public Set<String> signatureModifierKeywords() {
        return this.signatureModifierKeywords;
    }

    @NotNull
    public Set<String> knownTypeNames() {
        return this.knownTypeNames;
    }

    @NotNull
    public static LSMAnalyzerParameters forDialect(@NotNull SQLDialect dialect, @NotNull SQLSyntaxManager syntaxManager) {
        String[][] identifierQuotes = dialect.getIdentifierQuoteStrings();
        Map<String, String> identifierQuotPairs = Stream.of(identifierQuotes == null ? new String[0][] : identifierQuotes)
            .collect(Collectors.toUnmodifiableMap(q -> q[0], q -> q[1]));

        List<Map.Entry<Integer, Set<String>>> namedParameterPrefixes = Stream.of(Objects.requireNonNull(syntaxManager.getNamedParameterPrefixes()))
            .collect(Collectors.groupingBy(String::length, Collectors.toSet())).entrySet().stream()
            .sorted(Comparator.comparingInt(Map.Entry::getKey))
            .collect(Collectors.toList());

        Set<String> knownTypeNames = dialect.getDataTypes(null).stream()
            .map(name -> name.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT))
            .filter(name -> !name.isEmpty())
            .collect(Collectors.toUnmodifiableSet());

        return new LSMAnalyzerParameters(
            identifierQuotPairs,
            syntaxManager.isParametersEnabled(),
            syntaxManager.isAnonymousParametersEnabled(),
            syntaxManager.getAnonymousParameterMark(),
            namedParameterPrefixes,
            syntaxManager.isVariablesEnabled(),
            Arrays.asList(Objects.requireNonNullElseGet(dialect.getSingleLineComments(), () -> new String[0])).contains("#"),
            dialect.getProceduralSyntaxFamily(),
            dialect.getTrailingSelectIntoSupport(),
            dialect.getTypeModifierKeywords(),
            dialect.getSignatureModifierKeywords(),
            knownTypeNames
        );
    }
}
