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
import android.widget.FrameLayout;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import tw.nekomimi.nekogram.wsproxy.NativeProxy;
import tw.nekomimi.nekogram.wsproxy.TgWsProxyService;

public class NekoWsProxyActivity extends BaseNekoSettingsActivity {

    private static final int TYPE_HEADER      = 0;
    private static final int TYPE_SHADOW      = 1;
    private static final int TYPE_TEXT_CHECK  = 2;
    private static final int TYPE_TEXT_DETAIL = 3;
    private static final int TYPE_TEXT        = 4;
    private static final int TYPE_INFO        = 5;

    private static final int     DEFAULT_PORT        = 1443;
    private static final int     DEFAULT_MODE        = 1;
    private static final int     DEFAULT_POOL_SIZE   = 6;
    private static final int     DEFAULT_BUFFER      = 256;
    private static final boolean DEFAULT_FAKE_TLS    = true;
    private static final String  DEFAULT_FAKE_DOMAIN = "google.com";
    private static final boolean DEFAULT_CF_ENABLED  = true;
    private static final String  DEFAULT_CF_DOMAIN   = "";
    private static final int     DEFAULT_VERBOSE     = 0;
    private static final boolean DEFAULT_AUTO_START  = true;
    private static final String  DEFAULT_DC_IPS      = "";

    // Row indices
    private int statusRow, enableRow, pingRow, smartConfigRow,
                autoStartRow, advancedToggleRow, shadowAdvRow,
                connectionHeaderRow, portRow, modeRow, poolRow, bufferRow,
                shadow2Row,
                securityHeaderRow, secretRow, fakeTlsRow, fakeDomainRow,
                shadow3Row,
                cfHeaderRow, cfEnabledRow, cfDomainRow,
                shadow4Row, advancedHeaderRow,
                logLevelRow, dcIpsRow, shadow5Row,
                infoRow;

    private BroadcastReceiver statusReceiver;
    private final Handler pingHandler = new Handler(Looper.getMainLooper());

    // *** FIX: cache last ping value, updated from background thread ***
    private volatile int lastPingMs = -1;

    private final Runnable pingRunnable = new Runnable() {
        @Override
        public void run() {
            int port = getPrefs().getInt("port", DEFAULT_PORT);
            // Run the blocking socket check off the UI thread
            new Thread(() -> {
                int ping = TgWsProxyService.getPingMs(port);
                lastPingMs = ping;
                AndroidUtilities.runOnUIThread(() -> {
                    if (listAdapter != null && pingRow >= 0) {
                        listAdapter.notifyItemChanged(pingRow);
                    }
                });
            }, "proxy-ping").start();
            pingHandler.postDelayed(this, 2000);
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        statusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (listAdapter != null) listAdapter.notifyDataSetChanged();
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

    @Override
    protected void updateRows() {
        super.updateRows();
        int row = 0;
        statusRow         = row++;
        enableRow         = row++;
        pingRow           = row++;
        smartConfigRow    = row++;
        // *** copyLinkRow and actionRow REMOVED — auto-applied ***
        autoStartRow      = row++;
        advancedToggleRow = row++;
        shadowAdvRow      = row++;

        boolean showAdvanced = getPrefs().getBoolean("show_advanced", false);
        if (showAdvanced) {
            connectionHeaderRow = row++;
            portRow             = row++;
            modeRow             = row++;
            poolRow             = row++;
            bufferRow           = row++;
            shadow2Row          = row++;

            securityHeaderRow   = row++;
            secretRow           = row++;
            fakeTlsRow          = row++;
            if (getPrefs().getBoolean("fake_tls", DEFAULT_FAKE_TLS)) {
                fakeDomainRow   = row++;
            } else {
                fakeDomainRow   = -1;
            }
            shadow3Row          = row++;

            cfHeaderRow         = row++;
            cfEnabledRow        = row++;
            cfDomainRow         = row++;
            shadow4Row          = row++;

            logLevelRow         = row++;
            dcIpsRow            = row++;
            shadow5Row          = row++;
        } else {
            connectionHeaderRow = -1; portRow = -1; modeRow = -1; poolRow = -1; bufferRow = -1; shadow2Row = -1;
            securityHeaderRow = -1; secretRow = -1; fakeTlsRow = -1; fakeDomainRow = -1; shadow3Row = -1;
            cfHeaderRow = -1; cfEnabledRow = -1; cfDomainRow = -1; shadow4Row = -1;
            logLevelRow = -1; dcIpsRow = -1; shadow5Row = -1;
        }

        infoRow = row++;
        rowCount = row;

        if (listAdapter != null) listAdapter.notifyDataSetChanged();
    }

    @Override
    protected String getActionBarTitle() {
        return "TG WS Proxy";
    }

    @SuppressLint("ApplySharedPref")
    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position == enableRow) {
            toggleProxy();
        } else if (position == advancedToggleRow) {
            boolean show = !getPrefs().getBoolean("show_advanced", false);
            getPrefs().edit().putBoolean("show_advanced", show).commit();
            updateRows();
        } else if (position == portRow) {
            showInputDialog("Port",
                    String.valueOf(getPrefs().getInt("port", DEFAULT_PORT)),
                    InputType.TYPE_CLASS_NUMBER, v -> {
                        try {
                            int p = Integer.parseInt(v);
                            if (p > 1024 && p < 65536) {
                                getPrefs().edit().putInt("port", p).commit();
                                updateRows();
                                if (getPrefs().getBoolean("enabled", false)) restartProxy();
                            }
                        } catch (Exception ignored) {}
                    });
        } else if (position == modeRow) {
            String[] modes = {"Auto (CloudFlare)", "Direct (No CF)", "Manual DC"};
            int current = getPrefs().getInt("mode", DEFAULT_MODE);
            new android.app.AlertDialog.Builder(getParentActivity())
                    .setTitle("Connection Mode")
                    .setSingleChoiceItems(modes, current, (d, w) -> {
                        getPrefs().edit().putInt("mode", w).commit();
                        d.dismiss();
                        updateRows();
                        if (getPrefs().getBoolean("enabled", false)) restartProxy();
                    })
                    .show();
        } else if (position == poolRow) {
            String[] pools = {"2", "4", "6", "8", "10", "12", "16"};
            int current = getPrefs().getInt("pool_size", DEFAULT_POOL_SIZE);
            int idx = 2;
            for (int i = 0; i < pools.length; i++) {
                if (Integer.parseInt(pools[i]) == current) idx = i;
            }
            new android.app.AlertDialog.Builder(getParentActivity())
                    .setTitle("WS Pool Size")
                    .setSingleChoiceItems(pools, idx, (d, w) -> {
                        getPrefs().edit().putInt("pool_size",
                                Integer.parseInt(pools[w])).commit();
                        d.dismiss();
                        updateRows();
                        if (getPrefs().getBoolean("enabled", false)) restartProxy();
                    })
                    .show();
        } else if (position == bufferRow) {
            String[] bufs = {"64 KB", "128 KB", "256 KB", "512 KB"};
            int[] vals = {64, 128, 256, 512};
            int current = getPrefs().getInt("buffer", DEFAULT_BUFFER);
            int idx = 2;
            for (int i = 0; i < vals.length; i++) {
                if (vals[i] == current) idx = i;
            }
            new android.app.AlertDialog.Builder(getParentActivity())
                    .setTitle("Socket Buffer")
                    .setSingleChoiceItems(bufs, idx, (d, w) -> {
                        getPrefs().edit().putInt("buffer", vals[w]).commit();
                        d.dismiss();
                        updateRows();
                        if (getPrefs().getBoolean("enabled", false)) restartProxy();
                    })
                    .show();
        } else if (position == secretRow) {
            showInputDialog("Secret Key (32 hex chars)",
                    getPrefs().getString("secret", ""),
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS, v -> {
                        if (v.length() == 32) {
                            getPrefs().edit().putString("secret", v).commit();
                            updateRows();
                            if (getPrefs().getBoolean("enabled", false)) restartProxy();
                        }
                    });
        } else if (position == fakeTlsRow) {
            boolean on = !getPrefs().getBoolean("fake_tls", DEFAULT_FAKE_TLS);
            getPrefs().edit().putBoolean("fake_tls", on).commit();
            updateRows();
            if (getPrefs().getBoolean("enabled", false)) restartProxy();
        } else if (position == fakeDomainRow) {
            showInputDialog("FakeTLS Domain",
                    getPrefs().getString("fake_domain", DEFAULT_FAKE_DOMAIN),
                    InputType.TYPE_CLASS_TEXT, v -> {
                        getPrefs().edit().putString("fake_domain", v).commit();
                        updateRows();
                        if (getPrefs().getBoolean("enabled", false)) restartProxy();
                    });
        } else if (position == cfEnabledRow) {
            boolean on = !getPrefs().getBoolean("cf_enabled", DEFAULT_CF_ENABLED);
            getPrefs().edit().putBoolean("cf_enabled", on).commit();
            updateRows();
            if (getPrefs().getBoolean("enabled", false)) restartProxy();
        } else if (position == cfDomainRow) {
            showInputDialog("Custom CF Domain (optional)",
                    getPrefs().getString("cf_domain", DEFAULT_CF_DOMAIN),
                    InputType.TYPE_CLASS_TEXT, v -> {
                        getPrefs().edit().putString("cf_domain", v).commit();
                        updateRows();
                        if (getPrefs().getBoolean("enabled", false)) restartProxy();
                    });
        } else if (position == logLevelRow) {
            String[] levels = {"NULL (off)", "INFO", "ERROR", "DEBUG"};
            int current = getPrefs().getInt("verbose", DEFAULT_VERBOSE);
            new android.app.AlertDialog.Builder(getParentActivity())
                    .setTitle("Log Level")
                    .setSingleChoiceItems(levels, current, (d, w) -> {
                        getPrefs().edit().putInt("verbose", w).commit();
                        d.dismiss();
                        updateRows();
                        if (getPrefs().getBoolean("enabled", false)) restartProxy();
                    })
                    .show();
        } else if (position == autoStartRow) {
            boolean on = !getPrefs().getBoolean("auto_start", DEFAULT_AUTO_START);
            getPrefs().edit().putBoolean("auto_start", on).commit();
            updateRows();
        } else if (position == dcIpsRow) {
            showInputDialog("DC IPs (format: 2:IP,4:IP)",
                    getPrefs().getString("dc_ips", DEFAULT_DC_IPS),
                    InputType.TYPE_CLASS_TEXT, v -> {
                        getPrefs().edit().putString("dc_ips", v).commit();
                        updateRows();
                        if (getPrefs().getBoolean("enabled", false)) restartProxy();
                    });
        } else if (position == smartConfigRow) {
            runSmartConfig();
        }
    }

    private void toggleProxy() {
        boolean on = !getPrefs().getBoolean("enabled", false);
        getPrefs().edit().putBoolean("enabled", on).commit();

        Context ctx = getParentActivity();
        if (ctx == null) return;

        if (on) {
            String secret = getPrefs().getString("secret", "");
            if (secret.isEmpty()) {
                byte[] b = new byte[16];
                new SecureRandom().nextBytes(b);
                StringBuilder sb = new StringBuilder();
                for (byte bv : b) sb.append(String.format("%02x", bv));
                secret = sb.toString();
                getPrefs().edit().putString("secret", secret).commit();
            }
            startProxyService();
            // Auto-apply proxy to Telegram after proxy is ready
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
        intent.putExtra(TgWsProxyService.EXTRA_PORT,        p.getInt("port",        DEFAULT_PORT));
        intent.putExtra(TgWsProxyService.EXTRA_SECRET,      p.getString("secret",   ""));
        intent.putExtra(TgWsProxyService.EXTRA_POOL_SIZE,   p.getInt("pool_size",   DEFAULT_POOL_SIZE));
        intent.putExtra(TgWsProxyService.EXTRA_RECV_BUF,    p.getInt("buffer",      DEFAULT_BUFFER));
        intent.putExtra(TgWsProxyService.EXTRA_SEND_BUF,    p.getInt("buffer",      DEFAULT_BUFFER));
        intent.putExtra(TgWsProxyService.EXTRA_CF_ENABLED,  p.getBoolean("cf_enabled",  DEFAULT_CF_ENABLED));
        intent.putExtra(TgWsProxyService.EXTRA_CF_DOMAIN,   p.getString("cf_domain",    DEFAULT_CF_DOMAIN));
        intent.putExtra(TgWsProxyService.EXTRA_FAKE_TLS,    p.getBoolean("fake_tls",    DEFAULT_FAKE_TLS));
        intent.putExtra(TgWsProxyService.EXTRA_FAKE_DOMAIN, p.getString("fake_domain",  DEFAULT_FAKE_DOMAIN));
        intent.putExtra(TgWsProxyService.EXTRA_VERBOSE,     p.getInt("verbose",         DEFAULT_VERBOSE));
        intent.putExtra(TgWsProxyService.EXTRA_DC_IPS,      p.getString("dc_ips",       DEFAULT_DC_IPS));

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

    @SuppressLint("ApplySharedPref")
    private void autoApplyProxy() {
        int port = getPrefs().getInt("port", DEFAULT_PORT);
        String secret = getPrefs().getString("secret", "");
        if (secret.isEmpty()) return;

        String proxySecret = NativeProxy.getSecretWithPrefix();
        if (proxySecret == null) proxySecret = "dd" + secret;

        SharedConfig.ProxyInfo existingProxy = null;
        for (SharedConfig.ProxyInfo info : SharedConfig.proxyList) {
            if ("127.0.0.1".equals(info.address) && info.port == port) {
                existingProxy = info;
                break;
            }
        }

        SharedConfig.ProxyInfo proxyInfo;
        if (existingProxy != null) {
            existingProxy.secret = proxySecret;
            proxyInfo = existingProxy;
        } else {
            proxyInfo = new SharedConfig.ProxyInfo("127.0.0.1", port, "", "", proxySecret);
            SharedConfig.addProxy(proxyInfo);
        }

        SharedConfig.currentProxy = proxyInfo;
        SharedConfig.saveProxyList();

        android.content.SharedPreferences preferences =
                org.telegram.messenger.MessagesController.getGlobalMainSettings();
        preferences.edit()
                .putBoolean("proxy_enabled", true)
                .putString("proxy_ip",    "127.0.0.1")
                .putString("proxy_pass",  "")
                .putString("proxy_user",  "")
                .putInt("proxy_port",     port)
                .putString("proxy_secret", proxySecret)
                .commit();

        ConnectionsManager.setProxySettings(true, "127.0.0.1", port, "", "", proxySecret);
        org.telegram.messenger.NotificationCenter.getGlobalInstance().postNotificationName(
                org.telegram.messenger.NotificationCenter.proxySettingsChanged);
    }

    @SuppressLint("ApplySharedPref")
    private void disableTelegramProxy() {
        org.telegram.messenger.MessagesController.getGlobalMainSettings()
                .edit()
                .putBoolean("proxy_enabled", false)
                .commit();
        ConnectionsManager.setProxySettings(false, "", 0, "", "", "");
        org.telegram.messenger.NotificationCenter.getGlobalInstance().postNotificationName(
                org.telegram.messenger.NotificationCenter.proxySettingsChanged);
    }

    // -------------------------------------------------------------------------
    // Smart Config
    // -------------------------------------------------------------------------

    private static class TestConfig {
        int mode;
        boolean fakeTls;
        boolean cfEnabled;
        int poolSize;
        int buffer;
        int port;
        void apply(SharedPreferences.Editor ed, int port) {
            ed.putInt("mode", mode);
            ed.putBoolean("fake_tls", fakeTls);
            ed.putBoolean("cf_enabled", cfEnabled);
            ed.putInt("pool_size", poolSize);
            ed.putInt("buffer", buffer);
            ed.putInt("port", port);
        }
        String label() {
            return String.format(Locale.US, "mode=%d tls=%b cf=%b pool=%d buf=%d",
                    mode, fakeTls, cfEnabled, poolSize, buffer);
        }
    }

    private static class TestResult {
        TestConfig config;
        boolean portOpened;
        int pingMs = -1;
        int wsConnections = 0;
        int errors = 0;
        long testTimeMs;
        int score() {
            if (!portOpened) return Integer.MAX_VALUE;
            int s = pingMs < 0 ? 1000 : pingMs;
            s += errors * 100;
            s += (int)(testTimeMs / 10);
            return s;
        }
    }

    @SuppressLint("ApplySharedPref")
    private void runSmartConfig() {
        Context ctx = getParentActivity();
        if (ctx == null) return;

        final SharedPreferences prefs = getPrefs();
        final boolean wasEnabled = prefs.getBoolean("enabled", false);
        final int wasPort = prefs.getInt("port", DEFAULT_PORT);
        final String wasSecret = prefs.getString("secret", "");
        if (wasSecret.isEmpty()) {
            showToast("Set secret key first");
            return;
        }

        AlertDialog progressDialog = new AlertDialog(ctx, AlertDialog.ALERT_TYPE_SPINNER);
        progressDialog.setMessage("Smart Config: testing base modes…");
        progressDialog.setCanCancel(false);
        progressDialog.show();

        new Thread(() -> {
            List<TestResult> results = new ArrayList<>();

            int[] modes = {1, 0};
            boolean[] tlsOpts  = {true, false};
            boolean[] cfOpts   = {true, false};

            outer:
            for (int m : modes) {
                for (boolean tls : tlsOpts) {
                    for (boolean cf : cfOpts) {
                        if (isCancelled(progressDialog)) break outer;
                        TestConfig cfg = new TestConfig();
                        cfg.mode = m; cfg.fakeTls = tls; cfg.cfEnabled = cf;
                        cfg.poolSize = DEFAULT_POOL_SIZE; cfg.buffer = DEFAULT_BUFFER; cfg.port = wasPort;
                        AndroidUtilities.runOnUIThread(() ->
                                progressDialog.setMessage("Testing: " + cfg.label()));
                        results.add(testConfiguration(cfg, wasPort, 2500));
                    }
                }
            }

            if (results.isEmpty()) {
                AndroidUtilities.runOnUIThread(() -> {
                    progressDialog.dismiss();
                    showToast("No working configuration found");
                });
                return;
            }

            Collections.sort(results, Comparator.comparingInt(TestResult::score));
            TestResult bestBase = results.get(0);
            if (!bestBase.portOpened) {
                AndroidUtilities.runOnUIThread(() -> {
                    progressDialog.dismiss();
                    showToast("No working base configuration found");
                });
                return;
            }

            List<TestResult> fineResults = new ArrayList<>();
            int[] pools   = {2, 4, 6, 8, 12, 16};
            int[] buffers = {64, 128, 256, 512};

            outer2:
            for (int p : pools) {
                for (int b : buffers) {
                    if (isCancelled(progressDialog)) break outer2;
                    TestConfig cfg = new TestConfig();
                    cfg.mode = bestBase.config.mode;
                    cfg.fakeTls = bestBase.config.fakeTls;
                    cfg.cfEnabled = bestBase.config.cfEnabled;
                    cfg.poolSize = p; cfg.buffer = b; cfg.port = wasPort;
                    AndroidUtilities.runOnUIThread(() ->
                            progressDialog.setMessage("Tuning: pool=" + p + " buf=" + b + " KB"));
                    fineResults.add(testConfiguration(cfg, wasPort, 2000));
                }
            }

            Collections.sort(fineResults, Comparator.comparingInt(TestResult::score));
            TestResult best = fineResults.isEmpty() ? bestBase : fineResults.get(0);

            SharedPreferences.Editor ed = prefs.edit();
            best.config.apply(ed, wasPort);
            ed.commit();

            if (wasEnabled) {
                AndroidUtilities.runOnUIThread(() -> {
                    progressDialog.dismiss();
                    restartProxy();
                    updateRows();
                    showToast(String.format(Locale.US,
                            "Best: %s ping=%dms", best.config.label(), best.pingMs));
                });
            } else {
                AndroidUtilities.runOnUIThread(() -> {
                    progressDialog.dismiss();
                    updateRows();
                    showToast(String.format(Locale.US,
                            "Best config found: %s ping=%dms (enable to use)",
                            best.config.label(), best.pingMs));
                });
            }
        }).start();
    }

    private boolean isCancelled(AlertDialog dialog) {
        return dialog == null || !dialog.isShowing();
    }

    private void showToast(String text) {
        AndroidUtilities.runOnUIThread(() -> {
            Context c = getParentActivity();
            if (c == null) return;
            android.widget.Toast.makeText(c, text, android.widget.Toast.LENGTH_LONG).show();
        });
    }

    /**
     * Test one configuration — runs on a background thread (called from SmartConfig thread).
     * *** FIX: wait for stop to complete before sending START to avoid main thread blocking ***
     */
    private TestResult testConfiguration(TestConfig cfg, int port, int maxWaitMs) {
        TestResult result = new TestResult();
        result.config = cfg;
        Context ctx = ApplicationLoader.applicationContext;

        // Stop current proxy
        ctx.startService(new Intent(ctx, TgWsProxyService.class)
                .setAction(TgWsProxyService.ACTION_STOP));

        // *** FIX: wait for both isRunning=false AND stopInProgress=false ***
        for (int i = 0; i < 40; i++) {
            if (!TgWsProxyService.isRunning && !TgWsProxyService.isStopInProgress()) break;
            try { Thread.sleep(150); } catch (Exception ignored) {}
        }
        try { Thread.sleep(200); } catch (Exception ignored) {}

        SharedPreferences prefs = getPrefs();
        SharedPreferences.Editor ed = prefs.edit();
        cfg.apply(ed, port);
        ed.commit();

        Intent intent = new Intent(ctx, TgWsProxyService.class);
        intent.setAction(TgWsProxyService.ACTION_START);
        intent.putExtra(TgWsProxyService.EXTRA_PORT,        port);
        intent.putExtra(TgWsProxyService.EXTRA_SECRET,      prefs.getString("secret", ""));
        intent.putExtra(TgWsProxyService.EXTRA_POOL_SIZE,   cfg.poolSize);
        intent.putExtra(TgWsProxyService.EXTRA_RECV_BUF,    cfg.buffer);
        intent.putExtra(TgWsProxyService.EXTRA_SEND_BUF,    cfg.buffer);
        intent.putExtra(TgWsProxyService.EXTRA_CF_ENABLED,  cfg.cfEnabled);
        intent.putExtra(TgWsProxyService.EXTRA_CF_DOMAIN,   prefs.getString("cf_domain", ""));
        intent.putExtra(TgWsProxyService.EXTRA_FAKE_TLS,    cfg.fakeTls);
        intent.putExtra(TgWsProxyService.EXTRA_FAKE_DOMAIN, prefs.getString("fake_domain", "google.com"));
        intent.putExtra(TgWsProxyService.EXTRA_VERBOSE,     0);
        intent.putExtra(TgWsProxyService.EXTRA_DC_IPS,      prefs.getString("dc_ips", ""));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(intent);
        } else {
            ctx.startService(intent);
        }

        long startTime = System.currentTimeMillis();
        boolean opened = false;
        for (int i = 0; i < 15; i++) {
            if (TgWsProxyService.isPortOpen("127.0.0.1", port, 500)) {
                opened = true;
                break;
            }
            try { Thread.sleep(150); } catch (Exception ignored) {}
            if (System.currentTimeMillis() - startTime > maxWaitMs) break;
        }

        result.portOpened = opened;
        result.testTimeMs = System.currentTimeMillis() - startTime;

        if (opened) {
            int[] pings = new int[3];
            for (int i = 0; i < 3; i++) {
                pings[i] = TgWsProxyService.getPingMs(port);
                try { Thread.sleep(150); } catch (Exception ignored) {}
            }
            java.util.Arrays.sort(pings);
            result.pingMs = pings[1];

            try {
                String stats = NativeProxy.getStats();
                if (stats != null) {
                    if (stats.contains("ws=")) {
                        int idx = stats.indexOf("ws=");
                        int end = stats.indexOf(" ", idx);
                        if (end < 0) end = stats.length();
                        try { result.wsConnections = Integer.parseInt(stats.substring(idx + 3, end)); }
                        catch (Exception ignored) {}
                    }
                    if (stats.contains("err=")) {
                        int idx = stats.indexOf("err=");
                        int end = stats.indexOf(" ", idx);
                        if (end < 0) end = stats.length();
                        try { result.errors = Integer.parseInt(stats.substring(idx + 4, end)); }
                        catch (Exception ignored) {}
                    }
                }
            } catch (Exception ignored) {}
            try { Thread.sleep(500); } catch (Exception ignored) {}
        }

        ctx.startService(new Intent(ctx, TgWsProxyService.class)
                .setAction(TgWsProxyService.ACTION_STOP));
        // Wait for stop before next iteration
        for (int i = 0; i < 30; i++) {
            if (!TgWsProxyService.isRunning && !TgWsProxyService.isStopInProgress()) break;
            try { Thread.sleep(150); } catch (Exception ignored) {}
        }
        try { Thread.sleep(200); } catch (Exception ignored) {}

        return result;
    }

    // -------------------------------------------------------------------------
    // Prefs
    // -------------------------------------------------------------------------

    private SharedPreferences getPrefs() {
        SharedPreferences p = ApplicationLoader.applicationContext
                .getSharedPreferences("tgwsproxy", Context.MODE_PRIVATE);
        if (!p.contains("defaults_applied")) {
            p.edit()
                    .putInt    ("port",           DEFAULT_PORT)
                    .putInt    ("mode",           DEFAULT_MODE)
                    .putInt    ("pool_size",       DEFAULT_POOL_SIZE)
                    .putInt    ("buffer",          DEFAULT_BUFFER)
                    .putBoolean("fake_tls",        DEFAULT_FAKE_TLS)
                    .putString ("fake_domain",     DEFAULT_FAKE_DOMAIN)
                    .putBoolean("cf_enabled",      DEFAULT_CF_ENABLED)
                    .putString ("cf_domain",       DEFAULT_CF_DOMAIN)
                    .putInt    ("verbose",         DEFAULT_VERBOSE)
                    .putBoolean("auto_start",      DEFAULT_AUTO_START)
                    .putString ("dc_ips",          DEFAULT_DC_IPS)
                    .putBoolean("show_advanced",   false)
                    .putBoolean("defaults_applied", true)
                    .apply();
        }
        return p;
    }

    // -------------------------------------------------------------------------
    // Input dialog
    // -------------------------------------------------------------------------

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
                0, 16, 8, 16, 8));
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

    // -------------------------------------------------------------------------
    // Adapter
    // -------------------------------------------------------------------------

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    private class ListAdapter extends BaseListAdapter {
        public ListAdapter(Context context) { super(context); }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder,
                int position, boolean partial) {
            int viewType = holder.getItemViewType();
            SharedPreferences prefs = getPrefs();

            if (viewType == TYPE_HEADER) {
                HeaderCell cell = (HeaderCell) holder.itemView;
                if      (position == connectionHeaderRow) cell.setText("Connection");
                else if (position == securityHeaderRow)   cell.setText("Security");
                else if (position == cfHeaderRow)         cell.setText("CloudFlare");
                else if (position == advancedHeaderRow)   cell.setText("Advanced");

            } else if (viewType == TYPE_SHADOW) {
                holder.itemView.setBackground(Theme.getThemedDrawable(
                        mContext, R.drawable.greydivider,
                        Theme.key_windowBackgroundGrayShadow));

            } else if (viewType == TYPE_TEXT_CHECK) {
                TextCheckCell cell = (TextCheckCell) holder.itemView;
                if (position == enableRow) {
                    cell.setTextAndCheck("Enable TG WS Proxy",
                            prefs.getBoolean("enabled", false), true);
                } else if (position == fakeTlsRow) {
                    cell.setTextAndCheck("FakeTLS (ee-secret)",
                            prefs.getBoolean("fake_tls", DEFAULT_FAKE_TLS),
                            fakeDomainRow != -1);
                } else if (position == cfEnabledRow) {
                    cell.setTextAndCheck("CloudFlare CDN",
                            prefs.getBoolean("cf_enabled", DEFAULT_CF_ENABLED), true);
                } else if (position == autoStartRow) {
                    cell.setTextAndCheck("Auto-start on boot",
                            prefs.getBoolean("auto_start", DEFAULT_AUTO_START), false);
                }

            } else if (viewType == TYPE_TEXT_DETAIL) {
                TextDetailSettingsCell cell = (TextDetailSettingsCell) holder.itemView;
                if (position == statusRow) {
                    boolean enabled = prefs.getBoolean("enabled", false);
                    boolean running = TgWsProxyService.isRunning;
                    if (enabled && running) {
                        cell.setTextAndValue("Status",
                                "Running on 127.0.0.1:" + prefs.getInt("port", DEFAULT_PORT), true);
                    } else if (enabled) {
                        cell.setTextAndValue("Status", "Starting…", true);
                    } else {
                        cell.setTextAndValue("Status", "Stopped", true);
                    }
                } else if (position == secretRow) {
                    String s = prefs.getString("secret", "");
                    cell.setTextAndValue("Secret Key",
                            s.isEmpty() ? "Not set (auto-generated)"
                                        : "****" + s.substring(Math.max(0, s.length() - 4)),
                            true);
                } else if (position == pingRow) {
                    // *** FIX: use cached value — set by background thread ***
                    int ping = lastPingMs;
                    String value = ping >= 0 ? ping + " ms" : "—";
                    cell.setTextAndValue("Ping", value, true);
                    if (cell.getChildCount() > 1) {
                        View child = cell.getChildAt(1);
                        if (child instanceof android.widget.TextView) {
                            int colorKey = ping >= 0
                                    ? Theme.key_windowBackgroundWhiteGreenText
                                    : Theme.key_windowBackgroundWhiteGrayText2;
                            ((android.widget.TextView) child)
                                    .setTextColor(Theme.getColor(colorKey));
                        }
                    }
                }

            } else if (viewType == TYPE_TEXT) {
                TextCell cell = (TextCell) holder.itemView;
                if (position == portRow) {
                    cell.setTextAndValue("Port",
                            String.valueOf(prefs.getInt("port", DEFAULT_PORT)), true);
                } else if (position == modeRow) {
                    String[] modes = {"Auto (CF)", "Direct", "Manual DC"};
                    cell.setTextAndValue("Mode",
                            modes[prefs.getInt("mode", DEFAULT_MODE)], true);
                } else if (position == poolRow) {
                    cell.setTextAndValue("WS Pool Size",
                            String.valueOf(prefs.getInt("pool_size", DEFAULT_POOL_SIZE)), true);
                } else if (position == bufferRow) {
                    cell.setTextAndValue("Buffer Size",
                            prefs.getInt("buffer", DEFAULT_BUFFER) + " KB", true);
                } else if (position == fakeDomainRow) {
                    cell.setTextAndValue("FakeTLS Domain",
                            prefs.getString("fake_domain", DEFAULT_FAKE_DOMAIN), true);
                } else if (position == cfDomainRow) {
                    String d = prefs.getString("cf_domain", DEFAULT_CF_DOMAIN);
                    cell.setTextAndValue("Custom CF Domain",
                            d.isEmpty() ? "Auto" : d, true);
                } else if (position == logLevelRow) {
                    String[] levels = {"NULL", "INFO", "ERROR", "DEBUG"};
                    cell.setTextAndValue("Log Level",
                            levels[prefs.getInt("verbose", DEFAULT_VERBOSE)], true);
                } else if (position == dcIpsRow) {
                    String d = prefs.getString("dc_ips", DEFAULT_DC_IPS);
                    cell.setTextAndValue("DC IPs", d.isEmpty() ? "Auto" : d, true);
                } else if (position == advancedToggleRow) {
                    boolean show = prefs.getBoolean("show_advanced", false);
                    cell.setTextAndValue("Advanced Settings", show ? "▲" : "▼", true);
                    cell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
                } else if (position == smartConfigRow) {
                    cell.setText("Smart Config (Auto-Tune)", true);
                    cell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
                }

            } else if (viewType == TYPE_INFO) {
                TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                if (position == infoRow) {
                    cell.setText("TG WS Proxy connects to Telegram via WebSocket to bypass "
                            + "blocking. Proxy is applied automatically. Use Smart Config to "
                            + "find the fastest working settings. Disables automatically when "
                            + "a VPN is active.");
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == statusRow || position == secretRow || position == pingRow)
                return TYPE_TEXT_DETAIL;
            if (position == enableRow || position == fakeTlsRow
                    || position == cfEnabledRow || position == autoStartRow)
                return TYPE_TEXT_CHECK;
            if (position == connectionHeaderRow || position == securityHeaderRow
                    || position == cfHeaderRow || position == advancedHeaderRow)
                return TYPE_HEADER;
            if (position == shadowAdvRow || position == shadow2Row || position == shadow3Row
                    || position == shadow4Row || position == shadow5Row)
                return TYPE_SHADOW;
            if (position == infoRow) return TYPE_INFO;
            return TYPE_TEXT;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent, int viewType) {
            View view;
            if      (viewType == TYPE_HEADER)      view = new HeaderCell(mContext);
            else if (viewType == TYPE_TEXT_CHECK)  view = new TextCheckCell(mContext);
            else if (viewType == TYPE_TEXT_DETAIL) view = new TextDetailSettingsCell(mContext);
            else if (viewType == TYPE_SHADOW)      view = new ShadowSectionCell(mContext);
            else if (viewType == TYPE_INFO)        view = new TextInfoPrivacyCell(mContext);
            else                                   view = new TextCell(mContext);
            view.setLayoutParams(new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }
    }
}
