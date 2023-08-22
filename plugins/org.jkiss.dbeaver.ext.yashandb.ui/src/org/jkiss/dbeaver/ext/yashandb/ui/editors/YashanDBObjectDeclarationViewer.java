package org.jkiss.dbeaver.ext.yashandb.ui.editors;

import org.jkiss.dbeaver.ui.editors.sql.SQLSourceViewer;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description: YashanDBObjectDeclarationViewer helps to view object declaration, but not change it.
 */
public class YashanDBObjectDeclarationViewer extends SQLSourceViewer {
    public YashanDBObjectDeclarationViewer() {
    }

    @Override
    protected boolean isReadOnly() {
        return true;
    }
}
