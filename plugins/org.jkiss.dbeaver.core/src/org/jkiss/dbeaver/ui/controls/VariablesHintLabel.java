/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.EditTextDialog;
import org.jkiss.dbeaver.utils.GeneralUtils;

/**
 * VariablesHintLabel
 */
public class VariablesHintLabel
{

    public VariablesHintLabel(Composite parent, String[][] vars)
    {
        String varsText = GeneralUtils.generateVariablesLegend(vars);

        CLabel infoLabel = UIUtils.createInfoLabel(parent, "You can use variables in commands. Click to see the list.");
        Layout layout = parent.getLayout();
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        if (layout instanceof GridLayout) {
            gd.horizontalSpan = ((GridLayout) layout).numColumns;
        }
        infoLabel.setLayoutData(gd);
        infoLabel.setCursor(infoLabel.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
        infoLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                new EditTextDialog(parent.getShell(), "Variables", varsText, true).open();
            }
        });
        infoLabel.setToolTipText(varsText);

    }

}