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
package org.jkiss.dbeaver.model.websocket;

/**
 * WebSocket event constants
 */
public interface WSConstants {
    String WS_AUTH_HEADER = "SM-Auth-Token";

    String TOPIC_SERVER_CONFIG = "cb_config";
    String TOPIC_SERVER_STATE = "cb_server_state";
    String TOPIC_WORKSPACE_CONFIGURATION = "cb_workspace_configuration";
    String TOPIC_SESSION_LOG = "cb_session_log";
    String TOPIC_DATABASE_OUTPUT_LOG = "cb_database_output_log";
    String TOPIC_SESSION = "cb_session";
    String TOPIC_SESSION_TASK = "cb_session_task";
    String TOPIC_USER = "cb_user";
    String TOPIC_DATASOURCE = "cb_datasource";
    String TOPIC_DATASOURCE_FOLDER = "cb_datasource_folder";
    String TOPIC_DATASOURCE_CONNECTION = "cb_datasource_connection";
    String TOPIC_DELETE_TEMP_FOLDER = "cb_delete_temp_folder";
    String TOPIC_TASK = "cb_task";
    String TOPIC_USER_SECRET = "cb_user_secret";
    String TOPIC_SCRIPTS = "cb_scripts";
    String TOPIC_PROJECTS = "cb_projects";
    String TOPIC_OBJECT_PERMISSIONS = "cb_object_permissions";
    String TOPIC_SUBJECT_PERMISSIONS = "cb_subject_permissions";
    String TOPIC_TRANSACTION_COUNT = "cb_transaction";

    //TODO remove this enum
    @Deprecated
    enum EventAction {
        CREATE,
        DELETE,
        UPDATE
    }


}
