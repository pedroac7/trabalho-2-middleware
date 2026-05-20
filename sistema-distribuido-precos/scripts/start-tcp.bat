@echo off
setlocal

set "SCRIPT_DIR=%~dp0"

start "GATEWAY_TCP" cmd /k ""%SCRIPT_DIR%run-module.bat" "GATEWAY_TCP" "api-gateway" "gateway.Main" tcp 8080 9090 100 tcp"
start "VALIDADOR1_TCP" cmd /k ""%SCRIPT_DIR%run-module.bat" "VALIDADOR1_TCP" "validador-precos" "validador.Main" tcp 8081 127.0.0.1 9090"
start "VALIDADOR2_TCP" cmd /k ""%SCRIPT_DIR%run-module.bat" "VALIDADOR2_TCP" "validador-precos" "validador.Main" tcp 8084 127.0.0.1 9090"
start "REPOSITORIO1_TCP" cmd /k ""%SCRIPT_DIR%run-module.bat" "REPOSITORIO1_TCP" "repositorio-precos" "repositorio.Main" tcp 8082 127.0.0.1 9090"
start "REPOSITORIO2_TCP" cmd /k ""%SCRIPT_DIR%run-module.bat" "REPOSITORIO2_TCP" "repositorio-precos" "repositorio.Main" tcp 8083 127.0.0.1 9090"
