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

/*
 * Created on Jul 25, 2004
 */
package org.jkiss.dbeaver.erd.ui.directedit;

/**
 * Represents interface for outputting validation error messages to some widget
 * @author Serge Rider
 */
public interface ValidationMessageHandler
{

	void setMessageText(String text);

	/**
	 * Resets so that the validation message is no longer shown
	 */
	void reset();
}