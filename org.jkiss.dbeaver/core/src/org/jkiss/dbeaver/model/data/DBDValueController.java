/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.model.dbc.DBCSession;

/**
 * DBD Value Controller
 */
public interface DBDValueController
{
    DBCSession getSession() throws DBException;

    DBCColumnMetaData getColumnMetaData();

    Object getColumnValue(DBCColumnMetaData column);

    Object getValue();

    void updateValue(Object value);

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
