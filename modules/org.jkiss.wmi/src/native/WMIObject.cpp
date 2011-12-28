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

jstring WMIObject::GetObjectText(JNIEnv* pJavaEnv)
{
	CComBSTR bstrObjectText;
	ptrClassObject->GetObjectText(0, &bstrObjectText);
	if (pJavaEnv->ExceptionCheck()) {
		return NULL;
	}
	return MakeJavaString(pJavaEnv, bstrObjectText);
}

jobject WMIObject::GetPropertyValue(JNIEnv* pJavaEnv, jstring propName)
{
	CComBSTR bstrPropName;
	::GetJavaString(pJavaEnv, propName, &bstrPropName);

	CComVariant propValue;
	CIMTYPE cimType; // CIMTYPE_ENUMERATION
	LONG flavor;
	HRESULT hres = ptrClassObject->Get(bstrPropName, 0, &propValue, &cimType, &flavor);
	if (FAILED(hres)) {
		THROW_COMMON_ERROR((_bstr_t(L"Can't read property '") + (BSTR)bstrPropName) + L"' value", hres);
		return NULL;
	}
	return ::MakeJavaFromVariant(pJavaEnv, propValue, cimType);
}

void WMIObject::ReadProperties(JNIEnv* pJavaEnv, jobject javaObject, jobject propList)
{
	JNIMetaData& jniMeta = JNIMetaData::GetMetaData(pJavaEnv);

	// Fill class object properties
	HRESULT hres = ptrClassObject->BeginEnumeration(0);
	if (FAILED(hres)) {
		THROW_COMMON_ERROR(L"Could not start class object properties enumeration", hres);
		return;
	}

	for (;;) {
		CComBSTR propName;
		CComVariant propValue;
		CIMTYPE cimType; // CIMTYPE_ENUMERATION
		LONG flavor;
		hres = ptrClassObject->Next(0, &propName, &propValue, &cimType, &flavor);
		if (FAILED(hres)) {
			THROW_COMMON_ERROR(L"Could not obtain next class object from enumeration", hres);
			break;
		}
		if (hres == WBEM_S_NO_MORE_DATA) {
			break;
		}
		//wchar_t* propNameBSTR = propName;
		jstring javaPropName = ::MakeJavaString(pJavaEnv, propName);
		_ASSERT(javaPropName != NULL);
		if (javaPropName == NULL) {
			continue;
		}
		jobject javaPropValue = ::MakeJavaFromVariant(pJavaEnv, propValue, cimType);
		if (!pJavaEnv->ExceptionCheck()) {
			jobject javaPropObject = pJavaEnv->NewObject(
				jniMeta.wmiObjectPropertyClass, 
				jniMeta.wmiObjectPropertyConstructor,
				javaObject,
				javaPropName,
				(jint)cimType,
				(jint)flavor,
				javaPropValue);
			if (pJavaEnv->ExceptionCheck()) {
				break;
			}
			pJavaEnv->CallVoidMethod(propList, jniMeta.javaUtilListAddMethod, javaPropObject);
			DeleteLocalRef(pJavaEnv, javaPropObject);
		}
		DeleteLocalRef(pJavaEnv, javaPropName);
		DeleteLocalRef(pJavaEnv, javaPropValue);
		if (pJavaEnv->ExceptionCheck()) {
			break;
		}
	}

	hres = ptrClassObject->EndEnumeration();
	if (FAILED(hres)) {
		THROW_COMMON_ERROR(L"Could not finish class object enumeration", hres);
	}
}
