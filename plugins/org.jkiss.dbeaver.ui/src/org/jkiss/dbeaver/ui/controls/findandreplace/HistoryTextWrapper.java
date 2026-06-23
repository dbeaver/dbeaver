/*******************************************************************************
 * Copyright (c) 2024 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vector Informatik GmbH - initial API and implementation
 *******************************************************************************/
package org.jkiss.dbeaver.ui.controls.findandreplace;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.internal.findandreplace.FindReplaceMessages;
import org.eclipse.ui.internal.findandreplace.HistoryStore;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;

import java.util.function.Consumer;

/**
 * Wrap a Text Bar and a ToolItem to add an input history. The text is only
 * stored to history when requested by the Client code, the history is stored in
 * a {@code HistoryStore} provided by the client. The history bar behaves like a
 * normal {@code Text}.
 * <p>
 * Derived from <a href="https://github.com/eclipse-platform/eclipse.platform.ui/blob/master/bundles/org.eclipse.ui.workbench.texteditor/src/org/eclipse/ui/internal/findandreplace/overlay/HistoryTextWrapper.java">eclipse.platform.ui</a>
 */
public class HistoryTextWrapper extends Composite {
    @NotNull
    private final Text textBar;
    @NotNull
    private final AccessibleToolBar tools;
    @NotNull
    private final ToolItem dropDown;
    @NotNull
    private final HistoryStore history;
    @Nullable
    private SearchHistoryMenu menu;

    public HistoryTextWrapper(@NotNull HistoryStore history, @NotNull Composite parent, int style) {
        super(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(this);

        this.history = history;

        this.textBar = new Text(this, style);
        GridDataFactory.fillDefaults().grab(true, true).align(GridData.FILL, GridData.CENTER).applyTo(this.textBar);
        this.tools = new AccessibleToolBar(this);
        this.dropDown = new AccessibleToolItemBuilder(this.tools).withStyleBits(SWT.PUSH)
            .withToolTipText(FindReplaceMessages.FindReplaceOverlay_searchHistory_toolTip)
            .withImage(DBeaverIcons.getImage(UIIcon.FIND_REPLACE_OPEN_HISTORY))
            .withOperation(this::createHistoryMenuDropdown)
            .build();

        this.listenForKeyboardHistoryNavigation();
    }

    private static boolean okayToUse(@Nullable final Widget widget) {
        return widget != null && !widget.isDisposed();
    }

    public void storeHistory() {
        String string = this.textBar.getText();
        this.history.remove(string); // ensure findString is now on the newest index of the history
        this.history.add(string);
    }

    public void selectAll() {
        this.textBar.selectAll();
    }

    public void addModifyListener(@NotNull final ModifyListener listener) {
        this.textBar.addModifyListener(listener);
    }

    @Override
    public void addFocusListener(@NotNull final FocusListener listener) {
        this.textBar.addFocusListener(listener);
    }

    @Override
    public void addKeyListener(@NotNull final KeyListener listener) {
        this.textBar.addKeyListener(listener);
    }

    public void setMessage(@Nullable final String message) {
        this.textBar.setMessage(message);
    }

    public void setSelection(int i, int j) {
        this.textBar.setSelection(i, j);
    }

    @Nullable
    public String getSelectionText() {
        return this.textBar.getSelectionText();
    }

    @Override
    public boolean isFocusControl() {
        return this.textBar.isFocusControl();
    }

    @NotNull
    public String getText() {
        return this.textBar.getText();
    }

    public void setText(@Nullable String str) {
        this.textBar.setText(str);
    }

    @Override
    public void setBackground(@Nullable Color color) {
        this.textBar.setBackground(color);
    }

    public void setWidgetBackground(@Nullable Color color) {
        super.setBackground(color);

        this.tools.setBackground(color);
        this.dropDown.setBackground(color);
    }

    @Override
    public void setForeground(@Nullable Color color) {
        super.setForeground(color);

        this.textBar.setForeground(color);
        this.tools.setForeground(color);
    }

    @Override
    public boolean forceFocus() {
        if (!this.textBar.isDisposed()) {
            return this.textBar.forceFocus();
        }
        return false;
    }

    @Override
    public void notifyListeners(int eventType, @NotNull Event event) {
        super.notifyListeners(eventType, event);
        this.textBar.notifyListeners(eventType, event);
    }

    @NotNull
    public Text getTextBar() {
        return this.textBar;
    }

    @NotNull
    AccessibleToolBar getDropDownTool() {
        return this.tools;
    }

    private void listenForKeyboardHistoryNavigation() {
        this.addKeyListener(KeyListener.keyPressedAdapter(e -> {
            if (e.keyCode == SWT.ARROW_UP || e.keyCode == SWT.ARROW_DOWN) {
                int stepDirection = e.keyCode == SWT.ARROW_UP ? -1 : 1;
                this.navigateInHistory(stepDirection);
            }
        }));
    }

    private void createHistoryMenuDropdown() {
        if (this.menu != null && okayToUse(this.menu.getShell()) || !this.dropDown.isEnabled()) {
            return;
        }

        this.dropDown.setEnabled(false);
        Consumer<String> textUpdaterOnHistoryEntrySelection = selectedHistoryItem -> {
            if (selectedHistoryItem != null) {
                this.textBar.setText(selectedHistoryItem);
            }
        };

        this.menu = new SearchHistoryMenu(getShell(), this.history, textUpdaterOnHistoryEntrySelection);

        Point barPosition = this.textBar.toDisplay(0, 0);
        Rectangle dropDownBounds = this.dropDown.getBounds();
        this.menu.setPosition(barPosition.x, barPosition.y + dropDownBounds.height, this.textBar.getSize().x + dropDownBounds.width);
        this.menu.open();

        this.menu.getShell().addDisposeListener(e -> getShell().getDisplay().timerExec(100, HistoryTextWrapper.this::enableDropDown));
    }

    private void enableDropDown() {
        if (!this.dropDown.isDisposed()) {
            this.dropDown.setEnabled(true);
        }
    }

    private void navigateInHistory(int navigationOffset) {
        if (this.history.isEmpty()) {
            return;
        }

        int offset = this.history.indexOf(this.textBar.getText());

        offset += navigationOffset;
        offset = offset % this.history.size();

        if (offset < 0) {
            offset = this.history.size() - 1;
        }

        this.textBar.setText(this.history.get(offset));
    }

}
