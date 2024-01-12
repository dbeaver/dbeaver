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
package org.jkiss.dbeaver.erd.ui.notations;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.erd.ui.ERDUIConstants;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;

public class ERDNotationDescriptor extends AbstractDescriptor {

    private String id;
    private String name;
    private String description;
    private ERDNotation notation;
    private ObjectType lazyWrapper;

    Log log = Log.getLog(ERDNotationDescriptor.class.getName());

    protected ERDNotationDescriptor(IConfigurationElement cf) throws CoreException {
        super(cf);
        this.id = cf.getAttribute(RegistryConstants.ATTR_ID);
        this.name = cf.getAttribute(RegistryConstants.ATTR_NAME);
        this.description = cf.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.lazyWrapper = new ObjectType(cf.getAttribute(ERDUIConstants.ATTR_ERD_NOTATION));
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

    /**
     * The method instantiate ERDNotation object
     *
     * @return - Notation instance
     */
    public ERDNotation getNotation() {
        if (notation == null) {
            try {
                notation = lazyWrapper.createInstance(ERDNotation.class);
            } catch (DBException e) {
                log.error(e.getMessage());
            }
        }
        return notation;
    }

}
