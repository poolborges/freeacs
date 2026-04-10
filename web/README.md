FreeACS - Web Server
===========================
This project is a just a part of the whole product.

Overview
--------
The web interface of this product  

MySQL

```
docker run --name xaps_mysql -e MYSQL_ROOT_PASSWORD=root -d mysql:latest -p 3306:3306
docker cp [path-to-db]/mysql xaps_mysql:/tmp/mysql
docker exec -it xaps_mysql bash
$ cd /tmp/mysql
$ mysql -u root -p
****
$ create user 'xaps'@'%' identified by 'xaps';
$ create database xaps;
$ grant all on xaps.* to 'xaps'@'%';
$ exit
$ mysql -u xaps -p xaps < install*.sql
*****
$ exit :)
```
