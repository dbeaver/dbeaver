#include <jni.h>

#ifndef _WMI_Service
#define _WMI_Service

#include <jni.h>
#include "JNIMetaData.h"


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

	void Release(JNIEnv* pJavaEnv);

	void Connect(
		JNIEnv* pJavaEnv,
		LPWSTR domain, 
		LPWSTR host, 
		LPWSTR user, 
		LPWSTR password,
		LPWSTR locale,
		LPWSTR resource);

	//jobjectArray ExecuteQuery(JNIEnv* pJavaEnv, LPWSTR query, bool sync);
	jobject OpenNamespace(JNIEnv* pJavaEnv, LPWSTR nsName, LONG lFlags);
	void ExecuteQueryAsync(JNIEnv* pJavaEnv, LPWSTR query, jobject javaSinkObject, LONG lFlags);
	void EnumClasses(JNIEnv* pJavaEnv, LPWSTR superClass, jobject javaSinkObject, LONG lFlags);
	void EnumInstances(JNIEnv* pJavaEnv, LPWSTR className, jobject javaSinkObject, LONG lFlags);
	void CancelAsyncOperation(JNIEnv* pJavaEnv, jobject javaSinkObject);

	//void WriteLog(JNIEnv* pLocalEnv, LogType logType, LPCWSTR wcMessage, HRESULT hr = S_OK);

	static WMIService* GetFromObject(JNIEnv* pJavaEnv, jobject javaObject);

public:
	static jobject MakeWMIObject (JNIEnv* pJavaEnv, IWbemClassObject *pClassObject);
	bool RemoveObjectSink(JNIEnv* pJavaEnv, WMIObjectSink* pSink);

private:
	void MakeObjectSink(JNIEnv* pJavaEnv, jobject javaSinkObject, IWbemObjectSink** ppSink);

private:
	// Private vars
	jobject serviceJavaObject;

	//CComPtr<IWbemLocator> ptrWbemLocator;
	CComPtr<IWbemServices> ptrWbemServices;

	ObjectSinkVector sinkList;
	static JavaVM* pJavaVM;

public:
	static JavaVM* GetJavaVM() { return pJavaVM; }
	static void InitStaticState();
	static void TermStaticState();
};

#endif
