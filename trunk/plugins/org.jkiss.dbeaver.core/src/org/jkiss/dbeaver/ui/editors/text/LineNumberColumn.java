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


import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.source.*;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.rulers.IContributedRulerColumn;
import org.eclipse.ui.texteditor.rulers.RulerColumnDescriptor;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.core.DBeaverUI;

/**
 * The line number ruler contribution. Encapsulates a {@link org.eclipse.jface.text.source.LineNumberChangeRulerColumn} as a
 * contribution to the <code>rulerColumns</code> extension point. Instead of instantiating the
 * delegate itself, it calls <code>createLineNumberRulerColumn()</code> in
 * {@link org.eclipse.ui.texteditor.AbstractDecoratedTextEditor} via {@link ICompatibilityForwarder} to maintain compatibility
 * with previous releases.
 *
 * @since 3.3
 */
public class LineNumberColumn implements IContributedRulerColumn, IVerticalRulerInfo, IVerticalRulerInfoExtension {
    /**
     * Forwarder for preference checks and ruler creation. Needed to maintain the forwarded APIs in
     * {@link org.eclipse.ui.texteditor.AbstractDecoratedTextEditor}.
     */
    public static interface ICompatibilityForwarder {
        IVerticalRulerColumn createLineNumberRulerColumn();

        boolean isQuickDiffEnabled();

        boolean isLineNumberRulerVisible();
    }

    /**
     * The contribution id of the line number / change ruler.
     */
    public static final String ID = "org.jkiss.dbeaver.ui.editors.columns.linenumbers"; //$NON-NLS-1$

    private static final String FG_COLOR_KEY = "lineNumberForeground";
    private static final String BG_COLOR_KEY = "lineNumberBackground";
    private static final String USE_DEFAULT_BG_KEY = "lineNumberBackgroundDefault";
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
     * The delegate and implemenation of the ruler.
     */
    private IVerticalRulerColumn fDelegate;
    /**
     * Preference dispatcher that registers a single listener so we don't have to manage every
     * single preference listener.
     */
    private PropertyEventDispatcher fDispatcher;
    private ISourceViewer fViewer;
    private ICompatibilityForwarder fForwarder;

    /*
      * @see org.eclipse.ui.texteditor.rulers.IContributedRulerColumn#getDescriptor()
      */
    @Override
    public final RulerColumnDescriptor getDescriptor() {
        return fDescriptor;
    }

    /*
      * @see org.eclipse.ui.texteditor.rulers.IContributedRulerColumn#setDescriptor(org.eclipse.ui.texteditor.rulers.RulerColumnDescriptor)
      */
    @Override
    public final void setDescriptor(RulerColumnDescriptor descriptor) {
        Assert.isLegal(descriptor != null);
        Assert.isTrue(fDescriptor == null);
        fDescriptor = descriptor;
    }

    /*
      * @see org.eclipse.ui.texteditor.rulers.IContributedRulerColumn#setEditor(org.eclipse.ui.texteditor.ITextEditor)
      */
    @Override
    public final void setEditor(ITextEditor editor) {
        Assert.isLegal(editor != null);
        Assert.isTrue(fEditor == null);
        fEditor = editor;
    }

    /*
      * @see org.eclipse.ui.texteditor.rulers.IContributedRulerColumn#getEditor()
      */
    @Override
    public final ITextEditor getEditor() {
        return fEditor;
    }

    /*
      * @see org.eclipse.ui.texteditor.rulers.IContributedRulerColumn#columnCreated()
      */
    @Override
    public void columnCreated() {
    }

    /*
      * @see org.eclipse.ui.texteditor.rulers.AbstractContributedRulerColumn#columnRemoved()
      */
    @Override
    public void columnRemoved() {
        if (fDispatcher != null) {
            fDispatcher.dispose();
            fDispatcher = null;
        }
    }


    /*
      * @see org.eclipse.jface.text.source.IVerticalRulerColumn#createControl(org.eclipse.jface.text.source.CompositeRuler, org.eclipse.swt.widgets.Composite)
      */
    @Override
    public Control createControl(CompositeRuler parentRuler, Composite parentControl) {
        Assert.isTrue(fDelegate != null);
        ITextViewer viewer = parentRuler.getTextViewer();
        Assert.isLegal(viewer instanceof ISourceViewer);
        fViewer = (ISourceViewer) viewer;
        initialize();
        Control control = fDelegate.createControl(parentRuler, parentControl);
        return control;
    }

    /*
      * @see org.eclipse.jface.text.source.IVerticalRulerColumn#getControl()
      */
    @Override
    public Control getControl() {
        return fDelegate.getControl();
    }

    /*
      * @see org.eclipse.jface.text.source.IVerticalRulerColumn#getWidth()
      */
    @Override
    public int getWidth() {
        return fDelegate.getWidth();
    }

    /*
      * @see org.eclipse.jface.text.source.IVerticalRulerColumn#redraw()
      */
    @Override
    public void redraw() {
        fDelegate.redraw();
    }

    /*
      * @see org.eclipse.jface.text.source.IVerticalRulerColumn#setFont(org.eclipse.swt.graphics.Font)
      */
    @Override
    public void setFont(Font font) {
        fDelegate.setFont(font);
    }

    /*
      * @see org.eclipse.jface.text.source.IVerticalRulerColumn#setModel(org.eclipse.jface.text.source.IAnnotationModel)
      */
    @Override
    public void setModel(IAnnotationModel model) {
//        if (getQuickDiffPreference())
//            fDelegate.setModel(model);
    }

    /*
      * @see org.eclipse.jface.text.source.IVerticalRulerInfo#getLineOfLastMouseButtonActivity()
      */
    @Override
    public int getLineOfLastMouseButtonActivity() {
        if (fDelegate instanceof IVerticalRulerInfo)
            return ((IVerticalRulerInfo) fDelegate).getLineOfLastMouseButtonActivity();
        return -1;
    }

    /*
      * @see org.eclipse.jface.text.source.IVerticalRulerInfo#toDocumentLineNumber(int)
      */
    @Override
    public int toDocumentLineNumber(int y_coordinate) {
        if (fDelegate instanceof IVerticalRulerInfo)
            return ((IVerticalRulerInfo) fDelegate).toDocumentLineNumber(y_coordinate);
        return -1;
    }

    /*
      * @see org.eclipse.jface.text.source.IVerticalRulerInfoExtension#addVerticalRulerListener(org.eclipse.jface.text.source.IVerticalRulerListener)
      */
    @Override
    public void addVerticalRulerListener(IVerticalRulerListener listener) {
        if (fDelegate instanceof IVerticalRulerInfoExtension)
            ((IVerticalRulerInfoExtension) fDelegate).addVerticalRulerListener(listener);
    }

    /*
      * @see org.eclipse.jface.text.source.IVerticalRulerInfoExtension#getHover()
      */
    @Override
    public IAnnotationHover getHover() {
        if (fDelegate instanceof IVerticalRulerInfoExtension)
            return ((IVerticalRulerInfoExtension) fDelegate).getHover();
        return null;
    }

    /*
      * @see org.eclipse.jface.text.source.IVerticalRulerInfoExtension#getModel()
      */
    @Override
    public IAnnotationModel getModel() {
        if (fDelegate instanceof IVerticalRulerInfoExtension)
            return ((IVerticalRulerInfoExtension) fDelegate).getModel();
        return null;
    }

    /*
      * @see org.eclipse.jface.text.source.IVerticalRulerInfoExtension#removeVerticalRulerListener(org.eclipse.jface.text.source.IVerticalRulerListener)
      */
    @Override
    public void removeVerticalRulerListener(IVerticalRulerListener listener) {
        if (fDelegate instanceof IVerticalRulerInfoExtension)
            ((IVerticalRulerInfoExtension) fDelegate).removeVerticalRulerListener(listener);
    }

    private IPreferenceStore getPreferenceStore() {
        return DBeaverActivator.getInstance().getPreferenceStore();
    }

    private ISharedTextColors getSharedColors() {
        return DBeaverUI.getSharedTextColors();
    }

    /**
     * Initializes the given line number ruler column from the preference store.
     */
    private void initialize() {
        final IPreferenceStore store = getPreferenceStore();
        if (store == null)
            return;

        // initial set up
        updateForegroundColor(store, fDelegate);
        updateBackgroundColor(store, fDelegate);

        updateLineNumbersVisibility(fDelegate);

        fDelegate.redraw();

        // listen to changes
        fDispatcher = new PropertyEventDispatcher(store);

        fDispatcher.addPropertyChangeListener(FG_COLOR_KEY, new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                updateForegroundColor(store, fDelegate);
                fDelegate.redraw();
            }
        });
        IPropertyChangeListener backgroundHandler = new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                updateBackgroundColor(store, fDelegate);
                fDelegate.redraw();
            }
        };
        fDispatcher.addPropertyChangeListener(BG_COLOR_KEY, backgroundHandler);
        fDispatcher.addPropertyChangeListener(USE_DEFAULT_BG_KEY, backgroundHandler);

        fDispatcher.addPropertyChangeListener(LINE_NUMBER_KEY, new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                // only handle quick diff on/off information, but not ruler visibility (handled by AbstractDecoratedTextEditor)
                updateLineNumbersVisibility(fDelegate);
            }
        });
    }

    private void updateForegroundColor(IPreferenceStore store, IVerticalRulerColumn column) {
        RGB rgb = getColorFromStore(store, FG_COLOR_KEY);
        if (rgb == null)
            rgb = new RGB(0, 0, 0);
        ISharedTextColors sharedColors = getSharedColors();
        if (column instanceof LineNumberRulerColumn)
            ((LineNumberRulerColumn) column).setForeground(sharedColors.getColor(rgb));
    }

    private void updateBackgroundColor(IPreferenceStore store, IVerticalRulerColumn column) {
        // background color: same as editor, or system default
        RGB rgb;
        if (store.getBoolean(USE_DEFAULT_BG_KEY))
            rgb = null;
        else
            rgb = getColorFromStore(store, BG_COLOR_KEY);
        ISharedTextColors sharedColors = getSharedColors();
        if (column instanceof LineNumberRulerColumn)
            ((LineNumberRulerColumn) column).setBackground(sharedColors.getColor(rgb));
    }

    private void updateChangedColor(AnnotationPreference pref, IPreferenceStore store, IVerticalRulerColumn column) {
        if (pref != null && column instanceof IChangeRulerColumn) {
            RGB rgb = getColorFromAnnotationPreference(store, pref);
            ((IChangeRulerColumn) column).setChangedColor(getSharedColors().getColor(rgb));
        }
    }

    private void updateAddedColor(AnnotationPreference pref, IPreferenceStore store, IVerticalRulerColumn column) {
        if (pref != null && column instanceof IChangeRulerColumn) {
            RGB rgb = getColorFromAnnotationPreference(store, pref);
            ((IChangeRulerColumn) column).setAddedColor(getSharedColors().getColor(rgb));
        }
    }

    private void updateLineNumbersVisibility(IVerticalRulerColumn column) {
        if (column instanceof LineNumberChangeRulerColumn)
            ((LineNumberChangeRulerColumn) column).showLineNumbers(getLineNumberPreference());
    }

    /**
     * Returns whether the line number ruler column should be
     * visible according to the preference store settings. Subclasses may override this
     * method to provide a custom preference setting.
     *
     * @return <code>true</code> if the line numbers should be visible
     */
    private boolean getLineNumberPreference() {
        if (fForwarder != null)
            return fForwarder.isLineNumberRulerVisible();
        IPreferenceStore store = getPreferenceStore();
        return store != null ? store.getBoolean(LINE_NUMBER_KEY) : false;
    }

    /**
     * Extracts the color preference for the given preference from the given store.
     * If the given store indicates that the default value is to be used, or
     * the value stored in the preferences store is <code>null</code>,
     * the value is taken from the <code>AnnotationPreference</code>'s default
     * color value.
     * <p>
     * The return value is
     * </p>
     *
     * @param store the preference store
     * @param pref  the annotation preference
     * @return the RGB color preference, not <code>null</code>
     */
    private static RGB getColorFromAnnotationPreference(IPreferenceStore store, AnnotationPreference pref) {
        String key = pref.getColorPreferenceKey();
        RGB rgb = null;
        if (store.contains(key)) {
            if (store.isDefault(key))
                rgb = pref.getColorPreferenceValue();
            else
                rgb = PreferenceConverter.getColor(store, key);
        }
        if (rgb == null)
            rgb = pref.getColorPreferenceValue();
        return rgb;
    }

    private static RGB getColorFromStore(IPreferenceStore store, String key) {
        RGB rgb = null;
        if (store.contains(key)) {
            if (store.isDefault(key))
                rgb = PreferenceConverter.getDefaultColor(store, key);
            else
                rgb = PreferenceConverter.getColor(store, key);
        }
        return rgb;
    }

    /**
     * Initializes the given line number ruler column from the preference store.
     *
     * @param rulerColumn the ruler column to be initialized
     */
    public void initializeLineNumberRulerColumn(LineNumberRulerColumn rulerColumn) {
        IPreferenceStore store = getPreferenceStore();
        if (store != null) {
            updateForegroundColor(store, rulerColumn);
            updateBackgroundColor(store, rulerColumn);
            updateLineNumbersVisibility(rulerColumn);
            rulerColumn.redraw();
        }
    }

    /**
     * Returns <code>true</code> if the ruler is showing line numbers, <code>false</code> if it
     * is only showing change information.
     *
     * @return <code>true</code> if line numbers are shown, <code>false</code> otherwise
     */
    public boolean isShowingLineNumbers() {
        return fDelegate instanceof LineNumberChangeRulerColumn && ((LineNumberChangeRulerColumn) fDelegate).isShowingLineNumbers();
    }

    /**
     * Returns <code>true</code> if the ruler is showing change information, <code>false</code>
     * if it is only showing line numbers.
     *
     * @return <code>true</code> if change information is shown, <code>false</code> otherwise
     */
    public boolean isShowingChangeInformation() {
        return fDelegate instanceof LineNumberChangeRulerColumn && ((LineNumberChangeRulerColumn) fDelegate).isShowingChangeInformation();
    }

    public void setForwarder(ICompatibilityForwarder forwarder) {
        fForwarder= forwarder;
        fDelegate= forwarder.createLineNumberRulerColumn();
    }

}
