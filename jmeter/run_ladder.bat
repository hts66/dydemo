@echo off
rem ============================================================
rem  JMeter ladder concurrency test runner
rem  Usage:  run_ladder.bat <scenario> [tier list]
rem  Scenarios:  list_api | video_download | mixed_browse
rem  Examples:
rem    run_ladder.bat video_download
rem    run_ladder.bat video_download "10 50 100 200"
rem    run_ladder.bat mixed_browse "20 50 100"
rem ============================================================
setlocal
cd /d %~dp0

set JMETER=D:\app\jmeter\apache-jmeter-5.6.3\bin\jmeter.bat
set SCEN=%~1
if "%SCEN%"=="" (
  echo Usage: run_ladder.bat ^<scenario^> ^[tiers^]
  echo   scenario: list_api ^| video_download ^| mixed_browse
  exit /b 1
)

set TIERS=%~2
if "%TIERS%"=="" (
  if "%SCEN%"=="mixed_browse" (set TIERS=20 50 100) else (set TIERS=10 50 100 200)
)

for %%t in (%TIERS%) do (
  echo.
  echo ============================================================
  echo  Running %SCEN% with %%t threads ...
  echo ============================================================
  rd /s /q report_%SCEN%_%%t 2>nul
  call %JMETER% -n -t %SCEN%_concurrency.jmx -Jthreads=%%t -l result_%SCEN%_%%t.jtl -e -o report_%SCEN%_%%t
)

echo.
echo Done. Open report_%SCEN%_^<tier^>/index.html for the HTML report of each tier.
endlocal
