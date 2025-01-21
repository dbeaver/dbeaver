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

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Widget;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public /*abstract*/ class BreadcrumbViewer extends StructuredViewer {
    private final List<BreadcrumbItem> items = new ArrayList<>();
    private final Composite container;

    private ILabelProvider toolTipLabelProvider;

    public BreadcrumbViewer(@NotNull Composite parent) {
        container = new Composite(parent, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        container.setLayout(GridLayoutFactory.fillDefaults().extendedMargins(3, 0, 3, 0).spacing(0, 0).numColumns(1000).create());
    }

    @Override
    public Control getControl() {
        return container;
    }

    @Override
    protected Object getRoot() {
        if (items.isEmpty()) {
            return null;
        }
        return items.get(0);
    }

    @Override
    protected void inputChanged(Object input, Object oldInput) {
        if (container.isDisposed()) {
            return;
        }

        try (var ignored = UIUtils.disableRedraw(container)) {
            if (!items.isEmpty()) {
                items.get(items.size() - 1).setLast(false);
            }

            int lastIndex = buildItemChain(input);
            if (lastIndex > 0) {
                items.get(lastIndex - 1).setLast(true);
            }

            while (lastIndex < items.size()) {
                BreadcrumbItem item = items.remove(items.size() - 1);
                unmapElement(item.getData());
                item.dispose();
            }

            container.layout(true, true);
        }
    }

    @Override
    protected Widget doFindInputItem(Object element) {
        if (Objects.equals(element, getInput())) {
            return doFindItem(element);
        }
        return null;
    }

    @Override
    protected Widget doFindItem(Object element) {
        if (element == null) {
            return null;
        }
        for (BreadcrumbItem item : items) {
            if (Objects.equals(element, item.getData())) {
                return item;
            }
        }
        return null;
    }

    @Override
    protected void doUpdateItem(Widget widget, Object element, boolean fullMap) {
        if (!(widget instanceof BreadcrumbItem item)) {
            return;
        }

        if (fullMap) {
            associate(element, item);
        } else {
            unmapElement(item.getData());
            item.setData(element);
            mapElement(element, item);
        }

        item.refreshArrow();
    }

    @Override
    protected List<?> getSelectionFromWidget() {
        return List.of(); // TODO Support selection
    }

    @Override
    protected void internalRefresh(Object element) {
        try (var ignored = UIUtils.disableRedraw(container)) {
            BreadcrumbItem item = (BreadcrumbItem) doFindInputItem(element);
            if (item == null) {
                for (BreadcrumbItem item1 : items) {
                    item1.refresh();
                }
            } else {
                item.refresh();
            }
        }
    }

    @Override
    public void reveal(Object element) {
        // all elements are always visible
    }

    @Override
    protected void setSelectionToWidget(List l, boolean reveal) {
        // TODO Support selection
    }

    @Override
    protected void assertContentProviderType(IContentProvider provider) {
        Assert.isTrue(provider instanceof ITreeContentProvider);
    }

    @Override
    protected void handleDispose(DisposeEvent event) {
        for (BreadcrumbItem item : items) {
            item.dispose();
        }

        super.handleDispose(event);
    }

    @Nullable
    public ILabelProvider getToolTipLabelProvider() {
        return toolTipLabelProvider;
    }

    public void setToolTipLabelProvider(@Nullable ILabelProvider toolTipLabelProvider) {
        this.toolTipLabelProvider = toolTipLabelProvider;
    }

    private int buildItemChain(@Nullable Object element) {
        if (element == null) {
            return 0;
        }

        var provider = (ITreeContentProvider) getContentProvider();
        var parent = provider.getParent(element);
        int index = buildItemChain(parent);

        BreadcrumbItem item;
        if (index < items.size()) {
            item = items.get(index);
            unmapElement(item.getData());
        } else {
            item = createItem();
            items.add(item);
        }

        if (equals(element, item.getData())) {
            update(element, null);
        } else {
            item.setData(element);
            item.refresh();
        }

        mapElement(element, item);

        return index + 1;
    }

    @NotNull
    private BreadcrumbItem createItem() {
        BreadcrumbItem item = new BreadcrumbItem(this, container);
        item.setLabelProvider((ILabelProvider) getLabelProvider());
        item.setContentProvider((ITreeContentProvider) getContentProvider());
        if (toolTipLabelProvider != null) {
            item.setToolTipLabelProvider(toolTipLabelProvider);
        } else {
            item.setToolTipLabelProvider((ILabelProvider) getLabelProvider());
        }
        return item;
	}

    // public abstract void handleSelection(@NotNull BreadcrumbItem item);
    //
    // public abstract void populateContextMenu(@NotNull IMenuManager manager, @NotNull BreadcrumbItem item);
    //
    // public abstract void populateDropDownMenu(@NotNull TreeViewer viewer, @NotNull BreadcrumbItem item);
}
