# Drink Business RMI System

A distributed Java RMI-based drink business management system with SSL security, featuring headquarters (HQ) server, branch servers, and a JavaFX GUI client with comprehensive automation scripts for easy deployment across multiple machines.

## 🚀 Quick Start with Automation Scripts

### For Linux/macOS Users
```bash
# Make the script executable
chmod +x run-system.sh

# Run the system manager
./run-system.sh
```

### For Windows Users
```cmd
# Run the system manager
run-system.bat
```

The automation scripts provide:
- **Dynamic IP Detection**: Automatically detects your network IP for multi-machine deployment
- **Interactive Menus**: Choose between HQ Server, Branch Servers, or GUI Client
- **Database Seeder Integration**: Option to seed database before starting servers
- **Server Selection for GUI**: Choose which server to connect to when running the client
- **Comprehensive Logging**: All activities logged to `record.log`, errors to `errors.log`
- **Cross-Platform Support**: Works on Linux, macOS, and Windows

## 🏗️ System Architecture

### Main Server (Headquarters)
- **SSL-secured RMI server** on port **1099**
- Manages global data: drinks, customers, users, branches
- Hosts JavaFX GUI dashboards for: Admin, Customer Support, Auditor, Global Manager
- Provides comprehensive reporting and user management capabilities

### Branch Servers
- **Nairobi Branch**: Port **1103**
- **Kisumu Branch**: Port **1102** 
- **Mombasa Branch**: Port **1101**
- **Nakuru Branch**: Can run as HQ or Branch server

Each branch acts as a standalone RMI server handling local stock, order placement, and customer access.

### JavaFX GUI Client
- Modern JavaFX interface with role-based dashboards
- SSL-secured connections to servers
- Real-time order management and stock tracking
- User authentication and authorization
- Dynamic server selection and connection

## 👥 User Roles and Access Levels

| Role | Access Location | Responsibilities |
|------|----------------|------------------|
| Admin | HQ | Manage users, drinks, and view all reports |
| Customer Support | HQ | View customers and resolve order issues |
| Auditor | HQ | View sales/stock reports from all branches |
| Global Manager | HQ | Monitor all orders and performance |
| Branch Manager | Branch | Manage local stock and view branch sales |
| Branch Staff | Branch | Place orders and update stock |
| Customer | HQ or Branch | Browse drinks and place orders |

## 🤖 Automation Scripts Usage

### Script Features

#### Dynamic IP Detection
- Automatically detects your network IP address
- Works across different WiFi networks without manual configuration
- Falls back to localhost if network IP cannot be detected

#### Interactive Server Selection
- **HQ Server**: Runs the main headquarters server with SSL
- **Branch Server**: Choose from Nairobi, Kisumu, Mombasa, or Nakuru
- **GUI Client**: Select which server to connect to
- **Database Seeder**: Initialize database with sample data

#### Multi-Machine Deployment
Perfect for running on multiple laptops/computers:
- **Laptop 1**: Run HQ Server
- **Laptop 2**: Run Nairobi Branch
- **Laptop 3**: Run GUI Client connecting to any server
- **Laptop 4**: Run another GUI Client or branch

### Script Menu Options

1. **Run HQ Server**
   - Prompts to run database seeder first
   - Starts SSL-secured HQ server on port 1099
   - Automatically uses detected IP address

2. **Run Branch Server**
   - Prompts to run database seeder first
   - Choose branch: Nairobi (1103), Kisumu (1102), Mombasa (1101), or Nakuru (HQ mode)
   - Automatically configures IP and port

3. **Run GUI Client**
   - Select server to connect to:
     - Local HQ Server
     - Nairobi Branch
     - Kisumu Branch  
     - Mombasa Branch
     - Custom IP address
   - Launches JavaFX GUI with SSL connections

4. **Run Database Seeder Only**
   - Initializes database with sample data
   - Creates users, drinks, branches, and sample orders

### Logging System

- **record.log**: All system activities, server starts, connections
- **errors.log**: Error messages, exceptions, and failures
- Timestamped entries for easy debugging
- Color-coded console output for better visibility

## 🗄️ Database Schema

The system uses MySQL database `drinkdb` with the following tables:
- `customers` - Customer information
- `branches` - Branch locations (Nairobi, Mombasa, Kisumu, Nakuru)
- `drinks` - Available drinks and prices
- `stocks` - Inventory management per branch
- `orders` - Customer orders
- `order_items` - Individual items in orders
- `users` - Authentication and role management
- `payments` - Payment processing and tracking

## 🚀 Getting Started

### Prerequisites
- Java 17 or higher
- MySQL 8.0 or higher
- Maven 3.6 or higher
- JavaFX runtime (included in OpenJDK 17+)

### 1. Database Setup

1. **Install MySQL** and ensure it's running on localhost:3306

2. **Create the database and user**:
   ```sql
   CREATE DATABASE drinkdb;
   CREATE USER 'root'@'localhost' IDENTIFIED BY '12345';
   GRANT ALL PRIVILEGES ON drinkdb.* TO 'root'@'localhost';
   FLUSH PRIVILEGES;
   ```

### 2. Quick Start with Automation

#### Linux/macOS:
```bash
# Make script executable
chmod +x run-system.sh

# Run the system manager
./run-system.sh
```

#### Windows:
```cmd
run-system.bat
```

### 3. Manual Commands (Alternative)

If you prefer running commands manually:

#### HQ Server:
```bash
mvn compile exec:java -Dexec.mainClass="com.drinks.rmi.server.HQServer" -Dexec.args="config/hq-keystore.jks password123"
```

#### Branch Servers:
```bash
# Nairobi Branch
java -cp target/drink-business-rmi-1.0.0.jar com.drinks.rmi.server.BranchServer Nairobi 1103

# Kisumu Branch  
java -cp target/drink-business-rmi-1.0.0.jar com.drinks.rmi.server.BranchServer Kisumu 1102

# Mombasa Branch
java -cp target/drink-business-rmi-1.0.0.jar com.drinks.rmi.server.BranchServer Mombasa 1101
```

#### GUI Client:
```bash
mvn javafx:run
```

#### Database Seeder:
```bash
java -cp target/drink-business-rmi-1.0.0.jar com.drinks.rmi.util.DatabaseSeeder
```

## 🔐 Sample Users and Login Details

All users have the password: **password123**

### HQ Users
| Username | Role | Description |
|----------|------|-------------|
| `admin` | Admin | Full system administration |
| `support1` | Customer Support | Customer service operations |
| `auditor1` | Auditor | Financial and operational auditing |
| `globalmanager` | Global Manager | Overall business management |

### Branch Users
| Username | Role | Branch | Description |
|----------|------|--------|-----------|
| `nairobi_manager` | Manager | Nairobi | Branch management |
| `kisumu_manager` | Manager | Kisumu | Branch management |
| `mombasa_manager` | Manager | Mombasa | Branch management |
| `nakuru_manager` | Manager | Nakuru | Branch management |
| `nairobi_staff` | Staff | Nairobi | Daily operations |
| `kisumu_staff` | Staff | Kisumu | Daily operations |
| `mombasa_staff` | Staff | Mombasa | Daily operations |
| `nakuru_staff` | Staff | Nakuru | Daily operations |

### Customer Users
| Username | Customer Name | Description |
|----------|---------------|-------------|
| `john_doe` | John Doe | Sample customer with orders |
| `jane_smith` | Jane Smith | Sample customer with orders |
| `mike_johnson` | Mike Johnson | Sample customer with orders |

## 💡 Sample Use Cases

### Multi-Machine Deployment Scenario
1. **Laptop 1 (HQ Server)**: Run `./run-system.sh` → Choose "Run HQ Server" → Seed database
2. **Laptop 2 (Nairobi Branch)**: Run `./run-system.sh` → Choose "Run Branch Server" → Select "Nairobi"
3. **Laptop 3 (GUI Client)**: Run `./run-system.sh` → Choose "Run GUI Client" → Select server to connect to
4. **Laptop 4 (Another Client)**: Same as Laptop 3, can connect to any running server

### Customer Ordering Workflow
1. Customer logs in through GUI with username/password
2. Selects drinks from available catalog
3. Places order with quantities
4. System checks stock availability across branches
5. Order is processed and stock is updated
6. Payment is processed
7. Customer receives confirmation

### Branch Manager Operations
1. Manager logs into branch server via GUI
2. Views current stock levels for their branch
3. Updates inventory quantities
4. Reviews branch-specific orders
5. Generates local sales reports

### Admin Operations
1. Admin logs into HQ server via GUI
2. Manages user accounts across all branches
3. Adds/updates drink catalog globally
4. Views system-wide reports and analytics
5. Monitors all branch performance
6. Processes payments and financial reports

## 🌐 Network Configuration & Multi-Machine Setup

### Dynamic IP Detection
The automation scripts automatically detect your network IP, making it easy to:
- Switch between different WiFi networks
- Deploy across multiple machines without manual IP configuration
- Handle network changes seamlessly

### Deployment Scenarios

#### Single Machine Testing
- All services run on localhost with different ports
- Perfect for development and testing

#### Multi-Machine Production
- **HQ Server**: Any machine on network (port 1099, SSL secured)
- **Branch Servers**: Different machines (ports 1101-1103)
- **GUI Clients**: Any machine, connects to any server
- **Database**: Typically on HQ server machine

### Port Configuration
| Service | Port | SSL | Description |
|---------|------|-----|-------------|
| HQ Server | 1099 | ✅ | Main headquarters server |
| Nairobi Branch | 1103 | ❌ | Branch server |
| Kisumu Branch | 1102 | ❌ | Branch server |
| Mombasa Branch | 1101 | ❌ | Branch server |
| MySQL Database | 3306 | ❌ | Database server |

## 📊 Available Drinks

| ID | Name | Price (KES) | Category |
|----|------|-------------|----------|
| 1 | Soda | 50.00 | Soft Drink |
| 2 | Milk | 70.00 | Dairy |
| 3 | Water | 30.00 | Beverage |
| 4 | Alcohol | 150.00 | Alcoholic |
| 5 | Juice | 80.00 | Fruit Drink |
| 6 | Coffee | 120.00 | Hot Beverage |
| 7 | Tea | 60.00 | Hot Beverage |
| 8 | Energy Drink | 200.00 | Energy |

## 🔧 Troubleshooting

### Script-Related Issues

1. **Script Permission Denied (Linux/macOS)**
   ```bash
   chmod +x run-system.sh
   ```

2. **IP Detection Failed**
   - Script falls back to localhost automatically
   - Check network connectivity
   - Manually specify IP when prompted for "Custom IP"

3. **Maven Not Found**
   - Install Maven: `sudo apt install maven` (Ubuntu) or `brew install maven` (macOS)
   - Ensure Maven is in system PATH

4. **Java Not Found**
   - Install OpenJDK 17+: `sudo apt install openjdk-17-jdk`
   - Set JAVA_HOME environment variable

### System Issues

1. **Database Connection Failed**
   - Ensure MySQL is running: `sudo systemctl start mysql`
   - Verify credentials (root/12345)
   - Check if database `drinkdb` exists
   - Run database seeder through script menu

2. **RMI Registry Errors**
   - Ensure ports 1099-1103 are available
   - Check firewall settings: `sudo ufw allow 1099:1103/tcp`
   - Verify Java RMI is properly configured

3. **SSL Connection Issues**
   - Verify keystore files exist in `config/` directory
   - Check keystore password (password123)
   - Ensure SSL certificates are properly generated

4. **GUI Connection Failed**
   - Verify target server is running
   - Check IP address and port configuration
   - Try connecting to localhost first
   - Review error logs in `errors.log`

### Log Analysis

- **record.log**: Check for successful server starts and connections
- **errors.log**: Look for specific error messages and stack traces
- Console output: Real-time status and error information

## 📁 Project Structure

```
drink-business-rmi/
├── run-system.sh           # Linux/macOS automation script
├── run-system.bat          # Windows automation script
├── record.log              # System activity log
├── errors.log              # Error log
├── src/main/java/com/drinks/rmi/
│   ├── interfaces/         # RMI interfaces and DTOs
│   ├── server/            # HQ and Branch server implementations
│   ├── gui/               # JavaFX GUI application
│   ├── services/          # Business logic services
│   ├── util/              # Database utilities and seeder
│   └── security/          # Authentication and authorization
├── config/
│   ├── hq-keystore.jks    # SSL keystore for HQ server
│   └── database.properties # Database configuration
├── database/
│   ├── init.sql           # Database schema and initial data
│   └── migrations/        # SQL migration files
├── target/                # Compiled classes and JAR files
├── pom.xml               # Maven configuration
└── README.md             # This documentation
```

## 🚦 System Status Verification

After using the automation scripts, you should see:

### Successful HQ Server Start:
```
INFO: Detected IP address: 192.168.1.100
INFO: Starting HQ Server on IP: 192.168.1.100
HQ RMI Server is running on 192.168.1.100:1099...
```

### Successful Branch Server Start:
```
INFO: Starting Nairobi Branch Server on 192.168.1.100:1103
Nairobi Branch RMI Server is running on 192.168.1.100:1103...
```

### Successful GUI Client Start:
```
INFO: Starting GUI client connecting to server: 192.168.1.100
JavaFX GUI launched successfully
```

## 🎯 Key Features

- ✅ **SSL-Secured RMI Communication**
- ✅ **Dynamic IP Detection & Multi-Machine Support**
- ✅ **Interactive Automation Scripts (Linux & Windows)**
- ✅ **JavaFX GUI with Role-Based Dashboards**
- ✅ **Comprehensive Database Migration System**
- ✅ **Real-Time Order Management**
- ✅ **Stock Management Across Branches**
- ✅ **Payment Processing Integration**
- ✅ **Detailed Logging and Error Tracking**
- ✅ **User Authentication and Authorization**

---

**Note**: This system demonstrates advanced Java RMI concepts, SSL security, distributed computing, and modern GUI development. The automation scripts make it production-ready for multi-machine deployment scenarios.
