/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.source.IAnnotationPresentation;
import org.eclipse.jface.text.source.ImageUtilities;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.DBeaverIcons;

public class SQLProblemAnnotation extends MarkerAnnotation implements IAnnotationPresentation {
    public static final String MARKER_TYPE = "org.jkiss.dbeaver.ui.editors.sql.databaseScriptProblemMarker";
    public static final String TYPE = "org.eclipse.ui.workbench.texteditor.error";

    public SQLProblemAnnotation(@NotNull IMarker marker) {
        super(TYPE, marker);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void paint(GC gc, Canvas canvas, Rectangle r) {
        ImageUtilities.drawImage(DBeaverIcons.getImage(DBIcon.TINY_ERROR), gc, canvas, r, SWT.CENTER, SWT.TOP);
    }

}
