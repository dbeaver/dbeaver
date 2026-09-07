/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.app.standalone.tipoftheday;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.stm.STMUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.BaseThemeSettings;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.AbstractPopupPanel;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.net.URI;
import java.util.*;

public class ShowTipOfTheDayDialog extends AbstractPopupPanel {
    private static final String UI_SHOW_TIP_OF_THE_DAY_ON_STARTUP = "ui.show.tip.of.the.day.on.startup";
    private static final Log log = Log.getLog(ShowTipOfTheDayDialog.class);

    private static final String DIALOG_ID = "DBeaver." + ShowTipOfTheDayDialog.class.getSimpleName();

    private final List<Tip> tips;
    private boolean displayShowOnStartup;
    private StyledText tipText;
    private List<LinkRange> links = List.of();
    private int tipIndex;

    public ShowTipOfTheDayDialog(@NotNull Shell parentShell, @NotNull List<Tip> tips) {
        super(parentShell, TipOfTheDayMessages.tip_of_the_day_title);
        this.tips = List.copyOf(tips);
        setModeless(true);
        setBlockOnOpen(false);
    }

    public static boolean isShowOnStartup() {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        return CommonUtils.toBoolean(store.getString(UI_SHOW_TIP_OF_THE_DAY_ON_STARTUP), true);
    }

    public static void setShowOnStartup(boolean showOnStartup) {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        store.setValue(UI_SHOW_TIP_OF_THE_DAY_ON_STARTUP, String.valueOf(showOnStartup));
    }

    @Nullable
    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    @NotNull
    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        getShell().setText(TipOfTheDayMessages.tip_of_the_day_title);

        Font dialogFont = BaseThemeSettings.instance.baseFont;
        FontData[] fontData = dialogFont.getFontData();
        for (int i = 0; i < fontData.length; i++) {
            FontData fd = fontData[i];
            fontData[i] = new FontData(fd.getName(), fd.getHeight() + 1, SWT.NONE);
        }
        Font largeFont = new Font(dialogFont.getDevice(), fontData);
        parent.addDisposeListener(e -> largeFont.dispose());

        this.tipIndex = new Random(System.currentTimeMillis()).nextInt(this.tips.size());

        Composite dialogArea = super.createDialogArea(parent);

        Composite tipArea = new Composite(dialogArea, SWT.BORDER);
        tipArea.setLayoutData(new GridData(GridData.FILL_BOTH));
        tipArea.setLayout(GridLayoutFactory.fillDefaults().create());

        this.tipText = new StyledText(tipArea, SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL | SWT.NO_FOCUS);
        this.tipText.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).hint(300, 100).create());
        this.tipText.setMargins(5, 5, 5, 5);
        this.tipText.setFont(largeFont);
        this.tipText.addMouseListener(MouseListener.mouseUpAdapter(e -> {
            LinkRange link = this.getLinkAt(new Point(e.x, e.y));
            if (link != null) {
                this.navigateLink(link.href());
            }
        }));
        this.tipText.addMouseMoveListener(e -> {
            LinkRange link = this.getLinkAt(new Point(e.x, e.y));
            this.tipText.setCursor(link == null ? null : this.tipText.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
        });
        showTip();

        if (this.displayShowOnStartup) {
            Button showTipButton = UIUtils.createCheckbox(
                dialogArea,
                TipOfTheDayMessages.show_tips_on_startup,
                isShowOnStartup()
            );

            showTipButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e ->
                setShowOnStartup(showTipButton.getSelection())));
        }

        return dialogArea;
    }

    @Override
    protected boolean needsButtonBar() {
        return true;
    }

    private void navigateLink(@NotNull String href) {
        final URI uri = URI.create(href);
        switch (uri.getScheme()) {
            case "http":
            case "https":
                ShellUtils.launchProgram(href);
                break;
            case "prefs":
                close();
                UIUtils.asyncExec(() -> {
                    Object element = null;

                    if (uri.getFragment() != null) {
                        if (uri.getFragment().equals("project")) {
                            element = DBWorkbench.getPlatform().getNavigatorModel().getRoot()
                                .getProjectNode(DBWorkbench.getPlatform().getWorkspace().getActiveProject());
                        } else {
                            log.warn("Unknown element type: '" + uri.getFragment() + "'");
                        }
                    }

                    UIUtils.showPreferencesFor(UIUtils.getActiveWorkbenchShell(), element, uri.getHost());
                });
                break;
            case "view":
                close();
                UIUtils.asyncExec(() -> {
                    try {
                        UIUtils.getActiveWorkbenchWindow().getActivePage().showView(uri.getHost());
                    } catch (PartInitException e1) {
                        DBWorkbench.getPlatformUI().showError("Open view", "Error opening view " + uri.getHost(), e1);
                    }
                });
                break;
            default:
                log.warn("Unknown scheme: '" + uri.getScheme() + "'");
                break;
        }
    }

    private void showTip() {
        Tip tip = this.tips.get(this.tipIndex);
        List<StyleRange> styles = new ArrayList<>();
        List<LinkRange> parsedLinks = new ArrayList<>();
        for (Tip.Style tipStyle : tip.styles()) {
            int fontStyle = SWT.NORMAL;
            if (tipStyle.bold()) {
                fontStyle |= SWT.BOLD;
            }
            if (tipStyle.italic()) {
                fontStyle |= SWT.ITALIC;
            }
            StyleRange style = new StyleRange(tipStyle.start(), tipStyle.length(), null, null, fontStyle);
            style.underline = tipStyle.underline() || tipStyle.href() != null;
            style.underlineStyle = tipStyle.href() != null ? SWT.UNDERLINE_LINK : SWT.UNDERLINE_SINGLE;
            if (tipStyle.href() != null) {
                style.foreground = JFaceColors.getHyperlinkText(this.tipText.getDisplay());
                parsedLinks.add(new LinkRange(
                    tipStyle.start(),
                    tipStyle.start() + tipStyle.length(),
                    tipStyle.href()
                ));
            }
            styles.add(style);
        }
        this.tipText.setText(tip.text());
        this.tipText.setStyleRanges(styles.toArray(StyleRange[]::new));
        parsedLinks.sort(Comparator.comparingInt(LinkRange::start)); // links are already sorted implicitly, so just ensure - its fast
        this.links = parsedLinks;
        this.tipText.setTopIndex(0);
    }

    @Nullable
    private LinkRange getLinkAt(@NotNull Point point) {
        int offset = this.tipText.getOffsetAtPoint(point);
        if (offset < 0) {
            return null;
        }
        int index = STMUtils.binarySearchByKey(this.links, LinkRange::start, offset, Integer::compare);
        if (index < 0) {
            index = ~index - 1;
        }
        if (index < 0) {
            return null;
        }
        LinkRange link = this.links.get(index);
        return offset < link.end() ? link : null;
    }

    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent) {
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

    public void setDisplayShowOnStartup(boolean displayShowOnStartup) {
        this.displayShowOnStartup = displayShowOnStartup;
    }

    private record LinkRange(int start, int end, @NotNull String href) {
    }
}
