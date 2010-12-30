/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCColumnMetaData;

/**
 * DBD Value Controller
 */
public interface DBDValueController
{

    /**
     * Controller's data source
     * @return data source
     */
    DBPDataSource getDataSource();

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
