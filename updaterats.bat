@echo off
set git="https://aerialworks.ddns.net/ASF/RATS.git"
set dir="%cd%"

echo Updating RaTs! installation for libraries...
if EXIST libraries rmdir /S /Q libraries

echo Cloning git repository...
set tmpdir="%userprofile%\AppData\Local\Temp\build-rats-connective-http-standalone"
if EXIST "%tmpdir%" rmdir /S /Q "%tmpdir%"

mkdir "%tmpdir%"
git clone %git% "%tmpdir%"
cd "%tmpdir%"
echo.

echo Building...
goto execute

:execute
cmd /c java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain installation

if NOT EXIST %dir%\libraries mkdir %dir%\libraries
for /R build\Installations %%f in (*.jar) do copy /Y %%f %dir%\libraries >NUL
goto exitmeth

:exitmeth
cd "%dir%"
rmdir /S /Q %tmpdir%
echo.
