// WMISensor.cpp : Defines the entry point for the DLL application.
//

#include "stdafx.h"
#include "WMIObjectJNI.h"
#include "WMIObject.h"
#include "WMIUtils.h"

static const wchar_t* ERROR_NOT_INITIALIZED = L"WMI object was not initialized or was disposed";

/*
 * Class:     org_jkiss_wmi_service_WMIObject
 * Method:    readObjectText
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_jkiss_wmi_service_WMIObject_readObjectText(JNIEnv* pJavaEnv, jobject object)
{
	WMIObject* pObject = WMIObject::GetFromObject(pJavaEnv, object);
	if (pObject == NULL) {
		THROW_COMMON_EXCEPTION(ERROR_NOT_INITIALIZED);
		return NULL;
	}
	return pObject->GetObjectText(pJavaEnv);
}

/*
 * Class:     org_jkiss_wmi_service_WMIObject
 * Method:    readPropertyValue
 * Signature: (Ljava/lang/String;)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_org_jkiss_wmi_service_WMIObject_readPropertyValue(JNIEnv* pJavaEnv, jobject object, jstring propName)
{
	WMIObject* pObject = WMIObject::GetFromObject(pJavaEnv, object);
	if (pObject == NULL) {
		THROW_COMMON_EXCEPTION(ERROR_NOT_INITIALIZED);
		return NULL;
	}
	return pObject->GetPropertyValue(pJavaEnv, propName);
}

/*
 * Class:     org_jkiss_wmi_service_WMIObject
 * Method:    writePropertyValue
 * Signature: (Ljava/lang/String;Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_org_jkiss_wmi_service_WMIObject_writePropertyValue(JNIEnv* pJavaEnv, jobject object, jstring propName, jobject propValue)
{
	WMIObject* pObject = WMIObject::GetFromObject(pJavaEnv, object);
	if (pObject == NULL) {
		THROW_COMMON_EXCEPTION(ERROR_NOT_INITIALIZED);
		return;
	}

	return;
}

/*
 * Class:     org_jkiss_wmi_service_WMIObject
 * Method:    readProperties
 * Signature: (Ljava/util/List;)Ljava/util/List;
 */
JNIEXPORT void JNICALL Java_org_jkiss_wmi_service_WMIObject_readProperties(JNIEnv* pJavaEnv, jobject object, jobject propList)
{
	WMIObject* pObject = WMIObject::GetFromObject(pJavaEnv, object);
	if (pObject == NULL) {
		THROW_COMMON_EXCEPTION(ERROR_NOT_INITIALIZED);
		return;
	}

	pObject->ReadProperties(pJavaEnv, object, propList);
}

/*
 * Class:     org_jkiss_wmi_service_WMIObject
 * Method:    readMethod
 * Signature: (Ljava/util/List;)Ljava/util/List;
 */
JNIEXPORT void JNICALL Java_org_jkiss_wmi_service_WMIObject_readMethods(JNIEnv* pJavaEnv, jobject object, jobject methodList)
{
	WMIObject* pObject = WMIObject::GetFromObject(pJavaEnv, object);
	if (pObject == NULL) {
		THROW_COMMON_EXCEPTION(ERROR_NOT_INITIALIZED);
		return;
	}
}

/*
 * Class:     org_jkiss_wmi_service_WMIObject
 * Method:    readQualifiers
 * Signature: (ZLjava/lang/String;Ljava/util/List;)V
 */
JNIEXPORT void JNICALL Java_org_jkiss_wmi_service_WMIObject_readQualifiers(JNIEnv* pJavaEnv, jobject object, jboolean, jstring, jobject)
{
	WMIObject* pObject = WMIObject::GetFromObject(pJavaEnv, object);
	if (pObject == NULL) {
		THROW_COMMON_EXCEPTION(ERROR_NOT_INITIALIZED);
		return;
	}

}

/*
 * Class:     org_jkiss_wmi_service_WMIObject
 * Method:    releaseObject
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_jkiss_wmi_service_WMIObject_releaseObject(JNIEnv* pJavaEnv, jobject object)
{
	WMIObject* pObject = WMIObject::GetFromObject(pJavaEnv, object);
	if (pObject == NULL) {
		THROW_COMMON_EXCEPTION(ERROR_NOT_INITIALIZED);
		return;
	}
	pObject->Release(pJavaEnv, object);
	delete pObject;
}
