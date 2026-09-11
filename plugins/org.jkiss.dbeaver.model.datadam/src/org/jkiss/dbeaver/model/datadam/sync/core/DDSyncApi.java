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
package org.jkiss.dbeaver.model.datadam.sync.core;

/**
 * Remote synchronization endpoints.
 */
public interface DDSyncApi {

    String DATA_KEY_ENDPOINT = "/data/key";
    String GRAPHQL_ENDPOINT = "/graphql";

    String CONFIGURATION_FIELDS = """
        configurationId
        name
        version
        createTime: createdAt
        lastSyncTime: lastSyncAt""";

    String CONFIGURATION_WITH_PARTS_FIELDS = CONFIGURATION_FIELDS + """

        parts {
            key
            kind
            projectId
            version
            encryptedValue
        }""";

    String QUERY_LIST_CONFIGURATIONS = """
        query {
            configurations {
        %s
            }
        }""".formatted(CONFIGURATION_FIELDS.indent(4));

    String QUERY_GET_CONFIGURATION = """
        query($configurationId: ID!) {
            configuration(configurationId: $configurationId) {
        %s
            }
        }""".formatted(CONFIGURATION_WITH_PARTS_FIELDS.indent(4));

    String MUTATION_CREATE_CONFIGURATION = """
        mutation($input: CreateConfigurationInput!) {
            createConfiguration(input: $input) {
        %s
            }
        }""".formatted(CONFIGURATION_WITH_PARTS_FIELDS.indent(4));

    String MUTATION_UPDATE_CONFIGURATION = """
        mutation($configurationId: ID!, $input: UpdateConfigurationInput!) {
            updateConfiguration(configurationId: $configurationId, input: $input) {
                configuration {
        %s
                }
                conflictingKeys
            }
        }""".formatted(CONFIGURATION_WITH_PARTS_FIELDS.indent(8));

}
