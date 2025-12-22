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

import org.jkiss.dbeaver.model.runtime.features.DBRFeature;

/**
 * Data editor features
 */
public interface DataEditorFeatures {

    DBRFeature CATEGORY_DATA_EDITOR = DBRFeature.createCategory("Data editor", "Data editor features");

    DBRFeature DATA_EDIT_OPEN = DBRFeature.createFeature(CATEGORY_DATA_EDITOR, "Open data editor");

    DBRFeature CATEGORY_RESULT_SET_VIEWER = DBRFeature.createCategory(CATEGORY_DATA_EDITOR, "Result set viewer", "Result set viewer features");
    DBRFeature RESULT_SET_PANEL_OPEN = DBRFeature.createFeature(CATEGORY_RESULT_SET_VIEWER, "Open result set panel");
    DBRFeature RESULT_SET_REFRESH = DBRFeature.createFeature(CATEGORY_RESULT_SET_VIEWER, "Refresh result set");
    DBRFeature RESULT_SET_SCROLL = DBRFeature.createFeature(CATEGORY_RESULT_SET_VIEWER, "Scroll result set");

    DBRFeature RESULT_SET_PANEL_GROUPING = DBRFeature.createFeature(CATEGORY_RESULT_SET_VIEWER, "Use group by panel");
    DBRFeature RESULT_SET_PANEL_CALC = DBRFeature.createFeature(CATEGORY_RESULT_SET_VIEWER, "Use calc panel");
    DBRFeature RESULT_SET_PANEL_REFS = DBRFeature.createFeature(CATEGORY_RESULT_SET_VIEWER, "Use references panel");
    DBRFeature RESULT_SET_PANEL_METADATA = DBRFeature.createFeature(CATEGORY_RESULT_SET_VIEWER, "Use metadata panel");

}
