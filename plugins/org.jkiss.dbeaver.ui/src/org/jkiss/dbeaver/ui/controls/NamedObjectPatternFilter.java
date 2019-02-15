/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2018-2019 Andrew Khitrin (ahitrin@gmail.com)
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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.dialogs.PatternFilter;
import org.jkiss.dbeaver.model.DBPNamedObject;

/**
 * Pattern filter which find named objects by mask
 */
public class NamedObjectPatternFilter extends PatternFilter {
    public NamedObjectPatternFilter() {
        setIncludeLeadingWildcard(true);
    }

    protected boolean isLeafMatch(Viewer viewer, Object element) {
        if (element instanceof DBPNamedObject) {
            return wordMatches(((DBPNamedObject) element).getName());
        }
        return false;
    }

}
