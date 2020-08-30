/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.data.dialogs;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.eclipse.ui.themes.ITheme;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.SimpleTypedObject;
import org.jkiss.dbeaver.model.impl.data.DefaultValueHandler;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithResult;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetPreferences;
import org.jkiss.dbeaver.ui.controls.resultset.ThemeConstants;
import org.jkiss.dbeaver.ui.data.*;
import org.jkiss.dbeaver.ui.data.managers.DefaultValueManager;
import org.jkiss.dbeaver.ui.data.registry.ValueManagerRegistry;
import org.jkiss.dbeaver.ui.internal.UIMessages;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Structure object editor
 */
public class ComplexObjectEditor extends TreeViewer {

    private static final Log log = Log.getLog(ComplexObjectEditor.class);

    private static class ComplexElement {
        boolean created, modified;
        Object value;
    }

    private interface ComplexElementWrapper {
        Object getValue();
    }

    private static class CompositeField extends ComplexElement {
        final DBSAttributeBase attribute;
        DBDValueHandler valueHandler;

        private CompositeField(DBPDataSource dataSource, DBSAttributeBase attribute, @Nullable Object value)
        {
            this.attribute = attribute;
            this.value = value;
            this.valueHandler = DBUtils.findValueHandler(dataSource, attribute);
        }
    }

    private static class ArrayInfo {
        private DBDValueHandler valueHandler;
        private DBSDataType componentType;
    }

    private static class ArrayItem extends ComplexElement {
        final ArrayInfo array;
        int index;

        private ArrayItem(ArrayInfo array, int index, Object value)
        {
            this.array = array;
            this.index = index;
            this.value = value;
        }
    }

    private static class MapEntry extends ComplexElement implements ComplexElementWrapper {
        String name;
        Object value;

        MapEntry(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public Object getValue() {
            return value;
        }
    }

    private static class CollItem extends ComplexElement implements ComplexElementWrapper {
        int index;
        Object value;

        CollItem(int index, Object value) {
            this.index = index;
            this.value = value;
        }

        @Override
        public Object getValue() {
            return value;
        }
    }

    private final IValueController parentController;
    private final IValueEditor editor;
    private DBCExecutionContext executionContext;
    private final TreeEditor treeEditor;
    private IValueEditor curCellEditor;

    private Color backgroundAdded;
    private Color backgroundDeleted;
    private Color backgroundModified;

    private CopyAction copyNameAction;
    private CopyAction copyValueAction;
    private Action addElementAction;
    private Action removeElementAction;

    private Map<Object, ComplexElement[]> childrenMap = new IdentityHashMap<>();

    public ComplexObjectEditor(IValueController parentController, IValueEditor editor, int style)
    {
        super(parentController.getEditPlaceholder(), style | SWT.SINGLE | SWT.FULL_SELECTION);
        this.parentController = parentController;
        this.editor = editor;

        ITheme currentTheme = parentController.getValueSite().getWorkbenchWindow().getWorkbench().getThemeManager().getCurrentTheme();
        this.backgroundAdded = currentTheme.getColorRegistry().get(ThemeConstants.COLOR_SQL_RESULT_CELL_NEW_BACK);
        this.backgroundDeleted = currentTheme.getColorRegistry().get(ThemeConstants.COLOR_SQL_RESULT_CELL_DELETED_BACK);
        this.backgroundModified = currentTheme.getColorRegistry().get(ThemeConstants.COLOR_SQL_RESULT_CELL_MODIFIED_BACK);

        final Tree treeControl = super.getTree();
        treeControl.setHeaderVisible(true);
        treeControl.setLinesVisible(true);

        treeControl.addControlListener(new ControlAdapter() {
            private boolean packing = false;

            @Override
            public void controlResized(ControlEvent e)
            {
                if (!packing) {
                    packing = true;
                    UIUtils.packColumns(treeControl, true, new float[]{0.2f, 0.8f});
                    if (treeControl.getColumn(0).getWidth() < 100) {
                        treeControl.getColumn(0).setWidth(100);
                    }
                    treeControl.removeControlListener(this);
                }
            }
        });

        ColumnViewerToolTipSupport.enableFor(this, ToolTip.NO_RECREATE);

        {
            TreeViewerColumn column = new TreeViewerColumn(this, SWT.NONE);
            column.getColumn().setWidth(200);
            column.getColumn().setMoveable(true);
            column.getColumn().setText(UIMessages.ui_properties_name);
            column.setLabelProvider(new PropsLabelProvider(true));
        }

        {
            TreeViewerColumn column = new TreeViewerColumn(this, SWT.NONE);
            column.getColumn().setWidth(120);
            column.getColumn().setMoveable(true);
            column.getColumn().setText(UIMessages.ui_properties_value);
            column.setLabelProvider(new PropsLabelProvider(false));
        }

        treeEditor = new TreeEditor(treeControl);
        treeEditor.horizontalAlignment = SWT.RIGHT;
        treeEditor.verticalAlignment = SWT.CENTER;
        treeEditor.grabHorizontal = true;
        treeEditor.minimumWidth = 50;

        treeControl.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(MouseEvent e)
            {
                TreeItem item = treeControl.getItem(new Point(e.x, e.y));
                if (item != null && UIUtils.getColumnAtPos(item, e.x, e.y) == 1) {
                    showEditor(item, false);
                }
            }
        });

        treeControl.addTraverseListener(e -> {
            if (e.detail == SWT.TRAVERSE_RETURN) {
                final TreeItem[] selection = treeControl.getSelection();
                if (selection.length == 0) {
                    return;
                }
                if (treeEditor.getEditor() != null && !treeEditor.getEditor().isDisposed()) {
                    // Give a chance to catch it in editor handler
                    e.doit = true;
                    return;
                }
                showEditor(selection[0], (e.stateMask & SWT.SHIFT) == SWT.SHIFT);
                e.doit = false;
                e.detail = SWT.TRAVERSE_NONE;
            } else if (e.detail == SWT.TRAVERSE_ESCAPE) {
                e.doit = false;
                disposeOldEditor();
            }
        });

        super.setContentProvider(new StructContentProvider());

        this.copyNameAction = new CopyAction(true);
        this.copyValueAction = new CopyAction(false);
        this.addElementAction = new AddElementAction();
        this.removeElementAction = new RemoveElementAction();

        addElementAction.setEnabled(true);
        removeElementAction.setEnabled(false);

        addSelectionChangedListener(event -> {
            final IStructuredSelection selection = (IStructuredSelection)event.getSelection();
            if (selection == null || selection.isEmpty()) {
                copyNameAction.setEnabled(false);
                copyValueAction.setEnabled(false);
                removeElementAction.setEnabled(false);
                addElementAction.setEnabled(getInput() instanceof DBDCollection);
            } else {
                copyNameAction.setEnabled(true);
                copyValueAction.setEnabled(true);
                final Object element = selection.getFirstElement();
                if (element instanceof ArrayItem) {
                    removeElementAction.setEnabled(true);
                    addElementAction.setEnabled(true);
                }
            }
        });

        createContextMenu();
    }

    private void createContextMenu()
    {
        Control control = getControl();
        MenuManager menuMgr = new MenuManager();
        Menu menu = menuMgr.createContextMenu(control);
        menuMgr.addMenuListener(manager -> {
            if (!getSelection().isEmpty()) {
                manager.add(copyNameAction);
                manager.add(copyValueAction);
                manager.add(new Separator());
            }
            try {
                parentController.getValueManager().contributeActions(manager, parentController, editor);
            } catch (DBCException e) {
                log.error(e);
            }
        });
        menuMgr.setRemoveAllWhenShown(true);
        control.setMenu(menu);
        control.addDisposeListener(e -> menuMgr.dispose());
    }

    public void setModel(DBCExecutionContext executionContext, final Object value)
    {
        getTree().setRedraw(false);
        try {
            this.executionContext = executionContext;
            this.childrenMap.clear();
            setInput(value);
            expandToLevel(2);
        } finally {
            getTree().setRedraw(true);
        }
    }

    private void showEditor(final TreeItem item, boolean advanced) {
        // Clean up any previous editor control
        disposeOldEditor();
        if (item == null) {
            return;
        }

        try {
            IValueController valueController = new ComplexValueController(
                (ComplexElement)item.getData(),
                advanced ? IValueController.EditType.EDITOR : IValueController.EditType.INLINE);

            IValueEditor newCellEditor = valueController.getValueManager().createEditor(valueController);
            if (newCellEditor != null) {
                newCellEditor.createControl();
                if (newCellEditor instanceof IValueEditorStandalone) {
                    ((IValueEditorStandalone) newCellEditor).showValueEditor();
                } else {
                    Control editorControl = newCellEditor.getControl();
                    if (editorControl != null) {
                        Point editorSize = editorControl.computeSize(SWT.DEFAULT, SWT.DEFAULT);
                        treeEditor.minimumHeight = editorSize.y;
                        //treeEditor.minimumWidth = editorSize.y;
                        treeEditor.setEditor(editorControl, item, 1);
                        editorControl.setFocus();
                    }
                }
                if (!advanced) {
                    newCellEditor.primeEditorValue(valueController.getValue());
                }
                curCellEditor = newCellEditor;
            }
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Cell editor", "Can't open cell editor", e);
        }
    }

    private void disposeOldEditor()
    {
        curCellEditor = null;
        Control oldEditor = treeEditor.getEditor();
        if (oldEditor != null) oldEditor.dispose();
    }

    public Object extractValue() {
        Object complexValue = getInput();
        final ComplexElement[] items = childrenMap.get(complexValue);
        if (complexValue instanceof DBDValueCloneable) {
            try {
                complexValue = ((DBDValueCloneable) complexValue).cloneValue(new VoidProgressMonitor());
            } catch (DBCException e) {
                log.error("Error cloning complex value", e);
            }
        }
        if (complexValue instanceof DBDComposite) {
            for (ComplexElement item : items) {
                try {
                    ((DBDComposite) complexValue).setAttributeValue(((CompositeField) item).attribute, item.value);
                } catch (DBCException e) {
                    log.error("Error setting attribute value", e);
                }
            }
        } else if (complexValue instanceof DBDCollection) {
            if (items != null) {
                final Object[] newValues = new Object[items.length];
                for (int i = 0; i < items.length; i++) {
                    newValues[i] = items[i].value;
                }
                ((DBDCollection) complexValue).setContents(newValues);
            }
        }
        return complexValue;
    }

    private String getColumnText(ComplexElement obj, int columnIndex, DBDDisplayFormat format) {
        if (obj instanceof CompositeField) {
            CompositeField field = (CompositeField) obj;
            if (columnIndex == 0) {
                return field.attribute.getName();
            }
            return getValueText(field.valueHandler, field.attribute, field.value, format);
        } else if (obj instanceof ArrayItem) {
            ArrayItem item = (ArrayItem) obj;
            if (columnIndex == 0) {
                return String.valueOf(item.index);
            }
            return getValueText(item.array.valueHandler, item.array.componentType, item.value, format);
        } else if (obj instanceof MapEntry) {
            return columnIndex == 0 ? ((MapEntry) obj).name : CommonUtils.toString(((MapEntry) obj).value);
        } else if (obj instanceof CollItem) {
            return columnIndex == 0 ? String.valueOf(((CollItem) obj).index) : CommonUtils.toString(((CollItem) obj).value);
        }
        return String.valueOf(columnIndex);
    }

    private String getValueText(@NotNull DBDValueHandler valueHandler, @NotNull DBSTypedObject type, @Nullable Object value, @NotNull DBDDisplayFormat format)
    {
        if (value instanceof DBDCollection) {
            return "[" + ((DBDCollection) value).getComponentType().getName() + " - " + ((DBDCollection) value).getItemCount() + "]";
        } else if (value instanceof DBDComposite) {
            return "[" + ((DBDComposite) value).getDataType().getName() + "]";
        } else if (value instanceof DBDReference) {
            return "--> [" + ((DBDReference) value).getReferencedType().getName() + "]";
        } else {
            return valueHandler.getValueDisplayString(type, value, format);
        }
    }

    private void autoUpdateComplexValue() {
        if (DBWorkbench.getPlatform().getPreferenceStore().getBoolean(ResultSetPreferences.RS_EDIT_AUTO_UPDATE_VALUE)) {
            parentController.updateValue(extractValue(), false);
        }
    }

    private class ComplexValueController implements IValueController, IMultiController {
        private final ComplexElement item;
        private final DBDValueHandler valueHandler;
        private final DBSTypedObject type;
        private final String name;
        private final Object value;
        private final EditType editType;

        ComplexValueController(ComplexElement obj, EditType editType) throws DBCException {
            this.item = obj;
            if (this.item instanceof CompositeField) {
                CompositeField field = (CompositeField) this.item;
                valueHandler = field.valueHandler;
                type = field.attribute;
                name = field.attribute.getName();
                value = field.value;
            } else if (this.item instanceof ArrayItem) {
                ArrayItem arrayItem = (ArrayItem) this.item;
                valueHandler = arrayItem.array.valueHandler;
                type = arrayItem.array.componentType;
                name = type.getTypeName() + "["  + arrayItem.index + "]";
                value = arrayItem.value;
            } else if (this.item instanceof MapEntry) {
                valueHandler = DefaultValueHandler.INSTANCE;
                type = SimpleTypedObject.DEFAULT_TYPE;
                name = ((MapEntry) this.item).name;
                value = ((MapEntry) this.item).value;
            } else if (this.item instanceof CollItem) {
                valueHandler = DefaultValueHandler.INSTANCE;
                type = SimpleTypedObject.DEFAULT_TYPE;
                name = String.valueOf(((CollItem) this.item).index);
                value = ((CollItem) this.item).value;
            } else {
                throw new DBCException("Unsupported complex object element: " + this.item);
            }
            this.editType = editType;
        }

        @NotNull
        @Override
        public DBCExecutionContext getExecutionContext()
        {
            return executionContext;
        }

        @NotNull
        @Override
        public IDataController getDataController() {
            return parentController.getDataController();
        }

        @Override
        public String getValueName()
        {
            return name;
        }

        @Override
        public DBSTypedObject getValueType()
        {
            return type;
        }

        @Nullable
        @Override
        public Object getValue()
        {
            return value;
        }

        @Override
        public void updateValue(Object value, boolean updatePresentation)
        {
            if (CommonUtils.equalObjects(this.item.value, value)) {
                return;
            }
            this.item.value = value;
            this.item.modified = true;
            autoUpdateComplexValue();
            refresh(this.item);
        }

        @Override
        public void updateSelectionValue(Object value) {
            updateValue(value, true);
        }

        @Override
        public DBDValueHandler getValueHandler()
        {
            return valueHandler;
        }

        @Override
        public IValueManager getValueManager() {
            DBSTypedObject valueType = getValueType();
            if (valueType == null) {
                return DefaultValueManager.INSTANCE;
            }
            return ValueManagerRegistry.findValueManager(
                getExecutionContext().getDataSource(),
                valueType,
                getValueHandler().getValueObjectType(valueType));
        }

        @Override
        public EditType getEditType()
        {
            return editType;
        }

        @Override
        public boolean isReadOnly()
        {
            return parentController.isReadOnly();
        }

        @Override
        public IWorkbenchPartSite getValueSite()
        {
            return parentController.getValueSite();
        }

        @Override
        public Composite getEditPlaceholder()
        {
            return getTree();
        }

        @Override
        public void refreshEditor() {
            parentController.refreshEditor();
        }

        @Override
        public void showMessage(String message, DBPMessageType messageType)
        {

        }

        @Override
        public void closeInlineEditor() {
            disposeOldEditor();
        }

        @Override
        public void nextInlineEditor(boolean next) {
            disposeOldEditor();
        }
    }

    class StructContentProvider implements IStructuredContentProvider, ITreeContentProvider
    {
        StructContentProvider()
        {
        }

        @Override
        public void inputChanged(Viewer v, Object oldInput, Object newInput)
        {
        }

        @Override
        public void dispose()
        {
        }

        @Override
        public Object[] getElements(Object parent)
        {
            return getChildren(parent);
        }

        @Nullable
        @Override
        public Object getParent(Object child)
        {
            return null;
        }

        @Override
        public ComplexElement[] getChildren(Object parent)
        {
            ComplexElement[] children = childrenMap.get(parent);
            if (children != null) {
                return children;
            }

            // Unwrap complex items
            if (parent instanceof ComplexElementWrapper) {
                parent = ((ComplexElementWrapper) parent).getValue();
            }

            if (parent instanceof DBDComposite) {
                DBDComposite structure = (DBDComposite)parent;
                try {
                    DBSAttributeBase[] attributes = structure.getAttributes();
                    children = new CompositeField[attributes.length];
                    for (int i = 0; i < attributes.length; i++) {
                        DBSAttributeBase attr = attributes[i];
                        Object value = structure.getAttributeValue(attr);
                        children[i] = new CompositeField(structure.getDataType().getDataSource(), attr, value);
                    }
                } catch (DBException e) {
                    log.error("Error getting structure meta data", e);
                }
            } else if (parent instanceof DBDCollection) {
                DBDCollection array = (DBDCollection)parent;
                ArrayInfo arrayInfo = makeArrayInfo(array);

                children = new ArrayItem[array.getItemCount()];
                for (int i = 0; i < children.length; i++) {
                    children[i] = new ArrayItem(arrayInfo, i, array.getItem(i));
                }
            } else if (parent instanceof DBDReference) {
                final DBDReference reference = (DBDReference)parent;
                DBRRunnableWithResult<Object> runnable = new DBRRunnableWithResult<Object>() {
                    @Override
                    public void run(DBRProgressMonitor monitor) throws InvocationTargetException {
                        monitor.beginTask("Read object reference", 1);
                        try (DBCSession session = executionContext.openSession(monitor, DBCExecutionPurpose.UTIL, "Read reference value")) {
                            result = reference.getReferencedObject(session);
                        } catch (DBCException e) {
                            throw new InvocationTargetException(e);
                        } finally {
                            monitor.done();
                        }
                    }
                };
                UIUtils.runInUI(runnable);
                children = getChildren(runnable.getResult());
            } else if (parent instanceof CompositeField) {
                Object value = ((CompositeField) parent).value;
                if (isComplexType(value)) {
                    children = getChildren(value);
                }
            } else if (parent instanceof ArrayItem) {
                Object value = ((ArrayItem) parent).value;
                if (isComplexType(value)) {
                    children = getChildren(value);
                }
            } else if (parent instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) parent;
                children = new MapEntry[map.size()];
                Iterator<? extends Map.Entry<?, ?>> entries = map.entrySet().iterator();

                for (int i = 0; i < children.length; i++) {
                    Map.Entry<?, ?> entry = entries.next();
                    children[i] = new MapEntry(CommonUtils.toString(entry.getKey()), entry.getValue());
                }
            } else if (parent instanceof Collection) {
                Collection coll = (Collection)parent;
                children = new CollItem[coll.size()];
                Iterator iterator = coll.iterator();
                for (int i = 0; i < children.length; i++) {
                    children[i] = new CollItem(i, iterator.next());
                }
            }
            if (children == null) {
                children = new ComplexElement[0];
            }
            childrenMap.put(parent, children);

            return children;
        }

        private boolean isComplexType(Object value) {
            return value instanceof DBDComplexValue;
        }

        @Override
        public boolean hasChildren(Object parent)
        {
            if (parent instanceof ComplexElementWrapper) {
                parent = ((ComplexElementWrapper) parent).getValue();
            }

            return
                parent instanceof DBDComposite ||
                parent instanceof DBDCollection ||
                parent instanceof DBDReference ||
                (parent instanceof CompositeField && hasChildren(((CompositeField) parent).value)) ||
                (parent instanceof ArrayItem && hasChildren(((ArrayItem) parent).value)) ||
                (parent instanceof Map && !(((Map) parent).isEmpty())) ||
                (parent instanceof Collection && !(((Collection) parent).isEmpty()));
        }
    }

    @NotNull
    private ArrayInfo makeArrayInfo(DBDCollection array) {
        ArrayInfo arrayInfo = new ArrayInfo();
        arrayInfo.componentType = array.getComponentType();
        if (arrayInfo.componentType == null) {
            arrayInfo.valueHandler = parentController.getExecutionContext().getDataSource().getContainer().getDefaultValueHandler();;
        } else {
            arrayInfo.valueHandler = DBUtils.findValueHandler(arrayInfo.componentType.getDataSource(), arrayInfo.componentType);
        }
        return arrayInfo;
    }

    private void shiftArrayItems(ComplexElement[] arrayItems, int startIndex, int inc) {
        for (int i = startIndex; i < arrayItems.length; i++) {
            ((ArrayItem)arrayItems[i]).index += inc;
        }
    }

    private class PropsLabelProvider extends CellLabelProvider
    {
        private final boolean isName;
        PropsLabelProvider(boolean isName)
        {
            this.isName = isName;
        }

        public String getText(ComplexElement obj, int columnIndex)
        {
            return getColumnText(obj, columnIndex, DBDDisplayFormat.UI);
        }

        @Override
        public String getToolTipText(Object obj)
        {
            if (obj instanceof CompositeField) {
                return ((CompositeField) obj).attribute.getName() + " " + ((CompositeField) obj).attribute.getTypeName();
            }
            return null;
        }

        @Override
        public void update(ViewerCell cell)
        {
            ComplexElement element = (ComplexElement) cell.getElement();
            cell.setText(getText(element, cell.getColumnIndex()));
            if (element.created) {
                cell.setBackground(backgroundAdded);
            } else if (element.modified) {
                cell.setBackground(backgroundModified);
            } else {
                cell.setBackground(null);
            }
        }

    }

    private class CopyAction extends Action {
        private final boolean isName;
        CopyAction(boolean isName) {
            super(WorkbenchMessages.Workbench_copy + " " + getTree().getColumn(isName ? 0 : 1).getText());
            this.isName = isName;
        }

        @Override
        public void run()
        {
            final IStructuredSelection selection = getStructuredSelection();
            if (!selection.isEmpty()) {
                String text = getColumnText(
                    (ComplexElement) selection.getFirstElement(),
                    isName ? 0 : 1,
                    DBDDisplayFormat.NATIVE);
                if (text != null) {
                    UIUtils.setClipboardContents(getTree().getDisplay(), TextTransfer.getInstance(), text);
                }
            }
        }
    }

    private class AddElementAction extends Action {
        AddElementAction() {
            super("Add element", DBeaverIcons.getImageDescriptor(UIIcon.ROW_ADD));
        }

        @Override
        public void run() {
            disposeOldEditor();
            DBDCollection collection = (DBDCollection) getInput();
            ComplexElement[] arrayItems = childrenMap.get(collection);
            if (collection == null) {
                try {
                    collection = DBUtils.createNewAttributeValue(
                        parentController.getExecutionContext(),
                        parentController.getValueHandler(),
                        parentController.getValueType(),
                        DBDCollection.class);
                    setInput(collection);
                } catch (DBCException e) {
                    DBWorkbench.getPlatformUI().showError("New object create", "Error creating new collection", e);
                    return;
                }
            }
            if (arrayItems == null) {
                arrayItems = new ComplexElement[0];
                //log.error("Can't find children items for add");
                //return;
            }
            final IStructuredSelection selection = getStructuredSelection();
            ArrayItem newItem;
            if (selection.isEmpty()) {
                newItem = new ArrayItem(makeArrayInfo(collection), arrayItems.length, null);
            } else {
                ArrayItem curItem = (ArrayItem) selection.getFirstElement();
                newItem = new ArrayItem(curItem.array, curItem.index + 1, null);
            }
            shiftArrayItems(arrayItems, newItem.index, 1);
            arrayItems = ArrayUtils.insertArea(ComplexElement.class, arrayItems, newItem.index, new ComplexElement[] {newItem} );
            childrenMap.put(collection, arrayItems);

            refresh();

            final Widget treeItem = findItem(newItem);
            if (treeItem != null) {
                showEditor((TreeItem) treeItem, false);
            }

            autoUpdateComplexValue();
        }

    }

    private class RemoveElementAction extends Action {
        RemoveElementAction() {
            super("Remove element", DBeaverIcons.getImageDescriptor(UIIcon.ROW_DELETE));
        }

        @Override
        public void run() {
            final IStructuredSelection selection = getStructuredSelection();
            if (selection.isEmpty()) {
                return;
            }

            disposeOldEditor();

            DBDCollection collection = (DBDCollection) getInput();
            ComplexElement[] arrayItems = childrenMap.get(collection);
            if (arrayItems == null) {
                log.error("Can't find children items for delete");
                return;
            }
            ArrayItem item = (ArrayItem)selection.getFirstElement();
            int deleteIndex = item.index;
            shiftArrayItems(arrayItems, deleteIndex, -1);
            arrayItems = ArrayUtils.remove(ComplexElement.class, arrayItems, item);
            childrenMap.put(collection, arrayItems);
            if (deleteIndex >= arrayItems.length) {
                deleteIndex--;
            }
            if (deleteIndex >= 0) {
                setSelection(new StructuredSelection(arrayItems[deleteIndex]));
            }
            refresh();

            autoUpdateComplexValue();
        }
    }

    public void contributeActions(IContributionManager manager) {
        manager.add(addElementAction);
        manager.add(removeElementAction);
    }

}
