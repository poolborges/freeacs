FreeACS Fusion - Syslog Server
===============================

Fusion Syslog Server is a standard syslog server, fully compliant with RFC3164. The
value of such a server is very high if you have devices that are capable of logging syslog
messages. In that case you can redirect your devices to log to Fusion Syslog Server, and
start to take advantage of all the information the devices can give you.


Overview
--------
This syslog server is of course made to support this product, but it should in
theory work as a normal syslog server. There are a number of features with
the server, and a lot of effort has gone into logging, handling of disk-space,
failover (which I have disabled lately - too much trouble).

One major issue with the server is something called Syslog Event (specified
in Web/Shell). The server will check for such event and perform certain actions
based upon them.

Read more in docs