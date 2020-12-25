package org.jkiss.dbeaver.ui.actions;

import org.eclipse.core.expressions.PropertyTester;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.navigator.DBNLocalFolder;

public class FolderPropertyTester extends PropertyTester {

	static protected final Log log = Log.getLog(FolderPropertyTester.class);

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (!(receiver instanceof DBNLocalFolder)) {
        	log.info(String.format("%s cannot be used with %s type", this.getClass().getName(), receiver.getClass().getName()));
            return false;
        }
        DBNLocalFolder localFolder = (DBNLocalFolder) receiver;
        return localFolder.hasConnected();
    }
}
