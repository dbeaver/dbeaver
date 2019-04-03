/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.controls.resultset.panel.aggregate;

import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.aggregate.IAggregateFunction;
import org.jkiss.dbeaver.registry.functions.AggregateFunctionDescriptor;
import org.jkiss.dbeaver.registry.functions.FunctionsRegistry;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.*;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPanel;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

/**
 * RSV value view panel
 */
public class AggregateColumnsPanel implements IResultSetPanel {

    private static final Log log = Log.getLog(AggregateColumnsPanel.class);

    public static final String PANEL_ID = "column-aggregate";

    public static final String SETTINGS_SECTION_AGGREGATE = "panel-" + PANEL_ID;
    public static final String PARAM_GROUP_BY_COLUMNS = "groupByColumns";
    public static final String PARAM_GROUP_AS_STRINGS = "groupAsStrings";

    private static final DecimalFormat DOUBLE_FORMAT = new DecimalFormat("###,###,###,###,###,##0.###");
    private static final DecimalFormat INTEGER_FORMAT = new DecimalFormat("###,###,###,###,###,##0");

    private IResultSetPresentation presentation;
    private Tree aggregateTable;

    private boolean groupByColumns;
    private boolean aggregateAsStrings;
    //private boolean runServerQueries;

    private IDialogSettings panelSettings;

    private final List<AggregateFunctionDescriptor> enabledFunctions = new ArrayList<>();

    public AggregateColumnsPanel() {
    }

    @Override
    public Control createContents(final IResultSetPresentation presentation, Composite parent) {
        this.presentation = presentation;
        this.panelSettings = ResultSetUtils.getViewerSettings(SETTINGS_SECTION_AGGREGATE);

        loadSettings();

        this.aggregateTable = new Tree(parent, SWT.SINGLE | SWT.FULL_SELECTION);
        this.aggregateTable.setHeaderVisible(true);
        this.aggregateTable.setLinesVisible(true);
        new TreeColumn(this.aggregateTable, SWT.LEFT).setText("Function");
        new TreeColumn(this.aggregateTable, SWT.RIGHT).setText("Value");

        if (this.presentation instanceof ISelectionProvider) {
            ((ISelectionProvider) this.presentation).addSelectionChangedListener(event -> {
                if (presentation.getController().getVisiblePanel() == AggregateColumnsPanel.this) {
                    refresh(false);
                }
            });
        }

        MenuManager menuMgr = new MenuManager();
        menuMgr.addMenuListener(manager -> {
            manager.add(new CopyAction());
            manager.add(new CopyAllAction());
            manager.add(new Separator());
            fillToolBar(manager);
        });

        menuMgr.setRemoveAllWhenShown(true);
        this.aggregateTable.setMenu(menuMgr.createContextMenu(this.aggregateTable));
        this.aggregateTable.addDisposeListener(e -> menuMgr.dispose());

        aggregateTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                presentation.getController().updatePanelActions();
            }
        });

        return this.aggregateTable;
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    private void loadSettings() {
        aggregateAsStrings = panelSettings.getBoolean(PARAM_GROUP_AS_STRINGS);
        groupByColumns = panelSettings.getBoolean(PARAM_GROUP_BY_COLUMNS);
        IDialogSettings functionsSection = panelSettings.getSection("functions");
        if (functionsSection != null) {
            final Map<AggregateFunctionDescriptor, Integer> funcIndexes = new HashMap<>();

            for (IDialogSettings funcSection : functionsSection.getSections()) {
                String funcId = funcSection.getName();
                if (!funcSection.getBoolean("enabled")) {
                    continue;
                }
                AggregateFunctionDescriptor func = FunctionsRegistry.getInstance().getFunction(funcId);
                if (func == null) {
                    log.debug("Function '" + funcId + "' not found");
                } else {
                    funcIndexes.put(func, funcSection.getInt("index"));
                    enabledFunctions.add(func);
                }
            }
            enabledFunctions.sort(Comparator.comparingInt(funcIndexes::get));
        }

        if (enabledFunctions.isEmpty()) {
            loadDefaultFunctions();
        }
    }

    private void loadDefaultFunctions() {
        for (AggregateFunctionDescriptor func : FunctionsRegistry.getInstance().getFunctions()) {
            if (func.isDefault()) {
                enabledFunctions.add(func);
            }
        }
        enabledFunctions.sort(Comparator.comparing(AggregateFunctionDescriptor::getLabel));
    }

    private void saveSettings() {
        panelSettings.put(PARAM_GROUP_BY_COLUMNS, groupByColumns);
        panelSettings.put(PARAM_GROUP_AS_STRINGS, aggregateAsStrings);
        IDialogSettings functionsSection = UIUtils.getSettingsSection(panelSettings, "functions");

        for (AggregateFunctionDescriptor func : FunctionsRegistry.getInstance().getFunctions()) {
            IDialogSettings funcSection = UIUtils.getSettingsSection(functionsSection, func.getId());
            boolean enabled = enabledFunctions.contains(func);
            funcSection.put("enabled", enabled);
            if (enabled) {
                funcSection.put("index", enabledFunctions.indexOf(func));
            } else {
                funcSection.put("index", -1);
            }
        }
    }

    @Override
    public void activatePanel() {
        refresh(false);
    }

    @Override
    public void deactivatePanel() {

    }

    @Override
    public void refresh(boolean force) {
        aggregateTable.setRedraw(false);
        try {
            aggregateTable.removeAll();
            if (this.presentation instanceof ISelectionProvider) {
                ISelection selection = ((ISelectionProvider) presentation).getSelection();
                if (selection instanceof IResultSetSelection) {
                    aggregateSelection((IResultSetSelection)selection);
                }
            }
            UIUtils.packColumns(aggregateTable, true, null);
        } finally {
            aggregateTable.setRedraw(true);
        }
        saveSettings();
    }

    @Override
    public void contributeActions(IContributionManager manager) {
        fillToolBar(manager);
    }

    private void aggregateSelection(IResultSetSelection selection) {
        ResultSetModel model = presentation.getController().getModel();
        if (groupByColumns) {
            Map<DBDAttributeBinding, List<Object>> attrValues = new LinkedHashMap<>();
            for (Object element : selection.toList()) {
                DBDAttributeBinding attr = selection.getElementAttribute(element);
                ResultSetRow row = selection.getElementRow(element);
                Object cellValue = model.getCellValue(attr, row);
                List<Object> values = attrValues.computeIfAbsent(attr, k -> new ArrayList<>());
                values.add(cellValue);
            }

            for (Map.Entry<DBDAttributeBinding, List<Object>> entry : attrValues.entrySet()) {
                TreeItem attrItem = new TreeItem(aggregateTable, SWT.NONE);
                attrItem.setText(entry.getKey().getName());
                attrItem.setImage(DBeaverIcons.getImage(DBValueFormatting.getObjectImage(entry.getKey())));
                aggregateValues(attrItem, entry.getValue());
                attrItem.setExpanded(true);
            }
        } else {
            List<Object> allValues = new ArrayList<>(selection.size());
            for (Object element : selection.toList()) {
                DBDAttributeBinding attr = selection.getElementAttribute(element);
                ResultSetRow row = selection.getElementRow(element);
                Object cellValue = model.getCellValue(attr, row);
                allValues.add(cellValue);
            }
            aggregateValues(null, allValues);
        }
    }

    private void aggregateValues(TreeItem parentItem, Collection<Object> values) {
        List<AggregateFunctionDescriptor> functions = enabledFunctions;
        Map<IAggregateFunction, TreeItem> funcMap = new IdentityHashMap<>();
        for (AggregateFunctionDescriptor funcDesc : functions) {
            TreeItem funcItem = (parentItem == null) ?
                new TreeItem(aggregateTable, SWT.NONE) :
                new TreeItem(parentItem, SWT.NONE);
            funcItem.setData(funcDesc);
            funcItem.setText(0, funcDesc.getLabel());
            DBPImage icon = funcDesc.getIcon();
            if (icon != null) {
                funcItem.setImage(0, DBeaverIcons.getImage(icon));
            }
            try {
                IAggregateFunction func = funcDesc.createFunction();
                funcMap.put(func, funcItem);
            } catch (DBException e) {
                log.error(e);
            }
        }

        IAggregateFunction[] funcs = funcMap.keySet().toArray(new IAggregateFunction[funcMap.size()]);
        int[] funcCount = new int[funcs.length];
        for (Object element : values) {
            for (int i = 0; i < funcs.length; i++) {
                if (funcs[i].accumulate(element, aggregateAsStrings)) {
                    funcCount[i]++;
                }
            }
        }
        for (int i = 0; i < funcs.length; i++) {
            if (funcCount[i] <= 0) {
                continue;
            }
            IAggregateFunction func = funcs[i];
            Object result = func.getResult(funcCount[i]);
            if (result != null) {
                TreeItem treeItem = funcMap.get(func);
                String strValue;
                if (result instanceof Double || result instanceof Float || result instanceof BigDecimal) {
                    strValue = DOUBLE_FORMAT.format(result);
                } else if (result instanceof Integer || result instanceof Long || result instanceof Short) {
                    strValue = INTEGER_FORMAT.format(result);
                } else {
                    strValue = result.toString();
                }
                treeItem.setText(1, strValue);
            }
        }
    }

    public void clearValue()
    {
        aggregateTable.removeAll();
    }

    private void fillToolBar(IContributionManager contributionManager)
    {
        contributionManager.add(new AddFunctionAction());
        contributionManager.add(new RemoveFunctionAction());
        contributionManager.add(new ResetFunctionsAction());
        contributionManager.add(new Separator());
        contributionManager.add(new GroupByColumnsAction());
        contributionManager.add(new ValueTypeToggleAction());
    }

    private class GroupByColumnsAction extends Action {
        public GroupByColumnsAction() {
            super("Group by columns", IAction.AS_CHECK_BOX);
            setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.GROUP_BY_ATTR));
            setChecked(groupByColumns);
        }

        @Override
        public void run() {
            groupByColumns = !groupByColumns;
            setChecked(groupByColumns);
            refresh(false);
        }
    }

    private class ValueTypeToggleAction extends Action {
        public ValueTypeToggleAction() {
            super("Toggle numbers/strings aggregation", IAction.AS_CHECK_BOX);
            setImageDescriptor(DBeaverIcons.getImageDescriptor(
                aggregateAsStrings ? DBIcon.TYPE_STRING : DBIcon.TYPE_NUMBER));
            setChecked(aggregateAsStrings);
        }

        @Override
        public void run() {
            aggregateAsStrings = !aggregateAsStrings;
            setChecked(aggregateAsStrings);
            presentation.getController().updatePanelActions();
            refresh(false);
        }
    }

    private class AddFunctionAction extends Action {
        public AddFunctionAction() {
            super("Add function", DBeaverIcons.getImageDescriptor(UIIcon.OBJ_ADD));
        }

        @Override
        public void run() {
            List<AggregateFunctionDescriptor> missingFunctions = new ArrayList<>();
            for (AggregateFunctionDescriptor func : FunctionsRegistry.getInstance().getFunctions()) {
                if (!enabledFunctions.contains(func)) {
                    missingFunctions.add(func);
                }
            }
            if (!missingFunctions.isEmpty()) {
                Point location = aggregateTable.getDisplay().map(aggregateTable, null, new Point(10, 10));
                MenuManager menuManager = new MenuManager();

                for (final AggregateFunctionDescriptor func : missingFunctions) {
                    menuManager.add(new AddFunctionItemAction(func));
                }

                final Menu contextMenu = menuManager.createContextMenu(aggregateTable);
                contextMenu.setLocation(location);
                contextMenu.setVisible(true);

                aggregateTable.addDisposeListener(e -> menuManager.dispose());
            }
        }
    }

    private class AddFunctionItemAction extends Action {
        private final AggregateFunctionDescriptor func;

        public AddFunctionItemAction(AggregateFunctionDescriptor func) {
            super(func.getLabel(), DBeaverIcons.getImageDescriptor(func.getIcon()));
            this.func = func;
        }

        @Override
        public void run() {
            enabledFunctions.add(func);
            refresh(false);
        }
    }

    private class RemoveFunctionAction extends Action {
        public RemoveFunctionAction() {
            super("Remove function", DBeaverIcons.getImageDescriptor(UIIcon.OBJ_REMOVE));
        }

        @Override
        public boolean isEnabled() {
            return aggregateTable != null && !aggregateTable.isDisposed() && aggregateTable.getSelectionCount() > 0;
        }

        @Override
        public void run() {
            for (TreeItem item : aggregateTable.getSelection()) {
                AggregateFunctionDescriptor func = (AggregateFunctionDescriptor) item.getData();
                enabledFunctions.remove(func);
            }
            refresh(false);
        }
    }

    private class ResetFunctionsAction extends Action {
        public ResetFunctionsAction() {
            super("Reset", DBeaverIcons.getImageDescriptor(UIIcon.OBJ_REFRESH));
        }

        @Override
        public void run() {
            enabledFunctions.clear();
            loadDefaultFunctions();
            refresh(false);
        }
    }

    private class CopyAction extends Action {
        public CopyAction() {
            super("Copy Value");
        }

        @Override
        public void run() {
            StringBuilder result = new StringBuilder();
            for (TreeItem item : aggregateTable.getSelection()) {
                if (result.length() > 0) result.append("\n");
                if (item.getData() instanceof AggregateFunctionDescriptor) {
                    result.append(item.getText(1));
                } else {
                    result.append(item.getText(0));
                }
            }
            UIUtils.setClipboardContents(aggregateTable.getDisplay(), TextTransfer.getInstance(), result.toString());
        }
    }

    private class CopyAllAction extends Action {
        public CopyAllAction() {
            super("Copy All");
        }

        @Override
        public void run() {
            StringBuilder result = new StringBuilder();
            if (!groupByColumns) {
                for (TreeItem item : aggregateTable.getItems()) {
                    if (result.length() > 0) result.append("\n");
                    result.append(item.getText(0)).append("=").append(item.getText(1));
                }
            } else {
                for (TreeItem item : aggregateTable.getItems()) {
                    if (result.length() > 0) result.append("\n");
                    result.append(item.getText(0));
                    for (TreeItem funcItem : item.getItems()) {
                        result.append("\n\t");
                        result.append(funcItem.getText(0)).append("=").append(funcItem.getText(1));
                    }
                }
            }
            UIUtils.setClipboardContents(aggregateTable.getDisplay(), TextTransfer.getInstance(), result.toString());
        }
    }

}
