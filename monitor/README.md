FreeACS Fusion - Monitor Server
===============================
This project is a just a part of the whole product. 



Overview
--------
Monitor server is responsible for monitoring all the other servers of this 
product (TR-069 server, Web server, STUN server, etc). It also has the 
responsibility of sending emails if some "situation" has occurred. A quick
summary:

* Monitoring all other servers of this product
* Providing a web interface to see the state + version of all servers
* Providing one URL to use in an external monitoring system (ex: HP OpenView)
* Sending a heartbeat message every morning at 7 am to indicate that the product is running
* Sending trigger notifications

This server is very lightweight/simple, and it uses the Scheduler-system of
the Common-project a lot. 
