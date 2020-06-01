package com.vikas.myst.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.vikas.myst.R;

public class CustomLoading extends LinearLayout {
    public CustomLoading(Context context) {
        super(context);
    }

    public CustomLoading(final Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater inflater=(LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v= inflater.inflate(R.layout.custom_loading,this);
        if(v.getVisibility()==VISIBLE) {
            final TextView msg = v.findViewById(R.id.msg);
            v.postDelayed(new Runnable() {
                @Override
                public void run() {
                    msg.setVisibility(VISIBLE);
                }
            }, 5000);

        }
    }
}
