package org.jkiss.dbeaver.model.impl.resources.bookmarks;

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescriber;
import org.eclipse.core.runtime.content.IContentDescription;

import java.io.IOException;
import java.io.InputStream;

/**
 * Bookmark content type describer
 */
public class BookmarkContentTypeDescriber implements IContentDescriber {

    @Override
    public int describe(InputStream contents, IContentDescription description) throws IOException
    {
        return INDETERMINATE;
    }

    @Override
    public QualifiedName[] getSupportedOptions()
    {
        return new QualifiedName[] {  };
    }
}
