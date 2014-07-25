<?php
    require dirname(__DIR__) . '/vendor/autoload.php';

    $port = intval($argv[1]);

    $loop   = React\EventLoop\Factory::create();
    $chatter = new MedusaWebSocketApp\Chatter;

    // Listen for the web server to make a ZeroMQ push after an ajax request
    $context = new React\ZMQ\Context($loop);
    $pull = $context->getSocket(ZMQ::SOCKET_PULL);
    // Binding to 127.0.0.1 means the only client that can connect is itself
    $pull->bind('tcp://127.0.0.1:' . $argv[2]); 
    $pull->on('message', array($chatter, 'onMedusaCmdPush'));

    // Set up our WebSocket server for clients wanting real-time updates
    $webSock = new React\Socket\Server($loop);
    $webSock->listen($port, '0.0.0.0'); // Binding to 0.0.0.0 means remotes can connect
    $webServer = new Ratchet\Server\IoServer(
        new Ratchet\Http\HttpServer(
            new Ratchet\WebSocket\WsServer(
                $chatter
            )
        ),
        $webSock
    );

    $loop->run();