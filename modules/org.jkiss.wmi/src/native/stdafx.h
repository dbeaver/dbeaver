// stdafx.h : include file for standard system include files,
// or project specific include files that are used frequently, but
// are changed infrequently
//

#pragma once

#define _WIN32_DCOM

// Modify the following defines if you have to target a platform prior to the ones specified below.
// Refer to MSDN for the latest info on corresponding values for different platforms.
#ifndef WINVER				// Allow use of features specific to Windows XP or later.
#define WINVER 0x0501		// Change this to the appropriate value to target other versions of Windows.
#endif

#ifndef _WIN32_WINNT		// Allow use of features specific to Windows XP or later.                   
#define _WIN32_WINNT 0x0501	// Change this to the appropriate value to target other versions of Windows.
#endif						

#ifndef _WIN32_WINDOWS		// Allow use of features specific to Windows 98 or later.
#define _WIN32_WINDOWS 0x0410 // Change this to the appropriate value to target Windows Me or later.
#endif

#ifndef _WIN32_IE			// Allow use of features specific to IE 6.0 or later.
#define _WIN32_IE 0x0600	// Change this to the appropriate value to target other versions of IE.
#endif

#define WIN32_LEAN_AND_MEAN		// Exclude rarely-used stuff from Windows headers
// Windows Header Files:
#include <windows.h>


#define _ATL_CSTRING_EXPLICIT_CONSTRUCTORS	// some CString constructors will be explicit

#include <atlbase.h>
#include <atlstr.h>
#include <atlsafe.h>
#include <atlcom.h>
#include <atlcomcli.h>

#include <objbase.h>
//#include <iostream>
#include <vector>
#include <algorithm>

#include <comdef.h>

#include <Wbemidl.h>
# pragma comment(lib, "wbemuuid.lib")
# pragma comment(lib, "credui.lib")
#include <Wbemtime.h>
//# pragma comment(lib, "Framedyd.lib")

#include <wincred.h>
#include <crtdbg.h>

extern CComModule _Module;

// TODO: reference additional headers your program requires here
