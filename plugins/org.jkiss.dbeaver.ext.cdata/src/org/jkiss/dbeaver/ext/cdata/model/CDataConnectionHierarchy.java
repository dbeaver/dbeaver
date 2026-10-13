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
package org.jkiss.dbeaver.ext.cdata.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cdata.CDataAuthModel;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class CDataConnectionHierarchy {
    private static final String PROMPT_SECRETS_PROPERTY = "cdata.promptSecrets";

    public record Property(
        @NotNull String name,
        @NotNull String label,
        @NotNull String type,
        @NotNull String description,
        @NotNull String placeholder,
        @NotNull String defaultValue,
        @NotNull List<String> choices,
        boolean password,
        boolean required,
        boolean visible,
        @NotNull Map<String, List<Property>> rules
    ) {
    }

    public record Group(@NotNull String name, @NotNull List<Property> properties) {
    }

    private final List<Property> basic;
    private final List<Group> advanced;
    private final Map<String, String> values = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final Set<String> basicNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private final Set<String> advancedEdits = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private final Set<String> secretNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private final Set<String> urlPropertyNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private final Set<String> promptSecretNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    private CDataConnectionHierarchy(@NotNull List<Property> basic, @NotNull List<Group> advanced) {
        this.basic = basic;
        this.advanced = advanced;
        collectNames(basic, basicNames);
        collectSecretNames(basic, secretNames);
        advanced.forEach(group -> collectSecretNames(group.properties(), secretNames));
        secretNames.add("Password");
    }

    @NotNull
    public static CDataConnectionHierarchy parse(@NotNull String definition) throws DBException {
        try {
            JsonObject root = JsonParser.parseString(definition).getAsJsonObject();
            List<Property> basic = parseProperties(root.getAsJsonArray("basic"));
            List<Group> advanced = new ArrayList<>();
            for (JsonElement element : root.getAsJsonArray("advanced")) {
                JsonObject group = element.getAsJsonObject();
                advanced.add(new Group(string(group, "name"), parseProperties(group.getAsJsonArray("properties"))));
            }
            return new CDataConnectionHierarchy(basic, List.copyOf(advanced));
        } catch (RuntimeException e) {
            throw new DBException("Invalid CData connection hierarchy", e);
        }
    }

    @NotNull
    public Set<String> getPropertyNames() {
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        names.addAll(basicNames);
        for (Group group : advanced) {
            group.properties().forEach(property -> names.add(property.name()));
        }
        return names;
    }

    @NotNull
    public static Set<String> getPropertyNames(@NotNull Property property) {
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        collectNames(List.of(property), names);
        return names;
    }

    @NotNull
    public String updateUrl(@NotNull String source, @NotNull String url, @NotNull Set<String> editedNames) throws DBException {
        var properties = CDataConnectionUrl.parse(url, source);
        var formProperties = getConnectionProperties();
        for (String name : editedNames) {
            properties.remove(name);
            if (!name.equalsIgnoreCase("User") && !secretNames.contains(name) && formProperties.containsKey(name)) {
                properties.put(name, formProperties.get(name));
            }
        }
        return CDataConnectionUrl.build(source, properties);
    }

    public void loadConfiguration(@NotNull String source, @NotNull DBPConnectionConfiguration configuration) throws DBException {
        promptSecretNames.clear();
        String promptSecrets = configuration.getProviderProperty(PROMPT_SECRETS_PROPERTY);
        if (promptSecrets != null && !promptSecrets.isEmpty()) {
            promptSecretNames.addAll(Arrays.asList(promptSecrets.split(";")));
        }
        Map<String, String> properties = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        configuration.getProperties().forEach((name, value) -> {
            if (basicNames.contains(name)) {
                properties.put(name, value);
            }
        });
        configuration.getAuthProperties().forEach((name, value) -> {
            if (name.startsWith(CDataAuthModel.SECRET_PROPERTY_PREFIX)) {
                String propertyName = name.substring(CDataAuthModel.SECRET_PROPERTY_PREFIX.length());
                secretNames.add(propertyName);
                properties.put(propertyName, value);
            }
        });
        if (configuration.getUserName() != null && !configuration.getUserName().isEmpty()) {
            properties.put("User", configuration.getUserName());
        }
        if (configuration.getUserPassword() != null && !configuration.getUserPassword().isEmpty()) {
            properties.put("Password", configuration.getUserPassword());
        }
        var urlProperties = CDataConnectionUrl.parse(configuration.getUrl() == null ? "" : configuration.getUrl(), source);
        properties.putAll(urlProperties);
        loadValues(properties);
        urlPropertyNames.addAll(urlProperties.keySet());
    }

    public void saveConfiguration(@NotNull String source, @NotNull DBPConnectionConfiguration configuration) {
        var properties = getConnectionProperties();
        // advanced edits are saved by the driver properties page before the main page
        configuration.getProperties().forEach((name, value) -> {
            if (secretNames.contains(name) && !basicNames.contains(name) && !urlPropertyNames.contains(name)) {
                properties.put(name, value);
            }
        });
        saveCredentials(configuration, properties);
        configuration.setUrl(CDataConnectionUrl.build(source, properties));
    }

    public void loadUrl(@NotNull String source, @NotNull String url) throws DBException {
        var urlProperties = CDataConnectionUrl.parse(url, source);
        Map<String, String> properties = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        getConnectionProperties().forEach((name, value) -> {
            if (name.equalsIgnoreCase("User") || secretNames.contains(name)) {
                properties.put(name, value);
            }
        });
        properties.putAll(urlProperties);
        loadValues(properties);
        urlPropertyNames.addAll(urlProperties.keySet());
    }

    public void saveUrlConfiguration(@NotNull String source, @NotNull DBPConnectionConfiguration configuration) throws DBException {
        var urlProperties = CDataConnectionUrl.parse(configuration.getUrl() == null ? "" : configuration.getUrl(), source);
        Map<String, String> properties = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        properties.putAll(urlProperties);
        configuration.getProperties().forEach((name, value) -> {
            if (secretNames.contains(name) && !basicNames.contains(name)) {
                if (advancedEdits.contains(name)) {
                    properties.put(name, value);
                } else {
                    properties.putIfAbsent(name, value);
                }
            }
        });
        var formProperties = getConnectionProperties();
        formProperties.forEach((name, value) -> {
            if (name.equalsIgnoreCase("User") || secretNames.contains(name)) {
                properties.putIfAbsent(name, value);
            }
        });
        saveCredentials(configuration, properties);
        if (configuration.getUrl() == null || !properties.equals(urlProperties)) {
            configuration.setUrl(CDataConnectionUrl.build(source, properties));
        }
    }

    private void saveCredentials(@NotNull DBPConnectionConfiguration configuration, @NotNull Map<String, String> properties) {
        Map<String, String> driverProperties = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        driverProperties.putAll(configuration.getProperties());
        for (Property property : getAdvancedSecretProperties()) {
            String value = properties.getOrDefault(property.name(), driverProperties.get(property.name()));
            if (value != null && !value.isEmpty()) {
                promptSecretNames.add(property.name());
            } else if (advancedEdits.contains(property.name())) {
                promptSecretNames.remove(property.name());
            }
        }
        savePromptSecretNames(configuration);
        CDataAuthModel.clearSecrets(configuration);
        for (String name : secretNames) {
            String value = properties.remove(name);
            if (value == null && !basicNames.contains(name)) {
                value = driverProperties.get(name);
            }
            if (name.equalsIgnoreCase("Password")) {
                configuration.setUserPassword(value);
            } else if (value != null) {
                configuration.setAuthProperty(CDataAuthModel.SECRET_PROPERTY_PREFIX + name, value);
            }
        }
        configuration.setUserName(properties.remove("User"));
        configuration.getProperties().keySet().removeIf(secretNames::contains);
    }

    public void loadValues(@NotNull Map<String, String> properties) {
        values.clear();
        values.putAll(properties);
        advancedEdits.clear();
        urlPropertyNames.clear();
    }

    @NotNull
    public List<Property> getBasicProperties() {
        List<Property> result = new ArrayList<>();
        collectActive(basic, result);
        return result;
    }

    @NotNull
    public List<Property> getGeneralProperties() {
        List<Property> result = new ArrayList<>();
        collectSection(basic, result, false, false);
        return result;
    }

    @NotNull
    public List<Property> getAuthenticationProperties() {
        List<Property> result = new ArrayList<>();
        collectSection(basic, result, false, true);
        return result;
    }

    @NotNull
    public List<Property> getCredentialProperties() {
        Set<String> authenticationNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        getAuthenticationProperties().forEach(property -> authenticationNames.add(property.name()));
        List<Property> properties = new ArrayList<>(getBasicProperties().stream()
            .filter(property -> property.visible() && !property.name().equalsIgnoreCase("AuthScheme")
                && (authenticationNames.contains(property.name()) || property.password()))
            .toList());
        getAdvancedSecretProperties().stream()
            .filter(property -> property.visible() && (promptSecretNames.contains(property.name()) || !getValue(property).isEmpty()))
            .forEach(properties::add);
        return properties;
    }

    @NotNull
    public List<Property> getAdvancedProperties() {
        return advanced.stream().flatMap(group -> group.properties().stream())
            .filter(property -> !basicNames.contains(property.name()))
            .toList();
    }

    @NotNull
    public List<Property> getAdvancedSecretProperties() {
        return getAdvancedProperties().stream().filter(Property::password).toList();
    }

    public boolean isSecretProperty(@NotNull String name) {
        return secretNames.contains(name);
    }

    public void saveCredentialValues(@NotNull String source, @NotNull DBPConnectionConfiguration configuration) throws DBException {
        var urlProperties = CDataConnectionUrl.parse(configuration.getUrl() == null ? "" : configuration.getUrl(), source);
        boolean urlChanged = false;
        for (Property property : getCredentialProperties()) {
            String name = property.name();
            String value = getValue(property);
            configuration.getProperties().keySet().removeIf(name::equalsIgnoreCase);
            if (!property.password() && !name.equalsIgnoreCase("User")) {
                urlChanged |= !value.equals(urlProperties.put(name, value));
                continue;
            }
            urlChanged |= urlProperties.remove(name) != null;
            if (name.equalsIgnoreCase("User")) {
                configuration.setUserName(value);
            } else if (name.equalsIgnoreCase("Password")) {
                configuration.setUserPassword(value);
            } else {
                configuration.setAuthProperty(CDataAuthModel.SECRET_PROPERTY_PREFIX + name, value);
                if (!basicNames.contains(name)) {
                    promptSecretNames.add(name);
                }
            }
        }
        if (urlChanged) {
            configuration.setUrl(CDataConnectionUrl.build(source, urlProperties));
        }
        savePromptSecretNames(configuration);
    }

    private void savePromptSecretNames(@NotNull DBPConnectionConfiguration configuration) {
        // retain field names after Save password removes their values, so the connection prompt can request them
        if (promptSecretNames.isEmpty()) {
            configuration.removeProviderProperty(PROMPT_SECRETS_PROPERTY);
        } else {
            configuration.setProviderProperty(PROMPT_SECRETS_PROPERTY, String.join(";", promptSecretNames));
        }
    }

    private void collectSection(
        @NotNull List<Property> properties, @NotNull List<Property> result, boolean authentication, boolean authSection
    ) {
        for (Property property : properties) {
            boolean authProperty = authentication || property.name().equalsIgnoreCase("AuthScheme")
                || property.name().equalsIgnoreCase("User") || property.name().equalsIgnoreCase("Password");
            if (authProperty == authSection) {
                result.add(property);
            }
            collectSection(property.rules().getOrDefault(getValue(property), List.of()), result, authProperty, authSection);
        }
    }

    @NotNull
    public List<Group> getAdvancedGroups() {
        return advanced;
    }

    @NotNull
    public String getValue(@NotNull Property property) {
        return values.getOrDefault(property.name(), property.defaultValue());
    }

    public void setValue(@NotNull Property property, @NotNull String value, boolean advancedEdit) {
        // descendants may have different defaults and choices in the new branch
        if (!value.equals(getValue(property))) {
            Set<String> descendants = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            property.rules().values().forEach(children -> collectNames(children, descendants));
            descendants.forEach(values::remove);
            advancedEdits.removeAll(descendants);
        }
        values.put(property.name(), value);
        if (advancedEdit) {
            advancedEdits.add(property.name());
        }
    }

    @NotNull
    public Map<String, String> getConnectionProperties() {
        Map<String, String> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        result.putAll(values);
        basicNames.forEach(result::remove);
        for (Group group : advanced) {
            for (Property property : group.properties()) {
                if (!basicNames.contains(property.name()) || advancedEdits.contains(property.name())) {
                    String value = getValue(property);
                    if (!value.equals(property.defaultValue())) {
                        result.put(property.name(), value);
                    } else {
                        result.remove(property.name());
                    }
                }
            }
        }
        // basic defaults can differ from the driver's defaults, including an explicit empty value
        for (Property property : getBasicProperties()) {
            result.put(property.name(), getValue(property));
        }
        return result;
    }

    private void collectActive(@NotNull List<Property> properties, @NotNull List<Property> result) {
        for (Property property : properties) {
            result.add(property);
            collectActive(property.rules().getOrDefault(getValue(property), List.of()), result);
        }
    }

    private static void collectNames(@NotNull List<Property> properties, @NotNull Set<String> names) {
        for (Property property : properties) {
            names.add(property.name());
            property.rules().values().forEach(children -> collectNames(children, names));
        }
    }

    private static void collectSecretNames(@NotNull List<Property> properties, @NotNull Set<String> names) {
        for (Property property : properties) {
            if (property.password()) {
                names.add(property.name());
            }
            property.rules().values().forEach(children -> collectSecretNames(children, names));
        }
    }

    @NotNull
    private static List<Property> parseProperties(@NotNull JsonArray array) {
        List<Property> properties = new ArrayList<>();
        for (JsonElement element : array) {
            JsonObject json = element.getAsJsonObject();
            String name = string(json, "propertyName");
            if (name.isBlank() || name.indexOf('=') >= 0 || name.indexOf(';') >= 0) {
                throw new IllegalArgumentException("Invalid hierarchy property name");
            }
            List<String> choices = new ArrayList<>();
            if (json.has("enum")) {
                json.getAsJsonArray("enum").forEach(choice -> choices.add(choice.getAsString()));
            }
            Map<String, List<Property>> rules = new LinkedHashMap<>();
            if (json.has("hierarchyRules")) {
                json.getAsJsonObject("hierarchyRules").entrySet().forEach(entry ->
                    rules.put(entry.getKey(), parseProperties(entry.getValue().getAsJsonArray())));
            }
            properties.add(new Property(
                name, string(json, "name"), string(json, "type"), string(json, "description"), string(json, "placeholder"),
                string(json, "default"), List.copyOf(choices),
                Set.of("PASSWORD", "SENSITIVE").contains(string(json, "sensitivity")),
                "RequiredBasic".equals(string(json, "display")), !json.has("visible") || json.get("visible").getAsBoolean(),
                Map.copyOf(rules)
            ));
        }
        return List.copyOf(properties);
    }

    @NotNull
    private static String string(@NotNull JsonObject json, @NotNull String key) {
        JsonElement value = json.get(key);
        return value == null || value.isJsonNull() ? "" : value.getAsString();
    }
}
