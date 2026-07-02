/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.Pair;

import java.util.List;

public class ColorsDialog extends TrayDialog {
    private static final List<Pair<String, Integer>> SWT_COLORS = List.of(
        new Pair<>("SWT.COLOR_WHITE", SWT.COLOR_WHITE),
        new Pair<>("SWT.COLOR_BLACK", SWT.COLOR_BLACK),
        new Pair<>("SWT.COLOR_RED", SWT.COLOR_RED),
        new Pair<>("SWT.COLOR_DARK_RED", SWT.COLOR_DARK_RED),
        new Pair<>("SWT.COLOR_GREEN", SWT.COLOR_GREEN),
        new Pair<>("SWT.COLOR_DARK_GREEN", SWT.COLOR_DARK_GREEN),
        new Pair<>("SWT.COLOR_YELLOW", SWT.COLOR_YELLOW),
        new Pair<>("SWT.COLOR_DARK_YELLOW", SWT.COLOR_DARK_YELLOW),
        new Pair<>("SWT.COLOR_BLUE", SWT.COLOR_BLUE),
        new Pair<>("SWT.COLOR_DARK_BLUE", SWT.COLOR_DARK_BLUE),
        new Pair<>("SWT.COLOR_MAGENTA", SWT.COLOR_MAGENTA),
        new Pair<>("SWT.COLOR_DARK_MAGENTA", SWT.COLOR_DARK_MAGENTA),
        new Pair<>("SWT.COLOR_CYAN", SWT.COLOR_CYAN),
        new Pair<>("SWT.COLOR_DARK_CYAN", SWT.COLOR_DARK_CYAN),
        new Pair<>("SWT.COLOR_GRAY", SWT.COLOR_GRAY),
        new Pair<>("SWT.COLOR_DARK_GRAY", SWT.COLOR_DARK_GRAY),
        new Pair<>("SWT.COLOR_WIDGET_DARK_SHADOW", SWT.COLOR_WIDGET_DARK_SHADOW),
        new Pair<>("SWT.COLOR_WIDGET_NORMAL_SHADOW", SWT.COLOR_WIDGET_NORMAL_SHADOW),
        new Pair<>("SWT.COLOR_WIDGET_LIGHT_SHADOW", SWT.COLOR_WIDGET_LIGHT_SHADOW),
        new Pair<>("SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW", SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW),
        new Pair<>("SWT.COLOR_WIDGET_FOREGROUND", SWT.COLOR_WIDGET_FOREGROUND),
        new Pair<>("SWT.COLOR_WIDGET_BACKGROUND", SWT.COLOR_WIDGET_BACKGROUND),
        new Pair<>("SWT.COLOR_WIDGET_BORDER", SWT.COLOR_WIDGET_BORDER),
        new Pair<>("SWT.COLOR_LIST_FOREGROUND", SWT.COLOR_LIST_FOREGROUND),
        new Pair<>("SWT.COLOR_LIST_BACKGROUND", SWT.COLOR_LIST_BACKGROUND),
        new Pair<>("SWT.COLOR_LIST_SELECTION", SWT.COLOR_LIST_SELECTION),
        new Pair<>("SWT.COLOR_LIST_SELECTION_TEXT", SWT.COLOR_LIST_SELECTION_TEXT),
        new Pair<>("SWT.COLOR_INFO_FOREGROUND", SWT.COLOR_INFO_FOREGROUND),
        new Pair<>("SWT.COLOR_INFO_BACKGROUND", SWT.COLOR_INFO_BACKGROUND),
        new Pair<>("SWT.COLOR_TITLE_FOREGROUND", SWT.COLOR_TITLE_FOREGROUND),
        new Pair<>("SWT.COLOR_TITLE_BACKGROUND", SWT.COLOR_TITLE_BACKGROUND),
        new Pair<>("SWT.COLOR_TITLE_BACKGROUND_GRADIENT", SWT.COLOR_TITLE_BACKGROUND_GRADIENT),
        new Pair<>("SWT.COLOR_TITLE_INACTIVE_FOREGROUND", SWT.COLOR_TITLE_INACTIVE_FOREGROUND),
        new Pair<>("SWT.COLOR_TITLE_INACTIVE_BACKGROUND", SWT.COLOR_TITLE_INACTIVE_BACKGROUND),
        new Pair<>("SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT", SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT),
        new Pair<>("SWT.COLOR_LINK_FOREGROUND", SWT.COLOR_LINK_FOREGROUND),
        new Pair<>("SWT.COLOR_TRANSPARENT", SWT.COLOR_TRANSPARENT),
        new Pair<>("SWT.COLOR_TEXT_DISABLED_BACKGROUND", SWT.COLOR_TEXT_DISABLED_BACKGROUND),
        new Pair<>("SWT.COLOR_WIDGET_DISABLED_FOREGROUND", SWT.COLOR_WIDGET_DISABLED_FOREGROUND)
    );

    public ColorsDialog(@NotNull Shell shell) {
        super(shell);
    }

    @Override
    protected void configureShell(@NotNull Shell shell) {
        super.configureShell(shell);
        shell.setText("Colors");
    }

    @NotNull
    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);

        ScrolledComposite viewport = UIUtils.createScrolledComposite(composite, SWT.V_SCROLL);
        viewport.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).hint(450, 550).create());

        Composite container = new Composite(viewport, SWT.NONE);
        container.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).create());
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        for (Pair<String, Integer> pair : SWT_COLORS) {
            Label label = new Label(container, SWT.NONE);
            label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            label.setText(pair.getFirst());

            new ColoredComposite(container, composite.getDisplay().getSystemColor(pair.getSecond()), SWT.BORDER);
        }

        UIUtils.configureScrolledComposite(viewport, container);

        return composite;
    }

    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    private static class ColoredComposite extends Composite {
        public ColoredComposite(@NotNull Composite parent, @NotNull Color color, int style) {
            super(parent, style);
            addPaintListener(e -> {
                e.gc.setBackground(color);
                e.gc.fillRectangle(e.x, e.y, e.width, e.height);
            });
        }

        @NotNull
        @Override
        public Point computeSize(int wHint, int hHint, boolean changed) {
            return new Point(76, 24);
        }
    }
}
