@echo off
setlocal

set "SCRIPT_DIR=%~dp0"

start "GATEWAY_HTTP" cmd /k ""%SCRIPT_DIR%run-module.bat" "GATEWAY_HTTP" "api-gateway" "gateway.Main" http 8080 9090 100 http"
start "VALIDADOR1_HTTP" cmd /k ""%SCRIPT_DIR%run-module.bat" "VALIDADOR1_HTTP" "validador-precos" "validador.Main" http 8081 127.0.0.1 9090"
start "VALIDADOR2_HTTP" cmd /k ""%SCRIPT_DIR%run-module.bat" "VALIDADOR2_HTTP" "validador-precos" "validador.Main" http 8084 127.0.0.1 9090"
start "REPOSITORIO1_HTTP" cmd /k ""%SCRIPT_DIR%run-module.bat" "REPOSITORIO1_HTTP" "repositorio-precos" "repositorio.Main" http 8082 127.0.0.1 9090"
start "REPOSITORIO2_HTTP" cmd /k ""%SCRIPT_DIR%run-module.bat" "REPOSITORIO2_HTTP" "repositorio-precos" "repositorio.Main" http 8083 127.0.0.1 9090"
