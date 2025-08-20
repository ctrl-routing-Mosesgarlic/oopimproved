@echo off
setlocal enabledelayedexpansion

REM Drink Business RMI System Management Script (Windows)
REM This script manages the entire RMI system with dynamic IP detection

REM Log files
set ERROR_LOG=errors.log
set RECORD_LOG=record.log

REM Initialize log files
echo === Drink Business RMI System Started at %date% %time% === >> %RECORD_LOG%
echo === Error Log Started at %date% %time% === >> %ERROR_LOG%

REM Function to log messages
:log_message
set level=%1
set message=%2
set timestamp=%date% %time%
if "%level%"=="ERROR" (
    echo [%timestamp%] ERROR: %message% >> %ERROR_LOG%
    echo ERROR: %message%
) else (
    echo [%timestamp%] %level%: %message% >> %RECORD_LOG%
    echo %level%: %message%
)
goto :eof

REM Function to get local IP address
:get_local_ip
for /f "tokens=2 delims=:" %%i in ('ipconfig ^| findstr /i "IPv4"') do (
    set ip=%%i
    set ip=!ip: =!
    if not "!ip!"=="127.0.0.1" goto :ip_found
)
set ip=localhost
call :log_message "WARN" "Could not detect network IP, using localhost"
:ip_found
goto :eof

REM Function to clean and install the project
:clean_install_project
call :log_message "INFO" "Cleaning and installing project..."
mvn clean install -DskipTests >nul 2>>%ERROR_LOG%
if %errorlevel% equ 0 (
    call :log_message "INFO" "Project cleaned and installed successfully"
    exit /b 0
) else (
    call :log_message "ERROR" "Failed to clean and install project"
    exit /b 1
)

REM Function to compile the project
:compile_project
call :log_message "INFO" "Compiling project..."
mvn compile >nul 2>>%ERROR_LOG%
if %errorlevel% equ 0 (
    call :log_message "INFO" "Project compiled successfully"
    exit /b 0
) else (
    call :log_message "ERROR" "Failed to compile project"
    exit /b 1
)

REM Function to package the project
:package_project
call :log_message "INFO" "Packaging project..."
mvn package -DskipTests >nul 2>>%ERROR_LOG%
if %errorlevel% equ 0 (
    call :log_message "INFO" "Project packaged successfully"
    exit /b 0
) else (
    call :log_message "ERROR" "Failed to package project"
    exit /b 1
)

REM Function to run database seeder
:run_seeder
call :log_message "INFO" "Running database seeder..."
java -cp target/drink-business-rmi-1.0.0.jar com.drinks.rmi.util.DatabaseSeeder >>%RECORD_LOG% 2>>%ERROR_LOG%
if %errorlevel% equ 0 (
    call :log_message "INFO" "Database seeded successfully"
    exit /b 0
) else (
    call :log_message "ERROR" "Failed to seed database"
    exit /b 1
)

REM Function to run HQ Server
:run_hq_server
set server_ip=%1
call :log_message "INFO" "Starting HQ Server on IP: %server_ip%"
set JAVA_OPTS=-Djava.rmi.server.hostname=%server_ip%
mvn compile exec:java -Dexec.mainClass="com.drinks.rmi.server.HQServer" -Dexec.args="config/hq-keystore.jks password123" -Djava.rmi.server.hostname=%server_ip% >>%RECORD_LOG% 2>>%ERROR_LOG%
goto :eof

REM Function to run Branch Server
:run_branch_server
set branch_name=%1
set port=%2
set server_ip=%3
call :log_message "INFO" "Starting %branch_name% Branch Server on %server_ip%:%port%"
set JAVA_OPTS=-Djava.rmi.server.hostname=%server_ip%
java -cp target/drink-business-rmi-1.0.0.jar -Djava.rmi.server.hostname=%server_ip% com.drinks.rmi.server.BranchServer %branch_name% %port% >>%RECORD_LOG% 2>>%ERROR_LOG%
goto :eof

REM Function to run GUI
:run_gui
set server_ip=%1
call :log_message "INFO" "Starting GUI client connecting to server: %server_ip%"
set JAVA_OPTS=-Djava.rmi.server.hostname=%server_ip%
mvn javafx:run -Djava.rmi.server.hostname=%server_ip% >>%RECORD_LOG% 2>>%ERROR_LOG%
goto :eof

REM Function to select branch and run it directly
:select_and_run_branch
set current_ip=%1
echo Select Branch to run:
echo 1) Nairobi (Port: 1103)
echo 2) Kisumu (Port: 1102)
echo 3) Mombasa (Port: 1101)
echo 4) Nakuru (Custom HQ Server)
echo.
set /p choice="Enter your choice (1-4): "

if "%choice%"=="1" (
    call :log_message "INFO" "Starting Nairobi Branch Server on port 1103"
    call :run_branch_server "Nairobi" "1103" "%current_ip%"
) else if "%choice%"=="2" (
    call :log_message "INFO" "Starting Kisumu Branch Server on port 1102"
    call :run_branch_server "Kisumu" "1102" "%current_ip%"
) else if "%choice%"=="3" (
    call :log_message "INFO" "Starting Mombasa Branch Server on port 1101"
    call :run_branch_server "Mombasa" "1101" "%current_ip%"
) else if "%choice%"=="4" (
    call :log_message "INFO" "Starting Nakuru as HQ Server"
    call :run_hq_server "%current_ip%"
) else (
    call :log_message "ERROR" "Invalid branch selection"
    exit /b 1
)
goto :eof

REM Function to select server for GUI connection
:select_server_for_gui
set current_ip=%1
echo Select Server to connect to:
echo 1) Local HQ Server (%current_ip%:1099)
echo 2) Nairobi Branch (%current_ip%:1103)
echo 3) Kisumu Branch (%current_ip%:1102)
echo 4) Mombasa Branch (%current_ip%:1101)
echo 5) Custom IP Address
echo.
set /p choice="Enter your choice (1-5): "

if "%choice%"=="1" (
    set server_result=%current_ip%
) else if "%choice%"=="2" (
    set server_result=%current_ip%
) else if "%choice%"=="3" (
    set server_result=%current_ip%
) else if "%choice%"=="4" (
    set server_result=%current_ip%
) else if "%choice%"=="5" (
    set /p server_result="Enter custom IP address: "
) else (
    call :log_message "ERROR" "Invalid server selection"
    exit /b 1
)
goto :eof

REM Main menu function
:show_main_menu
echo.
echo === Drink Business RMI System Manager ===
echo 1) Run HQ Server
echo 2) Run Branch Server
echo 3) Run GUI Client
echo 4) Run Database Seeder Only
echo 5) Exit
echo.
goto :eof

REM Main script execution
:main

REM Check if Maven is installed
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    call :log_message "ERROR" "Maven is not installed or not in PATH"
    pause
    exit /b 1
)

REM Check if Java is installed
where java >nul 2>nul
if %errorlevel% neq 0 (
    call :log_message "ERROR" "Java is not installed or not in PATH"
    pause
    exit /b 1
)

REM Get current IP
call :get_local_ip
call :log_message "INFO" "Detected IP address: %ip%"
set CURRENT_IP=%ip%

REM Clean, install, compile and package project first
call :clean_install_project
if %errorlevel% neq 0 exit /b 1

call :compile_project
if %errorlevel% neq 0 exit /b 1

call :package_project
if %errorlevel% neq 0 exit /b 1

:menu_loop
call :show_main_menu
set /p main_choice="Enter your choice (1-5): "

if "%main_choice%"=="1" (
    REM Run HQ Server
    set /p run_seeder_choice="Do you want to run the database seeder first? (y/n): "
    
    if /i "%run_seeder_choice%"=="y" (
        call :run_seeder
        if !errorlevel! neq 0 (
            call :log_message "ERROR" "Seeder failed, but continuing with server startup..."
        )
    )
    
    call :log_message "INFO" "Starting HQ Server..."
    call :run_hq_server "%CURRENT_IP%"
    
) else if "%main_choice%"=="2" (
    REM Run Branch Server
    set /p run_seeder_choice="Do you want to run the database seeder first? (y/n): "
    
    if /i "%run_seeder_choice%"=="y" (
        call :run_seeder
        if !errorlevel! neq 0 (
            call :log_message "ERROR" "Seeder failed, but continuing with server startup..."
        )
    )
    
    REM Use the new simplified branch selection function
    call :select_and_run_branch "%CURRENT_IP%"
    
) else if "%main_choice%"=="3" (
    REM Run GUI Client
    call :select_server_for_gui "%CURRENT_IP%"
    call :run_gui "!server_result!"
    
) else if "%main_choice%"=="4" (
    REM Run Database Seeder Only
    call :run_seeder
    
) else if "%main_choice%"=="5" (
    REM Exit
    call :log_message "INFO" "System manager exiting..."
    exit /b 0
    
) else (
    call :log_message "ERROR" "Invalid choice. Please try again."
)

echo.
echo Press Enter to return to main menu...
pause >nul
goto menu_loop

REM Start main execution
call :main
