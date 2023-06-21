/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.dpi.client;

import com.google.gson.Gson;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.dpi.api.ApiEndpoint;
import org.jkiss.dbeaver.model.dpi.api.ApiParameter;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.utils.CommonUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST utils
 */
public class RestUtils {

    private static final Log log = Log.getLog(RestUtils.class);

    public static <T> T createRestClient(URL serverURL, Class<T> api) {
        return api.cast(Proxy.newProxyInstance(
            api.getClassLoader(),
            new Class[]{ api },
            new RestInvocationHandler(serverURL)));
    }

    private static class RestInvocationHandler implements InvocationHandler {

        private final RestClient client;

        public RestInvocationHandler(URL serverURL) {
            client = new RestClient(serverURL);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            ApiEndpoint apiEndpoint = method.getAnnotation(ApiEndpoint.class);
            if (apiEndpoint == null) {
                throw new DBCException("Only API endpoints are allowed. Method " + method.getName() + " is not annotated");
            }

            String endpoint = apiEndpoint.id();
            if (CommonUtils.isEmpty(endpoint)) {
                endpoint = method.getName();
            }

            Map<String, Object> parameters = new LinkedHashMap<>();

            Gson gson = RestClient.gson;

            int parameterCount = method.getParameterCount();
            for (int i = 0; i < parameterCount; i++) {
                for (Annotation pa : method.getParameterAnnotations()[i]) {
                    if (pa.annotationType() == ApiParameter.class) {
                        ApiParameter apiParam = (ApiParameter) pa;
                        String paramId = apiParam.value();
                        Object argValue = args[i];
                        parameters.put(paramId, argValue);
                        break;
                    }
                }
            }
            String response = client.sendRequest(endpoint, parameters);
            if (response == null) {
                return null;
            }
            return gson.fromJson(response, method.getReturnType());
        }
    }
}
