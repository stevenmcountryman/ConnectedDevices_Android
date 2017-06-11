//
// Copyright (c) Microsoft Corporation. All rights reserved.
//

package simplisidy.connecteddevices;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.microsoft.connecteddevices.RemoteLaunchUriStatus.SUCCESS;

public class DeviceActivity extends FragmentActivity {
    private Device device;
    private byte[] bytesToSend;
    private String fileNameToSend;
    private EditText _launchUriEditText;
    private RelativeLayout _sendOptionsLayout;
    private Button _browserButton;
    private boolean browserSelected = false;
    private Button _myTubeButton;
    private boolean myTubeSelected = false;
    private Button _tubeCastButton;
    private boolean tubeCastSelected = false;
    private Button _sendButton;
    private Button _attachButton;
    private Typeface iconFont;
    private TextView favBtn;
    private TextView editBtn;
    private TextView deviceName;
    private final String UNFAVORITE = "\uE1CE";
    private final String FAVORITE = "\uE1CF";
    private final String EDIT = "\uE70F";
    private final String SEND = "\uE122";
    private final String ATTACH = "\uE16C";
    private final String DELETE = "\uE107";
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

        _attachButton = (Button) findViewById(R.id.attach_file_btn);
        _attachButton.setTypeface(iconFont);
        _attachButton.setText(ATTACH);
        _attachButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                OnAttachClick();
            }
        });

        _sendButton = (Button) findViewById(R.id.launch_uri_btn);
        _sendButton.setTypeface(iconFont);
        _sendButton.setText(SEND);
        _sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                OnSendClick();
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

    public void OnAttachClick() {
        if (device.getSystem() != null) {
            if (_attachButton.getText() == DELETE) {
                bytesToSend = null;
                fileNameToSend = null;
                _launchUriEditText.setText("");
                _launchUriEditText.setEnabled(true);
                _attachButton.setText(ATTACH);
            }
            else {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);

                try {
                    startActivityForResult(Intent.createChooser(intent, "Select a file to send"), 17);
                } catch (android.content.ActivityNotFoundException ex) {
                    // Potentially direct the user to the Market with a Dialog
                    Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        switch (requestCode) {
            case 17:
                if (resultCode == RESULT_OK) {
                    try {
                        Uri uri = resultData.getData();
                        fileNameToSend = getFileName(uri);
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(uri);

                            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                            int nRead;
                            bytesToSend = new byte[inputStream.available()];
                            while ((nRead = inputStream.read(bytesToSend, 0, bytesToSend.length)) != -1) {
                                buffer.write(bytesToSend, 0, nRead);
                            }
                            buffer.flush();

                            _launchUriEditText.setText("attached file");
                            _launchUriEditText.setEnabled(false);
                            _attachButton.setText(DELETE);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } catch (Exception e) {
                        showNotification("Cannot access files from that app");
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, resultData);
    }
    String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    public void OnSendClick() {
        if (device.getSystem() != null) {
            if (bytesToSend != null) {
                attemptSendFile();
            }
            else {
                launchUri(new RemoteSystemConnectionRequest(device.getSystem()));
            }
        }
    }

    private void attemptSendFile() {
        String hostName = getIPAddress(true);
        beginFileTransfer(hostName);
    }

    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }

    private void beginFileTransfer(String hostName) {
        final RemoteSystemConnectionRequest connectionRequest = new RemoteSystemConnectionRequest(device.getSystem());
        try {
            String url = "share-app:?FileName=" + fileNameToSend + "&IpAddress=" + hostName;
            new RemoteLauncher().LaunchUriAsync(connectionRequest, url,
                    new IRemoteLauncherListener() {
                        @Override
                        public void onCompleted(RemoteLaunchUriStatus status) {
                        }
                    });
            beginListeningForSocket();
        } catch (ConnectedDevicesException e) {
            e.printStackTrace();
        }
    }

    private void beginListeningForSocket() {
        Thread socketThread = new Thread() {
            public void run() {
                ServerSocket serverSocket = null;
                Socket socket = null;
                DataInputStream input = null;
                DataOutputStream output = null;


                try {
                    serverSocket = new ServerSocket(1717);
                    boolean searching = true;
                    while (searching) {
                        try {
                            socket = serverSocket.accept();
                            input = new DataInputStream(socket.getInputStream());
                            output = new DataOutputStream(socket.getOutputStream());
                            searching = false;

                            int position = 0;
                            int total = bytesToSend.length;
                            int blockSize = 7171;
                            int currBlock = 7171;
                            byte[] chunkToSend = null;
                            if (bytesToSend != null) {
                                while (position < total) {
                                    if (total - position >= blockSize) {
                                        currBlock = blockSize;
                                        chunkToSend = new byte[currBlock];
                                    } else {
                                        currBlock = total - position;
                                        chunkToSend = new byte[currBlock];
                                    }
                                    output.writeBoolean(true);
                                    output.writeInt(chunkToSend.length);
                                    final long percentage = Math.round(((double)position / (double)total) * 100.0);
                                    output.writeInt((int)percentage);
                                    chunkToSend = Arrays.copyOfRange(bytesToSend, position, position + currBlock);
                                    output.write(chunkToSend);
                                    position = position + currBlock;
                                }
                                output.writeBoolean(false);
                            }

                        } catch (Exception e) {

                        }
                        finally {
                            try {
                                socket.close();
                                serverSocket.close();
                                input.close();
                                output.close();
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        _launchUriEditText.setText("");
                                        _launchUriEditText.setEnabled(true);
                                        _attachButton.setText(ATTACH);
                                        bytesToSend = null;
                                        fileNameToSend = null;
                                    }
                                });
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        socketThread.start();

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