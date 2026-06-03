package com.zegoggles.smssync.activity.donation;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import android.text.TextUtils;

import com.zegoggles.smssync.R;
import com.zegoggles.smssync.activity.Dialogs;

import java.util.ArrayList;
import java.util.List;

import static android.R.string.cancel;

/**
 * Fragment that displays the list of donation products.
 * Reports the user's selection by index to {@link ProductSelectionListener};
 * the activity resolves the live {@link com.android.billingclient.api.ProductDetails}
 * by that index from its in-memory list (U-028: eliminates SkuDetails JSON re-hydration).
 */
public class DonationListFragment extends Dialogs.BaseFragment {
    static final String SKUS = "skus";
    private ProductSelectionListener listener;

    interface ProductSelectionListener {
        void selectedProduct(int index);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ProductSelectionListener) {
            listener = (ProductSelectionListener) context;
        } else {
            throw new IllegalArgumentException("Context does not implement ProductSelectionListener");
        }
    }

    @SuppressWarnings("deprecation")
    @Override @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final ArrayList<Sku> skus = getArguments().getParcelableArrayList(SKUS);

        return new AlertDialog.Builder(getContext())
            .setTitle(R.string.ui_dialog_donate_message)
            .setItems(getOptions(skus), new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    listener.selectedProduct(which);
                }
            })
            .setNegativeButton(cancel, new OnClickListener() {
                @Override public void onClick(DialogInterface dialogInterface, int which) {
                    onCancel(dialogInterface);
                }
            })
            .create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        getActivity().finish();
    }

    private CharSequence[] getOptions(List<Sku> skus) {
        List<String> options = new ArrayList<String>();
        for (final Sku sku : skus) {
            String item = sku.getTitle();
            if (!TextUtils.isEmpty(sku.getPrice())) {
                item += "  " + sku.getPrice();
            }
            options.add(item);
        }
        String[] items = new String[options.size()];
        options.toArray(items);
        return items;
    }
}
