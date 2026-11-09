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
package org.jkiss.dbeaver.ext.postgresql.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ExpandableCompositeEx;

final class PostgreSubscriptionDialogLayout {
    static final int MARGIN = 20;
    static final int COMPACT_SPACING = 5;
    static final int ROW_SPACING = 10;
    static final int SECTION_SPACING = 20;
    static final int LABEL_WIDTH = 100;

    private PostgreSubscriptionDialogLayout() {
    }

    @NotNull
    static Composite createSection(@NotNull Composite parent, @NotNull String title, int style) {
        ExpandableCompositeEx section = UIUtils.createExpandableCompositeWithSeparator(parent, SWT.NONE,
            ExpandableCompositeEx.EXPANDED);
        section.setShowTextAsTitle(true);
        section.setText(title);
        section.clientVerticalSpacing = ROW_SPACING;
        section.setLayoutData(new GridData(style));
        Composite client = UIUtils.createComposite(section, 1);
        GridLayout layout = (GridLayout) client.getLayout();
        layout.marginLeft = MARGIN;
        section.setClient(client);
        return client;
    }

    static int alignLabels(int minimumWidth, @NotNull Composite... forms) {
        int width = minimumWidth;
        for (Composite form : forms) {
            int columns = ((GridLayout) form.getLayout()).numColumns;
            Control[] children = form.getChildren();
            for (int index = 0; index < children.length; index += columns) {
                if (children[index] instanceof Label label) {
                    width = Math.max(width, label.computeSize(SWT.DEFAULT, SWT.DEFAULT).x);
                }
            }
        }
        for (Composite form : forms) {
            int columns = ((GridLayout) form.getLayout()).numColumns;
            Control[] children = form.getChildren();
            for (int index = 0; index < children.length; index += columns) {
                if (children[index] instanceof Label label) {
                    GridData data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
                    data.widthHint = width;
                    label.setLayoutData(data);
                }
            }
        }
        return width;
    }
}
