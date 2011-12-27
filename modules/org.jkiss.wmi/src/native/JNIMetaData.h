#pragma once

#ifndef _JNI_META_DATA_H_
#define _JNI_META_DATA_H_

#include <jni.h>

static const char* CLASS_WMI_SERVICE = "com/symantec/cas/ucf/sensors/wmi/service/WMIService";
static const char* CLASS_WMI_OBJECT = "com/symantec/cas/ucf/sensors/wmi/service/WMIObject";
static const char* CLASS_WMI_OBJECT_SINK = "com/symantec/cas/ucf/sensors/wmi/service/WMIObjectSink";
static const char* CLASS_WMI_OBJECT_SINK_STATUS = "com/symantec/cas/ucf/sensors/wmi/service/WMIObjectSinkStatus";

class JNIMetaData
{
public:
	JNIMetaData(JNIEnv* pEnv);
	~JNIMetaData();

	JNIEnv* pJavaEnv;

	jclass wmiServiceClass;
	jfieldID wmiServiceHandleField;
	jfieldID wmiServiceLogField;

	jclass wmiObjectClass;
	jmethodID wmiObjectConstructor;
	jfieldID wmiObjectHandleField;

	jclass wmiObjectSinkClass;
	jmethodID wmiObjectSinkIndicateMethod;
	jmethodID wmiObjectSinkSetStatusMethod;

	jclass wmiObjectSinkStatusClass;

	jclass javaLangObjectClass;
	jclass javaLangByteClass;
	jclass javaLangCharClass;
	jclass javaLangBooleanClass;
	jclass javaLangShortClass;
	jclass javaLangIntegerClass;
	jclass javaLangLongClass;
	jclass javaLangFloatClass;
	jclass javaLangDoubleClass;
	jclass javaLangStringClass;
	jclass javaUtilDateClass;

	jmethodID javaLangByteConstructor;
	jmethodID javaLangCharConstructor;
	jmethodID javaLangBooleanConstructor;
	jmethodID javaLangShortConstructor;
	jmethodID javaLangIntegerConstructor;
	jmethodID javaLangLongConstructor;
	jmethodID javaLangFloatConstructor;
	jmethodID javaLangDoubleConstructor;
	jmethodID javaUtilDateConstructor;

	jmethodID javaLangObjectEqualsMethod;

private:
	jclass FindJavaClass(const char* className);
	jmethodID FindJavaMethod(jclass clazz, const char* methodName, const char* methodSig);
	void DeleteClassRef(jclass& clazz);
};

#endif