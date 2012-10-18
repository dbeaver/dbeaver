package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescriber;
import org.eclipse.core.runtime.content.IContentDescription;

import java.io.IOException;
import java.io.InputStream;

/**
 * SQL content type describer
 */
public class SQLContentTypeDescriber implements IContentDescriber {

    @Override
    public int describe(InputStream contents, IContentDescription description) throws IOException
    {
        return INDETERMINATE;
    }

    @Override
    public QualifiedName[] getSupportedOptions()
    {
        return new QualifiedName[] { SQLEditorInput.PROP_DATA_SOURCE_ID };
    }
}
