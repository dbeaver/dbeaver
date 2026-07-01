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
package org.jkiss.dbeaver.ui;

import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.themes.*;
import org.eclipse.ui.internal.util.PrefUtil;
import org.eclipse.ui.themes.ITheme;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UIFontPreferenceManager {

    @NotNull
    private final IPreferenceStore fontsPrefStore;

    @NotNull
    protected final IWorkbench workbench;
    @Nullable
    protected final IThemeEngine themeEngine;

    @NotNull
    public final IThemeRegistry themeRegistry;

    @NotNull
    protected ITheme currentTheme;
    @Nullable
    protected org.eclipse.e4.ui.css.swt.theme.ITheme currentCssTheme;

    @Nullable
    private Map<String, FontDefinition> allFontDefsById = null;

    @Nullable
    private Map<String, ThemeElementCategory> categoryById = null;

    public UIFontPreferenceManager() {
        this.fontsPrefStore = PrefUtil.getInternalPreferenceStore();

        this.workbench = Workbench.getInstance();
        this.themeEngine = this.workbench.getService(IThemeEngine.class);

        this.themeRegistry = WorkbenchPlugin.getDefault().getThemeRegistry();

        this.currentTheme = this.getCurrentTheme();
        this.currentCssTheme = this.getCurrentCssTheme();
    }

    @Nullable
    public FontDefinition findFontDefinition(@NotNull String fontId) {
        if (this.allFontDefsById == null) {
            this.allFontDefsById = Stream.of(this.themeRegistry.getFonts())
                .collect(Collectors.toMap(ThemeElementDefinition::getId, Function.identity()));
        }
        return this.allFontDefsById.get(fontId);
    }

    @Nullable
    public ThemeElementCategory findCategory(@NotNull String categoryId) {
        if (this.categoryById == null) {
            this.categoryById = Stream.of(this.themeRegistry.getCategories())
                .collect(Collectors.toMap(ThemeElementCategory::getId, Function.identity()));
        }
        return this.categoryById.get(categoryId);
    }

    public void setFontPreference(@NotNull String fontId, @NotNull FontData[] fontData) {
        FontDefinition fontDef = this.findFontDefinition(fontId);
        if (fontDef != null) {
            this.setFontPreference(fontDef, fontData);
        }
    }

    public void setFontPreference(@NotNull FontDefinition definition, @NotNull FontData[] fontData) {
        FontRegistry fontRegistry = UIUtils.getCurrentTheme().getFontRegistry();

        Set<FontDefinition> fontPreferencesToSet = new LinkedHashSet<>();
        this.collectFontsToUpdate(fontRegistry, definition, fontPreferencesToSet);
        this.putToPreferenceStore(fontPreferencesToSet, fontData);
    }

    private void putToPreferenceStore(
        @NotNull Set<FontDefinition> fontPreferencesToSet,
        @NotNull FontData[] data
    ) {
        // see org.eclipse.ui.internal.themes.ColorsAndFontsPreferencePage::performFontOk()
        FontRegistry fonts = UIUtils.getCurrentTheme().getFontRegistry();
        String fdString = PreferenceConverter.getStoredRepresentation(data);
        for (FontDefinition def : fontPreferencesToSet) {
            String key = createPreferenceKey(def);
            String storeString = this.fontsPrefStore.getString(key);
            def.appendState(ThemeElementDefinition.State.MODIFIED_BY_USER);

            if (!fdString.equals(storeString)) {
                this.fontsPrefStore.setValue(key, fdString);
            }

            fonts.put(def.getId(), data);
        }
    }

    private void collectFontsToUpdate(
        @NotNull FontRegistry fontRegistry,
        @NotNull FontDefinition definition,
        @NotNull Set<FontDefinition> fontPreferencesToSet
    ) {
        // see org.eclipse.ui.internal.themes.ColorsAndFontsPreferencePage::setFontPreferenceValue()
        for (FontDefinition fontDefinition : this.getDescendantFonts(definition)) {
            if (isDefault(fontRegistry, fontDefinition)) {
                this.collectFontsToUpdate(fontRegistry, fontDefinition, fontPreferencesToSet);
            }
        }
        fontPreferencesToSet.add(definition);
    }

    private boolean isDefault(@NotNull FontRegistry fontRegistry, @NotNull FontDefinition fontDef) {
        String defaultFontID = fontDef.getDefaultsTo();
        return defaultFontID != null && Arrays.equals(fontRegistry.getFontData(fontDef.getId()), fontRegistry.getFontData(defaultFontID));
    }

    @NotNull
    protected String createPreferenceKey(@NotNull FontDefinition definition) {
        if (definition.isOverridden() || definition.isAddedByCss()) {
            return ThemeElementHelper.createPreferenceKey(currentCssTheme, currentTheme, definition.getId());
        }
        return ThemeElementHelper.createPreferenceKey(currentTheme, definition.getId());
    }

    @NotNull
    protected ITheme getCurrentTheme() {
        return this.workbench.getThemeManager().getCurrentTheme();
    }

    @Nullable
    protected org.eclipse.e4.ui.css.swt.theme.ITheme getCurrentCssTheme() {
        if (this.themeEngine != null) {
            return this.themeEngine.getActiveTheme();
        }
        return null;
    }

    @NotNull
    protected FontData[] getFontAncestorValue(@NotNull IPreferenceStore fontsPrefStore, @NotNull FontDefinition definition) {
        FontDefinition ancestor = this.getFontAncestor(definition);
        if (ancestor == null) {
            return PreferenceConverter.getDefaultFontDataArray(fontsPrefStore, createPreferenceKey(definition));
        }
        return currentTheme.getFontRegistry().getFontData(ancestor.getId());
    }

    @Nullable
    private FontDefinition getFontAncestor(@NotNull FontDefinition definition) {
        String defaultsTo = definition.getDefaultsTo();
        if (defaultsTo == null) {
            return null;
        }
        return themeRegistry.findFont(defaultsTo);
    }

    @NotNull
    private FontDefinition[] getDescendantFonts(@NotNull FontDefinition definition) {
        List<FontDefinition> list = new ArrayList<>(5);
        String id = definition.getId();

        FontDefinition[] fonts = themeRegistry.getFonts();
        FontDefinition[] sorted = new FontDefinition[fonts.length];
        System.arraycopy(fonts, 0, sorted, 0, sorted.length);

        Arrays.sort(sorted, new IThemeRegistry.HierarchyComparator(fonts));

        for (FontDefinition fontDefinition : sorted) {
            if (id.equals(fontDefinition.getDefaultsTo())) {
                list.add(fontDefinition);
            }
        }
        return list.toArray(new FontDefinition[list.size()]);
    }

    @NotNull
    public FontData[] getDefaultFontData(@NotNull FontDefinition definition) {
        FontData[] fontData;
        if (definition.isOverridden()) {
            fontData = definition.getValue();
        } else if (definition.getDefaultsTo() != null) {
            fontData = getFontAncestorValue(this.fontsPrefStore, definition);
        } else {
            fontData = PreferenceConverter.getDefaultFontDataArray(this.fontsPrefStore, createPreferenceKey(definition));
        }
        return fontData;
    }

    public void savePrefs() {
        PrefUtil.savePrefs();
    }
}
