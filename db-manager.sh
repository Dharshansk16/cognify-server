#!/bin/bash

# Cognify Backend - Database Management Script

echo "🗄️  Cognify Database Manager"
echo "=============================="
echo ""
echo "1. Start databases"
echo "2. Stop databases"
echo "3. View database logs"
echo "4. Reset databases (delete all data)"
echo "5. Connect to PostgreSQL"
echo "6. Open Neo4j Browser"
echo "7. Check database status"
echo "8. Exit"
echo ""
read -p "Select option (1-8): " option

case $option in
    1)
        echo "🚀 Starting databases..."
        docker-compose up -d
        echo "✅ Databases started!"
        echo "   PostgreSQL: localhost:5433"
        echo "   Neo4j HTTP: localhost:7474"
        echo "   Neo4j Bolt: localhost:7687"
        ;;
    2)
        echo "🛑 Stopping databases..."
        docker-compose down
        echo "✅ Databases stopped!"
        ;;
    3)
        echo "📋 Database logs:"
        echo ""
        echo "Choose database:"
        echo "1. PostgreSQL"
        echo "2. Neo4j"
        read -p "Select (1-2): " db_option
        if [ "$db_option" = "1" ]; then
            docker logs cognify-postgres -f
        else
            docker logs cognify-neo4j -f
        fi
        ;;
    4)
        echo "⚠️  WARNING: This will delete all data!"
        read -p "Are you sure? (yes/no): " confirm
        if [ "$confirm" = "yes" ]; then
            echo "🗑️  Resetting databases..."
            docker-compose down -v
            docker-compose up -d
            echo "✅ Databases reset and restarted!"
        else
            echo "❌ Reset cancelled"
        fi
        ;;
    5)
        echo "🔌 Connecting to PostgreSQL..."
        docker exec -it cognify-postgres psql -U cognify -d cognifydb
        ;;
    6)
        echo "🌐 Opening Neo4j Browser..."
        open http://localhost:7474 || xdg-open http://localhost:7474 || echo "Please open http://localhost:7474 in your browser"
        echo "   Username: neo4j"
        echo "   Password: neo4j123"
        ;;
    7)
        echo "📊 Database Status:"
        docker-compose ps
        ;;
    8)
        echo "👋 Goodbye!"
        exit 0
        ;;
    *)
        echo "❌ Invalid option"
        exit 1
        ;;
esac
