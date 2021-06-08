package com.upbad.apps.autonotify;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.upbad.apps.autonotify.model.PackageData;

import java.util.ArrayList;
import java.util.List;

public class AppSearchAdapter extends ArrayAdapter<PackageData> {

    private final List<PackageData> allPackageDataList;
    private final List<PackageData> packageDataList;
    private final Context context;

    private PackageDataFilter packageDataFilter;

    @Override
    public int getCount() {
        return packageDataList.size();
    }

    @Nullable
    @Override
    public PackageData getItem(int position) {
        return packageDataList.get(position);
    }

    @Override
    public int getPosition(@Nullable PackageData item) {
        return packageDataList.indexOf(item);
    }

    public AppSearchAdapter(@NonNull Context context, int textViewResourceId, @NonNull List<PackageData> objects) {
        super(context, R.layout.item_app, textViewResourceId, objects);
        allPackageDataList = new ArrayList<>(objects);
        packageDataList = new ArrayList<>();
        packageDataList.addAll(objects);
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        PackageData packageData = getItem(position);
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            convertView = layoutInflater.inflate(R.layout.item_app, null);
        }
        // No need to set label since it is the TextView assigned to the adapter
        ImageView appIcon = convertView.findViewById(R.id.appIcon);
        appIcon.setImageDrawable(packageData.icon);
        TextView appUserId = convertView.findViewById(R.id.appUserId);
        appUserId.setText(String.valueOf(packageData.userId));
        return super.getView(position, convertView, parent);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        if (packageDataFilter == null) {
            packageDataFilter = new PackageDataFilter();
        }
        return packageDataFilter;
    }

    private class PackageDataFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults filterResults = new FilterResults();
            packageDataList.clear();
            if (constraint != null && constraint.length() > 0) {
                String constraintString = constraint.toString().toLowerCase();
                for (PackageData packageData : allPackageDataList) {
                    if (packageData.label.toLowerCase().contains(constraintString)
                            || packageData.packageName.toLowerCase().contains(constraintString)) {
                        packageDataList.add(packageData);
                    }
                }
            } else {
                packageDataList.addAll(allPackageDataList);
            }
            filterResults.count = packageDataList.size();
            filterResults.values = packageDataList;
            return filterResults;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            clear();
            addAll((List<PackageData>) results.values);
            notifyDataSetChanged();
        }
    }
}
