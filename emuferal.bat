@echo off
setlocal EnableDelayedExpansion

:MAIN
SET libs=
for %%i in (*.jar) do SET libs=!libs!;%%i
for /r "./libs" %%i in (*.jar) do SET libs=!libs!;%%i
SET libs=%libs:~1%

java -cp "%libs%" org.asf.emuferal.EmuFeral %*

if EXIST updater.jar goto UPDATE
exit

:UPDATE
java -cp updater.jar org.asf.emuferal.EmuFeralUpdater --update
del updater.jar
echo.
goto MAIN
