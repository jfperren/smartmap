package ch.epfl.smartmap.cache;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LongSparseArray;
import ch.epfl.smartmap.background.Notifications;
import ch.epfl.smartmap.background.ServiceContainer;
import ch.epfl.smartmap.background.SettingsManager;
import ch.epfl.smartmap.callbacks.NetworkRequestCallback;
import ch.epfl.smartmap.database.DatabaseHelperInterface;
import ch.epfl.smartmap.listeners.CacheListener;
import ch.epfl.smartmap.servercom.SmartMapClient;
import ch.epfl.smartmap.servercom.SmartMapClientException;

/**
 * Note from @jfperren
 * Since the architecture change (which was really needed otherwise we
 * wouldn't have been able to continue this project) happened 4 weeks after the start of the project, I had to
 * create Containers to carry the informations about the live instance and transform them in the
 * Cache into live instances. This allows us to have only one live instance of anything at the time.
 * Since a lot of classes already in place were all modifying these instances all at the time, I couldn't
 * implement unique-instance listeners, and therefore had to implement them in the Cache which results in a
 * loss of speed and several useless costful updates. The problem with this architecture is that all methods
 * that modify the cache need to be called from the cache and thus there is a lot of code in here.
 */

/**
 * The {@code Cache} contains every instance of {@code User}, {@code Event}, {@code Invitation} and
 * {@code Filter} that is used by the GUI. You can initialize the Cache from a DatabaseHelper with
 * {@code initFromDatabase}, and then update it with a SmartMapClient using {@code updateFromNetwork}. All
 * methods in the Cache that call the {@code SmartMapClient} use {@code AsyncTask}s and then update the
 * {@code Cache} with the results
 * 
 * @author jfperren
 */
public class Cache implements CacheInterface {

    private static final String TAG = Cache.class.getSimpleName();

    // SparseArrays containing live instances
    private final LongSparseArray<User> mUserInstances;
    private final LongSparseArray<Event> mEventInstances;
    private final LongSparseArray<Filter> mFilterInstances;
    private final LongSparseArray<Invitation> mInvitationInstances;

    // These sets are the keys for the LongSparseArrays
    private final Set<Long> mUserIds;
    private final Set<Long> mEventIds;
    private final Set<Long> mFilterIds;
    private final Set<Long> mInvitationIds;

    // Contains the ids of the friends
    private final Set<Long> mFriendIds;
    private long mSelfId;

    // Id for the next filter to be added
    private long nextFilterId;

    // Contains all listeners
    private final List<CacheListener> mListeners;

    /**
     * Constructor
     */
    public Cache() {
        // Init Data structures
        mUserInstances = new LongSparseArray<User>();
        mEventInstances = new LongSparseArray<Event>();
        mFilterInstances = new LongSparseArray<Filter>();
        mInvitationInstances = new LongSparseArray<Invitation>();

        mFriendIds = new HashSet<Long>();
        mSelfId = ServiceContainer.getSettingsManager().getUserId();

        mUserIds = new HashSet<Long>();
        mEventIds = new HashSet<Long>();
        mFilterIds = new HashSet<Long>();
        mInvitationIds = new HashSet<Long>();

        nextFilterId = Filter.DEFAULT_FILTER_ID + 1;

        mListeners = new ArrayList<CacheListener>();

        this.putUser(UserContainer.newEmptyContainer().setId(mSelfId)
            .setName(ServiceContainer.getSettingsManager().getUserName()));
    }

    /*
     * (non-Javadoc)
     * @see
     * ch.epfl.smartmap.cache.CacheInterface#acceptInvitation(ch.epfl.smartmap
     * .cache.Invitation,
     * ch.epfl.smartmap.callbacks.NetworkRequestCallback)
     */
    @Override
    public synchronized void acceptInvitation(final Invitation invitation,
        final NetworkRequestCallback<Void> callback) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                return Cache.this.acceptInvitationTaskInBackground(invitation, callback);
            }
        }.execute();
    }

    /*
     * (non-Javadoc)
     * @see
     * ch.epfl.smartmap.cache.CacheInterface#addOnCacheListener(ch.epfl.smartmap
     * .listeners.CacheListener)
     */
    @Override
    public synchronized void addOnCacheListener(CacheListener listener) {
        mListeners.add(listener);
    }

    /*
     * (non-Javadoc)
     * @see
     * ch.epfl.smartmap.cache.CacheInterface#addParticipantsToEvent(java.util
     * .Set,
     * ch.epfl.smartmap.cache.Event,
     * ch.epfl.smartmap.callbacks.NetworkRequestCallback)
     */
    @Override
    public synchronized void addParticipantsToEvent(Set<Long> ids, final Event event,
        final NetworkRequestCallback<Void> callback) {
        Set<Long> newParticipantIds = event.getContainerCopy().getParticipantIds();
        newParticipantIds.addAll(ids);

        final EventContainer newImmutableEvent =
            event.getContainerCopy().setParticipantIds(newParticipantIds);

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    ServiceContainer.getNetworkClient().joinEvent(newImmutableEvent.getId());
                    ServiceContainer.getCache().updateEvent(newImmutableEvent);
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                } catch (SmartMapClientException e) {
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                }
                return null;
            }
        }.execute();
    }

    /*
     * (non-Javadoc)
     * @see
     * ch.epfl.smartmap.cache.CacheInterface#createEvent(ch.epfl.smartmap.cache
     * .ImmutableEvent,
     * ch.epfl.smartmap.callbacks.NetworkRequestCallback)
     */
    @Override
    public synchronized void createEvent(final EventContainer createdEvent,
        final NetworkRequestCallback<Event> callback) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                return Cache.this.createEventTaskInBackground(createdEvent, callback);
            }
        }.execute();
    }

    /*
     * (non-Javadoc)
     * @see
     * ch.epfl.smartmap.cache.CacheInterface#declineInvitation(ch.epfl.smartmap
     * .cache.Invitation,
     * ch.epfl.smartmap.callbacks.NetworkRequestCallback)
     */
    @Override
    public synchronized void declineInvitation(final Invitation invitation,
        final NetworkRequestCallback<Void> callback) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                return Cache.this.declineInvitationTaskInBackground(invitation, callback);
            }

        }.execute();
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#getAllActiveFilters()
     */
    @Override
    public synchronized Set<Filter> getAllActiveFilters() {
        return this.getFilters(new SearchFilter<Filter>() {
            @Override
            public synchronized boolean filter(Filter filter) {
                return filter.isActive();
            }
        });
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#getAllCustomFilters()
     */
    @Override
    public synchronized Set<Filter> getAllCustomFilters() {
        Set<Long> customFilterIds = new HashSet<Long>(mFilterIds);
        customFilterIds.remove(Filter.DEFAULT_FILTER_ID);
        return this.getFilters(customFilterIds);
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#getAllEvents()
     */
    @Override
    public synchronized Set<Event> getAllEvents() {
        return this.getEvents(mEventIds);
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#getAllFilters()
     */
    @Override
    public synchronized Set<Filter> getAllFilters() {
        return this.getFilters(mFilterIds);
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#getAllFriends()
     */
    @Override
    public synchronized Set<User> getAllFriends() {
        return this.getUsers(mFriendIds);
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#getAllInvitations()
     */
    @Override
    public synchronized SortedSet<Invitation> getAllInvitations() {
        return this.getInvitations(mInvitationIds);
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#getAllUsers()
     */
    @Override
    public synchronized Set<User> getAllUsers() {
        return this.getUsers(mUserIds);
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#getAllVisibleEvents()
     */
    @Override
    public synchronized Set<Event> getAllVisibleEvents() {
        Set<Event> allVisibleEvents = new HashSet<Event>();
        for (Event event : this.getAllEvents()) {
            if (event.isVisible()) {
                allVisibleEvents.add(event);
            }
        }

        return this.getEvents(mEventIds);
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#getAllVisibleFriends()
     */
    @Override
    public synchronized Set<User> getAllVisibleFriends() {
        // Get all friends
        Set<Long> allVisibleUsersId = new HashSet<Long>();
        if (this.getDefaultFilter() != null) {
            // Get all friends
            allVisibleUsersId.addAll(this.getDefaultFilter().getVisibleFriends());
        } else {
            allVisibleUsersId.addAll(mFriendIds);
        }

        // For each active filter, keep friends in it
        for (Long id : mFilterIds) {
            Filter filter = this.getFilter(id);
            if (filter.isActive()) {
                allVisibleUsersId.retainAll(filter.getVisibleFriends());
            }
        }

        // Return all friends that passed all filters
        return this.getUsers(allVisibleUsersId);
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#getDefaultFilter()
     */
    @Override
    public synchronized Filter getDefaultFilter() {
        return this.getFilter(Filter.DEFAULT_FILTER_ID);
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#getEvent(long)
     */
    @Override
    public synchronized Event getEvent(long id) {
        return mEventInstances.get(id);
    }

    /*
     * (non-Javadoc)
     * @see
     * ch.epfl.smartmap.cache.CacheInterface#getEvents(ch.epfl.smartmap.cache
     * .Cache.SearchFilter)
     */
    @Override
    public synchronized Set<Event> getEvents(SearchFilter<Event> filter) {
        Set<Event> events = new HashSet<Event>();
        for (long id : mEventIds) {
            Event event = this.getEvent(id);
            if (filter.filter(event)) {
                events.add(event);
            }
        }
        return events;
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#getEvents(java.util.Set)
     */
    @Override
    public synchronized Set<Event> getEvents(Set<Long> ids) {
        Set<Event> events = new HashSet<Event>();
        for (long id : ids) {
            Event event = this.getEvent(id);
            if (event != null) {
                events.add(event);
            }
        }
        return events;
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#getFilter(long)
     */
    @Override
    public synchronized Filter getFilter(long id) {
        return mFilterInstances.get(id);
    }

    /*
     * (non-Javadoc)
     * @see
     * ch.epfl.smartmap.cache.CacheInterface#getFilters(ch.epfl.smartmap.cache
     * .Cache.SearchFilter)
     */
    @Override
    public synchronized Set<Filter> getFilters(SearchFilter<Filter> searchFilter) {
        Set<Filter> filters = new HashSet<Filter>();

        for (long id : mFilterIds) {
            Filter filter = this.getFilter(id);
            if ((filter != null) && searchFilter.filter(filter)) {
                filters.add(filter);
            }
        }

        return filters;
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#getFilters(java.util.Set)
     */
    @Override
    public synchronized Set<Filter> getFilters(Set<Long> ids) {
        Set<Filter> filters = new HashSet<Filter>();

        for (long id : ids) {
            Filter filter = this.getFilter(id);
            if (filter != null) {
                filters.add(filter);
            }
        }

        return filters;
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#getFriendIds()
     */
    @Override
    public synchronized Set<Long> getFriendIds() {
        return mFriendIds;
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#getInvitation(long)
     */
    @Override
    public synchronized Invitation getInvitation(long id) {
        return mInvitationInstances.get(id);
    }

    /*
     * (non-Javadoc)
     * @see
     * ch.epfl.smartmap.cache.CacheInterface#getInvitations(ch.epfl.smartmap
     * .cache.Cache.SearchFilter)
     */
    @Override
    public synchronized SortedSet<Invitation> getInvitations(SearchFilter<Invitation> filter) {
        SortedSet<Invitation> invitations = new TreeSet<Invitation>();

        for (long id : mInvitationIds) {
            Invitation invitation = mInvitationInstances.get(id);
            if ((filter == null) || ((invitation != null) && filter.filter(invitation))) {
                invitations.add(invitation);
            }
        }

        return invitations;
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#getInvitations(java.util.Set)
     */
    @Override
    public synchronized SortedSet<Invitation> getInvitations(Set<Long> ids) {
        SortedSet<Invitation> invitations = new TreeSet<Invitation>();
        for (long id : ids) {
            Invitation invitation = this.getInvitation(id);
            if (invitation != null) {
                invitations.add(invitation);
            }
        }
        return invitations;
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#getLiveEvents()
     */
    @Override
    public synchronized Set<Event> getLiveEvents() {
        return this.getEvents(new SearchFilter<Event>() {
            @Override
            public synchronized boolean filter(Event item) {
                return item.isLive();
            }
        });
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#getMyEvents()
     */
    @Override
    public synchronized Set<Event> getMyEvents() {
        return this.getEvents(new SearchFilter<Event>() {
            @Override
            public synchronized boolean filter(Event item) {
                return item.isOwn();
            }
        });
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#getNearEvents()
     */
    @Override
    public synchronized Set<Event> getNearEvents() {
        return this.getEvents(new SearchFilter<Event>() {
            @Override
            public synchronized boolean filter(Event item) {
                return item.isNear();
            }
        });
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#getParticipatingEvents()
     */
    @Override
    public synchronized Set<Event> getParticipatingEvents() {
        return this.getEvents(new SearchFilter<Event>() {
            @Override
            public synchronized boolean filter(Event item) {
                return item.isGoing();
            }
        });
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#getSelf()
     */
    @Override
    public synchronized User getSelf() {
        return mUserInstances.get(mSelfId);
    }

    /*
     * (non-Javadoc)
     * @see
     * ch.epfl.smartmap.cache.CacheInterface#getUnansweredFriendInvitations()
     */
    @Override
    public synchronized SortedSet<Invitation> getUnansweredFriendInvitations() {
        return this.getInvitations(new SearchFilter<Invitation>() {
            @Override
            public boolean filter(Invitation item) {
                int type = item.getType();
                int status = item.getStatus();
                return (type == Invitation.FRIEND_INVITATION)
                    && ((status == Invitation.READ) || (status == Invitation.UNREAD));
            }
        });
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#getUser(long)
     */
    @Override
    public synchronized User getUser(long id) {
        return mUserInstances.get(id);
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#getUsers(java.util.Set)
     */
    @Override
    public synchronized Set<User> getUsers(Set<Long> ids) {
        Set<User> users = new HashSet<User>();
        for (long id : ids) {
            User user = this.getUser(id);
            if (user != null) {
                users.add(user);
            }
        }
        return users;
    }

    /*
     * (non-Javadoc)
     * @see
     * ch.epfl.smartmap.cache.CacheInterface#initFromDatabase(ch.epfl.smartmap
     * .database.DatabaseHelper)
     */
    @Override
    public synchronized void initFromDatabase(DatabaseHelperInterface database) {
        // Clear previous values
        mEventInstances.clear();
        mUserInstances.clear();
        mFilterInstances.clear();
        mInvitationInstances.clear();

        // Clear ids
        mUserIds.clear();
        mEventIds.clear();
        mFilterIds.clear();
        mInvitationIds.clear();

        // Clear friend ids
        mFriendIds.clear();
        mSelfId = User.NO_ID;

        // Fill with database values
        this.putUsers(database.getAllUsers());
        this.putEvents(database.getAllEvents());
        this.putFilters(database.getAllFilters());
        this.putInvitations(database.getAllInvitations());

        // Notify listeners
        for (CacheListener listener : mListeners) {
            listener.onEventListUpdate();
            listener.onUserListUpdate();
            listener.onFilterListUpdate();
        }
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#inviteFriendsToEvent(long,
     * java.util.Set,
     * ch.epfl.smartmap.callbacks.NetworkRequestCallback)
     */
    @Override
    public synchronized void inviteFriendsToEvent(final long eventId, final Set<Long> usersIds,
        final NetworkRequestCallback<Void> callback) {

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    ServiceContainer.getNetworkClient().inviteUsersToEvent(eventId,
                        new ArrayList<Long>(usersIds));
                    callback.onSuccess(null);
                } catch (SmartMapClientException e) {
                    callback.onFailure(e);
                }
                return null;
            }
        }.execute();
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#inviteUser(long,
     * ch.epfl.smartmap.callbacks.NetworkRequestCallback)
     */
    @Override
    public synchronized void inviteUser(long id, final NetworkRequestCallback<Void> callback) {
        new AsyncTask<Long, Void, Void>() {
            @Override
            protected Void doInBackground(Long... params) {
                try {
                    ServiceContainer.getNetworkClient().inviteFriend(params[0]);
                    callback.onSuccess(null);
                } catch (SmartMapClientException e) {
                    Log.e(TAG, "Error while inviting friend: " + e);
                    callback.onFailure(e);
                }
                return null;
            }
        }.execute(id);
    }

    /*
     * (non-Javadoc)
     * @see
     * ch.epfl.smartmap.cache.CacheInterface#modifyOwnEvent(ch.epfl.smartmap
     * .cache.ImmutableEvent,
     * ch.epfl.smartmap.callbacks.NetworkRequestCallback)
     */
    @Override
    public synchronized void modifyOwnEvent(final EventContainer createdEvent,
        final NetworkRequestCallback<Void> callback) {
        new AsyncTask<EventContainer, Void, Void>() {

            @Override
            protected Void doInBackground(EventContainer... params) {
                try {
                    ServiceContainer.getNetworkClient().updateEvent(params[0]);
                    Cache.this.updateEvent(params[0]);
                    callback.onSuccess(null);
                } catch (SmartMapClientException e) {
                    callback.onFailure(e);
                    Log.e(TAG, "Error while modifying own event: " + e);
                }
                return null;
            }

        }.execute(createdEvent);
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#notifyEventListeners()
     */
    @Override
    public synchronized void notifyEventListeners() {
        for (CacheListener listener : mListeners) {
            listener.onEventListUpdate();
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * ch.epfl.smartmap.cache.CacheInterface#putEvent(ch.epfl.smartmap.cache
     * .ImmutableEvent)
     */
    @Override
    public synchronized void putEvent(EventContainer newEvent) {
        Set<EventContainer> singleton = new HashSet<EventContainer>();
        singleton.add(newEvent);
        this.putEvents(singleton);
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#putEvents(java.util.Set)
     */
    @Override
    public synchronized void putEvents(Set<EventContainer> newEvents) {
        boolean needToCallListeners = false;

        Set<UserContainer> usersToAdd = new HashSet<UserContainer>();
        Set<EventContainer> eventsToUpdate = new HashSet<EventContainer>();
        Set<EventContainer> eventsToAdd = new HashSet<EventContainer>();

        for (final EventContainer newEvent : newEvents) {
            // Get id
            long eventId = newEvent.getId();

            if (this.getEvent(eventId) != null) {
                // Put in the update list
                eventsToUpdate.add(newEvent);
            } else {
                // Need to add to Cache, check if contains all informations
                if (newEvent.getCreatorContainer() != null) {
                    eventsToAdd.add(newEvent);
                    usersToAdd.add(newEvent.getCreatorContainer());
                }
            }

            // Add users that need to be added
            this.putUsers(usersToAdd);

            // Add user to Container for new Events & Add to SparseArray
            for (EventContainer eventInfo : eventsToAdd) {
                needToCallListeners = true;
                eventInfo.setCreator(this.getUser(eventInfo.getCreatorContainer().getId()));
                mEventIds.add(eventInfo.getId());
                mEventInstances.put(eventInfo.getId(), Event.createFromContainer(eventInfo));
            }

            // Update Events to need to be updated & put true if update didnt
            // call listeners
            needToCallListeners = !this.updateEvents(eventsToUpdate) && needToCallListeners;

            // Update Listeners if needed
            if (needToCallListeners) {
                for (CacheListener listener : mListeners) {
                    listener.onEventListUpdate();
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * ch.epfl.smartmap.cache.CacheInterface#putFilter(ch.epfl.smartmap.cache
     * .ImmutableFilter)
     */
    @Override
    public synchronized void putFilter(FilterContainer newFilter) {
        Set<FilterContainer> singleton = new HashSet<FilterContainer>();
        singleton.add(newFilter);
        this.putFilters(singleton);
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#putFilters(java.util.Set)
     */
    @Override
    public synchronized void putFilters(Set<FilterContainer> newFilters) {
        boolean needToCallListeners = false;

        Set<FilterContainer> filtersToUpdate = new HashSet<FilterContainer>();

        for (FilterContainer newFilter : newFilters) {
            if (!mFilterIds.contains(newFilter.getId())) {
                // if not default
                if (newFilter.getId() != Filter.DEFAULT_FILTER_ID) {
                    // Need to set an id
                    newFilter.setId(nextFilterId);
                    nextFilterId++;
                }

                mFilterIds.add(newFilter.getId());
                mFilterInstances.put(newFilter.getId(), Filter.createFromContainer(newFilter));
                needToCallListeners = true;
            } else {
                // Put in update set
                filtersToUpdate.add(newFilter);
            }
        }

        // Update filters that need to be added
        needToCallListeners = !this.updateFilters(filtersToUpdate) && needToCallListeners;

        // Notify listeners
        if (needToCallListeners) {
            for (CacheListener listener : mListeners) {
                listener.onFilterListUpdate();
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * ch.epfl.smartmap.cache.CacheInterface#putInvitation(ch.epfl.smartmap.
     * cache.ImmutableInvitation)
     */
    @Override
    public synchronized void putInvitation(InvitationContainer invitationInfo) {
        Set<InvitationContainer> singleton = new HashSet<InvitationContainer>();
        singleton.add(invitationInfo);
        this.putInvitations(singleton);
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#putInvitations(java.util.Set)
     */
    @Override
    public synchronized void putInvitations(Set<InvitationContainer> invitationInfos) {
        boolean needToCallListeners = false;

        // Contains values to add later all at once
        Set<UserContainer> usersToAdd = new HashSet<UserContainer>();
        Set<EventContainer> eventsToAdd = new HashSet<EventContainer>();
        Set<InvitationContainer> invitationsToAdd = new HashSet<InvitationContainer>();

        this.processInvitations(invitationInfos, usersToAdd, eventsToAdd, invitationsToAdd);

        // Add all users
        this.putUsers(usersToAdd);

        // Add all events
        this.putEvents(eventsToAdd);

        // Create and add live instances of Invitations
        for (InvitationContainer invitationInfo : invitationsToAdd) {
            boolean isSetCorrectly = false;

            switch (invitationInfo.getType()) {
                case Invitation.FRIEND_INVITATION:
                case Invitation.ACCEPTED_FRIEND_INVITATION:
                    invitationInfo.setUser(this.getUser(invitationInfo.getUserInfos().getId()));
                    isSetCorrectly = invitationInfo.getUser() != null;
                    break;
                case Invitation.EVENT_INVITATION:
                    invitationInfo.setEvent(this.getEvent(invitationInfo.getEventInfos().getId()));
                    isSetCorrectly = invitationInfo.getEvent() != null;
                    break;
                default:
                    assert false;
                    break;
            }

            if (isSetCorrectly) {
                mInvitationIds.add(invitationInfo.getId());
                long invitationId = invitationInfo.getId();
                Invitation invitation = Invitation.createFromContainer(invitationInfo);
                mInvitationInstances.put(invitationInfo.getId(), invitation);
                if (invitationId != Invitation.ALREADY_RECEIVED) {
                    Notifications.createNotification(invitation, ServiceContainer.getSettingsManager()
                        .getContext());
                }
            }

            needToCallListeners = true;
        }

        if (needToCallListeners) {
            for (CacheListener listener : mListeners) {
                listener.onInvitationListUpdate();
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * ch.epfl.smartmap.cache.CacheInterface#putUser(ch.epfl.smartmap.cache.
     * ImmutableUser)
     */
    @Override
    public synchronized void putUser(UserContainer newFriend) {
        Set<UserContainer> singleton = new HashSet<UserContainer>();
        singleton.add(newFriend);
        this.putUsers(singleton);
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#putUsers(java.util.Set)
     */
    @Override
    public synchronized void putUsers(Set<UserContainer> newUsers) {
        boolean needToCallListeners = false;

        Set<UserContainer> usersToUpdate = new HashSet<UserContainer>();

        for (UserContainer newUser : newUsers) {
            mUserIds.add(newUser.getId());

            if (newUser.getFriendship() == User.FRIEND) {
                mFriendIds.add(newUser.getId());
            } else if (newUser.getFriendship() == User.SELF) {
                mSelfId = newUser.getId();
            }

            if (mUserInstances.get(newUser.getId()) == null) {
                if ((newUser.getFriendship() == User.FRIEND) || (newUser.getFriendship() == User.STRANGER)
                    || (newUser.getFriendship() == User.SELF)) {
                    mUserInstances.put(newUser.getId(), User.createFromContainer(newUser));
                    needToCallListeners = true;
                }
            } else {
                // Put in set for update
                usersToUpdate.add(newUser);
            }
        }

        // Update users that need to be updated
        needToCallListeners = !this.updateUsers(usersToUpdate) && needToCallListeners;

        // Notify listeners if needed
        if (needToCallListeners) {
            for (CacheListener listener : mListeners) {
                listener.onUserListUpdate();
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#readAllInvitations()
     */
    @Override
    public synchronized void readAllInvitations() {
        SortedSet<Invitation> unreadInvitations = this.getInvitations(new Cache.SearchFilter<Invitation>() {
            @Override
            public boolean filter(Invitation item) {
                // Get Unread invitations
                return item.getStatus() == Invitation.UNREAD;
            }
        });

        Set<InvitationContainer> readInvitations = new HashSet<InvitationContainer>();

        for (Invitation invitation : unreadInvitations) {
            readInvitations.add(invitation.getContainerCopy().setStatus(Invitation.READ));
        }

        this.updateInvitations(readInvitations);
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#removeEvent(long)
     */
    @Override
    public synchronized void removeEvent(long id) {
        Set<Long> singleton = new HashSet<Long>();
        singleton.add(id);
        this.removeEvents(singleton);
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#removeEvents(java.util.Set)
     */
    @Override
    public synchronized void removeEvents(Set<Long> ids) {
        boolean isListModified = false;

        for (long id : ids) {
            if (mEventIds.contains(id)) {
                // Remove id from sets
                mEventIds.remove(id);

                // Remove instance from array
                mEventInstances.remove(id);

                isListModified = true;
            }
        }

        // Notify listeners if needed
        if (isListModified) {
            for (CacheListener l : mListeners) {
                l.onEventListUpdate();
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#removeFilter(long)
     */
    @Override
    public synchronized void removeFilter(long id) {
        Set<Long> singleton = new HashSet<Long>();
        singleton.add(id);
        this.removeFilters(singleton);
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#removeFilters(java.util.Set)
     */
    @Override
    public synchronized void removeFilters(Set<Long> ids) {
        boolean isListModified = false;

        for (long id : ids) {
            // Check that we are not trying to remove the default filter
            if (mFilterIds.contains(id) && (id != Filter.DEFAULT_FILTER_ID)) {
                // Remove id from sets
                mFilterIds.remove(id);

                // Remove instance from array
                mFilterInstances.remove(id);

                isListModified = true;
            }
        }

        // Notify listeners if needed
        if (isListModified) {
            for (CacheListener l : mListeners) {
                l.onFilterListUpdate();
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#removeFriend(long,
     * ch.epfl.smartmap.callbacks.NetworkRequestCallback)
     */
    @Override
    public synchronized void removeFriend(long id, final NetworkRequestCallback<Void> callback) {
        Set<Long> singleton = new HashSet<Long>();
        singleton.add(id);
        this.removeFriends(singleton, callback);
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#removeFriends(java.util.Set,
     * ch.epfl.smartmap.callbacks.NetworkRequestCallback)
     */
    @Override
    public synchronized void removeFriends(Set<Long> ids, final NetworkRequestCallback<Void> callback) {
        boolean isListModified = false;
        for (long id : ids) {
            if (mFriendIds.contains(id)) {
                new AsyncTask<Long, Void, Void>() {
                    @Override
                    protected Void doInBackground(Long... params) {
                        try {
                            ServiceContainer.getNetworkClient().removeFriend(params[0]);
                            callback.onSuccess(null);
                        } catch (SmartMapClientException e) {
                            callback.onFailure(e);
                        }
                        return null;
                    }
                }.execute(id);

                // Remove id from sets
                mFriendIds.remove(id);
                mUserInstances.remove(id);
                // Remove instance from array
                mUserInstances.remove(id);

                isListModified = true;
            }
        }

        // Notify listeners if needed
        if (isListModified) {
            for (CacheListener l : mListeners) {
                l.onUserListUpdate();
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * ch.epfl.smartmap.cache.CacheInterface#removeParticipantsFromEvent(java.util.Set,
     * ch.epfl.smartmap.cache.Event, ch.epfl.smartmap.callbacks.NetworkRequestCallback)
     */
    @Override
    public synchronized void removeParticipantsFromEvent(Set<Long> ids, Event event,
        final NetworkRequestCallback<Void> callback) {
        Set<Long> newParticipantIds = event.getContainerCopy().getParticipantIds();
        newParticipantIds.removeAll(ids);

        final EventContainer newImmutableEvent =
            event.getContainerCopy().setParticipantIds(newParticipantIds);

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    ServiceContainer.getNetworkClient().leaveEvent(newImmutableEvent.getId());
                    ServiceContainer.getCache().updateEvent(newImmutableEvent);
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                } catch (SmartMapClientException e) {
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                }
                return null;
            }
        }.execute();
    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#removeUsers(java.util.Set)
     */
    @Override
    public synchronized void removeUsers(Set<Long> userIds) {
        boolean isListModified = false;

        for (long id : userIds) {
            if (this.getUser(id) != null) {
                mUserInstances.remove(id);
                mUserIds.remove(id);
                mFriendIds.remove(id);
                isListModified = true;
            }
        }

        if (isListModified) {
            for (CacheListener listener : mListeners) {
                listener.onUserListUpdate();
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * ch.epfl.smartmap.cache.CacheInterface#setBlockedStatus(ch.epfl.smartmap
     * .cache.ImmutableUser,
     * ch.epfl.smartmap.callbacks.NetworkRequestCallback)
     */
    @Override
    public synchronized void setBlockedStatus(final UserContainer user,
        final NetworkRequestCallback<Void> callback) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                return Cache.this.setBlockedStatusTaskInBackground(user, callback);
            }
        }.execute();
    }

    /*
     * (non-Javadoc)
     * @see
     * ch.epfl.smartmap.cache.CacheInterface#updateFromNetwork(ch.epfl.smartmap
     * .servercom.SmartMapClient,
     * ch.epfl.smartmap.callbacks.NetworkRequestCallback)
     */
    @Override
    public synchronized void updateFromNetwork(final SmartMapClient networkClient)
        throws SmartMapClientException {

        // Get settings
        SettingsManager settingsManager = ServiceContainer.getSettingsManager();

        // Sets with new values (avoid calling multiple times the
        // listeners)
        Set<UserContainer> updatedUsers = new HashSet<UserContainer>();
        Set<EventContainer> updatedEvents = new HashSet<EventContainer>();

        // Update self informations
        long myId = settingsManager.getUserId();
        UserContainer self = networkClient.getUserInfo(myId);
        if (self != null) {
            self.setImage(networkClient.getProfilePicture(myId));
            updatedUsers.add(self);
        }

        // Fetch friend ids
        Set<Long> friendIds = new HashSet<Long>(networkClient.getFriendsIds());
        Set<Long> friendPosIds = new HashSet<Long>();
        // Fetch friends via listFriendPos
        Set<UserContainer> listFriendPos = new HashSet<UserContainer>(networkClient.listFriendsPos());

        for (UserContainer positionInfos : listFriendPos) {
            // get id
            long id = positionInfos.getId();
            // Get other online info
            UserContainer onlineInfos = networkClient.getUserInfo(id);
            // Get picture
            Bitmap image = networkClient.getProfilePicture(id);
            // Put all inside container
            onlineInfos.setLocation(positionInfos.getLocation());
            onlineInfos.setLocationString(positionInfos.getLocationString());
            onlineInfos.setImage(image);

            // Put friend in Set
            friendPosIds.add(id);
            updatedUsers.add(onlineInfos);
        }

        // For friends that blocked us or that we blocked, try to find a value
        // in cache or database
        Set<Long> friendThatBlockedUsIds = friendIds;
        friendThatBlockedUsIds.removeAll(friendPosIds);
        for (long id : friendThatBlockedUsIds) {
            User cached = Cache.this.getUser(id);
            if (cached != null) {
                updatedUsers.add(cached.getContainerCopy());
            } else {
                UserContainer friend = ServiceContainer.getDatabase().getUser(id);
                if (friend != null) {
                    UserContainer onlineValues = networkClient.getUserInfo(id);
                    friend.setName(onlineValues.getName());
                } else {
                    friend = networkClient.getUserInfo(id);
                }
                Bitmap image = networkClient.getProfilePicture(id);
                friend.setImage(image);
                friend.setFriendship(User.FRIEND);
                updatedUsers.add(friend);
            }
        }

        // Get near Events
        Set<Long> nearEventIds =
            new HashSet<Long>(networkClient.getPublicEvents(settingsManager.getLocation().getLatitude(),
                settingsManager.getLocation().getLongitude(), settingsManager.getNearEventsMaxDistance()));

        // Update all cached event if needed
        for (long id : mEventIds) {
            // Get event infos
            EventContainer onlineInfos = networkClient.getEventInfo(id);
            // Check if event needs to be kept
            if (nearEventIds.contains(id) || (onlineInfos.getCreatorContainer().getId() == myId)
                || onlineInfos.getParticipantIds().contains(myId)) {
                // if so, put it in Set
                updatedEvents.add(onlineInfos);
                updatedUsers.add(onlineInfos.getCreatorContainer());
            }
        }

        // Update users from invitations
        for (Invitation invitation : Cache.this.getAllInvitations()) {
            if (invitation.getType() == Invitation.FRIEND_INVITATION) {
                // get id
                long id = invitation.getUser().getId();
                // Get online info
                UserContainer onlineInfos = networkClient.getUserInfo(id);
                // Get picture
                Bitmap image = networkClient.getProfilePicture(id);
                // Put all inside container
                onlineInfos.setImage(image);

                // Put friend in Set
                updatedUsers.add(onlineInfos);
            }
        }

        // Put new values in cache
        Cache.this.keepOnlyTheseUsers(updatedUsers);
        Cache.this.keepOnlyTheseEvents(updatedEvents);

    }

    /*
     * (non-Javadoc)
     * @see ch.epfl.smartmap.cache.CacheInterface#updateUserInfos(long)
     */
    @Override
    public synchronized void updateUserInfos(long id) {
        new AsyncTask<Long, Void, Void>() {
            @Override
            protected Void doInBackground(Long... params) {
                try {
                    UserContainer userInfos = ServiceContainer.getNetworkClient().getUserInfo(params[0]);
                    if ((userInfos != null) && (ServiceContainer.getNetworkClient() != null)) {
                        userInfos.setImage(ServiceContainer.getNetworkClient().getProfilePicture(params[0]));
                    }
                    Cache.this.updateUser(userInfos);
                } catch (SmartMapClientException e) {
                    Log.e(TAG, "SmartMapClientException : " + e);
                }
                return null;
            }
        }.execute(id);
    }

    /**
     * Accepts a friend invitation by sending a request to the server and
     * updating the cache
     * 
     * @param invitation
     * @throws SmartMapClientException
     */
    private synchronized void acceptFriendInvitation(Invitation invitation) throws SmartMapClientException {
        UserContainer newFriend =
            ServiceContainer.getNetworkClient().acceptInvitation(invitation.getUser().getId());
        ServiceContainer.getDatabase().deletePendingFriend(invitation.getUser().getId());
        newFriend.setFriendship(User.FRIEND);
        this.putUser(newFriend);
    }

    /**
     * Body of doInBackground in acceptInvitation asyncTask
     * 
     * @param invitation
     *            the invitation to accept
     * @param callback
     *            the callback to network request
     * @return Void
     */
    private Void
        acceptInvitationTaskInBackground(Invitation invitation, NetworkRequestCallback<Void> callback) {
        try {
            switch (invitation.getType()) {
                case Invitation.FRIEND_INVITATION:
                    this.acceptFriendInvitation(invitation);
                    break;
                case Invitation.EVENT_INVITATION:
                    long eventId = ((GenericInvitation) invitation).getEvent().getId();
                    ServiceContainer.getNetworkClient().joinEvent(eventId);
                    Cache.this.putEvent(ServiceContainer.getNetworkClient().getEventInfo(eventId));
                    break;
                default:
                    assert false;
                    break;
            }

            Cache.this.updateInvitation(invitation.getContainerCopy().setStatus(Invitation.ACCEPTED));

            callback.onSuccess(null);
        } catch (SmartMapClientException e) {
            Log.e(TAG, "Error while accepting invitation: " + e);
            callback.onFailure(e);
        }
        return null;
    }

    /**
     * Body of doInBackground in createEvent asyncTask
     * 
     * @param createdEvent
     *            the created event
     * @param callback
     *            the callback to network request
     * @return Void
     */
    private Void createEventTaskInBackground(EventContainer createdEvent,
        NetworkRequestCallback<Event> callback) {
        try {
            long eventId;
            eventId = ServiceContainer.getNetworkClient().createPublicEvent(createdEvent);
            // Add ID to event
            createdEvent.setId(eventId);
            // Puts event in Cache
            Cache.this.putEvent(createdEvent);
            if (callback != null) {
                callback.onSuccess(Cache.this.getEvent(eventId));
            }
        } catch (SmartMapClientException e) {
            Log.e(TAG, "Error while creating event: " + e);
            if (callback != null) {
                callback.onFailure(e);
            }
        }
        return null;
    }

    /**
     * Body of doInBackground in declineInvitation asyncTask
     * 
     * @param invitation
     *            the invitation to be declined
     * @param callback
     *            the callback to network request
     * @return Void
     */
    private Void declineInvitationTaskInBackground(Invitation invitation,
        NetworkRequestCallback<Void> callback) {
        try {
            switch (invitation.getType()) {
                case Invitation.FRIEND_INVITATION:
                    // Decline online
                    ServiceContainer.getNetworkClient().declineInvitation(invitation.getUser().getId());
                    ServiceContainer.getDatabase().deletePendingFriend(invitation.getUser().getId());
                    break;
                case Invitation.EVENT_INVITATION:
                    // No interaction needed here
                    break;
                default:
                    assert false;
                    break;
            }

            Cache.this.updateInvitation(invitation.getContainerCopy().setStatus(Invitation.DECLINED));

            callback.onSuccess(null);
        } catch (SmartMapClientException e) {
            Log.e(TAG, "Error while declining invitation: " + e);
            callback.onFailure(e);
        }
        return null;
    }

    private synchronized void keepOnlyTheseEvents(Set<EventContainer> events) {
        mEventIds.clear();
        mEventInstances.clear();
        this.putEvents(events);
    }

    private synchronized void keepOnlyTheseUsers(Set<UserContainer> users) {
        mFriendIds.clear();
        mUserIds.clear();
        mUserInstances.clear();
        this.putUsers(users);
    }

    /**
     * Processes invitations and puts them in sets to add to the cache if
     * necessary.
     * 
     * @param invitationInfos
     * @param usersToAdd
     * @param eventsToAdd
     * @param invitationsToAdd
     */
    private void processInvitations(Set<InvitationContainer> invitationInfos, Set<UserContainer> usersToAdd,
        Set<EventContainer> eventsToAdd, Set<InvitationContainer> invitationsToAdd) {
        for (final InvitationContainer invitationInfo : invitationInfos) {

            // Get Id
            if (invitationInfo.getId() == Invitation.NO_ID) {
                // Get Id from database
                long id = ServiceContainer.getDatabase().addInvitation(invitationInfo);
                invitationInfo.setId(id);
            }

            if ((invitationInfo.getId() != Invitation.ALREADY_RECEIVED)
                && (this.getInvitation(invitationInfo.getId()) == null)) {
                switch (invitationInfo.getType()) {
                    case Invitation.FRIEND_INVITATION:
                        // Check that it contains all informations
                        if (invitationInfo.getUserInfos() != null) {
                            invitationsToAdd.add(invitationInfo);
                            usersToAdd.add(invitationInfo.getUserInfos());
                        }
                        break;
                    case Invitation.ACCEPTED_FRIEND_INVITATION:
                        // Check that it contains all informations
                        UserContainer newFriend = invitationInfo.getUserInfos();
                        if (newFriend != null) {
                            newFriend.setFriendship(User.FRIEND);
                            usersToAdd.add(newFriend);
                            invitationsToAdd.add(invitationInfo);
                        }
                        // Acknowledge new friend
                        new AsyncTask<Long, Void, Void>() {
                            @Override
                            protected Void doInBackground(Long... params) {
                                try {
                                    ServiceContainer.getNetworkClient().ackAcceptedInvitation(params[0]);
                                } catch (SmartMapClientException e) {
                                    Log.e(TAG, "Error while acknowledging accpeted invitation : " + e);
                                }
                                return null;
                            }
                        }.execute(invitationInfo.getUserId());
                        break;
                    case Invitation.EVENT_INVITATION:
                        // Check that it contains all informations
                        if (invitationInfo.getEventInfos() != null) {
                            invitationsToAdd.add(invitationInfo);
                            eventsToAdd.add(invitationInfo.getEventInfos());
                        }
                        // Acknowledge event invitation
                        new AsyncTask<Long, Void, Void>() {
                            @Override
                            protected Void doInBackground(Long... params) {
                                try {
                                    ServiceContainer.getNetworkClient().ackEventInvitation(params[0]);
                                } catch (SmartMapClientException e) {
                                    Log.e(TAG, "Error while acknowledging event invitation : " + e);
                                }
                                return null;
                            }
                        }.execute(invitationInfo.getEventId());
                        break;
                    default:
                        assert false;
                        break;
                }
            }
        }
    }

    /**
     * Body of doInBackground in setBlockedStatus asyncTask
     * 
     * @param user
     *            the user to be blocked/unblocked
     * @param callback
     *            the callback to network request
     * @return Void
     */
    private Void setBlockedStatusTaskInBackground(UserContainer user, NetworkRequestCallback<Void> callback) {
        try {
            boolean changed = false;
            if (user.isBlocked() == User.BlockStatus.UNBLOCKED) {
                ServiceContainer.getNetworkClient().unblockFriend(user.getId());
                changed = Cache.this.updateUser(user);
            } else {
                ServiceContainer.getNetworkClient().blockFriend(user.getId());
                changed = Cache.this.updateUser(user);
            }
            if (changed) {
                for (CacheListener listener : mListeners) {
                    listener.onUserListUpdate();
                }
            }
            if (callback != null) {
                callback.onSuccess(null);
            }
        } catch (SmartMapClientException e) {
            if (callback != null) {
                callback.onFailure(e);
            }
        }
        return null;
    }

    private synchronized boolean updateEvent(EventContainer eventInfo) {
        Set<EventContainer> singleton = new HashSet<EventContainer>();
        singleton.add(eventInfo);
        return this.updateEvents(singleton);
    }

    private synchronized boolean updateEvents(Set<EventContainer> eventInfos) {
        boolean isListModified = false;
        for (EventContainer eventInfo : eventInfos) {
            Event event = this.getEvent(eventInfo.getId());
            if ((event != null) && event.update(eventInfo)) {
                isListModified = true;
            }
        }

        if (isListModified) {
            for (CacheListener listener : mListeners) {
                listener.onEventListUpdate();
            }
        }

        return isListModified;
    }

    @SuppressWarnings("unused")
    private synchronized boolean updateFilter(FilterContainer filterInfo) {
        Set<FilterContainer> singleton = new HashSet<FilterContainer>();
        singleton.add(filterInfo);
        return this.updateFilters(singleton);
    }

    private synchronized boolean updateFilters(Set<FilterContainer> filterInfos) {
        boolean isListModified = false;

        for (FilterContainer filterInfo : filterInfos) {
            Filter filter = this.getFilter(filterInfo.getId());
            if ((filter != null) && filter.update(filterInfo)) {
                isListModified = true;
            }
        }

        if (isListModified) {
            for (CacheListener listener : mListeners) {
                listener.onFilterListUpdate();
            }
        }

        return isListModified;
    }

    private synchronized boolean updateInvitation(InvitationContainer invitation) {
        Set<InvitationContainer> singleton = new HashSet<InvitationContainer>();
        singleton.add(invitation);
        return this.updateInvitations(singleton);
    }

    private synchronized boolean updateInvitations(Set<InvitationContainer> invitations) {
        boolean isListModified = false;
        for (InvitationContainer invitation : invitations) {
            isListModified = isListModified || this.getInvitation(invitation.getId()).update(invitation);
        }

        if (isListModified) {
            for (CacheListener listener : mListeners) {
                listener.onInvitationListUpdate();
            }
        }

        return isListModified;
    }

    /**
     * OK
     * 
     * @param userInfo
     */
    private synchronized boolean updateUser(UserContainer userInfo) {
        Set<UserContainer> singleton = new HashSet<UserContainer>();
        singleton.add(userInfo);
        return this.updateUsers(singleton);
    }

    /**
     * OK
     * 
     * @param userInfos
     */
    private synchronized boolean updateUsers(Set<UserContainer> userInfos) {
        boolean isListModified = false;

        Set<Long> usersWithNewTypeIds = new HashSet<Long>();
        Set<UserContainer> usersWithNewType = new HashSet<UserContainer>();

        for (UserContainer userInfo : userInfos) {
            User user = this.getUser(userInfo.getId());
            if (user != null) {
                // Check if friendship has changed
                if (user.getFriendship() == userInfo.getFriendship()) {
                    isListModified = user.update(userInfo) || isListModified;
                } else {
                    // Need to remove and add user again to change the instance
                    // type
                    usersWithNewTypeIds.add(userInfo.getId());
                    usersWithNewType.add(userInfo);
                }
            }
        }

        // Remove and add again users with new type
        if (!usersWithNewType.isEmpty()) {
            this.removeUsers(usersWithNewTypeIds);
            this.putUsers(usersWithNewType);
        }

        if (isListModified) {
            for (CacheListener listener : mListeners) {
                listener.onUserListUpdate();
            }
        }

        return isListModified;
    }

    /**
     * Allows to search efficiently through the Cache, by providing a filtering
     * method
     * 
     * @param <T>
     *            Type of items searched
     * @author jfperren
     */
    public interface SearchFilter<T> {
        boolean filter(T item);
    }
}
