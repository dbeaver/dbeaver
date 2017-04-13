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
package org.jkiss.dbeaver.ext.erd.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.*;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.palette.*;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.ui.actions.*;
import org.eclipse.gef.ui.palette.FlyoutPaletteComposite.FlyoutPreferences;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.gef.ui.properties.UndoablePropertySheetEntry;
import org.eclipse.jface.action.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.printing.PrintDialog;
import org.eclipse.swt.printing.Printer;
import org.eclipse.swt.printing.PrinterData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.*;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheetPage;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.erd.ERDActivator;
import org.jkiss.dbeaver.ext.erd.ERDConstants;
import org.jkiss.dbeaver.ext.erd.action.DiagramLayoutAction;
import org.jkiss.dbeaver.ext.erd.action.DiagramRefreshAction;
import org.jkiss.dbeaver.ext.erd.action.DiagramToggleGridAction;
import org.jkiss.dbeaver.ext.erd.action.ERDEditorPropertyTester;
import org.jkiss.dbeaver.ext.erd.directedit.StatusLineValidationMessageHandler;
import org.jkiss.dbeaver.ext.erd.dnd.DataEditDropTargetListener;
import org.jkiss.dbeaver.ext.erd.dnd.NodeDropTargetListener;
import org.jkiss.dbeaver.ext.erd.model.ERDNote;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.ext.erd.part.DiagramPart;
import org.jkiss.dbeaver.model.DBPDataSourceUser;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.controls.itemlist.ObjectSearcher;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

import java.io.FileOutputStream;
import java.util.*;

/**
 * Editor implementation based on the the example editor skeleton that is built in <i>Building
 * an editor </i> in chapter <i>Introduction to GEF </i>
 */
public abstract class ERDEditorPart extends GraphicalEditorWithFlyoutPalette
    implements DBPDataSourceUser, ISearchContextProvider, IRefreshablePart
{
    static final Log log = Log.getLog(ERDEditorPart.class);

    protected ProgressControl progressControl;

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
    private List<String> editPartActionIDs = new ArrayList<>();

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

    private boolean isLoaded;

    protected LoadingJob<EntityDiagram> diagramLoadingJob;
    private IPropertyChangeListener configPropertyListener;
    private PaletteRoot paletteRoot;

    /**
     * No-arg constructor
     */
    protected ERDEditorPart()
    {
    }

    /**
     * Initializes the editor.
     */
    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException
    {
        editDomain = new DefaultEditDomain(this);
        setEditDomain(editDomain);

        super.init(site, input);

        // add selection change listener
        //getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);

        configPropertyListener = new ConfigPropertyListener();
        ERDActivator.getDefault().getPreferenceStore().addPropertyChangeListener(configPropertyListener);
    }

    @Override
    public void createPartControl(Composite parent)
    {
        progressControl = new ProgressControl(parent, SWT.SHEET);
        progressControl.setShowDivider(true);

        Composite contentContainer = progressControl.createContentContainer();
        super.createPartControl(contentContainer);

        progressControl.createProgressPanel();
    }

    /**
     * The <code>CommandStackListener</code> that listens for
     * <code>CommandStack </code> changes.
     */
    @Override
    public void commandStackChanged(EventObject event)
    {
        // Reevaluate properties
        ActionUtils.evaluatePropertyState(ERDEditorPropertyTester.NAMESPACE + "." + ERDEditorPropertyTester.PROP_CAN_UNDO);
        ActionUtils.evaluatePropertyState(ERDEditorPropertyTester.NAMESPACE + "." + ERDEditorPropertyTester.PROP_CAN_REDO);

        // Update actions
        setDirty(getCommandStack().isDirty());

        super.commandStackChanged(event);
    }

    @Override
    public void dispose()
    {
        ERDActivator.getDefault().getPreferenceStore().removePropertyChangeListener(configPropertyListener);

        if (diagramLoadingJob != null) {
            diagramLoadingJob.cancel();
            diagramLoadingJob = null;
        }
        // remove selection listener
        //getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(this);
        // dispose the ActionRegistry (will dispose all actions)
        getActionRegistry().dispose();
        // important: always call super implementation of dispose
        super.dispose();
    }

    /**
     * Adaptable implementation for Editor
     */
    @Override
    public Object getAdapter(Class adapter)
    {
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
        } else if (IWorkbenchAdapter.class.equals(adapter)) {
            return new WorkbenchAdapter() {
                @Override
                public String getLabel(Object o)
                {
                    return "ERD Editor";
                }
            };
        }
        // the super implementation handles the rest
        return super.getAdapter(adapter);
    }

    @Override
    public void doSave(IProgressMonitor monitor)
    {

    }

    /**
     * Save as not allowed
     */
    @Override
    public void doSaveAs()
    {
        saveDiagramAs();
    }

    /**
     * Save as not allowed
     */
    @Override
    public boolean isSaveAsAllowed()
    {
        return true;
    }

    /**
     * Indicates if the editor has unsaved changes.
     *
     * @see org.eclipse.ui.part.EditorPart#isDirty
     */
    @Override
    public boolean isDirty()
    {
        return !isReadOnly() && isDirty;
    }

    public abstract boolean isReadOnly();

    /**
     * Returns the <code>CommandStack</code> of this editor's
     * <code>EditDomain</code>.
     *
     * @return the <code>CommandStack</code>
     */
    @Override
    public CommandStack getCommandStack()
    {
        return getEditDomain().getCommandStack();
    }

    /**
     * Returns the schema model associated with the editor
     *
     * @return an instance of <code>Schema</code>
     */
    public EntityDiagram getDiagram()
    {
        return getDiagramPart().getDiagram();
    }

    public DiagramPart getDiagramPart()
    {
        return rootPart == null ? null : (DiagramPart) rootPart.getContents();
    }

    /**
     * @see org.eclipse.ui.part.EditorPart#setInput(org.eclipse.ui.IEditorInput)
     */
    @Override
    protected void setInput(IEditorInput input)
    {
        super.setInput(input);
    }

    /**
     * Creates a PaletteViewerProvider that will be used to create palettes for
     * the view and the flyout.
     *
     * @return the palette provider
     */
    @Override
    protected PaletteViewerProvider createPaletteViewerProvider()
    {
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
    @Override
    protected void createGraphicalViewer(Composite parent)
    {
        GraphicalViewer viewer = createViewer(parent);

        // hook the viewer into the EditDomain
        setGraphicalViewer(viewer);

        configureGraphicalViewer();
        hookGraphicalViewer();
        initializeGraphicalViewer();

        // Set initial (empty) contents
        viewer.setContents(new EntityDiagram(null, "empty"));

        // Set context menu
        ContextMenuProvider provider = new ERDEditorContextMenuProvider(this);
        viewer.setContextMenu(provider);
        getSite().registerContextMenu(ERDEditorPart.class.getName() + ".EditorContext", provider, viewer);
    }

    private GraphicalViewer createViewer(Composite parent)
    {
        StatusLineValidationMessageHandler validationMessageHandler = new StatusLineValidationMessageHandler(getEditorSite());
        GraphicalViewer viewer = new ERDGraphicalViewer(this, validationMessageHandler);
        viewer.createControl(parent);

        // configure the viewer
        viewer.getControl().setBackground(ColorConstants.white);
        rootPart = new ScalableFreeformRootEditPart();
        viewer.setRootEditPart(rootPart);
        viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer));

        viewer.addDropTargetListener(new DataEditDropTargetListener(viewer));
        viewer.addDropTargetListener(new NodeDropTargetListener(viewer));

        // initialize the viewer with input
        viewer.setEditPartFactory(new ERDEditPartFactory());

        return viewer;
    }

    @Override
    protected void configureGraphicalViewer()
    {
        super.configureGraphicalViewer();

        GraphicalViewer graphicalViewer = getGraphicalViewer();

/*
        MenuManager manager = new MenuManager(getClass().getName(), getClass().getName());
        manager.setRemoveAllWhenShown(true);
        getEditorSite().registerContextMenu(getClass().getName() + ".EditorContext", manager, graphicalViewer, true); //$NON-NLS-1$
*/

        IPreferenceStore store = ERDActivator.getDefault().getPreferenceStore();

        graphicalViewer.setProperty(SnapToGrid.PROPERTY_GRID_ENABLED, store.getBoolean(ERDConstants.PREF_GRID_ENABLED));
        graphicalViewer.setProperty(SnapToGrid.PROPERTY_GRID_VISIBLE, store.getBoolean(ERDConstants.PREF_GRID_ENABLED));
        graphicalViewer.setProperty(SnapToGrid.PROPERTY_GRID_SPACING, new Dimension(
            store.getInt(ERDConstants.PREF_GRID_WIDTH),
            store.getInt(ERDConstants.PREF_GRID_HEIGHT)));

        // initialize actions
        createActions();

        // Setup zoom manager
        ZoomManager zoomManager = rootPart.getZoomManager();

        List<String> zoomLevels = new ArrayList<>(3);
        zoomLevels.add(ZoomManager.FIT_ALL);
        zoomLevels.add(ZoomManager.FIT_WIDTH);
        zoomLevels.add(ZoomManager.FIT_HEIGHT);
        zoomManager.setZoomLevelContributions(zoomLevels);

        zoomManager.setZoomLevels(
            new double[]{.1, .25, .5, .75, 1.0, 1.5, 2.0, 2.5, 3, 4}
        );

        IAction zoomIn = new ZoomInAction(zoomManager);
        IAction zoomOut = new ZoomOutAction(zoomManager);
        addAction(zoomIn);
        addAction(zoomOut);

        graphicalViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event)
            {
                String status;
                IStructuredSelection selection = (IStructuredSelection)event.getSelection();
                if (selection.isEmpty()) {
                    status = "";
                } else if (selection.size() == 1) {
                    status = CommonUtils.toString(selection.getFirstElement());
                } else {
                    status = String.valueOf(selection.size()) + " objects";
                }
                progressControl.setInfo(status);

                updateActions(editPartActionIDs);
            }
        });
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
    protected void setDirty(boolean dirty)
    {
        if (isDirty != dirty) {
            isDirty = dirty;
            firePropertyChange(IEditorPart.PROP_DIRTY);
        }
    }

    /**
     * Adds an action to this editor's <code>ActionRegistry</code>. (This is
     * a helper method.)
     *
     * @param action the action to add.
     */
    protected void addAction(IAction action)
    {
        getActionRegistry().registerAction(action);
        UIUtils.registerKeyBinding(getSite(), action);
    }

    /**
     * Updates the specified actions.
     *
     * @param actionIds the list of ids of actions to update
     */
    @Override
    protected void updateActions(List actionIds)
    {
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
    protected ERDOutlinePage getOverviewOutlinePage()
    {
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
    protected PropertySheetPage getPropertySheetPage()
    {
        if (null == undoablePropertySheetPage) {
            undoablePropertySheetPage = new PropertySheetPage();
            undoablePropertySheetPage.setRootEntry(new UndoablePropertySheetEntry(getCommandStack()));
        }

        return undoablePropertySheetPage;
    }

    /**
     * @return the preferences for the Palette Flyout
     */
    @Override
    protected FlyoutPreferences getPalettePreferences()
    {
        return new ERDPalettePreferences();
    }

    /**
     * @return the PaletteRoot to be used with the PaletteViewer
     */
    @Override
    protected PaletteRoot getPaletteRoot()
    {
        if (paletteRoot == null) {
            paletteRoot = createPaletteRoot();
        }
        return paletteRoot;
    }

    public PaletteRoot createPaletteRoot()
    {
        // create root
        PaletteRoot paletteRoot = new PaletteRoot();
        paletteRoot.setLabel("Entity Diagram");

        {
            // a group of default control tools
            PaletteDrawer controls = new PaletteDrawer("Tools", DBeaverIcons.getImageDescriptor(UIIcon.CONFIGURATION));

            paletteRoot.add(controls);

            // the selection tool
            ToolEntry selectionTool = new SelectionToolEntry();
            controls.add(selectionTool);

            // use selection tool as default entry
            paletteRoot.setDefaultEntry(selectionTool);

            // the marquee selection tool
            controls.add(new MarqueeToolEntry());


            if (!isReadOnly()) {
                // separator
                PaletteSeparator separator = new PaletteSeparator("tools");
                separator.setUserModificationPermission(PaletteEntry.PERMISSION_NO_MODIFICATION);
                controls.add(separator);

                final ImageDescriptor connectImage = ERDActivator.getImageDescriptor("icons/connect.png");
                controls.add(new ConnectionCreationToolEntry("Connection", "Create Connection", null, connectImage, connectImage));

                final ImageDescriptor noteImage = ERDActivator.getImageDescriptor("icons/note.png");
                controls.add(new CreationToolEntry(
                    "Note",
                    "Create Note",
                    new CreationFactory() {
                        @Override
                        public Object getNewObject()
                        {
                            return new ERDNote("Note");
                        }
                        @Override
                        public Object getObjectType()
                        {
                            return RequestConstants.REQ_CREATE;
                        }
                    },
                    noteImage,
                    noteImage));
            }
        }

/*
            PaletteDrawer drawer = new PaletteDrawer("New Component",
                ERDActivator.getImageDescriptor("icons/connection.gif"));

            List<CombinedTemplateCreationEntry> entries = new ArrayList<CombinedTemplateCreationEntry>();

            CombinedTemplateCreationEntry tableEntry = new CombinedTemplateCreationEntry("New Table", "Create a new table",
                ERDEntity.class, new DataElementFactory(ERDEntity.class),
                ERDActivator.getImageDescriptor("icons/table.gif"),
                ERDActivator.getImageDescriptor("icons/table.gif"));

            CombinedTemplateCreationEntry columnEntry = new CombinedTemplateCreationEntry("New Column", "Add a new column",
                ERDEntityAttribute.class, new DataElementFactory(ERDEntityAttribute.class),
                ERDActivator.getImageDescriptor("icons/column.gif"),
                ERDActivator.getImageDescriptor("icons/column.gif"));

            entries.add(tableEntry);
            entries.add(columnEntry);

            drawer.addAll(entries);

            paletteRoot.add(drawer);
*/

        return paletteRoot;

    }

    public boolean isLoaded()
    {
        return isLoaded;
    }

    public void refreshDiagram()
    {
        if (isLoaded) {
            loadDiagram();
        }
    }

    @Override
    public void refreshPart(Object source, boolean force)
    {
        refreshDiagram();
    }

    public void saveDiagramAs()
    {
        final Shell shell = getSite().getShell();
        FileDialog saveDialog = new FileDialog(shell, SWT.SAVE);
        saveDialog.setFilterExtensions(new String[]{"*.png", "*.gif", "*.bmp", "*.graphml"});
        saveDialog.setFilterNames(new String[]{
            "PNG format (*.png)",
            "GIF format (*.gif)",
//            "JPEG format (*.jpg)",
            "Bitmap format (*.bmp)",
            "GraphML (*.graphml)"
        });

        String filePath = DialogUtils.openFileDialog(saveDialog);
        if (filePath == null || filePath.trim().length() == 0) {
            return;
        }

        int imageType = SWT.IMAGE_BMP;
        if (filePath.toLowerCase().endsWith(".jpg")) {
            imageType = SWT.IMAGE_JPEG;
        } else if (filePath.toLowerCase().endsWith(".png")) {
            imageType = SWT.IMAGE_PNG;
        } else if (filePath.toLowerCase().endsWith(".gif")) {
            imageType = SWT.IMAGE_GIF;
        } else if (filePath.toLowerCase().endsWith(".graphml")) {
            new ERDExportGraphML(getDiagram(), getDiagramPart()).exportDiagramToGraphML(filePath);
            return;
        }

        IFigure figure = rootPart.getLayer(ScalableFreeformRootEditPart.PRINTABLE_LAYERS);
        Rectangle contentBounds = figure instanceof FreeformLayeredPane ? ((FreeformLayeredPane) figure).getFreeformExtent() : figure.getBounds();
        try {
            FileOutputStream fos = new FileOutputStream(filePath);
            try {
                Rectangle r = figure.getBounds();
                GC gc = null;
                Graphics g = null;
                try {
                    Image image = new Image(null, contentBounds.x * 2 + contentBounds.width, contentBounds.y * 2 + contentBounds.height);
                    try {
                        gc = new GC(image);
                        gc.setClipping(contentBounds.x, contentBounds.y, contentBounds.width, contentBounds.height);
                        g = new SWTGraphics(gc);
                        g.translate(r.x * -1, r.y * -1);
                        figure.paint(g);
                        ImageLoader imageLoader = new ImageLoader();
                        imageLoader.data = new ImageData[1];
                        if (imageType != SWT.IMAGE_JPEG) {
                            // Convert to 8bit color
                            imageLoader.data[0] = ImageUtils.makeWebImageData(image);
                        } else {
                            // Use maximum colors for JPEG
                            imageLoader.data[0] = image.getImageData();
                        }
                        imageLoader.save(fos, imageType);
                    } finally {
                        UIUtils.dispose(image);
                    }
                } finally {
                    if (g != null) {
                        g.dispose();
                    }
                    UIUtils.dispose(gc);
                }

                fos.flush();
            } finally {
                ContentUtils.close(fos);
            }
            RuntimeUtils.launchProgram(filePath);
            //UIUtils.showMessageBox(shell, "Save ERD", "Diagram has been exported to " + filePath, SWT.ICON_INFORMATION);
        } catch (Exception e) {
            UIUtils.showErrorDialog(getSite().getShell(), "Save ERD as image", null, e);
        }

    }

    public MenuManager createAttributeVisibilityMenu()
    {
        MenuManager avMenu = new MenuManager("Show Attributes");
        avMenu.add(new ChangeAttributeVisibilityAction(ERDAttributeVisibility.ALL));
        avMenu.add(new ChangeAttributeVisibilityAction(ERDAttributeVisibility.KEYS));
        avMenu.add(new ChangeAttributeVisibilityAction(ERDAttributeVisibility.PRIMARY));
        avMenu.add(new ChangeAttributeVisibilityAction(ERDAttributeVisibility.NONE));
        return avMenu;
    }

    public void printDiagram()
    {
        GraphicalViewer viewer = getGraphicalViewer();

        PrintDialog dialog = new PrintDialog(viewer.getControl().getShell(), SWT.NULL);
        PrinterData data = dialog.open();

        if (data != null) {
            IFigure rootFigure = rootPart.getLayer(ScalableFreeformRootEditPart.PRINTABLE_LAYERS);
            //EntityDiagramFigure diagramFigure = findFigure(rootFigure, EntityDiagramFigure.class);
            if (rootFigure != null) {
                PrintFigureOperation printOp = new PrintFigureOperation(new Printer(data), rootFigure);

                // Set print preferences
                IPreferenceStore store = ERDActivator.getDefault().getPreferenceStore();
                printOp.setPrintMode(store.getInt(ERDConstants.PREF_PRINT_PAGE_MODE));
                printOp.setPrintMargin(new Insets(
                    store.getInt(ERDConstants.PREF_PRINT_MARGIN_TOP),
                    store.getInt(ERDConstants.PREF_PRINT_MARGIN_LEFT),
                    store.getInt(ERDConstants.PREF_PRINT_MARGIN_BOTTOM),
                    store.getInt(ERDConstants.PREF_PRINT_MARGIN_RIGHT)
                ));
                // Run print
                printOp.run("Print ER diagram");
            }
        }
        //new PrintAction(this).run();
    }

    @Override
    public boolean isSearchPossible()
    {
        return true;
    }

    @Override
    public boolean isSearchEnabled()
    {
        return progressControl != null && progressControl.isSearchEnabled();
    }

    @Override
    public boolean performSearch(SearchType searchType)
    {
        return progressControl != null && progressControl.performSearch(searchType);
    }

    protected abstract void loadDiagram();

    private class ChangeAttributeVisibilityAction extends Action {
        private final ERDAttributeVisibility visibility;

        private ChangeAttributeVisibilityAction(ERDAttributeVisibility visibility)
        {
            super(visibility.getTitle(), IAction.AS_RADIO_BUTTON);
            this.visibility = visibility;
        }

        @Override
        public boolean isChecked()
        {
            return visibility == getDiagram().getAttributeVisibility();
        }

        @Override
        public void run()
        {
            getDiagram().setAttributeVisibility(visibility);
            refreshDiagram();
            //this.setChecked(true);
        }
    }

    private class ConfigPropertyListener implements IPropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent event)
        {
            GraphicalViewer graphicalViewer = getGraphicalViewer();
            if (graphicalViewer == null) {
                return;
            }
            if (ERDConstants.PREF_GRID_ENABLED.equals(event.getProperty())) {
                Boolean enabled = Boolean.valueOf(event.getNewValue().toString());
                graphicalViewer.setProperty(SnapToGrid.PROPERTY_GRID_ENABLED, enabled);
                graphicalViewer.setProperty(SnapToGrid.PROPERTY_GRID_VISIBLE, enabled);
            } else if (ERDConstants.PREF_GRID_WIDTH.equals(event.getProperty()) || ERDConstants.PREF_GRID_HEIGHT.equals(event.getProperty())) {
                final IPreferenceStore store = ERDActivator.getDefault().getPreferenceStore();
                graphicalViewer.setProperty(SnapToGrid.PROPERTY_GRID_SPACING, new Dimension(
                    store.getInt(ERDConstants.PREF_GRID_WIDTH),
                    store.getInt(ERDConstants.PREF_GRID_HEIGHT)));
            }
        }
    }

    protected class ProgressControl extends ProgressPageControl {

        private Searcher searcher;
        private ZoomComboContributionItem zoomCombo;

        private ProgressControl(Composite parent, int style)
        {
            super(parent, style);
            searcher = new Searcher();
        }

        @Override
        protected boolean cancelProgress()
        {
            if (diagramLoadingJob != null) {
                diagramLoadingJob.cancel();
                return true;
            }
            return false;
        }

        public ProgressVisualizer<EntityDiagram> createLoadVisualizer()
        {
            getGraphicalControl().setBackground(ColorConstants.lightGray);
            return new LoadVisualizer();
        }

        @Override
        protected void fillCustomToolbar(ToolBarManager toolBarManager) {
            ZoomManager zoomManager = rootPart.getZoomManager();

            String[] zoomStrings = new String[]{
                ZoomManager.FIT_ALL,
                ZoomManager.FIT_HEIGHT,
                ZoomManager.FIT_WIDTH
            };
            // Init zoom combo with dummy part service
            // to prevent zoom disable on part change - as it is standalone zoom control, not global one
            zoomCombo = new ZoomComboContributionItem(
                new IPartService() {
                    @Override
                    public void addPartListener(IPartListener listener)
                    {
                    }

                    @Override
                    public void addPartListener(IPartListener2 listener)
                    {
                    }

                    @Override
                    public IWorkbenchPart getActivePart()
                    {
                        return ERDEditorPart.this;
                    }

                    @Override
                    public IWorkbenchPartReference getActivePartReference()
                    {
                        return null;
                    }

                    @Override
                    public void removePartListener(IPartListener listener)
                    {
                    }

                    @Override
                    public void removePartListener(IPartListener2 listener)
                    {
                    }
                },
                zoomStrings);
            toolBarManager.add(zoomCombo);

            //toolBarManager.add(new UndoAction(ERDEditorPart.this));
            //toolBarManager.add(new RedoAction(ERDEditorPart.this));
            //toolBarManager.add(new PrintAction(ERDEditorPart.this));

            toolBarManager.add(new ZoomInAction(zoomManager));
            toolBarManager.add(new ZoomOutAction(zoomManager));
            toolBarManager.add(new Separator());
            //toolBarManager.add(createAttributeVisibilityMenu());
            toolBarManager.add(new DiagramLayoutAction(ERDEditorPart.this));
            toolBarManager.add(new DiagramToggleGridAction());
            toolBarManager.add(new DiagramRefreshAction(ERDEditorPart.this));
            toolBarManager.add(new Separator());
            {
                toolBarManager.add(ActionUtils.makeCommandContribution(
                        getSite(),
                        IWorkbenchCommandConstants.FILE_SAVE_AS,
                        "Save diagram in external format",
                        UIIcon.PICTURE_SAVE));
                toolBarManager.add(ActionUtils.makeCommandContribution(
                        getSite(),
                        IWorkbenchCommandConstants.FILE_PRINT,
                        "Print Diagram",
                        UIIcon.PRINT));
            }
            {
                Action configAction = new Action("Configuration") {
                    @Override
                    public void run()
                    {
                        UIUtils.showPreferencesFor(
                            getSite().getShell(),
                            ERDEditorPart.this,
                            ERDPreferencePage.PAGE_ID);
                    }
                };
                configAction.setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.CONFIGURATION));
                toolBarManager.add(configAction);
            }
        }

        @Override
        protected ISearchExecutor getSearchRunner()
        {
            return searcher;
        }

        private class LoadVisualizer extends ProgressVisualizer<EntityDiagram> {
            @Override
            public void visualizeLoading()
            {
                super.visualizeLoading();
            }

            @Override
            public void completeLoading(EntityDiagram entityDiagram)
            {
                super.completeLoading(entityDiagram);
                Control graphicalControl = getGraphicalControl();
                if (graphicalControl == null) {
                    return;
                }
                graphicalControl.setBackground(ColorConstants.white);
                isLoaded = true;
                Control control = getGraphicalViewer().getControl();
                if (control == null || control.isDisposed()) {
                    return;
                }

                if (entityDiagram != null) {
                    List<String> errorMessages = entityDiagram.getErrorMessages();
                    if (!errorMessages.isEmpty()) {
                        // log.debug(message);
                        List<Status> messageStatuses = new ArrayList<>(errorMessages.size());
                        for (String error : errorMessages) {
                            messageStatuses.add(new Status(Status.ERROR, DBeaverCore.getCorePluginID(), error));
                        }
                        MultiStatus status = new MultiStatus(DBeaverCore.getCorePluginID(), 0, messageStatuses.toArray(new IStatus[messageStatuses.size()]), null, null);

                        UIUtils.showErrorDialog(
                            control.getShell(),
                            "Diagram loading errors",
                            "Error(s) occurred during diagram loading. If these errors are recoverable then fix errors and then refresh/reopen diagram",
                            status);
                    }
                    setInfo(entityDiagram.getEntityCount() + " objects");
                } else {
                    setInfo("Empty diagram due to error (see error log)");
                }
                getCommandStack().flush();
                getGraphicalViewer().setContents(entityDiagram);
                zoomCombo.setZoomManager(rootPart.getZoomManager());
                //toolBarManager.getControl().setEnabled(true);
            }
        }

    }

    private class Searcher extends ObjectSearcher<DBPNamedObject> {

        @Override
        protected void setInfo(String message)
        {
            progressControl.setInfo(message);
        }

        @Override
        protected Collection<DBPNamedObject> getContent()
        {
            return getDiagramPart().getChildren();
        }

        @Override
        protected void selectObject(DBPNamedObject object)
        {
            if (object == null) {
                getGraphicalViewer().deselectAll();
            } else {
                getGraphicalViewer().select((EditPart)object);
            }
        }

        @Override
        protected void updateObject(DBPNamedObject object)
        {
        }

        @Override
        protected void revealObject(DBPNamedObject object)
        {
            getGraphicalViewer().reveal((EditPart)object);
        }
    }


}
