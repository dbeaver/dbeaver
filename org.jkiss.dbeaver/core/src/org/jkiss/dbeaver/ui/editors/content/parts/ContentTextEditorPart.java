package org.jkiss.dbeaver.ui.editors.content.parts;

import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.ext.IContentEditorPart;
import org.jkiss.dbeaver.ui.DBIcon;

/**
 * LOB text editor
 */
public class ContentTextEditorPart extends TextEditor implements IContentEditorPart {

    public ContentTextEditorPart() {
        setDocumentProvider(new ContentTextDocumentProvider());
    }

    @Override
    protected void updateStatusField(String category)
    {
        super.updateStatusField(
            category);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public IEditorActionBarContributor getActionBarContributor()
    {
        return null;
    }

    public String getContentTypeTitle()
    {
        return "Text";
    }

    public Image getContentTypeImage()
    {
        return DBIcon.TEXT.getImage();
    }

    public String getPreferedMimeType()
    {
        return "text";
    }

    public long getMaxContentLength()
    {
        return 10 * 1024 * 1024;
    }

    /**
     * Always return true cos' text editor can load any binary content
     * @return
     */
    public boolean isContentValid()
    {
        return true;
    }
}
