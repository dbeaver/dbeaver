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
package org.jkiss.dbeaver.ext.erd.model;

import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.gef.EditPartFactory;
import org.eclipse.gef.palette.PaletteRoot;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;

/**
 * ERD object adapter
 */
public interface ERDDecorator {

    boolean showCheckboxes();

    boolean allowEntityDuplicates();

    /**
     * Margin around entity figure. This affects diagram connections layout
     */
    Insets getDefaultEntityInsets();

    @NotNull
    EditPartFactory createPartFactory();

    void fillPalette(@NotNull PaletteRoot paletteRoot, boolean readOnly);

    /**
     * Create default entity attributes
     */
    void fillEntityFromObject(@NotNull DBRProgressMonitor monitor, @NotNull EntityDiagram diagram, @NotNull ERDEntity erdEntity);

    @Nullable
    ERDAssociation createAutoAssociation(ERDContainer diagram, @NotNull DBSEntityAssociation association, @NotNull ERDEntity sourceEntity, @NotNull ERDEntity targetEntity, boolean reflect);

}
