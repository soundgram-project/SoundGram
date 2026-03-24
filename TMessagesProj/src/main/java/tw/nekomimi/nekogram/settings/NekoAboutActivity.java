package tw.nekomimi.nekogram.settings;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.Cells.TextSettingsCell;

import tw.nekomimi.nekogram.DatacenterActivity;

public class NekoAboutActivity extends BaseNekoSettingsActivity {

    private int xChannelRow;
    private int channelTipsRow;
    private int datacenterStatusRow;
    private int supportRow;

    @Override
    protected void updateRows() {
        super.updateRows();

        xChannelRow = addRow();
        channelTipsRow = addRow();
        datacenterStatusRow = addRow();
        supportRow = addRow();
    }

    @Override
    protected String getActionBarTitle() {
        return getString(R.string.About);
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position == xChannelRow) {
            MessagesController.getInstance(currentAccount).openByUserName("SoundGram_project", NekoAboutActivity.this, 1);
        } else if (position == channelTipsRow) {
            MessagesController.getInstance(currentAccount).openByUserName("SoundGram_chat", NekoAboutActivity.this, 1);
        } else if (position == datacenterStatusRow) {
            presentFragment(new DatacenterActivity(0));
        }else if (position == supportRow){
            Browser.openUrl(getParentActivity(), "http://t.me/send?start=IVC5XMsGUEgz");

        }
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean partial) {
            if (holder.getItemViewType() == TYPE_SETTINGS) {
                TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                if (position == xChannelRow) {
                    textCell.setTextAndValue(getString(R.string.XChannel), "@SoundGram_project", true);
                } else if (position == channelTipsRow) {
                    textCell.setTextAndValue(getString(R.string.TipsChannel), "@" + "soundgram_chat", true);
                } else if (position == datacenterStatusRow) {
                    textCell.setText(getString(R.string.DatacenterStatus), false);
                }else if (position == supportRow){
                        textCell.setTextAndValue(getString(R.string.SourceCode), "Support", true);
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            return TYPE_SETTINGS;
        }
    }
}
