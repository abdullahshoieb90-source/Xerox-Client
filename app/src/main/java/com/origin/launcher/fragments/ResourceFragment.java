package com.origin.launcher.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import com.origin.launcher.bedrock.BedrockPackImporter;
import com.origin.launcher.utils.ThemeUtils;
import com.origin.launcher.discord.DiscordRPCHelper;
import com.origin.launcher.R;

public class ResourceFragment extends BaseThemedFragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ResourceTabAdapter tabAdapter;
    private TabLayoutMediator tabMediator;
    private BedrockPackImporter importer;
    private MaterialButton pickButton;
    private final ActivityResultLauncher<String> filePicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(), this::onBedrockFileSelected);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_resource, container, false);
        
        // Initialize tab components
        initializeTabs(view);
        
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        importer = new BedrockPackImporter(requireActivity(), getViewLifecycleOwner());
        pickButton = view.findViewById(R.id.pick_bedrock_file);
        pickButton.setOnClickListener(v -> filePicker.launch("*/*"));
        super.onViewCreated(view, savedInstanceState);
    }

    private void onBedrockFileSelected(Uri uri) {
        if (uri == null || importer == null) return;
        pickButton.setEnabled(false);
        pickButton.setText(R.string.bedrock_import_preparing);
        importer.openDocument(uri, result -> {
            pickButton.setEnabled(true);
            pickButton.setText(R.string.bedrock_pick_file);
            Toast.makeText(requireContext(), result == 0 ? R.string.bedrock_file_opened : result,
                    Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void onDestroyView() {
        if (importer != null) importer.close();
        importer = null;
        pickButton = null;
        if (tabMediator != null) tabMediator.detach();
        if (viewPager != null) viewPager.setAdapter(null);
        tabMediator = null;
        tabAdapter = null;
        tabLayout = null;
        viewPager = null;
        super.onDestroyView();
    }

    private void initializeTabs(View view) {
        tabLayout = view.findViewById(R.id.tab_layout);
        viewPager = view.findViewById(R.id.view_pager);
        
        // Set up the adapter
        tabAdapter = new ResourceTabAdapter(this);
        viewPager.setAdapter(tabAdapter);
        
        // Connect TabLayout with ViewPager2
        tabMediator = new TabLayoutMediator(tabLayout, viewPager,
            (tab, position) -> {
                switch (position) {
                    case 0:
                        tab.setText("Packs");
                        break;
                    case 1:
                        tab.setText("Addons");
                        break;
                    case 2:
                        tab.setText("Maps");
                        break;
                    default:
                        tab.setText("Tab " + (position + 1));
                }
            });
        tabMediator.attach();
        // Apply theme to TabLayout after attaching
        ThemeUtils.applyThemeToTabLayout(tabLayout);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        DiscordRPCHelper.getInstance().updateMenuPresence("Resource installer");
    }
    
    @Override
    public void onPause() {
        super.onPause();
        DiscordRPCHelper.getInstance().updateIdlePresence();
    }
    


    private static class ResourceTabAdapter extends FragmentStateAdapter {
        
        public ResourceTabAdapter(Fragment fragment) {
            super(fragment);
        }
        
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new ResourcePacksFragment();
                case 1:
                    return new ResourceAddonsFragment();
                case 2:
                    return new ResourceMapsFragment();
                default:
                    return new ResourcePacksFragment();
            }
        }
        
        @Override
        public int getItemCount() {
            return 3;
        }
    }
}