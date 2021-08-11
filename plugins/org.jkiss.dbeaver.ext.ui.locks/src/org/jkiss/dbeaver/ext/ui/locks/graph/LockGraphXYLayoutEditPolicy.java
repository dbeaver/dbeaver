/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2017 Andrew Khitrin (ahitrin@gmail.com) 
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
package org.jkiss.dbeaver.ext.ui.locks.graph;


import org.eclipse.gef3.EditPart;
import org.eclipse.gef3.commands.Command;
import org.eclipse.gef3.editpolicies.XYLayoutEditPolicy;
import org.eclipse.gef3.requests.CreateRequest;

public class LockGraphXYLayoutEditPolicy extends XYLayoutEditPolicy {

	@Override
	protected Command createChangeConstraintCommand(EditPart child,
			Object constraint) {
		return null;
	}

	@Override
	protected Command getCreateCommand(CreateRequest request) {
		return null;
	}



}
