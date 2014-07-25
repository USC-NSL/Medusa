/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package medusa.mobile.client;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import medusa.mobile.client.R;


public class MedusaDataReviewDialog extends Activity 
{
    private static final String TAG = "ReviewDialog";

    public static final String TYPE_PREVIEW = "preview";
    public static final String TYPE_REVIEW = "review";
    public static final String TYPE_USERINPUT = "userinput";
    
    private static final int DIALOG_REVIEW_YESNO = 1;
    private static final int DIALOG_REVIEW_LABELING = 2;
    private static final int DIALOG_REVIEW_TEXTDESC = 3;
    
    Bundle bundle;

    private String type;		// request type: "review" or "userinput"
        
    /* dialog specific */
    private String displayMsg;
    private String[] reviewLabels;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);

        bundle = getIntent().getExtras();
        type = bundle.getString("type");
        
        if (type.equals(TYPE_REVIEW) == true || type.equals(TYPE_PREVIEW) == true) {
	        String medusaletName = bundle.getString("medusalet_name");
	        String data_path= bundle.getString("paths").replace("|", "\t\r\n");
	        String pid = bundle.getString("pid");
	        String qid = bundle.getString("qid");;
	        String muid = bundle.getString("uids");;
	        String review_type = bundle.getString("review_type");
	        
	        Log.i(TAG, "* review_type=" + review_type + "\r\n"
	        			+ "medusalet=" + medusaletName + "\r\n"
	        			+ "paths=" + data_path + "\r\n"
	        			+ "muids=" + muid);
	        
	        displayMsg = "- Medusalet: " + medusaletName + "\r\n" 
					   + "- Files:\r\n" + data_path + "\r\n"
					   + "- PID: " + pid + "\r\n"
					   + "- QID: " + qid + "\r\n"
					   + "- UID: " + muid;
	        
	        if (type.equals(TYPE_PREVIEW) == true) { 
	        	displayMsg += "\r\n" + "- Review: " + bundle.getString("tagged_reviews");
	        }
	
	        if (review_type.equals(MedusaletBase.REVIEW_YESNO) == true) {
	        	showDialog(DIALOG_REVIEW_YESNO);
	        }
	        else if (review_type.equals(MedusaletBase.REVIEW_LABELING) == true) {
	        	String rlStr = bundle.getString("review_options");
	            reviewLabels = rlStr.split("\\|");
	            
	        	showDialog(DIALOG_REVIEW_LABELING);
	        }
	        else if (review_type.equals(MedusaletBase.REVIEW_TEXTDESC) == true) {
	        	showDialog(DIALOG_REVIEW_TEXTDESC);
	        }
	        else {
	        	Log.e(TAG, "! Unknown review type=" + review_type);
	        }
        }
        else if (type.equals(TYPE_USERINPUT) == true) {
        	displayMsg = "- Msg: " + bundle.getString("msg") + "\r\n"
        			   + "- TID: " + bundle.getString("tid") + "\r\n"
        			   + "- Medusalet: " + bundle.getString("medusalet_name") + "\r\n"
					   + "- PID: " + bundle.getString("pid") 
					   + ", QID: " + bundle.getString("qid") + "\r\n";
        	
        	showDialog(DIALOG_REVIEW_TEXTDESC);
        }
    }
    
    private void reportReview(String review)
    {
    	String uri = G.URIBASE_REPORT + "?action=reviewReport&amtid=" + G.getUID()
    				+ "&pid=" + bundle.getString("pid") + "&qid=" + bundle.getString("qid") 
    				+ "&muid=" + bundle.getString("uids") + "&mpath=" + bundle.getString("paths")
    				+ "&review=" + MedusaUtil.base64Encode(review);
		MedusaTransferManager.requestHttpReq(bundle.getString("medusalet_name"), "GET", uri, null);
		
		Log.d(TAG, "* reportReview message is sent. msg=" + review);
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_REVIEW_YESNO:
            return new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_email)
                    .setTitle("Crowd Sensing Review")
                    .setMessage(displayMsg)
                    .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            Intent itnt = new Intent(G.AQUA_SERVICE_NAME);
                            if (type.equals(TYPE_PREVIEW) == true) {
                            	bundle.putString("cmd", MedusaLoaderService.CMD_RESUME_UPLOAD);
                            }
                            else if (type.equals(TYPE_REVIEW) == true) {
                            	bundle.putString("cmd", MedusaLoaderService.CMD_UPDATE_REVIEW);
                            	bundle.putString("review_content", "yes");
                            }
                            itnt.putExtras(bundle);
                            startService(itnt);
                            
                            dialog.dismiss();
                            finish();
                        }
                    })
                    .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        	if (type.equals(TYPE_REVIEW) == true) {
                        		Intent itnt = new Intent(G.AQUA_SERVICE_NAME);
                            	bundle.putString("cmd", MedusaLoaderService.CMD_UPDATE_REVIEW);
                            	bundle.putString("review_content", "no");
	                            itnt.putExtras(bundle);
	                            startService(itnt);
                        	}
                            
                            dialog.dismiss();
                            finish();
                        }
                    })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        public void onCancel(DialogInterface dialog) {
                            finish();
                        }
                    }).create();
            
        case DIALOG_REVIEW_LABELING:
	        {
	        	LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
	        	View layout = inflater.inflate(R.layout.review_labeling, null);
	        	
	        	TextView text = (TextView) layout.findViewById(R.id.textViewDesc);
	        	text.setText(displayMsg);
	        	
	        	Spinner spinner = (Spinner) layout.findViewById(R.id.spinnerLabel);
	        	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, reviewLabels);
	        	spinner.setAdapter(adapter);
	        	spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
						Log.d(TAG, "* arg2=" + arg2 + " arg3=" + arg3);
					}
	
					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
					
					}
	        	});
				spinner.setSelection(0);
	        	
	        	AlertDialog dialog = new AlertDialog.Builder(this)
	        			.setIcon(android.R.drawable.ic_input_add)
	        			.setTitle("Crowd Sensing Review")
			            .setView(layout)
			            .setPositiveButton("DONE", new DialogInterface.OnClickListener() {
			                public void onClick(DialogInterface dialog, int whichButton) {
			                	/* report review result to the server. */
			                    Spinner spinner = (Spinner) ((AlertDialog)dialog).getWindow().findViewById(R.id.spinnerLabel);
			                    reportReview( (String) spinner.getSelectedItem() );
			                    
			                	Intent itnt = new Intent(G.AQUA_SERVICE_NAME);
			                    bundle.putString("cmd", MedusaLoaderService.CMD_UPDATE_REVIEW);
			                    bundle.putString("review_content", (String) spinner.getSelectedItem());
			                    itnt.putExtras(bundle);
			                    startService(itnt);
			                    
			                    dialog.dismiss();
			                    finish();
			                }
			            })
			            .setNegativeButton("DISMISS", new DialogInterface.OnClickListener() {
			                public void onClick(DialogInterface dialog, int whichButton) {
			                    dialog.dismiss();
			                    finish();
			                }
			            })
			            .setOnCancelListener(new DialogInterface.OnCancelListener() {
			                public void onCancel(DialogInterface dialog) {
			                    finish();
			                }
			            }).create();
	
	        	return dialog;
	        }
        case DIALOG_REVIEW_TEXTDESC:
	        {
	        	LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
	        	View layout = inflater.inflate(R.layout.review_textdesc, null);
	        	
	        	TextView text = (TextView) layout.findViewById(R.id.textViewDesc);
	        	text.setText(displayMsg);
	        	
	        	String title = (type.equals(TYPE_REVIEW) == true ? "Crowd Sensing Review" : "User Input Required");
	        	
	        	AlertDialog dialog = new AlertDialog.Builder(this)
	        			.setTitle(title)
			            .setView(layout)
			            .setPositiveButton("DONE", new DialogInterface.OnClickListener() {
			                public void onClick(DialogInterface dialog, int whichButton) {
			                	if (type.equals(TYPE_REVIEW) == true) {
			                		EditText editText = (EditText) ((AlertDialog)dialog).getWindow().findViewById(R.id.editMultilineText);
				                    reportReview( editText.getEditableText().toString() );
				                    
			                		Intent itnt = new Intent(G.AQUA_SERVICE_NAME);
				                    bundle.putString("cmd", MedusaLoaderService.CMD_UPDATE_REVIEW);
				                    bundle.putString("review_content", editText.getEditableText().toString());
				                    itnt.putExtras(bundle);
				                    startService(itnt);
			                	}
			                	else if (type.equals(TYPE_USERINPUT) == true) {
			                		EditText editText = (EditText) ((AlertDialog)dialog).getWindow().findViewById(R.id.editMultilineText);
			                		
			                		String uri = G.URIBASE_REPORT + "?action=report&type=cred_" + bundle.getString("tid") 
			                				+ "&amtid=" + G.getUID() + "&qtype=" + bundle.getString("medusalet_name")
			                				+ "&pid=" + bundle.getString("pid") + "&qid=" + bundle.getString("qid")	
			                				+ "&custom=" + MedusaUtil.base64Encode(editText.getEditableText().toString());
			                		MedusaTransferManager.requestHttpReq(bundle.getString("medusalet_name"), "GET", uri, null);
			                	}
			                	else {
			                		Log.e(TAG, "! wrong type for the textdesc dialog");
			                	}
			                    
			                    dialog.dismiss();
			                    finish();
			                }
			            })
			            .setNegativeButton("DISMISS", new DialogInterface.OnClickListener() {
			                public void onClick(DialogInterface dialog, int whichButton) {
			                    dialog.dismiss();
			                    finish();
			                }
			            })
			            .setOnCancelListener(new DialogInterface.OnCancelListener() {
			                public void onCancel(DialogInterface dialog) {
			                    finish();
			                }
			            })
			            .create();
	        	
	        	return dialog;
	        }
        default:
        	Log.e(TAG, "! Wrong Dialog ID=" + id);
        }
        return null;
    }
}
