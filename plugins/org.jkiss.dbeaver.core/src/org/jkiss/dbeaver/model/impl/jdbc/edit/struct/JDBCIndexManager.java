/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.edit.struct;

import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableIndex;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.struct.DBSTableIndexColumn;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;

/**
 * JDBC constraint manager
 */
public abstract class JDBCIndexManager<OBJECT_TYPE extends JDBCTableIndex<TABLE_TYPE>, TABLE_TYPE extends JDBCTable>
    extends JDBCObjectEditor<OBJECT_TYPE, TABLE_TYPE>
{

    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected IDatabasePersistAction[] makeObjectCreateActions(ObjectCreateCommand command)
    {
        final TABLE_TYPE table = command.getObject().getTable();
        final OBJECT_TYPE index = command.getObject();

        // Create index
        final String indexName = DBUtils.getQuotedIdentifier(index.getDataSource(), index.getName());
        index.setName(indexName);

        StringBuilder decl = new StringBuilder(40);
        decl
            .append("CREATE INDEX ").append(indexName) //$NON-NLS-1$
            .append(" ON ").append(table.getFullQualifiedName()) //$NON-NLS-1$
            .append(" ("); //$NON-NLS-1$
        // Get columns using void monitor
        boolean firstColumn = true;
        for (DBSTableIndexColumn indexColumn : command.getObject().getColumns(VoidProgressMonitor.INSTANCE)) {
            if (!firstColumn) decl.append(","); //$NON-NLS-1$
            firstColumn = false;
            decl.append(indexColumn.getName());
            if (!indexColumn.isAscending()) {
                decl.append(" DESC"); //$NON-NLS-1$
            }
        }
        decl.append(")"); //$NON-NLS-1$

        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction(CoreMessages.model_jdbc_create_new_index, decl.toString())
        };
    }

    @Override
    protected IDatabasePersistAction[] makeObjectDeleteActions(ObjectDeleteCommand command)
    {
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction(
                CoreMessages.model_jdbc_drop_index,
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

