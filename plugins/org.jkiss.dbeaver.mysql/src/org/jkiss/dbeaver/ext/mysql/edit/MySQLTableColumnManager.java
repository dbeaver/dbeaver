/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.edit;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableBase;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableColumn;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCTableColumnManager;
import org.jkiss.dbeaver.model.struct.DBSDataKind;
import org.jkiss.dbeaver.model.struct.DBSDataType;

import java.sql.Types;

/**
 * MySQL table column manager
 */
public class MySQLTableColumnManager extends JDBCTableColumnManager<MySQLTableColumn, MySQLTableBase> {

    public StringBuilder getNestedDeclaration(MySQLTableBase owner, DBECommandComposite<MySQLTableColumn, PropertyHandler> command)
    {
        StringBuilder decl = super.getNestedDeclaration(owner, command);
        final MySQLTableColumn column = command.getObject();
        if (column.isAutoIncrement()) {
            decl.append(" AUTO_INCREMENT");
        }
        if (!CommonUtils.isEmpty(column.getDefaultValue())) {
            decl.append(" DEFAULT '").append(column.getDefaultValue()).append("'");
        }
        if (!CommonUtils.isEmpty(column.getComment())) {
            decl.append(" COMMENT '").append(column.getComment()).append("'");
        }
        return decl;
    }

    protected MySQLTableColumn createNewObject(IWorkbenchWindow workbenchWindow, IEditorPart activeEditor, MySQLTableBase parent, Object copyFrom)
    {
        DBSDataType columnType = findBestDataType(parent.getDataSource(), "varchar");

        final MySQLTableColumn column = new MySQLTableColumn(parent);
        column.setName(JDBCObjectNameCaseTransformer.transformName(column, "NewColumn"));
        column.setTypeName(columnType == null ? "INTEGER" : columnType.getName());
        column.setMaxLength(columnType != null && columnType.getDataKind() == DBSDataKind.STRING ? 100 : 0);
        column.setValueType(columnType == null ? Types.INTEGER : columnType.getTypeNumber());
        column.setOrdinalPosition(-1);
        return column;
    }

    @Override
    protected IDatabasePersistAction[] makeObjectModifyActions(ObjectChangeCommand command)
    {
        final MySQLTableColumn column = command.getObject();

        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction(
                "Alter table column",
                "ALTER TABLE " + column.getTable().getFullQualifiedName() + " MODIFY COLUMN " + getNestedDeclaration(column.getTable(), command))};
    }
}
