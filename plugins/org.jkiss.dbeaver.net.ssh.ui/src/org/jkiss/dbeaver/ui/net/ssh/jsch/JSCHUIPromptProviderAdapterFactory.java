/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jkiss.dbeaver.ui.net.ssh.jsch;

import org.eclipse.core.runtime.IAdapterFactory;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.net.ssh.JSCHUserInfoPromptProvider;
import org.jkiss.dbeaver.model.net.ssh.SSHImplementationJsch;

public class JSCHUIPromptProviderAdapterFactory implements IAdapterFactory {

    private static final Log log = Log.getLog(JSCHUIPromptProviderAdapterFactory.class);

    private static final Class<?>[] CLASSES = new Class[] { SSHImplementationJsch.class };
    
    @Override
    public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
        if (adaptableObject instanceof SSHImplementationJsch) {
            if (adapterType == JSCHUserInfoPromptProvider.class) {
                try {
                    return adapterType.cast(new JSCHUIPromptProvider());
                } catch (Throwable e) {
                    log.error("Error installing creating UI prompt provider", e);
                    return null;
                }
            }
        }
        return null;
    }

    @Override
    public Class<?>[] getAdapterList() {
        return CLASSES;
    }

}
