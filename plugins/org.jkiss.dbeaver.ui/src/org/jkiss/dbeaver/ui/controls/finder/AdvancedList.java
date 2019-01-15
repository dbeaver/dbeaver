/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.controls.finder;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * AdvancedList
 */
public class AdvancedList extends Composite {
    private static final Log log = Log.getLog(AdvancedList.class);

    private Point itemSize = new Point(100, 100);

    private List<AdvancedListItem> items = new ArrayList<>();

    public AdvancedList(Composite parent, int style) {
        super(parent, style);

        RowLayout layout = new RowLayout(SWT.HORIZONTAL);
        layout.wrap = true;
        layout.fill = true;
        setLayout(layout);
    }

    void addItem(AdvancedListItem item) {
        items.add(item);
    }

    public AdvancedListItem[] getItems() {
        return items.toArray(new AdvancedListItem[0]);
    }

}
