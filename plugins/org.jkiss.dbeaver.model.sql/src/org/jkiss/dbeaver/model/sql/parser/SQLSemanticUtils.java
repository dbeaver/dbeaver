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
package org.jkiss.dbeaver.model.sql.parser;

import net.sf.jsqlparser.parser.ASTNodeAccess;
import net.sf.jsqlparser.parser.Node;
import net.sf.jsqlparser.parser.SimpleNode;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLSearchUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.utils.ListNode;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

public class SQLSemanticUtils {
    private static final Log log = Log.getLog(SQLSemanticUtils.class);

    public static void traverseQueryForTableNodes(@NotNull Statement statement, @NotNull Consumer<Table> onTableNode) {
        SimpleNode root = ((ASTNodeAccess) statement).getASTNode();
        ListNode<Node> stack = ListNode.of(root);
        HashSet<Table> handledSet = new HashSet<>();
        while (stack != null) {
            Node node = stack.data;
            stack = stack.next;
            if (node instanceof SimpleNode n && n.jjtGetValue() instanceof Table tableNode && handledSet.add(tableNode)) {
                onTableNode.accept(tableNode);
            }
            for (int i = node.jjtGetNumChildren() - 1; i >= 0; i--) {
                stack = ListNode.push(stack, node.jjtGetChild(i));
            }
        }
    }

    @Nullable
    public static DBSObject findObjectForTableNode(
        @NotNull DBRProgressMonitor monitor,
        @Nullable DBCExecutionContext executionContext,
        @NotNull DBPDataSource dataSource,
        @Nullable SQLIdentifierDetector identifierDetector,
        @NotNull Table table
    ) {
        DBSObjectContainer container = DBUtils.getAdapter(DBSObjectContainer.class, dataSource);
        if (container == null) {
            log.error("Data source " + dataSource.getName() + " is not an object container");
            return null;
        }
        List<String> nameList = new ArrayList<>();
        if (table.getDatabase() != null && !CommonUtils.isEmpty(table.getDatabase().getDatabaseName())) {
            nameList.add(table.getDatabase().getDatabaseName());
        }
        if (!CommonUtils.isEmpty(table.getSchemaName())) {
            nameList.add(table.getSchemaName());
        }
        nameList.add(table.getName());

        DBSObject[] selectedObjects = DBUtils.getSelectedObjects(executionContext);
        List<DBSObjectContainer> selectedContainers = new ArrayList<>();
        selectedContainers.add(container);
        for (DBSObject selectedObject : selectedObjects) {
            selectedContainers.add(DBUtils.getAdapter(DBSObjectContainer.class, selectedObject));
        }

        if (nameList.size() == 1) {
            // In this case we have only the table name. We do not know schema or catalog name.
            // Like in "SELECT * FROM Album".
            // We need to start looking for the entity from the closest container - e.g. schema, not from the datasource.
            // This will help us avoid the founding of catalogs/schemas that have an equal name of the table.
            // The usual order for databases like PG: datasource, database, schema. Let's reverse it.
            Collections.reverse(selectedContainers);
        }
        return findObjectForEntity(monitor, executionContext, identifierDetector, nameList, selectedContainers);
    }

    @Nullable
    private static DBSObject findObjectForEntity(
        @NotNull DBRProgressMonitor monitor,
        @Nullable DBCExecutionContext executionContext,
        @Nullable SQLIdentifierDetector identifierDetector,
        @NotNull List<String> nameList,
        @NotNull List<DBSObjectContainer> selectedContainers
    ) {
        if (identifierDetector == null) {
            identifierDetector = new SQLIdentifierDetector(executionContext == null ? BasicSQLDialect.INSTANCE : executionContext.getDataSource().getSQLDialect());
        }

        for (DBSObjectContainer oc : selectedContainers) {
            if (oc != null) {
                DBSObject object = SQLSearchUtils.findObjectByFQN(
                    monitor,
                    oc,
                    executionContext,
                    nameList,
                    false,
                    identifierDetector
                );
                if (object != null) {
                    return object;
                }
            }
        }
        return null;
    }
}
