FreeACS Fusion - Core Server
============================
This project is a just a part of the whole product. 


Overview
--------
The Core Server could just as well be named "Background Server" because it does
just that kind of stuff. Very simply put it does cleanup, make reports and some other
background processing. It has a vital importance is some tasks (Trigger
processing, Job processing). One should not run more than one instance of
this server, that would just make for trouble. The functions it performs are:

* Delete old jobs
* Delete old scripts
* Delete old syslog
* Detect missing heartbeat from devices (check syslog data)
* Enforce job rules (stop jobs if necessary)
* Generate reports
* Execute scripts (that's why we have the Shell-dependency)
* Release triggers

See docs-folder for more information
