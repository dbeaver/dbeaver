// WMISensor.cpp : Defines the entry point for the DLL application.
//

#include "stdafx.h"
#include "WMIUtils.h"

HMODULE hWMIUtils;
HMODULE hWbemCommon;

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
	int tz;
	int result = ::swscanf_s(cimTime, L"%04d%02d%02d%02d%02d%02d.000000%c%03d", 
		&resultTime.tm_year, &resultTime.tm_mon, &resultTime.tm_mday, &resultTime.tm_hour, &resultTime.tm_min, &resultTime.tm_sec, &tz);
	resultTime.tm_year -= 1900;
	resultTime.tm_mon--;
	return mktime(&resultTime) * 1000;
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

bstr_t& operator += (bstr_t& str, long arg)
{
	wchar_t buf[20];
	_ltow_s(arg, buf, 20, 10);
	str += buf;
	return str;
}

int main(char** args)
{
   int const x = 100;
   int* y;
   int const** z = &y;
   *z = &x;
   *y = 200;

   int c = 234;
}