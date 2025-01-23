/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls.breadcrumb;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;

final class BreadcrumbItemDropDown {
    private final BreadcrumbItem item;
    private final Composite composite;
    private final Label arrowElement;

    BreadcrumbItemDropDown(@NotNull BreadcrumbItem item, @NotNull Composite composite) {
        this.item = item;
        this.composite = composite;

        arrowElement = new Label(composite, SWT.NONE);
        arrowElement.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
        arrowElement.setImage(DBeaverIcons.getImage(UIIcon.TREE_EXPAND));
    }

    public void setEnabled(boolean enabled) {
        UIUtils.setControlVisible(arrowElement, enabled);
    }
}
