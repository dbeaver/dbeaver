/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.IMenuManager;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.properties.PropertySourceAbstract;

import java.sql.SQLException;
import java.sql.Struct;

/**
 * JDBC Struct value handler.
 * Handle STRUCT types.
 *
 * @author Serge Rider
 */
public class JDBCStructValueHandler extends JDBCAbstractValueHandler {

    static final Log log = LogFactory.getLog(JDBCStructValueHandler.class);

    public static final JDBCStructValueHandler INSTANCE = new JDBCStructValueHandler();

    @Override
    protected Object getColumnValue(
        DBCExecutionContext context,
        JDBCResultSet resultSet,
        DBSTypedObject column,
        int columnIndex)
        throws DBCException, SQLException
    {
        Object value = resultSet.getObject(columnIndex);

        if (value == null) {
            return new JDBCStruct(null);
        } else if (value instanceof Struct) {
            return new JDBCStruct((Struct) value);
        } else {
            throw new DBCException("Unsupported struct type: " + value.getClass().getName());
        }
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
    public int getFeatures()
    {
        return FEATURE_NONE;
    }

    @Override
    public Class getValueObjectType()
    {
        return Struct.class;
    }

    @Override
    public Object copyValueObject(DBCExecutionContext context, DBSTypedObject column, Object value)
        throws DBCException
    {
        return null;
    }

/*
    public String getValueDisplayString(DBSTypedObject column, Object value)
    {
        if (value instanceof JDBCStruct) {
            String displayString = value.toString();
            if (displayString != null) {
                return displayString;
            }
        }
        return super.getValueDisplayString(column, value);
    }
*/

    @Override
    public void fillContextMenu(IMenuManager menuManager, final DBDValueController controller)
        throws DBCException
    {
    }

    @Override
    public void fillProperties(PropertySourceAbstract propertySource, DBDValueController controller)
    {
        try {
            Object value = controller.getValue();
            if (value instanceof JDBCStruct) {
                propertySource.addProperty(
                    "sql_type", //$NON-NLS-1$
                    CoreMessages.model_jdbc_type_name,
                    ((JDBCStruct)value).getTypeName());
            }
        }
        catch (Exception e) {
            log.warn("Could not extract struct value information", e); //$NON-NLS-1$
        }
    }

    @Override
    public boolean editValue(final DBDValueController controller)
        throws DBException
    {
        return false;
    }

}