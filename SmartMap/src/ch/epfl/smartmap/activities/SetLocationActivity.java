package ch.epfl.smartmap.activities;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import ch.epfl.smartmap.R;
import ch.epfl.smartmap.background.ServiceContainer;
import ch.epfl.smartmap.map.DefaultZoomManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * This activity lets the user select a new position for the event on the map
 * 
 * @author agpmilli
 */
public class SetLocationActivity extends FragmentActivity {

    private static final int GOOGLE_PLAY_REQUEST_CODE = 10;

    private GoogleMap mGoogleMap;
    private SupportMapFragment mFragmentMap;
    private LatLng mEventPosition;

    /**
     * Display the map with the current location
     */
    public void displayMap() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this.getBaseContext());

        // Showing status
        if (status != ConnectionResult.SUCCESS) {
            // Google Play Services are not available
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, GOOGLE_PLAY_REQUEST_CODE);
            dialog.show();
        } else {
            // Google Play Services are available.

            mFragmentMap =
                (SupportMapFragment) this.getSupportFragmentManager().findFragmentById(R.id.set_location_map);

            // Getting GoogleMap object from the fragment
            mGoogleMap = mFragmentMap.getMap();

            // Enabling MyLocation Layer of Google Map
            mGoogleMap.setMyLocationEnabled(true);

            if (this.getIntent().getParcelableExtra(AddEventActivity.LOCATION_EXTRA) == null) {
                // Initialize Event position to my current Position so it isn't
                // null for test
                mEventPosition =
                    new LatLng(ServiceContainer.getSettingsManager().getLocation().getLatitude(), ServiceContainer
                        .getSettingsManager().getLocation().getLongitude());
            } else {
                mEventPosition = this.getIntent().getParcelableExtra(AddEventActivity.LOCATION_EXTRA);
            }

            new DefaultZoomManager(mFragmentMap).zoomWithAnimation(mEventPosition);

            mGoogleMap.addMarker(new MarkerOptions().position(mEventPosition).draggable(true));

            mGoogleMap.setOnMarkerDragListener(new OnMarkerDragListener() {

                @Override
                public void onMarkerDrag(Marker marker) {
                    // nothing
                }

                @Override
                public void onMarkerDragEnd(Marker marker) {
                    mEventPosition = marker.getPosition();
                }

                @Override
                public void onMarkerDragStart(Marker marker) {
                    // nothing
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        this.finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_set_location);

        // Makes the logo clickable (clicking it returns to previous activity)
        this.getActionBar().setDisplayHomeAsUpEnabled(true);
        this.getActionBar().setBackgroundDrawable(this.getResources().getDrawable(R.color.main_blue));

        // Maybe set it longer, how?
        Toast.makeText(this, this.getString(R.string.set_location_toast), Toast.LENGTH_LONG).show();

        this.displayMap();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        this.getMenuInflater().inflate(R.menu.set_location, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        int id = item.getItemId();
        switch (id) {
            case R.id.set_location_done:
                this.setResultBackToAddEvent();
                this.finish();
                break;
            case android.R.id.home:
                this.finish();
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setResultBackToAddEvent() {
        Intent addEventIntent = new Intent(this, AddEventActivity.class);
        addEventIntent.putExtra(AddEventActivity.LOCATION_EXTRA, mEventPosition);
        this.setResult(RESULT_OK, addEventIntent);
    }
}
