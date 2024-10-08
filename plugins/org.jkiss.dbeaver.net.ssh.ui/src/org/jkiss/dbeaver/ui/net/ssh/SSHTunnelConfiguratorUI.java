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
package org.jkiss.dbeaver.ui.net.ssh;

import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.ObjectPropertyConfiguratorWrapper;

/**
 * SSH tunnel configuration
 */
public class SSHTunnelConfiguratorUI extends ObjectPropertyConfiguratorWrapper<Object, DBWHandlerConfiguration> {

    @Override
    protected IObjectPropertyConfigurator<Object, DBWHandlerConfiguration> createConfigurator() {
        return switch (this.getEditIntention()) {
            case DEFAULT -> new SSHTunnelDefaultConfiguratorUI();
            case CREDENTIALS_ONLY -> new SSHTunnelCredsOnlyConfiguratorUI();
        };
    }

}
