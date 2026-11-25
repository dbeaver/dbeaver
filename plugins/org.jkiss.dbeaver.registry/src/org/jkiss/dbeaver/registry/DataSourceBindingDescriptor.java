/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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

import org.apache.commons.jexl3.JexlExpression;
import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Datasource binding descriptor
 */
public class DataSourceBindingDescriptor extends AbstractContextDescriptor {
    private static final Log log = Log.getLog(DataSourceBindingDescriptor.class);

    public static class DataSourceInfo {
        @Nullable
        private final String id;
        @Nullable
        private final String driver;
        @Nullable
        private final String condition;
        @Nullable
        private JexlExpression expression;
        private volatile boolean expressionInitialized;

        DataSourceInfo(@NotNull IConfigurationElement cfg) {
            this.condition = cfg.getAttribute("if");
            this.id = cfg.getAttribute("id");
            this.driver = cfg.getAttribute("driver");
        }

        public boolean appliesTo(@NotNull DBPDriver driver, @Nullable Object context) {
            if (!CommonUtils.isEmpty(id) && !driver.getProviderDescriptor().matchesId(id)) {
                return false;
            }
            if (!CommonUtils.isEmpty(this.driver) && !driver.matchesId(this.driver)) {
                return false;
            }
            JexlExpression expression = getExpression();
            if (expression != null) {
                try {
                    return CommonUtils.toBoolean(
                        expression.evaluate(makeContext(driver, context)));
                } catch (Exception e) {
                    log.debug("Error evaluating binding expression '" + expression + "'", e);
                    return false;
                }
            }
            return true;
        }

        @Nullable
        private JexlExpression getExpression() {
            String condition = this.condition;
            if (condition == null || condition.isEmpty()) {
                return null;
            }
            if (!expressionInitialized) {
                synchronized (this) {
                    if (!expressionInitialized) {
                        try {
                            expression = parseExpression(condition);
                        } catch (DBException ex) {
                            log.warn("Can't parse auth model datasource expression: " + condition, ex); //$NON-NLS-1$
                        }
                        expressionInitialized = true;
                    }
                }
            }
            return expression;
        }
    }

    @NotNull
    private final List<DataSourceInfo> dataSources = new ArrayList<>();

    public DataSourceBindingDescriptor(@NotNull IConfigurationElement config) {
        super(config);

        for (IConfigurationElement dsConfig : config.getChildren("datasource")) {
            this.dataSources.add(new DataSourceInfo(dsConfig));
        }
    }

    public boolean isDriverApplicable(@NotNull DBPDriver driver) {
        if (dataSources.isEmpty()) {
            return true;
        }
        for (DataSourceInfo dsi : dataSources) {
            if (dsi.appliesTo(driver, null)) {
                return true;
            }
        }
        return false;
    }

}
