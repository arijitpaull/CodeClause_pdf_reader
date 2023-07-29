package com.example.pdfreadercodeclause;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class Adapter extends RecyclerView.ViewHolder implements View.OnClickListener {

    public ItemClickListener listener;
    private final Context context;
    public TextView pdfTitle;
    public Adapter(@NonNull View itemView) {
        super(itemView);
        context = itemView.getContext();
        pdfTitle = itemView.findViewById(R.id.pdf_name);
    }

    @Override
    public void onClick(View view) {
        listener.onClick(view, getAdapterPosition(),false);

    }
}
