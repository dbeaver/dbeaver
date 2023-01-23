/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.DBeaverIcons;

import java.util.function.Consumer;

public class TextWithDropDown extends Composite {
    private final Text text;
    private final Menu menu;
    private final SelectionListener menuListener;

    public TextWithDropDown(@NotNull Composite parent, int style, int textStyle, @Nullable SelectionListener menuListener) {
        super(parent, style);

        setLayout(GridLayoutFactory.fillDefaults().numColumns(2).create());
        setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));

        this.text = new Text(this, textStyle);
        this.text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        this.menu = new Menu(parent.getShell());
        this.menuListener = menuListener;

        new Button(this, SWT.ARROW | SWT.DOWN).addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                final Control item = (Control) e.widget;
                final Rectangle rect = item.getBounds();
                final Point pt = item.getParent().toDisplay(new Point(rect.x, rect.y));
                menu.setLocation(pt.x, pt.y + rect.height);
                menu.setVisible(true);
            }
        });

        addDisposeListener(event -> menu.dispose());
    }

    @NotNull
    public MenuItem addMenuItem(@NotNull String text) {
        return addMenuItem(menu, text, null, null, SWT.NONE);
    }

    @NotNull
    public MenuItem addMenuItem(@NotNull Menu menu, @NotNull String text, @Nullable DBIcon image, @Nullable Object data, int style) {
        final MenuItem item = new MenuItem(menu, style);
        item.setText(text);
        item.setData(data);
        if (image != null) {
            item.setImage(DBeaverIcons.getImage(image));
        }
        if (menuListener != null) {
            item.addSelectionListener(menuListener);
        }
        return item;
    }

    @NotNull
    public MenuItem addMenuItemWithMenu(@NotNull String text, @Nullable DBIcon image, @NotNull Consumer<Menu> contributor) {
        return addMenuItemWithMenu(menu, text, image, contributor);
    }

    @NotNull
    public MenuItem addMenuItemWithMenu(@NotNull Menu parentMenu, @NotNull String text, @Nullable DBIcon image, @NotNull Consumer<Menu> menuContributor) {
        final MenuItem menuItem = addMenuItem(parentMenu, text, image, null, SWT.CASCADE);
        final Menu menu = new Menu(parentMenu);
        menuContributor.accept(menu);
        menuItem.setMenu(menu);
        return menuItem;
    }

    public void addMenuSeparator() {
        new MenuItem(menu, SWT.SEPARATOR);
    }

    @NotNull
    public Menu getMenuComponent() {
        return menu;
    }

    @NotNull
    public Text getTextComponent() {
        return text;
    }
}
