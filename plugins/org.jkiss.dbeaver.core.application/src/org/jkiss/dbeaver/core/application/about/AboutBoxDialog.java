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
package org.jkiss.dbeaver.core.application.about;

import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.InformationDialog;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.text.DateFormat;

/**
 * About box
 */
public class AboutBoxDialog extends InformationDialog
{
    public static final String PRODUCT_PROP_SUB_TITLE = "subTitle"; //$NON-NLS-1$
    public static final String PRODUCT_PROP_COPYRIGHT = "copyright"; //$NON-NLS-1$
    public static final String PRODUCT_PROP_WEBSITE = "website"; //$NON-NLS-1$
    public static final String PRODUCT_PROP_EMAIL = "email"; //$NON-NLS-1$
    private final Font NAME_FONT,TITLE_FONT;

    private Image ABOUT_IMAGE = AbstractUIPlugin.imageDescriptorFromPlugin(
        Platform.getProduct().getDefiningBundle().getSymbolicName(),
        "icons/dbeaver_about.png").createImage();

    public AboutBoxDialog(Shell shell)
    {
        super(shell);
        NAME_FONT = new Font(shell.getDisplay(), CoreMessages.dialog_about_font, 20, SWT.BOLD);
        TITLE_FONT = new Font(shell.getDisplay(), CoreMessages.dialog_about_font, 10, SWT.NORMAL);
    }

    @Override
    protected boolean isBanner() {
        return true;
    }

    @Override
    public boolean close() {
        NAME_FONT.dispose();
        TITLE_FONT.dispose();
        return super.close();
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(CoreMessages.dialog_about_title);
    }

    @Override
    protected boolean isResizable()
    {
        return true;
    }

    @Override
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

        IProduct product = Platform.getProduct();

        {
            Label nameLabel = new Label(group, SWT.NONE);
            nameLabel.setBackground(background);
            nameLabel.setFont(NAME_FONT);
            nameLabel.setText(product.getName());
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalAlignment = GridData.CENTER;
            nameLabel.setLayoutData(gd);
        }

        Label titleLabel = new Label(group, SWT.NONE);
        titleLabel.setBackground(background);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setText(product.getProperty(PRODUCT_PROP_SUB_TITLE));
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalAlignment = GridData.CENTER;
        titleLabel.setLayoutData(gd);
        titleLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(MouseEvent e) {
                BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
                    @Override
                    public void run() {
                        // Do not create InstallationDialog directly
                        // but execute "org.eclipse.ui.help.installationDialog" command
                        IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                        IHandlerService service = workbenchWindow.getService(IHandlerService.class);
                        if (service != null) {
                            try {
                                service.executeCommand("org.eclipse.ui.help.installationDialog", null); //$NON-NLS-1$
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
        gd.grabExcessHorizontalSpace = false;
        imageLabel.setLayoutData(gd);
        imageLabel.setImage(ABOUT_IMAGE);

        Label versionLabel = new Label(group, SWT.NONE);
        versionLabel.setBackground(background);
        versionLabel.setText(CoreMessages.dialog_about_label_version + GeneralUtils.getProductVersion().toString());
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalAlignment = GridData.CENTER;
        versionLabel.setLayoutData(gd);

        Label releaseTimeLabel = new Label(group, SWT.NONE);
        releaseTimeLabel.setBackground(background);
        releaseTimeLabel.setText(DateFormat.getDateInstance(DateFormat.LONG).format(GeneralUtils.getProductReleaseDate()));
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalAlignment = GridData.CENTER;
        releaseTimeLabel.setLayoutData(gd);

        Label authorLabel = new Label(group, SWT.NONE);
        authorLabel.setBackground(background);
        authorLabel.setText(product.getProperty(PRODUCT_PROP_COPYRIGHT));
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalAlignment = GridData.CENTER;
        authorLabel.setLayoutData(gd);

        Link siteLink = UIUtils.createLink(group, UIUtils.makeAnchor(product.getProperty(PRODUCT_PROP_WEBSITE)), new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                UIUtils.launchProgram(e.text);
            }
        });
        siteLink.setBackground(background);
        gd = new GridData();
        gd.horizontalAlignment = GridData.CENTER;
        siteLink.setLayoutData(gd);

        return parent;
    }

}