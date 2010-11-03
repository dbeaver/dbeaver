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
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.IPropertySource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.runtime.load.jobs.LoadingJob;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.views.properties.PropertyAnnoDescriptor;

import javax.imageio.spi.ServiceRegistry;
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

    //private final static Object LOADING_VALUE = new Object();
    private final static String LOADING_LABEL = "...";

    private static class ObjectColumn {
        Item item;
        Class<?> objectClass;
        PropertyAnnoDescriptor prop;

        private ObjectColumn(Item item, Class<?> objectClass, PropertyAnnoDescriptor prop) {
            this.item = item;
            this.objectClass = objectClass;
            this.prop = prop;
        }
    }

    private boolean isTree;
    private boolean isFitWidth;
    //private boolean showName;
    //private boolean loadProperties;

    private StructuredViewer itemsViewer;
    private List<ObjectColumn> columns = new ArrayList<ObjectColumn>();
    private SortListener sortListener;
    private IDoubleClickListener doubleClickHandler;
    private LoadingJob<Collection<OBJECT_TYPE>> loadingJob;

    private final TextLayout linkLayout;
    private final Color linkColor;
    private final Cursor linkCursor;

    // Current selection coordinates
    private transient int selectedItem = -1;
    private transient int selectedColumn = -1;
    // Sample flag. True only when initial content is packed. Used to provide actual cell data to Tree/Table pack() methods
    // After content is loaded is always false (and all hyperlink cells have empty text)
    private transient boolean sampleItems = false;

    public ObjectListControl(
        Composite parent,
        int style,
        IWorkbenchPart workbenchPart,
        IContentProvider contentProvider)
    {
        super(parent, style, workbenchPart);
        this.isTree = (contentProvider instanceof ITreeContentProvider);
        this.isFitWidth = false;
        this.linkLayout = new TextLayout(parent.getDisplay());
        this.linkColor = parent.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION);
        this.linkCursor = parent.getDisplay().getSystemCursor(SWT.CURSOR_HAND);

        if (isTree) {
            TreeViewer treeViewer = new TreeViewer(this, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
            final Tree tree = treeViewer.getTree();
            tree.setLinesVisible (true);
            tree.setHeaderVisible(true);
            itemsViewer = treeViewer;
            //TreeEditor editor = new TreeEditor(tree);
            
        } else {
            TableViewer tableViewer = new TableViewer(this, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
            final Table table = tableViewer.getTable();
            table.setLinesVisible (true);
            table.setHeaderVisible(true);
            itemsViewer = tableViewer;
            //TableEditor editor = new TableEditor(table);
        }
        itemsViewer.setContentProvider(contentProvider);
        itemsViewer.setLabelProvider(new ItemLabelProvider());
        itemsViewer.addDoubleClickListener(this);
        itemsViewer.getControl().addListener(SWT.PaintItem, new PaintListener());
        CellTrackListener mouseListener = new CellTrackListener();
        itemsViewer.getControl().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e)
            {
                if (isTree) {
                    detectTreeItem(e.x, e.y);
                } else {
                    detectTableItem(e.x, e.y);
                }
            }
            @Override
            public void mouseUp(MouseEvent e)
            {
                Item hoverItem;
                if (isTree) {
                    hoverItem = detectTreeItem(e.x, e.y);
                } else {
                    hoverItem = detectTableItem(e.x, e.y);
                }
                if (hoverItem != null && selectedColumn >= 0) {
                    Object element = hoverItem.getData();
                    int checkColumn = selectedColumn;
                    Object cellValue = getCellValue(element, checkColumn);
                    if (isHyperlink(cellValue) && getCellLinkBounds(hoverItem, checkColumn, cellValue).contains(e.x, e.y)) {
                        navigateHyperlink(cellValue);
                    }
                }
            }
        });

        itemsViewer.getControl().addMouseTrackListener(mouseListener);
        itemsViewer.getControl().addMouseMoveListener(mouseListener);

        GridData gd = new GridData(GridData.FILL_BOTH);
        itemsViewer.getControl().setLayoutData(gd);

        super.createProgressPanel();

        sortListener = new SortListener();

        itemsViewer.getControl().addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                if (loadingJob != null) {
                    loadingJob.cancel();
                    loadingJob = null;
                }
            }
        });
    }

    private TableItem detectTableItem(int x, int y)
    {
        selectedItem = -1;
        selectedColumn = -1;
        Point pt = new Point(x, y);
        TableItem item = getTable().getItem(pt);
        if (item == null) return null;
        int columnCount = getTable().getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            Rectangle rect = item.getBounds(i);
            if (rect.contains(pt)) {
                selectedItem = getTable().indexOf(item);
                selectedColumn = i;
                break;
            }
        }
        return item;
    }

    private TreeItem detectTreeItem(int x, int y)
    {
        selectedItem = -1;
        selectedColumn = -1;
        Point pt = new Point(x, y);
        TreeItem item = getTree().getItem(pt);
        if (item == null) return null;
        int columnCount = getTree().getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            Rectangle rect = item.getBounds(i);
            if (rect.contains(pt)) {
                selectedItem = getTree().indexOf(item);
                selectedColumn = i;
                break;
            }
        }
        return item;
    }

    protected String getSelectedText()
    {
        if (selectedItem == -1 || selectedColumn == -1) {
            return null;
        }
        if (isTree) {
            if (selectedItem >= getTree().getItemCount()) {
                return null;
            }
            return getTree().getItem(selectedItem).getText(selectedColumn);
        } else {
            if (selectedItem >= getTable().getItemCount()) {
                return null;
            }
            return getTable().getItem(selectedItem).getText(selectedColumn);
        }
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
        return itemsViewer;
    }

    public boolean isFitWidth()
    {
        return isFitWidth;
    }

    public void setFitWidth(boolean fitWidth)
    {
        isFitWidth = fitWidth;
    }

    protected boolean isHyperlink(Object cellValue)
    {
        return false;
    }

    protected void navigateHyperlink(Object cellValue)
    {
        // do nothing. by defalt all cells are not navigable
    }

    @Override
    public void dispose()
    {
        UIUtils.dispose(linkLayout);
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
        for (ObjectColumn column : columns) {
            column.item.dispose();
        }
        columns.clear();

        itemsViewer.setInput(Collections.<Object>emptyList());
        //itemMap.clear();
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

    private Object getCellValue(Object object, int columnIndex)
    {
        ObjectColumn column = columns.get(columnIndex);
        if (column.item.isDisposed()) {
            return null;
        }
        if (column.prop.isLazy()) {
            return LOADING_LABEL;
        }
        try {
            return column.prop.readValue(getObjectValue((OBJECT_TYPE) object), null);
        }
        catch (IllegalAccessException e) {
            log.error(e);
            return null;
        }
        catch (InvocationTargetException e) {
            log.error(e.getTargetException());
            return null;
        }
    }

    private Rectangle getCellLinkBounds(Item item, int column, Object cellValue) {
        prepareLinkStyle(cellValue, linkColor);

        Rectangle itemBounds;
        if (isTree) {
            itemBounds = ((TreeItem)item).getBounds(column);
        } else {
            itemBounds = ((TableItem)item).getBounds(column);
        }

        Rectangle linkBounds = linkLayout.getBounds();
        linkBounds.x += itemBounds.x;
        linkBounds.y += itemBounds.y + 1;
        linkBounds.height -= 2;

        return linkBounds;
    }


    protected abstract DBPDataSource getDataSource();
    /**
     * Returns object with properties
     * @param item list item
     * @return object which will be examined for properties
     */
    protected abstract Object getObjectValue(OBJECT_TYPE item);

    protected abstract Image getObjectImage(OBJECT_TYPE item);

    protected void createColumn(Class<?> objctClass, PropertyAnnoDescriptor prop)
    {
        Item newColumn;
        if (isTree) {
            TreeColumn column = new TreeColumn (getTree(), SWT.NONE);
            column.setText(prop.getDisplayName());
            column.setToolTipText(prop.getDescription());
            column.setData(prop);
            column.addListener(SWT.Selection, sortListener);
            newColumn = column;
        } else {
            TableColumn column = new TableColumn (getTable(), SWT.NONE);
            column.setText(prop.getDisplayName());
            column.setToolTipText(prop.getDescription());
            column.setData(prop);
            column.addListener(SWT.Selection, sortListener);
            newColumn = column;
        }
        this.columns.add(new ObjectColumn(newColumn, objctClass, prop));
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
                    Object value1 = getCellValue(e1, colIndex);
                    Object value2 = getCellValue(e2, colIndex);
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
            if (columnIndex == 0) {
                return getObjectImage((OBJECT_TYPE) element);
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
            Object cellValue = getCellValue(element, columnIndex);
            if (cellValue == null) {
                return "";
            }
            if (cellValue instanceof Boolean) {
                return "";
            }
            if (!sampleItems && isHyperlink(cellValue)) {
                return "";
            }
            return getCellString(cellValue);
        }

    }

    public class ObjectsLoadVisualizer extends ProgressVisualizer<Collection<OBJECT_TYPE>> {

        //private List<ItemCell<OBJECT_TYPE>> lazyItems = new ArrayList<ItemCell<OBJECT_TYPE>>();

        public void completeLoading(Collection<OBJECT_TYPE> items)
        {
            super.completeLoading(items);

            Control itemsControl = itemsViewer.getControl();
            if (itemsControl.isDisposed()) {
                return;
            }
            List<OBJECT_TYPE> objectList = new ArrayList<OBJECT_TYPE>();
            if (!CommonUtils.isEmpty(items)) {
                // Create columns
                List<Class<?>> classList = new ArrayList<Class<?>>();
                for (OBJECT_TYPE item : items) {
                    Object object = getObjectValue(item);
                    if (!classList.contains(object.getClass())) {
                        classList.add(object.getClass());
                    }
                    objectList.add(item);
                }

                for (Class<?> objectClass : classList) {
                    List<PropertyAnnoDescriptor> props = PropertyAnnoDescriptor.extractAnnotations(objectClass);
                    for (PropertyAnnoDescriptor prop : props) {
                        createColumn(objectClass, prop);
                    }
                }
            }
            if (objectList.isEmpty()) {
                setInfo("No items");
            } else {
                setInfo(objectList.size() + " items");
            }

            if (!itemsControl.isDisposed()) {
                // Pack columns
                sampleItems = true;
                try {
                    List<OBJECT_TYPE> sampleList;
                    if (objectList.size() > 200) {
                        sampleList = objectList.subList(0, 100);
                    } else {
                        sampleList = objectList;
                    }
                    itemsViewer.setInput(sampleList);

                    if (isTree) {
                        UIUtils.packColumns(getTree(), isFitWidth);
                    } else {
                        UIUtils.packColumns(getTable());
                    }
                } finally {
                    sampleItems = false;
                }
                itemsViewer.setInput(objectList);

/*
                // Load all lazy items in one job
                if (!CommonUtils.isEmpty(lazyItems)) {
                    LoadingUtils.executeService(
                        new ValueLoadService(lazyItems),
                        new ValuesLoadVisualizer());
                }
*/
            }
        }

    }

/*
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
            super.visualizeLoading();if (!getItemsViewer().getControl().isDisposed()) {
                getItemsViewer().refresh();
            }
        }
    }
*/

	class PaintListener implements Listener {

		public void handleEvent(Event event) {
            if (isDisposed()) {
                return;
            }
			switch(event.type) {
				case SWT.PaintItem: {
                    Object cellValue = getCellValue(event.item.getData(), event.index);
                    if (cellValue != null ) {
                        GC gc = event.gc;
                        if (cellValue instanceof Boolean) {
                            if (((Boolean)cellValue)) {
                                int columnWidth = UIUtils.getColumnWidth(columns.get(event.index).item);
                                Image image = DBIcon.CHECK.getImage();
                                gc.drawImage(image, event.x + (columnWidth - image.getBounds().width) / 2, event.y);
                                event.doit = false;
                            }
                        } else if (isHyperlink(cellValue)) {
                            boolean isSelected = linkColor.equals(gc.getBackground());
                            // Print link
                            prepareLinkStyle(cellValue, isSelected ? gc.getForeground() : linkColor);
                            linkLayout.draw(gc, event.x, event.y + 1);
                        }
                    }
					break;
				}
			}
		}
	}

    class CellTrackListener implements MouseTrackListener, MouseMoveListener {

        public void mouseEnter(MouseEvent e)
        {
        }

        public void mouseExit(MouseEvent e)
        {
            resetCursor();
        }

        public void mouseHover(MouseEvent e)
        {
        }

        private void resetCursor()
        {
            getItemsViewer().getControl().setCursor(null);
        }

        public void mouseMove(MouseEvent e)
        {
            Item hoverItem;
            if (isTree) {
                hoverItem = detectTreeItem(e.x, e.y);
            } else {
                hoverItem = detectTableItem(e.x, e.y);
            }
            if (hoverItem == null || selectedColumn < 0) {
                resetCursor();
            } else {
                Object element = hoverItem.getData();

                int checkColumn = selectedColumn;
                Object cellValue = getCellValue(element, checkColumn);
                if (cellValue == null) {
                    resetCursor();
                } else {
                    if (isHyperlink(cellValue) && getCellLinkBounds(hoverItem, checkColumn, cellValue).contains(e.x, e.y)) {
                        getItemsViewer().getControl().setCursor(linkCursor);
                    } else {
                        resetCursor();
                    }
                }
            }
        }
    }

    private void prepareLinkStyle(Object cellValue, Color foreground)
    {
        // Print link
        TextStyle linkStyle = new TextStyle(
            getFont(),
            foreground,
            null);
        linkStyle.underline = true;
        linkStyle.underlineStyle = SWT.UNDERLINE_LINK;

        String text = getCellString(cellValue);
        linkLayout.setText(text);
        linkLayout.setIndent(3);
        linkLayout.setStyle(linkStyle, 0, text.length());
    }

    private static String getCellString(Object value)
    {
        if (value instanceof DBPNamedObject) {
            value = ((DBPNamedObject)value).getName();
        }
        return UIUtils.makeStringForUI(value).toString();
    }
}
