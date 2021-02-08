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
package org.jkiss.dbeaver.registry.driver;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.SystemVariablesResolver;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * DriverDescriptorSerializer
 */
public class DriverDescriptorSerializer {

    static String replacePathVariables(String path) {
        return GeneralUtils.replaceVariables(path, new DriverVariablesResolver());
    }

    static String substitutePathVariables(Map<String, String> pathSubstitutions, String path) {
        for (Map.Entry<String, String> ps : pathSubstitutions.entrySet()) {
            if (path.startsWith(ps.getKey())) {
                path = GeneralUtils.variablePattern(ps.getValue()) + path.substring(ps.getKey().length());
                break;
            }
        }
        return path;
    }

    @NotNull
    protected Map<String, String> getPathSubstitutions() {
        Map<String, String> pathSubstitutions = new HashMap<>();
        {
            DriverVariablesResolver varResolver = new DriverVariablesResolver();
            String[] variables = new String[]{
                DriverVariablesResolver.VAR_DRIVERS_HOME,
                SystemVariablesResolver.VAR_WORKSPACE,
                SystemVariablesResolver.VAR_HOME,
                SystemVariablesResolver.VAR_APP_PATH,
                SystemVariablesResolver.VAR_DBEAVER_HOME};
            for (String varName : variables) {
                String varValue = varResolver.get(varName);
                if (!CommonUtils.isEmpty(varValue)) {
                    pathSubstitutions.put(varValue, varName);
                }
            }
        }
        return pathSubstitutions;
    }

}
