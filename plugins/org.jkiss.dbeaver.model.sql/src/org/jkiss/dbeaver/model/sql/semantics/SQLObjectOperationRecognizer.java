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
package org.jkiss.dbeaver.model.sql.semantics;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.lsm.LSMAnalyzer;
import org.jkiss.dbeaver.model.lsm.LSMAnalyzerParameters;
import org.jkiss.dbeaver.model.lsm.sql.dialect.LSMDialectRegistry;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLObjectOperation;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.stm.STMErrorListener;
import org.jkiss.dbeaver.model.stm.STMKnownRuleNames;
import org.jkiss.dbeaver.model.stm.STMSource;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Recognizes database object operations from DDL syntax trees produced by the LSM parser.
 */
public final class SQLObjectOperationRecognizer {
    private SQLObjectOperationRecognizer() {
    }

    /**
     * Parses one query at execution time and recognizes its database object operation.
     */
    @Nullable
    public static SQLObjectOperation recognize(
        @NotNull SQLDialect dialect,
        @NotNull String queryText
    ) {
        SQLSyntaxManager syntaxManager = new SQLSyntaxManager();
        syntaxManager.init(dialect, syntaxManager.getPreferenceStore());
        return recognize(dialect, syntaxManager, queryText);
    }

    /**
     * Parses one query at execution time using the supplied syntax settings and recognizes its first database object
     * operation.
     */
    @Nullable
    public static SQLObjectOperation recognize(
        @NotNull SQLDialect dialect,
        @NotNull SQLSyntaxManager syntaxManager,
        @NotNull String queryText
    ) {
        return recognizeAll(dialect, syntaxManager, queryText).stream().findFirst().orElse(null);
    }

    /**
     * Recognizes an operation from an error-free LSM {@code sqlQueries}, {@code sqlQuery}, or
     * {@code sqlSchemaStatement} root. A {@code sqlQueries} root must contain exactly one complete query.
     *
     * @return the recognized operation, or {@code null} for invalid, multiple, or unsupported statements
     */
    @Nullable
    public static SQLObjectOperation recognize(@NotNull STMTreeNode root) {
        return recognizeAll(root).stream().findFirst().orElse(null);
    }

    /**
     * Parses one query at execution time and recognizes its database object operations.
     *
     * @return recognized operations in source order, or an empty immutable list for invalid, multiple, or unsupported
     *     statements
     */
    @NotNull
    public static List<SQLObjectOperation> recognizeAll(
        @NotNull SQLDialect dialect,
        @NotNull String queryText
    ) {
        SQLSyntaxManager syntaxManager = new SQLSyntaxManager();
        syntaxManager.init(dialect, syntaxManager.getPreferenceStore());
        return recognizeAll(dialect, syntaxManager, queryText);
    }

    /**
     * Parses one query at execution time using the supplied syntax settings and recognizes its database object
     * operations.
     *
     * @return recognized operations in source order, or an empty immutable list for invalid, multiple, or unsupported
     *     statements
     */
    @NotNull
    public static List<SQLObjectOperation> recognizeAll(
        @NotNull SQLDialect dialect,
        @NotNull SQLSyntaxManager syntaxManager,
        @NotNull String queryText
    ) {
        LSMAnalyzer analyzer = LSMDialectRegistry.getInstance().getAnalyzerFactoryForDialect(dialect)
            .createAnalyzer(LSMAnalyzerParameters.forDialect(dialect, syntaxManager));
        RecognitionErrorListener errorListener = new RecognitionErrorListener();
        STMTreeNode tree = analyzer.parseSqlQueriesTree(STMSource.fromString(queryText), errorListener);
        if (tree == null || errorListener.hasErrors) {
            return List.of();
        }
        return recognizeAll(tree);
    }

    /**
     * Recognizes operations from an error-free LSM {@code sqlQueries}, {@code sqlQuery}, or
     * {@code sqlSchemaStatement} root. A {@code sqlQueries} root must contain exactly one complete query.
     *
     * @return recognized operations in source order, or an empty immutable list for invalid, multiple, or unsupported
     *     statements
     */
    @NotNull
    public static List<SQLObjectOperation> recognizeAll(@NotNull STMTreeNode root) {
        if (hasErrors(root) || root.getNodeName().equals(STMKnownRuleNames.sqlQueries) &&
            root.findChildrenOfName(STMKnownRuleNames.sqlQuery).size() != 1) {
            return List.of();
        }
        STMTreeNode schemaStatement = unwrapSchemaStatement(root);
        if (schemaStatement == null) {
            return List.of();
        }
        STMTreeNode statement = schemaStatement.findFirstNonErrorChild();
        if (statement == null) {
            return List.of();
        }
        if (statement.getNodeName().equals(STMKnownRuleNames.dropTableStatement)) {
            return createOperations(
                statement,
                SQLObjectOperation.Operation.DROP,
                SQLObjectOperation.ObjectKind.TABLE,
                STMKnownRuleNames.tableName
            );
        }
        if (statement.getNodeName().equals(STMKnownRuleNames.dropViewStatement)) {
            return createOperations(
                statement,
                SQLObjectOperation.Operation.DROP,
                SQLObjectOperation.ObjectKind.VIEW,
                STMKnownRuleNames.tableName
            );
        }
        SQLObjectOperation operation = recognizeSingle(statement);
        return operation == null ? List.of() : List.of(operation);
    }

    @Nullable
    private static SQLObjectOperation recognizeSingle(@NotNull STMTreeNode statement) {
        if (statement.getNodeName().equals(STMKnownRuleNames.schemaDefinition)) {
            STMTreeNode name = findDescendant(statement, STMKnownRuleNames.schemaName);
            if (name == null) {
                return findDescendant(statement, STMKnownRuleNames.authorizationIdentifier) == null ? null :
                    new SQLObjectOperation(SQLObjectOperation.Operation.CREATE, SQLObjectOperation.ObjectKind.SCHEMA, List.of());
            }
            return createOperation(SQLObjectOperation.Operation.CREATE, SQLObjectOperation.ObjectKind.SCHEMA, name);
        }
        if (statement.getNodeName().equals(STMKnownRuleNames.createTableStatement)) {
            return createOperation(
                SQLObjectOperation.Operation.CREATE,
                SQLObjectOperation.ObjectKind.TABLE,
                findDescendant(statement, STMKnownRuleNames.tableName)
            );
        }
        if (statement.getNodeName().equals(STMKnownRuleNames.createViewStatement)) {
            return createOperation(
                SQLObjectOperation.Operation.CREATE,
                SQLObjectOperation.ObjectKind.VIEW,
                findDescendant(statement, STMKnownRuleNames.tableName)
            );
        }
        if (statement.getNodeName().equals(STMKnownRuleNames.createIndexStatement)) {
            return createIndexOperation(statement, SQLObjectOperation.Operation.CREATE);
        }
        if (statement.getNodeName().equals(STMKnownRuleNames.createNamedObjectStatement)) {
            return createNamedOperation(
                statement,
                SQLObjectOperation.Operation.CREATE,
                STMKnownRuleNames.createObjectKind
            );
        }
        if (statement.getNodeName().equals(STMKnownRuleNames.createCatalogDatabaseStatement)) {
            return createContainerOperation(statement, SQLObjectOperation.Operation.CREATE);
        }
        if (statement.getNodeName().equals(STMKnownRuleNames.alterTableStatement)) {
            return createOperation(
                SQLObjectOperation.Operation.ALTER,
                SQLObjectOperation.ObjectKind.TABLE,
                findDescendant(statement, STMKnownRuleNames.tableName)
            );
        }
        if (statement.getNodeName().equals(STMKnownRuleNames.alterNamedObjectStatement)) {
            return createNamedOperation(
                statement,
                SQLObjectOperation.Operation.ALTER,
                STMKnownRuleNames.alterObjectKind
            );
        }
        if (statement.getNodeName().equals(STMKnownRuleNames.dropSchemaStatement)) {
            return createOperation(
                SQLObjectOperation.Operation.DROP,
                SQLObjectOperation.ObjectKind.SCHEMA,
                findDescendant(statement, STMKnownRuleNames.schemaName)
            );
        }
        if (statement.getNodeName().equals(STMKnownRuleNames.dropCatalogDatabaseStatement)) {
            return createContainerOperation(statement, SQLObjectOperation.Operation.DROP);
        }
        if (statement.getNodeName().equals(STMKnownRuleNames.dropTableStatement)) {
            return createOperation(
                SQLObjectOperation.Operation.DROP,
                SQLObjectOperation.ObjectKind.TABLE,
                findDescendant(statement, STMKnownRuleNames.tableName)
            );
        }
        if (statement.getNodeName().equals(STMKnownRuleNames.dropViewStatement)) {
            return createOperation(
                SQLObjectOperation.Operation.DROP,
                SQLObjectOperation.ObjectKind.VIEW,
                findDescendant(statement, STMKnownRuleNames.tableName)
            );
        }
        if (statement.getNodeName().equals(STMKnownRuleNames.dropProcedureStatement)) {
            return createOperation(
                SQLObjectOperation.Operation.DROP,
                SQLObjectOperation.ObjectKind.PROCEDURE,
                findDescendant(statement, STMKnownRuleNames.qualifiedName)
            );
        }
        if (statement.getNodeName().equals(STMKnownRuleNames.dropIndexStatement)) {
            return createIndexOperation(statement, SQLObjectOperation.Operation.DROP);
        }
        if (statement.getNodeName().equals(STMKnownRuleNames.dropNamedObjectStatement)) {
            return createNamedOperation(
                statement,
                SQLObjectOperation.Operation.DROP,
                STMKnownRuleNames.dropObjectKind
            );
        }
        if (statement.getNodeName().equals(STMKnownRuleNames.alterContainerStatement)) {
            SQLObjectOperation.Operation operation = findDescendant(statement, STMKnownRuleNames.renameContainerAction) == null ?
                SQLObjectOperation.Operation.ALTER : SQLObjectOperation.Operation.RENAME;
            return createContainerOperation(statement, operation);
        }
        if (statement.getNodeName().equals(STMKnownRuleNames.renameNamedObjectStatement)) {
            return createNamedOperation(
                statement,
                SQLObjectOperation.Operation.RENAME,
                STMKnownRuleNames.renameObjectKind
            );
        }
        return null;
    }

    @Nullable
    private static SQLObjectOperation createIndexOperation(
        @NotNull STMTreeNode statement,
        @NotNull SQLObjectOperation.Operation operation
    ) {
        STMTreeNode indexNameNode = statement.findFirstChildOfName(STMKnownRuleNames.qualifiedName);
        STMTreeNode tableNameNode = statement.findFirstChildOfName(STMKnownRuleNames.tableName);
        List<String> indexName = getNameParts(indexNameNode);
        List<String> tableName = getNameParts(tableNameNode);
        if (indexName.size() == 1 && tableName.size() > 1) {
            List<String> qualifiedIndexName = new ArrayList<>(tableName.size());
            qualifiedIndexName.addAll(tableName.subList(0, tableName.size() - 1));
            qualifiedIndexName.add(indexName.getFirst());
            indexName = List.copyOf(qualifiedIndexName);
        }
        return indexName.isEmpty() ? null :
            new SQLObjectOperation(operation, SQLObjectOperation.ObjectKind.INDEX, indexName);
    }

    @Nullable
    private static SQLObjectOperation createNamedOperation(
        @NotNull STMTreeNode statement,
        @NotNull SQLObjectOperation.Operation operation,
        @NotNull String kindNodeName
    ) {
        STMTreeNode kindNode = findDescendant(statement, kindNodeName);
        STMTreeNode nameNode = findDescendant(statement, STMKnownRuleNames.qualifiedName);
        if (kindNode == null || nameNode == null) {
            return null;
        }
        return createOperation(operation, getObjectKind(kindNode), nameNode);
    }

    @Nullable
    private static STMTreeNode unwrapSchemaStatement(@NotNull STMTreeNode root) {
        STMTreeNode node = root;
        if (node.getNodeName().equals(STMKnownRuleNames.sqlQueries)) {
            node = node.findFirstNonErrorChild();
        }
        if (node != null && node.getNodeName().equals(STMKnownRuleNames.sqlQuery)) {
            node = node.findFirstNonErrorChild();
        }
        return node != null && node.getNodeName().equals(STMKnownRuleNames.sqlSchemaStatement) ? node : null;
    }

    @Nullable
    private static SQLObjectOperation createContainerOperation(
        @NotNull STMTreeNode statement,
        @NotNull SQLObjectOperation.Operation operation
    ) {
        STMTreeNode kindNode = findDescendant(statement, STMKnownRuleNames.containerKind);
        if (kindNode == null) {
            kindNode = findDescendant(statement, STMKnownRuleNames.alterContainerKind);
        }
        STMTreeNode nameNode = findDescendant(statement, STMKnownRuleNames.qualifiedName);
        if (kindNode == null || nameNode == null) {
            return null;
        }
        return createOperation(operation, getObjectKind(kindNode), nameNode);
    }

    @NotNull
    private static SQLObjectOperation.ObjectKind getObjectKind(@NotNull STMTreeNode kindNode) {
        String kindName = kindNode.getText().toUpperCase(Locale.ENGLISH);
        if (kindName.equals("MATERIALIZEDVIEW")) {
            return SQLObjectOperation.ObjectKind.VIEW;
        }
        try {
            return SQLObjectOperation.ObjectKind.valueOf(kindName);
        } catch (IllegalArgumentException e) {
            return SQLObjectOperation.ObjectKind.OTHER;
        }
    }

    @Nullable
    private static SQLObjectOperation createOperation(
        @NotNull SQLObjectOperation.Operation operation,
        @NotNull SQLObjectOperation.ObjectKind objectKind,
        @Nullable STMTreeNode nameNode
    ) {
        if (nameNode == null) {
            return null;
        }
        List<String> nameParts = getNameParts(nameNode);
        return nameParts.isEmpty() ? null : new SQLObjectOperation(operation, objectKind, nameParts);
    }

    @NotNull
    private static List<String> getNameParts(@Nullable STMTreeNode nameNode) {
        if (nameNode == null) {
            return List.of();
        }
        STMTreeNode qualifiedName = nameNode.getNodeName().equals(STMKnownRuleNames.qualifiedName) ? nameNode :
            findDescendant(nameNode, STMKnownRuleNames.qualifiedName);
        if (qualifiedName == null) {
            return List.of();
        }
        return qualifiedName.findChildrenOfName(STMKnownRuleNames.identifier).stream()
            .map(STMTreeNode::getTextContent)
            .toList();
    }

    @Nullable
    private static STMTreeNode findDescendant(@NotNull STMTreeNode node, @NotNull String nodeName) {
        if (node.getNodeName().equals(nodeName)) {
            return node;
        }
        for (STMTreeNode child : node.findNonErrorChildren()) {
            STMTreeNode result = findDescendant(child, nodeName);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    @NotNull
    private static List<SQLObjectOperation> createOperations(
        @NotNull STMTreeNode statement,
        @NotNull SQLObjectOperation.Operation operation,
        @NotNull SQLObjectOperation.ObjectKind objectKind,
        @NotNull String nameNodeName
    ) {
        return statement.findChildrenOfName(nameNodeName).stream()
            .map(name -> createOperation(operation, objectKind, name))
            .filter(Objects::nonNull)
            .toList();
    }

    private static boolean hasErrors(@NotNull STMTreeNode node) {
        if (node instanceof ErrorNode || node.hasErrorChildren()) {
            return true;
        }
        for (STMTreeNode child : node.getChildren()) {
            if (hasErrors(child)) {
                return true;
            }
        }
        return false;
    }

    private static final class RecognitionErrorListener extends BaseErrorListener implements STMErrorListener {
        private boolean hasErrors;

        @Override
        public void syntaxError(
            @NotNull Recognizer<?, ?> recognizer,
            @Nullable Object offendingSymbol,
            int line,
            int charPositionInLine,
            @NotNull String msg,
            @Nullable RecognitionException e
        ) {
            hasErrors = true;
        }
    }
}
