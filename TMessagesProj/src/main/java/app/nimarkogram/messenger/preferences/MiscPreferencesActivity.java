package app.nimarkogram.messenger.preferences;

import android.view.View;

import java.util.ArrayList;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import app.nimarkogram.messenger.NimarkoConfig;
import app.nimarkogram.messenger.NimarkoFeatureHooks;

public class MiscPreferencesActivity extends BasePreferencesActivity {

    private static final int ID_WS_PROXY          = 101;
    private static final int ID_TEXT_ANIMATION    = 102;
    private static final int ID_EDGE_TO_EDGE      = 103;
    private static final int ID_NO_ROUNDING       = 104;
    private static final int ID_DISCUSS_INSTEAD_OF_MUTE = 105;

    @Override
    public String getTitle() {
        return LocaleController.getString(R.string.NM_Cat_Miscellaneous);
    }

    @Override
    public void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(LocaleController.getString(R.string.NM_Cat_Miscellaneous)));
        
        items.add(UItem.asButton(ID_WS_PROXY, LocaleController.getString(R.string.BypassNetwork), LocaleController.getString(R.string.BypassNetworkSubtitle)));
        items.add(UItem.asButton(ID_TEXT_ANIMATION, LocaleController.getString(R.string.TextAnimation), LocaleController.getString(R.string.TextAnimationSubtitle)));
        
        items.add(UItem.asShadow(null));
        items.add(UItem.asHeader(LocaleController.getString(R.string.NM_EXP_Header_Beta)));
        items.add(UItem.asCheck(ID_EDGE_TO_EDGE, LocaleController.getString(R.string.NM_EXP_EdgeToEdge))
                .setChecked(NimarkoConfig.edgeToEdgeMode));
        items.add(UItem.asCheck(ID_NO_ROUNDING, LocaleController.getString(R.string.NM_EXP_NoRounding))
                .setChecked(NimarkoConfig.noRounding));
        items.add(UItem.asCheck(ID_DISCUSS_INSTEAD_OF_MUTE, LocaleController.getString(R.string.NM_EXP_DiscussInsteadOfMute))
                .setChecked(NimarkoConfig.discussInsteadOfMute));
        items.add(UItem.asShadow(LocaleController.getString(R.string.NM_EXP_Footer)));
    }

    @Override
    public void onClick(UItem item, View view, int position, float x, float y) {
        int id = item.id;
        if (id == ID_WS_PROXY) {
            presentFragment(new soundgram.wsproxy.NekoWsProxyActivity());
        } else if (id == ID_TEXT_ANIMATION) {
            presentFragment(new soundgram.animation.TextAnimationSettingsActivity());
        } else if (id == ID_EDGE_TO_EDGE) {
            NimarkoConfig.toggleEdgeToEdgeMode();
            applyCheck(item, view, NimarkoConfig.edgeToEdgeMode);
            showRestartBulletin();
        } else if (id == ID_NO_ROUNDING) {
            NimarkoConfig.toggleNoRounding();
            applyCheck(item, view, NimarkoConfig.noRounding);
            showRestartBulletin();
        } else if (id == ID_DISCUSS_INSTEAD_OF_MUTE) {
            NimarkoConfig.toggleDiscussInsteadOfMute();
            applyCheck(item, view, NimarkoConfig.discussInsteadOfMute);
            NimarkoFeatureHooks.setDiscussInsteadOfMute(NimarkoConfig.discussInsteadOfMute);
        }
    }

    private void applyCheck(UItem item, View view, boolean value) {
        item.checked = value;
        if (view instanceof TextCheckCell) {
            ((TextCheckCell) view).setChecked(value);
        }
    }
}
