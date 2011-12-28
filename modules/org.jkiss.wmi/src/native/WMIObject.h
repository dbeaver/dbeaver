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

	static WMIObject* GetFromObject(JNIEnv* pJavaEnv, jobject javaObject);
private:
	// Private vars
	WMIService& service;
	CComPtr<IWbemClassObject> ptrClassObject;

public:
};

#endif
