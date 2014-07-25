<?php

	$userid = 'matt';
	$msg = 
'<xml>
	<app>
	<input>
		<name>HelloWorld</name>
		<wwid>matt</wwid>
		<cmdpush>ws_c2dm</cmdpush>
		<gvar>GVARINPUT=</gvar>
		<rrkey>ZDijBdROJrc6TViueBWYVhD5o8hSVzv9Civaj+Zl</rrkey>
		<timeout>7 hour</timeout>
	</input>
		<stage>
			<name>HelloWorld</name> <type>SPC</type>
			<binary>medusalet_helloworld</binary>		
			<trigger>none</trigger> 
			<toast>Hello</toast>
		</stage>
	</app>
</xml>';

    // Use ZeroMQ to forward the message to the Web Socket Server
    $socket = (new ZMQContext())->getSocket(ZMQ::SOCKET_PUSH, 'the chatter');
    $socket->connect("tcp://localhost:5555");
    $socket->send(json_encode(
    	array(
    		'medusaid'	=> $userid,
        	'payload'	=> $msg
    	)
    ));