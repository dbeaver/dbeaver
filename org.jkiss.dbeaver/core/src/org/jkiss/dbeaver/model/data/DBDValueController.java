/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;

/**
 * DBD Value Controller
 */
public interface DBDValueController
{
    DBCColumnMetaData getColumnMetaData();

    Object getColumnValue(DBCColumnMetaData column);

    Object getValue();

    void updateValue(Object value);

    DBDValueLocator getValueLocator();

    boolean isInlineEdit();

    boolean isReadOnly();

    IWorkbenchPartSite getValueSite();

    Composite getInlinePlaceholder();

    void closeInlineEditor();

    void showMessage(String message, boolean error);

    void nextInlineEditor(boolean next);
}