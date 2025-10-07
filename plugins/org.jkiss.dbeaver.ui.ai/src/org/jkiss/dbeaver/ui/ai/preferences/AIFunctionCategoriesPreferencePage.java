/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.ai.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.AISettings;
import org.jkiss.dbeaver.model.ai.registry.AIFunctionCategoryDescriptor;
import org.jkiss.dbeaver.model.ai.registry.AIFunctionDescriptor;
import org.jkiss.dbeaver.model.ai.registry.AIFunctionRegistry;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

public class AIFunctionCategoriesPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    private static final Log log = Log.getLog(AIFunctionCategoriesPreferencePage.class);

    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.ai.functions";

    private CheckboxTreeViewer treeViewer;
    private Map<AIFunctionCategoryDescriptor, List<AIFunctionDescriptor>> categoryMap;

    @Override
    public void init(IWorkbench workbench) {
    }

    @Override
    protected Composite createContents(Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 1, 5);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        UIUtils.createLabel(composite, "Configure available AI function categories and functions:");

        treeViewer = new CheckboxTreeViewer(composite, SWT.BORDER | SWT.FULL_SELECTION);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 340;
        treeViewer.getTree().setLayoutData(gd);

        treeViewer.setContentProvider(new TreeContentProvider());
        treeViewer.setLabelProvider(new TreeLabelProvider());
        treeViewer.setUseHashlookup(true);

        try {
            loadData();
            treeViewer.setInput(categoryMap);
            updateCheckState();
            treeViewer.expandAll();
        } catch (Exception e) {
            log.error("Error loading AI functions", e);
            UIUtils.createLabel(composite, "Error loading AI functions: " + e.getMessage());
        }

        treeViewer.addCheckStateListener(event -> {
            Object element = event.getElement();
            boolean checked = event.getChecked();

            if (element instanceof AIFunctionCategoryDescriptor category) {
                List<AIFunctionDescriptor> functions = categoryMap.get(category);
                if (functions != null) {
                    for (AIFunctionDescriptor function : functions) {
                        treeViewer.setChecked(function, checked);
                    }
                    treeViewer.setGrayed(category, false);
                }
            } else if (element instanceof AIFunctionDescriptor function) {
                AIFunctionCategoryDescriptor cat = findCategoryOf(function);
                if (cat != null) {
                    updateCategoryState(cat);
                }
            }
        });

        return composite;
    }

    private void loadData() {
        categoryMap = new LinkedHashMap<>();
        AIFunctionRegistry registry = AIFunctionRegistry.getInstance();

        Map<AIFunctionCategoryDescriptor, List<AIFunctionDescriptor>> byCat = registry.getFunctionsByCategory();
        for (Map.Entry<AIFunctionCategoryDescriptor, List<AIFunctionDescriptor>> e : byCat.entrySet()) {
            List<AIFunctionDescriptor> list = new ArrayList<>(e.getValue());
            list.sort(Comparator.comparing(AIFunctionDescriptor::getName, String.CASE_INSENSITIVE_ORDER));
            categoryMap.put(e.getKey(), list);
        }
    }

    private void updateCheckState() {
        AISettings settings = AISettingsManager.getInstance().getSettings();

        Set<String> enabledCategories = settings.getEnabledFunctionCategories();
        Set<String> enabledFunctions = settings.getEnabledFunctions();

        for (Map.Entry<AIFunctionCategoryDescriptor, List<AIFunctionDescriptor>> entry : categoryMap.entrySet()) {
            AIFunctionCategoryDescriptor category = entry.getKey();
            List<AIFunctionDescriptor> functions = entry.getValue();

            boolean allEnabled = true;
            boolean anyEnabled = false;

            for (AIFunctionDescriptor function : functions) {
                boolean functionEnabled =
                    enabledFunctions.contains(function.getId()) ||
                        enabledCategories.contains(category.getId());

                treeViewer.setChecked(function, functionEnabled);
                anyEnabled |= functionEnabled;
                if (!functionEnabled) {
                    allEnabled = false;
                }
            }

            treeViewer.setChecked(category, allEnabled);
            treeViewer.setGrayed(category, anyEnabled && !allEnabled);
        }
    }

    private void updateCategoryState(@NotNull AIFunctionCategoryDescriptor category) {
        List<AIFunctionDescriptor> functions = categoryMap.get(category);
        if (functions == null || functions.isEmpty()) {
            treeViewer.setChecked(category, false);
            treeViewer.setGrayed(category, false);
            return;
        }

        boolean allChecked = true;
        boolean anyChecked = false;

        for (AIFunctionDescriptor function : functions) {
            boolean checked = treeViewer.getChecked(function);
            anyChecked |= checked;
            if (!checked) {
                allChecked = false;
            }
        }

        treeViewer.setChecked(category, allChecked);
        treeViewer.setGrayed(category, anyChecked && !allChecked);
    }

    @Override
    public boolean performOk() {
        try {
            Set<String> enabledCategories = new HashSet<>();
            Set<String> enabledFunctions = new HashSet<>();

            for (Map.Entry<AIFunctionCategoryDescriptor, List<AIFunctionDescriptor>> entry : categoryMap.entrySet()) {
                AIFunctionCategoryDescriptor category = entry.getKey();
                List<AIFunctionDescriptor> functions = entry.getValue();

                boolean allFunctionsChecked = true;
                boolean anyFunctionChecked = false;

                for (AIFunctionDescriptor function : functions) {
                    if (treeViewer.getChecked(function)) {
                        enabledFunctions.add(function.getId());
                        anyFunctionChecked = true;
                    } else {
                        allFunctionsChecked = false;
                    }
                }

                if (anyFunctionChecked && allFunctionsChecked) {
                    enabledCategories.add(category.getId());
                }
            }

            AISettings settings = AISettingsManager.getInstance().getSettings();
            settings.setEnabledFunctionCategories(enabledCategories);
            settings.setEnabledFunctions(enabledFunctions);
            AISettingsManager.getInstance().saveSettings(settings);
            return true;
        } catch (Exception e) {
            log.error("Failed to save AI function settings", e);
            return false;
        }
    }

    @Override
    protected void performDefaults() {
        for (Map.Entry<AIFunctionCategoryDescriptor, List<AIFunctionDescriptor>> entry : categoryMap.entrySet()) {
            AIFunctionCategoryDescriptor category = entry.getKey();
            boolean enable = category.isEnabledByDefault();

            treeViewer.setChecked(category, enable);
            treeViewer.setGrayed(category, false);

            for (AIFunctionDescriptor function : entry.getValue()) {
                treeViewer.setChecked(function, enable);
            }
        }
        super.performDefaults();
    }

    private AIFunctionCategoryDescriptor findCategoryOf(AIFunctionDescriptor f) {
        String cid = f.getCategoryId();
        if (CommonUtils.isEmpty(cid)) {
            return null;
        }
        for (AIFunctionCategoryDescriptor c : categoryMap.keySet()) {
            if (cid.equals(c.getId())) {
                return c;
            }
        }
        return null;
    }

    private class TreeContentProvider implements ITreeContentProvider {
        @Override
        public Object[] getElements(Object inputElement) {
            return categoryMap.keySet().toArray();
        }

        @Override
        public Object[] getChildren(Object parentElement) {
            if (parentElement instanceof AIFunctionCategoryDescriptor category) {
                List<AIFunctionDescriptor> functions = categoryMap.get(category);
                return functions != null ? functions.toArray() : new Object[0];
            }
            return new Object[0];
        }

        @Override
        public Object getParent(Object element) {
            if (element instanceof AIFunctionDescriptor function) {
                return findCategoryOf(function);
            }
            return null;
        }

        @Override
        public boolean hasChildren(Object element) {
            return element instanceof AIFunctionCategoryDescriptor;
        }
    }

    private static class TreeLabelProvider extends LabelProvider {
        @Override
        public String getText(Object element) {
            if (element instanceof AIFunctionCategoryDescriptor c) {
                return c.getName();
            } else if (element instanceof AIFunctionDescriptor f) {
                return f.getName();
            }
            return super.getText(element);
        }
    }
}
