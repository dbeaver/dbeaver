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
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.AISettings;
import org.jkiss.dbeaver.model.ai.registry.AIFunctionDescriptor;
import org.jkiss.dbeaver.model.ai.registry.AIFunctionRegistry;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.*;

public class AIFunctionCategoriesPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    private static final Log log = Log.getLog(AIFunctionCategoriesPreferencePage.class);

    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.ai.functions";

    private CheckboxTreeViewer treeViewer;
    private Map<String, List<AIFunctionDescriptor>> categoryMap;

    @Override
    public void init(IWorkbench workbench) {
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 1, 5);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        UIUtils.createLabel(composite, "Configure available AI functions:");

        treeViewer = new CheckboxTreeViewer(composite, SWT.BORDER | SWT.FULL_SELECTION);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 300;
        treeViewer.getTree().setLayoutData(gd);

        treeViewer.setContentProvider(new TreeContentProvider());
        treeViewer.setLabelProvider(new TreeLabelProvider());

        try {
            loadData();
            log.debug("Loaded categories: " + categoryMap.keySet());
            treeViewer.setInput(categoryMap);
            updateCheckState();

            // Expand all categories to see functions
            treeViewer.expandAll();
        } catch (Exception e) {
            log.error("Error loading AI functions", e);
            UIUtils.createLabel(composite, "Error loading AI functions: " + e.getMessage());
        }

        treeViewer.addCheckStateListener(event -> {
            Object element = event.getElement();
            boolean checked = event.getChecked();

            if (element instanceof String category) {
                List<AIFunctionDescriptor> functions = categoryMap.get(category);
                if (functions != null) {
                    for (AIFunctionDescriptor function : functions) {
                        treeViewer.setChecked(function, checked);
                    }
                    treeViewer.setGrayed(category, false);
                }
            } else if (element instanceof AIFunctionDescriptor function) {
                updateCategoryState(function.getCategory());
            }
        });


        return composite;
    }

    private void updateCategoryState(String category) {
        List<AIFunctionDescriptor> functions = categoryMap.get(category);
        if (functions == null) return;

        boolean allChecked = true;
        boolean anyChecked = false;

        for (AIFunctionDescriptor function : functions) {
            boolean checked = treeViewer.getChecked(function);
            if (checked) {
                anyChecked = true;
            } else {
                allChecked = false;
            }
        }

        treeViewer.setChecked(category, allChecked);
        treeViewer.setGrayed(category, anyChecked && !allChecked);
    }


    private void loadData() {
        categoryMap = new LinkedHashMap<>();

        try {
            AIFunctionRegistry registry = AIFunctionRegistry.getInstance();
            List<AIFunctionDescriptor> allFunctions = registry.getAllFunctions();

            log.debug("Found " + allFunctions.size() + " AI functions");

            for (AIFunctionDescriptor function : allFunctions) {
                String category = function.getCategory();
                if (category == null || category.trim().isEmpty()) {
                    category = "Other";
                }

                log.debug("Function: " + function.getName() + ", Category: " + category);
                categoryMap.computeIfAbsent(category, k -> new ArrayList<>()).add(function);
            }

            categoryMap.entrySet().forEach(entry ->
                entry.getValue().sort(Comparator.comparing(AIFunctionDescriptor::getName)));

        } catch (Exception e) {
            log.error("Failed to load AI functions", e);
        }
    }

    private void updateCheckState() {
        try {
            AISettings settings = AISettingsManager.getInstance().getSettings();
            Set<String> enabledCategories = settings.getEnabledFunctionCategories();
            Set<String> enabledFunctions = settings.getEnabledFunctions();

            for (Map.Entry<String, List<AIFunctionDescriptor>> entry : categoryMap.entrySet()) {
                String category = entry.getKey();
                List<AIFunctionDescriptor> functions = entry.getValue();

                boolean allFunctionsEnabled = true;
                boolean anyFunctionEnabled = false;

                for (AIFunctionDescriptor function : functions) {
                    boolean functionEnabled = enabledFunctions.contains(function.getId()) ||
                        enabledCategories.contains(category);
                    treeViewer.setChecked(function, functionEnabled);

                    if (functionEnabled) {
                        anyFunctionEnabled = true;
                    } else {
                        allFunctionsEnabled = false;
                    }
                }

                treeViewer.setChecked(category, allFunctionsEnabled);
                treeViewer.setGrayed(category, anyFunctionEnabled && !allFunctionsEnabled);
            }
        } catch (Exception e) {
            log.error("Failed to update check state", e);
        }
    }


    @Override
    public boolean performOk() {
        try {
            Set<String> enabledCategories = new HashSet<>();
            Set<String> enabledFunctions = new HashSet<>();

            for (Map.Entry<String, List<AIFunctionDescriptor>> entry : categoryMap.entrySet()) {
                String category = entry.getKey();
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

                if (allFunctionsChecked && anyFunctionChecked) {
                    enabledCategories.add(category);
                }
            }

            AISettings settings = AISettingsManager.getInstance().getSettings();
            settings.setEnabledFunctionCategories(enabledCategories);
            settings.setEnabledFunctions(enabledFunctions);
            AISettingsManager.getInstance().saveSettings(settings);

            return true;
        } catch (Exception e) {
            log.error("Failed to save settings", e);
            return false;
        }
    }


    @Override
    protected void performDefaults() {
        for (Map.Entry<String, List<AIFunctionDescriptor>> entry : categoryMap.entrySet()) {
            String category = entry.getKey();
            treeViewer.setChecked(category, false);
            treeViewer.setGrayed(category, false);

            for (AIFunctionDescriptor function : entry.getValue()) {
                treeViewer.setChecked(function, false);
            }
        }
        super.performDefaults();
    }


    private class TreeContentProvider implements ITreeContentProvider {
        @Override
        public Object[] getElements(Object inputElement) {
            if (inputElement instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, List<AIFunctionDescriptor>> map = (Map<String, List<AIFunctionDescriptor>>) inputElement;
                return map.keySet().toArray();
            }
            return new Object[0];
        }

        @Override
        public Object[] getChildren(Object parentElement) {
            if (parentElement instanceof String category && categoryMap != null) {
                List<AIFunctionDescriptor> functions = categoryMap.get(category);
                return functions != null ? functions.toArray() : new Object[0];
            }
            return new Object[0];
        }

        @Override
        public Object getParent(Object element) {
            if (element instanceof AIFunctionDescriptor function) {
                String category = function.getCategory();
                return (category == null || category.trim().isEmpty()) ? "Other" : category;
            }
            return null;
        }

        @Override
        public boolean hasChildren(Object element) {
            if (element instanceof String category && categoryMap != null) {
                List<AIFunctionDescriptor> functions = categoryMap.get(category);
                return functions != null && !functions.isEmpty();
            }
            return false;
        }
    }

    private class TreeLabelProvider extends LabelProvider {
        @Override
        public String getText(Object element) {
            if (element instanceof String) {
                return (String) element;
            }
            if (element instanceof AIFunctionDescriptor function) {
                return function.getName();
            }
            return super.getText(element);
        }
    }
}

