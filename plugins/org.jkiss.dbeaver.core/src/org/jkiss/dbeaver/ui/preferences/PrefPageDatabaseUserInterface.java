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
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.fieldassist.ComboContentAdapter;
import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.internal.themes.*;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DesktopPlatform;
import org.jkiss.dbeaver.core.ui.services.ApplicationPolicyService;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.app.DBPPlatformLanguage;
import org.jkiss.dbeaver.model.app.DBPPlatformLanguageManager;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.registry.SWTBrowserRegistry;
import org.jkiss.dbeaver.registry.language.PlatformLanguageDescriptor;
import org.jkiss.dbeaver.registry.language.PlatformLanguageRegistry;
import org.jkiss.dbeaver.registry.timezone.TimezoneRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIFontPreferenceManager;
import org.jkiss.dbeaver.ui.UIFonts;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.contentassist.ContentAssistUtils;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorPreferences;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorPreferences.BreadcrumbLocation;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StringUtils;
import org.osgi.service.event.EventHandler;

import java.time.ZoneId;
import java.util.*;
import java.util.List;

/**
 * PrefPageDatabaseUserInterface
 */
public class PrefPageDatabaseUserInterface extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage {

    private static final Log log = Log.getLog(PrefPageDatabaseUserInterface.class);

    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main"; //$NON-NLS-1$

    private static final Collection<String> QUICK_FONT_IDS = List.of(
        UIFonts.DBeaver.MAIN_FONT,
        UIFonts.DBeaver.MONOSPACE_FONT
    );


    private Button automaticUpdateCheck;
    private Combo workspaceLanguage;

    @Nullable
    private Combo clientTimezone;

    private final boolean isStandalone = DesktopPlatform.isStandalone();
    private Combo browserCombo;
    private Button useEmbeddedBrowserAuth;

    private Button statusBarShowBreadcrumbsCheck;
    private Button statusBarShowStatusCheck;
    private Combo statusBarBreadcrumbPositionCombo;

    @Nullable
    private FontsController fontsController = null;

    public PrefPageDatabaseUserInterface()
    {
        super();
        setPreferenceStore(new PreferenceStoreDelegate(DBWorkbench.getPlatform().getPreferenceStore()));
    }

    @Override
    public void init(IWorkbench workbench)
    {

    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 1, 5);

        if (isStandalone && !ApplicationPolicyService.getInstance().isInstallUpdateDisabled()) {
            Composite groupObjects = UIUtils.createTitledComposite(
                composite,
                CoreMessages.pref_page_ui_general_group_general,
                2,
                GridData.VERTICAL_ALIGN_BEGINNING
            );
            automaticUpdateCheck = UIUtils.createCheckbox(
                groupObjects,
                CoreMessages.pref_page_ui_general_checkbox_automatic_updates,
                null,
                false,
                2);
        }
        if (isStandalone) {
            Composite regionalSettingsGroup = UIUtils.createTitledComposite(
                composite,
                CoreMessages.pref_page_ui_general_group_regional,
                2,
                GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
            workspaceLanguage = UIUtils.createLabelCombo(regionalSettingsGroup,
                CoreMessages.pref_page_ui_general_combo_language,
                CoreMessages.pref_page_ui_general_combo_language_tip,
                SWT.READ_ONLY | SWT.DROP_DOWN
            );
            workspaceLanguage.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            List<PlatformLanguageDescriptor> languages = PlatformLanguageRegistry.getInstance().getLanguages();
            DBPPlatformLanguage pLanguage = DBPPlatformDesktop.getInstance().getPlatformLanguage();
            for (int i = 0; i < languages.size(); i++) {
                PlatformLanguageDescriptor lang = languages.get(i);
                workspaceLanguage.add(lang.getLabel());
                if (CommonUtils.equalObjects(pLanguage, lang)) {
                    workspaceLanguage.select(i);
                }
            }
            if (workspaceLanguage.getSelectionIndex() < 0) {
                workspaceLanguage.select(0);
            }

            clientTimezone = UIUtils.createLabelCombo(regionalSettingsGroup,
                CoreMessages.pref_page_ui_general_combo_timezone,
                CoreMessages.pref_page_ui_general_combo_timezone_tip,
                SWT.DROP_DOWN
            );
            clientTimezone.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            clientTimezone.add(DBConstants.DEFAULT_TIMEZONE);
            for (String timezoneName : TimezoneRegistry.getTimezoneNames()) {
                clientTimezone.add(timezoneName);
            }
            clientTimezone.addModifyListener(e -> {
                updateApplyButton();
                getContainer().updateButtons();
            });
            IContentProposalProvider proposalProvider = (contents, position) -> {
                List<IContentProposal> proposals = new ArrayList<>();
                for (String item : clientTimezone.getItems()) {
                    if (StringUtils.containsIgnoreCase(item, contents.toLowerCase())) {
                        proposals.add(new ContentProposal(item));
                    }
                }
                return proposals.toArray(IContentProposal[]::new);
            };
            ContentAssistUtils.installContentProposal(clientTimezone, new ComboContentAdapter(), proposalProvider);

            Control tipLabelRestart = UIUtils.createInfoLabel(regionalSettingsGroup,
                CoreMessages.pref_page_ui_general_label_options_take_effect_after_restart
            );
            tipLabelRestart.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING,
                GridData.VERTICAL_ALIGN_BEGINNING,
                false,
                false,
                2,
                1
            ));

            Composite groupObjects = UIUtils.createTitledComposite(
                composite,
                CoreMessages.pref_page_ui_general_group_browser,
                2,
                GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING
            );
            if (RuntimeUtils.isWindows()) {
                browserCombo = UIUtils.createLabelCombo(groupObjects, CoreMessages.pref_page_ui_general_combo_browser,
                    SWT.READ_ONLY
                );
                browserCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
                for (SWTBrowserRegistry.BrowserSelection value : SWTBrowserRegistry.BrowserSelection.values()) {
                    browserCombo.add(value.getFullName(), value.ordinal());
                }
                Control tipLabel =
                    UIUtils.createInfoLabel(groupObjects, CoreMessages.pref_page_ui_general_combo_browser_tip);
                tipLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING,
                    GridData.VERTICAL_ALIGN_BEGINNING, false, false, 2, 1
                ));
            }

            useEmbeddedBrowserAuth = UIUtils.createCheckbox(groupObjects,
                CoreMessages.pref_page_ui_general_check_browser_auth,
                CoreMessages.pref_page_ui_general_check_browser_auth_tip,
                false,
                2
            );
            useEmbeddedBrowserAuth.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING,
                GridData.VERTICAL_ALIGN_BEGINNING,
                false,
                false,
                2,
                1
            ));
            if (browserCombo != null) {
                browserCombo.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        if (browserCombo.getSelectionIndex() == SWTBrowserRegistry.BrowserSelection.IE.ordinal()) {
                            useEmbeddedBrowserAuth.setEnabled(false);
                            useEmbeddedBrowserAuth.setSelection(false);
                        } else {
                            useEmbeddedBrowserAuth.setEnabled(true);
                        }
                    }
                });
            }

            this.fontsController = this.prepareFontsController(composite, QUICK_FONT_IDS);
        }

        Composite breadcrumbs = UIUtils.createTitledComposite(
            composite,
            CoreMessages.pref_page_ui_status_bar,
            2,
            GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING
        );
        statusBarShowBreadcrumbsCheck = UIUtils.createCheckbox(
            breadcrumbs,
            CoreMessages.pref_page_ui_status_bar_show_breadcrumbs_check_label,
            CoreMessages.pref_page_ui_status_bar_show_breadcrumbs_check_tip,
            true,
            1
        );
        statusBarShowBreadcrumbsCheck.addSelectionListener(SelectionListener.widgetSelectedAdapter(e ->
            statusBarBreadcrumbPositionCombo.setEnabled(statusBarShowBreadcrumbsCheck.getSelection())));

        statusBarBreadcrumbPositionCombo = new Combo(breadcrumbs, SWT.READ_ONLY | SWT.DROP_DOWN);
        statusBarBreadcrumbPositionCombo.add(CoreMessages.pref_page_ui_status_bar_show_breadcrumbs_status_bar_label);
        statusBarBreadcrumbPositionCombo.add(CoreMessages.pref_page_ui_status_bar_show_breadcrumbs_editors_label);
        statusBarBreadcrumbPositionCombo.select(0);

        statusBarShowStatusCheck = UIUtils.createCheckbox(
            breadcrumbs,
            CoreMessages.pref_page_ui_status_bar_show_status_line_check_label,
            CoreMessages.pref_page_ui_status_bar_show_status_line_check_tip,
            true,
            2
        );

        setSettings();
        return composite;
    }

    @NotNull
    private FontsController prepareFontsController(@NotNull Composite parent, @NotNull Collection<String> fontIds) {
        FontsController controller = new FontsController(parent);

        Map<String, Composite> groups = new HashMap<>();
        Composite catContainer = null;

        for (String fontId : fontIds) {
            FontDefinition fontDef = controller.findFontDefinition(fontId);
            if (fontDef != null) {
                catContainer = groups.get(fontDef.getCategoryId());
                if (catContainer == null) {
                    ThemeElementCategory cat = controller.findCategory(fontDef.getCategoryId());
                    if (cat == null) {
                        log.debug(
                            "Failed to find related category '" + fontDef.getCategoryId() + "' " +
                            "for the font definition '" + fontDef.getId() + "'"
                        );
                    }
                    String catName = cat == null ? "Uncategorized font preferences" : cat.getName();
                    String catDescription = cat == null ? null : cat.getDescription();
                    catContainer = UIUtils.createTitledComposite(parent, catName, SWT.NONE);
                    catContainer.getParent().setToolTipText(catDescription);
                    catContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
                    GridLayoutFactory.swtDefaults().margins(0, 3).numColumns(3).applyTo(catContainer);
                    groups.put(fontDef.getCategoryId(), catContainer);
                }

                controller.createEntry(catContainer, fontDef);
            } else {
                log.debug("Failed to find font definition '" + fontId + "' for quick settings of the 'User interface preference page.'");
            }
        }

        controller.refreshAllFontsInitial();

        if (this.getContainer() instanceof IWorkbenchPreferenceContainer wpc && catContainer != null) {
            Composite info = new Composite(catContainer, SWT.NONE);
            GridDataFactory.fillDefaults().span(3, 1).applyTo(info);
            GridLayoutFactory.fillDefaults().margins(0, 0).spacing(2, 2).numColumns(2).applyTo(info);

            UIUtils.createInfoLabel(info, "");
            UIUtils.createPreferenceLink(
                info,
                CoreMessages.pref_page_ui_general_link_more_color_and_font_settings,
                EditorUtils.COLORS_AND_FONTS_PAGE_ID,
                wpc, null
            );
        }

        parent.addDisposeListener(e -> controller.dispose());

        return controller;
    }

    private void setSettings() {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        if (isWindowsDesktopClient()) {
            SWTBrowserRegistry.getActiveBrowser();
            browserCombo.select(SWTBrowserRegistry.getActiveBrowser().ordinal());
            useEmbeddedBrowserAuth.setEnabled(!SWTBrowserRegistry.getActiveBrowser().equals(SWTBrowserRegistry.BrowserSelection.IE));
        }
        if (isStandalone) { 
            if (!ApplicationPolicyService.getInstance().isInstallUpdateDisabled()) {
                automaticUpdateCheck.setSelection(store.getBoolean(DBeaverPreferences.UI_AUTO_UPDATE_CHECK));
            }
            useEmbeddedBrowserAuth.setSelection(store.getBoolean(UIPreferences.UI_USE_EMBEDDED_AUTH));
        }
        final String timezone = store.getString(ModelPreferences.CLIENT_TIMEZONE);
        if (clientTimezone != null) {
            if (DBConstants.DEFAULT_TIMEZONE.equals(timezone)) {
                clientTimezone.setText(DBConstants.DEFAULT_TIMEZONE);
            } else {
                clientTimezone.setText(timezone);
            }
        }

        BreadcrumbLocation breadcrumbLocation = DatabaseEditorPreferences.BreadcrumbLocation.get(store);
        statusBarShowBreadcrumbsCheck.setSelection(breadcrumbLocation != DatabaseEditorPreferences.BreadcrumbLocation.HIDDEN);
        statusBarBreadcrumbPositionCombo.select(breadcrumbLocation == DatabaseEditorPreferences.BreadcrumbLocation.IN_EDITORS ? 1 : 0);
        statusBarBreadcrumbPositionCombo.setEnabled(statusBarShowBreadcrumbsCheck.getSelection());
        statusBarShowStatusCheck.setSelection(store.getBoolean(DBeaverPreferences.UI_STATUS_BAR_SHOW_STATUS_LINE));
    }

    @Override
    protected void performDefaults() {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        if (isStandalone) {
            useEmbeddedBrowserAuth.setSelection(store.getDefaultBoolean(UIPreferences.UI_USE_EMBEDDED_AUTH));
            if (!ApplicationPolicyService.getInstance().isInstallUpdateDisabled()) {
                automaticUpdateCheck.setSelection(store.getDefaultBoolean(DBeaverPreferences.UI_AUTO_UPDATE_CHECK));
            }
        }
        if (isWindowsDesktopClient()) {
            SWTBrowserRegistry.getActiveBrowser();
            browserCombo.select(SWTBrowserRegistry.getDefaultBrowser().ordinal());
        }
        if (clientTimezone != null) {
            UIUtils.setComboSelection(clientTimezone, store.getDefaultString(ModelPreferences.CLIENT_TIMEZONE));
        }

        BreadcrumbLocation location = BreadcrumbLocation.getDefault(store);
        statusBarShowBreadcrumbsCheck.setSelection(location != BreadcrumbLocation.HIDDEN);
        statusBarBreadcrumbPositionCombo.select(location == BreadcrumbLocation.IN_STATUS_BAR ? 0 : 1);
        statusBarShowStatusCheck.setSelection(store.getDefaultBoolean(DBeaverPreferences.UI_STATUS_BAR_SHOW_STATUS_LINE));

        if (this.fontsController != null) {
            this.fontsController.resetToDefaults();
        }
    }

    private boolean isWindowsDesktopClient() {
        return isStandalone && RuntimeUtils.isWindows();
    }

    @Override
    public boolean isValid() {
        return super.isValid();
    }

    @Override
    public boolean performOk()
    {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        if (isStandalone) {
            store.setValue(UIPreferences.UI_USE_EMBEDDED_AUTH, useEmbeddedBrowserAuth.getSelection());
            if (!ApplicationPolicyService.getInstance().isInstallUpdateDisabled()) {
                store.setValue(DBeaverPreferences.UI_AUTO_UPDATE_CHECK, automaticUpdateCheck.getSelection());
            } else {
                store.setValue(DBeaverPreferences.UI_AUTO_UPDATE_CHECK, Boolean.FALSE);
            }


            if (isWindowsDesktopClient()) {
                SWTBrowserRegistry.setActiveBrowser(SWTBrowserRegistry.BrowserSelection.values()[browserCombo.getSelectionIndex()]);
            }

            PrefUtils.savePreferenceStore(store);
            if (clientTimezone != null) {
                if (DBConstants.DEFAULT_TIMEZONE.equals(clientTimezone.getText())) {
                    TimezoneRegistry.setDefaultZone(null, true);
                } else {
                    TimezoneRegistry.setDefaultZone(
                        ZoneId.of(TimezoneRegistry.extractTimezoneId(clientTimezone.getText())), true);
                }
            }

            BreadcrumbLocation breadcrumbLocation;
            if (!statusBarShowBreadcrumbsCheck.getSelection()) {
                breadcrumbLocation = DatabaseEditorPreferences.BreadcrumbLocation.HIDDEN;
            } else if (statusBarBreadcrumbPositionCombo.getSelectionIndex() == 0) {
                breadcrumbLocation = DatabaseEditorPreferences.BreadcrumbLocation.IN_STATUS_BAR;
            } else {
                breadcrumbLocation = DatabaseEditorPreferences.BreadcrumbLocation.IN_EDITORS;
            }

            store.setValue(DBeaverPreferences.UI_STATUS_BAR_SHOW_BREADCRUMBS, breadcrumbLocation.name());
            store.setValue(DBeaverPreferences.UI_STATUS_BAR_SHOW_STATUS_LINE, statusBarShowStatusCheck.getSelection());

            if (workspaceLanguage.getSelectionIndex() >= 0) {
                PlatformLanguageDescriptor language = PlatformLanguageRegistry.getInstance().getLanguages()
                    .get(workspaceLanguage.getSelectionIndex());
                DBPPlatformLanguage curLanguage = DBPPlatformDesktop.getInstance().getPlatformLanguage();

                if (curLanguage != language) {
                    if (DBWorkbench.getPlatform() instanceof DBPPlatformLanguageManager languageManager) {
                        languageManager.setPlatformLanguage(language);
                    }
                    if (UIUtils.confirmAction(
                        getShell(),
                        "Restart " + GeneralUtils.getProductName(),
                        "You need to restart " + GeneralUtils.getProductName()
                            + " to perform actual language change.\nDo you want to restart?"
                    )) {
                        restartWorkbenchOnPrefChange();
                    }
                }
            }
        }

        if (this.fontsController != null) {
            this.fontsController.apply();
        }

        return true;
    }

    @Nullable
    @Override
    public IAdaptable getElement() {
        return null;
    }

    @Override
    public void setElement(IAdaptable element) {
    }

    private static class FontsController extends UIFontPreferenceManager {

        private class FontEntry {
            private static final ResourceBundle SWT_RESOURCE_BUNDLE = ResourceBundle.getBundle(ColorsAndFontsPreferencePage.class.getName());

            @NotNull
            private final FontDefinition definition;
            @NotNull
            private final Label example;

            @Nullable
            private Font currentFont = null;
            @Nullable
            private Font customFont = null;

            public FontEntry(@NotNull Composite container, @NotNull FontDefinition fontDef) {
                this.definition = fontDef;

                Label title  = UIUtils.createLabel(container, fontDef.getName() + ": ");
                title.setToolTipText(fontDef.getDescription());

                this.example = UIUtils.createLabel(container, "<font example placeholder>");
                this.example.setToolTipText(fontDef.getDescription());

                UIUtils.createPushButton(
                    container, null, CoreMessages.pref_page_user_interface_fonts_modify_tooltip, UIIcon.EDIT,
                    SelectionListener.widgetSelectedAdapter(e -> {
                        final FontDialog fontDialog = new FontDialog(container.getShell());
                        fontDialog.setEffectsVisible(false);
                        fontDialog.setFontList(this.example.getFont().getFontData());
                        final FontData data = fontDialog.open();
                        if (data != null) {
                            this.setFont(fontDialog.getFontList());
                        }
                    })
                );
            }

            private void setFont(@NotNull FontData[] fontData) {
                final Font oldFont = this.customFont;
                this.customFont = new Font(this.example.getFont().getDevice(), fontData);
                this.example.setFont(this.customFont);
                this.example.setText(this.prepareFontDescription(this.customFont));
                if (oldFont != null) {
                    oldFont.dispose();
                }
                updateLayout(this.example, null);
            }

            public void refresh(@NotNull FontRegistry fonts) {
                this.currentFont = fonts.get(this.definition.getId());
                this.example.setFont(this.currentFont);
                if (this.currentFont != null) {
                    this.example.setText(this.prepareFontDescription(this.currentFont));
                }
                this.releaseCustomFontIfExists();
            }

            public void resetToDefault() {
                this.setFont(getDefaultFontData(this.definition));
            }

            public boolean apply() {
                if (this.customFont != null) {
                    setFontPreference(this.definition, this.customFont.getFontData());
                    return true;
                } else {
                    return false;
                }
            }

            @NotNull
            private String prepareFontDescription(@NotNull Font currentFont) {
                FontData[] fontData = currentFont.getFontData();

                // recalculate sample text
                StringBuilder tmp = new StringBuilder();
                for (FontData currentFontData : fontData) {
                    tmp.append(currentFontData.getName());
                    tmp.append(' ');
                    tmp.append(currentFontData.getHeight());

                    int style = currentFontData.getStyle();
                    if ((style & SWT.BOLD) != 0) {
                        tmp.append(' ');
                        tmp.append(SWT_RESOURCE_BUNDLE.getString("boldFont")); //$NON-NLS-1$
                    }
                    if ((style & SWT.ITALIC) != 0) {
                        tmp.append(' ');
                        tmp.append(SWT_RESOURCE_BUNDLE.getString("italicFont")); //$NON-NLS-1$
                    }
                }
                return tmp.toString();
            }

            public void releaseCustomFontIfExists() {
                if (this.customFont != null) {
                    this.customFont.dispose();
                    this.customFont = null;
                }
            }
        }

        @NotNull
        private final IPropertyChangeListener themeChangeListener = event -> {
            if (this.isThemeChanged() || event.getProperty().equals(IThemeManager.CHANGE_CURRENT_THEME)) {
                this.refreshAllFonts();
            }
        };

        @NotNull
        private final EventHandler themeRegistryRestyledHandler = e -> {
            if (this.isThemeChanged()) {
                this.refreshAllFonts();
            }
        };

        @NotNull
        private final IPropertyChangeListener currentThemeListener = event -> {
            if (event.getSource() instanceof FontRegistry) {
                String fontId = event.getProperty();
                FontData[] fontData = (FontData[]) event.getNewValue();
                this.refreshFont(fontId, fontData);
            }
        };

        @NotNull
        private final Composite container;
        @NotNull
        private final Map<String, FontEntry> fontEntriesById = new HashMap<>();

        @NotNull
        private final IEventBroker eventBroker;

        public FontsController(@NotNull Composite container) {
            this.container = container;
            this.eventBroker = this.workbench.getService(IEventBroker.class);

            this.workbench.getThemeManager().addPropertyChangeListener(this.themeChangeListener);
            this.eventBroker.subscribe(WorkbenchThemeManager.Events.THEME_REGISTRY_RESTYLED, this.themeRegistryRestyledHandler);

            this.currentTheme.addPropertyChangeListener(this.currentThemeListener);
        }

        public void createEntry(@NotNull Composite catContainer, @NotNull FontDefinition fontDef) {
            this.fontEntriesById.put(fontDef.getId(), new FontEntry(catContainer, fontDef));
        }

        private void refreshFont(@NotNull String fontId, @NotNull FontData[] fontData) {
            FontEntry entry = this.fontEntriesById.get(fontId);
            if (entry != null) {
                FontRegistry fonts = UIUtils.getCurrentTheme().getFontRegistry();
                entry.refresh(fonts);
                this.updateLayout(entry.example, null);
            }
        }

        public void refreshAllFonts() {
            this.refreshAllFontsImpl(true);
        }

        public void refreshAllFontsInitial() {
            this.refreshAllFontsImpl(false);
        }

        private void refreshAllFontsImpl(boolean updateLayout) {
            FontRegistry fonts = UIUtils.getCurrentTheme().getFontRegistry();
            for (FontEntry entry : this.fontEntriesById.values()) {
                entry.refresh(fonts);
                if (updateLayout) {
                    this.updateLayout(entry.example, this.container);
                }
            }
            if (updateLayout) {
                this.updateLayout(this.container, null);
            }
        }

        public void resetToDefaults() {
            for (FontEntry entry : this.fontEntriesById.values()) {
                entry.resetToDefault();
            }

            this.updateLayout(this.container, null);
        }

        public void apply() {
            boolean changesMade = false;
            for (FontEntry entry : this.fontEntriesById.values()) {
                changesMade |= entry.apply();
            }
            if (changesMade) {
                this.savePrefs();
                eventBroker.send(WorkbenchThemeManager.Events.THEME_REGISTRY_MODIFIED, null);
            }
        }

        private void updateLayout(@NotNull Control start, @Nullable Control end) {
            for (Control cc = start, prev = null; cc != null && cc != end; cc = cc.getParent()) {
                if (cc instanceof ScrolledComposite sc) {
                    sc.layout(true, true);
                    if (prev != null) {
                        sc.setMinSize(prev.computeSize(SWT.DEFAULT, SWT.DEFAULT));
                    }
                    break;
                } else {
                    cc.pack(true);
                }
                prev = cc;
            }
        }

        private boolean isThemeChanged() {
            ITheme actualTheme = this.getCurrentTheme();
            org.eclipse.e4.ui.css.swt.theme.ITheme actualCssTheme = this.getCurrentCssTheme();
            boolean changed = this.currentTheme != actualTheme || this.currentCssTheme != actualCssTheme;
            if (changed) {
                this.currentTheme.removePropertyChangeListener(this.currentThemeListener);
                this.currentTheme = actualTheme;
                this.currentCssTheme = actualCssTheme;
                this.currentTheme.addPropertyChangeListener(this.currentThemeListener);
            }
            return changed;
        }

        public void dispose() {
            this.currentTheme.removePropertyChangeListener(this.currentThemeListener);
            this.eventBroker.unsubscribe(themeRegistryRestyledHandler);
            this.workbench.getThemeManager().removePropertyChangeListener(themeChangeListener);
            this.fontEntriesById.values().forEach(FontEntry::releaseCustomFontIfExists);
            this.fontEntriesById.clear();
        }
    }
}
