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
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Widget;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class BreadcrumbViewer extends StructuredViewer {
    private final List<BreadcrumbItem> breadcrumbItems = new ArrayList<>();

    private final Composite container;

    private ILabelProvider toolTipLabelProvider;
    private BreadcrumbItem selectedItem;

    public BreadcrumbViewer(@NotNull Composite parent) {
        container = new Composite(parent, SWT.NONE);
        container.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
        container.setLayout(GridLayoutFactory.fillDefaults().spacing(0, 0).numColumns(1000).create());
        container.addListener(SWT.Resize, e -> refresh());
    }

    @Override
    public Control getControl() {
        return container;
    }

    @Override
    protected Object getRoot() {
        if (breadcrumbItems.isEmpty()) {
            return null;
        }
        return breadcrumbItems.get(0);
    }

    @Override
    protected void inputChanged(Object input, Object oldInput) {
        if (container.isDisposed()) {
            return;
        }

        try (var ignored = UIUtils.disableRedraw(container)) {
            if (!breadcrumbItems.isEmpty()) {
                breadcrumbItems.get(breadcrumbItems.size() - 1).setTrailing(false);
            }

            int lastIndex = buildItemChain(input);
            if (lastIndex > 0) {
                breadcrumbItems.get(lastIndex - 1).setTrailing(true);
            }

            while (lastIndex < breadcrumbItems.size()) {
                BreadcrumbItem item = breadcrumbItems.remove(breadcrumbItems.size() - 1);
                unmapElement(item.getData());
                item.dispose();
            }

            updateSize();
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
        for (BreadcrumbItem item : breadcrumbItems) {
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

        item.refresh();
    }

    @Override
    protected List<?> getSelectionFromWidget() {
        if (selectedItem == null || selectedItem.getData() == null) {
            return List.of();
        }
        return List.of(selectedItem.getData());
    }

    @Override
    protected void internalRefresh(Object element) {
        try (var ignored = UIUtils.disableRedraw(container)) {
            BreadcrumbItem item = (BreadcrumbItem) doFindInputItem(element);
            if (item == null) {
                for (BreadcrumbItem item1 : breadcrumbItems) {
                    item1.refresh();
                }
            } else {
                item.refresh();
            }
            if (updateSize()) {
                container.layout(true, true);
            }
        }
    }

    @Override
    public void reveal(Object element) {
        // all elements are always visible
    }

    @Override
    protected void setSelectionToWidget(List l, boolean reveal) {
        if (l == null) {
            return;
        }

        for (Object element : l) {
            BreadcrumbItem item = (BreadcrumbItem) doFindItem(element);
            if (item != null) {
                selectedItem = item;
            }
        }
    }

    @Override
    protected void assertContentProviderType(IContentProvider provider) {
        Assert.isTrue(provider instanceof ITreeContentProvider);
    }

    @Override
    protected void handleDispose(DisposeEvent event) {
        for (BreadcrumbItem item : breadcrumbItems) {
            item.dispose();
        }

        super.handleDispose(event);
    }

    void selectItem(@Nullable BreadcrumbItem item) {
        selectedItem = item;
        setSelectionToWidget(getSelection(), false);
    }

    void fireMenuSelection(@NotNull Object element) {
        fireOpen(new OpenEvent(this, new StructuredSelection(element)));
    }

    void fireDoubleClick() {
        fireDoubleClick(new DoubleClickEvent(this, getSelection()));
    }

    @Nullable
    public ILabelProvider getToolTipLabelProvider() {
        return toolTipLabelProvider;
    }

    public void setToolTipLabelProvider(@Nullable ILabelProvider toolTipLabelProvider) {
        this.toolTipLabelProvider = toolTipLabelProvider;
    }

    /**
     * Configure the given drop down viewer. The given input is used for the viewers input. Clients
     * must at least set the label and the content provider for the viewer.
     *
     * @param viewer the viewer to configure
     * @param input  the input for the viewer
     */
    protected abstract void configureDropDownViewer(@NotNull TreeViewer viewer, @NotNull Object input);

    private boolean updateSize() {
        int width = container.getClientArea().width;
        int currentWidth = computeWidth();
        boolean requiresLayout = false;

        if (currentWidth > width) {
            int index = 0;
            while (currentWidth > width && index < breadcrumbItems.size()) {
                BreadcrumbItem item = breadcrumbItems.get(index);
                if (item.isShowText()) {
                    item.setShowText(false);
                    currentWidth = computeWidth();
                    requiresLayout = true;
                }

                index++;
            }
        } else if (currentWidth < width) {
            int index = breadcrumbItems.size() - 1;
            while (currentWidth < width && index >= 0) {
                BreadcrumbItem item = breadcrumbItems.get(index);
                if (!item.isShowText()) {
                    item.setShowText(true);
                    currentWidth = computeWidth();
                    if (currentWidth > width) {
                        item.setShowText(false);
                        index = 0;
                    } else {
                        requiresLayout = true;
                    }
                }

                index--;
            }
        }

        return requiresLayout;
    }

    private int computeWidth() {
        int result = 0;
        for (BreadcrumbItem item : breadcrumbItems) {
            result += item.computeWidth();
        }
        return result;
    }

    private int buildItemChain(@Nullable Object element) {
        if (element == null) {
            return 0;
        }

        var provider = (ITreeContentProvider) getContentProvider();
        var parent = provider.getParent(element);
        int index = buildItemChain(parent);

        BreadcrumbItem item;
        if (index < breadcrumbItems.size()) {
            item = breadcrumbItems.get(index);
            unmapElement(item.getData());
        } else {
            item = createItem();
            breadcrumbItems.add(item);
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
}
