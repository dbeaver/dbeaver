/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.misc;

import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;
import org.jkiss.dbeaver.ui.DBIcon;

/**
 * About box
 */
public class AboutBoxDialog extends Dialog
{
    private static final String STR_AUTHOR_INFO = "(C) JKISS project, 2011";
    private static final String STR_SITE_URL = "<a>http://dbeaver.jkiss.org/</a>";
    private static final String STR_EMAIL_URL = "<a>serge@jkiss.org</a>";
    private final Font TITLE_FONT;

    public AboutBoxDialog(Shell shell)
    {
        super(shell);
        TITLE_FONT = new Font(shell.getDisplay(), "SansSerif", 20, SWT.NORMAL);
    }

    @Override
    public boolean close() {
        TITLE_FONT.dispose();
        return super.close();
    }

    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("About DBeaver");
    }

    protected boolean isResizable()
    {
        return true;
    }

    protected Control createDialogArea(Composite parent)
    {
        Color background = JFaceColors.getBannerBackground(parent.getDisplay());
        //Color foreground = JFaceColors.getBannerForeground(parent.getDisplay());
        parent.setBackground(background);

        Composite group = new Composite(parent, SWT.NONE);
        group.setBackground(background);
        GridLayout layout = new GridLayout(1, false);
        layout.marginHeight = 20;
        layout.marginWidth = 20;
        group.setLayout(layout);

        GridData gd;

        String productDesc = "", productVersion = "";
        IProduct product = Platform.getProduct();
        if (product != null) {
			productDesc = product.getDescription();
            productVersion = product.getDefiningBundle().getVersion().toString();
		}

        Label titleLabel = new Label(group, SWT.NONE);
        titleLabel.setBackground(background);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setText(productDesc);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalAlignment = GridData.CENTER;
        titleLabel.setLayoutData(gd);
        titleLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(MouseEvent e) {
                BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
                    public void run() {
                        // Do not create InstallationDialog directly
                        // but execute "org.eclipse.ui.help.installationDialog" command
                        IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                        IHandlerService service = (IHandlerService) workbenchWindow.getService(IHandlerService.class);
                        if (service != null) {
                            try {
                                service.executeCommand("org.eclipse.ui.help.installationDialog", null);
                            } catch (Exception e1) {
                                // just ignore error
                            }
                        }
                    }
                });
            }
        });
        
        Label imageLabel = new Label(group, SWT.NONE);
        imageLabel.setBackground(background);

        gd = new GridData();
        gd.verticalAlignment = GridData.BEGINNING;
        gd.horizontalAlignment = GridData.CENTER;
        gd.grabExcessHorizontalSpace = true;
        imageLabel.setLayoutData(gd);
        imageLabel.setImage(DBIcon.ABOUT.getImage());

        Label versionLabel = new Label(group, SWT.NONE);
        versionLabel.setBackground(background);
        versionLabel.setText("Version " + productVersion);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalAlignment = GridData.CENTER;
        versionLabel.setLayoutData(gd);

        Label authorLabel = new Label(group, SWT.NONE);
        authorLabel.setBackground(background);
        authorLabel.setText(STR_AUTHOR_INFO);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalAlignment = GridData.CENTER;
        authorLabel.setLayoutData(gd);

        Link siteLink = new Link(group, SWT.NONE);
        siteLink.setText(STR_SITE_URL);
        siteLink.setBackground(background);
        siteLink.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                Program.launch(e.text);
            }
        });
        gd = new GridData();
        gd.horizontalAlignment = GridData.CENTER;
        siteLink.setLayoutData(gd);

        Link emailLink = new Link(group, SWT.NONE);
        emailLink.setText(STR_EMAIL_URL);
        emailLink.setBackground(background);
        emailLink.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                Program.launch("mailto:" + e.text);
            }
        });
        gd = new GridData();
        gd.horizontalAlignment = GridData.CENTER;
        emailLink.setLayoutData(gd);

        return parent;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalAlignment = GridData.CENTER; 
        parent.setLayoutData(gd);
        parent.setBackground(JFaceColors.getBannerBackground(parent.getDisplay()));
        Button button = createButton(
            parent,
            IDialogConstants.OK_ID,
            IDialogConstants.OK_LABEL,
            true);
        button.setFocus();
    }

}