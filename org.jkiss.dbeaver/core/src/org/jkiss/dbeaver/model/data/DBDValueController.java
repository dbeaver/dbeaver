package org.jkiss.dbeaver.model.data;

import org.eclipse.swt.widgets.Widget;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.model.dbc.DBCResultSet;
import org.jkiss.dbeaver.model.dbc.DBCException;

/**
 * DBDValueController
 */
public interface DBDValueController
{
    DBCColumnMetaData getColumnMetaData();

    Object getValue();

    void updateValue(Object value);

    DBDValueLocator getValueLocator();

    boolean isInlineEdit();

    boolean isReadOnly();

    IWorkbenchPartSite getValueSite();

    Composite getInlinePlaceholder();

    void closeInlineEditor();

    void showMessage(String message, boolean error);

}