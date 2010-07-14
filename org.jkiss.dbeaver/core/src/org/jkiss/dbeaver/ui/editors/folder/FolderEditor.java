/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.folder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.ext.ui.IMetaModelView;
import org.jkiss.dbeaver.model.meta.DBMModel;
import org.jkiss.dbeaver.ui.controls.itemlist.ItemListControl;
import org.jkiss.dbeaver.ui.editors.SinglePageDatabaseEditor;
import org.jkiss.dbeaver.utils.ViewUtils;

/**
 * FolderEditor
 */
public class FolderEditor extends SinglePageDatabaseEditor<FolderEditorInput> implements IMetaModelView
{
    static final Log log = LogFactory.getLog(FolderEditor.class);

    private ItemListControl itemControl;

    public void createPartControl(Composite parent)
    {
        itemControl = new ItemListControl(parent, SWT.NONE, this, getEditorInput().getTreeNode());
        itemControl.fillData();
        // Hook context menu
        ViewUtils.addContextMenu(this);
        // Add drag and drop support
        ViewUtils.addDragAndDropSupport(this);
        getSite().setSelectionProvider(itemControl.getSelectionProvider());
    }

    public DBMModel getMetaModel()
    {
        return getEditorInput().getTreeNode().getModel();
    }

    public Viewer getViewer()
    {
        return itemControl.getViewer();
    }

    public IWorkbenchPart getWorkbenchPart()
    {
        return this;
    }

}
