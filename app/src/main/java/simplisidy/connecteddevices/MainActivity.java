//
// Copyright (c) Microsoft Corporation. All rights reserved.
//

package simplisidy.connecteddevices;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.connecteddevices.IAuthCodeProvider;
import com.microsoft.connecteddevices.IPlatformInitializationHandler;
import com.microsoft.connecteddevices.Platform;
import com.microsoft.connecteddevices.PlatformInitializationStatus;

import java.util.Random;

public class MainActivity extends FragmentActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    // Use your own Client ID, assigned when your app was registered with MSA from https://apps.dev.microsoft.com/
    private static String CLIENT_ID = "00000000401D81E5";

    private int _permissionRequestCode = -1;
    private Button _signInButton;
    private String _oauthUrl;
    private String sharedString = "";
    WebView _web;
    Dialog _authDialog;
    private Platform.IAuthCodeHandler _authCodeHandler;
    private static String REDIRECT_URI = "https://login.live.com/oauth20_desktop.srf";
    public static Typeface ICON_FONT;
    public static SharedPreferences sharedpreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedpreferences = getSharedPreferences("ConnectedDevicesPrefs", Context.MODE_PRIVATE);

        ICON_FONT = Typeface.createFromAsset(getApplicationContext().getAssets(),"fonts/segmdl2.ttf");

        _signInButton = (Button) findViewById(R.id.sign_in_button);

        Random rng = new Random();
        _permissionRequestCode = rng.nextInt(128);
        int permissionCheck = ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_NETWORK_STATE);
        if (permissionCheck == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_NETWORK_STATE}, _permissionRequestCode);
            // InitializePlatform will be later invoked from onRequestPermissionsResult
        }
        else {
            Intent intent = getIntent();
            String action = intent.getAction();
            String type = intent.getType();
            if (Intent.ACTION_SEND.equals(action) && type != null) {
                if ("text/plain".equals(type)) {
                    this.sharedString = intent.getStringExtra(Intent.EXTRA_TEXT);
                }
            }

            InitializePlatform();
        }

    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        Platform.resume();
    }

    @Override
    public void onPause() {
        Platform.suspend();
        super.onPause();
    }

    public void onLoginClick(View view) {
        _authDialog = new Dialog(this);
        _authDialog.setContentView(R.layout.auth_dialog);
        _web = (WebView) _authDialog.findViewById(R.id.webv);
        _web.setWebChromeClient(new WebChromeClient());
        _web.getSettings().setJavaScriptEnabled(true);
        _web.getSettings().setDomStorageEnabled(true);
        _web.loadUrl(_oauthUrl);

        WebViewClient webViewClient = new WebViewClient() {
            boolean authComplete = false;

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (url.startsWith(REDIRECT_URI)) {
                    Uri uri = Uri.parse(url);
                    String code = uri.getQueryParameter("code");
                    String error = uri.getQueryParameter("error");
                    if (code != null && !authComplete) {
                        authComplete = true;
                        _signInButton.setEnabled(false);
                        _authDialog.dismiss();

                        if (_authCodeHandler != null) {
                            _authCodeHandler.onAuthCodeFetched(code);
                        }
                    } else if (error != null) {
                        authComplete = true;
                        Intent resultIntent = new Intent();
                        setResult(Activity.RESULT_CANCELED, resultIntent);
                        Toast.makeText(getApplicationContext(), "Error Occurred: " + error, Toast.LENGTH_SHORT).show();

                        _authDialog.dismiss();
                    }
                }
            }
        };

        _web.setWebViewClient(webViewClient);
        _authDialog.show();
        _authDialog.setCancelable(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == _permissionRequestCode) {
            // Platform handles if no permission granted for bluetooth, no need to do anything special.
            InitializePlatform();
            _permissionRequestCode = -1;
        }
    }

    private void InitializePlatform() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                Platform.initialize(getApplicationContext(), new IAuthCodeProvider() {
                    @Override
                    /**
                     * ConnectedDevices Platform needs the app to fetch a MSA auth_code using the given oauthUrl.
                     * When app is fetched the auth_code, it needs to invoke the authCodeHandler onAuthCodeFetched method.
                     */
                    public void fetchAuthCodeAsync(String oauthUrl, Platform.IAuthCodeHandler handler) {
                        _oauthUrl = oauthUrl;
                        _authCodeHandler = handler;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                _signInButton.setVisibility(View.VISIBLE);
                                _signInButton.setEnabled(true);
                            }
                        });
                    }

                    @Override
                    /**
                     * ConnectedDevices Platform needs your app's registered client ID.
                     */
                    public String getClientId() {
                        return "00000000401D81E5";
                    }
                }, new IPlatformInitializationHandler() {
                    @Override
                    public void onDone() {
                        Intent intent = new Intent(MainActivity.this, DeviceRecyclerActivity.class);
                        intent.putExtra("SharedText", sharedString);
                        startActivity(intent);
                    }

                    @Override
                    public void onError(PlatformInitializationStatus status) {
                        if (status == PlatformInitializationStatus.PLATFORM_FAILURE) {

                        } else if (status == PlatformInitializationStatus.TOKEN_ERROR) {

                        }
                    }
                });
            }
        });
    }
}
