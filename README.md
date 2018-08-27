# Manager Server [![Build Status](https://travis-ci.com/Citos-CTI/Manager-Server.svg?branch=master)](https://travis-ci.com/Citos-CTI/Manager-Server)

This is the backend which connects to the specific telephony servers. The Manager Clients will register here.

## Setup
### Network Structure/Architecture 
It's not required to run the Citos CTI Manager Server directly on the telephony server. It's highly recommended to adjust your firewall and network settings (espc. between Citos and telephony server) to prevent possible attacks.
### Requirements:
- Asterisk Server (tested with versions 12-15)
- Java 9 Runtime
### Download
Download here:
[Citos Manager Server Download](https://github.com/Citos-CTI/Manager-Server/releases/download/v1.0.3.3/Citos_Server.zip)
### Trial/Installation
- Unzip the files
- You can either install the Citos Manager Server or try it.
#### Trial
Before trying you must setup an user and add a config file. In the zip is already an example server.conf.example
For running a test with a dummy plugin use this config:
```
plugin=dummyheadless
own_server_port=12345
import_folder=/home/johannes
```
To set up an user place a user.csv at the position where the parameter import_folder points.

The .csv should look like this:
```
user1,password1,extension1
user2,password2,extension2

```
- Run the Server with:
```
java -jar citos-server.jar
```
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
#### Dummy(Headless)
