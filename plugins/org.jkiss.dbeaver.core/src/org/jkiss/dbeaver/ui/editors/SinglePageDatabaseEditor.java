/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.model.DBPDataSource;

/**
 * SinglePageDatabaseEditor
 */
public abstract class SinglePageDatabaseEditor<INPUT_TYPE extends IDatabaseEditorInput> extends AbstractDatabaseEditor<INPUT_TYPE>
{
    public void init(IEditorSite site, IEditorInput input)
        throws PartInitException
    {
        super.init(site, input);
    }

    public void dispose()
    {
        super.dispose();
    }

    public DBPDataSource getDataSource() {
        return getEditorInput() == null || getEditorInput().getDatabaseObject() == null ? null : getEditorInput().getDatabaseObject().getDataSource();
    }

}