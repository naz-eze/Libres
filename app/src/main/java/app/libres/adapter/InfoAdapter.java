package app.libres.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import app.libres.R;
import app.libres.model.InfoModel;

public class InfoAdapter extends ArrayAdapter<InfoModel> {
    public InfoAdapter(@NonNull Context context) {
        super(context, R.layout.item_card);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.item_card, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        final InfoModel model = getItem(position);
        holder.headerTextView.setText(model.getInfoHeader());
        holder.meaningTextView.setText(model.getInfoMeaning());
        holder.sourceTextView.setText(model.getInfoSource());
        holder.websiteTextView.setText(model.getInfoUrl());
        holder.cardContainerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(model.getInfoUrl()));
                v.getContext().startActivity(browserIntent);
            }
        });
        return convertView;
    }

    static class ViewHolder {
        TextView headerTextView;
        TextView meaningTextView;
        TextView sourceTextView;
        TextView websiteTextView;
        CardView cardContainerView;

        ViewHolder(View view) {
            cardContainerView = view.findViewById(R.id.card_container);
            headerTextView = view.findViewById(R.id.info_header);
            meaningTextView = view.findViewById(R.id.info_meaning);
            sourceTextView = view.findViewById(R.id.info_source);
            websiteTextView = view.findViewById(R.id.info_website);
        }
    }
}
