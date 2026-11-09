/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.datadam;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;

public class DDUIUtils {
    private DDUIUtils() {
    }

    @NotNull
    public static Label create(
        @NotNull Composite parent, @NotNull String text, @NotNull String linkText, @NotNull String linkUrl
    ) {
        Composite row = UIUtils.createComposite(parent, 2);
        row.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Label icon = new Label(row, SWT.NONE);
        icon.setImage(DBeaverIcons.getImage(DBIcon.SMALL_INFO));
        icon.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));

        Label label = new Label(row, SWT.WRAP);
        label.setText(text);
        GridData labelLayout = new GridData(SWT.FILL, SWT.CENTER, true, false);
        labelLayout.widthHint = 480;
        label.setLayoutData(labelLayout);

        new Label(row, SWT.NONE);
        Link link = UIUtils.createLink(row, "<a>" + linkText + "</a>",
            SelectionListener.widgetSelectedAdapter(e -> UIUtils.openWebBrowser(linkUrl)));
        link.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
        return label;
    }
}
