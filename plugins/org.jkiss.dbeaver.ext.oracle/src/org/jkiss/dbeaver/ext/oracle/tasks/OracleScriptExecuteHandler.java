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
package org.jkiss.dbeaver.ext.oracle.tasks;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.ext.oracle.model.dict.OracleConnectionType;
import org.jkiss.dbeaver.ext.oracle.oci.OCIUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.fs.DBFUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.registry.task.TaskPreferenceStore;
import org.jkiss.dbeaver.tasks.nativetool.AbstractNativeToolHandler;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OracleScriptExecuteHandler extends AbstractNativeToolHandler<OracleScriptExecuteSettings, DBSObject, OracleDataSource> {

    @Override
    public Collection<OracleDataSource> getRunInfo(OracleScriptExecuteSettings settings) {
        return Collections.singletonList((OracleDataSource) settings.getDataSourceContainer().getDataSource());
    }

    @Override
    protected OracleScriptExecuteSettings createTaskSettings(DBRRunnableContext context, DBTTask task) throws DBException {
        OracleScriptExecuteSettings settings = new OracleScriptExecuteSettings(task.getProject());
        settings.loadSettings(context, new TaskPreferenceStore(task));

        return settings;
    }

    @Override
    protected boolean needsModelRefresh() {
        return false;
    }

    @Override
    public void fillProcessParameters(OracleScriptExecuteSettings settings, OracleDataSource arg, List<String> cmd) throws IOException {
        String sqlPlusExec = RuntimeUtils.getNativeBinaryName("sqlplus"); //$NON-NLS-1$
        File sqlPlusBinary = new File(settings.getClientHome().getPath(), "bin/" + sqlPlusExec); //$NON-NLS-1$
        if (!sqlPlusBinary.exists()) {
            sqlPlusBinary = new File(settings.getClientHome().getPath(), sqlPlusExec);
        }
        if (!sqlPlusBinary.exists()) {
            throw new IOException("SQL*Plus binary not found in '" + settings.getClientHome().getPath().getAbsolutePath());
        }
        String dumpPath = sqlPlusBinary.getAbsolutePath();
        cmd.add(dumpPath);
    }

    @Override
    protected List<String> getCommandLine(OracleScriptExecuteSettings settings, OracleDataSource arg) throws IOException {
        List<String> cmd = new ArrayList<>();
        fillProcessParameters(settings, arg, cmd);
        DBPConnectionConfiguration conInfo = settings.getDataSourceContainer().getActualConnectionConfiguration();
        String url;
        String walletLocation = conInfo.getAuthProperty("oracle.wallet.dir");
        boolean hasWallet = walletLocation != null && !walletLocation.isEmpty();

        if ("TNS".equals(conInfo.getProviderProperty(OracleConstants.PROP_CONNECTION_TYPE))) {
            String name = conInfo.getServerName() != null
                ? conInfo.getServerName()
                : conInfo.getDatabaseName();
            url = name;
            String description = OCIUtils.readTnsNames(Paths.get(conInfo.getProviderProperty(OracleConstants.PROP_TNS_PATH)).toFile(), false).get(name);
            if (hasWallet) {
                url = patchWalletDirectory(description, walletLocation);
            }
        } else {
            boolean isSID = OracleConnectionType.SID.name()
                .equals(conInfo.getProviderProperty(OracleConstants.PROP_SID_SERVICE));

            String protocol = hasWallet ? "TCPS" : "TCP";
            String port = conInfo.getHostPort();

            if (isSID || hasWallet) {
                // Use full descriptor when SID or wallet is involved
                String serviceName = extractServiceName(conInfo.getUrl());
                String connectData;
                if (serviceName != null && hasWallet) {
                    connectData = "(SERVICE_NAME=" + serviceName + ")";
                } else {
                    connectData = "(SID=" + conInfo.getDatabaseName() + ")";
                }

                url =
                    "(DESCRIPTION=" +
                        "(ADDRESS=(PROTOCOL=" + protocol + ")" +
                        "(HOST=" + conInfo.getHostName() + ")" +
                        "(PORT=" + port + "))" +
                        "(CONNECT_DATA=" + connectData + ")" +
                        (hasWallet
                            ? "(SECURITY=(MY_WALLET_DIRECTORY=" + walletLocation + ")(ssl_server_dn_match=yes))"
                            : "") +
                        ")";
            } else {
                // Existing Easy Connect format
                url =
                    "//" + conInfo.getHostName() +
                        (port != null ? ":" + port : "") +
                        "/" + conInfo.getDatabaseName();
            }
        }
        final String role = conInfo.getProviderProperty(OracleConstants.PROP_INTERNAL_LOGON);
        if (role != null) {
            url += (" AS " + role);
        }
        cmd.add(conInfo.getUserName() + "/\"" + conInfo.getUserPassword() + "\"@" + url + ""); //$NON-NLS-1$ //$NON-NLS-2$
/*

        if (toolWizard.isVerbose()) {
            cmd.add("-v");
        }
        cmd.add("-q");

        cmd.add(toolWizard.getDatabaseObjects().getName());
*/
        return cmd;
    }

    private static String extractServiceName(String descriptor) {
        Pattern pattern = Pattern.compile(
            "\\(SERVICE_NAME\\s*=\\s*([^)]+)\\)",
            Pattern.CASE_INSENSITIVE);

        Matcher matcher = pattern.matcher(descriptor);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }

    @Override
    protected boolean isLogInputStream() {
        return true;
    }

    @Override
    protected void startProcessHandler(
        DBRProgressMonitor monitor,
        DBTTask task,
        OracleScriptExecuteSettings settings,
        OracleDataSource arg,
        ProcessBuilder processBuilder,
        Process process,
        Log log
    ) throws IOException, DBException {
        Path inputFile = DBFUtils.resolvePathFromString(monitor, task.getProject(), settings.getInputFile());
        if (!Files.exists(inputFile)) {
            throw new IOException("File '" + inputFile + "' doesn't exist");
        }
        super.startProcessHandler(monitor, task, settings, arg, processBuilder, process, log);
        new BinaryFileTransformerJob(monitor, task, inputFile, process.getOutputStream(), log).start();
    }

    public static String patchWalletDirectory(String descriptor, String walletDir) {
        String walletEntry = "(MY_WALLET_DIRECTORY=" + walletDir + ")";

        int securityPos = descriptor.toUpperCase().indexOf("(SECURITY=");
        if (securityPos >= 0) {
            int securityEnd = findMatchingParen(descriptor, securityPos);

            String securityBlock =
                descriptor.substring(securityPos, securityEnd + 1);

            int existingWallet =
                securityBlock.toUpperCase().indexOf("(MY_WALLET_DIRECTORY=");

            if (existingWallet >= 0) {
                int walletEnd =
                    findMatchingParen(securityBlock, existingWallet);

                securityBlock =
                    securityBlock.substring(0, existingWallet)
                        + walletEntry
                        + securityBlock.substring(walletEnd + 1);
            } else {
                securityBlock =
                    securityBlock.substring(0, securityBlock.length() - 1)
                        + walletEntry
                        + ")";
            }

            return descriptor.substring(0, securityPos)
                + securityBlock
                + descriptor.substring(securityEnd + 1);
        }

        int descriptionPos = descriptor.toUpperCase().indexOf("(DESCRIPTION=");
        if (descriptionPos < 0) {
            throw new IllegalArgumentException(
                "Descriptor does not contain DESCRIPTION block");
        }

        int descriptionEnd = findMatchingParen(descriptor, descriptionPos);

        return descriptor.substring(0, descriptionEnd)
            + "(SECURITY=" + walletEntry + ")"
            + descriptor.substring(descriptionEnd);
    }

    private static int findMatchingParen(String text, int openPos) {
        int depth = 0;

        for (int i = openPos; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }

        throw new IllegalArgumentException(
            "Unbalanced parentheses in descriptor");
    }

    @Override
    public boolean executeProcess(
        DBRProgressMonitor monitor,
        DBTTask task,
        OracleScriptExecuteSettings settings,
        OracleDataSource oracleDataSource,
        Log log
    ) throws IOException, InterruptedException {
        return super.executeProcess(monitor, task, settings, oracleDataSource, log);
    }
}
