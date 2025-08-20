#!/bin/bash

# Drink Business RMI System Management Script (Linux/macOS)
# This script manages the entire RMI system with dynamic IP detection

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Log files
ERROR_LOG="errors.log"
RECORD_LOG="record.log"

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
    
    # Set system properties for GUI
    export JAVA_OPTS="-Djava.rmi.server.hostname=$server_ip"
    
    mvn javafx:run -Djava.rmi.server.hostname="$server_ip" >> "$RECORD_LOG" 2>> "$ERROR_LOG"
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
    echo -e "${YELLOW}=== Drink Business RMI System Manager ===${NC}"
    echo "1) Run HQ Server"
    echo "2) Run Branch Server"
    echo "3) Run GUI Client"
    echo "4) Run Database Seeder Only"
    echo "5) Exit"
    echo
}

# Main script execution
main() {
    # Initialize log files
    echo "=== Drink Business RMI System Started at $(date) ===" >> "$RECORD_LOG"
    echo "=== Error Log Started at $(date) ===" >> "$ERROR_LOG"
    
    # Get current IP
    CURRENT_IP=$(get_local_ip)
    log_message "INFO" "Detected IP address: $CURRENT_IP"
    
    # Clean, install, compile and package project first
    if ! clean_install_project; then
        exit 1
    fi
    
    if ! compile_project; then
        exit 1
    fi
    
    if ! package_project; then
        exit 1
    fi
    
    while true; do
        show_main_menu
        read -p "Enter your choice (1-5): " main_choice
        
        case $main_choice in
            1)
                # Run HQ Server
                echo -e "${BLUE}Do you want to run the database seeder first? (y/n):${NC}"
                read -p "" run_seeder_choice
                
                if [[ $run_seeder_choice =~ ^[Yy]$ ]]; then
                    if ! run_seeder; then
                        log_message "ERROR" "Seeder failed, but continuing with server startup..."
                    fi
                fi
                
                log_message "INFO" "Starting HQ Server..."
                run_hq_server "$CURRENT_IP"
                ;;
                
            2)
                # Run Branch Server
                echo -e "${BLUE}Do you want to run the database seeder first? (y/n):${NC}"
                read -p "" run_seeder_choice
                
                if [[ $run_seeder_choice =~ ^[Yy]$ ]]; then
                    if ! run_seeder; then
                        log_message "ERROR" "Seeder failed, but continuing with server startup..."
                    fi
                fi
                
                # Use the new simplified branch selection function
                select_and_run_branch "$CURRENT_IP"
                ;;
                
            3)
                # Run GUI Client
                server_ip=$(select_server_for_gui "$CURRENT_IP")
                run_gui "$server_ip"
                ;;
                
            4)
                # Run Database Seeder Only
                run_seeder
                ;;
                
            5)
                # Exit
                log_message "INFO" "System manager exiting..."
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

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    log_message "ERROR" "Maven is not installed or not in PATH"
    exit 1
fi

# Check if Java is installed
if ! command -v java &> /dev/null; then
    log_message "ERROR" "Java is not installed or not in PATH"
    exit 1
fi

# Run main function
main
