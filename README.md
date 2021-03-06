# Manager Server [![Build Status](https://travis-ci.com/Citos-CTI/Manager-Server.svg?branch=master)](https://travis-ci.com/Citos-CTI/Manager-Server)

This is the backend which connects to the specific telephony servers. The Manager Clients will register here.

## Setup
### Network Structure/Architecture 
It's not required to run the Citos CTI Manager Server directly on the telephony server. It's highly recommended to adjust your firewall and network settings (espc. between Citos and telephony server) to prevent possible attacks.
### Requirements:
- Asterisk Server (tested with versions 12-15)
- Java 8 or higher Runtime
### Download
Download here:
[Citos Manager Server Download](https://github.com/Citos-CTI/Manager-Server/releases/download/v1.0.3.3/Citos_Server.zip)
### Trial/Installation
- Unzip the files
- You can either install the Citos Manager Server or try it as a Docker Container.
#### Trial
The trial docker container is shipped with everything predefined. Currently the trial version only works with a dummy telephony system. A Asterisk trial will be delivered as soon as possible. Depending on your configuration you may have to run the docker commands elevated (with sudo).
```
docker pull citos/manager-server-test:latest
docker run -d -p 12345:12345 citos/manager-server-test
```
A server is online on which you can register one client with the login:
```
user: vogel
password: vogel
```
You can now add user extensions in the client. The preregistered extensions are in the range from 201 - 220.
#### Installation
- Execute install.sh
- Go to /etc/citos-server and edit the server.conf config file as explained in the config file section
- Add users via the import folder 

### Configuration
Right now there are 3 server plugins available: 
- Asterisk 
- Dummy
- DummyHeadless (Version of Dummy which doesn't open a gui)
#### Asterisk
- Start with enabling the remote connection from Citos Manager Server to your Asterisk Server
- In /etc/asterisk/manager.conf you have to add an entry like this:
```
[citos]
secret=citos-pw
permit=0.0.0.0/0.0.0.0
read=all
write=all
```

Caution! Please restrict the access only on the features you need and only to the host with Citos Manager Server running.

- Your server.conf (in /etc/citos-server or in the application your .jar lies) should look like this:
```
plugin=asterisk
own_server_port=12345         // Port you want for Citos Manager Server
username=citos 
password=citos-pw
server_address=192.168.178.12 // IP or Hostname of your Asterisk Server
port=5038                     // Port of your Asterisk Server
```
#### Dummy(Headless)
For running a test with a dummy plugin use this config:
```
plugin=dummyheadless
own_server_port=12345
import_folder=/home/johannes
```
