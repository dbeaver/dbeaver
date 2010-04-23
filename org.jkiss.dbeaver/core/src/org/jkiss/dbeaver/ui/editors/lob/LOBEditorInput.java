/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.lob;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.ui.DBIcon;

/**
 * LOBEditorInput
 */
public class LOBEditorInput implements IEditorInput //IDatabaseEditorInput
{
    private DBDValueController valueController;

    public LOBEditorInput(DBDValueController valueController)
    {
        this.valueController = valueController;
    }

    public DBDValueController getValueController()
    {
        return valueController;
    }

    public boolean exists()
    {
        return false;
    }

    public ImageDescriptor getImageDescriptor()
    {
        return DBIcon.LOB.getImageDescriptor();
    }

    public String getName()
    {
        String tableName = valueController.getColumnMetaData().getTableName();
        return CommonUtils.isEmpty(tableName) ?
            valueController.getColumnMetaData().getColumnName() :
            tableName + "." + valueController.getColumnMetaData().getColumnName();
    }

    public IPersistableElement getPersistable()
    {
        return null;
    }

    public String getToolTipText()
    {
        return "LOB column editor";
    }

    public Object getAdapter(Class adapter)
    {
        return null;
    }

}