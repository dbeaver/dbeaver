// WMISensor.cpp : Defines the entry point for the DLL application.
//

#include "stdafx.h"
#include "WMIUtils.h"
#include "WMIService.h"

#ifdef _MANAGED
7ji#pragma managed(push, off)
#endif

CComModule _Module;

BEGIN_OBJECT_MAP(ObjectMap)
END_OBJECT_MAP()

BOOL WINAPI DllMain(
  HINSTANCE hinstDLL,
  DWORD fdwReason,
  LPVOID lpvReserved)
{
	if (fdwReason == DLL_PROCESS_ATTACH) {
		HRESULT hres =  ::CoInitializeEx(0, COINIT_MULTITHREADED); 
		if (FAILED(hres))
		{
			::printf("Failed to initialize COM library. Error code = %d", hres);
			return FALSE;
		}
		_Module.Init(ObjectMap, hinstDLL);
		hWMIUtils = ::LoadLibrary(L"wmiutils.dll");
		hWbemCommon = ::LoadLibrary(L"wbemcomn.dll");
		// Init WMI service state
		WMIService::InitStaticState();
	} else if (fdwReason == DLL_PROCESS_DETACH) {
		// Term WMI service state
		WMIService::TermStaticState();
		JNIMetaData::Destroy();

		if (hWMIUtils != NULL) {
			::FreeLibrary(hWMIUtils);
			hWMIUtils = NULL;
		}
		if (hWbemCommon != NULL) {
			::FreeLibrary(hWbemCommon);
			hWbemCommon = NULL;
		}
		_Module.Term();
		::CoUninitialize();
	}

	return TRUE;
}

#ifdef _MANAGED
#pragma managed(pop)
#endif

