package ch.epfl.smartmap.gui;

import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

/**
 * @author rbsteinm
 * 
 */
public class DrawerItemClickListener implements ListView.OnItemClickListener {

    private static final int INDEX_PROFILE = 0;
    private static final int INDEX_FRIENDS = 1;
    private static final int INDEX_EVENTS = 2;
    private static final int INDEX_FILTERS = 3;
    private static final int INDEX_SETTINGS = 4;

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch (position) {
            case INDEX_PROFILE:
                break;
            case INDEX_FRIENDS:
                Intent displayActivityIntent = new Intent(view.getContext(), FriendsActivity.class);
                view.getContext().startActivity(displayActivityIntent);
                break;
            case INDEX_EVENTS:
                break;
            case INDEX_FILTERS:
                break;
            case INDEX_SETTINGS:
                break;
            default:
                break;
        }
    }
}