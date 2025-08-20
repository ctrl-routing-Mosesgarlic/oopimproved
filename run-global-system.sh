#!/bin/bash

# Drink Business RMI Global System Management Script
# This script manages the RMI system for worldwide deployment across different countries/networks

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

# Function to log messages
log_message() {
    local level=$1
    local message=$2
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    if [ "$level" == "ERROR" ]; then
        echo "[$timestamp] ERROR: $message" >> "$ERROR_LOG"
        echo -e "${RED}ERROR: $message${NC}"
    else
        echo "[$timestamp] $level: $message" >> "$RECORD_LOG"
        echo -e "${GREEN}$level: $message${NC}"
    fi
}

# Function to get public IP address
get_public_ip() {
    local public_ip=""
    
    # Try multiple services to get public IP
    for service in "ifconfig.me" "ipinfo.io/ip" "icanhazip.com" "checkip.amazonaws.com"; do
        public_ip=$(curl -s --connect-timeout 5 "$service" 2>/dev/null | tr -d '\n\r')
        if [[ $public_ip =~ ^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$ ]]; then
            echo "$public_ip"
            return 0
        fi
    done
    
    log_message "WARN" "Could not detect public IP address"
    echo ""
}

# Function to get local IP address
get_local_ip() {
    local ip=""
    
    # Try different methods to get local IP
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
    fi
    
    echo "$ip"
}

# Function to display network information
show_network_info() {
    local local_ip=$(get_local_ip)
    local public_ip=$(get_public_ip)
    
    echo -e "${CYAN}=== Network Information ===${NC}"
    echo -e "${BLUE}Local IP Address:${NC} $local_ip"
    if [ -n "$public_ip" ]; then
        echo -e "${BLUE}Public IP Address:${NC} $public_ip"
        echo -e "${YELLOW}Note: For global access, use the public IP and ensure port forwarding is configured${NC}"
    else
        echo -e "${RED}Public IP: Could not detect (check internet connection)${NC}"
    fi
    echo -e "${PURPLE}Router Configuration Required:${NC}"
    echo "  - Port 1099: HQ Server (SSL)"
    echo "  - Port 1101: Mombasa Branch"
    echo "  - Port 1102: Kisumu Branch" 
    echo "  - Port 1103: Nairobi Branch"
    echo "  - Port 1100: Nakuru Branch"
    echo
}

# Function to select IP type for server deployment
select_ip_type() {
    echo -e "${BLUE}Select IP configuration for server:${NC}"
    echo "1) Local Network Only (LAN)"
    echo "2) Global Internet Access (WAN)"
    echo "3) Manual IP Entry"
    
    read -p "Enter your choice (1-3): " choice
    
    case $choice in
        1)
            echo "local"
            ;;
        2)
            echo "global"
            ;;
        3)
            echo "manual"
            ;;
        *)
            log_message "ERROR" "Invalid IP type selection"
            return 1
            ;;
    esac
}

# Function to get server IP based on selection
get_server_ip() {
    local ip_type=$1
    local local_ip=$(get_local_ip)
    local public_ip=$(get_public_ip)
    
    case $ip_type in
        "local")
            echo "$local_ip"
            ;;
        "global")
            if [ -n "$public_ip" ]; then
                echo "$public_ip"
            else
                log_message "ERROR" "Could not detect public IP for global deployment"
                echo "$local_ip"
            fi
            ;;
        "manual")
            read -p "Enter server IP address: " manual_ip
            if [[ $manual_ip =~ ^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$ ]]; then
                echo "$manual_ip"
            else
                log_message "ERROR" "Invalid IP address format"
                echo "$local_ip"
            fi
            ;;
        *)
            echo "$local_ip"
            ;;
    esac
}

# Function to save server configuration
save_server_config() {
    local server_type=$1
    local ip=$2
    local port=$3
    local location=$4
    
    echo "# Global Server Configuration - $(date)" >> "$GLOBAL_CONFIG"
    echo "$server_type,$ip,$port,$location,$(date)" >> "$GLOBAL_CONFIG"
    log_message "INFO" "Saved server config: $server_type at $ip:$port ($location)"
}

# Function to show available global servers
show_global_servers() {
    if [ ! -f "$GLOBAL_CONFIG" ]; then
        echo -e "${YELLOW}No global servers registered yet${NC}"
        return
    fi
    
    echo -e "${CYAN}=== Registered Global Servers ===${NC}"
    echo -e "${BLUE}Type\t\tIP Address\t\tPort\tLocation\t\tRegistered${NC}"
    echo "---------------------------------------------------------------------------------"
    
    local index=1
    while IFS=',' read -r server_type ip port location timestamp; do
        if [[ ! $server_type =~ ^# ]]; then
            printf "%-2s) %-12s %-15s %-7s %-15s %s\n" "$index" "$server_type" "$ip" "$port" "$location" "$timestamp"
            ((index++))
        fi
    done < "$GLOBAL_CONFIG"
    echo
}

# Function to select global server for connection
select_global_server() {
    show_global_servers
    
    if [ ! -f "$GLOBAL_CONFIG" ]; then
        echo "localhost"
        return
    fi
    
    echo -e "${BLUE}Select server to connect to:${NC}"
    echo "0) Manual IP entry"
    
    read -p "Enter server number (0 for manual): " choice
    
    if [ "$choice" == "0" ]; then
        read -p "Enter server IP address: " manual_ip
        echo "$manual_ip"
        return
    fi
    
    local index=1
    while IFS=',' read -r server_type ip port location timestamp; do
        if [[ ! $server_type =~ ^# ]]; then
            if [ "$index" == "$choice" ]; then
                echo "$ip"
                return
            fi
            ((index++))
        fi
    done < "$GLOBAL_CONFIG"
    
    log_message "ERROR" "Invalid server selection"
    echo "localhost"
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
    local location=$2
    
    log_message "INFO" "Starting Global HQ Server on $ip:1099 ($location)"
    save_server_config "HQ_Server" "$ip" "1099" "$location"
    
    # Set RMI hostname property for global access
    export JAVA_OPTS="-Djava.rmi.server.hostname=$ip -Djava.net.preferIPv4Stack=true"
    
    echo -e "${GREEN}🌍 Global HQ Server starting...${NC}"
    echo -e "${YELLOW}Public Access: rmi://$ip:1099${NC}"
    echo -e "${PURPLE}SSL Keystore: config/hq-keystore.jks${NC}"
    
    mvn compile exec:java -Dexec.mainClass="com.drinks.rmi.server.HQServer" \
        -Dexec.args="config/hq-keystore.jks password123" \
        -Djava.rmi.server.hostname="$ip" \
        -Djava.net.preferIPv4Stack=true >> "$RECORD_LOG" 2>> "$ERROR_LOG"
}

# Function to run Branch Server
run_branch_server() {
    local branch_name=$1
    local port=$2
    local ip=$3
    local location=$4
    
    log_message "INFO" "Starting Global $branch_name Branch Server on $ip:$port ($location)"
    save_server_config "${branch_name}_Branch" "$ip" "$port" "$location"
    
    # Set RMI hostname property for global access
    export JAVA_OPTS="-Djava.rmi.server.hostname=$ip -Djava.net.preferIPv4Stack=true"
    
    echo -e "${GREEN}🌍 Global $branch_name Branch Server starting...${NC}"
    echo -e "${YELLOW}Public Access: rmi://$ip:$port${NC}"
    
    java -cp target/drink-business-rmi-1.0.0.jar \
        -Djava.rmi.server.hostname="$ip" \
        -Djava.net.preferIPv4Stack=true \
        com.drinks.rmi.server.BranchServer "$branch_name" "$port" >> "$RECORD_LOG" 2>> "$ERROR_LOG"
}

# Function to run GUI with global server selection
run_global_gui() {
    echo -e "${CYAN}=== Global GUI Client ===${NC}"
    show_global_servers
    
    local server_ip=$(select_global_server)
    local location=""
    read -p "Enter your location (e.g., UK, India, Kenya): " location
    
    log_message "INFO" "Starting Global GUI client from $location connecting to server: $server_ip"
    
    # Set system properties for global GUI
    export JAVA_OPTS="-Djava.rmi.server.hostname=$server_ip -Drmi.server.host=$server_ip -Djava.net.preferIPv4Stack=true"
    
    echo -e "${GREEN}🌍 Global GUI Client starting from $location...${NC}"
    echo -e "${YELLOW}Connecting to: rmi://$server_ip${NC}"
    
    mvn javafx:run -Djava.rmi.server.hostname="$server_ip" \
        -Drmi.server.host="$server_ip" \
        -Djava.net.preferIPv4Stack=true >> "$RECORD_LOG" 2>> "$ERROR_LOG"
}

# Function to select branch and location for global deployment
select_global_branch() {
    local server_ip=$1
    
    echo -e "${BLUE}Select Branch for Global Deployment:${NC}"
    echo "1) Nairobi Branch (Port: 1103) - Kenya, Africa"
    echo "2) Kisumu Branch (Port: 1102) - Kenya, Africa"
    echo "3) Mombasa Branch (Port: 1101) - Kenya, Africa"
    echo "4) Nakuru Branch (Port: 1100) - Kenya, Africa"
    echo "5) Custom Branch (Manual Configuration)"
    
    read -p "Enter your choice (1-5): " choice
    read -p "Enter your location/country: " location
    
    case $choice in
        1)
            log_message "INFO" "Starting Global Nairobi Branch Server"
            run_branch_server "Nairobi" "1103" "$server_ip" "$location"
            ;;
        2)
            log_message "INFO" "Starting Global Kisumu Branch Server"
            run_branch_server "Kisumu" "1102" "$server_ip" "$location"
            ;;
        3)
            log_message "INFO" "Starting Global Mombasa Branch Server"
            run_branch_server "Mombasa" "1101" "$server_ip" "$location"
            ;;
        4)
            log_message "INFO" "Starting Global Nakuru Branch Server"
            run_branch_server "Nakuru" "1100" "$server_ip" "$location"
            ;;
        5)
            read -p "Enter branch name: " branch_name
            read -p "Enter port number: " port
            log_message "INFO" "Starting Global $branch_name Branch Server"
            run_branch_server "$branch_name" "$port" "$server_ip" "$location"
            ;;
        *)
            log_message "ERROR" "Invalid branch selection"
            return 1
            ;;
    esac
}

# Function to show global deployment guide
show_global_guide() {
    echo -e "${CYAN}=== Global Deployment Guide ===${NC}"
    echo -e "${YELLOW}🌍 Worldwide RMI System Deployment${NC}"
    echo
    echo -e "${BLUE}Network Requirements:${NC}"
    echo "1. Router Port Forwarding:"
    echo "   - Port 1099 → HQ Server (SSL)"
    echo "   - Port 1101-1103 → Branch Servers"
    echo "   - Port 1100 → Nakuru Branch"
    echo
    echo "2. Firewall Configuration:"
    echo "   - Allow inbound TCP connections on RMI ports"
    echo "   - Configure Windows/Linux firewall rules"
    echo
    echo "3. Public IP/DNS:"
    echo "   - Static public IP (recommended)"
    echo "   - Dynamic DNS service (alternative)"
    echo "   - Cloud hosting (AWS, GCP, Azure)"
    echo
    echo -e "${PURPLE}Global Deployment Scenario:${NC}"
    echo "🇺🇸 USA: HQ Server (Port 1099)"
    echo "🇬🇧 UK: GUI Client → Connect to USA HQ"
    echo "🌍 Africa: Branch Servers (Ports 1101-1103)"
    echo "🇮🇳 India: Additional Branch/Client"
    echo "🌏 Any Country: More clients connecting globally"
    echo
    echo -e "${GREEN}Security Considerations:${NC}"
    echo "- SSL/TLS encryption for HQ connections"
    echo "- VPN tunnels for additional security"
    echo "- Authentication and authorization"
    echo "- Network monitoring and logging"
    echo
}

# Main menu function
show_main_menu() {
    echo -e "${YELLOW}=== Global Drink Business RMI System Manager ===${NC}"
    echo -e "${CYAN}🌍 Worldwide Deployment Ready${NC}"
    echo
    echo "1) Run Global HQ Server"
    echo "2) Run Global Branch Server"
    echo "3) Run Global GUI Client"
    echo "4) Show Global Servers"
    echo "5) Run Database Seeder"
    echo "6) Show Network Information"
    echo "7) Global Deployment Guide"
    echo "8) Exit"
    echo
}

# Main script execution
main() {
    # Initialize log files
    echo "=== Global Drink Business RMI System Started at $(date) ===" >> "$RECORD_LOG"
    echo "=== Global Error Log Started at $(date) ===" >> "$ERROR_LOG"
    
    # Show initial network information
    show_network_info
    
    # Clean, install, and build project
    if ! clean_install_project; then
        exit 1
    fi
    
    while true; do
        show_main_menu
        read -p "Enter your choice (1-8): " main_choice
        
        case $main_choice in
            1)
                # Run Global HQ Server
                echo -e "${BLUE}Do you want to run the database seeder first? (y/n):${NC}"
                read -p "" run_seeder_choice
                
                if [[ $run_seeder_choice =~ ^[Yy]$ ]]; then
                    if ! run_seeder; then
                        log_message "ERROR" "Seeder failed, but continuing with server startup..."
                    fi
                fi
                
                ip_type=$(select_ip_type)
                server_ip=$(get_server_ip "$ip_type")
                read -p "Enter your location/country: " location
                
                run_hq_server "$server_ip" "$location"
                ;;
                
            2)
                # Run Global Branch Server
                echo -e "${BLUE}Do you want to run the database seeder first? (y/n):${NC}"
                read -p "" run_seeder_choice
                
                if [[ $run_seeder_choice =~ ^[Yy]$ ]]; then
                    if ! run_seeder; then
                        log_message "ERROR" "Seeder failed, but continuing with server startup..."
                    fi
                fi
                
                ip_type=$(select_ip_type)
                server_ip=$(get_server_ip "$ip_type")
                
                select_global_branch "$server_ip"
                ;;
                
            3)
                # Run Global GUI Client
                run_global_gui
                ;;
                
            4)
                # Show Global Servers
                show_global_servers
                ;;
                
            5)
                # Run Database Seeder
                run_seeder
                ;;
                
            6)
                # Show Network Information
                show_network_info
                ;;
                
            7)
                # Global Deployment Guide
                show_global_guide
                ;;
                
            8)
                # Exit
                log_message "INFO" "Global system manager exiting..."
                exit 0
                ;;
                
            *)
                log_message "ERROR" "Invalid choice. Please try again."
                ;;
        esac
        
        echo
        echo -e "${YELLOW}Press Enter to return to main menu...${NC}"
        read
    done
}

# Check dependencies
if ! command -v mvn &> /dev/null; then
    log_message "ERROR" "Maven is not installed or not in PATH"
    exit 1
fi

if ! command -v java &> /dev/null; then
    log_message "ERROR" "Java is not installed or not in PATH"
    exit 1
fi

if ! command -v curl &> /dev/null; then
    log_message "WARN" "curl is not installed - public IP detection may not work"
fi

# Run main function
main
