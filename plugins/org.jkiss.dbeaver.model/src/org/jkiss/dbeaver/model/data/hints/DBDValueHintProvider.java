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

package org.jkiss.dbeaver.model.data.hints;

/**
 * Abstract hint provider
 */
public interface DBDValueHintProvider {

    /**
     * Hint object: cell, column or row
     */
    enum HintObject {
        CELL,
        COLUMN,
        ROW,
    }

    int OPTION_INLINE = 1;          // Default tip for data viewer grid
    int OPTION_ADVANCED = 1 << 1;   // ?
    int OPTION_TOOLTIP = 1 << 2;    // Hint for tooltip. Maybe return different hint (extended)
    int OPTION_ACTION_TOOLTIP = 1 << 3; // Tooltip for hint action button
    int OPTION_ROW_EXPANDED = 1 << 4; // Passed row is expanded (has sub-rows)
    int OPTION_APPROXIMATE = 1 << 5; // Return hints ignoring actual row values

}
