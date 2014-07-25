<?php 

class c2dm {
	var $authString = "";

	/**
	 * Get Google login auth token
	 * @see http://code.google.com/apis/accounts/docs/AuthForInstalledApps.html
	 */
	function getAuthToken($username, $password) {

		// Initialize the curl object
		$curl = curl_init();

		curl_setopt($curl, CURLOPT_URL, "https://www.google.com/accounts/ClientLogin");
		curl_setopt($curl, CURLOPT_FOLLOWLOCATION, true);

		$data = array(
			'Email'         => $username,
			'Passwd'        => $password,
			'accountType'   => 'GOOGLE',
			'source'        => 'Google-cURL-Example',
			'service'       => 'ac2dm');

		curl_setopt($curl, CURLOPT_SSL_VERIFYPEER, 0);
		curl_setopt($curl, CURLOPT_POST, true);
		curl_setopt($curl, CURLOPT_RETURNTRANSFER, true);
		curl_setopt($curl, CURLOPT_POSTFIELDS, $data);

		$response = curl_exec($curl);
		curl_close($curl);

		// Get the Auth string
		preg_match("/Auth=([a-z0-9_\-]+)/i", $response, $matches);
		$this->authString = $matches[1];
		return $matches[1];
	}

	/**
	 * Send HTTP POST data form
	 */
	function sendMessage($deviceRegistrationId, $authstr,$msg, $msgType) {
		//$headers[] = 'Authorization: GoogleLogin auth='.$this->authString;
		$headers[] = 'Authorization: GoogleLogin auth='.$authstr;
		$data = array(
			'registration_id' => $deviceRegistrationId,
			'collapse_key' => $msgType,
			'data.message' => $msg // Add data here.
		);

		$curl = curl_init();

		curl_setopt($curl, CURLOPT_URL, "https://android.apis.google.com/c2dm/send");
		curl_setopt($curl, CURLOPT_FOLLOWLOCATION, true);
		curl_setopt($curl, CURLOPT_HTTPHEADER, $headers);
		curl_setopt($curl, CURLOPT_HEADER, 0);
		curl_setopt($curl, CURLOPT_POST, true);
		curl_setopt($curl, CURLOPT_RETURNTRANSFER, true);
		curl_setopt($curl, CURLOPT_POSTFIELDS, $data);

		$respone = curl_exec($curl);
		curl_close($curl);

		return $respone;
	}
}

?>
