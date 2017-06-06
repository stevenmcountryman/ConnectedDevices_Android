//
// Copyright (c) Microsoft Corporation. All rights reserved.
//

package simplisidy.connecteddevices;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.microsoft.connecteddevices.ConnectedDevicesException;
import com.microsoft.connecteddevices.IRemoteSystemDiscoveryListener;
import com.microsoft.connecteddevices.Platform;
import com.microsoft.connecteddevices.RemoteSystem;
import com.microsoft.connecteddevices.RemoteSystemDiscovery;
import com.microsoft.connecteddevices.RemoteSystemDiscoveryType;
import com.microsoft.connecteddevices.RemoteSystemDiscoveryTypeFilter;
import com.microsoft.connecteddevices.RemoteSystemKinds;
import com.microsoft.connecteddevices.RemoteSystemKindFilter;

public class DeviceRecyclerActivity extends AppCompatActivity {
    private DeviceRecyclerAdapter _devicesAdapter;
    private List<Device> _devices;
    private List<Device> favDevices;
    private List<Device> normDevices;
    private RecyclerView _recyclerView;
    private RemoteSystemDiscovery discovery = null;
    private RemoteSystemDiscovery.Builder discoveryBuilder;
    private String sharedText;
    private FloatingActionButton logOutButton;

    public static final String DEVICE_KEY = "device_key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.device_recycler);

        _recyclerView = (RecyclerView) findViewById(R.id.device_recycler);
        _recyclerView.setLayoutManager(new LinearLayoutManager(this));
        _recyclerView.setHasFixedSize(true);
        logOutButton = (FloatingActionButton) findViewById(R.id.logOutButton);
        logOutButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                logout();
            }
        });

        favDevices = new ArrayList<>();
        normDevices = new ArrayList<>();

        Intent intent = this.getIntent();
        sharedText = intent.getStringExtra("SharedText");

        initializeData();
    }

    private void logout() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Log out")
                .setMessage("Are you sure you want to log out of your Microsoft Account? This will cause the app to close and all stored data to be erased.")
                .setPositiveButton("yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        ((ActivityManager)getSystemService(ACTIVITY_SERVICE)).clearApplicationUserData();
                    }})
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }});

        alert.create();
        alert.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        Platform.resume();

        updateAndSortList();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    public void onPause() {
        Platform.suspend();
        super.onPause();
    }

    private void initializeData(){
        initializeAdapter();
        discoveryBuilder = new RemoteSystemDiscovery.Builder().setListener(new IRemoteSystemDiscoveryListener() {
            @Override
            public void onRemoteSystemAdded(RemoteSystem remoteSystem) {
                Device newDevice = new Device(remoteSystem);

                if (newDevice.isFavorite()) {
                    favDevices.add(newDevice);
                }
                else {
                    normDevices.add(newDevice);
                }

                sortList();
            }

            @Override
            public void onRemoteSystemUpdated(RemoteSystem remoteSystem) {
            }

            @Override
            public void onRemoteSystemRemoved(String remoteSystemId) {
            }

            @Override
            public void onComplete() {
            }
        });

        startDiscovery();
    }

    private void sortList() {
        _devices.clear();

        Collections.sort(favDevices, new Comparator<Device>() {
            @Override
            public int compare(Device d1, Device d2)
            {
                if (!d1.getType().equals(d2.getType())) {
                    return d1.getType().compareTo(d2.getType()); //overflow impossible since lengths are non-negative
                }
                return d1.getName().compareTo(d2.getName());
            }
        });
        Collections.sort(normDevices, new Comparator<Device>() {
            @Override
            public int compare(Device d1, Device d2)
            {
                if (!d1.getType().equals(d2.getType())) {
                    return d1.getType().compareTo(d2.getType()); //overflow impossible since lengths are non-negative
                }
                return d1.getName().compareTo(d2.getName());
            }
        });

        _devices.addAll(favDevices);
        _devices.addAll(normDevices);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _devicesAdapter.notifyDataSetChanged();
            }
        });
    }

    private void updateAndSortList() {
        favDevices.clear();
        normDevices.clear();

        for (Device d: _devices) {
            if (d.isFavorite()) {
                favDevices.add(d);
            }
            else {
                normDevices.add(d);
            }
        }

        this.sortList();
    }

    private void startDiscovery() {
        if (discovery != null) {
            try {
                discovery.stop();
            } catch (ConnectedDevicesException e) {
                e.printStackTrace();
            }
        }
        discovery = createDiscovery();
        try {
            discovery.start();
        } catch (ConnectedDevicesException e) {
            e.printStackTrace();
        }
    }

    private RemoteSystemDiscovery createDiscovery() {
        return discoveryBuilder.getResult();
    }

    private void initializeAdapter(){
        _devices = new ArrayList<>();
        _devicesAdapter = new DeviceRecyclerAdapter(_devices);
        _recyclerView.setAdapter(_devicesAdapter);
        _recyclerView.invalidate();

        _devicesAdapter.setOnItemClickListener(new DeviceRecyclerAdapter.ClickListener() {
            @Override
            public void onItemClick(int position, View v) {
                Device selectedDevice = _devices.get(position);
                Intent intent = new Intent(v.getContext(), DeviceActivity.class);
                intent.putExtra(DEVICE_KEY, selectedDevice);
                intent.putExtra("SharedText", sharedText);
                startActivity(intent);
            }
        });
    }
}
