/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.ui.tipoftheday;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ShowTipOfTheDayDialog extends BaseDialog {
    private static final Log log = Log.getLog(ShowTipOfTheDayDialog.class);

    private static final String DIALOG_ID = "DBeaver." + ShowTipOfTheDayDialog.class.getSimpleName();

    private final List<String> tips = new ArrayList<>();
    private Composite tipArea;
    private boolean displayShowOnStartup;
    private boolean showOnStartup;
    private FormText formText;
    private int tipIndex;

    public ShowTipOfTheDayDialog(Shell parentShell) {
        super(parentShell, "Tip of the day", DBIcon.TREE_INFO);
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings()
    {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    public void addTip(String tip) {
        this.tips.add(tip);
    }

    @Override
    protected Control createContents(Composite parent) {
        //[dbeaver/dbeaver#11526]
        Control contents = super.createContents(parent);
        UIUtils.asyncExec(() -> {
            if (!tipArea.isDisposed()) {
                tipArea.layout();
            }
        });
        return contents;
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        getShell().setText("Tip of the day");
        setTitle("Tip of the day");

        if (tips.isEmpty()) {
            tips.add("Empty tip list");
        }

        tipIndex = new Random(System.currentTimeMillis()).nextInt(tips.size());


        Font dialogFont = JFaceResources.getDialogFont();
        FontData[] fontData = dialogFont.getFontData();
        for (int i = 0; i < fontData.length; i++) {
            FontData fd = fontData[i];
            fontData[i] = new FontData(fd.getName(), fd.getHeight() + 1, SWT.NONE);
        }
        Font largeFont = new Font(dialogFont.getDevice(), fontData);
        parent.addDisposeListener(e -> largeFont.dispose());

        Composite dialogArea = super.createDialogArea(parent);

        tipArea = new Composite(dialogArea, SWT.BORDER);
        tipArea.setLayoutData(new GridData(GridData.FILL_BOTH));
        GridLayout gl = new GridLayout(1, false);
        gl.marginWidth = 0;
        gl.marginHeight = 0;
        tipArea.setLayout(gl);

        FormToolkit toolkit = new FormToolkit(parent.getDisplay());
        toolkit.setBorderStyle(SWT.NULL);
        Form form = toolkit.createForm(tipArea);
        form.setLayoutData(new GridData(GridData.FILL_BOTH));
        form.setLayout(new GridLayout(1, true));
        //form.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
        form.getBody().setLayoutData(new GridData(GridData.FILL_BOTH));
        form.getBody().setLayout(new GridLayout(1, true));

        formText = new FormText(form.getBody(), SWT.WRAP | SWT.NO_FOCUS);
        formText.marginWidth = 1;
        formText.marginHeight = 0;
        formText.setHyperlinkSettings(toolkit.getHyperlinkGroup());
        toolkit.adapt(formText, false, false);
        formText.setMenu(form.getBody().getMenu());

            //toolkit.createFormText(form.getBody(), false);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 300;
        gd.heightHint = 100;
        formText.setLayoutData(gd);
        formText.setFont(largeFont);
        formText.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            public void linkActivated(HyperlinkEvent e) {
                navigateLink(e);
            }
        });
        showTip();

        if (displayShowOnStartup) {
            Button showTipButton = toolkit.createButton(form.getBody(), "Show tips on startup", SWT.CHECK);
            showTipButton.setSelection(showOnStartup);
            showTipButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    showOnStartup = showTipButton.getSelection();
                }
            });

            form.getBody().setTabList(new Control[] { showTipButton });
        }

        return dialogArea;
    }

    private void navigateLink(HyperlinkEvent e) {
        Object href = e.getHref();
        if (href != null) {
            String linkURL = href.toString();
            if (linkURL.startsWith("http:") || linkURL.startsWith("https:")) {
                ShellUtils.launchProgram(linkURL);
            } else if (linkURL.startsWith("prefs:")) {
                String prefPageId = linkURL.substring(linkURL.indexOf("//") + 2);
                buttonPressed(IDialogConstants.OK_ID);
                UIUtils.asyncExec(() -> {
                    UIUtils.showPreferencesFor(
                        UIUtils.getActiveWorkbenchShell(),
                        null,
                        prefPageId);
                });
            } else if (linkURL.startsWith("view:")) {
                String viewId = linkURL.substring(linkURL.indexOf("//") + 2);
                buttonPressed(IDialogConstants.OK_ID);
                UIUtils.asyncExec(() -> {
                    try {
                        UIUtils.getActiveWorkbenchWindow().getActivePage().showView(viewId);
                    } catch (PartInitException e1) {
                        DBWorkbench.getPlatformUI().showError("Open view", "Error opening view " + viewId, e1);
                    }
                });
            }
        }
    }

    private void showTip() {
        String tipText = "<form><p>" + tips.get(tipIndex) + "</p></form>";
        try {
            formText.setText(tipText, true, false);
        } catch (Exception e) {
            log.error(e);
        }
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.BACK_ID, IDialogConstants.BACK_LABEL, false);
        createButton(parent, IDialogConstants.NEXT_ID, IDialogConstants.NEXT_LABEL, false);
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, true);

        UIUtils.asyncExec(() -> {
            Button okButton = getButton(IDialogConstants.OK_ID);
            if (okButton != null) {
                okButton.setFocus();
            }
        });
    }

    @Override
    protected void buttonPressed(int buttonId) {
        switch (buttonId) {
            case IDialogConstants.BACK_ID: {
                tipIndex = tipIndex == 0 ? tips.size() - 1 : tipIndex - 1;
                showTip();
                return;
            }
            case IDialogConstants.NEXT_ID: {
                tipIndex = tipIndex == tips.size() - 1 ? 0 : tipIndex + 1;
                showTip();
                return;
            }
        }
        super.buttonPressed(buttonId);
    }

    public boolean isDisplayShowOnStartup() {
        return displayShowOnStartup;
    }

    public void setDisplayShowOnStartup(boolean displayShowOnStartup) {
        this.displayShowOnStartup = displayShowOnStartup;
    }

    public boolean isShowOnStartup() {
        return showOnStartup;
    }

    public void setShowOnStartup(boolean showOnStartup) {
        this.showOnStartup = showOnStartup;
    }
}
