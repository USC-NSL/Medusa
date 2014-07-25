<?php
namespace MedusaWebSocketApp;
use Ratchet\MessageComponentInterface;
use Ratchet\ConnectionInterface;

class Chatter implements MessageComponentInterface {
    
    protected $DEBUG;

    protected $clients;

    protected $commands;

    protected $ping;

    public function __construct() {
        $this->DEBUG = true;
        $this->commands = array();
        $this->clients = new \SplObjectStorage;
        $this->ping = json_encode(array('type' => 'ping', 'payload' => 'null'));
    }

    public function onOpen(ConnectionInterface $conn) {
        $cid = $conn->resourceId;
        
        // Store the new connection to send messages to later
        $this->clients->attach($conn);

        if ($this->DEBUG) {
            echo "New connection from {$cid}\n";
        }
    }

    public function onMessage(ConnectionInterface $from, $msg) {
        $cmd = json_decode($msg, false);
        $type = $cmd->{'type'};
        $payload = $cmd->{'payload'};

        if ($type == 'medusaid') {
            $from->resourceId = $payload;
            if (array_key_exists($payload, $this->commands)) {
                foreach ($this->commands[$payload] as $old_cmd) {
                    $from->send(json_encode($old_cmd));
                }
                unset($this->commands[$payload]);
            }
            $from->send($this->ping);
            if ($this->DEBUG) {
                echo "Received message: $msg from client {$from->resourceId}.\n";
            }
        } else if ($type == 'pong') {
            $from->send($this->ping);
        }

    }

    public function onClose(ConnectionInterface $conn) {
        // The connection is closed, remove it, as we can no longer send it messages
        if ($this->DEBUG) {
            echo "Client {$conn->resourceId} has disconnected.\n";
        }
        $this->clients->detach($conn);
    }

    public function onError(ConnectionInterface $conn, \Exception $e) {
        $conn->close();
        if ($this->DEBUG) {
            echo "An error has occurred: {$e->getMessage()}\n";
        }
    }

    /**
     * @param entry the JSON'ified string we'll receive from the medusa interpreter process vio ZeroMQ
     */
    public function onMedusaCmdPush($entry) {
        $json = json_decode($entry, false);
        $mid = $json->{'medusaid'};
        $msg = $json->{'payload'};
        $cmd = array('type' => 'cmdpush', 'payload' => $msg);

        // Forward the command to the appropriate client
        if (!$this->sendTo($mid, $cmd)) {
            if (!in_array($mid, $this->commands)) {
                $this->commands[$mid] = array();   
            }
            array_push($this->commands[$mid], $cmd);
        }

        if ($this->DEBUG) {
            echo "onMedusaCmdPush: $entry\n";
        }
    }

    /**
     * @param mid a string representing a medusa id
     * @param msg an array containing the 'type' and 'payload' string keys
     */
    protected function sendTo($mid, $msg) {
        $sent = false;
        foreach ($this->clients as $client) {
            if ($client->resourceId == $mid) {
                $client->send(json_encode($msg));
                $sent = true;
            }
        }
        return $sent;
    }

    /**
     * echo the connection resource Id back to the client
     */
    protected function echoResourceId($conn, $cid) {
        $conn->send(json_encode(
                    array(
                        'type'      => 'websocketid',
                        'payload'   => $cid
                )
            )
        );
    }
}