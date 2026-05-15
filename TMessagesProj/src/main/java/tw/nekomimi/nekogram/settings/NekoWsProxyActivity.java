package tw.nekomimi.nekogram.settings;

import static org.telegram.messenger.AndroidUtilities.getCacheDir;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import tw.nekomimi.nekogram.wsproxy.NativeProxy;
import tw.nekomimi.nekogram.wsproxy.TgWsProxyService;

public class NekoWsProxyActivity extends BaseNekoSettingsActivity implements NotificationCenter.NotificationCenterDelegate {

    private static final int TYPE_HEADER      = 0;
    private static final int TYPE_SHADOW      = 1;
    private static final int TYPE_TEXT_CHECK  = 2;
    private static final int TYPE_TEXT_DETAIL = 3;
    private static final int TYPE_TEXT        = 4;
    private static final int TYPE_INFO        = 5;
    private static final int TYPE_CONSOLE     = 6;

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
    private int statusRow, enableRow, pingRow, smartConfigRow, consoleRow,
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

    // Smart Config inline console
    private boolean isSmartConfigRunning = false;
    private final List<String[]> smartConfigLogs = new ArrayList<>(); // {label, pingText}
    private java.lang.ref.WeakReference<ConsoleCell> consoleCellRef;

    // Ping: -2 = checking, -1 = no data, >=0 = ms
    private volatile int lastPingMs = -1;

    private final Runnable pingRunnable = new Runnable() {
        @Override
        public void run() {
            checkProxyPingFromList();
            triggerProxyCheckIfNeeded();
            AndroidUtilities.runOnUIThread(() -> {
                if (listAdapter != null && pingRow >= 0) {
                    listAdapter.notifyItemChanged(pingRow);
                }
            });
            pingHandler.postDelayed(this, 3000);
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
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.proxyCheckDone);
        pingHandler.post(pingRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (statusReceiver != null) {
            getParentActivity().unregisterReceiver(statusReceiver);
            statusReceiver = null;
        }
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.proxyCheckDone);
        pingHandler.removeCallbacks(pingRunnable);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.proxyCheckDone) {
            AndroidUtilities.runOnUIThread(() -> {
                checkProxyPingFromList();
                if (listAdapter != null && pingRow >= 0) {
                    listAdapter.notifyItemChanged(pingRow);
                }
            });
        }
    }

    private void checkProxyPingFromList() {
        int port = getPrefs().getInt("port", DEFAULT_PORT);
        SharedConfig.ProxyInfo target = null;
        for (SharedConfig.ProxyInfo info : SharedConfig.proxyList) {
            if ("127.0.0.1".equals(info.address) && info.port == port) {
                target = info;
                break;
            }
        }
        if (target == null) {
            lastPingMs = -1;
            return;
        }
        if (target.checking) {
            lastPingMs = -2;
        } else if (target.available) {
            lastPingMs = Math.toIntExact(target.ping);
        } else {
            lastPingMs = -1;
        }
    }

    private void triggerProxyCheckIfNeeded() {
        int port = getPrefs().getInt("port", DEFAULT_PORT);
        for (SharedConfig.ProxyInfo info : SharedConfig.proxyList) {
            if ("127.0.0.1".equals(info.address) && info.port == port) {
                if (!info.checking && SystemClock.elapsedRealtime() - info.availableCheckTime > 5000) {
                    info.checking = true;
                    info.proxyCheckPingId = ConnectionsManager.getInstance(currentAccount).checkProxy(
                            info.address, info.port, info.username, info.password, info.secret,
                            time -> AndroidUtilities.runOnUIThread(() -> {
                                info.availableCheckTime = SystemClock.elapsedRealtime();
                                info.checking = false;
                                if (time == -1) {
                                    info.available = false;
                                    info.ping = 0;
                                } else {
                                    info.ping = time;
                                    info.available = true;
                                }
                                NotificationCenter.getGlobalInstance().postNotificationName(
                                        NotificationCenter.proxyCheckDone, info);
                            })
                    );
                }
                break;
            }
        }
    }

    @Override
    protected void updateRows() {
        super.updateRows();
        int row = 0;
        statusRow         = row++;
        enableRow         = row++;
        pingRow           = row++;
        smartConfigRow    = row++;
        consoleRow        = isSmartConfigRunning ? row++ : -1;
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
    // Smart Config — Full 336 combinations with progress & cancel
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

    // Heuristic: higher = test earlier (more likely to work)
    private int scoreLikelihood(TestConfig cfg, SharedPreferences prefs) {
        int score = 0;
        if (cfg.mode == prefs.getInt("mode", DEFAULT_MODE)) score += 10;
        if (cfg.fakeTls == prefs.getBoolean("fake_tls", DEFAULT_FAKE_TLS)) score += 10;
        if (cfg.cfEnabled == prefs.getBoolean("cf_enabled", DEFAULT_CF_ENABLED)) score += 10;
        if (cfg.poolSize == prefs.getInt("pool_size", DEFAULT_POOL_SIZE)) score += 5;
        if (cfg.buffer == prefs.getInt("buffer", DEFAULT_BUFFER)) score += 5;
        if (cfg.mode == 1) score += 4;
        if (cfg.cfEnabled) score += 3;
        if (cfg.fakeTls) score += 2;
        if (cfg.poolSize >= 4 && cfg.poolSize <= 8) score += 1;
        if (cfg.buffer >= 128 && cfg.buffer <= 256) score += 1;
        return score;
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

        // Show inline console panel
        smartConfigLogs.clear();
        isSmartConfigRunning = true;
        AndroidUtilities.runOnUIThread(this::updateRows);

        // Generate all 336 combinations
        List<TestConfig> allConfigs = new ArrayList<>();
        int[] modes = {0, 1, 2};
        boolean[] tlsOpts = {true, false};
        boolean[] cfOpts = {true, false};
        int[] pools = {2, 4, 6, 8, 10, 12, 16};
        int[] buffers = {64, 128, 256, 512};

        for (int m : modes) {
            for (boolean tls : tlsOpts) {
                for (boolean cf : cfOpts) {
                    for (int p : pools) {
                        for (int b : buffers) {
                            TestConfig cfg = new TestConfig();
                            cfg.mode = m; cfg.fakeTls = tls; cfg.cfEnabled = cf;
                            cfg.poolSize = p; cfg.buffer = b; cfg.port = wasPort;
                            allConfigs.add(cfg);
                        }
                    }
                }
            }
        }

        // Sort by likelihood
        Collections.sort(allConfigs, (a, b) -> Integer.compare(scoreLikelihood(b, prefs), scoreLikelihood(a, prefs)));

        final int total = allConfigs.size();
        final AtomicInteger progress = new AtomicInteger(0);
        final AtomicBoolean cancelled = new AtomicBoolean(false);

        // Progress UI — стандартный Android Dialog
        android.widget.LinearLayout ll = new android.widget.LinearLayout(ctx);
        ll.setOrientation(android.widget.LinearLayout.VERTICAL);
        ll.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(16), AndroidUtilities.dp(24), AndroidUtilities.dp(16));

        android.widget.TextView title = new android.widget.TextView(ctx);
        title.setText("Smart Config");
        title.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 20);
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        ll.addView(title);

        android.widget.TextView status = new android.widget.TextView(ctx);
        status.setText("Preparing…");
        status.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 14);
        status.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        status.setPadding(0, AndroidUtilities.dp(8), 0, 0);
        ll.addView(status);

        android.widget.ProgressBar bar = new android.widget.ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(total);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(4));
        lp.topMargin = AndroidUtilities.dp(12);
        ll.addView(bar, lp);

        // Cancel button
        android.widget.TextView cancelBtn = new android.widget.TextView(ctx);
        cancelBtn.setText("Cancel");
        cancelBtn.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 16);
        cancelBtn.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        cancelBtn.setPadding(0, AndroidUtilities.dp(16), 0, 0);
        cancelBtn.setGravity(Gravity.CENTER);
        ll.addView(cancelBtn);

        android.app.Dialog progressDialog = new android.app.Dialog(ctx, R.style.Theme_TMessages);
        progressDialog.setContentView(ll);
        progressDialog.setCancelable(false);
        progressDialog.show();

        cancelBtn.setOnClickListener(v -> {
            cancelled.set(true);
            progressDialog.dismiss();
        });

        // Background testing
        new Thread(() -> {
            List<TestResult> results = new ArrayList<>();

            for (TestConfig cfg : allConfigs) {
                if (cancelled.get()) break;

                int cur = progress.incrementAndGet();
                AndroidUtilities.runOnUIThread(() -> {
                    if (progressDialog.isShowing()) {
                        bar.setProgress(cur);
                        status.setText(String.format(Locale.US, "Testing %d/%d\n%s", cur, total, cfg.label()));
                    }
                });

                TestResult r = testConfigurationFast(cfg, wasPort, 1500);
                final String pingText;
                if (!r.portOpened) {
                    pingText = "—";
                } else if (r.pingMs >= 0) {
                    pingText = r.pingMs + " ms";
                } else {
                    pingText = "ok";
                }
                final String[] logEntry = {cfg.label(), pingText};
                AndroidUtilities.runOnUIThread(() -> {
                    smartConfigLogs.add(logEntry);
                    ConsoleCell cell = consoleCellRef != null ? consoleCellRef.get() : null;
                    if (cell != null) cell.addLogLine(logEntry[0], logEntry[1]);
                });
                if (r.portOpened) {
                    results.add(r);
                }
            }

            AndroidUtilities.runOnUIThread(() -> {
                if (progressDialog.isShowing()) progressDialog.dismiss();
                hideConsole();
            });

            if (cancelled.get()) {
                showToast("Smart Config cancelled");
                if (wasEnabled) {
                    AndroidUtilities.runOnUIThread(this::restartProxy);
                }
                return;
            }
            if (results.isEmpty()) {
                showToast("No working configuration found");
                if (wasEnabled) {
                    AndroidUtilities.runOnUIThread(this::restartProxy);
                }
                return;
            }

            Collections.sort(results, Comparator.comparingInt(TestResult::score));
            TestResult best = results.get(0);

            SharedPreferences.Editor ed = prefs.edit();
            best.config.apply(ed, wasPort);
            ed.commit();

            AndroidUtilities.runOnUIThread(() -> {
                updateRows();
                if (wasEnabled) {
                    restartProxy();
                    showToast(String.format(Locale.US, "Best: %s • ping=%dms", best.config.label(), best.pingMs));
                } else {
                    showToast(String.format(Locale.US, "Best config: %s • ping=%dms (enable to use)", best.config.label(), best.pingMs));
                }
            });
        }).start();
    }


    // Fast test via direct JNI calls (no Android Service overhead)
    private TestResult testConfigurationFast(TestConfig cfg, int port, int maxWaitMs) {
        TestResult result = new TestResult();
        result.config = cfg;

        // 1. Stop any running proxy
        NativeProxy.stopProxy();
        for (int i = 0; i < 8; i++) {
            if (!TgWsProxyService.isPortOpen("127.0.0.1", port, 100)) break;
            try { Thread.sleep(100); } catch (Exception ignored) {}
        }

        // 2. Apply params
        SharedPreferences prefs = getPrefs();
        SharedPreferences.Editor ed = prefs.edit();
        cfg.apply(ed, port);
        ed.commit();

        NativeProxy.setPoolSize(cfg.poolSize);
        NativeProxy.setRecvBuffer(cfg.buffer);
        NativeProxy.setSendBuffer(cfg.buffer);
        NativeProxy.setCfProxyCacheDir(getCacheDir().getAbsolutePath());
        NativeProxy.setCfProxyConfig(cfg.cfEnabled, true, prefs.getString("cf_domain", ""));
        NativeProxy.setFakeTls(cfg.fakeTls, prefs.getString("fake_domain", "google.com"));

        // 3. Start
        int startRes = NativeProxy.startProxy("127.0.0.1", port,
                prefs.getString("dc_ips", ""), prefs.getString("secret", ""), 0);
        if (startRes != 0) {
            NativeProxy.stopProxy();
            return result;
        }

        // 4. Wait for port (max 1.5s)
        long t0 = System.currentTimeMillis();
        boolean opened = false;
        for (int i = 0; i < 15; i++) {
            if (TgWsProxyService.isPortOpen("127.0.0.1", port, 200)) {
                opened = true;
                break;
            }
            try { Thread.sleep(100); } catch (Exception ignored) {}
            if (System.currentTimeMillis() - t0 > maxWaitMs) break;
        }

        result.portOpened = opened;
        result.testTimeMs = System.currentTimeMillis() - t0;

        if (opened) {
            int[] pings = new int[3];
            for (int i = 0; i < 3; i++) {
                long s = System.currentTimeMillis();
                if (TgWsProxyService.isPortOpen("127.0.0.1", port, 300)) {
                    pings[i] = (int)(System.currentTimeMillis() - s);
                } else {
                    pings[i] = -1;
                }
                try { Thread.sleep(100); } catch (Exception ignored) {}
            }
            java.util.Arrays.sort(pings);
            result.pingMs = (pings[1] >= 0) ? pings[1] : (pings[0] >= 0 ? pings[0] : -1);

            try {
                String st = NativeProxy.getStats();
                if (st != null && st.contains("err=")) {
                    int idx = st.indexOf("err=");
                    int end = st.indexOf(" ", idx);
                    if (end < 0) end = st.length();
                    result.errors = Integer.parseInt(st.substring(idx + 4, end));
                }
            } catch (Exception ignored) {}
        }

        // 5. Cleanup
        NativeProxy.stopProxy();
        for (int i = 0; i < 8; i++) {
            if (!TgWsProxyService.isPortOpen("127.0.0.1", port, 100)) break;
            try { Thread.sleep(100); } catch (Exception ignored) {}
        }
        return result;
    }

    private void hideConsole() {
        ConsoleCell cell = consoleCellRef != null ? consoleCellRef.get() : null;
        if (cell != null && cell.isAttachedToWindow()) {
            cell.animate()
                    .alpha(0f)
                    .translationY(-AndroidUtilities.dp(16))
                    .setDuration(250)
                    .withEndAction(() -> {
                        isSmartConfigRunning = false;
                        consoleCellRef = null;
                        updateRows();
                    })
                    .start();
        } else {
            isSmartConfigRunning = false;
            consoleCellRef = null;
            updateRows();
        }
    }

    private void showToast(String text) {
        AndroidUtilities.runOnUIThread(() -> {
            Context c = getParentActivity();
            if (c == null) return;
            android.widget.Toast.makeText(c, text, android.widget.Toast.LENGTH_LONG).show();
        });
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
    // ConsoleCell — inline Smart Config log panel
    // -------------------------------------------------------------------------

    private class ConsoleCell extends FrameLayout {
        private final android.widget.LinearLayout logContainer;
        private final android.widget.ScrollView   scrollView;
        private static final int   MAX_VISIBLE_LINES = 5;
        private static final float LINE_HEIGHT_DP    = 22f;

        ConsoleCell(Context context) {
            super(context);

            int padH = AndroidUtilities.dp(14);
            int padV = AndroidUtilities.dp(10);
            setPadding(padH, padV, padH, padV);

            // Terminal-style dark background with green border
            android.graphics.drawable.GradientDrawable bg =
                    new android.graphics.drawable.GradientDrawable();
            bg.setColor(0xFF0D1117);
            bg.setStroke(AndroidUtilities.dp(1), 0xFF238636);
            bg.setCornerRadius(AndroidUtilities.dp(8));
            setBackground(bg);

            logContainer = new android.widget.LinearLayout(context);
            logContainer.setOrientation(android.widget.LinearLayout.VERTICAL);

            scrollView = new android.widget.ScrollView(context);
            scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
            scrollView.setVerticalScrollBarEnabled(false);
            scrollView.addView(logContainer,
                    new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));

            int scrollH = AndroidUtilities.dp(MAX_VISIBLE_LINES * LINE_HEIGHT_DP);
            addView(scrollView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, scrollH));
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            // Slide-in animation when row is inserted
            setAlpha(0f);
            setTranslationY(-AndroidUtilities.dp(14));
            animate().alpha(1f).translationY(0f).setDuration(220).start();
        }

        /** Called live from the background test loop — adds one row at a time. */
        void addLogLine(String configLabel, String pingText) {
            logContainer.addView(buildRow(configLabel, pingText));
            scrollView.post(() -> scrollView.fullScroll(android.widget.ScrollView.FOCUS_DOWN));
        }

        /** Called from onBindViewHolder to restore full log state after RecyclerView rebind. */
        void rebindLogs(List<String[]> logs) {
            logContainer.removeAllViews();
            for (String[] entry : logs) {
                logContainer.addView(buildRow(entry[0], entry[1]));
            }
            if (!logs.isEmpty()) {
                scrollView.post(() -> scrollView.fullScroll(android.widget.ScrollView.FOCUS_DOWN));
            }
        }

        private android.widget.LinearLayout buildRow(String configLabel, String pingText) {
            Context ctx = getContext();
            int lineH = AndroidUtilities.dp(LINE_HEIGHT_DP);

            android.widget.LinearLayout row = new android.widget.LinearLayout(ctx);
            row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            row.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, lineH));

            // Config label — left, flexible width
            android.widget.TextView labelTv = new android.widget.TextView(ctx);
            labelTv.setText(configLabel);
            labelTv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 10);
            labelTv.setTextColor(0xFFADBBCC);
            labelTv.setTypeface(android.graphics.Typeface.MONOSPACE);
            labelTv.setSingleLine(true);
            labelTv.setEllipsize(android.text.TextUtils.TruncateAt.END);
            labelTv.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.addView(labelTv, new android.widget.LinearLayout.LayoutParams(
                    0, android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1f));

            // Ping — right, colored
            android.widget.TextView pingTv = new android.widget.TextView(ctx);
            pingTv.setText(pingText);
            pingTv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 10);
            pingTv.setTypeface(android.graphics.Typeface.MONOSPACE);
            pingTv.setGravity(android.view.Gravity.CENTER_VERTICAL);
            int pingColor;
            if ("—".equals(pingText) || "fail".equals(pingText)) {
                pingColor = 0xFFE05555;
            } else {
                try {
                    int ms = Integer.parseInt(pingText.replace(" ms", "").trim());
                    pingColor = ms < 100 ? 0xFF39D353 : ms < 300 ? 0xFFFFAA33 : 0xFFE05555;
                } catch (Exception ignored) {
                    pingColor = 0xFF39D353;
                }
            }
            pingTv.setTextColor(pingColor);
            android.widget.LinearLayout.LayoutParams pingLp =
                    new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT);
            pingLp.leftMargin = AndroidUtilities.dp(8);
            row.addView(pingTv, pingLp);

            return row;
        }
    }

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
                    int ping = lastPingMs;
                    String value;
                    int colorKey;
                    if (ping == -2) {
                        value = "Checking…";
                        colorKey = Theme.key_windowBackgroundWhiteGrayText2;
                    } else if (ping >= 0) {
                        value = ping + " ms";
                        colorKey = Theme.key_windowBackgroundWhiteGreenText;
                    } else {
                        value = "—";
                        colorKey = Theme.key_windowBackgroundWhiteGrayText2;
                    }
                    cell.setTextAndValue("Ping", value, true);
                    if (cell.getChildCount() > 1) {
                        View child = cell.getChildAt(1);
                        if (child instanceof android.widget.TextView) {
                            ((android.widget.TextView) child).setTextColor(Theme.getColor(colorKey));
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
            } else if (viewType == TYPE_CONSOLE) {
                ConsoleCell cell = (ConsoleCell) holder.itemView;
                consoleCellRef = new java.lang.ref.WeakReference<>(cell);
                cell.rebindLogs(smartConfigLogs);
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
            if (position == consoleRow) return TYPE_CONSOLE;
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
            else if (viewType == TYPE_CONSOLE)     view = new ConsoleCell(mContext);
            else                                   view = new TextCell(mContext);
            view.setLayoutParams(new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }
    }
}
