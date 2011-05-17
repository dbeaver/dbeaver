/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.edit;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.mysql.model.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCTableManager;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;

/**
 * MySQL table manager
 */
public class MySQLTableManager extends JDBCTableManager<MySQLTable, MySQLCatalog> {

    private static final Class<?>[] CHILD_TYPES = {
        MySQLTableColumn.class,
        MySQLConstraint.class,
        MySQLForeignKey.class,
        MySQLIndex.class
    };

    @Override
    protected MySQLTable createNewObject(IWorkbenchWindow workbenchWindow, IEditorPart activeEditor, MySQLCatalog parent, Object copyFrom)
    {
        final MySQLTable table = new MySQLTable(parent);
        table.setName(JDBCObjectNameCaseTransformer.transformName(parent, "NewTable"));
        try {
            final MySQLTable.AdditionalInfo additionalInfo = table.getAdditionalInfo(VoidProgressMonitor.INSTANCE);
            additionalInfo.setEngine(parent.getDataSource().getDefaultEngine());
            additionalInfo.setCharset(parent.getDefaultCharset());
            additionalInfo.setCollation(parent.getDefaultCollation());
        } catch (DBCException e) {
            // Never be here
            log.error(e);
        }

        return table;
    }

    @Override
    protected void appendTableModifiers(MySQLTable table, ObjectChangeCommand tableProps, StringBuilder ddl)
    {
        try {
            final MySQLTable.AdditionalInfo additionalInfo = table.getAdditionalInfo(VoidProgressMonitor.INSTANCE);
            if (additionalInfo.getEngine() != null) {
                ddl.append("\nENGINE=").append(additionalInfo.getEngine().getName());
            }
            if (additionalInfo.getCharset() != null) {
                ddl.append("\nDEFAULT CHARSET=").append(additionalInfo.getCharset().getName());
            }
            if (additionalInfo.getCollation() != null) {
                ddl.append("\nCOLLATE=").append(additionalInfo.getCollation().getName());
            }
            if (!CommonUtils.isEmpty(table.getDescription())) {
                ddl.append("\nCOMMENT='").append(table.getDescription()).append("'");
            }
            if (additionalInfo.getAutoIncrement() > 0) {
                ddl.append("\nAUTO_INCREMENT=").append(additionalInfo.getAutoIncrement());
            }
        } catch (DBCException e) {
            log.error(e);
        }
    }

    public Class<?>[] getChildTypes()
    {
        return CHILD_TYPES;
    }

}
