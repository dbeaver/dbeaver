/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver;

import org.eclipse.jgit.annotations.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.editors.sql.SQLNewScriptTemplateVariablesResolver;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;
import java.util.stream.Collectors;

public class ConnectionNameResolver extends SQLNewScriptTemplateVariablesResolver {
    public static final String VAR_DEFAULT = "host_or_database";
    private final DataSourceDescriptor descriptor;

    public static Set<String> getConnectionVariables() {
        final Set<String> strings = Arrays.stream(ArrayUtils.concatArrays(DBPConnectionConfiguration.ALL_VARIABLES, new String[]{VAR_DEFAULT})).collect(Collectors.toSet());
        strings.remove(DBPConnectionConfiguration.VARIABLE_DATASOURCE);
        strings.remove(DBPConnectionConfiguration.VARIABLE_SERVER);
        return strings;
    };

    public static List<List<String>> getConnectionVariablesInfo() {
        final String[][] strings = ArrayUtils.concatArrays(
            ALL_VARIABLES_INFO,
            new String[][]{
                {VAR_DEFAULT, "Legacy name"},
            });
        List<List<String>> list = new ArrayList<>();
        for (String[] string : strings) {
            if (!string[0].equals(DBPConnectionConfiguration.VARIABLE_DATASOURCE)) {
                list.add(List.of(string));
            }
        }
        return list;
    }

    public ConnectionNameResolver(DBPDataSourceContainer dataSourceContainer, DBPConnectionConfiguration configuration, @Nullable DataSourceDescriptor descriptor) {
        super(dataSourceContainer, configuration);
        this.descriptor = descriptor;
    }

    private String generateLegacyConnectionName() {
        String newName = descriptor == null ? "" : getDataSourceContainer().getName(); //$NON-NLS-1$
        if (CommonUtils.isEmpty(newName)) {
            newName = getConfiguration().getDatabaseName();
            if (CommonUtils.isEmpty(newName) || newName.length() < 3 || CommonUtils.isInt(newName)) {
                // Database name is too short or not a string
                newName = getConfiguration().getHostName();
            }
            if (CommonUtils.isEmpty(newName)) {
                newName = getConfiguration().getServerName();
            }
            if (CommonUtils.isEmpty(newName)) {
                newName = getDataSourceContainer().getDriver().getName();
            }
            if (CommonUtils.isEmpty(newName)) {
                newName = CoreMessages.dialog_connection_wizard_final_default_new_connection_name;
            }
            StringTokenizer st = new StringTokenizer(newName, "/\\:,?=%$#@!^&*()"); //$NON-NLS-1$
            while (st.hasMoreTokens()) {
                newName = st.nextToken();
            }
            //newName = settings.getDriver().getName() + " - " + newName; //$NON-NLS-1$
            newName = CommonUtils.truncateString(newName, 50);
        }
        return newName;
    }

    @Override
    public String get(String name) {
        if (name.equals(VAR_DEFAULT)) {
            return generateLegacyConnectionName();
        }
        return super.get(name);
    }
}
