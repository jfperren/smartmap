package ch.epfl.smartmap.cache;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import ch.epfl.smartmap.R;

/**
 * A class to represent the user's friends
 * 
 * @author ritterni
 */
public class FriendInvitation implements Invitation, Displayable {
	private long mId;
	private Intent mIntent;
	private String mTitle;
	private String mText;
	private User mUser;
	private boolean mIsRead;
	public static final int DEFAULT_PICTURE = R.drawable.ic_default_user; // placeholder
	public static final int IMAGE_QUALITY = 100;
	public static final String PROVIDER_NAME = "SmartMapServers";

	@Override
	public Bitmap getPicture(Context context) {
		return mUser.getPicture(context);
	}

	@Override
	public String getName() {
		return mUser.getName();
	}

	@Override
	public String getShortInfos() {
		return new String("Position : " + mUser.getPositionName() + "\n" + "Last seen : "
		    + mUser.getLastSeen());
	}

	@Override
	public long getID() {
		return mId;
	}

	@Override
	public Intent getIntent() {
		return mIntent;
	}

	@Override
	public String getTitle() {
		return mTitle;
	}

	@Override
	public String getText() {
		return mText;
	}

	@Override
	public User getUser() {
		return mUser;
	}

	@Override
	public boolean isRead() {
		return mIsRead;
	}

	@Override
	public void setTitle(String title) {
		mTitle = title;
	}

	@Override
	public void setText(String text) {
		mText = text;
	}

	@Override
	public void setUser(User user) {
		mUser = user;
	}

	@Override
	public void setRead(boolean isRead) {
		mIsRead = isRead;

	}

	@Override
	public void setIntent(Intent intent) {
		mIntent = intent;

	}

	@Override
	public Location getLocation() {
		return mUser.getLocation();
	}

}