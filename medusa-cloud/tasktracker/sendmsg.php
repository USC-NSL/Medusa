<?php

	$userid = $argv[1];
	$msg = $argv[2];
    $port = $argv[3];

    // Use ZeroMQ to forward the message to the Web Socket Server
    $socket = (new ZMQContext())->getSocket(ZMQ::SOCKET_PUSH, 'the chatter');
    $socket->connect("tcp://localhost:$port");
    $socket->send(json_encode(
    	array(
    		'medusaid'	=> $userid,
        	'payload'	=> $msg
    	)
    ));