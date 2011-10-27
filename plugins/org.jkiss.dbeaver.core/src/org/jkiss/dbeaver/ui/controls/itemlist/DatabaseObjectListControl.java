/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.itemlist;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.controls.ObjectViewerRenderer;

/**
 * DatabaseObjectListControl
 */
public abstract class DatabaseObjectListControl<OBJECT_TYPE extends DBPObject> extends ObjectListControl<OBJECT_TYPE> {

    protected DatabaseObjectListControl(
        Composite parent,
        int style,
        IContentProvider contentProvider)
    {
        super(parent, style, contentProvider);
        setFitWidth(true);

        createContextMenu();
    }

    protected ObjectViewerRenderer createRenderer()
    {
        return new ObjectListRenderer();
    }

    private void createContextMenu()
    {
        Control control = getControl();
        MenuManager menuMgr = new MenuManager();
        Menu menu = menuMgr.createContextMenu(control);
        menuMgr.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(IMenuManager manager)
            {
                IAction copyAction = new Action(CoreMessages.controls_itemlist_action_copy) {
                    public void run()
                    {
                        String text = getRenderer().getSelectedText();
                        if (text != null) {
                            TextTransfer textTransfer = TextTransfer.getInstance();
                            Clipboard clipboard = new Clipboard(getDisplay());
                            clipboard.setContents(
                                new Object[]{text},
                                new Transfer[]{textTransfer});
                        }
                    }
                };
                copyAction.setEnabled(!getSelectionProvider().getSelection().isEmpty());
                manager.add(copyAction);
            }
        });
        menuMgr.setRemoveAllWhenShown(true);
        control.setMenu(menu);
    }

    private class ObjectListRenderer extends ViewerRenderer {
        public boolean isHyperlink(Object cellValue)
        {
            return cellValue instanceof DBSObject;
        }

        public void navigateHyperlink(Object cellValue)
        {
            if (cellValue instanceof DBSObject) {
                NavigatorHandlerObjectOpen.openEntityEditor((DBSObject) cellValue);
            }
        }
    }

}
