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
package org.jkiss.dbeaver.ui.net.ssh.jsch;

import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import org.eclipse.jsch.ui.UserInfoPrompter;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.net.ssh.JSCHUserInfoPromptProvider;
import org.jkiss.dbeaver.model.net.ssh.config.SSHAuthConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHHostConfiguration;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.net.ssh.SSHUIMessages;
import org.jkiss.utils.CommonUtils;

public class JSCHUIPromptProvider implements JSCHUserInfoPromptProvider {

    @NotNull
    @Override
    public UserInfo createUserInfoPrompt(@NotNull SSHHostConfiguration configuration, @NotNull Session session) {
        return new UIPrompter(configuration, session);
    }

    private static class UIPrompter extends UserInfoPrompter {
        private static final Log log = Log.getLog(JSCHUIPromptProvider.class);

        private final SSHHostConfiguration configuration;

        UIPrompter(@NotNull SSHHostConfiguration configuration, Session session) {
            super(session);
            this.configuration = configuration;
        }

        @Override
        public String[] promptKeyboardInteractive(String destination, String name, String instruction, String[] prompt, boolean[] echo) {
            if (shouldUsePassword()) {
                setPassword(configuration.getAuthConfiguration().getPassword());
            }
            return super.promptKeyboardInteractive(destination, name, instruction, prompt, echo);
        }

        @Override
        public boolean promptPassword(String message) {
            setPassword(configuration.getAuthConfiguration().getPassword());
            return true;
        }

        @Override
        public boolean promptPassphrase(String message) {
            setPassphrase(configuration.getAuthConfiguration().getPassword());
            return true;
        }

        @Override
        public void showMessage(String message) {
            // Just log it in debug
            log.debug("SSH server message:");
            log.debug(message);
        }

        private boolean shouldUsePassword() {
            final SSHAuthConfiguration auth = configuration.getAuthConfiguration();
            return auth.getType().usesPassword() && (auth.isSavePassword() || CommonUtils.isNotEmpty(auth.getPassword()));
        }

        @Override
        public boolean promptYesNo(String question) {
            return DBWorkbench.getPlatformUI().confirmAction(
                SSHUIMessages.jsch_remote_host_identifier_changed_warning_title,
                question,
                true
            );
        }
    }
}