// WMISensor.cpp : Defines the entry point for the DLL application.
//

#include "stdafx.h"
#include "WMIObject.h"
#include "WMIUtils.h"

WMIObject::WMIObject(JNIEnv * pJavaEnv, jobject javaObject, IWbemClassObject* pClassObject) :
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

jobject WMIObject::GetAttributeValue(JNIEnv* pJavaEnv, jstring propName)
{
	CComBSTR bstrPropName;
	::GetJavaString(pJavaEnv, propName, &bstrPropName);

	CComVariant propValue;
	CIMTYPE cimType; // CIMTYPE_ENUMERATION
	LONG flavor;
	HRESULT hres = ptrClassObject->Get(bstrPropName, 0, &propValue, &cimType, &flavor);
	if (FAILED(hres)) {
		THROW_COMMON_ERROR((_bstr_t(L"Can't read attribute '") + (BSTR)bstrPropName) + L"' value", hres);
		return NULL;
	}
	return ::MakeJavaFromVariant(pJavaEnv, propValue, cimType);
}

void WMIObject::ReadAttributes(JNIEnv* pJavaEnv, jobject javaObject, LONG lFlags, jobject propList)
{
	JNIMetaData& jniMeta = JNIMetaData::GetMetaData(pJavaEnv);

	// Fill class object properties
	HRESULT hres = ptrClassObject->BeginEnumeration(lFlags);
	if (FAILED(hres)) {
		THROW_COMMON_ERROR(L"Can't start class object attributes enumeration", hres);
		return;
	}

	for (;;) {
		CComBSTR propName;
		CComVariant propValue;
		CIMTYPE cimType; // CIMTYPE_ENUMERATION
		LONG flavor;
		hres = ptrClassObject->Next(0, &propName, &propValue, &cimType, &flavor);
		if (FAILED(hres)) {
			THROW_COMMON_ERROR(L"Can't obtain next attribute from enumeration", hres);
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
				jniMeta.wmiObjectAttributeClass, 
				jniMeta.wmiObjectAttributeConstructor,
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
		THROW_COMMON_ERROR(L"Can't finish class object enumeration", hres);
	}
}

void WMIObject::ReadMethods(JNIEnv* pJavaEnv, jobject javaObject, LONG lFlags, jobject methodList)
{
	JNIMetaData& jniMeta = JNIMetaData::GetMetaData(pJavaEnv);

	// Fill class object properties
	HRESULT hres = ptrClassObject->BeginMethodEnumeration(lFlags);
	if (FAILED(hres)) {
		THROW_COMMON_ERROR(L"Can't start class object methods enumeration", hres);
		return;
	}

	for (;;) {
		CComBSTR methodName;
		CComPtr<IWbemClassObject> ptrInSignature;
		CComPtr<IWbemClassObject> ptrOutSignature;
		hres = ptrClassObject->NextMethod(0, &methodName, &ptrInSignature, &ptrOutSignature);
		if (FAILED(hres)) {
			THROW_COMMON_ERROR(L"Can't obtain next method from enumeration", hres);
			break;
		}
		if (hres == WBEM_S_NO_MORE_DATA) {
			break;
		}
		//wchar_t* propNameBSTR = propName;
		jstring javaMethodName = ::MakeJavaString(pJavaEnv, methodName);
		_ASSERT(javaMethodName != NULL);
		if (javaMethodName == NULL) {
			continue;
		}
		jobject javaInSignature = ptrInSignature == NULL ? NULL : WMIService::MakeWMIObject(pJavaEnv, ptrInSignature);
		jobject javaOutSignature = ptrOutSignature == NULL ? NULL : WMIService::MakeWMIObject(pJavaEnv, ptrOutSignature);
		if (!pJavaEnv->ExceptionCheck()) {
			jobject javaMethodObject = pJavaEnv->NewObject(
				jniMeta.wmiObjectMethodClass, 
				jniMeta.wmiObjectMethodConstructor,
				javaObject,
				javaMethodName,
				javaInSignature,
				javaOutSignature);
			if (pJavaEnv->ExceptionCheck()) {
				break;
			}
			pJavaEnv->CallVoidMethod(methodList, jniMeta.javaUtilListAddMethod, javaMethodObject);
			DeleteLocalRef(pJavaEnv, javaMethodObject);
		}
		DeleteLocalRef(pJavaEnv, javaMethodName);
		if (javaInSignature != NULL) {
			DeleteLocalRef(pJavaEnv, javaInSignature);
		}
		if (javaOutSignature != NULL) {
			DeleteLocalRef(pJavaEnv, javaOutSignature);
		}
		if (pJavaEnv->ExceptionCheck()) {
			break;
		}
	}

	hres = ptrClassObject->EndMethodEnumeration();
	if (FAILED(hres)) {
		THROW_COMMON_ERROR(L"Can't finish class object enumeration", hres);
	}
}

void WMIObject::ReadQualifiers(JNIEnv* pJavaEnv, jobject javaObject, bool isAttribute, jstring propName, jobject qfList)
{
	JNIMetaData& jniMeta = JNIMetaData::GetMetaData(pJavaEnv);

	CComBSTR bstrPropName;
	if (propName != NULL) {
		::GetJavaString(pJavaEnv, propName, &bstrPropName);
	}
	CComPtr<IWbemQualifierSet> ptrQFSet;
	HRESULT hres = S_OK;
	if (bstrPropName == NULL) {
		hres = ptrClassObject->GetQualifierSet(&ptrQFSet);
	} else if (isAttribute) {
		hres = ptrClassObject->GetPropertyQualifierSet(bstrPropName, &ptrQFSet);
	} else {
		hres = ptrClassObject->GetMethodQualifierSet(bstrPropName, &ptrQFSet);
	}
	if (FAILED(hres)) {
		THROW_COMMON_ERROR(L"Can't create qualifiers enumeration", hres);
		return;
	}

	// Fill class object properties
	hres = ptrQFSet->BeginEnumeration(0);
	if (FAILED(hres)) {
		THROW_COMMON_ERROR(L"Can't begin qualifiers enumeration", hres);
		return;
	}

	for (;;) {
		CComBSTR qfName;
		CComVariant qfValue;
		LONG flavor;
		hres = ptrQFSet->Next(0, &qfName, &qfValue, &flavor);
		if (FAILED(hres)) {
			THROW_COMMON_ERROR(L"Can't obtain next qualifier from enumeration", hres);
			break;
		}
		if (hres == WBEM_S_NO_MORE_DATA) {
			break;
		}
		//wchar_t* propNameBSTR = propName;
		jstring javaQFName = ::MakeJavaString(pJavaEnv, qfName);
		_ASSERT(javaQFName != NULL);
		if (javaQFName == NULL) {
			continue;
		}
		jobject javaQFValue = ::MakeJavaFromVariant(pJavaEnv, qfValue, CIM_EMPTY);
		if (!pJavaEnv->ExceptionCheck()) {
			jobject javaPropObject = pJavaEnv->NewObject(
				jniMeta.wmiQualifierClass, 
				jniMeta.wmiQualifierConstructor,
				javaQFName,
				(jint)flavor,
				javaQFValue);
			if (pJavaEnv->ExceptionCheck()) {
				break;
			}
			pJavaEnv->CallVoidMethod(qfList, jniMeta.javaUtilListAddMethod, javaPropObject);
			DeleteLocalRef(pJavaEnv, javaPropObject);
		}
		DeleteLocalRef(pJavaEnv, javaQFName);
		DeleteLocalRef(pJavaEnv, javaQFValue);
		if (pJavaEnv->ExceptionCheck()) {
			break;
		}
	}

	hres = ptrQFSet->EndEnumeration();
	if (FAILED(hres)) {
		THROW_COMMON_ERROR(L"Can't finish qualifiers enumeration", hres);
	}
}
