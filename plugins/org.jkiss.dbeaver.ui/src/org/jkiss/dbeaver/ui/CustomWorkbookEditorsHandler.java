/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui;

import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.internal.EditorReference;
import org.eclipse.ui.internal.WorkbenchPartReference;
import org.eclipse.ui.internal.WorkbookEditorsHandler;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

public class CustomWorkbookEditorsHandler extends WorkbookEditorsHandler {
    private String pattern;

    @Override
    protected ViewerFilter getFilter() {
        return new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                return element instanceof EditorReference
                    && pattern != null
                    && SearchCellLabelProvider.matches(pattern, ((EditorReference) element).getTitle());
            }
        };
    }

    @Override
    protected void setLabelProvider(TableViewerColumn column) {
        column.setLabelProvider(new SearchCellLabelProvider() {
            @NotNull
            @Override
            public String getText(@NotNull Object element) {
                return getWorkbenchPartReferenceText((WorkbenchPartReference) element);
            }

            @NotNull
            @Override
            public Image getImage(@NotNull Object element) {
                return ((WorkbenchPartReference) element).getTitleImage();
            }

            @Override
            public String getToolTipText(@NotNull Object element) {
                return ((WorkbenchPartReference) element).getTitleToolTip();
            }

            @Nullable
            @Override
            public String getPattern() {
                return pattern;
            }
        });

        ColumnViewerToolTipSupport.enableFor(column.getViewer());
    }

    @Override
    protected void setMatcherString(String pattern) {
        this.pattern = pattern;

    }
}
