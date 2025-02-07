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
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.DoubleClickMouseAdapter;
import org.jkiss.utils.CommonUtils;

final class BreadcrumbItem extends Item {
    private static final int DROP_DOWN_MIN_WIDTH = 250;
    private static final int DROP_DOWN_MAX_WIDTH = 500;
    private static final int DROP_DOWN_MIN_HEIGHT = 200;

    private final BreadcrumbViewer viewer;

    private final Composite container;
    private final Label elementArrow;
    private final Label elementImage;
    private final Label elementText;

    private ILabelProvider labelProvider;
    private ITreeContentProvider contentProvider;
    private ILabelProvider toolTipLabelProvider;

    private Shell menuShell;
    private TreeViewer menuViewer;

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
        menuShell = new Shell(container.getShell(), SWT.RESIZE | SWT.TOOL | SWT.ON_TOP);
        menuShell.setLayout(new FillLayout());

        Object input = getData();

        menuViewer = new TreeViewer(menuShell, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
        menuViewer.addOpenListener(e -> {
            if (e.getSelection() instanceof IStructuredSelection selection) {
                Object element = selection.getFirstElement();
                if (element != null) {
                    openElement(element);
                }
            }
        });
        menuViewer.getTree().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseUp(MouseEvent e) {
                if (e.button != 1 || (OpenStrategy.getOpenMethod() & OpenStrategy.SINGLE_CLICK) != 0) {
                    return;
                }
                Tree tree = (Tree) e.widget;
                Item item = tree.getItem(new Point(e.x, e.y));
                if (item != null) {
                    openElement(item.getData());
                }
            }
        });

        viewer.configureDropDownViewer(menuViewer, input);
        menuViewer.setInput(input);

        configureShellBounds(menuShell);
        configureShellCloser(menuShell);

        menuShell.setVisible(true);
        menuShell.setFocus();
    }

    private void openElement(@NotNull Object element) {
        viewer.fireMenuSelection(element);

        if (menuShell != null) {
            menuShell.dispose();
        }
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

    private void configureShellBounds(@NotNull Shell shell) {
        shell.pack();

        var window = UIUtils.getActiveWorkbenchWindow();
        var windowSize = window.getShell().getSize();

        var shellSize = shell.getSize();
        int width = CommonUtils.clamp(shellSize.x, DROP_DOWN_MIN_WIDTH, DROP_DOWN_MAX_WIDTH);
        int height = CommonUtils.clamp(shellSize.y, DROP_DOWN_MIN_HEIGHT, windowSize.y / 2);

        var itemBounds = container.getBounds();
        var trimBounds = shell.computeTrim(0, 0, width, height);

        var location = new Point(0, 0);
        location.x = trimBounds.x + elementArrow.getSize().x;
        location.y -= height + trimBounds.y + itemBounds.y;

        shell.setLocation(container.toDisplay(location));
        shell.setSize(width, height);
    }

    private void configureShellCloser(@NotNull Shell shell) {
        Listener focusListener = e -> {
            switch (e.type) {
                case SWT.FocusIn -> {
                    Widget focusElement = e.widget;
                    boolean isFocusBreadcrumbTreeFocusWidget = focusElement == shell || focusElement instanceof Tree tree && tree.getShell() == shell;
                    boolean isFocusWidgetParentShell = focusElement instanceof Control control && control.getShell().getParent() == shell;

                    if (!isFocusBreadcrumbTreeFocusWidget && !isFocusWidgetParentShell) {
                        shell.close();
                    }
                }
                case SWT.FocusOut -> {
                    if (e.display.getActiveShell() == null) {
                        shell.close();
                    }
                }
            }
        };

        Display display = shell.getDisplay();
        display.addFilter(SWT.FocusIn, focusListener);
        display.addFilter(SWT.FocusOut, focusListener);

        ControlListener controlListener = new ControlListener() {
            @Override
            public void controlMoved(ControlEvent e) {
                shell.close();
            }

            @Override
            public void controlResized(ControlEvent e) {
                shell.close();
            }
        };

        container.addControlListener(controlListener);

        shell.addDisposeListener(e -> {
            display.removeFilter(SWT.FocusIn, focusListener);
            display.removeFilter(SWT.FocusOut, focusListener);

            if (!container.isDisposed()) {
                container.removeControlListener(controlListener);
            }
        });
        shell.addShellListener(new ShellAdapter() {
            @Override
            public void shellClosed(ShellEvent e) {
                menuShell = null;
                menuViewer = null;
            }
        });
    }
}
