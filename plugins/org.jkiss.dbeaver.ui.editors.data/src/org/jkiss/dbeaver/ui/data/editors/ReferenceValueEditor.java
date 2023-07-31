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
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.impl.AbstractExecutionSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.AbstractLoadService;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.ProgressLoaderVisualizer;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetUtils;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.data.IAttributeController;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.dbeaver.ui.editors.data.DatabaseDataEditor;
import org.jkiss.dbeaver.ui.editors.object.struct.EditDictionaryPage;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerObjectOpen;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.ReaderWriterLock.ExceptableFunction;

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

    private final IValueController valueController;
    private IValueEditor valueEditor;
    private DBSEntityReferrer refConstraint;
    private Table editorSelector;
    private Text valueFilterText;
    private CLabel pageStatusLabel;
    private static volatile boolean sortByValue = true; // It is static to save its value between editors
    private static volatile boolean sortAsc = true;
    private TableColumn prevSortColumn = null;
    private Font boldFont;
    private ViewController controller;

    private class ViewController {
        private final int pageSize;
        private long currOffset = 0;
        private long currPageNumber = 0;
        private long maxKnownPage = 0;
        private long maxKnownIndex = 0;
        private boolean nextPageAvailable = false;
        private boolean limitFound = false;
        private String filterPattern = null;
        private Object currentValue = null;

        public ViewController(int pageSize) {
            this.pageSize = pageSize;
        }
        
        @NotNull
        public String makeStatusString() {
            return limitFound ?
                NLS.bind(ResultSetMessages.reference_value_editor_current_pagination_value, currPageNumber + 1, maxKnownPage + 1) :
                NLS.bind(ResultSetMessages.reference_value_editor_current_page_value, currPageNumber + 1);
        }
        
        public boolean isNextPageAvailable() {
            return !(limitFound && currPageNumber >= maxKnownPage);
        }
        
        public boolean isPrevPageAvailable() {
            return currPageNumber > 0;
        }

        public void goToNextPage() {
            if (nextPageAvailable) {
                this.currPageNumber++;
                this.currOffset = this.currPageNumber * this.pageSize;
                this.reloadData(null);
            }
        }

        public void goToPrevPage() {
            if (this.isPrevPageAvailable()) {
                this.currPageNumber--;
                this.currOffset = this.currPageNumber * this.pageSize;
                this.reloadData(null);
            }
        }

        public void filter(@Nullable String pattern) {
            if (CommonUtils.isEmpty(CommonUtils.toString(pattern))) {
                this.reset(currentValue);
            } else if (CommonUtils.equalObjects(String.valueOf(filterPattern), String.valueOf(pattern))) {
                selectCurrentValue();
            } else {
                this.applyFilter(pattern);
            }
        }

        private void applyFilter(@NotNull String pattern) {
            this.currentValue = null;
            this.filterPattern = pattern;
            this.resetPages();
            this.reloadData(null);
        }

        public void reset(@Nullable Object valueToShow) {
            this.currentValue = valueToShow;
            this.filterPattern = null;
            this.resetPages();
            this.reloadData(valueToShow);
        }
        
        private void resetPages() {
            this.currOffset = 0;
            this.currPageNumber = 0;
            this.maxKnownPage = 0;
            this.maxKnownIndex = 0;
            this.limitFound = false;
        }

        public void reload() {
            if (filterPattern == null) {
                this.reset(this.currentValue);
            } else {
                this.applyFilter(this.filterPattern);
            }
        }

        private void reloadData(@Nullable Object valueToShow) {
            SelectorLoaderService loadingService = new SelectorLoaderService(accessor -> {
                List<DBDLabelValuePair> data;
                if (filterPattern == null) {
                    if (valueToShow != null) {
                        long valueIndex = accessor.findValueIndex(valueToShow);
                        long valuePageNumber = valueIndex / pageSize; // integer division without remainder and rounding
                        this.currPageNumber = valuePageNumber;
                        this.currOffset = valuePageNumber * this.pageSize;
                    }
                    data = accessor.getValues(currOffset, pageSize);
                } else {
                    data = accessor.getSimilarValues(filterPattern, false, true, currOffset, pageSize);
                }
                nextPageAvailable = data.size() >= pageSize;
                limitFound |= !nextPageAvailable;
                if (data.size() > 0) {
                    maxKnownPage = Math.max(maxKnownPage, this.currPageNumber);
                    maxKnownIndex = Math.max(maxKnownIndex, this.currPageNumber * this.pageSize + data.size());
                }
                if (limitFound) {
                    currPageNumber = Math.min(currPageNumber, maxKnownPage);
                }   
                return data;
            });
            LoadingJob.createService(loadingService, new SelectorLoaderVisualizer(loadingService)).schedule();
        }
    }

    public ReferenceValueEditor(IValueController valueController, IValueEditor valueEditor) {
        this.valueController = valueController;
        this.valueEditor = valueEditor;
        DBCExecutionContext executionContext = valueController.getExecutionContext();

        int pageSize = executionContext == null ? 200
            : executionContext.getDataSource().getContainer().getPreferenceStore().getInt(ModelPreferences.DICTIONARY_MAX_ROWS);

        this.controller = new ViewController(pageSize);
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

                Link hintLabel = UIUtils.createLink(
                    labelGroup,
                    "(<a>" + ResultSetMessages.reference_value_editor_define_description_value + "</a>)",
                    new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            EditDictionaryPage editDictionaryPage = new EditDictionaryPage(refTable);
                            if (editDictionaryPage.edit(parent.getShell())) {
                                controller.reload();
                            }
                        }
                    }
                );
                hintLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END));
            }
        }
        if (refConstraint instanceof DBSEntityAssociation) {
            valueFilterText = new Text(parent, SWT.BORDER);
            valueFilterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            valueFilterText.addModifyListener(e -> {
                String filterPattern = valueFilterText.getText();
                controller.filter(filterPattern);
            });
            valueFilterText.addPaintListener(e -> {
                if (valueFilterText.isEnabled() && valueFilterText.getCharCount() == 0) {
                    e.gc.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
                    e.gc.drawText(ResultSetMessages.reference_value_editor_search_hint_value, 2, 0, true);
                    e.gc.setFont(null);
                }
            });
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
                controller.reset(curEditorValue);
            }
        };
        if (control instanceof Text) {
            ((Text)control).addModifyListener(modifyListener);
        } else if (control instanceof StyledText) {
            ((StyledText)control).addModifyListener(modifyListener);
        }

        final Object curValue = valueController.getValue();

        controller.reset(curValue);

        return true;
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
    
    private Action actionGoBackward = new Action("Move Backward", DBeaverIcons.getImageDescriptor(UIIcon.ARROW_LEFT)) {
        @Override
        public void run() {
            controller.goToPrevPage();
        }
    }; 
    private Action actionGoForward = new Action("Move Forward", DBeaverIcons.getImageDescriptor(UIIcon.ARROW_RIGHT)) {
        @Override
        public void run() {
            controller.goToNextPage();
        }
    };

    private ControlContribution pageStatusLabelContribution = new ControlContribution(null) {
        @Override
        protected Control createControl(Composite parent) {
            return pageStatusLabel = new CLabel(parent, SWT.NONE);
        }
        
        @Override
        protected int computeWidth(Control control) {
            return control.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x;
        }
    };


    /**
     * Returns action to allow editor paging
     *
     * @return actions for paging
     */
    public ContributionItem[] getContributionItems() {
        return new ContributionItem[]{
            pageStatusLabelContribution,
            ActionUtils.makeActionContribution(actionGoBackward, false),
            ActionUtils.makeActionContribution(actionGoForward, false)
        };
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
            controller.reload();
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
        private ExceptableFunction<DBSDictionaryAccessor, List<DBDLabelValuePair>, DBException> action;

        private SelectorLoaderService(ExceptableFunction<DBSDictionaryAccessor, List<DBDLabelValuePair>, DBException> action) {
            super(ResultSetMessages.dialog_value_view_job_selector_name + valueController.getValueName() + " possible values");
            this.action = action;
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
            IAttributeController attributeController = (IAttributeController) valueController;
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
                try (DBSDictionaryAccessor accessor = enumConstraint.getDictionaryAccessor(
                    new AbstractExecutionSource(null, valueController.getExecutionContext(), ReferenceValueEditor.this), monitor,
                    precedingKeys, refColumn, sortAsc, !sortByValue)) {
                    List<DBDLabelValuePair> enumValues = action.apply(accessor);
                    if (monitor.isCanceled()) {
                        return null;
                    }
                    if (enumValues.isEmpty()) {
                        return null;
                    }
                    final DBDValueHandler colHandler = DBUtils.findValueHandler(fkAttribute.getDataSource(), fkAttribute);
                    return new EnumValuesData(enumValues, fkColumn, colHandler);
                } catch (Exception e) {
                    throw new DBException("Failed to load values", e);
                }
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
            boolean dataObtained = result != null && !result.keyValues.isEmpty();
            
            super.completeLoading(result);
            super.visualizeLoading();
            if (result != null) {
                updateDictionarySelector(result);
            }

            actionGoBackward.setEnabled(controller.isPrevPageAvailable());
            actionGoForward.setEnabled(controller.isNextPageAvailable());
            editorSelector.setEnabled(dataObtained || controller.currPageNumber > 0);
            if (pageStatusLabel != null) {
                pageStatusLabel.setText(controller.makeStatusString());
                pageStatusLabelContribution.update();
                UIUtils.asyncExec(() -> pageStatusLabel.getParent().getParent().pack(true));
            }
        }
    }
}
