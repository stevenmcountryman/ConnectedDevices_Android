//
// Copyright (c) Microsoft Corporation. All rights reserved.
//

package simplisidy.connecteddevices;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.microsoft.connecteddevices.ConnectedDevicesException;
import com.microsoft.connecteddevices.IRemoteLauncherListener;
import com.microsoft.connecteddevices.Platform;
import com.microsoft.connecteddevices.RemoteLaunchUriStatus;
import com.microsoft.connecteddevices.RemoteLauncher;
import com.microsoft.connecteddevices.RemoteSystemConnectionRequest;
import java.util.HashSet;
import java.util.Set;

import static com.microsoft.connecteddevices.RemoteLaunchUriStatus.SUCCESS;

public class DeviceActivity extends FragmentActivity {
    private Device device;
    private EditText _launchUriEditText;
    private RelativeLayout _sendOptionsLayout;
    private Button _browserButton;
    private boolean browserSelected = false;
    private Button _myTubeButton;
    private boolean myTubeSelected = false;
    private Button _tubeCastButton;
    private boolean tubeCastSelected = false;
    private Button _sendButton;
    private Typeface iconFont;
    private TextView favBtn;
    private TextView editBtn;
    private TextView deviceName;
    private final String UNFAVORITE = "\uE1CE";
    private final String FAVORITE = "\uE1CF";
    private final String EDIT = "\uE70F";
    private final String SEND = "\uE122";
    private Boolean isFavorite = false;
    private int HIGHLIGHT;
    private RelativeLayout notificationArea;
    private TextView notificationText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_view);
        iconFont = Typeface.createFromAsset(getApplicationContext().getAssets(),"fonts/segmdl2.ttf");

        notificationArea = (RelativeLayout) findViewById(R.id.notificationBox);
        notificationText = (TextView) findViewById(R.id.notificationText);

        _sendButton = (Button) findViewById(R.id.launch_uri_btn);
        _sendButton.setTypeface(iconFont);
        _sendButton.setText(SEND);
        _sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onLaunchClick();
            }
        });
        HIGHLIGHT = _sendButton.getHighlightColor();
        _sendOptionsLayout = (RelativeLayout) findViewById(R.id.sendOptionsLayout);
        _browserButton = (Button) findViewById(R.id.BrowserBtn);
        _browserButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ToggleBrowserButton();
            }
        });
        _myTubeButton = (Button) findViewById(R.id.MyTubeBtn);
        _myTubeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ToggleMyTubeButton();
            }
        });
        _tubeCastButton = (Button) findViewById(R.id.TubeCastBtn);
        _tubeCastButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ToggleTubeCastButton();
            }
        });
        _launchUriEditText = (EditText) findViewById(R.id.MessageBox);
        _launchUriEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.toString().length() > 0) {
                    _sendButton.setEnabled(true);
                    _sendButton.setTextColor(HIGHLIGHT);
                    CheckIsWebLink(editable.toString());
                }
                else {
                    _sendOptionsLayout.setVisibility(View.INVISIBLE);
                    _sendButton.setEnabled(false);
                    _sendButton.setTextColor(Color.GRAY);
                }
            }
        });

        Intent intent = this.getIntent();
        device = intent.getParcelableExtra(DeviceRecyclerActivity.DEVICE_KEY);
        String sharedText = intent.getStringExtra("SharedText");
        if (sharedText != null && sharedText.length() > 0) {
            _launchUriEditText.setText(sharedText);
        }
        if (device == null) {
            finish();
            return;
        }

        deviceName = (TextView) findViewById(R.id.device_name);
        deviceName.setText(device.getName());
        TextView deviceType = (TextView) findViewById(R.id.device_type);
        deviceType.setTypeface(iconFont);
        deviceType.setText(device.getIcon());

        favBtn = (TextView) findViewById(R.id.favoriteBtn);
        favBtn.setTypeface(iconFont);
        if (this.device.isFavorite()) {
            this.isFavorite = true;
            this.favBtn.setText(FAVORITE);
        }
        else {
            this.isFavorite = false;
            this.favBtn.setText(UNFAVORITE);
        }
        favBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ToggleFavoriteDevice();
            }
        });

        editBtn = (TextView) findViewById(R.id.editBtn);
        editBtn.setTypeface(iconFont);
        editBtn.setText(EDIT);
        editBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EditDeviceName();
            }
        });
    }

    private void ToggleFavoriteDevice() {
        if (this.isFavorite) {
            this.unfavoriteDevice();
            this.favBtn.setText(UNFAVORITE);
            this.isFavorite = false;
        }
        else {
            this.favoriteDevice();
            this.favBtn.setText(FAVORITE);
            this.isFavorite = true;
        }
    }

    private void favoriteDevice() {
        Set<String> favorites = MainActivity.sharedpreferences.getStringSet("FavoriteSet", new HashSet<String>());
        favorites.add(this.device.getID());

        SharedPreferences.Editor editor = MainActivity.sharedpreferences.edit();
        editor.remove("FavoriteSet");
        editor.commit();
        editor.putStringSet("FavoriteSet", favorites);
        editor.commit();
    }

    private void unfavoriteDevice() {
        Set<String> favorites = MainActivity.sharedpreferences.getStringSet("FavoriteSet", new HashSet<String>());
        favorites.remove(this.device.getID());

        SharedPreferences.Editor editor = MainActivity.sharedpreferences.edit();
        editor.remove("FavoriteSet");
        editor.commit();
        editor.putStringSet("FavoriteSet", favorites);
        editor.commit();
    }

    private void EditDeviceName() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Set device nickname");
        final EditText input = new EditText(this);
        input.setHint(device.getName());
        alert.setView(input)
                .setPositiveButton("save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        device.setName(input.getText().toString());
                        deviceName.setText(device.getName());
                    }})
                .setNegativeButton("clear", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        device.setName(null);
                        deviceName.setText(device.getName());
                    }});

        alert.create();
        alert.show();
    }

    private void ToggleBrowserButton() {
        if (browserSelected) {
            _browserButton.setTextColor(Color.BLACK);
            browserSelected = false;
        }
        else {
            _browserButton.setTextColor(HIGHLIGHT);
            browserSelected = true;

            if (_myTubeButton.isEnabled()) {
                _myTubeButton.setTextColor(Color.BLACK);
                myTubeSelected = false;
                _tubeCastButton.setTextColor(Color.BLACK);
                tubeCastSelected = false;
            }
        }
    }

    private void ToggleMyTubeButton() {
        if (myTubeSelected) {
            _myTubeButton.setTextColor(Color.BLACK);
            myTubeSelected = false;
        }
        else {
            _myTubeButton.setTextColor(HIGHLIGHT);
            myTubeSelected = true;

            _browserButton.setTextColor(Color.BLACK);
            browserSelected = false;
            if (_myTubeButton.isEnabled()) {
                _tubeCastButton.setTextColor(Color.BLACK);
                tubeCastSelected = false;
            }
        }
    }

    private void ToggleTubeCastButton() {
        if (tubeCastSelected) {
            _tubeCastButton.setTextColor(Color.BLACK);
            tubeCastSelected = false;
        }
        else {
            _tubeCastButton.setTextColor(HIGHLIGHT);
            tubeCastSelected = true;

            _browserButton.setTextColor(Color.BLACK);
            browserSelected = false;
            if (_myTubeButton.isEnabled()) {
                _myTubeButton.setTextColor(Color.BLACK);
                myTubeSelected = false;
            }
        }
    }

    private void CheckIsWebLink(String message) {
        if (message.toLowerCase().startsWith("http://") || message.toLowerCase().startsWith("https://")) {
            _sendOptionsLayout.setVisibility(View.VISIBLE);

            if (message.toLowerCase().contains("youtube.com/watch?"))
            {
                _myTubeButton.setEnabled(true);
                _tubeCastButton.setEnabled(true);
            }
            else
            {
                _myTubeButton.setEnabled(false);
                _tubeCastButton.setEnabled(false);


                _tubeCastButton.setTextColor(Color.GRAY);
                tubeCastSelected = false;
                _myTubeButton.setTextColor(Color.GRAY);
                myTubeSelected = false;
            }
        }
        else {
            _sendOptionsLayout.setVisibility(View.INVISIBLE);
        }
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

    public void onLaunchClick() {
        if (device.getSystem() != null) {
            launchUri(new RemoteSystemConnectionRequest(device.getSystem()));
        }
    }

    private void launchUri(final RemoteSystemConnectionRequest connectionRequest) {
        try {
            showNotification("Sharing to device");
            final String url = getMessageToSend();
            new RemoteLauncher().LaunchUriAsync(connectionRequest, url,
                    new IRemoteLauncherListener() {
                        @Override
                        public void onCompleted(RemoteLaunchUriStatus status) {
                            String message;
                            if (status == SUCCESS)
                            {
                                message = "Launch succeeded";
                            }
                            else if (status == RemoteLaunchUriStatus.PROTOCOL_UNAVAILABLE) {
                                message = "Please install the app on target";

                                if (url.startsWith("tubecast")) {
                                    launchStore(connectionRequest, "ms-windows-store://pdp/?productid=9wzdncrdx3fs");
                                }
                                else if (url.startsWith("mytube")) {
                                    launchStore(connectionRequest, "ms-windows-store://pdp/?productid=9wzdncrcwf3l");
                                }
                                else {
                                    launchStore(connectionRequest, "ms-windows-store://pdp/?productid=9nblggh4tssg");
                                }
                            }
                            else
                            {
                                message = "Launch failed with status " + status.toString();
                            }

                            showNotification(message);
                        }
                    });
        } catch (ConnectedDevicesException e) {
            e.printStackTrace();
        }
    }

    private void showNotification(String message) {
        notificationText.setText(message);
        notificationArea.setVisibility(View.VISIBLE);
        notificationArea.postDelayed(hide, 2000);
    }
    private Runnable hide = new Runnable() {
        @Override
        public void run() {
            notificationArea.setVisibility(View.GONE);
        }
    };


    private void launchStore(RemoteSystemConnectionRequest connectionRequest, String URL) {
        try {
            new RemoteLauncher().LaunchUriAsync(connectionRequest, URL,
                    new IRemoteLauncherListener() {
                        @Override
                        public void onCompleted(RemoteLaunchUriStatus status) {
                        }
                    });
        } catch (ConnectedDevicesException e) {
            e.printStackTrace();
        }
    }

    private String getMessageToSend() {
        String message = _launchUriEditText.getText().toString();
        if (browserSelected) {
            return message;
        }
        else if (tubeCastSelected) {
            return "tubecast:link=" + message;
        }
        else if (myTubeSelected) {
            return "mytube:link=" + message;
        }
        else {
            return "share-app:?Text=" + message;
        }
    }
}