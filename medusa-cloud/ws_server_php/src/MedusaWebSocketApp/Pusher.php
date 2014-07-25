<?php
namespace MedusaWebSocketApp;
use Ratchet\ConnectionInterface;
use Ratchet\Wamp\WampServerInterface;

class Pusher implements WampServerInterface {
    /**
     * A lookup of all the topics clients have subscribed to
     */
    protected $subscribedTopics = array();

    public function onSubscribe(ConnectionInterface $conn, $topic) {
        $this->subscribedTopics[$topic->getId()] = $topic;
        echo "onSubscribe for topic id {$topic->getId()}\n";
    }
    public function onUnSubscribe(ConnectionInterface $conn, $topic) {
        echo "onUnSubscribe for $topic\n";
    }
    public function onOpen(ConnectionInterface $conn) {
        echo "onOpen\n";
    }
    public function onClose(ConnectionInterface $conn) {
        echo "onClose\n";
    }
    public function onCall(ConnectionInterface $conn, $id, $topic, array $params) {
        // In this application if clients send data it's because the user hacked around in console
        $conn->callError($id, $topic, 'You are not allowed to make calls')->close();
    }
    public function onPublish(ConnectionInterface $conn, $topic, $event, array $exclude, array $eligible) {
        // In this application if clients send data it's because the user hacked around in console
        $conn->close();
    }
    public function onError(ConnectionInterface $conn, \Exception $e) {
        echo "onError: $e\n";
    }

    /**
     * @param string JSON'ified string we'll receive from ZeroMQ
     */
    public function onMedusaCmdPush($entry) {
        echo "onMedusaCmdPush: $entry\n";
        $entryData = json_decode($entry, true);

        // If the lookup topic object isn't set there is no one to publish to
        if (!array_key_exists($entryData['name'], $this->subscribedTopics)) {
            return;
        }

        $topic = $this->subscribedTopics[$entryData['name']];

        // re-send the data to all the clients subscribed to that catagory name
        $topic->broadcast($entryData);
    }
}