FreeACS Fusion - TR-069 Server
==============================
This project is a just a part of the whole product.


Overview
--------
This server is an essential part of this product, as it is running the TR-069
communication with the various devices (routers, ATA, RGWs, etc) connected to
it. The server supports TR-069 fairly well supporting all REQUIRED methods for
an ACS (see http://www.broadband-forum.org/technical/download/TR-069_Amendment-5.pdf 
chapter 3.6) with the sole exception of AutonomousTransferComplete. 

The server is responsive and offers a wide range of features. It has been
tested with several hundred thousands of devices (at Telenor). It has also been
tested with a range of devices:

* Eltek R7121  
* Eltek R7921  
* Inteno routers  
* Ping Communication RGW208EN  
* Ping Communication IAD208AN  
* Ping Communication NPA201E  
* Speedtouch 585i  
* Zyxel P2602  

No doubt the server can work with any other TR-069 device, if the device does 
not contain very troublesome bugs (which it very often does!).

The features offered by the server are:

* TR-069 provisioning of configuration and firmware
* TR-111 support (in combination with the STUN server (https://github.com/freeacs/stun.git))
* TR-XXX generic datamodel support (TR-098, TR-104, TR-181, etc.
* Automatic discovery of a new device - create necessary objects in database
* Download limit management
* Quirks handling of device not complying with standard
* Execution of complex provisioning, execution order, time of week, etc
* Spread-of-traffic - avoid troublesome peak loads of devices connecting
* Detailed logging, to file and syslog
* Two test-systems, to enable really complex testing of the devices

Also see the documentation found in the docs-folder

### Development
```bash
docker run --name acs -e MYSQL_USER=acs -e MYSQL_PASSWORD=acs -e MYSQL_DATABASE=acs -e MYSQL_RANDOM_ROOT_PASSWORD=true -p 3306:3306 -d --platform=linux/amd64 mysql:5.7
mysql -h 127.0.0.1 -u acs -pacs -P 3306 acs < tables/src/main/resources/install.sql
```
 

