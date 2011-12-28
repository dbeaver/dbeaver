#include <jni.h>

#ifndef _WMI_Object
#define _WMI_Object

#include <jni.h>
#include "WMIService.h"

class WMIObject {
public:
	WMIObject(JNIEnv* pJavaEnv, WMIService& service, jobject javaObject, IWbemClassObject* pClassObject);
	~WMIObject();

	void Release(JNIEnv* pJavaEnv, jobject javaObject);
	jstring GetObjectText(JNIEnv* pJavaEnv);
	jobject GetPropertyValue(JNIEnv* pJavaEnv, jstring propName);
	void ReadProperties(JNIEnv* pJavaEnv, jobject javaObject, jobject propList);

	static WMIObject* GetFromObject(JNIEnv* pJavaEnv, jobject javaObject);
private:
	// Private vars
	CComPtr<IWbemClassObject> ptrClassObject;

public:
};

#endif
