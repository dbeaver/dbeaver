// WMISensor.cpp : Defines the entry point for the DLL application.
//

#include "stdafx.h"
#include "WMIServiceJNI.h"
#include "WMIService.h"
#include "WMIUtils.h"

/*
 * Class:     org_jkiss_wmi_service_WMIService
 * Method:    connect
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_org_jkiss_wmi_service_WMIService_connect(
	JNIEnv * pJavaEnv, 
	jobject object, 
	jstring domain, 
	jstring host, 
	jstring user, 
	jstring password,
	jstring locale,
	jstring resource)
{
	WMIService* pService = new WMIService(pJavaEnv, object);
	if (pJavaEnv->ExceptionCheck()) {
		return;
	}
	
	CComBSTR bstrDomain, bstrHost, bstrUser, bstrPassword, bstrLocale, bstrResource;
	::GetJavaString(pJavaEnv, domain, &bstrDomain);
	::GetJavaString(pJavaEnv, host, &bstrHost);
	::GetJavaString(pJavaEnv, user, &bstrUser);
	::GetJavaString(pJavaEnv, password, &bstrPassword);
	::GetJavaString(pJavaEnv, locale, &bstrLocale);
	::GetJavaString(pJavaEnv, resource, &bstrResource);

	pService->Connect(pJavaEnv, bstrDomain, bstrHost, bstrUser, bstrPassword, bstrLocale, bstrResource);
	if (pJavaEnv->ExceptionCheck()) {
		return;
	}
}

/*
 * Class:     org_jkiss_wmi_service_WMIService
 * Method:    disconnect
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_jkiss_wmi_service_WMIService_close
  (JNIEnv * pJavaEnv, jobject object)
{
	WMIService* pService = WMIService::GetFromObject(pJavaEnv, object);
	if (pService != NULL) {
		pService->Close(pJavaEnv);
		delete pService;
	}
}

/*
 * Class:     org_jkiss_wmi_service_WMIService
 * Method:    executeQuery
 * Signature: (Ljava/lang/String;)Lcom/symantec/cas/ucf/sensors/wmi/pService/WQLResultSet;
 */
JNIEXPORT jobjectArray JNICALL Java_org_jkiss_wmi_service_WMIService_executeQuery(
	JNIEnv *pJavaEnv, 
	jobject object, 
	jstring query,
	jboolean bSync)
{
	WMIService* pService = WMIService::GetFromObject(pJavaEnv, object);
	if (pService == NULL) {
		THROW_COMMON_EXCEPTION(L"WMI Service is not initialized");
		return NULL;
	}
	if (query == NULL) {
		THROW_COMMON_EXCEPTION(L"NULL query specified");
		return NULL;
	}
	CComBSTR bstrQuery;
	::GetJavaString(pJavaEnv, query, &bstrQuery);
	return pService->ExecuteQuery(pJavaEnv, bstrQuery, bSync == JNI_TRUE);
}

JNIEXPORT void JNICALL Java_org_jkiss_wmi_service_WMIService_executeQueryAsync(
	JNIEnv *pJavaEnv, 
	jobject object, 
	jstring query,
	jobject sinkObject,
	jboolean bSendStatus)
{
	WMIService* pService = WMIService::GetFromObject(pJavaEnv, object);
	if (pService == NULL) {
		THROW_COMMON_EXCEPTION(L"WMI Service is not initialized");
		return;
	}
	if (query == NULL) {
		THROW_COMMON_EXCEPTION(L"NULL query specified");
		return;
	}
	if (sinkObject == NULL) {
		THROW_COMMON_EXCEPTION(L"NULL sink object specified");
		return;
	}
	CComBSTR bstrQuery;
	::GetJavaString(pJavaEnv, query, &bstrQuery);
	pService->ExecuteQueryAsync(pJavaEnv, bstrQuery, sinkObject, bSendStatus == JNI_TRUE);
}

JNIEXPORT void JNICALL Java_org_jkiss_wmi_service_WMIService_cancelAsyncOperation(
	JNIEnv *pJavaEnv, 
	jobject object,
	jobject sinkObject)
{
	WMIService* pService = WMIService::GetFromObject(pJavaEnv, object);
	if (pService == NULL) {
		THROW_COMMON_EXCEPTION(L"WMI Service is not initialized");
		return;
	}
	if (sinkObject == NULL) {
		THROW_COMMON_EXCEPTION(L"NULL sink object specified");
		return;
	}
	pService->CancelAsyncOperation(pJavaEnv, sinkObject);
}
