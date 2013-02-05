/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.dialogs.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithResult;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.UIUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

/**
 * Structure object editor
 */
public class ComplexObjectEditor extends TreeViewer {

    static final Log log = LogFactory.getLog(ComplexObjectEditor.class);

    private DBPDataSource dataSource;

    public ComplexObjectEditor(Composite parent, int style)
    {
        super(parent, style | SWT.SINGLE | SWT.FULL_SELECTION);

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

        super.setContentProvider(new StructContentProvider());
    }

    public void setModel(DBPDataSource dataSource, final DBDComplexType value)
    {
        getTree().setRedraw(false);
        try {
            this.dataSource = dataSource;
            setInput(value);
            expandToLevel(2);
        } finally {
            getTree().setRedraw(true);
        }
    }

    private static class FieldInfo {
        final DBSAttributeBase attribute;
        final Object value;
        DBDValueHandler valueHandler;

        private FieldInfo(DBPDataSource dataSource, DBSAttributeBase attribute, Object value)
        {
            this.attribute = attribute;
            this.value = value;
            this.valueHandler = DBUtils.findValueHandler(dataSource, attribute);
        }
    }

    private static class ArrayItem {
        final DBDArray array;
        final int index;
        final Object value;
        DBDValueHandler valueHandler;

        private ArrayItem(DBDArray array, int index, Object value)
        {
            this.array = array;
            this.index = index;
            this.value = value;
            this.valueHandler = DBUtils.findValueHandler(array.getObjectDataType().getDataSource(), array.getObjectDataType());
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
                    Collection<? extends DBSAttributeBase> attributes = structure.getAttributes();
                    Object[] children = new Object[attributes.size()];
                    int index = 0;
                    for (DBSAttributeBase attr : attributes) {
                        Object value = structure.getAttributeValue(attr);
                        children[index++] = new FieldInfo(structure.getObjectDataType().getDataSource(), attr, value);
                    }
                    return children;
                } catch (DBException e) {
                    log.error("Error getting structure meta data", e);
                }
            } else if (parent instanceof DBDArray) {
                DBDArray array = (DBDArray)parent;
                try {
                    Object[] contents = array.getContents();
                    ArrayItem[] items = new ArrayItem[contents.length];
                    for (int i = 0; i < contents.length; i++) {
                        items[i] = new ArrayItem(array, i, contents[i]);
                    }
                    return items;
                } catch (DBCException e) {
                    log.error("Error getting array content", e);
                }
            } else if (parent instanceof DBDReference) {
                final DBDReference reference = (DBDReference)parent;
                DBRRunnableWithResult<Object> runnable = new DBRRunnableWithResult<Object>() {
                    @Override
                    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                    {
                        DBCExecutionContext context = dataSource.openContext(monitor, DBCExecutionPurpose.UTIL, "Read reference value");
                        try {
                            result = reference.getReferencedObject(context);
                        } catch (DBCException e) {
                            throw new InvocationTargetException(e);
                        } finally {
                            context.close();
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
                if (value instanceof DBDComplexType) {
                    return getChildren(value);
                }
            } else if (parent instanceof ArrayItem) {
                Object value = ((ArrayItem) parent).value;
                if (value instanceof DBDComplexType) {
                    return getChildren(value);
                }
            }
            return new Object[0];
        }

        @Override
        public boolean hasChildren(Object parent)
        {
            return
                parent instanceof DBDStructure ||
                parent instanceof DBDArray ||
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
                return getValueText(item.valueHandler, item.array.getObjectDataType(), item.value);
            }
            return String.valueOf(columnIndex);
        }

        private String getValueText(DBDValueHandler valueHandler, DBSTypedObject type, Object value)
        {
            if (value instanceof DBDArray) {
                try {
                    Object[] contents = ((DBDArray) value).getContents();
                    return "[" + ((DBDArray) value).getObjectDataType().getName() + " - " + String.valueOf(contents == null ? 0 : contents.length) + "]";
                } catch (DBCException e) {
                    log.error(e);
                    return "N/A";
                }
            }
            if (value instanceof DBDComplexType) {
                return "[" + ((DBDComplexType) value).getObjectDataType().getName() + "]";
            }
            return valueHandler.getValueDisplayString(type, value, DBDDisplayFormat.UI);
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
