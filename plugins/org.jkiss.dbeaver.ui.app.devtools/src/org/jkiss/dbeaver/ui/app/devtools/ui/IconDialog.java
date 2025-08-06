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
package org.jkiss.dbeaver.ui.app.devtools.ui;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.app.devtools.handlers.ShowIconsHandler;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class IconDialog extends TrayDialog {
    private static final Log log = Log.getLog(ShowIconsHandler.class);

    private boolean showBorders = true;
    private final List<ImagePanel> panels = new ArrayList<>();
    private final Set<String> hiddenExtensions = new HashSet<>();

    public IconDialog(@NotNull Shell shell) {
        super(shell);
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Icons");
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);

        List<ImageLocation> images = new ArrayList<>();
        collectIcons((bundle, path) -> {
            if (path.contains("@2x")) {
                // Skip @2x variations for now
                return;
            }
            try {
                var url = bundle.getEntry(path);
                var image = ImageDescriptor.createFromURL(url).createImage(true);
                images.add(new ImageLocation(bundle, path, url, image));
            } catch (SWTException e) {
                log.debug("Failed to create image for " + bundle.getSymbolicName() + " - " + path + ": " + e.getMessage());
            }
        });

        Button showBordersCheck = new Button(composite, SWT.CHECK);
        showBordersCheck.setSelection(showBorders);
        showBordersCheck.setText("Show borders");
        showBordersCheck.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            showBorders = showBordersCheck.getSelection();
            panels.forEach(Control::redraw);
        }));

        Map<String, Integer> extensions = images.stream()
            .collect(Collectors.groupingBy(
                ImageLocation::extension,
                Collectors.summingInt(x -> 1)
            ));

        extensions.forEach((extension, count) -> {
            Button extensionCheck = new Button(composite, SWT.CHECK);
            extensionCheck.setSelection(true);
            extensionCheck.setText("%s (%d)".formatted(extension.toUpperCase(), count));
            extensionCheck.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
                if (extensionCheck.getSelection()) {
                    hiddenExtensions.remove(extension);
                } else {
                    hiddenExtensions.add(extension);
                }

                panels.forEach(Control::redraw);
            }));
        });

        ScrolledComposite viewport = UIUtils.createScrolledComposite(composite, SWT.V_SCROLL);
        viewport.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).hint(700, 500).create());

        Composite container = new Composite(viewport, SWT.NONE);
        container.setLayout(GridLayoutFactory.fillDefaults().spacing(0, 0).create());
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        UIUtils.configureScrolledComposite(viewport, container);

        Map<Rectangle, List<ImageLocation>> categories = images.stream()
            .sorted(Comparator.comparing(ImageLocation::filename))
            .collect(Collectors.groupingBy(image -> image.image().getBounds())).entrySet().stream()
            .sorted(Comparator
                .comparingInt((Map.Entry<Rectangle, List<ImageLocation>> e) -> e.getValue().size()) // by count
                .thenComparingInt(e -> e.getKey().width * e.getKey().height) // by density
                .reversed())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));

        for (Map.Entry<Rectangle, List<ImageLocation>> entry : categories.entrySet()) {
            createCategory(container, entry.getKey(), entry.getValue());
        }

        viewport.addDisposeListener(e -> {
            for (ImageLocation location : images) {
                location.image().dispose();
            }
        });

        return composite;
    }

    private void createCategory(@NotNull Composite parent, @NotNull Rectangle bounds, @NotNull List<ImageLocation> images) {
        Composite header = new Composite(parent, SWT.NONE);
        header.setLayout(new GridLayout(2, false));
        header.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        UIUtils.createLabel(header, "%s x %s (%s)".formatted(bounds.width, bounds.height, images.size()));
        UIUtils.createLabelSeparator(header, SWT.HORIZONTAL);

        ImagePanel panel = new ImagePanel(parent, images, new Point(bounds.width, bounds.height));
        panel.setLayoutData(new GridData(GridData.FILL_BOTH));

        panels.add(panel);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    private void collectIcons(@NotNull BiConsumer<Bundle, String> consumer) {
        BundleContext context = FrameworkUtil.getBundle(getClass()).getBundleContext();
        for (Bundle bundle : context.getBundles()) {
            String name = bundle.getSymbolicName();
            if (!name.startsWith("org.jkiss.dbeaver") && !name.startsWith("com.dbeaver")) {
                continue;
            }
            collectIcons(bundle, path -> consumer.accept(bundle, path));
        }
    }

    private static void collectIcons(@NotNull Bundle bundle, @NotNull Consumer<String> consumer) {
        collectIcons(bundle, "icons", consumer);
    }

    private static void collectIcons(@NotNull Bundle bundle, @NotNull String root, @NotNull Consumer<String> consumer) {
        Enumeration<String> paths = bundle.getEntryPaths(root);
        if (paths == null) {
            return;
        }
        while (paths.hasMoreElements()) {
            String path = paths.nextElement();
            if (path.endsWith("/")) {
                collectIcons(bundle, path, consumer);
            } else {
                consumer.accept(path);
            }
        }
    }

    private class ImagePanel extends Composite {
        private static final int SPACING = 5;

        private final List<ImageLocation> images;
        private final Point size;

        public ImagePanel(@NotNull Composite parent, @NotNull List<ImageLocation> images, @NotNull Point size) {
            super(parent, SWT.NONE);
            this.images = images;
            this.size = size;

            addPaintListener(e -> {
                e.gc.setForeground(e.display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
                e.gc.fillRectangle(0, 0, e.width, e.height);
                e.gc.setAdvanced(true);

                Rectangle area = getClientArea();
                int columns = getColumns(area.width, size.x);
                int x = 0;
                int y = 0;
                boolean transparent = false;

                for (int i = 0; i < images.size(); i++) {
                    if (i > 0 && i % columns == 0) {
                        x = 0;
                        y += size.y + SPACING;
                    }
                    var image = images.get(i);
                    var nowTransparent = hiddenExtensions.contains(image.extension());
                    if (transparent != nowTransparent) {
                        transparent = nowTransparent;
                        e.gc.setAlpha(nowTransparent ? 30 : 255);
                    }
                    if (showBorders) {
                        e.gc.drawRectangle(x, y, size.x + 1, size.y + 1);
                    }
                    e.gc.drawImage(image.image(), x + 1, y + 1);
                    x += size.x + SPACING;
                }
            });

            addMouseListener(MouseListener.mouseUpAdapter(e -> {
                if (e.button != 1) {
                    return;
                }
                ImageLocation image = getImageAt(e.x, e.y);
                if (image == null) {
                    return;
                }
                try {
                    var url = FileLocator.toFileURL(image.url());
                    ShellUtils.showInSystemExplorer(new File(url.toURI()));
                } catch (Exception ex) {
                    log.error("Error accessing icon " + image.url(), ex);
                }
            }));

            addMouseMoveListener(e -> {
                ImageLocation image = getImageAt(e.x, e.y);
                if (image == null) {
                    setToolTipText(null);
                } else {
                    setToolTipText("%s - %s".formatted(image.bundle().getSymbolicName(), image.path()));
                }
            });
        }

        @Override
        public Point computeSize(int wHint, int hHint, boolean changed) {
            if (wHint == SWT.DEFAULT) {
                return new Point(images.size() * (size.x + SPACING), size.y);
            } else {
                int columns = getColumns(wHint, size.x);
                int rows = Math.max(1, (images.size() + columns - 1) / columns);
                return new Point(columns * (size.x + SPACING), rows * (size.y + SPACING));
            }
        }

        @Nullable
        private ImageLocation getImageAt(int x, int y) {
            Rectangle area = getClientArea();
            if (x < 0 || y < 0 || x >= area.width || y >= area.height) {
                return null;
            }

            int column = x / (size.x + SPACING);
            int row = y / (size.y + SPACING);
            int index = row * getColumns(area.width, size.x) + column;
            if (index < 0 || index >= images.size()) {
                return null;
            }

            return images.get(index);
        }

        private int getColumns(int width, int size) {
            return Math.max(1, width / (size + SPACING));
        }
    }

    private record ImageLocation(Bundle bundle, String path, String filename, String extension, URL url, Image image) {
        private ImageLocation(Bundle bundle, String path, URL url, Image image) {
            this(
                bundle,
                path,
                path.substring(path.lastIndexOf('/') + 1),
                path.substring(path.lastIndexOf('.') + 1),
                url,
                image
            );
        }
    }
}
