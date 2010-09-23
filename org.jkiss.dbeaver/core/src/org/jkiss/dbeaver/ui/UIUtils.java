/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandImageService;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.services.IServiceLocator;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.util.SortedMap;

/**
 * UI Utils
 */
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
                if (!Character.isDigit(ch) && ch != '.' && ch != '-') {
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

    public static ToolItem createToolItem(ToolBar toolBar, IServiceLocator serviceLocator, final String commandId, SelectionListener listener)
    {
        ToolItem item = new ToolItem(toolBar, SWT.PUSH);

        final ICommandService commandService = (ICommandService)serviceLocator.getService(ICommandService.class);
        final ICommandImageService commandImageService = (ICommandImageService)serviceLocator.getService(ICommandImageService.class);
        //final IBindingService bindingService = (IBindingService)serviceLocator.getService(IBindingService.class);
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
/*
        TriggerSequence[] sequences = bindingService.getActiveBindingsFor(commandId);
        if (sequences != null && sequences.length == 1) {
            commandName += " (" + sequences[0].format() + ")";
        }
*/
        ImageDescriptor imageDescriptor = commandImageService.getImageDescriptor(commandId);

        item.setToolTipText(commandName);
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

    public static void packColumns(Table table)
    {
        table.setRedraw(false);
        try {
            int totalWidth = 0;
            for (TableColumn column : table.getColumns()) {
                column.pack();
                totalWidth += column.getWidth();
            }
            if (totalWidth > table.getClientArea().width) {
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
            if (totalWidth > tree.getClientArea().width) {
                int extraSpace = totalWidth - tree.getClientArea().width;
                for (TreeColumn tc : tree.getColumns()) {
                    double ratio = (double) tc.getWidth() / totalWidth;
                    tc.setWidth((int) (tc.getWidth() - extraSpace * ratio));
                }
            } else if (totalWidth < tree.getClientArea().width) {
                int extraSpace = tree.getClientArea().width - totalWidth;
                int columnCount = tree.getColumnCount();
                extraSpace /= columnCount;
                for (TreeColumn tc : tree.getColumns()) {
                    tc.setWidth(tc.getWidth() + extraSpace);
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

    public static void dispose(Widget widget)
    {
        if (widget != null) {
            widget.dispose();
        }
    }

    public static void dispose(Resource resource)
    {
        if (resource != null && !resource.isDisposed()) {
            resource.dispose();
        }
    }

    public static void showMessageBox(Shell shell, String title, String info)
    {
        MessageBox messageBox = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
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

    public static Text createLabelText(Composite parent, String label, String value)
    {
        return createLabelText(parent, label, value, SWT.BORDER);
    }

    public static Text createLabelText(Composite parent, String label, String value, int style)
    {
        createControlLabel(parent, label);

        Text text = new Text(parent, style);
        text.setText(value);

        if (parent.getLayout() instanceof GridLayout) {
            text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        }

        return text;
    }

    public static Button createLabelCheckbox(Composite parent, String label, boolean checked)
    {
        return createLabelCheckbox(parent, label, checked, SWT.NONE);
    }

    public static Button createLabelCheckbox(Composite parent, String label, boolean checked, int style)
    {
        createControlLabel(parent, label);

        Button button = new Button(parent, SWT.CHECK | style);
        if (checked) {
            button.setSelection(true);
        }

        return button;
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
        Composite ph = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(columns, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        ph.setLayout(gl);
        return ph;
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

}
