/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.impl.sql.edit.struct;

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
    protected DBEPersistAction[] makeObjectCreateActions(ObjectCreateCommand command)
    {
        final TABLE_TYPE table = command.getObject().getTable();
        final OBJECT_TYPE index = command.getObject();

        // Create index
        final String indexName = DBUtils.getQuotedIdentifier(index.getDataSource(), index.getName());
        index.setName(indexName);

        StringBuilder decl = new StringBuilder(40);
        decl.append("CREATE ");
        if (index.isUnique()) {
            decl.append("UNIQUE ");
        }
        decl.append("INDEX ").append(indexName) //$NON-NLS-1$
            .append(" ON ").append(table.getFullQualifiedName()) //$NON-NLS-1$
            .append(" ("); //$NON-NLS-1$
        // Get columns using void monitor
        boolean firstColumn = true;
        for (DBSTableIndexColumn indexColumn : CommonUtils.safeCollection(command.getObject().getAttributeReferences(VoidProgressMonitor.INSTANCE))) {
            if (!firstColumn) decl.append(","); //$NON-NLS-1$
            firstColumn = false;
            decl.append(indexColumn.getName());
            if (!indexColumn.isAscending()) {
                decl.append(" DESC"); //$NON-NLS-1$
            }
        }
        decl.append(")"); //$NON-NLS-1$

        return new DBEPersistAction[] {
            new SQLDatabasePersistAction(ModelMessages.model_jdbc_create_new_index, decl.toString())
        };
    }

    @Override
    protected DBEPersistAction[] makeObjectDeleteActions(ObjectDeleteCommand command)
    {
        return new DBEPersistAction[] {
            new SQLDatabasePersistAction(
                ModelMessages.model_jdbc_drop_index,
                getDropIndexPattern(command.getObject())
                    .replace(PATTERN_ITEM_TABLE, command.getObject().getTable().getFullQualifiedName())
                    .replace(PATTERN_ITEM_INDEX, command.getObject().getFullQualifiedName())
                    .replace(PATTERN_ITEM_INDEX_SHORT, command.getObject().getName()))
        };
    }

    protected String getDropIndexPattern(OBJECT_TYPE index)
    {
        return "DROP INDEX " + PATTERN_ITEM_INDEX; //$NON-NLS-1$
    }


}

