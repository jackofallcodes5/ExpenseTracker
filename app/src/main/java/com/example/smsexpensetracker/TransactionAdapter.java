package com.example.smsexpensetracker;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * TransactionAdapter.java
 * RecyclerView.Adapter that binds a list of Transaction objects
 * to the item_transaction.xml row layout.
 *
 * Each row displays: Date-Time | Amount | Type (color-coded)
 * Clicking a row opens the TransactionDialog.
 */
public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    // -------------------------------------------------------
    // Click listener interface
    // -------------------------------------------------------

    public interface OnItemClickListener {
        void onItemClick(Transaction transaction);
    }

    // -------------------------------------------------------
    // Fields
    // -------------------------------------------------------
    private final Context context;
    private List<Transaction> transactions;
    private final OnItemClickListener clickListener;

    // -------------------------------------------------------
    // Constructor
    // -------------------------------------------------------

    public TransactionAdapter(Context context,
                              List<Transaction> transactions,
                              OnItemClickListener clickListener) {
        this.context      = context;
        this.transactions = transactions;
        this.clickListener = clickListener;
    }

    // -------------------------------------------------------
    // RecyclerView.Adapter overrides
    // -------------------------------------------------------

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction t = transactions.get(position);

        // --- Date / Time ---
        holder.tvDateTime.setText(t.getDatetime());

        // --- Amount ---
        holder.tvAmount.setText(String.format("₹ %.2f", t.getAmount()));

        // --- Type with color coding ---
        String type = t.getType();
        holder.tvType.setText(type);

        if ("Credited".equalsIgnoreCase(type)) {
            holder.tvType.setTextColor(Color.parseColor("#2E7D32")); // Green
            holder.tvAmount.setTextColor(Color.parseColor("#2E7D32"));
        } else if ("Debited".equalsIgnoreCase(type)) {
            holder.tvType.setTextColor(Color.parseColor("#C62828")); // Red
            holder.tvAmount.setTextColor(Color.parseColor("#C62828"));
        } else {
            holder.tvType.setTextColor(Color.parseColor("#546E7A")); // Grey
            holder.tvAmount.setTextColor(Color.parseColor("#546E7A"));
        }

        // --- Row click listener ---
        holder.itemView.setOnClickListener(v -> clickListener.onItemClick(t));

        // Alternate row background for readability
        if (position % 2 == 0) {
            holder.itemView.setBackgroundColor(Color.parseColor("#FFFFFF"));
        } else {
            holder.itemView.setBackgroundColor(Color.parseColor("#F5F5F5"));
        }
    }

    @Override
    public int getItemCount() {
        return transactions == null ? 0 : transactions.size();
    }

    // -------------------------------------------------------
    // Public data update
    // -------------------------------------------------------

    /**
     * Replaces the full dataset and refreshes the RecyclerView.
     * Call this after re-querying the database (e.g., after an import or edit).
     */
    public void updateData(List<Transaction> newTransactions) {
        this.transactions = newTransactions;
        notifyDataSetChanged();
    }

    // -------------------------------------------------------
    // ViewHolder
    // -------------------------------------------------------

    /** Holds references to the views for a single row. */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDateTime;
        TextView tvAmount;
        TextView tvType;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvAmount   = itemView.findViewById(R.id.tvAmount);
            tvType     = itemView.findViewById(R.id.tvType);
        }
    }
}
