package com.vikas.myst.custom;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.vikas.myst.R;
import com.vikas.myst.bean.Contact;

import java.util.List;

public class ChatRecyclerViewAdapter extends RecyclerView.Adapter<ChatRecyclerViewAdapter.ViewHolder> {


    private List<Contact> cData;
    private LayoutInflater mInflater;
    private ChatRecyclerViewAdapter.ItemClickListener mClickListener;

    // data is passed into the constructor
    public ChatRecyclerViewAdapter(Context context, List<Contact> contacts) {
        this.mInflater = LayoutInflater.from(context);
        this.cData=contacts;
    }

    // inflates the row layout from xml when needed
    @Override
    public ChatRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.contact_list_unit, parent, false);
        return new ChatRecyclerViewAdapter.ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ChatRecyclerViewAdapter.ViewHolder holder, int position) {
        if(cData.get(position).getNumber().equalsIgnoreCase("7008529481"))
        holder.contactViewNumber.setText("chat with developer");
        else
        holder.contactViewNumber.setText(cData.get(position).getNumber());
        holder.contactViewName.setText(cData.get(position).getName());
        if(cData.get(position).getStatus().equalsIgnoreCase("myst"))
            holder.inviteButton.setVisibility(View.GONE);
        else
            holder.inviteButton.setVisibility(View.VISIBLE);
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return cData.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView contactViewName;
        TextView contactViewNumber;
        Button inviteButton;

        ViewHolder(View itemView) {
            super(itemView);
            contactViewName = itemView.findViewById(R.id.contactNameView);
            contactViewNumber = itemView.findViewById(R.id.contactNumberView);
            inviteButton=itemView.findViewById(R.id.inviteButton);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }

    // convenience method for getting data at click position
    public Contact getItem(int id) {
        return cData.get(id);
    }

    // allows clicks events to be caught
    public void setClickListener(ChatRecyclerViewAdapter.ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}