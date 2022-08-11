@echo off
setlocal EnableDelayedExpansion

:MAIN
SET libs=
for %%i in (*.jar) do SET libs=!libs!;%%i
for /r "./libs" %%i in (*.jar) do SET libs=!libs!;%%i
SET libs=%libs:~1%

java -cp "%libs%" org.asf.centuria.Centuria %*

if EXIST updater.jar goto UPDATE
exit

:UPDATE
java -cp updater.jar org.asf.centuria.CenturiaUpdater --update
del updater.jar
echo.
goto MAIN
