;DBeaver installer
;Start Menu Folder Selection Example Script
;Written by Serge Rieder
;Based on StartMenu.nsi by Joost Verburg

;--------------------------------
;Include Modern UI

  !include "MUI2.nsh"
  
;--------------------------------
;General

  ;Name and file
  Name "DBeaver"
  Caption "DBeaver Setup"
  BrandingText "Universal Database Manager"
  Icon "@product.dir@\docs\dbeaver.ico"
  ;OutFile "dbeaver_setup.exe"
  OutFile "@product.dir@\dist\@archivePrefix@-@productVersion@-@arch@-setup.exe"

  VIAddVersionKey "ProductName" "DBeaver"
  VIAddVersionKey "Comments" "Univarsal Database Manager"
  VIAddVersionKey "CompanyName" "JKISS"
  VIAddVersionKey "LegalTrademarks" "DBeaver is a trademark of JKISS"
  VIAddVersionKey "LegalCopyright" "JKISS"
  VIAddVersionKey "FileDescription" "DBeaver"
  VIAddVersionKey "FileVersion" "@productVersion@"
  VIProductVersion "@productVersion@.0"

; Definitions for Java 6.0
  !define JRE_VERSION "6.0"
  !define JRE_URL "http://javadl.sun.com/webapps/download/AutoDL?BundleId=52247"
 
; use javaw.exe to avoid dosbox.
; use java.exe to keep stdout/stderr
  !define JAVAEXE "javaw.exe"

  ;Default installation folder
  InstallDir "$PROGRAMFILES\DBeaver"
  
  ;Get installation folder from registry if available
  InstallDirRegKey HKCU "Software\DBeaver" ""

  ;Request application privileges for Windows Vista
  RequestExecutionLevel admin

  Var JAVA_LOCALE

;--------------------------------
;Variables

  Var StartMenuFolder

;--------------------------------
;Interface Settings

  !define MUI_ABORTWARNING
  !define MUI_ICON "@product.dir@\docs\dbeaver.ico"
  ;!define MUI_WELCOMEFINISHPAGE_BITMAP "@product.dir@\docs\jkiss.bmp"
  ;!define MUI_WELCOMEFINISHPAGE_BITMAP_NOSTRETCH

;--------------------------------
;Pages

  !insertmacro MUI_PAGE_WELCOME
  !insertmacro MUI_PAGE_LICENSE "@product.dir@\..\docs\licenses\dbeaver_license.txt"
  !insertmacro MUI_PAGE_COMPONENTS
  !insertmacro MUI_PAGE_DIRECTORY
  
  ;Start Menu Folder Page Configuration
  !define MUI_STARTMENUPAGE_REGISTRY_ROOT "HKCU" 
  !define MUI_STARTMENUPAGE_REGISTRY_KEY "Software\DBeaver" 
  !define MUI_STARTMENUPAGE_REGISTRY_VALUENAME "Start Menu Folder"
  
  !insertmacro MUI_PAGE_STARTMENU Application $StartMenuFolder
  
  !insertmacro MUI_PAGE_INSTFILES
  
  !insertmacro MUI_UNPAGE_CONFIRM
  !insertmacro MUI_UNPAGE_INSTFILES

  !define MUI_FINISHPAGE_RUN
  !define MUI_FINISHPAGE_RUN_TEXT "Launch DBeaver"
  !define MUI_FINISHPAGE_RUN_FUNCTION "LaunchDBeaver"
  
  !define MUI_FINISHPAGE_SHOWREADME ""
  !define MUI_FINISHPAGE_SHOWREADME_NOTCHECKED
  !define MUI_FINISHPAGE_SHOWREADME_TEXT "Create Desktop Shortcut"
  !define MUI_FINISHPAGE_SHOWREADME_FUNCTION FinishPageAction

  !insertmacro MUI_PAGE_FINISH

Function FinishPageAction
	CreateShortCut "$DESKTOP\DBeaver.lnk" "$INSTDIR\dbeaver.exe" "-nl $JAVA_LOCALE"
FunctionEnd

Function LaunchDBeaver
  ExecShell "" "$SMPROGRAMS\$StartMenuFolder\DBeaver.lnk"
FunctionEnd

;--------------------------------
;JRE

!include "FileFunc.nsh"
!insertmacro GetFileVersion
!insertmacro GetParameters
!include "WordFunc.nsh"
!insertmacro VersionCompare

!include "JRE.nsh"

;--------------------------------
;Languages

!define MUI_LANGDLL_REGISTRY_ROOT HKCU
!define MUI_LANGDLL_REGISTRY_KEY Software\DBeaver
!define MUI_LANGDLL_REGISTRY_VALUENAME Language

!define MUI_LANGDLL_ALWAYSSHOW

!insertmacro MUI_DEFAULT MUI_LANGDLL_WINDOWTITLE "Installer Language"
!insertmacro MUI_DEFAULT MUI_LANGDLL_INFO "Please select a language."

!define MUI_LANGDLL_ALLLANGUAGES

!insertmacro MUI_LANGUAGE "English"
!insertmacro MUI_LANGUAGE "Arabic"
!insertmacro MUI_LANGUAGE "Dutch"
!insertmacro MUI_LANGUAGE "French"
!insertmacro MUI_LANGUAGE "German"
!insertmacro MUI_LANGUAGE "Italian"
!insertmacro MUI_LANGUAGE "Russian"
!insertmacro MUI_LANGUAGE "SimpChinese"
!insertmacro MUI_LANGUAGE "Spanish"
!insertmacro MUI_LANGUAGE "Turkish"

!insertmacro MUI_RESERVEFILE_LANGDLL


;--------------------------------
;Installer Sections

Section "-DBeaver Core" SecCore

  ; Install JRE on demand
  SetShellVarContext all
  Call GetJRE
  
  ; If there is previous version of DBeaver - remove it's configuration and plugins
  RMDir /r $INSTDIR\configuration
  RMDir /r $INSTDIR\plugins
  RMDir /r $INSTDIR\licenses

  SetOutPath "$INSTDIR"
  
  ; Copy files
  File "..\raw\win32.@arch@\dbeaver\.eclipseproduct"
  File "..\raw\win32.@arch@\dbeaver\readme.txt"
  File "..\raw\win32.@arch@\dbeaver\dbeaver.exe"
  File /r "..\raw\win32.@arch@\dbeaver\configuration"
  File /r  /x org.jkiss.*.jar "..\raw\win32.@arch@\dbeaver\plugins"

  CreateDirectory $INSTDIR\licenses
  SetOutPath "$INSTDIR\licenses"

  File "..\raw\win32.@arch@\dbeaver\licenses\*.*"

  SetOutPath "$INSTDIR\plugins"
  
  File "..\raw\win32.@arch@\dbeaver\plugins\org.jkiss.dbeaver.core_*.jar"
  File "..\raw\win32.@arch@\dbeaver\plugins\org.jkiss.dbeaver.core.application_*.jar"
  File "..\raw\win32.@arch@\dbeaver\plugins\org.jkiss.utils_*.jar"
  
  ;Store installation folder
  WriteRegStr HKCU "Software\DBeaver" "" $INSTDIR
  
  ;Create uninstaller
  WriteUninstaller "$INSTDIR\Uninstall.exe"
  
  !insertmacro MUI_STARTMENU_WRITE_BEGIN Application
    
    ;Create shortcuts
    CreateDirectory "$SMPROGRAMS\$StartMenuFolder"
	CreateShortCut "$SMPROGRAMS\$StartMenuFolder\DBeaver.lnk" "$INSTDIR\dbeaver.exe" "-nl $JAVA_LOCALE"
    CreateShortCut "$SMPROGRAMS\$StartMenuFolder\Uninstall.lnk" "$INSTDIR\Uninstall.exe"
  
  !insertmacro MUI_STARTMENU_WRITE_END

SectionEnd

SectionGroup /e "Plugins"

	Section "Generic JDBC" SecGeneric

	  SetOutPath "$INSTDIR\plugins"
	  
	  File "..\raw\win32.@arch@\dbeaver\plugins\org.jkiss.dbeaver.ext.generic_*.jar"

	SectionEnd

	Section "MySQL Plugin" SecMySQL

	  SetOutPath "$INSTDIR\plugins"
	  
	  File "..\raw\win32.@arch@\dbeaver\plugins\org.jkiss.dbeaver.ext.mysql_*.jar"

	SectionEnd

	Section "Oracle Plugin" SecOracle

	  SetOutPath "$INSTDIR\plugins"

	  File "..\raw\win32.@arch@\dbeaver\plugins\org.jkiss.dbeaver.ext.oracle_*.jar"

	SectionEnd

	Section "WMI" SecWMI

	  SetOutPath "$INSTDIR\plugins"

	  File "..\raw\win32.@arch@\dbeaver\plugins\org.jkiss.wmi_*.jar"
	  File "..\raw\win32.@arch@\dbeaver\plugins\org.jkiss.dbeaver.ext.wmi_*.jar"

	SectionEnd

	Section "NoSQL" SecNoSQL

	  SetOutPath "$INSTDIR\plugins"

	  File "..\raw\win32.@arch@\dbeaver\plugins\org.jkiss.dbeaver.ext.nosql*.jar"

	SectionEnd

	Section "ER Diagrams" SecERD

	  SetOutPath "$INSTDIR\plugins"
	  
	  File "..\raw\win32.@arch@\dbeaver\plugins\org.jkiss.dbeaver.ext.erd_*.jar"

	SectionEnd
	
	Section "-Import 3rd Party Configurations" Sec3RD
		File "..\raw\win32.@arch@\dbeaver\plugins\org.jkiss.dbeaver.ext.import_config_*.jar"
	SectionEnd

SectionGroupEnd

Section "-Drivers" SecDrivers
  SetOutPath "$INSTDIR"
;  File /r "..\raw\win32.@arch@\dbeaver\drivers"

SectionEnd

;--------------------------------
;Descriptions

  ;Language strings
  LangString DESC_SecCore ${LANG_ENGLISH} "DBeaver core executables and resources."
  LangString DESC_SecGeneric ${LANG_ENGLISH} "Support of generic JDBC drivers."
  LangString DESC_SecMySQL ${LANG_ENGLISH} "Supports additional features for MySQL 5.x databases. Includes MySQL JDBC driver"
  LangString DESC_SecOracle ${LANG_ENGLISH} "Supports additional features for Oracle 8.x-11.x databases."
  LangString DESC_SecWMI ${LANG_ENGLISH} "Supports additional features for Windows Management Instrumentation (WMI)."
  LangString DESC_SecNoSQL ${LANG_ENGLISH} "NoSQL databases support (Cassandra)."
  LangString DESC_SecERD ${LANG_ENGLISH} "Provides support of ERD diagrams for schemas and individual tables."
  LangString DESC_SecDrivers ${LANG_ENGLISH} "Includes JDBC drivers for Oracle, DB2, PostgreSQL, SQL Server and Sybase."

  ;Assign language strings to sections
  !insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
    !insertmacro MUI_DESCRIPTION_TEXT ${SecCore} $(DESC_SecCore)
	!insertmacro MUI_DESCRIPTION_TEXT ${SecGeneric} $(DESC_SecGeneric)
	!insertmacro MUI_DESCRIPTION_TEXT ${SecMySQL} $(DESC_SecMySQL)
	!insertmacro MUI_DESCRIPTION_TEXT ${SecOracle} $(DESC_SecOracle)
	!insertmacro MUI_DESCRIPTION_TEXT ${SecWMI} $(DESC_SecWMI)
	!insertmacro MUI_DESCRIPTION_TEXT ${SecNoSQL} $(DESC_SecNoSQL)
	!insertmacro MUI_DESCRIPTION_TEXT ${SecERD} $(DESC_SecERD)
	!insertmacro MUI_DESCRIPTION_TEXT ${SecDrivers} $(DESC_SecDrivers)
  !insertmacro MUI_FUNCTION_DESCRIPTION_END
 
;--------------------------------
;Uninstaller Section

Section "Uninstall"

  SetShellVarContext all
  Delete "$INSTDIR\Uninstall.exe"

  Delete "$INSTDIR\.eclipseproduct"
  Delete "$INSTDIR\dbeaver.exe"
  Delete "$INSTDIR\readme.txt"
  Delete "$INSTDIR\license.txt"
  Delete "$INSTDIR\*.log"
  RMDir /r "$INSTDIR\configuration"
  RMDir /r "$INSTDIR\plugins"
  RMDir /r "$INSTDIR\drivers"
  RMDir "$INSTDIR"

  !insertmacro MUI_STARTMENU_GETFOLDER Application $StartMenuFolder

  Delete "$SMPROGRAMS\$StartMenuFolder\DBeaver.lnk"
  Delete "$SMPROGRAMS\$StartMenuFolder\Uninstall.lnk"
  Delete "$DESKTOP\DBeaver.lnk"
  RMDir "$SMPROGRAMS\$StartMenuFolder"

  DeleteRegKey /ifempty HKCU "Software\DBeaver"

SectionEnd

;--------------------------------
;Installer Functions

!define LOCALE_ILANGUAGE '0x1' ;System Language Resource ID
!define LOCALE_SLANGUAGE '0x2' ;System Language & Country [Cool]
!define LOCALE_SABBREVLANGNAME '0x3' ;System abbreviated language
!define LOCALE_SNATIVELANGNAME '0x4' ;System native language name [Cool]
!define LOCALE_ICOUNTRY '0x5' ;System country code
!define LOCALE_SCOUNTRY '0x6' ;System Country
!define LOCALE_SABBREVCTRYNAME '0x7' ;System abbreviated country name
!define LOCALE_SNATIVECTRYNAME '0x8' ;System native country name [Cool]
!define LOCALE_IDEFAULTLANGUAGE '0x9' ;System default language ID
!define LOCALE_IDEFAULTCOUNTRY  '0xA' ;System default country code
!define LOCALE_IDEFAULTCODEPAGE '0xB' ;System default oem code page

Function .onInit

    !insertmacro MUI_LANGDLL_DISPLAY

    StrCpy $JAVA_LOCALE en

    StrCmp $LANGUAGE 1025 0 +2
        StrCpy $JAVA_LOCALE ar
    StrCmp $LANGUAGE 1031 0 +2
        StrCpy $JAVA_LOCALE de
    StrCmp $LANGUAGE 1036 0 +2
        StrCpy $JAVA_LOCALE fr
    StrCmp $LANGUAGE 1040 0 +2
        StrCpy $JAVA_LOCALE it
    StrCmp $LANGUAGE 1043 0 +2
        StrCpy $JAVA_LOCALE nl
    StrCmp $LANGUAGE 1044 0 +2
        StrCpy $JAVA_LOCALE no
    StrCmp $LANGUAGE 1045 0 +2
        StrCpy $JAVA_LOCALE pl
    StrCmp $LANGUAGE 1048 0 +2
        StrCpy $JAVA_LOCALE ro
    StrCmp $LANGUAGE 1049 0 +2
        StrCpy $JAVA_LOCALE ru
    StrCmp $LANGUAGE 1053 0 +2
        StrCpy $JAVA_LOCALE sv
    StrCmp $LANGUAGE 1055 0 +2
        StrCpy $JAVA_LOCALE tr
    StrCmp $LANGUAGE 1066 0 +2
        StrCpy $JAVA_LOCALE vi
    StrCmp $LANGUAGE 2052 0 +2
        StrCpy $JAVA_LOCALE zh

  ;!insertmacro MUI_UNGETLANGUAGE

FunctionEnd

