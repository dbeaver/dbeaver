/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Image combo
 */
public class CSmartCombo<ITEM_TYPE> extends Composite {

    public interface TableFilter<FILTER_ITEM_TYPE> {
        String getFilterLabel();
        String getDefaultLabel();
        boolean isEnabled();
        boolean setEnabled(boolean enabled);
        boolean filter(FILTER_ITEM_TYPE item);
    }

    protected final ILabelProvider labelProvider;
    protected final List<ITEM_TYPE> items = new ArrayList<>();
    private TableFilter<ITEM_TYPE> tableFilter = null;
    private ITEM_TYPE selectedItem;
    private Label imageLabel;
    private StyledText text;
    private Tree dropDownControl;
    private int visibleItemCount = 10;
    private int widthHint = SWT.DEFAULT;
    private Shell popup;
    private long disposeTime = -1;
    private Button arrow;
    private boolean hasFocus;
    private Listener listener, filter;
    private Font font;
    private Point sizeHint;
    // Size of Text with border. Needed for calc of height in computeSize
    // On MacOS edit with border is much higher
    private final Point borderTextSize;

    public CSmartCombo(Composite parent, int style, ILabelProvider labelProvider)
    {
        super(parent, style = checkStyle(style));
        this.labelProvider = labelProvider;
        if (parent.getLayout() instanceof GridLayout) {
            this.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        }

        GridLayout gridLayout = new GridLayout(3, false);
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        gridLayout.marginTop = 0;
        gridLayout.marginBottom = 0;
        gridLayout.horizontalSpacing = 0;
        gridLayout.verticalSpacing = 0;
        this.setLayout(gridLayout);

        int heightHint = 0;
        {
            Text sampleText = new Text(this, SWT.NONE);
            sampleText.setText("X");
            borderTextSize = sampleText.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            heightHint = borderTextSize.y + 2;
            sampleText.dispose();
        }

        this.imageLabel = new Label(this, SWT.NONE);
        this.imageLabel.setLayoutData(new GridData(GridData.FILL_VERTICAL | GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_CENTER));

        this.text = new StyledText(this, SWT.SINGLE | SWT.READ_ONLY);
        this.text.setIndent(3);
        this.text.setCaret(null);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
        gd.heightHint = heightHint;
        this.text.setLayoutData(gd);

        this.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_ARROW));

        int arrowStyle = SWT.ARROW | SWT.DOWN;
        if ((style & SWT.FLAT) != 0) {
            arrowStyle |= SWT.FLAT;
        }
        this.arrow = new Button(this, arrowStyle);
        gd = new GridData(GridData.FILL_VERTICAL | GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_CENTER);
        gd.heightHint = heightHint;
        this.arrow.setLayoutData(gd);

        this.setEnabled(true, true);
        this.setForeground(UIStyles.getDefaultTextForeground());

        this.listener = event -> {
            if (isDisposed()) {
                return;
            }
            if (CSmartCombo.this.popup == event.widget) {
                popupEvent(event);
                return;
            }
            if (CSmartCombo.this.text == event.widget) {
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
                handleFocus(SWT.FocusOut);
            }
        };
        this.filter = event -> {
            Shell shell = ((Control) event.widget).getShell();
            if (shell == CSmartCombo.this.getShell()) {
                handleFocus(SWT.FocusOut);
            }
        };

        int[] comboEvents = {SWT.Dispose, SWT.Move, SWT.Resize};
        for (int comboEvent : comboEvents) {
            this.addListener(comboEvent, this.listener);
        }

        int[] textEvents = {SWT.KeyDown, SWT.KeyUp, SWT.Modify, SWT.MouseDown, SWT.MouseUp, SWT.Traverse, SWT.FocusIn};
        for (int textEvent : textEvents) {
            this.text.addListener(textEvent, this.listener);
        }

        int[] arrowEvents = {SWT.Selection, SWT.FocusIn};
        for (int arrowEvent : arrowEvents) {
            this.arrow.addListener(arrowEvent, this.listener);
        }
    }

    public void setWidthHint(int widthHint)
    {
        this.widthHint = widthHint;
    }

    public void setTableFilter(TableFilter<ITEM_TYPE> tableFilter) {
        this.tableFilter = tableFilter;
    }

    private void setEnabled(boolean enabled, boolean force)
    {
        if (force || enabled != isEnabled()) {
            super.setEnabled(enabled);
            imageLabel.setEnabled(enabled);
            text.setEnabled(enabled);

            if (!enabled) {
                this.setBackground(getParent().getBackground());
            } else {
                if (selectedItem != null && labelProvider instanceof IColorProvider) {
                    this.setBackground(((IColorProvider) labelProvider).getBackground(selectedItem));
                }
            }
        }
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        setEnabled(enabled, false);
    }

    @Override
    public void setForeground(Color foreground)
    {
        super.setForeground(foreground);
        this.imageLabel.setForeground(foreground);
        this.text.setForeground(foreground);
        this.arrow.setForeground(foreground);
    }

    @Override
    public void setBackground(Color background)
    {
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
    public void addItem(@Nullable ITEM_TYPE element)
    {
        items.add(element);
        if (items.size() == 1) {
            select(0);
        }
    }

    public void addItem(@Nullable ITEM_TYPE parent, @Nullable ITEM_TYPE element)
    {
        items.add(element);
        if (items.size() == 1) {
            select(0);
        }
    }

    public ITEM_TYPE getItem(int index)
    {
        return items.get(index);
    }

    public void addModifyListener(final ModifyListener listener)
    {
        checkWidget();
        addListener(SWT.Modify, event -> listener.modifyText(new ModifyEvent(event)));
    }

    public void addSelectionListener(final SelectionListener listener)
    {
        checkWidget();
        addListener(SWT.Selection, event -> listener.widgetSelected(new SelectionEvent(event)));
        addListener(SWT.DefaultSelection, event -> listener.widgetDefaultSelected(new SelectionEvent(event)));
    }

    public void addVerifyListener(final VerifyListener listener)
    {
        checkWidget();
        addListener(SWT.Verify, event -> listener.verifyText(new VerifyEvent(event)));
    }

    private static int checkStyle(int style)
    {
        int mask = SWT.BORDER | SWT.READ_ONLY | SWT.FLAT | SWT.LEFT_TO_RIGHT | SWT.RIGHT_TO_LEFT | SWT.CHECK;
        return style & mask;
    }

    @Override
    public Point computeSize(int wHint, int hHint, boolean changed)
    {
        if (wHint != SWT.DEFAULT) {
            return super.computeSize(wHint, hHint, changed);
        }
        checkWidget();

        int borderWidth = getBorderWidth ();
        Point arrowSize = arrow.computeSize (SWT.DEFAULT, SWT.DEFAULT, changed);
        arrowSize.x += 2 * borderWidth;
        arrowSize.y += 2 * borderWidth;

        Point textSize = super.computeSize(SWT.DEFAULT, SWT.DEFAULT, changed);
        Point listSize = new Point(0, 0);
        try {
            GC gc = new GC(getDisplay());
            for (ITEM_TYPE item : items) {
                String itemText = labelProvider.getText(item);
                Point point = gc.stringExtent(itemText);
                listSize.x = Math.max(listSize.x, point.x);
                listSize.y = Math.max(listSize.y, point.y);
            }
            gc.dispose();
        } catch (Throwable e) {
            // ignore. Something is wrong with GC? #4539
        }
        listSize.x += imageLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT, changed).x;
        listSize.x += arrowSize.x;
        listSize.x += 20;

        int height = Math.max(hHint, textSize.y);
        int width = Math.max(wHint, Math.max(listSize.x, textSize.x));
        if (widthHint != SWT.DEFAULT) {
            width = widthHint;
        }

        height = Math.max (height, arrowSize.y);
        height = Math.max (height, borderTextSize.y);

        return new Point (width, height);
    }

    public String getItemText(int index)
    {
        return labelProvider.getText(this.items.get(index));
    }

    public int getItemCount()
    {
        return this.items.size();
    }

    public List<ITEM_TYPE> getItems()
    {
        return items;
    }

    public ITEM_TYPE getSelectedItem() {
        return selectedItem;
    }

    public int getSelectionIndex()
    {
        return this.items.indexOf(this.selectedItem);
    }

    public String getText()
    {
        return this.labelProvider.getText(this.selectedItem);
    }

    public void remove(int index)
    {
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

    public void remove(ITEM_TYPE item)
    {
        remove(this.items.indexOf(item));
    }

    public void removeAll()
    {
        this.remove(-1);
    }

    public void select(int index)
    {
        checkWidget();

        String itemText;
        Image itemImage = null;
        Color itemBackground = null;
        if (index < 0) {
            selectedItem = null;
            itemText = "";
            itemImage = null;
            itemBackground = null;
        } else {
            selectedItem = this.items.get(index);
            itemText = labelProvider.getText(selectedItem);
            try {
                itemImage = labelProvider.getImage(selectedItem);
            } catch (Exception e) {
                // No image
            }
            if (labelProvider instanceof IColorProvider) {
                itemBackground = ((IColorProvider) labelProvider).getBackground(selectedItem);
            }
        }
        this.text.setText(itemText);
        if (itemImage != null) {
            this.imageLabel.setImage(itemImage);
        }
        if (itemBackground == null) {
            itemBackground = UIStyles.getDefaultTextBackground();
        }
        this.setBackground(itemBackground);
    }

    public void select(ITEM_TYPE item)
    {
        select(this.items.indexOf(item));
    }

    @Override
    public void setFont(Font font)
    {
        checkWidget();
        super.setFont(font);
        this.font = font;
        this.text.setFont(font);
    }

    public void setText(String string)
    {
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
    public void setToolTipText(String string)
    {
        checkWidget();
        super.setToolTipText(string);
        this.arrow.setToolTipText(string);
        this.imageLabel.setToolTipText(string);
        this.text.setToolTipText(string);
    }

    public void setVisibleItemCount(int count)
    {
        checkWidget();
        if (count < 0) {
            return;
        }
        this.visibleItemCount = count;
    }

    private void handleFocus(int type)
    {
        if (isDisposed()) {
            return;
        }
        switch (type) {
            case SWT.FocusIn: {
                if (this.hasFocus) {
                    return;
                }
                this.hasFocus = true;
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
                Control focusControl = getDisplay().getFocusControl();
                if (focusControl == this.arrow || focusControl == this.dropDownControl || focusControl == this) {
                    return;
                }
                this.hasFocus = false;
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

    private void createPopup()
    {
        Shell oldPopup = this.popup;
        if (oldPopup != null) {
            oldPopup.dispose();
        }

        // create shell and list
        this.popup = new Shell(getShell(), SWT.RESIZE | SWT.ON_TOP);
        int style = getStyle();
        int listStyle = SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION;
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

        if (tableFilter != null) {
            final Button filterButton = new Button(this.popup, SWT.PUSH | SWT.FLAT | SWT.CENTER);
            filterButton.setText("Show " + (tableFilter.isEnabled() ? tableFilter.getDefaultLabel() : tableFilter.getFilterLabel()));
            filterButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            filterButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    tableFilter.setEnabled(!tableFilter.isEnabled());
                    filterButton.setText("Show " + (tableFilter.isEnabled() ? tableFilter.getDefaultLabel() : tableFilter.getFilterLabel()));
                    updateTableItems();
                }
            });
        }

        // create a table instead of a list.
        Tree table = new Tree(this.popup, listStyle);
        table.setLayoutData(new GridData(GridData.FILL_BOTH));
        this.dropDownControl = table;
        if (this.font != null) {
            table.setFont(this.font);
        }
        new TreeColumn(table, SWT.LEFT);
        createTableItems(table);

        int[] popupEvents = {SWT.Close, SWT.Paint, SWT.Deactivate};
        for (int popupEvent : popupEvents) {
            this.popup.addListener(popupEvent, this.listener);
        }
        int[] listEvents = {SWT.MouseUp, SWT.Selection, SWT.Traverse, SWT.KeyDown, SWT.KeyUp, SWT.FocusIn, SWT.Dispose, SWT.Resize};
        for (int listEvent : listEvents) {
            table.addListener(listEvent, this.listener);
        }
    }

    private void updateTableItems() {
        Tree table = dropDownControl;
        table.removeAll();
        createTableItems(table);
        table.setFocus();
    }

    private void createTableItems(Tree table) {
        TableFilter<ITEM_TYPE> filter = tableFilter != null && tableFilter.isEnabled() ? tableFilter : null;
        for (ITEM_TYPE item : this.items) {
            if (filter != null && !filter.filter(item)) {
                continue;
            }
            String itemText = labelProvider.getText(item);
            Image itemImage = labelProvider.getImage(item);
            Color itemBackground = null, itemForeground = null;
            if (labelProvider instanceof IColorProvider) {
                itemBackground = ((IColorProvider) labelProvider).getBackground(item);
                itemForeground = ((IColorProvider) labelProvider).getForeground(item);
            }
            TreeItem newItem = new TreeItem(table, SWT.NONE);
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

    private boolean isDropped()
    {
        return this.popup != null && this.popup.getVisible();
    }

    protected void dropDown(boolean drop)
    {
        if (drop == isDropped()) {
            return;
        }
        if (!drop) {
            if (this.popup != null) {
                final Shell toDispose = this.popup;
                this.popup = null;
                this.dropDownControl = null;
                disposeTime = System.currentTimeMillis();
                UIUtils.asyncExec(toDispose::dispose);
            }
            return;
        }
        if (this.dropDownControl != null) {
            this.dropDownControl.removeListener(SWT.Dispose, this.listener);
        }
        createPopup();

        Point size = getSize();
        int itemCount = this.items.size();
        itemCount = (itemCount == 0) ? this.visibleItemCount : Math.min(this.visibleItemCount, itemCount);
        Tree table = dropDownControl;
        int itemHeight = table.getItemHeight() * itemCount;
        Point listSize = table.computeSize(SWT.DEFAULT, itemHeight, false);
        if (tableFilter != null) {
            listSize.y += popup.getChildren()[0].computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
        }
        ScrollBar verticalBar = table.getVerticalBar();
        if (verticalBar != null) {
            listSize.x -= verticalBar.getSize().x;
        }
        ScrollBar hScrollBar = table.getHorizontalBar();
        if (hScrollBar != null) {
            listSize.y += hScrollBar.getSize().y;
        }
        table.setBounds(1, 1, Math.max(size.x, listSize.x) - 30, listSize.y);

        {
            final TreeColumn column = table.getColumn(0);
            column.pack();
            final int maxSize = table.getSize().x - 10;// - 2;//table.getVerticalBar().getSize().x;
            if (column.getWidth() < maxSize) {
                //column.setWidth(maxSize);
            }
        }

        if (selectedItem != null) {
            for (TreeItem item : table.getItems()) {
                if (item.getData() == selectedItem) {
                    table.showItem(item);
                    table.setTopItem(item);
                    break;
                }
            }
        }
        Display display = getDisplay();
        Rectangle listRect = this.dropDownControl.getBounds();
        Rectangle parentRect = display.map(getParent(), null, getBounds());
        Point comboSize = getSize();
        Rectangle displayRect = getMonitor().getClientArea();
        int width = comboSize.x;
        int height = listRect.height;
        if (sizeHint != null) {
            width = sizeHint.x;
            height = sizeHint.y;
        }
        int x = parentRect.x;
        int y = parentRect.y + comboSize.y;
        if (y + height > displayRect.y + displayRect.height) {
            y = parentRect.y - height;
        }
        this.popup.setBounds(x, y, width, height);
        this.popup.layout();

        if (this.popup.getData("resizeListener") == null) {
            this.popup.addListener(SWT.Resize, event -> CSmartCombo.this.sizeHint = popup.getSize());
            this.popup.setData("resizeListener", Boolean.TRUE);
        }
        this.popup.setVisible(true);
        this.dropDownControl.setFocus();
    }

    private void listEvent(Event event)
    {
        switch (event.type) {
            case SWT.Dispose:
                if (getShell() != this.popup.getParent()) {
                    int selectionIndex = this.getSelectionIndex();
                    this.popup = null;
                    this.dropDownControl = null;
                    createPopup();
                }
                break;
            case SWT.FocusIn: {
                handleFocus(SWT.FocusIn);
                break;
            }
            case SWT.MouseUp: {
                if (event.button != 1) {
                    return;
                }
                dropDown(false);
                break;
            }
            case SWT.Selection: {
                Tree table = this.dropDownControl;
                TreeItem[] selection = table.getSelection();
                if (ArrayUtils.isEmpty(selection)) {
                    return;
                }
                final TreeItem tableItem = selection[0];
                ITEM_TYPE item = (ITEM_TYPE) tableItem.getData();
                select(item);
                table.setSelection(tableItem);
                Event e = new Event();
                e.time = event.time;
                e.stateMask = event.stateMask;
                e.doit = event.doit;
                notifyListeners(SWT.Selection, e);
                event.doit = e.doit;
                break;
            }
            case SWT.Traverse: {
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
                if (event.character == SWT.ESC) {
                    // Escape key cancels popup list
                    dropDown(false);
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

    private void arrowEvent(Event event)
    {
        switch (event.type) {
            case SWT.FocusIn: {
                handleFocus(SWT.FocusIn);
                break;
            }
            case SWT.Selection: {
                if (!isDropped() && (System.currentTimeMillis() - disposeTime) > 200) {
                    dropDown(true);
                }
                break;
            }
        }
    }

    private void comboEvent(Event event)
    {
        switch (event.type) {
            case SWT.Dispose:
                removeListener(SWT.Dispose, listener);
                notifyListeners(SWT.Dispose, event);
                event.type = SWT.None;

                if (this.popup != null && !this.popup.isDisposed()) {
                    this.dropDownControl.removeListener(SWT.Dispose, this.listener);
                    this.popup.dispose();
                }
                Shell shell = getShell();
                shell.removeListener(SWT.Deactivate, this.listener);
                Display display = getDisplay();
                display.removeFilter(SWT.FocusIn, this.filter);
                this.popup = null;
                this.dropDownControl = null;
                this.arrow = null;
                break;
            case SWT.Move:
                dropDown(false);
                break;
        }
    }

    private void popupEvent(Event event)
    {
        switch (event.type) {
            case SWT.Paint:
                // draw black rectangle around list
                Rectangle listRect = this.dropDownControl.getBounds();
                Color black = getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND);
                event.gc.setForeground(black);
                event.gc.drawRectangle(0, 0, listRect.width + 1, listRect.height + 1);
                break;
            case SWT.Close:
                event.doit = false;
                dropDown(false);
                break;
            case SWT.Deactivate:
                dropDown(false);
                break;
        }
    }

    private void textEvent(Event event)
    {
        switch (event.type) {
            case SWT.FocusIn: {
                handleFocus(SWT.FocusIn);
                break;
            }
            case SWT.KeyDown: {
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

                if (event.keyCode == SWT.ARROW_UP || event.keyCode == SWT.ARROW_DOWN) {
                    event.doit = false;
                    if ((event.stateMask & SWT.ALT) != 0) {
                        boolean dropped = isDropped();
                        //this.text.selectAll();
                        if (!dropped) {
                            setFocus();
                        }
                        dropDown(!dropped);
                        break;
                    }

                    int oldIndex = getSelectionIndex();
                    if (event.keyCode == SWT.ARROW_UP) {
                        select(Math.max(oldIndex - 1, 0));
                    } else {
                        select(Math.min(oldIndex + 1, getItemCount() - 1));
                    }
                    if (oldIndex != getSelectionIndex()) {
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
                boolean dropped = isDropped();
                //this.text.selectAll();
                if (!dropped && (System.currentTimeMillis() - disposeTime) > 200) {
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
