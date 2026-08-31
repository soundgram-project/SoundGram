package soundgram.utils;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import soundgram.icons.IconPackManager;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.io.File;
import java.util.ArrayList;

public class DeveloperActivity extends BaseFragment {

    private RecyclerListView listView;
    private ListAdapter listAdapter;

    private int clearIconsRow;
    private int rowCount;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        rowCount = 0;
        clearIconsRow = rowCount++;
        return true;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("Developer Options");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setAdapter(listAdapter = new ListAdapter(context));
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener((view, position) -> {
            if (position == clearIconsRow) {
                clearAllIconPacksAndCache();
            }
        });

        return fragmentView;
    }

    private void clearAllIconPacksAndCache() {
        try {
            IconPackManager.setActivePack(null);
            
            File dir = IconPackManager.getIconPacksDir();
            if (dir.exists()) {
                IconPackManager.deleteDirectory(dir);
            }
            
            File sharedPacksDir = new File(ApplicationLoader.applicationContext.getCacheDir(), "shared_packs");
            if (sharedPacksDir.exists()) {
                IconPackManager.deleteDirectory(sharedPacksDir);
            }
            File editIconsTempDir = new File(ApplicationLoader.applicationContext.getCacheDir(), "edit_icons_temp");
            if (editIconsTempDir.exists()) {
                IconPackManager.deleteDirectory(editIconsTempDir);
            }
            
            FileLog.d("DeveloperActivity: Cleared all loaded custom icon packs and cache files immediately. Application's icon state reset to default.");
            Toast.makeText(getParentActivity(), "All custom icon packs and caches cleared", Toast.LENGTH_SHORT).show();
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
            FileLog.e(e);
            Toast.makeText(getParentActivity(), "Error clearing custom icon packs", Toast.LENGTH_SHORT).show();
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = new TextCell(mContext);
            view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            TextCell cell = (TextCell) holder.itemView;
            if (position == clearIconsRow) {
                cell.setTextAndIcon("Clear Icon Packs & Cache", R.drawable.msg_delete, false);
                cell.setColors(Theme.key_text_RedRegular, Theme.key_text_RedRegular);
            }
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{TextCell.class}, null, null, null, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));
        return themeDescriptions;
    }
}
