// WMISensor.cpp : Defines the entry point for the DLL application.
//

#include "stdafx.h"
#include "WMIUtils.h"
#include "JNIMetaData.h"

HMODULE hWMIUtils;
HMODULE hWbemCommon;

bool WMIInitializeThread(JNIEnv* pJavaEnv)
{
	HRESULT hres =  ::CoInitializeEx(0, COINIT_MULTITHREADED);
//	if (hres == RPC_E_CHANGED_MODE) {
//		hres =  ::CoInitialize(0); 
//	}
    if (FAILED(hres)) {
		THROW_COMMON_ERROR(L"Failed to initialize COM library", hres);
		return false;
	}
//	if (hres == S_FALSE) {
//		// Already initialized
//		return true;
//	}

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
        return false;
    }
	return true;
}


void WMIUnInitializeThread()
{
	::CoUninitialize();
}



void DeleteLocalRef(JNIEnv *env, jobject object)
{
	if (object != NULL) {
		env->DeleteLocalRef(object);
	}
}

jstring MakeJavaString(JNIEnv *env, LPCWSTR string)
{
	return env->NewString((const jchar*)string, (jsize)::wcslen(string));
}

void FormatErrorMessage(LPCWSTR message, HRESULT error, BSTR* pBuffer)
{
	if(error == NO_ERROR) {
		error = GetLastError();
	}
	_bstr_t finalMessage = message;
	finalMessage += L" - ";
	{
		TCHAR systemMessage[1024];
		// Get system message for last error code
		DWORD count = ::FormatMessage(  
			FORMAT_MESSAGE_FROM_SYSTEM, 
			NULL, 
			error, 
			MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), 
			systemMessage, 
			sizeof(systemMessage) / sizeof(TCHAR) - 1, 
			NULL 
		);
		if (count <= 0) {
			// Try to get WMI error
			if (hWMIUtils != NULL || hWbemCommon != NULL) {
				count = ::FormatMessage(  
					FORMAT_MESSAGE_FROM_HMODULE, 
					hWMIUtils != NULL ? hWMIUtils : hWbemCommon,
					error, 
					MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), 
					systemMessage, 
					sizeof(systemMessage) / sizeof(TCHAR) - 1, 
					NULL 
				);
			}
		}
		if (count > 0) {
			finalMessage += systemMessage;
		} else {
			finalMessage += _T("Unknown Error");
		}
	}

	{
		char buffer[32];
		::_ltoa_s(error, buffer, 16);

		finalMessage += _T(" [0x");
		finalMessage += buffer;
		finalMessage += _T("]");
	}
	for (wchar_t* pos = finalMessage; *pos != NULL; pos++) {
		if (*pos == '\r' || *pos == '\n') {
			*pos = ' ';
		}
	}
	*pBuffer = finalMessage.Detach();
}

// Get last API error
void ThrowJavaException(JNIEnv *env, LPCSTR exceptionName, LPCWSTR message, HRESULT error) { 
	
	CComBSTR bstrMessage;
	FormatErrorMessage(message, error, &bstrMessage);
	ThrowJavaException(env, exceptionName, bstrMessage);
}

void ThrowJavaException(JNIEnv *env, LPCSTR exceptionName, LPCWSTR message) {
	jclass exceptionClass = env->FindClass(exceptionName);
	_ASSERT(exceptionClass != NULL);
	if (exceptionClass != NULL) {
		jmethodID cid = env->GetMethodID(exceptionClass, "<init>", "(Ljava/lang/String;)V");
		if (cid != NULL) {
			jthrowable exceptionObject = (jthrowable)env->NewObject(exceptionClass, cid, MakeJavaString(env, message));
			env->Throw(exceptionObject);
		}
		DeleteLocalRef(env, exceptionClass);
	}
}

void GetJavaString(JNIEnv *env, jstring string, BSTR* result)
{
	if (string == NULL) {
		*result = NULL;
		return;
	}
	const jchar *stringBytes = env->GetStringChars(string, NULL);
	*result = CComBSTR(env->GetStringLength(string), (LPCWSTR)stringBytes).Detach();
	env->ReleaseStringChars(string, stringBytes);
}

jlong ConvertCIMTimeToJavaTime(BSTR cimTime)
{
	struct tm resultTime;
	int microSecs = 0;
	int tz = 0;
	int result = ::swscanf_s(cimTime, L"%04d%02d%02d%02d%02d%02d.%06d%d", 
		&resultTime.tm_year, 
		&resultTime.tm_mon, 
		&resultTime.tm_mday, 
		&resultTime.tm_hour, 
		&resultTime.tm_min, 
		&resultTime.tm_sec, 
		&microSecs, 
		&tz);
	if (result != 8) {
		// Invalid CIM time
		return 0;
	}
	resultTime.tm_year -= 1900;
	resultTime.tm_mon--;
	time_t timeInSeconds = mktime(&resultTime);
	return timeInSeconds * 1000 + (microSecs / 1000);
}

LONG GetSafeArraySize(SAFEARRAY* pSafeArray)
{
	long lBound, uBound;
	::SafeArrayGetUBound(pSafeArray, 1, &uBound);
	::SafeArrayGetLBound(pSafeArray, 1, &lBound);
	return uBound >= lBound ? uBound - lBound + 1: 0;
}

jlong GetCurrentJavaTime()
{
	FILETIME fileTime;
	::GetSystemTimeAsFileTime(&fileTime);
	jlong javaTime = ((jlong)fileTime.dwHighDateTime << 32) + fileTime.dwLowDateTime;
	return javaTime / 10000;
}

jobjectArray MakeJavaArrayFromVector(JNIEnv* pJavaEnv, jclass clazz, const JavaObjectVector& objects)
{
	jobjectArray result = pJavaEnv->NewObjectArray((jsize)objects.size(), clazz, NULL);
	for (size_t index = 0; index < objects.size(); index++) {
		pJavaEnv->SetObjectArrayElement(result, (jsize)index, objects[index]);
		DeleteLocalRef(pJavaEnv, objects[index]);
	}
	return result;
}

jobject MakeJavaFromVariant(JNIEnv* pJavaEnv, CComVariant& var, CIMTYPE cimType)
{
	JNIMetaData& jniMeta = JNIMetaData::GetMetaData(pJavaEnv);
	JavaType javaType;

	VARTYPE varType = var.vt;
	bool isArray = (varType & VT_ARRAY) != 0;
	if (isArray) {
		varType &= ~VT_ARRAY;
	}

	switch (varType) {
	case VT_EMPTY:
	case VT_NULL:
	case VT_VOID:
		// Null value
		return NULL;

	case VT_I1:
	case VT_UI1:
		javaType = JT_BYTE;
		break;
	case VT_I2:
	case VT_UI2:
		javaType = JT_SHORT;
		break;
	case VT_I4: 
	case VT_UI4:
	case VT_INT:
	case VT_UINT:
		javaType = JT_INT;
		break;
	case VT_I8:
	case VT_UI8:
		javaType = JT_LONG;
		break;
	case VT_R4: 
		javaType = JT_FLOAT;
		break;
	case VT_R8: 
		javaType = JT_DOUBLE;
		break;
	case VT_DATE: 
		javaType = JT_DATE;
		break;
	case VT_BOOL: 
		javaType = JT_BOOL;
		break;
	case VT_BSTR: 
		javaType = JT_STRING;
		break;

	case VT_ARRAY:
	case VT_SAFEARRAY:
	case VT_DECIMAL:
	case VT_VARIANT:
	default:
		// Unsupported type
		THROW_COMMON_EXCEPTION(L"Unsupported VARIANT type");
		return NULL;
	}

	if (isArray) {
		// Array
		// There two kinds of arrays - primitive and object
		switch (javaType) {
		case JT_BYTE:
			return MakeJavaByteArrayFromSafeArray(pJavaEnv, var.parray);
		case JT_BOOL:
			return MakeJavaBoolArrayFromSafeArray(pJavaEnv, var.parray);
		case JT_SHORT:
			return MakeJavaShortArrayFromSafeArray(pJavaEnv, var.parray);
		case JT_INT:
			return MakeJavaIntArrayFromSafeArray(pJavaEnv, var.parray);
		case JT_LONG:
			return MakeJavaLongArrayFromSafeArray(pJavaEnv, var.parray);
		case JT_FLOAT:
			return MakeJavaFloatArrayFromSafeArray(pJavaEnv, var.parray);
		case JT_DOUBLE:
			return MakeJavaDoubleArrayFromSafeArray(pJavaEnv, var.parray);
		default:
			{
				jclass elementClass;
				switch (javaType) {
					case JT_STRING: elementClass = jniMeta.javaLangStringClass; break;
					default: elementClass = jniMeta.javaLangObjectClass; break;
				}
				return MakeJavaObjectArrayFromSafeVector(pJavaEnv, var.parray, javaType, elementClass);
			}
		}
	} else {
		// Single value
		switch (javaType) {
		case JT_CHAR:
			return pJavaEnv->NewObject(jniMeta.javaLangCharClass, jniMeta.javaLangCharConstructor, var.cVal);
		case JT_BYTE:
			return pJavaEnv->NewObject(jniMeta.javaLangByteClass, jniMeta.javaLangByteConstructor, var.cVal);
		case JT_BOOL:
			return pJavaEnv->NewObject(jniMeta.javaLangBooleanClass, jniMeta.javaLangBooleanConstructor, var.boolVal == VARIANT_TRUE ? JNI_TRUE : JNI_FALSE);
		case JT_SHORT:
			return pJavaEnv->NewObject(jniMeta.javaLangShortClass, jniMeta.javaLangShortConstructor, var.iVal);
		case JT_INT:
			return pJavaEnv->NewObject(jniMeta.javaLangIntegerClass, jniMeta.javaLangIntegerConstructor, var.lVal);
		case JT_LONG:
			return pJavaEnv->NewObject(jniMeta.javaLangLongClass, jniMeta.javaLangLongConstructor, var.llVal);
		case JT_FLOAT:
			return pJavaEnv->NewObject(jniMeta.javaLangFloatClass, jniMeta.javaLangFloatConstructor, var.fltVal);
		case JT_DOUBLE:
			return pJavaEnv->NewObject(jniMeta.javaLangDoubleClass, jniMeta.javaLangDoubleConstructor, var.dblVal);
		case JT_DATE:
			// TODO: correct date value
			return pJavaEnv->NewObject(jniMeta.javaUtilDateClass, jniMeta.javaUtilDateConstructor, (jlong)var.date);
		case JT_STRING:
			if (cimType == CIM_DATETIME) {
				javaType = JT_DATE;
				return pJavaEnv->NewObject(jniMeta.javaUtilDateClass, jniMeta.javaUtilDateConstructor, ConvertCIMTimeToJavaTime(var.bstrVal));
			} else {
				return MakeJavaString(pJavaEnv, var.bstrVal);
			}
		default:
			// Unsupported type
			THROW_COMMON_EXCEPTION(L"Unsupported Java type");
			return NULL;
		}
	}
}

jbyteArray MakeJavaByteArrayFromSafeArray(JNIEnv* pJavaEnv, SAFEARRAY* pSafeArray)
{
	jsize arraySize = ::GetSafeArraySize(pSafeArray);
	jbyteArray result = pJavaEnv->NewByteArray(arraySize);
	jbyte HUGEP * byteArray;
	HRESULT hr = ::SafeArrayAccessData(pSafeArray, (void HUGEP* FAR*)&byteArray);
	if (FAILED(hr)) {
		THROW_COMMON_ERROR(L"Can't access safe array byte data", hr);
		return NULL;
	}
	pJavaEnv->SetByteArrayRegion(result, 0, arraySize, byteArray);
	::SafeArrayUnaccessData(pSafeArray);
	return result;
}

jbooleanArray MakeJavaBoolArrayFromSafeArray(JNIEnv* pJavaEnv, SAFEARRAY* pSafeArray)
{
	jsize arraySize = ::GetSafeArraySize(pSafeArray);
	jbooleanArray result = pJavaEnv->NewBooleanArray(arraySize);
	jboolean HUGEP * boolArray;
	HRESULT hr = ::SafeArrayAccessData(pSafeArray, (void HUGEP* FAR*)&boolArray);
	if (FAILED(hr)) {
		THROW_COMMON_ERROR(L"Can't access safe array bool data", hr);
		return NULL;
	}
	pJavaEnv->SetBooleanArrayRegion(result, 0, arraySize, boolArray);
	::SafeArrayUnaccessData(pSafeArray);
	return result;
}

jshortArray MakeJavaShortArrayFromSafeArray(JNIEnv* pJavaEnv, SAFEARRAY* pSafeArray)
{
	THROW_COMMON_EXCEPTION(L"Short arrays not implemented");
	return NULL;
}

jintArray MakeJavaIntArrayFromSafeArray(JNIEnv* pJavaEnv, SAFEARRAY* pSafeArray)
{
	jsize arraySize = ::GetSafeArraySize(pSafeArray);
	jintArray result = pJavaEnv->NewIntArray(arraySize);
	jint HUGEP * intArray;
	HRESULT hr = ::SafeArrayAccessData(pSafeArray, (void HUGEP* FAR*)&intArray);
	if (FAILED(hr)) {
		THROW_COMMON_ERROR(L"Can't access safe array int data", hr);
		return NULL;
	}
	pJavaEnv->SetIntArrayRegion(result, 0, arraySize, intArray);
	::SafeArrayUnaccessData(pSafeArray);
	return result;
}

jlongArray MakeJavaLongArrayFromSafeArray(JNIEnv* pJavaEnv, SAFEARRAY* pSafeArray)
{
	THROW_COMMON_EXCEPTION(L"Long arrays not implemented");
	return NULL;
}

jfloatArray MakeJavaFloatArrayFromSafeArray(JNIEnv* pJavaEnv, SAFEARRAY* pSafeArray)
{
	THROW_COMMON_EXCEPTION(L"Float arrays not implemented");
	return NULL;
}

jdoubleArray MakeJavaDoubleArrayFromSafeArray(JNIEnv* pJavaEnv, SAFEARRAY* pSafeArray)
{
	THROW_COMMON_EXCEPTION(L"Double arrays not implemented");
	return NULL;
}

jobjectArray MakeJavaObjectArrayFromSafeVector(JNIEnv* pJavaEnv, SAFEARRAY* pSafeArray, JavaType elementType, jclass arrayClass)
{
	jsize arraySize = ::GetSafeArraySize(pSafeArray);
	jobjectArray result = pJavaEnv->NewObjectArray(arraySize, arrayClass, NULL);

	if (elementType == JT_STRING) {
		BSTR HUGEP * pStrings;
		HRESULT hr = ::SafeArrayAccessData(pSafeArray, (void HUGEP* FAR*)&pStrings);
		if (FAILED(hr)) {
			THROW_COMMON_ERROR(L"Can't access safe array strings data", hr);
			return NULL;
		}
		for (int i = 0; i < arraySize; i++) {
			jstring arrString = MakeJavaString(pJavaEnv, pStrings[i]);
			pJavaEnv->SetObjectArrayElement(result, i, arrString);
			DeleteLocalRef(pJavaEnv, arrString);
		}
		::SafeArrayUnaccessData(pSafeArray);
	} else {
		// Unsupported type
		THROW_COMMON_EXCEPTION(L"Unsupported object type of safe array");
	}

	return result;
}

bstr_t& operator += (bstr_t& str, long arg)
{
	wchar_t buf[20];
	_ltow_s(arg, buf, 20, 10);
	str += buf;
	return str;
}
