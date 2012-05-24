/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.edit;

import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.utils.CommonUtils;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.mysql.MySQLMessages;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableBase;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableColumn;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCTableColumnManager;
import org.jkiss.dbeaver.model.struct.DBSDataKind;
import org.jkiss.dbeaver.model.struct.DBSDataType;

import java.sql.Types;

/**
 * MySQL table column manager
 */
public class MySQLTableColumnManager extends JDBCTableColumnManager<MySQLTableColumn, MySQLTableBase> {

    @Override
    public StringBuilder getNestedDeclaration(MySQLTableBase owner, DBECommandComposite<MySQLTableColumn, PropertyHandler> command)
    {
        StringBuilder decl = super.getNestedDeclaration(owner, command);
        final MySQLTableColumn column = command.getObject();
        if (column.isSequence()) {
            decl.append(" AUTO_INCREMENT"); //$NON-NLS-1$
        }
        if (!CommonUtils.isEmpty(column.getDefaultValue())) {
            decl.append(" DEFAULT '").append(column.getDefaultValue()).append("'"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if (!CommonUtils.isEmpty(column.getComment())) {
            decl.append(" COMMENT '").append(column.getComment()).append("'"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return decl;
    }

    @Override
    protected MySQLTableColumn createDatabaseObject(IWorkbenchWindow workbenchWindow, IEditorPart activeEditor, DBECommandContext context, MySQLTableBase parent, Object copyFrom)
    {
        DBSDataType columnType = findBestDataType(parent.getDataSource(), "varchar"); //$NON-NLS-1$

        final MySQLTableColumn column = new MySQLTableColumn(parent);
        column.setName(DBObjectNameCaseTransformer.transformName(column, getNewColumnName(context, parent)));
        column.setTypeName(columnType == null ? "INTEGER" : columnType.getName()); //$NON-NLS-1$
        column.setMaxLength(columnType != null && columnType.getDataKind() == DBSDataKind.STRING ? 100 : 0);
        column.setValueType(columnType == null ? Types.INTEGER : columnType.getValueType());
        column.setOrdinalPosition(-1);
        return column;
    }

    @Override
    protected IDatabasePersistAction[] makeObjectModifyActions(ObjectChangeCommand command)
    {
        final MySQLTableColumn column = command.getObject();

        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction(
                MySQLMessages.edit_table_column_manager_action_alter_table_column,
                "ALTER TABLE " + column.getTable().getFullQualifiedName() + " MODIFY COLUMN " + getNestedDeclaration(column.getTable(), command))}; //$NON-NLS-1$ //$NON-NLS-2$
    }
}
