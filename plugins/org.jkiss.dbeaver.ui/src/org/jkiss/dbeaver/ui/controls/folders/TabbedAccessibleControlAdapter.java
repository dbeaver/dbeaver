/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.controls.folders;

import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.ACC;
import org.eclipse.swt.accessibility.AccessibleControlAdapter;
import org.eclipse.swt.accessibility.AccessibleControlEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

public class TabbedAccessibleControlAdapter extends AccessibleControlAdapter {

    private final TabbedFolderList control;

    public TabbedAccessibleControlAdapter(TabbedFolderList list) {
        this.control = list;
    }

    public void getChildAtPoint(AccessibleControlEvent e) {
        Point testPoint = control.toControl(e.x, e.y);
        int childID = ACC.CHILDID_NONE;
        for (int i = 0; i < control.getElements().length; i++) {
            if (control.getElementAt(i).getBounds().contains(testPoint)) {
                childID = i;
                break;
            }
        }
        if (childID == ACC.CHILDID_NONE) {
            Rectangle location = control.getBounds();
            location.x = location.y = 0;
            location.height = location.height - control.getClientArea().height;
            if (location.contains(testPoint)) {
                childID = ACC.CHILDID_SELF;
            }
        }
        e.childID = childID;
    }

    @Override
    public void getLocation(AccessibleControlEvent e) {
        Rectangle location = null;
        Point pt = null;
        int childID = e.childID;
        if (childID == ACC.CHILDID_SELF) {
            location = control.getBounds();
            pt = control.getParent().toDisplay(location.x, location.y);
        } else {
            if (childID >= 0 && childID < control.getElements().length && control.getElementAt(childID).isVisible()) {
                if (!control.getElementAt(childID).isDisposed()) {
                    location = control.getElementAt(childID).getBounds();
                }
            }
            if (location != null) {
                pt = control.toDisplay(location.x, location.y);
            }
        }
        if (location != null && pt != null) {
            e.x = pt.x;
            e.y = pt.y;
            e.width = location.width;
            e.height = location.height;
        }
    }

    @Override
    public void getChildCount(AccessibleControlEvent e) {
        e.detail = control.getElements().length;
    }

    @Override
    public void getDefaultAction(AccessibleControlEvent e) {
        String action = null;
        int childID = e.childID;
        if (childID >= 0 && childID < control.getElements().length) {
            action = SWT.getMessage("SWT_Switch"); //$NON-NLS-1$
        }
        e.result = action;
    }

    @Override
    public void getFocus(AccessibleControlEvent e) {
        int childID = ACC.CHILDID_NONE;
        if (control.isFocusControl()) {
            if (control.getSelectionIndex() == -1) {
                childID = ACC.CHILDID_SELF;
            } else {
                childID = control.getSelectionIndex();
            }
        }
        e.childID = childID;
    }

    @Override
    public void getRole(AccessibleControlEvent e) {
        int role = 0;
        int childID = e.childID;
        if (childID == ACC.CHILDID_SELF) {
            role = ACC.ROLE_TABFOLDER;
        } else if (childID >= 0 && childID < control.getElements().length) {
            role = ACC.ROLE_TABITEM;
        }
        e.detail = role;
    }

    @Override
    public void getSelection(AccessibleControlEvent e) {
        e.childID = (control.getSelectionIndex() == -1) ? ACC.CHILDID_NONE : control.getSelectionIndex();
    }

    @Override
    public void getState(AccessibleControlEvent e) {
        int state = 0;
        int childID = e.childID;
        if (childID == ACC.CHILDID_SELF) {
            state = ACC.STATE_NORMAL;
        } else if (childID >= 0 && childID < control.getElements().length) {
            state = ACC.STATE_SELECTABLE;
            if (control.isFocusControl()) {
                state |= ACC.STATE_FOCUSABLE;
            }
            if (control.getSelectionIndex() == childID) {
                state |= ACC.STATE_SELECTED;
                if (control.isFocusControl()) {
                    state |= ACC.STATE_FOCUSED;
                }
            }
        }
        e.detail = state;
    }

    @Override
    public void getChildren(AccessibleControlEvent e) {
        int childIdCount = control.getElements().length;
        Object[] children = new Object[childIdCount];
        for (int i = 0; i < childIdCount; i++) {
            children[i] = Integer.valueOf(i);
        }
        e.children = children;
    }

}
