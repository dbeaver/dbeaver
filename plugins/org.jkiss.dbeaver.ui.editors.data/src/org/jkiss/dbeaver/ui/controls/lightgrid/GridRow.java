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
 * Grid row
 */
class GridRow implements IGridRow {

    private final Object element;
    private final int originalPosition;
    private final int position;

    public GridRow(Object element, int originalPosition, int position) {
        this.element = element;
        this.originalPosition = originalPosition;
        this.position = position;
    }

    @Override
    public Object getElement() {
        return element;
    }

    @Override
    public IGridRow getParent() {
        return null;
    }

    @Override
    public int getVisualPosition() {
        return position;
    }

    @Override
    public int getRelativeIndex() {
        return originalPosition;
    }

    @Override
    public int getLevel() {
        return 0;
    }

    @Override
    public String toString() {
        return String.valueOf(originalPosition + 1);
    }
}