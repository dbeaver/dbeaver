/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.runtime;

import java.lang.reflect.InvocationTargetException;

/**
 * Runnable which stores some result
 */
public abstract class RunnableWithResultEx<RESULT_TYPE>
    extends RunnableWithResult<RunnableWithResultEx.ResultOrError<RESULT_TYPE>> {

    public static class ResultOrError<RESULT_TYPE> { 
        private final RESULT_TYPE result;
        private final Exception exception;
        
        public ResultOrError(RESULT_TYPE result, Exception exception) {
            this.result = result;
            this.exception = exception;
        }

        /**
         * Return result or throw InvocationTargetException if exception occurred during execution
         */
        public RESULT_TYPE getResultOrRethrow() throws InvocationTargetException {
            if (exception == null) {
                return result;
            } else {
                throw new InvocationTargetException(exception, "Exception caught during delegated operation");
            }
        }
    }
    
    private boolean isCancelled = false;
    
    @Override
    public final ResultOrError<RESULT_TYPE> runWithResult() {
        try {
            return new ResultOrError<>(this.runWithResultImpl(), null);
        } catch (Exception ex) {
            return new ResultOrError<>(null, ex);
        }
    }

    protected abstract RESULT_TYPE runWithResultImpl() throws Exception;

    /**
     * Cancel execution
     */
    public final void cancel() { 
        if (!isCancelled) {
            isCancelled = true;
            onCancelled();
        }
    }

    protected void onCancelled() { }

    /**
     * Return result or throw InvocationTargetException on execution failure
     */
    public final RESULT_TYPE getResultOrRethrow() throws InvocationTargetException {
        return getResult().getResultOrRethrow();
    }
}
