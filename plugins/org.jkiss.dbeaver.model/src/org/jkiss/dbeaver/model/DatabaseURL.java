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
import org.jkiss.utils.Pair;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * JDBCUtils
 */
public class DatabaseURL {

    private static final Log log = Log.getLog(DatabaseURL.class);

    private static final char URL_GROUP_START = '{'; //$NON-NLS-1$
    private static final char URL_GROUP_END = '}'; //$NON-NLS-1$
    private static final char URL_OPTIONAL_START = '['; //$NON-NLS-1$
    private static final char URL_OPTIONAL_END = ']'; //$NON-NLS-1$

    public static String generateUrlByTemplate(DBPDriver driver, DBPConnectionConfiguration connectionInfo) {
        String urlTemplate = driver.getSampleURL();
        return DatabaseURL.generateUrlByTemplate(urlTemplate, connectionInfo);
    }

    public static String generateUrlByTemplate(String urlTemplate, DBPConnectionConfiguration connectionInfo) {
        if (!CommonUtils.isEmpty(connectionInfo.getUrl()) &&
            CommonUtils.isEmpty(connectionInfo.getHostPort()) &&
            CommonUtils.isEmpty(connectionInfo.getHostName()) &&
            CommonUtils.isEmpty(connectionInfo.getServerName()) &&
            CommonUtils.isEmpty(connectionInfo.getDatabaseName()))
        {
            // No parameters, just URL - so URL it is
            return connectionInfo.getUrl();
        }
        try {
            if (CommonUtils.isEmptyTrimmed(urlTemplate)) {
                return connectionInfo.getUrl();
            }
            MetaURL metaURL = parseSampleURL(urlTemplate);
            StringBuilder url = new StringBuilder();
            for (String component : metaURL.getUrlComponents()) {
                String newComponent = component;
                if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
                    newComponent = newComponent.replace(makePropPattern(DBConstants.PROP_HOST), connectionInfo.getHostName());
                }
                if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
                    newComponent = newComponent.replace(makePropPattern(DBConstants.PROP_PORT), connectionInfo.getHostPort());
                }
                if (!CommonUtils.isEmpty(connectionInfo.getServerName())) {
                    newComponent = newComponent.replace(makePropPattern(DBConstants.PROP_SERVER), connectionInfo.getServerName());
                }
                if (!CommonUtils.isEmpty(connectionInfo.getDatabaseName())) {
                    newComponent = newComponent.replace(makePropPattern(DBConstants.PROP_DATABASE), connectionInfo.getDatabaseName());
                    newComponent = newComponent.replace(makePropPattern(DBConstants.PROP_FOLDER), connectionInfo.getDatabaseName());
                    newComponent = newComponent.replace(makePropPattern(DBConstants.PROP_FILE), connectionInfo.getDatabaseName());
                }
                newComponent = newComponent.replace(makePropPattern(DBConstants.PROP_USER), CommonUtils.notEmpty(connectionInfo.getUserName()));
                // support of {password} pattern was removed for security reasons (see dbeaver/pro#1888)
                //newComponent = newComponent.replace(makePropPattern(DBConstants.PROP_PASSWORD), CommonUtils.notEmpty(connectionInfo.getUserPassword()));

                if (newComponent.startsWith("[")) { //$NON-NLS-1$
                    if (!newComponent.equals(component)) {
                        url.append(newComponent.substring(1, newComponent.length() - 1));
                    }
                } else {
                    url.append(newComponent);
                }
            }
            return url.toString();
        } catch (DBException e) {
            log.error(e);
            return null;
        }
    }

    private static String makePropPattern(String prop)
    {
        return "{" + prop + "}"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    public static class MetaURL {

        private List<String> urlComponents = new ArrayList<>();
        private Set<String> availableProperties = new HashSet<>();
        private Set<String> requiredProperties = new HashSet<>();

        public List<String> getUrlComponents() {
            return urlComponents;
        }

        public Set<String> getAvailableProperties() {
            return availableProperties;
        }

        public Set<String> getRequiredProperties() {
            return requiredProperties;
        }
    }

    public static MetaURL parseSampleURL(String sampleURL) throws DBException {
        MetaURL metaURL = new MetaURL();
        int offsetPos = 0;
        for (; ; ) {
            int divPos = sampleURL.indexOf(URL_GROUP_START, offsetPos);
            if (divPos == -1) {
                break;
            }
            int divPos2 = sampleURL.indexOf(URL_GROUP_END, divPos);
            if (divPos2 == -1) {
                throw new DBException("Bad sample URL: " + sampleURL);
            }
            String propName = sampleURL.substring(divPos + 1, divPos2);
            boolean isOptional = false;
            int optDiv1 = sampleURL.lastIndexOf(URL_OPTIONAL_START, divPos);
            int optDiv1c = sampleURL.lastIndexOf(URL_OPTIONAL_END, divPos);
            int optDiv2 = sampleURL.indexOf(URL_OPTIONAL_END, divPos2);
            int optDiv2c = sampleURL.indexOf(URL_OPTIONAL_START, divPos2);
            if (optDiv1 != -1 && optDiv2 != -1 && (optDiv1c == -1 || optDiv1c < optDiv1) && (optDiv2c == -1 || optDiv2c > optDiv2)) {
                divPos = optDiv1;
                divPos2 = optDiv2;
                isOptional = true;
            }
            if (divPos > offsetPos) {
                metaURL.urlComponents.add(sampleURL.substring(offsetPos, divPos));
            }
            metaURL.urlComponents.add(sampleURL.substring(divPos, divPos2 + 1));
            metaURL.availableProperties.add(propName);
            if (!isOptional) {
                metaURL.requiredProperties.add(propName);
            }
            offsetPos = divPos2 + 1;
        }
        if (offsetPos < sampleURL.length()) {
            metaURL.urlComponents.add(sampleURL.substring(offsetPos));
        }
/*
        // Check for required parts
        for (String component : urlComponents) {
            boolean isRequired = !component.startsWith("[");
            int divPos = component.indexOf('{');
            if (divPos != -1) {
                int divPos2 = component.indexOf('}', divPos);
                if (divPos2 != -1) {
                    String propName = component.substring(divPos + 1, divPos2);
                    availableProperties.add(propName);
                    if (isRequired) {
                        requiredProperties.add(propName);
                    }
                }
            }
        }
*/
        return metaURL;
    }


    @NotNull
    public static Pattern getPattern(@NotNull String sampleUrl) {
        String pattern = sampleUrl;
        pattern = CommonUtils.replaceAll(pattern, "\\[(.*?)]", m -> "\\\\E(?:\\\\Q" + m.group(1) + "\\\\E)?\\\\Q");
        pattern = CommonUtils.replaceAll(pattern, "\\{(.*?)}", m -> "\\\\E(\\?<\\\\Q" + m.group(1) + "\\\\E>" + getPropertyRegex(m.group(1)) + ")\\\\Q");
        pattern = "^\\Q" + pattern + "\\E";

        return Pattern.compile(pattern);
    }

    @Nullable
    public static DBPConnectionConfiguration extractConfigurationFromUrl(@NotNull String sampleUrl, @NotNull String targetUrl) {
        final Matcher matcher = getPattern(sampleUrl).matcher(targetUrl);
        if (!matcher.find()) {
            return null;
        }
        final Map<String, String> properties = getProperties(sampleUrl).stream()
            .map(x -> new Pair<>(x, matcher.group(x)))
            .filter(x -> CommonUtils.isNotEmpty(x.getSecond()))
            .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
        if (properties.isEmpty()) {
            return null;
        }
        final DBPConnectionConfiguration configuration = new DBPConnectionConfiguration();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
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
                return ".+?";
            default:
                return "[\\\\w\\\\-_.~]+";
        }
    }

    @NotNull
    private static List<String> getProperties(@NotNull String sampleUrl) {
        final Matcher matcher = Pattern.compile("\\{(.*?)}").matcher(sampleUrl);
        final List<String> properties = new ArrayList<>();
        while (matcher.find()) {
            properties.add(matcher.group(1));
        }
        return properties;
    }
}
