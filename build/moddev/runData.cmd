@echo off
setlocal
for /f "tokens=2 delims=:." %%x in ('chcp') do set _codepage=%%x
chcp 65001>nul
cd D:\MC Mods Custom\Satisfactory Ore Nodes\run
"C:\Program Files\Eclipse Adoptium\jdk-21.0.6.7-hotspot\bin\java.exe" "@D:\MC Mods Custom\Satisfactory Ore Nodes\build\moddev\dataRunClasspath.txt" "@D:\MC Mods Custom\Satisfactory Ore Nodes\build\moddev\dataRunVmArgs.txt" "-Dfml.modFolders=satisfactory_ore_nodes%%%%D:\MC Mods Custom\Satisfactory Ore Nodes\build\classes\java\main;satisfactory_ore_nodes%%%%D:\MC Mods Custom\Satisfactory Ore Nodes\build\resources\main" net.neoforged.devlaunch.Main "@D:\MC Mods Custom\Satisfactory Ore Nodes\build\moddev\dataRunProgramArgs.txt"
if not ERRORLEVEL 0 (  echo Minecraft failed with exit code %ERRORLEVEL%  pause)
chcp %_codepage%>nul
endlocal