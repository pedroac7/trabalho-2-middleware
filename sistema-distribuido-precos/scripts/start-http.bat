@echo off
setlocal

set "SCRIPT_DIR=%~dp0"

start "GATEWAY_MIDDLEWARE" cmd /k ""%SCRIPT_DIR%run-module.bat" "GATEWAY_MIDDLEWARE" "api-gateway" "gateway.Main" 8080 9090 100 http middleware"
start "VALIDADOR1_MIDDLEWARE" cmd /k ""%SCRIPT_DIR%run-module.bat" "VALIDADOR1_MIDDLEWARE" "validador-precos" "validador.Main" 8081 127.0.0.1 9090 middleware"
start "VALIDADOR2_MIDDLEWARE" cmd /k ""%SCRIPT_DIR%run-module.bat" "VALIDADOR2_MIDDLEWARE" "validador-precos" "validador.Main" 8084 127.0.0.1 9090 middleware"
start "REPOSITORIO1_MIDDLEWARE" cmd /k ""%SCRIPT_DIR%run-module.bat" "REPOSITORIO1_MIDDLEWARE" "repositorio-precos" "repositorio.Main" 8082 127.0.0.1 9090 middleware"
start "REPOSITORIO2_MIDDLEWARE" cmd /k ""%SCRIPT_DIR%run-module.bat" "REPOSITORIO2_MIDDLEWARE" "repositorio-precos" "repositorio.Main" 8083 127.0.0.1 9090 middleware"