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
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.custom.CTabItem;

public class TabbedFolderAccessibleAdapter extends AccessibleAdapter {
    private final TabbedFolderList control;

    public TabbedFolderAccessibleAdapter(TabbedFolderList list) {
        this.control = list;
    }

    @Override
    public void getName(AccessibleEvent e) {
        TabbedFolderInfo item = null;
        int childID = e.childID;
        if (childID == ACC.CHILDID_SELF) {
            if (control.getSelectionIndex() != -1) {
                item = control.getElements()[control.getSelectionIndex()];
            }
        } else if (childID >= 0 && childID < control.getElements().length) {
            item = control.getElements()[childID];
        }
        e.result = item == null ? null : stripMnemonic(item.getText());
    }

    @Override
    public void getHelp(AccessibleEvent e) {
        String help = null;
        int childID = e.childID;
        if (childID == ACC.CHILDID_SELF) {
            help = control.getToolTipText();
        } else if (childID >= 0 && childID < control.getElements().length) {
            help = control.getElements()[childID].getTooltip();
        }
        e.result = help;
    }

    public static String stripMnemonic (String string) {
        int index = 0;
        int length = string.length();
        do {
            while ((index < length) && (string.charAt(index) != '&'))
                index++;
            if (++index >= length)
                return string;
            if (string.charAt(index) != '&') {
                return string.substring(0, index - 1) + string.substring(index, length);
            }
            index++;
        } while (index < length);
        return string;
    }
}
