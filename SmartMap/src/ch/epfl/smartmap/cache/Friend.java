package ch.epfl.smartmap.cache;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import ch.epfl.smartmap.R;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * A class to represent the user's friends
 * 
 * @author ritterni
 */

public class Friend implements User, Displayable, Parcelable {

    private final long mId; // the user's unique ID
    private String mName; // the user's name as it will be displayed
    private String mPhoneNumber;
    private String mEmail;
    private String mPositionName;
    private GregorianCalendar mLastSeen;
    private final Location mLocation;
    private boolean mVisible;

    public static final String NO_NAME = "";
    public static final String NO_NUMBER = "No phone number specified";
    public static final String NO_EMAIL = "No email address specified";
    public static final String POSITION_UNKNOWN = "Unknown position";
    public static final int DEFAULT_PICTURE = R.drawable.ic_default_user; // placeholder
    public static final int IMAGE_QUALITY = 100;
    public static final String PROVIDER_NAME = "SmartMapServers";
    public static final long ONLINE_TIMEOUT = 1000 * 60 * 3; // time in millis
    // until a user is
    // considered
    // offline

    public static final float MARKER_ANCHOR_X = (float) 0.5;
    public static final float MARKER_ANCHOR_Y = 1;
    public static final int PICTURE_WIDTH = 50;
    public static final int PICTURE_HEIGHT = 50;

    private static final int LEFT_SHIFT_COUNT = 32;

    public static final Parcelable.Creator<User> CREATOR = new Parcelable.Creator<User>() {
        @Override
        public Friend createFromParcel(Parcel source) {
            return new Friend(source);
        }

        @Override
        public Friend[] newArray(int size) {
            return new Friend[size];
        }
    };

    /**
     * Friend constructor
     * 
     * @param userID
     *            The id of the contact we're creating
     * @param userName
     *            The name of the friend
     * @param userNumber
     *            The friend's phone number
     * @author ritterni
     */
    public Friend(long userID, String userName) {
        if (userID < 0) {
            throw new IllegalArgumentException("Invalid user ID!");
        }
        if (userName == null) {
            throw new IllegalArgumentException("Invalid user name!");
        }
        mId = userID;
        mName = userName;
        mPhoneNumber = NO_NUMBER;
        mEmail = NO_EMAIL;
        mPositionName = POSITION_UNKNOWN;
        mLastSeen = new GregorianCalendar();
        mLastSeen.setTimeInMillis(0);
        mLocation = new Location(PROVIDER_NAME);
        mVisible = true;
    }

    public Friend(long userID, String userName, double latitude, double longitude) {
        this(userID, userName);
        this.setLatitude(latitude);
        this.setLongitude(longitude);
    }

    public Friend(Parcel in) {
        mId = in.readLong();
        mName = in.readString();
        mPhoneNumber = in.readString();
        mEmail = in.readString();
        mLocation = in.readParcelable(Location.class.getClassLoader());
        mPositionName = in.readString();
        mLastSeen = new GregorianCalendar();
        mLastSeen.setTimeInMillis(in.readLong());
        boolean[] booleans = new boolean[1];
        in.readBooleanArray(booleans);
        mVisible = booleans[0];
    }

    @Override
    public void deletePicture(Context context) {
        File file = new File(context.getFilesDir(), mId + ".png");
        if (file.exists()) {
            file.delete();
        }
    }

    /*
     * (non-Javadoc)
     * @see android.os.Parcelable#describeContents()
     */
    @Override
    public int describeContents() {
        return 1;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        Friend other = (Friend) obj;
        if (mId != other.mId) {
            return false;
        }
        if (mName == null) {
            if (other.mName != null) {
                return false;
            }
        } else if (!mName.equals(other.mName)) {
            return false;
        }
        return true;
    }

    @Override
    public String getEmail() {
        return mEmail;
    }

    @Override
    public long getID() {
        return mId;
    }

    @Override
    public GregorianCalendar getLastSeen() {
        GregorianCalendar g = new GregorianCalendar(TimeZone.getTimeZone("GMT+01:00"));
        g.setTimeInMillis(mLastSeen.getTimeInMillis());
        return g;
    }

    @Override
    public LatLng getLatLng() {
        return new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
    }

    @Override
    public Location getLocation() {
        return mLocation;
    }

    /*
     * (non-Javadoc)
     * @see
     * ch.epfl.smartmap.cache.Displayable#getMarkerOptions(android.content.Context
     * )
     * @author hugo-S
     */
    @Override
    public MarkerOptions getMarkerOptions(Context context) {
        Bitmap friendProfilePicture =
            Bitmap.createScaledBitmap(this.getPicture(context), PICTURE_WIDTH, PICTURE_HEIGHT, false);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(this.getLatLng()).title(this.getName())
            .icon(BitmapDescriptorFactory.fromBitmap(friendProfilePicture))
            .anchor(MARKER_ANCHOR_X, MARKER_ANCHOR_Y);
        return markerOptions;
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public String getNumber() {
        return mPhoneNumber;
    }

    @Override
    public Bitmap getPicture(Context context) {

        File file = new File(context.getFilesDir(), mId + ".png");

        Bitmap pic = null;

        if (file.exists()) {
            pic = BitmapFactory.decodeFile(file.getAbsolutePath());
        } else {
            pic = BitmapFactory.decodeResource(context.getResources(), DEFAULT_PICTURE);
        }
        return pic;
    }

    @Override
    public String getPositionName() {
        return mPositionName;
    }

    @Override
    public String getShortInfos() {
        String info = "";
        if (this.isOnline() && !this.getPositionName().equals("")) {
            info = "Currently in " + this.getPositionName();
        } else if (this.isOnline()) {
            info = "Online right now";
        } else if (!this.getPositionName().equals("")) {
            info = "Last seen near " + this.getPositionName();
        } else {
            info = "Currently offline";
        }
        return info;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + (int) (mId ^ (mId >>> LEFT_SHIFT_COUNT));
        result = (prime * result) + (mName == null ? 0 : mName.hashCode());
        return result;
    }

    @Override
    public boolean isOnline() {
        return (new GregorianCalendar().getTimeInMillis() - mLastSeen.getTimeInMillis()) < ONLINE_TIMEOUT;
    }

    @Override
    public boolean isVisible() {
        return mVisible;
    }

    @Override
    public void setEmail(String newEmail) {
        mEmail = newEmail;
    }

    @Override
    public void setLastSeen(GregorianCalendar date) {
        GregorianCalendar g = new GregorianCalendar(TimeZone.getTimeZone("GMT+01:00"));
        g.setTimeInMillis(date.getTimeInMillis());
        mLastSeen = g;
    }

    @Override
    public void setLatitude(double latitude) {
        mLocation.setLatitude(latitude);

    }

    @Override
    public void setLocation(Location p) {
        mLocation.set(p);
    }

    @Override
    public void setLongitude(double longitude) {
        mLocation.setLongitude(longitude);

    }

    @Override
    public void setName(String newName) {
        if (newName.isEmpty() || (newName == null)) {
            throw new IllegalArgumentException("Invalid user name!");
        }
        mName = newName;
    }

    @Override
    public void setNumber(String newNumber) {
        mPhoneNumber = newNumber;
    }

    @Override
    @Deprecated
    public void setOnline(boolean status) {
        // deprecated
    }

    @Override
    public void setPicture(Bitmap pic, Context context) {

        File file = new File(context.getFilesDir(), mId + ".png");

        if (file.exists()) {
            file.delete();
        }

        try {
            FileOutputStream out = context.openFileOutput(mId + ".png", Context.MODE_PRIVATE);
            pic.compress(Bitmap.CompressFormat.PNG, IMAGE_QUALITY, out);
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setPositionName(String posName) {
        mPositionName = posName;
    }

    @Override
    public void setVisible(boolean isVisible) {
        mVisible = isVisible;
    }

    /*
     * (non-Javadoc)
     * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mId);
        dest.writeString(mName);
        dest.writeString(mPhoneNumber);
        dest.writeString(mEmail);
        dest.writeParcelable(mLocation, flags);
        dest.writeString(mPositionName);
        dest.writeLong(mLastSeen.getTimeInMillis());
        boolean[] booleans = new boolean[]{mVisible};
        dest.writeBooleanArray(booleans);
    }
}