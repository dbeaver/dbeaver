/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.editors.lob.LOBEditor;
import org.jkiss.dbeaver.ui.views.properties.PropertySourceAbstract;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * JDBC string value handler
 */
public class JDBCLOBValueHandler extends JDBCAbstractValueHandler {

    static Log log = LogFactory.getLog(JDBCLOBValueHandler.class);

    public static final JDBCLOBValueHandler INSTANCE = new JDBCLOBValueHandler();

    protected Object getValueObject(ResultSet resultSet, DBSTypedObject columnType, int columnIndex)
        throws DBCException, SQLException
    {
        return resultSet.getObject(columnIndex);
    }

    protected void bindParameter(PreparedStatement statement, DBSTypedObject paramType, int paramIndex, Object value)
        throws DBCException, SQLException
    {
    }

    public void fillContextMenu(IMenuManager menuManager, DBDValueController controller)
        throws DBCException
    {
        Action saveAction = new Action() {
            @Override
            public void run() {
            }
        };
        saveAction.setText("Save to file ...");
        menuManager.add(saveAction);
    }

    public void fillProperties(PropertySourceAbstract propertySource, DBDValueController controller)
    {
        propertySource.addProperty(
            "max_length",
            "Max Length",
            controller.getColumnMetaData().getDisplaySize());
    }

    public boolean editValue(final DBDValueController controller)
        throws DBException
    {
        if (controller.isInlineEdit()) {
            // Open inline editor
            return false;
        } else {
            // Open LOB editor
            return LOBEditor.openEditor(controller);
        }
    }

}