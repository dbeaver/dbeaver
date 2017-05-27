/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.impl.sql.edit.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableIndex;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndexColumn;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * JDBC constraint manager
 */
public abstract class SQLIndexManager<OBJECT_TYPE extends JDBCTableIndex<? extends DBSObjectContainer, TABLE_TYPE>, TABLE_TYPE extends JDBCTable>
    extends SQLObjectEditor<OBJECT_TYPE, TABLE_TYPE>
{

    @Override
    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected void addObjectCreateActions(List<DBEPersistAction> actions, ObjectCreateCommand command)
    {
        final TABLE_TYPE table = command.getObject().getTable();
        final OBJECT_TYPE index = command.getObject();

        // Create index
        final String indexName = DBUtils.getQuotedIdentifier(index.getDataSource(), index.getName());
        index.setName(indexName);

        StringBuilder decl = new StringBuilder(40);
        decl.append("CREATE");
        appendIndexModifiers(index, decl);
        decl.append(" INDEX ").append(indexName); //$NON-NLS-1$
        appendIndexType(index, decl);
        decl.append(" ON ").append(table.getFullyQualifiedName(DBPEvaluationContext.DDL)) //$NON-NLS-1$
            .append(" ("); //$NON-NLS-1$
        try {
            // Get columns using void monitor
            boolean firstColumn = true;
            for (DBSTableIndexColumn indexColumn : CommonUtils.safeCollection(command.getObject().getAttributeReferences(new VoidProgressMonitor()))) {
                if (!firstColumn) decl.append(","); //$NON-NLS-1$
                firstColumn = false;
                decl.append(DBUtils.getQuotedIdentifier(indexColumn));
                appendIndexColumnModifiers(decl, indexColumn);
            }
        } catch (DBException e) {
            log.error(e);
        }
        decl.append(")"); //$NON-NLS-1$

        actions.add(
            new SQLDatabasePersistAction(ModelMessages.model_jdbc_create_new_index, decl.toString())
        );
    }

    protected void appendIndexType(OBJECT_TYPE index, StringBuilder decl) {

    }

    protected void appendIndexModifiers(OBJECT_TYPE index, StringBuilder decl) {
        if (index.isUnique()) {
            decl.append(" UNIQUE");
        }
    }

    protected void appendIndexColumnModifiers(StringBuilder decl, DBSTableIndexColumn indexColumn) {
        if (!indexColumn.isAscending()) {
            decl.append(" DESC"); //$NON-NLS-1$
        }
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command)
    {
        actions.add(
            new SQLDatabasePersistAction(
                ModelMessages.model_jdbc_drop_index,
                getDropIndexPattern(command.getObject())
                    .replace(PATTERN_ITEM_TABLE, command.getObject().getTable().getFullyQualifiedName(DBPEvaluationContext.DDL))
                    .replace(PATTERN_ITEM_INDEX, command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL))
                    .replace(PATTERN_ITEM_INDEX_SHORT, command.getObject().getName()))
        );
    }

    protected String getDropIndexPattern(OBJECT_TYPE index)
    {
        return "DROP INDEX " + PATTERN_ITEM_INDEX; //$NON-NLS-1$
    }


}

