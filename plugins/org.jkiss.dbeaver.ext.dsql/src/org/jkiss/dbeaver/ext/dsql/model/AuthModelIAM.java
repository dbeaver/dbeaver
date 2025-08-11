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
package org.jkiss.dbeaver.ext.dsql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dsql.constants.DSQLConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.impl.auth.AuthModelDatabaseNative;
import org.jkiss.dbeaver.model.net.DBWUtils;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.dsql.DsqlUtilities;
import software.amazon.awssdk.regions.Region;

public class AuthModelIAM extends AuthModelDatabaseNative<AuthModelIAMCredentials> {

    AwsCredentialsProvider credentialsProvider;

    @NotNull
    @Override
    public AuthModelIAMCredentials createCredentials() {
        return new AuthModelIAMCredentials();
    }

    @NotNull
    @Override
    public AuthModelIAMCredentials loadCredentials(@NotNull DBPDataSourceContainer dataSource, @NotNull DBPConnectionConfiguration configuration) {
        AuthModelIAMCredentials credentials = super.loadCredentials(dataSource, configuration);
        try {
            loadTokenFromAWSCredentials(credentials, dataSource, configuration);
            credentials.setParseError(null);
        } catch (DBException e) {
            credentials.setParseError(e);
        }
        return credentials;
    }

    private void loadTokenFromAWSCredentials(AuthModelIAMCredentials credentials, DBPDataSourceContainer dataSource, DBPConnectionConfiguration configuration) throws DBException {
        DBPConnectionConfiguration originalConfiguration = dataSource.getConnectionConfiguration();
        DBWUtils.ConnectivityParameters cnnParams = DBWUtils.getConnectivityParameters(originalConfiguration, dataSource.getDriver());

        String authType = configuration.getProperty(DSQLConstants.AUTH_TYPE);
        if (authType == null) return;

        String accessKey = configuration.getProperty(DSQLConstants.AWS_ACCESS_KEY);
        String secretKey = configuration.getProperty(DSQLConstants.AWS_SECRET_KEY);
        String sessionToken = configuration.getProperty(DSQLConstants.AWS_SESSION_TOKEN);
        String profile = configuration.getProperty(DSQLConstants.AWS_PROFILE);
        String token = configuration.getProperty(DSQLConstants.DSQL_TOKEN);
        String regionStr = configuration.getProperty(DSQLConstants.AWS_REGION);
        String hostName = cnnParams.hostName();
        String userName = cnnParams.userName();

        if (authType.equals(DSQLConstants.AUTH_TYPES.DSQL_TOKEN.toString())) {
            credentials.setUserPassword(token);
            return;
        }

        if (authType.equals(DSQLConstants.AUTH_TYPES.AWS_SESSION_CREDENTIALS.toString())) {
            credentialsProvider = StaticCredentialsProvider.create(AwsSessionCredentials.builder()
                .accessKeyId(accessKey)
                .secretAccessKey(secretKey)
                .sessionToken(sessionToken)
                .build());
        } else if (authType.equals(DSQLConstants.AUTH_TYPES.AWS_PROFILE.toString())) {
            credentialsProvider = ProfileCredentialsProvider.create(profile);
        }

        DsqlUtilities dsqlUtilities = DsqlUtilities.builder()
            .region(Region.of(regionStr))
            .credentialsProvider(credentialsProvider)
            .build();

        token = userName.equals(DSQLConstants.ADMIN_USERNAME)
            ? dsqlUtilities.generateDbConnectAdminAuthToken(builder -> builder.hostname(hostName).region(Region.of(regionStr)))
            : dsqlUtilities.generateDbConnectAuthToken(builder -> builder.hostname(hostName).region(Region.of(regionStr)));

        credentials.setUserPassword(token);
    }
}
