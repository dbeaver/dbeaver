#========================================================================
# Created on:   15/10/2018 08:00 AM
# Filename:   copyHTMLToOutlook.ps1  
#========================================================================



#Begin Loop
#Creates an outlook object
Add-Type -Assembly Microsoft.Office.Interop.Outlook

$ol = New-Object -comObject Outlook.Application  
$mail = $ol.CreateItem(0)  

$mail.HTMLBody  = Get-Clipboard
$mail.Display() 





