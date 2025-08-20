@echo off
setlocal enabledelayedexpansion

REM ========================================================================
REM Drink Business RMI System Management Script (Windows)
REM Combined Local & Global Deployment with Unified Logging
REM Supports both LAN and worldwide deployment scenarios
REM ========================================================================

REM Color codes for Windows
set RED=0C
set GREEN=0A
set YELLOW=0E
set BLUE=09
set CYAN=0B
set PURPLE=0D
set WHITE=0F

REM Log files
set ERROR_LOG=errors.log
set RECORD_LOG=record.log
set GLOBAL_CONFIG=global-servers.conf

REM Initialize log files
echo === Drink Business RMI System Started at %date% %time% === >> %RECORD_LOG%
echo === Error Log Started at %date% %time% === >> %ERROR_LOG%

REM Function to log messages
:log_message
set level=%~1
set message=%~2
set timestamp=%date% %time%
if "%level%"=="ERROR" (
    echo [%timestamp%] ERROR: %message% >> %ERROR_LOG%
    color %RED%
    echo ERROR: %message%
    color %WHITE%
) else if "%level%"=="SUCCESS" (
    echo [%timestamp%] SUCCESS: %message% >> %RECORD_LOG%
    color %GREEN%
    echo SUCCESS: %message%
    color %WHITE%
) else if "%level%"=="WARN" (
    echo [%timestamp%] WARN: %message% >> %RECORD_LOG%
    color %YELLOW%
    echo WARN: %message%
    color %WHITE%
) else if "%level%"=="INFO" (
    echo [%timestamp%] INFO: %message% >> %RECORD_LOG%
    color %CYAN%
    echo INFO: %message%
    color %WHITE%
) else (
    echo [%timestamp%] %level%: %message% >> %RECORD_LOG%
    color %GREEN%
    echo %level%: %message%
    color %WHITE%
)
goto :eof

REM Function to get local IP address
:get_local_ip
for /f "tokens=2 delims=:" %%i in ('ipconfig ^| findstr /i "IPv4"') do (
    set ip=%%i
    set ip=!ip: =!
    if not "!ip!"=="127.0.0.1" (
        if not "!ip!"=="169.254" goto :ip_found
    )
)
set ip=localhost
call :log_message "WARN" "Could not detect network IP, using localhost"
goto :eof
:ip_found
goto :eof

REM Function to get public IP address (completely silent)
:get_public_ip
set public_ip=

REM Try multiple services for public IP detection (silently)
curl -s --connect-timeout 5 ifconfig.me > temp_ip.txt 2>nul
if %errorlevel% equ 0 (
    set /p public_ip=<temp_ip.txt
    del temp_ip.txt >nul 2>&1
    if not "%public_ip%"=="" goto :eof
)

curl -s --connect-timeout 5 ipinfo.io/ip > temp_ip.txt 2>nul
if %errorlevel% equ 0 (
    set /p public_ip=<temp_ip.txt
    del temp_ip.txt >nul 2>&1
    if not "%public_ip%"=="" goto :eof
)

curl -s --connect-timeout 5 api.ipify.org > temp_ip.txt 2>nul
if %errorlevel% equ 0 (
    set /p public_ip=<temp_ip.txt
    del temp_ip.txt >nul 2>&1
    if not "%public_ip%"=="" goto :eof
)

REM If all services fail
set public_ip=
goto :eof

REM Function to get public IP with logging (for display purposes)
:get_public_ip_with_log
call :log_message "INFO" "Detecting public IP address..."
call :get_public_ip
if not "%public_ip%"=="" (
    call :log_message "SUCCESS" "Public IP detected: %public_ip%"
) else (
    call :log_message "WARN" "Could not detect public IP address"
)
goto :eof

REM Function to display network information
:show_network_info
echo.
color %CYAN%
echo ========================================
echo          NETWORK INFORMATION
echo ========================================
color %WHITE%

call :get_local_ip
call :get_public_ip_with_log

echo Local IP Address:  %ip%
echo Public IP Address: %public_ip%
echo.
echo Network Configuration Required:
echo - Router Port Forwarding: Forward external ports to this machine
echo - Firewall Rules: Allow inbound TCP connections on RMI ports
echo - Public IP Access: Ensure %public_ip% is accessible from internet
echo.
goto :eof

REM Function to save global server configuration
:save_global_server
set server_name=%~1
set server_ip=%~2
set server_port=%~3
set location=%~4

echo %server_name%,%server_ip%,%server_port%,%location%,%date% %time% >> %GLOBAL_CONFIG%
call :log_message "INFO" "Global server registered: %server_name% at %server_ip%:%server_port% (%location%)"
goto :eof

REM Function to display registered global servers
:show_global_servers
echo.
color %YELLOW%
echo ========================================
echo        REGISTERED GLOBAL SERVERS
echo ========================================
color %WHITE%

if not exist %GLOBAL_CONFIG% (
    echo No global servers registered yet.
    echo.
    goto :eof
)

set counter=1
for /f "tokens=1,2,3,4,5 delims=," %%a in (%GLOBAL_CONFIG%) do (
    echo !counter!. %%a
    echo    IP: %%b    Port: %%c    Location: %%d
    echo    Registered: %%e
    echo.
    set /a counter+=1
)
goto :eof

REM Function to clean and install the project
:clean_install_project
call :log_message "INFO" "Cleaning and installing project..."
mvn clean install -DskipTests >nul 2>>%ERROR_LOG%
if %errorlevel% equ 0 (
    call :log_message "SUCCESS" "Project cleaned and installed successfully"
    exit /b 0
) else (
    call :log_message "ERROR" "Failed to clean and install project"
    exit /b 1
)

REM Function to run database seeder
:run_database_seeder
call :log_message "INFO" "Running database seeder..."
java -cp target/drink-business-rmi-1.0.0.jar com.drinks.rmi.util.DatabaseSeeder 2>>%ERROR_LOG%
if %errorlevel% equ 0 (
    call :log_message "SUCCESS" "Database seeded successfully"
) else (
    call :log_message "ERROR" "Failed to seed database"
)
goto :eof

REM Function to run HQ server
:run_hq_server
set server_ip=%~1
call :log_message "INFO" "Starting HQ Server on %server_ip%:1099"
set JAVA_OPTS=-Djava.rmi.server.hostname=%server_ip%
mvn compile exec:java -Dexec.mainClass="com.drinks.rmi.server.HQServer" -Dexec.args="config/hq-keystore.jks password123" 2>>%ERROR_LOG%
goto :eof

REM Function to run branch server
:run_branch_server
set branch_name=%~1
set branch_port=%~2
set server_ip=%~3
call :log_message "INFO" "Starting %branch_name% Branch Server on %server_ip%:%branch_port%"
set JAVA_OPTS=-Djava.rmi.server.hostname=%server_ip%
java -cp target/drink-business-rmi-1.0.0.jar com.drinks.rmi.server.BranchServer %branch_name% %branch_port% 2>>%ERROR_LOG%
goto :eof

REM Function to run GUI client
:run_gui_client
set server_ip=%~1
call :log_message "INFO" "Starting GUI client connecting to %server_ip%"
set JAVA_OPTS=-Djava.rmi.server.hostname=%server_ip%
mvn javafx:run 2>>%ERROR_LOG%
goto :eof

REM Function to select and run branch server
:select_and_run_branch
set current_ip=%~1
echo.
color %BLUE%
echo Select Branch to run:
echo 1) Nairobi (Port: 1103)
echo 2) Kisumu (Port: 1102)
echo 3) Mombasa (Port: 1101)
echo 4) Nakuru (Port: 1100)
color %WHITE%
echo.
set /p choice=Enter your choice (1-4): 

if "%choice%"=="1" (
    call :run_branch_server "Nairobi" "1103" "%current_ip%"
) else if "%choice%"=="2" (
    call :run_branch_server "Kisumu" "1102" "%current_ip%"
) else if "%choice%"=="3" (
    call :run_branch_server "Mombasa" "1101" "%current_ip%"
) else if "%choice%"=="4" (
    call :run_branch_server "Nakuru" "1100" "%current_ip%"
) else (
    call :log_message "ERROR" "Invalid branch selection"
)
goto :eof

REM Function to select server for GUI connection
:select_gui_server
echo.
color %BLUE%
echo Select server to connect to:
echo 1) HQ Server (Port: 1099)
echo 2) Nairobi Branch (Port: 1103)
echo 3) Kisumu Branch (Port: 1102)
echo 4) Mombasa Branch (Port: 1101)
echo 5) Nakuru Branch (Port: 1100)
echo 6) Custom IP Address
color %WHITE%
echo.
set /p choice=Enter your choice (1-6): 

call :get_local_ip

if "%choice%"=="1" (
    call :run_gui_client "%ip%"
) else if "%choice%"=="2" (
    call :run_gui_client "%ip%"
) else if "%choice%"=="3" (
    call :run_gui_client "%ip%"
) else if "%choice%"=="4" (
    call :run_gui_client "%ip%"
) else if "%choice%"=="5" (
    call :run_gui_client "%ip%"
) else if "%choice%"=="6" (
    set /p custom_ip=Enter server IP address: 
    call :run_gui_client "!custom_ip!"
) else (
    call :log_message "ERROR" "Invalid server selection"
)
goto :eof

REM Function to run global HQ server
:run_global_hq_server
echo.
color %GREEN%
echo ========================================
echo         GLOBAL HQ SERVER SETUP
echo ========================================
color %WHITE%

call :show_network_info

echo Select IP configuration for HQ Server:
echo 1. Local Network Access (LAN)
echo 2. Global Internet Access (WAN)
echo 3. Manual IP Entry
echo.
set /p ip_choice=Enter choice (1-3): 

if "%ip_choice%"=="1" (
    call :get_local_ip
    set server_ip=%ip%
    set deployment_type=Local
) else if "%ip_choice%"=="2" (
    call :get_public_ip
    set server_ip=%public_ip%
    set deployment_type=Global
) else if "%ip_choice%"=="3" (
    set /p server_ip=Enter server IP address: 
    set deployment_type=Manual
) else (
    echo Invalid choice. Using local IP.
    call :get_local_ip
    set server_ip=%ip%
    set deployment_type=Local
)

echo.
set /p location=Enter server location (e.g., your country/city): 

REM Ask about database seeding
echo.
echo Do you want to run the database seeder before starting the server? (y/n)
set /p seed_choice=Choice: 
if /i "%seed_choice%"=="y" call :run_database_seeder

REM Clean and install project
call :clean_install_project
if %errorlevel% neq 0 goto :end

REM Save global server configuration
call :save_global_server "HQ_Server" "%server_ip%" "1099" "%location%"

REM Set Java options for global deployment
set JAVA_OPTS=-Djava.rmi.server.hostname=%server_ip% -Djava.net.preferIPv4Stack=true -Djavax.net.ssl.keyStore=config/hq-keystore.jks -Djavax.net.ssl.keyStorePassword=password123

call :log_message "INFO" "Starting Global HQ Server on %server_ip%:1099 (%deployment_type% deployment)"
call :log_message "INFO" "Location: %location%"
call :log_message "INFO" "Clients worldwide can connect to: rmi://%server_ip%:1099"

REM Start HQ Server
call :run_hq_server "%server_ip%"
goto :eof

REM Function to run global branch server
:run_global_branch_server
echo.
color %GREEN%
echo ========================================
echo       GLOBAL BRANCH SERVER SETUP
echo ========================================
color %WHITE%

call :show_network_info

echo Select branch to run globally:
echo 1. Nairobi Branch (Port 1103)
echo 2. Kisumu Branch (Port 1102)
echo 3. Mombasa Branch (Port 1101)
echo 4. Nakuru Branch (Port 1100)
echo.
set /p branch_choice=Enter choice (1-4): 

if "%branch_choice%"=="1" (
    set branch_name=Nairobi
    set branch_port=1103
) else if "%branch_choice%"=="2" (
    set branch_name=Kisumu
    set branch_port=1102
) else if "%branch_choice%"=="3" (
    set branch_name=Mombasa
    set branch_port=1101
) else if "%branch_choice%"=="4" (
    set branch_name=Nakuru
    set branch_port=1100
) else (
    echo Invalid choice. Defaulting to Nairobi.
    set branch_name=Nairobi
    set branch_port=1103
)

echo Select IP configuration for %branch_name% Branch Server:
echo 1. Local Network Access (LAN)
echo 2. Global Internet Access (WAN)
echo 3. Manual IP Entry
echo.
set /p ip_choice=Enter choice (1-3): 

if "%ip_choice%"=="1" (
    call :get_local_ip
    set server_ip=%ip%
    set deployment_type=Local
) else if "%ip_choice%"=="2" (
    call :get_public_ip
    set server_ip=%public_ip%
    set deployment_type=Global
) else if "%ip_choice%"=="3" (
    set /p server_ip=Enter server IP address: 
    set deployment_type=Manual
) else (
    echo Invalid choice. Using local IP.
    call :get_local_ip
    set server_ip=%ip%
    set deployment_type=Local
)

echo.
set /p location=Enter server location (e.g., your country/city): 

REM Clean and install project
call :clean_install_project
if %errorlevel% neq 0 goto :end

REM Save global server configuration
call :save_global_server "%branch_name%_Branch" "%server_ip%" "%branch_port%" "%location%"

REM Set Java options for global deployment
set JAVA_OPTS=-Djava.rmi.server.hostname=%server_ip% -Djava.net.preferIPv4Stack=true

call :log_message "INFO" "Starting Global %branch_name% Branch Server on %server_ip%:%branch_port% (%deployment_type% deployment)"
call :log_message "INFO" "Location: %location%"
call :log_message "INFO" "Clients worldwide can connect to: rmi://%server_ip%:%branch_port%"

REM Start Branch Server
call :run_branch_server "%branch_name%" "%branch_port%" "%server_ip%"
goto :eof

REM Function to run global GUI client
:run_global_gui_client
echo.
color %GREEN%
echo ========================================
echo        GLOBAL GUI CLIENT SETUP
echo ========================================
color %WHITE%

echo Select server connection method:
echo 1. Connect to registered global server
echo 2. Manual server IP entry
echo 3. Show network information
echo.
set /p connect_choice=Enter choice (1-3): 

if "%connect_choice%"=="1" (
    call :select_global_server
    if defined selected_server_ip (
        set target_server_ip=%selected_server_ip%
        set target_server_port=%selected_server_port%
        set target_server_name=%selected_server_name%
    ) else (
        goto :run_global_gui_client
    )
) else if "%connect_choice%"=="2" (
    set /p target_server_ip=Enter server IP address: 
    set /p target_server_port=Enter server port (1099 for HQ, 1100-1103 for branches): 
    set target_server_name=Manual_Server
) else if "%connect_choice%"=="3" (
    call :show_network_info
    pause
    goto :run_global_gui_client
) else (
    echo Invalid choice. Please try again.
    goto :run_global_gui_client
)

echo.
set /p client_location=Enter your location (e.g., your country/city): 

REM Clean and install project
call :clean_install_project
if %errorlevel% neq 0 goto :end

call :log_message "INFO" "Starting Global GUI Client from %client_location%"
call :log_message "INFO" "Connecting to: %target_server_name% at %target_server_ip%:%target_server_port%"

REM Set system properties for global connection
set JAVA_OPTS=-Djava.rmi.server.hostname=%target_server_ip% -Djava.net.preferIPv4Stack=true

REM Start GUI Client
call :run_gui_client "%target_server_ip%"
goto :eof

REM Function to select global server
:select_global_server
call :show_global_servers

if not exist %GLOBAL_CONFIG% (
    echo No servers available. Please register servers first.
    pause
    goto :eof
)

echo Enter server number to connect to:
set /p server_choice=Choice: 

set counter=1
for /f "tokens=1,2,3,4,5 delims=," %%a in (%GLOBAL_CONFIG%) do (
    if !counter! equ %server_choice% (
        set selected_server_name=%%a
        set selected_server_ip=%%b
        set selected_server_port=%%c
        set selected_location=%%d
        goto :server_selected
    )
    set /a counter+=1
)

echo Invalid selection. Please try again.
pause
goto :select_global_server

:server_selected
call :log_message "INFO" "Selected server: %selected_server_name% at %selected_server_ip%:%selected_server_port%"
goto :eof

REM Function to show global deployment guide
:show_global_guide
echo.
color %CYAN%
echo ========================================
echo       GLOBAL DEPLOYMENT GUIDE
echo ========================================
color %WHITE%
echo.
echo NETWORK REQUIREMENTS FOR GLOBAL ACCESS:
echo.
echo 1. ROUTER CONFIGURATION (Port Forwarding):
echo    - HQ Server: Forward external port 1099 to this machine
echo    - Branch Servers: Forward ports 1100-1103 to branch machines
echo.
echo 2. FIREWALL CONFIGURATION:
echo    Windows: netsh advfirewall firewall add rule name="RMI Ports" dir=in action=allow protocol=TCP localport=1099-1103
echo.
echo 3. PUBLIC IP REQUIREMENTS:
echo    - Static Public IP (recommended for servers)
echo    - Dynamic DNS service (for changing IPs)
echo    - Cloud hosting (AWS, Azure, Google Cloud)
echo.
echo 4. SECURITY CONSIDERATIONS:
echo    - HQ Server uses SSL encryption (port 1099)
echo    - Consider VPN for additional security
echo    - Monitor connection logs regularly
echo.
echo 5. GLOBAL CONNECTION EXAMPLES:
echo    - USA HQ Server: rmi://123.456.789.012:1099
echo    - Kenya Branch: rmi://234.567.890.123:1103
echo    - Client from UK connecting to USA HQ
echo    - Client from India connecting to Kenya Branch
echo.
echo 6. TROUBLESHOOTING:
echo    - Test port accessibility: telnet SERVER_IP PORT
echo    - Check firewall: netsh advfirewall show allprofiles
echo    - Verify router port forwarding configuration
echo.
pause
goto :eof

REM Main menu function
:show_main_menu
cls
color %BLUE%
echo ========================================
echo    DRINK BUSINESS RMI SYSTEM MANAGER
echo   Combined Local ^& Global Deployment
echo ========================================
color %WHITE%
echo.
color %YELLOW%
echo LOCAL DEPLOYMENT OPTIONS:
color %WHITE%
echo 1) Run Local HQ Server
echo 2) Run Local Branch Server
echo 3) Run Local GUI Client
echo 4) Run Database Seeder Only
echo.
color %CYAN%
echo GLOBAL DEPLOYMENT OPTIONS:
color %WHITE%
echo 5) Run Global HQ Server (worldwide access)
echo 6) Run Global Branch Server (worldwide access)
echo 7) Run Global GUI Client (connect worldwide)
echo.
color %PURPLE%
echo INFORMATION ^& UTILITIES:
color %WHITE%
echo 8) Show Network Information
echo 9) Show Registered Global Servers
echo 10) Global Deployment Guide
echo 11) Exit
echo.
goto :eof

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
echo %server_result%
goto :eof

REM Main script execution
:main
call :log_message "INFO" "Drink Business RMI System Manager Started"

:menu_loop
call :show_main_menu
set /p main_choice=Enter your choice (1-11): 

if "%main_choice%"=="1" (
    REM Run Local HQ Server
    call :get_local_ip
    call :log_message "INFO" "Starting Local HQ Server on %ip%"
    
    echo Do you want to run the database seeder first? (y/n)
    set /p seed_choice=Choice: 
    if /i "%seed_choice%"=="y" (
        call :clean_install_project
        if %errorlevel% neq 0 goto :menu_loop
        call :run_database_seeder
    )
    
    call :clean_install_project
    if %errorlevel% neq 0 goto :menu_loop
    call :run_hq_server "%ip%"
) else if "%main_choice%"=="2" (
    REM Run Local Branch Server
    call :get_local_ip
    call :clean_install_project
    if %errorlevel% neq 0 goto :menu_loop
    call :select_and_run_branch "%ip%"
) else if "%main_choice%"=="3" (
    REM Run Local GUI Client
    call :clean_install_project
    if %errorlevel% neq 0 goto :menu_loop
    call :select_gui_server
) else if "%main_choice%"=="4" (
    REM Run Database Seeder Only
    call :clean_install_project
    if %errorlevel% neq 0 goto :menu_loop
    call :run_database_seeder
    pause
) else if "%main_choice%"=="5" (
    REM Run Global HQ Server
    call :run_global_hq_server
) else if "%main_choice%"=="6" (
    REM Run Global Branch Server
    call :run_global_branch_server
) else if "%main_choice%"=="7" (
    REM Run Global GUI Client
    call :run_global_gui_client
) else if "%main_choice%"=="8" (
    REM Show Network Information
    call :show_network_info
    pause
) else if "%main_choice%"=="9" (
    REM Show Registered Global Servers
    call :show_global_servers
    pause
) else if "%main_choice%"=="10" (
    REM Global Deployment Guide
    call :show_global_guide
) else if "%main_choice%"=="11" (
    REM Exit
    call :log_message "INFO" "Drink Business RMI System Manager ended"
    color %GREEN%
    echo Thank you for using the Drink Business RMI System!
    echo Check logs: %RECORD_LOG% and %ERROR_LOG%
    color %WHITE%
    goto :end
) else (
    color %RED%
    echo Invalid choice. Please try again.
    color %WHITE%
    pause
)

goto :menu_loop

:end
pause

REM Run main function
call :main

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
