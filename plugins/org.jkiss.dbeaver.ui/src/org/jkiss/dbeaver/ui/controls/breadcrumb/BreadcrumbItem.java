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
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;

final class BreadcrumbItem extends Item {
    private static final int MAX_DROP_DOWN_ITEMS = 20;
    private final Log log = Log.getLog(BreadcrumbItem.class);

    private final BreadcrumbViewer viewer;

    private final Composite container;
    private final Label elementArrow;
    private final Label elementImage;
    private final Label elementText;

    private ILabelProvider labelProvider;
    private ITreeContentProvider contentProvider;
    private ILabelProvider toolTipLabelProvider;

    private Shell menuShell;

    public BreadcrumbItem(@NotNull BreadcrumbViewer viewer, @NotNull Composite parent) {
        super(parent, SWT.NONE);
        this.viewer = viewer;

        container = new Composite(parent, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        container.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).spacing(0, 0).create());

        elementArrow = new Label(container, SWT.NONE);
        elementArrow.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
        elementArrow.setImage(DBeaverIcons.getImage(UIIcon.TREE_EXPAND));

        var detailComposite = new Composite(container, SWT.NONE);
        detailComposite.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        detailComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).spacing(0, 0).create());

        var imageComposite = new Composite(detailComposite, SWT.NONE);
        imageComposite.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        imageComposite.setLayout(GridLayoutFactory.fillDefaults().margins(2, 1).create());

        var textComposite = new Composite(detailComposite, SWT.NONE);
        textComposite.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        textComposite.setLayout(GridLayoutFactory.fillDefaults().margins(2, 2).create());

        elementImage = new Label(imageComposite, SWT.NONE);
        elementImage.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

        elementText = new Label(textComposite, SWT.NONE);
        elementText.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

        addElementListener(detailComposite);
        addElementListener(imageComposite);
        addElementListener(textComposite);
        addElementListener(elementImage);
        addElementListener(elementText);
    }

    @Override
    public void dispose() {
        super.dispose();
        container.dispose();
    }

    public void refresh() {
        Object input = getData();

        setText(labelProvider.getText(input));
        setImage(labelProvider.getImage(input));
        setToolTipText(toolTipLabelProvider.getText(input));
        setArrowVisible(contentProvider.getParent(getData()) != null);
    }

    @NotNull
    public BreadcrumbViewer getViewer() {
        return viewer;
    }

    @NotNull
    public Composite getContainer() {
        return container;
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

    public void setImage(@Nullable Image image) {
        if (image != elementImage.getImage()) {
            elementImage.setImage(image);
        }
    }

    public void setText(@Nullable String text) {
        if (text == null) {
            text = "";
        }
        if (!text.equals(elementText.getText())) {
            elementText.setText(text);
        }
    }

    public void setToolTipText(@Nullable String toolTipText) {
        elementText.getParent().setToolTipText(toolTipText);
        elementText.setToolTipText(toolTipText);
        elementImage.setToolTipText(toolTipText);
    }

    public void setTrailing(boolean trailing) {
        ((GridData) container.getLayoutData()).grabExcessHorizontalSpace = trailing;
    }

    private void setArrowVisible(boolean visible) {
        UIUtils.setControlVisible(elementArrow, visible);
    }

    private void showMenu() {
        menuShell = new Shell(container.getShell(), SWT.RESIZE | SWT.CLOSE | SWT.TOOL | SWT.ON_TOP);
        menuShell.setLayout(new FillLayout());

        var menuViewer = new TreeViewer(menuShell, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
        var input = getData();
        viewer.configureDropDownViewer(menuViewer, input);
        menuViewer.setInput(input);

        menuShell.setBounds(100, 100, 200, 200);
        menuShell.setVisible(true);
    }

    private void openElement(@NotNull Object element) {
        viewer.fireMenuSelection(element);
    }

    private void addElementListener(@NotNull Control control) {
        control.addMenuDetectListener(e -> showMenu());
        control.addMouseListener(MouseListener.mouseDoubleClickAdapter(e -> {
            BreadcrumbViewer viewer = getViewer();
            viewer.selectItem(BreadcrumbItem.this);
            viewer.fireDoubleClick();
        }));
    }
}
