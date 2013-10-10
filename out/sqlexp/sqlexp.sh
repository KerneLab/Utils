#! /bin/sh
#java -jar sqlexp.jar -host localhost -port 1521 -user test -pass test -db orcl -charset UTF-8 -file ./demo_table.sql -table demo_table -columns name val -keys id -rule "where id>1 order by id" -mode update
java -jar sqlexp.jar -host localhost -port 1521 -user test -pass test -db orcl "$@"