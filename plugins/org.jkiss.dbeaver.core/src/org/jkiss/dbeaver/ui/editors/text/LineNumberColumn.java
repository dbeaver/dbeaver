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
package org.jkiss.dbeaver.ui.editors.text;


import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.source.*;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.rulers.IContributedRulerColumn;
import org.eclipse.ui.texteditor.rulers.RulerColumnDescriptor;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPPreferenceListener;
import org.jkiss.dbeaver.model.DBPPreferenceStore;

/**
 * The line number ruler contribution. Encapsulates a {@link org.eclipse.jface.text.source.LineNumberChangeRulerColumn} as a
 * contribution to the <code>rulerColumns</code> extension point.
 *
 * @since 3.3
 */
public class LineNumberColumn implements IContributedRulerColumn, IVerticalRulerInfo, IVerticalRulerInfoExtension {
    /**
     * The contribution id of the line number / change ruler.
     */
    public static final String ID = "org.jkiss.dbeaver.ui.editors.columns.linenumbers"; //$NON-NLS-1$

    private final static String LINE_NUMBER_KEY = "lineNumberRuler";

    /**
     * The contribution descriptor.
     */
    private RulerColumnDescriptor fDescriptor;
    /**
     * The target editor.
     */
    private ITextEditor fEditor;
    /**
     * The delegate and implementation of the ruler.
     */
    private IVerticalRulerColumn fDelegate;
    /**
     * Preference dispatcher that registers a single listener so we don't have to manage every
     * single preference listener.
     */
    private PropertyEventDispatcher fDispatcher;
    private CompositeRuler parentRuler;

    public LineNumberColumn()
    {
        LineNumberChangeRulerColumn rulerColumn = new LineNumberChangeRulerColumn(DBeaverUI.getSharedTextColors());
        rulerColumn.setHover(new TextChangeHover());
        initializeLineNumberRulerColumn(rulerColumn);
        fDelegate = rulerColumn;
    }

    @Override
    public final RulerColumnDescriptor getDescriptor()
    {
        return fDescriptor;
    }

    @Override
    public final void setDescriptor(RulerColumnDescriptor descriptor)
    {
        Assert.isLegal(descriptor != null);
        Assert.isTrue(fDescriptor == null);
        fDescriptor = descriptor;
    }

    @Override
    public final void setEditor(ITextEditor editor)
    {
        Assert.isLegal(editor != null);
        Assert.isTrue(fEditor == null);
        fEditor = editor;
    }

    @Override
    public final ITextEditor getEditor()
    {
        return fEditor;
    }

    @Override
    public void columnCreated()
    {
    }

    @Override
    public void columnRemoved()
    {
        if (fDispatcher != null) {
            fDispatcher.dispose();
            fDispatcher = null;
        }
    }


    @Override
    public Control createControl(CompositeRuler parentRuler, Composite parentControl)
    {
        Assert.isTrue(fDelegate != null);
        this.parentRuler = parentRuler;
        initialize();
        return fDelegate.createControl(parentRuler, parentControl);
    }

    @Override
    public Control getControl()
    {
        return fDelegate.getControl();
    }

    @Override
    public int getWidth()
    {
        return fDelegate.getWidth();
    }

    @Override
    public void redraw()
    {
        updateBackgroundColor(fDelegate);
        updateForegroundColor(fDelegate);
        fDelegate.redraw();
    }

    @Override
    public void setFont(Font font)
    {
        fDelegate.setFont(font);
    }

    @Override
    public void setModel(IAnnotationModel model)
    {
//        if (getQuickDiffPreference())
//            fDelegate.setModel(model);
    }

    @Override
    public int getLineOfLastMouseButtonActivity()
    {
        if (fDelegate instanceof IVerticalRulerInfo)
            return ((IVerticalRulerInfo) fDelegate).getLineOfLastMouseButtonActivity();
        return -1;
    }

    @Override
    public int toDocumentLineNumber(int y_coordinate)
    {
        if (fDelegate instanceof IVerticalRulerInfo)
            return ((IVerticalRulerInfo) fDelegate).toDocumentLineNumber(y_coordinate);
        return -1;
    }

    @Override
    public void addVerticalRulerListener(IVerticalRulerListener listener)
    {
        if (fDelegate instanceof IVerticalRulerInfoExtension)
            ((IVerticalRulerInfoExtension) fDelegate).addVerticalRulerListener(listener);
    }

    @Override
    public IAnnotationHover getHover()
    {
        if (fDelegate instanceof IVerticalRulerInfoExtension)
            return ((IVerticalRulerInfoExtension) fDelegate).getHover();
        return null;
    }

    @Override
    public IAnnotationModel getModel()
    {
        if (fDelegate instanceof IVerticalRulerInfoExtension)
            return ((IVerticalRulerInfoExtension) fDelegate).getModel();
        return null;
    }

    @Override
    public void removeVerticalRulerListener(IVerticalRulerListener listener)
    {
        if (fDelegate instanceof IVerticalRulerInfoExtension)
            ((IVerticalRulerInfoExtension) fDelegate).removeVerticalRulerListener(listener);
    }

    private DBPPreferenceStore getPreferenceStore()
    {
        return DBeaverCore.getGlobalPreferenceStore();
    }

    private ISharedTextColors getSharedColors()
    {
        return DBeaverUI.getSharedTextColors();
    }

    /**
     * Initializes the given line number ruler column from the preference store.
     */
    private void initialize()
    {
        final DBPPreferenceStore store = getPreferenceStore();
        if (store == null)
            return;

        // initial set up
        updateForegroundColor(fDelegate);
        updateBackgroundColor(fDelegate);

        updateLineNumbersVisibility(fDelegate);

        fDelegate.redraw();

        // listen to changes
        fDispatcher = new PropertyEventDispatcher(store);

        fDispatcher.addPropertyChangeListener(LINE_NUMBER_KEY, new DBPPreferenceListener() {
            @Override
            public void preferenceChange(PreferenceChangeEvent event)
            {
                // only handle quick diff on/off information, but not ruler visibility (handled by AbstractDecoratedTextEditor)
                updateLineNumbersVisibility(fDelegate);
            }
        });
    }

    private void updateForegroundColor(IVerticalRulerColumn column)
    {
        if (parentRuler.getTextViewer() != null && column instanceof LineNumberRulerColumn) {
            ((LineNumberRulerColumn) column).setForeground(parentRuler.getTextViewer().getTextWidget().getForeground());
        }
    }

    private void updateBackgroundColor(IVerticalRulerColumn column)
    {
        if (parentRuler.getTextViewer() != null && column instanceof LineNumberRulerColumn) {
            ((LineNumberRulerColumn) column).setBackground(parentRuler.getTextViewer().getTextWidget().getBackground());
        }
    }

    private void updateLineNumbersVisibility(IVerticalRulerColumn column)
    {
        if (column instanceof LineNumberChangeRulerColumn)
            ((LineNumberChangeRulerColumn) column).showLineNumbers(true);
    }

    /**
     * Initializes the given line number ruler column from the preference store.
     *
     * @param rulerColumn the ruler column to be initialized
     */
    public void initializeLineNumberRulerColumn(LineNumberRulerColumn rulerColumn)
    {
        DBPPreferenceStore store = getPreferenceStore();
        if (store != null) {
            updateLineNumbersVisibility(rulerColumn);
        }
    }

    /**
     * Returns <code>true</code> if the ruler is showing change information, <code>false</code>
     * if it is only showing line numbers.
     *
     * @return <code>true</code> if change information is shown, <code>false</code> otherwise
     */
    public boolean isShowingChangeInformation()
    {
        return fDelegate instanceof LineNumberChangeRulerColumn && ((LineNumberChangeRulerColumn) fDelegate).isShowingChangeInformation();
    }

}
