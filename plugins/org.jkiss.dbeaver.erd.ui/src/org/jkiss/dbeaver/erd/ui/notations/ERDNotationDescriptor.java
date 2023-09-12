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
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;

public class ERDNotationDescriptor extends AbstractDescriptor {

    public static final String ERD_STYLE_NOTATION_EXT_ATTRIBUTE_ID = "id"; // $NON-NLS-N$
    public static final String ERD_STYLE_NOTATION_EXT_ATTRIBUTE_NAME = "name"; // $NON-NLS-N$
    public static final String ERD_STYLE_NOTATION_EXT_ATTRIBUTE_DESCRIPTION = "description"; // $NON-NLS-N$
    public static final String ERD_STYLE_NOTATION_EXT_ATTRIBUTE_DEFAULT = "isDefault"; // $NON-NLS-N$
    public static final String ERD_STYLE_NOTATION_EXT_ATTRIBUTE_NOTATION = "notation"; // $NON-NLS-N$

    private String id;
    private String name;
    private String description;
    private boolean isDefault = false;
    private ERDNotation notation;

    protected ERDNotationDescriptor(IConfigurationElement cf) throws CoreException {
        super(cf);
        this.id = cf.getAttribute(ERD_STYLE_NOTATION_EXT_ATTRIBUTE_ID);
        this.name = cf.getAttribute(ERD_STYLE_NOTATION_EXT_ATTRIBUTE_NAME);
        this.description = cf.getAttribute(ERD_STYLE_NOTATION_EXT_ATTRIBUTE_DESCRIPTION);
        this.isDefault = Boolean.valueOf(cf.getAttribute(ERD_STYLE_NOTATION_EXT_ATTRIBUTE_DEFAULT));
        this.notation = (ERDNotation) cf.createExecutableExtension(ERD_STYLE_NOTATION_EXT_ATTRIBUTE_NOTATION);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ERDNotation getNotation() {
        return notation;
    }

    public boolean isDefault() {
        return isDefault;
    }

}
