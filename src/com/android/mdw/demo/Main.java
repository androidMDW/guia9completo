package com.android.mdw.demo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import oauth.signpost.OAuth;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;

import org.json.JSONException;
import org.json.JSONObject;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.AsyncFacebookRunner.RequestListener;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.facebook.android.LoginButton;
import com.facebook.android.SessionEvents;
import com.facebook.android.SessionEvents.AuthListener;
import com.facebook.android.SessionEvents.LogoutListener;
import com.facebook.android.SessionStore;
import com.facebook.android.Util;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class Main extends Activity {
    public static final String APP_ID = "175729095772478";
        
    private Button btnTwLogin;
    private OnClickListener twitter_auth, twitter_clearauth;
    
    private TextView txtFbStatus, txtTwStatus;
    private boolean twitter_active = false, facebook_active = false;    
    private AsyncFacebookRunner mAsyncRunner;
    
    private static CommonsHttpOAuthProvider provider = 
    		new CommonsHttpOAuthProvider(
            "https://api.twitter.com/oauth/request_token",
            "https://api.twitter.com/oauth/access_token",
            "https://api.twitter.com/oauth/authorize");
    
    private static CommonsHttpOAuthConsumer consumer =
    		new CommonsHttpOAuthConsumer(
    			"7iEjG84wItGvXaIZFXAyZg",
    			"sZKCJaUN8BgmYy4r9Z7h1I4BEHV8aAd6Ujw3hofQ4k");

    
    private static String ACCESS_KEY = null;
    private static String ACCESS_SECRET = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        LoginButton mLoginButton = (LoginButton) findViewById(R.id.btnFbLogin);
        btnTwLogin = (Button)findViewById(R.id.btnTwLogin);
        
        txtTwStatus = (TextView) this.findViewById(R.id.txtTwStatus);
        txtFbStatus = (TextView) this.findViewById(R.id.txtFbStatus);
        
        Facebook mFacebook = new Facebook(APP_ID);
        mAsyncRunner = new AsyncFacebookRunner(mFacebook);        
                
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String stored_keys = prefs.getString("KEY", "");
        String stored_secret = prefs.getString("SECRET", "");

        if (!stored_keys.equals("") && !stored_secret.equals("")) {        	
        	twitter_active = true;
        }
        
        SessionStore.restore(mFacebook, this);
        facebook_active = mFacebook.isSessionValid();
        if (facebook_active) {
        	updateFbStatus();
        }        
        
        SessionEvents.addAuthListener(new AuthListener() {        	
        	@Override
        	public void onAuthSucceed() {   
        		updateFbStatus();
        	}
        	
        	@Override
        	public void onAuthFail(String error) {
        		txtFbStatus.setText("Facebook status: imposible iniciar sesi—n " + error);        		
        	}
        });
        
        SessionEvents.addLogoutListener(new LogoutListener() {
        	
        	@Override
        	public void onLogoutFinish() {
        		txtFbStatus.setText("Facebook status: sesi—n no iniciada");
        	}
        	
        	@Override
        	public void onLogoutBegin() {
        		txtFbStatus.setText("Facebook status: cerrando sesi—n...");
        	}
        });
                
        mLoginButton.init(this, mFacebook);
        
        twitter_auth = new OnClickListener() {
			@Override
			public void onClick(View v) {    				
				txtTwStatus.setText("Twitter status: iniciando sesi—n");

				try {
					String authUrl = provider.retrieveRequestToken(consumer, "mdw://twitter");
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)));
				} catch (OAuthMessageSignerException e) {
					e.printStackTrace();
				} catch (OAuthNotAuthorizedException e) {
					e.printStackTrace();
				} catch (OAuthExpectationFailedException e) {
					e.printStackTrace();
				} catch (OAuthCommunicationException e) {					
					e.printStackTrace();
				}  				
				 
			}
		};
        
		twitter_clearauth = new OnClickListener() {
			@Override
			public void onClick(View v) {    	
    			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    		    SharedPreferences.Editor editor = prefs.edit();    		    
    		    editor.putString("KEY", null);
    		    editor.putString("SECRET", null);
    		    editor.commit();
    		    btnTwLogin.setText("Autorizar twitter");
    		    txtTwStatus.setText("Twitter status: sesi—n no iniciada ");
    		    btnTwLogin.setOnClickListener(twitter_auth);
			    
			}
		};
			
        if (twitter_active) {            
        	txtTwStatus.setText("Twitter status: sesi—n iniciada ");
			btnTwLogin.setText("Deautorizar twitter");
            btnTwLogin.setOnClickListener(twitter_clearauth);
        } else {        	
        	btnTwLogin.setText("Autorizar twitter");
            btnTwLogin.setOnClickListener(twitter_auth);
        }        
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	Uri uri = this.getIntent().getData();  
    	if (uri != null && uri.toString().startsWith("mdw://twitter")) {
    	    String verifier = uri.getQueryParameter(OAuth.OAUTH_VERIFIER);
    	    try {    	    	
    	    	provider.retrieveAccessToken(consumer,verifier);
    			ACCESS_KEY = consumer.getToken();
    			ACCESS_SECRET = consumer.getTokenSecret();
    			
    			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    		    SharedPreferences.Editor editor = prefs.edit();    		    
    		    editor.putString("KEY", ACCESS_KEY);
    		    editor.putString("SECRET", ACCESS_SECRET);
    		    editor.commit();
    		    
    			TextView txtTwStatus = (TextView) this.findViewById(R.id.txtTwStatus);
	            txtTwStatus.setText("Twitter status: sesi—n iniciada ");
	            
				btnTwLogin.setText("Deautorizar twitter");
	            btnTwLogin.setOnClickListener(twitter_clearauth);

			} catch (OAuthMessageSignerException e) {
				e.printStackTrace();
			} catch (OAuthNotAuthorizedException e) {
				e.printStackTrace();
			} catch (OAuthExpectationFailedException e) {
				e.printStackTrace();
			} catch (OAuthCommunicationException e) {
				e.printStackTrace();
			}
    	    
    	} 
    	
        if (facebook_active) {
        	updateFbStatus();
        }    	
    }
    
    private void updateFbStatus(){
		mAsyncRunner.request("me", new RequestListener() {
			@Override
			public void onMalformedURLException(MalformedURLException e, Object state) {}
			
			@Override
			public void onIOException(IOException e, Object state) {}
			
			@Override
			public void onFileNotFoundException(FileNotFoundException e, Object state) {}
			
			@Override
			public void onFacebookError(FacebookError e, Object state) {}
			
			@Override
			public void onComplete(String response, Object state) {
				 try {
						JSONObject json = Util.parseJson(response);
						final String id = json.getString("id");
						final String name = json.getString("name");
						Main.this.runOnUiThread(new Runnable() {
							
							@Override
							public void run() {
								txtFbStatus.setText("Facebook status: sesi—n iniciada como " + name + " con el id " + id);									
							}
						});
					} catch (JSONException e) {
						e.printStackTrace();
					} catch (FacebookError e) {
						e.printStackTrace();
					}						
			}
		});
    	
    }
}