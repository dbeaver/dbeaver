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
package org.jkiss.dbeaver.ui.controls.lightgrid;

/**
 * Nested grid row
 */
class GridRowNested implements IGridRow {

    private final IGridRow parent;
    private final int position;

    public GridRowNested(IGridRow parent, int position) {
        this.parent = parent;
        this.position = position;
    }

    @Override
    public Object getElement() {
        return parent.getElement();
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
        return position - parent.getVisualPosition() - 1;
    }

    @Override
    public int getRowDepth() {
        return parent.getRowDepth() + 1;
    }

    @Override
    public String toString() {
        return position + ":" + getRelativeIndex();
    }
}