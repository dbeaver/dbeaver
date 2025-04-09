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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.EmptyAction;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.DoubleClickMouseAdapter;
import org.jkiss.dbeaver.utils.NLS;
import org.jkiss.utils.CommonUtils;

final class BreadcrumbItem extends Item {
    private static final int DROP_DOWN_MAX_ITEMS = 30;

    private final BreadcrumbViewer viewer;

    private final Composite container;
    private final Label elementArrow;
    private final Label elementImage;
    private final Label elementText;
    private final Composite detailComposite;
    private final Composite imageComposite;
    private final Composite textComposite;
    private final MenuManager menuManager;

    private ILabelProvider labelProvider;
    private ITreeContentProvider contentProvider;
    private ILabelProvider toolTipLabelProvider;

    private boolean showText = true;

    public BreadcrumbItem(@NotNull BreadcrumbViewer viewer, @NotNull Composite parent) {
        super(parent, SWT.NONE);
        this.viewer = viewer;

        container = new Composite(parent, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, true));
        container.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).spacing(0, 0).create());

        elementArrow = new Label(container, SWT.NONE);
        elementArrow.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
        elementArrow.setImage(DBeaverIcons.getImage(UIIcon.TREE_EXPAND));

        detailComposite = new Composite(container, SWT.NONE);
        detailComposite.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        detailComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).spacing(0, 0).create());

        imageComposite = new Composite(detailComposite, SWT.NONE);
        imageComposite.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        imageComposite.setLayout(GridLayoutFactory.fillDefaults().margins(2, 1).create());

        textComposite = new Composite(detailComposite, SWT.NONE);
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

        menuManager = new MenuManager();
        menuManager.setRemoveAllWhenShown(true);
        menuManager.addMenuListener(manager -> {
            var contentProvider = viewer.getDropDownContentProvider();
            var elements = contentProvider.getElements(getData());

            for (int i = 0; i < Math.min(elements.length, DROP_DOWN_MAX_ITEMS); i++) {
                var element = elements[i];
                var labelProvider = (ILabelProvider) viewer.getLabelProvider();
                var name = labelProvider.getText(element);
                var image = labelProvider.getImage(element);

                manager.add(new Action(name, ImageDescriptor.createFromImage(image)) {
                    @Override
                    public void run() {
                        openElement(element);
                    }
                });
            }

            if (elements.length > DROP_DOWN_MAX_ITEMS) {
                manager.add(new EmptyAction(NLS.bind("... {0} more", elements.length - DROP_DOWN_MAX_ITEMS)));
            }
        });
    }

    @Override
    public void dispose() {
        super.dispose();
        container.dispose();
        menuManager.dispose();
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
        textComposite.setToolTipText(toolTipText);
        elementText.setToolTipText(toolTipText);
        elementImage.setToolTipText(toolTipText);
    }

    public void setTrailing(boolean trailing) {
        ((GridData) container.getLayoutData()).grabExcessHorizontalSpace = trailing;
    }

    public boolean isShowText() {
        return showText;
    }

    public void setShowText(boolean showText) {
        if (this.showText == showText) {
            return;
        }
        this.showText = showText;
        UIUtils.setControlVisible(textComposite, showText);
        if (showText) {
            detailComposite.setTabList(new Control[]{textComposite});
        } else {
            detailComposite.setTabList(new Control[]{imageComposite});
        }
    }

    public int computeWidth() {
        return container.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
    }

    private void setArrowVisible(boolean visible) {
        UIUtils.setControlVisible(elementArrow, visible);
    }

    private void showMenu() {
        Point location = detailComposite.toDisplay(0, 0);

        if (CommonUtils.isBitSet(viewer.getStyle(), SWT.TOP)) {
            // Adjust the location so the menu won't cover the viewer
            location.y += detailComposite.getSize().y;
        }

        Menu menu = menuManager.createContextMenu(container);
        menu.setLocation(location);
        menu.setVisible(true);
    }

    private void openElement(@NotNull Object element) {
        viewer.fireMenuSelection(element);
    }

    private void addElementListener(@NotNull Control control) {
        control.addMenuDetectListener(e -> showMenu());
        control.addMouseListener(new DoubleClickMouseAdapter() {
            @Override
            public void onMouseSingleClick(@NotNull MouseEvent e) {
                showMenu();
            }

            @Override
            public void onMouseDoubleClick(@NotNull MouseEvent e) {
                BreadcrumbViewer viewer = getViewer();
                viewer.selectItem(BreadcrumbItem.this);
                viewer.fireDoubleClick();
            }
        });
    }
}
