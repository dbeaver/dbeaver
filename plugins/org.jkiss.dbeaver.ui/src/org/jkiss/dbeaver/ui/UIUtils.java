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
package org.jkiss.dbeaver.ui;

import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.ui.model.application.ui.menu.MHandledItem;
import org.eclipse.e4.ui.workbench.renderers.swt.HandledContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.ui.services.IServiceLocator;
import org.eclipse.ui.swt.IFocusService;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPConnectionType;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.runtime.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.DummyRunnableContext;
import org.jkiss.dbeaver.runtime.RunnableContextDelegate;
import org.jkiss.dbeaver.ui.contentassist.ContentAssistUtils;
import org.jkiss.dbeaver.ui.contentassist.SmartTextContentAdapter;
import org.jkiss.dbeaver.ui.contentassist.StringContentProposalProvider;
import org.jkiss.dbeaver.ui.controls.CustomSashForm;
import org.jkiss.dbeaver.ui.dialogs.EditTextDialog;
import org.jkiss.dbeaver.ui.dialogs.MessageBoxBuilder;
import org.jkiss.dbeaver.ui.dialogs.Reply;
import org.jkiss.dbeaver.ui.internal.UIActivator;
import org.jkiss.dbeaver.ui.internal.UIMessages;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.SortedMap;

/**
 * UI Utils
 */
public class UIUtils {
    private static final Log log = Log.getLog(UIUtils.class);

    private static final String INLINE_WIDGET_EDITOR_ID = "org.jkiss.dbeaver.ui.InlineWidgetEditor";
    private static final Color COLOR_BLACK = new Color(null, 0, 0, 0);
    public static final Color COLOR_WHITE = new Color(null, 255, 255, 255);
    public static final Color COLOR_GREEN_CONTRAST = new Color(null, 23, 135, 58);
    public static final Color COLOR_VALIDATION_ERROR = new Color(255, 220, 220);
    
    private static final Color COLOR_WHITE_DARK = new Color(null, 192, 192, 192);
    private static final SharedTextColors SHARED_TEXT_COLORS = new SharedTextColors();
    private static final SharedFonts SHARED_FONTS = new SharedFonts();
    private static final String MAX_LONG_STRING = String.valueOf(Long.MAX_VALUE);

    public static VerifyListener getIntegerVerifyListener(Locale locale)
    {
        final DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);
        return e -> {
            for (int i = 0; i < e.text.length(); i++) {
                char ch = e.text.charAt(i);
                if (!Character.isDigit(ch) && ch != symbols.getMinusSign() && ch != symbols.getGroupingSeparator()) {
                    e.doit = false;
                    return;
                }
            }
            e.doit = true;
        };
    }

    public static VerifyListener getNumberVerifyListener(Locale locale)
    {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);
        final char[] allowedChars = new char[] { symbols.getDecimalSeparator(), symbols.getGroupingSeparator(),
            symbols.getMinusSign(), symbols.getZeroDigit(), symbols.getMonetaryDecimalSeparator(), '+', '.', ',' };
        final String exponentSeparator = symbols.getExponentSeparator();
        return e -> {
            for (int i = 0; i < e.text.length(); i++) {
                char ch = e.text.charAt(i);
                if (!Character.isDigit(ch) && !ArrayUtils.contains(allowedChars, ch) && exponentSeparator.indexOf(ch) == -1) {
                    e.doit = false;
                    return;
                }
            }
            e.doit = true;
        };
    }

    public static VerifyListener getUnsignedLongOrEmptyTextVerifyListener(Text text) {
        return e -> {
            if (e.text.isEmpty()) {
                e.doit = true;
                return;
            }
            for (int i = 0; i < e.text.length(); i++) {
                if (!Character.isDigit(e.text.charAt(i))) {
                    e.doit = false;
                    return;
                }
            }
            String newText = text.getText().substring(0, e.start) + e.text + text.getText().substring(e.end);
            if (newText.length() < MAX_LONG_STRING.length()) {
                e.doit = true;
                return;
            }
            if (newText.length() > MAX_LONG_STRING.length()) {
                e.doit = false;
                return;
            }
            e.doit = newText.compareTo(MAX_LONG_STRING) <= 0;
        };
    }

    public static void createToolBarSeparator(Composite toolBar, int style) {
        Label label = new Label(toolBar, SWT.NONE);
        label.setImage(DBeaverIcons.getImage((style & SWT.HORIZONTAL) == SWT.HORIZONTAL ? UIIcon.SEPARATOR_H : UIIcon.SEPARATOR_V));
    }

    public static void createLabelSeparator(Composite toolBar, int style) {
        Label label = new Label(toolBar, SWT.SEPARATOR | style);
        label.setLayoutData(new GridData(style == SWT.HORIZONTAL ? GridData.FILL_HORIZONTAL : GridData.FILL_VERTICAL));
    }

    public static void createToolBarSeparator(ToolBar toolBar, int style) {
        Label label = new Label(toolBar, SWT.NONE);
        label.setImage(DBeaverIcons.getImage((style & SWT.HORIZONTAL) == SWT.HORIZONTAL ? UIIcon.SEPARATOR_H : UIIcon.SEPARATOR_V));
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

    public static void executeOnResize(Control control, Runnable runnable) {
        control.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                control.removeControlListener(this);
                runnable.run();
            }
        });
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
                if (totalWidth < clientArea.width) {
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
                    } finally {
                        gc.dispose();
                    }
                }
            }
            if (fit && totalWidth < clientArea.width) {
                int sbWidth = table.getBorderWidth() * 2;
                if (table.getVerticalBar() != null) {
                    sbWidth = sbWidth + table.getVerticalBar().getSize().x;
                }
                if (columns.length > 0) {
                    float extraSpace = (clientArea.width - totalWidth - sbWidth) / columns.length - 1;
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
            final TreeColumn[] columns = tree.getColumns();
            for (TreeColumn column : columns) {
                column.pack();
            }

            Rectangle clientArea = tree.getClientArea();
            if (clientArea.isEmpty()) {
                return;
            }
            int totalWidth = 0;
            for (TreeColumn column : columns) {
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
                            tc.setWidth(newWidth);
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

    public static TableItem getPreviousTableItem(Table table, TableItem item) {
        TableItem[] items = table.getItems();
        for (int i = 1; i < items.length; i++) {
            if (items[i] == item) {
                return items[i - 1];
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

    public static void showMessageBox(final Shell shell, final String title, final String info, final int messageType) {
        DBPImage icon = null;
        if (messageType == SWT.ICON_ERROR) {
            icon = DBIcon.STATUS_ERROR;
        } else if (messageType == SWT.ICON_WARNING) {
            icon = DBIcon.STATUS_WARNING;
        } else if (messageType == SWT.ICON_QUESTION) {
            icon = DBIcon.STATUS_QUESTION;
        } else if (messageType == SWT.ICON_INFORMATION) {
            icon = DBIcon.STATUS_INFO;
        }

        Runnable messageBoxRunnable;
        if (icon != null)  {
            final DBPImage finalIcon = icon;
            messageBoxRunnable = () -> MessageBoxBuilder.builder(shell != null ? shell : getActiveWorkbenchShell())
                .setTitle(title)
                .setMessage(info)
                .setReplies(Reply.OK)
                .setDefaultReply(Reply.OK)
                .setPrimaryImage(finalIcon)
                .showMessageBox();
        } else {
            //show legacy message box
            messageBoxRunnable = () -> {
                Shell activeShell = shell != null ? shell : getActiveWorkbenchShell();
                MessageBox messageBox = new MessageBox(activeShell, messageType | SWT.OK);
                messageBox.setMessage(info);
                messageBox.setText(title);
                messageBox.open();
            };
        }

        syncExec(messageBoxRunnable);
    }

    public static boolean confirmAction(final String title, final String question) {
        return confirmAction(null, title, question);
    }

    public static boolean confirmAction(@Nullable Shell shell, final String title, final String question) {
        return confirmAction(shell, title, question, DBIcon.STATUS_QUESTION);
    }

    public static boolean confirmAction(@Nullable Shell shell, String title, String message, @NotNull DBPImage image) {
        final Reply[] reply = {null};
        syncExec(() -> reply[0] = MessageBoxBuilder.builder(shell != null ? shell : getActiveWorkbenchShell())
            .setTitle(title)
            .setMessage(message)
            .setReplies(Reply.YES, Reply.NO)
            .setDefaultReply(Reply.NO)
            .setPrimaryImage(image)
            .showMessageBox()
        );

        return reply[0] == Reply.YES;
    }

    /**
     * Confirm action with custom labels
     *
     */
    public static boolean confirmAction(@Nullable Shell shell, String title, String message, @NotNull DBPImage image, String[] buttons) {
        final Reply[] reply = { null };
        syncExec(() -> reply[0] = MessageBoxBuilder.builder(shell != null ? shell : getActiveWorkbenchShell())
            .setTitle(title)
            .setMessage(message)
            .setLabels(buttons)
            .setDefaultReply(Reply.NO)
            .setPrimaryImage(image)
            .setDefaultFocus(buttons.length - 1)
            .showMessageBox());
        return reply[0] == Reply.OK;
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

    public static int getTextHeight(@NotNull Control control) {
        return getTextSize(control, "X").y;
    }

    @NotNull
    public static Point getTextSize(@NotNull Control control, @NotNull String text) {
        GC gc = new GC(control);
        try {
            return gc.textExtent(text);
        } finally {
            gc.dispose();
        }
    }

    public static Font makeBoldFont(Font normalFont)
    {
        return modifyFont(normalFont, SWT.BOLD);
    }

    @NotNull
    public static Font modifyFont(@NotNull Font normalFont, int style) {
        final FontData[] data = normalFont.getFontData();
        for (FontData fd : data) {
            fd.setStyle(fd.getStyle() | style);
        }
        return new Font(normalFont.getDevice(), data);
    }


    /**
     * Modifies the size of the given font by applying the specified modifier to the current font size.
     *
     * @param normalFont the original font whose size needs to be modified.
     * @param modifier the amount by which to modify the font size. Positive values increase the size,
     *                 and negative values decrease it.
     * @return a new {@link Font} object with the modified size.
     */
    @NotNull
    public static Font modifyFontSize(@NotNull Font normalFont, int modifier) {
        final FontData[] data = normalFont.getFontData();
        for (FontData fd : data) {
            fd.setHeight(fd.getHeight() + modifier);
        }
        return new Font(normalFont.getDevice(), data);
    }

    public static Group createControlGroup(Composite parent, String label, int columns, int layoutStyle, int widthHint)
    {
        Group group = new Group(parent, SWT.NONE);
        group.setText(label);

        if (parent.getLayout() instanceof GridLayout) {
            GridData gd = new GridData(layoutStyle);
            if (widthHint > 0) {
                gd.widthHint = widthHint;
            }
            group.setLayoutData(gd);
        }

        GridLayout gl = new GridLayout(columns, false);
        group.setLayout(gl);

        return group;
    }

    public static Label createControlLabel(Composite parent, String label) {
        return createControlLabel(parent, label, 1);
    }

    public static Label createControlLabel(Composite parent, String label, int hSpan) {
        Label textLabel = new Label(parent, SWT.NONE);
        textLabel.setText(label + ": "); //$NON-NLS-1$
        // Vert align center. Because height of single line control may differ from label height. This makes form ugly.
        // For multiline texts we need to set vert align manually.
        GridData gd = new GridData(GridData.VERTICAL_ALIGN_CENTER /*| GridData.HORIZONTAL_ALIGN_END*/);
        gd.horizontalSpan = hSpan;
        textLabel.setLayoutData(gd);
        return textLabel;
    }

    public static Label createLabel(Composite parent, String label) {
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

    @NotNull
    public static Control createInfoLabel(@NotNull Composite parent, @NotNull String text) {
        return createInfoLabel(parent, text, null);
    }

    @NotNull
    public static Control createInfoLabel(@NotNull Composite parent, @NotNull String text, @Nullable Runnable listener) {
        return createInfoLabel(parent, text, SWT.NONE, 1, listener);
    }

    @NotNull
    public static Control createInfoLabel(@NotNull Composite parent, @NotNull String text, int gridStyle, int hSpan) {
        return createInfoLabel(parent, text, gridStyle, hSpan, null);
    }

    @NotNull
    public static Control createWarningLabel(
        @NotNull Composite parent,
        @NotNull String text,
        int gridStyle,
        int hSpan
    ) {
        return createInfoLabel(parent, text, gridStyle, hSpan, null, DBeaverIcons.getImage(DBIcon.SMALL_WARNING));
    }

    @NotNull
    public static Control createInfoLabel(
        @NotNull Composite parent,
        @NotNull String text,
        int gridStyle,
        int hSpan,
        @Nullable Runnable callback
    ) {
        return createInfoLabel(parent, text, gridStyle, hSpan, callback, DBeaverIcons.getImage(DBIcon.SMALL_INFO));
    }

    @NotNull
    public static Control createInfoLabel(
        @NotNull Composite parent,
        @NotNull String text,
        int gridStyle,
        int hSpan,
        @Nullable Runnable callback,
        @NotNull Image image
    ) {
        final Control control;

        if (callback == null) {
            final CLabel label = new CLabel(parent, SWT.NONE);
            label.setImage(image);
            label.setText(text);
            control = label;
        } else {
            control = createInfoLink(parent, "<a href=\"#\">" + text + "</a>", callback).getParent();
        }

        if (gridStyle != SWT.NONE || hSpan > 1) {
            final GridData gd = new GridData(gridStyle);
            gd.horizontalSpan = hSpan;
            control.setLayoutData(gd);
        }

        return control;
    }

    @NotNull
    public static Link createInfoLink(@NotNull Composite parent, @NotNull String text, @NotNull Runnable callback) {
        return createInfoLink(parent, text, callback, SWT.NONE, 1, SWT.DEFAULT);
    }

    @NotNull
    public static Link createInfoLink(
        @NotNull Composite parent,
        @NotNull String text,
        @NotNull Runnable callback,
        int style,
        int colsSpan,
        int widthHint
    ) {
        final Composite composite = new Composite(parent, style);
        composite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).create());
        composite.setLayoutData(GridDataFactory.fillDefaults()
            .span(colsSpan, 1)
            .hint(widthHint, SWT.DEFAULT)
            .grab(true, false).create());
        final Label imageLabel = new Label(composite, SWT.NONE);
        imageLabel.setImage(DBeaverIcons.getImage(DBIcon.SMALL_INFO));
        imageLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));

        final Link link = new Link(composite, SWT.NONE);
        link.setText(text);
        link.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> callback.run()));
        link.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

        return link;
    }

    public static Text createLabelText(Composite parent, String label, String value) {
        return createLabelText(parent, label, value, SWT.BORDER);
    }

    public static Text createLabelText(Composite parent, String label, String value, int style) {
        return createLabelText(parent, label, value, style, new GridData(GridData.FILL_HORIZONTAL));
    }

    @NotNull
    public static Text createLabelText(@NotNull Composite parent, @NotNull String label, @Nullable String value, int style, @Nullable Object layoutData) {
        Label controlLabel = createControlLabel(parent, label);

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
    public static Text createLabelTextAdvanced(@NotNull Composite parent, @NotNull String label, @Nullable String value, int style) {
        Label controlLabel = createControlLabel(parent, label);
        Composite panel = createComposite(parent, 2);
        panel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Text text = new Text(panel, style);
        text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        fixReadonlyTextBackground(text);
        if (value != null) {
            text.setText(value);
        }
        ToolBar editTB = new ToolBar(panel, SWT.HORIZONTAL);
        ToolItem editButton = new ToolItem(editTB, SWT.DOWN);
        //Button editButton = new Button(panel, SWT.DOWN);
        //editButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        //editButton.setText("...");
        editButton.setImage(DBeaverIcons.getImage(UIIcon.EDIT)); //$NON-NLS-1$
        editButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String newText = EditTextDialog.editText(parent.getShell(), label, text.getText());
                if (newText != null) {
                    text.setText(newText);
                }
            }
        });
        editTB.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

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

    public static Button createCheckbox(Composite parent, boolean checked)
    {
        final Button button = new Button(parent, SWT.CHECK);
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

    public static ToolItem createToolItem(ToolBar parent, String text, DBPImage icon, SelectionListener selectionListener) {
        return createToolItem(parent, text, icon != null ? DBeaverIcons.getImage(icon) : null, selectionListener);
    }

    public static ToolItem createToolItem(ToolBar parent, String title, String text, DBPImage icon, SelectionListener selectionListener) {
        ToolItem toolItem = createToolItem(parent, text, icon != null ? DBeaverIcons.getImage(icon) : null, selectionListener);
        if (title != null) {
            toolItem.setText(title);
        }
        return toolItem;
    }

    public static ToolItem createToolItem(ToolBar parent, String text, Image icon, SelectionListener selectionListener) {
        ToolItem button = new ToolItem(parent, SWT.PUSH);
        button.setToolTipText(text);
        if (icon != null) {
            button.setImage(icon);
        }
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
    public static Shell getActiveShell() {
        final Display display = Display.getCurrent();
        final Shell activeShell = display.getActiveShell();
        if (activeShell != null) {
            return activeShell;
        }
        final Shell[] shells = display.getShells();
        for (Shell shell : shells) {
            if (shell.isVisible()) {
                return shell;
            }
        }
        return shells.length > 0 ? shells[0] : null;
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

    public static Composite createComposite(@NotNull Composite parent, int columns) {
        Composite ph = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(columns, false);
        gl.marginWidth = 0;
        gl.marginHeight = 0;
        ph.setLayout(gl);
        return ph;
    }

    /**
     * Creates {@link ScrolledComposite} from the {@link Composite}
     *
     * @param parent composite parent
     * @param style composite style
     * @return ScrolledComposite
     */
    @NotNull
    public static ScrolledComposite createScrolledComposite(@NotNull Composite parent, int style) {
        ScrolledComposite scrolledComposite = new ScrolledComposite(parent, style);
        scrolledComposite.setLayout(new GridLayout(1, false));
        scrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        return scrolledComposite;
    }

    /**
     * Configures created composite to detect resize and be appropriately sized with its contents
     *
     * @param scrolledComposite composite to configure
     * @param content it's contents
     */
    public static void configureScrolledComposite(@NotNull ScrolledComposite scrolledComposite, @NotNull Control content) {
        scrolledComposite.setContent(content);
        scrolledComposite.setExpandHorizontal(true);
        scrolledComposite.setExpandVertical(true);
        scrolledComposite.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                Rectangle area = scrolledComposite.getClientArea();
                Point size = content.computeSize(
                    (scrolledComposite.getStyle() & SWT.HORIZONTAL) != 0 ? SWT.DEFAULT : area.width,
                    (scrolledComposite.getStyle() & SWT.VERTICAL) != 0 ? SWT.DEFAULT : area.height
                );

                content.setSize(size);
                scrolledComposite.setMinSize(size);
            }
        });
    }

    public static Composite createPlaceholder(@NotNull Composite parent, int columns, int spacing) {
        Composite ph = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(columns, false);
        gl.verticalSpacing = spacing;
        gl.horizontalSpacing = spacing;
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        ph.setLayout(gl);
        return ph;
    }

    public static Composite createFormPlaceholder(Composite parent, int columns, int hSpan)
    {
        Composite ph = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(columns, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        ph.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = hSpan;
        ph.setLayoutData(gd);
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

    public static Label createHorizontalLine(Composite parent) {
        return createHorizontalLine(parent, 1, 0);
    }

    public static Label createHorizontalLine(Composite parent, int hSpan, int vIndent) {
        Label horizontalLine = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridData gd = new GridData(GridData.FILL, GridData.FILL, true, false, 1, 1);
        gd.horizontalSpan = hSpan;
        gd.verticalIndent = vIndent;
        horizontalLine.setLayoutData(gd);
        return horizontalLine;
    }

    public static Label createVerticalLine(Composite parent) {
        Label horizontalLine = new Label(parent, SWT.SEPARATOR | SWT.VERTICAL);
        if (parent.getLayout() instanceof GridLayout) {
            GridData gd = new GridData(GridData.FILL, GridData.FILL, false, true, 1, 1);
            horizontalLine.setLayoutData(gd);
        }
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
    public static CustomSashForm createPartDivider(final IWorkbenchPart workbenchPart, Composite parent, int style) {
        return new CustomSashForm(parent, style);
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

    @NotNull
    public static Button createDialogButton(@NotNull Composite parent, @Nullable String label, @Nullable SelectionListener selectionListener) {
        return createDialogButton(parent, label, null, (DBPImage) null, selectionListener);
    }

    @NotNull
    public static Button createDialogButton(@NotNull Composite parent, @Nullable String label, @Nullable DBPImage icon, @Nullable String toolTip, @Nullable SelectionListener selectionListener) {
        return createDialogButton(parent, label, toolTip, icon, selectionListener);
    }

    @NotNull
    public static Button createDialogButton(
        @NotNull Composite parent,
        @Nullable String label,
        @Nullable String toolTip,
        @Nullable DBPImage icon,
        @Nullable SelectionListener selectionListener
    ) {
        Button button = new Button(parent, SWT.PUSH);
        button.setText(label);
        button.setFont(JFaceResources.getDialogFont());
        if (icon != null) {
            button.setImage(DBeaverIcons.getImage(icon));
        }
        if (toolTip != null) {
            button.setToolTipText(toolTip);
        }

        button.setLayoutData(getDialogButtonLayoutData(parent, button));

        if (selectionListener != null) {
            button.addSelectionListener(selectionListener);
        }
        return button;
    }

    private static Object getDialogButtonLayoutData(@NotNull Composite parent, @NotNull Button button) {
        int buttonWidth = getDialogButtonWidth(button);
        if (parent.getLayout() instanceof GridLayout) {
            GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
            gridData.widthHint = buttonWidth;
            return gridData;
        } else if (parent.getLayout() instanceof RowLayout) {
            return new RowData(buttonWidth, SWT.DEFAULT);
        } else {
            return null;
        }
    }

    /**
     * Returns the width of the button. The width is calculated based on the button font.
     *
     * @param button the button.
     * @return the width of the button.
     */
    public static int getDialogButtonWidth(@NotNull Button button) {
        GC gc = new GC(button);
        try {
            gc.setFont(JFaceResources.getDialogFont());
            int widthHint = Dialog.convertHorizontalDLUsToPixels(gc.getFontMetrics(), IDialogConstants.BUTTON_WIDTH);
            Point minSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
            return Math.max(widthHint, minSize.x);
        } finally {
            gc.dispose();
        }
    }

    @NotNull
    public static Button createRadioButton(@NotNull Composite parent, @Nullable String label, @NotNull Object data, @Nullable SelectionListener selectionListener)
    {
        Button button = new Button(parent, SWT.RADIO);
        button.setText(label);
        if (selectionListener != null) {
            button.addSelectionListener(selectionListener);
        }
        button.setData(data);
        return button;
    }

    public static void setHelp(Control control, String pluginId, String helpContextID)
    {
        if (control != null && !control.isDisposed()) {
            PlatformUI.getWorkbench().getHelpSystem().setHelp(control, pluginId + "." + helpContextID); //$NON-NLS-1$
        }
    }

    public static void setHelp(Control control, String helpContextID)
    {
        setHelp(control, UIActivator.PLUGIN_ID, helpContextID);
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

    /**
     * Creates a new link that opens the given preference page either in the current
     * preference container, is present, or in a new modal dialog.
     */
    @NotNull
    public static Link createPreferenceLink(
        @NotNull Composite parent,
        @NotNull String message,
        @NotNull String pageId,
        @Nullable IWorkbenchPreferenceContainer pageContainer,
        @Nullable Object pageData
    ) {
        final IPreferenceNode node = findPreferenceNode(pageId);
        final Link link = new Link(parent, 0);

        if (node == null) {
            link.setText(NLS.bind(WorkbenchMessages.PreferenceNode_NotFound, pageId));
        } else {
            final boolean canOpenHere = findPreferenceNode(pageContainer, pageId) != null;
            final String label = canOpenHere ? node.getLabelText() : NLS.bind(UIMessages.link_external_label, node.getLabelText());
            link.setText(NLS.bind(message, label));
            link.setToolTipText(canOpenHere ? null : UIMessages.link_external_tip);
            link.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
                if (pageContainer != null && canOpenHere) {
                    // Open in the same dialog
                    pageContainer.openPage(pageId, pageData);
                } else {
                    // Open in a new dialog
                    PreferencesUtil.createPreferenceDialogOn(
                        link.getShell(),
                        pageId,
                        new String[]{pageId},
                        pageData,
                        PreferencesUtil.OPTION_NONE
                    ).open();
                }
            }));
        }

        return link;
    }

    @Nullable
    private static IPreferenceNode findPreferenceNode(@NotNull String pageId) {
        return findPreferenceNode(PlatformUI.getWorkbench().getPreferenceManager(), pageId);
    }

    @Nullable
    private static IPreferenceNode findPreferenceNode(@Nullable IWorkbenchPreferenceContainer container, @NotNull String pageId) {
        if (container instanceof PreferenceDialog dialog) {
            return findPreferenceNode(dialog.getPreferenceManager(), pageId);
        }
        return null;
    }

    @Nullable
    private static IPreferenceNode findPreferenceNode(@NotNull PreferenceManager preferenceManager, @NotNull String pageId) {
        return preferenceManager.getElements(PreferenceManager.POST_ORDER).stream()
            .filter(next -> next.getId().equals(pageId))
            .findFirst()
            .orElse(null);
    }

    public static void addFocusTracker(IServiceLocator serviceLocator, String controlID, Control control)
    {
        IFocusService focusService = serviceLocator.getService(IFocusService.class);
        if (focusService == null) {
            focusService = UIUtils.getActiveWorkbenchWindow().getService(IFocusService.class);
        }
        if (focusService != null) {
            IFocusService finalFocusService = focusService;
            finalFocusService.addFocusTracker(control, controlID);

            control.addDisposeListener(e -> {
                // Unregister from focus service
                finalFocusService.removeFocusTracker(control);
            });
        } else {
            log.debug("Focus service not found in " + serviceLocator);
        }
    }

    public static void addDefaultEditActionsSupport(final IServiceLocator site, final Control control) {
        UIUtils.addFocusTracker(site, UIUtils.INLINE_WIDGET_EDITOR_ID, control);
    }


    @NotNull
    public static IDialogSettings getDialogSettings(@NotNull String dialogId)
    {
        IDialogSettings workbenchSettings = UIActivator.getDefault().getDialogSettings();
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

    public static void putSectionValueWithType(IDialogSettings dialogSettings, @NotNull String key, Object value) {
        if (value == null) {
            dialogSettings.put(key, ((String) null));
            return;
        }

        if (value instanceof Double) {
            dialogSettings.put(key, (Double) value);
        } else
        if (value instanceof Float) {
            dialogSettings.put(key, (Float) value);
        } else
        if (value instanceof Integer) {
            dialogSettings.put(key, (Integer) value);
        } else
        if (value instanceof Long) {
            dialogSettings.put(key, (Long) value);
        } else
        if (value instanceof String) {
            dialogSettings.put(key, (String) value);
        } else
        if (value instanceof Boolean) {
            dialogSettings.put(key, (Boolean) value);
        } else {
            // do nothing
        }
        dialogSettings.put(key + "_type", value.getClass().getSimpleName());
    }

    public static Object getSectionValueWithType(IDialogSettings dialogSettings, @NotNull String key) {
        String type = dialogSettings.get(key + "_type");
        if (type != null) {
            switch (type) {
                case "Double": return dialogSettings.getDouble(key);
                case "Float": return dialogSettings.getFloat(key);
                case "Integer": return dialogSettings.getInt(key);
                case "Long": return dialogSettings.getLong(key);
                case "String": return dialogSettings.get(key);
                case "Boolean": return dialogSettings.getBoolean(key);
            }
        }
        return dialogSettings.get(key);
    }

    @Nullable
    public static IWorkbenchPartSite getWorkbenchPartSite(IServiceLocator serviceLocator)
    {
        IWorkbenchPartSite partSite = serviceLocator.getService(IWorkbenchPartSite.class);
        if (partSite == null) {
            IWorkbenchPart activePart = serviceLocator.getService(IWorkbenchPart.class);
            if (activePart == null) {
                IWorkbenchWindow workbenchWindow = getActiveWorkbenchWindow();
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
        Collection<?> contextIds = getActiveWorkbenchWindow().getService(IContextService.class).getActiveContextIds();
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
                IWorkbenchWindow activeWindow = getActiveWorkbenchWindow();
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

    public static boolean isUIThread() {
        return Display.getCurrent() != null;
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
        if (control == null || control.isDisposed()) {
            return false;
        }
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
        tabItem.addDisposeListener(e -> {
            final Control control = tabItem.getControl();
            if (!control.isDisposed()) {
                control.dispose();
            }
        });
    }

    public static TreeItem getTreeItem(Tree tree, Object data) {
        for (TreeItem item : tree.getItems()) {
            if (item.getData() == data) {
                return item;
            }
            TreeItem child = getTreeItem(item, data);
            if (child != null) {
                return child;
            }
        }
        return null;
    }

    private static TreeItem getTreeItem(TreeItem parent, Object data) {
        for (TreeItem item : parent.getItems()) {
            if (item.getData() == data) {
                return item;
            }
            TreeItem child = getTreeItem(item, data);
            if (child != null) {
                return child;
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

    public static boolean isInDialog() {
        try {
            Shell activeShell = Display.getCurrent().getActiveShell();
            return activeShell != null && isInDialog(activeShell);
        } catch (Exception e) {
            // IF we are in wrong thread
            return false;
        }
    }

    public static boolean isInDialog(Control control) {
        return control.getShell().getData() instanceof Dialog;
    }

    public static boolean isInWizard(Control control) {
        return control.getShell().getData() instanceof IWizardContainer;
    }

    public static Link createLink(Composite parent, String text, SelectionListener listener) {
        Link link = new Link(parent, SWT.NONE);
        link.setText(text);
        link.addSelectionListener(listener);
        return link;
    }

    public static void postEvent(Control ownerControl, final Event event) {
        final Display display = ownerControl.getDisplay();
        asyncExec(() -> display.post(event));
    }

    public static void drawMessageOverControl(Control control, PaintEvent e, String message, int offset) {
        drawMessageOverControl(control, e.gc, message, offset);
    }

    public static void drawMessageOverControl(Control control, GC gc, String message, int offset) {
        Rectangle bounds = control.getBounds();
        final int height = gc.textExtent(message).y;
        for (String line : message.split("\n")) {
            line = line.trim();
            Point ext = gc.textExtent(line);
            gc.drawText(line,
                (bounds.width - ext.x) / 2,
                (bounds.height - height) / 2 + offset);
            offset += ext.y;
        }
    }

    public static SharedTextColors getSharedTextColors() {
        return SHARED_TEXT_COLORS;
    }

    public static SharedFonts getSharedFonts() {
        return SHARED_FONTS;
    }

    public static void run(
        IRunnableContext runnableContext,
        boolean fork,
        boolean cancelable,
        final DBRRunnableWithProgress runnableWithProgress)
        throws InvocationTargetException, InterruptedException {
        runnableContext.run(fork, cancelable,
            monitor -> runnableWithProgress.run(RuntimeUtils.makeMonitor(monitor)));
    }

    public static AbstractUIJob runUIJob(String jobName, final DBRRunnableWithProgress runnableWithProgress) {
        return runUIJob(jobName, 0, runnableWithProgress);
    }

    public static AbstractUIJob runUIJob(String jobName, int timeout, final DBRRunnableWithProgress runnableWithProgress) {
        AbstractUIJob job = new AbstractUIJob(jobName) {
            @Override
            public IStatus runInUIThread(DBRProgressMonitor monitor) {
                try {
                    runnableWithProgress.run(monitor);
                } catch (InvocationTargetException e) {
                    return GeneralUtils.makeExceptionStatus(e);
                } catch (InterruptedException e) {
                    return Status.CANCEL_STATUS;
                }
                return Status.OK_STATUS;
            }
        };
        job.setSystem(true);
        job.schedule(timeout);
        return job;
    }

    @Nullable
    public static IWorkbenchWindow findActiveWorkbenchWindow() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        if (window != null) {
            return window;
        }
        IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
        if (windows.length > 0) {
            return windows[0];
        }
        return null;
    }

    @NotNull
    public static IWorkbenchWindow getActiveWorkbenchWindow() {
        IWorkbenchWindow workbenchWindow = findActiveWorkbenchWindow();
        if (workbenchWindow == null) {
            throw new IllegalStateException("No workbench window");
        }
        return workbenchWindow;
    }

    public static IWorkbenchWindow getParentWorkbenchWindow(Control control) {
        for (Control p = control.getParent(); p != null; p = p.getParent()) {
            if (p.getData() instanceof IWorkbenchWindow) {
                return (IWorkbenchWindow) p.getData();
            }
        }
        return null;
    }

    @Nullable
    public static Shell getActiveWorkbenchShell() {
        if (PlatformUI.isWorkbenchRunning()) {
            IWorkbench workbench = PlatformUI.getWorkbench();
            IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
            if (window != null) {
                Shell shell = window.getShell();
                if (shell != null && shell.isVisible()) {
                    return shell;
                }
            }
        }
        return getActiveShell();
    }

    public static DBRRunnableContext getDefaultRunnableContext() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        if (workbench != null && workbench.getActiveWorkbenchWindow() != null) {
            return new RunnableContextDelegate(workbench.getActiveWorkbenchWindow());
        } else {
            return (fork, cancelable, runnable) -> runnable.run(new VoidProgressMonitor());
        }
    }

    public static DBRRunnableContext getDialogRunnableContext() {
        return (fork, cancelable, runnable) -> runInProgressDialog(runnable);
    }

    /**
     * Runs task in Eclipse progress service.
     * NOTE: this call can't be canceled if it will block in IO
     */
    public static void runInProgressService(final DBRRunnableWithProgress runnable)
        throws InvocationTargetException, InterruptedException {
        getDefaultRunnableContext().run(true, true, runnable);
    }

    public static <T, R> T runWithMonitor(final DBRRunnableWithReturn<T> runnable) throws DBException  {
        Object[] result = new Object[1];
        try {
            getDefaultRunnableContext().run(true, true, monitor -> {
                try {
                    result[0] = runnable.runTask(monitor);
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof DBException dbe) {
                throw dbe;
            } else {
                throw new DBException("Internal error", e.getTargetException());
            }
        } catch (Throwable e) {
            log.error(e);
        }
        return (T) result[0];
    }

    /**
     * Runs task in Eclipse progress dialog.
     * NOTE: this call can't be canceled if it will block in IO
     */
    public static void runInProgressDialog(final DBRRunnableWithProgress runnable) throws InvocationTargetException {
        try {
            IRunnableContext runnableContext;
            IWorkbench workbench = PlatformUI.getWorkbench();
            IWorkbenchWindow workbenchWindow = workbench.getActiveWorkbenchWindow();
            if (workbenchWindow != null) {
                runnableContext = new ProgressMonitorDialog(workbench.getActiveWorkbenchWindow().getShell());
            } else {
                runnableContext = workbench.getProgressService();
            }
            runnableContext.run(true, true, monitor -> runnable.run(RuntimeUtils.makeMonitor(monitor)));
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    public static void runInUI(IRunnableContext context, final DBRRunnableWithProgress runnable) {
        try {
            PlatformUI.getWorkbench().getProgressService().runInUI(context,
                monitor -> runnable.run(RuntimeUtils.makeMonitor(monitor)), ResourcesPlugin.getWorkspace().getRoot());
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError(null, null, e.getTargetException());
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    public static void runInUI(final DBRRunnableWithProgress runnable) {
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        IRunnableContext context = window != null ? window : DummyRunnableContext.INSTANCE;
        runInUI(context, runnable);
    }

    public static Display getDisplay() {
        try {
            return PlatformUI.getWorkbench().getDisplay();
        } catch (Exception e) {
            return Display.getDefault();
        }
    }

    public static void timerExec(int milliseconds, @NotNull Runnable runnable) {
        try {
            Display display = getDisplay();
            if (!display.isDisposed()) {
                display.timerExec(milliseconds, runnable);
            }
        } catch (Exception e) {
            log.debug(e);
        }
    }

    public static void asyncExec(Runnable runnable) {
        try {
            Display display = getDisplay();
            if (!display.isDisposed()) {
                display.asyncExec(runnable);
            }
        } catch (Exception e) {
            log.debug(e);
        }
    }

    public static void syncExec(Runnable runnable) {
        try {
            Display display = getDisplay();
            if (!display.isDisposed()) {
                display.syncExec(runnable);
            }
        } catch (Exception e) {
            log.debug(e);
        }
    }

    public static <T> T syncExec(RunnableWithResult<T> runnable) {
        try {
            getDisplay().syncExec(runnable);
            return runnable.getResult();
        } catch (Exception e) {
            log.debug(e);
            return null;
        }
    }

    @Nullable
    public static Color getSharedColor(@Nullable String rgbString) {
        if (CommonUtils.isEmpty(rgbString)) {
            return null;
        }
        return SHARED_TEXT_COLORS.getColor(rgbString);
    }

    @Nullable
    public static Color getSharedColor(@Nullable RGB rgb) {
        if (rgb == null) {
            return null;
        }
        return SHARED_TEXT_COLORS.getColor(rgb);
    }

    public static Color getConnectionColor(DBPConnectionConfiguration connectionInfo) {
        String rgbString = connectionInfo.getConnectionColor();
        if (CommonUtils.isEmpty(rgbString)) {
            rgbString = connectionInfo.getConnectionType().getColor();
        }
        if (CommonUtils.isEmpty(rgbString)) {
            return null;
        }
        return getConnectionColorByRGB(rgbString);
    }

    public static Color getConnectionTypeColor(DBPConnectionType connectionType) {
        String rgbString = connectionType.getColor();
        if (CommonUtils.isEmpty(rgbString)) {
            return null;
        }
        return getConnectionColorByRGB(rgbString);
    }

    public static Color getConnectionColorByRGB(String rgbStringOrId) {
        if (rgbStringOrId.isEmpty()) {
            return null;
        }
        if (Character.isAlphabetic(rgbStringOrId.charAt(0))) {
            // Some color constant
            RGB rgb = getActiveWorkbenchWindow().getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry().getRGB(rgbStringOrId);
            return SHARED_TEXT_COLORS.getColor(rgb);
        } else {
            Color connectionColor = SHARED_TEXT_COLORS.getColor(rgbStringOrId);
            if (connectionColor.getBlue() == 255 && connectionColor.getRed() == 255 && connectionColor.getGreen() == 255) {
                // For white color return just null to avoid explicit color set.
                // It is important for dark themes
                return null;
            }
            return connectionColor;
        }
    }

    /**
     * Create centralized shell from default display
     *
     */
    public static Shell createCenteredShell(Shell parent) {
        final Rectangle bounds = parent.getBounds();
        final int x = bounds.x + bounds.width / 2 - 120;
        final int y = bounds.y + bounds.height / 2 - 170;
        final Shell shell = new Shell(parent);
        shell.setLocation(x, y);
        return shell;
    }

    public static void centerShell(Shell parent, Shell shell) {
        if (parent == null || shell == null) {
            return;
        }
        Point size = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        final Rectangle parentBounds = parent.getBounds();
        final int x = parentBounds.x + (parentBounds.width - size.x) / 2;
        final int y = parentBounds.y + (parentBounds.height - size.y) / 2;

        shell.setLocation(x, y);
    }

    public static Image getShardImage(String id) {
        return PlatformUI.getWorkbench().getSharedImages().getImage(id);
    }

    public static ImageDescriptor getShardImageDescriptor(String id) {
        return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(id);
    }

    public static void addVariablesToControl(@NotNull Control controlForTip, @NotNull String[] variables, String toolTipPattern) {
        final StringContentProposalProvider proposalProvider = new StringContentProposalProvider(Arrays
            .stream(variables)
            .map(GeneralUtils::variablePattern)
            .toArray(String[]::new));

        UIUtils.setContentProposalToolTip(controlForTip, toolTipPattern, variables);

        ContentAssistUtils.installContentProposal(controlForTip, new SmartTextContentAdapter(), proposalProvider);
    }

    public static void setContentProposalToolTip(Control control, String toolTip, String ... variables) {
        control.setToolTipText(getSupportedVariablesTip(toolTip, variables));

    }

    @NotNull
    public static String getSupportedVariablesTip(String toolTip, String ... variables) {
        StringBuilder varsTip = new StringBuilder();
        varsTip.append(toolTip).append(". ").append(UIMessages.pref_page_connections_tool_tip_text_allowed_variables).append(":\n");
        for (int i = 0; i < variables.length; i++) {
            String var = variables[i];
            if (i > 0) varsTip.append(",\n");
            varsTip.append("  ").append(GeneralUtils.variablePattern(var));
        }
        varsTip.append("."); //$NON-NLS-1$
        return varsTip.toString();
    }

    public static CoolItem createCoolItem(CoolBar coolBar, Control control) {
        CoolItem item = new CoolItem(coolBar, SWT.NONE);
        item.setControl(control);
        Point size = control.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        Point preferred = item.computeSize(size.x, size.y);
        item.setPreferredSize(preferred);
        return item;
    }

    public static void resizeShell(@NotNull Shell shell) {
        final Point compSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
        resizeShell(shell, compSize);
    }

    public static void resizeShell(@NotNull Shell shell, Point compSize) {
        final Rectangle displayArea = shell.getDisplay().getClientArea();
        final Point shellLocation = shell.getLocation();
        final Point shellSize = shell.getSize();
        boolean needsLayout = false;

        if (shellSize.x < compSize.x || shellSize.y < compSize.y) {
            shellSize.x = Math.max(shellSize.x, compSize.x);
            shellSize.y = Math.max(shellSize.y, compSize.y);
            shell.setSize(shellSize);
        }

        if (shellLocation.x + shellSize.x > displayArea.width || shellLocation.y + shellSize.y > displayArea.height) {
            shellLocation.x = CommonUtils.clamp(displayArea.width - shellSize.x, 0, shellLocation.x);
            shellLocation.y = CommonUtils.clamp(displayArea.height - shellSize.y, 0, shellLocation.y);
            shell.setLocation(shellLocation.x, shellLocation.y);
            needsLayout = true;
        }

        if (needsLayout) {
            shell.layout(true);
        }
    }

    public static void waitJobCompletion(AbstractJob job) {
        waitJobCompletion(job, null);
    }

    public static void waitJobCompletion(@NotNull AbstractJob job, @Nullable IProgressMonitor monitor) {
        // Wait until job finished
        Display display = Display.getCurrent();
        while (!job.isFinished() && !DBWorkbench.getPlatform().isShuttingDown()) {
            if (monitor != null && monitor.isCanceled()) {
                job.cancel();
            }
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        display.update();
    }

    public static void waitInUI(DBRCondition condition, long waitTime) {
        syncExec(() -> {
            long startTime = System.currentTimeMillis();
            Display display = Display.getCurrent();
            do  {
                if (!display.readAndDispatch()) {
                    RuntimeUtils.pause(100);
                }
            } while (!condition.isConditionMet() && (System.currentTimeMillis() - startTime) < waitTime);
            display.update();
        });
    }

    public static void fixReadonlyTextBackground(Text textField) {
        // There is still no good workaround: https://bugs.eclipse.org/bugs/show_bug.cgi?id=340889
        if (false) {
            if (RuntimeUtils.isWindows()) {
                // On Windows everything is fine
                return;
            }
            if ((textField.getStyle() & SWT.READ_ONLY) == SWT.READ_ONLY) {
                textField.setBackground(textField.getDisplay().getSystemColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW));
            } else {
                textField.setBackground(null);
            }
        }
    }

    public static ColorRegistry getColorRegistry() {
        return PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry();
    }

    public static Color getGlobalColor(String colorName) {
        return getColorRegistry().get(colorName);
    }

    public static Control createEmptyLabel(Composite parent, int horizontalSpan, int verticalSpan)
    {
        Label emptyLabel = new Label(parent, SWT.NONE);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
        gd.horizontalSpan = horizontalSpan;
        gd.verticalSpan = verticalSpan;
        gd.widthHint = 0;
        emptyLabel.setLayoutData(gd);
        return emptyLabel;
    }

    public static void disposeChildControls(Composite composite) {
        for (Control child : composite.getChildren()) {
            child.dispose();
        }
    }

    //////////////////////////////////////////
    // From E4 sources

    /**
     * Returns the grey value in which the given color would be drawn in grey-scale.
     */
    public static double greyLevel(RGB rgb) {
        if (rgb.red == rgb.green && rgb.green == rgb.blue)
            return rgb.red;
        return (0.299 * rgb.red + 0.587 * rgb.green + 0.114 * rgb.blue + 0.5);
    }

    /**
     * Returns whether the given color is dark or light depending on the colors grey-scale level.
     */
    public static boolean isDark(RGB rgb) {
        return greyLevel(rgb) < 128;
    }
    
    /**
     * Calculate the Contrast color based on Luma(brightness)
     * https://en.wikipedia.org/wiki/Luma_(video)
     *
     * Do not dispose returned color.
     */
    public static Color getContrastColor(Color color) {
        if (color == null) {
            return COLOR_BLACK;
        }
        double luminance = 1 - (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue()) / 255;
        if (luminance > 0.5) {
            return UIStyles.isDarkTheme() ? COLOR_WHITE_DARK : COLOR_WHITE;
        }
        return COLOR_BLACK;
    }

    public static Color getInvertedColor(Color color) {
        return new Color(255 - color.getRed(), 255 - color.getGreen(), 255 - color.getBlue());
    }

    public static void openWebBrowser(String url) {
        url = url.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("ftp://")) {
            url = "http://" + url;
        }
        ShellUtils.launchProgram(url);
    }

    public static void setBackgroundForAll(Control control, Color color) {
        if (!(control instanceof Button)) {
            control.setBackground(color);
        }
        if (control instanceof Composite) {
            for (Control ch : ((Composite) control).getChildren()) {
                setBackgroundForAll(ch, color);
            }
        }
    }

    public static <T extends Control> void addEmptyTextHint(T control, DBRValueProvider<String, T> tipProvider) {
        final Font hintFont = UIUtils.modifyFont(control.getFont(), SWT.ITALIC);

        control.addDisposeListener(e -> hintFont.dispose());
        control.addPaintListener(e -> {
            String tip = tipProvider.getValue(control);
            if (tip != null && isEmptyTextControl(control) && !control.isFocusControl()) {
                final GC gc = e.gc;
                final Point textSize = gc.textExtent(tip);
                final Point controlSize = control.computeSize(SWT.DEFAULT, SWT.DEFAULT);
                final int baseline = (controlSize.y - textSize.y) / 2;

                gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
                gc.setFont(hintFont);
                gc.drawText(tip, baseline, baseline, true);
                gc.setFont(null);
            }
        });
    }

    private static boolean isEmptyTextControl(Control control) {
        return (control instanceof Text && ((Text) control).getCharCount() == 0) ||
            (control instanceof StyledText && ((StyledText) control).getCharCount() == 0) ||
            (control instanceof Combo && ((Combo) control).getText().isEmpty());
    }

    public static void expandAll(AbstractTreeViewer treeViewer) {
        Control control = treeViewer.getControl();
        control.setRedraw(false);
        try {
            // Do not use expandAll(true) as it is not supported by Eclipse versions before 2019
            treeViewer.expandAll();
        } finally {
            control.setRedraw(true);
        }
    }

    public static Font getMonospaceFont() {
        return PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getFontRegistry().get(UIFonts.DBEAVER_FONTS_MONOSPACE);
    }

    public static <T extends Control> T getParentOfType(Control control, Class<T> parentType) {
        while (control != null) {
            if (parentType.isInstance(control)) {
                return parentType.cast(control);
            }
            control = control.getParent();
        }
        return null;
    }

    public static Object normalizePropertyValue(Object text) {
        if (text instanceof String) {
            return CommonUtils.toString(text).trim();
        }
        return text;
    }

    public static void setControlVisible(Control control, boolean visible) {
        control.setVisible(visible);
        Object gd = control.getLayoutData();
        if (gd instanceof GridData) {
            ((GridData) gd).exclude = !visible;
        }
    }

    public static void drawTextWithBackground(@NotNull GC gc, @NotNull String text, int x, int y) {
        final Point size = gc.textExtent(text);
        final int centerX = x - size.x / 2;
        final int centerY = y - size.y;

        gc.setForeground(UIStyles.getDefaultTextForeground());
        gc.setBackground(UIStyles.getDefaultTextBackground());

        gc.fillRectangle(centerX - 2, centerY - 2, size.x + 4, size.y + 4);
        gc.drawText(text, centerX, centerY, true);
        gc.drawRoundRectangle(centerX - 3, centerY - 3, size.x + 5, size.y + 5, 5, 5);
    }

    public static void installMacOSFocusLostSubstitution(@NotNull Widget widget, @NotNull Runnable onFocusLost) {
        if (!RuntimeUtils.isMacOS()) {
            return;
        }
        if (widget instanceof Combo || widget instanceof CCombo) {
            widget.addListener(SWT.Selection, new TypedListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    onFocusLost.run();
                }
            }));
        } else {
            widget.addDisposeListener(e -> onFocusLost.run());
        }
    }

    public static void installAndUpdateMainFont(@NotNull Control control) {
        final IPropertyChangeListener listener = event -> {
            if (event.getProperty().equals(UIFonts.DBEAVER_FONTS_MAIN_FONT)) {
                applyMainFont(control);
            }
        };

        PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(listener);
        control.addDisposeListener(e -> PlatformUI.getWorkbench().getThemeManager().removePropertyChangeListener(listener));

        applyMainFont(control);
    }

    public static void applyMainFont(@Nullable Control control) {
        if (control == null || control.isDisposed() || mainFontIsDefault()) {
            return;
        }
        applyMainFont(control, JFaceResources.getFont(UIFonts.DBEAVER_FONTS_MAIN_FONT));
    }

    @Nullable
    public static Text recreateTextControl(@Nullable Text original, int style) {
        if (original == null || original.getStyle() == style) {
            return original;
        }

        final Composite parent = original.getParent();
        final Control[] tabList = parent.getTabList();

        final Text text = new Text(parent, style);
        text.setText(original.getText());
        text.setLayoutData(original.getLayoutData());
        text.moveAbove(original);

        copyListeners(original, text, SWT.DefaultSelection);
        copyListeners(original, text, SWT.Modify);
        copyListeners(original, text, SWT.Verify);

        original.dispose();

        for (int i = 0; i < tabList.length; i++) {
            if (tabList[i] == original) {
                tabList[i] = text;
            }
        }

        parent.setTabList(tabList);
        parent.layout(true, true);

        return text;
    }

    private static void copyListeners(@NotNull Widget source, @NotNull Widget target, int eventType) {
        for (Listener listener : source.getListeners(eventType)) {
            target.addListener(eventType, listener);
        }
    }

    private static void applyMainFont(@NotNull Control control, @NotNull Font font) {
        control.setFont(font);

        if (control instanceof Composite) {
            for (Control element : ((Composite) control).getChildren()) {
                applyMainFont(element, font);
            }
        }
    }

    private static boolean mainFontIsDefault() {
        final FontData[] mainFontData = JFaceResources.getFontRegistry().getFontData(UIFonts.DBEAVER_FONTS_MAIN_FONT);
        final FontData[] defaultFontData = JFaceResources.getFontRegistry().getFontData(JFaceResources.DEFAULT_FONT);
        return Arrays.equals(mainFontData, defaultFontData);
    }

    @Nullable
    public static ToolItem findToolItemByCommandId(@NotNull ToolBarManager toolbarManager, @NotNull String commandId) {
        for (ToolItem item : toolbarManager.getControl().getItems()) {
            Object data = item.getData();
            if (data instanceof CommandContributionItem) {
                ParameterizedCommand cmd = ((CommandContributionItem) data).getCommand(); 
                if (cmd != null && commandId.equals(cmd.getId())) {
                    return item;
                }
            } else if (data instanceof HandledContributionItem) {
                MHandledItem model = ((HandledContributionItem) data).getModel();
                if (model != null ) {
                    ParameterizedCommand cmd = model.getWbCommand();
                    if (cmd != null && commandId.equals(cmd.getId())) {
                        return item;
                    }
                }
            }
        }
        return null;
    }

    public static void populateToolItemCommandIds(ToolBarManager toolbarManager) {
        // used for accessibility automation, see dbeaver-qa-auto
        for (ToolItem item : toolbarManager.getControl().getItems()) {
            Object data = item.getData();
            if (data instanceof CommandContributionItem) {
                ParameterizedCommand cmd = ((CommandContributionItem) data).getCommand(); 
                if (cmd != null) {
                    item.setData("commandId", cmd.getId());
                }
            } else if (data instanceof HandledContributionItem) {
                MHandledItem model = ((HandledContributionItem) data).getModel();
                if (model != null) {
                    ParameterizedCommand cmd = model.getWbCommand();
                    if (cmd != null) {
                        item.setData("commandId", cmd.getId());
                    }
                }
            }
        }
    }

    public static void enableDoubleBuffering(@NotNull Control control) {
        if ((control.getStyle() & SWT.DOUBLE_BUFFERED) != 0) {
            // Already enabled - no op
            return;
        }
        try {
            final Field styleField = Widget.class.getDeclaredField("style");
            if (!styleField.canAccess(control)) {
                styleField.setAccessible(true);
            }
            styleField.set(control, styleField.getInt(control) | SWT.DOUBLE_BUFFERED);
        } catch (Exception e) {
            log.error("Unable to enable double buffering", e.getCause());
        }
    }

    public static String getCatalogSchemaTerms(@Nullable DBPDataSourceContainer dataSourceContainer, boolean checkChangePossibility) {
        DBPDataSource dataSource = dataSourceContainer == null ? null : dataSourceContainer.getDataSource();
        if (dataSource != null) {
            DBPDataSourceInfo dataSourceInfo = dataSource.getInfo();
            boolean showCatalog = true;
            boolean showSchema = true;
            if (checkChangePossibility) {
                DBCExecutionContext defaultContext = DBUtils.getDefaultContext(dataSource, false);
                DBCExecutionContextDefaults<?, ?> contextDefaults = defaultContext.getContextDefaults();
                if (contextDefaults != null) {
                    showCatalog = contextDefaults.getDefaultCatalog() != null || contextDefaults.supportsCatalogChange();
                    showSchema = contextDefaults.getDefaultSchema() != null || contextDefaults.supportsSchemaChange();
                }
            }

            String catalogTerm = showCatalog ? dataSourceInfo.getCatalogTerm() : null;
            String schemaTerm = showSchema ? dataSourceInfo.getSchemaTerm() : null;
            if (CommonUtils.isEmpty(catalogTerm)) {
                if (!CommonUtils.isEmpty(schemaTerm)) {
                    return schemaTerm;
                }
            } else if (CommonUtils.isEmpty(schemaTerm)) {
                return catalogTerm;
            } else {
                return catalogTerm + "/" + schemaTerm;
            }
        }
        return UIMessages.label_catalog_schema;
    }
}
