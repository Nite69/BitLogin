/*
 * Copyright 2012-2013 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package fi.kotipalo.h.bitlogin;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.widget.TextView;
import jim.h.common.android.lib.zxing.config.ZXingLibConfig;
import jim.h.common.android.lib.zxing.integrator.IntentIntegrator;
import jim.h.common.android.lib.zxing.integrator.IntentResult;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;

public class Bitlogin extends Activity {
    private Handler        handler = new Handler();
    private TextView       txtScanResult;
    private ZXingLibConfig zxingLibConfig;
    private static MessageHandler mh;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bitlogin);

        txtScanResult = (TextView) findViewById(R.id.scan_result);
        zxingLibConfig = new ZXingLibConfig();
        zxingLibConfig.useFrontLight = true;

        View btnScan = findViewById(R.id.scan_button);
        mh = new MessageHandler(getApplicationContext());
        btnScan.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            	// debugging:
				//String uname = "uname";
				//BitIdentity identity = new BitIdentity("serverPubKey", uname);
				//String myPubkey = identity.getPubKey();
				//String message = "skey="+"sessionKey"+"&uname="+"uname"+"&pubkey="+myPubkey;
				//String signature = identity.generateSignaTure(message);
				//txtScanResult.setText(signature);
                IntentIntegrator.initiateScan(Bitlogin.this, "QR_CODE",null,zxingLibConfig);
            }
        });
	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case IntentIntegrator.REQUEST_CODE: // 扫描结果
                IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode,
                        resultCode, data);
                if (scanResult == null) {
                    return;
                }
                final String result = scanResult.getContents();
                if (result != null) 
                {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                        	BitLoginUri uri = null;
                        	try {
                        		//uri = new BitLoginUri("bitid:192.168.7.15:8080/CryptoIDDemo/cid?id=c~BF2AD9A5B018EC44A485B772DC85E3A4~uuno~1EvcDxhp3hfR5TA9aXYk1gcRRgXzuGAZ99");
                        		uri = new BitLoginUri(result);
                        	} catch (Exception e) {
                                txtScanResult.setText("BLU Exception: " + e.getMessage());
                                return;
                        	}
                        	//String debugMain = "Scheme="+uri.getScheme()+", addr="+uri.getAddress();
                        	//String debugParams="\n";
                        	//for (Map.Entry<String, String> entry: uri.getParams().entrySet()) {
                        	//	debugParams = debugParams+entry.getKey() + "=" + entry.getValue() + "\n";
                        	//}
                            //txtScanResult.setText(result);
                            //txtScanResult.setText(debugMain+debugParams);

                        	// MessageHandler.getInstance();
                        	String responseUri = MessageHandler.handlebitidUri(uri);
                        	//responseUri = "bitid:192.168.7.15:8080/CryptoIDDemo/cid?id=c~BF2AD9A5B018EC44A485B772DC85E3A4~uuno~1EvcDxhp3hfR5TA9aXYk1gcRRgXzuGAZ99";
							txtScanResult.setText("Connecting to:"+responseUri);
							new responseLoginRequest().execute(responseUri);
                        }
                    });
                }
                break;
            default:
        }
    }
	
	//@Override
	//public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
	//	getMenuInflater().inflate(R.menu.bitlogin, menu);
	//	return true;
	//}

	private class responseLoginRequest extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... responseUrls) {
			String reply = "error";
			for (String responseUrl:responseUrls) {
				reply = connect(responseUrl);
			}
			return reply;
		}


		private String connect(String responseUri) {
			try {
				DefaultHttpClient httpClient = new DefaultHttpClient();
				HttpGet httpGet = new HttpGet(responseUri);
				HttpResponse httpResponse;
				httpResponse = httpClient.execute(httpGet);
			    HttpEntity httpEntity = httpResponse.getEntity();
			    String output = EntityUtils.toString(httpEntity);
			    return output;
				//txtScanResult.setText("Got response:" + responseUri+"..."+output);
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				//txtScanResult.setText("CP Exception:" + responseUri+"..."+e.getMessage());
				e.printStackTrace();
				return "CP Exception:" + responseUri+"..."+e.getMessage();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//txtScanResult.setText("IO Exception:" + responseUri+"..."+e.getMessage());
				e.printStackTrace();
				return "IO Exception:" + responseUri+"..."+e.getMessage();
			} catch (Exception e) {
				//txtScanResult.setText("Exception:" + responseUri+"..."+e.getMessage());
				e.printStackTrace();
				return "Exception:" + responseUri+"..."+e.getMessage();
			}
		}
	     protected void onPostExecute(String result) {
	    	 txtScanResult.setText(result);
	     }
		
	}
    
}
