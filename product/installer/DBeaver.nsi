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
  Icon "..\..\docs\dbeaver.ico"
  ;OutFile "dbeaver_setup.exe"
  OutFile "..\@buildId@-setup.exe"

  VIAddVersionKey "ProductName" "DBeaver"
  VIAddVersionKey "Comments" "Univarsal Database Manager"
  VIAddVersionKey "CompanyName" "JKISS"
  VIAddVersionKey "LegalTrademarks" "DBeaver is a trademark of JKISS"
  VIAddVersionKey "LegalCopyright" "JKISS"
  VIAddVersionKey "FileDescription" "DBeaver"
  VIAddVersionKey "FileVersion" "1.0.0"
  VIProductVersion "1.0.0.0"

; Definitions for Java 6.0
  !define JRE_VERSION "6.0"
  !define JRE_URL "http://javadl.sun.com/webapps/download/AutoDL?BundleId=24936&/jre-6u10-windows-i586-p.exe"
 
; use javaw.exe to avoid dosbox.
; use java.exe to keep stdout/stderr
  !define JAVAEXE "javaw.exe"

  ;Default installation folder
  InstallDir "$PROGRAMFILES\DBeaver"
  
  ;Get installation folder from registry if available
  InstallDirRegKey HKCU "Software\DBeaver" ""

  ;Request application privileges for Windows Vista
  RequestExecutionLevel admin

;--------------------------------
;Variables

  Var StartMenuFolder

;--------------------------------
;Interface Settings

  !define MUI_ABORTWARNING
  !define MUI_ICON "..\..\docs\dbeaver.ico"
  ;!define MUI_WELCOMEFINISHPAGE_BITMAP "..\..\docs\jkiss.bmp"
  ;!define MUI_WELCOMEFINISHPAGE_BITMAP_NOSTRETCH

;--------------------------------
;Pages

  !insertmacro MUI_PAGE_WELCOME
  !insertmacro MUI_PAGE_LICENSE "..\..\docs\license.txt"
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
  !insertmacro MUI_PAGE_FINISH

Function LaunchDBeaver
  ExecShell "" "$SMPROGRAMS\$StartMenuFolder\DBeaver.lnk"
FunctionEnd

;--------------------------------
;Languages
 
  !insertmacro MUI_LANGUAGE "English"

!include "FileFunc.nsh"
!insertmacro GetFileVersion
!insertmacro GetParameters
!include "WordFunc.nsh"
!insertmacro VersionCompare

!include "JRE.nsh"

;--------------------------------
;Installer Sections

Section "-DBeaver Core" SecCore

  ; Install JRE on demand
  SetShellVarContext all
  Call GetJRE
  
  ; If there is previous version of DBeaver - remove it's configuration
  RMDir /r $INSTDIR\configuration

  SetOutPath "$INSTDIR"
  
  ; Copy files
  File "raw\win32.x86\dbeaver\.eclipseproduct"
  File "raw\win32.x86\dbeaver\dbeaver.exe"
  File /r "raw\win32.x86\dbeaver\configuration"
  File /r  /x org.jkiss.*.jar "raw\win32.x86\dbeaver\plugins"
  
  SetOutPath "$INSTDIR\plugins"
  
  File "raw\win32.x86\dbeaver\plugins\org.jkiss.dbeaver.core_1.0.0.*.jar"
  
  ;Store installation folder
  WriteRegStr HKCU "Software\DBeaver" "" $INSTDIR
  
  ;Create uninstaller
  WriteUninstaller "$INSTDIR\Uninstall.exe"
  
  !insertmacro MUI_STARTMENU_WRITE_BEGIN Application
    
    ;Create shortcuts
    CreateDirectory "$SMPROGRAMS\$StartMenuFolder"
	CreateShortCut "$SMPROGRAMS\$StartMenuFolder\DBeaver.lnk" "$INSTDIR\dbeaver.exe"
    CreateShortCut "$SMPROGRAMS\$StartMenuFolder\Uninstall.lnk" "$INSTDIR\Uninstall.exe"
  
  !insertmacro MUI_STARTMENU_WRITE_END

SectionEnd

SectionGroup /e "Plugins"

	Section "Generic JDBC" SecGeneric

	  SetOutPath "$INSTDIR\plugins"
	  
	  File "raw\win32.x86\dbeaver\plugins\org.jkiss.dbeaver.ext.generic_1.0.0.jar"

	SectionEnd

	Section "MySQL Plugin" SecMySQL

	  SetOutPath "$INSTDIR\plugins"
	  
	  File "raw\win32.x86\dbeaver\plugins\org.jkiss.dbeaver.ext.mysql_1.0.0.jar"

	SectionEnd

	Section "ER Diagrams" SecERD

	  SetOutPath "$INSTDIR\plugins"
	  
	  File "raw\win32.x86\dbeaver\plugins\org.jkiss.dbeaver.ext.erd_1.0.0.jar"

	SectionEnd
	
	Section "-Import 3rd Party Configurations" Sec3RD
		File "raw\win32.x86\dbeaver\plugins\org.jkiss.dbeaver.ext.import_config_1.0.0.jar"
	SectionEnd

SectionGroupEnd

Section "Drivers" SecDrivers

  SetOutPath "$INSTDIR"
  
  File /r "raw\win32.x86\dbeaver\drivers"

SectionEnd

;--------------------------------
;Descriptions

  ;Language strings
  LangString DESC_SecCore ${LANG_ENGLISH} "DBeaver core executables and resources."
  LangString DESC_SecGeneric ${LANG_ENGLISH} "Support of generic JDBC drivers."
  LangString DESC_SecMySQL ${LANG_ENGLISH} "Supports additional features for MySQL 5.x databases. Includes MySQL JDBC driver"
  LangString DESC_SecERD ${LANG_ENGLISH} "Provides support of ERD diagrams for schemas and individual tables."
  LangString DESC_SecDrivers ${LANG_ENGLISH} "Includes JDBC drivers for Oracle, DB2, PostgreSQL, SQL Server and Sybase."

  ;Assign language strings to sections
  !insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
    !insertmacro MUI_DESCRIPTION_TEXT ${SecCore} $(DESC_SecCore)
	!insertmacro MUI_DESCRIPTION_TEXT ${SecGeneric} $(DESC_SecGeneric)
	!insertmacro MUI_DESCRIPTION_TEXT ${SecMySQL} $(DESC_SecMySQL)
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
  RMDir /r "$INSTDIR\configuration"
  RMDir /r "$INSTDIR\plugins"
  RMDir /r "$INSTDIR\drivers"
  RMDir "$INSTDIR"

  !insertmacro MUI_STARTMENU_GETFOLDER Application $StartMenuFolder

  Delete "$SMPROGRAMS\$StartMenuFolder\DBeaver.lnk"
  Delete "$SMPROGRAMS\$StartMenuFolder\Uninstall.lnk"
  RMDir "$SMPROGRAMS\$StartMenuFolder"

  DeleteRegKey /ifempty HKCU "Software\DBeaver"

SectionEnd