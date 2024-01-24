/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.e4;

import org.eclipse.e4.ui.internal.workbench.renderers.swt.AbstractTableInformationControl;
import org.eclipse.e4.ui.internal.workbench.renderers.swt.BasicPartList;
import org.eclipse.e4.ui.model.application.ui.MDirtyable;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUILabel;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.renderers.swt.StackRenderer;
import org.eclipse.e4.ui.workbench.swt.internal.copy.SearchPattern;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.SearchCellLabelProvider;
import org.jkiss.dbeaver.ui.UIUtils;

import java.lang.reflect.Method;

public class DBeaverPartList extends BasicPartList {
    private static final Log log = Log.getLog(DBeaverPartList.class);

    private final StackRenderer renderer;

    public DBeaverPartList(
        @Nullable Shell parent,
        int shellStyle,
        int treeStyler,
        @NotNull EPartService partService,
        @NotNull MElementContainer<?> input,
        @NotNull StackRenderer renderer,
        boolean mru
    ) {
        super(parent, shellStyle, treeStyler, partService, input, renderer, mru);
        this.renderer = renderer;
    }

    @Override
    protected TableViewer createTableViewer(Composite parent, int style) {
        final Table table = new Table(parent, SWT.SINGLE | style & ~SWT.MULTI);
        table.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));

        final TableViewer viewer = new TableViewer(table);
        viewer.addFilter(new NamePatternFilter());
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.setLabelProvider(new CellLabelProvider());

        ColumnViewerToolTipSupport.enableFor(viewer);

        return viewer;
    }

    @Nullable
    private SearchPattern getMatcher() {
        try {
            final Method method = AbstractTableInformationControl.class.getDeclaredMethod("getMatcher");
            method.setAccessible(true);
            return (SearchPattern) method.invoke(this);
        } catch (Throwable e) {
            log.error("Error retrieving part list matcher", e);
            return null;
        }
    }

    private class NamePatternFilter extends ViewerFilter {
        @Override
        public boolean select(Viewer viewer, Object parentElement, Object element) {
            final SearchPattern matcher = getMatcher();
            if (matcher == null) {
                return true;
            }
            final ILabelProvider provider = (ILabelProvider) ((ContentViewer) viewer).getLabelProvider();
            final String name = provider.getText(element);
            return SearchCellLabelProvider.matches(matcher.getPattern(), name);
        }
    }

    private class CellLabelProvider extends SearchCellLabelProvider {
        private final Font italicFont;
        private final Font italicBoldFont;

        public CellLabelProvider() {
            this.italicFont = UIUtils.modifyFont(Display.getDefault().getSystemFont(), SWT.ITALIC);
            this.italicBoldFont = UIUtils.modifyFont(Display.getDefault().getSystemFont(), SWT.BOLD | SWT.ITALIC);
        }

        @Nullable
        @Override
        public String getPattern() {
            final SearchPattern matcher = getMatcher();
            if (matcher != null) {
                return matcher.getPattern();
            } else {
                return null;
            }
        }

        @NotNull
        @Override
        public String getText(@NotNull Object element) {
            if (element instanceof MDirtyable && ((MDirtyable) element).isDirty()) {
                return "*" + ((MUILabel) element).getLocalizedLabel();
            } else {
                return ((MUILabel) element).getLocalizedLabel();
            }
        }

        @Nullable
        @Override
        public Font getFont(Object element) {
            if (isShowing(element)) {
                return null;
            } else {
                return italicFont;
            }
        }

        @NotNull
        @Override
        public Font getMatchFont(@NotNull Object element) {
            if (isShowing(element)) {
                return boldFont;
            } else {
                return italicBoldFont;
            }
        }

        @NotNull
        @Override
        public Image getImage(@NotNull Object element) {
            return renderer.getImage((MUILabel) element);
        }

        @Override
        public String getToolTipText(Object element) {
            return renderer.getToolTip((MUILabel) element);
        }

        @Override
        public void dispose() {
            italicFont.dispose();
            italicBoldFont.dispose();
            super.dispose();
        }

        private boolean isShowing(@NotNull Object element) {
            final CTabItem item = renderer.findItemForPart((MPart) element);
            return item != null && item.isShowing();
        }
    }
}
