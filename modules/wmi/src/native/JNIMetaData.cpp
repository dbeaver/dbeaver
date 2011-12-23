#include "StdAfx.h"
#include "JNIMetaData.h"
#include "WMIUtils.h"

JNIMetaData::JNIMetaData(JNIEnv* pEnv) : pJavaEnv(pEnv)
{
	javaLangObjectClass = FindJavaClass("java/lang/Object");
	javaLangByteClass = FindJavaClass("java/lang/Byte");
	javaLangCharClass = FindJavaClass("java/lang/Character");
	javaLangBooleanClass = FindJavaClass("java/lang/Boolean");
	javaLangShortClass = FindJavaClass("java/lang/Short");
	javaLangIntegerClass = FindJavaClass("java/lang/Integer");
	javaLangLongClass = FindJavaClass("java/lang/Long");
	javaLangFloatClass = FindJavaClass("java/lang/Float");
	javaLangDoubleClass = FindJavaClass("java/lang/Double");
	javaLangStringClass = FindJavaClass("java/lang/String");
	javaUtilDateClass = FindJavaClass("java/util/Date");

	javaLangByteConstructor = FindJavaMethod(javaLangByteClass, "<init>", "(B)V");
	javaLangCharConstructor = FindJavaMethod(javaLangCharClass, "<init>", "(C)V");
	javaLangBooleanConstructor = FindJavaMethod(javaLangBooleanClass, "<init>", "(Z)V");
	javaLangShortConstructor = FindJavaMethod(javaLangShortClass, "<init>", "(S)V");
	javaLangIntegerConstructor = FindJavaMethod(javaLangIntegerClass, "<init>", "(I)V");
	javaLangLongConstructor = FindJavaMethod(javaLangLongClass, "<init>", "(J)V");
	javaLangFloatConstructor = FindJavaMethod(javaLangFloatClass, "<init>", "(F)V");
	javaLangDoubleConstructor = FindJavaMethod(javaLangDoubleClass, "<init>", "(D)V");
	javaUtilDateConstructor = FindJavaMethod(javaUtilDateClass, "<init>", "(J)V");

	javaLangObjectEqualsMethod = FindJavaMethod(javaLangObjectClass, "equals", "(Ljava/lang/Object;)Z");

	wmiServiceClass = FindJavaClass(CLASS_WMI_SERVICE);
	wmiServiceHandleField = pJavaEnv->GetFieldID(wmiServiceClass, "serviceHandle", "J");
	wmiServiceLogField = pJavaEnv->GetFieldID(wmiServiceClass, "serviceLog", "Lorg/apache/commons/logging/Log;");

	wmiObjectClass = FindJavaClass(CLASS_WMI_OBJECT);
	wmiObjectConstructor = FindJavaMethod(
		wmiObjectClass, 
		"<init>", 
		"()V");
	wmiObjectAddPropertyMethod = FindJavaMethod(
		wmiObjectClass, 
		"addProperty", 
		"(Ljava/lang/String;Ljava/lang/Object;)V");

	wmiObjectSinkClass = FindJavaClass(CLASS_WMI_OBJECT_SINK);
	wmiObjectSinkIndicateMethod = FindJavaMethod(
		wmiObjectSinkClass,
		"indicate",
		"([Lcom/symantec/cas/ucf/sensors/wmi/service/WMIObject;)V");
	wmiObjectSinkSetStatusMethod = FindJavaMethod(
		wmiObjectSinkClass,
		"setStatus",
		"(Lcom/symantec/cas/ucf/sensors/wmi/service/WMIObjectSinkStatus;ILjava/lang/String;Lcom/symantec/cas/ucf/sensors/wmi/service/WMIObject;)V");
		 
	wmiObjectSinkStatusClass = FindJavaClass(CLASS_WMI_OBJECT_SINK_STATUS);
}

JNIMetaData::~JNIMetaData(void)
{
	DeleteClassRef(javaLangObjectClass);
	DeleteClassRef(javaLangByteClass);
	DeleteClassRef(javaLangCharClass);
	DeleteClassRef(javaLangBooleanClass);
	DeleteClassRef(javaLangShortClass);
	DeleteClassRef(javaLangIntegerClass);
	DeleteClassRef(javaLangLongClass);
	DeleteClassRef(javaLangFloatClass);
	DeleteClassRef(javaLangDoubleClass);
	DeleteClassRef(javaLangStringClass);
	DeleteClassRef(javaUtilDateClass);
	DeleteClassRef(wmiServiceClass);
	DeleteClassRef(wmiObjectClass);
	DeleteClassRef(wmiObjectSinkClass);
}

jclass JNIMetaData::FindJavaClass(const char* className)
{
	jclass clazz = pJavaEnv->FindClass(className);
	_ASSERT(clazz != NULL);
	if (clazz == NULL) {
		return NULL;
	}
	jclass globalRef = (jclass)pJavaEnv->NewGlobalRef(clazz);
	DeleteLocalRef(pJavaEnv, clazz);
	return globalRef;
}

jmethodID JNIMetaData::FindJavaMethod(jclass clazz, const char* methodName, const char* methodSig)
{
	jmethodID mid = pJavaEnv->GetMethodID(
		clazz,
		methodName,
		methodSig);
	_ASSERT(mid != NULL);
	return mid;
}

void JNIMetaData::DeleteClassRef(jclass& clazz)
{
	if (clazz != NULL) {
		this->pJavaEnv->DeleteGlobalRef(clazz);
		clazz = NULL;
	}
}