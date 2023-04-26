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
package org.jkiss.dbeaver.ui.data.editors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.AbstractLoadService;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.ProgressLoaderVisualizer;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ThemeConstants;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.data.IAttributeController;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.dbeaver.ui.editors.data.DatabaseDataEditor;
import org.jkiss.dbeaver.ui.editors.object.struct.EditDictionaryPage;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerObjectOpen;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * ReferenceValueEditor
 *
 * @author Serge Rider
 */
public class ReferenceValueEditor {
    private static final Log log = Log.getLog(ReferenceValueEditor.class);

    private final Color selectionColor = UIUtils.getColorRegistry().get(ThemeConstants.COLOR_SQL_RESULT_SET_SELECTION_BACK);
    private final IValueController valueController;
    private IValueEditor valueEditor;
    private DBSEntityReferrer refConstraint;
    private Table editorSelector;
    private static volatile boolean sortByValue = true; // It is static to save its value between editors
    private static volatile boolean sortAsc = true;
    private TableColumn prevSortColumn = null;
    private volatile boolean dictLoaded = false;
    private Object lastPattern;
    private Object firstValue = null;
    private Object lastValue = null;
    private int maxResults;
    private Font boldFont;


    public ReferenceValueEditor(IValueController valueController, IValueEditor valueEditor) {
        this.valueController = valueController;
        this.valueEditor = valueEditor;
        DBCExecutionContext executionContext = valueController.getExecutionContext();
        if (executionContext != null) {
            this.maxResults =
                executionContext.getDataSource().getContainer().getPreferenceStore().getInt(
                    ModelPreferences.DICTIONARY_MAX_ROWS);
        }
    }

    public void setValueEditor(IValueEditor valueEditor) {
        this.valueEditor = valueEditor;
    }

    public boolean isReferenceValue()
    {
        return getEnumerableConstraint() != null;
    }

    @Nullable
    private DBSEntityReferrer getEnumerableConstraint()
    {
        if (valueController instanceof IAttributeController) {
            return ResultSetUtils.getEnumerableConstraint(((IAttributeController) valueController).getBinding());
        }
        return null;
    }

    public boolean createEditorSelector(final Composite parent)
    {
        if (!(valueController instanceof IAttributeController)) {
            return false;
        }
        refConstraint = getEnumerableConstraint();
        if (refConstraint == null) {
            return false;
        }

        this.boldFont = UIUtils.makeBoldFont(parent.getFont());
        parent.addDisposeListener(e -> this.boldFont.dispose());

        if (refConstraint instanceof DBSEntityAssociation) {
            final DBSEntityAssociation association = (DBSEntityAssociation)refConstraint;
            if (association.getReferencedConstraint() != null) {
                final DBSEntity refTable = association.getReferencedConstraint().getParentObject();
                Composite labelGroup = UIUtils.createPlaceholder(parent, 2);
                labelGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                Link dictLabel = UIUtils.createLink(
                    labelGroup,
                    NLS.bind(ResultSetMessages.dialog_value_view_label_dictionary, refTable.getName()), new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            // Open
                            final IWorkbenchWindow window = valueController.getValueSite().getWorkbenchWindow();
                            UIUtils.runInUI(window, monitor -> {
                                DBNDatabaseNode tableNode = DBNUtils.getNodeByObject(
                                    monitor,
                                    refTable,
                                    true
                                );
                                if (tableNode != null) {
                                    NavigatorHandlerObjectOpen.openEntityEditor(tableNode, DatabaseDataEditor.class.getName(), window);
                                }
                            });
                        }
                    });
                dictLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

                Link hintLabel = UIUtils.createLink(labelGroup, "(<a>Define Description</a>)", new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        EditDictionaryPage editDictionaryPage = new EditDictionaryPage(refTable);
                        if (editDictionaryPage.edit(parent.getShell())) {
                            reloadSelectorValues(null, true);
                        }
                    }
                });
                hintLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END));
            }
        }

        editorSelector = new Table(parent, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
        editorSelector.setLinesVisible(true);
        editorSelector.setHeaderVisible(true);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 150;
        //gd.widthHint = 300;
        //gd.grabExcessVerticalSpace = true;
        //gd.grabExcessHorizontalSpace = true;
        editorSelector.setLayoutData(gd);
        TableColumn valueColumn = UIUtils.createTableColumn(editorSelector, SWT.LEFT, ResultSetMessages.dialog_value_view_column_value);
        valueColumn.setData(Boolean.TRUE);

        TableColumn descColumn = UIUtils.createTableColumn(editorSelector, SWT.LEFT, ResultSetMessages.dialog_value_view_column_description);
        descColumn.setData(Boolean.FALSE);


        SortListener sortListener = new SortListener();
        valueColumn.addListener(SWT.Selection, sortListener);
        descColumn.addListener(SWT.Selection, sortListener);
        if (!sortByValue) {
            editorSelector.setSortColumn(descColumn);
            editorSelector.setSortDirection(sortAsc ? SWT.DOWN : SWT.UP);
            prevSortColumn = descColumn;
        }

        editorSelector.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                if (valueEditor.isReadOnly()) {
                    return;
                }
                TableItem[] selection = editorSelector.getSelection();
                if (selection != null && selection.length > 0) {
                    Object value = selection[0].getData();
                    //editorControl.setText(selection[0].getText());
                    try {
                        valueEditor.primeEditorValue(value);
                    } catch (DBException e1) {
                        log.error(e1);
                    }
                }
            }
        });
        {
            MenuManager menuMgr = new MenuManager();
            menuMgr.addMenuListener(manager -> {
                manager.add(new CopyAction());
                manager.add(new Separator());
            });

            menuMgr.setRemoveAllWhenShown(true);
            this.editorSelector.setMenu(menuMgr.createContextMenu(this.editorSelector));
            this.editorSelector.addDisposeListener(e -> menuMgr.dispose());
        }

        Control control = valueEditor.getControl();
        ModifyListener modifyListener = e -> {
            Object curEditorValue;
            try {
                curEditorValue = valueEditor.extractEditorValue();
            } catch (DBException e1) {
                log.error(e1);
                return;
            }
            // Try to select current value in the table
            final String curTextValue = valueController.getValueHandler().getValueDisplayString(
                ((IAttributeController) valueController).getBinding(),
                curEditorValue,
                DBDDisplayFormat.EDIT);
            boolean newValueFound = false;
            TableItem[] items = editorSelector.getItems();
            for (TableItem item : items) {
                if (curTextValue.equalsIgnoreCase(item.getText(0)) || curTextValue.equalsIgnoreCase(item.getText(1))) {
                    editorSelector.deselectAll();
                    item.setFont(boldFont);
                    editorSelector.showItem(item);
                    newValueFound = true;
                } else {
                    item.setFont(null);
                }
            }

            if (!newValueFound) {
                reloadSelectorValues(curEditorValue, false);
            }
        };
        if (control instanceof Text) {
            ((Text)control).addModifyListener(modifyListener);
        } else if (control instanceof StyledText) {
            ((StyledText)control).addModifyListener(modifyListener);
        }

        if (refConstraint instanceof DBSEntityAssociation) {
            final Text valueFilterText = new Text(parent, SWT.BORDER);
            valueFilterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            valueFilterText.addModifyListener(e -> {
                String filterPattern = valueFilterText.getText();
                reloadSelectorValues(filterPattern, false);
            });
            valueFilterText.addPaintListener(e -> {
                if (valueFilterText.isEnabled() && valueFilterText.getCharCount() == 0) {
                    e.gc.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
                    e.gc.drawText("Type part of dictionary value to search",
                        2, 0, true);
                    e.gc.setFont(null);
                }
            });
        }
        final Object curValue = valueController.getValue();

        reloadSelectorValues(curValue, false);

        return true;
    }

    private void reloadSelectorValues(Object pattern, boolean force) {
        reloadSelectorValues(pattern, force, 0);
    }

    private void reloadSelectorValues(Object pattern, boolean force, int offset) {
        if (!force && dictLoaded && CommonUtils.equalObjects(String.valueOf(lastPattern), String.valueOf(pattern))) {
            selectCurrentValue();
            return;
        }
        lastPattern = pattern;
        dictLoaded = true;
        SelectorLoaderService loadingService = new SelectorLoaderService(offset);
        if (pattern != null) {
            loadingService.setPattern(pattern);
        }
        LoadingJob.createService(
            loadingService,
            new SelectorLoaderVisualizer(loadingService))
            .schedule();
    }

    private void updateDictionarySelector(EnumValuesData valuesData) {
        if (editorSelector == null || editorSelector.isDisposed()) {
            return;
        }
        editorSelector.setRedraw(false);
        try {
            editorSelector.removeAll();
            for (DBDLabelValuePair entry : valuesData.keyValues) {
                TableItem discItem = new TableItem(editorSelector, SWT.NONE);
                discItem.setText(0,
                    valuesData.keyHandler.getValueDisplayString(
                        valuesData.keyColumn.getAttribute(),
                        entry.getValue(),
                        DBDDisplayFormat.EDIT));
                discItem.setText(1, entry.getLabel());
                discItem.setData(entry.getValue());
            }

            selectCurrentValue();

            UIUtils.packColumns(editorSelector, false);
        } finally {
            editorSelector.setRedraw(true);
        }
    }


    /**
     * Returns action to allow editor paging
     *
     * @return actions for paging
     */
    public ContributionItem[] getContributionItems() {
        MoveToNextPageAction moveBackward = new MoveToNextPageAction("Move Backward", true,
            DBeaverIcons.getImageDescriptor(UIIcon.ARROW_LEFT));
        MoveToNextPageAction moveForward = new MoveToNextPageAction("Move Forward", false,
            DBeaverIcons.getImageDescriptor(UIIcon.ARROW_RIGHT));

        return new ContributionItem[]{ ActionUtils.makeActionContribution(moveBackward, false),
            ActionUtils.makeActionContribution(moveForward, false) };
    }

    private void selectCurrentValue() {
        Control editorControl = valueEditor.getControl();
        if (editorControl != null && !editorControl.isDisposed()) {
            try {
                Object curValue = valueEditor.extractEditorValue();
                final String curTextValue = valueController.getValueHandler().getValueDisplayString(
                        ((IAttributeController) valueController).getBinding(),
                        curValue,
                        DBDDisplayFormat.EDIT
                );
                TableItem curItem = null;
                int curItemIndex = -1;
                TableItem[] items = editorSelector.getItems();
                for (int i = 0; i < items.length; i++) {
                    TableItem item = items[i];
                    if (item.getText(0).equals(curTextValue)) {
                        curItem = item;
                        curItemIndex = i;
                    } else {
                        item.setFont(null);
                    }
                }
                editorSelector.deselectAll();
                if (curItem != null) {
                    curItem.setFont(boldFont);
                    editorSelector.showItem(curItem);
                    // Show cur item on top
                    editorSelector.setTopIndex(curItemIndex);
                }
            } catch (DBException e) {
                log.error(e);
            }
        }
    }


    private class CopyAction extends Action {
        public CopyAction() {
            super("Copy Value");
        }

        @Override
        public void run() {
            StringBuilder result = new StringBuilder();
            for (TableItem item : editorSelector.getSelection()) {
                if (result.length() > 0) result.append("\n");
                result.append(item.getText(0));
            }
            UIUtils.setClipboardContents(editorSelector.getDisplay(), TextTransfer.getInstance(), result.toString());
        }
    }

    private class SortListener implements Listener {
        private int sortDirection = sortAsc ? SWT.DOWN : SWT.UP;

        public SortListener() {
        }

        @Override
        public void handleEvent(Event event) {
            TableColumn column = (TableColumn) event.widget;
            if (prevSortColumn == column) {
                // Set reverse order
                sortDirection = (sortDirection == SWT.UP ? SWT.DOWN : SWT.UP);
            }
            prevSortColumn = column;
            sortByValue = (Boolean)column.getData();
            sortAsc = sortDirection == SWT.DOWN;
            editorSelector.setSortColumn(column);
            editorSelector.setSortDirection(sortDirection);
            reloadSelectorValues(lastPattern, true);

        }
    }

    private static class EnumValuesData {
        List<DBDLabelValuePair> keyValues;
        DBSEntityAttributeRef keyColumn;
        DBDValueHandler keyHandler;

        EnumValuesData(Collection<DBDLabelValuePair> keyValues, DBSEntityAttributeRef keyColumn, DBDValueHandler keyHandler) {
            this.keyValues = new ArrayList<>(keyValues);
            this.keyColumn = keyColumn;
            this.keyHandler = keyHandler;
        }
    }

    class SelectorLoaderService extends AbstractLoadService<EnumValuesData> {

        int offset;
        private Object pattern;

        public Object getLastValue() {
            return lastValue;
        }

        private SelectorLoaderService(int offset) {
            super(ResultSetMessages.dialog_value_view_job_selector_name + valueController.getValueName() + " possible values");
            this.offset = offset;
        }

        public void setPattern(@Nullable Object pattern)
        {
            this.pattern = pattern;
        }


        @Override
        public EnumValuesData evaluate(DBRProgressMonitor monitor) {
            if (editorSelector.isDisposed() || valueController.getExecutionContext() == null) {
                return null;
            }
            EnumValuesData[] result = new EnumValuesData[1];
            try {
                DBExecUtils.tryExecuteRecover(monitor, valueController.getExecutionContext().getDataSource(), param -> {
                    try {
                        result[0] = readEnum(monitor);
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                });
            } catch (DBException e) {
                // error
                // just ignore
                log.warn(e.getMessage());
            }
            return result[0];
        }

        @Nullable
        private EnumValuesData readEnum(DBRProgressMonitor monitor) throws DBException {
    /*
                final Map<Object, String> keyValues = new TreeMap<>((o1, o2) -> {
                    if (o1 instanceof Comparable && o2 instanceof Comparable) {
                        return ((Comparable) o1).compareTo(o2);
                    }
                    if (o1 == o2) {
                        return 0;
                    } else if (o1 == null) {
                        return -1;
                    } else if (o2 == null) {
                        return 1;
                    } else {
                        return o1.toString().compareTo(o2.toString());
                    }
                });
    */

            IAttributeController attributeController = (IAttributeController)valueController;
            final DBSEntityAttribute tableColumn = attributeController.getBinding().getEntityAttribute();
            if (tableColumn == null) {
                return null;
            }
            final DBSEntityAttributeRef fkColumn = DBUtils.getConstraintAttribute(monitor, refConstraint, tableColumn);
            if (fkColumn == null) {
                return null;
            }
            DBSEntityAssociation association;
            if (refConstraint instanceof DBSEntityAssociation) {
                association = (DBSEntityAssociation)refConstraint;
            } else {
                return null;
            }
            DBSEntityAttribute activeRefColumn = DBUtils.getReferenceAttribute(monitor, association, tableColumn,
                false);
            if (activeRefColumn == null) {
                return null;
            }
            return getEnumValuesData(monitor, attributeController, fkColumn, association, activeRefColumn);
        }

        @Nullable
        private EnumValuesData getEnumValuesData(DBRProgressMonitor monitor, IAttributeController attributeController,
                                                 DBSEntityAttributeRef fkColumn, DBSEntityAssociation association,
                                                 DBSEntityAttribute refColumn) throws DBException {
            List<DBDAttributeValue> precedingKeys = null;
            List<? extends DBSEntityAttributeRef> allColumns = CommonUtils.safeList(refConstraint.getAttributeReferences(
                monitor));
            if (allColumns.size() > 1 && allColumns.get(0) != fkColumn) {
                // Our column is not a first on in foreign key.
                // So, fill uo preceeding keys
                List<DBDAttributeBinding> rowAttributes = attributeController.getRowController().getRowAttributes();
                precedingKeys = new ArrayList<>();
                for (DBSEntityAttributeRef precColumn : allColumns) {
                    if (precColumn == fkColumn) {
                        // Enough
                        break;
                    }
                    DBSEntityAttribute precAttribute = precColumn.getAttribute();
                    if (precAttribute != null) {
                        DBDAttributeBinding rowAttr = DBUtils.findBinding(rowAttributes, precAttribute);
                        if (rowAttr != null) {
                            Object precValue = attributeController.getRowController().getAttributeValue(rowAttr);
                            precedingKeys.add(new DBDAttributeValue(precAttribute, precValue));
                        }
                    }
                }
            }
            final DBSEntityAttribute fkAttribute = fkColumn.getAttribute();
            final DBSEntityConstraint refConstraint = association.getReferencedConstraint();
            final DBSDictionary enumConstraint = (DBSDictionary) refConstraint.getParentObject();
            if (fkAttribute != null && enumConstraint != null) {
                List<DBDLabelValuePair> enumValues = enumConstraint.getDictionaryEnumeration(monitor, refColumn,
                    pattern, precedingKeys, false, sortAsc, sortByValue, offset, maxResults);
//                        for (DBDLabelValuePair pair : enumValues) {
//                            keyValues.put(pair.getValue(), pair.getLabel());
//                        }
                if (monitor.isCanceled()) {
                    return null;
                }
                if (enumValues.isEmpty()) {
                    return null;
                }
                if (enumValues.size() >= 1) {
                    firstValue = enumValues.get(0).getValue();
                    lastValue = enumValues.get(enumValues.size() - 1).getValue();
                }
                final DBDValueHandler colHandler = DBUtils.findValueHandler(fkAttribute.getDataSource(), fkAttribute);
                return new EnumValuesData(enumValues, fkColumn, colHandler);
            }

            return null;
        }


        @Override
        public Object getFamily() {
            return valueController.getExecutionContext();
        }

    }

    private class SelectorLoaderVisualizer extends ProgressLoaderVisualizer<EnumValuesData> {
        public SelectorLoaderVisualizer(SelectorLoaderService loadingService) {
            super(loadingService, editorSelector);
        }

        @Override
        public void visualizeLoading() {
            super.visualizeLoading();
        }

        @Override
        public void completeLoading(EnumValuesData result) {
            super.completeLoading(result);
            super.visualizeLoading();
            if (result != null) {
                updateDictionarySelector(result);
            }
        }
    }

    private class MoveToNextPageAction extends Action {
        boolean backwardMove;

        private void updateList() throws DBException {
            if (backwardMove && firstValue != null) {
                reloadSelectorValues(firstValue, true, Math.min(-Math.floorDiv(maxResults, 2), -1));
            } else if (!backwardMove && lastValue != null) {
                reloadSelectorValues(lastValue, true, Math.max(Math.round((float) maxResults / 2) + 1, 1));
            }
        }

        private MoveToNextPageAction(String text, boolean backwardMove, ImageDescriptor image) {
            super(text, image);
            this.backwardMove = backwardMove;
        }

        @Override
        public void run() {
            super.run();
            try {
                updateList();
            } catch (DBException e) {
                log.error("Can't load new dictionary values", e);
            }
        }

    }

}
