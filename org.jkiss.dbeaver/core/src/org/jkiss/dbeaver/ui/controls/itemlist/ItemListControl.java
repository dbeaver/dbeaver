/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.itemlist;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.IPropertySource;
import org.jkiss.dbeaver.ext.ui.INavigatorModelView;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNTreeFolder;
import org.jkiss.dbeaver.model.navigator.DBNTreeNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.tree.DBXTreeNode;
import org.jkiss.dbeaver.runtime.load.AbstractLoadService;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.runtime.load.jobs.LoadingJob;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
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
public class ItemListControl extends ProgressPageControl implements INavigatorModelView, IDoubleClickListener
{
    static final Log log = LogFactory.getLog(ItemListControl.class);

    private final static Object LOADING_VALUE = new Object();

    private DBNNode node;
    private boolean loadProperties;
    private TableViewer itemsViewer;
    private List<TableColumn> columns = new ArrayList<TableColumn>();
    private SortListener sortListener;
    private Map<DBNNode, ItemRow> itemMap = new IdentityHashMap<DBNNode, ItemRow>();
    private ISelectionProvider selectionProvider;
    private IDoubleClickListener doubleClickHandler;

    private LoadingJob<List<DBNNode>> loadingJob;

    public ItemListControl(
        Composite parent,
        int style,
        IWorkbenchPart workbenchPart,
        DBNNode node)
    {
        super(parent, style, workbenchPart);
        this.node = node;
        this.loadProperties = true;

        this.setLayout(new GridLayout(1, true));

        itemsViewer = new TableViewer(this, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
        final Table table = itemsViewer.getTable();
        table.setLinesVisible (true);
        table.setHeaderVisible(true);
        //table.addListener(SWT.MeasureItem, paintListener);
        table.addListener(SWT.PaintItem, new PaintListener());
        GridData gd = new GridData(GridData.FILL_BOTH);
        table.setLayoutData(gd);
        itemsViewer.setContentProvider(new ListContentProvider());
        itemsViewer.setLabelProvider(new ItemLabelProvider());
        itemsViewer.addDoubleClickListener(this);

        super.createProgressPanel();

        sortListener = new SortListener();

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

        ViewUtils.addContextMenu(this);

        itemsViewer.getControl().addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                if (loadingJob != null) {
                    loadingJob.cancel();
                    loadingJob = null;
                }
            }
        });
    }

    public boolean isLoadProperties() {
        return loadProperties;
    }

    public void setLoadProperties(boolean loadProperties) {
        this.loadProperties = loadProperties;
    }

    @Override
    public void dispose()
    {
        if (!itemsViewer.getControl().isDisposed()) {
            itemsViewer.getControl().dispose();
        }
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
        if (loadingJob != null) {
            // Don't do it twice
            return;
        }
        loadingJob = LoadingUtils.executeService(
            new ItemLoadService(metaNode),
            new ItemsLoadVisualizer());
        loadingJob.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
                loadingJob = null;
            }
        });
    }

    public ISelectionProvider getSelectionProvider()
    {
        return selectionProvider;
    }

    public DBNNode getRootNode() {
        return node;
    }

    public TableViewer getNavigatorViewer()
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
            DBNNode dbmNode = ViewUtils.getSelectedNode(this);
            if (dbmNode == null) {
                return;
            }
            ViewUtils.runAction(dbmNode.getDefaultAction(), workbenchPart, itemsViewer.getSelection());
        }
    }

    private ItemCell getCellByIndex(ItemRow row, int index)
    {
        TableColumn column = columns.get(index);
        if (column.isDisposed()) {
            return null;
        }
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
                    ItemRow row1 = itemMap.get(DBNNode.class.cast(e1));
                    ItemRow row2 = itemMap.get(DBNNode.class.cast(e2));
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
            ItemRow row = itemMap.get(DBNNode.class.cast(element));
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
            ItemRow row = itemMap.get(DBNNode.class.cast(element));
            if (columnIndex == 0) {
                return row.object.getNodeName();
            }
            ItemCell cell = getCellByIndex(row, columnIndex);
            if (cell == null || cell.value == null) {
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

    private class ItemLoadService extends AbstractLoadService<List<DBNNode>> {

        private DBXTreeNode metaNode;

        protected ItemLoadService(DBXTreeNode metaNode)
        {
            super("Loading items");
            this.metaNode = metaNode;
        }

        public List<DBNNode> evaluate()
            throws InvocationTargetException, InterruptedException
        {
            try {
                List<DBNNode> items = new ArrayList<DBNNode>();
                List<? extends DBNNode> children = node.getChildren(getProgressMonitor());
                if (CommonUtils.isEmpty(children)) {
                    return items;
                }
                for (DBNNode item : children) {
                    if (getProgressMonitor().isCanceled()) {
                        break;
                    }
                    if (item instanceof DBNTreeFolder) {
                        continue;
                    }
                    if (metaNode != null) {
                        if (!(item instanceof DBNTreeNode)) {
                            continue;
                        }
                        if (((DBNTreeNode)item).getMeta() != metaNode) {
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

    private class ItemsLoadVisualizer extends ProgressVisualizer<List<DBNNode>> {

        private List<ItemCell> lazyItems = new ArrayList<ItemCell>();

        public void completeLoading(List<DBNNode> items)
        {
            super.completeLoading(items);

            Table table = itemsViewer.getTable();
            if (table.isDisposed()) {
                return;
            }
            TableColumn nameColumn = new TableColumn (table, SWT.NONE);
            nameColumn.setText("Name");
            nameColumn.setToolTipText("Name");
            nameColumn.addListener(SWT.Selection, sortListener);

            ItemListControl.this.columns.add(nameColumn);

            List<DBNNode> objectList = new ArrayList<DBNNode>();
            if (!CommonUtils.isEmpty(items)) {
                for (DBNNode item : items) {
                    addRow(item);
                    objectList.add(item);
                }
            }
            if (itemMap.isEmpty()) {
                setInfo("No items");
            } else {
                setInfo(itemMap.size() + " items");
            }

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

        private void addRow(DBNNode item)
        {
            DBSObject itemObject = item.getObject();

            List<PropertyAnnoDescriptor> annoProps = null;
            if (loadProperties) {
                if (itemObject instanceof IAdaptable) {
                    IPropertySource propertySource = (IPropertySource)((IAdaptable)itemObject).getAdapter(IPropertySource.class);
                    if (propertySource != null) {
                        annoProps = PropertyAnnoDescriptor.extractProperties(propertySource);
                    }
                }
                if (annoProps == null) {
                    annoProps = PropertyAnnoDescriptor.extractAnnotations(itemObject);
                }
            }

            List<ItemCell> cells = new ArrayList<ItemCell>();
            if (annoProps != null) {
                for (PropertyAnnoDescriptor descriptor : annoProps) {
                    // Check control is disposed
                    if (isDisposed()) {
                        break;
                    }
                    // Skip unviewable items
                    if (!descriptor.isViewable()) {
                        continue;
                    }
                    // Add column if necessary
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
            }
            ItemRow row = new ItemRow(item, cells);
            itemMap.put(item, row);
            //rows.add(row);

            if (itemMap.size() % 10 == 0) {
                setInfo(itemMap.size() + " items");
            }
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
                    if (isDisposed() || getProgressMonitor().isCanceled()) {
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
            if (isDisposed()) {
                return;
            }
			switch(event.type) {
				case SWT.PaintItem: {
                    ItemRow row = itemMap.get(DBNNode.class.cast(event.item.getData()));
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
	}

}
