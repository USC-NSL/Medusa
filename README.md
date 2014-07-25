# Medusa
*A Programming Framework for Crowd-Sensing Applications*

Have you ever wished, in the middle of a research project, for a 
convenient way to recruit smartphone and tablet users worldwide 
to contribute data (images, videos, sound) using the sensors on 
their devices for your project?

Medusa is software that makes this capability, that we call 
crowd-sensing, possible. Medusa will let you program a crowd-sensing 
task easily and its software will automatically handle the tedious 
aspects of crowd-sensing: recruiting contributors, paying them, 
and processing and collecting data from mobile devices. 
Medusa also respects contributor privacy and anonymity.

The [demo video](http://www.youtube.com/watch?v=jL1dGA21ciA) explains 
Medusa with an example.

## Project Details

Medusa was developed by Moo-Ryong Ra in collaboration with Bin Liu, Tom La Porta, Ramesh Govindan, and Matthew McCartney at [USC](http://www.usc.edu).

A thorough description of Medusa can be found at the Networked Systems Lab [project page for Medusa](http://nsl.cs.usc.edu/Projects/Medusa) 

## Getting Started

This document provides explains how to configure the Medusa cloud and client components.

### Requirements

This project is tested on the following platforms, devices, and dependencies:

- Medusa Cloud
    - OS: Ubuntu 9.04, 10.04, and Mint15
        - Basic dependencies
            - ```apt-get install python-mysqldb```
            - ```apt-get install php5-common libapache2-mod-php5 php5-cli php5-curl```
            - ```apt-get install sendmail-bin```
    - TaskTracker
        - python 2.7.x
        - LAMP
            - Tested with Ubuntu 9.04(jaunty), 10.04(lucid), and Mint15
            - Apache web server (tested on 2.2.22)
            - Mysql (tested on 5.5.x)
            - PHP (tested on 5.4.9)
    - WorkerManager
        - ```apache-tomcat-6.0.35``` with the jdk-1.5.x.
    - Websocket (GCM C2D alternative)
        - Requirements
            - PHP
                - [PECL](http://pecl.php.net/) extension for PHP (aka, PEAR)
                    - ```apt-get install php-pear```
                    - ```apt-get install php5-dev```
                    - ```apt-get install php5-mcrypt```
            - [ZeroMQ](http://zeromq.org/) for communication between the Medusa Server and the  WebSocket Server
                - Follow [these](http://zeromq.org/bindings:php) instructions
                    - Download the source, extract, and ```cd``` into it, the perform:
                        - ```./autogen```, ```./configure```, ```make```, and then ```make install```
                - ```pecl install zmq-beta```
                - add ```extension=zmq.so``` line to your to php.ini file
        - [Other Considerations](http://socketo.me/docs/deploy)
            - if you plan on connecting on the order of hundreds of Medusa clients, you may want to update your system’s maximum number of *allowed per-process file descriptors*
                - see [ulimit](http://ss64.com/bash/ulimit.html) for more details
            - [Libevent](http://libevent.org/) speeds up server-side web socket operations
                - ```apt-get install libevent libevent-dev```
                - ```pecl install libevent```
                - add the ```extension=libevent.so``` line to your php.ini
- Android client
    - Depends on the [OpenCV-2.3.1](http://sourceforge.net/projects/opencvlibrary/files/opencv-android/2.3.1/OpenCV-2.3.1-android-bin.tar.bz2/download) library
    - Tested on the following phones
        - Google Nexus 5 with Android 4.4.4
        - Google Nexus I with Android 2.3.6
        - Samsung Galaxy Nexus with Android 4.0.4

### Configure the Medusa cloud components.

Let the root directory of the Medusa Cloud source code be **medusa_cloud**.
Then make the **medusa_cloud** directory available to the web, so that the tasktracker can communicate with the client.

Prepare a linux account to access the mysql database for the Medusa server.
Go to the **medusa_cloud/config/** directory, and install the database schema.

```mysql -u [mysql_username] -p[mysql password] < medusa_db_schema.sql```

This creates a database named *medusa* and a set of tables inside of it.

Update the following configuration files within the directory **medusa_cloud/config/**
    - db_host.info
        - The hostname on which the mysql server containing the medusa database is run
            - ex: ```localhost```
    - db_account.info
        - The mysql_username|mysql_password of a mysql user with read and write permissions to the medusa database 
            - ex: ```medusa_username|medusa_password```
    - port.info
        - ```<medusa_acceptor_port>|<medusa_soc_port>|<web_socket_server_port>|<web_socket_zmq_port>```
    - c2d_messaging_system.info
        - Set to either ws_c2dm (for websocket), c2dm (for GCM), or sms
    - remote_host.info
        - The public facing IP on which the mdscript_acceptor.py is run

Login to mysql database, find CS_env table in the *medusa* database. Change the URLs appropriately.

#### Configure the WorkerManager
Install the Apache Tomcat server, then extract **medusa_cloud/workermanager/medusa_hit_server.tar.gz** to the **apache-tomcat-root/webapps/medusa_hit_server/** directory. 
Verify that the *CS_env* table in the *medusa* database has the same information.
Extract **medusa_cloud/workermanager/config_files.tar.gz** to **/opt/config_files/**.

#### Permissions
You may be unable to store uploaded data or to run the programs generated by the MedAuthor web authoring tool. 
If this is the case, you may need to make the account that runs the medusa server and the account for the web server, e.g. *www-data*, in the same group. Then, go to the **medusa_cloud/tasktracker/** and give write permissions for a set of directories as follows:

```chmod g+w data logs program```

... and restart the apache web server.

#### Running the Medusa Cloud Server

The medusa cloud requires two daemon processes running in the background. 
In the **medusa_cloud/tasktracker/** directory, run the following commands.

```nohup ./mdscript_runner.py & ```
```nohup ./mdscript_acceptor.py & ```

If using websockets and you get an error like this:
*Failed to bind the ZMQ: Address already in use* 
Restart the server. 
If the issue still persists, change the ZMQ port used on line 13 of **medusa_cloud/ws_server_php/bin/chat-server.php**

Finally, if using GCM, you will need a Google GCM account. Register an account and modify the API_KEY stored in **medusa_cloud/config/GCM.info**

Now, the Medusa cloud is ready to run.

### Android client setup

Open the G.java file in **medusa_android/Medusa** project. Change the following variables appropriately.

First, to use the TaskTracker properly, change the following entries.

```java
/* Task tracker location */
public static final String SERVER_URL = "http://xxx.xxx.xxx.xxx";
public static final String URIBASE_UPLOAD = SERVER_URL + "/Medusa/medusa-cloud/tasktracker/web_tt_upload.php";
public static final String URIBASE_REPORT = SERVER_URL + "/Medusa/medusa-cloud/web_tt_service.php";
```

Second, you need a master account to use Google's GCM notification service.

```java
//GCM
public static final String SENDER_ID = "GCM_SENDER_ID_HERE";
```

Third, to use reverse incentive mechanism as in the auditioning app, you should properly configure the following two values.

```java
/* Worker's AMT Requestor ID */
public static final String AMT_WRID = "AKIAJ4DYABCDEFGHIJKL";
/* Worker's AMT Requestor Key. */
public static final String AMT_WRKEY = "oyg+tsrPO3cQ85abcdefghijklmnopqrstuvwxyz";
```

Fourth, configure the websocket address location and port number

```java
/* Web Socket Server*/
public static String WSS_HOSTNAME = "127.0.0.1";
public static String WSS_PORT = "9002";```

The first and the second are mandatory. The third and fourth are optional depending on your usage. Compile **medusa_android>/Medusa** project and install it in a phone.

### Run the HelloWorld app.
To verify all configurations are correct, execute a simple app.

Run Medusa client on the phone. Setup the client's WID using the menu or the automatic popup window, say **wid**.

On the server, go to **medusa_cloud/tasktracker/program/**. Find hello_ws.xml, and set the **wwid** field the the **wid** you entered on the client.

To run the program, at **medusa/cloud/tasktracker/**. execute the following command.

```python ./run_xml.py program/hello_ws.xml```

If you can see or here a *HELLO WORLD* message with voice instruction on the phone, the whole system is configured correctly.