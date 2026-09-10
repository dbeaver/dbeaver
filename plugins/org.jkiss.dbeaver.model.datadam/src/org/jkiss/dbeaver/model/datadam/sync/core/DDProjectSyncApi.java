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
 * Remote endpoints for standalone project sharing (separate from Configuration sync).
 */
interface DDProjectSyncApi {

    String PROJECT_FIELDS = """
        id: projectId
        name
        description
        projectOwner: ownerAccountId
        createTime
        updateTime""";

    String PROJECT_FILE_FIELDS = """
        fileName
        encryptedContents
        fingerprint""";

    String QUERY_LIST_PROJECTS = """
        query {
            projects {
        %s
            }
        }""".formatted(PROJECT_FIELDS.indent(4));

    String MUTATION_CREATE_PROJECT = """
        mutation($input: CreateProjectInput!) {
            createProject(input: $input) {
        %s
            }
        }""".formatted(PROJECT_FIELDS.indent(4));

    String MUTATION_UPDATE_PROJECT = """
        mutation($projectId: ID!, $input: UpdateProjectInput!) {
            updateProject(projectId: $projectId, input: $input) {
        %s
            }
        }""".formatted(PROJECT_FIELDS.indent(4));

    String MUTATION_DELETE_PROJECT = """
        mutation($projectId: ID!) {
            deleteProject(projectId: $projectId)
        }""";

    String QUERY_PULL_PROJECT_CONFIGURATION = """
        query($projectId: ID!) {
            pullProjectConfiguration(projectId: $projectId) {
                configurationFingerprint
                files {
        %s
                }
            }
        }""".formatted(PROJECT_FILE_FIELDS.indent(8));

    String MUTATION_PUSH_PROJECT_CONFIGURATION = """
        mutation($projectId: ID!, $input: PushProjectConfigurationInput!) {
            pushProjectConfiguration(projectId: $projectId, input: $input) {
                id: revisionId
                userId
                updateTime
                configurationFingerprint
            }
        }""";

}
