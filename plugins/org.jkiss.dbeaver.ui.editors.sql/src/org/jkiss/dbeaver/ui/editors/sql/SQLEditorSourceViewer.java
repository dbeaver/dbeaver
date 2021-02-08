/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.hyperlink.IHyperlinkPresenter;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;

public class SQLEditorSourceViewer extends ProjectionViewer {

    /**
     * Creates an instance of this class with the given parameters.
     *
     * @param parent the SWT parent control
     * @param ruler the vertical ruler (annotation area)
     * @param overviewRuler the overview ruler
     * @param showsAnnotationOverview <code>true</code> if the overview ruler should be shown
     * @param styles the SWT style bits
     */
    public SQLEditorSourceViewer( Composite parent, IVerticalRuler ruler,
            IOverviewRuler overviewRuler, boolean showsAnnotationOverview,
            int styles ) {
        super( parent, ruler, overviewRuler, showsAnnotationOverview, styles );
    }

    void refreshTextSelection(){
        ITextSelection selection = (ITextSelection)getSelection();
        fireSelectionChanged(selection.getOffset(), selection.getLength());
    }

    @Override
    protected StyledText createTextWidget(Composite parent, int styles) {
        StyledText textWidget = super.createTextWidget(parent, styles);
        //textWidget.setAlwaysShowScrollBars(false);
        return textWidget;
    }

    // Let source viewer reconfiguration possible (https://dbeaver.io/forum/viewtopic.php?f=2&t=2939)
    public void setHyperlinkPresenter(IHyperlinkPresenter hyperlinkPresenter) throws IllegalStateException {
        if (fHyperlinkManager != null) {
            fHyperlinkManager.uninstall();
            fHyperlinkManager= null;
        }
        super.setHyperlinkPresenter(hyperlinkPresenter);
    }

}