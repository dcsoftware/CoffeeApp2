package it.blqlabs.android.coffeeapp2.backend;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import it.blqlabs.android.coffeeapp2.R;
import it.blqlabs.android.coffeeapp2.database.TransactionEntity;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;

/**
 * Created by davide on 20/02/15.
 */
public class HistoryAdapter extends CursorAdapter{

    private ItemClickListener itemClickListener;
    private DateFormat dateFormat = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());

    public HistoryAdapter(Context context, boolean autoRequery, @NonNull ItemClickListener itemClickListener) {
        super(context, null, autoRequery);
        this.itemClickListener = itemClickListener;
    }

    public HistoryAdapter(Context context, boolean autoRequery) {
        super(context, null, autoRequery);
    }

    private static class ViewHolder {
        private final TextView dateTextView;
        private final TextView amountTextView;

        public ViewHolder(View view) {
            dateTextView = (TextView) view.findViewById(R.id.dateTextView);
            amountTextView = (TextView) view.findViewById(R.id.amountTextView);
        }
    }


    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.history_item, parent, false);
        view.setTag(new ViewHolder(view));
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TransactionEntity transaction = cupboard().withCursor(cursor).get(TransactionEntity.class);
        // fetch the author entity too
        ViewHolder holder = (ViewHolder) view.getTag();

        holder.dateTextView.setText(transaction.timestamp);
        holder.amountTextView.setText(transaction.amount);
    }

    public interface ItemClickListener {

    }

}
