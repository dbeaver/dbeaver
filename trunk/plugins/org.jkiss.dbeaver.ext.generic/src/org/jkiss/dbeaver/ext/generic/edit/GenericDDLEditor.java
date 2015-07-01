package org.jkiss.dbeaver.ext.generic.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorNested;

public class GenericDDLEditor extends SQLEditorNested<GenericTable> {

    public GenericDDLEditor()
    {
    }

    @Override
    protected boolean isReadOnly()
    {
        return true;
    }

    @Override
    protected String getSourceText(DBRProgressMonitor monitor) throws DBException
    {
        GenericTable sourceObject = getSourceObject();
        if (sourceObject.isView()) {
            return sourceObject.getDataSource().getMetaModel().getViewDDL(monitor, sourceObject);
        }
        if (!sourceObject.isPersisted()) {
            return "";
        }
        return sourceObject.getDataSource().getMetaModel().getTableDDL(monitor, sourceObject);
    }

    @Override
    protected void setSourceText(String sourceText)
    {
        // We are read-only
    }

}