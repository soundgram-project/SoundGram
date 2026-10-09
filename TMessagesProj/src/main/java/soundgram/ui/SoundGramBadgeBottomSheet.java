package soundgram.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.Random;

public class SoundGramBadgeBottomSheet extends BottomSheet {

    public SoundGramBadgeBottomSheet(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context, false, resourcesProvider);

        boolean isDark = Theme.isCurrentThemeDark();

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(24));

        // ── Animated badge icon ──────────────────────────────────────────────
        BadgeIconView iconView = new BadgeIconView(context, resourcesProvider);
        root.addView(iconView, LayoutHelper.createLinear(
                160, 160, Gravity.CENTER_HORIZONTAL, 0, 16, 0, 4));

        // ── Title ────────────────────────────────────────────────────────────
        TextView title = new TextView(context);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
        title.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        title.setGravity(Gravity.CENTER);
        title.setText(tr("title"));
        root.addView(title, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                Gravity.CENTER_HORIZONTAL, 24, 0, 24, 6));

        // ── Subtitle ─────────────────────────────────────────────────────────
        TextView subtitle = new TextView(context);
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        subtitle.setTextColor(getThemedColor(Theme.key_dialogTextGray2));
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setLineSpacing(AndroidUtilities.dp(3), 1f);
        subtitle.setText(tr("subtitle"));
        root.addView(subtitle, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                Gravity.CENTER_HORIZONTAL, 24, 0, 24, 20));

        // ── Divider label "Перевод по СБП / МИР" ─────────────────────────────
        addSectionLabel(root, context, tr("section_mir"), isDark);

        // ── MIR Card ──────────────────────────────────────────────────────────
        View cardView = buildMirCard(context, isDark);
        root.addView(cardView, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 20, 6, 20, 20));

        // ── Divider label "Другие варианты" ──────────────────────────────────
        addSectionLabel(root, context, tr("section_other"), isDark);

        // ── CryptoBot button ─────────────────────────────────────────────────
        TextView cryptoBtn = new TextView(context);
        cryptoBtn.setGravity(Gravity.CENTER);
        cryptoBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        cryptoBtn.setTypeface(AndroidUtilities.bold());
        cryptoBtn.setTextColor(0xFFFFFFFF);
        cryptoBtn.setText(tr("btn_crypto"));
        cryptoBtn.setPadding(0, AndroidUtilities.dp(14), 0, AndroidUtilities.dp(14));
        GradientDrawable cryptoBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF7B61FF, 0xFF5B8DEF});
        cryptoBg.setCornerRadius(AndroidUtilities.dp(14));
        cryptoBtn.setBackground(cryptoBg);
        cryptoBtn.setOnClickListener(v -> {
            Browser.openUrl(getContext(), "http://t.me/send?start=IVC5XMsGUEgz");
            dismiss();
        });
        root.addView(cryptoBtn, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 20, 6, 20, 10));

        // ── Boosty button ────────────────────────────────────────────────────
        TextView boostyBtn = new TextView(context);
        boostyBtn.setGravity(Gravity.CENTER);
        boostyBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        boostyBtn.setTypeface(AndroidUtilities.bold());
        boostyBtn.setTextColor(0xFFFFFFFF);
        boostyBtn.setText(tr("btn_boosty"));
        boostyBtn.setPadding(0, AndroidUtilities.dp(14), 0, AndroidUtilities.dp(14));
        GradientDrawable boostyBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFFF15F2C, 0xFFFF8D3F});
        boostyBg.setCornerRadius(AndroidUtilities.dp(14));
        boostyBtn.setBackground(boostyBg);
        boostyBtn.setOnClickListener(v -> {
            Browser.openUrl(getContext(), "https://boosty.to/soundgram_project/donate");
            dismiss();
        });
        root.addView(boostyBtn, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 20, 0, 20, 12));

        // ── "Send receipt" button ─────────────────────────────────────────────
        TextView receiptBtn = new TextView(context);
        receiptBtn.setGravity(Gravity.CENTER);
        receiptBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        receiptBtn.setTypeface(AndroidUtilities.bold());
        receiptBtn.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueText));
        receiptBtn.setText(tr("btn_receipt"));
        receiptBtn.setOnClickListener(v -> {
            dismiss();
            long targetUserId = 1641176373L;
            org.telegram.ui.ActionBar.BaseFragment lastFragment = org.telegram.ui.LaunchActivity.getLastFragment();
            if (lastFragment != null) {
                android.os.Bundle args = new android.os.Bundle();
                args.putLong("user_id", targetUserId);
                lastFragment.presentFragment(new org.telegram.ui.ProfileActivity(args));
            } else {
                Browser.openUrl(getContext(), "tg://openmessage?user_id=" + targetUserId);
            }
        });
        root.addView(receiptBtn, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                Gravity.CENTER_HORIZONTAL, 24, 4, 24, 0));

        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(root);
        setCustomView(scrollView);
    }

    // ── Section label helper ──────────────────────────────────────────────────
    private void addSectionLabel(LinearLayout parent, Context ctx, String text, boolean isDark) {
        TextView label = new TextView(ctx);
        label.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        label.setTypeface(AndroidUtilities.bold());
        label.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        label.setText(text.toUpperCase());
        label.setLetterSpacing(0.08f);
        parent.addView(label, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24, 0, 24, 0));
    }

    // ── MIR Card ──────────────────────────────────────────────────────────────
    private View buildMirCard(Context ctx, boolean isDark) {
        FrameLayout card = new FrameLayout(ctx) {
            private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint shimmerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final RectF bounds = new RectF();
            private final Path clip = new Path();
            private float shimmerX = -1f;
            private final ValueAnimator shimmerAnim;
            private LinearGradient shimmerGradient;

            {
                shimmerAnim = ValueAnimator.ofFloat(-1f, 2f);
                shimmerAnim.setDuration(2200);
                shimmerAnim.setRepeatCount(ValueAnimator.INFINITE);
                shimmerAnim.setStartDelay(800);
                shimmerAnim.addUpdateListener(a -> {
                    shimmerX = (float) a.getAnimatedValue();
                    invalidate();
                });
                shimmerAnim.start();
            }

            @Override
            protected void onSizeChanged(int w, int h, int oldW, int oldH) {
                super.onSizeChanged(w, h, oldW, oldH);
                bounds.set(0, 0, w, h);
                clip.reset();
                clip.addRoundRect(bounds, AndroidUtilities.dp(18), AndroidUtilities.dp(18), Path.Direction.CW);

                // card gradient background
                bgPaint.setShader(new LinearGradient(0, 0, w, h,
                        isDark ? new int[]{0xFF1A1A2E, 0xFF16213E, 0xFF0F3460}
                               : new int[]{0xFF1B2A4A, 0xFF243B6B, 0xFF1A3A6E},
                        null, Shader.TileMode.CLAMP));

                shimmerGradient = new LinearGradient(0, 0, w, 0,
                        new int[]{0x00FFFFFF, 0x22FFFFFF, 0x00FFFFFF},
                        new float[]{0f, 0.5f, 1f},
                        Shader.TileMode.CLAMP);
            }

            @Override
            protected void dispatchDraw(Canvas canvas) {
                canvas.save();
                canvas.clipPath(clip);
                canvas.drawRoundRect(bounds, AndroidUtilities.dp(18), AndroidUtilities.dp(18), bgPaint);

                // shimmer
                if (shimmerGradient != null && shimmerX >= 0) {
                    android.graphics.Matrix m = new android.graphics.Matrix();
                    m.setTranslate(getWidth() * shimmerX, 0);
                    shimmerGradient.setLocalMatrix(m);
                    shimmerPaint.setShader(shimmerGradient);
                    canvas.drawRect(bounds, shimmerPaint);
                }

                super.dispatchDraw(canvas);
                canvas.restore();
            }

            @Override
            protected void onDetachedFromWindow() {
                super.onDetachedFromWindow();
                shimmerAnim.cancel();
            }
        };

        card.setPadding(
                AndroidUtilities.dp(20), AndroidUtilities.dp(18),
                AndroidUtilities.dp(20), AndroidUtilities.dp(18));
        card.setMinimumHeight(AndroidUtilities.dp(130));

        LinearLayout inner = new LinearLayout(ctx);
        inner.setOrientation(LinearLayout.VERTICAL);

        // Row 1: chip + MIR logo text
        LinearLayout row1 = new LinearLayout(ctx);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER_VERTICAL);

        View chip = new View(ctx);
        GradientDrawable chipBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFFFFD700, 0xFFC8860A});
        chipBg.setCornerRadius(AndroidUtilities.dp(4));
        chip.setBackground(chipBg);
        LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(
                AndroidUtilities.dp(36), AndroidUtilities.dp(26));
        row1.addView(chip, chipLp);

        View spacer = new View(ctx);
        row1.addView(spacer, new LinearLayout.LayoutParams(0, 0, 1f));

        TextView brand = new TextView(ctx);
        brand.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        brand.setTypeface(AndroidUtilities.bold());
        brand.setTextColor(0xFFFFFFFF);
        brand.setAlpha(0.85f);
        brand.setText("МИР");
        row1.addView(brand, LayoutHelper.createLinear(
                LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        inner.addView(row1, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 20));

        // Card number
        TextView number = new TextView(ctx);
        number.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        number.setTypeface(Typeface.create("monospace", Typeface.BOLD));
        number.setTextColor(0xFFFFFFFF);
        number.setLetterSpacing(0.12f);
        number.setText("2200 7019 9922 2661");
        inner.addView(number, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

        // Row 3: holder + hint
        LinearLayout row3 = new LinearLayout(ctx);
        row3.setOrientation(LinearLayout.HORIZONTAL);
        row3.setGravity(Gravity.BOTTOM);

        LinearLayout holderCol = new LinearLayout(ctx);
        holderCol.setOrientation(LinearLayout.VERTICAL);

        TextView holderLabel = new TextView(ctx);
        holderLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 9);
        holderLabel.setTextColor(0x99FFFFFF);
        holderLabel.setText(tr("holder_label"));
        holderLabel.setLetterSpacing(0.06f);
        holderCol.addView(holderLabel);

        TextView holderName = new TextView(ctx);
        holderName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        holderName.setTypeface(AndroidUtilities.bold());
        holderName.setTextColor(0xFFFFFFFF);
        holderName.setText("VLASOV SERGEY");
        holderCol.addView(holderName);

        row3.addView(holderCol, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView copyHint = new TextView(ctx);
        copyHint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
        copyHint.setTextColor(0xAAFFFFFF);
        copyHint.setText(tr("copy_hint"));
        row3.addView(copyHint, LayoutHelper.createLinear(
                LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT,
                Gravity.BOTTOM));

        inner.addView(row3, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        card.addView(inner, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        card.setOnClickListener(v -> {
            AndroidUtilities.addToClipboard("2200701999222661");
            Toast.makeText(ctx, tr("toast_copied"), Toast.LENGTH_SHORT).show();
        });

        return card;
    }

    private static String getLang() {
        try {
            if (LocaleController.getInstance() != null && LocaleController.getInstance().getCurrentLocaleInfo() != null) {
                String code = LocaleController.getInstance().getCurrentLocaleInfo().shortName;
                if (code != null && !code.isEmpty()) {
                    return code.toLowerCase(java.util.Locale.ROOT);
                }
            }
        } catch (Exception ignored) {}
        try {
            String iso = LocaleController.getSystemLocaleStringIso639();
            if (iso != null && !iso.isEmpty()) {
                return iso.toLowerCase(java.util.Locale.ROOT);
            }
        } catch (Exception ignored) {}
        return "en";
    }

    private static String tr(String key) {
        String lang = getLang();
        switch (key) {
            case "title":
                return "SoundGram Supporter";
            case "subtitle":
                if (lang.startsWith("ru")) return "Поддержите развитие SoundGram от 5$ — получите\nэксклюзивный значок и ранние релизы.";
                if (lang.startsWith("uk")) return "Підтримайте розвиток SoundGram від 5$ — отримайте\nексклюзивний значок та ранні релізи.";
                if (lang.startsWith("be")) return "Падтрымайце развіццё SoundGram ад 5$ — атрымайце\nэксклюзіўны значок і раннія рэлізы.";
                if (lang.startsWith("es")) return "Apoya el desarrollo de SoundGram desde 5$ — obtén una\ninsignia exclusiva y versiones anticipadas.";
                if (lang.startsWith("de")) return "Unterstütze die SoundGram-Entwicklung ab 5$ — erhalte ein\nexklusives Abzeichen und Vorabversionen.";
                if (lang.startsWith("fr")) return "Soutenez le développement de SoundGram dès 5$ — obtenez un\nbadge exclusif et les versions préliminaires.";
                if (lang.startsWith("it")) return "Supporta lo sviluppo di SoundGram da 5$ — ottieni un\nbadge esclusivo e le versioni in anteprima.";
                if (lang.startsWith("tr")) return "SoundGram gelişimini 5$'dan itibaren destekleyin — özel bir\nrozet ve erken sürümler kazanın.";
                if (lang.startsWith("uz")) return "SoundGram rivojlanishini 5$ dan qo'llab-quvvatlang — maxsus\nbelgi va erta nashrlarga ega bo'ling.";
                if (lang.startsWith("kk")) return "SoundGram дамуын 5$ бастап қолдаңыз — эксклюзивті\nбелгіше мен ерте шығарылымдарды алыңыз.";
                if (lang.startsWith("pt")) return "Apoie o desenvolvimento do SoundGram a partir de 5$ — ganhe um\nselo exclusivo e lançamentos antecipados.";
                if (lang.startsWith("ar")) return "ادعم تطوير SoundGram بدءاً من 5$ — واحصل على\nشارة حصرية وإصدارات مبكرة.";
                if (lang.startsWith("fa")) return "از توسعه SoundGram با حداقل ۵ دلار حمایت کنید — نشان\nاختصاصی و نسخه‌های اولیه را دریافت کنید.";
                return "Support SoundGram development from $5 — get an\nexclusive badge and early releases.";

            case "section_mir":
                if (lang.startsWith("ru")) return "Перевод по МИР (РФ, РБ)";
                if (lang.startsWith("uk")) return "Переказ на карту МИР (РФ, РБ)";
                if (lang.startsWith("be")) return "Перавод па МІР (РФ, РБ)";
                if (lang.startsWith("es")) return "Transferencia por tarjeta MIR (RU, BY)";
                if (lang.startsWith("de")) return "Überweisung per MIR-Karte (RU, BY)";
                if (lang.startsWith("fr")) return "Virement par carte MIR (RU, BY)";
                if (lang.startsWith("it")) return "Bonifico tramite carta MIR (RU, BY)";
                if (lang.startsWith("tr")) return "MIR kartı ile transfer (RU, BY)";
                if (lang.startsWith("uz")) return "MIR kartasi orqali o'tkazma (RU, BY)";
                if (lang.startsWith("kk")) return "МИР картасы арқылы аударым (RU, BY)";
                if (lang.startsWith("pt")) return "Transferência via cartão MIR (RU, BY)";
                if (lang.startsWith("ar")) return "التحويل عبر بطاقة MIR (روسيا، بيلاروس)";
                if (lang.startsWith("fa")) return "انتقال از طریق کارت MIR (روسیه، بلاروس)";
                return "Transfer via MIR card (RU, BY)";

            case "section_other":
                if (lang.startsWith("ru")) return "Для других стран";
                if (lang.startsWith("uk")) return "Для інших країн";
                if (lang.startsWith("be")) return "Для іншых краін";
                if (lang.startsWith("es")) return "Para otros países";
                if (lang.startsWith("de")) return "Andere Länder";
                if (lang.startsWith("fr")) return "Autres pays";
                if (lang.startsWith("it")) return "Altri paesi";
                if (lang.startsWith("tr")) return "Diğer ülkeler";
                if (lang.startsWith("uz")) return "Boshqa mamlakatlar uchun";
                if (lang.startsWith("kk")) return "Басқа елдер үшін";
                if (lang.startsWith("pt")) return "Outros países";
                if (lang.startsWith("ar")) return "لدول أخرى";
                if (lang.startsWith("fa")) return "برای سایر کشورها";
                return "Other countries";

            case "btn_crypto":
                if (lang.startsWith("ru")) return "Оплатить через CryptoBot";
                if (lang.startsWith("uk")) return "Оплатити через CryptoBot";
                if (lang.startsWith("be")) return "Аплаціць праз CryptoBot";
                if (lang.startsWith("es")) return "Pagar con CryptoBot";
                if (lang.startsWith("de")) return "Mit CryptoBot bezahlen";
                if (lang.startsWith("fr")) return "Payer via CryptoBot";
                if (lang.startsWith("it")) return "Paga tramite CryptoBot";
                if (lang.startsWith("tr")) return "CryptoBot ile Öde";
                if (lang.startsWith("uz")) return "CryptoBot orqali to'lash";
                if (lang.startsWith("kk")) return "CryptoBot арқылы төлеу";
                if (lang.startsWith("pt")) return "Pagar via CryptoBot";
                if (lang.startsWith("ar")) return "الدفع عبر CryptoBot";
                if (lang.startsWith("fa")) return "پرداخت از طریق CryptoBot";
                return "Pay via CryptoBot";

            case "btn_boosty":
                if (lang.startsWith("ru")) return "Поддержать через Boosty";
                if (lang.startsWith("uk")) return "Підтримати через Boosty";
                if (lang.startsWith("be")) return "Падтрымаць праз Boosty";
                if (lang.startsWith("es")) return "Apoyar en Boosty";
                if (lang.startsWith("de")) return "Über Boosty unterstützen";
                if (lang.startsWith("fr")) return "Soutenir via Boosty";
                if (lang.startsWith("it")) return "Supporta tramite Boosty";
                if (lang.startsWith("tr")) return "Boosty ile Destekle";
                if (lang.startsWith("uz")) return "Boosty orqali qo'llab-quvvatlash";
                if (lang.startsWith("kk")) return "Boosty арқылы қолдау";
                if (lang.startsWith("pt")) return "Apoiar via Boosty";
                if (lang.startsWith("ar")) return "الدعم عبر Boosty";
                if (lang.startsWith("fa")) return "حمایت از طریق Boosty";
                return "Support via Boosty";

            case "btn_receipt":
                if (lang.startsWith("ru")) return "Отправить чек об оплате →";
                if (lang.startsWith("uk")) return "Надіслати чек про оплату →";
                if (lang.startsWith("be")) return "Адправіць чэк аб аплаце →";
                if (lang.startsWith("es")) return "Enviar recibo de pago →";
                if (lang.startsWith("de")) return "Zahlungsbeleg senden →";
                if (lang.startsWith("fr")) return "Envoyer le reçu de paiement →";
                if (lang.startsWith("it")) return "Invia ricevuta di pagamento →";
                if (lang.startsWith("tr")) return "Ödeme makbuzunu gönder →";
                if (lang.startsWith("uz")) return "To'lov chekini yuborish →";
                if (lang.startsWith("kk")) return "Төлем чегін жіберу →";
                if (lang.startsWith("pt")) return "Enviar comprovante de pagamento →";
                if (lang.startsWith("ar")) return "إرسال إيصال الدفع ←";
                if (lang.startsWith("fa")) return "ارسال رسید پرداخت ←";
                return "Send payment receipt →";

            case "holder_label":
                if (lang.startsWith("ru")) return "ВЛАДЕЛЕЦ";
                if (lang.startsWith("uk")) return "ВЛАСНИК";
                if (lang.startsWith("be")) return "УЛАДАЛЬНІК";
                if (lang.startsWith("es")) return "TITULAR";
                if (lang.startsWith("de")) return "KARTENINHABER";
                if (lang.startsWith("fr")) return "TITULAIRE";
                if (lang.startsWith("it")) return "INTESTATARIO";
                if (lang.startsWith("tr")) return "KART SAHİBİ";
                if (lang.startsWith("uz")) return "KARTA EGASI";
                if (lang.startsWith("kk")) return "КАРТА ИЕСІ";
                if (lang.startsWith("pt")) return "TITULAR";
                if (lang.startsWith("ar")) return "صاحب البطاقة";
                if (lang.startsWith("fa")) return "دارنده کارت";
                return "CARDHOLDER";

            case "copy_hint":
                if (lang.startsWith("ru")) return "Нажмите для копирования";
                if (lang.startsWith("uk")) return "Натисніть для копіювання";
                if (lang.startsWith("be")) return "Націсніце для капіявання";
                if (lang.startsWith("es")) return "Toca para copiar";
                if (lang.startsWith("de")) return "Tippen zum Kopieren";
                if (lang.startsWith("fr")) return "Appuyez pour copier";
                if (lang.startsWith("it")) return "Tocca per copiare";
                if (lang.startsWith("tr")) return "Kopyalamak için dokunun";
                if (lang.startsWith("uz")) return "Nusxalash uchun bosing";
                if (lang.startsWith("kk")) return "Көшіру үшін басыңыз";
                if (lang.startsWith("pt")) return "Toque para copiar";
                if (lang.startsWith("ar")) return "اضغط للنسخ";
                if (lang.startsWith("fa")) return "برای کپی ضربه بزنید";
                return "Tap to copy";

            case "toast_copied":
                if (lang.startsWith("ru")) return "Номер карты скопирован";
                if (lang.startsWith("uk")) return "Номер картки скопійовано";
                if (lang.startsWith("be")) return "Нумар карткі скапіяваны";
                if (lang.startsWith("es")) return "Número de tarjeta copiado";
                if (lang.startsWith("de")) return "Kartennummer kopiert";
                if (lang.startsWith("fr")) return "Numéro de carte copié";
                if (lang.startsWith("it")) return "Numero di carta copiato";
                if (lang.startsWith("tr")) return "Kart numarası kopyalandı";
                if (lang.startsWith("uz")) return "Karta raqami nusxalandi";
                if (lang.startsWith("kk")) return "Карта нөмірі көшірілді";
                if (lang.startsWith("pt")) return "Número do cartão copiado";
                if (lang.startsWith("ar")) return "تم نسخ رقم البطاقة";
                if (lang.startsWith("fa")) return "شماره کارت کپی شد";
                return "Card number copied";
        }
        return "";
    }

    // ── Animated badge icon ────────────────────────────────────────────────────
    private static class BadgeIconView extends View {
        private final Drawable badge;
        private final android.graphics.Camera camera = new android.graphics.Camera();
        private final android.graphics.Matrix matrix = new android.graphics.Matrix();
        private final Theme.ResourcesProvider resourcesProvider;
        private float rotX, rotY;
        private float velX, velY;
        private float lastTX, lastTY;
        private boolean dragging;
        private long touchT;
        private float touchStartX, touchStartY;

        private final ArrayList<Particle> particles = new ArrayList<>();
        private final Random rng = new Random();
        private final Paint notePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private ValueAnimator anim;
        private boolean running;

        private final Runnable loop = new Runnable() {
            @Override public void run() {
                boolean more = false;
                if (!dragging) {
                    if (Math.abs(velX) > 0.05f || Math.abs(velY) > 0.05f) {
                        rotX += velX; rotY += velY;
                        velX *= 0.91f; velY *= 0.91f;
                        more = true;
                    }
                    if (Math.abs(rotX) > 0.05f) {
                        rotX += (0 - rotX) * 0.1f; more = true;
                    }
                }
                if (!particles.isEmpty()) { tickParticles(); more = true; }
                if (more) { invalidate(); postOnAnimation(this); }
                else running = false;
            }
        };

        BadgeIconView(Context ctx, Theme.ResourcesProvider resourcesProvider) {
            super(ctx);
            this.resourcesProvider = resourcesProvider; // Сохраняем ссылку

            badge = ContextCompat.getDrawable(ctx, R.drawable.ic_soundgram_badge);
            notePaint.setTextSize(AndroidUtilities.dp(22));
            notePaint.setTextAlign(Paint.Align.CENTER);

            // Spin in
            anim = ValueAnimator.ofFloat(0f, 360f);
            anim.setDuration(1100);
            anim.setInterpolator(new android.view.animation.DecelerateInterpolator());
            anim.addUpdateListener(a -> { rotY = (float) a.getAnimatedValue(); invalidate(); });
            anim.start();
            emit(5); startLoop();
        }

        private void startLoop() {
            if (!running) { running = true; postOnAnimation(loop); }
        }

        private void emit(int n) {
            for (int i = 0; i < n && particles.size() < 28; i++)
                particles.add(new Particle());
        }

        private void tickParticles() {
            for (int i = 0; i < particles.size(); i++) {
                Particle p = particles.get(i);
                p.x += p.vx; p.y += p.vy; p.alpha -= 0.008f;
                if (p.alpha <= 0) { particles.remove(i--); }
            }
        }

        private void spin() {
            if (anim != null) anim.cancel();
            emit(8);
            float base = rotY;
            anim = ValueAnimator.ofFloat(0f, 360f);
            anim.setDuration(950);
            anim.setInterpolator(new android.view.animation.DecelerateInterpolator());
            anim.addUpdateListener(a -> { rotY = base + (float) a.getAnimatedValue(); invalidate(); });
            anim.start(); startLoop();
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            float x = e.getX(), y = e.getY();
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchT = System.currentTimeMillis();
                    touchStartX = x; touchStartY = y;
                    dragging = true; lastTX = x; lastTY = y;
                    velX = velY = 0;
                    if (anim != null && anim.isRunning()) anim.cancel();
                    break;
                case MotionEvent.ACTION_MOVE:
                    float dx = x - lastTX, dy = y - lastTY;
                    rotY += dx * 0.5f; rotX -= dy * 0.5f;
                    velX = dx * 0.5f; velY = -dy * 0.5f;
                    lastTX = x; lastTY = y;
                    if (Math.abs(dx) > 2 || Math.abs(dy) > 2) emit(1);
                    invalidate(); startLoop();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    dragging = false;
                    long dur = System.currentTimeMillis() - touchT;
                    float dist = (float) Math.hypot(x - touchStartX, y - touchStartY);
                    if (dur < 250 && dist < AndroidUtilities.dp(8)) {
                        spin();
                    } else {
                        if (anim != null) anim.cancel();
                        float sx = rotX, sy = rotY;
                        anim = ValueAnimator.ofFloat(1f, 0f);
                        anim.setDuration(550);
                        anim.setInterpolator(new android.view.animation.OvershootInterpolator());
                        anim.addUpdateListener(a -> {
                            float v = (float) a.getAnimatedValue();
                            rotX = sx * v; rotY = sy * v; invalidate();
                        });
                        anim.start(); startLoop();
                    }
                    break;
            }
            return true;
        }

        @Override protected void onDraw(Canvas canvas) {
            int cx = getWidth() / 2, cy = getHeight() / 2;
            for (Particle p : particles) {
                notePaint.setAlpha((int)(255 * Math.max(0, p.alpha)));
                notePaint.setColor(p.color);
                canvas.drawText(p.note, cx + p.x, cy + p.y, notePaint);
            }
            canvas.save();
            camera.save();
            camera.rotateX(rotX); camera.rotateY(rotY);
            camera.getMatrix(matrix); camera.restore();
            matrix.preTranslate(-cx, -cy); matrix.postTranslate(cx, cy);
            canvas.concat(matrix);
            int s = AndroidUtilities.dp(144);
            if (badge != null) {
                badge.setBounds(cx - s/2, cy - s/2, cx + s/2, cy + s/2);
                badge.draw(canvas);
            }
            canvas.restore();
        }

        @Override protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            if (anim != null) anim.cancel();
        }

        private class Particle {
            float x, y, vx, vy, alpha = 1f;
            String note;
            int color;
            Particle() {
                String[] notes = {"♪","♫","♩","♬"};
                note = notes[rng.nextInt(notes.length)];
                x = (rng.nextFloat() - 0.5f) * AndroidUtilities.dp(36);
                y = (rng.nextFloat() - 0.5f) * AndroidUtilities.dp(36);
                vx = (rng.nextFloat() - 0.5f) * AndroidUtilities.dp(2.5f);
                vy = (rng.nextFloat() - 0.5f) * AndroidUtilities.dp(2.5f);
                int[] cols = {
                        Theme.getColor(Theme.key_windowBackgroundWhiteBlueText, resourcesProvider),
                        Theme.getColor(Theme.key_featuredStickers_addButton, resourcesProvider),
                        0xFFD4AF37, 0xFF7B61FF
                };
                color = cols[rng.nextInt(cols.length)];
            }
        }
    }
}
