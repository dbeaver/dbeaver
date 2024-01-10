/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls.imageview;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;

import java.io.InputStream;

/**
 * Image editor, based on image viewer.
 */
public class BrowserImageEditor extends BrowserImageViewer {

    public BrowserImageEditor(Composite parent, int style) {
        super(parent, style);

        {
            // Status & toolbar
            Composite statusGroup = new Composite(this, SWT.NONE);
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            statusGroup.setLayoutData(gd);

            GridLayout layout = new GridLayout(2, false);
            layout.verticalSpacing = 0;
            layout.horizontalSpacing = 0;
            statusGroup.setLayout(layout);

            Text messageLabel = new Text(statusGroup, SWT.READ_ONLY);
            messageLabel.setText(""); //$NON-NLS-1$
            gd = new GridData(GridData.FILL_HORIZONTAL);
            messageLabel.setLayoutData(gd);

            {
                ToolBarManager toolBar = new ToolBarManager(SWT.NONE);
                toolBar.createControl(statusGroup);
            }
        }
    }

    @Override
    public boolean loadImage(@NotNull InputStream inputStream) {
        super.loadImage(inputStream);
        return true;
    }
}
