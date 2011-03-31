/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.folder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.ext.ui.INavigatorModelView;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.ui.controls.itemlist.ItemListControl;
import org.jkiss.dbeaver.ui.editors.SinglePageDatabaseEditor;
import org.jkiss.dbeaver.utils.ViewUtils;

/**
 * FolderEditor
 */
public class FolderEditor extends SinglePageDatabaseEditor<FolderEditorInput> implements INavigatorModelView
{
    static final Log log = LogFactory.getLog(FolderEditor.class);

    private ItemListControl itemControl;

    public void createPartControl(Composite parent)
    {
        itemControl = new ItemListControl(parent, SWT.NONE, this, getEditorInput().getTreeNode(), null);
        itemControl.createProgressPanel();
        itemControl.loadData();
        // Hook context menu
        ViewUtils.addContextMenu(this, itemControl.getNavigatorViewer());
        // Add drag and drop support
        ViewUtils.addDragAndDropSupport(itemControl.getNavigatorViewer());
        getSite().setSelectionProvider(itemControl.getSelectionProvider());
    }

    public DBNNode getRootNode() {
        return getEditorInput().getTreeNode();
    }

    public Viewer getNavigatorViewer()
    {
        return itemControl.getNavigatorViewer();
    }

    public void refreshDatabaseContent(DBNEvent event) {
        Display.getDefault().asyncExec(new Runnable() {
            public void run()
            {

                if (!itemControl.isDisposed()) {
                    itemControl.loadData();
                }

            }
        });
    }
}
