package ch.epfl.smartmap.database;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import ch.epfl.smartmap.R;
import ch.epfl.smartmap.cache.DefaultFilter;
import ch.epfl.smartmap.cache.Event;
import ch.epfl.smartmap.cache.Filter;
import ch.epfl.smartmap.cache.Friend;
import ch.epfl.smartmap.cache.ImmutableEvent;
import ch.epfl.smartmap.cache.ImmutableUser;
import ch.epfl.smartmap.cache.Localisable;
import ch.epfl.smartmap.cache.User;

/**
 * SQLite helper
 * 
 * @author ritterni
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 5;
    private static final String DATABASE_NAME = "SmartMapDB";

    public static final String TABLE_USER = "users";
    public static final String TABLE_FILTER = "filters";
    public static final String TABLE_FILTER_USER = "filter_users";
    public static final String TABLE_EVENT = "events";
    public static final String TABLE_INVITATIONS = "invitations";
    public static final String TABLE_PENDING = "pending";

    private static final String KEY_USER_ID = "userID";
    private static final String KEY_NAME = "name";
    private static final String KEY_NUMBER = "number";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_LONGITUDE = "longitude";
    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_POSNAME = "posName";
    private static final String KEY_LASTSEEN = "lastSeen";
    private static final String KEY_VISIBLE = "isVisible";

    private static final String KEY_ID = "id";
    private static final String KEY_FILTER_ID = "filterID";

    private static final String KEY_DATE = "date";
    private static final String KEY_ENDDATE = "endDate";

    private static final String KEY_CREATOR_NAME = "creatorName";
    private static final String KEY_EVTDESC = "eventDescription";

    // Columns for the User table
    private static final String[] USER_COLUMNS = {KEY_USER_ID, KEY_NAME, KEY_NUMBER, KEY_EMAIL,
        KEY_LONGITUDE, KEY_LATITUDE, KEY_POSNAME, KEY_LASTSEEN, KEY_VISIBLE};

    // Columns for the Filter table
    private static final String[] FILTER_COLUMNS = {KEY_ID, KEY_NAME};

    // Columns for the Filter/User table
    private static final String[] FILTER_USER_COLUMNS = {KEY_ID, KEY_FILTER_ID, KEY_USER_ID};

    // Columns for the Event table
    private static final String[] EVENT_COLUMNS = {KEY_ID, KEY_NAME, KEY_EVTDESC, KEY_USER_ID, KEY_LONGITUDE,
        KEY_LATITUDE, KEY_DATE, KEY_ENDDATE};

    // Columns for the Invitations table
    private static final String[] INVITATION_COLUMNS = {KEY_USER_ID, KEY_NAME};

    // Columns for the Invitations table
    private static final String[] PENDING_COLUMNS = {KEY_USER_ID, KEY_NAME};

    // Table of users
    private static final String CREATE_TABLE_USER = "CREATE TABLE IF NOT EXISTS " + TABLE_USER + "("
        + KEY_USER_ID + " INTEGER PRIMARY KEY," + KEY_NAME + " TEXT," + KEY_NUMBER + " TEXT," + KEY_EMAIL
        + " TEXT," + KEY_LONGITUDE + " DOUBLE," + KEY_LATITUDE + " DOUBLE," + KEY_POSNAME + " TEXT,"
        + KEY_LASTSEEN + " INTEGER," + KEY_VISIBLE + " INTEGER" + ")";

    // Table of filters
    private static final String CREATE_TABLE_FILTER = "CREATE TABLE IF NOT EXISTS " + TABLE_FILTER + "("
        + KEY_ID + " INTEGER PRIMARY KEY," + KEY_NAME + " TEXT" + ")";

    // Table that maps filters to users
    private static final String CREATE_TABLE_FILTER_USER = "CREATE TABLE IF NOT EXISTS " + TABLE_FILTER_USER
        + "(" + KEY_ID + " INTEGER PRIMARY KEY," + KEY_FILTER_ID + " INTEGER," + KEY_USER_ID + " INTEGER"
        + ")";

    // Table of events
    private static final String CREATE_TABLE_EVENT = "CREATE TABLE IF NOT EXISTS " + TABLE_EVENT + "("
        + KEY_ID + " INTEGER PRIMARY KEY," + KEY_NAME + " TEXT," + KEY_EVTDESC + " TEXT," + KEY_USER_ID
        + " INTEGER," + KEY_LONGITUDE + " DOUBLE," + KEY_LATITUDE + " DOUBLE," + KEY_DATE + " INTEGER,"
        + KEY_ENDDATE + " INTEGER" + ")";

    // Table of invitations
    private static final String CREATE_TABLE_INVITATIONS = "CREATE TABLE IF NOT EXISTS " + TABLE_INVITATIONS
        + "(" + KEY_USER_ID + " INTEGER PRIMARY KEY," + KEY_NAME + " TEXT" + ")";

    // Table of invitations
    private static final String CREATE_TABLE_PENDING = "CREATE TABLE IF NOT EXISTS " + TABLE_PENDING + "("
        + KEY_USER_ID + " INTEGER PRIMARY KEY," + KEY_NAME + " TEXT" + ")";
    private static final int IMAGE_QUALITY = 100;

    private static DatabaseHelper mInstance;
    private static SQLiteDatabase DATABASE;
    private final Context mContext;

    /**
     * DatabaseHelper constructor. Will be made private, so use initialize() or
     * getInstance() instead.
     * 
     * @param context
     *            The application's context, used to access the files
     */
    @Deprecated
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    /**
     * Stores an event in the database. If there's already an event with the
     * same ID, updates that event instead
     * The event must have an ID (given by the server)!
     * 
     * @param event
     *            The event to store
     */
    public void addEvent(Event event) throws IllegalArgumentException {
        if (event.getId() < 0) {
            throw new IllegalArgumentException("Invalid event ID");
        }

        Cursor cursor =
            DATABASE.query(TABLE_EVENT, EVENT_COLUMNS, KEY_ID + " = ?",
                new String[]{String.valueOf(event.getId())}, null, null, null, null);

        // We check if the event is already there
        if (!cursor.moveToFirst()) {
            ContentValues values = new ContentValues();
            values.put(KEY_ID, event.getId());
            values.put(KEY_NAME, event.getName());
            values.put(KEY_EVTDESC, event.getDescription());
            values.put(KEY_USER_ID, event.getCreatorId());
            values.put(KEY_LONGITUDE, event.getLocation().getLongitude());
            values.put(KEY_LATITUDE, event.getLocation().getLatitude());
            values.put(KEY_DATE, event.getStartDate().getTimeInMillis());
            values.put(KEY_ENDDATE, event.getEndDate().getTimeInMillis());

            DATABASE.insert(TABLE_EVENT, null, values);
        } else {
            this.updateEvent(event);
        }

        cursor.close();

    }

    /**
     * Adds a filter/userlist/friendlist to the database, and gives an ID to the
     * filter
     * 
     * @param filter
     *            The filter/list to add
     * @return The ID of the newly added filter in the filter database
     */
    public long addFilter(Filter filter) {
        // First we insert the filter in the table of lists
        ContentValues filterValues = new ContentValues();
        filterValues.put(KEY_NAME, filter.getListName());
        long filterID = DATABASE.insert(TABLE_FILTER, null, filterValues);

        // Then we add the filter-user pairs to another table
        ContentValues pairValues = null;
        for (long id : filter.getList()) {
            pairValues = new ContentValues();
            pairValues.put(KEY_FILTER_ID, filterID);
            pairValues.put(KEY_USER_ID, id);
            DATABASE.insert(TABLE_FILTER_USER, null, pairValues);
        }

        filter.setID(filterID); // sets an ID so the filter can be easily
                                // accessed

        return filterID;
    }

    /**
     * Adds a pending friend request to the database
     * 
     * @param user
     *            The user who made the request (only need name and ID)
     * @return 1 if the invitation was added, 0 if it was already there
     */
    public int addInvitation(User user) {
        Cursor cursor =
            DATABASE.query(TABLE_INVITATIONS, INVITATION_COLUMNS, KEY_USER_ID + " = ?",
                new String[]{String.valueOf(user.getId())}, null, null, null, null);

        int result = 0;
        if (!cursor.moveToFirst()) {
            ContentValues values = new ContentValues();
            values.put(KEY_USER_ID, user.getId());
            values.put(KEY_NAME, user.getName());

            DATABASE.insert(TABLE_INVITATIONS, null, values);

            result = 1;
        }
        cursor.close();
        return result;
    }

    /**
     * Adds a pending sent friend request to the database
     * 
     * @param user
     *            The user who was sent a request
     */
    public void addPendingFriend(User user) {
        Cursor cursor =
            DATABASE.query(TABLE_PENDING, PENDING_COLUMNS, KEY_USER_ID + " = ?",
                new String[]{String.valueOf(user.getId())}, null, null, null, null);

        if (!cursor.moveToFirst()) {
            ContentValues values = new ContentValues();
            values.put(KEY_USER_ID, user.getId());
            values.put(KEY_NAME, user.getName());

            DATABASE.insert(TABLE_PENDING, null, values);

        }
        cursor.close();
    }

    /**
     * Adds a user to the internal database. If an user with the same ID already
     * exists, updates that user instead.
     * 
     * @param user
     *            The user to add to the database
     */
    public void addUser(ImmutableUser user) {
        Cursor cursor =
            DATABASE.query(TABLE_USER, USER_COLUMNS, KEY_USER_ID + " = ?",
                new String[]{String.valueOf(user.getId())}, null, null, null, null);

        if (!cursor.moveToFirst()) {
            ContentValues values = new ContentValues();
            values.put(KEY_USER_ID, user.getId());
            values.put(KEY_NAME, user.getName());
            values.put(KEY_NUMBER, user.getPhoneNumber());
            values.put(KEY_EMAIL, user.getEmail());
            values.put(KEY_LONGITUDE, user.getLocation().getLongitude());
            values.put(KEY_LATITUDE, user.getLocation().getLatitude());
            values.put(KEY_POSNAME, user.getLocationString());
            values.put(KEY_LASTSEEN, user.getLocation().getTime());

            DATABASE.insert(TABLE_USER, null, values);
        } else {
            this.updateFriend(user);
        }

        cursor.close();
    }

    /**
     * Clears the database. Mainly for testing purposes.
     */
    public void clearAll() {
        DATABASE.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
        DATABASE.execSQL("DROP TABLE IF EXISTS " + TABLE_FILTER);
        DATABASE.execSQL("DROP TABLE IF EXISTS " + TABLE_FILTER_USER);
        DATABASE.execSQL("DROP TABLE IF EXISTS " + TABLE_EVENT);
        DATABASE.execSQL("DROP TABLE IF EXISTS " + TABLE_INVITATIONS);
        DATABASE.execSQL("DROP TABLE IF EXISTS " + TABLE_PENDING);

        this.onCreate(DATABASE);
    }

    /**
     * Deletes an event from the database
     * 
     * @param event
     *            The event to delete
     */
    public void deleteEvent(long id) {

        DATABASE.delete(TABLE_EVENT, KEY_ID + " = ?", new String[]{String.valueOf(id)});

    }

    /**
     * Deletes a filter from the database
     * 
     * @param filter
     *            The filter to delete
     */
    public void deleteFilter(long id) {
        // delete the filter from the table of filters
        DATABASE.delete(TABLE_FILTER, KEY_ID + " = ?", new String[]{String.valueOf(id)});

        // then delete all the rows that reference this filter in the
        // filter-user table
        DATABASE.delete(TABLE_FILTER_USER, KEY_FILTER_ID + " = ?", new String[]{String.valueOf(id)});

    }

    /**
     * Deletes an invitation from the database (call this when accepting or
     * declining an invitation)
     * 
     * @param id
     *            The inviter's id
     */
    public void deleteInvitation(long id) {

        DATABASE.delete(TABLE_INVITATIONS, KEY_USER_ID + " = ?", new String[]{String.valueOf(id)});
    }

    /**
     * Deletes a pending friend request from the database
     * 
     * @param id
     *            The invited user's id
     */
    public void deletePendingFriend(long id) {

        DATABASE.delete(TABLE_PENDING, KEY_USER_ID + " = ?", new String[]{String.valueOf(id)});
    }

    /**
     * Deletes a user from the database
     * 
     * @param id
     *            The user's id
     */
    public void deleteUser(long id) {

        DATABASE.delete(TABLE_USER, KEY_USER_ID + " = ?", new String[]{String.valueOf(id)});
    }

    /**
     * @return the {@code List} of all events
     */
    public List<ImmutableEvent> getAllEvents() {
        ArrayList<ImmutableEvent> events = new ArrayList<ImmutableEvent>();

        String query = "SELECT  * FROM " + TABLE_EVENT;

        Cursor cursor = DATABASE.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                events.add(this.getEvent(cursor.getLong(cursor.getColumnIndex(KEY_ID))));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return events;
    }

    /**
     * @return the {@code List} of all Filters
     */
    public List<Filter> getAllFilters() {

        ArrayList<Filter> filters = new ArrayList<Filter>();

        String query = "SELECT  * FROM " + TABLE_FILTER;

        Cursor cursor = DATABASE.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                // using getFilter to add this row's filter to the list
                filters.add(this.getFilter(cursor.getLong(cursor.getColumnIndex(KEY_ID))));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return filters;
    }

    /**
     * @return the {@code List} of all friends
     */
    // public List<User> getAllFriends() {
    // ArrayList<User> friends = new ArrayList<User>();
    //
    // String query = "SELECT  * FROM " + TABLE_USER;
    //
    // Cursor cursor = mDatabase.rawQuery(query, null);
    //
    // Friend friend = null;
    // if (cursor.moveToFirst()) {
    // do {
    // friend =
    // new Friend(cursor.getLong(cursor.getColumnIndex(KEY_USER_ID)), cursor.getString(cursor
    // .getColumnIndex(KEY_NAME)));
    // friend.setPhoneNumber(cursor.getString(cursor.getColumnIndex(KEY_NUMBER)));
    // friend.setEmail(cursor.getString(cursor.getColumnIndex(KEY_EMAIL)));
    // friend.setLongitude(cursor.getDouble(cursor.getColumnIndex(KEY_LONGITUDE)));
    // friend.setLatitude(cursor.getDouble(cursor.getColumnIndex(KEY_LATITUDE)));
    // friend.setPositionName(cursor.getString(cursor.getColumnIndex(KEY_POSNAME)));
    // GregorianCalendar cal = new GregorianCalendar();
    // cal.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(KEY_LASTSEEN)));
    // friend.setLastSeen(cal.getTime());
    // friend.setVisible(cursor.getInt(cursor.getColumnIndex(KEY_VISIBLE)) == 1); // int to boolean
    // friends.add(friend);
    // } while (cursor.moveToNext());
    // }
    //
    // cursor.close();
    // return friends;
    // }

    /**
     * @param id
     *            The event's ID
     * @return The event associated to this ID
     */
    public ImmutableEvent getEvent(long id) {

        // SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor =
            DATABASE.query(TABLE_EVENT, EVENT_COLUMNS, KEY_ID + " = ?", new String[]{String.valueOf(id)},
                null, null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();
        }

        GregorianCalendar startDate = new GregorianCalendar();
        GregorianCalendar endDate = new GregorianCalendar();
        startDate.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(KEY_DATE)));
        endDate.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(KEY_ENDDATE)));

        Location location = new Location(Localisable.PROVIDER_NAME);
        location.setLongitude(cursor.getDouble(cursor.getColumnIndex(KEY_LONGITUDE)));
        location.setLatitude(cursor.getDouble(cursor.getColumnIndex(KEY_LATITUDE)));
        String locationString = cursor.getString(cursor.getColumnIndex(KEY_POSNAME));

        String name = cursor.getString(cursor.getColumnIndex(KEY_NAME));
        String description = cursor.getString(cursor.getColumnIndex(KEY_EVTDESC));
        long creatorId = cursor.getColumnIndex(KEY_USER_ID);

        ImmutableEvent event =
            new ImmutableEvent(id, name, creatorId, description, startDate, endDate, location,
                locationString, new ArrayList<Long>());

        cursor.close();

        return event;
    }

    /**
     * Gets a specific filter by its id
     * 
     * @param name
     *            The filter's id
     * @return The filter as a FriendList object
     */
    public Filter getFilter(long id) {

        // SQLiteDatabase db = this.getWritableDatabase();

        // First query to get the filter's name
        Cursor cursor =
            DATABASE.query(TABLE_FILTER, FILTER_COLUMNS, KEY_ID + " = ?", new String[]{String.valueOf(id)},
                null, null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();
        }

        DefaultFilter filter = new DefaultFilter(cursor.getString(cursor.getColumnIndex(KEY_NAME)));
        filter.setID(id);

        // Second query to get the associated list of IDs
        cursor =
            DATABASE.query(TABLE_FILTER_USER, FILTER_USER_COLUMNS, KEY_FILTER_ID + " = ?",
                new String[]{String.valueOf(id)}, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                filter.addUser(cursor.getLong(cursor.getColumnIndex(KEY_USER_ID)));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return filter;
    }

    /**
     * Gets a user from the database
     * 
     * @param id
     *            The user's unique ID
     * @return The user as a Friend object
     */
    public ImmutableUser getFriend(long id) {

        Cursor cursor =
            DATABASE.query(TABLE_USER, USER_COLUMNS, KEY_USER_ID + " = ?", new String[]{String.valueOf(id)},
                null, null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();

            String name = cursor.getString(cursor.getColumnIndex(KEY_NAME));
            String phoneNumber = cursor.getString(cursor.getColumnIndex(KEY_NUMBER));
            String email = cursor.getString(cursor.getColumnIndex(KEY_EMAIL));
            long lastSeen = cursor.getLong(cursor.getColumnIndex(KEY_LASTSEEN));
            double longitude = cursor.getDouble(cursor.getColumnIndex(KEY_LONGITUDE));
            double latitude = cursor.getDouble(cursor.getColumnIndex(KEY_LATITUDE));
            Location location = new Location("database");
            location.setLongitude(longitude);
            location.setLatitude(latitude);
            location.setTime(lastSeen);
            String locationString = cursor.getString(cursor.getColumnIndex(KEY_POSNAME));
            Bitmap image = getPictureById(id);

            cursor.close();

            return new ImmutableUser(id, name, phoneNumber, email, location, locationString, image);
        }

        return ImmutableUser.NOT_FOUND;
    }

    /**
     * Returns a list of all pending received invitations
     * 
     * @return A list of users who sent requests
     */
    public List<ImmutableUser> getInvitations() {
        List<ImmutableUser> invitations = new ArrayList<ImmutableUser>();

        String query = "SELECT  * FROM " + TABLE_INVITATIONS;

        Cursor cursor = DATABASE.rawQuery(query, null);

        ImmutableUser friend = null;
        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndex(KEY_USER_ID));
                friend =
                    new ImmutableUser(id, cursor.getString(cursor.getColumnIndex(KEY_NAME)), User.NO_NUMBER,
                        User.NO_EMAIL, new Location(""), "", getPictureById(id));

                invitations.add(friend);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return invitations;
    }

    /**
     * Returns a list of all pending friends
     * 
     * @return A list of users who were sent friend requests
     */
    public List<ImmutableUser> getPendingFriends() {
        List<ImmutableUser> friends = new ArrayList<ImmutableUser>();

        String query = "SELECT  * FROM " + TABLE_PENDING;

        Cursor cursor = DATABASE.rawQuery(query, null);

        ImmutableUser friend = null;
        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndex(KEY_USER_ID));
                friend =
                    new ImmutableUser(id, cursor.getString(cursor.getColumnIndex(KEY_NAME)), User.NO_NUMBER,
                        User.NO_EMAIL, new Location(""), "", getPictureById(id));

                friends.add(friend);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return friends;
    }

    /**
     * Returns a user's picture from the internal storage
     * 
     * @param userId
     *            The user's ID
     * @return The user's profile picture if it exists, a default picture otherwise
     */
    public Bitmap getPictureById(long userId) {
        File file = new File(mContext.getFilesDir(), userId + ".png");
        Bitmap pic = null;
        if (file.exists()) {
            pic = BitmapFactory.decodeFile(file.getAbsolutePath());
        }
        if (pic == null) {
            pic = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_default_user);
        }
        return pic;
    }

    /**
     * Fills the friend database with server data
     */
    // public void initializeAllFriends() {
    // NetworkSmartMapClient client = NetworkSmartMapClient.getInstance();
    // try {
    // List<Long> friends = client.getFriendsIds();
    // for (long userID : friends) {
    // this.addUser(client.getUserInfo(userID));
    // }
    // } catch (SmartMapClientException e) {
    // Log.e("UpdateService", e.getMessage());
    // }
    // }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USER);
        db.execSQL(CREATE_TABLE_FILTER);
        db.execSQL(CREATE_TABLE_FILTER_USER);
        db.execSQL(CREATE_TABLE_EVENT);
        db.execSQL(CREATE_TABLE_INVITATIONS);
        db.execSQL(CREATE_TABLE_PENDING);
    }

    /**
     * Uses listFriendsPos() to update the entire friends database with updated
     * positions
     * 
     * @return The number of rows (i.e. friends) that were updated
     */
    // public int refreshFriendsPos() {
    // int updatedRows = 0;
    // try {
    // List<User> updatedUsers = NetworkSmartMapClient.getInstance().listFriendsPos();
    //
    // for (User user : updatedUsers) {
    // this.updateUser(user);
    // updatedRows++;
    // }
    // } catch (SmartMapClientException e) {
    // e.printStackTrace();
    // }
    // return updatedRows;
    // }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FILTER);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FILTER_USER);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EVENT);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INVITATIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PENDING);
        this.onCreate(db);
    }

    // /**
    // * Fully updates the friends database (not only positions)
    // */
    // public void refreshFriendsInfo() {
    // List<User> friends = this.getAllFriends();
    // NetworkSmartMapClient client = NetworkSmartMapClient.getInstance();
    // for (User f : friends) {
    // try {
    // this.updateUser(client.getUserInfo(f.getID()));
    // } catch (SmartMapClientException e) {
    // e.printStackTrace();
    // }
    // }
    // }

    /**
     * Stores a profile picture
     * 
     * @param picture
     *            The picture to store
     * @param userId
     *            The user's ID
     */
    public void setUserPicture(Bitmap picture, long userId) {
        File file = new File(mContext.getFilesDir(), userId + ".png");

        if (file.exists()) {
            file.delete();
        }

        try {
            FileOutputStream out = mContext.openFileOutput(userId + ".png", Context.MODE_PRIVATE);
            picture.compress(Bitmap.CompressFormat.PNG, IMAGE_QUALITY, out);
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates an event
     * 
     * @param event
     *            The event to update
     * @return The number of rows that were affected
     */
    public int updateEvent(Event event) {

        ContentValues values = new ContentValues();
        values.put(KEY_ID, event.getId());
        values.put(KEY_NAME, event.getName());
        values.put(KEY_EVTDESC, event.getDescription());
        values.put(KEY_USER_ID, event.getCreatorId());
        values.put(KEY_LONGITUDE, event.getLocation().getLongitude());
        values.put(KEY_LATITUDE, event.getLocation().getLatitude());
        values.put(KEY_DATE, event.getStartDate().getTimeInMillis());
        values.put(KEY_ENDDATE, event.getEndDate().getTimeInMillis());

        int rows =
            DATABASE
                .update(TABLE_EVENT, values, KEY_ID + " = ?", new String[]{String.valueOf(event.getId())});

        return rows;
    }

    /**
     * Updates a filter
     * 
     * @param filter
     *            The updated filter
     */
    public void updateFilter(Filter filter) {

        this.deleteFilter(filter.getId());
        this.addFilter(filter);
    }

    /**
     * Updates a user's values
     * 
     * @param friend
     *            The user to update
     * @return The number of rows that were updated
     */
    public int updateFriend(ImmutableUser friend) {
        ContentValues values = new ContentValues();

        if (friend.getId() != User.NO_ID) {
            values.put(KEY_USER_ID, friend.getId());
        }
        if (friend.getName() != Friend.NO_NAME) {
            values.put(KEY_NAME, friend.getName());
        }
        if (friend.getPhoneNumber() != Friend.NO_NUMBER) {
            values.put(KEY_NUMBER, friend.getPhoneNumber());
        }
        if (friend.getEmail() != Friend.NO_EMAIL) {
            values.put(KEY_EMAIL, friend.getEmail());
        }
        if ((friend.getLocation().getLatitude() != User.NO_LATITUDE)
            || (friend.getLocation().getLongitude() != User.NO_LONGITUDE)) {
            values.put(KEY_LONGITUDE, friend.getLocation().getLongitude());
            values.put(KEY_LATITUDE, friend.getLocation().getLatitude());
        }
        if (friend.getLocationString() != Friend.NO_LOCATION_STRING) {
            values.put(KEY_POSNAME, friend.getLocationString());
        }

        int rows =
            DATABASE.update(TABLE_USER, values, KEY_USER_ID + " = ?",
                new String[]{String.valueOf(friend.getId())});

        return rows;
    }

    /**
     * @return The instance of DatabaseHelper
     */
    public static DatabaseHelper getInstance() {
        return mInstance;
    }

    /**
     * Initializes the database helper (should be called once when starting the
     * app)
     * 
     * @param context
     *            The app's context, needed to access the files
     * @return The DatabaseHelper instance
     */
    public static DatabaseHelper initialize(Context context) {
        if (mInstance == null) {
            mInstance = new DatabaseHelper(context);
            DATABASE = mInstance.getWritableDatabase();
        }

        return mInstance;
    }
}