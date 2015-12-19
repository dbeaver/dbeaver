/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.dialogs.data;

import org.eclipse.jface.action.IContributionManager;
import org.jkiss.dbeaver.Log;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithResult;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.dbeaver.ui.data.IValueEditorStandalone;
import org.jkiss.dbeaver.ui.data.IValueManager;
import org.jkiss.dbeaver.ui.data.registry.DataManagerRegistry;

import java.lang.reflect.InvocationTargetException;

/**
 * Structure object editor
 */
public class ComplexObjectEditor extends TreeViewer {

    static final Log log = Log.getLog(ComplexObjectEditor.class);

    private IWorkbenchPartSite partSite;
    private DBCExecutionContext executionContext;
    private final TreeEditor treeEditor;
    private IValueEditor curCellEditor;

    public ComplexObjectEditor(IWorkbenchPartSite partSite, Composite parent, int style)
    {
        super(parent, style | SWT.SINGLE | SWT.FULL_SELECTION);

        this.partSite = partSite;
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

        TreeViewerColumn column = new TreeViewerColumn(this, SWT.NONE);
        column.getColumn().setWidth(200);
        column.getColumn().setMoveable(true);
        column.getColumn().setText(CoreMessages.ui_properties_name);
        column.setLabelProvider(new PropsLabelProvider(true));


        column = new TreeViewerColumn(this, SWT.NONE);
        column.getColumn().setWidth(120);
        column.getColumn().setMoveable(true);
        column.getColumn().setText(CoreMessages.ui_properties_value);
        column.setLabelProvider(new PropsLabelProvider(false));

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
                    showEditor(item, true);
                }
            }

            @Override
            public void mouseUp(MouseEvent e)
            {
/*
                TreeItem item = treeControl.getItem(new Point(e.x, e.y));
                if (item != null) {
                    if (UIUtils.getColumnAtPos(item, e.x, e.y) == 1) {
                        showEditor(item, false);
                    }
                }
*/
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
                    showEditor(selection[0], false);
                    e.doit = false;
                    e.detail = SWT.TRAVERSE_NONE;
                }
            }
        });

        super.setContentProvider(new StructContentProvider());
    }

    public void setModel(DBCExecutionContext executionContext, final DBDComplexValue value)
    {
        getTree().setRedraw(false);
        try {
            this.executionContext = executionContext;
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

        DBDValueHandler valueHandler = null;
        DBSTypedObject type = null;
        String name = "Unknown";
        Object value = null;
        Object obj = item.getData();
        if (obj instanceof FieldInfo) {
            FieldInfo field = (FieldInfo) obj;
            valueHandler = field.valueHandler;
            type = field.attribute;
            name = field.attribute.getName();
            value = field.value;
        } else if (obj instanceof ArrayItem) {
            ArrayItem arrayItem = (ArrayItem) obj;
            valueHandler = arrayItem.valueHandler;
            type = arrayItem.array.getComponentType();
            name = type.getTypeName() + "["  + arrayItem.index + "]";
            value = arrayItem.value;
        } else {
            //
        }
        if (valueHandler == null) {
            return;
        }
        IValueController valueController = new ComplexValueController(
            valueHandler,
            type,
            name,
            value,
            advanced ? IValueController.EditType.EDITOR : IValueController.EditType.INLINE);
        try {
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
            UIUtils.showErrorDialog(getControl().getShell(), "Cell editor", "Can't open cell editor", e);
        }
    }

    private void disposeOldEditor()
    {
        curCellEditor = null;
        Control oldEditor = treeEditor.getEditor();
        if (oldEditor != null) oldEditor.dispose();
    }

    private static class FieldInfo {
        final DBSAttributeBase attribute;
        final Object value;
        DBDValueHandler valueHandler;

        private FieldInfo(DBPDataSource dataSource, DBSAttributeBase attribute, @Nullable Object value)
        {
            this.attribute = attribute;
            this.value = value;
            this.valueHandler = DBUtils.findValueHandler(dataSource, attribute);
        }
    }

    private static class ArrayItem {
        final DBDCollection array;
        final int index;
        final Object value;
        DBDValueHandler valueHandler;

        private ArrayItem(DBDCollection array, int index, Object value)
        {
            this.array = array;
            this.index = index;
            this.value = value;
            this.valueHandler = DBUtils.findValueHandler(array.getComponentType().getDataSource(), array.getComponentType());
        }
    }

    private class ComplexValueController implements IValueController {
        private final DBDValueHandler valueHandler;
        private final DBSTypedObject type;
        private final String name;
        private final Object value;
        private final EditType editType;
        public ComplexValueController(DBDValueHandler valueHandler, DBSTypedObject type, String name, @Nullable Object value, EditType editType)
        {
            this.valueHandler = valueHandler;
            this.type = type;
            this.name = name;
            this.value = value;
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
        public void updateValue(Object value)
        {
            // Do nothing
        }

        @Override
        public DBDValueHandler getValueHandler()
        {
            return valueHandler;
        }

        @Override
        public IValueManager getValueManager() {
            DBSTypedObject valueType = getValueType();
            return DataManagerRegistry.findValueManager(
                getExecutionContext().getDataSource().getContainer(),
                valueType.getDataKind(),
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
            return true;
        }

        @Override
        public IWorkbenchPartSite getValueSite()
        {
            return partSite;
        }

        @Override
        public Composite getEditPlaceholder()
        {
            return getTree();
        }

        @Override
        public IContributionManager getEditBar()
        {
            return null;
        }

        @Override
        public void showMessage(String message, boolean error)
        {

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
        public Object[] getChildren(Object parent)
        {
            if (parent instanceof DBDStructure) {
                DBDStructure structure = (DBDStructure)parent;
                try {
                    DBSAttributeBase[] attributes = structure.getAttributes();
                    Object[] children = new Object[attributes.length];
                    for (int i = 0; i < attributes.length; i++) {
                        DBSAttributeBase attr = attributes[i];
                        Object value = structure.getAttributeValue(attr);
                        children[i] = new FieldInfo(structure.getDataType().getDataSource(), attr, value);
                    }
                    return children;
                } catch (DBException e) {
                    log.error("Error getting structure meta data", e);
                }
            } else if (parent instanceof DBDCollection) {
                DBDCollection array = (DBDCollection)parent;
                ArrayItem[] items = new ArrayItem[array.getItemCount()];
                for (int i = 0; i < items.length; i++) {
                    items[i] = new ArrayItem(array, i, array.getItem(i));
                }
                return items;
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
                DBeaverUI.runInUI(DBeaverUI.getActiveWorkbenchWindow(), runnable);
/*
                try {
                    DBeaverUI.runInProgressService(runnable);
                } catch (InvocationTargetException e) {
                    UIUtils.showErrorDialog(getControl().getShell(), "Value read", "Error read reference", e.getTargetException());
                } catch (InterruptedException e) {
                    // ok
                }
*/
                return getChildren(runnable.getResult());
            } else if (parent instanceof FieldInfo) {
                Object value = ((FieldInfo) parent).value;
                if (isComplexType(value)) {
                    return getChildren(value);
                }
            } else if (parent instanceof ArrayItem) {
                Object value = ((ArrayItem) parent).value;
                if (isComplexType(value)) {
                    return getChildren(value);
                }
            }
            return new Object[0];
        }

        private boolean isComplexType(Object value) {
            return value instanceof DBDComplexValue;
        }

        @Override
        public boolean hasChildren(Object parent)
        {
            return
                parent instanceof DBDStructure ||
                parent instanceof DBDCollection ||
                parent instanceof DBDReference ||
                (parent instanceof FieldInfo && hasChildren(((FieldInfo) parent).value)) ||
                (parent instanceof ArrayItem && hasChildren(((ArrayItem) parent).value));
        }
    }

    private class PropsLabelProvider extends CellLabelProvider
    {
        private final boolean isName;
        public PropsLabelProvider(boolean isName)
        {
            this.isName = isName;
        }

        public String getText(Object obj, int columnIndex)
        {
            if (obj instanceof FieldInfo) {
                FieldInfo field = (FieldInfo) obj;
                if (isName) {
                    return field.attribute.getName();
                }
                return getValueText(field.valueHandler, field.attribute, field.value);
            } else if (obj instanceof ArrayItem) {
                ArrayItem item = (ArrayItem) obj;
                if (isName) {
                    return String.valueOf(item.index);
                }
                return getValueText(item.valueHandler, item.array.getComponentType(), item.value);
            }
            return String.valueOf(columnIndex);
        }

        private String getValueText(@NotNull DBDValueHandler valueHandler, @NotNull DBSTypedObject type, @Nullable Object value)
        {
            if (value instanceof DBDCollection) {
                return "[" + ((DBDCollection) value).getComponentType().getName() + " - " + ((DBDCollection) value).getItemCount() + "]";
            } else if (value instanceof DBDStructure) {
                return "[" + ((DBDStructure) value).getDataType().getName() + "]";
            } else if (value instanceof DBDReference) {
                return "--> [" + ((DBDReference) value).getReferencedType().getName() + "]";
            } else {
                return valueHandler.getValueDisplayString(type, value, DBDDisplayFormat.UI);
            }
        }

        @Override
        public String getToolTipText(Object obj)
        {
            if (obj instanceof FieldInfo) {
                return ((FieldInfo) obj).attribute.getName() + " " + ((FieldInfo) obj).attribute.getTypeName();
            }
            return "";
        }

        @Override
        public void update(ViewerCell cell)
        {
            Object element = cell.getElement();
            cell.setText(getText(element, cell.getColumnIndex()));
        }

    }

}
