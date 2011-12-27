#include <jni.h>

#ifndef _WMI_Object
#define _WMI_Object

#include <jni.h>
#include "WMIService.h"

class WMIObject {
public:
	WMIObject(JNIEnv* pJavaEnv, WMIService& service, jobject javaObject);
	~WMIObject();

	void Release(JNIEnv* pJavaEnv);

	static WMIObject* GetFromObject(JNIEnv* pJavaEnv, jobject javaObject);
private:
	// Private vars
	WMIService& service;
	jobject objectJavaObject;

public:
};

#endif
