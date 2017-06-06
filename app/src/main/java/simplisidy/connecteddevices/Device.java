//
// Copyright (c) Microsoft Corporation. All rights reserved.
//

package simplisidy.connecteddevices;


import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.microsoft.connecteddevices.RemoteSystem;
import com.microsoft.connecteddevices.RemoteSystemKinds;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

public class Device implements Parcelable {
    private String name;
    private String type;
    private String id;
    private boolean isAvailableByProximity;
    private RemoteSystem system = null;

    Device(RemoteSystem system) {
        this.system = system;
        id = system.getId();
        name = system.getDisplayName();
        type = system.getKind().toString();
        isAvailableByProximity = system.isAvailableByProximity();
    }

    protected Device(Parcel in) {
        id = in.readString();

        try {
            system = DeviceStorage.getDevice(id);
            name = system.getDisplayName();
            type = system.getKind().toString();
            isAvailableByProximity = system.isAvailableByProximity();
        } catch (NoSuchElementException e) {
            Log.e("Device", "Device was not found in storage");
            e.printStackTrace();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        // The system must be stored since it is not parcelable
        try {
            DeviceStorage.addDevice(id, system);
        } catch (RuntimeException e) {
            Log.e("Device", "Device already has been added to storage");
            e.printStackTrace();
        }
    }

    public String getID() {
        return this.id;
    }

    public String getName() {
        return MainActivity.sharedpreferences.getString(this.id, this.name);
    }

    public Boolean nickNameSet() {
        if (this.getName().equals(this.name)) return false;
        return true;
    }

    public void setName(String newName) {
        SharedPreferences.Editor editor = MainActivity.sharedpreferences.edit();
        if (newName != null && newName.length() > 0) {
            editor.remove(this.id);
            editor.commit();
            editor.putString(this.id, newName);
            editor.commit();
        }
        else {
            editor.remove(this.id);
            editor.commit();
        }
    }

    public String getType() { return type; }

    public RemoteSystem getSystem() { return system; }

    public String getIcon() {
        if (this.system.getKind() == RemoteSystemKinds.PHONE)
        {
            return "\uE8EA";
        }
        else if (this.system.getKind() == RemoteSystemKinds.DESKTOP)
        {
            return "\uE212";
        }
        else if (this.system.getKind() == RemoteSystemKinds.XBOX)
        {
            return "\uE7FC";
        }
        else if (this.system.getKind() == RemoteSystemKinds.HOLOGRAPHIC)
        {
            return "\uE1A6";
        }
        else if (this.system.getKind() == RemoteSystemKinds.HUB)
        {
            return "\uE8AE";
        }
        else return null;
    }


    public Boolean isFavorite() {
        Set<String> favorites = MainActivity.sharedpreferences.getStringSet("FavoriteSet", new HashSet<String>());

        if (favorites.contains(this.id)) {
            return true;
        }
        else {
            return false;
        }
    }

    public boolean getIsAvailableByProximity() { return isAvailableByProximity; }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Device> CREATOR = new Parcelable.Creator<Device>() {
        @Override
        public Device createFromParcel(Parcel in) {
            return new Device(in);
        }

        @Override
        public Device[] newArray(int size) {
            return new Device[size];
        }
    };
}
