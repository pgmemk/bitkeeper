bitkeeper
=========

Bitjoe uses Bitkeeper to save business transactions. 
1. Bitcoin blockchain is a precious resources and we can only put a hash of the transaction in it. 
2. The body of the transaction bitjoe sends to bitkeeper for keeping.

In this incarnation bitkeeper uses Kademlia DHT (distributed hash table) implementation by http://tomp2p.net/
Kademlia is a state of the art DHT used by practically all mainstream p2p programs, like bittorrent.

Running
========================
Bitkeeper provides RESTful service with 2 parameters:
key and val
if both are specified, value is saved for a key provided, e.g. http://127.0.0.1:8080?key=k1&val=v1
If only key is specified, then value is returned: e.g. http://127.0.0.1:8080?key=k1

Installation
========================

1. Clone this repo using: git clone https://github.com/urbien/bitkeeper
2. run: java io.tradle.bitkeeper.KeeperServer http-server-ip-address http-server-port DHT-server-port DHT-second-server-port DHT-server-ip-address

Then run a second bitkeeper with reverse DHT ports and a different http port: e.g.: 127.0.0.1 8081 7001  7002 127.0.0.1
Now you can send save value on one bitkeeper server and pick it up on another bitkeeper server, e.g.:
http://127.0.0.1:8080?key=k1&val=v1
http://127.0.0.1:8081?key=k1
