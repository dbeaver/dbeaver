// WMISensor.cpp : Defines the entry point for the DLL application.
//

#include "stdafx.h"
#include "WMIService.h"
#include "WMIObject.h"
#include "WMIObjectSink.h"
#include "WMIUtils.h"

#define FIELD_NAME_SERVICE_HANDLE ("serviceHandle")

class WMIThreadInfo {
public:
	DWORD nThreadId;
	JNIEnv* pThreadEnv;
	ObjectSinkVector sinks;
};

typedef std::vector< WMIThreadInfo* > ThreadInfoVector;

static CComCriticalSection csSinkThreads;
JavaVM* WMIService::pJavaVM = NULL;
//static ThreadInfoVector threadInfos;

WMIService::WMIService(JNIEnv* pJavaEnv, jobject javaObject) :
	bCoInitialized(false)
{
	serviceJavaObject = pJavaEnv->NewGlobalRef(javaObject);
	if (!pJavaEnv->ExceptionCheck()) {
		pJavaEnv->SetLongField(serviceJavaObject, JNIMetaData::GetMetaData(pJavaEnv).wmiServiceHandleField, (jlong)this);
	}

	{
		CComCritSecLock<CComCriticalSection> guard(csSinkThreads);
		if (pJavaVM == NULL) {
			pJavaEnv->GetJavaVM(&pJavaVM);
			_ASSERT(pJavaVM != NULL);
		}

	}
}

WMIService::~WMIService()
{
}

WMIService* WMIService::GetFromObject(JNIEnv* pJavaEnv, jobject javaObject)
{
	jclass objectClass = pJavaEnv->GetObjectClass(javaObject);
	jfieldID fid = pJavaEnv->GetFieldID(objectClass, "serviceHandle", "J");
	DeleteLocalRef(pJavaEnv, objectClass);
	_ASSERT(fid != NULL);
	if (fid == NULL) {
		return NULL;
	}
	return (WMIService*)pJavaEnv->GetLongField(javaObject, fid);
}

void WMIService::WriteLog(JNIEnv* pLocalEnv, LogType logType, LPCWSTR wcMessage, HRESULT hr)
{
#ifdef DEBUG
	_RPTW1(_CRT_WARN, L"%s\n", wcMessage);
#endif

	// Get log field
	const char* cLogMethodName = "debug";
	switch (logType) {
		case LT_TRACE: cLogMethodName = "trace"; break;
		case LT_DEBUG: cLogMethodName = "debug"; break;
		case LT_INFO: cLogMethodName = "info"; break;
		case LT_ERROR: cLogMethodName = "error"; break;
		case LT_FATAL: cLogMethodName = "fatal"; break;
		default: 
			// Unsuported log type
			return;
	}

	JNIMetaData& jniMeta = JNIMetaData::GetMetaData(pLocalEnv);
	jobject logObject = pLocalEnv->GetObjectField(serviceJavaObject, jniMeta.wmiServiceLogField);
	_ASSERT(logObject != NULL);
	if (logObject != NULL) {
		// Get log method
		jclass logClass = pLocalEnv->GetObjectClass(logObject);
		jmethodID logMethodID = pLocalEnv->GetMethodID(logClass, cLogMethodName, "(Ljava/lang/Object;)V");
		DeleteLocalRef(pLocalEnv, logClass);
		_ASSERT(logMethodID != NULL);
		if (logMethodID != NULL) {
			CComBSTR errorMessage;
			if (FAILED(hr)) {
				FormatErrorMessage(wcMessage, hr, &errorMessage);
				wcMessage = errorMessage;
			}
			jstring jMessage = MakeJavaString(pLocalEnv, wcMessage);
			pLocalEnv->CallVoidMethod(logObject, logMethodID, jMessage);
			DeleteLocalRef(pLocalEnv, jMessage);
		}
		DeleteLocalRef(pLocalEnv, logObject);
	}

	// Remove any exceptions occured in this method
	if (pLocalEnv->ExceptionCheck()) {
		pLocalEnv->ExceptionClear();
	}
}

/*
 * Class:     com_symantec_cas_ucf_sensors_wmi_service_WMIService
 * Method:    connect
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
 */
void WMIService::Connect(
	JNIEnv* pJavaEnv,
	LPWSTR domain, 
	LPWSTR host, 
	LPWSTR user, 
	LPWSTR password,
	LPWSTR locale,
	LPWSTR resource)
{
    if (this->ptrWbemServices != NULL) {
		THROW_COMMON_EXCEPTION(L"WMI Locator was already initialized");
		return;
	}
	HRESULT hres =  ::CoInitializeEx(0, COINIT_MULTITHREADED); 
    if (FAILED(hres)) {
		THROW_COMMON_ERROR(L"Failed to initialize COM library", hres);
		return;
	}
	bCoInitialized = true;

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

    // Step 3: ---------------------------------------------------
    // Obtain the initial locator to WMI -------------------------

	CComPtr<IWbemLocator> ptrWbemLocator;
    hres = CoCreateInstance(
        CLSID_WbemLocator,             
        0, 
        CLSCTX_INPROC_SERVER, 
        IID_IWbemLocator, 
		(LPVOID *) &ptrWbemLocator);
    if (FAILED(hres)) {
		THROW_COMMON_ERROR(L"Failed to create IWbemLocator object", hres);
		return;
    }

	// Connect to server
	CComBSTR resourceURI;
	if (resource != NULL) {
		resourceURI.Append(L"\\\\");
		if (host != NULL) {
			resourceURI.Append(host);
		} else {
			resourceURI.Append(L".");
		}
		if (resource[0] != '\\') {
			resourceURI.Append(L"\\");
		}
		resourceURI.Append(resource);
	}
	CComBSTR resourceDomain;
	if (domain != NULL) {
		resourceDomain.Append(L"NTLMDOMAIN:");
		resourceDomain.Append(domain);
	}
	hres = ptrWbemLocator->ConnectServer(
        resourceURI,
		user,				// User name
		password,		// User password
		locale == NULL ? L"MS_409" : locale,	// Locale
        NULL,                           // Security flags
        resourceDomain,					// Authority
        0,                              // Context object
        &ptrWbemServices                  // IWbemServices proxy
        );
    if (FAILED(hres)) {
        THROW_COMMON_ERROR(L"Failed to connect to WMI Service", hres);
		return;
    }

    // Set security levels on a WMI connection ------------------
    hres = CoSetProxyBlanket(
		ptrWbemServices,					// Indicates the proxy to set
		RPC_C_AUTHN_WINNT,           // RPC_C_AUTHN_xxx
		RPC_C_AUTHZ_NONE,            // RPC_C_AUTHZ_xxx
		NULL,                        // Server principal name 
		RPC_C_AUTHN_LEVEL_CALL,      // RPC_C_AUTHN_LEVEL_xxx 
		RPC_C_IMP_LEVEL_IMPERSONATE, // RPC_C_IMP_LEVEL_xxx
		NULL,                        // client identity
		EOAC_NONE                    // proxy capabilities 
		);
    if (FAILED(hres)) {
        THROW_COMMON_ERROR(L"Could not set proxy blanket", hres);
		return;
    }

	WriteLog(pJavaEnv, LT_INFO, bstr_t("WMI Service connected to ") + (LPCWSTR)resource);
}

void WMIService::Release(JNIEnv* pJavaEnv)
{
	ptrWbemServices = NULL;
	WriteLog(pJavaEnv, LT_INFO, L"WMI Service closed");

	if (serviceJavaObject != NULL) {
		pJavaEnv->SetLongField(serviceJavaObject, JNIMetaData::GetMetaData(pJavaEnv).wmiServiceHandleField, 0);
		pJavaEnv->DeleteGlobalRef(serviceJavaObject);
		serviceJavaObject = NULL;
	}

	if (bCoInitialized) {
		::CoUninitialize();
	}
}

	
/*
jobjectArray WMIService::ExecuteQuery(JNIEnv* pJavaEnv, LPWSTR queryString, bool sync)
{
	if (queryString == NULL) {
		THROW_COMMON_EXCEPTION(L"Empty query specified");
		return NULL;
	}
	if (pWbemServices == NULL) {
		THROW_COMMON_EXCEPTION(L"WMI Service is not initialized");
		return NULL;
	}

    // Use the IWbemServices pointer to make requests of WMI ----
	this->WriteLog(pJavaEnv, LT_DEBUG, bstr_t(L"WQL: ") + queryString);
	jlong startTime = ::GetCurrentJavaTime();
    CComPtr<IEnumWbemClassObject> pEnumerator;
	long lFlags = WBEM_FLAG_FORWARD_ONLY | WBEM_FLAG_DIRECT_READ;
	if (!sync) lFlags |= WBEM_FLAG_RETURN_IMMEDIATELY;
    HRESULT hres = pWbemServices->ExecQuery(
        L"WQL",
        queryString,
        lFlags, 
        NULL,
        &pEnumerator);
    if (FAILED(hres)) {
        THROW_COMMON_ERROR(L"Could not execute query", hres);
        return NULL;
    }

	jlong parseTime = ::GetCurrentJavaTime();
	{
		bstr_t msg(L"Query finished in ");
		msg += (long)(parseTime - startTime);
		msg += L"ms";
		this->WriteLog(pJavaEnv, LT_DEBUG, msg);
	}

	JavaObjectVector rows;

	int objectsCount = 0;
	while (pEnumerator != NULL) {
		CComPtr<IWbemClassObject> pClassObject;
		ULONG uReturn = 0;
        hres = pEnumerator->Next(WBEM_INFINITE, 1, &pClassObject, &uReturn);
        if (uReturn == 0) {
            break;
        }
		if (FAILED(hres)) {
			this->WriteLog(pJavaEnv, LT_ERROR, L"Could not obtain next class object", hres);
			continue;
		}
		objectsCount++;
		jobject rowObject = MakeWMIObject(pJavaEnv, pClassObject);

		CHECK_JAVA_EXCEPTION_NULL();

		if (rowObject != NULL) {
			rows.push_back(rowObject);
		}
	}
	{
		jlong endTime = ::GetCurrentJavaTime();
		bstr_t msg(L"Query returned [");
		msg += (long)rows.size();
		msg += L"] object(s), parse time: ";
		msg += (long)(endTime - parseTime);
		msg += L"ms";

		this->WriteLog(pJavaEnv, LT_DEBUG, msg);
	}

	// Make object array from rows vector
	return ::MakeJavaArrayFromVector(pJavaEnv, JNIMetaData::GetMetaData(pJavaEnv).wmiObjectClass, rows);
}
*/

jobject WMIService::OpenNamespace(JNIEnv* pJavaEnv, LPWSTR nsName, LONG lFlags)
{
	CComPtr<IWbemServices> ptrNamespace;
	HRESULT hres = ptrWbemServices->OpenNamespace(nsName, lFlags, NULL, &ptrNamespace, NULL);
	if (FAILED(hres)) {
		THROW_COMMON_ERROR(L"Could not open namespace", hres);
		return NULL;
	}

	JNIMetaData& jniMeta = JNIMetaData::GetMetaData(pJavaEnv);
	jobject logObject = pJavaEnv->GetObjectField(serviceJavaObject, jniMeta.wmiServiceLogField);
	if (pJavaEnv->ExceptionCheck()) {
		return NULL;
	}
	
	jobject newServiceObject = pJavaEnv->NewObject(jniMeta.wmiServiceClass, jniMeta.wmiServiceConstructor, logObject);
	if (pJavaEnv->ExceptionCheck()) {
		return NULL;
	}
	WMIService* pServiceHandler = new WMIService(pJavaEnv, newServiceObject);
	pServiceHandler->ptrWbemServices = ptrNamespace;

	WriteLog(pJavaEnv, LT_INFO, bstr_t("Connected to WMI namespace ") + nsName);

	return newServiceObject;
}

void WMIService::MakeObjectSink(JNIEnv* pJavaEnv, jobject javaSinkObject, IWbemObjectSink** ppSink)
{
	CComPtr<WMIObjectSink> pSink = new CComObject<WMIObjectSink>();
	pSink->InitSink(this, pJavaEnv, javaSinkObject);

	CComPtr<IWbemObjectSink> pSecuredSink;

	// Make unsecured appartments for sink
	{
		CComPtr<IUnsecuredApartment> pUnsecApp;
		HRESULT hr = CoCreateInstance(CLSID_UnsecuredApartment, NULL, 
			CLSCTX_LOCAL_SERVER, IID_IUnsecuredApartment, 
			(void**)&pUnsecApp);
		if (pUnsecApp != NULL) {
			CComPtr<IUnknown> pStubUnk;
			pUnsecApp->CreateObjectStub(
			   pSink,
			   &pStubUnk);
			if (pStubUnk != NULL) {
				pStubUnk.QueryInterface(&pSecuredSink);
				if (pSecuredSink != NULL) {
					this->WriteLog(pJavaEnv, LT_DEBUG, L"Using unsecured appartments for async queries");
				}
			}
		}
	}

    //IEnumWbemClassObject* pEnumerator = NULL;
	if (pSecuredSink != NULL) {
		*ppSink = pSecuredSink.Detach();
	} else {
		*ppSink = pSink.Detach();
	}

	sinkList.push_back(pSink);
}

void WMIService::ExecuteQueryAsync(JNIEnv* pJavaEnv, LPWSTR queryString, jobject javaSinkObject, LONG lFlags)
{
	if (queryString == NULL) {
		THROW_COMMON_EXCEPTION(L"Empty query specified");
		return;
	}
	if (ptrWbemServices == NULL) {
		THROW_COMMON_EXCEPTION(L"WMI Service is not initialized");
		return;
	}

	CComPtr<IWbemObjectSink> pActiveSink;
	MakeObjectSink(pJavaEnv, javaSinkObject, &pActiveSink);

    // Use the IWbemServices pointer to make requests of WMI ----
	this->WriteLog(pJavaEnv, LT_DEBUG, bstr_t(L"Async WQL: ") + queryString);
	lFlags |= WBEM_FLAG_DIRECT_READ;
	//if (sendStatus) lFlags |= WBEM_FLAG_SEND_STATUS;

	HRESULT hres = ptrWbemServices->ExecQueryAsync(
        L"WQL",
        queryString,
        lFlags, 
        NULL,
		pActiveSink);
    if (FAILED(hres)) {
        THROW_COMMON_ERROR(L"Could not execute query", hres);
		return;
    }
}

void WMIService::EnumClasses(JNIEnv* pJavaEnv, LPWSTR baseClass, jobject javaSinkObject, LONG lFlags)
{
	if (ptrWbemServices == NULL) {
		THROW_COMMON_EXCEPTION(L"WMI Service is not initialized");
		return;
	}

	CComPtr<IWbemObjectSink> pActiveSink;
	MakeObjectSink(pJavaEnv, javaSinkObject, &pActiveSink);

	HRESULT hres = ptrWbemServices->CreateClassEnumAsync(
        baseClass,
        lFlags,
        NULL,
		pActiveSink);
    if (FAILED(hres)) {
        THROW_COMMON_ERROR(L"Could not create classes enumerator", hres);
		return;
    }
}

void WMIService::EnumInstances(JNIEnv* pJavaEnv, LPWSTR className, jobject javaSinkObject, LONG lFlags)
{
	if (ptrWbemServices == NULL) {
		THROW_COMMON_EXCEPTION(L"WMI Service is not initialized");
		return;
	}

	CComPtr<IWbemObjectSink> pActiveSink;
	MakeObjectSink(pJavaEnv, javaSinkObject, &pActiveSink);

	HRESULT hres = ptrWbemServices->CreateInstanceEnumAsync(
        className,
        lFlags,
        NULL,
		pActiveSink);
    if (FAILED(hres)) {
        THROW_COMMON_ERROR(L"Could not create classes enumerator", hres);
		return;
    }
}

void WMIService::CancelAsyncOperation(JNIEnv* pJavaEnv, jobject javaSinkObject)
{
	_ASSERT(javaSinkObject != NULL);
	if (javaSinkObject == NULL) {
		THROW_COMMON_EXCEPTION(L"NULL sink object specified");
		return;
	}
	if (ptrWbemServices == NULL) {
		THROW_COMMON_EXCEPTION(L"WMI Service is not initialized");
		return;
	}
	WriteLog(pJavaEnv, LT_DEBUG, L"Cancel async call");

	WMIObjectSink* pSink = NULL;
	for (ObjectSinkVector::iterator i = sinkList.begin(); i != sinkList.end(); i++) {
		jboolean bEquals = pJavaEnv->CallBooleanMethod(
			javaSinkObject, 
			JNIMetaData::GetMetaData(pJavaEnv).javaLangObjectEqualsMethod, 
			(*i)->GetJavaSinkObject());
		if (bEquals == JNI_TRUE) {
			pSink = (*i);
		}
	}
	if (pSink == NULL) {
		THROW_COMMON_EXCEPTION(L"Could not find internal sink for specified object");
	}

	HRESULT hres = ptrWbemServices->CancelAsyncCall(pSink);
	if (FAILED(hres)) {
		THROW_COMMON_ERROR(L"Could not cancel async call", hres);
	}
}

bool WMIService::RemoveObjectSink(JNIEnv* pJavaEnv, WMIObjectSink* pSink)
{
	ObjectSinkVector::iterator i = std::find(sinkList.begin(), sinkList.end(), pSink);
	if (i != sinkList.end()) {
		sinkList.erase(i);
		return true;
	}
	return false;
}

jobject WMIService::MakeWMIObject(JNIEnv* pJavaEnv, IWbemClassObject *pClassObject)
{
	// Create instance
	JNIMetaData& jniMeta = JNIMetaData::GetMetaData(pJavaEnv);
	jobject pWmiObject = pJavaEnv->NewObject(jniMeta.wmiObjectClass, jniMeta.wmiObjectConstructor);
	if (pWmiObject == NULL) {
		this->WriteLog(pJavaEnv, LT_ERROR, L"Can't instantiate WMI java object");
		return NULL;
	}
	WMIObject* pObject = new WMIObject(pJavaEnv, *this, pWmiObject, pClassObject);
/*
	// Fill class object properties
	hres = pClassObject->BeginEnumeration(0);
	if (FAILED(hres)) {
		this->WriteLog(pJavaEnv, LT_ERROR, L"Could not start class object properties enumeration", hres);
		return NULL;
	}

	for (;;) {
		CComBSTR propName;
		CComVariant propValue;
		CIMTYPE cimType; // CIMTYPE_ENUMERATION
		LONG flavor;
		hres = pClassObject->Next(0, &propName, &propValue, &cimType, &flavor);
		if (FAILED(hres)) {
			this->WriteLog(pJavaEnv, LT_ERROR, L"Could not obtain next class object from enumeration", hres);
			break;
		}
		if (hres == WBEM_S_NO_MORE_DATA) {
			break;
		}
		//wchar_t* propNameBSTR = propName;
		jstring javaPropName = MakeJavaString(pJavaEnv, propName);
		_ASSERT(javaPropName != NULL);
		if (javaPropName == NULL) {
			continue;
		}
		jobject javaPropValue = MakeJavaFromVariant(pJavaEnv, propValue, cimType);
		if (!pJavaEnv->ExceptionCheck()) {
			pJavaEnv->CallVoidMethod(pWmiObject, jniMeta.wmiObjectAddPropertyMethod, javaPropName, javaPropValue);
		}
		DeleteLocalRef(pJavaEnv, javaPropName);
		DeleteLocalRef(pJavaEnv, javaPropValue);
		if (pJavaEnv->ExceptionCheck()) {
			break;
		}
	}

	hres = pClassObject->EndEnumeration();
	if (FAILED(hres)) {
		this->WriteLog(pJavaEnv, LT_ERROR, L"Could not finish class object enumeration", hres);
	}
*/
	if (pJavaEnv->ExceptionCheck()) {
		DeleteLocalRef(pJavaEnv, pWmiObject);
		return NULL;
	} else {
		return pWmiObject;
	}
}

/*
JNIEnv* WMIService::AcquireSinkEnv(WMIObjectSink* pSink)
{
	_ASSERT(pJavaVM != NULL);
	if (pJavaVM == NULL) {
		return NULL;
	}
	CComCritSecLock<CComCriticalSection> guard(csSinkThreads);

	DWORD curThreadId = ::GetCurrentThreadId();
	WMIThreadInfo* pCurThread = NULL;
	for (size_t i = 0; i < threadInfos.size(); i++) {
		if (threadInfos[i]->nThreadId == curThreadId) {
			pCurThread = threadInfos[i];
			break;
		}
	}
	if (pCurThread != NULL) {
		ObjectSinkVector::iterator i = std::find(pCurThread->sinks.begin(), pCurThread->sinks.end(), pSink);
		if (i == pCurThread->sinks.end()) {
			pCurThread->sinks.push_back(pSink);
		}
	} else {
		pCurThread = new WMIThreadInfo();
		pCurThread->nThreadId = curThreadId;
		pCurThread->sinks.push_back(pSink);
		pJavaVM->AttachCurrentThread((void**)&pCurThread->pThreadEnv, NULL);
		threadInfos.push_back(pCurThread);
	}

	return pCurThread->pThreadEnv;
}

void WMIService::ReleaseSinkEnv(WMIObjectSink* pSink)
{
	_ASSERT(pJavaVM != NULL);
	if (pJavaVM == NULL) {
		return;
	}
	CComCritSecLock<CComCriticalSection> guard(csSinkThreads);
	// Remove this sink from all threads
	// Detach for current thread only - other ones a dead :(
	DWORD curThreadId = ::GetCurrentThreadId();
	WMIThreadInfo* pCurThread = NULL;


	ThreadInfoVector removeThreads;
	for (ThreadInfoVector::iterator i = threadInfos.begin(); i != threadInfos.end(); i++) {
		ObjectSinkVector::iterator curSink = std::find((*i)->sinks.begin(), (*i)->sinks.end(), pSink);
		if (curSink != (*i)->sinks.end()) {
			(*i)->sinks.erase(curSink);
		}
		if ((*i)->sinks.empty()) {
			if ((*i)->nThreadId == curThreadId) {
				// Detach thread
				pJavaVM->DetachCurrentThread();
			} else {
				// Dunno what to do - we have some thread which is not current
				// Need a way to kill it
				_RPTW0(_CRT_ERROR, L"Can't detach sink thread\n");
			}
			removeThreads.push_back(pCurThread);
		}
	}
	for (ThreadInfoVector::iterator i = removeThreads.begin(); i != removeThreads.end(); i++) {
		removeThreads.erase(
			std::find(threadInfos.begin(), threadInfos.end(), *i));
		delete *i;
	}
}*/

void WMIService::InitStaticState()
{
	csSinkThreads.Init();
}

void WMIService::TermStaticState()
{
	csSinkThreads.Term();
}
