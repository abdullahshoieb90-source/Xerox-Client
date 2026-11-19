package com.origin.launcher.fragments;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.origin.launcher.BuildConfig;
import com.origin.launcher.Launcher.MinecraftLauncher;
import com.origin.launcher.R;
import com.origin.launcher.manager.ThemeManager;
import com.origin.launcher.utils.ThemeUtils;
import com.origin.launcher.versions.GameVersion;
import com.origin.launcher.versions.VersionManager;

public class HomeFragment extends BaseThemedFragment {
    private TextView listener;
    private MaterialButton launchButton;
    private MaterialButton versionsButton;
    private MaterialButton shareLogsButton;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        listener = view.findViewById(R.id.listener);
        launchButton = view.findViewById(R.id.mbl2_load);
        versionsButton = view.findViewById(R.id.versions_button);
        shareLogsButton = view.findViewById(R.id.share_logs_button);
        ((TextView) view.findViewById(R.id.textview1)).setText(
                getString(R.string.bedrock_client_version, BuildConfig.VERSION_NAME));

        launchButton.setOnClickListener(v -> {
            if (!MinecraftLauncher.launch(requireActivity())) {
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.bedrock_game_title)
                        .setMessage(R.string.bedrock_launch_failed)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        });
        versionsButton.setOnClickListener(v -> {
            GameVersion installed = installedVersion();
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.bedrock_game_title)
                    .setMessage(installed == null ? getString(R.string.bedrock_game_missing)
                            : getString(R.string.bedrock_installed_version, installed.versionCode))
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        });
        shareLogsButton.setOnClickListener(v -> {
            Intent share = new Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, listener.getText().toString());
            startActivity(Intent.createChooser(share, getString(R.string.bedrock_share_status)));
        });
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        GameVersion installed = installedVersion();
        listener.setText(installed == null ? getString(R.string.bedrock_game_missing)
                : getString(R.string.bedrock_installed_version, installed.versionCode));
    }

    private GameVersion installedVersion() {
        VersionManager versions = VersionManager.get(requireContext());
        versions.loadAllVersions();
        return versions.getSelectedVersion();
    }

    @Override
    protected void onApplyTheme() {
        View view = getView();
        if (view == null || listener == null) return;
        ThemeManager theme = ThemeManager.getInstance();
        if (!theme.isThemeLoaded()) return;
        ThemeUtils.applyThemeToButton(launchButton, requireContext());
        for (MaterialButton button : new MaterialButton[] {versionsButton, shareLogsButton}) {
            ThemeUtils.applyThemeToButton(button, requireContext());
            button.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            button.setStrokeWidth(0);
        }
        versionsButton.setIconTint(ColorStateList.valueOf(theme.getColor("onSurfaceVariant")));
        listener.setTextColor(theme.getColor("onSurfaceVariant"));
        MaterialCardView statusCard = view.findViewById(R.id.logCard);
        statusCard.setCardBackgroundColor(theme.getColor("surfaceVariant"));
        statusCard.setStrokeColor(theme.getColor("outline"));
    }

    @Override
    public void onDestroyView() {
        listener = null;
        launchButton = null;
        versionsButton = null;
        shareLogsButton = null;
        super.onDestroyView();
    }
}
