curl --header "Authorization: GoogleLogin auth=$1" "https://android.apis.google.com/c2dm/send" -d registration_id=$2 -d "data.payload=$3" -d collapse_key=0
