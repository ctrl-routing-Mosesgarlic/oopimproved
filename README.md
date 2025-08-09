# Drink Business RMI System

A Java RMI-based distributed system for a drinks company with headquarters (HQ) and 4 branch servers supporting role-based access, stock management, ordering, and reporting.

## 🏗️ System Architecture

### Main Server (Headquarters)
- Central RMI server on port **1099**
- Manages global data: drinks, customers, users, branches
- Hosts role-based dashboards for: Admin, Customer Support, Auditor, Global Manager
- Provides comprehensive reporting capabilities

### Branch Servers
- **Nakuru Branch**: Port **1100**
- **Mombasa Branch**: Port **1101** 
- **Kisumu Branch**: Port **1102**
- **Nairobi Branch**: Port **1103**

Each branch acts as a standalone RMI server handling local stock, order placement, and customer access.

### Client Application
- Command-line interface with role-based menus
- User login portal with authentication
- Access control and dashboard based on user role
- Customers can place orders via branch or HQ

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

## 🗄️ Database Schema

The system uses MySQL database `drinkdb` with the following tables:
- `customers` - Customer information
- `branches` - Branch locations (Nakuru, Mombasa, Kisumu, Nairobi)
- `drinks` - Available drinks and prices
- `stocks` - Inventory management per branch
- `orders` - Customer orders
- `order_items` - Individual items in orders
- `users` - Authentication and role management

## 🚀 Getting Started

### Prerequisites
- Java 17 or higher
- MySQL 8.0 or higher
- Maven 3.6 or higher

### 1. Database Setup

1. **Install MySQL** and ensure it's running on localhost:3306

2. **Create the database and user**:
   ```sql
   CREATE DATABASE drinkdb;
   CREATE USER 'root'@'localhost' IDENTIFIED BY '12345';
   GRANT ALL PRIVILEGES ON drinkdb.* TO 'root'@'localhost';
   FLUSH PRIVILEGES;
   ```

3. **Initialize the database**:
   ```bash
   mysql -u root -p12345 < database/init.sql
   ```

### 2. Build the Project

```bash
cd drink-business-rmi
mvn clean compile
```

### 3. Start the Servers

#### Start HQ Server (Terminal 1)
```bash
mvn exec:java -Dexec.mainClass="com.drinks.rmi.server.HQServer"
```

#### Start Branch Servers (Separate Terminals)

**Nakuru Branch (Terminal 2)**:
```bash
mvn exec:java -Dexec.mainClass="com.drinks.rmi.server.BranchServer" -Dexec.args="Nakuru 1100"
```

**Mombasa Branch (Terminal 3)**:
```bash
mvn exec:java -Dexec.mainClass="com.drinks.rmi.server.BranchServer" -Dexec.args="Mombasa 1101"
```

**Kisumu Branch (Terminal 4)**:
```bash
mvn exec:java -Dexec.mainClass="com.drinks.rmi.server.BranchServer" -Dexec.args="Kisumu 1102"
```

**Nairobi Branch (Terminal 5)**:
```bash
mvn exec:java -Dexec.mainClass="com.drinks.rmi.server.BranchServer" -Dexec.args="Nairobi 1103"
```

### 4. Launch Client Application

```bash
mvn exec:java -Dexec.mainClass="com.drinks.rmi.client.DrinkBusinessClient"
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
|----------|------|--------|-------------|
| `nakuru_manager` | Manager | Nakuru | Branch management |
| `mombasa_manager` | Manager | Mombasa | Branch management |
| `kisumu_manager` | Manager | Kisumu | Branch management |
| `nairobi_manager` | Manager | Nairobi | Branch management |
| `nakuru_staff` | Staff | Nakuru | Daily operations |
| `mombasa_staff` | Staff | Mombasa | Daily operations |
| `kisumu_staff` | Staff | Kisumu | Daily operations |
| `nairobi_staff` | Staff | Nairobi | Daily operations |

### Customer Users
| Username | Role | Customer Name |
|----------|------|---------------|
| `customer1` | Customer | John Doe |
| `customer2` | Customer | Jane Smith |
| `customer3` | Customer | Mike Johnson |

## 📡 RMI Services

### Common Remote Interfaces
- **AuthService** - User authentication and management
- **DrinkService** - Drink catalog management
- **StockService** - Inventory management
- **OrderService** - Order processing
- **ReportService** - Business reporting (HQ only)

### Service Endpoints

#### HQ Server (localhost:1099)
- `HQ_AuthService`
- `HQ_DrinkService`
- `HQ_StockService`
- `HQ_OrderService`
- `HQ_ReportService`

#### Branch Servers
- `NAKURU_AuthService` (localhost:1100)
- `MOMBASA_AuthService` (localhost:1101)
- `KISUMU_AuthService` (localhost:1102)
- `NAIROBI_AuthService` (localhost:1103)
- *(Similar pattern for other services)*

## 💡 Sample Use Cases

### Customer Ordering
1. Customer logs in with username/password
2. Selects server (HQ or specific branch)
3. Views available drinks
4. Places order with quantities
5. System checks stock availability
6. Order is processed and stock is updated
7. Customer receives confirmation

### Branch Manager Operations
1. Manager logs into branch server
2. Views current stock levels
3. Updates inventory quantities
4. Reviews branch orders
5. Generates local reports

### Admin Operations
1. Admin logs into HQ server
2. Manages global drink catalog
3. Creates new staff users
4. Views system-wide reports
5. Monitors all branch activities

## 🛠️ Technical Details

### Architecture Components
- **RMI Registry**: Service discovery and binding
- **Connection Pooling**: HikariCP for database connections
- **Password Security**: BCrypt hashing
- **Logging**: SLF4J with Logback
- **Serialization**: Custom DTOs for data transfer

### Database Configuration
- **Host**: localhost:3306
- **Database**: drinkdb
- **Username**: root
- **Password**: 12345

### Port Configuration
- **HQ Server**: 1099
- **Nakuru Branch**: 1100
- **Mombasa Branch**: 1101
- **Kisumu Branch**: 1102
- **Nairobi Branch**: 1103

## Distributed System Architecture

This application uses Java RMI for distributed communication between:
- 1 Main HQ Server (port 1099)
- Multiple Branch Servers (ports 1100-1103)
- Multiple GUI Clients

Key features:
- Secure SSL communication
- Service versioning (v1)
- Heartbeat monitoring
- Connection pooling
- Automatic failover and retry logic

## Deployment Instructions

### 1. HQ Server Setup
```bash
java -Djavax.net.ssl.keyStore=config/hq-keystore.jks \
     -Djavax.net.ssl.keyStorePassword=securepassword123 \
     -jar drink-business-rmi.jar
```

### 2. Branch Server Setup (Example for Nakuru)
```bash
java -Djavax.net.ssl.trustStore=config/branch-truststore.jks \
     -Dmain.server=hq.example.com \
     -jar drink-business-branch.jar Nakuru
```

### 3. Client Application
```bash
java -Djavax.net.ssl.trustStore=config/client-truststore.jks \
     -jar drink-business-client.jar
```

## SSL Certificate Setup

1. Generate HQ Server Keystore:
```bash
keytool -genkeypair -alias hq-server -keyalg RSA -keystore hq-keystore.jks
```

2. Export HQ Public Certificate:
```bash
keytool -exportcert -alias hq-server -file hq-cert.cer -keystore hq-keystore.jks
```

3. Import HQ Certificate into Branch/Client Truststores:
```bash
keytool -importcert -alias hq-server -file hq-cert.cer -keystore branch-truststore.jks
```

## Database Configuration

- MySQL database expected on HQ server
- Connection settings in `database.properties`
- Sample data loaded from `database/init.sql`

## Monitoring

- Heartbeats every 5 seconds
- 10 second timeout marks branch as offline
- Load balancer tracks active connections and server load

## 📈 Future Enhancements

- GUI client using JavaFX
- Real-time notifications
- Advanced reporting features
- Mobile client support
- Enhanced security features
- Load balancing capabilities

## 📞 Support

For technical support or questions about the system:
1. Check the troubleshooting section
2. Review server logs for error messages
3. Verify database connectivity
4. Ensure all required services are running

---

**Note**: This system is designed for educational purposes and demonstrates RMI-based distributed computing concepts. For production use, additional security, error handling, and scalability features should be implemented.

## 📊 Available Drinks

| ID | Name | Price (KES) |
|----|------|-------------|
| 1 | Soda | 50.00 |
| 2 | Milk | 70.00 |
| 3 | Water | 30.00 |
| 4 | Alcohol | 150.00 |
| 5 | Juice | 80.00 |
| 6 | Coffee | 120.00 |
| 7 | Tea | 60.00 |
| 8 | Energy Drink | 200.00 |

## 🔧 Troubleshooting

### Common Issues

1. **Database Connection Failed**
   - Ensure MySQL is running
   - Verify credentials (root/12345)
   - Check if database `drinkdb` exists

2. **RMI Registry Errors**
   - Ensure ports 1099-1103 are available
   - Check firewall settings
   - Verify Java RMI is properly configured

3. **Service Not Found**
   - Confirm servers are running
   - Check RMI registry bindings
   - Verify service names match exactly

4. **Login Failed**
   - Use correct username/password combinations
   - Ensure database has sample users
   - Check password hashing implementation

### Logs Location
- Server logs are displayed in console
- Client logs are displayed in console
- Database errors are logged with full stack traces

## 📁 Project Structure

```
drink-business-rmi/
├── src/main/java/com/drinks/rmi/
│   ├── interfaces/          # RMI interfaces and DTOs
│   ├── server/             # Server implementations
│   ├── client/             # Client application
│   └── common/             # Shared utilities
├── database/
│   └── init.sql            # Database initialization script
├── pom.xml                 # Maven configuration
└── README.md              # This file
```

## 🚦 System Status Verification

After starting all servers, you should see:
- HQ Server: "HQ RMI Server is running..."
- Branch Servers: "[Branch] Branch RMI Server is running..."
- Client: Connection successful messages
# oopimproved
