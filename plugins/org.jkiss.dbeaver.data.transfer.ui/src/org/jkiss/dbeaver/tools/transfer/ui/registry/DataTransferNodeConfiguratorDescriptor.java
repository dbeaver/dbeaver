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

package org.jkiss.dbeaver.tools.transfer.ui.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.wizard.IWizardPage;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.utils.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * DataTransferNodeDescriptor
 */
public class DataTransferNodeConfiguratorDescriptor extends AbstractDescriptor
{
    private static final Log log = Log.getLog(DataTransferNodeConfiguratorDescriptor.class);

    @NotNull
    private final String id;
    private final List<DataTransferPageDescriptor> pageTypes = new ArrayList<>();

    DataTransferNodeConfiguratorDescriptor(IConfigurationElement config)
    {
        super(config);

        this.id = config.getAttribute("node");
        loadNodeConfigurations(config);
    }

    void loadNodeConfigurations(IConfigurationElement config) {
        for (IConfigurationElement pageConfig : ArrayUtils.safeArray(config.getChildren("page"))) {
            pageTypes.add(new DataTransferPageDescriptor(pageConfig));
        }
    }

    @NotNull
    public String getId()
    {
        return id;
    }

    List<DataTransferPageDescriptor> patPageDescriptors() {
        return pageTypes;
    }

    public DataTransferPageDescriptor getPageDescriptor(IWizardPage page) {
        for (DataTransferPageDescriptor pd : pageTypes) {
            if (pd.getPageClass().getImplName().equals(page.getClass().getName())) {
                return pd;
            }
        }
        return null;
    }

    public IWizardPage[] createWizardPages(IWizardPage[] existingPages, boolean consumerOptional, boolean producerOptional, boolean settingsPage)
    {
        List<IWizardPage> pages = new ArrayList<>();
        for (DataTransferPageDescriptor page : pageTypes) {
            if (page.isConsumerSelector() && !consumerOptional) continue;
            if (page.isProducerSelector() && !producerOptional) continue;
            if (settingsPage != (page.getPageType() == DataTransferPageType.SETTINGS)) continue;
            try {
                ObjectType type = page.getPageClass();
                type.checkObjectClass(IWizardPage.class);

                Class<?> pageClass = type.getObjectClass();
                boolean added = false;
                if (!ArrayUtils.isEmpty(existingPages)) {
                    for (IWizardPage ep : existingPages) {
                        if (pageClass == ep.getClass()) {
                            pages.add(ep);
                            added = true;
                            break;
                        }
                    }
                }
                if (!added) {
                    pages.add(type.createInstance(IWizardPage.class));
                }
            } catch (Throwable e) {
                log.error("Can't create wizard page", e);
            }
        }
        return pages.toArray(new IWizardPage[0]);
    }

    @Override
    public String toString() {
        return id;
    }

}
