package tw.nekomimi.nekogram.settings;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.security.SecureRandom;

import tw.nekomimi.nekogram.wsproxy.TgWsProxyService;

public class NekoWsProxyActivity extends BaseNekoSettingsActivity {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_SHADOW = 1;
    private static final int TYPE_TEXT_CHECK = 2;
    private static final int TYPE_TEXT_DETAIL = 3;
    private static final int TYPE_TEXT = 4;

    private int statusRow, enableRow, shadow1Row, 
                portRow, secretRow, 
                spoofRow, spoofDomainRow,
                shadow2Row, applyRow;

    private BroadcastReceiver statusReceiver;

    @Override
    public void onResume() {
        super.onResume();
        statusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (listAdapter != null) listAdapter.notifyDataSetChanged();
            }
        };
        IntentFilter filter = new IntentFilter(TgWsProxyService.ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParentActivity().registerReceiver(statusReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            getParentActivity().registerReceiver(statusReceiver, filter);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (statusReceiver != null) {
            getParentActivity().unregisterReceiver(statusReceiver);
            statusReceiver = null;
        }
    }

    @Override
    protected void updateRows() {
        super.updateRows();
        statusRow     = addRow();
        enableRow     = addRow();
        shadow1Row    = addRow();
        portRow       = addRow();
        secretRow     = addRow();
        spoofRow      = addRow();
        spoofDomainRow= addRow();
        shadow2Row    = addRow();
        applyRow      = addRow();
        if (listAdapter != null) listAdapter.notifyDataSetChanged();
    }

    @Override
    protected String getActionBarTitle() {
        return "🔒 SoundGram Proxy";
    }

    @SuppressLint("ApplySharedPref")
    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position == enableRow) {
            toggleProxy();
        } else if (position == portRow) {
            showInputDialog("Порт", String.valueOf(getPrefs().getInt("wsproxy_port", 1443)),
                InputType.TYPE_CLASS_NUMBER, v -> {
                try {
                    int p = Integer.parseInt(v);
                    if (p > 1024 && p < 65536) {
                        getPrefs().edit().putInt("wsproxy_port", p).commit();
                        listAdapter.notifyItemChanged(portRow);
                    }
                } catch (Exception ignored) {}
            });
        } else if (position == secretRow) {
            showInputDialog("Секрет", getPrefs().getString("wsproxy_secret", ""),
                InputType.TYPE_CLASS_TEXT, v -> {
                if (v.length() == 32) {
                    getPrefs().edit().putString("wsproxy_secret", v).commit();
                    listAdapter.notifyItemChanged(secretRow);
                }
            });
        } else if (position == spoofRow) {
            boolean on = !getPrefs().getBoolean("wsproxy_spoof", false);
            getPrefs().edit().putBoolean("wsproxy_spoof", on).commit();
            listAdapter.notifyItemChanged(spoofRow);
            listAdapter.notifyItemChanged(spoofDomainRow);
        } else if (position == spoofDomainRow) {
            String[] domains = {"vk.com", "yandex.ru", "mail.ru", "ok.ru"};
            android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(getParentActivity());
            b.setTitle("Маскировка под");
            b.setItems(domains, (d, w) -> {
                getPrefs().edit().putString("wsproxy_spoof_domain", domains[w]).commit();
                listAdapter.notifyItemChanged(spoofDomainRow);
            });
            b.show();
        } else if (position == applyRow) {
            applyProxy();
        }
    }

    private void toggleProxy() {
        boolean on = !getPrefs().getBoolean("wsproxy_enabled", false);
        getPrefs().edit().putBoolean("wsproxy_enabled", on).commit();
        
        Context ctx = getParentActivity();
        if (ctx == null) return;
        
        if (on) {
            // Генерируем секрет если нет
            String secret = getPrefs().getString("wsproxy_secret", "");
            if (secret.isEmpty()) {
                byte[] b = new byte[16];
                new SecureRandom().nextBytes(b);
                StringBuilder sb = new StringBuilder();
                for (byte v : b) sb.append(String.format("%02x", v));
                secret = sb.toString();
                getPrefs().edit().putString("wsproxy_secret", secret).commit();
            }
            
            Intent intent = new Intent(ctx, TgWsProxyService.class);
            intent.setAction(TgWsProxyService.ACTION_START);
            intent.putExtra(TgWsProxyService.EXTRA_PORT, getPrefs().getInt("wsproxy_port", 1443));
            intent.putExtra(TgWsProxyService.EXTRA_SECRET, secret);
            intent.putExtra(TgWsProxyService.EXTRA_POOL_SIZE, getPrefs().getInt("wsproxy_pool", 4));
            intent.putExtra(TgWsProxyService.EXTRA_CF_ENABLED, getPrefs().getBoolean("wsproxy_cf", true));
            intent.putExtra(TgWsProxyService.EXTRA_CF_DOMAIN, getPrefs().getString("wsproxy_cf_domain", ""));
            intent.putExtra(TgWsProxyService.EXTRA_SPOOF_SNI, getPrefs().getBoolean("wsproxy_spoof", false));
            intent.putExtra(TgWsProxyService.EXTRA_SPOOF_DOMAIN, getPrefs().getString("wsproxy_spoof_domain", "vk.com"));
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent);
            } else {
                ctx.startService(intent);
            }
        } else {
            ctx.startService(new Intent(ctx, TgWsProxyService.class)
                .setAction(TgWsProxyService.ACTION_STOP));
        }
        
        listAdapter.notifyItemChanged(enableRow);
        listAdapter.notifyItemChanged(statusRow);
    }

    private SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("wsproxy", Context.MODE_PRIVATE);
    }

    private void applyProxy() {
        int port = getPrefs().getInt("wsproxy_port", 1443);
        String secret = getPrefs().getString("wsproxy_secret", "");
        if (secret.isEmpty()) {
            AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity(), resourceProvider);
            b.setMessage("Сначала включите прокси");
            b.setPositiveButton("OK", null);
            b.show();
            return;
        }
        String url = String.format("tg://proxy?server=127.0.0.1&port=%d&secret=dd%s", port, secret);
        try {
            getParentActivity().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity(), resourceProvider);
            b.setMessage("Не удалось открыть tg://proxy");
            b.setPositiveButton("OK", null);
            b.show();
        }
    }

    private void showInputDialog(String title, String current, int inputType, OnValueSet l) {
        Context c = getParentActivity();
        if (c == null) return;
        AlertDialog.Builder b = new AlertDialog.Builder(c, resourceProvider);
        b.setTitle(title);
        EditTextBoldCursor et = new EditTextBoldCursor(c);
        et.setText(current);
        et.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        et.setBackground(null);
        et.setInputType(inputType);
        et.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        android.widget.FrameLayout fl = new android.widget.FrameLayout(c);
        fl.addView(et, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 16, 8, 16, 8));
        b.setView(fl);
        b.setPositiveButton("OK", (d, w) -> {
            if (l != null) l.onValue(et.getText().toString());
        });
        b.setNegativeButton("Отмена", null);
        b.show();
        et.requestFocus();
        AndroidUtilities.showKeyboard(et);
    }

    private interface OnValueSet { void onValue(String v); }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    private class ListAdapter extends BaseListAdapter {
        public ListAdapter(Context context) { super(context); }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean partial) {
            int viewType = holder.getItemViewType();
            
            if (viewType == TYPE_HEADER) {
                HeaderCell cell = (HeaderCell) holder.itemView;
                if (position == statusRow) {
                    cell.setText("Статус");
                }
            } else if (viewType == TYPE_SHADOW) {
                holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
            } else if (viewType == TYPE_TEXT_CHECK) {
                TextCheckCell cell = (TextCheckCell) holder.itemView;
                if (position == enableRow) {
                    boolean enabled = getPrefs().getBoolean("wsproxy_enabled", false);
                    cell.setTextAndCheck("Включить SoundGram Proxy", enabled, true);
                } else if (position == spoofRow) {
                    boolean enabled = getPrefs().getBoolean("wsproxy_spoof", false);
                    cell.setTextAndCheck("🛡 Обход белых списков РФ", enabled, true);
                }
            } else if (viewType == TYPE_TEXT_DETAIL) {
                TextDetailSettingsCell cell = (TextDetailSettingsCell) holder.itemView;
                if (position == statusRow) {
                    boolean enabled = getPrefs().getBoolean("wsproxy_enabled", false);
                    boolean running = TgWsProxyService.isRunning;
                    if (enabled && running) {
                        cell.setTextAndValue("🟢 Работает", "127.0.0.1:" + getPrefs().getInt("wsproxy_port", 1443), true);
                    } else if (enabled) {
                        cell.setTextAndValue("🟡 Запускается", "Подождите…", true);
                    } else {
                        cell.setTextAndValue("🔴 Выключен", "Нажмите для включения", true);
                    }
                } else if (position == secretRow) {
                    String s = getPrefs().getString("wsproxy_secret", "");
                    cell.setTextAndValue("Секретный ключ", s.isEmpty() ? "Не задан" : "••••" + s.substring(28), true);
                }
            } else if (viewType == TYPE_TEXT) {
                TextCell cell = (TextCell) holder.itemView;
                if (position == portRow) {
                    cell.setTextAndValue("Порт", String.valueOf(getPrefs().getInt("wsproxy_port", 1443)), true);
                } else if (position == spoofDomainRow) {
                    cell.setTextAndValue("Маскировка под", getPrefs().getString("wsproxy_spoof_domain", "vk.com"), true);
                } else if (position == applyRow) {
                    cell.setTextAndValueAndIcon("🚀 Подключить в Telegram", "tg://proxy", R.drawable.msg_link, false);
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == statusRow) return TYPE_TEXT_DETAIL;
            if (position == shadow1Row || position == shadow2Row) return TYPE_SHADOW;
            if (position == enableRow || position == spoofRow) return TYPE_TEXT_CHECK;
            if (position == secretRow) return TYPE_TEXT_DETAIL;
            return TYPE_TEXT;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            if (viewType == TYPE_HEADER) view = new HeaderCell(mContext);
            else if (viewType == TYPE_TEXT_CHECK) view = new TextCheckCell(mContext);
            else if (viewType == TYPE_TEXT_DETAIL) view = new TextDetailSettingsCell(mContext);
            else if (viewType == TYPE_SHADOW) view = new ShadowSectionCell(mContext);
            else view = new TextCell(mContext);
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }
    }
}
