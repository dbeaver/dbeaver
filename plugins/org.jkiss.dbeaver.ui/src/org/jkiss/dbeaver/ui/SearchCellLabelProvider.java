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
package org.jkiss.dbeaver.ui;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * A label provider that highlights matching parts of a label against the supplied pattern.
 */
public abstract class SearchCellLabelProvider extends StyledCellLabelProvider implements ILabelProvider, IFontProvider {
    protected final Font boldFont;

    public static boolean matches(@NotNull String pattern, @NotNull String value) {
        return match(pattern, value) != null;
    }

    public SearchCellLabelProvider() {
        this.boldFont = UIUtils.makeBoldFont(Display.getCurrent().getSystemFont());
    }

    @Override
    public void update(@NotNull ViewerCell cell) {
        final String pattern = getPattern();
        final Object element = cell.getElement();
        final String text = getText(element);

        cell.setText(text);
        cell.setImage(getImage(element));
        cell.setFont(getFont(element));

        final List<int[]> ranges;

        if (CommonUtils.isEmpty(pattern)) {
            ranges = List.of();
        } else {
            ranges = match(pattern, text);
        }

        if (CommonUtils.isEmpty(ranges)) {
            cell.setStyleRanges(null);
        } else {
            final StyledString.Styler styler = new BoldStylerProvider(getMatchFont(element)).getBoldStyler();
            final StyledString ss = new StyledString(text);
            for (int[] range : ranges) {
                ss.setStyle(range[0], range[1], styler);
            }
            cell.setStyleRanges(ss.getStyleRanges());
        }

        super.update(cell);
    }

    @Nullable
    @Override
    public Font getFont(Object element) {
        return null;
    }

    @NotNull
    public Font getMatchFont(@NotNull Object element) {
        return boldFont;
    }

    @Nullable
    public abstract Image getImage(Object element);

    @NotNull
    public abstract String getText(Object element);

    @Nullable
    public abstract String getPattern();

    @Override
    public void dispose() {
        boldFont.dispose();
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
