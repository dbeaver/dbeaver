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
package org.jkiss.dbeaver.ext.dynamodb;

public class DynamoDBConstants {

    public static final String PLUGIN_ID = "org.jkiss.dbeaver.ext.dynamodb";
    public static final String DRIVER_ID = "aws_dynamodb";
    public static final String DEFAULT_REGION = "us-east-1";

    public static final String PROP_REGION = "region";
    public static final String PROP_ENDPOINT = "endpoint";
    public static final String PROP_CONNECTION_NAME = "connectionName";

    public static final String PROP_ROLE_ARN = "role.arn";
    public static final String PROP_EXTERNAL_ID = "role.externalId";
}
