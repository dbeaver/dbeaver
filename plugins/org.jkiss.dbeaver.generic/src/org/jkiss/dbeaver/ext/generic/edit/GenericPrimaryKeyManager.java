/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.edit;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.generic.model.GenericConstraintColumn;
import org.jkiss.dbeaver.ext.generic.model.GenericPrimaryKey;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCConstraintManager;
import org.jkiss.dbeaver.model.struct.DBSConstraintType;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;

import java.util.Collection;
import java.util.Collections;

/**
 * Generic constraint manager
 */
public class GenericPrimaryKeyManager extends JDBCConstraintManager<GenericPrimaryKey, GenericTable> {

    protected Collection<DBSConstraintType> getSupportedConstraintTypes()
    {
        return Collections.singletonList(DBSConstraintType.PRIMARY_KEY);
    }

    protected GenericPrimaryKey createNewConstraint(
        IWorkbenchWindow workbenchWindow,
        GenericTable parent,
        DBSConstraintType constraintType,
        Collection<DBSTableColumn> constraintColumns,
        Object from)
    {
        final GenericPrimaryKey primaryKey = new GenericPrimaryKey(
            parent,
            null,
            null,
            constraintType,
            false);
        primaryKey.setName(JDBCObjectNameCaseTransformer.transformName(primaryKey, CommonUtils.escapeIdentifier(parent.getName()) + "_PK"));
        int colIndex = 1;
        for (DBSTableColumn tableColumn : constraintColumns) {
            primaryKey.addColumn(
                new GenericConstraintColumn(
                    primaryKey,
                    (GenericTableColumn) tableColumn,
                    colIndex++));
        }
        return primaryKey;
    }

}
