/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.IToolTipProvider;
import org.eclipse.swt.graphics.Image;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

public interface IGridLabelProvider extends IColorProvider, IFontProvider, IToolTipProvider {

    String OPTION_EXCLUDE_COLUMN_NAME_FOR_WIDTH_CALC = "OPTION_EXCLUDE_COLUMN_NAME_FOR_WIDTH_CALC";

    @NotNull
    String getText(Object element);

    @Nullable
    String getDescription(Object element);

    @Nullable
    Image getImage(Object element);

    Object getGridOption(String option);

}
