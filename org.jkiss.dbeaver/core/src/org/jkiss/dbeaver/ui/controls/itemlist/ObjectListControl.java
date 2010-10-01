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
import org.jkiss.dbeaver.runtime.load.jobs.LoadingJob;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
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

    private boolean loadProperties;
    private TableViewer itemsViewer;
    private List<TableColumn> columns = new ArrayList<TableColumn>();
    private SortListener sortListener;
    private Map<Object, ItemRow> itemMap = new IdentityHashMap<Object, ItemRow>();
    private ISelectionProvider selectionProvider;
    private IDoubleClickListener doubleClickHandler;

    protected class ItemCell
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

        public Object getObject()
        {
            return getObjectValue(object);
        }
    }

    protected class ItemRow
    {
        final OBJECT_TYPE object;
        final List<ItemCell> props;

        ItemRow(OBJECT_TYPE object, List<ItemCell> props)
        {
            this.object = object;
            this.props = props;
        }
        Object getValue(int index)
        {
            return index >= props.size() ? null : props.get(index).value;
        }

        public Object getObject()
        {
            return getObjectValue(object);
        }
    }

    private LoadingJob<List<OBJECT_TYPE>> loadingJob;

    public ObjectListControl(
        Composite parent,
        int style,
        IWorkbenchPart workbenchPart)
    {
        super(parent, style, workbenchPart);
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

        itemsViewer.getControl().addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                if (loadingJob != null) {
                    loadingJob.cancel();
                    loadingJob = null;
                }
            }
        });
    }

    public TableViewer getItemsViewer()
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

    @Override
    public void dispose()
    {
        if (!itemsViewer.getControl().isDisposed()) {
            itemsViewer.getControl().dispose();
        }
        super.dispose();
    }

    protected void loadData(LoadingJob<List<OBJECT_TYPE>> job)
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
        for (TableColumn column : columns) {
            column.dispose();
        }
        columns.clear();

        itemsViewer.getTable().clearAll();
        itemsViewer.setItemCount(0);
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

    /**
     * Returns object with properties
     * @param item list item
     * @return object which will be exmined for properties
     */
    protected abstract Object getObjectValue(OBJECT_TYPE item);

    protected abstract String getObjectLabel(OBJECT_TYPE item);

    protected abstract Image getObjectImage(OBJECT_TYPE item);

    private class SortListener implements Listener
    {
        int sortDirection = SWT.DOWN;
        TableColumn prevColumn = null;

        public void handleEvent(Event e) {
            Collator collator = Collator.getInstance();
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
                    ItemRow row1 = itemMap.get(e1);
                    ItemRow row2 = itemMap.get(e2);
                    if (colIndex == 0) {
                        result = getObjectLabel(row1.object).compareToIgnoreCase(getObjectLabel(row2.object));
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
            ItemRow row = itemMap.get(element);
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
            ItemRow row = itemMap.get(element);
            if (columnIndex == 0) {
                return getObjectLabel(row.object);
            }
            ItemCell cell = getCellByIndex(row, columnIndex);
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

    protected class ObjectsLoadVisualizer extends ProgressVisualizer<List<OBJECT_TYPE>> {

        private List<ItemCell> lazyItems = new ArrayList<ItemCell>();

        public List<ItemCell> getLazyItems()
        {
            return lazyItems;
        }

        public void completeLoading(List<OBJECT_TYPE> items)
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

            ObjectListControl.this.columns.add(nameColumn);

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

            if (!table.isDisposed()) {
                table.setRedraw(false);

                itemsViewer.setInput(objectList);

                for (TableColumn column : columns) {
                    column.pack();
                }
                table.setRedraw(true);

                // Load all lazy items in one job
                if (!CommonUtils.isEmpty(lazyItems)) {
                    loadLazyItems(lazyItems);
                }
            }
        }

        protected void loadLazyItems(List<ItemCell> lazyItems)
        {
/*
            LoadingUtils.executeService(
                new ValueLoadService(),
                new ValuesLoadVisualizer());
*/
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

                        ObjectListControl.this.columns.add(propColumn);
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
    }

	class PaintListener implements Listener {
		public void handleEvent(Event event) {
            if (isDisposed()) {
                return;
            }
			switch(event.type) {
				case SWT.PaintItem: {
                    ItemRow row = itemMap.get(event.item.getData());
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
