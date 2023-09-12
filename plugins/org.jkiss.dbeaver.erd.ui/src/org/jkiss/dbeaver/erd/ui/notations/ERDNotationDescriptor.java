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
package org.jkiss.dbeaver.erd.ui.notations;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

public class ERDNotationDescriptor {
    public static final String ERD_STYLE_NOTATION_EXT_ATTRIBUTE_ID = "id"; //$NON-NLS-N$
    public static final String ERD_STYLE_NOTATION_EXT_ATTRIBUTE_NAME = "name"; //$NON-NLS-N$
    public static final String ERD_STYLE_NOTATION_EXT_ATTRIBUTE_DESCRIPTION = "description"; //$NON-NLS-N$
    public static final String ERD_STYLE_NOTATION_EXT_ATTRIBUTE_NOTATION = "notation"; //$NON-NLS-N$

    private String id;
    private String name;
    private String description;
    private ERDNotation notation;

    public ERDNotationDescriptor(IConfigurationElement cf) throws CoreException {
        this.id = cf.getAttribute(ERD_STYLE_NOTATION_EXT_ATTRIBUTE_ID);
        this.name = cf.getAttribute(ERD_STYLE_NOTATION_EXT_ATTRIBUTE_NAME);
        this.description = cf.getAttribute(ERD_STYLE_NOTATION_EXT_ATTRIBUTE_DESCRIPTION);
        this.notation = (ERDNotation) cf.createExecutableExtension(ERD_STYLE_NOTATION_EXT_ATTRIBUTE_NOTATION);
    }

    public ERDNotationDescriptor(String id, String name, String description, ERDNotation notation) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.notation = notation;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ERDNotation getNotation() {
        return notation;
    }

    public void setNotation(ERDNotation notation) {
        this.notation = notation;
    }

}
