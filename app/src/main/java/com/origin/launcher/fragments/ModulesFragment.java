package com.origin.launcher.fragments;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.origin.launcher.R;
import com.origin.launcher.activity.InbuiltModsActivity;
import com.origin.launcher.bedrock.BedrockPackImporter;
import com.origin.launcher.bedrock.BedrockResourceModule;
import com.origin.launcher.manager.ThemeManager;
import com.origin.launcher.utils.ThemeUtils;

/** The original Modules screen: import supported visuals, and show legacy modules as unavailable. */
public class ModulesFragment extends BaseThemedFragment {
    private static final String TOUCH_MODULES = "inbuilt_mods_entry";
    private static final String[][] MODULES = {
        {TOUCH_MODULES, "Touch Modules", "Browse AutoSprint, Quick Drop, hotbar buttons and more."},
        {"Nohurtcam", "No hurt cam", "Removes camera shake on damage."},
        {"Nofog", "No Fog", "Removes in-game fog."},
        {"better_brightness", "Better Brightness", "Improves visibility in the dark."},
        {"particles_disabler", "Particles Disabler", "Disables particles."},
        {"java_clouds", "Java Fancy Clouds", "Changes clouds to Java-style clouds."},
        {"java_cubemap", "Java Cubemap", "Changes the sky cubemap."},
        {"classic_skins", "Classic Vanilla skins", "Uses the classic vanilla skins."},
        {"no_flipbook_animations", "No flipbook animation", "Disables animated block textures."},
        {"no_shadows", "No Shadows", "Disables shadows."},
        {"xelo_title", "Xelo Title", "Replaces the classic title image."},
        {"double_tppview", "2x tpp view", "Increases third-person camera distance."},
        {"white_block_outline", "White Block Outline", "Changes the selection outline to white."},
        {"no_pumpkin_overlay", "No pumpkin overlay", "Clears the carved-pumpkin screen overlay."},
        {"no_spyglass_overlay", "No spyglass overlay", "Clears the spyglass frame, not the zoom."},
        {"no_eating_animation", "No Eating animation", "Disables the eating animation."},
        {"no_bow_animation", "No Bow animation", "Disables the bow animation."},
        {"portal_optimizer", "Portal optimizer", "Tweaks portal rendering."},
        {"psm", "Particle single mapping", "Tweaks the particle material."},
        {"custom_cross_hair", "Custom CrossHair", "Changes the crosshair image."}
    };

    private BedrockPackImporter importer;
    private boolean importing;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_modules, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        importer = new BedrockPackImporter(requireActivity(), getViewLifecycleOwner());
        view.findViewById(R.id.back_button).setOnClickListener(v ->
                getParentFragmentManager().popBackStack());
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    protected void onApplyTheme() {
        View root = getView();
        if (root == null) return;
        ThemeManager theme = ThemeManager.getInstance();
        root.setBackgroundColor(theme.getColor("background"));
        ImageView back = root.findViewById(R.id.back_button);
        back.setColorFilter(theme.getColor("onBackground"));
        LinearLayout cards = root.findViewById(R.id.modulesContainer);
        cards.removeAllViews();
        for (String[] item : MODULES) cards.addView(createModuleCard(item));
    }

    private View createModuleCard(String[] item) {
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(12);
        card.setLayoutParams(cardParams);
        card.setRadius(dp(12));
        card.setCardElevation(0);
        card.setStrokeWidth(dp(1));
        ThemeUtils.applyThemeToCard(card, requireContext());

        LinearLayout row = new LinearLayout(requireContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.addView(row);

        LinearLayout labels = new LinearLayout(requireContext());
        labels.setOrientation(LinearLayout.VERTICAL);
        row.addView(labels, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        ThemeManager theme = ThemeManager.getInstance();

        TextView title = new TextView(requireContext());
        title.setText(TOUCH_MODULES.equals(item[0]) ? getString(R.string.bedrock_touch_title) : item[1]);
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(theme.getColor("onSurface"));
        labels.addView(title);

        TextView description = new TextView(requireContext());
        description.setText(TOUCH_MODULES.equals(item[0]) ? getString(R.string.bedrock_touch_status) : item[2]);
        description.setTextSize(14);
        description.setPadding(0, dp(4), 0, 0);
        description.setTextColor(theme.getColor("onSurfaceVariant"));
        labels.addView(description);

        if (TOUCH_MODULES.equals(item[0])) {
            ImageView more = new ImageView(requireContext());
            more.setImageResource(android.R.drawable.ic_menu_more);
            more.setColorFilter(theme.getColor("onSurfaceVariant"));
            row.addView(more, dp(24), dp(24));
            card.setFocusable(true);
            card.setOnClickListener(v -> startActivity(new Intent(requireContext(), InbuiltModsActivity.class)));
            return card;
        }

        BedrockResourceModule module = BedrockResourceModule.forLegacyKey(item[0]);
        TextView status = new TextView(requireContext());
        status.setTextSize(12);
        status.setPadding(0, dp(8), 0, 0);
        status.setTextColor(theme.getColor(module == null ? "onSurfaceVariant" : "primary"));
        status.setText(module == null ? R.string.bedrock_legacy_unavailable : R.string.bedrock_module_pack);
        labels.addView(status);
        if (module == null) return card;

        MaterialButton button = new MaterialButton(requireContext());
        button.setText(R.string.bedrock_import);
        button.setAllCaps(false);
        button.setContentDescription(getString(R.string.bedrock_import_module, item[1]));
        button.setEnabled(!importing);
        button.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle(item[1])
                .setMessage(R.string.bedrock_module_instructions)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.bedrock_import, (dialog, which) -> importModule(module))
                .show());
        ThemeUtils.applyThemeToButton(button, requireContext());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonParams.setMarginStart(dp(8));
        row.addView(button, buttonParams);
        return card;
    }

    private void importModule(BedrockResourceModule module) {
        if (importer == null || importing) return;
        importing = true;
        onApplyTheme();
        importer.openModule(module, result -> {
            importing = false;
            onApplyTheme();
            Toast.makeText(requireContext(), result == 0 ? R.string.bedrock_module_opened : result,
                    Toast.LENGTH_LONG).show();
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        if (importer != null) importer.close();
        importer = null;
        importing = false;
        super.onDestroyView();
    }
}
