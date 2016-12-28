/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.ui.DBeaverIcons;

import java.util.Arrays;

/**
 * Instances of this class are controls that allow the user
 * to choose an item from a list of items, or optionally
 * enter a new value by typing it into an editable text
 * field. Often, <code>Combo</code>s are used in the same place
 * where a single selection <code>List</code> widget could
 * be used but space is limited. A <code>Combo</code> takes
 * less space than a <code>List</code> widget and shows
 * similar information.
 * <p>
 * Note: Since <code>Combo</code>s can contain both a list
 * and an editable text field, it is possible to confuse methods
 * which access one versus the other (compare for example,
 * <code>clearSelection()</code> and <code>deselectAll()</code>).
 * The API documentation is careful to indicate either "the
 * receiver's list" or the "the receiver's text field" to
 * distinguish between the two cases.
 * </p><p>
 * Note that although this class is a subclass of <code>Composite</code>,
 * it does not make sense to add children to it, or set a layout on it.
 * </p>
 * <dl>
 * <dt><b>Styles:</b></dt>
 * <dd>DROP_DOWN, READ_ONLY, SIMPLE</dd>
 * <dt><b>Events:</b></dt>
 * <dd>DefaultSelection, Modify, Selection, Verify</dd>
 * </dl>
 * <p>
 * Note: Only one of the styles DROP_DOWN and SIMPLE may be specified.
 * </p><p>
 * IMPORTANT: This class is <em>not</em> intended to be subclassed.
 * </p>
 *
 * @see List
 */

public class CImageCombo extends Composite {

    private Label imageLabel;
    private Text text;
    private Table table;
    private int visibleItemCount = 4;
    private int widthHint = SWT.DEFAULT;
    private Shell popup;
    private Button arrow;
    private boolean hasFocus;
    private Listener listener, filter;
    private Font font;
    private Point sizeHint;

    /**
     * Constructs a new instance of this class given its parent
     * and a style value describing its behavior and appearance.
     * <p>
     * The style value is either one of the style constants defined in
     * class <code>SWT</code> which is applicable to instances of this
     * class, or must be built by <em>bitwise OR</em>'ing together
     * (that is, using the <code>int</code> "|" operator) two or more
     * of those <code>SWT</code> style constants. The class description
     * lists the style constants that are applicable to the class.
     * Style bits are also inherited from superclasses.
     * </p>
     *
     * @param parent a composite control which will be the parent of the new instance (cannot be null)
     * @param style  the style of control to construct
     * @throws IllegalArgumentException <ul>
     *                                  <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
     *                                  </ul>
     * @throws SWTException             <ul>
     *                                  <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the parent</li>
     *                                  <li>ERROR_INVALID_SUBCLASS - if this class is not an allowed subclass</li>
     *                                  </ul>
     * @see SWT#DROP_DOWN
     * @see SWT#READ_ONLY
     * @see SWT#SIMPLE
     * @see Widget#checkSubclass
     * @see Widget#getStyle
     */
    public CImageCombo(Composite parent, int style)
    {
        super(parent, style = checkStyle(style));
        this.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_CENTER));

        GridLayout gridLayout = new GridLayout(3, false);
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        gridLayout.horizontalSpacing = 3;
        this.setLayout(gridLayout);

        this.imageLabel = new Label(this, SWT.NONE);
        this.imageLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_CENTER));

        this.text = new Text(this, SWT.READ_ONLY);
        this.text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER));

        this.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_ARROW));

        int arrowStyle = SWT.ARROW | SWT.DOWN;
        if ((style & SWT.FLAT) != 0) {
            arrowStyle |= SWT.FLAT;
        }
        this.arrow = new Button(this, arrowStyle);
        this.arrow.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_CENTER));

        setEnabled(true, true);

        this.listener = new Listener() {
            @Override
            public void handleEvent(Event event)
            {
                if (CImageCombo.this.popup == event.widget) {
                    popupEvent(event);
                    return;
                }
                if (CImageCombo.this.text == event.widget) {
                    textEvent(event);
                    return;
                }
                if (CImageCombo.this.table == event.widget) {
                    listEvent(event);
                    return;
                }
                if (CImageCombo.this.arrow == event.widget) {
                    arrowEvent(event);
                    return;
                }
                if (CImageCombo.this == event.widget) {
                    comboEvent(event);
                    return;
                }
                if (getShell() == event.widget) {
                    handleFocus(SWT.FocusOut);
                }
            }
        };
        this.filter = new Listener() {
            @Override
            public void handleEvent(Event event)
            {
                Shell shell = ((Control) event.widget).getShell();
                if (shell == CImageCombo.this.getShell()) {
                    handleFocus(SWT.FocusOut);
                }
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

        createPopup(-1);
        //initAccessible();
    }

    public void setWidthHint(int widthHint)
    {
        this.widthHint = widthHint;
    }

    private void setEnabled(boolean enabled, boolean force)
    {
        if (force || enabled != isEnabled()) {
            super.setEnabled(enabled);
            imageLabel.setEnabled(enabled);
            text.setEnabled(enabled);
            if (enabled && table != null) {
                if (getSelectionIndex() >= 0) {
                    select(getSelectionIndex());
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
        super.setBackground(background);
        this.imageLabel.setBackground(background);
        this.text.setBackground(background);
        this.arrow.setBackground(background);
    }

    /**
     * Adds the argument to the end of the receiver's list.
     *
     *
     * @param string the new item
     * @param background item background color
     * @throws IllegalArgumentException <ul>
     *                                  <li>ERROR_NULL_ARGUMENT - if the string is null</li>
     *                                  </ul>
     * @throws SWTException             <ul>
     *                                  <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     *                                  <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
     *                                  </ul>
     */
    public void add(@Nullable DBPImage icon, String string, @Nullable Color background, @Nullable Object data)
    {
        checkWidget();
        if (string == null) {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }
        Image image = icon == null ? null : DBeaverIcons.getImage(icon);
        TableItem newItem = new TableItem(this.table, SWT.NONE);
        newItem.setText(string);
        newItem.setData(data);
        if (image != null) {
            newItem.setImage(image);
            if (imageLabel.getImage() == null) {
                imageLabel.setImage(image);
            }
        }
        if (background != null) {
            newItem.setBackground(background);
        }
    }

    public Object getData(int index)
    {
        checkWidget();
        if (index < 0 || index >= table.getItemCount()) {
            SWT.error(SWT.ERROR_INVALID_ARGUMENT);
        }
        return table.getItem(index).getData();
    }

    /**
     * Adds the listener to the collection of listeners who will
     * be notified when the receiver's text is modified, by sending
     * it one of the messages defined in the <code>ModifyListener</code>
     * interface.
     *
     * @param listener the listener which should be notified
     * @throws IllegalArgumentException <ul>
     *                                  <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
     *                                  </ul>
     * @throws SWTException             <ul>
     *                                  <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     *                                  <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
     *                                  </ul>
     * @see ModifyListener
     */
    public void addModifyListener(final ModifyListener listener)
    {
        checkWidget();
        addListener(SWT.Modify, new Listener() {
            @Override
            public void handleEvent(Event event)
            {
                listener.modifyText(new ModifyEvent(event));
            }
        });
    }

    /**
     * Adds the listener to the collection of listeners who will
     * be notified when the user changes the receiver's selection, by sending
     * it one of the messages defined in the <code>SelectionListener</code>
     * interface.
     * <p>
     * <code>widgetSelected</code> is called when the user changes the combo's list selection.
     * <code>widgetDefaultSelected</code> is typically called when ENTER is pressed the combo's text area.
     * </p>
     *
     * @param listener the listener which should be notified
     * @throws IllegalArgumentException <ul>
     *                                  <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
     *                                  </ul>
     * @throws SWTException             <ul>
     *                                  <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     *                                  <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
     *                                  </ul>
     * @see SelectionListener
     * @see SelectionEvent
     */
    public void addSelectionListener(final SelectionListener listener)
    {
        checkWidget();
        addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                listener.widgetSelected(new SelectionEvent(event));
            }
        });
        addListener(SWT.DefaultSelection, new Listener() {
            @Override
            public void handleEvent(Event event)
            {
                listener.widgetDefaultSelected(new SelectionEvent(event));
            }
        });
    }

    /**
     * Adds the listener to the collection of listeners who will
     * be notified when the receiver's text is verified, by sending
     * it one of the messages defined in the <code>VerifyListener</code>
     * interface.
     *
     * @param listener the listener which should be notified
     * @throws IllegalArgumentException <ul>
     *                                  <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
     *                                  </ul>
     * @throws SWTException             <ul>
     *                                  <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     *                                  <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
     *                                  </ul>
     * @see VerifyListener
     * @since 3.1
     */
    public void addVerifyListener(final VerifyListener listener)
    {
        checkWidget();
        addListener(SWT.Verify, new Listener() {
            @Override
            public void handleEvent(Event event)
            {
                listener.verifyText(new VerifyEvent(event));
            }
        });
    }

    static int checkStyle(int style)
    {
        int mask = SWT.BORDER | SWT.READ_ONLY | SWT.FLAT | SWT.LEFT_TO_RIGHT | SWT.RIGHT_TO_LEFT;
        return style & mask;
    }

    @Override
    public Point computeSize(int wHint, int hHint, boolean changed)
    {
        checkWidget();
        Point textSize = super.computeSize(SWT.DEFAULT, SWT.DEFAULT, changed);
        Point listSize = this.table.computeSize(wHint, SWT.DEFAULT, changed);
        listSize.x += imageLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT, changed).x;
        listSize.x += arrow.computeSize(SWT.DEFAULT, SWT.DEFAULT, changed).x;
        listSize.x += 6;

        int height = Math.max(hHint, textSize.y);
        int width = Math.max(wHint, Math.max(textSize.x, listSize.x));
        if (widthHint != SWT.DEFAULT) {
            width = widthHint;
        }
        return new Point(width + 10, height);
    }

    /**
     * Returns the item at the given, zero-relative index in the
     * receiver's list. Throws an exception if the index is out
     * of range.
     *
     * @param index the index of the item to return
     * @return the item at the given index
     * @throws IllegalArgumentException <ul>
     *                                  <li>ERROR_INVALID_RANGE - if the index is not between 0 and the number of elements in the list minus 1 (inclusive)</li>
     *                                  </ul>
     * @throws SWTException             <ul>
     *                                  <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     *                                  <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
     *                                  </ul>
     */
    public String getItemText(int index)
    {
        checkWidget();
        return this.table.getItem(index).getText();
    }

    public TableItem getItem(int index)
    {
        checkWidget();
        return this.table.getItem(index);
    }

    /**
     * Returns the number of items contained in the receiver's list.
     *
     * @return the number of items
     * @throws SWTException <ul>
     *                      <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     *                      <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
     *                      </ul>
     */
    public int getItemCount()
    {
        checkWidget();
        return this.table.getItemCount();
    }

    /**
     * Returns a (possibly empty) array of <code>String</code>s which are
     * the items in the receiver's list.
     * <p>
     * Note: This is not the actual structure used by the receiver
     * to maintain its list of items, so modifying the array will
     * not affect the receiver.
     * </p>
     *
     * @return the items in the receiver's list
     * @throws SWTException <ul>
     *                      <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     *                      <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
     *                      </ul>
     */
    public TableItem[] getItems()
    {
        checkWidget();
        return this.table.getItems();
    }

    /**
     * Returns the zero-relative index of the item which is currently
     * selected in the receiver's list, or -1 if no item is selected.
     *
     * @return the index of the selected item
     * @throws SWTException <ul>
     *                      <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     *                      <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
     *                      </ul>
     */
    public int getSelectionIndex()
    {
        checkWidget();
        return this.table.getSelectionIndex();
    }

    /**
     * Returns a string containing a copy of the contents of the
     * receiver's text field, or an empty string if there are no
     * contents.
     *
     * @return the receiver's text
     * @throws SWTException <ul>
     *                      <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     *                      <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
     *                      </ul>
     */
    public String getText()
    {
        checkWidget();
        return this.text.getText();
    }

    /**
     * Searches the receiver's list starting at the first item
     * (index 0) until an item is found that is equal to the
     * argument, and returns the index of that item. If no item
     * is found, returns -1.
     *
     * @param string the search item
     * @return the index of the item
     * @throws IllegalArgumentException <ul>
     *                                  <li>ERROR_NULL_ARGUMENT - if the string is null</li>
     *                                  </ul>
     * @throws SWTException             <ul>
     *                                  <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     *                                  <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
     *                                  </ul>
     */
    public int indexOf(String string)
    {
        checkWidget();
        if (string == null) {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }
        return Arrays.asList(getStringsFromTable()).indexOf(string);
    }

    /**
     * Removes the item from the receiver's list at the given
     * zero-relative index.
     *
     * @param index the index for the item
     * @throws IllegalArgumentException <ul>
     *                                  <li>ERROR_INVALID_RANGE - if the index is not between 0 and the number of elements in the list minus 1 (inclusive)</li>
     *                                  </ul>
     * @throws SWTException             <ul>
     *                                  <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     *                                  <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
     *                                  </ul>
     */
    public void remove(int index)
    {
        checkWidget();
        if (index == getSelectionIndex() && index > 0) {
            select(0);
        }
        this.table.remove(index);
    }

    /**
     * Removes the items from the receiver's list which are
     * between the given zero-relative start and end
     * indices (inclusive).
     *
     * @param start the start of the range
     * @param end   the end of the range
     * @throws IllegalArgumentException <ul>
     *                                  <li>ERROR_INVALID_RANGE - if either the start or end are not between 0 and the number of elements in the list minus 1 (inclusive)</li>
     *                                  </ul>
     * @throws SWTException             <ul>
     *                                  <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     *                                  <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
     *                                  </ul>
     */
    public void remove(int start, int end)
    {
        checkWidget();
        this.table.remove(start, end);
    }

    /**
     * Searches the receiver's list starting at the first item
     * until an item is found that is equal to the argument,
     * and removes that item from the list.
     *
     * @param string the item to remove
     * @throws IllegalArgumentException <ul>
     *                                  <li>ERROR_NULL_ARGUMENT - if the string is null</li>
     *                                  <li>ERROR_INVALID_ARGUMENT - if the string is not found in the list</li>
     *                                  </ul>
     * @throws SWTException             <ul>
     *                                  <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     *                                  <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
     *                                  </ul>
     */
    public void remove(String string)
    {
        checkWidget();
        if (string == null) {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }
        int index = -1;
        for (int i = 0, n = this.table.getItemCount(); i < n; i++) {
            if (this.table.getItem(i).getText().equals(string)) {
                index = i;
                break;
            }
        }
        remove(index);
    }

    /**
     * Removes all of the items from the receiver's list and clear the
     * contents of receiver's text field.
     * <p/>
     *
     * @throws SWTException <ul>
     *                      <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     *                      <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
     *                      </ul>
     */
    public void removeAll()
    {
        checkWidget();
        this.text.setText(""); //$NON-NLS-1$
        this.table.removeAll();
    }

    /**
     * Selects the item at the given zero-relative index in the receiver's
     * list.  If the item at the index was already selected, it remains
     * selected. Indices that are out of range are ignored.
     *
     * @param index the index of the item to select
     * @throws SWTException <ul>
     *                      <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     *                      <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
     *                      </ul>
     */
    public void select(int index)
    {
        checkWidget();
        if (index == -1) {
            this.table.deselectAll();
            this.text.setText(""); //$NON-NLS-1$
            this.setBackground(null);
            return;
        }
        if (0 <= index && index < this.table.getItemCount()) {
            if (index != getSelectionIndex()) {
                TableItem item = this.table.getItem(index);
                if (item.getImage() != null) {
                    this.imageLabel.setImage(item.getImage());
                }
                if (item.getBackground() != null) {
                    this.setBackground(item.getBackground());
                }
                this.text.setText(item.getText());
                //this.text.selectAll();
                this.table.select(index);
                this.table.showSelection();
            }
        }
    }

    public void select(Object data)
    {
        checkWidget();
        if (data == null) {
            select(-1);
            return;
        }
        int itemCount = this.table.getItemCount();
        for (int i = 0; i < itemCount; i++) {
            if (data.equals(this.table.getItem(i).getData())) {
                select(i);
                break;
            }
        }
    }

    @Override
    public void setFont(Font font)
    {
        checkWidget();
        super.setFont(font);
        this.font = font;
        this.text.setFont(font);
        this.table.setFont(font);
    }

    /**
     * Sets the contents of the receiver's text field to the
     * given string.
     * <p>
     * Note: The text field in a <code>Combo</code> is typically
     * only capable of displaying a single line of text. Thus,
     * setting the text to a string containing line breaks or
     * other special characters will probably cause it to
     * display incorrectly.
     * </p>
     *
     * @param string the new text
     * @throws IllegalArgumentException <ul>
     *                                  <li>ERROR_NULL_ARGUMENT - if the string is null</li>
     *                                  </ul>
     * @throws SWTException             <ul>
     *                                  <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     *                                  <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
     *                                  </ul>
     */
    public void setText(String string)
    {
        checkWidget();
        if (string == null) {
            string = "";
        }
        int index = -1;
        for (int i = 0, n = this.table.getItemCount(); i < n; i++) {
            if (this.table.getItem(i).getText().equals(string)) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            this.table.deselectAll();
            this.text.setText(string);
            return;
        }
        this.text.setText(string);
        //this.text.selectAll();
        this.table.setSelection(index);
        this.table.showSelection();
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

    /**
     * Sets the number of items that are visible in the drop
     * down portion of the receiver's list.
     * <p>
     * Note: This operation is a hint and is not supported on
     * platforms that do not have this concept.
     * </p>
     *
     * @param count the new number of items to be visible
     * @throws SWTException <ul>
     *                      <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     *                      <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
     *                      </ul>
     * @since 3.0
     */
    public void setVisibleItemCount(int count)
    {
        checkWidget();
        if (count < 0) {
            return;
        }
        this.visibleItemCount = count;
    }

    void handleFocus(int type)
    {
        if (isDisposed()) {
            return;
        }
        switch (type) {
            case SWT.FocusIn: {
                if (this.hasFocus) {
                    return;
                }
//                if (getEditable()) {
//                    this.text.selectAll();
//                }
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
                if (focusControl == this.arrow || focusControl == this.table || focusControl == this) {
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

    void createPopup(int selectionIndex)
    {
        Shell oldPopup = this.popup;

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
        this.popup.setLayout(new FillLayout());
        // create a table instead of a list.
        Table oldTable = this.table;
        this.table = new Table(this.popup, listStyle);
        if (this.font != null) {
            this.table.setFont(this.font);
        }
        new TableColumn(table, SWT.LEFT);
        if (oldTable != null) {
            for (TableItem oldItem : oldTable.getItems()) {
                TableItem newItem = new TableItem(this.table, oldItem.getStyle());
                newItem.setText(oldItem.getText());
                newItem.setImage(oldItem.getImage());
                newItem.setData(oldItem.getData());
                newItem.setBackground(oldItem.getBackground());
                newItem.setForeground(oldItem.getForeground());
            }
        }

        int[] popupEvents = {SWT.Close, SWT.Paint, SWT.Deactivate};
        for (int popupEvent : popupEvents) {
            this.popup.addListener(popupEvent, this.listener);
        }
        int[] listEvents = {SWT.MouseUp, SWT.Selection, SWT.Traverse, SWT.KeyDown, SWT.KeyUp, SWT.FocusIn, SWT.Dispose, SWT.Resize};
        for (int listEvent : listEvents) {
            this.table.addListener(listEvent, this.listener);
        }

        if (selectionIndex != -1) {
            this.table.setSelection(selectionIndex);
        }
        if (oldPopup != null) {
            oldPopup.dispose();
        }
    }

    boolean isDropped()
    {
        return this.popup.getVisible();
    }

    void dropDown(boolean drop)
    {
        if (drop == isDropped()) {
            return;
        }
        if (!drop) {
            this.popup.setVisible(false);
            if (!isDisposed() && this.arrow.isFocusControl()) {
                this.setFocus();
            }
            return;
        }

        boolean newPopup = getShell() != this.popup.getParent();
        if (newPopup) {
            int selectionIndex = this.table.getSelectionIndex();
            this.table.removeListener(SWT.Dispose, this.listener);
            createPopup(selectionIndex);
        }

        // Commented because it increased table size on each dropdown

        Point size = getSize();
        int itemCount = this.table.getItemCount();
        itemCount = (itemCount == 0) ? this.visibleItemCount : Math.min(this.visibleItemCount, itemCount);
        int itemHeight = this.table.getItemHeight() * itemCount;
        Point listSize = this.table.computeSize(SWT.DEFAULT, itemHeight, false);
        ScrollBar verticalBar = table.getVerticalBar();
        if (verticalBar != null) {
            listSize.x -= verticalBar.getSize().x;
        }
        this.table.setBounds(1, 1, Math.max(size.x - 2, listSize.x), listSize.y);

        int index = this.table.getSelectionIndex();
        if (index != -1) {
            this.table.setTopIndex(index);
        }
        Display display = getDisplay();
        Rectangle listRect = this.table.getBounds();
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

        {
            final TableColumn column = table.getColumn(0);
            column.pack();
            final int maxSize = table.getSize().x - 2;// - 2;//table.getVerticalBar().getSize().x;
            if (column.getWidth() < maxSize) {
                column.setWidth(maxSize);
            }
        }
        if (this.popup.getData("resizeListener") == null) {
            this.popup.addListener(SWT.Resize, new Listener() {
                @Override
                public void handleEvent(Event event) {
                    CImageCombo.this.sizeHint = popup.getSize();
                }
            });
            this.popup.setData("resizeListener", Boolean.TRUE);
        }
        this.popup.setVisible(true);
        this.table.setFocus();
    }

    void listEvent(Event event)
    {
        switch (event.type) {
            case SWT.Dispose:
                if (getShell() != this.popup.getParent()) {
                    int selectionIndex = this.table.getSelectionIndex();
                    this.popup = null;
                    this.table = null;
                    createPopup(selectionIndex);
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
                int index = this.table.getSelectionIndex();
                if (index == -1) {
                    return;
                }
                TableItem item = this.table.getItem(index);
                this.text.setText(item.getText());
                //this.text.selectAll();
                if (item.getImage() != null) {
                    this.imageLabel.setImage(item.getImage());
                }
                if (item.getBackground() != null) {
                    this.setBackground(item.getBackground());
                }
                this.table.setSelection(index);
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
                table.getColumn(0).setWidth(table.getSize().x - table.getVerticalBar().getSize().x);
                break;
            }
        }
    }

    void arrowEvent(Event event)
    {
        switch (event.type) {
            case SWT.FocusIn: {
                handleFocus(SWT.FocusIn);
                break;
            }
            case SWT.Selection: {
                dropDown(!isDropped());
                break;
            }
        }
    }

    void comboEvent(Event event)
    {
        switch (event.type) {
            case SWT.Dispose:
                if (this.popup != null && !this.popup.isDisposed()) {
                    this.table.removeListener(SWT.Dispose, this.listener);
                    this.popup.dispose();
                }
                Shell shell = getShell();
                shell.removeListener(SWT.Deactivate, this.listener);
                Display display = getDisplay();
                display.removeFilter(SWT.FocusIn, this.filter);
                this.popup = null;
                this.table = null;
                this.arrow = null;
                break;
            case SWT.Move:
                dropDown(false);
                break;
        }
    }

    void popupEvent(Event event)
    {
        switch (event.type) {
            case SWT.Paint:
                // draw black rectangle around list
                Rectangle listRect = this.table.getBounds();
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

    String[] getStringsFromTable()
    {
        String[] items = new String[this.table.getItems().length];
        for (int i = 0, n = items.length; i < n; i++) {
            items[i] = this.table.getItem(i).getText();
        }
        return items;
    }

    void textEvent(Event event)
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
                this.table.deselectAll();
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
                dropDown(!dropped);
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
