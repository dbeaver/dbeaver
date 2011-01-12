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
  Caption "DBeaver"
  BrandingText "Universal Database Manager"
  Icon "..\..\plugins\org.jkiss.dbeaver.core\icons\logo\dbeaver.ico"
  OutFile "dbeaver_setup.exe"

  VIAddVersionKey "ProductName" "DBeaver"
  VIAddVersionKey "Comments" "Univarsal Database Manager"
  VIAddVersionKey "CompanyName" "JKISS"
  VIAddVersionKey "LegalTrademarks" "DBeaver is a trademark of JKISS"
  VIAddVersionKey "LegalCopyright" "JKISS"
  VIAddVersionKey "FileDescription" "DBeaver"
  VIAddVersionKey "FileVersion" "1.0.0"
  VIProductVersion "1.0.0.0"

; Definitions for Java 6.0
  !define JRE_VERSION "7.0"
  !define JRE_URL "http://javadl.sun.com/webapps/download/AutoDL?BundleId=24936&/jre-6u10-windows-i586-p.exe"
 
; use javaw.exe to avoid dosbox.
; use java.exe to keep stdout/stderr
  !define JAVAEXE "javaw.exe"

  ;Default installation folder
  InstallDir "$PROGRAMFILES\DBeaver"
  
  ;Get installation folder from registry if available
  InstallDirRegKey HKCU "Software\DBeaver" ""

  ;Request application privileges for Windows Vista
  RequestExecutionLevel user

;--------------------------------
;Variables

  Var StartMenuFolder

;--------------------------------
;Interface Settings

  !define MUI_ABORTWARNING

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
  !insertmacro MUI_PAGE_FINISH

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

  Call GetJRE
  
  SetOutPath "$INSTDIR"
  
  File "..\..\dist\dbeaver\.eclipseproduct"
  File "..\..\dist\dbeaver\dbeaver.exe"
  File /r "..\..\dist\dbeaver\configuration"
  
  ;Store installation folder
  WriteRegStr HKCU "Software\DBeaver" "" $INSTDIR
  
  ;Create uninstaller
  WriteUninstaller "$INSTDIR\Uninstall.exe"
  
  !insertmacro MUI_STARTMENU_WRITE_BEGIN Application
    
    ;Create shortcuts
    CreateDirectory "$SMPROGRAMS\$StartMenuFolder"
    CreateShortCut "$SMPROGRAMS\$StartMenuFolder\Uninstall.lnk" "$INSTDIR\Uninstall.exe"
  
  !insertmacro MUI_STARTMENU_WRITE_END

SectionEnd

SectionGroup /e "Plugins"

	Section "Generic JDBC" SecGeneric

	  SetOutPath "$INSTDIR"
	  
	  ;ADD YOUR OWN FILES HERE...

	SectionEnd

	Section "MySQL Plugin" SecMySQL

	  SetOutPath "$INSTDIR"
	  
	  ;ADD YOUR OWN FILES HERE...

	SectionEnd

	Section "ER Diagrams" SecERD

	  SetOutPath "$INSTDIR"
	  
	  ;ADD YOUR OWN FILES HERE...

	SectionEnd

SectionGroupEnd

;--------------------------------
;Descriptions

  ;Language strings
  LangString DESC_SecCore ${LANG_ENGLISH} "DBeaver core executables and resources."
  LangString DESC_SecGeneric ${LANG_ENGLISH} "Support of generic JDBC drivers. Includes drivers for Oracle, DB2, PostgreSQL, SQL Server and Sybase"
  LangString DESC_SecMySQL ${LANG_ENGLISH} "Supports additional features for MySQL 5.x databases. Includes MySQL JDBC driver"
  LangString DESC_SecERD ${LANG_ENGLISH} "Provides support of ERD diagrams for schemas and individual tables."

  ;Assign language strings to sections
  !insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
    !insertmacro MUI_DESCRIPTION_TEXT ${SecCore} $(DESC_SecCore)
	!insertmacro MUI_DESCRIPTION_TEXT ${SecGeneric} $(DESC_SecGeneric)
	!insertmacro MUI_DESCRIPTION_TEXT ${SecMySQL} $(DESC_SecMySQL)
	!insertmacro MUI_DESCRIPTION_TEXT ${SecERD} $(DESC_SecERD)
  !insertmacro MUI_FUNCTION_DESCRIPTION_END
 
;--------------------------------
;Uninstaller Section

Section "Uninstall"

  ;ADD YOUR OWN FILES HERE...

  Delete "$INSTDIR\Uninstall.exe"

  RMDir "$INSTDIR"
  
  !insertmacro MUI_STARTMENU_GETFOLDER Application $StartMenuFolder
    
  Delete "$SMPROGRAMS\$StartMenuFolder\Uninstall.lnk"
  RMDir "$SMPROGRAMS\$StartMenuFolder"
  
  DeleteRegKey /ifempty HKCU "Software\DBeaver"

SectionEnd