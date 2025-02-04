/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.test.ui;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.test.handlers.ShowIconsHandler;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;
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

        Map<Rectangle, List<DBPImage>> icons = collectIcons().stream()
            .collect(Collectors.groupingBy(icon -> DBeaverIcons.getImage(icon).getBounds()))
            .entrySet().stream()
            .sorted(Map.Entry.<Rectangle, List<DBPImage>>comparingByValue(Comparator.comparingInt(List::size)).reversed())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));

        icons.forEach((bounds, images) -> {
            Composite header = new Composite(container, SWT.NONE);
            header.setLayout(new GridLayout(2, false));
            header.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            UIUtils.createLabel(header, "%s x %s (%s)".formatted(bounds.width, bounds.height, images.size()));
            UIUtils.createLabelSeparator(header, SWT.HORIZONTAL);

            Composite group = new Composite(container, SWT.NONE);
            group.setLayout(new RowLayout());
            group.setLayoutData(new GridData(GridData.FILL_BOTH));

            for (DBPImage image : images) {
                Label label = new Label(group, SWT.NONE);
                label.setImage(DBeaverIcons.getImage(image));
                label.setToolTipText(image.getLocation());
                label.addMouseListener(MouseListener.mouseUpAdapter(e -> {
                    try {
                        var url = FileLocator.toFileURL(new URL(image.getLocation()));
                        ShellUtils.showInSystemExplorer(new File(url.toURI()));
                    } catch (Exception ex) {
                        log.error("Error accessing icon " + image.getLocation(), ex);
                    }
                }));
            }
        });

        return container;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @NotNull
    private static Collection<DBPImage> collectIcons() {
        List<DBPImage> icons = new ArrayList<>();
        List<Class<?>> classes = List.of(DBIcon.class, UIIcon.class);

        for (Class<?> cls : classes) {
            for (Field field : cls.getFields()) {
                if (Modifier.isStatic(field.getModifiers()) && DBPImage.class.isAssignableFrom(field.getType())) {
                    try {
                        icons.add((DBPImage) field.get(null));
                    } catch (IllegalAccessException e) {
                        log.error("Error accessing icon " + field.getDeclaringClass().getSimpleName() + '.' + field.getName(), e);
                    }
                }
            }
        }

        return icons;
    }
}
