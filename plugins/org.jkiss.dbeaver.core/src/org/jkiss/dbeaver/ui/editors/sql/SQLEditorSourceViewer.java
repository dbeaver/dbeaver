/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
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

}