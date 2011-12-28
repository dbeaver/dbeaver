// WMISensor.cpp : Defines the entry point for the DLL application.
//

#include "stdafx.h"
#include "WMIObject.h"
#include "WMIUtils.h"

WMIObject::WMIObject(JNIEnv * pJavaEnv, WMIService& serviceRef, jobject javaObject, IWbemClassObject* pClassObject) :
	service(serviceRef),
	ptrClassObject(pClassObject)
{
	pJavaEnv->SetLongField(javaObject, service.GetJNIMeta().wmiObjectHandleField, (jlong)this);
}

WMIObject::~WMIObject()
{
	
}

void WMIObject::Release(JNIEnv* pJavaEnv, jobject javaObject)
{
	if (javaObject != NULL) {
		pJavaEnv->SetLongField(javaObject, service.GetJNIMeta().wmiObjectHandleField, 0l);
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
