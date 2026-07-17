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
package org.jkiss.dbeaver.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.utils.CommonUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * JDBCUtils
 */
public class DatabaseURL {

    public static class Generic {

        public static final String TEMPLATE = "[jdbc:]{driver}://[{user}:{password}@]{host}[:{port}][/{database}]";

        public static final String TEMPLATE_WITH_PARAMS =
            "[jdbc:]{driver}://[{user}:{password}@]{host}[:{port}][/{database}][?{prop}={value}[&{prop}={value}...]]";

        public static final String TEMPLATE_WITH_PARAM_GROUPS =
            "[jdbc:]{driver}://[{user}:{password}@]{host}[:{port}][/{database}][?{param:{prop}={value}}[&{param:{prop}={value}}...]]";

        public static final String PARAM_GROUP = "param";
        public static final String PARAM_PROP = "prop";
        public static final String PARAM_VALUE = "value";

        @NotNull
        public static Map<String, String> extractExtraParams(@NotNull StringTemplate.ParamEntries root) {
            List<StringTemplate.ParamEntries> extraParamGroups = root.getGroups().get(PARAM_GROUP);
            if (extraParamGroups != null) {
                Map<String, String> params = new HashMap<>(extraParamGroups.size());
                for (StringTemplate.ParamEntries paramGroup : extraParamGroups) {
                    String paramName = paramGroup.getFirstParamValue(PARAM_PROP);
                    String paramValue = paramGroup.getFirstParamValue(PARAM_VALUE);
                    params.put(paramName, paramValue);
                }
                return params;
            } else {
                return Collections.emptyMap();
            }
        }
    }

    private static final Log log = Log.getLog(DatabaseURL.class);

    @NotNull
    private static final Object lock = new Object();

    @NotNull
    private static final WeakHashMap<String, StringTemplate> templates = new WeakHashMap<>();

    @NotNull
    private static StringTemplate getUrlTemplate(@NotNull String templateString) throws StringTemplate.StringTemplateFormatException {
        synchronized (lock) {
            StringTemplate template = templates.get(templateString);
            if (template == null) {
                template = StringTemplate.parseTemplate(templateString, p -> getPropertyRegex(p.name()));
                templates.put(templateString, template);
            }
            return template;
        }
    }

    @Nullable
    public static String generateUrlByTemplate(
        @NotNull DBPDriver driver, @NotNull DBPConnectionConfiguration cnnInfo
    ) throws DBException {
        String urlTemplate = driver.getSampleURL();
        if (CommonUtils.isEmpty(urlTemplate)) {
            throw new DBException("Cannot generate database URL with empty sample URL template for " + driver.getName());
        } else {
            return DatabaseURL.generateUrlByTemplate(urlTemplate, cnnInfo, Collections.emptyMap());
        }
    }

    @Nullable
    public static String generateUrlByTemplate(
        @NotNull String urlTemplate, @NotNull DBPConnectionConfiguration cnnInfo
    ) throws DBException {
        return DatabaseURL.generateUrlByTemplate(urlTemplate, cnnInfo, Collections.emptyMap());
    }

    @Nullable
    public static String generateUrlByTemplate(
        @NotNull String urlTemplate,
        @NotNull DBPConnectionConfiguration connectionInfo,
        @NotNull Map<String, String> extraParams
    ) throws DBException {
        if (!CommonUtils.isEmpty(connectionInfo.getUrl()) &&
            CommonUtils.isEmpty(connectionInfo.getHostPort()) &&
            CommonUtils.isEmpty(connectionInfo.getHostName()) &&
            CommonUtils.isEmpty(connectionInfo.getServerName()) &&
            CommonUtils.isEmpty(connectionInfo.getDatabaseName())) {
            // No parameters, just URL - so URL it is
            return connectionInfo.getUrl();
        }
        if (CommonUtils.isEmptyTrimmed(urlTemplate)) {
            return connectionInfo.getUrl();
        }

        Map<String, String> params = new HashMap<>(extraParams.size() + 10); // 7 builtin prams x default load factor 0.75
        params.putAll(extraParams);
        if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
            params.put(DBConstants.PROP_HOST, connectionInfo.getHostName());
        }
        if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
            params.put(DBConstants.PROP_PORT, connectionInfo.getHostPort());
        }
        if (!CommonUtils.isEmpty(connectionInfo.getServerName())) {
            params.put(DBConstants.PROP_SERVER, connectionInfo.getServerName());
        }
        if (!CommonUtils.isEmpty(connectionInfo.getDatabaseName())) {
            params.put(DBConstants.PROP_DATABASE, connectionInfo.getDatabaseName());
            params.put(DBConstants.PROP_FOLDER, connectionInfo.getDatabaseName());
            params.put(DBConstants.PROP_FILE, connectionInfo.getDatabaseName());
        }

        // Old logic was always using empty string when no username presented,
        // but this might bring unwanted tails of login-related optional fragments.
        // Let's complain when the username is required but not presented, and get rid of unwanted optional fragments.
        if (!CommonUtils.isEmpty(connectionInfo.getUserName())) {
            params.put(DBConstants.PROP_USER, connectionInfo.getUserName());
        }

        try {
            StringTemplate template = getUrlTemplate(urlTemplate);
            String result = template.prepareString(params);
            return result;
        } catch (StringTemplate.StringTemplateException ex) {
            throw new DBException("Failed to generate database URL by template " + ex.getTemplateString(), ex);
        }
    }

    @Nullable
    public static DBPConnectionConfiguration extractConfigurationFromUrl(@NotNull String sampleUrl, @NotNull String targetUrl) {
        Map<String, String> params;
        try {
            params = getUrlTemplate(sampleUrl).extractSingletonParametersMap(targetUrl);
        } catch (DBException e) {
            log.debug("Failed to extract configuration from the url", e);
            return null;
        }
        if (params == null || params.isEmpty()) {
            return null;
        }
        final DBPConnectionConfiguration configuration = new DBPConnectionConfiguration();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            switch (entry.getKey()) {
                case DBConstants.PROP_HOST:
                    configuration.setHostName(entry.getValue());
                    break;
                case DBConstants.PROP_PORT:
                    configuration.setHostPort(entry.getValue());
                    break;
                case DBConstants.PROP_DATABASE:
                case DBConstants.PROP_FOLDER:
                case DBConstants.PROP_FILE:
                    configuration.setDatabaseName(entry.getValue());
                    break;
                case DBConstants.PROP_SERVER:
                    configuration.setServerName(entry.getValue());
                    break;
                case DBConstants.PROP_USER:
                    configuration.setUserName(entry.getValue());
                    break;
                case DBConstants.PROP_PASSWORD:
                    configuration.setUserPassword(entry.getValue());
                    break;
                default:
                    log.debug("Unknown property: " + entry.getKey());
                    break;
            }
        }
        return configuration;
    }

    @NotNull
    private static String getPropertyRegex(@NotNull String property) {
        switch (property) {
            case DBConstants.PROP_FOLDER:
            case DBConstants.PROP_FILE:
                return ".+";
            default:
                return "[\\w\\-_.~]+";
        }
    }

    @NotNull
    public static Pattern getUrlPattern(@NotNull String sampleURL) throws StringTemplate.StringTemplateFormatException {
        return new Pattern(getUrlTemplate(sampleURL));
    }

    public static class Pattern {
        @NotNull
        private final StringTemplate template;

        public Pattern(@NotNull StringTemplate template) {
            this.template = template;
        }

        @NotNull
        public Set<String> getAvailablePropertyNames() {
            return this.template.getParametersInfo().keySet();
        }

        @NotNull
        public Set<String> getMandatoryPropertyNames() {
            return this.template.getParametersInfo().values().stream()
                                .filter(StringTemplate.ParameterInfo::isMandatory)
                                .map(StringTemplate.ParameterInfo::name)
                                .collect(Collectors.toSet());
        }

        public boolean hasProperty(@NotNull String propName) {
            return this.template.getParametersInfo().containsKey(propName);
        }

        public boolean hasMandatoryProperty(@NotNull String propName) {
            StringTemplate.ParameterInfo p = this.template.getParametersInfo().get(propName);
            return p != null && p.isMandatory();
        }

        @Nullable
        public Map<String, String> tryRecognize(@NotNull String urlString) {
            return this.template.extractSingletonParametersMap(urlString);
        }

        @Nullable
        public StringTemplate.ParamEntries tryRecognizeHierarchical(@NotNull String urlString) {
            return this.template.extractAllParametersTree(urlString);
        }
    }
}
