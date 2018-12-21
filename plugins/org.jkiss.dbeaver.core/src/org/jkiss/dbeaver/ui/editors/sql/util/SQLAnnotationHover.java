/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.editors.sql.util;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * SQLAnnotationHover
 */
public class SQLAnnotationHover extends AbstractSQLEditorTextHover
    implements ITextHover, IAnnotationHover, ITextHoverExtension, ITextHoverExtension2
{
    private static final Log log = Log.getLog(SQLAnnotationHover.class);

    private List<Annotation> annotations = new ArrayList<>();
    private SQLEditorBase editor;
    private IHyperlinkDetector[] hyperlinkDetectors;

    public SQLAnnotationHover(SQLEditorBase editor)
    {
        setEditor(editor);
    }

    /**
     * Returns the information which should be presented when a hover popup is shown for the specified hover region. The
     * hover region has the same semantics as the region returned by <code>getHoverRegion</code>. If the returned
     * information is <code>null</code> or empty no hover popup will be shown.
     *
     * @deprecated
     * @param textViewer  the viewer on which the hover popup should be shown
     * @param hoverRegion the text range in the viewer which is used to determine the hover display information
     * @return the hover popup display information
     */
    @Override
    public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion)
    {
        Object hoverInfo2 = getHoverInfo2(textViewer, hoverRegion);
        return hoverInfo2 == null ? null : hoverInfo2.toString();
    }

    @Override
    public Object getHoverInfo2(ITextViewer textViewer, IRegion hoverRegion)
    {
        if (hyperlinkDetectors == null) {
            hyperlinkDetectors = editor.getViewerConfiguration().getHyperlinkDetectors(editor.getViewer());
        }
/*
        IAnnotationModel model = textViewer instanceof ISourceViewer ? ((ISourceViewer) textViewer).getAnnotationModel() : null;
        //avoids finding annotations again
        if (annotations.size() == 0) {
            findAnnotations(hoverRegion.getOffset(), model, null, 0);
        }
*/
/*
        for (IHyperlinkDetector hyperlinkDetector : hyperlinkDetectors) {
            IHyperlink[] hyperlinks = hyperlinkDetector.detectHyperlinks(textViewer, hoverRegion, false);
            if (!CommonUtils.isEmpty(hyperlinks)) {
                return hyperlinks[0];
            }
        }
*/
        return null;
    }

    /**
     * Returns the text region which should serve as the source of information to compute the hover popup display
     * information. The popup has been requested for the given offset.
     * <p/>
     * For example, if hover information can be provided on a per method basis in a source viewer, the offset should be
     * used to find the enclosing method and the source range of the method should be returned.
     *
     * @param textViewer the viewer on which the hover popup should be shown
     * @param offset     the offset for which the hover request has been issued
     * @return the hover region used to compute the hover display information
     */
    @Override
    public IRegion getHoverRegion(ITextViewer textViewer, int offset)
    {
        return SQLWordFinder.findWord(textViewer.getDocument(), offset);
    }

    /**
     * Returns the text which should be presented in the a hover popup window. This information is requested based on
     * the specified line number.
     *
     * @param sourceViewer the source viewer this hover is registered with
     * @param lineNumber   the line number for which information is requested
     * @return the requested information or <code>null</code> if no such information exists
     */
    @Override
    public String getHoverInfo(ISourceViewer sourceViewer, int lineNumber)
    {
        if (annotations.size() == 0) {
            findAnnotations(-1, sourceViewer.getAnnotationModel(), sourceViewer.getDocument(), lineNumber);
        }

        return null;//getHoverInfo();
    }

    /**
     * Finds annotations either by offset or by lineNumber
     */
    private void findAnnotations(int offset, IAnnotationModel model, IDocument document, int lineNumber)
    {
        annotations.clear();
        if (model == null) {
            if (editor != null) {
                ITextEditor editor = this.editor;
                model = editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
            }
        }
        if (model == null) {
            return;
        }
        for (Iterator<?> it = model.getAnnotationIterator(); it.hasNext();) {
            Annotation annotation = (Annotation) it.next();
            Position position = model.getPosition(annotation);

            //if position is null, just return.
            if (position == null) {
                return;
            }
            try {
                if (position.overlapsWith(offset, 1) || document != null
                    && document.getLineOfOffset(position.offset) == lineNumber) {
                    annotations.add(annotation);
                }
            }
            catch (BadLocationException e) {
                log.error(e);
            }
        }
    }
/*

    private String getHoverInfo()
    {
        String text = null;
        IPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        for (Annotation annotation : annotations) {
            if (annotation instanceof MarkerAnnotation) {
                try {
                    IMarker marker = ((MarkerAnnotation) annotation).getMarker();
                    if (marker.getType().equals(SQLConstants.SYNTAX_MARKER_TYPE)
                        || marker.getType().equals(SQLConstants.PORTABILITY_MARKER_TYPE)) {
                        if (store.getBoolean(SQLPreferenceConstants.SHOW_SYNTAX_ERROR_DETAIL)) {
                            text = (String) marker.getBinding(IMarker.MESSAGE);
                        } else {
                            text = (String) marker.getBinding(SQLConstants.SHORT_MESSAGE);
                        }
                        //TODO: consider combine multiple annotations
                        break;
                    }

                }
                catch (CoreException e) {
                    log.error(e);
                }
            }
        }
        annotations.clear();
        return text;
    }
*/

    @Override
    public void setEditor(IEditorPart editor)
    {
        this.editor = (SQLEditorBase) editor;
    }

    public IInformationControlCreator getHoverControlCreator()
    {
        return new IInformationControlCreator() {
            @Override
            public IInformationControl createInformationControl(Shell parent)
            {
                DefaultInformationControl control = new DefaultInformationControl(parent, true);
                control.setSizeConstraints(60, 10);
                return control;
            }
        };
    }

}