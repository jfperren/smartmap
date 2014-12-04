package ch.epfl.smartmap.activities;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import ch.epfl.smartmap.R;
import ch.epfl.smartmap.cache.Cache;
import ch.epfl.smartmap.cache.Event;
import ch.epfl.smartmap.gui.EventsListItemAdapter;

/**
 * This activity shows an event in a complete screens. It display in addition two buttons: one to invite friends, and
 * one to see the event on the map.
 * 
 * @author SpicyCH
 */
public class EventInformationActivity extends Activity {

    private static final String TAG = EventInformationActivity.class.getSimpleName();

    private Event mEvent;
    private TextView mEventTitle;
    private TextView mEventCreator;
    private TextView mStart;
    private TextView mEnd;
    private TextView mEventDescription;
    private TextView mPlaceNameAndCountry;
    private Context mContext;

    /**
     * Used to get the event id the getExtra of the starting intent, and to pass the retrieved event from doInBackground
     * to onPostExecute.
     */
    private final static String EVENT_KEY = "EVENT";
    private final static String CREATOR_NAME_KEY = "CREATOR_NAME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_show_event_information);
        this.getActionBar().setBackgroundDrawable(this.getResources().getDrawable(R.color.main_blue));

        mContext = this.getApplicationContext();

        // This activity needs a (positive) event id to process. If none given, we finish it.
        if (this.getIntent().getLongExtra(EVENT_KEY, -1) > 0) {

            long eventId = this.getIntent().getLongExtra(EVENT_KEY, -1);

            Log.d(TAG, "Received event id " + eventId);

            // Need an AsyncTask because getEventById searches on our server if event not stored in cache.
            AsyncTask<Long, Void, Map<String, Object>> loadEvent = new AsyncTask<Long, Void, Map<String, Object>>() {

                @Override
                protected Map<String, Object> doInBackground(Long... params) {

                    Log.d(TAG, "Retrieving event...");

                    long eventId = params[0];

                    Map<String, Object> output = new HashMap<String, Object>();

                    Event event = Cache.getInstance().getEventById(eventId);
                    output.put(EVENT_KEY, event);

                    String creatorName = Cache.getInstance().getUserById(event.getCreatorId()).getName();
                    output.put(CREATOR_NAME_KEY, creatorName);

                    return output;
                }

                @Override
                protected void onPostExecute(Map<String, Object> result) {

                    Log.d(TAG, "Processing event...");

                    final Event event = (Event) result.get(EVENT_KEY);
                    final String creatorName = (String) result.get(CREATOR_NAME_KEY);

                    if ((event == null) || (creatorName == null)) {
                        Log.e(TAG, "The server returned a null event or creatorName");

                        Toast.makeText(mContext, mContext.getString(R.string.show_event_server_error),
                                Toast.LENGTH_SHORT).show();

                    } else {
                        mEvent = event;
                        EventInformationActivity.this.initializeGUI(event, creatorName);
                    }

                }

            };

            loadEvent.execute(eventId);
        } else {
            Log.e(TAG, "No event id put in the putextra of the intent that started this activity.");
            Toast.makeText(mContext, mContext.getString(R.string.error_client_side), Toast.LENGTH_SHORT).show();
            this.finish();
        }

    }

    /**
     * Triggered when the user clicks the "Invite friends" button.<br />
     * It launches InviteFriendsActivity for a result.
     * 
     * @param v
     * @author SpicyCH
     */
    public void inviteFriendsToEvent(View v) {
        Intent inviteFriends = new Intent(this, InviteFriendsActivity.class);
        this.startActivityForResult(inviteFriends, 1);
    }

    @Override
    public void onBackPressed() {
        if (this.getIntent().getBooleanExtra("NOTIFICATION", false) == true) {
            this.startActivity(new Intent(this, MainActivity.class));
        }
        this.finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        this.getMenuInflater().inflate(R.menu.show_event_information, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
            case android.R.id.home:
                if (this.getIntent().getBooleanExtra("NOTIFICATION", false) == true) {
                    this.startActivity(new Intent(this, MainActivity.class));
                }
                this.finish();
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Triggered when the button 'Shop on the map' is pressed. Opens the map at the location of the event.
     * 
     * @param v
     *            the button who has been clicked
     * 
     * @author SpicyCH
     */
    public void openMapAtEventLocation(View v) {
        Intent showEventIntent = new Intent(this, MainActivity.class);
        showEventIntent.putExtra("location", mEvent.getLocation());
        this.startActivity(showEventIntent);
    }

    /**
     * Initializes the different views of this activity.
     * 
     * @author SpicyCH
     */
    private void initializeGUI(Event mEvent, String creatorName) {

        this.setTitle(mEvent.getName());

        mEventTitle = (TextView) this.findViewById(R.id.show_event_info_event_name);
        mEventTitle.setText(mEvent.getName());

        mEventCreator = (TextView) this.findViewById(R.id.show_event_info_creator);
        mEventCreator.setText(this.getString(R.string.show_event_by) + " " + creatorName);

        mStart = (TextView) this.findViewById(R.id.show_event_info_start);
        mStart.setText(EventsListItemAdapter.getTextFromDate(mEvent.getStartDate(), mEvent.getEndDate(), "start"));

        mEnd = (TextView) this.findViewById(R.id.show_event_info_end);
        mEnd.setText(EventsListItemAdapter.getTextFromDate(mEvent.getStartDate(), mEvent.getEndDate(), "end"));

        mEventDescription = (TextView) this.findViewById(R.id.show_event_info_description);
        mEventDescription.setText(this.getString(R.string.show_event_info_event_description) + ":\n"
                + mEvent.getDescription());

        mPlaceNameAndCountry = (TextView) this.findViewById(R.id.show_event_info_town_and_country);
        mPlaceNameAndCountry.setText(mEvent.getLocationString() + ", " + "Country");
    }
}