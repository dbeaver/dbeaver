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
JNIEXPORT jobject JNICALL Java_org_jkiss_wmi_service_WMIService_connect(
	JNIEnv* pJavaEnv, 
	jclass serviceClass,
	jstring domain, 
	jstring host, 
	jstring user, 
	jstring password,
	jstring locale,
	jstring resource)
{
	// Init COM for current thread
	if (!WMIInitializeThread(pJavaEnv)) return NULL;

	JNIMetaData& jniMeta = JNIMetaData::GetMetaData(pJavaEnv);
	jobject newServiceObject = pJavaEnv->NewObject(jniMeta.wmiServiceClass, jniMeta.wmiServiceConstructor);
	if (pJavaEnv->ExceptionCheck()) {
		return NULL;
	}
	//pJavaEnv->SetObjectField(newServiceObject, jniMeta.wmiServiceLogField, logObject);

	WMIService* pService = new WMIService(pJavaEnv, newServiceObject);
	
	CComBSTR bstrDomain, bstrHost, bstrUser, bstrPassword, bstrLocale, bstrResource;
	::GetJavaString(pJavaEnv, domain, &bstrDomain);
	::GetJavaString(pJavaEnv, host, &bstrHost);
	::GetJavaString(pJavaEnv, user, &bstrUser);
	::GetJavaString(pJavaEnv, password, &bstrPassword);
	::GetJavaString(pJavaEnv, locale, &bstrLocale);
	::GetJavaString(pJavaEnv, resource, &bstrResource);

	pService->Connect(pJavaEnv, bstrDomain, bstrHost, bstrUser, bstrPassword, bstrLocale, bstrResource);
	if (pJavaEnv->ExceptionCheck()) {
		DeleteLocalRef(pJavaEnv, newServiceObject);
		return NULL;
	}

	return newServiceObject;
}

/*
 * Class:     org_jkiss_wmi_service_WMIService
 * Method:    disconnect
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_jkiss_wmi_service_WMIService_close
  (JNIEnv * pJavaEnv, jobject object)
{
	if (!WMIInitializeThread(pJavaEnv)) return;

	WMIService* pService = WMIService::GetFromObject(pJavaEnv, object);
	if (pService != NULL) {
		pService->Release(pJavaEnv);
		delete pService;
	}
}

JNIEXPORT jobject JNICALL Java_org_jkiss_wmi_service_WMIService_openNamespace
  (JNIEnv * pJavaEnv, jobject object, jstring nsName)
{
	// Init COM for current thread
	if (!WMIInitializeThread(pJavaEnv)) return NULL;

	WMIService* pService = WMIService::GetFromObject(pJavaEnv, object);
	if (pService == NULL) {
		THROW_COMMON_EXCEPTION(L"WMI Service is not initialized");
		return NULL;
	}

	CComBSTR bstrNS;
	::GetJavaString(pJavaEnv, nsName, &bstrNS);
	return pService->OpenNamespace(pJavaEnv, bstrNS, 0);
}
/*
 * Class:     org_jkiss_wmi_service_WMIService
 * Method:    executeQuery
 *
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
}*/

JNIEXPORT void JNICALL Java_org_jkiss_wmi_service_WMIService_executeQuery(
	JNIEnv *pJavaEnv, 
	jobject object, 
	jstring query,
	jobject sinkObject,
	jlong lFlags)
{
	// Init COM for current thread
	if (!WMIInitializeThread(pJavaEnv)) return;

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
	pService->ExecuteQueryAsync(pJavaEnv, bstrQuery, sinkObject, (LONG)lFlags);
}

JNIEXPORT void JNICALL Java_org_jkiss_wmi_service_WMIService_enumClasses(
	JNIEnv *pJavaEnv, 
	jobject object, 
	jstring superClass,
	jobject sinkObject,
	jlong lFlags)
{
	// Init COM for current thread
	if (!WMIInitializeThread(pJavaEnv)) return;

	WMIService* pService = WMIService::GetFromObject(pJavaEnv, object);
	if (pService == NULL) {
		THROW_COMMON_EXCEPTION(L"WMI Service is not initialized");
		return;
	}
	if (sinkObject == NULL) {
		THROW_COMMON_EXCEPTION(L"NULL sink object specified");
		return;
	}
	CComBSTR bstrSuperClass;
	::GetJavaString(pJavaEnv, superClass, &bstrSuperClass);
	pService->EnumClasses(pJavaEnv, bstrSuperClass, sinkObject, (LONG)lFlags);
}

JNIEXPORT void JNICALL Java_org_jkiss_wmi_service_WMIService_enumInstances(
	JNIEnv *pJavaEnv, 
	jobject object, 
	jstring className,
	jobject sinkObject,
	jlong lFlags)
{
	// Init COM for current thread
	if (!WMIInitializeThread(pJavaEnv)) return;

	WMIService* pService = WMIService::GetFromObject(pJavaEnv, object);
	if (pService == NULL) {
		THROW_COMMON_EXCEPTION(L"WMI Service is not initialized");
		return;
	}
	if (sinkObject == NULL) {
		THROW_COMMON_EXCEPTION(L"NULL sink object specified");
		return;
	}
	CComBSTR bstrClassName;
	::GetJavaString(pJavaEnv, className, &bstrClassName);
	pService->EnumInstances(pJavaEnv, bstrClassName, sinkObject, (LONG)lFlags);
}

JNIEXPORT void JNICALL Java_org_jkiss_wmi_service_WMIService_cancelSink(
	JNIEnv *pJavaEnv, 
	jobject object,
	jobject sinkObject)
{
	// Init COM for current thread
	if (!WMIInitializeThread(pJavaEnv)) return;

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
