/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.editors.text;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IUndoManager;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.revisions.IRevisionRulerColumn;
import org.eclipse.jface.text.source.*;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.StatusTextEditor;
import org.eclipse.ui.texteditor.rulers.*;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPCommentsManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.ProjectFileEditorInput;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
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

    @Override
    public Object getAdapter(Class adapter)
    {
        if (IRevisionRulerColumn.class.equals(adapter)) {
            if (fLineNumberRulerColumn instanceof IRevisionRulerColumn)
                return fLineNumberRulerColumn;
        }
        return super.getAdapter(adapter);
    }

    public Document getDocument()
    {
        IDocumentProvider provider = getDocumentProvider();
        return provider == null ? null : (Document)provider.getDocument(getEditorInput());
    }

    @Override
    public void createPartControl(Composite parent)
    {
        setPreferenceStore(DBeaverCore.getGlobalPreferenceStore());

        super.createPartControl(parent);
    }

    @Override
    protected void editorContextMenuAboutToShow(IMenuManager menu)
    {
        super.editorContextMenuAboutToShow(menu);
    }

    public TextViewer getTextViewer()
    {
        return (TextViewer) getSourceViewer();
    }

    public ISourceViewer getViewer()
    {
        return getSourceViewer();
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
    @Override
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
        List descriptors = RulerColumnRegistry.getDefault().getColumnDescriptors();
        for (Iterator t = descriptors.iterator(); t.hasNext();) {
            final RulerColumnDescriptor descriptor= (RulerColumnDescriptor) t.next();
            if (!descriptor.isIncludedInMenu() || !support.isColumnSupported(descriptor))
                continue;
            final boolean isVisible= support.isColumnVisible(descriptor);
            IAction action= new Action("Show " + descriptor.getName(), IAction.AS_CHECK_BOX) {
                @Override
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

    @Override
    public void showChangeInformation(boolean show)
    {
        if (show == isChangeInformationShowing())
            return;

        IColumnSupport columnSupport = (IColumnSupport) getAdapter(IColumnSupport.class);

        // only handle visibility of the combined column, but not the number/change only state
        if (show && fLineColumn == null) {
            RulerColumnDescriptor lineNumberColumnDescriptor = RulerColumnRegistry.getDefault().getColumnDescriptor(LineNumberColumn.ID);
            if (lineNumberColumnDescriptor != null)
                columnSupport.setColumnVisible(lineNumberColumnDescriptor, true);
        } else if (!show && fLineColumn != null && !isLineNumberRulerVisible()) {
            columnSupport.setColumnVisible(fLineColumn.getDescriptor(), false);
            fLineColumn = null;
        }
    }

    @Override
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
        fLineNumberRulerColumn = new LineNumberChangeRulerColumn(DBeaverUI.getSharedTextColors());
        ((IChangeRulerColumn) fLineNumberRulerColumn).setHover(new TextChangeHover());
        initializeLineNumberRulerColumn(fLineNumberRulerColumn);
        return fLineNumberRulerColumn;
    }

    @Override
    protected final IColumnSupport createColumnSupport() {
        return new ColumnSupport(this, RulerColumnRegistry.getDefault()) {
            @Override
            protected void initializeColumn(IContributedRulerColumn column) {
                super.initializeColumn(column);
                RulerColumnDescriptor descriptor= column.getDescriptor();
                IVerticalRuler ruler = getVerticalRuler();
                if (ruler instanceof CompositeRuler) {
                    if (LineNumberColumn.ID.equals(descriptor.getId())) {
                        fLineColumn= ((LineNumberColumn) column);
                        fLineColumn.setForwarder(new LineNumberColumn.ICompatibilityForwarder() {
                            @Override
                            public IVerticalRulerColumn createLineNumberRulerColumn() {
                                return BaseTextEditor.this.createLineNumberRulerColumn();
                            }
                            @Override
                            public boolean isQuickDiffEnabled() {
                                return false;
                            }
                            @Override
                            public boolean isLineNumberRulerVisible() {
                                return BaseTextEditor.this.isLineNumberRulerVisible();
                            }
                        });
                    }
                }
            }
            @Override
            public void dispose() {
                fLineColumn= null;
                super.dispose();
            }
        };
    }

    @Override
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
                    RulerColumnDescriptor lineNumberColumnDescriptor= RulerColumnRegistry.getDefault().getColumnDescriptor(LineNumberColumn.ID);
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

    public DBPCommentsManager getCommentsSupport()
    {
        return null;
    }

    protected boolean isReadOnly()
    {
        return false;
    }

    public void loadFromExternalFile()
    {
        final File loadFile = ContentUtils.openFile(getSite().getShell(), new String[]{"*.sql", "*.txt", "*.*"});
        if (loadFile == null) {
            return;
        }

        String newContent = null;
        try {
            Reader reader = new InputStreamReader(
                new FileInputStream(loadFile),
                ContentUtils.DEFAULT_FILE_CHARSET);
            try {
                StringWriter buffer = new StringWriter();
                IOUtils.copyText(reader, buffer, 10000);
                newContent = buffer.toString();
            }
            finally {
                reader.close();
            }
        }
        catch (IOException e) {
            UIUtils.showErrorDialog(
                getSite().getShell(),
                "Can't load file",
                "Can't load file '" + loadFile.getAbsolutePath() + "' - " + e.getMessage());
        }
        if (newContent != null) {
            getDocument().set(newContent);
        }
    }

    public void saveToExternalFile()
    {
        IEditorInput editorInput = getEditorInput();
        String fileName = (editorInput instanceof ProjectFileEditorInput ?
            ((ProjectFileEditorInput)getEditorInput()).getFile().getName() : null);

        final File saveFile = ContentUtils.selectFileForSave(getSite().getShell(), "Save SQL script", new String[] { "*.sql", "*.txt", "*.*"}, fileName);
        if (saveFile == null) {
            return;
        }

        try {
            DBeaverUI.runInProgressDialog(new DBRRunnableWithProgress() {
                @Override
                public void run(final DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                {
                    UIUtils.runInUI(getSite().getShell(), new Runnable() {
                        @Override
                        public void run()
                        {
                            doSave(monitor.getNestedMonitor());
                        }
                    });

                    try {
                        ContentUtils.saveContentToFile(new StringReader(getDocument().get()), saveFile, ContentUtils.DEFAULT_FILE_CHARSET_NAME, monitor);
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (InterruptedException e) {
            // do nothing
        } catch (InvocationTargetException e) {
            UIUtils.showErrorDialog(getSite().getShell(), "Save failed", null, e.getTargetException());
        }
    }

}