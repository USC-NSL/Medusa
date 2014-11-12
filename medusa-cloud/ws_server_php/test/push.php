<?php
    $entryData = array(
        'name'      => 'Evently',
        'created'   => time(),
        'num'       => 0,
        'rand'      => 0.7,
        'flag'      => false
    );

    // This is our new stuff
    $context = new ZMQContext();
    $socket = $context->getSocket(ZMQ::SOCKET_PUSH, 'my pusher');
    $socket->connect("tcp://localhost:5555");

    $socket->send(json_encode($entryData));