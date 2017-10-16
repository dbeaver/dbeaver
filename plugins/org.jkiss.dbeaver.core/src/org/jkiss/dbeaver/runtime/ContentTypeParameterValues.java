package org.jkiss.dbeaver.runtime;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.IParameterValues;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;

public class ContentTypeParameterValues implements IParameterValues {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Map getParameterValues()
    {
        final Map values = new HashMap();
        IContentType[] allContentTypes = Platform.getContentTypeManager().getAllContentTypes();
        for (IContentType contentType : allContentTypes) {
            values.put(contentType.getName(), contentType.getId());
        }
        return values;
    }

}
