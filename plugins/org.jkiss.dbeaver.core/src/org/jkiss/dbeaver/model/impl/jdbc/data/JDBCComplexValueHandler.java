/*
 * Copyright (C) 2010-2014 Serge Rieder
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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.data.DBDComplexValue;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.data.DBDValueEditor;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
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
        return FEATURE_VIEWER | FEATURE_EDITOR;
    }

    @Override
    protected Object fetchColumnValue(
        DBCSession session,
        JDBCResultSet resultSet,
        DBSTypedObject type,
        int index)
        throws DBCException, SQLException
    {
        Object value = resultSet.getObject(index);
        return getValueFromObject(session, type, value, false);
    }

    @Override
    protected void bindParameter(
        JDBCSession session,
        JDBCPreparedStatement statement,
        DBSTypedObject paramType,
        int paramIndex,
        Object value)
        throws DBCException, SQLException
    {
        throw new DBCException("Unsupported value type: " + value);
    }

/*
    @Override
    public void contributeProperties(@NotNull PropertySourceAbstract propertySource, @NotNull DBDValueController controller)
    {
        super.contributeProperties(propertySource, controller);
        try {
            Object value = controller.getValue();
            if (value instanceof DBDComplexValue) {
                propertySource.addProperty(
                    PROP_CATEGORY_COMPLEX,
                    "object_type", //$NON-NLS-1$
                    CoreMessages.model_jdbc_type_name,
                    ((DBDComplexValue) value).getObjectDataType().getName());
            }
        } catch (Exception e) {
            log.warn("Could not extract complex type information", e); //$NON-NLS-1$
        }
    }
*/

    @Override
    public DBDValueEditor createEditor(@NotNull final DBDValueController controller)
        throws DBException
    {
        switch (controller.getEditType()) {
            case PANEL:
                return new ValueEditor<Tree>(controller) {
                    ComplexObjectEditor editor;

                    @Override
                    public void primeEditorValue(@Nullable Object value) throws DBException
                    {
                        editor.setModel(controller.getDataSource(), (DBDComplexValue) value);
                    }

                    @Override
                    protected Tree createControl(Composite editPlaceholder)
                    {
                        editor = new ComplexObjectEditor(controller.getValueSite(), controller.getEditPlaceholder(), SWT.BORDER);
                        editor.setModel(controller.getDataSource(), (DBDComplexValue) controller.getValue());
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