package ch.epfl.smartmap.gui;

import java.util.Calendar;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import ch.epfl.smartmap.R;
import ch.epfl.smartmap.cache.Event;
import ch.epfl.smartmap.util.Utils;

/**
 * <p>
 * Adapter used to display the events in a list view. Used in {@link ch.epfl.smartmap.activities.ShowEventsActivity}.
 * </p>
 * <p>
 * To make the scrolling smooth, we use the view adapter design pattern. <br />
 * See <a href= "http://developer.android.com/training/improving-layouts/smooth-scrolling.html#ViewHolder"
 * >developer.android on ViewHolder</a>
 * </p>
 * 
 * @author SpicyCH
 */
public class EventsListItemAdapter extends ArrayAdapter<Event> {

    private final Context mContext;

    /**
     * Constructor
     * 
     * @param context
     * @param itemsArrayList
     */
    public EventsListItemAdapter(Context context, List<Event> itemsArrayList) {
        super(context, R.layout.gui_event_list_item, itemsArrayList);

        mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // Create inflater, get Friend View from the xml via Adapter
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        EventViewHolder viewHolder;
        Event event = this.getItem(position);

        View newConvertView;

        if (convertView == null) {

            newConvertView = inflater.inflate(R.layout.gui_event_list_item, parent, false);

            viewHolder = new EventViewHolder();

            // Set the ViewHolder with the gui_event_list_item fields
            viewHolder.setEventNameTextView((TextView) newConvertView.findViewById(R.id.event_list_item_event_name));
            viewHolder.setPlaceNameTextView((TextView) newConvertView.findViewById(R.id.event_list_item_place_name));
            viewHolder.setDatesTextView((TextView) newConvertView.findViewById(R.id.event_list_item_dates));

            // Needed by the code that makes each item clickable
            viewHolder.setEventId(event.getId());

            // Store the holder with the view
            newConvertView.setTag(viewHolder);
        } else {
            newConvertView = convertView;
            viewHolder = (EventViewHolder) newConvertView.getTag();
        }

        // Set fields with event's attributes

        Calendar start = event.getStartDate();
        Calendar end = event.getEndDate();

        String startString = Utils.getDateString(start) + " " + Utils.getTimeString(start);
        String endString = Utils.getDateString(end) + " " + Utils.getTimeString(end);

        String placeNameText = mContext.getString(R.string.events_list_item_adapter_near_with_first_uppercase) + " "
                + event.getLocationString();
        String startAndEndText = startString + " - " + endString;

        viewHolder.getPlaceNameTextView().setText(placeNameText);
        viewHolder.getDatesTextView().setText(startAndEndText);

        viewHolder.getEventNameTextView().setText(event.getName());

        newConvertView.setId(position);

        return newConvertView;
    }

}