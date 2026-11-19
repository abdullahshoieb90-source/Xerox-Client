package com.origin.launcher.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.origin.launcher.Adapter.InbuiltModsAdapter;
import com.origin.launcher.Launcher.inbuilt.manager.InbuiltModManager;
import com.origin.launcher.Launcher.inbuilt.manager.InbuiltModSizeStore;
import com.origin.launcher.Launcher.inbuilt.model.InbuiltMod;
import com.origin.launcher.R;
import com.origin.launcher.animation.DynamicAnim;
import com.origin.launcher.bedrock.BedrockHotbarPack;
import com.origin.launcher.bedrock.BedrockPackImporter;

import java.util.Comparator;
import java.util.List;

public class InbuiltModsActivity extends BaseThemedActivity {
    private static final int REQ_CUSTOMIZE_INBUILT = 1001;
    private static final String PENDING_HOTBAR_EXPORT = "pending_hotbar_export";

    private RecyclerView recyclerView;
    private InbuiltModsAdapter adapter;
    private InbuiltModManager modManager;
    private TextView emptyText;
    private TextView hotbarSelection;
    private MaterialButton hotbarImportButton;
    private BedrockPackImporter importer;
    private BedrockHotbarPack pendingSave;
    private boolean preparingPack;
    private final ActivityResultLauncher<String> saveHotbarFile = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/octet-stream"), this::saveHotbar);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbuilt_mods);

        View root = findViewById(android.R.id.content);
        if (root != null) DynamicAnim.applyPressScaleRecursively(root);

        modManager = InbuiltModManager.getInstance(this);
        InbuiltModSizeStore.getInstance().init(getApplicationContext());
        importer = new BedrockPackImporter(this, this);
        if (savedInstanceState != null && savedInstanceState.containsKey(PENDING_HOTBAR_EXPORT)) {
            pendingSave = new BedrockHotbarPack(savedInstanceState.getInt(PENDING_HOTBAR_EXPORT));
        }
        setupViews();
        loadMods();
    }

    private void setupViews() {
        ImageButton closeButton = findViewById(R.id.close_inbuilt_button);
        closeButton.setOnClickListener(v -> finish());
        DynamicAnim.applyPressScale(closeButton);
        ImageButton customiseButton = findViewById(R.id.customise_inbuilt_mod_button);
        customiseButton.setContentDescription(getString(R.string.bedrock_touch_preview));
        customiseButton.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(R.string.bedrock_touch_preview)
                .setMessage(R.string.bedrock_touch_preview_note)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> startActivityForResult(
                        new Intent(this, InbuiltModsCustomizeActivity.class), REQ_CUSTOMIZE_INBUILT))
                .show());
        DynamicAnim.applyPressScale(customiseButton);

        hotbarSelection = findViewById(R.id.inbuilt_hotbar_selection);
        hotbarImportButton = findViewById(R.id.inbuilt_hotbar_import);
        hotbarImportButton.setOnClickListener(v -> showHotbarImport());

        recyclerView = findViewById(R.id.inbuilt_mods_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        emptyText = findViewById(R.id.empty_inbuilt_text);
        adapter = new InbuiltModsAdapter(modManager);
        adapter.setOnToggleClickListener((mod, enable) -> {
            if (enable) modManager.addMod(mod.getId());
            else modManager.removeMod(mod.getId());
            if (!BedrockHotbarPack.supports(mod.getId())) {
                Toast.makeText(this, getString(enable ? R.string.bedrock_touch_preview_added
                        : R.string.bedrock_touch_preview_removed, mod.getName()), Toast.LENGTH_SHORT).show();
            }
            loadMods();
        });
        recyclerView.setAdapter(adapter);
    }

    private BedrockHotbarPack selectedPack() {
        return BedrockHotbarPack.fromSelectedMods(modManager.getAddedModIds());
    }

    private void showHotbarImport() {
        BedrockHotbarPack pack = selectedPack();
        if (!pack.hasSlots() || preparingPack || pendingSave != null) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.bedrock_hotbar_title)
                .setMessage(getString(R.string.bedrock_hotbar_instructions, pack.getSlotsLabel()))
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.bedrock_hotbar_save, (dialog, which) -> chooseSaveLocation(pack))
                .setPositiveButton(R.string.bedrock_import, (dialog, which) -> importHotbar(pack))
                .show();
    }

    private void importHotbar(BedrockHotbarPack pack) {
        setPreparing(true);
        importer.openHotbar(pack, result -> {
            setPreparing(false);
            int message = result == 0 ? R.string.bedrock_module_opened : result;
            if (result == R.string.bedrock_handoff_failed) message = R.string.bedrock_hotbar_handoff_failed;
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }

    private void chooseSaveLocation(BedrockHotbarPack pack) {
        pendingSave = pack;
        updateHotbarControls();
        try {
            saveHotbarFile.launch(pack.getFileName());
        } catch (ActivityNotFoundException ignored) {
            pendingSave = null;
            updateHotbarControls();
            Toast.makeText(this, R.string.bedrock_hotbar_save_failed, Toast.LENGTH_LONG).show();
        }
    }

    private void saveHotbar(Uri destination) {
        BedrockHotbarPack pack = pendingSave;
        pendingSave = null;
        if (destination == null || pack == null || !pack.hasSlots()) {
            updateHotbarControls();
            return;
        }
        setPreparing(true);
        importer.saveHotbar(pack, destination, result -> {
            setPreparing(false);
            Toast.makeText(this, result == 0 ? R.string.bedrock_hotbar_saved : result,
                    Toast.LENGTH_LONG).show();
        });
    }

    private void setPreparing(boolean preparing) {
        preparingPack = preparing;
        updateHotbarControls();
    }

    private void updateHotbarControls() {
        BedrockHotbarPack pack = selectedPack();
        hotbarSelection.setText(pack.hasSlots()
                ? getString(R.string.bedrock_hotbar_selection, pack.getSlotsLabel())
                : getString(R.string.bedrock_hotbar_empty));
        hotbarImportButton.setText(preparingPack ? R.string.bedrock_import_preparing : R.string.bedrock_hotbar_import);
        hotbarImportButton.setEnabled(pack.hasSlots() && !preparingPack && pendingSave == null);
    }

    private void loadMods() {
        List<InbuiltMod> mods = modManager.getAllMods(this);
        // Show the selectable slots before the preview-only entries.
        mods.sort(Comparator.comparing(mod -> !BedrockHotbarPack.supports(mod.getId())));
        adapter.updateMods(mods);
        emptyText.setVisibility(mods.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(mods.isEmpty() ? View.GONE : View.VISIBLE);
        recyclerView.post(() -> DynamicAnim.staggerRecyclerChildren(recyclerView));
        updateHotbarControls();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (pendingSave != null) outState.putInt(PENDING_HOTBAR_EXPORT, pendingSave.getSelectionMask());
    }

    @Override
    protected void onDestroy() {
        importer.close();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_CUSTOMIZE_INBUILT || resultCode != RESULT_OK || data == null) return;

        InbuiltModSizeStore sizeStore = InbuiltModSizeStore.getInstance();
        for (InbuiltMod mod : modManager.getAllMods(this)) {
            String id = mod.getId();
            int opacity = data.getIntExtra("opacity_" + id, -1);
            if (opacity > 0) modManager.setOverlayButtonOpacity(id, opacity);

            float x = data.getFloatExtra("posx_" + id, -1f);
            float y = data.getFloatExtra("posy_" + id, -1f);
            if (x >= 0f && y >= 0f) {
                sizeStore.setPositionX(id, x);
                sizeStore.setPositionY(id, y);
            }
        }
    }
}
