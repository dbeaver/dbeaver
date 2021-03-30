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
package org.jkiss.dbeaver.model.impl.app;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.utils.CommonUtils;

/**
 * DBeaver application descriptor.
 */
public class ApplicationDescriptor extends AbstractDescriptor {

    private final String id;
    private final String productFamily;
    private final String licenseProductId;
    private final String name;
    private final String description;
    private final String parentId;
    private final String[] umbrellaProductIds;
    private boolean serverApplication;
    private ApplicationDescriptor parent;
    private boolean finalApplication = true;

    ApplicationDescriptor(IConfigurationElement config) {
        super(config);
        this.id = config.getAttribute("id");
        this.productFamily = CommonUtils.toString(config.getAttribute("family"), "DB");
        this.licenseProductId = CommonUtils.toString(config.getAttribute("licenseProductId"), this.id);
        this.name = config.getAttribute("name");
        this.description = config.getAttribute("description");
        this.parentId = config.getAttribute("parent");
        String umbrella = config.getAttribute("umbrella");
        if (!CommonUtils.isEmptyTrimmed(umbrella)) {
            this.umbrellaProductIds = umbrella.split(",");
        } else {
            this.umbrellaProductIds = new String[0];
        }
        this.serverApplication = CommonUtils.toBoolean(config.getAttribute("server"));
    }

    public String getId() {
        return id;
    }

    public String getLicenseProductId() {
        return licenseProductId;
    }

    public String getProductFamily() {
        return productFamily;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ApplicationDescriptor getParent() {
        return parent;
    }

    void setParent(ApplicationDescriptor parent) {
        this.parent = parent;
        this.parent.finalApplication = false;
    }

    public String[] getUmbrellaProductIds() {
        return umbrellaProductIds;
    }

    public boolean isServerApplication() {
        return serverApplication;
    }

    boolean isFinalApplication() {
        return finalApplication;
    }

    String getParentId() {
        return parentId;
    }

}
