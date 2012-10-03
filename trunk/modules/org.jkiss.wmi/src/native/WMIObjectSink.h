#pragma once

#ifndef _WMI_OBJECT_SINK_H_
#define _WMI_OBJECT_SINK_H_

#include "WMIService.h"

class ATL_NO_VTABLE WMIObjectSink : 
	public CComObjectRootEx<CComSingleThreadModel>,
	public IWbemObjectSink
{
public:
	WMIObjectSink();
	virtual ~WMIObjectSink();

	jobject GetJavaSinkObject()
	{
		return javaSinkObject;
	}

	void InitSink(WMIService* pService, JNIEnv* pJavaEnv, jobject javaObject);
	void TermSink(JNIEnv* pJavaEnv);

    virtual HRESULT STDMETHODCALLTYPE Indicate( 
        long lObjectCount,
        IWbemClassObject **ppClassObject);
    
    virtual HRESULT STDMETHODCALLTYPE SetStatus( 
        long lFlags,
        HRESULT hResult,
        BSTR strParam,
        IWbemClassObject *pClassObject);

DECLARE_NO_REGISTRY()
DECLARE_NOT_AGGREGATABLE(WMIObjectSink)
DECLARE_PROTECT_FINAL_CONSTRUCT()

BEGIN_COM_MAP(WMIObjectSink)
   COM_INTERFACE_ENTRY(IWbemObjectSink)
END_COM_MAP()

private:
	void FlushObjectsCache(JNIEnv* pJavaEnv);
private:
	WMIService* pService;
	jobject javaSinkObject;

	std::vector< CComPtr<IWbemClassObject> > objectsCache;
	//JavaVM* pJavaVM;
	//JNIEnv* pThreadEnv;
	//DWORD hThread;

};


#endif