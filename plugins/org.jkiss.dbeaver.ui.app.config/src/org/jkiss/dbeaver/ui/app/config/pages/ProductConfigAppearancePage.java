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
package org.jkiss.dbeaver.ui.app.config.pages;

import org.eclipse.e4.ui.css.swt.theme.ITheme;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.navigator.DBNBrowseSettings;
import org.jkiss.dbeaver.registry.DataSourceNavigatorSettings;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.app.config.nls.ProductConfigMessages;
import org.jkiss.dbeaver.ui.dialogs.connection.EditConnectionNavigatorSettingsDialog;
import org.jkiss.dbeaver.ui.forms.*;

import java.util.function.Consumer;

public class ProductConfigAppearancePage extends ProductConfigWizardPage {
    private final IThemeEngine themeEngine;
    private final UIObservable<ITheme> currentTheme;

    private final UIObservable<Preset> navigatorPreset;
    private final UIObservable<DBNBrowseSettings> navigatorSettings;

    private enum Preset {
        SIMPLE,
        ADVANCED,
        CUSTOM
    }

    public ProductConfigAppearancePage() {
        super(ProductConfigMessages.appearance_title, ProductConfigMessages.appearance_description);

        themeEngine = PlatformUI.getWorkbench().getService(IThemeEngine.class);
        currentTheme = UIObservable.of(themeEngine.getActiveTheme(), ITheme.class);
        currentTheme.addChangeListener((ignored, theme) -> {
            themeEngine.setTheme(theme, false);
            getWizard().markForRestart();
        });

        var settings = DataSourceNavigatorSettings.getDefaultSettings();
        navigatorPreset = UIObservable.of(determinePresetFromSettings(settings));
        navigatorSettings = UIObservable.of(settings, DBNBrowseSettings.class);
        navigatorSettings.addChangeListener((o, n) -> navigatorPreset.set(determinePresetFromSettings(n)));
    }

    @Override
    public void createControl(@NotNull Composite parent) {
        setControl(UIPanelBuilder.build(parent, buildPanel()));
    }

    @Override
    public boolean isPageApplicable() {
        return DBWorkbench.getPlatform().getApplication().isStandalone();
    }

    @Override
    public void applySettings() {
        // The only difference from the change listener is that we persist the theme here
        themeEngine.setTheme(currentTheme.get(), true);

        var preset = navigatorPreset.get();
        var settings = determineSettingsFromPreset(preset);
        DataSourceNavigatorSettings.setDefaultSettings(settings);
    }

    @NotNull
    private Consumer<UIPanelBuilder> buildPanel() {
        return pb -> pb
            .margins(10, 10)
            .row(rb -> rb.label(ProductConfigMessages.appearance_theme_header))
            .indent(pb1 -> {
                for (ITheme theme : themeEngine.getThemes()) {
                    pb1.row(rb -> rb.radioButton(theme.getLabel(), UIObservables.equals(currentTheme, theme)));
                }
            })
            .row(rb -> rb.label(ProductConfigMessages.appearance_navigator_header))
            .indent(pb1 -> pb1
                .row(rb -> rb.radioButton(
                    ProductConfigMessages.appearance_navigator_simple,
                    UIObservables.equals(navigatorPreset, Preset.SIMPLE)))
                .row(rb -> rb.radioButton(
                    ProductConfigMessages.appearance_navigator_advanced,
                    UIObservables.equals(navigatorPreset, Preset.ADVANCED)))
                .row(rb -> rb
                    .radioButton(
                        ProductConfigMessages.appearance_navigator_custom,
                        UIObservables.equals(navigatorPreset, Preset.CUSTOM))
                    .link(ProductConfigMessages.appearance_navigator_custom_configure, e -> {
                        var dialog = new EditConnectionNavigatorSettingsDialog(
                            getShell(),
                            navigatorSettings.get(),
                            null);
                        if (dialog.open() == IDialogConstants.OK_ID) {
                            navigatorSettings.set(dialog.getNavigatorSettings());
                        }
                    }))
                .row(rb -> rb.weblink(ProductConfigMessages.appearance_navigator_hint)))
            .row(UIRowBuilder::verticalSpacer)
            .row(rb -> rb
                .panel(pb1 -> pb1
                    .align(UIAlignX.FILL)
                    .grow(UIGrowX.ALWAYS)
                    .row(rb1 -> rb1
                    // TODO introduce a dedicated icon+label control
                    .label(lb -> lb
                        .image(DBIcon.SMALL_INFO)
                        .align(UIAlignY.TOP))
                    .label(lb -> lb
                        .text(ProductConfigMessages.appearance_theme_hint)
                        .wrap()
                        .align(UIAlignX.FILL)
                        .grow(UIGrowX.ALWAYS)))));
    }

    @NotNull
    private Preset determinePresetFromSettings(@NotNull DBNBrowseSettings settings) {
        if (settings.equals(DataSourceNavigatorSettings.PRESET_SIMPLE.getSettings())) {
            return Preset.SIMPLE;
        } else if (settings.equals(DataSourceNavigatorSettings.PRESET_ADVANCED.getSettings())) {
            return Preset.ADVANCED;
        } else {
            return Preset.CUSTOM;
        }
    }

    @NotNull
    private DBNBrowseSettings determineSettingsFromPreset(@NotNull Preset preset) {
        return switch (preset) {
            case SIMPLE -> DataSourceNavigatorSettings.PRESET_SIMPLE.getSettings();
            case ADVANCED -> DataSourceNavigatorSettings.PRESET_ADVANCED.getSettings();
            case CUSTOM -> navigatorSettings.get();
        };
    }
}
