#========================================================================
# Created on:   15/10/2018 08:00 AM
# Filename:   copyHTMLToOutlook.ps1  
#========================================================================

param ([string]$HtmlFile)

#Begin Loop
#Creates an outlook object
Add-Type -Assembly Microsoft.Office.Interop.Outlook

$content = [IO.File]::ReadAllText($HtmlFile)

$ol = New-Object -comObject Outlook.Application  
$mail = $ol.CreateItem(0)  

$mail.HTMLBody  = $content
$mail.Display() 





