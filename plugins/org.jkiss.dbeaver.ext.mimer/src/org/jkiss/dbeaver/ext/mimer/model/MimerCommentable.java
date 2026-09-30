/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mimer.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Implemented by every Mimer SQL object type that reads its own {@code COMMENT ON <keyword>} text
 * via {@link MimerUtils#readObjectComment} - lets {@link MimerUtils#addCommentModifyAction} build
 * the "Comment ..." persist action for any of them without knowing which concrete class it's
 * working with. Every implementer already had this exact {@code getComment(monitor)} signature
 * before this interface existed; adding it is purely additive.
 *
 * @author Mimer Information Technology
 */
public interface MimerCommentable extends DBSObject {

    String getComment(@NotNull DBRProgressMonitor monitor) throws DBException;
}
