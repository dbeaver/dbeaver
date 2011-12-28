// WMISensor.cpp : Defines the entry point for the DLL application.
//

#include "stdafx.h"
#include "WMIObjectJNI.h"
#include "WMIObject.h"
#include "WMIUtils.h"

/*
 * Class:     org_jkiss_wmi_service_WMIObject
 * Method:    readObjectText
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_jkiss_wmi_service_WMIObject_readObjectText
  (JNIEnv *, jobject)
{
	return NULL;
}

/*
 * Class:     org_jkiss_wmi_service_WMIObject
 * Method:    readPropertyValue
 * Signature: (Ljava/lang/String;)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_org_jkiss_wmi_service_WMIObject_readPropertyValue
  (JNIEnv *, jobject, jstring)
{
	return NULL;
}

/*
 * Class:     org_jkiss_wmi_service_WMIObject
 * Method:    writePropertyValue
 * Signature: (Ljava/lang/String;Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_org_jkiss_wmi_service_WMIObject_writePropertyValue
  (JNIEnv *, jobject, jstring, jobject)
{
	return;
}

/*
 * Class:     org_jkiss_wmi_service_WMIObject
 * Method:    readProperties
 * Signature: (Ljava/util/List;)Ljava/util/List;
 */
JNIEXPORT jobject JNICALL Java_org_jkiss_wmi_service_WMIObject_readProperties
  (JNIEnv *, jobject, jobject)
{
	return NULL;
}

/*
 * Class:     org_jkiss_wmi_service_WMIObject
 * Method:    readMethod
 * Signature: (Ljava/util/List;)Ljava/util/List;
 */
JNIEXPORT jobject JNICALL Java_org_jkiss_wmi_service_WMIObject_readMethod
  (JNIEnv *, jobject, jobject)
{
	return NULL;
}

/*
 * Class:     org_jkiss_wmi_service_WMIObject
 * Method:    readQualifiers
 * Signature: (ZLjava/lang/String;Ljava/util/List;)V
 */
JNIEXPORT void JNICALL Java_org_jkiss_wmi_service_WMIObject_readQualifiers
  (JNIEnv *, jobject, jboolean, jstring, jobject)
{

}

/*
 * Class:     org_jkiss_wmi_service_WMIObject
 * Method:    releaseObject
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_jkiss_wmi_service_WMIObject_releaseObject
  (JNIEnv * pJavaEnv, jobject object)
{
	WMIObject* pObject = WMIObject::GetFromObject(pJavaEnv, object);
	if (pObject == NULL) {
		THROW_COMMON_EXCEPTION(L"WMI Service is not initialized");
		return;
	}
	pObject->Release(pJavaEnv, object);
	delete pObject;
}
