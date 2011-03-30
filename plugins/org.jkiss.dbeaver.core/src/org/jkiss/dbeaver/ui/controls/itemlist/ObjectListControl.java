/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.itemlist;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.AbstractJob;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.runtime.load.jobs.LoadingJob;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.views.properties.ObjectAttributeDescriptor;
import org.jkiss.dbeaver.ui.views.properties.ObjectPropertyDescriptor;

import java.lang.reflect.InvocationTargetException;
import java.text.Collator;
import java.util.*;
import java.util.List;

/**
 * ObjectListControl
 */
public abstract class ObjectListControl<OBJECT_TYPE> extends ProgressPageControl
{
    static final Log log = LogFactory.getLog(ObjectListControl.class);

    private final static String LOADING_LABEL = "...";
    private final static String DATA_OBJECT_COLUMN = "objectColumn";
    private final static int LAZY_LOAD_DELAY = 100;

    private IWorkbenchPart workbenchPart;
    private boolean isTree;
    private boolean isFitWidth;
    private boolean isBrief;
    //private boolean showName;
    //private boolean loadProperties;

    private ColumnViewer itemsViewer;
    private List<ObjectColumn> columns = new ArrayList<ObjectColumn>();
    private SortListener sortListener;
    private IDoubleClickListener doubleClickHandler;
    private Map<Item, LazyObject> lazyObjects;
    private ListPropertySource listPropertySource;
    private Job lazyLoadingJob = null;

    private final TextLayout linkLayout;
    private final Color linkColor;
    private final Cursor linkCursor;
    private final Cursor arrowCursor;

    // Current selection coordinates
    private transient int selectedItem = -1;
    private transient int selectedColumn = -1;
    // Sample flag. True only when initial content is packed. Used to provide actual cell data to Tree/Table pack() methods
    // After content is loaded is always false (and all hyperlink cells have empty text)
    private transient boolean sampleItems = false;

    private volatile Object curListObject;
    private volatile LoadingJob<Collection<OBJECT_TYPE>> loadingJob;

    public ObjectListControl(
        Composite parent,
        int style,
        IWorkbenchPart workbenchPart,
        IContentProvider contentProvider)
    {
        super(parent, style);
        this.workbenchPart = workbenchPart;
        this.isTree = (contentProvider instanceof ITreeContentProvider);
        this.isFitWidth = false;
        this.linkLayout = new TextLayout(parent.getDisplay());
        this.linkColor = parent.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION);
        this.linkCursor = parent.getDisplay().getSystemCursor(SWT.CURSOR_HAND);
        this.arrowCursor = parent.getDisplay().getSystemCursor(SWT.CURSOR_ARROW);

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
            //UIUtils.applyCustomTolTips(table);
            //TableEditor editor = new TableEditor(table);
        }
        itemsViewer.getControl().setCursor(arrowCursor);
        itemsViewer.setContentProvider(contentProvider);
        itemsViewer.setLabelProvider(new ItemLabelProvider());
        itemsViewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event)
            {
                if (doubleClickHandler != null) {
                    // Uee provided double click
                    doubleClickHandler.doubleClick(event);
                }
            }
        });
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
                if (hoverItem != null && selectedColumn >= 0 && e.button == 1) {
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
        itemsViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event)
            {
                IStructuredSelection selection = (IStructuredSelection)event.getSelection();
                if (selection.isEmpty()) {
                    curListObject = null;
                } else {
                    curListObject = selection.getFirstElement();
                }
            }
        });
        GridData gd = new GridData(GridData.FILL_BOTH);
        itemsViewer.getControl().setLayoutData(gd);

        super.createProgressPanel();

        listPropertySource = new ListPropertySource();
        sortListener = new SortListener();

        // Add selection listener
        itemsViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event)
            {
                String status;
                IStructuredSelection selection = (IStructuredSelection)itemsViewer.getSelection();
                if (selection.isEmpty()) {
                    status = "";
                } else if (selection.size() == 1) {
                    Object selectedNode = selection.getFirstElement();
                    status = getCellString(selectedNode);
                } else {
                    status = String.valueOf(selection.size()) + " objects";
                }
                setInfo(status);
            }
        });
    }

    public IWorkbenchPart getWorkbenchPart()
    {
        return workbenchPart;
    }

    protected abstract LoadingJob<Collection<OBJECT_TYPE>> createLoadService();

    private TableItem detectTableItem(int x, int y)
    {
        selectedItem = -1;
        selectedColumn = -1;
        Point pt = new Point(x, y);
        TableItem item = getTable().getItem(pt);
        if (item == null) return null;
        selectedColumn = UIUtils.getColumnAtPos(getTable(), item, x, y);
        selectedItem = getTable().indexOf(item);
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

    public ColumnViewer getItemsViewer()
    {
        return itemsViewer;
    }

    public ISelectionProvider getSelectionProvider()
    {
        return itemsViewer;
    }

    public void setFitWidth(boolean fitWidth)
    {
        isFitWidth = fitWidth;
    }

    public void setBrief(boolean brief)
    {
        isBrief = brief;
    }

    protected boolean isHyperlink(Object cellValue)
    {
        return false;
    }

    protected void navigateHyperlink(Object cellValue)
    {
        // do nothing. by default all cells are not navigable
    }

    @Override
    public void dispose()
    {
        if (loadingJob != null) {
            // Cancel running job
            loadingJob.cancel();
            loadingJob = null;
        }
        UIUtils.dispose(linkLayout);
        super.dispose();
    }

    public void loadData()
    {
        if (loadingJob != null) {
            return;
        }
        loadingJob = createLoadService();
        loadingJob.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event)
            {
                loadingJob = null;
            }
        });
        loadingJob.schedule(LAZY_LOAD_DELAY);
    }

    public void clearData()
    {
        for (ObjectColumn column : columns) {
            column.item.dispose();
        }
        columns.clear();
        if (!itemsViewer.getControl().isDisposed()) {
            itemsViewer.setInput(Collections.<Object>emptyList());
        }
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

    private synchronized void addLazyObject(Item item, ObjectColumn column)
    {
        if (lazyObjects == null) {
            lazyObjects = new TreeMap<Item, LazyObject>(new Comparator<Item>() {
                public int compare(Item o1, Item o2)
                {
                    int index1, index2;
                    if (isTree) {
                        index1 = getTree().indexOf((TreeItem) o1);
                        index2 = getTree().indexOf((TreeItem) o2);
                    } else {
                        index1 = getTable().indexOf((TableItem) o1);
                        index2 = getTable().indexOf((TableItem) o2);
                    }
                    return index1 - index2;
                }
            });
        }
        LazyObject lazyObject = lazyObjects.get(item);
        if (lazyObject == null) {
            lazyObject = new LazyObject(item.getData());
            lazyObjects.put(item, lazyObject);
            //System.out.println("LAZY: " + object);
        }
        if (!lazyObject.columns.contains(column)) {
            lazyObject.columns.add(column);
        }
        if (lazyLoadingJob == null) {
            lazyLoadingJob = new LazyLoaderJob();
            lazyLoadingJob.addJobChangeListener(new JobChangeAdapter() {
                @Override
                public void done(IJobChangeEvent event)
                {
                    synchronized (ObjectListControl.this) {
                        if (lazyObjects == null || lazyObjects.isEmpty()) {
                            lazyLoadingJob = null;
                        } else {
                            lazyLoadingJob.schedule(LAZY_LOAD_DELAY);
                        }
                    }
                }
            });
            lazyLoadingJob.schedule(LAZY_LOAD_DELAY);
        }
    }

    private synchronized Map<Item, LazyObject> obtainLazyObjects()
    {
        if (lazyObjects == null) {
            return null;
        }
        Map<Item, LazyObject> tmp = lazyObjects;
        lazyObjects = null;
        return tmp;
    }

    private Object getCellValue(Object object, int columnIndex)
    {
        if (columnIndex >= columns.size()) {
            return null;
        }
        ObjectColumn column = columns.get(columnIndex);
        if (column.item.isDisposed()) {
            return null;
        }
        Object objectValue = getObjectValue((OBJECT_TYPE) object);
        if (objectValue == null) {
            return null;
        }
        ObjectPropertyDescriptor prop = column.propMap.get(objectValue.getClass());
        if (prop == null) {
            return null;
        }
        if (prop.isLazy(objectValue, true)) {
            return LOADING_LABEL;
        }
        try {
            return prop.readValue(objectValue, null);
        }
        catch (InvocationTargetException e) {
            log.error(e.getTargetException());
            return null;
        }
        catch (Exception e) {
            log.error(e);
            return null;
        }
    }

    private Rectangle getCellLinkBounds(Item item, int column, Object cellValue) {
        prepareLinkStyle(cellValue, linkColor);

        Rectangle itemBounds;
        if (isTree) {
            itemBounds = ((TreeItem)item).getTextBounds(column);
        } else {
            itemBounds = ((TableItem)item).getTextBounds(column);
        }

        Rectangle linkBounds = linkLayout.getBounds();
        linkBounds.x += itemBounds.x;
        linkBounds.y += itemBounds.y + 1;
        linkBounds.height -= 2;

        return linkBounds;
    }

    protected Class<?>[] getListBaseTypes()
    {
        return null;
    }

    /**
     * Returns object with properties
     * @param item list item
     * @return object which will be examined for properties
     */
    protected Object getObjectValue(OBJECT_TYPE item)
    {
        return item;
    }

    /**
     * Returns object's image
     * @return image or null
     */
    protected Image getObjectImage(OBJECT_TYPE item)
    {
        return null;
    }

    protected void createColumn(Class<?> objectClass, ObjectPropertyDescriptor prop)
    {
        ObjectColumn objectColumn = null;
        for (ObjectColumn col : columns) {
            if (col.id.equals(prop.getId())) {
                objectColumn = col;
                break;
            }
        }
        if (objectColumn == null) {
            Item newColumn;
            if (isTree) {
                TreeColumn column = new TreeColumn (getTree(), SWT.NONE);
                column.setText(prop.getDisplayName());
                column.setToolTipText(prop.getDescription());
                //column.setData(prop);
                column.addListener(SWT.Selection, sortListener);
                newColumn = column;
            } else {
                TableColumn column = new TableColumn (getTable(), SWT.NONE);
                column.setText(prop.getDisplayName());
                column.setToolTipText(prop.getDescription());
                //column.setData(prop);
                column.addListener(SWT.Selection, sortListener);
                newColumn = column;
            }
            objectColumn = new ObjectColumn(newColumn, CommonUtils.toString(prop.getId()));
            objectColumn.addProperty(objectClass, prop);
            this.columns.add(objectColumn);
            newColumn.setData(DATA_OBJECT_COLUMN, objectColumn);
        } else {
            objectColumn.addProperty(objectClass, prop);
            String oldTitle = objectColumn.item.getText();
            if (!oldTitle.contains(prop.getDisplayName())) {
                objectColumn.item.setText(CommonUtils.capitalizeWord(objectColumn.id));
            }
        }
    }

    //////////////////////////////////////////////////////
    // Property source implementation

    private class ListPropertySource implements IPropertySource {

        public Object getEditableValue()
        {
            return curListObject;
        }

        public IPropertyDescriptor[] getPropertyDescriptors()
        {
            Set<IPropertyDescriptor> props = new LinkedHashSet<IPropertyDescriptor>();
            for (ObjectColumn column : columns) {
                props.addAll(column.propMap.values());
            }
            return props.toArray(new IPropertyDescriptor[props.size()]);
        }

        public Object getPropertyValue(Object id)
        {
            return null;
        }

        public boolean isPropertySet(Object id)
        {
            return false;
        }

        public void resetPropertyValue(Object id)
        {
        }

        public void setPropertyValue(Object id, Object value)
        {
        }
    }

    //////////////////////////////////////////////////////
    // Column descriptor

    private static class ObjectColumn {
        String id;
        Item item;
        Map<Class<?>, ObjectPropertyDescriptor> propMap = new IdentityHashMap<Class<?>, ObjectPropertyDescriptor>();

        private ObjectColumn(Item item, String id) {
            this.id = id;
            this.item = item;
        }
        void addProperty(Class<?> objectClass, ObjectPropertyDescriptor prop)
        {
            this.propMap.put(objectClass, prop);
        }
    }

    //////////////////////////////////////////////////////
    // Lazy object info

    private static class LazyObject {
        final Object object;
        final List<ObjectColumn> columns = new ArrayList<ObjectColumn>();
        private LazyObject(Object object)
        {
            this.object = object;
        }
    }

    //////////////////////////////////////////////////////
    // List sorter

    private class SortListener implements Listener
    {
        int sortDirection = SWT.DOWN;
        Item prevColumn = null;

        public void handleEvent(Event e) {
            Collator collator = Collator.getInstance();
            Item column = (Item)e.widget;
            final int colIndex = isTree ? getTree().indexOf((TreeColumn) column) : getTable().indexOf((TableColumn)column );
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
                    } else if (value1 instanceof Comparable && value1.getClass() == value2.getClass()) {
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
            if (!sampleItems && isHyperlink(cellValue)) {
                return "";
            }
            return getCellString(cellValue);
        }
    }

    public class ObjectsLoadVisualizer extends ProgressVisualizer<Collection<OBJECT_TYPE>> {

        public ObjectsLoadVisualizer()
        {
        }

        public void completeLoading(Collection<OBJECT_TYPE> items)
        {
            super.completeLoading(items);
            clearData();

            final Control itemsControl = itemsViewer.getControl();
            if (itemsControl.isDisposed()) {
                return;
            }

            // Collect list of items' classes
            final List<Class<?>> classList = new ArrayList<Class<?>>();
            Class<?>[] baseTypes = getListBaseTypes();
            if (!CommonUtils.isEmpty(baseTypes)) {
                Collections.addAll(classList, baseTypes);
            }
            if (!CommonUtils.isEmpty(items)) {
                for (OBJECT_TYPE item : items) {
                    Object object = getObjectValue(item);
                    if (!classList.contains(object.getClass())) {
                        classList.add(object.getClass());
                    }
                    if (isTree) {
                        collectItemClasses(item, classList);
                    }
                }
            }

            // Create columns from classes' annotations
            for (Class<?> objectClass : classList) {
                List<ObjectPropertyDescriptor> props = ObjectAttributeDescriptor.extractAnnotations(listPropertySource, objectClass);
                for (ObjectPropertyDescriptor prop : props) {
                    if (!prop.isViewable()) {
                        continue;
                    }
                    createColumn(objectClass, prop);
                }
            }

            // Set viewer content
            final List<OBJECT_TYPE> objectList = CommonUtils.isEmpty(items) ? Collections.<OBJECT_TYPE>emptyList() : new ArrayList<OBJECT_TYPE>(items);
            setInfo(getItemsLoadMessage(objectList.size()));

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
                        ((TreeViewer)itemsViewer).expandAll();
                        UIUtils.packColumns(getTree(), isFitWidth);
                    } else {
                        UIUtils.packColumns(getTable());
                    }
                } finally {
                    sampleItems = false;
                }

                // Set real content
                itemsViewer.setInput(objectList);
            }
        }

        private void collectItemClasses(OBJECT_TYPE item, List<Class<?>> classList)
        {
            ITreeContentProvider contentProvider = (ITreeContentProvider) itemsViewer.getContentProvider();
            if (!contentProvider.hasChildren(item)) {
                return;
            }
            Object[] children = contentProvider.getChildren(item);
            if (!CommonUtils.isEmpty(children)) {
                for (Object child : children) {
                    OBJECT_TYPE childItem = (OBJECT_TYPE)child;
                    Object objectValue = getObjectValue(childItem);
                    if (!classList.contains(objectValue.getClass())) {
                        classList.add(objectValue.getClass());
                    }
                    collectItemClasses(childItem, classList);
                }
            }
        }

        protected String getItemsLoadMessage(int count)
        {
            if (count == 0) {
                return "No items";
            } else {
                return count + " items";
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
                    Object cellValue = getCellValue(event.item.getData(), event.index);
                    if (cellValue == LOADING_LABEL) {
                        Object itemValue;
                        if (isTree) {
                            itemValue = ((TreeItem)event.item).getText(event.index);
                        } else {
                            itemValue = ((TableItem)event.item).getText(event.index);
                        }
                        if (itemValue == LOADING_LABEL) {
                            addLazyObject((Item)event.item, columns.get(event.index));
                        }
                    }
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
                            Rectangle textBounds;
                            if (event.item instanceof TreeItem) {
                                textBounds = ((TreeItem) event.item).getTextBounds(event.index);
                            } else {
                                textBounds = ((TableItem) event.item).getTextBounds(event.index);
                            }
                            linkLayout.draw(gc, textBounds.x, textBounds.y + 1);
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
            getItemsViewer().getControl().setCursor(arrowCursor);
        }

        public void mouseMove(MouseEvent e)
        {
            Item hoverItem;
            if (isTree) {
                hoverItem = detectTreeItem(e.x, e.y);
            } else {
                hoverItem = detectTableItem(e.x, e.y);
            }
            //String tip = null;
            if (hoverItem == null || selectedColumn < 0) {
                resetCursor();
            } else {
                Object element = hoverItem.getData();

                int checkColumn = selectedColumn;
                Object cellValue = getCellValue(element, checkColumn);
                if (cellValue == null) {
                    resetCursor();
                } else {
                    //tip = getCellString(cellValue);
                    if (isHyperlink(cellValue) && getCellLinkBounds(hoverItem, checkColumn, cellValue).contains(e.x, e.y)) {
                        getItemsViewer().getControl().setCursor(linkCursor);
                    } else {
                        resetCursor();
                    }
                }
            }
            //setToolTipText(tip);
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
        linkLayout.setIndent(0);
        linkLayout.setStyle(linkStyle, 0, text.length());
    }

    private static String getCellString(Object value)
    {
        if (value == null || value instanceof Boolean) {
            return "";
        }
        if (value instanceof DBPNamedObject) {
            value = ((DBPNamedObject)value).getName();
        }
        return UIUtils.makeStringForUI(value).toString();
    }

    private class LazyLoaderJob extends AbstractJob {
        public LazyLoaderJob()
        {
            super("Lazy objects loader");
        }

        @Override
        protected IStatus run(final DBRProgressMonitor monitor)
        {
            final Map<Item, LazyObject> objectMap = obtainLazyObjects();
            if (isDisposed()) {
                return Status.OK_STATUS;
            }
            for (Map.Entry<Item, LazyObject> entry : objectMap.entrySet()) {
                if (monitor.isCanceled()) {
                    break;
                }
                final Item item = entry.getKey();
                final LazyObject lazyObject = entry.getValue();
                Object object = getObjectValue((OBJECT_TYPE) lazyObject.object);
                if (object == null) {
                    continue;
                }
                final List<String> stringValues = new ArrayList<String>(lazyObject.columns.size());
                for (ObjectColumn column : lazyObject.columns) {
                    if (monitor.isCanceled() || isDisposed()) {
                        break;
                    }
                    try {
                        ObjectPropertyDescriptor prop = column.propMap.get(object.getClass());
                        if (prop != null) {
                            Object lazyValue = prop.readValue(object, monitor);
                            if (isHyperlink(lazyValue)) {
                                stringValues.add("");
                            } else {
                                stringValues.add(getCellString(lazyValue));
                            }
                        } else {
                            stringValues.add("");
                        }
                    }
                    catch (IllegalAccessException e) {
                        log.error(e);
                        return RuntimeUtils.makeExceptionStatus(e);
                    }
                    catch (InvocationTargetException e) {
                        log.error(e.getTargetException());
                        return RuntimeUtils.makeExceptionStatus(e.getTargetException());
                    }
                }

                // Save read values in tree/table items
                if (!isDisposed()) {
                    getDisplay().syncExec(new Runnable() {
                        public void run()
                        {
                            for (int i = 0, columnsSize = lazyObject.columns.size(); i < columnsSize; i++) {
                                ObjectColumn column = lazyObject.columns.get(i);
                                int columnIndex = columns.indexOf(column);
                                String stringValue = stringValues.get(i);
                                if (monitor.isCanceled() || isDisposed() || item.isDisposed()) {
                                    return;
                                }
                                if (item instanceof TreeItem) {
                                    ((TreeItem) item).setText(columnIndex, stringValue);
                                } else {
                                    ((TableItem) item).setText(columnIndex, stringValue);
                                }
                            }
                        }
                    });
                }
            }

            return Status.OK_STATUS;
        }
    }

}
