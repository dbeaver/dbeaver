/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.data.DBDComplexType;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.data.DBDValueEditor;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.dialogs.data.ComplexObjectEditor;
import org.jkiss.dbeaver.ui.dialogs.data.DefaultValueViewDialog;
import org.jkiss.dbeaver.ui.properties.PropertySourceAbstract;

import java.sql.SQLException;

/**
 * Complex value handler.
 * Handle complex types.
 *
 * @author Serge Rider
 */
public abstract class JDBCComplexValueHandler extends JDBCAbstractValueHandler {

    static final Log log = LogFactory.getLog(JDBCComplexValueHandler.class);

    public static final String PROP_CATEGORY_COMPLEX = "Complex";

    @Override
    public int getFeatures()
    {
        return FEATURE_VIEWER | FEATURE_EDITOR | FEATURE_SHOW_ICON;
    }

    @Override
    protected Object fetchColumnValue(
        DBCExecutionContext context,
        JDBCResultSet resultSet,
        DBSTypedObject type,
        int index)
        throws DBCException, SQLException
    {
        Object value = resultSet.getObject(index);
        return getValueFromObject(context, type, value, false);
    }

    @Override
    protected void bindParameter(
        JDBCExecutionContext context,
        JDBCPreparedStatement statement,
        DBSTypedObject paramType,
        int paramIndex,
        Object value)
        throws DBCException, SQLException
    {
        throw new DBCException("Unsupported value type: " + value);
    }

    @Override
    public void fillProperties(PropertySourceAbstract propertySource, DBDValueController controller)
    {
        super.fillProperties(propertySource, controller);
        try {
            Object value = controller.getValue();
            if (value instanceof DBDComplexType) {
                propertySource.addProperty(
                    PROP_CATEGORY_COMPLEX,
                    "object_type", //$NON-NLS-1$
                    CoreMessages.model_jdbc_type_name,
                    ((DBDComplexType) value).getObjectDataType().getName());
            }
        } catch (Exception e) {
            log.warn("Could not extract complex type information", e); //$NON-NLS-1$
        }
    }

    @Override
    public DBDValueEditor createEditor(final DBDValueController controller)
        throws DBException
    {
        switch (controller.getEditType()) {
            case PANEL:
                return new ValueEditor<Tree>(controller) {
                    ComplexObjectEditor editor;

                    @Override
                    public void primeEditorValue(Object value) throws DBException
                    {
                        editor.setModel(controller.getDataSource(), (DBDComplexType) value);
                    }

                    @Override
                    protected Tree createControl(Composite editPlaceholder)
                    {
                        editor = new ComplexObjectEditor(controller.getValueSite(), controller.getEditPlaceholder(), SWT.BORDER);
                        editor.setModel(controller.getDataSource(), (DBDComplexType) controller.getValue());
                        return editor.getTree();
                    }

                    @Override
                    public Object extractEditorValue()
                    {
                        return editor.getInput();
                    }
                };
            case EDITOR:
                return new DefaultValueViewDialog(controller);
            default:
                return null;
        }
    }

}