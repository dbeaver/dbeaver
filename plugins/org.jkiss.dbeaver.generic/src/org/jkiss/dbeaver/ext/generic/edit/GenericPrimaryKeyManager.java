/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.edit;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.generic.model.GenericPrimaryKey;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCConstraintManager;
import org.jkiss.dbeaver.model.struct.DBSConstraintType;

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

    protected GenericPrimaryKey createNewConstraint(IWorkbenchWindow workbenchWindow, GenericTable parent, Object copyFrom)
    {
        final GenericPrimaryKey primaryKey = new GenericPrimaryKey(
            parent,
            null,
            null,
            DBSConstraintType.PRIMARY_KEY,
            false);
        primaryKey.setName(JDBCObjectNameCaseTransformer.transformName(primaryKey, CommonUtils.escapeIdentifier(parent.getName()) + "_PK"));
        return primaryKey;
    }

}
