/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ui.controls.itemlist;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.IDataSourceContainerProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.AbstractJob;
import org.jkiss.dbeaver.runtime.load.jobs.LoadingJob;
import org.jkiss.dbeaver.runtime.properties.*;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ObjectViewerRenderer;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

/**
 * ObjectListControl
 */
public abstract class ObjectListControl<OBJECT_TYPE> extends ProgressPageControl {
    static final Log log = Log.getLog(ObjectListControl.class);

    private final static LazyValue DEF_LAZY_VALUE = new LazyValue("..."); //$NON-NLS-1$
    private final static String DATA_OBJECT_COLUMN = "objectColumn"; //$NON-NLS-1$
    private final static int LAZY_LOAD_DELAY = 100;
    private final static Object NULL_VALUE = new Object();

    private boolean isFitWidth;

    private ColumnViewer itemsViewer;
    //private ColumnViewerEditor itemsEditor;
    @NotNull
    private final List<ObjectColumn> columns = new ArrayList<>();
    private IDoubleClickListener doubleClickHandler;
    private PropertySourceAbstract listPropertySource;

    private ObjectViewerRenderer renderer;

    // Sample flag. True only when initial content is packed. Used to provide actual cell data to Tree/Table pack() methods
    // After content is loaded is always false (and all hyperlink cells have empty text)
    private transient boolean sampleItems = false;

    private volatile OBJECT_TYPE curListObject;
    private volatile LoadingJob<Collection<OBJECT_TYPE>> loadingJob;

    private Job lazyLoadingJob = null;
    private Map<OBJECT_TYPE, List<ObjectColumn>> lazyObjects;
    private final Map<OBJECT_TYPE, Map<String, Object>> lazyCache = new IdentityHashMap<>();
    private volatile boolean lazyLoadCanceled;
    private List<OBJECT_TYPE> objectList = null;

    public ObjectListControl(
        Composite parent,
        int style,
        IContentProvider contentProvider)
    {
        super(parent, style);
        this.isFitWidth = false;

        int viewerStyle = getDefaultListStyle();
        if ((style & SWT.SHEET) == 0) {
            viewerStyle |= SWT.BORDER;
        }

        EditorActivationStrategy editorActivationStrategy;
        if (contentProvider instanceof ITreeContentProvider) {
            TreeViewer treeViewer = new TreeViewer(this, viewerStyle);
            final Tree tree = treeViewer.getTree();
            tree.setLinesVisible (true);
            tree.setHeaderVisible(true);
            itemsViewer = treeViewer;
            editorActivationStrategy = new EditorActivationStrategy(treeViewer);
            TreeViewerEditor.create(treeViewer, editorActivationStrategy, ColumnViewerEditor.TABBING_CYCLE_IN_ROW);
            // We need measure item listener to prevent collapse/expand on double click
            // Looks like a bug in SWT: http://www.eclipse.org/forums/index.php/t/257325/
            treeViewer.getControl().addListener(SWT.MeasureItem, new Listener(){
                @Override
                public void handleEvent(Event event) {
                    // Just do nothing
                }});
        } else {
            TableViewer tableViewer = new TableViewer(this, viewerStyle);
            final Table table = tableViewer.getTable();
            table.setLinesVisible (true);
            table.setHeaderVisible(true);
            itemsViewer = tableViewer;
            //UIUtils.applyCustomTolTips(table);
            //itemsEditor = new TableEditor(table);
            editorActivationStrategy = new EditorActivationStrategy(tableViewer);
            TableViewerEditor.create(tableViewer, editorActivationStrategy, ColumnViewerEditor.TABBING_CYCLE_IN_ROW);
        }
        //editorActivationStrategy.setEnableEditorActivationWithKeyboard(true);
        renderer = createRenderer();
        itemsViewer.getColumnViewerEditor().addEditorActivationListener(new EditorActivationListener());

        itemsViewer.setContentProvider(contentProvider);
        //itemsViewer.setLabelProvider(new ItemLabelProvider());
        itemsViewer.getControl().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(MouseEvent e) {
                if (doubleClickHandler != null) {
                    // Uee provided double click
                    doubleClickHandler.doubleClick(new DoubleClickEvent(itemsViewer, itemsViewer.getSelection()));
                }
            }
        });
        itemsViewer.getControl().addListener(SWT.PaintItem, new PaintListener());
        itemsViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event)
            {
                IStructuredSelection selection = (IStructuredSelection)event.getSelection();
                if (selection.isEmpty()) {
                    setCurListObject(null);
                } else {
                    setCurListObject((OBJECT_TYPE) selection.getFirstElement());
                }
            }
        });
        GridData gd = new GridData(GridData.FILL_BOTH);
        itemsViewer.getControl().setLayoutData(gd);
        //PropertiesContributor.getInstance().addLazyListener(this);

        // Add selection listener
        itemsViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event)
            {
                String status;
                IStructuredSelection selection = (IStructuredSelection)itemsViewer.getSelection();
                if (selection.isEmpty()) {
                    status = ""; //$NON-NLS-1$
                } else if (selection.size() == 1) {
                    Object selectedNode = selection.getFirstElement();
                    status = ObjectViewerRenderer.getCellString(selectedNode);
                } else {
                    status = NLS.bind(CoreMessages.controls_object_list_status_objects, selection.size());
                }
                setInfo(status);
            }
        });
    }

    protected int getDefaultListStyle() {
        return SWT.MULTI | SWT.FULL_SELECTION;
    }

    public ObjectViewerRenderer getRenderer()
    {
        return renderer;
    }

    public PropertySourceAbstract getListPropertySource()
    {
        if (this.listPropertySource == null) {
            this.listPropertySource = createListPropertySource();
        }
        return listPropertySource;
    }

    protected PropertySourceAbstract createListPropertySource()
    {
        return new DefaultListPropertySource();
    }

    protected CellLabelProvider getColumnLabelProvider(int columnIndex)
    {
        return new ObjectColumnLabelProvider(columnIndex);
    }

    @Override
    protected boolean cancelProgress()
    {
        synchronized (this) {
            if (loadingJob != null) {
                loadingJob.cancel();
                return true;
            }
        }
        return false;
    }

    public OBJECT_TYPE getCurrentListObject()
    {
        return curListObject;
    }

    protected void setCurListObject(@Nullable OBJECT_TYPE curListObject)
    {
        this.curListObject = curListObject;
    }

    public ColumnViewer getItemsViewer()
    {
        return itemsViewer;
    }

    public Composite getControl()
    {
        // Both table and tree are composites so its ok
        return (Composite) itemsViewer.getControl();
    }

    public ISelectionProvider getSelectionProvider()
    {
        return itemsViewer;
    }

    protected ObjectColumn getColumn(int index)
    {
        return columns.get(index);
    }

    @Nullable
    protected ObjectPropertyDescriptor getObjectProperty(OBJECT_TYPE object, int columnIndex)
    {
        return columns.get(columnIndex).getProperty(getObjectValue(object));
    }

    public void setFitWidth(boolean fitWidth)
    {
        isFitWidth = fitWidth;
    }

    @Override
    public void disposeControl()
    {
        //PropertiesContributor.getInstance().removeLazyListener(this);
        synchronized (this) {
            if (loadingJob != null) {
                // Cancel running job
                loadingJob.cancel();
                loadingJob = null;
            }
        }
        renderer.dispose();
        super.disposeControl();
    }

    public synchronized boolean isLoading()
    {
        return loadingJob != null;
    }

    public void loadData()
    {
        loadData(true);
    }

    public void loadData(boolean lazy)
    {
        if (this.loadingJob != null) {
            try {
                for (int i = 0; i < 4; i++) {
                    Thread.sleep(500);
                    if (this.loadingJob == null) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                // interrupted
            }
            if (loadingJob != null) {
                UIUtils.showMessageBox(getShell(), "Load", "Service is busy", SWT.ICON_WARNING);
                return;
            }
            return;
        }
        getListPropertySource();

        clearLazyCache();
        this.lazyLoadCanceled = false;

        if (lazy) {
            // start loading service
            synchronized (this) {
                this.loadingJob = createLoadService();
                if (this.loadingJob != null) {
                    this.loadingJob.addJobChangeListener(new JobChangeAdapter() {
                        @Override
                        public void done(IJobChangeEvent event)
                        {
                            loadingJob = null;
                        }
                    });
                    this.loadingJob.schedule(LAZY_LOAD_DELAY);
                }
            }
        } else {
            // Load data synchronously
            final LoadingJob<Collection<OBJECT_TYPE>> loadService = createLoadService();
            if (loadService != null) {
                loadService.syncRun();
            }
        }
    }

    private void setListData(Collection<OBJECT_TYPE> items, boolean append)
    {
        final Control itemsControl = itemsViewer.getControl();
        if (itemsControl.isDisposed()) {
            return;
        }
        itemsControl.setRedraw(false);
        try {
            final boolean reload = !append && (objectList == null) || (columns.isEmpty());

            if (reload) {
                clearListData();
            }

            {
                // Collect list of items' classes
                final List<Class<?>> classList = new ArrayList<>();
                Class<?>[] baseTypes = getListBaseTypes(items);
                if (!ArrayUtils.isEmpty(baseTypes)) {
                    Collections.addAll(classList, baseTypes);
                }
                if (!CommonUtils.isEmpty(items)) {
                    for (OBJECT_TYPE item : items) {
                        Object object = getObjectValue(item);
                        if (object != null && !classList.contains(object.getClass())) {
                            classList.add(object.getClass());
                        }
                        if (renderer.isTree()) {
                            collectItemClasses(item, classList);
                        }
                    }
                }

                IPropertyFilter propertyFilter = new DataSourcePropertyFilter(
                    ObjectListControl.this instanceof IDataSourceContainerProvider ?
                        ((IDataSourceContainerProvider)ObjectListControl.this).getDataSourceContainer() :
                        null);

                // Collect all properties
                List<ObjectPropertyDescriptor> allProps = ObjectAttributeDescriptor.extractAnnotations(getListPropertySource(), classList, propertyFilter);

                // Create columns from classes' annotations
                for (ObjectPropertyDescriptor prop : allProps) {
                    if (!prop.isViewable() || prop.isHidden()) {
                        continue;
                    }
                    if (!getListPropertySource().hasProperty(prop)) {
                        getListPropertySource().addProperty(prop);
                        createColumn(prop);
                    }
                }
            }

            if (!itemsControl.isDisposed()) {
                if (reload || objectList.isEmpty()) {
                    // Set viewer content
                    objectList = CommonUtils.isEmpty(items) ? new ArrayList<OBJECT_TYPE>() : new ArrayList<>(items);

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

                        if (renderer.isTree()) {
                            ((TreeViewer)itemsViewer).expandAll();
                            UIUtils.packColumns(getTree(), isFitWidth, null);
                        } else {
                            UIUtils.packColumns(getTable(), isFitWidth);
                        }
                    } finally {
                        sampleItems = false;
                    }
                    // Set real content
                    itemsViewer.setInput(objectList);
                } else if (items != null) {
                    if (append) {
                        // Simply append new list to the tail
                        for (OBJECT_TYPE newObject : items) {
                            if (!objectList.contains(newObject)) {
                                objectList.add(newObject);
                            }
                        }
                    } else {
                        // Update object list
                        if (!objectList.equals(items)) {
                            int newListSize = items.size();
                            int itemIndex = 0;
                            for (OBJECT_TYPE newObject : items) {
                                if (itemIndex >= objectList.size()) {
                                    // Add to tail
                                    objectList.add(itemIndex, newObject);
                                } else {
                                    OBJECT_TYPE oldObject = objectList.get(itemIndex);
                                    if (!CommonUtils.equalObjects(oldObject, newObject)) {
                                        // Replace old object
                                        objectList.set(itemIndex, newObject);
                                    }
                                }
                                itemIndex++;
                            }
                            while (objectList.size() > newListSize) {
                                objectList.remove(objectList.size() - 1);
                            }
                        }
                    }

                    itemsViewer.refresh();
                }
            }
        } finally {
            itemsControl.setRedraw(true);
        }
        setInfo(getItemsLoadMessage(objectList.size()));
    }

    public void appendListData(Collection<OBJECT_TYPE> items)
    {
        setListData(items, true);
    }

    public Collection<OBJECT_TYPE> getListData() {
        return objectList;
    }

    public void clearListData()
    {
        for (ObjectColumn column : columns) {
            column.item.dispose();
        }
        columns.clear();
        if (!itemsViewer.getControl().isDisposed()) {
            itemsViewer.setInput(Collections.emptyList());
        }
        if (listPropertySource != null) {
            listPropertySource.clearProperties();
        }
        clearLazyCache();
    }

    private void collectItemClasses(OBJECT_TYPE item, List<Class<?>> classList)
    {
        ITreeContentProvider contentProvider = (ITreeContentProvider) itemsViewer.getContentProvider();
        if (!contentProvider.hasChildren(item)) {
            return;
        }
        Object[] children = contentProvider.getChildren(item);
        if (!ArrayUtils.isEmpty(children)) {
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

    private void clearLazyCache()
    {
        synchronized (lazyCache) {
            lazyCache.clear();
        }
    }

    protected String getItemsLoadMessage(int count)
    {
        if (count == 0) {
            return CoreMessages.controls_object_list_message_no_items;
        } else {
            return NLS.bind(CoreMessages.controls_object_list_message_items, count);
        }
    }

    public void setDoubleClickHandler(IDoubleClickListener doubleClickHandler)
    {
        this.doubleClickHandler = doubleClickHandler;
    }

    private Tree getTree()
    {
        return ((TreeViewer)itemsViewer).getTree();
    }

    private Table getTable()
    {
        return ((TableViewer)itemsViewer).getTable();
    }

    private synchronized void addLazyObject(OBJECT_TYPE object, ObjectColumn column)
    {
        if (lazyObjects == null) {
            lazyObjects = new LinkedHashMap<>();
        }
        List<ObjectColumn> objectColumns = lazyObjects.get(object);
        if (objectColumns == null) {
            objectColumns = new ArrayList<>();
            lazyObjects.put(object, objectColumns);
        }
        if (!objectColumns.contains(column)) {
            objectColumns.add(column);
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

    @Nullable
    private synchronized Map<OBJECT_TYPE, List<ObjectColumn>> obtainLazyObjects()
    {
        synchronized (lazyCache) {
            if (lazyObjects == null) {
                return null;
            }
            Map<OBJECT_TYPE, List<ObjectColumn>> tmp = lazyObjects;
            lazyObjects = null;
            return tmp;
        }
    }

    @Nullable
    private Object getCellValue(Object element, int columnIndex)
    {
        OBJECT_TYPE object = (OBJECT_TYPE)element;

        if (columnIndex >= columns.size()) {
            return null;
        }
        ObjectColumn column = columns.get(columnIndex);
        if (column.item.isDisposed()) {
            return null;
        }
        Object objectValue = getObjectValue(object);
        if (objectValue == null) {
            return null;
        }
        ObjectPropertyDescriptor prop = getPropertyByObject(column, objectValue);
        if (prop == null) {
            return null;
        }
        //if (!prop.isReadOnly(objectValue) && isNewObject(object)) {
            // Non-editable properties are empty for new objects
            //return null;
        //}
        if (prop.isLazy(objectValue, true)) {
            synchronized (lazyCache) {
                final Map<String, Object> cache = lazyCache.get(object);
                if (cache != null) {
                    final Object value = cache.get(column.id);
                    if (value != null) {
                        if (value == NULL_VALUE) {
                            return null;
                        } else {
                            return value;
                        }
                    }
                }
            }
            if (prop.supportsPreview()) {
                final Object previewValue = getListPropertySource().getPropertyValue(null, objectValue, prop);
                if (previewValue != null) {
                    return new LazyValue(previewValue);
                }
            }
            return DEF_LAZY_VALUE;
        }
        return getListPropertySource().getPropertyValue(null, objectValue, prop);
    }

    @Nullable
    private static ObjectPropertyDescriptor getPropertyByObject(ObjectColumn column, Object objectValue)
    {
        ObjectPropertyDescriptor prop = null;
        for (Class valueClass = objectValue.getClass(); prop == null && valueClass != Object.class; valueClass = valueClass.getSuperclass()) {
            prop = column.propMap.get(valueClass);
        }
        if (prop == null) {
            for (Map.Entry<Class<?>, ObjectPropertyDescriptor> entry : column.propMap.entrySet()) {
                if (entry.getKey().isInstance(objectValue)) {
                    prop = entry.getValue();
                    break;
                }
            }
        }
        return prop;
    }

    @Nullable
    protected Class<?>[] getListBaseTypes(Collection<OBJECT_TYPE> items)
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
     * @param item object
     * @return image or null
     */
    @Nullable
    protected DBPImage getObjectImage(OBJECT_TYPE item)
    {
        return null;
    }

    protected boolean isNewObject(OBJECT_TYPE objectValue)
    {
        return false;
    }

    @NotNull
    protected Set<DBPPropertyDescriptor> getAllProperties()
    {
        Set<DBPPropertyDescriptor> props = new LinkedHashSet<>();
        for (ObjectColumn column : columns) {
            props.addAll(column.propMap.values());
        }
        return props;
    }

    protected void createColumn(ObjectPropertyDescriptor prop)
    {
        ObjectColumn objectColumn = null;
        for (ObjectColumn col : columns) {
            if (col.id.equals(prop.getId())) {
                objectColumn = col;
                break;
            }
        }
        // Use prop class from top parent
        Class<?> propClass = prop.getParent() == null ? prop.getDeclaringClass() : prop.getParent().getDeclaringClass();
        if (objectColumn == null) {
            Item columnItem;
            ViewerColumn newColumn;
            if (renderer.isTree()) {
                TreeViewerColumn viewerColumn = new TreeViewerColumn ((TreeViewer) itemsViewer, SWT.NONE);
                viewerColumn.getColumn().setText(prop.getDisplayName());
                viewerColumn.getColumn().setToolTipText(prop.getDescription());
                viewerColumn.getColumn().addListener(SWT.Selection, renderer.getSortListener());
                newColumn = viewerColumn;
                columnItem = viewerColumn.getColumn();
            } else {
                TableViewerColumn viewerColumn = new TableViewerColumn ((TableViewer) itemsViewer, SWT.NONE);
                viewerColumn.getColumn().setText(prop.getDisplayName());
                viewerColumn.getColumn().setToolTipText(prop.getDescription());
                //column.setData(prop);
                viewerColumn.getColumn().addListener(SWT.Selection, renderer.getSortListener());
                newColumn = viewerColumn;
                columnItem = viewerColumn.getColumn();
            }
            newColumn.setLabelProvider(getColumnLabelProvider(columns.size()));
            final EditingSupport editingSupport = makeEditingSupport(newColumn, columns.size());
            if (editingSupport != null) {
                newColumn.setEditingSupport(editingSupport);
            }
            objectColumn = new ObjectColumn(newColumn, columnItem, CommonUtils.toString(prop.getId()));
            objectColumn.addProperty(propClass, prop);
            this.columns.add(objectColumn);
            columnItem.setData(DATA_OBJECT_COLUMN, objectColumn);
        } else {
            objectColumn.addProperty(propClass, prop);
            String oldTitle = objectColumn.item.getText();
            if (!oldTitle.contains(prop.getDisplayName())) {
                objectColumn.item.setText(CommonUtils.capitalizeWord(objectColumn.id));
            }
        }
    }

    //////////////////////////////////////////////////////
    // Overridable functions

    protected abstract LoadingJob<Collection<OBJECT_TYPE>> createLoadService();

    protected ObjectViewerRenderer createRenderer()
    {
        return new ViewerRenderer();
    }

    //////////////////////////////////////////////////////
    // Edit

    @Nullable
    protected EditingSupport makeEditingSupport(ViewerColumn viewerColumn, int columnIndex)
    {
        return null;
    }

    //////////////////////////////////////////////////////
    // Editor activation

    private class EditorActivationStrategy extends ColumnViewerEditorActivationStrategy {

        public EditorActivationStrategy(ColumnViewer viewer)
        {
            super(viewer);
        }

        @Override
        protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event)
        {
            ViewerCell cell = (ViewerCell)event.getSource();
            if (renderer.isHyperlink(getCellValue(cell.getElement(), cell.getColumnIndex())) &&
                getItemsViewer().getControl().getCursor() == getItemsViewer().getControl().getDisplay().getSystemCursor(SWT.CURSOR_HAND)) {
                return false;
            }
            return super.isEditorActivationEvent(event);
        }
    }

    private static class EditorActivationListener extends ColumnViewerEditorActivationListener {
        @Override
        public void beforeEditorActivated(ColumnViewerEditorActivationEvent event)
        {
        }

        @Override
        public void afterEditorActivated(ColumnViewerEditorActivationEvent event)
        {
        }

        @Override
        public void beforeEditorDeactivated(ColumnViewerEditorDeactivationEvent event)
        {
        }

        @Override
        public void afterEditorDeactivated(ColumnViewerEditorDeactivationEvent event)
        {
        }
    }


    //////////////////////////////////////////////////////
    // Property source implementation

    private class DefaultListPropertySource extends PropertySourceAbstract {

        public DefaultListPropertySource()
        {
            super(ObjectListControl.this, ObjectListControl.this, true);
        }

        @Override
        public Object getSourceObject()
        {
            return getCurrentListObject();
        }

        @Override
        public Object getEditableValue()
        {
            return getObjectValue(getCurrentListObject());
        }

        @Override
        public DBPPropertyDescriptor[] getPropertyDescriptors2()
        {
            Set<DBPPropertyDescriptor> props = getAllProperties();
            return props.toArray(new DBPPropertyDescriptor[props.size()]);
        }

    }

    //////////////////////////////////////////////////////
    // Column descriptor

    protected static class ObjectColumn {
        String id;
        Item item;
        ViewerColumn column;
        Map<Class<?>, ObjectPropertyDescriptor> propMap = new IdentityHashMap<>();

        private ObjectColumn(ViewerColumn column, Item item, String id) {
            this.id = id;
            this.column = column;
            this.item = item;
        }
        void addProperty(Class<?> objectClass, ObjectPropertyDescriptor prop)
        {
            this.propMap.put(objectClass, prop);
        }

        @Nullable
        public ObjectPropertyDescriptor getProperty(Object element)
        {
            return element == null ? null : getPropertyByObject(this, element);
        }
    }

    //////////////////////////////////////////////////////
    // List sorter

    protected class ObjectColumnLabelProvider extends ColumnLabelProvider
    {
        protected final int columnIndex;

        ObjectColumnLabelProvider(int columnIndex)
        {
            this.columnIndex = columnIndex;
        }

        @Nullable
        @Override
        public Image getImage(Object element)
        {
            if (columnIndex == 0) {
                return DBeaverIcons.getImage(getObjectImage((OBJECT_TYPE) element));
            }
            return null;
        }

        @Override
        public String getText(Object element)
        {
            Object cellValue = getCellValue(element, columnIndex);
            if (cellValue instanceof LazyValue) {
                cellValue = ((LazyValue)cellValue).value;
            }
            if (!sampleItems && renderer.isHyperlink(cellValue)) {
                return ""; //$NON-NLS-1$
            }
            return ObjectViewerRenderer.getCellString(cellValue);
        }

    }

    public class ObjectsLoadVisualizer extends ProgressVisualizer<Collection<OBJECT_TYPE>> {

        public ObjectsLoadVisualizer()
        {
        }

        @Override
        public void completeLoading(Collection<OBJECT_TYPE> items)
        {
            super.completeLoading(items);
            setListData(items, false);
        }

    }

    public class ObjectActionVisualizer extends ProgressVisualizer<Void> {

        public ObjectActionVisualizer()
        {
        }

        @Override
        public void completeLoading(Void v)
        {
            super.completeLoading(v);
        }
    }

    private static class LazyValue {
        private final Object value;

        private LazyValue(Object value)
        {
            this.value = value;
        }

        @Override
        public String toString()
        {
            return value.toString();
        }
    }

	class PaintListener implements Listener {

		@Override
        public void handleEvent(Event event) {
            if (isDisposed()) {
                return;
            }
			switch(event.type) {
				case SWT.PaintItem: if (event.index < columns.size()) {
                    final OBJECT_TYPE object = (OBJECT_TYPE)event.item.getData();
                    final Object objectValue = getObjectValue(object);
                    Object cellValue = getCellValue(object, event.index);
                    final ObjectColumn objectColumn = columns.get(event.index);
                    if (cellValue instanceof LazyValue) {
                        if (!lazyLoadCanceled) {
                            addLazyObject(object, objectColumn);
                        }
                    } else if (cellValue != null ) {
                        ObjectPropertyDescriptor prop = getPropertyByObject(objectColumn, objectValue);
                        if (prop != null) {
                            renderer.paintCell(event, object, event.index, prop.isEditable(objectValue));
                        }
                    }
					break;
				}
			}
		}
	}

    private class LazyLoaderJob extends AbstractJob {
        public LazyLoaderJob()
        {
            super(CoreMessages.controls_object_list_job_props_read);
        }

        @Override
        protected IStatus run(final DBRProgressMonitor monitor)
        {
            final Map<OBJECT_TYPE, List<ObjectColumn>> objectMap = obtainLazyObjects();
            if (isDisposed()) {
                return Status.OK_STATUS;
            }
            monitor.beginTask(CoreMessages.controls_object_list_monitor_load_lazy_props, objectMap.size());
            for (Map.Entry<OBJECT_TYPE, List<ObjectColumn>> entry : objectMap.entrySet()) {
                if (monitor.isCanceled() || isDisposed()) {
                    break;
                }
                final OBJECT_TYPE element = entry.getKey();
                Object object = getObjectValue(element);
                if (object == null) {
                    continue;
                }
                Map<String, Object> objectCache;
                synchronized (lazyCache) {
                    objectCache = lazyCache.get(element);
                    if (objectCache == null) {
                        objectCache = new HashMap<>();
                        lazyCache.put(element, objectCache);
                    }
                }
                String objectName = GeneralUtils.makeDisplayString(getObjectValue(element)).toString();
                monitor.subTask(NLS.bind(CoreMessages.controls_object_list_monitor_load_props, objectName));
                for (ObjectColumn column : entry.getValue()) {
                    if (monitor.isCanceled() || isDisposed()) {
                        break;
                    }
                    ObjectPropertyDescriptor prop = getPropertyByObject(column, object);
                    if (prop != null) {
                        try {
                            synchronized (lazyCache) {
                                if (objectCache.containsKey(prop.getId())) {
                                    // This property already cached
                                    continue;
                                }
                            }
                            Object lazyValue = prop.readValue(object, monitor);
                            if (lazyValue == null) {
                                lazyValue = NULL_VALUE;
                            }
                            synchronized (lazyCache) {
                                objectCache.put(prop.getId(), lazyValue);
                            }
                        }
                        catch (Throwable e) {
                            if (e instanceof InvocationTargetException) {
                                e = ((InvocationTargetException) e).getTargetException();
                            }
                            log.error("Error reading property '" + prop.getId() + "' from " + object, e); //$NON-NLS-1$ //$NON-NLS-2$
                            // do not return error - it causes a lot of error boxes
                            //return RuntimeUtils.makeExceptionStatus(e);
                        }
                }
                }
                monitor.worked(1);
                if (!isDisposed()) {
                    getDisplay().asyncExec(new Runnable() {
                        @Override
                        public void run()
                        {
                            if (!isDisposed()) {
                                itemsViewer.update(element, null);
                            }
                        }
                    });
                }
            }
            monitor.done();

/*
            // Update viewer
            if (!isDisposed()) {
                getDisplay().asyncExec(new Runnable() {
                    public void run()
                    {
                        itemsViewer.getControl().setRedraw(false);
                        try {
                            itemsViewer.update(objectMap.keySet().toArray(), null);
                        } finally {
                            itemsViewer.getControl().setRedraw(true);
                        }
                    }
                });
            }
*/
            if (monitor.isCanceled()) {
                lazyLoadCanceled = true;
                obtainLazyObjects();
            }
            return Status.OK_STATUS;
        }
    }

    protected class ViewerRenderer extends ObjectViewerRenderer {
        protected ViewerRenderer()
        {
            super(itemsViewer);
        }

        @Nullable
        @Override
        protected Object getCellValue(Object element, int columnIndex)
        {
            return ObjectListControl.this.getCellValue(element, columnIndex);
        }
    }

}
