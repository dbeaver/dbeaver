package org.jkiss.dbeaver.model.runtime;

import org.eclipse.core.runtime.IStatus;

/**
 * Process execution listener
 */
public interface DBRProcessListener {

    void onProcessFinish(IStatus status);

}
