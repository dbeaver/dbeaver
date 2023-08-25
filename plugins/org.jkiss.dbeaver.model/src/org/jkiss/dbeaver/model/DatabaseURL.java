/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * JDBCUtils
 */
public class DatabaseURL {

    private static final Log log = Log.getLog(DatabaseURL.class);

    private static final char URL_GROUP_START = '{'; // $NON-NLS-1$
    private static final char URL_GROUP_END = '}'; // $NON-NLS-1$
    private static final char URL_OPTIONAL_START = '['; // $NON-NLS-1$
    private static final char URL_OPTIONAL_END = ']'; // $NON-NLS-1$

    private static final char[] URL_TEMPLATE_SEPARATOR_CHARS = new char[]{
        URL_GROUP_START, URL_GROUP_END, URL_OPTIONAL_START, URL_OPTIONAL_END
    };
    
    private static final Map<String, Function<DBPConnectionConfiguration, String>> accessorByName = Map.of(
        DBConstants.PROP_HOST, c -> CommonUtils.nullIfEmpty(c.getHostName()),
        DBConstants.PROP_PORT, c -> CommonUtils.nullIfEmpty(c.getHostPort()),
        DBConstants.PROP_SERVER, c -> CommonUtils.nullIfEmpty(c.getServerName()),
        DBConstants.PROP_DATABASE, c -> CommonUtils.nullIfEmpty(c.getDatabaseName()),
        DBConstants.PROP_FOLDER, c -> CommonUtils.nullIfEmpty(c.getDatabaseName()),
        DBConstants.PROP_FILE, c -> CommonUtils.nullIfEmpty(c.getDatabaseName()),
        DBConstants.PROP_USER, c -> CommonUtils.notEmpty(c.getUserName()),
        DBConstants.PROP_PASSWORD, c -> CommonUtils.notEmpty(c.getUserPassword())
    );
    private static final Function<DBPConnectionConfiguration, String> defaultValueAccessor = c -> null; 

    public static String generateUrlByTemplate(DBPDriver driver, DBPConnectionConfiguration connectionInfo) {
        String urlTemplate = driver.getSampleURL();
        return DatabaseURL.generateUrlByTemplate(urlTemplate, connectionInfo);
    }

    public static String generateUrlByTemplate(String urlTemplate, DBPConnectionConfiguration connectionInfo) {
        if (!CommonUtils.isEmpty(connectionInfo.getUrl()) &&
            CommonUtils.isEmpty(connectionInfo.getHostPort()) &&
            CommonUtils.isEmpty(connectionInfo.getHostName()) &&
            CommonUtils.isEmpty(connectionInfo.getServerName()) &&
            CommonUtils.isEmpty(connectionInfo.getDatabaseName())) {
            // No parameters, just URL - so URL it is
            return connectionInfo.getUrl();
        }
        try {
            if (CommonUtils.isEmptyTrimmed(urlTemplate)) {
                return connectionInfo.getUrl();
            }
            UrlTemplateInfo templateInfo = parseUrlTemplate(urlTemplate);
            UrlTemplateInstance url = new UrlTemplateInstance(templateInfo);
            url.populateParams(paramName -> accessorByName.getOrDefault(paramName, defaultValueAccessor).apply(connectionInfo));
            String urlString = url.prepareUrlString();
            return urlString;
        } catch (DBException e) {
            log.error(e);
            return null;
        }
    }

    public static class UrlTemplateInfo {
        private final Map<String, UrlTemplateParameterNode> allParamsByName;
        private final Map<String, UrlTemplateParameterNode> requiredParamsByName;
        private final UrlTemplateNode root;
        private final int maxOptsSeqNodeId;
        private final int maxParamId;

        public UrlTemplateInfo(UrlTemplateNode root, int maxParamId, int maxOptsSeqNodeId,
            Map<String, UrlTemplateParameterNode> paramsByName, Map<String, UrlTemplateParameterNode> requiredParamsByName) {
            this.allParamsByName = paramsByName;
            this.requiredParamsByName = requiredParamsByName;
            this.root = root;
            this.maxOptsSeqNodeId = maxOptsSeqNodeId;
            this.maxParamId = maxParamId;
        }

        public Set<String> getAvailableProperties() {
            return Collections.unmodifiableSet(allParamsByName.keySet());
        }

        public Set<String> getRequiredProperties() {
            return Collections.unmodifiableSet(requiredParamsByName.keySet());
        }
    }

    public static class UrlTemplateInstance {
        private final UrlTemplateInfo template;
        private final int[] paramsSpecifiedByOptsNodeId;
        private final String[] valueByParamId;

        public UrlTemplateInstance(UrlTemplateInfo template) {
            this.template = template;
            this.paramsSpecifiedByOptsNodeId = new int[template.maxOptsSeqNodeId];
            this.valueByParamId = new String[template.maxParamId];
        }

        public boolean setParam(String name, String value) {
            UrlTemplateParameterNode node = template.allParamsByName.get(name);
            if (node != null) {
                applyParamValue(node, value);
                return true;
            } else {
                return false;
            }
        }

        private void applyParamValue(UrlTemplateParameterNode node, String value) {
            if (value != null) {
                if (valueByParamId[node.paramId] == null) {
                    node.dependantOptSeqIds.forEach(n -> paramsSpecifiedByOptsNodeId[n]++);
                }
            } else {
                if (valueByParamId[node.paramId] != null) {
                    node.dependantOptSeqIds.forEach(n -> paramsSpecifiedByOptsNodeId[n]--);
                }
            }
            valueByParamId[node.paramId] = value;
        }

        public void populateParams(Function<String, String> paramValueProvider) {
            for (UrlTemplateParameterNode node : template.allParamsByName.values()) {
                String value = paramValueProvider.apply(node.name);
                applyParamValue(node, value);
            }
        }

        public String prepareUrlString() {
            StringBuilder sb = new StringBuilder();
            template.root.apply(new UrlTemplateNodeVisitor() {
                @Override
                public void visitSequence(UrlTemplateSequenceNode seq) {
                    seq.nodes.forEach(n -> n.apply(this));
                }

                @Override
                public void visitOptionalSequence(UrlTemplateOptionalSequenceNode seq) {
                    if (paramsSpecifiedByOptsNodeId[seq.optsSeqNodeId] > 0) {
                        seq.nodes.forEach(n -> n.apply(this));
                    }
                }

                @Override
                public void visitFixed(UrlTemplateFixedNode fixed) {
                    sb.append(fixed.text);
                }

                @Override
                public void visitParameter(UrlTemplateParameterNode param) {
                    String value = valueByParamId[param.paramId];
                    if (value != null) {
                        sb.append(value);
                    }
                }
            });
            return sb.toString();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            template.root.apply(new UrlTemplateNodeVisitor() {
                @Override
                public void visitSequence(UrlTemplateSequenceNode seq) {
                    seq.nodes.forEach(n -> n.apply(this));
                }

                @Override
                public void visitOptionalSequence(UrlTemplateOptionalSequenceNode seq) {
                    sb.append(URL_OPTIONAL_START);
                    seq.nodes.forEach(n -> n.apply(this));
                    sb.append(URL_OPTIONAL_END);
                }

                @Override
                public void visitFixed(UrlTemplateFixedNode fixed) {
                    sb.append(fixed.text);
                }

                @Override
                public void visitParameter(UrlTemplateParameterNode param) {
                    sb.append(URL_GROUP_START).append(param.name).append(":");
                    String value = valueByParamId[param.paramId];
                    if (value == null) {
                        sb.append("NULL");
                    } else {
                        sb.append("\"").append(value.replace("\"", "\\\"")).append("\"");
                    }
                    sb.append(URL_GROUP_END);
                }
            });
            return super.toString() + "(" + sb.toString() + ")";
        }
    }

    private interface UrlTemplateNodeVisitor {

        void visitSequence(UrlTemplateSequenceNode seq);

        void visitOptionalSequence(UrlTemplateOptionalSequenceNode seq);

        void visitFixed(UrlTemplateFixedNode fixed);

        void visitParameter(UrlTemplateParameterNode param);
    }

    private abstract static class UrlTemplateNode {

        public abstract void apply(UrlTemplateNodeVisitor visitor);

        @Override
        public final String toString() {
            StringBuilder sb = new StringBuilder();
            this.apply(new UrlTemplateNodeVisitor() {
                @Override
                public void visitSequence(UrlTemplateSequenceNode seq) {
                    seq.nodes.forEach(n -> n.apply(this));
                }

                @Override
                public void visitOptionalSequence(UrlTemplateOptionalSequenceNode seq) {
                    sb.append(URL_OPTIONAL_START);
                    seq.nodes.forEach(n -> n.apply(this));
                    sb.append(URL_OPTIONAL_END);
                }

                @Override
                public void visitFixed(UrlTemplateFixedNode fixed) {
                    sb.append(fixed.text);
                }

                @Override
                public void visitParameter(UrlTemplateParameterNode param) {
                    sb.append(URL_GROUP_START).append(param.name).append(URL_GROUP_END);
                }
            });
            return super.toString() + "(" + sb.toString() + ")";
        }
    }

    private static class UrlTemplateSequenceNode extends UrlTemplateNode {

        public final List<UrlTemplateNode> nodes;

        public UrlTemplateSequenceNode(UrlTemplateNode... nodes) {
            this.nodes = nodes == null || nodes.length == 0 ? new ArrayList<>() : List.copyOf(Arrays.asList(nodes));
        }

        @Override
        public void apply(UrlTemplateNodeVisitor visitor) {
            visitor.visitSequence(this);
        }
    }

    private static class UrlTemplateOptionalSequenceNode extends UrlTemplateSequenceNode {

        public final int optsSeqNodeId;

        public UrlTemplateOptionalSequenceNode(int optsSeqNodeId, UrlTemplateNode... nodes) {
            super(nodes);
            this.optsSeqNodeId = optsSeqNodeId;
        }

        @Override
        public void apply(UrlTemplateNodeVisitor visitor) {
            visitor.visitOptionalSequence(this);
        }
    }

    private static class UrlTemplateFixedNode extends UrlTemplateNode {

        public final String text;

        public UrlTemplateFixedNode(String text) {
            this.text = text;
        }

        @Override
        public void apply(UrlTemplateNodeVisitor visitor) {
            visitor.visitFixed(this);
        }
    }

    private static class UrlTemplateParameterNode extends UrlTemplateNode {

        public final int paramId;
        public final String name;
        public final Set<Integer> dependantOptSeqIds;

        public UrlTemplateParameterNode(int paramId, String name, Set<Integer> dependantOptSeqIds) {
            this.paramId = paramId;
            this.name = name;
            this.dependantOptSeqIds = dependantOptSeqIds;
        }

        @Override
        public void apply(UrlTemplateNodeVisitor visitor) {
            visitor.visitParameter(this);
        }
    }

    public static UrlTemplateInfo parseUrlTemplate(String text) throws DBException {
        Map<String, UrlTemplateParameterNode> paramsByName = new HashMap<>();
        Map<String, UrlTemplateParameterNode> requiredParamsByName = new HashMap<>();
        Stack<UrlTemplateSequenceNode> stack = new Stack<>();
        Stack<Integer> currentOptSeqIds = new Stack<>();
        stack.push(new UrlTemplateSequenceNode());
        int optGroupsCount = 0;

        // TODO consider escaping

        int fixedStart = 0;
        for (int pos = 0; pos < text.length(); pos++) {
            char c = text.charAt(pos);
            if (ArrayUtils.contains(URL_TEMPLATE_SEPARATOR_CHARS, c)) {
                if (pos > fixedStart) {
                    stack.peek().nodes.add(new UrlTemplateFixedNode(text.substring(fixedStart, pos)));
                }
                fixedStart = pos + 1;
            }
            switch (c) {
                case URL_GROUP_START: {
                    int start = pos + 1;
                    do {
                        pos++;
                        if (pos >= text.length()) {
                            throw new DBException("Bad URL template (Incomplete parameter group name at " + start + "): " + text);
                        }
                        c = text.charAt(pos);
                    } while (c != URL_GROUP_END);
                    if (pos - 1 <= start) {
                        throw new DBException("Bad URL template (Missing parameter group name at " + start + "): " + text);
                    }
                    String paramName = text.substring(start, pos);
                    UrlTemplateParameterNode param = paramsByName.computeIfAbsent(
                	    paramName, name -> new UrlTemplateParameterNode(paramsByName.size(), name, Set.copyOf(currentOptSeqIds))
            	    );
                    if (stack.size() <= 1) {
                        requiredParamsByName.putIfAbsent(paramName, param);
                    }
                    stack.peek().nodes.add(param);
                    fixedStart = pos + 1;
                } break;
                case URL_OPTIONAL_START: {
                    int optSeqId = optGroupsCount++;
                    currentOptSeqIds.push(optSeqId);
                    stack.push(new UrlTemplateOptionalSequenceNode(optSeqId));
                } break;
                case URL_OPTIONAL_END: {
                    if (stack.size() < 2) {
                        throw new DBException("Bad URL template (Unexpected optional group end at " + pos + "): " + text);
                    }
                    currentOptSeqIds.pop();
                    var child = stack.pop();
                    stack.peek().nodes.add(child);
                } break;
            }
        }
        
        if (stack.size() > 1) {
            throw new DBException("Bad URL template (Incomplete optional group): " + text);
        }
        if (fixedStart < text.length()) {
            stack.peek().nodes.add(new UrlTemplateFixedNode(text.substring(fixedStart)));
        }

        return new UrlTemplateInfo(stack.peek(), paramsByName.size(), optGroupsCount, paramsByName, requiredParamsByName);
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
        pattern = "^\\Q" + pattern + "\\E$";
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
