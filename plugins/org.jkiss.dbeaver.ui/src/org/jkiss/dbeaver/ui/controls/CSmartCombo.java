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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.css.CSSUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Image combo
 */
public class CSmartCombo<ITEM_TYPE> extends Composite {

    private static final int IMAGE_TEXT_SPACING = 3;
    private static final int POPUP_BORDER_WIDTH = 1;
    private static final boolean ANIMATION_ENABLED = true;
    private static final int POPUP_ANIMATION_DURATION = 100;
    private static final int POPUP_ANIMATION_FRAME = 16;

    protected final ILabelProvider labelProvider;
    protected final List<ITEM_TYPE> items = new ArrayList<>();
    private ITEM_TYPE selectedItem;
    private final Label imageLabel;
    private final StyledText text;
    private Table dropDownControl;
    private Color dropDownBackground;
    private int visibleItemCount = 10;
    private Composite popup;
    private Composite closingPopup;
    private int popupAnimation;
    private Label arrow;
    private boolean hasFocus;
    private boolean backgroundInitialized;
    private boolean customBackground;
    private boolean forwardingKeyEvent;
    private final Listener listener;
    private final Listener filter;
    private final Listener popupFilter;

    public CSmartCombo(@NotNull Composite parent, int style, @NotNull ILabelProvider labelProvider) {
        super(parent, checkStyle(style));
        this.labelProvider = labelProvider;
        if (parent.getLayout() instanceof GridLayout) {
            this.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        }
        if ((style & SWT.BORDER) != 0) {
            new CompositeBorderPainter(this);
        }

        GridLayout gridLayout = new GridLayout(3, false);
        gridLayout.marginHeight = 2;
        gridLayout.marginWidth = 2;
        gridLayout.marginTop = 0;
        gridLayout.marginBottom = 0;
        gridLayout.horizontalSpacing = 0;
        gridLayout.verticalSpacing = 0;
        this.setLayout(gridLayout);
        this.setBackgroundMode(SWT.INHERIT_FORCE);

        this.imageLabel = new Label(this, SWT.NONE);
        this.imageLabel.setLayoutData(new GridData(GridData.FILL_VERTICAL | GridData.HORIZONTAL_ALIGN_BEGINNING));

        this.text = new StyledText(this, SWT.SINGLE | SWT.READ_ONLY |
            (style & (SWT.LEFT_TO_RIGHT | SWT.RIGHT_TO_LEFT)));
        this.text.setEditable(false);
        Caret caret = new Caret(this.text, SWT.NONE);
        caret.setSize(0, 0);
        this.text.setCaret(caret);
        this.text.addListener(SWT.Selection, event ->
            this.text.setSelection(this.text.getCaretOffset()));
        this.text.addListener(SWT.Paint, event -> {
            if (this.hasFocus && this.text.isEnabled()) {
                Rectangle clientArea = this.text.getClientArea();
                event.gc.drawFocus(0, 0, clientArea.width - 1, clientArea.height - 1);
            }
        });
        GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
        this.text.setLayoutData(gd);

        this.arrow = new Label(this, SWT.NONE);
        this.arrow.setImage(DBeaverIcons.getImage(UIIcon.TREE_COLLAPSE));
        gd = new GridData(GridData.FILL_VERTICAL | GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_CENTER);
        this.arrow.setLayoutData(gd);

        Cursor arrowCursor = getDisplay().getSystemCursor(SWT.CURSOR_ARROW);
        this.setCursor(arrowCursor);
        this.text.setCursor(arrowCursor);
        this.setEnabled(true, true);
        this.setForeground(UIStyles.getDefaultTextForeground());

        this.listener = event -> {
            if (isDisposed()) {
                return;
            }
            if (CSmartCombo.this.text == event.widget) {
                textEvent(event);
                return;
            }
            if (CSmartCombo.this.imageLabel == event.widget) {
                textEvent(event);
                return;
            }
            if (CSmartCombo.this.dropDownControl == event.widget) {
                listEvent(event);
                return;
            }
            if (CSmartCombo.this.arrow == event.widget) {
                arrowEvent(event);
                return;
            }
            if (CSmartCombo.this == event.widget) {
                comboEvent(event);
                return;
            }
            if (getShell() == event.widget) {
                UIUtils.asyncExec(() -> {
                    if (!isDisposed()) {
                        Shell activeShell = getDisplay().getActiveShell();
                        if (activeShell != getShell()) {
                            dropDown(false);
                            handleFocus(SWT.FocusOut);
                        }
                    }
                });
            }
        };
        this.filter = event -> {
            Control control = (Control) event.widget;
            if (control.getShell() == CSmartCombo.this.getShell() && !isOwnControl(control)) {
                UIUtils.asyncExec(() -> {
                    if (!isDisposed()) {
                        handleFocus(SWT.FocusOut);
                    }
                });
            }
        };
        this.popupFilter = event -> {
            if (event.widget instanceof Control control &&
                !UIUtils.isParent(CSmartCombo.this, control) &&
                !UIUtils.isParent(CSmartCombo.this.popup, control)) {
                dropDown(false);
            }
        };

        int[] comboEvents = {
            SWT.Dispose, SWT.Move, SWT.Resize, SWT.FocusIn, SWT.FocusOut, SWT.MouseDown, SWT.MouseUp
        };
        for (int comboEvent : comboEvents) {
            this.addListener(comboEvent, this.listener);
        }

        int[] textEvents = {SWT.KeyDown, SWT.KeyUp, SWT.MouseDown, SWT.MouseUp, SWT.Traverse, SWT.FocusIn};
        for (int textEvent : textEvents) {
            this.text.addListener(textEvent, this.listener);
            this.imageLabel.addListener(textEvent, this.listener);
        }

        int[] arrowEvents = {SWT.MouseDown, SWT.MouseUp, SWT.FocusIn};
        for (int arrowEvent : arrowEvents) {
            this.arrow.addListener(arrowEvent, this.listener);
        }

        UIUtils.asyncExec(() -> {
            if (!isDisposed()) {
                layout(true, true);
            }
        });
    }

    private boolean isOwnControl(Control control) {
        return control == this || UIUtils.isParent(this, control) ||
            this.popup != null && !this.popup.isDisposed() && UIUtils.isParent(this.popup, control);
    }

    private void setEnabled(boolean enabled, boolean force) {
        if (force || enabled != isEnabled()) {
            super.setEnabled(enabled);
            imageLabel.setEnabled(enabled);
            text.setEnabled(enabled);
            if (enabled) {
                updateBackground();
            }
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        setEnabled(enabled, false);
    }

    @Override
    public void setForeground(Color foreground) {
        super.setForeground(foreground);
        this.imageLabel.setForeground(foreground);
        this.text.setForeground(foreground);
        this.arrow.setForeground(foreground);
    }

    @Override
    public void setBackground(Color background) {
        if (background == getBackground()) {
            return;
        }
        super.setBackground(background);
        this.imageLabel.setBackground(background);
        this.text.setBackground(background);
        this.arrow.setBackground(background);
    }

    /**
     * Adds element
     */
    public void addItem(@Nullable ITEM_TYPE element) {
        items.add(element);
        if (items.size() == 1) {
            select(0);
        }
    }

    public void addItem(@Nullable ITEM_TYPE parent, @Nullable ITEM_TYPE element) {
        items.add(element);
        if (items.size() == 1) {
            select(0);
        }
    }

    @NotNull
    public ITEM_TYPE getItem(int index) {
        return items.get(index);
    }

    public void addModifyListener(@NotNull ModifyListener listener) {
        checkWidget();
        addListener(SWT.Modify, event -> listener.modifyText(new ModifyEvent(event)));
    }

    public void addSelectionListener(@NotNull SelectionListener listener) {
        checkWidget();
        addListener(SWT.Selection, event -> listener.widgetSelected(new SelectionEvent(event)));
        addListener(SWT.DefaultSelection, event -> listener.widgetDefaultSelected(new SelectionEvent(event)));
    }

    public void addVerifyListener(@NotNull VerifyListener listener) {
        checkWidget();
        addListener(SWT.Verify, event -> listener.verifyText(new VerifyEvent(event)));
    }

    private static int checkStyle(int style) {
        int mask = SWT.READ_ONLY | SWT.FLAT | SWT.LEFT_TO_RIGHT | SWT.RIGHT_TO_LEFT | SWT.CHECK;
        return SWT.NO_FOCUS | (style & mask);
    }

    @Override
    public boolean setFocus() {
        checkWidget();
        if (!isEnabled() || !getVisible()) {
            return false;
        }
        return this.text.setFocus();
    }

    @Override
    public boolean isFocusControl() {
        checkWidget();
        return this.text.isFocusControl() ||
            this.dropDownControl != null && !this.dropDownControl.isDisposed() && this.dropDownControl.isFocusControl() ||
            super.isFocusControl();
    }

    @Override
    public boolean traverse(int traversal) {
        if (traversal == SWT.TRAVERSE_ARROW_NEXT || traversal == SWT.TRAVERSE_TAB_NEXT) {
            return this.text.traverse(traversal);
        }
        return super.traverse(traversal);
    }

    public String getItemText(int index) {
        return labelProvider.getText(this.items.get(index));
    }

    public int getItemCount() {
        return this.items.size();
    }

    @NotNull
    public List<ITEM_TYPE> getItems() {
        return items;
    }

    @Nullable
    public ITEM_TYPE getSelectedItem() {
        return selectedItem;
    }

    public int getSelectionIndex() {
        return this.items.indexOf(this.selectedItem);
    }

    public String getText() {
        return this.labelProvider.getText(this.selectedItem);
    }

    public void remove(int index) {
        checkWidget();
        if (index < 0) {
            selectedItem = null;
            this.items.clear();
            this.select(-1);
        } else {
            if (selectedItem == items.get(index)) {
                if (index < items.size() - 1) {
                    selectedItem = items.get(index + 1);
                } else if (index > 0) {
                    selectedItem = items.get(index - 1);
                } else {
                    selectedItem = null;
                }
            }
            this.items.remove(index);
            this.select(getSelectionIndex());
        }
    }

    public void remove(ITEM_TYPE item) {
        remove(this.items.indexOf(item));
    }

    public void removeAll() {
        this.remove(-1);
    }

    public void select(int index) {
        checkWidget();

        String itemText;
        Image itemImage = null;
        if (index < 0) {
            selectedItem = null;
            itemText = "";
        } else {
            selectedItem = this.items.get(index);
            itemText = labelProvider.getText(selectedItem);
            try {
                itemImage = labelProvider.getImage(selectedItem);
            } catch (Exception e) {
                // No image
            }
        }
        if (this.dropDownControl != null && !this.dropDownControl.isDisposed()) {
            if (index < 0) {
                this.dropDownControl.deselectAll();
            } else if (this.dropDownControl.getSelectionIndex() != index) {
                this.dropDownControl.setSelection(index);
                this.dropDownControl.showSelection();
            }
        }
        this.text.setText(itemText);
        if (this.imageLabel.getImage() != itemImage) {
            this.imageLabel.setImage(itemImage);
            ((GridData) this.text.getLayoutData()).horizontalIndent = itemImage == null ? 0 : IMAGE_TEXT_SPACING;
            this.imageLabel.getParent().layout(true, true);
        }
        updateBackground();
    }

    private void updateBackground() {
        Color background = null;
        if (selectedItem != null && labelProvider instanceof IColorProvider cp) {
            background = cp.getBackground(selectedItem);
        }
        if (background != null) {
            setBackground(background);
            customBackground = true;
            backgroundInitialized = true;
        } else if (!backgroundInitialized || customBackground) {
            setBackground(null);
            CSSUtils.applyStyles(this);
            if (!UIStyles.isDarkTheme()) {
                setBackground(getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
            }
            customBackground = false;
            backgroundInitialized = true;
        }
    }

    public void select(ITEM_TYPE item) {
        select(this.items.indexOf(item));
    }

    @Override
    public void setFont(Font font) {
        checkWidget();
        super.setFont(font);
        this.text.setFont(font);
    }

    public void setText(String string) {
        checkWidget();
        if (string == null) {
            string = "";
        }
        for (int i = 0; i < this.items.size(); i++) {
            if (this.labelProvider.getText(items.get(i)).equals(string)) {
                select(i);
                break;
            }
        }
    }

    @Override
    public void setToolTipText(String string) {
        checkWidget();
        super.setToolTipText(string);
        this.arrow.setToolTipText(string);
        this.imageLabel.setToolTipText(string);
        this.text.setToolTipText(string);
    }

    public void setVisibleItemCount(int count) {
        checkWidget();
        if (count < 0) {
            return;
        }
        this.visibleItemCount = count;
    }

    private void handleFocus(int type) {
        if (isDisposed()) {
            return;
        }
        switch (type) {
            case SWT.FocusIn: {
                if (this.hasFocus) {
                    return;
                }
                this.hasFocus = true;
                this.text.redraw();
                Shell shell = getShell();
                shell.removeListener(SWT.Deactivate, this.listener);
                shell.addListener(SWT.Deactivate, this.listener);
                Display display = getDisplay();
                display.removeFilter(SWT.FocusIn, this.filter);
                display.addFilter(SWT.FocusIn, this.filter);
                Event e = new Event();
                notifyListeners(SWT.FocusIn, e);
                break;
            }
            case SWT.FocusOut: {
                if (!this.hasFocus) {
                    return;
                }
                if (isDropped()) {
                    return;
                }
                Control focusControl = getDisplay().getFocusControl();
                if (focusControl == this.arrow || focusControl == this.dropDownControl ||
                    focusControl == this.text || focusControl == this) {
                    return;
                }
                this.hasFocus = false;
                this.text.redraw();
                Shell shell = getShell();
                shell.removeListener(SWT.Deactivate, this.listener);
                Display display = getDisplay();
                display.removeFilter(SWT.FocusIn, this.filter);
                Event e = new Event();
                notifyListeners(SWT.FocusOut, e);
                break;
            }
        }
    }

    private void createPopup() {
        if (this.closingPopup != null && !this.closingPopup.isDisposed()) {
            this.closingPopup.dispose();
            this.closingPopup = null;
        }
        Composite oldPopup = this.popup;
        if (oldPopup != null) {
            oldPopup.dispose();
        }

        // create shell and list
        this.popup = new Composite(getShell(), SWT.NO_FOCUS);
        this.popup.setVisible(false);
        int style = getStyle();
        int listStyle = SWT.SINGLE | SWT.FULL_SELECTION;
        if (items.size() > visibleItemCount) {
            listStyle |= SWT.V_SCROLL;
        } else {
            listStyle |= SWT.NO_SCROLL;
        }
        if ((style & SWT.FLAT) != 0) {
            listStyle |= SWT.FLAT;
        }
        if ((style & SWT.RIGHT_TO_LEFT) != 0) {
            listStyle |= SWT.RIGHT_TO_LEFT;
        }
        if ((style & SWT.LEFT_TO_RIGHT) != 0) {
            listStyle |= SWT.LEFT_TO_RIGHT;
        }
        if ((style & SWT.CHECK) != 0) {
            listStyle |= SWT.CHECK;
        }
        GridLayout gl = new GridLayout(1, true);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        gl.verticalSpacing = 0;
        gl.horizontalSpacing = 0;
        this.popup.setLayout(gl);

        Composite border = new Composite(this.popup, SWT.NONE);
        border.setLayoutData(new GridData(GridData.FILL_BOTH));
        GridLayout borderLayout = new GridLayout(1, true);
        borderLayout.marginHeight = POPUP_BORDER_WIDTH;
        borderLayout.marginWidth = POPUP_BORDER_WIDTH;
        borderLayout.verticalSpacing = 0;
        borderLayout.horizontalSpacing = 0;
        border.setLayout(borderLayout);

        // create a table instead of a list.
        Table table = new Table(border, listStyle);
        table.setLayoutData(new GridData(GridData.FILL_BOTH));
        this.dropDownControl = table;
        CSSUtils.applyStyles(this.popup);
        this.dropDownBackground = UIStyles.isDarkTheme()
            ? this.popup.getBackground()
            : getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
        table.setBackground(this.dropDownBackground);
        border.addListener(SWT.Paint, event -> {
            Rectangle clientArea = border.getClientArea();
            event.gc.setForeground(UIUtils.getSharedTextColors().getColor(UIUtils.blend(
                table.getForeground().getRGB(), this.dropDownBackground.getRGB(), 20)));
            event.gc.drawRectangle(0, 0, clientArea.width - 1, clientArea.height - 1);
        });
        new TableColumn(table, SWT.LEFT);
        createTableItems(table);
        table.addListener(SWT.EraseItem, event -> {
            if ((event.detail & SWT.SELECTED) == 0 && event.item instanceof TableItem item) {
                event.gc.setBackground(item.getBackground());
                event.gc.fillRectangle(0, event.y, table.getClientArea().width, event.height);
                event.detail &= ~SWT.BACKGROUND;
            }
        });

        updateListListeners(table, true);
    }

    private void updateListListeners(Table table, boolean add) {
        int[] events = {SWT.MouseUp, SWT.Selection, SWT.Traverse, SWT.KeyDown, SWT.KeyUp, SWT.FocusIn, SWT.Resize};
        for (int event : events) {
            if (add) {
                table.addListener(event, this.listener);
            } else {
                table.removeListener(event, this.listener);
            }
        }
    }

    private void updateTableItems() {
        Table table = dropDownControl;
        table.removeAll();
        createTableItems(table);
    }

    private void createTableItems(Table table) {
        for (ITEM_TYPE item : this.items) {
            String itemText = labelProvider.getText(item);
            Image itemImage = labelProvider.getImage(item);
            Color itemBackground = null, itemForeground = null;
            if (labelProvider instanceof IColorProvider) {
                itemBackground = ((IColorProvider) labelProvider).getBackground(item);
                itemForeground = ((IColorProvider) labelProvider).getForeground(item);
            }
            if (itemBackground != null && itemForeground == null) {
                itemForeground = UIStyles.getContrastColor(itemBackground);
            } else if (itemBackground == null) {
                itemBackground = table.getBackground();
                if (itemForeground == null) {
                    itemForeground = table.getForeground();
                }
            }
            TableItem newItem = new TableItem(table, SWT.NONE);
            newItem.setData(item);
            newItem.setText(itemText);
            newItem.setImage(itemImage);
            newItem.setBackground(itemBackground);
            newItem.setForeground(itemForeground);
            if (item == selectedItem) {
                table.setSelection(newItem);
            }
        }
    }

    private boolean isDropped() {
        return this.popup != null && this.popup.getVisible();
    }

    protected void dropDown(boolean drop) {
        if (drop == isDropped()) {
            return;
        }
        if (!drop) {
            if (this.popup != null) {
                boolean restoreFocus = this.dropDownControl != null && this.dropDownControl.isFocusControl();
                final Composite toDispose = this.popup;
                Rectangle popupBounds = toDispose.getBounds();
                Rectangle comboBounds = getDisplay().map(getParent(), getShell(), getBounds());
                boolean opensUpward = popupBounds.y < comboBounds.y;
                updateListListeners(this.dropDownControl, false);
                this.popup = null;
                this.dropDownControl = null;
                this.closingPopup = toDispose;
                getDisplay().removeFilter(SWT.MouseDown, this.popupFilter);
                if (restoreFocus) {
                    setFocus();
                }
                animatePopup(toDispose, popupBounds, false, opensUpward, () -> {
                    if (this.closingPopup == toDispose) {
                        this.closingPopup = null;
                    }
                    if (!toDispose.isDisposed()) {
                        toDispose.dispose();
                    }
                });
            }
            return;
        }
        createPopup();
        Shell shell = getShell();
        shell.removeListener(SWT.Deactivate, this.listener);
        shell.addListener(SWT.Deactivate, this.listener);

        Point size = getSize();
        int itemCount = this.items.size();
        itemCount = (itemCount == 0) ? this.visibleItemCount : Math.min(this.visibleItemCount, itemCount);
        Table table = dropDownControl;
        int itemHeight = table.getItemHeight() * itemCount;
        Point listSize = table.computeSize(SWT.DEFAULT, itemHeight, false);
        listSize.y = table.computeTrim(0, 0, 0, itemHeight).height + POPUP_BORDER_WIDTH * 2;
        ScrollBar verticalBar = table.getVerticalBar();
        if (verticalBar != null) {
            listSize.x -= verticalBar.getSize().x;
        }
        table.setBounds(1, 1, Math.max(size.x, listSize.x) - 30, listSize.y);

        if (selectedItem != null) {
            for (TableItem item : table.getItems()) {
                if (item.getData() == selectedItem) {
                    table.showItem(item);
                    break;
                }
            }
        }
        Display display = getDisplay();
        Rectangle listRect = this.dropDownControl.getBounds();
        Rectangle parentRect = display.map(getParent(), null, getBounds());
        Point comboSize = getSize();
        Rectangle displayRect = display.map(shell, null, shell.getClientArea());
        int width = comboSize.x;
        int height = listRect.height;
        int x = parentRect.x;
        int y = parentRect.y + comboSize.y;
        if (y + height > displayRect.y + displayRect.height) {
            y = parentRect.y - height;
        }
        Point popupLocation = display.map(null, shell, new Point(x, y));
        Rectangle popupBounds = new Rectangle(popupLocation.x, popupLocation.y, width, height);
        this.popup.setBounds(popupBounds);
        this.popup.layout(true, true);

        {
            final TableColumn column = table.getColumn(0);
            column.pack();
            final int maxSize = table.getClientArea().width - 1;
            if (column.getWidth() < maxSize) {
                column.setWidth(maxSize);
            }
        }

        this.popup.moveAbove(null);
        this.popup.setVisible(true);
        table.setBackground(this.dropDownBackground);
        getDisplay().addFilter(SWT.MouseDown, this.popupFilter);
        Composite openingPopup = this.popup;
        animatePopup(openingPopup, popupBounds, true, y < parentRect.y, () -> {
            if (this.popup == openingPopup && !table.isDisposed()) {
                table.setFocus();
            }
        });
    }

    private void animatePopup(
        Composite control,
        Rectangle bounds,
        boolean opening,
        boolean opensUpward,
        @Nullable Runnable completion
    ) {
        if (!ANIMATION_ENABLED) {
            control.setBounds(bounds);
            control.layout(true, true);
            if (completion != null) {
                completion.run();
            }
            return;
        }
        int animation = ++this.popupAnimation;
        long startTime = System.currentTimeMillis();
        int bottom = bounds.y + bounds.height;
        Runnable step = new Runnable() {
            @Override
            public void run() {
                if (control.isDisposed() || animation != popupAnimation) {
                    return;
                }
                double progress = Math.min(
                    1.0,
                    (double) (System.currentTimeMillis() - startTime) / POPUP_ANIMATION_DURATION
                );
                double easedProgress = opening
                    ? 1.0 - Math.pow(1.0 - progress, 3)
                    : progress * progress;
                int height = Math.max(1, (int) Math.round(bounds.height *
                    (opening ? easedProgress : 1.0 - easedProgress)));
                int y = opensUpward ? bottom - height : bounds.y;
                control.setBounds(bounds.x, y, bounds.width, height);
                control.layout(true, true);
                if (progress < 1.0) {
                    control.getDisplay().timerExec(POPUP_ANIMATION_FRAME, this);
                } else {
                    if (opening) {
                        control.setBounds(bounds);
                        control.layout(true, true);
                    }
                    if (completion != null) {
                        completion.run();
                    }
                }
            }
        };
        step.run();
    }

    private void listEvent(Event event) {
        switch (event.type) {
            case SWT.FocusIn: {
                handleFocus(SWT.FocusIn);
                break;
            }
            case SWT.MouseUp: {
                if (event.button != 1) {
                    return;
                }
                TableItem item = this.dropDownControl.getItem(new Point(event.x, event.y));
                ITEM_TYPE selected = item == null ? null : (ITEM_TYPE) item.getData();
                boolean selectionChanged = item != null && selected != selectedItem;
                dropDown(false);
                if (selectionChanged) {
                    selectItem(selected, event);
                }
                break;
            }
            case SWT.Selection: {
                TableItem[] selection = this.dropDownControl.getSelection();
                if (selection.length > 0) {
                    selectItem((ITEM_TYPE) selection[0].getData(), event);
                }
                break;
            }
            case SWT.Traverse: {
                if (event.detail == SWT.TRAVERSE_TAB_NEXT || event.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
                    event.doit = this.text.traverse(event.detail);
                    event.detail = SWT.TRAVERSE_NONE;
                    if (event.doit) {
                        dropDown(false);
                    }
                    return;
                }
                switch (event.detail) {
                    case SWT.TRAVERSE_RETURN:
                    case SWT.TRAVERSE_ESCAPE:
                    case SWT.TRAVERSE_ARROW_PREVIOUS:
                    case SWT.TRAVERSE_ARROW_NEXT:
                        event.doit = false;
                        break;
                }
                Event e = new Event();
                e.time = event.time;
                e.detail = event.detail;
                e.doit = event.doit;
                e.character = event.character;
                e.keyCode = event.keyCode;
                notifyListeners(SWT.Traverse, e);
                event.doit = e.doit;
                event.detail = e.detail;
                break;
            }
            case SWT.KeyUp: {
                Event e = new Event();
                e.time = event.time;
                e.character = event.character;
                e.keyCode = event.keyCode;
                e.stateMask = event.stateMask;
                notifyListeners(SWT.KeyUp, e);
                break;
            }
            case SWT.KeyDown: {
                if (event.character == SWT.ESC || event.keyCode == SWT.ESC) {
                    // Escape key cancels popup list
                    event.doit = false;
                    dropDown(false);
                    break;
                }
                if ((event.stateMask & SWT.ALT) != 0
                    && (event.keyCode == SWT.ARROW_UP || event.keyCode == SWT.ARROW_DOWN)) {
                    dropDown(false);
                }
                if (event.character == SWT.CR) {
                    // Enter causes default selection
                    dropDown(false);
                    Event e = new Event();
                    e.time = event.time;
                    e.stateMask = event.stateMask;
                    notifyListeners(SWT.DefaultSelection, e);
                }
                // At this point the widget may have been disposed.
                // If so, do not continue.
                if (isDisposed()) {
                    break;
                }
                Event e = new Event();
                e.time = event.time;
                e.character = event.character;
                e.keyCode = event.keyCode;
                e.stateMask = event.stateMask;
                notifyListeners(SWT.KeyDown, e);
                break;

            }
            case SWT.Resize: {
                //table.pack();
                break;
            }
        }
    }

    private void selectItem(ITEM_TYPE item, Event event) {
        if (item == selectedItem) {
            return;
        }
        select(item);
        Event selectionEvent = new Event();
        selectionEvent.time = event.time;
        selectionEvent.stateMask = event.stateMask;
        selectionEvent.doit = event.doit;
        notifyListeners(SWT.Selection, selectionEvent);
        event.doit = selectionEvent.doit;
    }

    private void arrowEvent(Event event) {
        switch (event.type) {
            case SWT.FocusIn: {
                handleFocus(SWT.FocusIn);
                break;
            }
            case SWT.MouseDown: {
                setFocus();
                handleFocus(SWT.FocusIn);
                if (isDropped()) {
                    dropDown(false);
                } else {
                    dropDown(true);
                }
                break;
            }
        }
    }

    private void comboEvent(Event event) {
        switch (event.type) {
            case SWT.Dispose:
                removeListener(SWT.Dispose, listener);
                notifyListeners(SWT.Dispose, event);
                event.type = SWT.None;
                this.popupAnimation++;

                if (this.popup != null && !this.popup.isDisposed()) {
                    this.popup.dispose();
                }
                if (this.closingPopup != null && !this.closingPopup.isDisposed()) {
                    this.closingPopup.dispose();
                }
                Shell shell = getShell();
                shell.removeListener(SWT.Deactivate, this.listener);
                Display display = getDisplay();
                display.removeFilter(SWT.FocusIn, this.filter);
                display.removeFilter(SWT.MouseDown, this.popupFilter);
                this.popup = null;
                this.closingPopup = null;
                this.dropDownControl = null;
                this.arrow = null;
                break;
            case SWT.Move:
                dropDown(false);
                break;
            case SWT.FocusOut:
                this.text.setSelection(this.text.getCaretOffset());
                break;
            case SWT.FocusIn:
            case SWT.MouseDown:
            case SWT.MouseUp:
                textEvent(event);
                break;
            case SWT.KeyDown:
            case SWT.KeyUp:
                if (forwardingKeyEvent) {
                    break;
                }
                forwardingKeyEvent = true;
                try {
                    textEvent(event);
                } finally {
                    forwardingKeyEvent = false;
                }
                break;
        }
    }

    private void textEvent(Event event) {
        switch (event.type) {
            case SWT.FocusIn: {
                handleFocus(SWT.FocusIn);
                break;
            }
            case SWT.KeyDown: {
                if (event.character == SWT.ESC || event.keyCode == SWT.ESC) {
                    if (isDropped()) {
                        event.doit = false;
                        dropDown(false);
                    } else {
                        getShell().traverse(SWT.TRAVERSE_ESCAPE);
                    }
                    break;
                }
                if (event.character == SWT.CR) {
                    dropDown(false);
                    Event e = new Event();
                    e.time = event.time;
                    e.stateMask = event.stateMask;
                    notifyListeners(SWT.DefaultSelection, e);
                }
                //At this point the widget may have been disposed.
                // If so, do not continue.
                if (isDisposed()) {
                    break;
                }

                if (event.keyCode == SWT.ARROW_UP || event.keyCode == SWT.ARROW_DOWN ||
                    event.keyCode == SWT.PAGE_UP || event.keyCode == SWT.PAGE_DOWN ||
                    event.keyCode == SWT.HOME || event.keyCode == SWT.END) {
                    event.doit = false;
                    if ((event.stateMask & SWT.ALT) != 0 &&
                        (event.keyCode == SWT.ARROW_UP || event.keyCode == SWT.ARROW_DOWN)) {
                        boolean dropped = isDropped();
                        //this.text.selectAll();
                        if (!dropped) {
                            setFocus();
                        }
                        dropDown(!dropped);
                        break;
                    }

                    int oldIndex = getSelectionIndex();
                    int lastIndex = getItemCount() - 1;
                    if (lastIndex < 0) {
                        break;
                    }
                    int pageSize = this.dropDownControl == null
                        ? this.visibleItemCount
                        : Math.max(1, this.dropDownControl.getClientArea().height / this.dropDownControl.getItemHeight());
                    int newIndex = switch (event.keyCode) {
                        case SWT.ARROW_UP -> Math.max(oldIndex - 1, 0);
                        case SWT.ARROW_DOWN -> Math.min(oldIndex + 1, lastIndex);
                        case SWT.PAGE_UP -> Math.max(oldIndex - pageSize, 0);
                        case SWT.PAGE_DOWN -> Math.min(oldIndex + pageSize, lastIndex);
                        case SWT.HOME -> 0;
                        case SWT.END -> lastIndex;
                        default -> oldIndex;
                    };
                    if (oldIndex != newIndex) {
                        select(newIndex);
                        Event e = new Event();
                        e.time = event.time;
                        e.stateMask = event.stateMask;
                        notifyListeners(SWT.Selection, e);
                    }
                    //At this point the widget may have been disposed.
                    // If so, do not continue.
                    if (isDisposed()) {
                        break;
                    }
                }

                // Further work : Need to add support for incremental search in 
                // pop up list as characters typed in text widget

                Event e = new Event();
                e.time = event.time;
                e.character = event.character;
                e.keyCode = event.keyCode;
                e.stateMask = event.stateMask;
                notifyListeners(SWT.KeyDown, e);
                break;
            }
            case SWT.KeyUp: {
                Event e = new Event();
                e.time = event.time;
                e.character = event.character;
                e.keyCode = event.keyCode;
                e.stateMask = event.stateMask;
                notifyListeners(SWT.KeyUp, e);
                break;
            }
            case SWT.Modify: {
                Event e = new Event();
                e.time = event.time;
                notifyListeners(SWT.Modify, e);
                break;
            }
            case SWT.MouseDown: {
                if (event.button != 1) {
                    return;
                }
                setFocus();
                handleFocus(SWT.FocusIn);
                event.doit = false;
                boolean dropped = isDropped();
                //this.text.selectAll();
                if (dropped) {
                    dropDown(false);
                } else {
                    dropDown(true);
                }
                break;
            }
            case SWT.MouseUp: {
                if (event.button != 1) {
                    return;
                }
                break;
            }
            case SWT.Traverse: {
                switch (event.detail) {
                    case SWT.TRAVERSE_RETURN:
                    case SWT.TRAVERSE_ARROW_PREVIOUS:
                    case SWT.TRAVERSE_ARROW_NEXT:
                        // The enter causes default selection and
                        // the arrow keys are used to manipulate the list contents so
                        // do not use them for traversal.
                        event.doit = false;
                        break;
                }

                Event e = new Event();
                e.time = event.time;
                e.detail = event.detail;
                e.doit = event.doit;
                e.character = event.character;
                e.keyCode = event.keyCode;
                notifyListeners(SWT.Traverse, e);
                event.doit = e.doit;
                event.detail = e.detail;
                break;
            }
        }
    }

}
