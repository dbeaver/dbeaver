/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCColumnMetaData;
import org.jkiss.dbeaver.model.struct.DBSColumnBase;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

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
     * Column meta data
     * @return meta data
     */
    DBSColumnBase getColumnMetaData();

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
