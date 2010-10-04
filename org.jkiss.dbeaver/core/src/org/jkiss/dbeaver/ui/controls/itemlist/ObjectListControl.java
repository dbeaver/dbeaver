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
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.IPropertySource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.runtime.load.jobs.LoadingJob;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.views.properties.PropertyAnnoDescriptor;

import java.lang.reflect.InvocationTargetException;
import java.text.Collator;
import java.util.*;
import java.util.List;

/**
 * ObjectListControl
 */
public abstract class ObjectListControl<OBJECT_TYPE> extends ProgressPageControl implements IDoubleClickListener
{
    static final Log log = LogFactory.getLog(ObjectListControl.class);

    private final static Object LOADING_VALUE = new Object();

    private boolean isTree;
    private boolean isFitWidth;
    private boolean showName;
    private boolean loadProperties;

    private StructuredViewer itemsViewer;
    private List<Item> columns = new ArrayList<Item>();
    private SortListener sortListener;
    private Map<Object, ItemRow<OBJECT_TYPE>> itemMap = new IdentityHashMap<Object, ItemRow<OBJECT_TYPE>>();
    private ISelectionProvider selectionProvider;
    private IDoubleClickListener doubleClickHandler;
    private LoadingJob<Collection<OBJECT_TYPE>> loadingJob;

    public ObjectListControl(
        Composite parent,
        int style,
        IWorkbenchPart workbenchPart,
        IContentProvider contentProvider)
    {
        super(parent, style, workbenchPart);
        this.isTree = contentProvider instanceof ITreeContentProvider;
        this.isFitWidth = false;
        this.showName = true;
        this.loadProperties = true;

        if (isTree) {
            TreeViewer treeViewer = new TreeViewer(this, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
            final Tree tree = treeViewer.getTree();
            tree.setLinesVisible (true);
            tree.setHeaderVisible(true);
            itemsViewer = treeViewer;
            
        } else {
            TableViewer tableViewer = new TableViewer(this, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
            final Table table = tableViewer.getTable();
            table.setLinesVisible (true);
            table.setHeaderVisible(true);
            itemsViewer = tableViewer;
        }
        itemsViewer.setContentProvider(contentProvider);
        itemsViewer.setLabelProvider(new ItemLabelProvider());
        itemsViewer.addDoubleClickListener(this);
        itemsViewer.getControl().addListener(SWT.PaintItem, new PaintListener());

        GridData gd = new GridData(GridData.FILL_BOTH);
        itemsViewer.getControl().setLayoutData(gd);

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

        itemsViewer.getControl().addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                if (loadingJob != null) {
                    loadingJob.cancel();
                    loadingJob = null;
                }
            }
        });
    }

    protected boolean cancelProgress()
    {
        if (loadingJob != null) {
            loadingJob.cancel();
            return true;
        }
        return false;
    }

    private Tree getTree()
    {
        return ((TreeViewer)itemsViewer).getTree();
    }

    private Table getTable()
    {
        return ((TableViewer)itemsViewer).getTable();
    }

    public Viewer getItemsViewer()
    {
        return itemsViewer;
    }

    public ISelectionProvider getSelectionProvider()
    {
        return selectionProvider;
    }

    public boolean isLoadProperties() {
        return loadProperties;
    }

    public void setLoadProperties(boolean loadProperties) {
        this.loadProperties = loadProperties;
    }

    public boolean isShowName()
    {
        return showName;
    }

    public void setShowName(boolean showName)
    {
        this.showName = showName;
    }

    public boolean isFitWidth()
    {
        return isFitWidth;
    }

    public void setFitWidth(boolean fitWidth)
    {
        isFitWidth = fitWidth;
    }

    @Override
    public void dispose()
    {
        if (!itemsViewer.getControl().isDisposed()) {
            itemsViewer.getControl().dispose();
        }
        super.dispose();
    }

    protected void loadData(LoadingJob<Collection<OBJECT_TYPE>> job)
    {
        if (loadingJob != null) {
            // Don't do it twice
            return;
        }
        loadingJob = job;
        loadingJob.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
                loadingJob = null;
            }
        });
    }

    public void clearData()
    {
        for (Item column : columns) {
            column.dispose();
        }
        columns.clear();

        itemsViewer.setInput(Collections.<Object>emptyList());
        itemMap.clear();
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
        }
    }

    private ItemCell<OBJECT_TYPE> getCellByIndex(ItemRow<OBJECT_TYPE> row, int index)
    {
        Item column = columns.get(index);
        if (column.isDisposed()) {
            return null;
        }
        for (ItemCell<OBJECT_TYPE> cell : row.props) {
            if (cell.prop.getId().equals(column.getData())) {
                return cell;
            }
        }
        return null;
    }

    protected abstract DBPDataSource getDataSource();
    /**
     * Returns object with properties
     * @param item list item
     * @return object which will be examined for properties
     */
    protected abstract Object getObjectValue(OBJECT_TYPE item);

    protected abstract String getObjectLabel(OBJECT_TYPE item);

    protected abstract Image getObjectImage(OBJECT_TYPE item);

    protected void createColumn(String name, String toolTip, Object data)
    {
        Item newColumn;
        if (isTree) {
            TreeColumn column = new TreeColumn (getTree(), SWT.NONE);
            column.setText(name);
            column.setToolTipText(toolTip);
            column.setData(data);
            column.addListener(SWT.Selection, sortListener);
            newColumn = column;
        } else {
            TableColumn column = new TableColumn (getTable(), SWT.NONE);
            column.setText(name);
            column.setToolTipText(toolTip);
            column.setData(data);
            column.addListener(SWT.Selection, sortListener);
            newColumn = column;
        }
        this.columns.add(newColumn);
    }

    private class SortListener implements Listener
    {
        int sortDirection = SWT.DOWN;
        Item prevColumn = null;

        public void handleEvent(Event e) {
            Collator collator = Collator.getInstance();
            Item column = (Item)e.widget;
            final int colIndex = columns.indexOf(column);
            if (prevColumn == column) {
                // Set reverse order
                sortDirection = sortDirection == SWT.UP ? SWT.DOWN : SWT.UP;
            }
            prevColumn = column;
            if (isTree) {
                getTree().setSortColumn((TreeColumn) column);
                getTree().setSortDirection(sortDirection);
            } else {
                getTable().setSortColumn((TableColumn) column);
                getTable().setSortDirection(sortDirection);
            }

            itemsViewer.setSorter(new ViewerSorter(collator) {
                public int compare(Viewer viewer, Object e1, Object e2)
                {
                    int result;
                    ItemRow<OBJECT_TYPE> row1 = itemMap.get(e1);
                    ItemRow<OBJECT_TYPE> row2 = itemMap.get(e2);
                    if (showName && colIndex == 0) {
                        result = getObjectLabel(row1.object).compareToIgnoreCase(getObjectLabel(row2.object));
                    } else {
                        Object value1 = row1.getValue(showName ? colIndex - 1 : colIndex);
                        Object value2 = row2.getValue(showName ? colIndex - 1 : colIndex);
                        if (value1 == null && value2 == null) {
                            result = 0;
                        } else if (value1 == null) {
                            result = -1;
                        } else if (value2 == null) {
                            result = 1;
                        } else if (value1 instanceof Comparable) {
                            result = ((Comparable)value1).compareTo(value2);
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
            ItemRow<OBJECT_TYPE> row = itemMap.get(element);
            if (columnIndex == 0) {
                return getObjectImage(row.object);
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
            ItemRow<OBJECT_TYPE> row = itemMap.get(element);
            if (showName && columnIndex == 0) {
                return getObjectLabel(row.object);
            }
            ItemCell<OBJECT_TYPE> cell = getCellByIndex(row, columnIndex);
            if (cell == null || cell.value == null) {
                return "";
            }
            if (cell.value instanceof Boolean) {
                return "";
            }
            if (cell.value == LOADING_VALUE) {
                return "...";
            }
            return UIUtils.makeStringForUI(cell.value).toString();
        }

    }

    public class ObjectsLoadVisualizer extends ProgressVisualizer<Collection<OBJECT_TYPE>> {

        private List<ItemCell<OBJECT_TYPE>> lazyItems = new ArrayList<ItemCell<OBJECT_TYPE>>();

        public void completeLoading(Collection<OBJECT_TYPE> items)
        {
            super.completeLoading(items);

            Control itemsControl = itemsViewer.getControl();
            if (itemsControl.isDisposed()) {
                return;
            }
            if (showName) {
                createColumn("Name", "Name", null);
            }

            List<OBJECT_TYPE> objectList = new ArrayList<OBJECT_TYPE>();
            if (!CommonUtils.isEmpty(items)) {
                for (OBJECT_TYPE item : items) {
                    addRow(item);
                    objectList.add(item);
                }
            }
            if (itemMap.isEmpty()) {
                setInfo("No items");
            } else {
                setInfo(itemMap.size() + " items");
            }

            if (!itemsControl.isDisposed()) {
                itemsViewer.setInput(objectList);

                if (isTree) {
                    UIUtils.packColumns(getTree(), isFitWidth);
                } else {
                    UIUtils.packColumns(getTable());
                }

                // Load all lazy items in one job
                if (!CommonUtils.isEmpty(lazyItems)) {
                    LoadingUtils.executeService(
                        new ValueLoadService(lazyItems),
                        new ValuesLoadVisualizer());
                }
            }
        }

        private void addRow(OBJECT_TYPE item)
        {
            Object itemObject = getObjectValue(item);

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

            List<ItemCell<OBJECT_TYPE>> cells = new ArrayList<ItemCell<OBJECT_TYPE>>();
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
                    Item propColumn = null;
                    for (Item column : columns) {
                        if (descriptor.getId().equals(column.getData())) {
                            propColumn = column;
                            break;
                        }
                    }
                    if (propColumn == null) {
                        createColumn(descriptor.getDisplayName(), descriptor.getDescription(), descriptor.getId());
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
                    ItemCell<OBJECT_TYPE> cell = new ItemCell<OBJECT_TYPE>(item, descriptor, value);
                    cells.add(cell);
                    if (descriptor.isLazy()) {
                        lazyItems.add(cell);
                    }
                }
            }
            ItemRow<OBJECT_TYPE> row = new ItemRow<OBJECT_TYPE>(item, cells);
            itemMap.put(item, row);
            //rows.add(row);

            if (itemMap.size() % 10 == 0) {
                setInfo(itemMap.size() + " items");
            }
        }
    }

    private class ValueLoadService extends DatabaseLoadService<Object> {
        private Collection<ItemCell<OBJECT_TYPE>> lazyItems;
        public ValueLoadService(Collection<ItemCell<OBJECT_TYPE>> lazyItems)
        {
            super("Load item values", getDataSource());
            this.lazyItems = lazyItems;
        }

        public Object evaluate()
            throws InvocationTargetException, InterruptedException
        {
            for (ItemCell<OBJECT_TYPE> item : lazyItems) {
                // Check control is disposed
                if (isDisposed() || getProgressMonitor().isCanceled()) {
                    break;
                }
                // Extract value with progress monitor
                try {
                    item.value = item.prop.readValue(getObjectValue(item.object), getProgressMonitor());
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

    private class ValuesLoadVisualizer extends ProgressVisualizer<Object> {

        public void visualizeLoading()
        {
            super.visualizeLoading();
            if (!getItemsViewer().getControl().isDisposed()) {
                getItemsViewer().refresh();
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
                    ItemRow<OBJECT_TYPE> row = itemMap.get(event.item.getData());
                    ItemCell<OBJECT_TYPE> cell = row == null ? null : getCellByIndex(row, event.index);
                    if (cell != null && cell.value instanceof Boolean) {
                        if (((Boolean)cell.value)) {
                            int columnWidth = UIUtils.getColumnWidth(columns.get(event.index));
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

    private static class ItemCell<OBJECT_TYPE>
    {
        final OBJECT_TYPE object;
        final PropertyAnnoDescriptor prop;
        Object value;

        ItemCell(OBJECT_TYPE object, PropertyAnnoDescriptor prop, Object value)
        {
            this.object = object;
            this.prop = prop;
            this.value = value;
        }

    }

    private static class ItemRow<OBJECT_TYPE>
    {
        final OBJECT_TYPE object;
        final List<ItemCell<OBJECT_TYPE>> props;

        ItemRow(OBJECT_TYPE object, List<ItemCell<OBJECT_TYPE>> props)
        {
            this.object = object;
            this.props = props;
        }
        Object getValue(int index)
        {
            return index >= props.size() ? null : props.get(index).value;
        }
    }

}
