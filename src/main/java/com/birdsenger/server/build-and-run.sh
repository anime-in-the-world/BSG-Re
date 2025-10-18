#!/bin/bash

echo "🔨 Building BirdSenger Socket Server..."

# Download all dependencies if not present
if [ ! -f "netty-socketio.jar" ]; then
    echo "📦 Downloading dependencies..."
    wget -q https://repo1.maven.org/maven2/com/corundumstudio/socketio/netty-socketio/2.0.6/netty-socketio-2.0.6.jar -O netty-socketio.jar
    wget -q https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar -O gson.jar
    wget -q https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.1/postgresql-42.7.1.jar -O postgresql.jar
    wget -q https://repo1.maven.org/maven2/io/netty/netty-all/4.1.104.Final/netty-all-4.1.104.Final.jar -O netty-all.jar
    wget -q https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar -O slf4j-api.jar
    wget -q https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.9/slf4j-simple-2.0.9.jar -O slf4j-simple.jar
fi

# Compile
echo "⚙️  Compiling..."
javac -cp ".:netty-socketio.jar:gson.jar:postgresql.jar:netty-all.jar:slf4j-api.jar" SocketServer.java

if [ $? -ne 0 ]; then
    echo "❌ Compilation failed!"
    exit 1
fi

# Create uber JAR
echo "📦 Creating uber JAR..."
rm -rf temp
mkdir temp
cd temp

# Unzip all dependencies
unzip -q ../netty-socketio.jar
unzip -q ../gson.jar
unzip -q ../postgresql.jar
unzip -q ../netty-all.jar
unzip -q ../slf4j-api.jar
unzip -q ../slf4j-simple.jar

# Copy compiled class
cp ../SocketServer.class .

# Remove conflicting files
rm -rf META-INF/*.SF META-INF/*.DSA META-INF/*.RSA

# Create manifest
echo "Main-Class: SocketServer" > manifest.txt

# Create final JAR with manifest
jar cfm ../birdsengerserver.jar manifest.txt *

cd ..
rm -rf temp

echo ""
echo "✅ SUCCESS! JAR created: birdsengerserver.jar"
echo ""
echo "To run: java -jar birdsengerserver.jar"
echo ""