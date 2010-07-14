package org.jkiss.dbeaver.ext.erd.views;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.jgraph.JGraph;
import org.jgraph.event.GraphSelectionEvent;
import org.jgraph.event.GraphSelectionListener;
import org.jgraph.graph.ConnectionSet;
import org.jgraph.graph.GraphConstants;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.erd.layout.Layouter;
import org.jkiss.dbeaver.ext.erd.model.ERDLink;
import org.jkiss.dbeaver.ext.erd.model.ERDModel;
import org.jkiss.dbeaver.ext.erd.model.ERDNode;
import org.jkiss.dbeaver.ext.erd.model.ERDTable;
import org.jkiss.dbeaver.ext.erd.sugiyama.SugiyamaEdge;
import org.jkiss.dbeaver.ext.erd.sugiyama.SugiyamaLayouter;
import org.jkiss.dbeaver.ext.erd.sugiyama.SugiyamaNode;
import org.jkiss.dbeaver.ext.ui.IObjectEditor;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSForeignKey;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSStructureContainer;
import org.jkiss.dbeaver.model.struct.DBSTable;
import org.jkiss.dbeaver.runtime.load.AbstractLoadService;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * ERDEditor
 */
public class ERDEditor extends EditorPart implements IObjectEditor
{
    static final Log log = LogFactory.getLog(ERDEditor.class);

    private DBSStructureContainer container;
    private ERDModel graphModel;
    private boolean loaded = false;

    //private ScrolledComposite scroller;
    private Composite graphContainer;
    private ProgressControl progressControl;
    private JGraph graph;
    private Graphics graphics;

    public void doSave(IProgressMonitor monitor)
    {
    }

    public void doSaveAs()
    {
    }

    public void init(IEditorSite site, IEditorInput input)
            throws PartInitException {
        setSite(site);
        setInput(input);
    }

    @Override
    public void dispose()
    {
        progressControl.dispose();
        super.dispose();
    }

    public boolean isDirty()
    {
        return false;
    }

    public boolean isSaveAsAllowed()
    {
        return false;
    }

    public void createPartControl(Composite parent)
    {
/*
        scroller = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        scroller.setLayout(new FillLayout());
        scroller.setExpandHorizontal(true);
        scroller.setExpandVertical(true);
        scroller.setAlwaysShowScrollBars(true);
        //scroller.setBackground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_RED));
        
*/
        progressControl = new ProgressControl(parent, SWT.NONE, this.getSite().getPart());

        graphContainer = new Composite(progressControl, SWT.EMBEDDED);
        graphContainer.setLayout(new GridLayout(1, true));
        GridData gd = new GridData(GridData.FILL_BOTH);
        graphContainer.setLayoutData(gd);
        //graphContainer.setBackground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_GREEN));
        //scroller.setContent(graphContainer);

        progressControl.createProgressPanel();
    }

    public void setFocus()
    {
    }

    public void activatePart()
    {
        if (!loaded) {
            graphModel = new ERDModel();

            graph = new JGraph(graphModel);
            // Control-drag should clone selection
            graph.setCloneable(false);
            // Enable edit without final RETURN keystroke
            graph.setInvokesStopCellEditing(true);
            // When over a cell, jump to its default port (we only have one, anyway)
            graph.setJumpToDefaultPort(true);

            graph.getGraphLayoutCache().setFactory(new ERDViewFactory());

            // Create awt frame
            java.awt.Frame graphFrame = SWT_AWT.new_Frame(graphContainer);
            graphFrame.add(graph);

            // Add the Graph as Center Component
            graphFrame.add(new JScrollPane(graph), java.awt.BorderLayout.CENTER);
            graph.addGraphSelectionListener(new GraphSelectionListener() {
                public void valueChanged(GraphSelectionEvent e)
                {
                    changeSelection(e.getCell());
                }
            });
            this.graphics = graphFrame.getGraphics();

            LoadingUtils.executeService(
                new AbstractLoadService<Object>("Load graph") {
                    public Object evaluate()
                        throws InvocationTargetException, InterruptedException
                    {
                        try {
                            loadModel(getProgressMonitor());
                        }
                        catch (DBException e) {
                            log.error(e);
                        }

                        loaded = true;
                        return null;
                    }
                },
                progressControl.createVisualizer());
        }
    }

    private void changeSelection(final Object cell)
    {
        // Execute it in UI thread beacuse jgraph events are in awt thread
        getSite().getShell().getDisplay().asyncExec(
            new Runnable() {
                public void run()
                {
                    if (cell instanceof ERDNode) {
                        progressControl.setInfo(((ERDNode)cell).getId());
                    } else if (cell == null) {
                        progressControl.setInfo("");
                    } else {
                        progressControl.setInfo(cell.toString());
                    }
                }
            });
    }

    public void deactivatePart()
    {
    }

    public DBPObject getObject()
    {
        return container;
    }

    public void setObject(DBPObject object)
    {
        if (!(object instanceof DBSStructureContainer)) {
            throw new IllegalArgumentException("object must be of type " + DBSStructureContainer.class);
        }
        container = (DBSStructureContainer)object;
    }

    private void loadModel(DBRProgressMonitor monitor)
        throws DBException
    {
        Map attributes = new Hashtable();
        ConnectionSet cs = new ConnectionSet();

        List<ERDTable> tables = new ArrayList<ERDTable>();
        List<ERDLink> links = new ArrayList<ERDLink>();
        container.cacheStructure(
            monitor,
            DBSStructureContainer.STRUCT_ENTITIES | DBSStructureContainer.STRUCT_RELATIONS | DBSStructureContainer.STRUCT_ATTRIBUTES);
        if (container instanceof DBSTable) {
            tables.add(new ERDTable((DBSTable)container));
        } else {
            // Add content
            for (DBSObject node : container.getChildren(monitor)) {
                if (node instanceof DBSTable) {
                    tables.add(new ERDTable((DBSTable)node));
                }
            }
        }

        // Layot tables
        layoutTables(monitor, tables);

        // Add links
        for (ERDTable table : tables) {
            for (DBSForeignKey foreignKey : table.getTable().getForeignKeys(monitor)) {
                DBSTable refTable = foreignKey.getReferencedKey().getTable();

                ERDTable refTableNode = null;
                for (ERDTable tmp : tables) {
                    if (tmp.getTable() == refTable) {
                        refTableNode = tmp;
                        break;
                    }
                }
                if (refTableNode == null) {
                    log.debug("Can't find model node for table " + refTable.getFullQualifiedName());
                    continue;
                }

                // Create Edge
                ERDLink edge = new ERDLink(foreignKey, refTableNode, table);
                // Fetch the ports from the new vertices, and connect them with the edge
                cs.connect(edge, refTableNode.getChildAt(0), table.getChildAt(0));
                //edge.setSource(table);
                //edge.setTarget(refTableNode);
                int arrow = GraphConstants.ARROW_CIRCLE;
                GraphConstants.setLineEnd(edge.getAttributes(), arrow);
                GraphConstants.setEndFill(edge.getAttributes(), true);
                GraphConstants.setDashPattern(edge.getAttributes(), new float[] {10, 5});
                //GraphConstants.setLineStyle(edge.getAttributes(), GraphConstants.STYLE_SPLINE);
                links.add(edge);
            }
        }

        // Insert the cells via the cache, so they get selected
        List<Object> cells = new ArrayList<Object>(tables.size() + links.size());
        cells.addAll(tables);
        cells.addAll(links);

        {
            // Layout
            Layouter layouter = new SugiyamaLayouter();
            for (ERDTable table : tables) {
                layouter.add(new SugiyamaNode(table));
            }
            for (ERDLink link : links) {
                layouter.add(new SugiyamaEdge(link));
            }
            layouter.layout();
        }
        graphModel.insert(cells.toArray(), attributes, cs, null, null);
    }

    private void layoutTables(DBRProgressMonitor monitor, List<ERDTable> tables)
        throws DBException
    {
        // Set table properties
        int tablesCount = tables.size();

        // make square where height = 2/3 of width
        int colCount = (int)Math.round(Math.sqrt(3 * tablesCount / 2));

        int colIndex = 0, rowIndex = 0;
        for (ERDTable table : tables) {
            table.calculateContent(monitor, graphics);
            int tableWidth = table.getRectWidth(), tableHeight = table.getRectHeight();

            // Set bounds
            GraphConstants.setBounds(table.getAttributes(), new Rectangle2D.Double(
                colIndex * (tableWidth + 20) + 20,
                rowIndex * (tableHeight + 20) + 20,
                tableWidth,
                tableHeight));

            // Set black border
            GraphConstants.setBorderColor(table.getAttributes(), java.awt.Color.black);
            GraphConstants.setBackground(table.getAttributes(), java.awt.Color.yellow);
            GraphConstants.setOpaque(table.getAttributes(), true);
            GraphConstants.setSizeable(table.getAttributes(), true);
            GraphConstants.setSelectable(table.getAttributes(), true);

            // Add a Floating Port
            table.addPort(null, table.getTable().getFullQualifiedName() + "/Center");

            colIndex++;
            if (colIndex >= colCount) {
                colIndex = 0;
                rowIndex++;
            }
        }
    }

    private class ProgressControl extends ProgressPageControl {
        private ToolItem itemZoomIn;
        private ToolItem itemZoomOut;
        private ToolItem itemZoomNorm;
        private ToolItem itemRefresh;

        private ProgressControl(Composite parent, int style, IWorkbenchPart workbenchPart) {
            super(parent, style, workbenchPart);
        }

        @Override
        public void dispose() {
            UIUtils.dispose(itemZoomIn);
            UIUtils.dispose(itemZoomOut);
            UIUtils.dispose(itemZoomNorm);
            UIUtils.dispose(itemRefresh);
            super.dispose();
        }

        protected int getProgressCellCount()
        {
            return 3;
        }

        @Override
        protected Composite createProgressPanel(Composite container) {
            Composite infoGroup = super.createProgressPanel(container);

            ToolBar toolBar = new ToolBar(infoGroup, SWT.FLAT | SWT.HORIZONTAL);
            GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
            toolBar.setLayoutData(gd);
            itemZoomIn = UIUtils.createToolItem(toolBar, "Zoom In", DBIcon.ZOOM_IN, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                    synchronized (ERDEditor.this) {
                        if (graph.getScale() < 2.0) {
                            graph.setScale(graph.getScale() + 0.1);
                        }
                    }
                }
            });
            itemZoomOut = UIUtils.createToolItem(toolBar, "Zoom Out", DBIcon.ZOOM_OUT, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                    synchronized (ERDEditor.this) {
                        if (graph.getScale() > 0.2) {
                            graph.setScale(graph.getScale() - 0.1);
                        }
                    }
                }
            });
            itemZoomNorm = UIUtils.createToolItem(toolBar, "Standard Zoom", DBIcon.ZOOM, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                    synchronized (ERDEditor.this) {
                        graph.setScale(1.0);
                    }
                }
            });
            itemRefresh = UIUtils.createToolItem(toolBar, "Refresh", DBIcon.RS_REFRESH, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {

                }
            });
            return infoGroup;
        }

    }


}