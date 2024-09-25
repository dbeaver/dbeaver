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
package org.jkiss.dbeaver.ui.controls.lightgrid;

/**
 * Nested grid row
 */
class GridRowNested implements IGridRow {

    private final IGridRow parent;
    private final int position;
    private final int index;
    private final int level;
    private final Object element;

    public GridRowNested(IGridRow parent, int position, int index, int level, Object element) {
        this.parent = parent;
        this.position = position;
        this.index = index;
        this.level = level;
        this.element = element;
    }

    @Override
    public Object getElement() {
        return element;
    }

    @Override
    public IGridRow getParent() {
        return parent;
    }

    @Override
    public int getVisualPosition() {
        return position;
    }

    @Override
    public int getRelativeIndex() {
        return index;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public String toString() {
        final String[] parts = new String[getLevel() + 1];
        for (IGridRow r = this; r != null; r = r.getParent()) {
            parts[r.getLevel()] = String.valueOf(r.getRelativeIndex() + 1);
        }
        return String.join(".", parts);
    }
}