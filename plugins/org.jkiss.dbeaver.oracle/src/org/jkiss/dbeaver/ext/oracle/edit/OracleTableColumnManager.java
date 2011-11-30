/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.edit;

import org.jkiss.dbeaver.ext.oracle.OracleMessages;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataType;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.utils.CommonUtils;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableBase;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableColumn;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCTableColumnManager;
import org.jkiss.dbeaver.model.struct.DBSDataKind;
import org.jkiss.dbeaver.model.struct.DBSDataType;

import java.sql.Types;

/**
 * Oracle table column manager
 */
public class OracleTableColumnManager extends JDBCTableColumnManager<OracleTableColumn, OracleTableBase> {

    public StringBuilder getNestedDeclaration(OracleTableBase owner, DBECommandComposite<OracleTableColumn, PropertyHandler> command)
    {
        StringBuilder decl = super.getNestedDeclaration(owner, command);
        final OracleTableColumn column = command.getObject();
        if (column.isAutoIncrement()) {
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

    protected OracleTableColumn createDatabaseObject(IWorkbenchWindow workbenchWindow, IEditorPart activeEditor, DBECommandContext context, OracleTableBase parent, Object copyFrom)
    {
        DBSDataType columnType = findBestDataType(parent.getDataSource(), "varchar2"); //$NON-NLS-1$

        final OracleTableColumn column = new OracleTableColumn(parent);
        column.setName(DBObjectNameCaseTransformer.transformName(column, getNewColumnName(context, parent)));
        column.setType((OracleDataType) columnType);
        column.setTypeName(columnType == null ? "INTEGER" : columnType.getName()); //$NON-NLS-1$
        column.setMaxLength(columnType != null && columnType.getDataKind() == DBSDataKind.STRING ? 100 : 0);
        column.setValueType(columnType == null ? Types.INTEGER : columnType.getValueType());
        column.setOrdinalPosition(-1);
        return column;
    }

    @Override
    protected IDatabasePersistAction[] makeObjectModifyActions(ObjectChangeCommand command)
    {
        final OracleTableColumn column = command.getObject();

        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction(
                OracleMessages.edit_oracle_table_column_manager_action_alter_table_column,
                "ALTER TABLE " + column.getTable().getFullQualifiedName() + " MODIFY COLUMN " + getNestedDeclaration(column.getTable(), command))}; //$NON-NLS-1$ //$NON-NLS-2$
    }
}
