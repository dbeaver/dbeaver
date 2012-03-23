/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.text;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IUndoManager;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.revisions.IRevisionRulerColumn;
import org.eclipse.jface.text.source.*;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.StatusTextEditor;
import org.eclipse.ui.texteditor.rulers.*;
import org.jkiss.dbeaver.core.DBeaverCore;

import java.util.Iterator;
import java.util.List;

/**
 * Abstract text editor.
 * Contains some common dbeaver text editor adaptions.
 */
public abstract class BaseTextEditor extends StatusTextEditor {

    private final static String LINE_NUMBER_RULER = "lineNumberRule";

    private LineNumberRulerColumn fLineNumberRulerColumn;
    private LineNumberColumn fLineColumn;

    @Override
    public void dispose()
    {
        fLineNumberRulerColumn = null;
        fLineColumn = null;
        super.dispose();
    }

    public Object getAdapter(Class adapter)
    {
        if (IRevisionRulerColumn.class.equals(adapter)) {
            if (fLineNumberRulerColumn instanceof IRevisionRulerColumn)
                return fLineNumberRulerColumn;
        }
        return super.getAdapter(adapter);
    }

    @Override
    public void createPartControl(Composite parent)
    {
        setPreferenceStore(DBeaverCore.getInstance().getGlobalPreferenceStore());

        super.createPartControl(parent);
    }

    protected void editorContextMenuAboutToShow(IMenuManager menu)
    {
        super.editorContextMenuAboutToShow(menu);
    }

    public TextViewer getTextViewer()
    {
        return (TextViewer) getSourceViewer();
    }

    public void enableUndoManager(boolean enable)
    {
        TextViewer textViewer = getTextViewer();
        final IUndoManager undoManager = textViewer.getUndoManager();
        if (undoManager != null) {
            if (!enable) {
                undoManager.disconnect();
            } else {
                undoManager.connect(textViewer);
            }
        }
    }

    public void handleActivate()
    {
        safelySanityCheckState(getEditorInput());
    }

    /*
      * @see org.eclipse.ui.texteditor.AbstractTextEditor#rulerContextMenuAboutToShow(org.eclipse.jface.action.IMenuManager)
      * @since 3.1
      */
    protected void rulerContextMenuAboutToShow(IMenuManager menu) {
        menu.add(new Separator(ITextEditorActionConstants.GROUP_RULERS));
        menu.add(new Separator(ITextEditorActionConstants.GROUP_REST));

        super.rulerContextMenuAboutToShow(menu);

        addRulerContributionActions(menu);
    }

    /**
     * Adds "show" actions for all contributed rulers that support it.
     *
     * @param menu the ruler context menu
     * @since 3.3
     */
    private void addRulerContributionActions(IMenuManager menu) {
        // store directly in generic editor preferences
        final IColumnSupport support= (IColumnSupport) getAdapter(IColumnSupport.class);
        IPreferenceStore store = getPreferenceStore();
        final RulerColumnPreferenceAdapter adapter= new RulerColumnPreferenceAdapter(store, AbstractTextEditor.PREFERENCE_RULER_CONTRIBUTIONS);
        List descriptors= RulerColumnRegistry.getDefault().getColumnDescriptors();
        for (Iterator t= descriptors.iterator(); t.hasNext();) {
            final RulerColumnDescriptor descriptor= (RulerColumnDescriptor) t.next();
            if (!descriptor.isIncludedInMenu() || !support.isColumnSupported(descriptor))
                continue;
            final boolean isVisible= support.isColumnVisible(descriptor);
            IAction action= new Action("Show column ruler " + descriptor.getName(), IAction.AS_CHECK_BOX) {
                public void run() {
                    if (descriptor.isGlobal())
                        // column state is modified via preference listener of AbstractTextEditor
                        adapter.setEnabled(descriptor, !isVisible);
                    else
                        // directly modify column for this editor instance
                        support.setColumnVisible(descriptor, !isVisible);
                }
            };
            action.setChecked(isVisible);
            action.setImageDescriptor(descriptor.getIcon());
            menu.appendToGroup(ITextEditorActionConstants.GROUP_RULERS, action);
        }
    }

    /**
     * Toggles the line number global preference and shows the line number ruler
     * accordingly.
     *
     * @since 3.1
     */
    private void toggleLineNumberRuler() {
        // globally
        IPreferenceStore store= getPreferenceStore();
        store.setValue(LINE_NUMBER_RULER, !isLineNumberRulerVisible());
    }

    public void showChangeInformation(boolean show)
    {
        if (show == isChangeInformationShowing())
            return;

        IColumnSupport columnSupport = (IColumnSupport) getAdapter(IColumnSupport.class);

        // only handle visibility of the combined column, but not the number/change only state
        if (show && fLineColumn == null) {
            RulerColumnDescriptor lineNumberColumnDescriptor = RulerColumnRegistry.getDefault().getColumnDescriptor(org.eclipse.ui.internal.texteditor.LineNumberColumn.ID);
            if (lineNumberColumnDescriptor != null)
                columnSupport.setColumnVisible(lineNumberColumnDescriptor, true);
        } else if (!show && fLineColumn != null && !isLineNumberRulerVisible()) {
            columnSupport.setColumnVisible(fLineColumn.getDescriptor(), false);
            fLineColumn = null;
        }
    }

    public boolean isChangeInformationShowing()
    {
        return fLineColumn != null && fLineColumn.isShowingChangeInformation();
    }

    /**
     * Returns whether the line number ruler column should be
     * visible according to the preference store settings. Subclasses may override this
     * method to provide a custom preference setting.
     *
     * @return <code>true</code> if the line numbers should be visible
     */
    protected boolean isLineNumberRulerVisible()
    {
        return true;
//        IPreferenceStore store = getPreferenceStore();
//        return store != null && store.getBoolean(LINE_NUMBER_RULER);
    }

    protected void initializeLineNumberRulerColumn(LineNumberRulerColumn rulerColumn)
    {
        /*
         * Left for compatibility. See LineNumberColumn.
         */
        if (fLineColumn != null)
            fLineColumn.initializeLineNumberRulerColumn(rulerColumn);
    }

    /**
     * Creates a new line number ruler column that is appropriately initialized.
     *
     * @return the created line number column
     */
    protected IVerticalRulerColumn createLineNumberRulerColumn()
    {
        fLineNumberRulerColumn = new LineNumberChangeRulerColumn(DBeaverCore.getInstance().getSharedTextColors());
        ((IChangeRulerColumn) fLineNumberRulerColumn).setHover(new TextChangeHover());
        initializeLineNumberRulerColumn(fLineNumberRulerColumn);
        return fLineNumberRulerColumn;
    }

    protected final IColumnSupport createColumnSupport() {
        return new ColumnSupport(this, RulerColumnRegistry.getDefault()) {
            protected void initializeColumn(IContributedRulerColumn column) {
                super.initializeColumn(column);
                RulerColumnDescriptor descriptor= column.getDescriptor();
                IVerticalRuler ruler = getVerticalRuler();
                if (ruler instanceof CompositeRuler) {
                    if (org.eclipse.ui.internal.texteditor.LineNumberColumn.ID.equals(descriptor.getId())) {
                        fLineColumn= ((LineNumberColumn) column);
                        fLineColumn.setForwarder(new LineNumberColumn.ICompatibilityForwarder() {
                            public IVerticalRulerColumn createLineNumberRulerColumn() {
                                return BaseTextEditor.this.createLineNumberRulerColumn();
                            }
                            public boolean isQuickDiffEnabled() {
                                return false;
                            }
                            public boolean isLineNumberRulerVisible() {
                                return BaseTextEditor.this.isLineNumberRulerVisible();
                            }
                        });
                    }
                }
            }
            public void dispose() {
                fLineColumn= null;
                super.dispose();
            }
        };
    }

    protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {

        try {

            ISourceViewer sourceViewer= getSourceViewer();
            if (sourceViewer == null)
                return;

            String property= event.getProperty();

            if (LINE_NUMBER_RULER.equals(property)) {
                // only handle visibility of the combined column, but not the number/change only state
                IColumnSupport columnSupport= (IColumnSupport)getAdapter(IColumnSupport.class);
                if (isLineNumberRulerVisible() && fLineColumn == null) {
                    RulerColumnDescriptor lineNumberColumnDescriptor= RulerColumnRegistry.getDefault().getColumnDescriptor(org.eclipse.ui.internal.texteditor.LineNumberColumn.ID);
                    if (lineNumberColumnDescriptor != null)
                        columnSupport.setColumnVisible(lineNumberColumnDescriptor, true);
                } else if (!isLineNumberRulerVisible() && fLineColumn != null && !fLineColumn.isShowingChangeInformation()) {
                    columnSupport.setColumnVisible(fLineColumn.getDescriptor(), false);
                    fLineColumn= null;
                }
            }
        } finally {
            super.handlePreferenceStoreChanged(event);
        }
    }

    @Override
    protected IVerticalRuler createVerticalRuler()
    {
        return new CompositeRuler();
    }

    /*
    private void showChangeRulerInformation() {
        IVerticalRuler ruler= getVerticalRuler();
        if (!(ruler instanceof CompositeRuler) || fLineColumn == null)
            return;

        CompositeRuler compositeRuler= (CompositeRuler)ruler;

        // fake a mouse move (some hovers rely on this to determine the hovered line):
        int x= fLineColumn.getControl().getLocation().x;

        ISourceViewer sourceViewer= getSourceViewer();
        StyledText textWidget= sourceViewer.getTextWidget();
        int caretOffset= textWidget.getCaretOffset();
        int caretLine= textWidget.getLineAtOffset(caretOffset);
        int y= textWidget.getLinePixel(caretLine);

        compositeRuler.setLocationOfLastMouseButtonActivity(x, y);

        IAnnotationHover hover= fLineColumn.getHover();
        showFocusedRulerHover(hover, sourceViewer, caretOffset);
    }
*/

}