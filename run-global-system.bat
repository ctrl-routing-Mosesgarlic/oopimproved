@echo off
setlocal enabledelayedexpansion

REM ========================================================================
REM Drink Business RMI Global System Management Script (Windows)
REM Supports worldwide deployment with public IP detection and global connectivity
REM ========================================================================

REM Color codes for Windows
set RED=0C
set GREEN=0A
set YELLOW=0E
set BLUE=09
set CYAN=0B
set WHITE=0F

REM Log files
set ERROR_LOG=errors.log
set RECORD_LOG=record.log
set GLOBAL_SERVERS_FILE=global-servers.conf

REM Initialize log files
echo === Drink Business RMI Global System Started at %date% %time% === >> %RECORD_LOG%
echo === Global Error Log Started at %date% %time% === >> %ERROR_LOG%

REM Function to log messages
:log_message
set level=%1
set message=%~2
set timestamp=%date% %time%
if "%level%"=="ERROR" (
    echo [%timestamp%] ERROR: %message% >> %ERROR_LOG%
    color %RED%
    echo ERROR: %message%
    color %WHITE%
) else (
    echo [%timestamp%] %level%: %message% >> %RECORD_LOG%
    if "%level%"=="SUCCESS" color %GREEN%
    if "%level%"=="INFO" color %CYAN%
    if "%level%"=="WARN" color %YELLOW%
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

REM Function to get public IP address
:get_public_ip
call :log_message "INFO" "Detecting public IP address..."
set public_ip=

REM Try multiple services for public IP detection
curl -s --connect-timeout 5 ifconfig.me > temp_ip.txt 2>nul
if %errorlevel% equ 0 (
    set /p public_ip=<temp_ip.txt
    del temp_ip.txt >nul 2>&1
    goto :public_ip_found
)

curl -s --connect-timeout 5 ipinfo.io/ip > temp_ip.txt 2>nul
if %errorlevel% equ 0 (
    set /p public_ip=<temp_ip.txt
    del temp_ip.txt >nul 2>&1
    goto :public_ip_found
)

curl -s --connect-timeout 5 api.ipify.org > temp_ip.txt 2>nul
if %errorlevel% equ 0 (
    set /p public_ip=<temp_ip.txt
    del temp_ip.txt >nul 2>&1
    goto :public_ip_found
)

REM If all services fail
call :log_message "WARN" "Could not detect public IP automatically"
set public_ip=UNKNOWN
goto :eof

:public_ip_found
call :log_message "SUCCESS" "Public IP detected: %public_ip%"
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
call :get_public_ip

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

echo %server_name%,%server_ip%,%server_port%,%location%,%date% %time% >> %GLOBAL_SERVERS_FILE%
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

if not exist %GLOBAL_SERVERS_FILE% (
    echo No global servers registered yet.
    echo.
    goto :eof
)

set counter=1
for /f "tokens=1,2,3,4,5 delims=," %%a in (%GLOBAL_SERVERS_FILE%) do (
    echo !counter!. %%a
    echo    IP: %%b    Port: %%c    Location: %%d
    echo    Registered: %%e
    echo.
    set /a counter+=1
)
goto :eof

REM Function to select global server
:select_global_server
call :show_global_servers
if not exist %GLOBAL_SERVERS_FILE% (
    echo No servers available. Please register servers first.
    pause
    goto :eof
)

echo Enter server number to connect to:
set /p server_choice=Choice: 

set counter=1
for /f "tokens=1,2,3,4,5 delims=," %%a in (%GLOBAL_SERVERS_FILE%) do (
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
echo 1. Local Network Access (LAN) - IP: %ip%
echo 2. Global Internet Access (WAN) - IP: %public_ip%
echo 3. Manual IP Entry
echo.
set /p ip_choice=Enter choice (1-3): 

if "%ip_choice%"=="1" (
    set server_ip=%ip%
    set deployment_type=Local
) else if "%ip_choice%"=="2" (
    set server_ip=%public_ip%
    set deployment_type=Global
) else if "%ip_choice%"=="3" (
    set /p server_ip=Enter server IP address: 
    set deployment_type=Manual
) else (
    echo Invalid choice. Using local IP.
    set server_ip=%ip%
    set deployment_type=Local
)

echo.
set /p location=Enter server location (e.g., USA, UK, Kenya): 

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
mvn compile exec:java -Dexec.mainClass="com.drinks.rmi.server.HQServer" -Dexec.args="config/hq-keystore.jks password123" 2>>%ERROR_LOG%

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
echo 1. Local Network Access (LAN) - IP: %ip%
echo 2. Global Internet Access (WAN) - IP: %public_ip%
echo 3. Manual IP Entry
echo.
set /p ip_choice=Enter choice (1-3): 

if "%ip_choice%"=="1" (
    set server_ip=%ip%
    set deployment_type=Local
) else if "%ip_choice%"=="2" (
    set server_ip=%public_ip%
    set deployment_type=Global
) else if "%ip_choice%"=="3" (
    set /p server_ip=Enter server IP address: 
    set deployment_type=Manual
) else (
    echo Invalid choice. Using local IP.
    set server_ip=%ip%
    set deployment_type=Local
)

echo.
set /p location=Enter server location (e.g., Kenya, Uganda, Tanzania): 

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
java -cp target/drink-business-rmi-1.0.0.jar com.drinks.rmi.server.BranchServer %branch_name% %branch_port% 2>>%ERROR_LOG%

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
set /p client_location=Enter your location (e.g., UK, India, South Africa): 

REM Clean and install project
call :clean_install_project
if %errorlevel% neq 0 goto :end

call :log_message "INFO" "Starting Global GUI Client from %client_location%"
call :log_message "INFO" "Connecting to: %target_server_name% at %target_server_ip%:%target_server_port%"

REM Set system properties for global connection
set JAVA_OPTS=-Djava.rmi.server.hostname=%target_server_ip% -Drmi.server.host=%target_server_ip% -Djava.net.preferIPv4Stack=true

REM Start GUI Client
mvn javafx:run -Drmi.server.host=%target_server_ip% 2>>%ERROR_LOG%

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

REM Main menu
:main_menu
cls
color %BLUE%
echo ========================================
echo    DRINK BUSINESS RMI GLOBAL SYSTEM
echo         Windows Management Script
echo ========================================
color %WHITE%
echo.
echo Global Deployment Options:
echo 1. Run Global HQ Server (SSL-secured, worldwide access)
echo 2. Run Global Branch Server (choose branch + global IP)
echo 3. Run Global GUI Client (connect to any global server)
echo 4. Show Network Information
echo 5. Show Registered Global Servers
echo 6. Global Deployment Guide
echo 7. Exit
echo.
set /p choice=Enter your choice (1-7): 

if "%choice%"=="1" call :run_global_hq_server
if "%choice%"=="2" call :run_global_branch_server
if "%choice%"=="3" call :run_global_gui_client
if "%choice%"=="4" call :show_network_info & pause
if "%choice%"=="5" call :show_global_servers & pause
if "%choice%"=="6" call :show_global_guide
if "%choice%"=="7" goto :end

if not "%choice%"=="7" goto :main_menu

:end
call :log_message "INFO" "Global RMI System Management Script ended"
color %WHITE%
echo.
echo Thank you for using the Drink Business RMI Global System!
echo Check logs: %RECORD_LOG% and %ERROR_LOG%
pause
