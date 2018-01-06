/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui;

import org.eclipse.jface.action.*;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.IControlContentAdapter;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.services.IServiceLocator;
import org.eclipse.ui.swt.IFocusService;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.FindReplaceAction;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPConnectionType;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.ui.controls.*;
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
import java.util.*;

/**
 * UI Utils
 */
public class UIUtils {

    private static final Log log = Log.getLog(UIUtils.class);

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
            symbols.getMinusSign(), symbols.getZeroDigit(), symbols.getMonetaryDecimalSeparator(), '+', '.', ',' };
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

    public static void createToolBarSeparator(ToolBar toolBar, int style) {
        Label label = new Label(toolBar, SWT.NONE);
        label.setImage(DBeaverIcons.getImage(UIIcon.DRAG_HANDLE));
        new ToolItem(toolBar, SWT.SEPARATOR).setControl(label);
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
            final Rectangle clientArea = table.getBounds();
            if (clientArea.width > 0 && totalWidth > clientArea.width) {
                for (TableColumn column : columns) {
                    int colWidth = column.getWidth();
                    if (colWidth > totalWidth / 3) {
                        // If some columns are too big (more than 33% of total width)
                        // Then shrink them to 30%
                        column.setWidth(totalWidth / 3);
                        totalWidth -= colWidth;
                        totalWidth += column.getWidth();
                    }
                }
                int extraSpace = totalWidth - clientArea.width;

                GC gc = new GC(table);
                try {
                    for (TableColumn tc : columns) {
                        double ratio = (double) tc.getWidth() / totalWidth;
                        int newWidth = (int) (tc.getWidth() - extraSpace * ratio);
                        int minWidth = gc.stringExtent(tc.getText()).x;
                        minWidth += 5;
                        if (newWidth < minWidth) {
                            newWidth = minWidth;
                        }
                        tc.setWidth(newWidth);
                    }
                }
                finally {
                    gc.dispose();
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
            Rectangle clientArea = tree.getClientArea();
            if (clientArea.isEmpty()) {
                return;
            }
            int totalWidth = 0;
            final TreeColumn[] columns = tree.getColumns();
            for (TreeColumn column : columns) {
                column.pack();
                int colWidth = column.getWidth();
                if (colWidth > clientArea.width) {
                    // Too wide column - make it a bit narrower
                    colWidth = clientArea.width;
                    column.setWidth(colWidth);
                }
                totalWidth += colWidth;
            }
            if (fit) {
                int areaWidth = clientArea.width;
//                if (tree.getVerticalBar() != null) {
//                    areaWidth -= tree.getVerticalBar().getSize().x;
//                }
                if (totalWidth > areaWidth) {
                    GC gc = new GC(tree);
                    try {
                        int extraSpace = totalWidth - areaWidth;
                        for (TreeColumn tc : columns) {
                            double ratio = (double) tc.getWidth() / totalWidth;
                            int newWidth = (int) (tc.getWidth() - extraSpace * ratio);
                            int minWidth = gc.stringExtent(tc.getText()).x;
                            minWidth += 5;
                            if (newWidth < minWidth) {
                                newWidth = minWidth;
                            }
                            tc.setWidth((int) newWidth);
                        }
                    } finally {
                        gc.dispose();
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
                Shell activeShell = shell != null ? shell : DBeaverUI.getActiveWorkbenchShell();
                MessageBox messageBox = new MessageBox(activeShell, messageType | SWT.OK);
                messageBox.setMessage(info);
                messageBox.setText(title);
                messageBox.open();
            }
        };
        DBeaverUI.syncExec(runnable);
    }

    public static boolean confirmAction(final String title, final String question)
    {
        return confirmAction(null, title, question);
    }

    public static boolean confirmAction(final Shell shell, final String title, final String question)
    {
        return new UIConfirmation() {
            @Override
            public Boolean runTask() {
                Shell activeShell = shell != null ? shell : DBeaverUI.getActiveWorkbenchShell();
                MessageBox messageBox = new MessageBox(activeShell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
                messageBox.setMessage(question);
                messageBox.setText(title);
                int response = messageBox.open();
                return (response == SWT.YES);
            }
        }.confirm();
    }

    public static int getFontHeight(Control control) {
        return getFontHeight(control.getFont());
    }

    public static int getFontHeight(Font font) {
        FontData[] fontData = font.getFontData();
        if (fontData.length == 0) {
            return 20;
        }
        return fontData[0].getHeight();
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
        // TODO: Should we make it right-aligned? Looks good but not in Eclipse-style
        textLabel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_CENTER /*| GridData.HORIZONTAL_ALIGN_END*/));
        return textLabel;
    }

    public static Label createLabel(Composite parent, String label)
    {
        Label textLabel = new Label(parent, SWT.NONE);
        textLabel.setText(label);

        return textLabel;
    }

    public static Label createLabel(Composite parent, @NotNull DBPImage image)
    {
        Label imageLabel = new Label(parent, SWT.NONE);
        imageLabel.setImage(DBeaverIcons.getImage(image));

        return imageLabel;
    }


    public static CLabel createInfoLabel(Composite parent, String text) {
        CLabel tipLabel = new CLabel(parent, SWT.NONE);
        tipLabel.setImage(JFaceResources.getImage(org.eclipse.jface.dialogs.Dialog.DLG_IMG_MESSAGE_INFO));
        tipLabel.setText(text);
        return tipLabel;
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
        fixReadonlyTextBackground(text);
        if (value != null) {
            text.setText(value);
        }

        if (layoutData != null) {
            text.setLayoutData(layoutData);
        }

        return text;
    }

    @NotNull
    public static Spinner createLabelSpinner(@NotNull Composite parent, @NotNull String label, @Nullable String tooltip, int value, int minimum, int maximum) {
        final Label l = createControlLabel(parent, label);
        if (tooltip != null) {
            l.setToolTipText(tooltip);
        }

        return createSpinner(parent, tooltip, value, minimum, maximum);
    }

    @NotNull
    public static Spinner createSpinner(Composite parent, String tooltip, int value, int minimum, int maximum) {
        Spinner spinner = new Spinner(parent, SWT.BORDER);
        spinner.setMinimum(minimum);
        spinner.setMaximum(maximum);
        spinner.setSelection(value);
        if (tooltip != null) {
            spinner.setToolTipText(tooltip);
        }

        return spinner;
    }

    @NotNull
    public static Spinner createLabelSpinner(@NotNull Composite parent, @NotNull String label, int value, int minimum, int maximum)
    {
        return createLabelSpinner(parent, label, null, value, minimum, maximum);
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

    public static Button createCheckbox(Composite parent, String label, String tooltip, boolean checked, int hSpan) {
        Button checkbox = createCheckbox(parent, label, checked);
        if (tooltip != null) {
            checkbox.setToolTipText(tooltip);
        }
        if (hSpan > 1) {
            GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            gd.horizontalSpan = hSpan;
            checkbox.setLayoutData(gd);
        }
        return checkbox;
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
        return createLabelCombo(parent, label, null, style);
    }

    public static Combo createLabelCombo(Composite parent, String label, String tooltip, int style)
    {
        Label labelControl = createControlLabel(parent, label);
        if (tooltip != null) {
            labelControl.setToolTipText(tooltip);
        }

        final Combo combo = new Combo(parent, style);
        combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        if (tooltip != null) {
            combo.setToolTipText(tooltip);
        }

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

    public static void updateContributionItems(IContributionManager manager) {
        for (IContributionItem item : manager.getItems()) {
            item.update();
        }
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

    public static void setGridSpan(Control control, int horizontalSpan, int verticalSpan) {
        GridData gd;
        final Object layoutData = control.getLayoutData();
        if (layoutData == null) {
            if (control.getParent().getLayout() instanceof GridLayout) {
                gd = new GridData();
                control.setLayoutData(gd);
            } else {
                log.debug("Can't set grid span for layout: " + control.getParent().getLayout());
                return;
            }
        } else if (layoutData instanceof GridData) {
            gd = (GridData) layoutData;
        } else {
            log.debug("Can't set grid span for non-grid layout: " + layoutData.getClass().getName());
            return;
        }
        gd.horizontalSpan = horizontalSpan;
        gd.verticalSpan = verticalSpan;
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

//    public static Combo createEncodingCombo(Composite parent, String curCharset)
//    {
//
//    }

    public static Combo createEncodingCombo(Composite parent, @Nullable String curCharset)
    {
        Combo encodingCombo = new Combo(parent, SWT.DROP_DOWN);
        encodingCombo.setVisibleItemCount(30);
        SortedMap<String, Charset> charsetMap = Charset.availableCharsets();
        int index = 0;
        int defIndex = -1;
        for (String csName : charsetMap.keySet()) {
            Charset charset = charsetMap.get(csName);
            encodingCombo.add(charset.displayName());
            if (curCharset != null) {
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
            }
            index++;
        }
        if (defIndex >= 0) {
            encodingCombo.select(defIndex);
        } else if (curCharset != null) {
            log.warn("Charset '" + curCharset + "' is not recognized"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return encodingCombo;
    }

    @NotNull
    public static SashForm createPartDivider(final IWorkbenchPart workbenchPart, Composite parent, int style)
    {
        final CustomSashForm sash = new CustomSashForm(parent, style);

        return sash;
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
        return createPushButton(parent, label, image, null);
    }

    @NotNull
    public static Button createPushButton(@NotNull Composite parent, @Nullable String label, @Nullable Image image, @Nullable SelectionListener selectionListener)
    {
        Button button = new Button(parent, SWT.PUSH);
        if (label != null) {
            button.setText(label);
        }
        if (image != null) {
            button.setImage(image);
        }
        if (selectionListener != null) {
            button.addSelectionListener(selectionListener);
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

    public static void showPreferencesFor(Shell shell, Object element, String ... defPageID)
    {
        PreferenceDialog propDialog;
        if (element == null) {
            propDialog = PreferencesUtil.createPreferenceDialogOn(shell, defPageID[0], defPageID, null, PreferencesUtil.OPTION_NONE);
        } else {
            propDialog = PreferencesUtil.createPropertyDialogOn(shell, element, defPageID[0], null, null, PreferencesUtil.OPTION_NONE);
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

    public static void addDefaultEditActionsSupport(final IServiceLocator site, final Control control) {
        UIUtils.addFocusTracker(site, UIUtils.INLINE_WIDGET_EDITOR_ID, control);
        control.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                // Unregister from focus service
                UIUtils.removeFocusTracker(site, control);
            }
        });
    }


    @NotNull
    public static IDialogSettings getDialogSettings(@NotNull String dialogId)
    {
        IDialogSettings workbenchSettings = DBeaverActivator.getInstance().getDialogSettings();
        return getSettingsSection(workbenchSettings, dialogId);
    }

    @NotNull
    public static IDialogSettings getSettingsSection(@NotNull IDialogSettings parent, @NotNull String sectionId)
    {
        IDialogSettings section = parent.getSection(sectionId);
        if (section == null) {
            section = parent.addNewSection(sectionId);
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
        Collection<?> contextIds = DBeaverUI.getActiveWorkbenchWindow().getService(IContextService.class).getActiveContextIds();
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

    public static void enableWithChildren(Control control, boolean enable)
    {
        control.setEnabled(enable);
        if (control instanceof Composite) {
            for (Control child : ((Composite)control).getChildren()) {
                if (child instanceof Composite) {
                    enableWithChildren(child, enable);
                } else {
                    child.setEnabled(enable);
                }
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

    public static void disposeControlOnItemDispose(final CTabItem tabItem) {
        tabItem.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                final Control control = tabItem.getControl();
                if (!control.isDisposed()) {
                    control.dispose();
                }
            }
        });
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

    public static Link createLink(Composite parent, String text, SelectionListener listener) {
        Link link = new Link(parent, SWT.NONE);
        link.setText(text);
        link.addSelectionListener(listener);
        return link;
    }

    public static CellEditor createPropertyEditor(final IServiceLocator serviceLocator, Composite parent, DBPPropertySource source, DBPPropertyDescriptor property, int style)
    {
        if (source == null) {
            return null;
        }
        final Object object = source.getEditableValue();
        if (!property.isEditable(object)) {
            return null;
        }
        CellEditor cellEditor = UIUtils.createCellEditor(parent, object, property, style);
        if (cellEditor != null) {
            final Control editorControl = cellEditor.getControl();
            addDefaultEditActionsSupport(serviceLocator, editorControl);
        }
        return cellEditor;
    }

    public static CellEditor createCellEditor(Composite parent, Object object, DBPPropertyDescriptor property, int style)
    {
        // List
        if (property instanceof IPropertyValueListProvider) {
            final IPropertyValueListProvider listProvider = (IPropertyValueListProvider) property;
            final Object[] items = listProvider.getPossibleValues(object);
            if (items != null) {
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
            return new CustomCheckboxCellEditor(parent, style);
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

    public static void postEvent(Control ownerControl, final Event event) {
        final Display display = ownerControl.getDisplay();
        DBeaverUI.asyncExec(new Runnable() {
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

    public static boolean launchProgram(String path)
    {
        return Program.launch(path);
    }

    public static void fillDefaultStyledTextContextMenu(final StyledText text) {
        MenuManager menuMgr = new MenuManager();
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager)
            {
                UIUtils.fillDefaultStyledTextContextMenu(manager, text);
            }
        });
        menuMgr.setRemoveAllWhenShown(true);
        text.setMenu(menuMgr.createContextMenu(text));
    }

    public static void fillDefaultStyledTextContextMenu(IMenuManager menu, final StyledText text) {
        final Point selectionRange = text.getSelectionRange();
        menu.add(new StyledTextAction(IWorkbenchCommandConstants.EDIT_COPY, selectionRange.y > 0, text, ST.COPY));
        menu.add(new StyledTextAction(IWorkbenchCommandConstants.EDIT_PASTE, text.getEditable(), text, ST.PASTE));
        menu.add(new StyledTextAction(IWorkbenchCommandConstants.EDIT_CUT, selectionRange.y > 0, text, ST.CUT));
        menu.add(new StyledTextAction(IWorkbenchCommandConstants.EDIT_SELECT_ALL, true, text, ST.SELECT_ALL));
        IFindReplaceTarget stFindReplaceTarget = new StyledTextFindReplaceTarget(text);
        menu.add(new FindReplaceAction(
            ResourceBundle.getBundle("org.eclipse.ui.texteditor.ConstructedEditorMessages"),
            "Editor.FindReplace.",
            text.getShell(),
            stFindReplaceTarget));
        menu.add(new GroupMarker("styled_text_additions"));
    }

    public static void fillDefaultTableContextMenu(IMenuManager menu, final Table table) {
        menu.add(new Action(CoreMessages.controls_itemlist_action_copy) {
            @Override
            public void run() {
                StringBuilder text = new StringBuilder();
                for (TableItem item : table.getSelection()) {
                    if (text.length() > 0) text.append("\n");
                    text.append(item.getText());
                }
                UIUtils.setClipboardContents(table.getDisplay(), TextTransfer.getInstance(), text.toString());
            }
        });
    }

    public static void addFileOpenOverlay(Text text, SelectionListener listener) {
        final Image browseImage = DBeaverIcons.getImage(DBIcon.TREE_FOLDER);
        final Rectangle iconBounds = browseImage.getBounds();
        text.addPaintListener(new PaintListener() {
            @Override
            public void paintControl(PaintEvent e) {
                final Rectangle bounds = ((Text) e.widget).getBounds();
                e.gc.drawImage(browseImage, bounds.width - iconBounds.width - 2, 0);
            }
        });
    }

    public static Combo createDelimiterCombo(Composite group, String label, String[] options, String defDelimiter, boolean multiDelims) {
        createControlLabel(group, label);
        Combo combo = new Combo(group, SWT.BORDER | SWT.DROP_DOWN);
        combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        for (String option : options) {
            combo.add(CommonUtils.escapeDisplayString(option));
        }
        if (!multiDelims) {
            if (!ArrayUtils.contains(options, defDelimiter)) {
                combo.add(CommonUtils.escapeDisplayString(defDelimiter));
            }
            String[] items = combo.getItems();
            for (int i = 0, itemsLength = items.length; i < itemsLength; i++) {
                String delim = CommonUtils.unescapeDisplayString(items[i]);
                if (delim.equals(defDelimiter)) {
                    combo.select(i);
                    break;
                }
            }
        } else {
            combo.setText(CommonUtils.escapeDisplayString(defDelimiter));
        }
        return combo;
    }

    private static class StyledTextAction extends Action {
        private final StyledText styledText;
        private final int action;
        public StyledTextAction(String actionId, boolean enabled, StyledText styledText, int action) {
            super(ActionUtils.findCommandName(actionId));
            this.setActionDefinitionId(actionId);
            this.setEnabled(enabled);
            this.styledText = styledText;
            this.action = action;
        }

        @Override
        public void run() {
            styledText.invokeAction(action);
        }
    }

    @Nullable
    public static Color getSharedColor(@Nullable String rgbString) {
        if (CommonUtils.isEmpty(rgbString)) {
            return null;
        }
        return DBeaverUI.getSharedTextColors().getColor(rgbString);
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

    public static Shell createCenteredShell(Shell parent) {

        final Rectangle bounds = parent.getBounds();
        final int x = bounds.x + bounds.width / 2 - 120;
        final int y = bounds.y + bounds.height / 2 - 170;

        final Shell shell = new Shell( parent );

        shell.setBounds( x, y, 0, 0 );

        return shell;
    }

    public static Image getShardImage(String id) {
        return PlatformUI.getWorkbench().getSharedImages().getImage(id);
    }

    public static ImageDescriptor getShardImageDescriptor(String id) {
        return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(id);
    }

    public static void installContentProposal(Control control, IControlContentAdapter contentAdapter, IContentProposalProvider provider) {
        installContentProposal(control, contentAdapter, provider, false, true);
    }

    public static void installContentProposal(Control control, IControlContentAdapter contentAdapter, IContentProposalProvider provider, boolean autoActivation, boolean insertAfter) {
        try {
            KeyStroke keyStroke = autoActivation ? null : KeyStroke.getInstance("Ctrl+Space");
            final ContentProposalAdapter proposalAdapter = new ContentProposalAdapter(
                control,
                contentAdapter,
                provider,
                keyStroke,
                autoActivation ? ".abcdefghijklmnopqrstuvwxyz_$(".toCharArray() : ".(".toCharArray());
            proposalAdapter.setProposalAcceptanceStyle(insertAfter ? ContentProposalAdapter.PROPOSAL_INSERT : ContentProposalAdapter.PROPOSAL_REPLACE);
            proposalAdapter.setPopupSize(new Point(300, 200));
        } catch (ParseException e) {
            log.error("Error installing filters content assistant");
        }
    }

    public static void setContentProposalToolTip(Control control, String toolTip, String ... variables) {
        StringBuilder varsTip = new StringBuilder();
        for (String var : variables) {
            if (varsTip.length() > 0) varsTip.append(", ");
            varsTip.append(GeneralUtils.variablePattern(var));
        }
        varsTip.append(".");
        control.setToolTipText(toolTip + ".\nAllowed variables: " + varsTip);

    }

    public static CoolItem createCoolItem(CoolBar coolBar, Control control) {
        CoolItem item = new CoolItem(coolBar, SWT.NONE);
        item.setControl(control);
        Point size = control.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        Point preferred = item.computeSize(size.x, size.y);
        item.setPreferredSize(preferred);
        return item;
    }

    public static void resizeShell(Shell shell) {
        Point shellSize = shell.getSize();
        Point compSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        compSize.y += 20;
        if (shellSize.y < compSize.y) {
            shell.setSize(compSize);
            shell.layout(true);
        }
    }

    public static void waitJobCompletion(AbstractJob job) {
        // Wait until job finished
        Display display = Display.getCurrent();
        while (!job.isFinished()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        display.update();
    }

    public static void fixReadonlyTextBackground(Text textField) {
        if ((textField.getStyle() & SWT.READ_ONLY) == SWT.READ_ONLY) {
            // Do nothing because in E4.6 there is no good solution: https://bugs.eclipse.org/bugs/show_bug.cgi?id=340889
            //textField.setBackground(textField.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        } else {
            textField.setBackground(null);
        }
    }

    public static ColorRegistry getColorRegistry() {
        return PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry();
    }

    public static Color getGlobalColor(String colorName) {
        return getColorRegistry().get(colorName);
    }

}
