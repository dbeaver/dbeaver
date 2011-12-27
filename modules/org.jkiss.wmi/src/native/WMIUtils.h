
#ifndef _WMI_UTILS
#define _WMI_UTILS

#include <jni.h>

typedef std::vector<jobject> JavaObjectVector;

extern HMODULE hWMIUtils;
extern HMODULE hWbemCommon;

void DeleteLocalRef(JNIEnv *env, jobject object);
jstring MakeJavaString(JNIEnv *env, LPCWSTR string);
void FormatErrorMessage(LPCWSTR message, HRESULT error, BSTR* pBuffer);
void ThrowJavaException(JNIEnv *env, LPCSTR exceptionName, LPCWSTR message, HRESULT error);
void ThrowJavaException(JNIEnv *env, LPCSTR exceptionName, LPCWSTR message);
void GetJavaString(JNIEnv *env, jstring string, BSTR* result);
jlong ConvertCIMTimeToJavaTime(BSTR cimTime);
LONG GetSafeArraySize(SAFEARRAY* pSafeArray);
jlong GetCurrentJavaTime();
jobjectArray MakeJavaArrayFromVector(JNIEnv* pJavaEnv, jclass clazz, const JavaObjectVector& objects);

#define EXCEPTION_WMI_GENERAL ("org/jkiss/wmi/service/WMIException")

#define CHECK_JAVA_EXCEPTION() if (pJavaEnv->ExceptionCheck()) return;
#define CHECK_JAVA_EXCEPTION_NULL() if (pJavaEnv->ExceptionCheck()) return NULL;

#define THROW_COMMON_EXCEPTION(message) ThrowJavaException(pJavaEnv, EXCEPTION_WMI_GENERAL, message)
#define THROW_COMMON_ERROR(message, result) ThrowJavaException(pJavaEnv, EXCEPTION_WMI_GENERAL, message, result)

bstr_t& operator += (bstr_t& str, long arg);

#endif
