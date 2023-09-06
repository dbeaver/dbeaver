package org.jkiss.dbeaver.ext.yashandb.debug.ui.internal;

import org.jkiss.dbeaver.debug.ui.DBGEditorAdvisor;

public class YashanDBSourceEditorAdvisor implements DBGEditorAdvisor {

    private static final String YASHANDB_SOURCE_VIEW = "source.declaration"; //$NON-NLS-1$

    @Override
    public String getSourceFolderId() {
        return YASHANDB_SOURCE_VIEW;
    }

}
