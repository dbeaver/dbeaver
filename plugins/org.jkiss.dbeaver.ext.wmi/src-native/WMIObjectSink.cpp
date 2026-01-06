#include "StdAfx.h"
#include "WMIObjectSink.h"
#include "WMIUtils.h"

static const long MAX_CACHE_SIZE = 1000;

WMIObjectSink::WMIObjectSink() :
	pService(NULL),
	javaSinkObject(NULL)
{
}

void WMIObjectSink::InitSink(WMIService* pSvc, JNIEnv* pJavaEnv, jobject javaObject)
{
	_ASSERT(pSvc != NULL);
	_ASSERT(javaObject != NULL);

	pService = pSvc;
	javaSinkObject = pJavaEnv->NewGlobalRef(javaObject);
}

void WMIObjectSink::TermSink(JNIEnv* pJavaEnv)
{
	if (javaSinkObject != NULL) {
		pJavaEnv->DeleteGlobalRef(javaSinkObject);
		javaSinkObject = NULL;
	}
}

WMIObjectSink::~WMIObjectSink(void)
{
}
/*
void WMIObjectSink::AttachThread()
{
	if (hThread == NULL) {
		hThread = ::GetCurrentThreadId();
	} else {
		DWORD hCurThread = ::GetCurrentThreadId();
		_ASSERT(hThread == hCurThread);
	}

	_ASSERT(pJavaVM != NULL);
	if (pJavaVM != NULL && pJavaEnv == NULL) {
		pJavaVM->AttachCurrentThread((void**)&pJavaEnv, NULL);
		_ASSERT(pJavaEnv != NULL);
	}
}

*/

void WMIObjectSink::FlushObjectsCache(JNIEnv* pJavaEnv)
{
	JNIMetaData& jniMeta = JNIMetaData::GetMetaData(pJavaEnv);
	JavaObjectVector objects;
	for (size_t i = 0; i < objectsCache.size(); i++) {
		objects.push_back(pService->MakeWMIObject(pJavaEnv, objectsCache[i]));
	}
	objectsCache.clear();
	jobjectArray javaArray = ::MakeJavaArrayFromVector(pJavaEnv, jniMeta.wmiObjectClass, objects);

	pJavaEnv->CallVoidMethod(
		javaSinkObject, 
		jniMeta.wmiObjectSinkIndicateMethod,
		javaArray);

	DeleteLocalRef(pJavaEnv, javaArray);

	if (pJavaEnv->ExceptionCheck()) {
		//pService->WriteLog(pJavaEnv, LT_ERROR, L"Can't call indicate for object sink");
		pJavaEnv->ExceptionClear();
	}
}

HRESULT WMIObjectSink::Indicate( 
    long lObjectCount,
    IWbemClassObject **ppClassObject)
{
	if (lObjectCount <= 0) {
		return WBEM_S_NO_ERROR;
	}
	for (long i = 0; i < lObjectCount; i++) {
		objectsCache.push_back(ppClassObject[i]);
	}
	if (objectsCache.size() >= MAX_CACHE_SIZE) {
		JNIEnv* pJavaEnv = NULL;
		WMIService::GetJavaVM()->AttachCurrentThread((void**)&pJavaEnv, NULL);
		_ASSERT(pJavaEnv != NULL);
		if (pJavaEnv != NULL) {
			FlushObjectsCache(pJavaEnv);
			WMIService::GetJavaVM()->DetachCurrentThread();
		}
	}

	return WBEM_S_NO_ERROR;
}

HRESULT WMIObjectSink::SetStatus( 
    long lFlags,
    HRESULT hResult,
    BSTR strParam,
    IWbemClassObject *pClassObject)
{
	const char* statusName = "unknown";
	if (lFlags == WBEM_STATUS_COMPLETE) {
		if (FAILED(hResult)) {
			statusName = "error";
		} else {
			statusName = "complete";
		}
	} else if (lFlags == WBEM_STATUS_PROGRESS) {
		statusName = "progress";
	} else if (lFlags == WBEM_STATUS_REQUIREMENTS) {
		statusName = "requirements";
	}

	JNIEnv* pJavaEnv = NULL;
	WMIService::GetJavaVM()->AttachCurrentThread((void**)&pJavaEnv, NULL);
	if (pJavaEnv == NULL) {
		return WBEM_E_FAILED;
	}

	if (!objectsCache.empty()) {
		FlushObjectsCache(pJavaEnv);
	}

	JNIMetaData& jniMeta = JNIMetaData::GetMetaData(pJavaEnv);
	jfieldID statusEnumID = pJavaEnv->GetStaticFieldID(
		jniMeta.wmiObjectSinkStatusClass,
		statusName,
		"Lorg/jkiss/wmi/service/WMIObjectSinkStatus;");
	_ASSERT(statusEnumID != NULL);
	jobject statusEnum = pJavaEnv->GetStaticObjectField(
		jniMeta.wmiObjectSinkStatusClass,
		statusEnumID);
	_ASSERT(statusEnum != NULL);
	if (statusEnum == NULL) {
		//pService->WriteLog(pJavaEnv, LT_ERROR, L"Can't detect object sink status java object");
	}

	jobject javaClassObject = NULL;
	if (pClassObject != NULL) {
		javaClassObject = pService->MakeWMIObject(pJavaEnv, pClassObject);
	}

	jstring javaParam = NULL;
	if (strParam != NULL) {
		javaParam = MakeJavaString(pJavaEnv, strParam);
	} else if (FAILED(hResult)) {
		// Put error to param string
		CComBSTR errorMessage;
		::FormatErrorMessage(L"Async error", hResult, &errorMessage);
		javaParam = MakeJavaString(pJavaEnv, errorMessage);
	}

	pJavaEnv->CallVoidMethod(
		javaSinkObject, 
		jniMeta.wmiObjectSinkSetStatusMethod,
		statusEnum,
		(jint)hResult,
		javaParam,
		javaClassObject);

	DeleteLocalRef(pJavaEnv, statusEnum);
	DeleteLocalRef(pJavaEnv, javaParam);
	DeleteLocalRef(pJavaEnv, javaClassObject);

	if (pJavaEnv->ExceptionCheck()) {
		//pJavaEnv->ExceptionDescribe();
		//pService->WriteLog(pJavaEnv, LT_ERROR, L"Can't set status for object sink");
		pJavaEnv->ExceptionClear();
	}

	// Check for end of sink
	if (lFlags == WBEM_STATUS_COMPLETE || FAILED(hResult)) {
		pService->RemoveObjectSink(pJavaEnv, this);
	}

	this->TermSink(pJavaEnv);

	// Detach thread
	jint result = WMIService::GetJavaVM()->DetachCurrentThread();

	return WBEM_S_NO_ERROR;
}
