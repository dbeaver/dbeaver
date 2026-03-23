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
package org.jkiss.dbeaver.ext.dynamodb.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.dynamodb.DynamoDBConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.utils.CommonUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;

public class DynamoDBCredentialsFactory {

    @NotNull
    public static AwsCredentialsProvider createCredentialsProvider(
            @NotNull DBPDataSourceContainer container) {
        DBPConnectionConfiguration cfg = container.getConnectionConfiguration();

        // Base credentials: static keys or default chain
        AwsCredentialsProvider baseProvider = createBaseProvider(cfg);

        // If Role ARN is specified, wrap with STS AssumeRole
        String roleArn = cfg.getProviderProperty(DynamoDBConstants.PROP_ROLE_ARN);
        if (!CommonUtils.isEmpty(roleArn)) {
            return createAssumeRoleProvider(cfg, baseProvider, roleArn);
        }

        return baseProvider;
    }

    @NotNull
    private static AwsCredentialsProvider createBaseProvider(@NotNull DBPConnectionConfiguration cfg) {
        String accessKey = cfg.getUserName();
        String secretKey = cfg.getUserPassword();

        if (!CommonUtils.isEmpty(accessKey) && !CommonUtils.isEmpty(secretKey)) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey));
        }
        return DefaultCredentialsProvider.create();
    }

    @NotNull
    private static AwsCredentialsProvider createAssumeRoleProvider(
            @NotNull DBPConnectionConfiguration cfg,
            @NotNull AwsCredentialsProvider baseProvider,
            @NotNull String roleArn) {
        String region = cfg.getProviderProperty(DynamoDBConstants.PROP_REGION);
        if (CommonUtils.isEmpty(region)) {
            region = cfg.getServerName();
        }
        if (CommonUtils.isEmpty(region)) {
            region = DynamoDBConstants.DEFAULT_REGION;
        }

        String externalId = cfg.getProviderProperty(DynamoDBConstants.PROP_EXTERNAL_ID);

        StsClient stsClient = StsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(baseProvider)
                .build();

        StsAssumeRoleCredentialsProvider.Builder assumeRoleBuilder =
                StsAssumeRoleCredentialsProvider.builder()
                        .stsClient(stsClient)
                        .refreshRequest(r -> {
                            r.roleArn(roleArn);
                            r.roleSessionName("dbeaver-dynamodb");
                            if (!CommonUtils.isEmpty(externalId)) {
                                r.externalId(externalId);
                            }
                        });

        return assumeRoleBuilder.build();
    }
}
