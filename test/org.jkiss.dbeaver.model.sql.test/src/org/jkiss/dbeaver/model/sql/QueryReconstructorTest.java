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
import org.jkiss.dbeaver.model.sql.parser.QueryReconstructor;
import org.jkiss.dbeaver.model.sql.parser.SQLSemanticProcessor;
import org.jkiss.junit.DBeaverUnitTest;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class QueryReconstructorTest extends DBeaverUnitTest {

    @NotNull
    private final Map<String, Pattern> patternsCache = new HashMap<>();

    private record ColumnInfo(
        @NotNull String name
    ) {
    }

    private record TableInfo(
        @NotNull String name,
        @NotNull String qualifiedName,
        @NotNull List<ColumnInfo> columns
    ) {
    }

    private record SchemaInfo(
        @NotNull String name,
        @NotNull List<TableInfo> tables
    ) {
    }

    private record CatalogInfo(
        @NotNull String name,
        @NotNull List<SchemaInfo> schemas,
        @NotNull List<TableInfo> allTables
    ) {
    }

    private enum JoinKind {
        DEFAULT,
        INNER,
        LEFT,
        RIGHT
    }

    private record ResultColumn(
        @NotNull String referenceString,
        @NotNull ColumnInfo columnInfo,
        @Nullable Pair<String, String> comments
    ) {
        @NotNull
        public ResultColumn update(@Nullable Pair<String, String> comments) {
            return new ResultColumn(this.referenceString, this.columnInfo, comments);
        }
    }

    private record SourceModel(
        @NotNull String name,
        @NotNull TableInfo table,
        @Nullable String alias,
        @NotNull JoinKind joinKind,
        @NotNull String joinCondition,
        @NotNull List<ResultColumn> resultColumns,
        @Nullable Pair<String, String> comments
    ) {
        @NotNull
        public SourceModel update(@NotNull List<ResultColumn> resultColumns, @Nullable Pair<String, String> comments) {
            return new  SourceModel(
                this.name,
                this.table,
                this.alias,
                this.joinKind,
                this.joinCondition,
                resultColumns,
                comments
            );
        }

        @NotNull
        public String prepareString() {
            String sourceIntro = this.name + (this.alias == null ? "" : (" AS " + this.alias));
            return switch (this.joinKind()) {
                case INNER -> "INNER";
                case LEFT -> "LEFT";
                case RIGHT -> "RIGHT";
                default -> "";
            } + (this.joinKind == JoinKind.DEFAULT ? sourceIntro : (" JOIN " + sourceIntro + " ON " + this.joinCondition));
        }
    }

    private record QueryModel(
        int columnsCount,
        @NotNull List<SourceModel> sources
    ) {
        @NotNull
        public String prepareString() {
            return String.join("\n", this.prepareStringParts(true));
        }

        @NotNull
        public String prepareStringWithoutComments() {
            return String.join("\n", this.prepareStringParts(false).stream().filter(CommonUtils::isNotEmpty).toList());
        }

        @NotNull
        public List<String> prepareStringParts(boolean withComments) {
            var lines = new ArrayList<String>();
            lines.add("SELECT");
            int i = this.sources.stream().mapToInt(s -> s.resultColumns.size()).sum();
            for (var s : this.sources) {
                for (var c : s.resultColumns) {
                    var comments = c.comments();
                    if (withComments && comments != null && comments.getFirst() != null) {
                        lines.add(comments.getFirst());
                    }
                    lines.add(c.referenceString + (i-->1 ? "," : ""));
                    if (withComments && comments != null && comments.getSecond() != null) {
                        lines.add(comments.getSecond());
                    }
                }
            }
            lines.add("FROM");
            for (var s : this.sources) {
                var comments = s.comments();
                if (withComments && comments != null && comments.getFirst() != null) {
                    lines.add(comments.getFirst());
                }

                lines.add(s.prepareString());

                if (withComments && comments != null && comments.getSecond() != null) {
                    lines.add(comments.getSecond());
                }
            }
            return lines;
        }
    }

    private static class DbModelGenerator {
        private final Random rnd = new Random(33);
        private long commentCounter = 0;

        @NotNull
        private String generateNextComment() {
            return "-- " + (commentCounter++);
        }

        @NotNull
        public String generateLinearName(@Nullable String prefix, int minLength, int number) {
            StringBuilder sb = new StringBuilder();
            if (prefix != null) {
                sb.append(prefix);
            }

            for (int i = 0; i < minLength; i++) {
                sb.append((char) this.rnd.nextInt('a', 'z'));
            }

            final int m = 'z' - 'a';
            for (int n = number; n > 0; n = n / m) {
                sb.append((char) ('a' + n % m));
            }

            return sb.toString();
        }

        @NotNull
        public CatalogInfo generateDbModel(int schemaCount, int tablesAvgCount, int columnsAvgCount) {
            var schemas = new ArrayList<SchemaInfo>(schemaCount);
            var allTables = new ArrayList<TableInfo>();
            for (int i = 0; i < schemaCount; i++) {
                var schemaName = this.generateLinearName("s_", 3, i);
                var tablesCount = rnd.nextInt(tablesAvgCount - tablesAvgCount / 4, tablesAvgCount + tablesAvgCount / 4);
                var tables = new ArrayList<TableInfo>(tablesCount);
                for (int j = 0; j < tablesCount; j++) {
                    var columnsCount = rnd.nextInt(columnsAvgCount - columnsAvgCount / 4, columnsAvgCount + columnsAvgCount / 4);
                    var columns = new ArrayList<ColumnInfo>(columnsCount);
                    var tableName = this.generateLinearName("t_", 3, j);
                    for (int k = 0; k < columnsCount; k++) {
                        columns.add(new ColumnInfo(this.generateLinearName("c_", 2, k)));
                    }
                    var t = new TableInfo(tableName, schemaName + "." + tableName, columns);
                    tables.add(t);
                    allTables.add(t);
                }
                schemas.add(new SchemaInfo(schemaName, tables));
            }
            return new CatalogInfo("cat", schemas, allTables);
        }

        @NotNull
        private HashSet<Integer> generateDistinctNumbers(int amount, int from, int to) {
            HashSet<Integer> numbers = new  HashSet<>();
            while (numbers.size() < amount) {
                numbers.add(rnd.nextInt(from, to));
            }
            return numbers;
        }

        @NotNull
        public QueryModel generateQuery(@NotNull CatalogInfo db, int sourcesCount) {
            var froms = new ArrayList<SourceModel>();
            int totalColumnsCount = 0;
            {
                int anum = 0;
                var tablesList = this.generateDistinctNumbers(sourcesCount, 0, db.allTables.size())
                    .stream()
                    .map(db.allTables::get)
                    .toList();
                for (int i = 0; i < tablesList.size(); i++) {
                    var table =  tablesList.get(i);
                    var tableName = rnd.nextInt(0, 100) < 70 ? table.name : table.qualifiedName;
                    var tableAlias = rnd.nextInt(0, 100) < 50 ? null : this.generateLinearName("a", 0, anum++);
                    var tableRef = tableAlias == null ? "" : (tableAlias + ".");

                    var sourceResultColumns = new ArrayList<ResultColumn>();
                    var columnsCount =  rnd.nextInt(2, table.columns.size());
                    for (int columIndex : this.generateDistinctNumbers(columnsCount, 0, table.columns.size())) {
                        ColumnInfo column = table.columns.get(columIndex);
                        sourceResultColumns.add(new ResultColumn(tableRef + column.name, column, null));
                        totalColumnsCount++;
                    }

                    var sourceToJoin = i == 0 ? null : froms.get(rnd.nextInt(froms.size()));
                    var source = new SourceModel(
                        tableName,
                        table,
                        tableAlias,
                        i == 0 ? JoinKind.DEFAULT : JoinKind.values()[rnd.nextInt(1, JoinKind.values().length)],
                        i == 0 ? "" : (
                            tableRef + table.columns.get(rnd.nextInt(table.columns.size())).name + " = " +
                            (sourceToJoin.alias == null ? "" : (sourceToJoin.alias + ".")) +
                             sourceToJoin.table.columns.get(rnd.nextInt(sourceToJoin.table.columns.size())).name
                        ),
                        sourceResultColumns,
                        null
                    );
                    froms.add(source);
                }
            }

            return new QueryModel(totalColumnsCount, froms);
        }

        @NotNull
        private  Pair<String, String> generateCommentEntry() {
            var comment = this.generateNextComment();
            var p = this.rnd.nextInt(0, 100);
            return Pair.of(
                p > 50 ? comment : null,
                p > 50 ? null : comment
            );
        }

        @NotNull
        public QueryModel generateComments(@NotNull QueryModel query, int commentsPerColumns, int commentsPerSources) {
            HashSet<Integer> sourceIndexes = this.generateDistinctNumbers(commentsPerSources, 0, query.sources.size());
            HashSet<Integer> columnIndexes = this.generateDistinctNumbers(commentsPerColumns, 0, query.columnsCount);

            var sources = new ArrayList<>(query.sources());

            int columnIndex = 0;
            for (int i = 0; i < sources.size(); i++) {
                var source = sources.get(i);
                var resultColumns = new ArrayList<>(source.resultColumns);
                for (int j = 0; j < resultColumns.size(); j++) {
                    if (columnIndexes.contains(columnIndex)) {
                        resultColumns.set(j, resultColumns.get(j).update(this.generateCommentEntry()));
                    }
                    columnIndex++;
                }
                sources.set(i, source.update(resultColumns, sourceIndexes.contains(i) ? this.generateCommentEntry() : null));
            }

            return new QueryModel(query.columnsCount, sources);
        }

        @NotNull
        public TestCase generateTestCase(
            @NotNull CatalogInfo db,
            @NotNull QueryModel query,
             int columnsToAdd,
             int columnsToRemove,
             int sourcesToAdd,
             int sourcesToRemove
        ) {
            var modifiedSources = new ArrayList<SourceModel>();
            var commentsToAssert = new HashSet<String>();
            var removalsToAssert = new HashSet<String>();
            var additionsToAssert = new HashSet<String>();

            int totalResultColumns = 0;

            // removals
            {
                Set<Integer> columnIndexesToRemove = columnsToRemove == 0 ? Collections.emptySet()
                    : this.generateDistinctNumbers(columnsToRemove, 0, query.columnsCount);
                Set<Integer> sourceIndexesToRemove = sourcesToAdd == 0 ? Collections.emptySet()
                    : this.generateDistinctNumbers(sourcesToRemove, 1, query.sources().size());

                boolean lastColumnToRemove = columnIndexesToRemove.contains(query.columnsCount - 1);
                boolean firstSourceToRemove = sourceIndexesToRemove.contains(0);

                int columnIndex = 0;
                for (int i = 0; i < query.sources.size(); i++) {
                    SourceModel source = query.sources.get(i);
                    if (sourceIndexesToRemove.contains(i)) {
                        removalsToAssert.add(source.prepareString());
                        columnIndex += source.resultColumns.size();
                    } else {
                        var newResultColumns = new ArrayList<ResultColumn>();
                        for (int j = 0; j < source.resultColumns.size(); j++) {
                            var column = source.resultColumns.get(j);
                            if (columnIndexesToRemove.contains(columnIndex)) {
                                removalsToAssert.add(column.referenceString());
                            } else {
                                newResultColumns.add(column);
                                totalResultColumns++;
                            }
                            var columnComment = column.comments();
                            if (columnComment != null && !columnIndexesToRemove.contains(columnIndex) &&
                                !columnIndexesToRemove.contains(columnIndex - 1) && !columnIndexesToRemove.contains(columnIndex + 1) &&
                                !(firstSourceToRemove && columnIndex == query.columnsCount())) {
                                if (columnComment.getFirst() != null) {
                                    commentsToAssert.add(columnComment.getFirst());
                                }
                                if (columnComment.getSecond() != null) {
                                    commentsToAssert.add(columnComment.getSecond());
                                }
                            }
                            columnIndex++;
                        }
                        modifiedSources.add(source.update(newResultColumns, source.comments()));
                    }

                    var sourceComments = source.comments();
                    if (sourceComments != null && !sourceIndexesToRemove.contains(i) &&
                        !sourceIndexesToRemove.contains(i - 1) && !sourceIndexesToRemove.contains(i + 1) &&
                        !(lastColumnToRemove && i == 0)) {
                        if (sourceComments.getFirst() != null) {
                            commentsToAssert.add(sourceComments.getFirst());
                        }
                        if (sourceComments.getSecond() != null) {
                            commentsToAssert.add(sourceComments.getSecond());
                        }
                    }
                }
            }

            // additions
            if (sourcesToAdd > 0 || columnsToAdd > 0) {
                int anum = 0;
                List<TableInfo> newSourceTables = new ArrayList<>(db.allTables);
                query.sources.stream().map(SourceModel::table).forEach(newSourceTables::remove);
                newSourceTables = generateDistinctNumbers(Math.min(sourcesToAdd, newSourceTables.size()), 0, newSourceTables.size())
                    .stream()
                    .map(newSourceTables::get)
                    .toList();

                var existingColumns = modifiedSources.stream()
                    .flatMap(s -> s.resultColumns().stream())
                    .map(rc -> rc.columnInfo)
                    .collect(Collectors.toSet());
                var columnsAvailable = newSourceTables.stream().mapToInt(t -> t.columns().size()).sum();
                var newSourceColumnIndexes = this.generateDistinctNumbers(
                    Math.min(columnsToAdd, columnsAvailable), 0, columnsAvailable
                );

                int newSourceColumnIndex = 0;
                for (TableInfo table : newSourceTables) {
                    var tableName = rnd.nextInt(0, 100) < 70 ? table.name : table.qualifiedName;
                    var tableAlias = rnd.nextInt(0, 100) < 50 ? null : this.generateLinearName("aa", 0, anum++);
                    var tableRef = tableAlias == null ? "" : (tableAlias + ".");

                    var sourceResultColumns = new ArrayList<ResultColumn>();
                    for (ColumnInfo column : table.columns()) {
                        if (newSourceColumnIndexes.contains(newSourceColumnIndex) && !existingColumns.contains(column)) {
                            String referenceString = tableRef + column.name;
                            additionsToAssert.add(referenceString);
                            sourceResultColumns.add(new ResultColumn(referenceString, column, null));
                            totalResultColumns++;
                        }
                        newSourceColumnIndex++;
                    }

                    var sourceToJoin = modifiedSources.get(rnd.nextInt(modifiedSources.size()));
                    var source = new SourceModel(
                        tableName, table, tableAlias,
                        JoinKind.values()[rnd.nextInt(1, JoinKind.values().length)],
                        (
                            tableRef + table.columns.get(rnd.nextInt(table.columns.size())).name + " = " +
                                (sourceToJoin.alias == null ? "" : (sourceToJoin.alias + ".")) +
                                sourceToJoin.table.columns.get(rnd.nextInt(sourceToJoin.table.columns.size())).name
                        ),
                        sourceResultColumns,
                        null
                    );
                    additionsToAssert.add(source.prepareString());
                    modifiedSources.add(rnd.nextInt(1, modifiedSources.size()), source);
                }
            }

            var modifiedQuery = new QueryModel(totalResultColumns, modifiedSources);

            removalsToAssert.removeIf(r -> modifiedQuery.sources().stream().anyMatch(s -> s.prepareString().contains(r)));
            removalsToAssert.removeAll(additionsToAssert);
            additionsToAssert.removeAll(removalsToAssert);

            return new TestCase(
                query.prepareString(),
                modifiedQuery.prepareStringWithoutComments(),
                commentsToAssert,
                removalsToAssert,
                additionsToAssert
            );
        }
    }

    private record TestCase(
        @NotNull String originalTextWithComments,
        @NotNull String newTextWithoutComments,
        @NotNull Set<String> commentsToAssert,
        @NotNull Set<String> removalsToAssert,
        @NotNull Set<String> additionsToAssert
    ) {
    }

    @Test
    public void diffCases() throws Exception {
        final int testQueries = 20;
        final int commentCasesPerQuery = 10;
        final int modificationCasesPerCommentsCase = 10;

        int commentAssertsChecked = 0;
        int removalAssertsChecked = 0;
        int additionAssertsChecked = 0;

        var g = new DbModelGenerator();
        var db = g.generateDbModel(3, 10, 10);

        for (int i = 0; i < testQueries; i++) {
            final int sourceTablesCount = g.rnd.nextInt(2, 6);
            var originalQuery = g.generateQuery(db, sourceTablesCount);
            var sanitizedOriginalText = SQLSemanticProcessor.parseQuery(BasicSQLDialect.INSTANCE, originalQuery.prepareString()).toString();

            for (int j = 0; j < commentCasesPerQuery; j++) {
                var cq = g.generateComments(
                    originalQuery,
                    originalQuery.columnsCount / 3,
                    originalQuery.sources.size() / 2
                );
                final int amountOfColumnModifications = cq.columnsCount / 3;
                final int amountOfSourceModifications = cq.sources.size() / 3;

                var r = new QueryReconstructor(BasicSQLDialect.INSTANCE, cq.prepareString());

                for (int k = 0; k < modificationCasesPerCommentsCase; k++) {
                    final int columnsToRemove =  g.rnd.nextInt(
                        1,
                        Math.max(2, Math.min(cq.columnsCount - 1, amountOfColumnModifications))
                    );
                    final int sourcesToRemove =  g.rnd.nextInt(
                        0,
                        Math.max(1, Math.min(cq.sources.size() - 1, amountOfSourceModifications))
                    );
                    var t = g.generateTestCase(db, cq, 0, columnsToRemove, 0, sourcesToRemove);

                    String reconstructedText = r.reconstructFromOriginalFragments(t.newTextWithoutComments);
                    for (String comment : t.commentsToAssert()) {
                        Assert.assertTrue(reconstructedText.contains(comment));
                        commentAssertsChecked++;
                    }
                }
                for (int k = 0; k < modificationCasesPerCommentsCase; k++) {
                    final int columnsToAdd = g.rnd.nextInt(1, Math.max(2, amountOfColumnModifications));
                    final int sourcesToAdd = g.rnd.nextInt(1, Math.max(2, amountOfSourceModifications));
                    var t = g.generateTestCase(db, cq, columnsToAdd, 0, sourcesToAdd, 0);

                    String reconstructedText = r.reconstructFromOriginalFragments(t.newTextWithoutComments);
                    for (String comment : t.commentsToAssert()) {
                        Assert.assertTrue(reconstructedText.contains(comment));
                        commentAssertsChecked++;
                    }
                }
                for (int k = 0; k < modificationCasesPerCommentsCase; k++) {
                    final int columnsToRemove =  g.rnd.nextInt(
                        1,
                        Math.max(2, Math.min(cq.columnsCount - 1, amountOfColumnModifications))
                    );
                    final int columnsToAdd = g.rnd.nextInt(1, Math.max(2, amountOfColumnModifications));
                    final int sourcesToRemove =  g.rnd.nextInt(
                        0,
                        Math.max(1, Math.min(cq.sources.size() - 1, amountOfSourceModifications))
                    );
                    final int sourcesToAdd = g.rnd.nextInt(1, Math.max(2, amountOfSourceModifications));
                    var t = g.generateTestCase(db, cq, columnsToAdd, columnsToRemove, sourcesToAdd, sourcesToRemove);

                    String rawReconstructedText = r.reconstructFromOriginalFragments(t.newTextWithoutComments);
                    String cleanReconstructedText = rawReconstructedText.replaceAll("\\s+", " ");
                    for (String comment : t.commentsToAssert()) {
                        Assert.assertTrue(cleanReconstructedText.contains(comment));
                        commentAssertsChecked++;
                    }
                    String sanitizedReconstructedText = SQLSemanticProcessor.parseQuery(
                        BasicSQLDialect.INSTANCE,
                        rawReconstructedText
                    ).toString();
                    for (String removed : t.removalsToAssert()) {
                        assertContains(sanitizedOriginalText, removed);
                        assertNotContains(sanitizedReconstructedText, removed);
                        removalAssertsChecked++;
                    }
                    for (String added : t.additionsToAssert()) {
                        assertNotContains(sanitizedOriginalText, added);
                        assertContains(sanitizedReconstructedText, added);
                        additionAssertsChecked++;
                    }
                }
            }
        }
        System.out.println("Comment asserts checked: " + commentAssertsChecked);
        System.out.println("Removal asserts checked: " + removalAssertsChecked);
        System.out.println("Addition asserts checked: " + additionAssertsChecked);
    }

    @NotNull
    private Pattern getPattern(@NotNull String string) {
        // using regex-based matching with alphanumerical boundaries instead of String::contains
        // because substrings are containing each other sometimes
        return this.patternsCache.computeIfAbsent(string, s -> Pattern.compile("\\b" + Pattern.quote(s) + "\\b"));
    }

    private void assertContains(@NotNull String original, @NotNull String substring) {
        Assert.assertTrue(getPattern(substring).matcher(original).find());
    }

    private void assertNotContains(@NotNull String original, @NotNull String substring) {
        Assert.assertFalse(getPattern(substring).matcher(original).find());
    }
}
