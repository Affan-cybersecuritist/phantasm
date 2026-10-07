#!/usr/bin/env bash
# Phantasm: Dynamic In-Memory Bytecode Decryption And Execution Demo Runner
set -e
cd "$(dirname "$0")"

echo "=========================================================================="
echo "  Phantasm: Dynamic In-Memory Bytecode Decryption And Execution (Java 17+) "
echo "=========================================================================="

# Check if Maven is installed and available
if command -v mvn &> /dev/null; then
    echo ">> Maven detected. Building and executing via Maven exec plugin..."
    mvn clean compile exec:java -Dexec.mainClass="com.phantasm.launcher.PhantasmLauncher"
    exit 0
fi

echo ">> Maven not detected. Running direct javac/java pipeline..."

LIB_DIR="lib"
SQLITE_JAR="$LIB_DIR/sqlite-jdbc-3.36.0.3.jar"
mkdir -p build protected "$LIB_DIR"

if [ ! -f "$SQLITE_JAR" ]; then
    echo ">> Downloading sqlite-jdbc driver..."
    curl -sSL "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.45.1.0/sqlite-jdbc-3.45.1.0.jar" -o "$SQLITE_JAR" || \
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.45.1.0/sqlite-jdbc-3.45.1.0.jar' -OutFile '$SQLITE_JAR'"
fi

CP="build:$SQLITE_JAR"
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" || "$OSTYPE" == "win32" ]]; then
    CP="build;$SQLITE_JAR"
fi

echo ">> Compiling all Java 17+ source files under src/..."
javac -cp "$SQLITE_JAR" -d build $(find src -name "*.java")

echo ">> Executing PhantasmLauncher..."
java -cp "$CP" com.phantasm.launcher.PhantasmLauncher
