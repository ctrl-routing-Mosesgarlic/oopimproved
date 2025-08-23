#!/bin/bash

# ========================================================================
# Drink Business RMI System Management Script (Linux/macOS)
# Combined Local & Global Deployment with Unified Logging
# Supports both LAN and worldwide deployment scenarios
# ========================================================================

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Log files
ERROR_LOG="errors.log"
RECORD_LOG="record.log"
GLOBAL_CONFIG="global-servers.conf"

# Initialize log files
echo "=== Drink Business RMI System Started at $(date) ===" >> "$RECORD_LOG"
echo "=== Error Log Started at $(date) ===" >> "$ERROR_LOG"

# Function to log messages
log_message() {
    local level=$1
    local message=$2
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    if [ "$level" == "ERROR" ]; then
        echo "[$timestamp] ERROR: $message" >> "$ERROR_LOG"
        echo -e "${RED}ERROR: $message${NC}"
    elif [ "$level" == "SUCCESS" ]; then
        echo "[$timestamp] SUCCESS: $message" >> "$RECORD_LOG"
        echo -e "${GREEN}SUCCESS: $message${NC}"
    elif [ "$level" == "WARN" ]; then
        echo "[$timestamp] WARN: $message" >> "$RECORD_LOG"
        echo -e "${YELLOW}WARN: $message${NC}"
    elif [ "$level" == "INFO" ]; then
        echo "[$timestamp] INFO: $message" >> "$RECORD_LOG"
        echo -e "${CYAN}INFO: $message${NC}"
    else
        echo "[$timestamp] $level: $message" >> "$RECORD_LOG"
        echo -e "${GREEN}$level: $message${NC}"
    fi
}

# Function to get local IP address
get_local_ip() {
    local ip=""
    
    # Try different methods to get IP
    if command -v ip &> /dev/null; then
        ip=$(ip route get 8.8.8.8 | awk '{print $7; exit}' 2>/dev/null)
    elif command -v ifconfig &> /dev/null; then
        ip=$(ifconfig | grep -E "inet.*broadcast" | awk '{print $2}' | head -1)
    elif command -v hostname &> /dev/null; then
        ip=$(hostname -I | awk '{print $1}')
    fi
    
    # Fallback to localhost if no IP found
    if [ -z "$ip" ] || [ "$ip" == "127.0.0.1" ]; then
        ip="localhost"
        log_message "WARN" "Could not detect network IP, using localhost"
    fi
    
    echo "$ip"
}

# Function to get public IP address (completely silent)
get_public_ip() {
    local public_ip=""
    
    # Try multiple services to get public IP (completely silent)
    for service in "ifconfig.me" "ipinfo.io/ip" "icanhazip.com" "checkip.amazonaws.com" "api.ipify.org"; do
        public_ip=$(curl -s --connect-timeout 5 "$service" 2>/dev/null | tr -d '\n\r' | grep -E '^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$')
        if [[ -n "$public_ip" && $public_ip =~ ^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$ ]]; then
            echo "$public_ip"
            return 0
        fi
    done
    
    echo ""
}

# Function to get public IP with logging (for display purposes)
get_public_ip_with_log() {
    log_message "INFO" "Detecting public IP address..."
    local public_ip=$(get_public_ip)
    if [[ -n "$public_ip" ]]; then
        log_message "SUCCESS" "Public IP detected: $public_ip"
        echo "$public_ip"
    else
        log_message "WARN" "Could not detect public IP address"
        echo ""
    fi
}

# Function to display network information
show_network_info() {
    echo
    echo -e "${CYAN}========================================${NC}"
    echo -e "${CYAN}          NETWORK INFORMATION${NC}"
    echo -e "${CYAN}========================================${NC}"
    
    local local_ip=$(get_local_ip)
    local public_ip=$(get_public_ip_with_log)
    
    echo -e "Local IP Address:  ${GREEN}$local_ip${NC}"
    echo -e "Public IP Address: ${GREEN}$public_ip${NC}"
    echo
    echo "Network Configuration Required:"
    echo "- Router Port Forwarding: Forward external ports to this machine"
    echo "- Firewall Rules: Allow inbound TCP connections on RMI ports"
    echo "- Public IP Access: Ensure $public_ip is accessible from internet"
    echo
}

# Function to save global server configuration
save_global_server() {
    local server_name="$1"
    local server_ip="$2"
    local server_port="$3"
    local location="$4"
    
    echo "$server_name,$server_ip,$server_port,$location,$(date '+%Y-%m-%d %H:%M:%S')" >> "$GLOBAL_CONFIG"
    log_message "INFO" "Global server registered: $server_name at $server_ip:$server_port ($location)"
}

# Function to display registered global servers
show_global_servers() {
    echo
    echo -e "${YELLOW}========================================${NC}"
    echo -e "${YELLOW}        REGISTERED GLOBAL SERVERS${NC}"
    echo -e "${YELLOW}========================================${NC}"
    
    if [ ! -f "$GLOBAL_CONFIG" ]; then
        echo "No global servers registered yet."
        echo
        return
    fi
    
    local counter=1
    while IFS=',' read -r name ip port location timestamp; do
        echo -e "${GREEN}$counter. $name${NC}"
        echo "    IP: $ip    Port: $port    Location: $location"
        echo "    Registered: $timestamp"
        echo
        ((counter++))
    done < "$GLOBAL_CONFIG"
}

# Function to select global server
select_global_server() {
    show_global_servers
    
    if [ ! -f "$GLOBAL_CONFIG" ]; then
        echo "No servers available. Please register servers first."
        read -p "Press Enter to continue..."
        return 1
    fi
    
    echo -n "Enter server number to connect to: "
    read server_choice
    
    local counter=1
    while IFS=',' read -r name ip port location timestamp; do
        if [ "$counter" -eq "$server_choice" ]; then
            selected_server_name="$name"
            selected_server_ip="$ip"
            selected_server_port="$port"
            selected_location="$location"
            log_message "INFO" "Selected server: $name at $ip:$port"
            return 0
        fi
        ((counter++))
    done < "$GLOBAL_CONFIG"
    
    echo "Invalid selection. Please try again."
    return 1
}

# Function to clean and install the project
clean_install_project() {
    log_message "INFO" "Cleaning and installing project..."
    if mvn clean install -DskipTests > /dev/null 2>> "$ERROR_LOG"; then
        log_message "INFO" "Project cleaned and installed successfully"
        return 0
    else
        log_message "ERROR" "Failed to clean and install project"
        return 1
    fi
}

# Function to compile the project
compile_project() {
    log_message "INFO" "Compiling project..."
    if mvn compile > /dev/null 2>> "$ERROR_LOG"; then
        log_message "INFO" "Project compiled successfully"
        return 0
    else
        log_message "ERROR" "Failed to compile project"
        return 1
    fi
}

# Function to package the project
package_project() {
    log_message "INFO" "Packaging project..."
    if mvn package -DskipTests > /dev/null 2>> "$ERROR_LOG"; then
        log_message "INFO" "Project packaged successfully"
        return 0
    else
        log_message "ERROR" "Failed to package project"
        return 1
    fi
}

# Function to run database seeder
run_seeder() {
    log_message "INFO" "Running database seeder..."
    if java -cp target/drink-business-rmi-1.0.0.jar com.drinks.rmi.util.DatabaseSeeder >> "$RECORD_LOG" 2>> "$ERROR_LOG"; then
        log_message "INFO" "Database seeded successfully"
        return 0
    else
        log_message "ERROR" "Failed to seed database"
        return 1
    fi
}

# Function to run HQ Server
run_hq_server() {
    local ip=$1
    log_message "INFO" "Starting HQ Server on IP: $ip"
    
    # Set RMI hostname property
    export JAVA_OPTS="-Djava.rmi.server.hostname=$ip"
    
    mvn compile exec:java -Dexec.mainClass="com.drinks.rmi.server.HQServer" \
        -Dexec.args="config/hq-keystore.jks password123" \
        -Djava.rmi.server.hostname="$ip" >> "$RECORD_LOG" 2>> "$ERROR_LOG"
}

# Function to run Branch Server
run_branch_server() {
    local branch_name=$1
    local port=$2
    local ip=$3
    
    log_message "INFO" "Starting $branch_name Branch Server on $ip:$port"
    
    # Set RMI hostname property
    export JAVA_OPTS="-Djava.rmi.server.hostname=$ip"
    
    java -cp target/drink-business-rmi-1.0.0.jar \
        -Djava.rmi.server.hostname="$ip" \
        com.drinks.rmi.server.BranchServer "$branch_name" "$port" >> "$RECORD_LOG" 2>> "$ERROR_LOG"
}

# Function to run GUI
run_gui() {
    local server_ip=$1
    log_message "INFO" "Starting GUI client connecting to server: $server_ip"
    
    # Set system properties for GUI (both old and new property names for compatibility)
    export JAVA_OPTS="-Djava.rmi.server.hostname=$server_ip -Drmi.server.host=$server_ip"
    
    export RMI_SERVER_HOST="$server_ip"
    mvn javafx:run -Drmi.server.host="$server_ip" -Djava.rmi.server.hostname="$server_ip" -Djavax.net.ssl.keyStore=config/hq-keystore.jks -Djavax.net.ssl.keyStorePassword=securepassword123 -Djavax.net.ssl.trustStore=config/hq-keystore.jks -Djavax.net.ssl.trustStorePassword=securepassword123 >> "$RECORD_LOG" 2>> "$ERROR_LOG"
}

# Function to select branch and run it directly
select_and_run_branch() {
    local current_ip=$1
    
    echo -e "${BLUE}Select Branch to run:${NC}"
    echo "1) Nairobi (Port: 1103)"
    echo "2) Kisumu (Port: 1102)"
    echo "3) Mombasa (Port: 1101)"
    echo "4) Nakuru (Custom HQ Server)"
    
    read -p "Enter your choice (1-4): " choice
    
    case $choice in
        1)
            log_message "INFO" "Starting Nairobi Branch Server on port 1103"
            run_branch_server "Nairobi" "1103" "$current_ip"
            ;;
        2)
            log_message "INFO" "Starting Kisumu Branch Server on port 1102"
            run_branch_server "Kisumu" "1102" "$current_ip"
            ;;
        3)
            log_message "INFO" "Starting Mombasa Branch Server on port 1101"
            run_branch_server "Mombasa" "1101" "$current_ip"
            ;;
        4)
            log_message "INFO" "Starting Nakuru as Branch Server on port 1100"
            run_branch_server "Nakuru" "1100" "$current_ip"
            ;;
        *)
            log_message "ERROR" "Invalid branch selection"
            return 1
            ;;
    esac
}

# Function to select server for GUI connection
select_gui_server() {
    echo -e "${BLUE}Select server to connect to:${NC}"
    echo "1) HQ Server (Port: 1099)"
    echo "2) Nairobi Branch (Port: 1103)"
    echo "3) Kisumu Branch (Port: 1102)"
    echo "4) Mombasa Branch (Port: 1101)"
    echo "5) Nakuru Branch (Port: 1100)"
    echo "6) Custom IP Address"
    
    read -p "Enter your choice (1-6): " choice
    
    # Ask for connection type (local vs external)
    echo
    echo -e "${YELLOW}Select connection type:${NC}"
    echo "1) Local Network (localhost/LAN)"
    echo "2) External/Remote Server (WAN/Internet)"
    read -p "Enter connection type (1-2): " conn_type
    
    local server_ip
    local current_ip=$(get_local_ip)
    
    if [ "$conn_type" == "1" ]; then
        # Local connection - use current machine's IP
        server_ip="$current_ip"
        echo -e "${GREEN}Using local IP: $server_ip${NC}"
    elif [ "$conn_type" == "2" ]; then
        # External connection - ask for remote IP
        read -p "Enter remote server IP address: " server_ip
        echo -e "${GREEN}Using remote IP: $server_ip${NC}"
    else
        echo -e "${RED}Invalid connection type. Defaulting to local.${NC}"
        server_ip="$current_ip"
    fi
    
    case $choice in
        1)
            log_message "INFO" "Connecting GUI to HQ Server at $server_ip:1099"
            run_gui "$server_ip"
            ;;
        2)
            log_message "INFO" "Connecting GUI to Nairobi Branch at $server_ip:1103"
            run_gui "$server_ip"
            ;;
        3)
            log_message "INFO" "Connecting GUI to Kisumu Branch at $server_ip:1102"
            run_gui "$server_ip"
            ;;
        4)
            log_message "INFO" "Connecting GUI to Mombasa Branch at $server_ip:1101"
            run_gui "$server_ip"
            ;;
        5)
            log_message "INFO" "Connecting GUI to Nakuru Branch at $server_ip:1100"
            run_gui "$server_ip"
            ;;
        6)
            if [ "$conn_type" == "2" ]; then
                # For custom IP with external connection, ask for specific IP again
                read -p "Enter custom server IP address: " custom_ip
                server_ip="$custom_ip"
            fi
            log_message "INFO" "Connecting GUI to custom server at $server_ip"
            run_gui "$server_ip"
            ;;
        *)
            log_message "ERROR" "Invalid server selection"
            return 1
            ;;
    esac
}

# Function to run global HQ server
run_global_hq_server() {
    echo
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}         GLOBAL HQ SERVER SETUP${NC}"
    echo -e "${GREEN}========================================${NC}"
    
    show_network_info
    
    echo "Select IP configuration for HQ Server:"
    echo "1. Local Network Access (LAN)"
    echo "2. Global Internet Access (WAN)"
    echo "3. Manual IP Entry"
    echo
    read -p "Enter choice (1-3): " ip_choice
    
    local server_ip
    local deployment_type
    
    case $ip_choice in
        1)
            server_ip=$(get_local_ip)
            deployment_type="Local"
            ;;
        2)
            server_ip=$(get_public_ip)
            deployment_type="Global"
            ;;
        3)
            read -p "Enter server IP address: " server_ip
            deployment_type="Manual"
            ;;
        *)
            echo "Invalid choice. Using local IP."
            server_ip=$(get_local_ip)
            deployment_type="Local"
            ;;
    esac
    
    echo
    read -p "Enter server location (e.g., your country/city): " location
    
    # Ask about database seeding
    echo
    read -p "Do you want to run the database seeder before starting the server? (y/n): " seed_choice
    if [[ $seed_choice =~ ^[Yy]$ ]]; then
        run_seeder
    fi
    
    # Clean and install project
    if ! clean_install_project; then
        return 1
    fi
    
    # Save global server configuration
    save_global_server "HQ_Server" "$server_ip" "1099" "$location"
    
    # Set Java options for global deployment
    export JAVA_OPTS="-Djava.rmi.server.hostname=$server_ip -Djava.net.preferIPv4Stack=true -Djavax.net.ssl.keyStore=config/hq-keystore.jks -Djavax.net.ssl.keyStorePassword=password123"
    
    log_message "INFO" "Starting Global HQ Server on $server_ip:1099 ($deployment_type deployment)"
    log_message "INFO" "Location: $location"
    log_message "INFO" "Clients worldwide can connect to: rmi://$server_ip:1099"
    
    # Start HQ Server
    run_hq_server "$server_ip"
}

# Function to run global branch server
run_global_branch_server() {
    echo
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}       GLOBAL BRANCH SERVER SETUP${NC}"
    echo -e "${GREEN}========================================${NC}"
    
    show_network_info
    
    echo "Select branch to run globally:"
    echo "1. Nairobi Branch (Port 1103)"
    echo "2. Kisumu Branch (Port 1102)"
    echo "3. Mombasa Branch (Port 1101)"
    echo "4. Nakuru Branch (Port 1100)"
    echo
    read -p "Enter choice (1-4): " branch_choice
    
    local branch_name
    local branch_port
    
    case $branch_choice in
        1)
            branch_name="Nairobi"
            branch_port="1103"
            ;;
        2)
            branch_name="Kisumu"
            branch_port="1102"
            ;;
        3)
            branch_name="Mombasa"
            branch_port="1101"
            ;;
        4)
            branch_name="Nakuru"
            branch_port="1100"
            ;;
        *)
            echo "Invalid choice. Defaulting to Nairobi."
            branch_name="Nairobi"
            branch_port="1103"
            ;;
    esac
    
    echo "Select IP configuration for $branch_name Branch Server:"
    echo "1. Local Network Access (LAN)"
    echo "2. Global Internet Access (WAN)"
    echo "3. Manual IP Entry"
    echo
    read -p "Enter choice (1-3): " ip_choice
    
    local server_ip
    local deployment_type
    
    case $ip_choice in
        1)
            server_ip=$(get_local_ip)
            deployment_type="Local"
            ;;
        2)
            server_ip=$(get_public_ip)
            deployment_type="Global"
            ;;
        3)
            read -p "Enter server IP address: " server_ip
            deployment_type="Manual"
            ;;
        *)
            echo "Invalid choice. Using local IP."
            server_ip=$(get_local_ip)
            deployment_type="Local"
            ;;
    esac
    
    echo
    read -p "Enter server location (e.g., your country/city): " location
    
    # Clean and install project
    if ! clean_install_project; then
        return 1
    fi
    
    # Save global server configuration
    save_global_server "${branch_name}_Branch" "$server_ip" "$branch_port" "$location"
    
    # Set Java options for global deployment
    export JAVA_OPTS="-Djava.rmi.server.hostname=$server_ip -Djava.net.preferIPv4Stack=true"
    
    log_message "INFO" "Starting Global $branch_name Branch Server on $server_ip:$branch_port ($deployment_type deployment)"
    log_message "INFO" "Location: $location"
    log_message "INFO" "Clients worldwide can connect to: rmi://$server_ip:$branch_port"
    
    # Start Branch Server
    run_branch_server "$branch_name" "$branch_port" "$server_ip"
}

# Function to run global GUI client
run_global_gui_client() {
    echo
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}        GLOBAL GUI CLIENT SETUP${NC}"
    echo -e "${GREEN}========================================${NC}"
    
    echo "Select server connection method:"
    echo "1. Connect to registered global server"
    echo "2. Manual server IP entry"
    echo "3. Show network information"
    echo
    read -p "Enter choice (1-3): " connect_choice
    
    local target_server_ip
    local target_server_port
    local target_server_name
    
    case $connect_choice in
        1)
            if select_global_server; then
                target_server_ip="$selected_server_ip"
                target_server_port="$selected_server_port"
                target_server_name="$selected_server_name"
            else
                return 1
            fi
            ;;
        2)
            read -p "Enter server IP address: " target_server_ip
            read -p "Enter server port (1099 for HQ, 1100-1103 for branches): " target_server_port
            target_server_name="Manual_Server"
            ;;
        3)
            show_network_info
            read -p "Press Enter to continue..."
            run_global_gui_client
            return
            ;;
        *)
            echo "Invalid choice. Please try again."
            run_global_gui_client
            return
            ;;
    esac
    
    echo
    read -p "Enter your location (e.g., your country/city): " client_location
    
    # Clean and install project
    if ! clean_install_project; then
        return 1
    fi
    
    log_message "INFO" "Starting Global GUI Client from $client_location"
    log_message "INFO" "Connecting to: $target_server_name at $target_server_ip:$target_server_port"
    
    # Set system properties for global connection
    export JAVA_OPTS="-Djava.rmi.server.hostname=$target_server_ip -Djava.net.preferIPv4Stack=true"
    
    # Start GUI Client
    run_gui "$target_server_ip"
}

# Function to select server for GUI connection
select_server_for_gui() {
    local current_ip=$1
    
    echo -e "${BLUE}Select Server to connect to:${NC}"
    echo "1) Local HQ Server ($current_ip:1099)"
    echo "2) Nairobi Branch ($current_ip:1103)"
    echo "3) Kisumu Branch ($current_ip:1102)"
    echo "4) Mombasa Branch ($current_ip:1101)"
    echo "5) Custom IP Address"
    
    read -p "Enter your choice (1-5): " choice
    
    case $choice in
        1)
            echo "$current_ip"
            ;;
        2)
            echo "$current_ip"
            ;;
        3)
            echo "$current_ip"
            ;;
        4)
            echo "$current_ip"
            ;;
        5)
            read -p "Enter custom IP address: " custom_ip
            echo "$custom_ip"
            ;;
        *)
            log_message "ERROR" "Invalid server selection"
            exit 1
            ;;
    esac
}

# Main menu function
show_main_menu() {
    clear
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}    DRINK BUSINESS RMI SYSTEM MANAGER${NC}"
    echo -e "${BLUE}   Combined Local & Global Deployment${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo
    echo -e "${YELLOW}LOCAL DEPLOYMENT OPTIONS:${NC}"
    echo "1) Run Local HQ Server"
    echo "2) Run Local Branch Server"
    echo "3) Run Local GUI Client"
    echo "4) Run Database Seeder Only"
    echo
    echo -e "${CYAN}GLOBAL DEPLOYMENT OPTIONS:${NC}"
    echo "5) Run Global HQ Server (worldwide access)"
    echo "6) Run Global Branch Server (worldwide access)"
    echo "7) Run Global GUI Client (connect worldwide)"
    echo
    echo -e "${PURPLE}INFORMATION & UTILITIES:${NC}"
    echo "8) Show Network Information"
    echo "9) Show Registered Global Servers"
    echo "10) Global Deployment Guide"
    echo "11) Exit"
    echo
}

# Function to show global deployment guide
show_global_guide() {
    echo
    echo -e "${CYAN}========================================${NC}"
    echo -e "${CYAN}       GLOBAL DEPLOYMENT GUIDE${NC}"
    echo -e "${CYAN}========================================${NC}"
    echo
    echo "NETWORK REQUIREMENTS FOR GLOBAL ACCESS:"
    echo
    echo "1. ROUTER CONFIGURATION (Port Forwarding):"
    echo "   - HQ Server: Forward external port 1099 to this machine"
    echo "   - Branch Servers: Forward ports 1100-1103 to branch machines"
    echo
    echo "2. FIREWALL CONFIGURATION:"
    echo "   Linux: sudo ufw allow 1099:1103/tcp"
    echo "   Linux: sudo ufw allow 1100/tcp"
    echo
    echo "3. PUBLIC IP REQUIREMENTS:"
    echo "   - Static Public IP (recommended for servers)"
    echo "   - Dynamic DNS service (for changing IPs)"
    echo "   - Cloud hosting (AWS, Azure, Google Cloud)"
    echo
    echo "4. SECURITY CONSIDERATIONS:"
    echo "   - HQ Server uses SSL encryption (port 1099)"
    echo "   - Consider VPN for additional security"
    echo "   - Monitor connection logs regularly"
    echo
    echo "5. GLOBAL CONNECTION EXAMPLES:"
    echo "   - USA HQ Server: rmi://123.456.789.012:1099"
    echo "   - Kenya Branch: rmi://234.567.890.123:1103"
    echo "   - Client from UK connecting to USA HQ"
    echo "   - Client from India connecting to Kenya Branch"
    echo
    read -p "Press Enter to continue..."
}

# Main script execution
main() {
    log_message "INFO" "Drink Business RMI System Manager Started"
    
    while true; do
        show_main_menu
        read -p "Enter your choice (1-11): " main_choice
        
        case $main_choice in
            1)
                # Run Local HQ Server
                CURRENT_IP=$(get_local_ip)
                log_message "INFO" "Starting Local HQ Server on $CURRENT_IP"
                
                read -p "Do you want to run the database seeder first? (y/n): " seed_choice
                if [[ $seed_choice =~ ^[Yy]$ ]]; then
                    if ! clean_install_project; then continue; fi
                    run_seeder
                fi
                
                if ! clean_install_project; then continue; fi
                run_hq_server "$CURRENT_IP"
                ;;
            2)
                # Run Local Branch Server
                CURRENT_IP=$(get_local_ip)
                if ! clean_install_project; then continue; fi
                select_and_run_branch "$CURRENT_IP"
                ;;
            3)
                # Run Local GUI Client
                if ! clean_install_project; then continue; fi
                select_gui_server
                ;;
            4)
                # Run Database Seeder Only
                if ! clean_install_project; then continue; fi
                run_seeder
                read -p "Press Enter to continue..."
                ;;
            5)
                # Run Global HQ Server
                run_global_hq_server
                ;;
            6)
                # Run Global Branch Server
                run_global_branch_server
                ;;
            7)
                # Run Global GUI Client
                run_global_gui_client
                ;;
            8)
                # Show Network Information
                show_network_info
                read -p "Press Enter to continue..."
                ;;
            9)
                # Show Registered Global Servers
                show_global_servers
                read -p "Press Enter to continue..."
                ;;
            10)
                # Global Deployment Guide
                show_global_guide
                ;;
            11)
                # Exit
                log_message "INFO" "Drink Business RMI System Manager ended"
                echo -e "${GREEN}Thank you for using the Drink Business RMI System!${NC}"
                echo "Check logs: $RECORD_LOG and $ERROR_LOG"
                exit 0
                ;;
            *)
                echo -e "${RED}Invalid choice. Please try again.${NC}"
                read -p "Press Enter to continue..."
                ;;
        esac
    done
}

# Run main function
main "$@"
