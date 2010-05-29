/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.runtime.sql.ISQLQueryListener;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.model.dbc.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * DBD Value Controller
 */
public interface DBDValueController
{
    /**
     * Controller's session
     * @return session
     * @throws DBException
     */
    DBCSession getSession() throws DBException;

    /**
     * Row controller
     * @return row controller
     */
    DBDRowController getRow();

    /**
     * Column meta data
     * @return meta data
     */
    DBCColumnMetaData getColumnMetaData();

    /**
     * Column unique ID string
     * @return string
     */
    String getColumnId();

    /**
     * Column value
     * @return
     */
    Object getValue();

    /**
     * Updates value
     * @param value value
     */
    void updateValue(Object value);

    /**
     * Updates value immediately. Executes SQL update statement for this cell only.
     * @param value new value
     * @param progressMonitor progress monitor
     */
    void updateValueImmediately(Object value, ISQLQueryListener listener) throws DBException;

    DBDValueLocator getValueLocator();

    DBDValueHandler getValueHandler();

    boolean isInlineEdit();

    boolean isReadOnly();

    IWorkbenchPartSite getValueSite();

    Composite getInlinePlaceholder();

    void closeInlineEditor();

    void nextInlineEditor(boolean next);

    void registerEditor(DBDValueEditor editor);

    void unregisterEditor(DBDValueEditor editor);

    void showMessage(String message, boolean error);

}
