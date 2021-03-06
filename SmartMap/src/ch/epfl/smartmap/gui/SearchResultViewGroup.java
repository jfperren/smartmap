package ch.epfl.smartmap.gui;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import ch.epfl.smartmap.R;
import ch.epfl.smartmap.cache.Displayable;

/**
 * Layout that contains different SearchResultViews that can be dynamically
 * added/removed.
 * 
 * @author jfperren
 */
public class SearchResultViewGroup extends LinearLayout {

    // Margins & Paddings
    private static final int NO_RESULT_VIEW_VERTICAL_PADDING = 150;
    private static final int SEPARATOR_LEFT_PADDING = 10;
    private static final int SEPARATOR_RIGHT_PADDING = 10;

    // Colors
    private static final int SEPARATOR_BACKGROUND_COLOR = R.color.bottomSliderBackground;

    // Text Sizes
    private static final float NO_RESULT_VIEW_TEXT_SIZE = 25f;

    // Others
    private static final int ITEMS_PER_PAGE = 10;

    // Children Views
    private final Button mMoreResultsButton;
    private final TextView mEmptyListTextView;

    // Informations about current state
    private int mCurrentItemNb;
    private List<Displayable> mCurrentResultList;
    private VisualState mCurrentVisualState;

    /**
     * Constructor with empty list
     * 
     * @param context
     *            Context this View lives in
     */
    public SearchResultViewGroup(Context context) {
        this(context, new ArrayList<Displayable>());
    }

    /**
     * Constructor
     * 
     * @param context
     *            Context this View lives in
     * @param results
     *            List of results that will be displayed
     */
    public SearchResultViewGroup(Context context, List<Displayable> results) {
        super(context);

        // Layout parameters
        this.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT));
        this.setOrientation(VERTICAL);
        this.setBackgroundResource(R.drawable.div_background);

        // Create button
        mMoreResultsButton = new MoreResultsButton(context, this);
        // Create TextView that needs to be displayed when no result is found
        mEmptyListTextView = new TextView(context);
        mEmptyListTextView.setText("");
        mEmptyListTextView.setTextColor(this.getResources().getColor(R.color.main_blue));
        mEmptyListTextView.setTextSize(NO_RESULT_VIEW_TEXT_SIZE);
        mEmptyListTextView.setPadding(0, NO_RESULT_VIEW_VERTICAL_PADDING, 0, NO_RESULT_VIEW_VERTICAL_PADDING);
        mEmptyListTextView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT));
        mEmptyListTextView.setTextAlignment(TEXT_ALIGNMENT_CENTER);

        // Create all SearchresultViews
        this.setResultList(results);
    }

    /**
     * Display the searchResultViewGroup in MINIMIZED mode
     */
    public void displayMinimized() {
        mCurrentItemNb = 0;
        this.removeAllViews();
        if (mCurrentResultList.isEmpty()) {
            this.setBackgroundResource(0);
            this.addView(mEmptyListTextView);
            mCurrentVisualState = VisualState.EMPTY;
        } else {
            this.setBackgroundResource(R.drawable.div_background);
            this.addMoreViews();
        }
    }

    /**
     * @return True if there is no result to display
     */
    public boolean isEmpty() {
        return mCurrentResultList.isEmpty();
    }

    /**
     * Sets a new list of results
     * 
     * @param newResultList
     *            New list of results
     */
    public void setResultList(List<Displayable> newResultList) {
        mCurrentResultList = new ArrayList<Displayable>(newResultList);
        this.displayMinimized();
    }

    /**
     * If possible, add {@code ITEMS_PER_PAGE} more {@code SearchResultView}s
     */
    private void addMoreViews() {
        this.removeView(mMoreResultsButton);
        // Computes the number of items to add
        int newItemsNb = Math.min(ITEMS_PER_PAGE, mCurrentResultList.size() - mCurrentItemNb);
        // Add SearchResultViews
        for (int i = mCurrentItemNb; i < (mCurrentItemNb + newItemsNb); i++) {
            this.addView(new SearchResultView(this.getContext(), mCurrentResultList.get(i)));
            this.addView(new Divider(this.getContext()));
        }

        mCurrentItemNb += newItemsNb;
        // Sets the new visual state
        if (mCurrentItemNb == mCurrentResultList.size()) {
            mCurrentVisualState = VisualState.MAX;
        } else {
            this.addView(mMoreResultsButton);
            mCurrentVisualState = VisualState.EXPANDED;
        }
    }

    /**
     * Extend the searchResultViewGroup if there are more results to show
     */
    private void showMoreResults() {
        if (mCurrentVisualState == VisualState.EMPTY) {
            assert false : "Cannot expand an empty SearchResultViewGroup";
        } else if (mCurrentVisualState == VisualState.MAX) {
            assert false : "Cannot expand a SearchResultViewGroup that is already fully expanded";
        } else {
            this.addMoreViews();
        }
    }

    /**
     * Horizontal bar separating two different search results.
     * 
     * @author jfperren
     */
    private static class Divider extends LinearLayout {
        public Divider(Context context) {
            super(context);
            this.setPadding(SEPARATOR_LEFT_PADDING, 0, SEPARATOR_RIGHT_PADDING, 0);
            this.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 1));
            this.setBackgroundResource(SEPARATOR_BACKGROUND_COLOR);
        }
    }

    /**
     * Button showing more Search results when clicked
     * 
     * @author jfperren
     */
    private static class MoreResultsButton extends Button {
        public MoreResultsButton(Context context, final SearchResultViewGroup searchResultViewGroup) {
            super(context);
            this.setText(R.string.more_results);
            this.setBackgroundResource(0);
            this.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    searchResultViewGroup.showMoreResults();
                }
            });
        }
    }

    /**
     * Visual state of a ViewGroup
     * 
     * @author jfperren
     */
    private enum VisualState {
        MINIMIZED,
        EXPANDED,
        MAX,
        EMPTY;
    }
}