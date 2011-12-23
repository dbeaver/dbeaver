#include <jni.h>

#ifndef _WMI_Service
#define _WMI_Service

#include <jni.h>
#include "JNIMetaData.h"


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

enum LogType {
	LT_TRACE,
	LT_DEBUG,
	LT_INFO,
	LT_WARN,
	LT_ERROR,
	LT_FATAL,
};

class WMIObjectSink;
typedef std::vector< CComPtr<WMIObjectSink> > ObjectSinkVector;


class WMIService {
public:
	WMIService(JNIEnv* pJavaEnv, jobject javaObject);
	~WMIService();

	void Close(JNIEnv* pJavaEnv);

	void Connect(
		JNIEnv* pJavaEnv,
		LPWSTR domain, 
		LPWSTR host, 
		LPWSTR user, 
		LPWSTR password,
		LPWSTR locale);

	jobjectArray ExecuteQuery(JNIEnv* pJavaEnv, LPWSTR query, bool sync);

	void ExecuteQueryAsync(JNIEnv* pJavaEnv, LPWSTR query, jobject javaSinkObject, bool sendStatus);
	void CancelAsyncOperation(JNIEnv* pJavaEnv, jobject javaSinkObject);

	void WriteLog(JNIEnv* pLocalEnv, LogType logType, LPCWSTR wcMessage, HRESULT hr = S_OK);

	static WMIService* GetFromObject(JNIEnv* pJavaEnv, jobject javaObject);

public:
	const JNIMetaData& GetJNIMeta() { return jniMeta; }

	jobject MakeWMIObject (JNIEnv* pJavaEnv, IWbemClassObject *pClassObject);
	bool RemoveObjectSink(JNIEnv* pJavaEnv, WMIObjectSink* pSink);

private:

	jobject MakeJavaFromVariant(JNIEnv* pJavaEnv, CComVariant& var, CIMTYPE cimType = CIM_ILLEGAL);

	jbyteArray MakeJavaByteArrayFromSafeArray(JNIEnv* pJavaEnv, SAFEARRAY* pSafeArray);
	jbooleanArray MakeJavaBoolArrayFromSafeArray(JNIEnv* pJavaEnv, SAFEARRAY* pSafeArray);
	jshortArray MakeJavaShortArrayFromSafeArray(JNIEnv* pJavaEnv, SAFEARRAY* pSafeArray);
	jintArray MakeJavaIntArrayFromSafeArray(JNIEnv* pJavaEnv, SAFEARRAY* pSafeArray);
	jlongArray MakeJavaLongArrayFromSafeArray(JNIEnv* pJavaEnv, SAFEARRAY* pSafeArray);
	jfloatArray MakeJavaFloatArrayFromSafeArray(JNIEnv* pJavaEnv, SAFEARRAY* pSafeArray);
	jdoubleArray MakeJavaDoubleArrayFromSafeArray(JNIEnv* pJavaEnv, SAFEARRAY* pSafeArray);
	jobjectArray MakeJavaObjectArrayFromSafeVector(JNIEnv* pJavaEnv, SAFEARRAY* pSafeArray, JavaType elementType, jclass arrayClass);

private:
	// Private vars
	jobject serviceJavaObject;

	IWbemLocator *pWbemLocator;
	IWbemServices *pWbemServices;

	JNIMetaData jniMeta;

	ObjectSinkVector sinkList;
	static JavaVM* pJavaVM;

public:
/*
	static JNIEnv* AcquireSinkEnv(WMIObjectSink* pSink);
	static void ReleaseSinkEnv(WMIObjectSink* pSink);
	static void RemoveSink(WMIObjectSink* pSink);
*/

	static JavaVM* GetJavaVM() { return pJavaVM; }
	static void InitStaticState();
	static void TermStaticState();
};

#endif
