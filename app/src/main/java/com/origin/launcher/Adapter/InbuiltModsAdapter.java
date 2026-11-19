package com.origin.launcher.Adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.origin.launcher.R;
import com.origin.launcher.Launcher.inbuilt.manager.InbuiltModManager;
import com.origin.launcher.Launcher.inbuilt.model.InbuiltMod;
import com.origin.launcher.Launcher.inbuilt.model.ModIds;
import com.origin.launcher.animation.DynamicAnim;
import com.origin.launcher.bedrock.BedrockHotbarPack;

import java.util.ArrayList;
import java.util.List;

public class InbuiltModsAdapter extends RecyclerView.Adapter<InbuiltModsAdapter.ViewHolder> {

    public interface OnToggleClickListener {
        void onToggleClick(InbuiltMod mod, boolean enable);
    }

    private final InbuiltModManager modManager;
    private List<InbuiltMod> mods = new ArrayList<>();
    private OnToggleClickListener onToggleClickListener;

    public InbuiltModsAdapter(InbuiltModManager modManager) {
        this.modManager = modManager;
    }

    public void setOnToggleClickListener(OnToggleClickListener listener) {
        this.onToggleClickListener = listener;
    }

    public void updateMods(List<InbuiltMod> mods) {
        this.mods = mods;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inbuilt_mod, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InbuiltMod mod = mods.get(position);
        Context context = holder.itemView.getContext();

        holder.name.setText(mod.getName());
        boolean isAdded = modManager.isModAdded(mod.getId());
        boolean isHotbar = BedrockHotbarPack.supports(mod.getId());
        holder.description.setText(isHotbar
                ? (isAdded ? R.string.bedrock_hotbar_selected : R.string.bedrock_hotbar_unselected)
                : R.string.bedrock_touch_legacy_only);
        int action = isHotbar
                ? (isAdded ? R.string.bedrock_hotbar_deselect : R.string.bedrock_hotbar_select)
                : (isAdded ? R.string.bedrock_touch_preview_remove : R.string.bedrock_touch_preview_add);
        holder.addButton.setText(action);
        holder.addButton.setAllCaps(false);
        holder.addButton.setContentDescription(context.getString(R.string.bedrock_touch_module_action,
                context.getString(action), mod.getName()));
        holder.addButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context,
                isAdded ? android.R.color.holo_red_light : android.R.color.holo_green_light)));

        holder.configSpinner.setOnItemSelectedListener(null);
        if (ModIds.AUTO_SPRINT.equals(mod.getId()) && isAdded) {
            holder.configContainer.setVisibility(View.VISIBLE);

            String[] options = {
                    context.getString(R.string.autosprint_key_ctrl),
                    context.getString(R.string.autosprint_key_shift)
            };
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                    context,
                    R.layout.spinner_item_inbuilt,
                    options
            );
            spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_inbuilt);
            holder.configSpinner.setAdapter(spinnerAdapter);

            int currentKey = modManager.getAutoSprintKey();
            holder.configSpinner.setSelection(
                    currentKey == KeyEvent.KEYCODE_SHIFT_LEFT ? 1 : 0
            );

            holder.configSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                    int key = pos == 1 ? KeyEvent.KEYCODE_SHIFT_LEFT : KeyEvent.KEYCODE_CTRL_LEFT;
                    modManager.setAutoSprintKey(key);
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        } else {
            holder.configContainer.setVisibility(View.GONE);
        }

        holder.addButton.setOnClickListener(v -> {
            if (onToggleClickListener != null) {
                onToggleClickListener.onToggleClick(mod, !modManager.isModAdded(mod.getId()));
            }
        });
        DynamicAnim.applyPressScale(holder.addButton);
    }

    @Override
    public int getItemCount() {
        return mods.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, description;
        Button addButton;
        LinearLayout configContainer;
        Spinner configSpinner;

        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.inbuilt_mod_name);
            description = itemView.findViewById(R.id.inbuilt_mod_description);
            addButton = itemView.findViewById(R.id.inbuilt_mod_add_button);
            configContainer = itemView.findViewById(R.id.config_container);
            configSpinner = itemView.findViewById(R.id.config_spinner);
        }
    }
}