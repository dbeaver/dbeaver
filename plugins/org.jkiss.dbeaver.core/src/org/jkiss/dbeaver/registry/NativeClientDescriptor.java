/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.model.DBWorkbench;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.runtime.OSDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * NativeClientDescriptor
 */
public class NativeClientDescriptor extends AbstractDescriptor {

    private final List<NativeClientDistributionDescriptor> distributions = new ArrayList<>();
    private String id;
    private String label;

    NativeClientDescriptor(IConfigurationElement config) {
        super(config.getContributor().getName());
        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.label = config.getAttribute(RegistryConstants.ATTR_LABEL);
        for (IConfigurationElement clientElement : config.getChildren("dist")) {
            this.distributions.add(new NativeClientDistributionDescriptor(clientElement));
        }
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public NativeClientDistributionDescriptor findDistribution() {
        OSDescriptor localSystem = DBWorkbench.getPlatform().getLocalSystem();
        for (NativeClientDistributionDescriptor distr : distributions) {
            if (distr.getOs().matches(localSystem)) {
                return distr;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return id;
    }

}
