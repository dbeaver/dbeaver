/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.edit;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCTableColumnManager;
import org.jkiss.dbeaver.model.struct.DBSDataKind;
import org.jkiss.dbeaver.model.struct.DBSDataType;

import java.sql.Types;

/**
 * Generic table column manager
 */
public class GenericTableColumnManager extends JDBCTableColumnManager<GenericTableColumn, GenericTable> {

    @Override
    protected GenericTableColumn createDatabaseObject(IWorkbenchWindow workbenchWindow, IEditorPart activeEditor, DBECommandContext context, GenericTable parent, Object copyFrom)
    {
        DBSDataType columnType = findBestDataType(parent.getDataSource(), DBConstants.DEFAULT_DATATYPE_NAMES);

        final GenericTableColumn column = new GenericTableColumn(parent);
        column.setName(DBObjectNameCaseTransformer.transformName(column, getNewColumnName(context, parent)));
        column.setTypeName(columnType == null ? "INTEGER" : columnType.getName());
        column.setMaxLength(columnType != null && columnType.getDataKind() == DBSDataKind.STRING ? 100 : 0);
        column.setValueType(columnType == null ? Types.INTEGER : columnType.getValueType());
        column.setOrdinalPosition(-1);
        return column;
    }

}
