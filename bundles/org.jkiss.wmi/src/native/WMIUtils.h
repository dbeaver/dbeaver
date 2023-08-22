
#ifndef _WMI_UTILS
#define _WMI_UTILS

#include <jni.h>

typedef std::vector<jobject> JavaObjectVector;

enum JavaType {
	JT_CHAR,
	JT_BYTE,
	JT_BOOL,
	JT_SHORT,
	JT_INT,
	JT_LONG,
	JT_FLOAT,
	JT_DOUBLE,
	JT_DATE,
	JT_STRING,
	JT_ARRAY
};

extern HMODULE hWMIUtils;
extern HMODULE hWbemCommon;

bool WMIInitializeThread(JNIEnv* pJavaEnv);
void WMIUnInitializeThread();


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

jobject MakeJavaFromVariant(JNIEnv* pJavaEnv, CComVariant& var, CIMTYPE cimType = CIM_ILLEGAL);

jbyteArray MakeJavaByteArrayFromSafeArray(JNIEnv* pJavaEnv, SAFEARRAY* pSafeArray);
jbooleanArray MakeJavaBoolArrayFromSafeArray(JNIEnv* pJavaEnv, SAFEARRAY* pSafeArray);
jshortArray MakeJavaShortArrayFromSafeArray(JNIEnv* pJavaEnv, SAFEARRAY* pSafeArray);
jintArray MakeJavaIntArrayFromSafeArray(JNIEnv* pJavaEnv, SAFEARRAY* pSafeArray);
jlongArray MakeJavaLongArrayFromSafeArray(JNIEnv* pJavaEnv, SAFEARRAY* pSafeArray);
jfloatArray MakeJavaFloatArrayFromSafeArray(JNIEnv* pJavaEnv, SAFEARRAY* pSafeArray);
jdoubleArray MakeJavaDoubleArrayFromSafeArray(JNIEnv* pJavaEnv, SAFEARRAY* pSafeArray);
jobjectArray MakeJavaObjectArrayFromSafeVector(JNIEnv* pJavaEnv, SAFEARRAY* pSafeArray, JavaType elementType, jclass arrayClass);


#define EXCEPTION_WMI_GENERAL ("org/jkiss/wmi/service/WMIException")

#define CHECK_JAVA_EXCEPTION() if (pJavaEnv->ExceptionCheck()) return;
#define CHECK_JAVA_EXCEPTION_NULL() if (pJavaEnv->ExceptionCheck()) return NULL;

#define THROW_COMMON_EXCEPTION(message) ThrowJavaException(pJavaEnv, EXCEPTION_WMI_GENERAL, message)
#define THROW_COMMON_ERROR(message, result) ThrowJavaException(pJavaEnv, EXCEPTION_WMI_GENERAL, message, result)

bstr_t& operator += (bstr_t& str, long arg);

#endif
