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

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.jkiss.code.NotNull;

final class BreadcrumbItem extends Item {
    private final BreadcrumbViewer viewer;

    private final Composite container;
    private final BreadcrumbItemDetails detailsBlock;
    private final BreadcrumbItemDropDown expandBlock;

    private ILabelProvider labelProvider;
    private ITreeContentProvider contentProvider;
    private ILabelProvider toolTipLabelProvider;

    private boolean last;

    public BreadcrumbItem(@NotNull BreadcrumbViewer viewer, @NotNull Composite parent) {
        super(parent, SWT.NONE);
        this.viewer = viewer;

        container = new Composite(parent, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        container.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).spacing(0, 0).create());

        detailsBlock = new BreadcrumbItemDetails(this, container);
        expandBlock = new BreadcrumbItemDropDown(this, container);
    }

    @Override
    public void dispose() {
        super.dispose();
        container.dispose();
    }

    public void refresh() {
        Object input = getData();

        detailsBlock.setText(labelProvider.getText(input));
        detailsBlock.setImage(labelProvider.getImage(input));
        detailsBlock.setToolTipText(toolTipLabelProvider.getText(input));

        refreshArrow();
    }

    public void refreshArrow() {
        expandBlock.setEnabled(contentProvider.hasChildren(getData()));
    }

    @NotNull
    public BreadcrumbViewer getViewer() {
        return viewer;
    }

    @NotNull
    public Rectangle getBounds() {
        return container.getBounds();
    }

    public void setLabelProvider(@NotNull ILabelProvider labelProvider) {
        this.labelProvider = labelProvider;
    }

    public void setContentProvider(@NotNull ITreeContentProvider contentProvider) {
        this.contentProvider = contentProvider;
    }

    public void setToolTipLabelProvider(@NotNull ILabelProvider toolTipLabelProvider) {
        this.toolTipLabelProvider = toolTipLabelProvider;
    }

    public void setLast(boolean last) {
        this.last = last;
        ((GridData) container.getLayoutData()).grabExcessHorizontalSpace = last;
    }
}
