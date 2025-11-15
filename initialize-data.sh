#!/bin/bash

# Initialize Stock Data Script
# This script downloads 10 years of historical data for all ETFs
# Run this once when first setting up the application

echo "======================================"
echo "Stock Investment Guide - Data Initialization"
echo "======================================"
echo ""
echo "This will download 10 years of historical data for:"
echo "  - QQQM (Nasdaq-100)"
echo "  - VOO (S&P 500)"
echo "  - SOXX (Semiconductor Index)"
echo ""
echo "This may take 1-2 minutes due to rate limiting delays."
echo ""
read -p "Continue? (y/n) " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[Yy]$ ]]
then
    echo "Cancelled."
    exit 1
fi

# Check if server is running
echo ""
echo "Checking if server is running..."
if ! curl -s http://localhost:8080/api/stocks/health > /dev/null 2>&1; then
    echo "ERROR: Server is not running on http://localhost:8080"
    echo "Please start the server first with: ./mvnw spring-boot:run"
    exit 1
fi

echo "Server is running. Starting data initialization..."
echo ""

# Call the initialize endpoint
response=$(curl -s -X POST "http://localhost:8080/api/stocks/initialize?years=10")

echo "Response:"
echo "$response" | python3 -m json.tool 2>/dev/null || echo "$response"

echo ""
echo "======================================"
echo "Initialization complete!"
echo "======================================"
echo ""
echo "You can now use the web interface at http://localhost:8080"
