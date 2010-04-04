package org.jkiss.dbeaver.model.impl.data;

import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueAnnotation;
import org.jkiss.dbeaver.model.data.DBDValueLocator;
import org.jkiss.dbeaver.model.dbc.DBCResultSet;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.DBException;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.swt.widgets.Widget;

/**
 * Standard JDBC value handler
 */
public class JDBCUnsupportedValueHandler implements DBDValueHandler {

    public Object getValueObject(DBCResultSet resultSet, int columnIndex)
        throws DBCException
    {
        return resultSet.getObject(columnIndex);
    }

    public DBDValueAnnotation[] getValueAnnotations(DBCColumnMetaData column)
        throws DBCException
    {
        return null;
    }

    public void editValue(DBCColumnMetaData column, Object value, DBDValueLocator valueLocator, boolean inlineEdit,
                          boolean readOnly, IWorkbenchPartSite valueSite, Widget valueWidget)
        throws DBException
    {
    }

    public void dispose()
    {
    }

}
