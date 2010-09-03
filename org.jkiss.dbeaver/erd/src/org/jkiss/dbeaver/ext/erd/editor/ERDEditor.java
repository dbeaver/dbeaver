/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.editor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.gef.*;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CommandStackListener;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.palette.*;
import org.eclipse.gef.ui.actions.*;
import org.eclipse.gef.ui.palette.FlyoutPaletteComposite.FlyoutPreferences;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.gef.ui.properties.UndoablePropertySheetEntry;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheetPage;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabaseObjectManager;
import org.jkiss.dbeaver.ext.erd.Activator;
import org.jkiss.dbeaver.ext.erd.action.DiagramLayoutAction;
import org.jkiss.dbeaver.ext.erd.action.DiagramRefreshAction;
import org.jkiss.dbeaver.ext.erd.directedit.StatusLineValidationMessageHandler;
import org.jkiss.dbeaver.ext.erd.model.ERDAssociation;
import org.jkiss.dbeaver.ext.erd.model.ERDTable;
import org.jkiss.dbeaver.ext.erd.model.ERDTableColumn;
import org.jkiss.dbeaver.ext.erd.part.DiagramPart;
import org.jkiss.dbeaver.ext.erd.dnd.DataEditDropTargetListener;
import org.jkiss.dbeaver.ext.erd.dnd.DataElementFactory;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.ext.ui.IDatabaseObjectEditor;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.load.AbstractLoadService;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Editor implementation based on the the example editor skeleton that is built in <i>Building
 * an editor </i> in chapter <i>Introduction to GEF </i>
 */
public class ERDEditor extends GraphicalEditorWithFlyoutPalette
    implements
    CommandStackListener,
    ISelectionListener,
    IDatabaseObjectEditor<IDatabaseObjectManager<DBSEntityContainer>> {
    static final Log log = LogFactory.getLog(ERDEditor.class);

    private IDatabaseObjectManager<DBSEntityContainer> objectManager;
    private EntityDiagram entityDiagram;

    private ProgressControl progressControl;

    /**
     * the undoable <code>IPropertySheetPage</code>
     */
    private PropertySheetPage undoablePropertySheetPage;

    /**
     * the graphical viewer
     */
    private ScalableFreeformRootEditPart rootPart;

    /**
     * the list of action ids that are to EditPart actions
     */
    private List<String> editPartActionIDs = new ArrayList<String>();

    /**
     * the list of action ids that are to CommandStack actions
     */
    private List<String> stackActionIDs = new ArrayList<String>();

    /**
     * the list of action ids that are editor actions
     */
    private List<String> editorActionIDs = new ArrayList<String>();

    /**
     * the overview outline page
     */
    private ERDOutlinePage outlinePage;

    /**
     * the <code>EditDomain</code>
     */
    private DefaultEditDomain editDomain;

    /**
     * the dirty state
     */
    private boolean isDirty;

    private boolean isReadOnly;
    private boolean isLoaded;

    /**
     * No-arg constructor
     */
    public ERDEditor() {
    }

    /**
     * Initializes the editor.
     */
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        // Editor is readonly if editor input is not a file but database object
        this.isReadOnly = !(input instanceof IFileEditorInput);

        editDomain = new DefaultEditDomain(this);
        setEditDomain(editDomain);

        // store site and input
        setSite(site);
        setInput(input);

        // add CommandStackListener
        getCommandStack().addCommandStackListener(this);

        // add selection change listener
        getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);

        // initialize actions
        createActions();

    }

    @Override
    public void createPartControl(Composite parent) {
        progressControl = new ProgressControl(parent, SWT.NONE, this.getSite().getPart());

        super.createPartControl(progressControl.createContentContainer());

        progressControl.createProgressPanel();
    }

    /**
     * the selection listener implementation
     */
    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        updateActions(editPartActionIDs);
    }

    /**
     * The <code>CommandStackListener</code> that listens for
     * <code>CommandStack </code> changes.
     */
    public void commandStackChanged(EventObject event) {
        updateActions(stackActionIDs);
        setDirty(getCommandStack().isDirty());
    }

    public void dispose() {
        if (progressControl != null && !progressControl.isDisposed()) {
            progressControl.dispose();
        }
        // remove CommandStackListener
        getCommandStack().removeCommandStackListener(this);
        // remove selection listener
        getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(this);
        // dispose the ActionRegistry (will dispose all actions)
        getActionRegistry().dispose();
        // important: always call super implementation of dispose
        super.dispose();
    }

    /**
     * Adaptable implementation for Editor
     */
    public Object getAdapter(Class adapter) {
        // we need to handle common GEF elements we created
        if (adapter == GraphicalViewer.class || adapter == EditPartViewer.class) {
            return getGraphicalViewer();
        } else if (adapter == CommandStack.class) {
            return getCommandStack();
        } else if (adapter == EditDomain.class) {
            return getEditDomain();
        } else if (adapter == ActionRegistry.class) {
            return getActionRegistry();
        } else if (adapter == IPropertySheetPage.class) {
            return getPropertySheetPage();
        } else if (adapter == IContentOutlinePage.class) {
            return getOverviewOutlinePage();
        } else if (adapter == ZoomManager.class) {
            return getGraphicalViewer().getProperty(ZoomManager.class.toString());
        }
        // the super implementation handles the rest
        return super.getAdapter(adapter);
    }

    /**
     * Saves the schema model to the file
     *
     * @see EditorPart#doSave
     */
    public void doSave(IProgressMonitor monitor) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream objectOut = new ObjectOutputStream(out);
            objectOut.writeObject(entityDiagram);
            objectOut.close();
            IEditorInput input = getEditorInput();
            if (input instanceof IFileEditorInput) {
                IFile file = ((IFileEditorInput) input).getFile();
                try {
                    file.setContents(new ByteArrayInputStream(out.toByteArray()), true, false, monitor);
                } finally {
                    out.close();
                }
            }
        }
        catch (Exception e) {
            log.error("Could not save diagram", e);
        }
        getCommandStack().markSaveLocation();
    }

    /**
     * Save as not allowed
     */
    public void doSaveAs() {
        throw new UnsupportedOperationException();
    }

    /**
     * Save as not allowed
     */
    public boolean isSaveAsAllowed() {
        return false;
    }

    /**
     * Indicates if the editor has unsaved changes.
     *
     * @see EditorPart#isDirty
     */
    public boolean isDirty() {
        return !isReadOnly && isDirty;
    }

    /**
     * Returns the <code>CommandStack</code> of this editor's
     * <code>EditDomain</code>.
     *
     * @return the <code>CommandStack</code>
     */
    public CommandStack getCommandStack() {
        return getEditDomain().getCommandStack();
    }

    /**
     * Returns the schema model associated with the editor
     *
     * @return an instance of <code>Schema</code>
     */
    public EntityDiagram getDiagram() {
        return entityDiagram;
    }

    public DiagramPart getDiagramPart() {
        return (DiagramPart)rootPart.getContents();
    }

    /**
     * @see org.eclipse.ui.part.EditorPart#setInput(org.eclipse.ui.IEditorInput)
     */
    protected void setInput(IEditorInput input) {
        super.setInput(input);

        if (input instanceof IFileEditorInput) {
            loadContentFromFile((IFileEditorInput) input);
        } else {
            // Setup empty schema for now
            // Actual data will be loaded later in activatePart
            entityDiagram = new EntityDiagram(null, "empty");
        }
    }

    private EntityDiagram loadFromDatabase(DBRProgressMonitor monitor)
        throws DBException {
        if (getEntityContainer() == null) {
            log.error("Database object must be entity container to render ERD diagram");
            return null;
        }
        entityDiagram = new EntityDiagram(getEntityContainer(), getEntityContainer().getName());

        // Cache structure
        getEntityContainer().cacheStructure(monitor, DBSEntityContainer.STRUCT_ENTITIES | DBSEntityContainer.STRUCT_ASSOCIATIONS | DBSEntityContainer.STRUCT_ATTRIBUTES);

        // Load entities
        Map<DBSObject, ERDTable> tableMap = new HashMap<DBSObject, ERDTable>();
        Collection<? extends DBSObject> entities = getEntityContainer().getChildren(monitor);
        for (DBSObject entity : entities) {
            if (entity instanceof DBSTable) {
                DBSTable dbsTable = (DBSTable)entity;
                ERDTable table = new ERDTable(dbsTable);

                try {
                    List<DBSTableColumn> idColumns = DBUtils.getBestTableIdentifier(monitor, dbsTable);

                    Collection<? extends DBSTableColumn> columns = dbsTable.getColumns(monitor);
                    for (DBSTableColumn column : columns) {
                        ERDTableColumn c1 = new ERDTableColumn(column, idColumns.contains(column));
                        table.addColumn(c1);
                    }
                } catch (DBException e) {
                    // just skip this problematic columns
                    log.debug("Could not load entity '" + entity.getName() + "'columns", e);
                }
                entityDiagram.addTable(table);
                tableMap.put(entity, table);
            }
        }

        // Load relations
        for (DBSObject entity : entities) {
            if (entity instanceof DBSTable) {
                ERDTable table1 = tableMap.get(entity);
                try {
                    Set<DBSTableColumn> fkColumns = new HashSet<DBSTableColumn>();
                    // Make associations
                    Collection<? extends DBSForeignKey> fks = ((DBSTable) entity).getForeignKeys(monitor);
                    for (DBSForeignKey fk : fks) {
                        fkColumns.addAll(DBUtils.getTableColumns(monitor, fk));
                        ERDTable table2 = tableMap.get(fk.getReferencedKey().getTable());
                        if (table2 == null) {
                            log.warn("Table '" + fk.getReferencedKey().getTable().getFullQualifiedName() + "' not found in ERD");
                        } else {
                            if (table1 != table2) {
                                new ERDAssociation(fk, table2, table1);
                            }
                        }
                    }

                    // Mark column's fk flag
                    for (ERDTableColumn column : table1.getColumns()) {
                        if (fkColumns.contains(column.getObject())) {
                            column.setInForeignKey(true);
                        }
                    }

                } catch (DBException e) {
                    log.warn("Could not load entity '" + entity.getName() + "' foreign keys", e);
                }
            }
        }

        return entityDiagram;
    }

    private void loadContentFromFile(IFileEditorInput input) {
        IFile file = input.getFile();
        try {
            setPartName(file.getName());
            InputStream is = file.getContents(true);
            ObjectInputStream ois = new ObjectInputStream(is);
            entityDiagram = (EntityDiagram) ois.readObject();
            ois.close();
        }
        catch (Exception e) {
            log.error("Error loading diagram from file '" + file.getFullPath().toString() + "'", e);
            entityDiagram = getContent();
        }
    }

    /**
     * Creates a PaletteViewerProvider that will be used to create palettes for
     * the view and the flyout.
     *
     * @return the palette provider
     */
    protected PaletteViewerProvider createPaletteViewerProvider() {
        return new ERDPaletteViewerProvider(editDomain);
    }

    public GraphicalViewer getViewer()
    {
        return super.getGraphicalViewer();
    }

    /**
     * Creates a new <code>GraphicalViewer</code>, configures, registers and
     * initializes it.
     *
     * @param parent the parent composite
     */
    protected void createGraphicalViewer(Composite parent) {
        GraphicalViewer viewer = createViewer(parent);

        GraphicalViewerKeyHandler graphicalViewerKeyHandler = new GraphicalViewerKeyHandler(viewer);
        KeyHandler parentKeyHandler = graphicalViewerKeyHandler.setParent(getCommonKeyHandler());
        viewer.setKeyHandler(parentKeyHandler);

        // hook the viewer into the EditDomain
        setGraphicalViewer(viewer);

        configureGraphicalViewer();
        hookGraphicalViewer();
        initializeGraphicalViewer();

        // Set initial contents
        viewer.setContents(entityDiagram);

        // Set context menu
        ContextMenuProvider provider = new ERDEditorContextMenuProvider(this, getActionRegistry());
        viewer.setContextMenu(provider);
        getSite().registerContextMenu("org.jkiss.dbeaver.ext.erd.editor.contextmenu", provider, viewer);
    }

    private GraphicalViewer createViewer(Composite parent) {
        StatusLineValidationMessageHandler validationMessageHandler = new StatusLineValidationMessageHandler(getEditorSite());
        GraphicalViewer viewer = new ERDGraphicalViewer(validationMessageHandler);
        viewer.createControl(parent);

        // configure the viewer
        viewer.getControl().setBackground(ColorConstants.white);
        rootPart = new ScalableFreeformRootEditPart();
        viewer.setRootEditPart(rootPart);
        viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer));

        viewer.addDropTargetListener(new DataEditDropTargetListener(viewer));

        // initialize the viewer with input
        viewer.setEditPartFactory(new ERDEditPartFactory());

        return viewer;
    }

    @Override
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();

        ZoomManager zoomManager = rootPart.getZoomManager();

        List<String> zoomLevels = new ArrayList<String>(3);
        zoomLevels.add(ZoomManager.FIT_ALL);
        zoomLevels.add(ZoomManager.FIT_WIDTH);
        zoomLevels.add(ZoomManager.FIT_HEIGHT);
        zoomManager.setZoomLevelContributions(zoomLevels);

        zoomManager.setZoomLevels(
            new double[] { .1, .25, .5, .75, 1.0, 1.5, 2.0, 2.5, 3, 4 }
        );

        IAction zoomIn = new ZoomInAction(zoomManager);
        IAction zoomOut = new ZoomOutAction(zoomManager);
        addAction(zoomIn);
        addAction(zoomOut);
    }

    protected KeyHandler getCommonKeyHandler() {

        KeyHandler sharedKeyHandler = new KeyHandler();
        sharedKeyHandler.put(KeyStroke.getPressed(SWT.DEL, 127, 0), getActionRegistry().getAction(
            ActionFactory.DELETE.getId()));
        sharedKeyHandler.put(KeyStroke.getPressed(SWT.F2, 0), getActionRegistry().getAction(
            GEFActionConstants.DIRECT_EDIT));

        return sharedKeyHandler;
    }

    /**
     * Sets the dirty state of this editor.
     * <p/>
     * <p/>
     * An event will be fired immediately if the new state is different than the
     * current one.
     *
     * @param dirty the new dirty state to set
     */
    protected void setDirty(boolean dirty) {
        if (isDirty != dirty) {
            isDirty = dirty;
            firePropertyChange(IEditorPart.PROP_DIRTY);
        }
    }

    /**
     * Creates actions and registers them to the ActionRegistry.
     */
    protected void createActions() {
        addStackAction(new UndoAction(this));
        addStackAction(new RedoAction(this));
        addEditPartAction(new DeleteAction((IWorkbenchPart) this));
        addEditorAction(new SaveAction(this));
        addEditorAction(new PrintAction(this));
    }

    /**
     * Adds an <code>EditPart</code> action to this editor.
     * <p/>
     * <p/>
     * <code>EditPart</code> actions are actions that depend and work on the
     * selected <code>EditPart</code>s.
     *
     * @param action the <code>EditPart</code> action
     */
    protected void addEditPartAction(SelectionAction action) {
        getActionRegistry().registerAction(action);
        editPartActionIDs.add(action.getId());
    }

    /**
     * Adds an <code>CommandStack</code> action to this editor.
     * <p/>
     * <p/>
     * <code>CommandStack</code> actions are actions that depend and work on
     * the <code>CommandStack</code>.
     *
     * @param action the <code>CommandStack</code> action
     */
    protected void addStackAction(StackAction action) {
        getActionRegistry().registerAction(action);
        stackActionIDs.add(action.getId());
    }

    /**
     * Adds an editor action to this editor.
     * <p/>
     * <p/>
     * <Editor actions are actions that depend and work on the editor.
     *
     * @param action the editor action
     */
    protected void addEditorAction(WorkbenchPartAction action) {
        getActionRegistry().registerAction(action);
        editorActionIDs.add(action.getId());
    }

    /**
     * Adds an action to this editor's <code>ActionRegistry</code>. (This is
     * a helper method.)
     *
     * @param action the action to add.
     */
    protected void addAction(IAction action) {
        getActionRegistry().registerAction(action);
        UIUtils.registerKeyBinding(getSite(), action);
    }

    /**
     * Updates the specified actions.
     *
     * @param actionIds the list of ids of actions to update
     */
    protected void updateActions(List actionIds) {
        for (Iterator<?> ids = actionIds.iterator(); ids.hasNext();) {
            IAction action = getActionRegistry().getAction(ids.next());
            if (null != action && action instanceof UpdateAction) {
                ((UpdateAction) action).update();
            }

        }
    }

    /**
     * Returns the overview for the outline view.
     *
     * @return the overview
     */
    protected ERDOutlinePage getOverviewOutlinePage() {
        if (null == outlinePage && null != getGraphicalViewer()) {
            RootEditPart rootEditPart = getGraphicalViewer().getRootEditPart();
            if (rootEditPart instanceof ScalableFreeformRootEditPart) {
                outlinePage = new ERDOutlinePage((ScalableFreeformRootEditPart) rootEditPart);
            }
        }

        return outlinePage;
    }

    /**
     * Returns the undoable <code>PropertySheetPage</code> for this editor.
     *
     * @return the undoable <code>PropertySheetPage</code>
     */
    protected PropertySheetPage getPropertySheetPage() {
        if (null == undoablePropertySheetPage) {
            undoablePropertySheetPage = new PropertySheetPage();
            undoablePropertySheetPage.setRootEntry(new UndoablePropertySheetEntry(getCommandStack()));
        }

        return undoablePropertySheetPage;
    }

    /*
      */

    protected void firePropertyChange(int propertyId) {
        super.firePropertyChange(propertyId);
        updateActions(editorActionIDs);
    }

    /**
     * @return the preferences for the Palette Flyout
     */
    protected FlyoutPreferences getPalettePreferences() {
        return new ERDPalettePreferences();
    }

    /**
     * @return the PaletteRoot to be used with the PaletteViewer
     */
    protected PaletteRoot getPaletteRoot() {
        return createPaletteRoot();
    }

    /**
     * Returns the content of this editor
     *
     * @return the model object
     */
    private EntityDiagram getContent() {
        return new EntityDiagram(null, "Schema");//ContentCreator().getContent();
    }

    DBSEntityContainer getEntityContainer()
    {
        return objectManager.getObject();
    }

    public IDatabaseObjectManager<DBSEntityContainer> getObjectManager() {
        return objectManager;
    }

    public void initObjectEditor(IDatabaseObjectManager<DBSEntityContainer> manager) {
        objectManager = manager;
    }

    public void activatePart() {
        if (isLoaded) {
            return;
        }
        loadDiagram();
        isLoaded = true;
    }

    public void deactivatePart() {

    }

    public void refreshDiagram()
    {
        loadDiagram();
    }

    private void loadDiagram()
    {
        LoadingUtils.executeService(
            new AbstractLoadService<EntityDiagram>("Load schema") {
                public EntityDiagram evaluate()
                    throws InvocationTargetException, InterruptedException {
                    try {
                        return loadFromDatabase(getProgressMonitor());
                    }
                    catch (DBException e) {
                        log.error(e);
                    }

                    return null;
                }
            },
            progressControl.createLoadVisualizer());
    }

    public PaletteRoot createPaletteRoot() {
        // create root
        PaletteRoot paletteRoot = new PaletteRoot();

        // a group of default control tools
        PaletteGroup controls = new PaletteGroup("Controls");
        paletteRoot.add(controls);

        // the selection tool
        ToolEntry selectionTool = new SelectionToolEntry();
        controls.add(selectionTool);

        // use selection tool as default entry
        paletteRoot.setDefaultEntry(selectionTool);

        // the marquee selection tool
        controls.add(new MarqueeToolEntry());

        // a separator
        PaletteSeparator separator = new PaletteSeparator(Activator.PLUGIN_ID + ".palette.separator");
        separator.setUserModificationPermission(PaletteEntry.PERMISSION_NO_MODIFICATION);
        controls.add(separator);

        if (!isReadOnly) {
            controls.add(new ConnectionCreationToolEntry("Connections", "Create Connections", null,
                Activator.getImageDescriptor("icons/relationship.gif"),
                Activator.getImageDescriptor("icons/relationship.gif")));

            PaletteDrawer drawer = new PaletteDrawer("New Component",
                Activator.getImageDescriptor("icons/connection.gif"));

            List<CombinedTemplateCreationEntry> entries = new ArrayList<CombinedTemplateCreationEntry>();

            CombinedTemplateCreationEntry tableEntry = new CombinedTemplateCreationEntry("New Table", "Create a new table",
                ERDTable.class, new DataElementFactory(ERDTable.class),
                Activator.getImageDescriptor("icons/table.gif"),
                Activator.getImageDescriptor("icons/table.gif"));

            CombinedTemplateCreationEntry columnEntry = new CombinedTemplateCreationEntry("New Column", "Add a new column",
                ERDTableColumn.class, new DataElementFactory(ERDTableColumn.class),
                Activator.getImageDescriptor("icons/column.gif"),
                Activator.getImageDescriptor("icons/column.gif"));

            entries.add(tableEntry);
            entries.add(columnEntry);

            drawer.addAll(entries);

            paletteRoot.add(drawer);
        }

        return paletteRoot;

    }

    public DBPDataSource getDataSource() {
        return objectManager.getDataSource();
    }

    private class ProgressControl extends ProgressPageControl {

        private ToolBarManager toolBarManager;
        private ZoomComboContributionItem zoomCombo;

        private ProgressControl(Composite parent, int style, IWorkbenchPart workbenchPart) {
            super(parent, style, workbenchPart);
        }

        @Override
        public void dispose() {
            super.dispose();
        }

        protected int getProgressCellCount() {
            return 3;
        }

        @Override
        protected Composite createProgressPanel(Composite container) {
            Composite infoGroup = super.createProgressPanel(container);

            ZoomManager zoomManager = rootPart.getZoomManager();

            toolBarManager = new ToolBarManager();

            String[] zoomStrings = new String[] {
                ZoomManager.FIT_ALL,
                ZoomManager.FIT_HEIGHT,
                ZoomManager.FIT_WIDTH
            };
            zoomCombo = new ZoomComboContributionItem(ERDEditor.this.getSite().getPage(), zoomStrings);
            toolBarManager.add(zoomCombo);

            //toolBarManager.add(new UndoAction(ERDEditor.this));
            //toolBarManager.add(new RedoAction(ERDEditor.this));
            //toolBarManager.add(new PrintAction(ERDEditor.this));
            toolBarManager.add(new Separator());
            toolBarManager.add(new ZoomInAction(zoomManager));
            toolBarManager.add(new ZoomOutAction(zoomManager));
            toolBarManager.add(new DiagramLayoutAction(ERDEditor.this));
            toolBarManager.add(new DiagramRefreshAction(ERDEditor.this));
            toolBarManager.add(new Separator());
            {
                PrintAction printAction = new PrintAction(ERDEditor.this);
                printAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ETOOL_PRINT_EDIT));
                toolBarManager.add(printAction);
            }

            toolBarManager.createControl(infoGroup);
            toolBarManager.getControl().setEnabled(false);

            return infoGroup;
        }

        public ProgressVisualizer<EntityDiagram> createLoadVisualizer() {
            return new LoadVisualizer();
        }

        private class LoadVisualizer extends ProgressVisualizer<EntityDiagram> {
            @Override
            public void completeLoading(EntityDiagram entityDiagram) {
                super.completeLoading(entityDiagram);
                if (entityDiagram != null) {
                    setInfo(entityDiagram.getEntityCount() + " objects");
                } else {
                    setInfo("Error");
                }
                getGraphicalViewer().setContents(entityDiagram);
                zoomCombo.setZoomManager(rootPart.getZoomManager());
                toolBarManager.getControl().setEnabled(true);
            }
        }

    }

}