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

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Font;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.EditorReference;
import org.eclipse.ui.internal.IWorkbenchThemeConstants;
import org.eclipse.ui.internal.WorkbenchPartReference;
import org.eclipse.ui.internal.WorkbookEditorsHandler;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CustomWorkbookEditorsHandler extends WorkbookEditorsHandler {
    private String pattern;

    @Override
    protected ViewerFilter getFilter() {
        return new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                if (pattern == null || !(viewer instanceof TableViewer)) {
                    return true;
                }
                String name = null;
                if (element instanceof EditorReference) {
                    name = ((EditorReference) element).getTitle();
                }
                if (name == null) {
                    return false;
                }
                return match(pattern, name) != null;
            }
        };
    }

    @Override
    protected void setLabelProvider(TableViewerColumn column) {
        column.setLabelProvider(new StyledCellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                if (cell.getElement() instanceof WorkbenchPartReference) {
                    final WorkbenchPartReference ref = (WorkbenchPartReference) cell.getElement();
                    final String text = getWorkbenchPartReferenceText(ref);

                    cell.setText(text);
                    cell.setImage(ref.getTitleImage());

                    final List<int[]> ranges;

                    if (CommonUtils.isEmpty(pattern)) {
                        ranges = Collections.emptyList();
                    } else {
                        ranges = match(pattern, text);
                    }

                    if (CommonUtils.isEmpty(ranges)) {
                        cell.setStyleRanges(null);
                    } else {
                        final Font font = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getFontRegistry().getItalic(IWorkbenchThemeConstants.TAB_TEXT_FONT);
                        final StyledString.Styler styler = new BoldStylerProvider(font).getBoldStyler();
                        final StyledString ss = new StyledString(text);
                        for (int[] range : ranges) {
                            ss.setStyle(range[0], range[1], styler);
                        }
                        cell.setStyleRanges(ss.getStyleRanges());
                    }

                    cell.getControl().redraw();
                }
            }

            @Override
            public String getToolTipText(Object element) {
                if (element instanceof WorkbenchPartReference) {
                    return ((WorkbenchPartReference) element).getTitleToolTip();
                } else {
                    return super.getToolTipText(element);
                }
            }
        });

        ColumnViewerToolTipSupport.enableFor(column.getViewer());
    }

    @Override
    protected void setMatcherString(String pattern) {
        this.pattern = pattern;
    }

    @Nullable
    private static List<int[]> match(@NotNull String pattern, @NotNull String value) {
        final List<int[]> ranges = new ArrayList<>();
        for (int p = 0, v = 0, start = -1; p <= pattern.length() && v <= value.length(); v++) {
            if (p != pattern.length() && v == value.length()) {
                return null;
            }
            if (p < pattern.length() && Character.toLowerCase(pattern.charAt(p)) == Character.toLowerCase(value.charAt(v))) {
                if (start < 0) {
                    start = v;
                }
                p++;
            } else if (start >= 0) {
                ranges.add(new int[]{start, v - start});
                start = -1;
            }
        }
        return ranges;
    }
}
