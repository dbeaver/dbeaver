#include "StdAfx.h"
#include "JNIMetaData.h"
#include "WMIUtils.h"
#include <map>

//static CComCriticalSection csSinkThreads;

//typedef std::map<JNIEnv*, JNIMetaData*> MetaDataMap;
//static MetaDataMap s_MetaDataMap;

JNIMetaData* JNIMetaData::instance = NULL;

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
	javaUtilListClass = FindJavaClass("java/util/List");

	javaLangByteConstructor = FindJavaMethod(javaLangByteClass, "<init>", "(B)V");
	javaLangCharConstructor = FindJavaMethod(javaLangCharClass, "<init>", "(C)V");
	javaLangBooleanConstructor = FindJavaMethod(javaLangBooleanClass, "<init>", "(Z)V");
	javaLangShortConstructor = FindJavaMethod(javaLangShortClass, "<init>", "(S)V");
	javaLangIntegerConstructor = FindJavaMethod(javaLangIntegerClass, "<init>", "(I)V");
	javaLangLongConstructor = FindJavaMethod(javaLangLongClass, "<init>", "(J)V");
	javaLangFloatConstructor = FindJavaMethod(javaLangFloatClass, "<init>", "(F)V");
	javaLangDoubleConstructor = FindJavaMethod(javaLangDoubleClass, "<init>", "(D)V");
	javaUtilDateConstructor = FindJavaMethod(javaUtilDateClass, "<init>", "(J)V");
	javaUtilListAddMethod = FindJavaMethod(javaUtilListClass, "add", "(Ljava/lang/Object;)Z");
	javaLangObjectEqualsMethod = FindJavaMethod(javaLangObjectClass, "equals", "(Ljava/lang/Object;)Z");

	wmiServiceClass = FindJavaClass(CLASS_WMI_SERVICE);
	wmiServiceConstructor = FindJavaMethod(
		wmiServiceClass, 
		"<init>", 
		"()V");
	wmiServiceHandleField = pJavaEnv->GetFieldID(wmiServiceClass, "serviceHandle", "J");

	wmiObjectClass = FindJavaClass(CLASS_WMI_OBJECT);
	wmiObjectConstructor = FindJavaMethod(
		wmiObjectClass, 
		"<init>", 
		"()V");
	wmiObjectHandleField = pJavaEnv->GetFieldID(wmiObjectClass, "objectHandle", "J");

	wmiQualifierClass = FindJavaClass(CLASS_WMI_QUALIFIER);
	wmiQualifierConstructor = FindJavaMethod(
		wmiQualifierClass, 
		"<init>", 
		"(Ljava/lang/String;ILjava/lang/Object;)V");

	wmiObjectSinkClass = FindJavaClass(CLASS_WMI_OBJECT_SINK);
	wmiObjectSinkIndicateMethod = FindJavaMethod(
		wmiObjectSinkClass,
		"indicate",
		"([Lorg/jkiss/wmi/service/WMIObject;)V");
	wmiObjectSinkSetStatusMethod = FindJavaMethod(
		wmiObjectSinkClass,
		"setStatus",
		"(Lorg/jkiss/wmi/service/WMIObjectSinkStatus;ILjava/lang/String;Lorg/jkiss/wmi/service/WMIObject;)V");
		 
	wmiObjectSinkStatusClass = FindJavaClass(CLASS_WMI_OBJECT_SINK_STATUS);

	wmiObjectAttributeClass = FindJavaClass(CLASS_WMI_OBJECT_ATTRIBUTE);
	wmiObjectAttributeConstructor = FindJavaMethod(
		wmiObjectAttributeClass, 
		"<init>", 
		"(Lorg/jkiss/wmi/service/WMIObject;Ljava/lang/String;IILjava/lang/Object;)V");

	wmiObjectMethodClass = FindJavaClass(CLASS_WMI_OBJECT_METHOD);
	wmiObjectMethodConstructor = FindJavaMethod(
		wmiObjectMethodClass, 
		"<init>", 
		"(Lorg/jkiss/wmi/service/WMIObject;Ljava/lang/String;Lorg/jkiss/wmi/service/WMIObject;Lorg/jkiss/wmi/service/WMIObject;)V");
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
	DeleteClassRef(javaUtilListClass);

	DeleteClassRef(wmiServiceClass);
	DeleteClassRef(wmiObjectClass);
	DeleteClassRef(wmiQualifierClass);
	DeleteClassRef(wmiObjectSinkClass);
	DeleteClassRef(wmiObjectSinkStatusClass);
	DeleteClassRef(wmiObjectAttributeClass);
	DeleteClassRef(wmiObjectMethodClass);
}

JNIMetaData& JNIMetaData::GetMetaData(JNIEnv* pEnv)
{
/*
	JNIMetaData* pMetaData = s_MetaDataMap[pEnv];
	if (pMetaData == NULL) {
		pMetaData = new JNIMetaData(pEnv);
		s_MetaDataMap[pEnv] = pMetaData;
	}
	return *pMetaData;
*/
	if (instance == NULL) {
		instance = new JNIMetaData(pEnv);
	}
	return *instance;
}
/*
void JNIMetaData::Destroy()
{
	for (MetaDataMap::iterator iter = s_MetaDataMap.begin(); iter != s_MetaDataMap.end(); iter++) {
		delete iter->second;
	}
	s_MetaDataMap.clear();
}
*/
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
	if (clazz == NULL) {
		return NULL;
	}
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