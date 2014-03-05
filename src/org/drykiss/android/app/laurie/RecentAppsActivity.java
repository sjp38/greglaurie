
package org.drykiss.android.app.laurie;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.drykiss.android.app.laurie.ad.AdvertisementManager;

import java.util.ArrayList;
import java.util.List;

public class RecentAppsActivity extends Activity {
    private static final String TAG = "Laurie_recnetAppsActivity";

    private BaseAdapter mAdapter;
    private View mAdView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recent_apps);

        mAdapter = new RecentAppsAdapter(getApplicationContext());

        final ListView listView = (ListView) findViewById(R.id.recentAppsListView);
        listView.setAdapter(mAdapter);

        mAdView = AdvertisementManager.getAdvertisementView(this);
        LinearLayout adLayout = (LinearLayout) findViewById(R.id.advertiseLayout);
        adLayout.addView(mAdView);
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdapter.notifyDataSetChanged();
    }

    private class RecentAppsAdapter extends BaseAdapter {
        private static final int MAX_RECENT_TASKS = 20;

        private final int mIconWidth = (int) getResources().getDimension(
                R.dimen.recent_app_icon_width);
        private final int mIconHeight = (int) getResources().getDimension(
                R.dimen.recent_app_icon_height);

        private Context mContext;
        private ArrayList<RecentTag> mRecentTags = new ArrayList<RecentTag>();

        public RecentAppsAdapter(Context context) {
            super();

            mContext = context;
            loadRecentApps();
        }

        @Override
        public void notifyDataSetChanged() {
            loadRecentApps();
            super.notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mRecentTags.size();
        }

        @Override
        public Object getItem(int position) {
            return mRecentTags.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            RecentViewHolder holder = null;
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.recent_app_list, null);
                TextView tv = (TextView) convertView.findViewById(R.id.recentAppTextView);
                TextView killButton = (TextView) convertView
                        .findViewById(R.id.killRecentAppTextView);
                ImageView icon = (ImageView) convertView.findViewById(R.id.iconImageView);
                holder = new RecentViewHolder();
                holder.textView = tv;
                holder.icon = icon;
                holder.id = position;
                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        RecentViewHolder holder = (RecentViewHolder) v.getTag();
                        RecentTag tag = mRecentTags.get(holder.id);
                        if (tag.intent != null) {
                            tag.intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY
                                    | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
                            boolean hasProblem = false;
                            try {
                                mContext.startActivity(tag.intent);
                            } catch (Exception e) {
                                hasProblem = true;
                                Toast.makeText(RecentAppsActivity.this,
                                        R.string.can_not_start_recent_app, Toast.LENGTH_SHORT)
                                        .show();
                                Log.w(TAG, "Unable to launch recent task", e);
                            } finally {
                                if (!hasProblem) {
                                    finish();
                                }
                            }
                        }

                    }
                });

                killButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        View convertView = (View) v.getParent();
                        RecentViewHolder holder = (RecentViewHolder) convertView.getTag();
                        RecentTag tag = mRecentTags.get(holder.id);
                        if (tag.intent != null) {
                            final PackageManager pm = getApplicationContext().getPackageManager();
                            final ActivityManager am = (ActivityManager) getSystemService(
                                    Context.ACTIVITY_SERVICE);
                            am.killBackgroundProcesses(tag.intent.resolveActivity(pm)
                                    .getPackageName());
                            notifyDataSetChanged();
                        } else {
                            Toast.makeText(RecentAppsActivity.this, R.string.can_not_kill_app,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                convertView.setTag(holder);
            } else {
                holder = (RecentViewHolder) convertView.getTag();
            }

            RecentTag tag = (RecentTag) getItem(position);
            holder.textView.setText(tag.title);
            holder.icon.setImageDrawable(tag.icon);
            holder.id = position;

            return convertView;
        }

        private void loadRecentApps() {
            final ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            final PackageManager pm = (PackageManager) mContext.getPackageManager();

            final List<ActivityManager.RecentTaskInfo> recentTasks = am.getRecentTasks(
                    MAX_RECENT_TASKS, ActivityManager.RECENT_IGNORE_UNAVAILABLE);

            final List<RunningAppProcessInfo> procs = am.getRunningAppProcesses();

            ActivityInfo homeInfo = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME).resolveActivityInfo(pm, 0);
            mRecentTags.clear();
            final int numTasks = recentTasks.size();
            for (int i = 0; i < numTasks; i++) {
                ActivityManager.RecentTaskInfo info = recentTasks.get(i);
                if (info.id == -1) {
                    Toast.makeText(RecentAppsActivity.this, "dead process " + info.toString(),
                            Toast.LENGTH_SHORT).show();
                }
                boolean found = false;
                for (int j = 0; j < procs.size(); j++) {
                    final String[] pkgs = procs.get(j).pkgList;
                    for (int k = 0; k < pkgs.length; k++) {
                        if (info.baseIntent.resolveActivity(pm).getPackageName().equals(pkgs[k])) {
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        break;
                    }
                }
                if (!found) {
                    continue;
                }

                Intent intent = new Intent(info.baseIntent);
                if (info.origActivity != null) {
                    intent.setComponent(info.origActivity);
                }

                if (homeInfo != null) {
                    if (homeInfo.packageName.equals(intent.getComponent().getPackageName())
                            && homeInfo.name.equals(intent.getComponent().getClassName())) {
                        continue;
                    }
                }

                intent.setFlags((intent.getFlags() & ~Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                        | Intent.FLAG_ACTIVITY_NEW_TASK);
                final ResolveInfo resolveInfo = pm.resolveActivity(intent, 0);
                if (resolveInfo != null) {
                    final ActivityInfo activityInfo = resolveInfo.activityInfo;
                    final String title = activityInfo.loadLabel(pm).toString();
                    Drawable icon = activityInfo.loadIcon(pm);
                    Bitmap bitmap = ((BitmapDrawable) icon).getBitmap();
                    icon = new BitmapDrawable(Bitmap.createScaledBitmap(bitmap, mIconWidth,
                            mIconHeight, true));

                    if (title != null && title.length() > 0 && icon != null) {
                        mRecentTags.add(new RecentTag(title, icon, info, intent));
                    }
                }
            }
        }

        private class RecentViewHolder {
            TextView textView;
            ImageView icon;
            int id;
        }

        private class RecentTag {
            String title;
            Drawable icon;

            ActivityManager.RecentTaskInfo info;
            Intent intent;

            public RecentTag() {
                super();
            }

            public RecentTag(String title, Drawable icon, RecentTaskInfo info, Intent intent) {
                super();
                this.title = title;
                this.icon = icon;
                this.info = info;
                this.intent = intent;
            }
        }
    }
}
