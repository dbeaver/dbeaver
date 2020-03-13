/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.firebird.model.plan;

/**
 * Manage raising exception with more context information.
 *
 * @author tomashorak@post.cz
 */
class FireBirdPlanException extends Exception {

	/**
	 * FireBirdPlanException
	 */
	private static final long serialVersionUID = 1L;

	public FireBirdPlanException(String expected, String actual, int position, String plan) {
		super(makeMessage(expected, actual, position, plan));
	}
	
	public FireBirdPlanException(String unexpected, int position, String plan) {
		super(makeMessage(unexpected, position, plan));
	}
	
	public FireBirdPlanException(String info, String index) {
		super(String.format("Error when getting info about %1$s index(%2$s)", index, info));
	}
	
	private static String makeMessage(String expected, String actual, int position, String plan)
	{
		return addPlanMark(
				String.format("Error parsing plan - expected %1$s at position %2$d but got %3$s", expected, position, actual), 
				position, plan);
	}
	
	private static String makeMessage(String unexpected, int position, String plan)
	{
		return addPlanMark(
				String.format("Error parsing plan - unexpected token %1$s at position %2$d", unexpected, position), 
				position, plan);
	}
	
	private static String addPlanMark(String message, int position, String plan)
	{
		StringBuilder sb = new StringBuilder(message);
		sb.append("\n");
		sb.append(plan.substring(0, position - 1));
		sb.append("^^^");
		sb.append(plan.substring(position));
        return sb.toString();
	}
    
}