#include <jni.h>

#ifndef _WMI_Object
#define _WMI_Object

#include <jni.h>
#include "WMIService.h"

class WMIObject {
public:
	WMIObject(JNIEnv* pJavaEnv, jobject javaObject, IWbemClassObject* pClassObject);
	~WMIObject();

	void Release(JNIEnv* pJavaEnv, jobject javaObject);
	jstring GetObjectText(JNIEnv* pJavaEnv);
	jobject GetAttributeValue(JNIEnv* pJavaEnv, jstring propName);
	void ReadAttributes(JNIEnv* pJavaEnv, jobject javaObject, LONG lFlags, jobject propList);
	void ReadMethods(JNIEnv* pJavaEnv, jobject javaObject, LONG lFlags, jobject methodList);
	void ReadQualifiers(JNIEnv* pJavaEnv, jobject javaObject, bool isAttribute, jstring propName, jobject qfList);

	static WMIObject* GetFromObject(JNIEnv* pJavaEnv, jobject javaObject);
private:
	// Private vars
	CComPtr<IWbemClassObject> ptrClassObject;

public:
};

#endif
