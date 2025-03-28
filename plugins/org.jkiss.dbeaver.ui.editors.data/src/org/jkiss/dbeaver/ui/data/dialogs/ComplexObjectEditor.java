/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.DBValueFormatting;
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
import org.jkiss.dbeaver.ui.controls.TreeContentProvider;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetPreferences;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetThemeSettings;
import org.jkiss.dbeaver.ui.data.*;
import org.jkiss.dbeaver.ui.data.managers.BaseValueManager;
import org.jkiss.dbeaver.ui.data.managers.DefaultValueManager;
import org.jkiss.dbeaver.ui.data.registry.ValueManagerRegistry;
import org.jkiss.dbeaver.ui.editors.data.internal.DataEditorsMessages;
import org.jkiss.dbeaver.ui.internal.UIMessages;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Structure object editor
 */
public class ComplexObjectEditor extends TreeViewer {

    private static final Log log = Log.getLog(ComplexObjectEditor.class);

    private final IValueController parentController;
    private final IValueEditor editor;
    private DBCExecutionContext executionContext;
    private final TreeEditor treeEditor;
    private IValueEditor curCellEditor;

    private final Color backgroundAdded;
    private final Color backgroundDeleted;
    private final Color backgroundModified;
    private final Color foregroundReadOnly;

    private final CopyAction copyNameAction;
    private final CopyAction copyValueAction;
    private final SetToNullAction setToNullAction;
    private final Action addElementAction;
    private final Action removeElementAction;
    private final Action moveElementUpAction;
    private final Action moveElementDownAction;

    // Model object -> intermediate wrapper object
    private final Map<Object, Object> cache = new IdentityHashMap<>();

    public ComplexObjectEditor(IValueController parentController, IValueEditor editor, int style)
    {
        super(parentController.getEditPlaceholder(), style | SWT.SINGLE | SWT.FULL_SELECTION);
        this.parentController = parentController;
        this.editor = editor;

        this.backgroundAdded = ResultSetThemeSettings.instance.backgroundAdded;
        this.backgroundDeleted = ResultSetThemeSettings.instance.backgroundDeleted;
        this.backgroundModified = ResultSetThemeSettings.instance.backgroundModified;
        this.foregroundReadOnly = ResultSetThemeSettings.instance.foregroundNull;

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
                    treeControl.removeControlListener(this);
                    UIUtils.packColumns(treeControl, true, new float[]{0.2f, 0.8f});
                    if (treeControl.getColumn(0).getWidth() < 100) {
                        treeControl.getColumn(0).setWidth(100);
                    }
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
        this.setToNullAction = new SetToNullAction();
        this.addElementAction = new AddElementAction();
        this.removeElementAction = new RemoveElementAction();
        this.moveElementUpAction = new MoveElementAction(SWT.UP);
        this.moveElementDownAction = new MoveElementAction(SWT.DOWN);

        addSelectionChangedListener(event -> updateActions());
        createContextMenu();
        updateActions();
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
                manager.add(setToNullAction);
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
            this.cache.clear();
            setInput(wrap(null, value));
            expandAll();
            updateActions();
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

        if (isComplexType(item.getData())) {
            // No editor for complex types themselves (only leaves can be edited)
            return;
        }

        try {
            IValueController valueController = new ComplexValueController(
                (ComplexElementItem) item.getData(),
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
        return unwrap(getInput());
    }

    @NotNull
    private String getColumnText(ComplexElementItem item, int columnIndex, DBDDisplayFormat format) {
        if (columnIndex == 0) {
            return item.getName();
        } else {
            return getValueText(item.getValueHandler(), item.getDataType(), item.value, format);
        }
    }

    @Nullable
    private Image getColumnImage(@NotNull ComplexElementItem item, int columnIndex) {
        if (columnIndex == 0) {
            return DBeaverIcons.getImage(DBValueFormatting.getTypeImage(item.getDataType()));
        } else {
            return null;
        }
    }

    private String getValueText(@NotNull DBDValueHandler valueHandler, @NotNull DBSTypedObject type, @Nullable Object value, @NotNull DBDDisplayFormat format)
    {
        if (value instanceof CollectionElement) {
            final CollectionElement element = (CollectionElement) value;
            if (element.source instanceof DBDCollection) {
                return "[" + ((DBDCollection) element.source).getComponentType().getName() + " - " + element.items.size() + "]";
            } else {
                return "[" + element.items.size() + "]";
            }
        } else if (value instanceof CompositeElement) {
            return "[" + ((CompositeElement) value).type.getName() + "]";
        } else if (value instanceof ReferenceElement) {
            return "--> [" + ((ReferenceElement) value).reference.getReferencedType().getName() + "]";
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
        private final ComplexElementItem item;
        private final DBDValueHandler valueHandler;
        private final DBSTypedObject type;
        private final String name;
        private final Object value;
        private final EditType editType;

        ComplexValueController(ComplexElementItem obj, EditType editType) throws DBCException {
            this.item = obj;
            this.editType = editType;

            if (obj instanceof CollectionElement.Item) {
                final CollectionElement.Item item = (CollectionElement.Item) obj;
                this.valueHandler = item.getValueHandler();
                this.type = item.getDataType();
                this.name = type.getTypeName() + "[" + item.getName() + "]";
                this.value = item.value;
            } else if (obj instanceof CompositeElement.Item) {
                final CompositeElement.Item item = (CompositeElement.Item) obj;
                this.valueHandler = item.getValueHandler();
                this.type = item.getDataType();
                this.name = item.attribute.getName();
                this.value = item.value;
            } else {
                throw new DBCException("Unsupported complex object element: " + this.item);
            }
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
            return parentController.isReadOnly() || ComplexObjectEditor.isReadOnlyType(item.getParent());
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

    private class StructContentProvider extends TreeContentProvider {
        @Override
        public Object[] getElements(Object parent) {
            return getChildren(parent);
        }

        @Override
        public ComplexElementItem[] getChildren(Object parent) {
            if (parent instanceof ComplexElementReferrer) {
                return getChildren(wrap(parent, ((ComplexElementReferrer) parent).getReferencedValue()));
            }
            if (parent instanceof ComplexElementItem) {
                return getChildren(((ComplexElementItem) parent).value);
            }
            if (parent instanceof ComplexElement) {
                return ((ComplexElement) parent).getChildren();
            }
            return new ComplexElementItem[0];
        }

        @Override
        public boolean hasChildren(Object parent) {
            return isComplexType(parent);
        }
    }

    @Nullable
    private Object wrap(@Nullable Object parent, @Nullable Object object) {
        if (cache.containsKey(object)) {
            return cache.get(object);
        }

        if (object instanceof Collection<?>) {
            final Collection<?> collection = (Collection<?>) object;
            final CollectionElement element;

            if (parent == null) {
                element = new CollectionRootElement(collection);
                element.items.add(new CollectionRootElement.Item(element, wrap(element, object)));
            } else {
                element = new CollectionElement(parent, collection);
                for (Object item : collection) {
                    element.items.add(new CollectionElement.Item(element, wrap(element, item)));
                }
            }

            cache.put(object, element);

            return element;
        }

        if (object instanceof DBDComposite) {
            final DBDComposite composite = (DBDComposite) object;
            final CompositeElement element = new CompositeElement(parent, composite);

            try {
                for (DBSAttributeBase attribute : composite.getAttributes()) {
                    element.items.add(new CompositeElement.Item(element, attribute, wrap(element, composite.getAttributeValue(attribute))));
                }
            } catch (DBCException e) {
                log.error("Error getting structure meta data", e);
            }

            cache.put(object, element);

            return element;
        }

        if (object instanceof DBDReference) {
            final DBDReference reference = (DBDReference) object;
            final ReferenceElement element = new ReferenceElement(parent, reference);

            cache.put(object, element);

            return element;
        }

        return object;
    }

    @NotNull
    public static Object unwrap(@NotNull Object object) {
        if (object instanceof ComplexElement) {
            return ((ComplexElement) object).extract(new VoidProgressMonitor());
        }

        if (object instanceof ComplexElementItem) {
            return unwrap(((ComplexElementItem) object).value);
        }

        return object;
    }

    private static boolean isComplexType(@NotNull Object object) {
        return object instanceof CollectionElement
            || object instanceof CollectionElement.Item && isComplexType(((CollectionElement.Item) object).value)
            || object instanceof CompositeElement
            || object instanceof CompositeElement.Item && isComplexType(((CompositeElement.Item) object).value)
            || object instanceof ReferenceElement;
    }

    private static boolean isReadOnlyType(@Nullable Object object) {
        if (object instanceof ReferenceElement) {
            return true;
        }
        if (object instanceof ComplexElementItem) {
            return isReadOnlyType(((ComplexElementItem) object).getParent());
        }
        if (object instanceof ComplexElement) {
            return isReadOnlyType(((ComplexElement) object).getParent());
        }
        return false;
    }

    private class PropsLabelProvider extends CellLabelProvider {
        private final boolean title;
        public PropsLabelProvider(boolean isTitle) {
            this.title = isTitle;
        }

        @Override
        public String getToolTipText(Object obj) {
            if (obj instanceof CompositeElement.Item) {
                return ((CompositeElement.Item) obj).attribute.getName() + " " + ((CompositeElement.Item) obj).attribute.getTypeName();
            }
            return null;
        }

        @Override
        public void update(ViewerCell cell)
        {
            ComplexElementItem element = (ComplexElementItem) cell.getElement();
            cell.setText(getColumnText(element, cell.getColumnIndex(), DBDDisplayFormat.UI));
            cell.setImage(getColumnImage(element, cell.getColumnIndex()));
            if (element.created) {
                cell.setBackground(backgroundAdded);
            } else if (element.modified) {
                cell.setBackground(backgroundModified);
            } else {
                cell.setBackground(null);
            }
            if (!title && isComplexType(element)) {
                cell.setForeground(foregroundReadOnly);
            }
        }

    }

    private class CopyAction extends Action {
        private final boolean isName;
        CopyAction(boolean isName) {
            super(NLS.bind(DataEditorsMessages.complex_object_editor_dialog_menu_copy_element, getTree().getColumn(isName ? 0 : 1).getText()));
            this.isName = isName;
        }

        @Override
        public void run()
        {
            final IStructuredSelection selection = getStructuredSelection();
            if (!selection.isEmpty()) {
                String text = getColumnText(
                    (ComplexElementItem) selection.getFirstElement(),
                    isName ? 0 : 1,
                    DBDDisplayFormat.NATIVE);
                if (text != null) {
                    UIUtils.setClipboardContents(getTree().getDisplay(), TextTransfer.getInstance(), text);
                }
            }
        }
    }

    private class SetToNullAction extends Action {
        public SetToNullAction() {
            super(DataEditorsMessages.complex_object_editor_dialog_menu_set_element_to_null);
        }

        @Override
        public void run() {
            final ITreeSelection selection = getStructuredSelection();
            if (selection.isEmpty()) {
                return;
            }
            try {
                final IValueController valueController = new ComplexValueController(
                    (ComplexElementItem) selection.getFirstElement(),
                    IValueController.EditType.NONE
                );
                valueController.updateValue(
                    BaseValueManager.makeNullValue(valueController),
                    true
                );
            } catch (DBCException e) {
                log.error("Error setting value attribute to NULL", e);
            }
        }
    }

    private class AddElementAction extends Action {
        AddElementAction() {
            super(DataEditorsMessages.complex_object_editor_dialog_menu_add_element, DBeaverIcons.getImageDescriptor(UIIcon.ROW_ADD));
        }

        @Override
        public void run() {
            disposeOldEditor();
            final CollectionElement collection = getElement();
            if (collection != null && collection.source instanceof DBDFixedSizeCollection) {
                try {
                    Object populatedCollection = ((DBDFixedSizeCollection) collection.source).populateCollection();
                    ComplexObjectEditor.this.setModel(executionContext, populatedCollection);
                } catch (DBCException e) {
                    log.error("Failed to populate the collection");
                }
                refresh();
            } else {
                final CollectionElement.Item item = new CollectionElement.Item(collection, null);

                collection.items.add(item);
                item.created = true;
                refresh();
                final TreeItem treeItem = (TreeItem) findItem(item);

                if (treeItem != null) {
                    showEditor(treeItem, false);
                }
            }
            autoUpdateComplexValue();
        }

        @Nullable
        private CollectionElement getElement() {
            final ComplexElementItem element = (ComplexElementItem) getStructuredSelection().getFirstElement();
            return GeneralUtils.adapt(element, CollectionElement.class);
        }

    }

    private class RemoveElementAction extends Action {
        RemoveElementAction() {
            super(DataEditorsMessages.complex_object_editor_dialog_menu_remove_element, DBeaverIcons.getImageDescriptor(UIIcon.ROW_DELETE));
        }

        @Override
        public void run() {
            disposeOldEditor();

            final CollectionElement.Item item = (CollectionElement.Item) getStructuredSelection().getFirstElement();
            final CollectionElement collection = item.collection;
            final int index = collection.items.indexOf(item);

            collection.items.remove(index);

            if (!collection.items.isEmpty()) {
                setSelection(new StructuredSelection(collection.items.get(CommonUtils.clamp(index, 0, collection.items.size() - 1))));
            }

            refresh();
            autoUpdateComplexValue();
        }
    }

    private class MoveElementAction extends Action {
        private final int direction;

        MoveElementAction(int dir) {
            super(dir == SWT.UP
                      ? DataEditorsMessages.complex_object_editor_dialog_menu_move_up_element
                      : DataEditorsMessages.complex_object_editor_dialog_menu_move_down_element,
                DBeaverIcons.getImageDescriptor(dir == SWT.UP ? UIIcon.ARROW_UP : UIIcon.ARROW_DOWN));
            this.direction = dir;
        }

        @Override
        public void run() {
            disposeOldEditor();

            final CollectionElement.Item item = (CollectionElement.Item) getStructuredSelection().getFirstElement();
            final CollectionElement collection = item.collection;
            final int index = collection.items.indexOf(item);

            if (direction == SWT.UP) {
                if (index == 0) {
                    return;
                }
                swap(collection, index, index - 1);
                setSelection(new StructuredSelection(collection.items.get(index - 1)));
            } else if (direction == SWT.DOWN) {
                if (index == collection.items.size() - 1) {
                    return;
                }
                swap(collection, index, index + 1);
                setSelection(new StructuredSelection(collection.items.get(index + 1)));
            }

            refresh();
            autoUpdateComplexValue();
        }

        private void swap(@NotNull CollectionElement collection, int firstIndex, int secondIndex) {
            final CollectionElement.Item temp = collection.items.get(firstIndex);
            collection.items.set(firstIndex, collection.items.get(secondIndex));
            collection.items.set(secondIndex, temp);
        }
    }

    public void contributeActions(IContributionManager manager) {
        manager.add(addElementAction);
        manager.add(removeElementAction);
        manager.add(new Separator());
        manager.add(moveElementUpAction);
        manager.add(moveElementDownAction);
    }

    private void updateActions() {
        final Object object = getStructuredSelection().getFirstElement();
        final boolean editable = !parentController.isReadOnly() && !isReadOnlyType(object);
        final boolean extendable = GeneralUtils.adapt(object, CollectionElement.class) != null;

        copyNameAction.setEnabled(object != null);
        copyValueAction.setEnabled(object != null);
        setToNullAction.setEnabled(editable);

        if (editable && object instanceof CollectionElement.Item) {
            final CollectionElement.Item item = (CollectionElement.Item) object;
            final CollectionElement collection = item.collection;
            final boolean child = !(collection instanceof CollectionRootElement);
            final int index = collection.items.indexOf(item);
            boolean isFixedSize = collection.source instanceof DBDFixedSizeCollection;
            if (isFixedSize) {
                setToNullAction.setEnabled(setToNullAction.isEnabled() && ((DBDFixedSizeCollection) collection.source).canSetElementsToNull());
            }
            addElementAction.setEnabled(extendable && (!isFixedSize | collection.source.isEmpty()));
            removeElementAction.setEnabled(child && !isFixedSize);
            moveElementUpAction.setEnabled(child && index > 0 && !isFixedSize);
            moveElementDownAction.setEnabled(child && index < collection.items.size() - 1 && !isFixedSize);
        } else {
            addElementAction.setEnabled(editable && extendable);
            removeElementAction.setEnabled(false);
            moveElementUpAction.setEnabled(false);
            moveElementDownAction.setEnabled(false);
        }
    }

    private abstract static class ComplexElementItem implements IAdaptable {
        protected boolean created;
        protected boolean modified;
        protected Object value;

        @NotNull
        public abstract String getName();

        @NotNull
        public abstract DBSTypedObject getDataType();

        @NotNull
        public abstract DBDValueHandler getValueHandler();

        @NotNull
        public abstract ComplexElement getParent();

        @Override
        public <T> T getAdapter(Class<T> adapter) {
            if (adapter.isInstance(value)) {
                return adapter.cast(value);
            } else {
                return null;
            }
        }
    }

    private interface ComplexElement {
        @NotNull
        Object extract(@NotNull DBRProgressMonitor monitor);

        @NotNull
        ComplexElementItem[] getChildren();

        @Nullable
        Object getParent();
    }

    private interface ComplexElementReferrer {
        @Nullable
        Object getReferencedValue();
    }

    private static class CollectionElement implements ComplexElement {
        private final Object parent;
        private final Collection<?> source;
        protected final List<Item> items;

        public CollectionElement(@Nullable Object parent, @NotNull Collection<?> source) {
            this.parent = parent;
            this.source = source;
            this.items = new ArrayList<>();
        }

        @NotNull
        @Override
        public Object extract(@NotNull DBRProgressMonitor monitor) {
            Collection<?> collection = source;

            if (collection instanceof DBDValueCloneable) {
                try {
                    collection = (DBDCollection) ((DBDValueCloneable) collection).cloneValue(monitor);
                } catch (DBException e) {
                    log.error("Error cloning collection value", e);
                }
            }

            final Stream<Object> values = items.stream().map(ComplexObjectEditor::unwrap);

            if (collection instanceof DBDCollection) {
                ((DBDCollection) collection).setContents(values.toArray());
            } else if (collection instanceof Set) {
                return values.collect(Collectors.toSet());
            } else {
                return values.collect(Collectors.toList());
            }

            return collection;
        }

        @NotNull
        @Override
        public ComplexElementItem[] getChildren() {
            return items.toArray(ComplexElementItem[]::new);
        }

        @Nullable
        @Override
        public Object getParent() {
            return parent;
        }

        private static class Item extends ComplexElementItem {
            private final CollectionElement collection;

            public Item(@NotNull CollectionElement collection, @Nullable Object value) {
                this.collection = collection;
                this.value = value;
            }

            @NotNull
            @Override
            public String getName() {
                return String.valueOf(collection.items.indexOf(this) + 1);
            }

            @NotNull
            @Override
            public DBSTypedObject getDataType() {
                if (collection.source instanceof DBDCollection) {
                    return ((DBDCollection) collection.source).getComponentType();
                } else {
                    return SimpleTypedObject.DEFAULT_TYPE;
                }
            }

            @NotNull
            @Override
            public DBDValueHandler getValueHandler() {
                if (collection.source instanceof DBDCollection) {
                    return ((DBDCollection) collection.source).getComponentValueHandler();
                } else {
                    return DefaultValueHandler.INSTANCE;
                }
            }

            @NotNull
            @Override
            public ComplexElement getParent() {
                return collection;
            }
        }
    }

    private static class CollectionRootElement extends CollectionElement {
        public CollectionRootElement(@NotNull Collection<?> source) {
            super(null, source);
        }

        @NotNull
        @Override
        public Object extract(@NotNull DBRProgressMonitor monitor) {
            return ((ComplexElement) items.get(0).value).extract(monitor);
        }

        private static class Item extends CollectionElement.Item {
            public Item(@NotNull CollectionElement collection, @Nullable Object value) {
                super(collection, value);
            }

            @NotNull
            @Override
            public String getName() {
                return DataEditorsMessages.complex_object_editor_root_element_name;
            }
        }
    }

    private static class CompositeElement implements ComplexElement {
        private final Object parent;
        private final DBDComposite source;
        private final List<Item> items;
        private final DBSDataType type;

        public CompositeElement(@Nullable Object parent, @NotNull DBDComposite source) {
            this.parent = parent;
            this.source = source;
            this.items = new ArrayList<>();
            this.type = source.getDataType();
        }

        @NotNull
        @Override
        public Object extract(@NotNull DBRProgressMonitor monitor) {
            DBDComposite composite = source;

            if (composite instanceof DBDValueCloneable) {
                try {
                    composite = (DBDComposite) ((DBDValueCloneable) composite).cloneValue(monitor);
                } catch (DBException e) {
                    log.error("Error cloning composite value", e);
                }
            }

            for (Item item : items) {
                try {
                    composite.setAttributeValue(item.attribute, unwrap(item.value));
                } catch (DBCException e) {
                    log.error("Error setting composite attribute value", e);
                }
            }

            return composite;
        }

        @NotNull
        @Override
        public ComplexElementItem[] getChildren() {
            return items.toArray(ComplexElementItem[]::new);
        }

        @Nullable
        @Override
        public Object getParent() {
            return parent;
        }

        private static class Item extends ComplexElementItem {
            private final CompositeElement composite;
            private final DBSAttributeBase attribute;

            public Item(@NotNull CompositeElement composite, @NotNull DBSAttributeBase attribute, @Nullable Object value) {
                this.composite = composite;
                this.attribute = attribute;
                this.value = value;
            }

            @NotNull
            @Override
            public String getName() {
                return attribute.getName();
            }

            @NotNull
            @Override
            public DBSTypedObject getDataType() {
                return attribute;
            }

            @NotNull
            @Override
            public DBDValueHandler getValueHandler() {
                return DBUtils.findValueHandler(composite.type.getDataSource(), attribute);
            }

            @NotNull
            @Override
            public ComplexElement getParent() {
                return composite;
            }
        }
    }

    private class ReferenceElement implements ComplexElement, ComplexElementReferrer {
        private final Object parent;
        private final DBDReference reference;
        private final Object value;

        public ReferenceElement(@Nullable Object parent, @NotNull DBDReference reference) {
            final DBRRunnableWithResult<Object> runnable = new DBRRunnableWithResult<>() {
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

            this.parent = parent;
            this.reference = reference;
            this.value = runnable.getResult();
        }

        @NotNull
        @Override
        public Object extract(@NotNull DBRProgressMonitor monitor) {
            return reference;
        }

        @NotNull
        @Override
        public ComplexElementItem[] getChildren() {
            return new ComplexElementItem[0];
        }

        @Nullable
        @Override
        public Object getParent() {
            return parent;
        }

        @Nullable
        @Override
        public Object getReferencedValue() {
            return value;
        }
    }
}
