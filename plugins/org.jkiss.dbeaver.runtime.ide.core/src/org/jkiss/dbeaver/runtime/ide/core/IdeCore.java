/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.runtime.ide.core;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class IdeCore {
	
	public static final String BUNDLE_SYMBOLIC_NAME = "org.jkiss.dbeaver.runtime.ide.core"; //$NON-NLS-1$
	
	public static IStatus createError(String message) {
		return new Status(IStatus.ERROR, BUNDLE_SYMBOLIC_NAME, message);
	}

	public static IStatus createError(String message, Throwable t) {
		return new Status(IStatus.ERROR, BUNDLE_SYMBOLIC_NAME, message, t);
	}

    public static IStatus createCancel(String message) {
        return new Status(IStatus.CANCEL, BUNDLE_SYMBOLIC_NAME, message);
    }

}
