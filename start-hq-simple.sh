#!/bin/bash

# Simple HQ Server startup script without SSL for testing

echo "=== Starting HQ Server (Simple Mode) ==="

# Kill any existing Java processes related to our app
pkill -f "drink-business-rmi"
pkill -f "rmiregistry"

# Wait a moment for cleanup
sleep 2

# Start RMI registry in background (without SSL for now)
echo "Starting RMI registry..."
rmiregistry 1099 &
REGISTRY_PID=$!
echo "RMI registry started with PID: $REGISTRY_PID"

# Wait for registry to be ready
sleep 3

# Start HQ server
echo "Starting HQ server..."
java -cp target/drink-business-rmi-1.0.0.jar com.drinks.rmi.server.HQServerSimple

# Cleanup on exit
trap "kill $REGISTRY_PID 2>/dev/null" EXIT
