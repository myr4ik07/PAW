package com.blogspot.copyraite.PAW.orders;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.blogspot.copyraite.PAW.R;

import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {

    private List<Item> items;

    public MyAdapter(List<Item> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Item item = items.get(position);
        holder.textViewBold.setText(item.getBoldText());
        holder.textViewNormal.setText(item.getNormalText());

        // Обробник подій для TextView
        holder.textViewBold.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent CollectionOrdersDocPage = new Intent(v.getContext(), CollectionOrdersDoc.class);
                CollectionOrdersDocPage.putExtra("REF_ID", item.getRef_id()); // Передаємо ref_id
                v.getContext().startActivity(CollectionOrdersDocPage);
            }
        });

        // Обробник подій для Склада
//        holder.textViewNormal.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(v.getContext(), SecondActivity.class);
//                v.getContext().startActivity(intent);
//            }
//        });

    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void clearData() {
        items.clear();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView textViewBold;
        TextView textViewNormal;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewBold = itemView.findViewById(R.id.textViewBold);
            textViewNormal = itemView.findViewById(R.id.textViewNormal);
        }
    }

    public static class Item {
        private String boldText; // замовлення
        private String normalText; // склад
        private String ref_id;

        public Item(String boldText, String normalText, String ref_id) {
            this.boldText = boldText;
            this.normalText = normalText;
            this.ref_id = ref_id;
        }

        public Item(String boldText, String normalText) {
            this.boldText = boldText;
            this.normalText = normalText;
        }

        public String getRef_id() {
            return ref_id;
        }

        public String getBoldText() {
            return boldText;
        }

        public String getNormalText() {
            return normalText;
        }
    }

}