package org.jkiss.dbeaver.ext.kognitio;

import org.jkiss.dbeaver.ext.generic.GenericDataSourceProvider;

public class KognitioDataSourceProvider extends GenericDataSourceProvider {
    public KognitioDataSourceProvider() {
    }

    @Override
    public long getFeatures() {
        return FEATURE_NONE;
    }
}
