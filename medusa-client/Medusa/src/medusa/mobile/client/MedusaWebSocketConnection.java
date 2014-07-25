/**
 * 'MedusaWebSocketConnection'
 *
 * @author   : Matt McCartney (mmccartn@usc.edu)
 **/

package medusa.mobile.client;

import android.os.Handler;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import de.tavendo.autobahn.WebSocket;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketConnectionHandler;
import de.tavendo.autobahn.WebSocketException;

public final class MedusaWebSocketConnection {

	private final String wsuri;
	private final Handler handler;
	private final WebSocket mConnection;
	private boolean isConnecting = false;
	private boolean isConnected = false;
	private int wsid;
	private String medusaid;
	private TextViewLogger logView = null;
	private final String pong;

	private class WSMsg {

		@SerializedName("type")
		public String type;
		@SerializedName("payload")
		public String payload;

		public WSMsg(String type, String msg) {
			this.type = type;
			this.payload = msg;
		}

		@Override
		public String toString() {
			return "{type: " + type + ", payload: " + payload + "}";
		}
	}

	public MedusaWebSocketConnection(String medusaid) {
		super();
		handler = new Handler();
		this.medusaid = medusaid;
		mConnection = new WebSocketConnection();
		wsuri = "ws://" + G.WSS_HOSTNAME + ":" + G.WSS_PORT;
		pong = (new Gson()).toJson(new WSMsg("pong", ""));
	}

	public MedusaWebSocketConnection(String medusaid, TextViewLogger l) {
		this(medusaid);
		logView = l;
	}

	public void updateMedusaid(String mid) {
		medusaid = mid;
		sendMessage("medusaid", medusaid);
	}
	
	public boolean isConnected() {
		return mConnection != null && mConnection.isConnected();
	}

	public void disconnect() {
		if (isConnected()) {
			if (logView != null) {
				logToBoth("WS Status: Disconnecting.");
			}
			mConnection.disconnect();
		} else if (logView != null) {
			logToBoth("WS Disconnect: mConnection!=null:" 
					+ Boolean.toString(mConnection!=null) 
					+ ", mConnection.isConnected():" 
					+ Boolean.toString(mConnection.isConnected())
					+ ", isConnected:" 
					+ Boolean.toString(isConnected)
			);
		}
	}

	public void connect() {
		
		try {
			if (mConnection != null && !mConnection.isConnected() && !isConnecting) {
				isConnecting = true;
				if (logView != null) {
					logToBoth("WS Status: Connecting to " + wsuri + " ..");
				}
				mConnection.connect(wsuri, new WebSocketConnectionHandler() {
					@Override
					public void onOpen() {
						isConnected = true;
						isConnecting = false;
						if (logView != null) {
							logToBoth("WS Status: Connected to " + wsuri);
						}
						updateMedusaid(medusaid);
					}

					@Override
					public void onTextMessage(String payload) {
						Gson gson = new Gson();
						WSMsg msg = gson.fromJson(payload, WSMsg.class);
						/*if (logView != null) {
							logToBoth("WS msg: " + gson.toJson(msg));
						}*/
						handleMessage(msg);
						isConnecting = false;
					}

					@Override
					public void onClose(int code, String reason) {
						isConnecting = false;
						isConnected = false;
						if (logView != null) {
							logToBoth("WS Status: Connection lost.");
						}
					}
				});
			} else if (logView != null) {
				logToBoth("WS Connect: mConnection!=null:" 
						+ Boolean.toString(mConnection!=null) 
						+ ", !mConnection.isConnected():" 
						+ Boolean.toString(!mConnection.isConnected())
						+ ", !isConnecting:" 
						+ Boolean.toString(!isConnecting)
						+ ", !isConnected:" 
						+ Boolean.toString(!isConnected)
				);
			}
		} catch (WebSocketException e) {
			if (logView != null) {
				logToBoth("WS error:", e);
			}
		}
	}

	private void sendMessage(String type, String msg) {
		if (mConnection != null && mConnection.isConnected() && !isConnecting) {
			Gson gson = new Gson();
			mConnection.sendTextMessage(gson.toJson(new WSMsg(type, msg)));
		} else {
			if (logView != null) {
				logToBoth("WS abandoning message transfer: " + msg);
			}
		}
	}

	private void handleMessage(WSMsg msg) {
		if (msg.type.equals("websocketid")) {
			wsid = Integer.parseInt(msg.payload);
			/**
			 * I'm not planning on using the web socket connection id by the 
			 * medusa interpreter process, so for now I see no reason to sync
			 * but if I wanted to, the below command will do just that
			 * MedusaUtil.syncC2DMVars(G.C2DM_ID, msg.payload);
			 */
		} else if (msg.type.equals("cmdpush")) {
			if (logView != null) {
				logToBoth("MedusaWS Message * MWS-Msg:" + msg);
			}
		    MedusaUtil.invokeMedusalet(msg.payload);
		} else if (msg.type.equals("ping")) {
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					mConnection.sendTextMessage(pong);
				}
			}, 1000L);
		}
	}

	/** show log on screen **/
	private void logToScreen(String string) {
		logView.printlnwt(string);
	}

	/** write log to both screen and file **/
	private void logToBoth(String string) {
		Log.d(G.TAG, string);
		logToScreen(string);
	}

	/** write log to both screen and file **/
	private void logToBoth(String string, Exception e) {
		Log.e(G.TAG, string, e);
		logToScreen(string);
		logToScreen(e.getMessage());
	}

}
