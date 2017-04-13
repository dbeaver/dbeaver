/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.services.IServiceLocator;
import org.eclipse.ui.swt.IFocusService;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPConnectionType;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.RunnableWithResult;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceInvalidateHandler;
import org.jkiss.dbeaver.ui.controls.CustomCheckboxCellEditor;
import org.jkiss.dbeaver.ui.controls.CustomComboBoxCellEditor;
import org.jkiss.dbeaver.ui.controls.CustomNumberCellEditor;
import org.jkiss.dbeaver.ui.controls.CustomTextCellEditor;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.dialogs.StandardErrorDialog;
import org.jkiss.dbeaver.ui.dialogs.driver.DriverEditDialog;
import org.jkiss.dbeaver.ui.editors.text.BaseTextEditor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.BeanUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.SortedMap;

/**
 * UI Utils
 */
public class UIUtils {

    static final Log log = Log.getLog(UIUtils.class);

    public static final String DEFAULT_TIMESTAMP_PATTERN = "yyyy.MM.dd HH:mm";
    public static final String INLINE_WIDGET_EDITOR_ID = "org.jkiss.dbeaver.ui.InlineWidgetEditor";

    public static VerifyListener getIntegerVerifyListener(Locale locale)
    {
        final DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);
        return new VerifyListener() {
            @Override
            public void verifyText(VerifyEvent e)
            {
                for (int i = 0; i < e.text.length(); i++) {
                    char ch = e.text.charAt(i);
                    if (!Character.isDigit(ch) && ch != symbols.getMinusSign() && ch != symbols.getGroupingSeparator()) {
                        e.doit = false;
                        return;
                    }
                }
                e.doit = true;
            }
        };
    }

    public static VerifyListener getNumberVerifyListener(Locale locale)
    {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);
        final char[] allowedChars = new char[] { symbols.getDecimalSeparator(), symbols.getGroupingSeparator(),
            symbols.getMinusSign(), symbols.getZeroDigit(), symbols.getMonetaryDecimalSeparator(), '+' };
        final String exponentSeparator = symbols.getExponentSeparator();
        return new VerifyListener() {
            @Override
            public void verifyText(VerifyEvent e)
            {
                for (int i = 0; i < e.text.length(); i++) {
                    char ch = e.text.charAt(i);
                    if (!Character.isDigit(ch) && !ArrayUtils.contains(allowedChars, ch) && exponentSeparator.indexOf(ch) == -1) {
                        e.doit = false;
                        return;
                    }
                }
                e.doit = true;
            }
        };
    }

    public static ToolItem createToolItem(ToolBar toolBar, String text, Image icon, final IAction action)
    {
        ToolItem item = new ToolItem(toolBar, SWT.PUSH);
        item.setToolTipText(text);
        if (icon != null) {
            item.setImage(icon);
        }
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                action.run();
            }
        });
        return item;
    }

    public static TableColumn createTableColumn(Table table, int style, String text)
    {
        TableColumn column = new TableColumn(table, style);
        column.setText(text);
        return column;
    }

    public static TreeColumn createTreeColumn(Tree tree, int style, String text)
    {
        TreeColumn column = new TreeColumn(tree, style);
        column.setText(text);
        return column;
    }

    public static void packColumns(Table table)
    {
        packColumns(table, false);
    }

    public static void packColumns(Table table, boolean fit)
    {
        table.setRedraw(false);
        try {
            int totalWidth = 0;
            final TableColumn[] columns = table.getColumns();
            for (TableColumn column : columns) {
                column.pack();
                totalWidth += column.getWidth();
            }
            final Rectangle clientArea = table.getClientArea();
            if (clientArea.width > 0 && totalWidth > clientArea.width) {
                for (TableColumn column : columns) {
                    int colWidth = column.getWidth();
                    if (colWidth > totalWidth / 3) {
                        // If some columns is too big (more than 33% of total width)
                        // Then shrink it to 30%
                        column.setWidth(totalWidth / 3);
                        totalWidth -= colWidth;
                        totalWidth += column.getWidth();
                    }
                }
                int extraSpace = totalWidth - clientArea.width;

                for (TableColumn tc : columns) {
                    double ratio = (double) tc.getWidth() / totalWidth;
                    int newWidth = (int) (tc.getWidth() - extraSpace * ratio);
                    tc.setWidth(newWidth);
                }
            }
            if (fit && totalWidth < clientArea.width) {
                int sbWidth = 0;
                if (table.getVerticalBar() != null) {
                    sbWidth = table.getVerticalBar().getSize().x;
                }
                if (columns.length > 0) {
                    float extraSpace = (clientArea.width - totalWidth - sbWidth) / columns.length;
                    for (TableColumn tc : columns) {
                        tc.setWidth((int) (tc.getWidth() + extraSpace));
                    }
                }
            }
        } finally {
            table.setRedraw(true);
        }
    }

    public static void packColumns(@NotNull Tree tree)
    {
        packColumns(tree, false, null);
    }

    public static void packColumns(@NotNull Tree tree, boolean fit, @Nullable float[] ratios)
    {
        tree.setRedraw(false);
        try {
            // Check for disposed items
            // TODO: it looks like SWT error. Sometimes tree items are disposed and NPE is thrown from column.pack
            for (TreeItem item : tree.getItems()) {
                if (item.isDisposed()) {
                    return;
                }
            }
            int totalWidth = 0;
            final TreeColumn[] columns = tree.getColumns();
            for (TreeColumn column : columns) {
                column.pack();
                totalWidth += column.getWidth();
            }
            Rectangle clientArea = tree.getClientArea();
            if (clientArea.isEmpty()) {
                return;
            }
            if (fit) {
                int areaWidth = clientArea.width;
                if (tree.getVerticalBar() != null) {
                    areaWidth -= tree.getVerticalBar().getSize().x;
                }
                if (totalWidth > areaWidth) {
                    int extraSpace = totalWidth - areaWidth;
                    for (TreeColumn tc : columns) {
                        double ratio = (double) tc.getWidth() / totalWidth;
                        tc.setWidth((int) (tc.getWidth() - extraSpace * ratio));
                    }
                } else if (totalWidth < areaWidth) {
                    float extraSpace = areaWidth - totalWidth;
                    if (columns.length > 0) {
                        if (ratios == null || ratios.length < columns.length) {
                            extraSpace /= columns.length;
                            extraSpace--;
                            for (TreeColumn tc : columns) {
                                tc.setWidth((int) (tc.getWidth() + extraSpace));
                            }
                        } else {
                            for (int i = 0; i < columns.length; i++) {
                                TreeColumn tc = columns[i];
                                tc.setWidth((int) (tc.getWidth() + extraSpace * ratios[i]));
                            }
                        }
                    }
                }
            }
        } finally {
            tree.setRedraw(true);
        }
    }

    public static void maxTableColumnsWidth(Table table)
    {
        table.setRedraw(false);
        try {
            int columnCount = table.getColumnCount();
            if (columnCount > 0) {
                int totalWidth = 0;
                final TableColumn[] columns = table.getColumns();
                for (TableColumn tc : columns) {
                    tc.pack();
                    totalWidth += tc.getWidth();
                }
                final Rectangle clientArea = table.getClientArea();
                if (totalWidth < clientArea.width) {
                    int extraSpace = clientArea.width - totalWidth;
                    extraSpace /= columnCount;
                    for (TableColumn tc : columns) {
                        tc.setWidth(tc.getWidth() + extraSpace);
                    }
                }
            }
        } finally {
            table.setRedraw(true);
        }
    }

    public static int getColumnAtPos(TableItem item, int x, int y)
    {
        int columnCount = item.getParent().getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            Rectangle rect = item.getBounds(i);
            if (rect.contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    public static int getColumnAtPos(TreeItem item, int x, int y)
    {
        int columnCount = item.getParent().getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            Rectangle rect = item.getBounds(i);
            if (rect.contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    public static void sortTable(Table table, Comparator<TableItem> comparator)
    {
        int columnCount = table.getColumnCount();
        String[] values = new String[columnCount];
        TableItem[] items = table.getItems();
        for (int i = 1; i < items.length; i++) {
            for (int j = 0; j < i; j++) {
                TableItem item = items[i];
                if (comparator.compare(item, items[j]) < 0) {
                    for (int k = 0; k < columnCount; k++) {
                        values[k] = item.getText(k);
                    }
                    Object data = item.getData();
                    boolean checked = item.getChecked();
                    item.dispose();

                    item = new TableItem(table, SWT.NONE, j);
                    item.setText(values);
                    item.setData(data);
                    item.setChecked(checked);
                    items = table.getItems();
                    break;
                }
            }
        }
    }

    public static TableItem getNextTableItem(Table table, TableItem item) {
        TableItem[] items = table.getItems();
        for (int i = 0; i < items.length - 1; i++) {
            if (items[i] == item) {
                return items[i + 1];
            }
        }
        return null;
    }

    public static TreeItem getNextTreeItem(Tree tree, TreeItem item) {
        TreeItem[] items = tree.getItems();
        for (int i = 0; i < items.length - 1; i++) {
            if (items[i] == item) {
                return items[i + 1];
            }
        }
        return null;
    }

    public static void dispose(Widget widget)
    {
        if (widget != null && !widget.isDisposed()) {
            try {
                widget.dispose();
            } catch (Exception e) {
                log.debug("widget dispose error", e);
            }
        }
    }

    public static void dispose(Resource resource)
    {
        if (resource != null && !resource.isDisposed()) {
            try {
                resource.dispose();
            } catch (Exception e) {
                log.debug("Resource dispose error", e);
            }
        }
    }

    public static void showMessageBox(final Shell shell, final String title, final String info, final int messageType)
    {
        Runnable runnable = new Runnable() {
            @Override
            public void run()
            {
                MessageBox messageBox = new MessageBox(shell, messageType | SWT.OK);
                messageBox.setMessage(info);
                messageBox.setText(title);
                messageBox.open();
            }
        };
        runInUI(shell, runnable);
    }

    public static boolean confirmAction(final Shell shell, final String title, final String question)
    {
        RunnableWithResult<Boolean> confirmer = new RunnableWithResult<Boolean>() {
            @Override
            public void run()
            {
                MessageBox messageBox = new MessageBox(shell, SWT.ICON_WARNING | SWT.YES | SWT.NO);
                messageBox.setMessage(question);
                messageBox.setText(title);
                int response = messageBox.open();
                result = (response == SWT.YES);
            }
        };
        runInUI(shell, confirmer);
        return confirmer.getResult();
    }

    public static Font makeBoldFont(Font normalFont)
    {
        return modifyFont(normalFont, SWT.BOLD);
    }

    public static Font modifyFont(Font normalFont, int style)
    {
        FontData[] fontData = normalFont.getFontData();
        fontData[0].setStyle(fontData[0].getStyle() | style);
        return new Font(normalFont.getDevice(), fontData[0]);
    }

    public static Group createControlGroup(Composite parent, String label, int columns, int layoutStyle, int widthHint)
    {
        Group group = new Group(parent, SWT.NONE);
        group.setText(label);

        GridData gd = new GridData(layoutStyle);
        if (widthHint > 0) {
            gd.widthHint = widthHint;
        }
        group.setLayoutData(gd);

        GridLayout gl = new GridLayout(columns, false);
        group.setLayout(gl);

        return group;
    }

    public static Label createControlLabel(Composite parent, String label)
    {
        Label textLabel = new Label(parent, SWT.NONE);
        textLabel.setText(label + ": "); //$NON-NLS-1$

        return textLabel;
    }

    public static Label createTextLabel(Composite parent, String label)
    {
        Label textLabel = new Label(parent, SWT.NONE);
        textLabel.setText(label);

        return textLabel;
    }

    public static Label createImageLabel(Composite parent, DBPImage image)
    {
        Label imageLabel = new Label(parent, SWT.NONE);
        imageLabel.setImage(DBeaverIcons.getImage(image));

        return imageLabel;
    }

    public static Text createLabelText(Composite parent, String label, String value)
    {
        return createLabelText(parent, label, value, SWT.BORDER);
    }

    public static Text createLabelText(Composite parent, String label, String value, int style)
    {
        return createLabelText(parent, label, value, style, new GridData(GridData.FILL_HORIZONTAL));
    }

    @NotNull
    public static Text createLabelText(@NotNull Composite parent, @NotNull String label, @Nullable String value, int style,
        @Nullable Object layoutData)
    {
        createControlLabel(parent, label);

        Text text = new Text(parent, style);
        if (value != null) {
            text.setText(value);
        }

        if (layoutData != null) {
            text.setLayoutData(layoutData);
        }

        return text;
    }

    @NotNull
    public static Spinner createLabelSpinner(@NotNull Composite parent, @NotNull String label, int value, int minimum, int maximum)
    {
        createControlLabel(parent, label);

        Spinner spinner = new Spinner(parent, SWT.BORDER);
        spinner.setMinimum(minimum);
        spinner.setMaximum(maximum);
        spinner.setSelection(value);

        return spinner;
    }

    @NotNull
    public static Button createLabelCheckbox(Composite parent, String label, boolean checked)
    {
        return createLabelCheckbox(parent, label, null, checked, SWT.NONE);
    }

    @NotNull
    public static Button createLabelCheckbox(Composite parent, String label, String tooltip, boolean checked)
    {
        return createLabelCheckbox(parent, label, tooltip, checked, SWT.NONE);
    }

    @NotNull
    public static Button createLabelCheckbox(@NotNull Composite parent, @NotNull String label, @Nullable String tooltip,
        boolean checked, int style)
    {
        Label labelControl = createControlLabel(parent, label);
        // labelControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        final Button button = new Button(parent, SWT.CHECK | style);
        if (checked) {
            button.setSelection(true);
        }
        labelControl.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseUp(MouseEvent e)
            {
                if (!button.isDisposed() && button.isVisible() && button.isEnabled()) {
                    button.setSelection(!button.getSelection());
                    button.notifyListeners(SWT.Selection, new Event());
                }
            }
        });

        if (tooltip != null) {
            labelControl.setToolTipText(tooltip);
            button.setToolTipText(tooltip);
        }
        return button;
    }

    public static Button createCheckbox(Composite parent, String label, boolean checked)
    {
        final Button button = new Button(parent, SWT.CHECK);
        button.setText(label);
        if (checked) {
            button.setSelection(true);
        }

        return button;
    }

    public static Combo createLabelCombo(Composite parent, String label, int style)
    {
        Label labelControl = createControlLabel(parent, label);
        // labelControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        final Combo combo = new Combo(parent, style);
        combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        return combo;
    }

    public static Button createToolButton(Composite parent, String text, SelectionListener selectionListener)
    {
        Button button = new Button(parent, SWT.PUSH);
        button.setText(text);
        button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        if (selectionListener != null) {
            button.addSelectionListener(selectionListener);
        }
        return button;
    }

    @Nullable
    public static Shell getActiveShell()
    {
        IWorkbench workbench = PlatformUI.getWorkbench();
        return workbench == null ? null : getShell(workbench.getActiveWorkbenchWindow());
    }

    @Nullable
    public static Shell getShell(IShellProvider provider)
    {
        return provider == null ? null : provider.getShell();
    }

    @Nullable
    public static Shell getShell(IWorkbenchPart part)
    {
        return part == null ? null : getShell(part.getSite());
    }

    @Nullable
    public static Integer getTextInteger(Text text)
    {
        String str = text.getText();
        str = str.trim();
        if (str.length() == 0) {
            return null;
        }
        try {
            return Integer.valueOf(str);
        } catch (NumberFormatException e) {
            log.debug(e);
            return null;
        }
    }

    @Nullable
    public static IHandlerActivation registerKeyBinding(IServiceLocator serviceLocator, IAction action)
    {
        IHandlerService handlerService = serviceLocator.getService(IHandlerService.class);
        if (handlerService != null) {
            return handlerService.activateHandler(action.getActionDefinitionId(), new ActionHandler(action));
        } else {
            return null;
        }
    }

    public static Composite createPlaceholder(Composite parent, int columns)
    {
        return createPlaceholder(parent, columns, 0);
    }

    public static Composite createPlaceholder(Composite parent, int columns, int spacing)
    {
        Composite ph = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(columns, false);
        gl.verticalSpacing = spacing;
        gl.horizontalSpacing = spacing;
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        ph.setLayout(gl);
        return ph;
    }

    public static Label createHorizontalLine(Composite parent)
    {
        Label horizontalLine = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        horizontalLine.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 1, 1));
        return horizontalLine;
    }

    @Nullable
    public static String getComboSelection(Combo combo)
    {
        int selectionIndex = combo.getSelectionIndex();
        if (selectionIndex < 0) {
            return null;
        }
        return combo.getItem(selectionIndex);
    }

    public static boolean setComboSelection(Combo combo, String value)
    {
        if (value == null) {
            return false;
        }
        int count = combo.getItemCount();
        for (int i = 0; i < count; i++) {
            if (value.equals(combo.getItem(i))) {
                combo.select(i);
                return true;
            }
        }
        return false;
    }

    public static Combo createEncodingCombo(Composite parent, String curCharset)
    {
        if (curCharset == null) {
            curCharset = GeneralUtils.getDefaultFileEncoding();
        }
        Combo encodingCombo = new Combo(parent, SWT.DROP_DOWN);
        encodingCombo.setVisibleItemCount(30);
        SortedMap<String, Charset> charsetMap = Charset.availableCharsets();
        int index = 0;
        int defIndex = -1;
        for (String csName : charsetMap.keySet()) {
            Charset charset = charsetMap.get(csName);
            encodingCombo.add(charset.displayName());
            if (charset.displayName().equalsIgnoreCase(curCharset)) {
                defIndex = index;
            }
            if (defIndex < 0) {
                for (String alias : charset.aliases()) {
                    if (alias.equalsIgnoreCase(curCharset)) {
                        defIndex = index;
                    }
                }
            }
            index++;
        }
        if (defIndex >= 0) {
            encodingCombo.select(defIndex);
        } else {
            log.warn("Charset '" + curCharset + "' is not recognized"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return encodingCombo;
    }

    @NotNull
    public static SashForm createPartDivider(final IWorkbenchPart workbenchPart, Composite parent, int style)
    {
        final SashForm sash = new SashForm(parent, style);
        /*
         * //sash.setSashWidth(10); final IWorkbenchWindow workbenchWindow = workbenchPart.getSite().getWorkbenchWindow();
         * //sash.setBackground(sashActiveBackground);
         * 
         * final IPartListener partListener = new IPartListener() {
         * 
         * @Override public void partBroughtToTop(IWorkbenchPart part) { }
         * 
         * @Override public void partOpened(IWorkbenchPart part) { }
         * 
         * @Override public void partClosed(IWorkbenchPart part) { }
         * 
         * @Override
         * 
         * @SuppressWarnings("restriction") public void partActivated(IWorkbenchPart part) { if (part == workbenchPart) { Color
         * sashActiveBackground = workbenchWindow.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry().get(
         * IWorkbenchThemeConstants.ACTIVE_TAB_BG_END); if (sashActiveBackground == null) { sashActiveBackground =
         * Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW); } sash.setBackground(sashActiveBackground); } }
         * 
         * @Override public void partDeactivated(IWorkbenchPart part) { if (part == workbenchPart) {
         * sash.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND)); } } };
         * 
         * final IPageListener pageListener = workbenchWindow.getActivePage() != null ? null : new IPageListener() {
         * 
         * @Override public void pageActivated(IWorkbenchPage page) { }
         * 
         * @Override public void pageOpened(IWorkbenchPage page) { page.addPartListener(partListener); }
         * 
         * @Override public void pageClosed(IWorkbenchPage page) { page.removePartListener(partListener); } }; if (pageListener !=
         * null) { // No active page yet, wait for it in listener workbenchWindow.addPageListener(pageListener); } else { // Add
         * listener to active page workbenchWindow.getActivePage().addPartListener(partListener); }
         * 
         * sash.addDisposeListener(new DisposeListener() {
         * 
         * @Override public void widgetDisposed(DisposeEvent e) { if (pageListener != null) {
         * workbenchWindow.removePageListener(pageListener); } if (workbenchWindow.getActivePage() != null) {
         * workbenchWindow.getActivePage().removePartListener(partListener); } } });
         */

        return sash;
    }

    public static void showErrorDialog(@Nullable Shell shell, @NotNull String title, @Nullable String message, @Nullable Throwable error)
    {
        if (error != null) {
            log.error(error);
        }

        showErrorDialog(shell, title, error == null ? null : message, error == null ? new Status(IStatus.ERROR,
            DBeaverCore.PLUGIN_ID, message) : GeneralUtils.makeExceptionStatus(error));
    }

    public static void showErrorDialog(@Nullable Shell shell, @NotNull String title, @Nullable String message)
    {
        showErrorDialog(shell, title, message, (Throwable) null);
    }

    public static void showErrorDialog(@Nullable final Shell shell, @NotNull final String title, @Nullable final String message, @NotNull final IStatus status)
    {
        for (IStatus s = status; s != null; ) {
            if (s.getException() instanceof DBException) {
                if (showDatabaseError(shell, title, message, (DBException) s.getException())) {
                    // If this DB error was handled by some DB-specific way then just don't care about it
                    return;
                }
                break;
            }
            if (s.getChildren() != null && s.getChildren().length > 0) {
                s = s.getChildren()[0];
            } else {
                break;
            }
        }
        // log.debug(message);
        Runnable runnable = new Runnable() {
            @Override
            public void run()
            {
                // Display the dialog
                StandardErrorDialog dialog = new StandardErrorDialog(shell == null ? DBeaverUI.getActiveWorkbenchShell() : shell,
                    title, message, RuntimeUtils.stripStack(status), IStatus.ERROR);
                dialog.open();
            }
        };
        runInUI(shell, runnable);
    }

    public static void runInUI(@Nullable Shell shell, @NotNull Runnable runnable)
    {
        final Display display = shell == null || shell.isDisposed() ? Display.getDefault() : shell.getDisplay();
        if (display.getThread() != Thread.currentThread()) {
            display.syncExec(runnable);
        } else {
            runnable.run();
        }
    }

    public static void runInDetachedUI(@Nullable Shell shell, @NotNull Runnable runnable)
    {
        if (shell == null) {
            Display.getDefault().asyncExec(runnable);
        } else {
            try {
                shell.getDisplay().asyncExec(runnable);
            } catch (SWTException e) {
                // DF: Widget has been disposed, too late for some processing then..
            }
        }
    }

    @NotNull
    public static String formatMessage(@Nullable String message, @Nullable Object... args)
    {
        if (message == null) {
            return ""; //$NON-NLS-1$
        } else {
            return MessageFormat.format(message, args);
        }
    }

    @NotNull
    public static Button createPushButton(@NotNull Composite parent, @Nullable String label, @Nullable Image image)
    {
        Button button = new Button(parent, SWT.PUSH);
        if (label != null) {
            button.setText(label);
        }
        if (image != null) {
            button.setImage(image);
        }
        return button;
    }

    public static void setHelp(Control control, String pluginId, String helpContextID)
    {
        PlatformUI.getWorkbench().getHelpSystem().setHelp(control, pluginId + "." + helpContextID); //$NON-NLS-1$
    }

    public static void setHelp(Control control, String helpContextID)
    {
        setHelp(control, DBeaverCore.PLUGIN_ID, helpContextID);
    }

    @NotNull
    public static Text createOutputFolderChooser(final Composite parent, @Nullable String label,
        @Nullable ModifyListener changeListener)
    {
        UIUtils.createControlLabel(parent, label != null ? label : CoreMessages.data_transfer_wizard_output_label_directory);
        Composite chooserPlaceholder = UIUtils.createPlaceholder(parent, 2);
        chooserPlaceholder.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        final Text directoryText = new Text(chooserPlaceholder, SWT.BORDER);
        directoryText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        if (changeListener != null) {
            directoryText.addModifyListener(changeListener);
        }

        final Runnable folderChooser = new Runnable() {
            @Override
            public void run()
            {
                DirectoryDialog dialog = new DirectoryDialog(parent.getShell(), SWT.NONE);
                dialog.setMessage(CoreMessages.data_transfer_wizard_output_dialog_directory_message);
                dialog.setText(CoreMessages.data_transfer_wizard_output_dialog_directory_text);
                String directory = directoryText.getText();
                if (!CommonUtils.isEmpty(directory)) {
                    dialog.setFilterPath(directory);
                }
                directory = dialog.open();
                if (directory != null) {
                    directoryText.setText(directory);
                }
            }
        };
        directoryText.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseUp(MouseEvent e)
            {
                folderChooser.run();
            }
        });

        Button openFolder = new Button(chooserPlaceholder, SWT.PUSH | SWT.FLAT);
        openFolder.setImage(DBeaverIcons.getImage(DBIcon.TREE_FOLDER));
        openFolder.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_CENTER | GridData.HORIZONTAL_ALIGN_CENTER));
        openFolder.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                folderChooser.run();
            }
        });
        return directoryText;
    }

    public static String makeAnchor(String text)
    {
        return "<a>" + text + "</a>"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Nullable
    public static <T> T findView(IWorkbenchWindow workbenchWindow, Class<T> viewClass)
    {
        IViewReference[] references = workbenchWindow.getActivePage().getViewReferences();
        for (IViewReference ref : references) {
            IViewPart view = ref.getView(false);
            if (view != null && viewClass.isAssignableFrom(view.getClass())) {
                return viewClass.cast(view);
            }
        }
        return null;
    }

    @Nullable
    public static IViewPart findView(IWorkbenchWindow workbenchWindow, String viewId)
    {
        IViewReference[] references = workbenchWindow.getActivePage().getViewReferences();
        for (IViewReference ref : references) {
            if (ref.getId().equals(viewId)) {
                return ref.getView(false);
            }
        }
        return null;
    }

    public static void setClipboardContents(Display display, Transfer transfer, Object contents)
    {
        Clipboard clipboard = new Clipboard(display);
        clipboard.setContents(new Object[] { contents }, new Transfer[] { transfer });
        clipboard.dispose();
    }

    public static void updateMainWindowTitle(IWorkbenchWindow window)
    {
        IProject activeProject = DBeaverCore.getInstance().getProjectRegistry().getActiveProject();
        IProduct product = Platform.getProduct();
        String title = product == null ? "Unknown" : product.getName(); //$NON-NLS-1$
        if (activeProject != null) {
            title += " - " + activeProject.getName(); //$NON-NLS-1$
        }
        IWorkbenchPage activePage = window.getActivePage();
        if (activePage != null) {
            IEditorPart activeEditor = activePage.getActiveEditor();
            if (activeEditor != null) {
                title += " - [ " + activeEditor.getTitle() + " ]";
            }
        }
        window.getShell().setText(title);
    }

    public static void showPreferencesFor(Shell shell, Object element, String defPageID)
    {
        PreferenceDialog propDialog;
        if (element == null) {
            propDialog = PreferencesUtil.createPreferenceDialogOn(shell, defPageID, new String[] { defPageID }, null, PreferencesUtil.OPTION_NONE);
        } else {
            propDialog = PreferencesUtil.createPropertyDialogOn(shell, element, defPageID, null, null, PreferencesUtil.OPTION_NONE);
        }
        if (propDialog != null) {
            propDialog.open();
        }
    }

    public static void addFocusTracker(IServiceLocator serviceLocator, String controlID, Control control)
    {
        final IFocusService focusService = serviceLocator.getService(IFocusService.class);
        if (focusService != null) {
            focusService.addFocusTracker(control, controlID);
        } else {
            log.debug("Focus service not found in " + serviceLocator);
        }
    }

    public static void removeFocusTracker(IServiceLocator serviceLocator, Control control)
    {
        if (PlatformUI.getWorkbench().isClosing()) {
            // TODO: it is a bug in eclipse. During workbench shutdown disposed service returned.
            return;
        }
        final IFocusService focusService = serviceLocator.getService(IFocusService.class);
        if (focusService != null) {
            focusService.removeFocusTracker(control);
        } else {
            log.debug("Focus service not found in " + serviceLocator);
        }
    }

    public static IDialogSettings getDialogSettings(String dialogId)
    {
        IDialogSettings workbenchSettings = DBeaverActivator.getInstance().getDialogSettings();
        IDialogSettings section = workbenchSettings.getSection(dialogId);
        if (section == null) {
            section = workbenchSettings.addNewSection(dialogId);
        }
        return section;
    }

    @Nullable
    public static IWorkbenchPartSite getWorkbenchPartSite(IServiceLocator serviceLocator)
    {
        IWorkbenchPartSite partSite = serviceLocator.getService(IWorkbenchPartSite.class);
        if (partSite == null) {
            IWorkbenchPart activePart = serviceLocator.getService(IWorkbenchPart.class);
            if (activePart == null) {
                IWorkbenchWindow workbenchWindow = DBeaverUI.getActiveWorkbenchWindow();
                if (workbenchWindow != null) {
                    IWorkbenchPage activePage = workbenchWindow.getActivePage();
                    if (activePage != null) {
                        activePart = activePage.getActivePart();
                    }
                }
            }
            if (activePart != null) {
                partSite = activePart.getSite();
            }
        }
        return partSite;
    }

    public static boolean isContextActive(String contextId)
    {
        Collection<?> contextIds = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
            .getService(IContextService.class).getActiveContextIds();
        for (Object id : contextIds) {
            if (contextId.equals(id)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static ISelectionProvider getSelectionProvider(IServiceLocator serviceLocator)
    {
        ISelectionProvider selectionProvider = serviceLocator.getService(ISelectionProvider.class);
        if (selectionProvider != null) {
            return selectionProvider;
        }
        IWorkbenchPartSite partSite = getWorkbenchPartSite(serviceLocator);
        if (partSite == null) {
            IWorkbenchPart activePart = serviceLocator.getService(IWorkbenchPart.class);
            if (activePart == null) {
                IWorkbenchWindow activeWindow = DBeaverUI.getActiveWorkbenchWindow();
                if (activeWindow != null) {
                    activePart = activeWindow.getActivePage().getActivePart();
                }
            }
            if (activePart != null) {
                partSite = activePart.getSite();
            }
        }
        if (partSite != null) {
            return partSite.getSelectionProvider();
        } else {
            return null;
        }
    }

    public static void enableWithChildren(Composite composite, boolean enable)
    {
        composite.setEnabled(enable);
        for (Control child : composite.getChildren()) {
            if (child instanceof Composite) {
                enableWithChildren((Composite) child, enable);
            } else {
                child.setEnabled(enable);
            }
        }
    }

    /**
     * Determine whether this control or any of it's child has focus
     * 
     * @param control
     *            control to check
     * @return true if it has focus
     */
    public static boolean hasFocus(Control control)
    {
        Control focusControl = control.getDisplay().getFocusControl();
        if (focusControl == null) {
            return false;
        }
        for (Control fc = focusControl; fc != null; fc = fc.getParent()) {
            if (fc == control) {
                return true;
            }
        }
        return false;
    }

    /**
     * Eclipse hack. Disables/enabled all key bindings in specified site's part. Works only if host editor is extender of
     * AbstractTextEditor Uses reflection because setActionActivation is private method
     * TODO: find better way to disable key bindings or prioritize event handling to widgets
     * 
     * @param partSite workbench part site
     * @param enable enable or disable
     */
    @Deprecated
    public static void enableHostEditorKeyBindings(IWorkbenchPartSite partSite, boolean enable)
    {
        IWorkbenchPart part = partSite.getPart();
        if (part instanceof AbstractTextEditor) {
            AbstractTextEditor hostEditor = (AbstractTextEditor) part;
            if (hostEditor instanceof BaseTextEditor) {
                StyledText textWidget = ((BaseTextEditor) hostEditor).getTextViewer().getTextWidget();
                if (textWidget == null || textWidget.isDisposed()) {
                    return;
                }
            }
            try {
                Method activatorMethod = AbstractTextEditor.class.getDeclaredMethod("setActionActivation", Boolean.TYPE);
                activatorMethod.setAccessible(true);
                activatorMethod.invoke(hostEditor, enable);
            } catch (Throwable e) {
                if (e instanceof InvocationTargetException) {
                    e = ((InvocationTargetException) e).getTargetException();
                }
                log.warn("Can't disable text editor action activations", e);
            }
            //hostEditor.getEditorSite().getActionBarContributor().setActiveEditor(hostEditor);
        }
    }

    public static void enableHostEditorKeyBindingsSupport(final IWorkbenchPartSite partSite, Control control)
    {
        if (!(partSite.getPart() instanceof AbstractTextEditor)) {
            return;
        }

        final boolean[] activated = new boolean[] {false};
        control.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (!activated[0]) {
                    UIUtils.enableHostEditorKeyBindings(partSite, false);
                    activated[0] = true;
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (activated[0]) {
                    UIUtils.enableHostEditorKeyBindings(partSite, true);
                    activated[0] = false;
                }
            }
        });
        control.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                if (activated[0]) {
                    UIUtils.enableHostEditorKeyBindings(partSite, true);
                    activated[0] = false;
                }
            }
        });
    }

    public static CTabItem getTabItem(CTabFolder tabFolder, Object data)
    {
        for (CTabItem item : tabFolder.getItems()) {
            if (item.getData() == data) {
                return item;
            }
        }
        return null;
    }

    public static TreeItem getTreeItem(Tree tree, Object data)
    {
        for (TreeItem item : tree.getItems()) {
            if (item.getData() == data) {
                return item;
            }
        }
        return null;
    }

    public static int blend(int v1, int v2, int ratio)
    {
        return (ratio * v1 + (100 - ratio) * v2) / 100;
    }

    public static RGB blend(RGB c1, RGB c2, int ratio)
    {
        int r = blend(c1.red, c2.red, ratio);
        int g = blend(c1.green, c2.green, ratio);
        int b = blend(c1.blue, c2.blue, ratio);
        return new RGB(r, g, b);
    }

    public static boolean isParent(Control parent, Control child) {
        for (Control c = child; c != null; c = c.getParent()) {
            if (c == parent) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInDialog(Control control) {
        return control.getShell().getData() instanceof org.eclipse.jface.dialogs.Dialog;
    }

    public static boolean validateAndSave(DBRProgressMonitor monitor, ISaveablePart saveable)
    {
        if (!saveable.isDirty()) {
            return true;
        }
        SaveRunner saveRunner = new SaveRunner(monitor, saveable);
        runInUI(null, saveRunner);
        return saveRunner.getResult();
    }

    public static Link createLink(Composite parent, String text, SelectionListener listener) {
        Link link = new Link(parent, SWT.NONE);
        link.setText(text);
        link.addSelectionListener(listener);
        return link;
    }

    public static CellEditor createPropertyEditor(final IServiceLocator serviceLocator, Composite parent, DBPPropertySource source, DBPPropertyDescriptor property)
    {
        if (source == null) {
            return null;
        }
        final Object object = source.getEditableValue();
        if (!property.isEditable(object)) {
            return null;
        }
        CellEditor cellEditor = UIUtils.createCellEditor(parent, object, property);
        if (cellEditor != null) {
            final Control editorControl = cellEditor.getControl();
            UIUtils.addFocusTracker(serviceLocator, UIUtils.INLINE_WIDGET_EDITOR_ID, editorControl);
            editorControl.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(DisposeEvent e) {
                    UIUtils.removeFocusTracker(serviceLocator, editorControl);
                }
            });
        }
        return cellEditor;
    }

    public static CellEditor createCellEditor(Composite parent, Object object, DBPPropertyDescriptor property)
    {
        // List
        if (property instanceof IPropertyValueListProvider) {
            final IPropertyValueListProvider listProvider = (IPropertyValueListProvider) property;
            final Object[] items = listProvider.getPossibleValues(object);
            if (!ArrayUtils.isEmpty(items)) {
                final String[] strings = new String[items.length];
                for (int i = 0, itemsLength = items.length; i < itemsLength; i++) {
                    strings[i] = items[i] instanceof DBPNamedObject ? ((DBPNamedObject)items[i]).getName() : CommonUtils.toString(items[i]);
                }
                final CustomComboBoxCellEditor editor = new CustomComboBoxCellEditor(
                    parent,
                    strings,
                    SWT.DROP_DOWN | (listProvider.allowCustomValue() ? SWT.NONE : SWT.READ_ONLY));
                return editor;
            }
        }
        Class<?> propertyType = property.getDataType();
        if (propertyType == null || CharSequence.class.isAssignableFrom(propertyType)) {
            return new CustomTextCellEditor(parent);
        } else if (BeanUtils.isNumericType(propertyType)) {
            return new CustomNumberCellEditor(parent, propertyType);
        } else if (BeanUtils.isBooleanType(propertyType)) {
            return new CustomCheckboxCellEditor(parent);
            //return new CheckboxCellEditor(parent);
        } else if (propertyType.isEnum()) {
            final Object[] enumConstants = propertyType.getEnumConstants();
            final String[] strings = new String[enumConstants.length];
            for (int i = 0, itemsLength = enumConstants.length; i < itemsLength; i++) {
                strings[i] = ((Enum)enumConstants[i]).name();
            }
            return new CustomComboBoxCellEditor(
                parent,
                strings,
                SWT.DROP_DOWN | SWT.READ_ONLY);
        } else {
            log.warn("Unsupported property type: " + propertyType.getName());
            return null;
        }
    }

    public static boolean showDatabaseError(Shell shell, String title, String message, DBException error)
    {
        DBPDataSource dataSource = error.getDataSource();
        DBPErrorAssistant errorAssistant = DBUtils.getAdapter(DBPErrorAssistant.class, dataSource);
        if (errorAssistant != null) {
            DBPErrorAssistant.ErrorType errorType = ((DBPErrorAssistant) dataSource).discoverErrorType(error);
            switch (errorType) {
                case CONNECTION_LOST:
                    DataSourceInvalidateHandler.showConnectionLostDialog(shell, message, error);
                    return true;
                case DRIVER_CLASS_MISSING:
                    DriverEditDialog.showBadConfigDialog(shell, message, error);
                    return true;
            }
        }

        return false;
    }

    public static void postEvent(Control ownerControl, final Event event) {
        final Display display = ownerControl.getDisplay();
        display.asyncExec(new Runnable() {
            @Override
            public void run() {
                display.post(event);
            }
        });
    }

    public static void drawMessageOverControl(Control control, PaintEvent e, String message, int offset) {
        Rectangle bounds = control.getBounds();
        Point ext = e.gc.textExtent(message);
        e.gc.drawText(message, (bounds.width - ext.x) / 2, bounds.height / 3 + offset);
    }

    private static class SaveRunner implements Runnable {
        private final DBRProgressMonitor monitor;
        private final ISaveablePart saveable;
        private boolean result;

        private SaveRunner(DBRProgressMonitor monitor, ISaveablePart saveable)
        {
            this.monitor = monitor;
            this.saveable = saveable;
        }

        public boolean getResult()
        {
            return result;
        }

        @Override
        public void run()
        {
            int choice = -1;
            if (saveable instanceof ISaveablePart2) {
                choice = ((ISaveablePart2) saveable).promptToSaveOnClose();
            }
            if (choice == -1 || choice == ISaveablePart2.DEFAULT) {
                Shell shell;
                String saveableName;
                if (saveable instanceof IWorkbenchPart) {
                    shell = ((IWorkbenchPart) saveable).getSite().getShell();
                    saveableName = ((IWorkbenchPart) saveable).getTitle();
                } else {
                    shell = DBeaverUI.getActiveWorkbenchShell();
                    saveableName = CommonUtils.toString(saveable);
                }
                int confirmResult = ConfirmationDialog.showConfirmDialog(
                    shell,
                    DBeaverPreferences.CONFIRM_EDITOR_CLOSE,
                    ConfirmationDialog.QUESTION_WITH_CANCEL,
                    saveableName);
                switch (confirmResult) {
                    case IDialogConstants.YES_ID:
                        choice = ISaveablePart2.YES;
                        break;
                    case IDialogConstants.NO_ID:
                        choice = ISaveablePart2.NO;
                        break;
                    default:
                        choice = ISaveablePart2.CANCEL;
                        break;
                }
            }
            switch (choice) {
                case ISaveablePart2.YES: //yes
                    saveable.doSave(RuntimeUtils.getNestedMonitor(monitor));
                    result = !saveable.isDirty();
                    break;
                case ISaveablePart2.NO: //no
                    result = true;
                    break;
                case ISaveablePart2.CANCEL: //cancel
                default:
                    result = false;
                    break;
            }
        }
    }

    public static Color getConnectionColor(DBPConnectionConfiguration connectionInfo) {
        String rgbString = connectionInfo.getConnectionColor();
        if (CommonUtils.isEmpty(rgbString)) {
            rgbString = connectionInfo.getConnectionType().getColor();
        }
        if (CommonUtils.isEmpty(rgbString)) {
            return null;
        }
        Color connectionColor = DBeaverUI.getSharedTextColors().getColor(rgbString);
        if (connectionColor.getBlue() == 255 && connectionColor.getRed() == 255 && connectionColor.getGreen() == 255) {
            // For white color return just null to avoid explicit color set.
            // It is important for dark themes
            return null;
        }
        return connectionColor;
    }

    public static Color getConnectionTypeColor(DBPConnectionType connectionType) {
        String rgbString = connectionType.getColor();
        if (CommonUtils.isEmpty(rgbString)) {
            return null;
        }
        return DBeaverUI.getSharedTextColors().getColor(StringConverter.asRGB(rgbString));
    }

}
