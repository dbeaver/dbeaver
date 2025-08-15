/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.dsql.constants;

public class DSQLConstants {

    // Configuration Constants
    public static String AUTH_TYPE = "auth_type";
    public enum AUTH_TYPES {
        AWS_SESSION_CREDENTIALS,
        AWS_PROFILE,
        DSQL_TOKEN
    }
    public static String AWS_ACCESS_KEY = "aws_access_key";
    public static String AWS_SECRET_KEY = "aws_secret_key";
    public static String AWS_SESSION_TOKEN = "aws_session_token";
    public static String AWS_PROFILE = "aws_profile";
    public static String DSQL_TOKEN = "dsql_token";
    public static String AWS_REGION = "aws_region";

    // DSQL Supported AWS Regions
    public static String[] SUPPORTED_REGIONS = new String[] {
        "us-east-1", "us-east-2", "us-west-2",
        "eu-west-1", "eu-west-2", "eu-west-3",
        "ap-northeast-1", "ap-northeast-2", "ap-northeast-3"
    };

    public static String ADMIN_USERNAME = "admin";
    public static String DEFAULT_PROFILE = "default";
}
