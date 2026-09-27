#!/bin/sh
set -e
DB="${MYSQL_DATABASE:-smart-campus}"
mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 "$DB" < /tmp/cleanup-paper-test-data.sql
mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 "$DB" < /tmp/seed-paper-test-data.sql
