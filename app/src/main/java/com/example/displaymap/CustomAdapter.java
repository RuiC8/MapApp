package com.example.displaymap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;

import static java.util.Objects.requireNonNull;

public class CustomAdapter extends BaseAdapter{
    private Context context;
    private static ArrayList<Model> modelArrayList;
    CustomAdapter(Context context, ArrayList<Model> modelArrayList) {
        this.context = context;
        CustomAdapter.modelArrayList = modelArrayList;
    }
    @Override
    public int getViewTypeCount() {
        return getCount();
    }
    @Override
    public int getItemViewType(int position) {
        return position;
    }
    @Override
    public int getCount() {
        return modelArrayList.size();
    }
    @Override
    public Object getItem(int position) {
        return modelArrayList.get(position);
    }
    @Override
    public long getItemId(int position) {
        return 0;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            LayoutInflater inflater = (LayoutInflater)
                    context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = requireNonNull(inflater).inflate(R.layout.listitem, null, true);
            holder.checkBox = convertView.findViewById(R.id.checkBox);
            holder.tvPlayer = convertView.findViewById(R.id.symptomNameList);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        //holder.checkBox.setText("Checkbox " + position);
        holder.tvPlayer.setText(modelArrayList.get(position).getItem());
        holder.checkBox.setChecked(modelArrayList.get(position).getSelected());
        holder.checkBox.setTag(R.integer.btnPlusView, convertView);
        holder.checkBox.setTag(position);
        holder.checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Integer pos = (Integer) holder.checkBox.getTag();
                //Toast.makeText(context, "Checkbox " +pos+ " Selected!",
                //        Toast.LENGTH_SHORT).show();
                if (modelArrayList.get(pos).getSelected()) {
                    modelArrayList.get(pos).setSelected(false);
                } else {
                    modelArrayList.get(pos).setSelected(true);
                }
            }
        });
        return convertView;
    }
    private class ViewHolder {
        CheckBox checkBox;
        private TextView tvPlayer;
    }
}
