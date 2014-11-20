package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.stats.model.AuthorModel;
import org.wordpress.android.ui.stats.model.CommentsModel;
import org.wordpress.android.ui.stats.model.FollowerModel;
import org.wordpress.android.ui.stats.model.FollowersModel;
import org.wordpress.android.ui.stats.model.SingleItemModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.widgets.TypefaceCache;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class StatsFollowersFragment extends StatsAbstractListFragment implements RadioGroup.OnCheckedChangeListener {
    public static final String TAG = StatsFollowersFragment.class.getSimpleName();

    private RadioGroup mRadioGroup;

    private static final String SELECTED_BUTTON_INDEX = "SELECTED_BUTTON_INDEX";
    private int mSelectedButtonIndex = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        mRadioGroup = (RadioGroup) view.findViewById(R.id.stats_pager_tabs);


        int dp8 = DisplayUtils.dpToPx(view.getContext(), 8);
        int dp80 = DisplayUtils.dpToPx(view.getContext(), 80);

        //String[] titles = getTabTitles();

        String[] titles = {"WordPress.com", "Email"};

        for (int i = 0; i < titles.length; i++) {
            RadioButton rb = (RadioButton) inflater.inflate(R.layout.stats_radio_button, null, false);
            RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(RadioGroup.LayoutParams.WRAP_CONTENT,
                    RadioGroup.LayoutParams.WRAP_CONTENT);
            rb.setTypeface((TypefaceCache.getTypeface(view.getContext())));

            params.setMargins(0, 0, dp8, 0);
            rb.setMinimumWidth(dp80);
            rb.setGravity(Gravity.CENTER);
            rb.setLayoutParams(params);
            rb.setText(titles[i]);
            mRadioGroup.addView(rb);

            if (i == mSelectedButtonIndex)
                rb.setChecked(true);
        }

        mRadioGroup.setVisibility(View.VISIBLE);
        mRadioGroup.setOnCheckedChangeListener(this);

        mTotalsLabel.setVisibility(View.VISIBLE);
        mTotalsLabel.setText("Total comment followers: 0");

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            AppLog.d(AppLog.T.STATS, this.getTag() + " > restoring instance state");
            if (savedInstanceState.containsKey(SELECTED_BUTTON_INDEX)) {
                mSelectedButtonIndex = savedInstanceState.getInt(SELECTED_BUTTON_INDEX);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        AppLog.d(AppLog.T.STATS, this.getTag() + " > saving instance state");
        outState.putInt(SELECTED_BUTTON_INDEX, mSelectedButtonIndex);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        // checkedId will be -1 when the selection is cleared
        if (checkedId == -1)
            return;

        int index  = group.indexOfChild(group.findViewById(checkedId));
        if (index == -1)
            return;

        mSelectedButtonIndex = index;

        View view = this.getView();
        TextView entryLabel = (TextView) view.findViewById(R.id.stats_list_entry_label);
        entryLabel.setText(getEntryLabelResId());

        updateUI();
    }

    @Override
    protected void showLoadingUI() {
        super.showLoadingUI();
        mRadioGroup.setVisibility(View.GONE);
        mTotalsLabel.setVisibility(View.GONE);
    }

    @Override
    protected void updateUI() {
        mRadioGroup.setVisibility(View.VISIBLE);
        mTotalsLabel.setVisibility(View.VISIBLE);

        if (mDatamodel == null) {
            showEmptyUI(true);
            mTotalsLabel.setText(getTotalFollowersLabel(0));
            return;
        }

        FollowersModel followersModel = (FollowersModel) mDatamodel;
        ArrayAdapter adapter = null;
        if (mSelectedButtonIndex == 0) {
            List<FollowerModel> mSubscribers = followersModel.getFollowers();
            if (mSubscribers != null && mSubscribers.size() > 0) {
                adapter = new DotComFollowerAdapter(getActivity(), mSubscribers);
            }
        } else {

        }

        if (adapter != null) {
            StatsUIHelper.reloadLinearLayout(getActivity(), adapter, mList, getMaxNumberOfItemsToShowInList());
            showEmptyUI(false);
            if ( mSelectedButtonIndex == 0 ) {
                mTotalsLabel.setText(getTotalFollowersLabel(followersModel.getTotalWPCom()));
            } else {
                mTotalsLabel.setText(getTotalFollowersLabel(followersModel.getTotalEmail()));
            }
        } else {
            showEmptyUI(true);
            mTotalsLabel.setText(getTotalFollowersLabel(0));
        }
    }

    @Override
    protected boolean isViewAllOptionAvailable() {
        return false;
    }

    private String getTotalFollowersLabel(int total) {
        if ( mSelectedButtonIndex == 0 ) {
            return "Total WordPress.com Followers: " + total;
        }

        return "Total Email Followers: " + total;
    }


    @Override
    protected boolean isExpandableList() {
        return false;
    }

    private class TopPostsAndPagesAdapter extends ArrayAdapter<SingleItemModel> {

        private final List<SingleItemModel> list;
        private final Activity context;
        private final LayoutInflater inflater;

        public TopPostsAndPagesAdapter(Activity context, List<SingleItemModel> list) {
            super(context, R.layout.stats_list_cell, list);
            this.context = context;
            this.list = list;
            inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = convertView;
            // reuse views
            if (rowView == null) {
                rowView = inflater.inflate(R.layout.stats_list_cell, null);
                // configure view holder
                StatsViewHolder viewHolder = new StatsViewHolder(rowView);
                rowView.setTag(viewHolder);
            }

            final SingleItemModel currentRowData = list.get(position);
            StatsViewHolder holder = (StatsViewHolder) rowView.getTag();
            // fill data
            // entries
            holder.setEntryTextOrLink(currentRowData.getUrl(), currentRowData.getTitle());
            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(currentRowData.getTotals()));

            // no icon
            holder.networkImageView.setVisibility(View.GONE);

            return rowView;
        }
    }

    private class DotComFollowerAdapter extends ArrayAdapter<FollowerModel> {

        private final List<FollowerModel> list;
        private final Activity context;
        private final LayoutInflater inflater;

        public DotComFollowerAdapter(Activity context, List<FollowerModel> list) {
            super(context, R.layout.stats_list_cell, list);
            this.context = context;
            this.list = list;
            inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = convertView;
            // reuse views
            if (rowView == null) {
                rowView = inflater.inflate(R.layout.stats_list_cell, null);
                // configure view holder
                StatsViewHolder viewHolder = new StatsViewHolder(rowView);
                rowView.setTag(viewHolder);
            }

            final FollowerModel currentRowData = list.get(position);
            StatsViewHolder holder = (StatsViewHolder) rowView.getTag();

            // entries
            holder.setEntryTextOrLink(currentRowData.getURL(), currentRowData.getLabel());

            // since date

            holder.totalsTextView.setText(getSinceLabel(currentRowData.getDateSubscribed()));

            // Avatar
            holder.showNetworkImage(currentRowData.getAvatar());

            // no icon
            holder.networkImageView.setVisibility(View.VISIBLE);

            return rowView;
        }

        private int roundUp(double num, double divisor) {
            double unrounded = num / divisor;
            return (int) (unrounded + 0.5);
        }

        private String getSinceLabel(String dataSubscribed) {

            Date currentDateTime = new Date(StatsUtils.getCurrentDateTimeMsTZ(getLocalTableBlogID()));

            try {
                SimpleDateFormat from = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = from.parse(dataSubscribed);

                // See http://momentjs.com/docs/#/displaying/fromnow/
                long currentDifference = Math.abs(
                        StatsUtils.getDateDiff(date, currentDateTime, TimeUnit.SECONDS)
                );

                if (currentDifference <= 45 ) {
                    return "seconds ago";
                }
                if (currentDifference < 90 ) {
                    return "a minute ago";
                }

                // 90 seconds to 45 minutes
                if (currentDifference <= 2700 ) {
                    long minutes = this.roundUp(currentDifference, 60);
                    return minutes + " minutes";
                }

                // 45 to 90 minutes
                if (currentDifference <= 5400 ) {
                    return "an hour ago";
                }

                // 90 minutes to 22 hours
                if (currentDifference <= 79200 ) {
                    long hours = this.roundUp(currentDifference, 60*60);
                    return hours + " hours";
                }

                // 22 to 36 hours
                if (currentDifference <= 129600 ) {
                    return "A day";
                }

                // 36 hours to 25 days
                // 86400 secs in a day -  2160000 secs in 25 days
                if (currentDifference <= 2160000 ) {
                    long days = this.roundUp(currentDifference, 86400);
                    return days + " days";
                }

                // 25 to 45 days
                // 3888000 secs in 45 days
                if (currentDifference <= 3888000 ) {
                    return "A month";
                }

                // 45 to 345 days
                // 2678400 secs in a month - 29808000 secs in 345 days
                if (currentDifference <= 29808000 ) {
                    long months = this.roundUp(currentDifference, 2678400);
                    return months + " months";
                }

                // 345 to 547 days (1.5 years)
                if (currentDifference <= 47260800 ) {
                    return  "A year";
                }

                // 548 days+
                // 31536000 secs in a year
                long years = this.roundUp(currentDifference, 31536000);
                return years + " years";

            } catch (ParseException e) {
                AppLog.e(AppLog.T.STATS, e);
            }

            return "";
        }
    }

    @Override
    protected int getEntryLabelResId() {
        return R.string.stats_entry_followers;
    }

    @Override
    protected int getTotalsLabelResId() {
        return R.string.stats_totals_followers;
    }

    @Override
    protected int getEmptyLabelTitleResId() {
        return R.string.stats_empty_followers;
    }

    @Override
    protected int getEmptyLabelDescResId() {
        return R.string.stats_empty_followers_desc;
    }

    @Override
    protected StatsService.StatsEndpointsEnum getSectionToUpdate() {
        return StatsService.StatsEndpointsEnum.FOLLOWERS;
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_followers);
    }
}
