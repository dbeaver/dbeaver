/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.itemlist;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.IPropertySource;
import org.jkiss.dbeaver.ext.ui.IMetaModelView;
import org.jkiss.dbeaver.model.meta.DBMModel;
import org.jkiss.dbeaver.model.meta.DBMNode;
import org.jkiss.dbeaver.model.meta.DBMTreeFolder;
import org.jkiss.dbeaver.model.meta.DBMTreeNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.tree.DBXTreeNode;
import org.jkiss.dbeaver.runtime.load.AbstractLoadService;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.views.properties.PropertyAnnoDescriptor;
import org.jkiss.dbeaver.utils.ViewUtils;

import java.lang.reflect.InvocationTargetException;
import java.text.Collator;
import java.util.*;
import java.util.List;

/**
 * ItemListControl
 */
public class ItemListControl extends ProgressPageControl implements IMetaModelView, IDoubleClickListener
{
    static final Log log = LogFactory.getLog(ItemListControl.class);

    private final static Object LOADING_VALUE = new Object();

    private DBMNode node;

    private TableViewer itemsViewer;
    private List<TableColumn> columns = new ArrayList<TableColumn>();
    private SortListener sortListener;
    private Map<DBSObject, ItemRow> itemMap = new IdentityHashMap<DBSObject, ItemRow>();
    private ISelectionProvider selectionProvider;
    private IDoubleClickListener doubleClickHandler;

    private int loadCount = 0;

    public ItemListControl(
        Composite parent,
        int style,
        IWorkbenchPart workbenchPart,
        DBMNode node)
    {
        super(parent, style, workbenchPart);
        this.node = node;

        this.setLayout(new GridLayout(1, true));

        itemsViewer = new TableViewer(this, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
        final Table table = itemsViewer.getTable();
        table.setLinesVisible (true);
        table.setHeaderVisible(true);
        //table.addListener(SWT.MeasureItem, paintListener);
        table.addListener(SWT.PaintItem, new PaintListener());
        GridData gd = new GridData(GridData.FILL_BOTH);
        table.setLayoutData(gd);
        itemsViewer.setContentProvider(new IStructuredContentProvider()
        {
            public void dispose()
            {
            }
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
            {
            }
            public Object[] getElements(Object inputElement)
            {
                if (inputElement instanceof List) {
                    return ((List)inputElement).toArray();
                }
                return null;
            }
        });
        itemsViewer.setLabelProvider(new ItemLabelProvider());
        itemsViewer.addDoubleClickListener(this);

        super.createProgressPanel(this);

        sortListener = new SortListener();

        TableColumn nameColumn = new TableColumn (table, SWT.NONE);
        nameColumn.setText("Name");
        nameColumn.setToolTipText("Name");
        nameColumn.addListener(SWT.Selection, sortListener);

        this.columns.add(nameColumn);

        // Make selection provider
        selectionProvider = new ISelectionProvider()
        {
            public void addSelectionChangedListener(ISelectionChangedListener listener)
            {
                itemsViewer.addSelectionChangedListener(listener);
            }

            public ISelection getSelection()
            {
                return itemsViewer.getSelection();
            }

            public void removeSelectionChangedListener(ISelectionChangedListener listener)
            {
                itemsViewer.removeSelectionChangedListener(listener);
            }

            public void setSelection(ISelection selection)
            {
                itemsViewer.setSelection(selection);
            }
        };
    }

    @Override
    public void dispose()
    {
        itemsViewer.getControl().dispose();
        super.dispose();
    }

    public void clearData()
    {
        for (TableColumn column : columns) {
            column.dispose();
        }
        columns.clear();

        itemsViewer.getTable().clearAll();
        itemsViewer.setItemCount(0);
        itemMap.clear();
    }

    public void fillData()
    {
        this.fillData(null);
    }

    public void fillData(DBXTreeNode metaNode)
    {
        LoadingUtils.executeService(
            new ItemLoadService(metaNode),
            new ItemsLoadVisualizer());
    }

    public ISelectionProvider getSelectionProvider()
    {
        return selectionProvider;
    }

    public DBMNode getNode()
    {
        return node;
    }

    public DBMModel getMetaModel()
    {
        return node.getModel();
    }

    public TableViewer getViewer()
    {
        return itemsViewer;
    }

    public IWorkbenchPart getWorkbenchPart()
    {
        return workbenchPart;
    }

    public IDoubleClickListener getDoubleClickHandler()
    {
        return doubleClickHandler;
    }

    public void setDoubleClickHandler(IDoubleClickListener doubleClickHandler)
    {
        this.doubleClickHandler = doubleClickHandler;
    }

    public void doubleClick(DoubleClickEvent event)
    {
        if (doubleClickHandler != null) {
            // USe provided double click
            doubleClickHandler.doubleClick(event);
        } else {
            // Run default node action
            DBMNode dbmNode = ViewUtils.getSelectedNode(this);
            if (dbmNode == null) {
                return;
            }
            ViewUtils.runAction(dbmNode.getDefaultAction(), workbenchPart, itemsViewer.getSelection());
        }
    }

    private ItemCell getCellByIndex(ItemRow row, int index)
    {
        TableColumn column = columns.get(index);
        for (ItemCell cell : row.props) {
            if (cell.prop.getId().equals(column.getData())) {
                return cell;
            }
        }
        return null;
    }

    private class SortListener implements Listener
    {
        int sortDirection = SWT.DOWN;
        TableColumn prevColumn = null;

        public void handleEvent(Event e) {
            Collator collator = Collator.getInstance(Locale.getDefault());
            TableColumn column = (TableColumn)e.widget;
            final int colIndex = columns.indexOf(column);
            if (prevColumn == column) {
                // Set reverse order
                sortDirection = sortDirection == SWT.UP ? SWT.DOWN : SWT.UP;
            }
            prevColumn = column;
            itemsViewer.getTable().setSortColumn(column);
            itemsViewer.getTable().setSortDirection(sortDirection);

            itemsViewer.setSorter(new ViewerSorter(collator) {
                public int compare(Viewer viewer, Object e1, Object e2)
                {
                    int result;
                    ItemRow row1 = itemMap.get(DBSObject.class.cast(e1));
                    ItemRow row2 = itemMap.get(DBSObject.class.cast(e2));
                    if (colIndex == 0) {
                        result = row1.object.getNodeName().compareToIgnoreCase(row2.object.getNodeName());
                    } else {
                        Object value1 = row1.getValue(colIndex - 1);
                        Object value2 = row2.getValue(colIndex - 1);
                        if (value1 == null && value2 == null) {
                            result = 0;
                        } else if (value1 == null) {
                            result = -1;
                        } else if (value2 == null) {
                            result = 1;
                        } else {
                            result = value1.toString().compareToIgnoreCase(value2.toString());
                        }
                    }
                    return sortDirection == SWT.DOWN ? result : -result;
                }
            });
        }
    }

    /**
     * ItemLabelProvider
     */
    class ItemLabelProvider extends LabelProvider implements ITableLabelProvider
    {
        public Image getColumnImage(Object element, int columnIndex)
        {
            ItemRow row = itemMap.get(DBSObject.class.cast(element));
            if (columnIndex == 0) {
                return row.object.getNodeIconDefault();
            }
/*
            ItemCell cell = getCellByIndex(row, columnIndex);
            if (cell.value instanceof ILoadService) {
                return rotateImages[loadCount % 4];
            }
*/
            return null;
        }
    
        public String getColumnText(Object element, int columnIndex)
        {
            ItemRow row = itemMap.get(DBSObject.class.cast(element));
            if (columnIndex == 0) {
                return row.object.getNodeName();
            }
            ItemCell cell = getCellByIndex(row, columnIndex);
            if (cell.value == null) {
                return "";
            }
            if (cell.value instanceof Boolean) {
                return "";
            }
            if (cell.value instanceof DBSObject) {
                return ((DBSObject)cell.value).getName();
            }
            if (cell.value == LOADING_VALUE) {
                return "...";
            }
            return UIUtils.makeStringForUI(cell.value).toString();
        }

    }

    private class ItemLoadService extends AbstractLoadService<List<DBMNode>> {

        private DBXTreeNode metaNode;

        protected ItemLoadService(DBXTreeNode metaNode)
        {
            super("Loading items");
            this.metaNode = metaNode;
        }

        public List<DBMNode> evaluate()
            throws InvocationTargetException, InterruptedException
        {
            try {
                List<DBMNode> items = new ArrayList<DBMNode>();
                List<? extends DBMNode> children = node.getChildren(getProgressMonitor());
                if (CommonUtils.isEmpty(children)) {
                    return items;
                }
                for (DBMNode item : children) {
                    if (item instanceof DBMTreeFolder) {
                        continue;
                    }
                    if (metaNode != null) {
                        if (!(item instanceof DBMTreeNode)) {
                            continue;
                        }
                        if (((DBMTreeNode)item).getMeta() != metaNode) {
                            continue;
                        }
                    }
                    items.add(item);
                }
                return items;
            } catch (Throwable ex) {
                if (ex instanceof InvocationTargetException) {
                    throw (InvocationTargetException)ex;
                } else {
                    throw new InvocationTargetException(ex);
                }
            }
        }
    }

    private class ItemsLoadVisualizer extends ProgressVisualizer<List<DBMNode>> {

        private List<ItemCell> lazyItems = new ArrayList<ItemCell>();

        public void completeLoading(List<DBMNode> items)
        {
            super.completeLoading(items);

            List<DBSObject> objectList = new ArrayList<DBSObject>();
            if (!CommonUtils.isEmpty(items)) {
                for (DBMNode item : items) {
                    addRow(item);
                    objectList.add(item.getObject());
                }
            }
            if (itemMap.isEmpty()) {
                setInfo("No items");
            } else {
                setInfo(itemMap.size() + " items");
            }

            Table table = itemsViewer.getTable();
            if (!table.isDisposed()) {
                table.setRedraw(false);

                itemsViewer.setInput(objectList);

                for (TableColumn column : columns) {
                    column.pack();
                }
                table.setRedraw(true);

                // Load all lazy items in one job
                if (!CommonUtils.isEmpty(lazyItems)) {
                    LoadingUtils.executeService(
                        new ValueLoadService(),
                        new ValuesLoadVisualizer());
                }
            }
        }

        private DBSObject addRow(DBMNode item)
        {
            DBSObject itemObject = item.getObject();
            List<PropertyAnnoDescriptor> annoProps = null;
            if (itemObject instanceof IAdaptable) {
                IPropertySource propertySource = (IPropertySource)((IAdaptable)itemObject).getAdapter(IPropertySource.class);
                if (propertySource != null) {
                    annoProps = PropertyAnnoDescriptor.extractProperties(propertySource);
                }
            }
            if (annoProps == null) {
                annoProps = PropertyAnnoDescriptor.extractAnnotations(itemObject);
            }

            List<ItemCell> cells = new ArrayList<ItemCell>();
            for (PropertyAnnoDescriptor descriptor : annoProps) {
                // Check control is disposed
                if (isDisposed()) {
                    break;
                }
                // Skip unviewable items
                if (!descriptor.isViewable()) {
                    continue;
                }
                // Add coulmn if nexxessary
                TableColumn propColumn = null;
                for (TableColumn column : columns) {
                    if (descriptor.getId().equals(column.getData())) {
                        propColumn = column;
                        break;
                    }
                }
                if (propColumn == null) {
                    propColumn = new TableColumn(itemsViewer.getTable(), SWT.NONE);
                    propColumn.setText(descriptor.getDisplayName());
                    propColumn.setToolTipText(descriptor.getDescription());
                    propColumn.setData(descriptor.getId());
                    propColumn.addListener(SWT.Selection, sortListener);

                    ItemListControl.this.columns.add(propColumn);
                }
                // Read property value
                Object value;
                if (descriptor.isLazy()) {
                    value = LOADING_VALUE;
                } else {
                    try {
                        value = descriptor.readValue(itemObject, null);
                    }
                    catch (IllegalAccessException e) {
                        log.error(e);
                        continue;
                    }
                    catch (InvocationTargetException e) {
                        log.error(e.getTargetException());
                        continue;
                    }
                }
                ItemCell cell = new ItemCell(item, descriptor, value);
                cells.add(cell);
                if (descriptor.isLazy()) {
                    lazyItems.add(cell);
                }
            }
            ItemRow row = new ItemRow(item, cells);
            itemMap.put(item.getObject(), row);
            //rows.add(row);

            if (itemMap.size() % 10 == 0) {
                setInfo(itemMap.size() + " items");
            }

            return item.getObject();
        }

        private class ValueLoadService extends AbstractLoadService<Object> {
            public ValueLoadService()
            {
                super("Load item values");
            }

            public Object evaluate()
                throws InvocationTargetException, InterruptedException
            {
                for (ItemCell item : lazyItems) {
                    // Check control is disposed
                    if (isDisposed()) {
                        break;
                    }
                    // Extract value with progress monitor
                    try {
                        item.value = item.prop.readValue(item.node.getObject(), getProgressMonitor());
                    }
                    catch (InvocationTargetException e) {
                        log.warn(e.getTargetException());
                        item.value = "???";
                    }
                    catch (IllegalAccessException e) {
                        log.warn(e);
                        item.value = "???";
                    }
                }
                return null;
            }
        }
    }

    private class ValuesLoadVisualizer extends ProgressVisualizer<Object> {

        public void visualizeLoading()
        {
            super.visualizeLoading();
            if (!itemsViewer.getTable().isDisposed()) {
                itemsViewer.refresh();
            }
        }
    }

	class PaintListener implements Listener {
		public void handleEvent(Event event) {
			switch(event.type) {
				case SWT.PaintItem: {
                    ItemRow row = itemMap.get(DBSObject.class.cast(event.item.getData()));
                    ItemCell cell = row == null ? null : getCellByIndex(row, event.index);
                    if (cell != null && cell.value instanceof Boolean) {
                        if (((Boolean)cell.value)) {
                            int columnWidth = columns.get(event.index).getWidth();
                            Image image = DBIcon.CHECK.getImage();
                            event.gc.drawImage(image, event.x + (columnWidth - image.getBounds().width) / 2, event.y);
                            event.doit = false;
                        }
                    }
					break;
				}
			}
		}
	};
}
