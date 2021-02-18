/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls.finder.viewer;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Widget;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.finder.AdvancedList;
import org.jkiss.dbeaver.ui.controls.finder.AdvancedListItem;

import java.util.ArrayList;
import java.util.List;

/**
 * AdvancedListViewer
 */
public class AdvancedListViewer extends StructuredViewer {
    private static final Log log = Log.getLog(AdvancedListViewer.class);

    private AdvancedList control;

    public AdvancedListViewer(Composite parent, int style) {
        this.control = new AdvancedList(parent, style);

        this.control.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                fireSelectionChanged(new SelectionChangedEvent(AdvancedListViewer.this, getSelection()));
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                fireDoubleClick(new DoubleClickEvent(AdvancedListViewer.this, getSelection()));
            }
        });

        control.addPaintListener(e -> {
            ViewerFilter[] filters = getFilters();
            if (control.getItems().length == 0 && filters != null && filters.length > 0) {
                UIUtils.drawMessageOverControl(control, e, "No items found", 0);
            }
        });
    }

    @Override
    public Control getControl() {
        return control;
    }

    @Override
    protected Widget doFindInputItem(Object element) {
        return null;
    }

    @Override
    protected Widget doFindItem(Object element) {
        return null;
    }

    @Override
    protected void doUpdateItem(Widget item, Object element, boolean fullMap) {

    }

    @Override
    protected List<Object> getSelectionFromWidget() {
        List<Object> list = new ArrayList<>();
        AdvancedListItem item = this.control.getSelectedItem();
        if (item != null) {
            list.add(item.getData());
        }
        return list;
    }

    @Override
    protected void internalRefresh(Object element) {
        control.removeAll();

        IStructuredContentProvider contentProvider = (IStructuredContentProvider) getContentProvider();
        ILabelProvider labelProvider = (ILabelProvider) getLabelProvider();
        //IToolTipProvider toolTipProvider = labelProvider instanceof IToolTipProvider ? (IToolTipProvider) labelProvider : null;
        Object[] elements = contentProvider.getElements(element);
        for (ViewerFilter filter : getFilters()) {
            elements = filter.filter(this, (Object)null, elements);
        }
        for (Object item : elements) {
            new AdvancedListItem(control, item, labelProvider);
        }

        control.redraw();
    }

    @Override
    public void reveal(Object element) {
        //control.showControl();
    }

    @Override
    protected void setSelectionToWidget(List l, boolean reveal) {

    }

    @Override
    protected void inputChanged(Object input, Object oldInput) {
        super.inputChanged(input, oldInput);
        internalRefresh(getInput());
    }

    @Override
    public void setFilters(ViewerFilter... filters) {
        super.setFilters(filters);
        control.refreshFilters();
    }

    @Override
    public void resetFilters() {
        super.resetFilters();
        control.refreshFilters();
    }
}
