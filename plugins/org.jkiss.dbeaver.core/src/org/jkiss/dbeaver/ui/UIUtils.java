/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.internal.IWorkbenchThemeConstants;
import org.eclipse.ui.services.IServiceLocator;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.dialogs.StandardErrorDialog;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.SortedMap;

/**
 * UI Utils
 */
@SuppressWarnings("restriction")
public class UIUtils {

    static final Log log = LogFactory.getLog(UIUtils.class);

    public static final VerifyListener INTEGER_VERIFY_LISTENER = new VerifyListener() {
        public void verifyText(VerifyEvent e)
        {
            for (int i = 0; i < e.text.length(); i++) {
                char ch = e.text.charAt(i);
                if (!Character.isDigit(ch) && ch != '-' && ch != '+') {
                    e.doit = false;
                    return;
                }
            }
        }
    };

    public static final VerifyListener NUMBER_VERIFY_LISTENER = new VerifyListener() {
        public void verifyText(VerifyEvent e)
        {
            for (int i = 0; i < e.text.length(); i++) {
                char ch = e.text.charAt(i);
                if (!Character.isDigit(ch) && ch != '.' && ch != '-' && ch != 'e' && ch != 'E') {
                    e.doit = false;
                    return;
                }
            }
        }
    };

    public static Object makeStringForUI(Object object)
    {
        if (object == null) {
            return "";
        }
        if (object instanceof Number) {
            return NumberFormat.getInstance().format(object);
        }
        return object;
    }

/*
    public static ToolItem createToolItem(ToolBar toolBar, IServiceLocator serviceLocator, final String commandId, SelectionListener listener)
    {
        final ToolItem item = new ToolItem(toolBar, SWT.PUSH);

        final ICommandService commandService = (ICommandService)serviceLocator.getService(ICommandService.class);
        final ICommandImageService commandImageService = (ICommandImageService)serviceLocator.getService(ICommandImageService.class);
        final IBindingService bindingService = (IBindingService)serviceLocator.getService(IBindingService.class);
        //final IHandlerService handlerService = (IHandlerService)serviceLocator.getService(IHandlerService.class);

        final Command command = commandService.getCommand(commandId);
        if (command == null) {
            log.error("Command '" + commandId + "' not found");
            return item;
        }
        String commandName;
        try {
            commandName = command.getName();
        } catch (NotDefinedException e) {
            commandName = commandId;
        }
        final String toolTip = commandName;
        ImageDescriptor imageDescriptor = commandImageService.getImageDescriptor(commandId);

        item.setToolTipText(toolTip);
        if (imageDescriptor != null) {
            final Image image = imageDescriptor.createImage();
            item.setImage(image);
            item.addDisposeListener(new DisposeListener() {
                public void widgetDisposed(DisposeEvent e)
                {
                    image.dispose();
                }
            });
        }
        item.addSelectionListener(listener);
        toolBar.addListener(SWT.MouseEnter, new Listener() {
            private String origToolTip;
            public void handleEvent(Event event)
            {
                if (origToolTip == null) {
                    origToolTip = item.getToolTipText();
                }
                TriggerSequence sequence = bindingService.getBestActiveBindingFor(commandId);
                if (sequence != null) {
                    String newToolTip = origToolTip + " (" + sequence.format() + ")";
                    if (!item.getToolTipText().equals(newToolTip)) {
                        item.setToolTipText(newToolTip);
                    }
                }
            }
        });

        return item;
    }

    public static ToolItem createToolItem(ToolBar toolBar, String text, DBIcon icon, SelectionListener listener)
    {
        ToolItem item = new ToolItem(toolBar, SWT.PUSH);
        item.setToolTipText(text);
        item.setImage(icon.getImage());
        item.addSelectionListener(listener);
        return item;
    }
*/

    public static ToolItem createToolItem(ToolBar toolBar, String text, DBIcon icon, final IAction action)
    {
        ToolItem item = new ToolItem(toolBar, SWT.PUSH);
        item.setToolTipText(text);
        item.setImage(icon.getImage());
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
        table.setRedraw(false);
        try {
            TableColumn column = new TableColumn(table, style);
            column.setText(text);
            return column;
        } finally {
            table.setRedraw(true);
        }
    }

    public static void packColumn(Item column)
    {
        if (column instanceof TableColumn) {
            ((TableColumn)column).pack();
        } else if (column instanceof TreeColumn) {
            ((TreeColumn)column).pack();
        }
    }

    public static int getColumnWidth(Item column)
    {
        if (column instanceof TableColumn) {
            return ((TableColumn)column).getWidth();
        } else if (column instanceof TreeColumn) {
            return ((TreeColumn)column).getWidth();
        } else {
            return 0;
        }
    }

    public static void packColumns(Table table)
    {
        table.setRedraw(false);
        try {
            int totalWidth = 0;
            for (TableColumn column : table.getColumns()) {
                column.pack();
                totalWidth += column.getWidth();
            }
            if (table.getClientArea().width > 0 && totalWidth > table.getClientArea().width) {
                int extraSpace = totalWidth - table.getClientArea().width;
                for (TableColumn tc : table.getColumns()) {
                    double ratio = (double) tc.getWidth() / totalWidth;
                    tc.setWidth((int) (tc.getWidth() - extraSpace * ratio));
                }
            }
        } finally {
            table.setRedraw(true);
        }
    }

    public static void packColumns(Tree tree)
    {
        packColumns(tree, false);
    }
    public static void packColumns(Tree tree, boolean fit)
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
            for (TreeColumn column : tree.getColumns()) {
                column.pack();
                totalWidth += column.getWidth();
            }
            Rectangle clientArea = tree.getClientArea();
            if (clientArea.isEmpty()) {
                return;
            }
            if (fit) {
                if (totalWidth > clientArea.width) {
                    int extraSpace = totalWidth - clientArea.width;
                    for (TreeColumn tc : tree.getColumns()) {
                        double ratio = (double) tc.getWidth() / totalWidth;
                        tc.setWidth((int) (tc.getWidth() - extraSpace * ratio));
                    }
                } else if (totalWidth < clientArea.width) {
                    int extraSpace = clientArea.width - totalWidth;
                    int columnCount = tree.getColumnCount();
                    if (columnCount > 0) {
                        extraSpace /= columnCount;
                        for (TreeColumn tc : tree.getColumns()) {
                            tc.setWidth(tc.getWidth() + extraSpace);
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
                for (TableColumn tc : table.getColumns()) {
                    tc.pack();
                    totalWidth += tc.getWidth();
                }
                if (totalWidth < table.getClientArea().width) {
                    int extraSpace = table.getClientArea().width - totalWidth;
                    extraSpace /= columnCount;
                    for (TableColumn tc : table.getColumns()) {
                        tc.setWidth(tc.getWidth() + extraSpace);
                    }
                }
            }
        }
        finally {
            table.setRedraw(true);
        }
    }

    public static int getColumnAtPos(Table table, TableItem item, int x, int y)
    {
        int columnCount = table.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            Rectangle rect = item.getBounds(i);
            if (rect.contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    public static void dispose(Widget widget)
    {
        if (widget != null && !widget.isDisposed()) {
            widget.dispose();
        }
    }

    public static void dispose(Resource resource)
    {
        if (resource != null && !resource.isDisposed()) {
            resource.dispose();
        }
    }

    public static void showMessageBox(Shell shell, String title, String info, int messageType)
    {
        MessageBox messageBox = new MessageBox(shell, messageType | SWT.OK);
        messageBox.setMessage(info);
        messageBox.setText(title);
        messageBox.open();
    }

    public static boolean confirmAction(Shell shell, String title, String question)
    {
        MessageBox messageBox = new MessageBox(shell, SWT.ICON_WARNING | SWT.YES | SWT.NO);
        messageBox.setMessage(question);
        messageBox.setText(title);
        int response = messageBox.open();
        return response == SWT.YES;
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
        textLabel.setText(label + ": ");

        return textLabel;
    }

    public static Label createTextLabel(Composite parent, String label)
    {
        Label textLabel = new Label(parent, SWT.NONE);
        textLabel.setText(label);

        return textLabel;
    }

    public static Label createImageLabel(Composite parent, Image image)
    {
        Label imageLabel = new Label(parent, SWT.NONE);
        imageLabel.setImage(image);

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

    public static Text createLabelText(Composite parent, String label, String value, int style, Object layoutData)
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

    public static Spinner createLabelSpinner(Composite parent, String label, int value, int minimum, int maximum)
    {
        createControlLabel(parent, label);

        Spinner spinner = new Spinner(parent, SWT.BORDER);
        spinner.setMinimum(minimum);
        spinner.setMaximum(maximum);
        spinner.setSelection(value);

        return spinner;
    }

    public static Button createLabelCheckbox(Composite parent, String label, boolean checked)
    {
        return createLabelCheckbox(parent, label, checked, SWT.NONE);
    }

    public static Button createLabelCheckbox(Composite parent, String label, boolean checked, int style)
    {
        Label labelControl = createControlLabel(parent, label);
        labelControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        final Button button = new Button(parent, SWT.CHECK | style);
        if (checked) {
            button.setSelection(true);
        }
        labelControl.addMouseListener(new MouseAdapter() {
            public void mouseUp(MouseEvent e)
            {
                if (!button.isDisposed() && button.isVisible() && button.isEnabled()) {
                    button.setSelection(!button.getSelection());
                }
            }
        });

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

    public static Text createCheckText(Composite parent, String label, String value, boolean checked, int textWidth)
    {
        final Button checkbox = UIUtils.createCheckbox(parent, label, checked);
        final Text text = new Text(parent, SWT.BORDER);
        text.setText(value);
        text.setEnabled(checked);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        if (textWidth > 0) {
            gd.widthHint = 200;
        }
        text.setLayoutData(gd);
        text.setData("check", checkbox);

        checkbox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                text.setEnabled(checkbox.getSelection());
            }
        });

        return text;
    }

    public static void enableCheckText(Text text, boolean enable)
    {
        if (text != null) {
            final Button checkbox = (Button) text.getData("check");
            if (checkbox != null) {
                checkbox.setEnabled(enable);
                text.setEnabled(enable && checkbox.getSelection());
            }
        }
    }

    public static Shell getActiveShell()
    {
        IWorkbench workbench = PlatformUI.getWorkbench();
        return workbench == null ? null : getShell(workbench.getActiveWorkbenchWindow());
    }

    public static Shell getShell(IShellProvider provider)
    {
        return provider == null ? null : provider.getShell();
    }
    
    public static Shell getShell(IWorkbenchPart part)
    {
        return part == null ? null : getShell(part.getSite());
    }

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

    public static IHandlerActivation registerKeyBinding(IServiceLocator serviceLocator, IAction action)
    {
        IHandlerService handlerService = (IHandlerService)serviceLocator.getService(IHandlerService.class);
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
            curCharset = ContentUtils.getDefaultFileEncoding();
        }
        Combo encodingCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        encodingCombo.setVisibleItemCount(30);
        SortedMap<String,Charset> charsetMap = Charset.availableCharsets();
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
            log.warn("Charset '" + curCharset + "' is not recognized");
        }
        return encodingCombo;
    }

    public static SashForm createPartDivider(final IWorkbenchPart workbenchPart, Composite parent, int style)
    {
        final SashForm sash = new SashForm(parent, style);
        //sash.setSashWidth(10);
        final IWorkbenchWindow workbenchWindow = workbenchPart.getSite().getWorkbenchWindow();
        //sash.setBackground(sashActiveBackground);

        final IPartListener partListener = new IPartListener() {
            public void partBroughtToTop(IWorkbenchPart part) { }
            public void partOpened(IWorkbenchPart part) { }
            public void partClosed(IWorkbenchPart part) { }
            @SuppressWarnings("restriction")
			public void partActivated(IWorkbenchPart part)
            {
                if (part == workbenchPart) {
                    Color sashActiveBackground = workbenchWindow.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry().get(
                        IWorkbenchThemeConstants.ACTIVE_TAB_BG_END);
                    if (sashActiveBackground == null) {
                        sashActiveBackground = Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
                    }
                    sash.setBackground(sashActiveBackground);
                }
            }
            public void partDeactivated(IWorkbenchPart part) {
                if (part == workbenchPart) {
                    sash.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
                }
            }
        };

        final IPageListener pageListener = workbenchWindow.getActivePage() != null ? null : new IPageListener() {
            public void pageActivated(IWorkbenchPage page) { }
            public void pageOpened(IWorkbenchPage page) {
                page.addPartListener(partListener);
            }
            public void pageClosed(IWorkbenchPage page) {
                page.removePartListener(partListener);
            }
        };
        if (pageListener != null) {
            // No active page yet, wait for it in listener
            workbenchWindow.addPageListener(pageListener);
        } else {
            // Add listener to active page
            workbenchWindow.getActivePage().addPartListener(partListener);
        }

        sash.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e)
            {
                if (pageListener != null) {
                    workbenchWindow.removePageListener(pageListener);
                }
                if (workbenchWindow.getActivePage() != null) {
                    workbenchWindow.getActivePage().removePartListener(partListener);
                }
            }
        });

        return sash;
    }

    public static void showErrorDialog(
        Shell shell,
        String title,
        String message,
        Throwable error)
    {
        log.error(error);

        // Display the dialog
        StandardErrorDialog dialog = new StandardErrorDialog(shell,
            title,
            null,
            RuntimeUtils.makeExceptionStatus(error),
            IStatus.ERROR);
        dialog.open();
    }

    public static void showErrorDialog(
        Shell shell,
        String title,
        String message)
    {
        //log.debug(message);
        // Display the dialog
        StandardErrorDialog dialog = new StandardErrorDialog(
            shell,
            title,
            null,
            new Status(IStatus.ERROR, DBeaverConstants.PLUGIN_ID, message),
            IStatus.ERROR);
        dialog.open();
    }

    public static String formatMessage(String message, Object ... args)
    {
        if (message == null) {
            return "";
        } else { 
            return MessageFormat.format(message, args);
        }
    }

    public static Button createPushButton(Composite parent, String label, Image image)
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

}
