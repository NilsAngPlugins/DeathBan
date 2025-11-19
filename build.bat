@echo off
setlocal enabledelayedexpansion

cd /d "%~dp0"

set WRAPPER=gradlew.bat
set GRADLE_EXE=%ProgramData%\chocolatey\lib\gradle\tools\gradle-9.1.0\bin\gradle.bat

echo === NilsANG-DeathBan Build ===

if not exist "%WRAPPER%" (
	echo Gradle Wrapper nicht gefunden. Erzeuge Wrapper 8.10.2 ...
	if exist "%GRADLE_EXE%" (
		call "%GRADLE_EXE%" wrapper --gradle-version 8.10.2 --distribution-type bin --no-daemon
	) else (
		call gradle wrapper --gradle-version 8.10.2 --distribution-type bin --no-daemon
	)
)

if not exist "%WRAPPER%" (
	echo Fehler: Gradle Wrapper konnte nicht erzeugt werden.
	exit /b 1
)

echo === Gradle-Version ===
call "%WRAPPER%" -v

echo === Build: shadowJar ===
call "%WRAPPER%" clean shadowJar --no-daemon
if errorlevel 1 (
	echo Fehler: Build fehlgeschlagen.
	exit /b 1
)

set OUT=build\libs\NilsANG-DeathBan-0.1.0-SNAPSHOT.jar
if not exist "%OUT%" (
	echo Fehler: Artefakt nicht gefunden: %OUT%
	dir build\libs
	exit /b 1
)

if not exist "..\server\plugins" mkdir "..\server\plugins"
copy /Y "%OUT%" "..\server\plugins\NilsANG-DeathBan.jar" >nul
if errorlevel 1 (
	echo Fehler: Kopieren fehlgeschlagen.
	exit /b 1
)

echo OK: Kopiert nach plugins\NilsANG-DeathBan.jar
exit /b 0
