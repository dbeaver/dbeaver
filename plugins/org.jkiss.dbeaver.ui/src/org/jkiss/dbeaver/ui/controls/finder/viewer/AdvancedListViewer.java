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
package org.jkiss.dbeaver.ui.controls.finder.viewer;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Widget;
import org.jkiss.dbeaver.Log;
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
        if (parent.getLayout() instanceof GridLayout) {
            this.control.setLayoutData(new GridData(GridData.FILL_BOTH));
        }
    }

    @Override
    public AdvancedList getControl() {
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
    protected List getSelectionFromWidget() {
        return new ArrayList();
    }

    @Override
    protected void internalRefresh(Object element) {
        IStructuredContentProvider contentProvider = (IStructuredContentProvider) getContentProvider();
        ILabelProvider labelProvider = (ILabelProvider) getLabelProvider();
        Object[] elements = contentProvider.getElements(element);
        for (Object item : elements) {
            String text = labelProvider.getText(item);
            Image icon = labelProvider.getImage(item);
            AdvancedListItem listItem = new AdvancedListItem(control, text, icon);
            listItem.setData(item);
        }
    }

    @Override
    public void reveal(Object element) {

    }

    @Override
    protected void setSelectionToWidget(List l, boolean reveal) {

    }

    @Override
    protected void inputChanged(Object input, Object oldInput) {
        super.inputChanged(input, oldInput);
        internalRefresh(getInput());
    }
}
