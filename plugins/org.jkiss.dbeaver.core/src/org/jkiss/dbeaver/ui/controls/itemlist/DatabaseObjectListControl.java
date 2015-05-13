/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.controls.itemlist;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
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

    @Override
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
            @Override
            public void menuAboutToShow(IMenuManager manager)
            {
                IAction copyAction = new Action(CoreMessages.controls_itemlist_action_copy) {
                    @Override
                    public void run()
                    {
                        String text = getRenderer().getSelectedText();
                        if (text != null) {
                            UIUtils.setClipboardContents(getDisplay(), TextTransfer.getInstance(), text);
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
        @Override
        public boolean isHyperlink(Object cellValue)
        {
            return cellValue instanceof DBSObject;
        }

        @Override
        public void navigateHyperlink(Object cellValue)
        {
            if (cellValue instanceof DBSObject) {
                NavigatorHandlerObjectOpen.openEntityEditor((DBSObject) cellValue);
            }
        }
    }

}
