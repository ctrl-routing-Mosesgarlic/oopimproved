#!/bin/bash

# Test script to verify run-system.sh connection functionality
# This script tests the exact same environment setup as run-system.sh

echo "=== Testing run-system.sh Connection Setup ==="
echo

# Set the server IP (same as what you would enter in run-system.sh)
SERVER_IP="192.168.100.241"
echo "Testing connection to: $SERVER_IP"

# Export environment variable (same as run-system.sh does)
export RMI_SERVER_HOST="$SERVER_IP"

# Set Java system properties (same as run-system.sh does)
export JAVA_OPTS="-Djava.rmi.server.hostname=$SERVER_IP -Drmi.server.host=$SERVER_IP"
export MAVEN_OPTS="-Drmi.server.host=$SERVER_IP -Djava.rmi.server.hostname=$SERVER_IP"

echo "Environment setup:"
echo "  RMI_SERVER_HOST: $RMI_SERVER_HOST"
echo "  JAVA_OPTS: $JAVA_OPTS"
echo "  MAVEN_OPTS: $MAVEN_OPTS"
echo

# Test 1: Network connectivity
echo "=== Test 1: Network Connectivity ==="
if ping -c 2 "$SERVER_IP" > /dev/null 2>&1; then
    echo "✅ Ping to $SERVER_IP successful"
else
    echo "❌ Ping to $SERVER_IP failed"
fi

# Test 2: Port accessibility
echo
echo "=== Test 2: Port Accessibility ==="
if nc -zv "$SERVER_IP" 1099 2>/dev/null; then
    echo "✅ Port 1099 is accessible on $SERVER_IP"
else
    echo "❌ Port 1099 is not accessible on $SERVER_IP"
fi

# Test 3: Compile and run Java test
echo
echo "=== Test 3: Java RMI Connection Test ==="
echo "Compiling test connection class..."

# Compile the test class with the same classpath as the main project
if mvn compile -q; then
    echo "✅ Project compilation successful"
    
    # Run the test with the same environment as run-system.sh would use
    echo "Running connection test with environment variables..."
    java -cp "target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)" \
         -Drmi.server.host="$SERVER_IP" \
         -Djava.rmi.server.hostname="$SERVER_IP" \
         TestConnection
else
    echo "❌ Project compilation failed"
fi

echo
echo "=== Test 4: Simulated GUI Launch Test ==="
echo "This would be equivalent to running: ./run-system.sh -> option 7 -> option 2 -> $SERVER_IP"
echo "Environment variables that would be passed to JavaFX:"
echo "  RMI_SERVER_HOST=$RMI_SERVER_HOST"
echo "  System properties: -Drmi.server.host=$SERVER_IP -Djava.rmi.server.hostname=$SERVER_IP"

echo
echo "=== Test Summary ==="
echo "If all tests above passed, the GUI client should connect to $SERVER_IP successfully"
echo "through the run-system.sh script."
