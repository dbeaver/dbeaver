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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
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

        ScrolledComposite viewport = UIUtils.createScrolledComposite(composite, SWT.V_SCROLL);
        viewport.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).hint(700, 500).create());

        Composite container = new Composite(viewport, SWT.NONE);
        container.setLayout(GridLayoutFactory.fillDefaults().spacing(0, 0).create());
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        UIUtils.configureScrolledComposite(viewport, container);

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

        Map<Rectangle, List<ImageLocation>> categories = images.stream()
            .sorted(Comparator.comparing(ImageLocation::path))
            .collect(Collectors.groupingBy(image -> image.image().getBounds())).entrySet().stream()
            .sorted(Map.Entry.<Rectangle, List<ImageLocation>> comparingByValue(Comparator.comparingInt(List::size)).reversed())
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

    private static void createCategory(@NotNull Composite parent, @NotNull Rectangle bounds, @NotNull List<ImageLocation> images) {
        Composite header = new Composite(parent, SWT.NONE);
        header.setLayout(new GridLayout(2, false));
        header.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        UIUtils.createLabel(header, "%s x %s (%s)".formatted(bounds.width, bounds.height, images.size()));
        UIUtils.createLabelSeparator(header, SWT.HORIZONTAL);

        Composite group = new Composite(parent, SWT.NONE);
        group.setLayout(new RowLayout());
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        for (ImageLocation image : images) {
            Label label = new Label(group, SWT.NONE);
            label.setImage(image.image());
            label.setToolTipText("%s - %s".formatted(image.bundle().getSymbolicName(), image.path()));
            label.addMouseListener(MouseListener.mouseUpAdapter(e -> {
                try {
                    var url = FileLocator.toFileURL(image.url());
                    ShellUtils.showInSystemExplorer(new File(url.toURI()));
                } catch (Exception ex) {
                    log.error("Error accessing icon " + image.url(), ex);
                }
            }));
        }
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

    private record ImageLocation(Bundle bundle, String path, URL url, Image image) {
    }
}
