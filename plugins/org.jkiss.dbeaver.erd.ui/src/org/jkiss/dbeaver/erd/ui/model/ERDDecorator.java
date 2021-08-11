/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.erd.ui.model;

import org.eclipse.draw2dl.geometry.Dimension;
import org.eclipse.draw2dl.geometry.Insets;
import org.eclipse.gef3.EditPartFactory;
import org.eclipse.gef3.palette.PaletteRoot;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

/**
 * ERD object adapter
 */
public interface ERDDecorator {

    boolean showCheckboxes();

    boolean supportsAttributeVisibility();

    boolean supportsStructureEdit();

    /**
     * Margin around entity figure. This affects diagram connections layout
     */
    @NotNull
    Insets getDefaultEntityInsets();

    /**
     * Snap size for entity figures. Rounds entity position to this snap size.
     */
    @Nullable
    Dimension getEntitySnapSize();

    @NotNull
    EditPartFactory createPartFactory();

    void fillPalette(@NotNull PaletteRoot paletteRoot, boolean readOnly);

}
