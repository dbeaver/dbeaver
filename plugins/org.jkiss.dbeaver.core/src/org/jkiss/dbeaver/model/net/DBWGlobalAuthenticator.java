package org.jkiss.dbeaver.model.net;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.registry.encode.EncryptionException;
import org.jkiss.dbeaver.registry.encode.SecuredPasswordEncrypter;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;
import org.jkiss.utils.CommonUtils;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

/**
 * Global authenticator
 */
public class DBWGlobalAuthenticator extends Authenticator {

    static final Log log = LogFactory.getLog(DBWGlobalAuthenticator.class);

    private static DBWGlobalAuthenticator instance = new DBWGlobalAuthenticator();

    private SecuredPasswordEncrypter encrypter;

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        IPreferenceStore store = DBeaverCore.getInstance().getGlobalPreferenceStore();

        String proxyHost = store.getString(PrefConstants.UI_PROXY_HOST);
        if (!CommonUtils.isEmpty(proxyHost) && proxyHost.equalsIgnoreCase(getRequestingHost()) &&
            store.getInt(PrefConstants.UI_PROXY_PORT) == getRequestingPort())
        {
            String userName = store.getString(PrefConstants.UI_PROXY_USER);
            String userPassword = store.getString(PrefConstants.UI_PROXY_PASSWORD);
            if (CommonUtils.isEmpty(userName) || CommonUtils.isEmpty(userPassword)) {

            }
        }

        return null;
    }

    public static DBWGlobalAuthenticator getInstance() {
        return instance;
    }

    private String encryptPassword(String password) throws EncryptionException {
        if (encrypter == null) {
            encrypter = new SecuredPasswordEncrypter();
        }
        return encrypter.encrypt(password);
    }

    private String decryptPassword(String password) throws EncryptionException {
        if (encrypter == null) {
            encrypter = new SecuredPasswordEncrypter();
        }
        return encrypter.decrypt(password);
    }

}
