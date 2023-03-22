@echo off
set git="https://github.com/Cpeers1/Centuria.git"
set dir=%cd%

echo Updating standalone installation for testing...

echo Cloning git repository...
set tmpdir="%userprofile%\AppData\Local\Temp\build-centuria-standalone-module"
if EXIST "%tmpdir%" rmdir /S /Q "%tmpdir%"
mkdir "%tmpdir%"
git clone %git% "%tmpdir%"
cd /d %tmpdir%
echo.

echo Building...
goto execute

:execute
cmd /c java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain installation

if NOT EXIST "%dir%\server" mkdir "%dir%\server"

robocopy /E /NFL /NDL /NJH /NJS /nc /ns /np build/Installations "%dir%\server"
for /R libraries %%f in (*-javadoc.jar) do copy /Y %%f "%dir%\server\libs" >NUL
for /R libraries %%f in (*-sources.jar) do copy /Y %%f "%dir%\server\libs" >NUL

if NOT EXIST "%dir%\libraries" mkdir "%dir%\libraries"
copy /Y build\Installations\Centuria.jar "%dir%\libraries" >NUL
robocopy /E /NFL /NDL /NJH /NJS /nc /ns /np "%dir%\server\libs" "%dir%\libraries"

if NOT EXIST "%dir%\emulibs" mkdir "%dir%\emulibs"
copy /Y build\Installations\Centuria.jar "%dir%\emulibs" >NUL
robocopy /E /NFL /NDL /NJH /NJS /nc /ns /np "%dir%\server\libs" "%dir%\emulibs"

:exitmeth
cd /d %dir%
rmdir /S /Q %tmpdir%
echo.
