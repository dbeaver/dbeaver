package org.jkiss.dbeaver.ui.net.ssh.jsch;

import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import org.eclipse.jsch.ui.UserInfoPrompter;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.ssh.JSCHUserInfoPromptProvider;
import org.jkiss.utils.CommonUtils;

public class JSCHUIPromptProvider implements JSCHUserInfoPromptProvider {

    @Override
    public UserInfo createUserInfoPrompt(DBWHandlerConfiguration configuration, Session session) {
        return new UIPrompter(configuration, session);
    }

    static class UIPrompter extends UserInfoPrompter {
        private static final Log log = Log.getLog(JSCHUIPromptProvider.class);

        private final DBWHandlerConfiguration configuration;

        UIPrompter(DBWHandlerConfiguration configuration, Session session) {
            super(session);
            this.configuration = configuration;
        }

        @Override
        public String[] promptKeyboardInteractive(String destination, String name, String instruction, String[] prompt, boolean[] echo) {
            if (shouldUsePassword()) {
                setPassword(configuration.getPassword());
            }
            return super.promptKeyboardInteractive(destination, name, instruction, prompt, echo);
        }

        @Override
        public boolean promptPassword(String message) {
            if (shouldUsePassword()) {
                setPassword(configuration.getPassword());
                return true;
            }
            return super.promptPassword(message);
        }

        @Override
        public boolean promptPassphrase(String message) {
            if (shouldUsePassword()) {
                setPassphrase(configuration.getPassword());
                return true;
            }
            return super.promptPassphrase(message);
        }

        @Override
        public void showMessage(String message) {
            // Just log it in debug
            log.debug("SSH server message:");
            log.debug(message);
        }

        private boolean shouldUsePassword() {
            return configuration.isSavePassword() || CommonUtils.isNotEmpty(configuration.getPassword());
        }
    }
}