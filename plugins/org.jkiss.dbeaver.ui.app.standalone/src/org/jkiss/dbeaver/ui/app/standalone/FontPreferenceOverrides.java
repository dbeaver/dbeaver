/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.app.standalone;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.IPreferencePage;
import org.eclipse.jface.preference.IPreferencePageContainer;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.themes.ColorDefinition;
import org.eclipse.ui.internal.themes.FontDefinition;
import org.eclipse.ui.internal.themes.ICategorizedThemeElementDefinition;
import org.eclipse.ui.internal.themes.IHierarchalThemeElementDefinition;
import org.eclipse.ui.internal.themes.IThemeElementDefinition;
import org.eclipse.ui.internal.themes.IThemeRegistry;
import org.eclipse.ui.internal.themes.ThemeElementCategory;
import org.eclipse.ui.internal.themes.WorkbenchThemeManager;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;
import org.jkiss.dbeaver.ui.DBIconBinary;
import org.jkiss.dbeaver.ui.DBeaverIcons;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FontPreferenceOverrides {

    // Original implementation at org.eclipse.ui.internal.themes.ColorsAndFontsPreferencePage.ThemeContentProvider
    private static class FilteredThemeContentProvider implements ITreeContentProvider {
        private final Set<String> prefIdsToHide;
        
        private final Map<String, Object[]> categoryMap = new HashMap<>(7);
        private final IThemeRegistry themeRegistry = WorkbenchPlugin.getDefault().getThemeRegistry();
        private final IThemeManager themeManager;
        private IThemeRegistry registry;
        private IPropertyChangeListener themeChangeListener;
        private ITheme currentTheme;
        
        public FilteredThemeContentProvider(Set<String> prefIdsToHide) {
            this.prefIdsToHide = prefIdsToHide;
            
            themeManager = PlatformUI.getWorkbench().getThemeManager();
            themeChangeListener = event -> {
                if (event.getProperty().equals(IThemeManager.CHANGE_CURRENT_THEME)) {
                    currentTheme = themeManager.getCurrentTheme();
                }
            };
            currentTheme = themeManager.getCurrentTheme();
            themeManager.addPropertyChangeListener(themeChangeListener);
        }

        private boolean isIdToHide(String id) {
            return id != null && prefIdsToHide.contains(id);
        }

        @Override
        public Object[] getChildren(Object parentElement) {            
            if (parentElement instanceof ThemeElementCategory) {
                String categoryId = ((ThemeElementCategory) parentElement).getId();
                Object[] defintions = categoryMap.get(categoryId);
                if (defintions == null) {
                    defintions = getCategoryChildren(categoryId);                    
                    categoryMap.put(categoryId, defintions);
                }
                return defintions;
            }

            ArrayList<IHierarchalThemeElementDefinition> list = new ArrayList<>();
            IHierarchalThemeElementDefinition def = (IHierarchalThemeElementDefinition) parentElement;
            String id = def.getId();
            IHierarchalThemeElementDefinition[] defs;
            if (def instanceof ColorDefinition) {
                defs = registry.getColors();
            } else {
                defs = registry.getFonts();
            }

            for (IHierarchalThemeElementDefinition elementDefinition : defs) {
                if (id.equals(elementDefinition.getDefaultsTo()) && catIdEquals(
                        ((ICategorizedThemeElementDefinition) def).getCategoryId(),
                        ((ICategorizedThemeElementDefinition) elementDefinition).getCategoryId())) {
                    list.add(elementDefinition);
                }
            }
            return list.toArray();
        }
        
        private boolean catIdEquals(String string, String string2) {
            if ((string == null && string2 == null))
                return true;
            if (string == null || string2 == null)
                return false;
            if (string.equals(string2))
                return true;
            return false;
        }

        private Object[] getCategoryChildren(String categoryId) {
            if (isIdToHide(categoryId)) {
                return new Object[0];
            }
            
            ArrayList<IThemeElementDefinition> list = new ArrayList<>();

            if (categoryId != null) {
                for (ThemeElementCategory category : registry.getCategories()) {
                    if (categoryId.equals(category.getParentId()) && !isIdToHide(category.getId())) {
                        Set<?> bindings = themeRegistry.getPresentationsBindingsFor(category);
                        if (bindings == null) {
                            list.add(category);
                        }
                    }
                }
            }
            ColorDefinition[] colorDefinitions = themeRegistry.getColorsFor(currentTheme.getId());
            for (ColorDefinition colorDefinition : colorDefinitions) {
                if (!colorDefinition.isEditable() || isIdToHide(colorDefinition.getId())) {
                    continue;
                }
                String catId = colorDefinition.getCategoryId();
                if ((catId == null && categoryId == null)
                        || (catId != null && categoryId != null && categoryId.equals(catId))) {
                    if (colorDefinition.getDefaultsTo() != null && parentIsInSameCategory(colorDefinition)) {
                        continue;
                    }
                    list.add(colorDefinition);
                }
            }
            FontDefinition[] fontDefinitions = themeRegistry.getFontsFor(currentTheme.getId());
            for (FontDefinition fontDefinition : fontDefinitions) {
                if (!fontDefinition.isEditable() || isIdToHide(fontDefinition.getId())) {
                    continue;
                }
                String catId = fontDefinition.getCategoryId();
                if ((catId == null && categoryId == null)
                        || (catId != null && categoryId != null && categoryId.equals(catId))) {
                    if (fontDefinition.getDefaultsTo() != null && parentIsInSameCategory(fontDefinition)) {
                        continue;
                    }
                    list.add(fontDefinition);
                }
            }
            return list.toArray(new Object[list.size()]);
        }

        private boolean parentIsInSameCategory(ColorDefinition definition) {
            String defaultsTo = definition.getDefaultsTo();
            for (ColorDefinition colorDef : registry.getColors()) {
                if (colorDef.getId().equals(defaultsTo)
                    && catIdEquals(colorDef.getCategoryId(), definition.getCategoryId())
                ) {
                    return true;
                }
            }
            return false;
        }

        private boolean parentIsInSameCategory(FontDefinition definition) {
            String defaultsTo = definition.getDefaultsTo();
            for (FontDefinition fontDef : registry.getFonts()) {
                if (fontDef.getId().equals(defaultsTo)
                    && catIdEquals(fontDef.getCategoryId(), definition.getCategoryId())
                ) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Object getParent(Object element) {            
            if (element instanceof ThemeElementCategory)
                return registry;

            if (element instanceof ColorDefinition) {
                String defaultId = ((IHierarchalThemeElementDefinition) element).getDefaultsTo();
                if (defaultId != null) {
                    ColorDefinition defaultElement = registry.findColor(defaultId);
                    if (parentIsInSameCategory(defaultElement))
                        return defaultElement;
                }
                String categoryId = ((ColorDefinition) element).getCategoryId();
                return registry.findCategory(categoryId);
            }

            if (element instanceof FontDefinition) {
                String defaultId = ((FontDefinition) element).getDefaultsTo();
                if (defaultId != null) {
                    FontDefinition defaultElement = registry.findFont(defaultId);
                    if (parentIsInSameCategory(defaultElement))
                        return defaultElement;
                }
                String categoryId = ((FontDefinition) element).getCategoryId();
                return registry.findCategory(categoryId);
            }

            return null;
        }

        @Override
        public boolean hasChildren(Object element) {            
            if (element instanceof ThemeElementCategory) {
                if (isIdToHide(((ThemeElementCategory)element).getId())) {
                    return false;
                }
                return true;
            }

            IHierarchalThemeElementDefinition def = (IHierarchalThemeElementDefinition) element;
            String id = def.getId();
            if (isIdToHide(id)) {
                return false;
            }
            
            IHierarchalThemeElementDefinition[] defs;
            if (def instanceof ColorDefinition) {
                defs = registry.getColors();
            } else {
                defs = registry.getFonts();
            }

            for (IHierarchalThemeElementDefinition elementDefinition : defs) {
                if (id.equals(elementDefinition.getDefaultsTo())
                    && catIdEquals(
                        ((ICategorizedThemeElementDefinition) def).getCategoryId(),
                        ((ICategorizedThemeElementDefinition) elementDefinition).getCategoryId()
                    )
                ) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public Object[] getElements(Object inputElement) {            
            ArrayList<Object> list = new ArrayList<>();
            Object[] uncatChildren = getCategoryChildren(null);
            list.addAll(Arrays.asList(uncatChildren));
            for (ThemeElementCategory category : ((IThemeRegistry) inputElement).getCategories()) {
                if (category.getParentId() == null && !isIdToHide(category.getId())) {
                    Set<?> bindings = themeRegistry.getPresentationsBindingsFor(category);
                    if (bindings == null) {
                        Object[] children = getChildren(category);
                        if (children != null && children.length > 0) {
                            list.add(category);
                        }
                    }
                }
            }
            return list.toArray(new Object[list.size()]);
        }

        @Override
        public void dispose() {
            categoryMap.clear();
            themeManager.removePropertyChangeListener(themeChangeListener);
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            categoryMap.clear();
            registry = (IThemeRegistry) newInput;
        }

    }

    private static class MyFontsPrefPage implements IPreferencePage, IMessageProvider {
        private final PreferencePage fontsPage;
        private final Set<String> prefIdsToHide;

        public MyFontsPrefPage(PreferencePage fontsPage, Set<String> prefIdsToHide) {
            this.fontsPage = fontsPage;
            this.prefIdsToHide = prefIdsToHide;
        }

        @Override
        public void createControl(Composite parent) {
            fontsPage.createControl(parent);
            
            // see ColorsAndFontsPreferencePage.createContents(..)
            Control[] prefsPageParts = parent.getChildren(); // page content container and defaults&apply buttons container 
            Composite prefsPageContent = (Composite)prefsPageParts[Math.max(prefsPageParts.length - 1, 0)];
            Composite advancedCompositeSash = (Composite)prefsPageContent.getChildren()[0];
            Composite mainColumn = (Composite)advancedCompositeSash.getChildren()[0];
            FilteredTree tree = (FilteredTree)mainColumn.getChildren()[1];
            
            tree.getViewer().setContentProvider(new FilteredThemeContentProvider(prefIdsToHide));
        }

        @Override
        public void dispose() {
            fontsPage.dispose();
        }

        @Override
        public Control getControl() {
            return fontsPage.getControl();
        }

        @Override
        public String getDescription() {
            return fontsPage.getDescription();
        }

        @Override
        public String getErrorMessage() {
            return fontsPage.getErrorMessage();
        }

        @Override
        public Image getImage() {
            return fontsPage.getImage();
        }

        @Override
        public String getMessage() {
            return fontsPage.getMessage();
        }

        @Override
        public String getTitle() {
            return fontsPage.getTitle();
        }

        @Override
        public void performHelp() {
            fontsPage.performHelp();
        }

        @Override
        public void setDescription(String description) {
            fontsPage.setDescription(description);
        }

        @Override
        public void setImageDescriptor(ImageDescriptor image) {
            fontsPage.setImageDescriptor(image);
        }

        @Override
        public void setTitle(String title) {
            fontsPage.setTitle(title);
        }

        @Override
        public void setVisible(boolean visible) {
            fontsPage.setVisible(visible);
        }

        @Override
        public int getMessageType() {
            return fontsPage.getMessageType();
        }

        @Override
        public Point computeSize() {
            return fontsPage.computeSize();
        }

        @Override
        public boolean isValid() {
            return fontsPage.isValid();
        }

        @Override
        public boolean okToLeave() {
            return fontsPage.okToLeave();
        }

        @Override
        public boolean performCancel() {
            return fontsPage.performCancel();
        }

        @Override
        public boolean performOk() {
            return fontsPage.performOk();
        }

        @Override
        public void setContainer(IPreferencePageContainer preferencePageContainer) {
            fontsPage.setContainer(preferencePageContainer);
        }

        @Override
        public void setSize(Point size) {
            fontsPage.setSize(size);
        }
    }
    
    private static class FontPreferenceNodePageOverride extends PreferenceNode {
        private PreferenceNode originalNode;
        private Set<String> prefIdsToHide;
        private IPreferencePage page = null;
        
        public FontPreferenceNodePageOverride(PreferenceNode originalNode, Set<String> prefIdsToHide) {
            super(originalNode.getId());
            this.originalNode = originalNode;
            this.prefIdsToHide = prefIdsToHide;
        }

        @Override
        public String getLabelText() {
            return originalNode.getLabelText();
        }
        
        @Override
        public Image getLabelImage() {
            return originalNode.getLabelImage();
        }
        
        @Override
        public void createPage() {
            originalNode.createPage();
            IPreferencePage originalPage = originalNode.getPage();
            if (originalPage instanceof PreferencePage) {
                page = new MyFontsPrefPage((PreferencePage)originalPage, prefIdsToHide);
                if (getLabelImage() != null) {
                    page.setImageDescriptor(DBeaverIcons.getImageDescriptor(new DBIconBinary(null, originalNode.getLabelImage())));
                }
                page.setTitle(getLabelText());
            } else {
                page = originalPage;
            }
        }
        
        @Override
        public IPreferencePage getPage() {
            return page;
        }
        
        @Override 
        public void setPage(IPreferencePage page) {
            this.page = page;
            if (page == null) {
                originalNode.setPage(null);
            }
        }
        
        @Override
        public void disposeResources() {
            super.disposeResources();
            if (page != null) {
                page.dispose();
                page = null;
            }
            originalNode.disposeResources();
        }   
    }

    public static void hideFontPrefs(PreferenceManager pm, Set<String> prefIdsToHide) {
        String wbPrefPageId = ApplicationWorkbenchAdvisor.WORKBENCH_PREF_PAGE_ID ;
        String viewsCatId = wbPrefPageId  + "/org.eclipse.ui.preferencePages.Views";
        String fontsPrefPageId = wbPrefPageId + "/org.eclipse.ui.preferencePages.Views/org.eclipse.ui.preferencePages.ColorsAndFonts";
        
        IPreferenceNode catNode = pm.find(viewsCatId);
        IPreferenceNode rawFontsNode = pm.find(fontsPrefPageId);
        
        if (rawFontsNode instanceof PreferenceNode) {
            catNode.remove(rawFontsNode);
            catNode.add(new FontPreferenceNodePageOverride((PreferenceNode)rawFontsNode, prefIdsToHide));
        }
    }

    public static void overrideFontPrefValues(Map<String, List<String>> fontOverrides) {
        WorkbenchThemeManager.getInstance().addPropertyChangeListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                String fontPropertyId = event.getProperty();
                List<String> fontIdsToOverride = fontOverrides.get(fontPropertyId);
                if (fontIdsToOverride != null) {
                    FontRegistry fonts = WorkbenchThemeManager.getInstance().getCurrentTheme().getFontRegistry();
                    FontData[] data = fonts.getFontData(fontPropertyId);
                    for (String fontId: fontIdsToOverride) {
                        fonts.put(fontId, data);
                    }
                }
            }
        });        
    }
}
