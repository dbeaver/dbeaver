/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.dialogs.data;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.themes.ITheme;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithResult;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ThemeConstants;
import org.jkiss.dbeaver.ui.data.*;
import org.jkiss.dbeaver.ui.data.registry.ValueManagerRegistry;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.IdentityHashMap;
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
            column.getColumn().setText(CoreMessages.ui_properties_name);
            column.setLabelProvider(new PropsLabelProvider(true));
        }

        {
            TreeViewerColumn column = new TreeViewerColumn(this, SWT.NONE);
            column.getColumn().setWidth(120);
            column.getColumn().setMoveable(true);
            column.getColumn().setText(CoreMessages.ui_properties_value);
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

        treeControl.addTraverseListener(new TraverseListener() {
            @Override
            public void keyTraversed(TraverseEvent e)
            {
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
                }
            }
        });

        super.setContentProvider(new StructContentProvider());

        this.copyNameAction = new CopyAction(true);
        this.copyValueAction = new CopyAction(false);
        this.addElementAction = new AddElementAction();
        this.removeElementAction = new RemoveElementAction();

        addElementAction.setEnabled(true);
        removeElementAction.setEnabled(false);

        addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
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
            }
        });

        createContextMenu();
    }

    private void createContextMenu()
    {
        Control control = getControl();
        MenuManager menuMgr = new MenuManager();
        Menu menu = menuMgr.createContextMenu(control);
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager)
            {
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
            }
        });
        menuMgr.setRemoveAllWhenShown(true);
        control.setMenu(menu);
    }

    @Override
    public DBDComplexValue getInput() {
        return (DBDComplexValue)super.getInput();
    }

    public void setModel(DBCExecutionContext executionContext, final DBDComplexValue value)
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

            curCellEditor = valueController.getValueManager().createEditor(valueController);
            if (curCellEditor != null) {
                curCellEditor.createControl();
                if (curCellEditor instanceof IValueEditorStandalone) {
                    ((IValueEditorStandalone) curCellEditor).showValueEditor();
                } else if (curCellEditor.getControl() != null) {
                    treeEditor.setEditor(curCellEditor.getControl(), item, 1);
                }
                if (!advanced) {
                    curCellEditor.primeEditorValue(valueController.getValue());
                }
            }
        } catch (DBException e) {
            DBUserInterface.getInstance().showError("Cell editor", "Can't open cell editor", e);
        }
    }

    private void disposeOldEditor()
    {
        curCellEditor = null;
        Control oldEditor = treeEditor.getEditor();
        if (oldEditor != null) oldEditor.dispose();
    }

    public Object extractValue() {
        DBDComplexValue complexValue = getInput();
        final ComplexElement[] items = childrenMap.get(complexValue);
        if (complexValue instanceof DBDValueCloneable) {
            try {
                complexValue = (DBDComplexValue) ((DBDValueCloneable) complexValue).cloneValue(new VoidProgressMonitor());
            } catch (DBCException e) {
                log.error("Error cloning complex value", e);
            }
        }
        if (complexValue instanceof DBDComposite) {
            for (int i = 0; i < items.length; i++) {
                ((DBDComposite) complexValue).setAttributeValue(((CompositeField)items[i]).attribute, items[i].value);
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

    private class ComplexValueController implements IValueController, IMultiController {
        private final ComplexElement item;
        private final DBDValueHandler valueHandler;
        private final DBSTypedObject type;
        private final String name;
        private final Object value;
        private final EditType editType;

        public ComplexValueController(ComplexElement obj, EditType editType) throws DBCException {
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
            refresh(this.item);
        }

        @Override
        public DBDValueHandler getValueHandler()
        {
            return valueHandler;
        }

        @Override
        public IValueManager getValueManager() {
            DBSTypedObject valueType = getValueType();
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
        public StructContentProvider()
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
                    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                    {
                        try (DBCSession session = executionContext.openSession(monitor, DBCExecutionPurpose.UTIL, "Read reference value")) {
                            result = reference.getReferencedObject(session);
                        } catch (DBCException e) {
                            throw new InvocationTargetException(e);
                        }
                    }
                };
                DBeaverUI.runInUI(runnable);
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
            }
            if (children != null) {
                childrenMap.put(parent, children);
            }
            return children;
        }

        private boolean isComplexType(Object value) {
            return value instanceof DBDComplexValue;
        }

        @Override
        public boolean hasChildren(Object parent)
        {
            return
                parent instanceof DBDComposite ||
                parent instanceof DBDCollection ||
                parent instanceof DBDReference ||
                (parent instanceof CompositeField && hasChildren(((CompositeField) parent).value)) ||
                (parent instanceof ArrayItem && hasChildren(((ArrayItem) parent).value));
        }
    }

    @NotNull
    private ArrayInfo makeArrayInfo(DBDCollection array) {
        ArrayInfo arrayInfo = new ArrayInfo();
        arrayInfo.componentType = array.getComponentType();
        arrayInfo.valueHandler = DBUtils.findValueHandler(arrayInfo.componentType.getDataSource(), arrayInfo.componentType);
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
        public PropsLabelProvider(boolean isName)
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
        public CopyAction(boolean isName) {
            super(CoreMessages.controls_itemlist_action_copy + " " + getTree().getColumn(isName ? 0 : 1).getText());
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
        public AddElementAction() {
            super("Add element", DBeaverIcons.getImageDescriptor(UIIcon.ROW_ADD));
        }

        @Override
        public void run() {
            DBDCollection collection = (DBDCollection) getInput();
            ComplexElement[] arrayItems = childrenMap.get(collection);
            if (arrayItems == null) {
                log.error("Can't find children items for add");
                return;
            }
            final IStructuredSelection selection = getStructuredSelection();
            ArrayItem newItem;
            if (selection.isEmpty()) {
                newItem = new ArrayItem(makeArrayInfo(collection), 0, null);
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
        }
    }

    private class RemoveElementAction extends Action {
        public RemoveElementAction() {
            super("Remove element", DBeaverIcons.getImageDescriptor(UIIcon.ROW_DELETE));
        }

        @Override
        public void run() {
            final IStructuredSelection selection = getStructuredSelection();
            if (selection.isEmpty()) {
                return;
            }

            DBDCollection collection = (DBDCollection) getInput();
            ComplexElement[] arrayItems = childrenMap.get(collection);
            if (arrayItems == null) {
                log.error("Can't find children items for delete");
                return;
            }
            ArrayItem item = (ArrayItem)selection.getFirstElement();
            shiftArrayItems(arrayItems, item.index, -1);
            arrayItems = ArrayUtils.remove(ComplexElement.class, arrayItems, item);
            childrenMap.put(collection, arrayItems);
            refresh();
        }
    }

    public void contributeActions(IContributionManager manager) {
        manager.add(addElementAction);
        manager.add(removeElementAction);
    }

}
