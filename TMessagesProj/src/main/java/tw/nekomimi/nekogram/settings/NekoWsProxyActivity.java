package tw.nekomimi.nekogram.settings;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.security.SecureRandom;
import android.view.Gravity;
import android.widget.FrameLayout;

import tw.nekomimi.nekogram.wsproxy.NativeProxy;
import tw.nekomimi.nekogram.wsproxy.TgWsProxyService;

public class NekoWsProxyActivity extends BaseNekoSettingsActivity {

    // ── View types ──────────────────────────────────────────────────────────
    private static final int TYPE_HEADER      = 0;
    private static final int TYPE_SHADOW      = 1;
    private static final int TYPE_TEXT_CHECK  = 2;
    private static final int TYPE_TEXT_DETAIL = 3;
    private static final int TYPE_TEXT        = 4;
    private static final int TYPE_INFO        = 5;

    // ── Defaults (match Go engine defaults exactly) ──────────────────────────
    private static final int     DEFAULT_PORT        = 1443;
    private static final int     DEFAULT_POOL_SIZE   = 4;       // Go: defaultPoolSz = 4
    private static final int     DEFAULT_BUFFER      = 256;
    private static final boolean DEFAULT_FAKE_TLS    = false;   // Go: fakeTlsEnabled = false
    private static final String  DEFAULT_FAKE_DOMAIN = "google.com";
    private static final boolean DEFAULT_CF_ENABLED  = true;
    private static final String  DEFAULT_CF_DOMAIN   = "";
    private static final boolean DEFAULT_AUTO_START  = true;
    private static final String  DEFAULT_DC_IPS      = "";

    // ── Row indices ──────────────────────────────────────────────────────────
    private int statusRow, enableRow, pingRow, autoStartRow,
                shadow1Row,
                connectionHeaderRow, portRow, poolRow, bufferRow,
                shadow2Row,
                securityHeaderRow, secretRow, fakeTlsRow, fakeDomainRow,
                shadow3Row,
                cfHeaderRow, cfEnabledRow, cfDomainRow,
                shadow4Row,
                advancedHeaderRow, dcIpsRow,
                shadow5Row,
                infoRow;

    // ── Ping ─────────────────────────────────────────────────────────────────
    // -1 = not available, >=0 = milliseconds
    // Measured as direct TCP connect to 127.0.0.1:port (same as standalone tgwsproxy app).
    // NOT using ConnectionsManager.checkProxy() — that measures full MTProto handshake
    // through the proxy and returns 500 ms+, which is misleading.
    private volatile int lastPingMs = -1;

    private final Handler pingHandler = new Handler(Looper.getMainLooper());
    private final Runnable pingRunnable = new Runnable() {
        @Override public void run() {
            updatePingAsync();
            pingHandler.postDelayed(this, 3000);
        }
    };

    private BroadcastReceiver statusReceiver;

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void onResume() {
        super.onResume();
        statusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (listAdapter != null) listAdapter.notifyDataSetChanged();
                updatePingAsync();
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(TgWsProxyService.ACTION_START);
        filter.addAction(TgWsProxyService.ACTION_STOP);
        filter.addAction(TgWsProxyService.ACTION_STATE_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParentActivity().registerReceiver(
                    statusReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            getParentActivity().registerReceiver(statusReceiver, filter);
        }
        pingHandler.post(pingRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (statusReceiver != null) {
            getParentActivity().unregisterReceiver(statusReceiver);
            statusReceiver = null;
        }
        pingHandler.removeCallbacks(pingRunnable);
    }

    // ── Ping — via Telegram's ConnectionsManager.checkProxy() ───────────────
    // Same method Telegram uses internally, so the value matches what you see
    // in Settings → Data & Storage → Proxy. Shows -2 while checking.

    private void updatePingAsync() {
        if (!TgWsProxyService.isRunning) {
            lastPingMs = -1;
            notifyPingChanged();
            return;
        }
        final int    port   = getPrefs().getInt("port", DEFAULT_PORT);
        final String secret = getPrefs().getString("secret", "");
        if (secret.isEmpty()) {
            lastPingMs = -1;
            notifyPingChanged();
            return;
        }

        // Find or create the proxy entry in SharedConfig so checkProxy can use it
        SharedConfig.ProxyInfo target = null;
        for (SharedConfig.ProxyInfo info : SharedConfig.proxyList) {
            if ("127.0.0.1".equals(info.address) && info.port == port) {
                target = info;
                break;
            }
        }
        if (target == null) {
            // Proxy not yet registered — nothing to check
            lastPingMs = -1;
            notifyPingChanged();
            return;
        }

        // If already checking, reflect that in UI without launching a second check
        if (target.checking) {
            lastPingMs = -2;
            notifyPingChanged();
            return;
        }

        // Throttle: re-check only if last result is older than 5 s
        if (android.os.SystemClock.elapsedRealtime() - target.availableCheckTime < 5000) {
            lastPingMs = target.available ? (int) target.ping : -1;
            notifyPingChanged();
            return;
        }

        // Launch async check via Telegram (same as Telegram's own proxy ping)
        final SharedConfig.ProxyInfo proxyInfo = target;
        proxyInfo.checking = true;
        lastPingMs = -2;
        notifyPingChanged();

        proxyInfo.proxyCheckPingId = ConnectionsManager
                .getInstance(currentAccount)
                .checkProxy(
                        proxyInfo.address, proxyInfo.port,
                        proxyInfo.username, proxyInfo.password, proxyInfo.secret,
                        time -> AndroidUtilities.runOnUIThread(() -> {
                            proxyInfo.availableCheckTime = android.os.SystemClock.elapsedRealtime();
                            proxyInfo.checking = false;
                            if (time == -1) {
                                proxyInfo.available = false;
                                proxyInfo.ping      = 0;
                                lastPingMs          = -1;
                            } else {
                                proxyInfo.ping      = time;
                                proxyInfo.available = true;
                                lastPingMs          = (int) time;
                            }
                            notifyPingChanged();
                        }));
    }

    private void notifyPingChanged() {
        AndroidUtilities.runOnUIThread(() -> {
            if (listAdapter != null && pingRow >= 0) {
                listAdapter.notifyItemChanged(pingRow);
            }
        });
    }

    // ── Row layout ───────────────────────────────────────────────────────────

    @Override
    protected void updateRows() {
        super.updateRows();
        int row = 0;

        statusRow           = row++;
        enableRow           = row++;
        pingRow             = row++;
        autoStartRow        = row++;
        shadow1Row          = row++;

        connectionHeaderRow = row++;
        portRow             = row++;
        poolRow             = row++;
        bufferRow           = row++;
        shadow2Row          = row++;

        securityHeaderRow   = row++;
        secretRow           = row++;
        fakeTlsRow          = row++;
        fakeDomainRow       = getPrefs().getBoolean("fake_tls", DEFAULT_FAKE_TLS) ? row++ : -1;
        shadow3Row          = row++;

        cfHeaderRow         = row++;
        cfEnabledRow        = row++;
        cfDomainRow         = row++;
        shadow4Row          = row++;

        advancedHeaderRow   = row++;
        dcIpsRow            = row++;
        shadow5Row          = row++;

        infoRow             = row++;
        rowCount            = row;

        if (listAdapter != null) listAdapter.notifyDataSetChanged();
    }

    @Override
    protected String getActionBarTitle() {
        return "TG WS Proxy";
    }

    // ── Click handling ───────────────────────────────────────────────────────

    @SuppressLint("ApplySharedPref")
    @Override
    protected void onItemClick(View view, int position, float x, float y) {

        if (position == enableRow) {
            toggleProxy();

        } else if (position == autoStartRow) {
            boolean on = !getPrefs().getBoolean("auto_start", DEFAULT_AUTO_START);
            getPrefs().edit().putBoolean("auto_start", on).commit();
            updateRows();

        } else if (position == portRow) {
            showInputDialog("Local port",
                    String.valueOf(getPrefs().getInt("port", DEFAULT_PORT)),
                    InputType.TYPE_CLASS_NUMBER, v -> {
                        try {
                            int p = Integer.parseInt(v.trim());
                            if (p > 1024 && p < 65536) {
                                getPrefs().edit().putInt("port", p).commit();
                                updateRows();
                                if (getPrefs().getBoolean("enabled", false)) restartProxy();
                            }
                        } catch (Exception ignored) {}
                    });

        } else if (position == poolRow) {
            String[] opts = {"2", "4", "6", "8", "10", "12", "16"};
            int cur = getPrefs().getInt("pool_size", DEFAULT_POOL_SIZE);
            int idx = 1; // default index = 4
            for (int i = 0; i < opts.length; i++) {
                if (Integer.parseInt(opts[i]) == cur) { idx = i; break; }
            }
            new android.app.AlertDialog.Builder(getParentActivity())
                    .setTitle("WebSocket pool size")
                    .setSingleChoiceItems(opts, idx, (d, w) -> {
                        getPrefs().edit()
                                .putInt("pool_size", Integer.parseInt(opts[w])).commit();
                        d.dismiss();
                        updateRows();
                        if (getPrefs().getBoolean("enabled", false)) restartProxy();
                    })
                    .show();

        } else if (position == bufferRow) {
            String[] labels = {"64 KB", "128 KB", "256 KB", "512 KB"};
            int[]    vals   = {64, 128, 256, 512};
            int cur = getPrefs().getInt("buffer", DEFAULT_BUFFER);
            int idx = 2;
            for (int i = 0; i < vals.length; i++) {
                if (vals[i] == cur) { idx = i; break; }
            }
            new android.app.AlertDialog.Builder(getParentActivity())
                    .setTitle("Socket buffer size")
                    .setSingleChoiceItems(labels, idx, (d, w) -> {
                        getPrefs().edit().putInt("buffer", vals[w]).commit();
                        d.dismiss();
                        updateRows();
                        if (getPrefs().getBoolean("enabled", false)) restartProxy();
                    })
                    .show();

        } else if (position == secretRow) {
            String current = getPrefs().getString("secret", "");
            // Offer: edit or regenerate
            new android.app.AlertDialog.Builder(getParentActivity())
                    .setTitle("Secret key")
                    .setItems(new String[]{"Edit manually", "Generate new key"}, (d, w) -> {
                        if (w == 0) {
                            showInputDialog("Secret key (32 hex characters)", current,
                                    InputType.TYPE_CLASS_TEXT
                                            | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS, v -> {
                                        if (v.trim().length() == 32) {
                                            getPrefs().edit()
                                                    .putString("secret", v.trim()).commit();
                                            updateRows();
                                            if (getPrefs().getBoolean("enabled", false))
                                                restartProxy();
                                        } else {
                                            showToast("Secret must be exactly 32 hex characters");
                                        }
                                    });
                        } else {
                            String s = generateSecret();
                            getPrefs().edit().putString("secret", s).commit();
                            updateRows();
                            if (getPrefs().getBoolean("enabled", false)) restartProxy();
                        }
                    })
                    .show();

        } else if (position == fakeTlsRow) {
            boolean on = !getPrefs().getBoolean("fake_tls", DEFAULT_FAKE_TLS);
            getPrefs().edit().putBoolean("fake_tls", on).commit();
            updateRows();
            if (getPrefs().getBoolean("enabled", false)) restartProxy();

        } else if (position == fakeDomainRow) {
            showInputDialog("FakeTLS domain",
                    getPrefs().getString("fake_domain", DEFAULT_FAKE_DOMAIN),
                    InputType.TYPE_CLASS_TEXT, v -> {
                        if (!v.trim().isEmpty()) {
                            getPrefs().edit()
                                    .putString("fake_domain", v.trim()).commit();
                            updateRows();
                            if (getPrefs().getBoolean("enabled", false)) restartProxy();
                        }
                    });

        } else if (position == cfEnabledRow) {
            boolean on = !getPrefs().getBoolean("cf_enabled", DEFAULT_CF_ENABLED);
            getPrefs().edit().putBoolean("cf_enabled", on).commit();
            updateRows();
            if (getPrefs().getBoolean("enabled", false)) restartProxy();

        } else if (position == cfDomainRow) {
            showInputDialog("Custom Cloudflare domain (leave blank for auto)",
                    getPrefs().getString("cf_domain", DEFAULT_CF_DOMAIN),
                    InputType.TYPE_CLASS_TEXT, v -> {
                        getPrefs().edit()
                                .putString("cf_domain", v.trim()).commit();
                        updateRows();
                        if (getPrefs().getBoolean("enabled", false)) restartProxy();
                    });

        } else if (position == dcIpsRow) {
            showInputDialog("Manual DC IP overrides (e.g. 2:IP,4:IP)",
                    getPrefs().getString("dc_ips", DEFAULT_DC_IPS),
                    InputType.TYPE_CLASS_TEXT, v -> {
                        getPrefs().edit().putString("dc_ips", v.trim()).commit();
                        updateRows();
                        if (getPrefs().getBoolean("enabled", false)) restartProxy();
                    });
        }
    }

    // ── Enable / disable proxy ───────────────────────────────────────────────

    private void toggleProxy() {
        boolean on = !getPrefs().getBoolean("enabled", false);
        getPrefs().edit().putBoolean("enabled", on).commit();

        Context ctx = getParentActivity();
        if (ctx == null) return;

        if (on) {
            // Ensure a secret exists
            if (getPrefs().getString("secret", "").isEmpty()) {
                getPrefs().edit().putString("secret", generateSecret()).commit();
            }
            startProxyService();
            // Auto-apply proxy settings after service has time to start
            AndroidUtilities.runOnUIThread(this::autoApplyProxy, 3500);
        } else {
            ctx.startService(new Intent(ctx, TgWsProxyService.class)
                    .setAction(TgWsProxyService.ACTION_STOP));
            disableTelegramProxy();
        }

        updateRows();
    }

    private void startProxyService() {
        Context ctx = getParentActivity();
        if (ctx == null) return;
        SharedPreferences p = getPrefs();

        Intent intent = new Intent(ctx, TgWsProxyService.class);
        intent.setAction(TgWsProxyService.ACTION_START);
        intent.putExtra(TgWsProxyService.EXTRA_PORT,        p.getInt("port",          DEFAULT_PORT));
        intent.putExtra(TgWsProxyService.EXTRA_SECRET,      p.getString("secret",     ""));
        intent.putExtra(TgWsProxyService.EXTRA_POOL_SIZE,   p.getInt("pool_size",     DEFAULT_POOL_SIZE));
        intent.putExtra(TgWsProxyService.EXTRA_RECV_BUF,    p.getInt("buffer",        DEFAULT_BUFFER));
        intent.putExtra(TgWsProxyService.EXTRA_SEND_BUF,    p.getInt("buffer",        DEFAULT_BUFFER));
        intent.putExtra(TgWsProxyService.EXTRA_CF_ENABLED,  p.getBoolean("cf_enabled", DEFAULT_CF_ENABLED));
        intent.putExtra(TgWsProxyService.EXTRA_CF_DOMAIN,   p.getString("cf_domain",  DEFAULT_CF_DOMAIN));
        intent.putExtra(TgWsProxyService.EXTRA_FAKE_TLS,    p.getBoolean("fake_tls",  DEFAULT_FAKE_TLS));
        intent.putExtra(TgWsProxyService.EXTRA_FAKE_DOMAIN, p.getString("fake_domain", DEFAULT_FAKE_DOMAIN));
        intent.putExtra(TgWsProxyService.EXTRA_VERBOSE,     0);
        intent.putExtra(TgWsProxyService.EXTRA_DC_IPS,      p.getString("dc_ips",     DEFAULT_DC_IPS));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(intent);
        } else {
            ctx.startService(intent);
        }
    }

    private void restartProxy() {
        Context ctx = getParentActivity();
        if (ctx == null) return;
        ctx.startService(new Intent(ctx, TgWsProxyService.class)
                .setAction(TgWsProxyService.ACTION_STOP));
        AndroidUtilities.runOnUIThread(() -> {
            if (getPrefs().getBoolean("enabled", false)) {
                startProxyService();
                AndroidUtilities.runOnUIThread(this::autoApplyProxy, 3500);
            }
        }, 1500);
    }

    // ── Telegram proxy settings helpers ─────────────────────────────────────

    @SuppressLint("ApplySharedPref")
    private void autoApplyProxy() {
        int port = getPrefs().getInt("port", DEFAULT_PORT);
        String secret = getPrefs().getString("secret", "");
        if (secret.isEmpty()) return;

        String proxySecret = NativeProxy.getSecretWithPrefix();
        if (proxySecret == null) proxySecret = "dd" + secret;

        SharedConfig.ProxyInfo existing = null;
        for (SharedConfig.ProxyInfo info : SharedConfig.proxyList) {
            if ("127.0.0.1".equals(info.address) && info.port == port) {
                existing = info;
                break;
            }
        }

        SharedConfig.ProxyInfo proxyInfo;
        if (existing != null) {
            existing.secret = proxySecret;
            proxyInfo = existing;
        } else {
            proxyInfo = new SharedConfig.ProxyInfo("127.0.0.1", port, "", "", proxySecret);
            SharedConfig.addProxy(proxyInfo);
        }

        SharedConfig.currentProxy = proxyInfo;
        SharedConfig.saveProxyList();

        org.telegram.messenger.MessagesController.getGlobalMainSettings()
                .edit()
                .putBoolean("proxy_enabled", true)
                .putString("proxy_ip",       "127.0.0.1")
                .putString("proxy_pass",     "")
                .putString("proxy_user",     "")
                .putInt("proxy_port",        port)
                .putString("proxy_secret",   proxySecret)
                .commit();

        ConnectionsManager.setProxySettings(true, "127.0.0.1", port, "", "", proxySecret);
        org.telegram.messenger.NotificationCenter.getGlobalInstance()
                .postNotificationName(
                        org.telegram.messenger.NotificationCenter.proxySettingsChanged);
    }

    @SuppressLint("ApplySharedPref")
    private void disableTelegramProxy() {
        org.telegram.messenger.MessagesController.getGlobalMainSettings()
                .edit()
                .putBoolean("proxy_enabled", false)
                .commit();
        ConnectionsManager.setProxySettings(false, "", 0, "", "", "");
        org.telegram.messenger.NotificationCenter.getGlobalInstance()
                .postNotificationName(
                        org.telegram.messenger.NotificationCenter.proxySettingsChanged);
    }

    // ── Preferences ─────────────────────────────────────────────────────────

    private SharedPreferences getPrefs() {
        SharedPreferences p = ApplicationLoader.applicationContext
                .getSharedPreferences("tgwsproxy", Context.MODE_PRIVATE);
        if (!p.contains("defaults_applied")) {
            p.edit()
                    .putInt    ("port",             DEFAULT_PORT)
                    .putInt    ("pool_size",         DEFAULT_POOL_SIZE)
                    .putInt    ("buffer",            DEFAULT_BUFFER)
                    .putBoolean("fake_tls",          DEFAULT_FAKE_TLS)
                    .putString ("fake_domain",       DEFAULT_FAKE_DOMAIN)
                    .putBoolean("cf_enabled",        DEFAULT_CF_ENABLED)
                    .putString ("cf_domain",         DEFAULT_CF_DOMAIN)
                    .putBoolean("auto_start",        DEFAULT_AUTO_START)
                    .putString ("dc_ips",            DEFAULT_DC_IPS)
                    .putBoolean("defaults_applied",  true)
                    .apply();
        }
        return p;
    }

    // ── Utilities ────────────────────────────────────────────────────────────

    private static String generateSecret() {
        byte[] b = new byte[16];
        new SecureRandom().nextBytes(b);
        StringBuilder sb = new StringBuilder(32);
        for (byte bv : b) sb.append(String.format("%02x", bv));
        return sb.toString();
    }

    private void showToast(String text) {
        AndroidUtilities.runOnUIThread(() -> {
            Context c = getParentActivity();
            if (c != null)
                android.widget.Toast.makeText(c, text, android.widget.Toast.LENGTH_LONG).show();
        });
    }

    private void showInputDialog(String title, String current,
                                 int inputType, OnValueSet listener) {
        Context c = getParentActivity();
        if (c == null) return;
        AlertDialog.Builder b = new AlertDialog.Builder(c, resourceProvider);
        b.setTitle(title);
        EditTextBoldCursor et = new EditTextBoldCursor(c);
        et.setText(current);
        et.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        et.setBackground(null);
        et.setInputType(inputType);
        et.setPadding(
                AndroidUtilities.dp(16), AndroidUtilities.dp(12),
                AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        FrameLayout fl = new FrameLayout(c);
        fl.addView(et, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                Gravity.TOP, 16, 8, 16, 8));
        b.setView(fl);
        b.setPositiveButton("OK", (d, w) -> {
            if (listener != null) listener.onValue(et.getText().toString());
        });
        b.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        b.show();
        et.requestFocus();
        AndroidUtilities.showKeyboard(et);
    }

    private interface OnValueSet { void onValue(String v); }

    // ── Adapter ──────────────────────────────────────────────────────────────

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    private class ListAdapter extends BaseListAdapter {
        ListAdapter(Context context) { super(context); }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder,
                                     int position, boolean partial) {
            final SharedPreferences prefs = getPrefs();
            final int vt = holder.getItemViewType();

            if (vt == TYPE_HEADER) {
                HeaderCell cell = (HeaderCell) holder.itemView;
                if      (position == connectionHeaderRow) cell.setText("Connection");
                else if (position == securityHeaderRow)   cell.setText("Security");
                else if (position == cfHeaderRow)         cell.setText("Cloudflare");
                else if (position == advancedHeaderRow)   cell.setText("Advanced");

            } else if (vt == TYPE_SHADOW) {
                holder.itemView.setBackground(Theme.getThemedDrawable(
                        mContext, R.drawable.greydivider,
                        Theme.key_windowBackgroundGrayShadow));

            } else if (vt == TYPE_TEXT_CHECK) {
                TextCheckCell cell = (TextCheckCell) holder.itemView;
                if (position == enableRow) {
                    cell.setTextAndCheck("Enable TG WS Proxy",
                            prefs.getBoolean("enabled", false), true);
                } else if (position == autoStartRow) {
                    cell.setTextAndCheck("Auto-start on boot",
                            prefs.getBoolean("auto_start", DEFAULT_AUTO_START), true);
                } else if (position == fakeTlsRow) {
                    cell.setTextAndCheck("FakeTLS (ee-secret)",
                            prefs.getBoolean("fake_tls", DEFAULT_FAKE_TLS),
                            fakeDomainRow != -1);
                } else if (position == cfEnabledRow) {
                    cell.setTextAndCheck("Cloudflare CDN",
                            prefs.getBoolean("cf_enabled", DEFAULT_CF_ENABLED), true);
                }

            } else if (vt == TYPE_TEXT_DETAIL) {
                TextDetailSettingsCell cell = (TextDetailSettingsCell) holder.itemView;
                if (position == statusRow) {
                    boolean enabled = prefs.getBoolean("enabled", false);
                    boolean running = TgWsProxyService.isRunning;
                    String value;
                    if (enabled && running) {
                        value = "Running on 127.0.0.1:" + prefs.getInt("port", DEFAULT_PORT);
                    } else if (enabled) {
                        value = "Starting…";
                    } else {
                        value = "Stopped";
                    }
                    cell.setTextAndValue("Status", value, true);

                } else if (position == pingRow) {
                    String value;
                    int colorKey;
                    if (!TgWsProxyService.isRunning) {
                        value    = "—";
                        colorKey = Theme.key_windowBackgroundWhiteGrayText2;
                    } else if (lastPingMs >= 0) {
                        value    = lastPingMs + " ms";
                        colorKey = lastPingMs < 100
                                ? Theme.key_windowBackgroundWhiteGreenText
                                : Theme.key_windowBackgroundWhiteRedText3;
                    } else {
                        value    = "Checking…";
                        colorKey = Theme.key_windowBackgroundWhiteGrayText2;
                    }
                    cell.setTextAndValue("Ping", value, true);
                    // Colour the value text
                    if (cell.getChildCount() > 1) {
                        android.view.View child = cell.getChildAt(1);
                        if (child instanceof android.widget.TextView) {
                            ((android.widget.TextView) child)
                                    .setTextColor(Theme.getColor(colorKey));
                        }
                    }

                } else if (position == secretRow) {
                    String s = prefs.getString("secret", "");
                    String display = s.isEmpty()
                            ? "Tap to set or generate"
                            : "••••" + s.substring(Math.max(0, s.length() - 6));
                    cell.setTextAndValue("Secret key", display, true);
                }

            } else if (vt == TYPE_TEXT) {
                TextCell cell = (TextCell) holder.itemView;
                if (position == portRow) {
                    cell.setTextAndValue("Local port",
                            String.valueOf(prefs.getInt("port", DEFAULT_PORT)), true);
                } else if (position == poolRow) {
                    cell.setTextAndValue("WS pool size",
                            String.valueOf(prefs.getInt("pool_size", DEFAULT_POOL_SIZE)), true);
                } else if (position == bufferRow) {
                    cell.setTextAndValue("Socket buffer",
                            prefs.getInt("buffer", DEFAULT_BUFFER) + " KB", true);
                } else if (position == fakeDomainRow) {
                    cell.setTextAndValue("FakeTLS domain",
                            prefs.getString("fake_domain", DEFAULT_FAKE_DOMAIN), true);
                } else if (position == cfDomainRow) {
                    String d = prefs.getString("cf_domain", DEFAULT_CF_DOMAIN);
                    cell.setTextAndValue("Custom CF domain",
                            d.isEmpty() ? "Auto" : d, true);
                } else if (position == dcIpsRow) {
                    String d = prefs.getString("dc_ips", DEFAULT_DC_IPS);
                    cell.setTextAndValue("DC IP overrides",
                            d.isEmpty() ? "None" : d, true);
                }

            } else if (vt == TYPE_INFO) {
                TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                if (position == infoRow) {
                    cell.setText(
                            "TG WS Proxy tunnels Telegram traffic through a WebSocket "
                            + "connection (optionally via Cloudflare CDN) to bypass "
                            + "throttling. The proxy is applied to Telegram automatically "
                            + "and paused when a VPN is active.");
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == connectionHeaderRow || position == securityHeaderRow
                    || position == cfHeaderRow   || position == advancedHeaderRow)
                return TYPE_HEADER;

            if (position == shadow1Row  || position == shadow2Row
                    || position == shadow3Row || position == shadow4Row
                    || position == shadow5Row)
                return TYPE_SHADOW;

            if (position == enableRow   || position == autoStartRow
                    || position == fakeTlsRow || position == cfEnabledRow)
                return TYPE_TEXT_CHECK;

            if (position == statusRow || position == pingRow || position == secretRow)
                return TYPE_TEXT_DETAIL;

            if (position == infoRow)
                return TYPE_INFO;

            return TYPE_TEXT;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent, int viewType) {
            android.view.View view;
            switch (viewType) {
                case TYPE_HEADER:      view = new HeaderCell(mContext);              break;
                case TYPE_TEXT_CHECK:  view = new TextCheckCell(mContext);           break;
                case TYPE_TEXT_DETAIL: view = new TextDetailSettingsCell(mContext);  break;
                case TYPE_SHADOW:      view = new ShadowSectionCell(mContext);       break;
                case TYPE_INFO:        view = new TextInfoPrivacyCell(mContext);     break;
                default:               view = new TextCell(mContext);                break;
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }
    }
}
