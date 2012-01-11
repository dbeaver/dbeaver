// WMISensor.cpp : Defines the entry point for the DLL application.
//

#include "stdafx.h"
#include "WMIServiceJNI.h"
#include "WMIService.h"
#include "WMIUtils.h"


JNIEXPORT void JNICALL Java_org_jkiss_wmi_service_WMIService_initializeThread(
	JNIEnv* pJavaEnv, 
	jclass serviceClass)
{
	HRESULT hres =  ::CoInitializeEx(0, COINIT_MULTITHREADED); 
//	if (hres == RPC_E_CHANGED_MODE) {
//		hres =  ::CoInitialize(0); 
//	}
    if (FAILED(hres)) {
		THROW_COMMON_ERROR(L"Failed to initialize COM library", hres);
		return;
	}

	hres =  ::CoInitializeSecurity(
        NULL, 
        -1,                          // COM authentication
        NULL,                        // Authentication services
        NULL,                        // Reserved
        RPC_C_AUTHN_LEVEL_DEFAULT,   // Default authentication 
        RPC_C_IMP_LEVEL_IMPERSONATE, // Default Impersonation  
        NULL,                        // Authentication info
        EOAC_NONE,                   // Additional capabilities 
        NULL                         // Reserved
        );
    if (FAILED(hres) && hres != RPC_E_TOO_LATE) {
		THROW_COMMON_ERROR(L"Failed to initialize security", hres);
        return;
    }
}

JNIEXPORT void JNICALL Java_org_jkiss_wmi_service_WMIService_unInitializeThread(
	JNIEnv* pJavaEnv, 
	jclass serviceClass)
{
	::CoUninitialize();
}



/*
 * Class:     org_jkiss_wmi_service_WMIService
 * Method:    connect
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT jobject JNICALL Java_org_jkiss_wmi_service_WMIService_connect(
	JNIEnv* pJavaEnv, 
	jclass serviceClass,
	jobject logObject, 
	jstring domain, 
	jstring host, 
	jstring user, 
	jstring password,
	jstring locale,
	jstring resource)
{
	JNIMetaData& jniMeta = JNIMetaData::GetMetaData(pJavaEnv);
	jobject newServiceObject = pJavaEnv->NewObject(jniMeta.wmiServiceClass, jniMeta.wmiServiceConstructor);
	if (pJavaEnv->ExceptionCheck()) {
		return NULL;
	}
	pJavaEnv->SetObjectField(newServiceObject, jniMeta.wmiServiceLogField, logObject);

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
	WMIService* pService = WMIService::GetFromObject(pJavaEnv, object);
	if (pService != NULL) {
		pService->Release(pJavaEnv);
		delete pService;
	}
}

JNIEXPORT jobject JNICALL Java_org_jkiss_wmi_service_WMIService_openNamespace
  (JNIEnv * pJavaEnv, jobject object, jstring nsName)
{
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
