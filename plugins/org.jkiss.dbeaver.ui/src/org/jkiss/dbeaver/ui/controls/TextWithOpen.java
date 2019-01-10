/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.DBeaverIcons;

/**
 * TextWithOpen
 */
public class TextWithOpen extends Composite
{
    private final Text text;

    public TextWithOpen(Composite parent)
    {
        super(parent, SWT.NONE);
        final GridLayout gl = new GridLayout(2, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        gl.verticalSpacing = 0;
        gl.horizontalSpacing = 5;
        setLayout(gl);

        text = new Text(this, SWT.BORDER);
        text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        ToolBar toolbar = new ToolBar(this, SWT.FLAT);
        final ToolItem toolItem = new ToolItem(toolbar, SWT.NONE);
        toolItem.setImage(DBeaverIcons.getImage(DBIcon.TREE_FOLDER));
        toolItem.setToolTipText("Browse");
        toolItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                openBrowser();
            }
        });

        final GridData gd = new GridData(GridData.VERTICAL_ALIGN_CENTER | GridData.HORIZONTAL_ALIGN_CENTER);
        toolbar.setLayoutData(gd);

//        final Image browseImage = DBeaverIcons.getImage(DBIcon.TREE_FOLDER);
//        final Rectangle iconBounds = browseImage.getBounds();
//        Label button = new Label(this, SWT.NONE);
//        button.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));
//        button.setImage(browseImage);
//        final GridData gd = new GridData(GridData.VERTICAL_ALIGN_CENTER | GridData.HORIZONTAL_ALIGN_CENTER);
//        gd.widthHint = iconBounds.width;
//        gd.heightHint = iconBounds.height;
//        button.setLayoutData(gd);
    }

    public String getText() {
        return text.getText();
    }

    public void setText(String str) {
        text.setText(str);
    }

    protected void openBrowser() {

    }

    public Text getTextControl() {
        return text;
    }
}