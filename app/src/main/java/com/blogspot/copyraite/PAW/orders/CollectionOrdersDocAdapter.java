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

public class CollectionOrdersDocAdapter extends RecyclerView.Adapter<CollectionOrdersDocAdapter.MyViewHolder> {

    private List<Item> items;

    public CollectionOrdersDocAdapter(List<Item> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list_doc, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Item item = items.get(position);
        holder.textViewProduct.setText(item.getProduct());
        holder.textViewQuantity.setText(item.getQuantity());

        // Обробник подій для рядка (натиснути по рядку)
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), CollectionOrdersPhoto.class);
                intent.putExtra("product_id", item.getProduct_id());
                v.getContext().startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView textViewProduct;
        TextView textViewQuantity;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewProduct = itemView.findViewById(R.id.textViewProduct);
            textViewQuantity = itemView.findViewById(R.id.textViewQuantity);
        }
    }

    public static class Item {
        private String product;
        private String quantity;
        private String product_id;

        public Item(String product, String quantity) {
            this.product = product;
            this.product_id = product_id;
            this.quantity = quantity;
        }

        public Item(String product, String quantity, String product_id) {
            this.product = product;
            this.product_id = product_id;
            this.quantity = quantity;
        }

        public String getProduct_id() {
            return product_id;
        }

        public String getProduct() {
            return product;
        }

        public String getQuantity() {
            return quantity;
        }
    }
}