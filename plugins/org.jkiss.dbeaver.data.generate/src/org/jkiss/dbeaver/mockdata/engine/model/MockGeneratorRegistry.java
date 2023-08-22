// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.engine.model;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.DBPDataSource;
import java.util.Iterator;
import org.eclipse.core.runtime.IConfigurationElement;
import java.util.Set;
import org.jkiss.utils.CommonUtils;
import java.util.Collection;
import java.util.Arrays;
import java.util.HashSet;
import org.eclipse.core.runtime.IExtensionRegistry;
import java.util.ArrayList;
import org.eclipse.core.runtime.Platform;
import java.util.List;
import org.jkiss.dbeaver.Log;

public class MockGeneratorRegistry
{
    static final String TAG_GENERATOR = "generator";
    public static final String FK_GENERATOR_ID = "fkGenerator";
    private static final Log log;
    private static MockGeneratorRegistry instance;
    private final List<MockGeneratorDescriptor> generators;
    
    static {
        log = Log.getLog((Class)MockGeneratorRegistry.class);
        MockGeneratorRegistry.instance = null;
    }
    
    public static synchronized MockGeneratorRegistry getInstance() {
        if (MockGeneratorRegistry.instance == null) {
            (MockGeneratorRegistry.instance = new MockGeneratorRegistry()).loadExtensions(Platform.getExtensionRegistry());
        }
        return MockGeneratorRegistry.instance;
    }
    
    private MockGeneratorRegistry() {
        this.generators = new ArrayList<MockGeneratorDescriptor>();
    }
    
    private void loadExtensions(final IExtensionRegistry registry) {
        final Set<String> replacedSet = new HashSet<String>();
        final IConfigurationElement[] extConfigs = registry.getConfigurationElementsFor("org.jkiss.dbeaver.mockGenerator");
        IConfigurationElement[] array;
        for (int length = (array = extConfigs).length, i = 0; i < length; ++i) {
            final IConfigurationElement ext = array[i];
            if ("generator".equals(ext.getName())) {
                final MockGeneratorDescriptor generatorDescriptor = new MockGeneratorDescriptor(ext);
                this.generators.add(generatorDescriptor);
                final String[] replaces = generatorDescriptor.getReplaces();
                if (replaces != null) {
                    replacedSet.addAll(Arrays.asList(replaces));
                }
                if (!CommonUtils.isEmpty((Collection)generatorDescriptor.getPresets())) {
                    for (final MockGeneratorDescriptor.Preset preset : generatorDescriptor.getPresets()) {
                        this.generators.add(new MockGeneratorDescriptor(ext, preset));
                    }
                }
            }
        }
        for (final String replaced : replacedSet) {
            final MockGeneratorDescriptor generator = this.getGenerator(replaced);
            if (generator != null) {
                this.generators.remove(generator);
            }
        }
    }
    
    public void dispose() {
        this.generators.clear();
    }
    
    @Nullable
    public MockGeneratorDescriptor findGenerator(final DBPDataSource dataSource, final DBSTypedObject typedObject) {
        for (final MockGeneratorDescriptor descriptor : this.generators) {
            if ((!descriptor.isGlobal() && descriptor.supportsDataSource(dataSource) && descriptor.supportsType(typedObject)) || (descriptor.isGlobal() && descriptor.supportsType(typedObject))) {
                return descriptor;
            }
        }
        return null;
    }
    
    public List<MockGeneratorDescriptor> findAllGenerators(final DBPDataSource dataSource, final DBSTypedObject typedObject) {
        final List<MockGeneratorDescriptor> result = new ArrayList<MockGeneratorDescriptor>();
        for (final MockGeneratorDescriptor descriptor : this.generators) {
            if ("fkGenerator".equalsIgnoreCase(descriptor.getId())) {
                continue;
            }
            if ((descriptor.isGlobal() || descriptor.supportsDataSource(dataSource) || !descriptor.supportsType(typedObject)) && (!descriptor.isGlobal() || !descriptor.supportsType(typedObject))) {
                continue;
            }
            result.add(descriptor);
        }
        return result;
    }
    
    public MockGeneratorDescriptor getGenerator(final String id) {
        for (final MockGeneratorDescriptor descriptor : this.generators) {
            if (id.equals(descriptor.getId())) {
                return descriptor;
            }
        }
        return null;
    }
}
