#! /bin/sh
#java -jar sqlexp.jar -user test -pass test -tns orcl -charset UTF-8 -file ./demo_table.sql -table demo_table -columns name val -keys id -rule "where id>1 order by id" -mode update
java -jar sqlexp.jar -user test -pass test -tns orcl "$@"