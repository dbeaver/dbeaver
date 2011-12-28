// WMISensor.cpp : Defines the entry point for the DLL application.
//

#include "stdafx.h"
#include "WMIObject.h"
#include "WMIUtils.h"

WMIObject::WMIObject(JNIEnv * pJavaEnv, WMIService& service, jobject javaObject, IWbemClassObject* pClassObject) :
	ptrClassObject(pClassObject)
{
	pJavaEnv->SetLongField(javaObject, JNIMetaData::GetMetaData(pJavaEnv).wmiObjectHandleField, (jlong)this);
}

WMIObject::~WMIObject()
{
	
}

void WMIObject::Release(JNIEnv* pJavaEnv, jobject javaObject)
{
	if (javaObject != NULL) {
		pJavaEnv->SetLongField(javaObject, JNIMetaData::GetMetaData(pJavaEnv).wmiObjectHandleField, 0l);
	}
	ptrClassObject = NULL;
}

WMIObject* WMIObject::GetFromObject(JNIEnv* pJavaEnv, jobject javaObject)
{
	jclass objectClass = pJavaEnv->GetObjectClass(javaObject);
	jfieldID fid = pJavaEnv->GetFieldID(objectClass, "objectHandle", "J");
	DeleteLocalRef(pJavaEnv, objectClass);
	_ASSERT(fid != NULL);
	if (fid == NULL) {
		return NULL;
	}
	return (WMIObject*)pJavaEnv->GetLongField(javaObject, fid);
}

void WMIObject::GetObjectText(BSTR* pstrObjectText)
{
	ptrClassObject->GetObjectText(0, pstrObjectText);
}
