package ir.aeliux.usbtoolkit;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;

import ir.aeliux.usbtoolkit.data.GadgetState;
import ir.aeliux.usbtoolkit.databinding.ActivityGadgetDetailsBinding;
import ir.aeliux.usbtoolkit.util.DataConversion;
import ir.aeliux.usbtoolkit.util.Views;
import ir.aeliux.usbtoolkit.widget.MaterialContainer;

public class GadgetDetailsActivity extends BaseActivity {
    private final String TAG = "GadgetDetailsActivity";
    private static final String EXTRA_GADGET = "gadget";
    private ActivityGadgetDetailsBinding binding;

    private GadgetState gadget;

    public static Intent intent(Context ctx, GadgetState state) {
        return new Intent(ctx, GadgetDetailsActivity.class)
                .putExtra(EXTRA_GADGET, state);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityGadgetDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar(binding.toolbar);
        applyWindowInsets(binding.main);

        if (Build.VERSION.SDK_INT >= 33) {
            gadget = getIntent().getParcelableExtra(EXTRA_GADGET, GadgetState.class);
        } else {
            gadget = getIntent().getParcelableExtra(EXTRA_GADGET);
        }
        if (gadget == null) {
            Log.e(TAG, "No GadgetState is supplied");
            finish();
            return;
        }

        refresh();
    }

    private void refresh() {
        binding.toolbar.setSubtitle(gadget.name);

        disableContainer(binding.secUdc);
        disableContainer(binding.secDevice);
        disableContainer(binding.secUsb);

        binding.infoBound.setSubtitle(gadget.bound ? "Bound" : "Not Bound");
        binding.infoBoundUdc.setSubtitle(gadget.bound ? gadget.boundUdc : null);

        binding.infoVendorId.setSubtitle(DataConversion.intToString(gadget.vendorId, 16));
        binding.infoProductId.setSubtitle(DataConversion.intToString(gadget.productId, 16));
        binding.infoManufacturer.setSubtitle(gadget.manufacturer);
        binding.infoProduct.setSubtitle(gadget.product);
        binding.infoSerialNumber.setSubtitle(gadget.serialNumber);

        binding.infoBmAttributes.setSubtitle(gadget.formatBmAttributes());
        binding.infoBcdUsb.setSubtitle(gadget.formatBcdUsb());
        binding.infoBcdDevice.setSubtitle(gadget.formatBcdDevice());
    }

    private void disableContainer(MaterialContainer container) {
        Views.setEnabledRecursively(container.getContentContainer(), false);
        container.getContentContainer().setAlpha(0.75f);
    }
}