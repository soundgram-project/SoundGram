# -*- coding: utf-8 -*-
"""
Generate complete translations for TR, AR, FA, IW, AZ, UZ for all 460 keys.
"""
import json, os

with open("tools/missing_460_en_ru.json", "r", encoding="utf-8") as f:
    base = json.load(f)

en_dict = base["en"]
ru_dict = base["ru"]

with open("tools/existing_translations.json", "r", encoding="utf-8") as f:
    existing = json.load(f)

# Vocabulary mapping: key -> dict of {tr, ar, fa, iw, az, uz}
ME_MAP = {
    "AP_CenterTitle": {
        "tr": "Ortalanmış başlık", "ar": "عنوان متوسط", "fa": "عنوان در وسط",
        "iw": "כותרת ממורכזת", "az": "Mərkəzləşdirilmiş başlıq", "uz": "Markazlashtirilgan sarlavha"
    },
    "AP_DisableDividers": {
        "tr": "Ayırıcıları gizle", "ar": "إخفاء الفواصل", "fa": "پنهان کردن جداکننده‌ها",
        "iw": "הסתר מפרידים", "az": "Ayırıcıları gizlət", "uz": "Ajratuvchilarni yashirish"
    },
    "AP_GlareOnElements": {
        "tr": "Öğelerdeki parlama", "ar": "الوهج على العناصر", "fa": "درخشش روی عناصر",
        "iw": "השתקפות על אלמנטים", "az": "Elementlərdə parıltı", "uz": "Elementlardagi chaqnash"
    },
    "AP_GlareOnElementsInfo": {
        "tr": "Sıvı cam öğelerinde üst ve alt vurguları göster.",
        "ar": "إظهار الإبرازات العلوية والسفلية على عناصر الزجاج السائل.",
        "fa": "نمایش بازتاب‌های بالا و پایین در عناصر شیشه مایع.",
        "iw": "הצג השתקפויות עליונות ותחתונות על אלמנטי זכוכית נוזלית.",
        "az": "Maye şüşə elementlərində yuxarı və aşağı parıltıları göstər.",
        "uz": "Suyuq shisha elementlarida yuqori va pastki chaqnashlarni ko‘rsatish."
    },
    "AP_Header": {
        "tr": "Başlık", "ar": "الرأس", "fa": "سربرگ",
        "iw": "כותרת עליונה", "az": "Başlıq", "uz": "Sarlavha"
    },
    "AP_Header_Appearance": {
        "tr": "Görünüm", "ar": "المظهر", "fa": "ظاهر",
        "iw": "מראה", "az": "Görünüş", "uz": "Ko‘rinish"
    },
    "AP_HideSearchBar": {
        "tr": "Sohbet arama çubuğunu gizle", "ar": "إخفاء شريط بحث المحادثات", "fa": "پنهان کردن نوار جستجوی گفتگوها",
        "iw": "הסתר שורת חיפוש צ\'אטים", "az": "Çat axtarış panelini gizlət", "uz": "Chat qidiruv panelini yashirish"
    },
    "AP_IconReplacements": {
        "tr": "Simge paketi", "ar": "حزمة الأيقونات", "fa": "بسته آیکون",
        "iw": "חבילת אייקונים", "az": "İkon paketi", "uz": "Belgilar to‘plami"
    },
    "AP_ShowPencilIcon": {
        "tr": "Düzenlenen mesajları işaretle", "ar": "تمييز الرسائل المعدلة", "fa": "علامت‌گذاری پیام‌های ویرایش شده",
        "iw": "סמן הודעות שנערכו", "az": "Düzəliş edilmiş mesajları işarələ", "uz": "Tahrirlangan xabarlarni belgilash"
    },
    "AP_SystemEmoji": {
        "tr": "Sistem emojileri", "ar": "رموز تعبيرية للنظام", "fa": "اموجی‌های سیستم",
        "iw": "אימוג\'י מערכת", "az": "Sistem emojiləri", "uz": "Tizim emojilari"
    },
    "AP_SystemFonts": {
        "tr": "Sistem yazı tipleri", "ar": "خطوط النظام", "fa": "فونت‌های سیستم",
        "iw": "גופני מערכת", "az": "Sistem şriftləri", "uz": "Tizim shriftlari"
    },
    "AP_Tablet_Mode": {
        "tr": "Tablet modu", "ar": "وضع الجهاز اللوحي", "fa": "حالت تبلت",
        "iw": "מצב טאבלט", "az": "Planşet rejimi", "uz": "Planshet rejimi"
    },
    "AccActionPause": {
        "tr": "Duraklat", "ar": "إيقاف مؤقت", "fa": "مکث",
        "iw": "השהה", "az": "Fasilə", "uz": "Pauza"
    },
    "AccActionPlay": {
        "tr": "Oynat", "ar": "تشغيل", "fa": "پخش",
        "iw": "נגן", "az": "Oynat", "uz": "Ijro etish"
    },
    "AccDescrCustomEmoji": {
        "tr": "Özel emojiler", "ar": "رموز مخصصة", "fa": "اموجی سفارشی",
        "iw": "אימוג\'י מותאם", "az": "Fərdi emojilər", "uz": "Maxsus emojilar"
    },
    "AccDescrGIFs": {
        "tr": "GIF\'ler", "ar": "صور متحركة", "fa": "گیف‌ها",
        "iw": "גיפים", "az": "GIF-lər", "uz": "GIFlar"
    },
    "AccDescrStickers": {
        "tr": "Çıkartmalar", "ar": "ملصقات", "fa": "استیکرها",
        "iw": "מדבקות", "az": "Stikerlər", "uz": "Stikerlar"
    },
    "ActionsChartTitle": {
        "tr": "Eylemler", "ar": "الإجراءات", "fa": "اقدامات",
        "iw": "פעולות", "az": "Fəaliyyətlər", "uz": "Harakatlar"
    },
    "AppName": {
        "tr": "SoundGram", "ar": "SoundGram", "fa": "SoundGram",
        "iw": "SoundGram", "az": "SoundGram", "uz": "SoundGram"
    },
    "AreYouSure": {
        "tr": "Emin misiniz?", "ar": "هل أنت متأكد؟", "fa": "آیا مطمئن هستید؟",
        "iw": "האם אתה בטוח?", "az": "Əminsiniz?", "uz": "Ishonchingiz komilmi?"
    },
    "AvatarPreviewSearchMessages": {
        "tr": "Mesajları Ara", "ar": "بحث في الرسائل", "fa": "جستجوی پیام‌ها",
        "iw": "חיפוש הודעות", "az": "Mesajları axtar", "uz": "Xabarlarni qidirish"
    },
    "Back": {
        "tr": "Geri", "ar": "رجوع", "fa": "بازگشت",
        "iw": "חזור", "az": "Geri", "uz": "Orqaga"
    },
    "BusinessHoursProfile": {
        "tr": "Çalışma saatleri", "ar": "ساعات العمل", "fa": "ساعات کاری",
        "iw": "שעות פעילות", "az": "İş saatları", "uz": "Ish soatlari"
    },
    "BusinessHoursProfileNowOpen": {
        "tr": "Açık", "ar": "مفتوح", "fa": "باز است",
        "iw": "פתוח", "az": "Açıqdır", "uz": "Ochiq"
    },
    "BusinessProfileLocation": {
        "tr": "Konum", "ar": "الموقع", "fa": "موقعیت مکانی",
        "iw": "מיקום", "az": "Məkan", "uz": "Manzil"
    },
    "CP_AdminActions": {
        "tr": "Yönetici kısayolları", "ar": "إجراءات المشرفين", "fa": "میانبرهای مدیر",
        "iw": "קיצורי דרך למנהלים", "az": "Admin qısayolları", "uz": "Admin amallari"
    },
    "CP_ArchiveStories": {
        "tr": "Hikayeleri arşivle", "ar": "أرشفة القصص", "fa": "بایگانی داستان‌ها",
        "iw": "ארכב סטוריז", "az": "Hekayələri arxivlə", "uz": "Hikoyalarni arxivlash"
    },
    "CP_ArchiveStories_Desc": {
        "tr": "Yeni hikayeleri otomatik olarak arşive taşıyın.",
        "ar": "نقل القصص الجديدة إلى الأرشيف تلقائيًا.",
        "fa": "انتقال خودکار داستان‌های جدید به بایگانی.",
        "iw": "העבר סטוריז חדשים לארכיון באופן אוטומטי.",
        "az": "Yeni hekayələri avtomatik olaraq arxivə köçürün.",
        "uz": "Yangi hikoyalarni avtomatik tarzda arxivga o‘tkazish."
    },
    "CP_AutoPauseVideo": {
        "tr": "Medyayı otomatik duraklat", "ar": "إيقاف مؤقت للوسائط تلقائيًا", "fa": "مکث خودکار رسانه",
        "iw": "השהיית מדיה אוטומטית", "az": "Medianı avtomatik dayandır", "uz": "Mediani avtomatik to‘xtatish"
    },
    "CP_AutoPauseVideo_Desc": {
        "tr": "Uygulama arka plana geçtiğinde duraklatın.",
        "ar": "إيقاف مؤقت عند انتقال التطبيق إلى الخلفية.",
        "fa": "مکث هنگام رفتن برنامه به پس‌زمینه.",
        "iw": "השהה כאשר האפליקציה עוברת לרקע.",
        "az": "Tətbiq arxa fona keçdikdə pauza edin.",
        "uz": "Ilova fon rejimiga o‘tganda to‘xtatib turish."
    },
    "CP_AutoQuoteReplies": {
        "tr": "Yanıtları alıntıla", "ar": "اقتباس الردود", "fa": "پاسخ‌ها را نقل قول کنید",
        "iw": "צטט תגובות", "az": "Cavabları sitat gətir", "uz": "Javoblarni iqtibos qilish"
    },
    "CP_AutoQuoteReplies_Desc": {
        "tr": "Standart yanıt yerine alıntı kullanın.",
        "ar": "استخدم اقتباسًا بدلاً من الرد العادي.",
        "fa": "استفاده از نقل قول به جای پاسخ معمولی.",
        "iw": "השתמש בציטוט במקום תשובה רגילה.",
        "az": "Standart cavab əvəzinə sitatdan istifadə edin.",
        "uz": "Oddiy javob o‘rniga iqtibosdan foydalanish."
    },
    "CP_Camera2Desc": {
        "tr": "Camera2 açıkken gizlenen özellikler Camera2 etkinken çalışmaz.",
        "ar": "لن تعمل الميزات المخفية عند تمكين Camera2.",
        "fa": "قابلیت‌های پنهان شده در صورت فعال بودن Camera2 کار نمی‌کنند.",
        "iw": "תכונות המוסתרות עם Camera2 לא יפעלו.",
        "az": "Camera2 aktiv olduqda gizlədilən funksiyalar işləməyəcək.",
        "uz": "Camera2 yoqilganda yashiringan funksiyalar ishlamaydi."
    },
    "CP_CameraDualCamera": {
        "tr": "Çift kamera", "ar": "كاميرا مزدوجة", "fa": "دوربین دوگانه",
        "iw": "מצלמה כפולה", "az": "İkili kamera", "uz": "Ikkita kamera"
    },
    "CP_CameraDualCamera_Desc": {
        "tr": "Kayıt sırasında daha hızlı geçiş için her iki kamerayı da kaydeder.",
        "ar": "تسجيل رسائل الفيديو على كلا الكاميرتين للتبديل السريع.",
        "fa": "ضبط ویدیوی پیام با هر دو دوربین برای جابجایی سریع‌تر.",
        "iw": "הקלט הודעות וידאו בשתי המצלמות למעבר מהיר יותר.",
        "az": "Qeyd zamanı daha sürətli keçid üçün hər iki kamerada çəkir.",
        "uz": "Yozish paytida tezroq almashish uchun ikkala kamerada ham yozadi."
    },
    "CP_CameraQuality": {
        "tr": "Kamera Kalitesi", "ar": "جودة الكاميرا", "fa": "کیفیت دوربین",
        "iw": "איכות מצלמה", "az": "Kamera keyfiyyəti", "uz": "Kamera sifati"
    },
    "CP_CameraStabilisation": {
        "tr": "Kamera stabilizasyonu", "ar": "تثبيت الكاميرا", "fa": "لرزش‌گیر دوربین",
        "iw": "ייצוב מצלמה", "az": "Kamera sabitləşdirməsi", "uz": "Kamera barqarorligi"
    },
    "CP_CameraType": {
        "tr": "Kamera Türü", "ar": "نوع الكاميرا", "fa": "نوع دوربین",
        "iw": "סוג מצלמה", "az": "Kamera növü", "uz": "Kamera turi"
    },
    "CP_CameraTypeSystem": {
        "tr": "Sistem", "ar": "النظام", "fa": "سیستم",
        "iw": "מערכת", "az": "Sistem", "uz": "Tizim"
    },
    "CP_CameraUW": {
        "tr": "Ultra Genişten Başlat", "ar": "البدء من العدسة العريضة", "fa": "شروع از اولترا واید",
        "iw": "התחל מעדשה רחבה", "az": "Geniş bucaqlı kameradan başla", "uz": "Keng burchakli kameradan boshlash"
    },
    "CP_CameraUW_Desc": {
        "tr": "Video mesaj kaydı Ultra Geniş kameradan başlar.",
        "ar": "يبدأ تسجيل رسائل الفيديو من الكاميرا فائقة الاتساع.",
        "fa": "ضبط پیام ویدیویی از دوربین اولترا واید آغاز می‌شود.",
        "iw": "הקלטת הודעות וידאו תתחיל מהמצלמה הרחבה.",
        "az": "Video mesaj qeydi geniş bucaqlı kameradan başlayacaq.",
        "uz": "Dumaloq video xabarlarni yozish keng burchakli kameradan boshlanadi."
    },
    "CP_CameraXDesc": {
        "tr": "CameraX yalnızca belirli cihazlarda desteklenir.",
        "ar": "CameraX مدعوم فقط على أجهزة محددة.",
        "fa": "CameraX فقط در دستگاه‌های منتخب پشتیبانی می‌شود.",
        "iw": "CameraX נתמך רק במכשירים נבחרים.",
        "az": "CameraX yalnız seçilmiş cihazlarda dəstəklənir.",
        "uz": "CameraX faqat tanlangan qurilmalarda qo‘llab-quvvatlanadi."
    },
    "CP_Category_Camera": {
        "tr": "Kamera", "ar": "الكاميرا", "fa": "دوربین",
        "iw": "מצלמה", "az": "Kamera", "uz": "Kamera"
    },
    "CP_CenterCameraControlButtons": {
        "tr": "Kontrol butonlarını ortala", "ar": "توسيط أزرار التحكم", "fa": "وسط‌چین کردن دکمه‌های کنترل",
        "iw": "מרכז כפתורי בקרה", "az": "İdarəetmə düymələrini mərkəzləşdir", "uz": "Boshqaruv tugmalarini markazlashtirish"
    },
    "CP_CenterCameraControlButtons_Desc": {
        "tr": "Kamera değiştirme ve fener butonlarını ortalayın.",
        "ar": "توسيط أزرار تبديل الكاميرا والفلاش.",
        "fa": "دکمه‌های تغییر دوربین و چراغ قوه را در وسط قرار دهید.",
        "iw": "מרכז את לחצני החלפת המצלמה והפנס.",
        "az": "Kamera dəyişdirmə və fənər düymələrini mərkəzləşdirin.",
        "uz": "Kamerani almashtirish va chiroq tugmalarini markazlashtiring."
    },
    "CP_ChatMenuShortcuts": {
        "tr": "Sohbet menüsü kısayolları", "ar": "إجراءات قائمة الدردشة", "fa": "میانبرهای منوی چت",
        "iw": "קיצורי תפריט צ\'אט", "az": "Çat menyusu qısayolları", "uz": "Chat menyusi amallari"
    },
    "CP_CustomWallpapers": {
        "tr": "Sohbete özel duvar kağıtları", "ar": "خلفيات خاصة لكل محادثة", "fa": "تصاویر پس‌زمینه اختصاصی چت",
        "iw": "טפטים ייעודיים לצ\'אט", "az": "Fərdi çat divar kağızları", "uz": "Alohida chat fon rasmlari"
    },
    "CP_CustomWallpapers_Desc": {
        "tr": "Ayrı sohbetler için ayarlanan duvar kağıtlarını göster.",
        "ar": "إظهار الخلفيات المحددة للمحادثات الفردية.",
        "fa": "نمایش تصاویر پس‌زمینه تنظیم شده برای چت‌های خاص.",
        "iw": "הצג טפטים שהוגדרו לצ\'אטים נפרדים.",
        "az": "Ayrı-ayrı çatlar üçün təyin edilmiş divar kağızlarını göstər.",
        "uz": "Alohida chatlar uchun belgilangan fon rasmlarini ko‘rsatish."
    },
    "CP_DefaultCameraDesc": {
        "tr": "Telegram\'ın yerleşik kamerasını kullanın.", "ar": "استخدام كاميرا تيليجرام المدمجة.", "fa": "استفاده از دوربین داخلی تلگرام.",
        "iw": "השתמש במצלמה המובנית של טלגרם.", "az": "Telegram-ın daxili kamerasından istifadə edin.", "uz": "Telegramning o‘rnatilgan kamerasidan foydalanish."
    },
    "CP_DeleteForAll": {
        "tr": "Varsayılan olarak herkesten sil", "ar": "الحذف للجميع افتراضيًا", "fa": "حذف برای همه به طور پیش‌فرض",
        "iw": "מחק עבור כולם כברירת מחדל", "az": "Standart olaraq hamı üçün sil", "uz": "Standart bo‘yicha hammadan o‘chirish"
    },
    "CP_DeleteForAll_Desc": {
        "tr": "Silme iletişim kutusunda bu seçeneği önceden seçin.",
        "ar": "تحديد هذا الخيار مسبقًا في نافذة الحذف.",
        "fa": "انتخاب پیش‌فرض این گزینه در کادر حذف.",
        "iw": "בחר מראש אפשרות זו בחלון המחיקה.",
        "az": "Silmə pəncərəsində bu variantı əvvəlcədən seçin.",
        "uz": "O‘chirish oynasida bu bandni oldindan belgilash."
    },
    "CP_DisableCam": {
        "tr": "Kamera düğmesi altta", "ar": "زر الكاميرا في الأسفل", "fa": "دکمه دوربین در پایین",
        "iw": "כפתור מצלמה למטה", "az": "Kamera düyməsi aşağıda", "uz": "Kamera tugmasi pastda"
    },
    "CP_DisableCam_Desc": {
        "tr": "Kamera düğmesi galeri düğmesinin yanında görünür.",
        "ar": "سيظهر زر الكاميرا بجانب زر المعرض.",
        "fa": "دکمه دوربین کنار دکمه گالری ظاهر می‌شود.",
        "iw": "כפתור המצלמה יופיע ליד כפתור הגלריה.",
        "az": "Kamera düyməsi qalereya düyməsinin yanında görünəcək.",
        "uz": "Kamera tugmasi galereya tugmasi yonida ko‘rinadi."
    },
    "CP_DisablePremStickAnim": {
        "tr": "Premium çıkartma efektlerini devre dışı bırak", "ar": "تعطيل تأثيرات الملصقات المميزة", "fa": "غیرفعال کردن جلوه‌های استیکر پرمیوم",
        "iw": "בטל אפקטי מדבקות פרימיום", "az": "Premium stiker effektlərini söndür", "uz": "Premium stiker effektlarini o‘chirish"
    },
    "CP_DisablePremStickAnim_Desc": {
        "tr": "Bir çıkartmaya dokunulduğunda efektleri oynatma.",
        "ar": "عدم تشغيل التأثيرات عند النقر على الملصق.",
        "fa": "هنگام لمس استیکر افکتی پخش نشود.",
        "iw": "אל תפעיל אפקטים בלחיצה על מדבקה.",
        "az": "Stikerə toxunduqda effekti oynatma.",
        "uz": "Stiker bosilganda effektlarni ijro etmaslik."
    },
    "CP_DisablePremStickAutoPlay": {
        "tr": "Premium çıkartmaların otomatik oynatılmasını devre dışı bırak",
        "ar": "تعطيل التشغيل التلقائي للملصقات المميزة",
        "fa": "غیرفعال کردن پخش خودکار استیکرهای پرمیوم",
        "iw": "בטל הפעלה אוטומטית של מדבקות פרימיום",
        "az": "Premium stikerlərin avtomatik oxudulmasını söndür",
        "uz": "Premium stikerlarning avtomatik ijrosini o‘chirish"
    },
    "CP_DisablePremStickAutoPlay_Desc": {
        "tr": "Premium çıkartma animasyonlarını otomatik oynatma.",
        "ar": "عدم تشغيل رسوم الملصقات المميزة تلقائيًا.",
        "fa": "پویانمایی استیکرهای پرمیوم خودکار پخش نشود.",
        "iw": "אל תפעיל אוטומטית הנפשות של מדבקות פרימיום.",
        "az": "Premium stiker animasiyalarını avtomatik başlatma.",
        "uz": "Premium stiker animatsiyalarini avtomatik ishga tushirmaslik."
    },
    "CP_DisableReactionAnim": {
        "tr": "Tepki animasyonunu devre dışı bırak", "ar": "تعطيل رسوم التفاعلات", "fa": "غیرفعال کردن پویانمایی واکنش‌ها",
        "iw": "בטל הנפשת תגובות", "az": "Reaksiya animasiyasını söndür", "uz": "Reaksiya animatsiyasini o‘chirish"
    },
    "CP_DisableReactionAnim_Desc": {
        "tr": "Tepkileri büyük animasyon olmadan uygulayın.",
        "ar": "تطبيق التفاعلات دون الرسوم المتحركة الكبيرة.",
        "fa": "اعمال واکنش‌ها بدون انیمیشن بزرگ.",
        "iw": "החל תגובות ללא ההנפשה הגדולה.",
        "az": "Reaksiyaları böyük animasiya olmadan tətbiq edin.",
        "uz": "Katta animatsiyasiz reaksiyalarni qo‘yish."
    },
    "CP_DisableReactionsOverlay": {
        "tr": "Tepki panelini gizle", "ar": "إخفاء لوحة التفاعلات", "fa": "پنهان کردن پنل واکنش‌ها",
        "iw": "הסתר חלונית תגובות", "az": "Reaksiya panelini gizlət", "uz": "Reaksiyalar panelini yashirish"
    },
    "CP_DisableReactionsOverlay_Desc": {
        "tr": "Mesajların üzerinde tepkileri gösterme.",
        "ar": "عدم إظهار التفاعلات فوق الرسائل.",
        "fa": "عدم نمایش واکنش‌ها بالای پیام‌ها.",
        "iw": "אל תציג תגובות מעל הודעות.",
        "az": "Mesajların üstündə reaksiyaları göstərmə.",
        "uz": "Xabarlar ustida reaksiyalarni ko‘rsatmaslik."
    },
    "CP_DisableSwipeToNext": {
        "tr": "Kanal kaydırmayı devre dışı bırak", "ar": "تعطيل التمرير بين القنوات", "fa": "غیرفعال کردن جابجایی بین کانال‌ها",
        "iw": "בטל מעבר ערוצים בהחלקה", "az": "Kanallar arası sürüşdürməni söndür", "uz": "Kanallar o‘rtasida siljishni o‘chirish"
    },
    "CP_DisableSwipeToNext_Desc": {
        "tr": "Kaydırma sırasında geçerli kanalda kalın.",
        "ar": "البقاء في القناة الحالية أثناء التمرير.",
        "fa": "هنگام پیمایش در همان کانال بمانید.",
        "iw": "הישאר בערוץ הנוכחי בעת גלילה.",
        "az": "Sürüşdürərkən cari kanalda qalın.",
        "uz": "Varaqlash paytida joriy kanalda qolish."
    },
    "CP_DisableVibration": {
        "tr": "Titreşimi devre dışı bırak", "ar": "تعطيل الاهتزاز", "fa": "غیرفعال کردن لرزش",
        "iw": "בטל רטט", "az": "Vibrasiyanı söndür", "uz": "Tebranishni o‘chirish"
    },
    "CP_DoubleTapAction": {
        "tr": "Çift dokunma eylemi", "ar": "إجراء النقر المزدوج", "fa": "عملیات دوبار ضربه",
        "iw": "פעולת הקשה כפולה", "az": "İkiqat toxunma fəaliyyəti", "uz": "Ikki marta bosish amali"
    },
    "CP_Filters_Header": {
        "tr": "Klasörler", "ar": "المجلدات", "fa": "پوشه‌ها",
        "iw": "תיקיות", "az": "Qovluqlar", "uz": "Jildlar"
    },
    "CP_ForwardMsgDate": {
        "tr": "İletilen mesaj tarihini göster", "ar": "إظهار تاريخ الرسالة المحولة", "fa": "نمایش تاریخ پیام هدایت شده",
        "iw": "הצג תאריך הודעה מועברת", "az": "Yönləndirilmiş mesaj tarixini göstər", "uz": "Uzatilgan xabar sanasini ko‘rsatish"
    },
    "CP_Header_DialogList": {
        "tr": "Sohbet listesi", "ar": "قائمة المحادثات", "fa": "لیست گفتگوها",
        "iw": "רשימת צ\'אטים", "az": "Çat siyahısı", "uz": "Chatlar ro‘yxati"
    },
    "CP_Header_Videomessages": {
        "tr": "Video mesajları", "ar": "رسائل الفيديو", "fa": "پیام‌های ویدیویی",
        "iw": "הודעות וידאו", "az": "Video mesajlar", "uz": "Video xabarlar"
    },
    "CP_HideKbdOnScroll": {
        "tr": "Kaydırmada klavyeyi gizle", "ar": "إخفاء لوحة المفاتيح عند التمرير", "fa": "پنهان کردن صفحه کلید هنگام پیمایش",
        "iw": "הסתר מקלדת בגלילה", "az": "Sürüşdürərkən klaviaturanı gizlət", "uz": "Varaqlaganda klaviaturani yashirish"
    },
    "CP_HideMuteUnmuteButton": {
        "tr": "Kanal ses çubuğunu gizle", "ar": "إخفاء شريط صوت القناة", "fa": "پنهان کردن نوار صدای کانال",
        "iw": "הסתר שורת השתקת ערוץ", "az": "Kanal səs panelini gizlət", "uz": "Kanal ovoz panelini yashirish"
    },
    "CP_HideSendAsChannel": {
        "tr": "«Kanal olarak gönder» seçeneğini gizle", "ar": "إخفاء «إرسال كقناة»", "fa": "پنهان کردن «ارسال به عنوان کانال»",
        "iw": "הסתר «שלח כערוץ»", "az": "«Kanal adından göndər»i gizlət", "uz": "«Kanal nomidan yuborish»ni yashirish"
    },
    "CP_HideSendAsChannelDesc": {
        "tr": "Menüye emoji düğmesine basılı tutarak erişilebilir.",
        "ar": "تبقى القائمة متاحة بالضغط المطول على زر الرموز.",
        "fa": "منو با نگه داشتن دکمه اموجی در دسترس باقی می‌ماند.",
        "iw": "התפריט נשאר זמין בלחיצה ארוכה על האימוג\'י.",
        "az": "Menyu emoji düyməsini basıb saxlamaqla əlçatan qalacaq.",
        "uz": "Menyu emoji tugmasini bosib turish orqali ochiladi."
    },
    "CP_HideStories": {
        "tr": "Hikayeleri gizle", "ar": "إخفاء القصص", "fa": "پنهان کردن داستان‌ها",
        "iw": "הסתר סטוריז", "az": "Hekayələri gizlət", "uz": "Hikoyalarni yashirish"
    },
    "CP_HideStories_Desc": {
        "tr": "Hikayeleri sohbetlerden ve profillerden kaldırın.",
        "ar": "إزالة القصص من المحادثات والملفات الشخصية.",
        "fa": "حذف داستان‌ها از چت‌ها و نمایه‌ها.",
        "iw": "הסר סטוריז מצ\'אטים ומפרופילים.",
        "az": "Hekayələri çatlardan və profillərdən silin.",
        "uz": "Hikoyalarni chatlar va profillardan olib tashlash."
    },
    "CP_LeftBottomButtonAction": {
        "tr": "Sol buton eylemi", "ar": "إجراء الزر الأيسر", "fa": "عملیات دکمه چپ",
        "iw": "פעולת לחצן שמאלי", "az": "Sol düymə fəaliyyəti", "uz": "Chap tugma amali"
    },
    "CP_MainTabs_ForceOpenChats_Desc": {
        "tr": "Arama butonu her zaman sohbet aramasını açar.",
        "ar": "زر البحث يفتح دائمًا بحث المحادثات.",
        "fa": "دکمه جستجو همیشه جستجوی گفتگوها را باز می‌کند.",
        "iw": "כפתور החיפוש פותח תמיד חיפוש צ\'אטים.",
        "az": "Axtarış düyməsi həmişə çat axtarışını açır.",
        "uz": "Qidiruv tugmasi doim chatlar qidiruvini ochadi."
    },
    "CP_MainTabs_Header": {
        "tr": "Alt sekmeler", "ar": "علامات التبويب السفلية", "fa": "تب‌های پایینی",
        "iw": "לשוניות תחתונות", "az": "Aşağı tablar", "uz": "Pastki bo‘limlar"
    },
    "CP_MessageMenu": {
        "tr": "Mesaj menüsü", "ar": "قائمة الرسالة", "fa": "منوی پیام",
        "iw": "תפריט הודעה", "az": "Mesaj menyusu", "uz": "Xabar menyusi"
    },
    "CP_MessageMenuCompactLayout": {
        "tr": "Kompakt menü düzeni", "ar": "تخطيط القائمة المضغوط", "fa": "طرح فشرده منو",
        "iw": "פריסת תפריט קומפקטית", "az": "Kompakt menyu düzümü", "uz": "Ixcham menyu tartibi"
    },
    "CP_MessageMenuCompactLayout_Desc": {
        "tr": "En fazla dört ana eylemi büyük düğmeler olarak gösterin.",
        "ar": "إظهار ما يصل إلى أربعة إجراءات كأزرار كبيرة.",
        "fa": "نمایش تا چهار اقدام اصلی به صورت دکمه‌های بزرگ.",
        "iw": "הצג עד ארבע פעולות ראשיות כלחצנים גדולים.",
        "az": "Dördə qədər əsas hərəkəti böyük düymələr kimi göstərin.",
        "uz": "To‘rttagacha asosiy amalni katta tugmalar ko‘rinishida ko‘rsatish."
    },
    "CP_MessageMenuCompactLayout_Dot": {
        "tr": "Nokta, uzun basma eylemlerini belirtir.",
        "ar": "نقطة تميز إجراءات الضغط المطول.",
        "fa": "یک نقطه اقدامات با فشار طولانی را مشخص می‌کند.",
        "iw": "נקודה מסמנת פעולות בלחיצה ארוכה.",
        "az": "Nöqtə basıb saxlama hərəkətini bildirir.",
        "uz": "Nuqta bosib turish orqali bajariladigan amalni bildiradi."
    },
    "CP_MessageMenuItems": {
        "tr": "Mesaj menüsü öğeleri", "ar": "عناصر قائمة الرسالة", "fa": "گزینه‌های منوی پیام",
        "iw": "פריטי תפריט הודעה", "az": "Mesaj menyusu elementləri", "uz": "Xabar menyusi elementlari"
    },
    "CP_Message_Filtering": {
        "tr": "Mesaj filtreleme", "ar": "تصفية الرسائل", "fa": "فیلتر کردن پیام‌ها",
        "iw": "סינון הודעות", "az": "Mesaj filtrləməsi", "uz": "Xabarlarni filtrlash"
    },
    "CP_Message_Filtering_Collapse": {
        "tr": "Mesajları daralt", "ar": "طي الرسائل", "fa": "جمع کردن پیام‌ها",
        "iw": "כווץ הודעות", "az": "Mesajları bük", "uz": "Xabarlarni yig‘ish"
    },
    "CP_Message_Filtering_Collapse_Desc": {
        "tr": "Filtrelenen mesajları otomatik olarak daraltın.",
        "ar": "طي الرسائل المفلترة تلقائيًا.",
        "fa": "جمع کردن خودکار پیام‌های فیلتر شده.",
        "iw": "כווץ אוטומטית הודעות מסוננות.",
        "az": "Filtrlənmiş mesajları avtomatik bükün.",
        "uz": "Filtrlangan xabarlarni avtomatik tarzda yig‘ish."
    },
    "CP_Messages_Size": {
        "tr": "Balon boyutu", "ar": "حجم الفقاعة", "fa": "اندازه حباب",
        "iw": "גודל בועה", "az": "Baloncuk ölçüsü", "uz": "Xabar o‘lchami"
    },
    "CP_PlayVideo": {
        "tr": "Videoyu ses düğmeleriyle oynat", "ar": "تشغيل الفيديو بأزرار الصوت", "fa": "پخش ویدیو با کلیدهای صدا",
        "iw": "הפעל וידאו עם מקשי עוצמת שמע", "az": "Videonı səs düymələri ilə başlat", "uz": "Videoni ovoz tugmalari bilan ijro etish"
    },
    "CP_PlayVideo_Desc": {
        "tr": "Görünür videoyu bir ses düğmesiyle başlatın.",
        "ar": "بدء تشغيل الفيديو المرئي بزر الصوت.",
        "fa": "شروع ویدیوی قابل مشاهده با یک دکمه صدا.",
        "iw": "הפעל וידאו גלוי באמצעות כפתור עוצמת שמע.",
        "az": "Görünən videonu səs düyməsi ilə başladın.",
        "uz": "Ko‘rinib turgan videoni ovoz tugmasi bilan boshlash."
    },
    "CP_ProfileReplyBackground": {
        "tr": "Mesajlar ve profiller", "ar": "الرسائل والملفات الشخصية", "fa": "پیام‌ها و نمایه‌ها",
        "iw": "הודעות ופרופילים", "az": "Mesajlar və profillər", "uz": "Xabarlar va profillar"
    },
    "CP_SilenceNonContacts": {
        "tr": "Kayıtlı olmayanları sessize al", "ar": "كتم صوت غير جهات الاتصال", "fa": "بی‌صدا کردن افراد ناشناس",
        "iw": "השתק מי שאינו באנשי הקשר", "az": "Kontaktda olmayanları səssiz et", "uz": "Kontaktda bo‘lmaganlarni ovozsiz qilish"
    },
    "CP_SilenceNonContacts_Desc": {
        "tr": "Bildirim seslerini yalnızca kişiler için çalın.",
        "ar": "تشغيل أصوات الإشعارات لجهات الاتصال فقط.",
        "fa": "پخش صدای اعلان فقط برای مخاطبین.",
        "iw": "השמע צלילי התראה רק עבור אנשי קשר.",
        "az": "Bildiriş səslərini yalnız kontaktlar üçün səsləndir.",
        "uz": "Faqat kontaktlar uchun bildirishnoma tovushlarini chiqarish."
    },
    "CP_Slider_RecentEmojisAndStickers": {
        "tr": "Son emojiler ve çıkartmalar", "ar": "الرموز والملصقات الأخيرة", "fa": "اموجی‌ها و استیکرهای اخیر",
        "iw": "אימוג\'י ומדבקות אחרונים", "az": "Son emojilər və stikerlər", "uz": "Oxirgi emojilar va stikerlar"
    },
    "CP_Snowflakes_Header": {
        "tr": "Kar taneleri", "ar": "رقاقات الثلج", "fa": "دانه‌های برف",
        "iw": "פתיתי שלג", "az": "Qar dənələri", "uz": "Qor parchalari"
    },
    "CP_SortByUnread": {
        "tr": "Okunmamışa göre sırala", "ar": "ترتيب حسب غير المقروءة", "fa": "مرتب‌سازی بر اساس خوانده نشده",
        "iw": "מיין לפי שלא נקראו", "az": "Oxunmamışlara görə sırala", "uz": "O‘qilmaganlar bo‘yicha saralash"
    },
    "CP_SystemCameraDesc": {
        "tr": "Sistem kamerasına doğrudan erişin.", "ar": "الوصول المباشر إلى كاميرا النظام.", "fa": "دسترسی مستقیم به دوربین سیستم.",
        "iw": "גישה ישירה למצלמת המערכת.", "az": "Sistem kamerasına birbaşa giriş.", "uz": "Tizim kamerasiga to‘g‘ridan-to‘g‘ri kirish."
    },
    "CP_TimeOnStick": {
        "tr": "Çıkartmalarda zamanı gizle", "ar": "إخفاء الوقت على الملصقات", "fa": "پنهان کردن زمان در استیکرها",
        "iw": "הסתר זמן במדבקות", "az": "Stikerlərdə vaxtı gizlət", "uz": "Stikerlarda vaqtni yashirish"
    },
    "CP_UnarchiveOnSwipe": {
        "tr": "Kaydırmayla arşivden çıkar", "ar": "إلغاء الأرشفة بالتمرير", "fa": "خروج از بایگانی با کشیدن",
        "iw": "בטל ארכוב בהחלקה", "az": "Sürüşdürməklə arxivdən çıxar", "uz": "Siljish orqali arxivdan chiqarish"
    },
    "CP_UnreadBadgeOnBackButton": {
        "tr": "Okunmamış sohbetler sayacı", "ar": "عداد المحادثات غير المقروءة", "fa": "شمارنده چت‌های خوانده نشده",
        "iw": "מונה צ\'אטים שלא נקראו", "az": "Oxunmamış çat sayğacı", "uz": "O‘qilmagan chatlar hisoblagichi"
    },
    "CP_UnreadBadgeOnBackButton_Desc": {
        "tr": "Geri düğmesinde okunmamış sayısını gösterin.",
        "ar": "إظهار عدد الرسائل غير المقروءة على زر الرجوع.",
        "fa": "نمایش تعداد خوانده نشده‌ها در دکمه بازگشت.",
        "iw": "הצג מספר הודעות שלא נקראו בכפתור חזור.",
        "az": "«Geri» düyməsində oxunmamışların sayını göstər.",
        "uz": "«Orqaga» tugmasida o‘qilmagan xabarlar sonini ko‘rsatish."
    },
    "CP_VibrateInChats": {
        "tr": "Sohbet içi titreşim", "ar": "الاهتزاز داخل الدردشة", "fa": "لرزش درون چت",
        "iw": "רטט בתוך הצ\'אט", "az": "Çatdaxili vibrasiya", "uz": "Chat ichidagi tebranish"
    },
    "CP_VibrateInChats_Desc": {
        "tr": "Açık bir sohbete mesaj geldiğinde titret.",
        "ar": "اهتزاز عند وصول رسالة في محادثة مفتوحة.",
        "fa": "هنگام رسیدن پیام در چت باز بلرزد.",
        "iw": "רטוט כאשר הודעה מגיעה בצ\'אט פתוח.",
        "az": "Açıq çatda mesaj gəldikdə titrəşsin.",
        "uz": "Ochiq chatda xabar kelganda tebranish."
    },
    "CP_VideoSeekDuration": {
        "tr": "Video sarma adımı", "ar": "خطوة تقديم الفيديو", "fa": "گام جلو/عقب بردن ویدیو",
        "iw": "שלב הרצת וידאו", "az": "Videonun irəli/geri addımı", "uz": "Videoni o‘tkazish qadami"
    },
    "Cancel": {
        "tr": "İptal", "ar": "إلغاء", "fa": "لغو",
        "iw": "ביטול", "az": "İmtina", "uz": "Bekor qilish"
    },
    "ChangePasscode": {
        "tr": "Kodu Değiştir", "ar": "تغيير رمز القفل", "fa": "تغییر گذرواژه",
        "iw": "שנה קוד גישה", "az": "Kodu dəyiş", "uz": "Parolni o‘zgartirish"
    },
    "ChannelAdministrators": {
        "tr": "Yöneticiler", "ar": "المشرفون", "fa": "مدیران",
        "iw": "מנהלים", "az": "Administratorlar", "uz": "Administratorlar"
    },
    "ChannelMembers": {
        "tr": "Üyeler", "ar": "الأعضاء", "fa": "اعضا",
        "iw": "חברים", "az": "Üzvlər", "uz": "A’zolar"
    },
    "ChannelPermissions": {
        "tr": "İzinler", "ar": "الصلاحيات", "fa": "دسترسی‌ها",
        "iw": "הרשאות", "az": "İcazələr", "uz": "Ruxsatlar"
    },
    "Clear": {
        "tr": "Temizle", "ar": "مسح", "fa": "پاک کردن",
        "iw": "נקה", "az": "Təmizlə", "uz": "Tozalash"
    },
    "ClearMessageDatabase": {
        "tr": "Mesaj Veritabanını Temizle", "ar": "مسح قاعدة بيانات الرسائل", "fa": "پاک کردن پایگاه داده پیام‌ها",
        "iw": "נקה מאגר הודעות", "az": "Mesaj məlumat bazasını təmizlə", "uz": "Xabarlar ma’lumotlar bazasini tozalash"
    },
    "ClearMessageDatabaseNotification": {
        "tr": "Mesaj veritabanı temizlendi.", "ar": "تم مسح قاعدة بيانات الرسائل.", "fa": "پایگاه داده پیام‌ها پاک شد.",
        "iw": "מאגר ההודעות נוקה.", "az": "Mesaj məlumat bazası təmizləndi.", "uz": "Xabarlar ma’lumotlar bazasi tozalandi."
    },
    "Copy": {
        "tr": "Kopyala", "ar": "نسخ", "fa": "کپی",
        "iw": "העתק", "az": "Kopyala", "uz": "Nusxa olish"
    },
    "CopyLink": {
        "tr": "Bağlantıyı Kopyala", "ar": "نسخ الرابط", "fa": "کپی پیوند",
        "iw": "העתק קישור", "az": "Linki kopyala", "uz": "Havoladan nusxa olish"
    },
    "Deactivate": {
        "tr": "ŞİMDİ SİL", "ar": "حذف الآن", "fa": "اکنون حذف کنید",
        "iw": "מחק כעת", "az": "İNDİ SİL", "uz": "HOZIR O‘CHIRISH"
    },
    "Default": {
        "tr": "Varsayılan", "ar": "افتراضي", "fa": "پیش‌فرض",
        "iw": "ברירת מחדל", "az": "Standart", "uz": "Standart"
    },
    "Delete": {
        "tr": "Sil", "ar": "حذف", "fa": "حذف",
        "iw": "מחק", "az": "Sil", "uz": "O‘chirish"
    },
    "DirectShare": {
        "tr": "Doğrudan Paylaşım", "ar": "المشاركة المباشرة", "fa": "اشتراک‌گذاری مستقیم",
        "iw": "שיתוף ישיר", "az": "Birbaşa paylaşım", "uz": "To‘g‘ridan-to‘g‘ri ulashish"
    },
    "DirectShareInfo": {
        "tr": "Son sohbetleri Android paylaşım menüsünde göster", "ar": "إظهار المحادثات الأخيرة في قائمة مشاركة أندرويد",
        "fa": "نمایش چت‌های اخیر در منوی اشتراک‌گذاری اندروید", "iw": "הצג צ\'אטים אחרונים בתפריט שיתוף של Android",
        "az": "Son çatarı Android paylaşım menyusunda göstər", "uz": "Oxirgi chatlarni Android ulashish menyusida ko‘rsatish"
    },
    "Disable": {
        "tr": "Devre dışı bırak", "ar": "تعطيل", "fa": "غیرفعال",
        "iw": "בטל", "az": "Söndür", "uz": "O‘chirish"
    },
    "DisablePasscodeTurnOff": {
        "tr": "Kapat", "ar": "إيقاف", "fa": "خاموش کردن",
        "iw": "כבה", "az": "Söndür", "uz": "O‘chirish"
    },
    "Done": {
        "tr": "Bitti", "ar": "تم", "fa": "انجام شد",
        "iw": "סיום", "az": "Hazırdır", "uz": "Bajarildi"
    },
    "DontReadStoriesPackets": {
        "tr": "Hikayeleri Okuma", "ar": "عدم قراءة القصص", "fa": "داستان‌ها خوانده نشود",
        "iw": "אל תקרא סטוריז", "az": "Hekayələri oxuma", "uz": "Hikoyalarni o‘qimaslik"
    },
    "DontSendOnlinePackets": {
        "tr": "Çevrimiçi Gönderme", "ar": "عدم إرسال حالة الاتصال", "fa": "وضعیت آنلاین ارسال نشود",
        "iw": "אל תשלח מצב מחובר", "az": "Onlayn göndərmə", "uz": "Onlayn holatni yubormaslik"
    },
    "DontSendReadMessagePackets": {
        "tr": "Mesajları Okuma", "ar": "عدم قراءة الرسائل", "fa": "پیام‌ها خوانده نشود",
        "iw": "אל תקרא הודעות", "az": "Mesajları oxuma", "uz": "Xabarlarni o‘qimaslik"
    },
    "DontSendUploadProgress": {
        "tr": "Yazıyor Gönderme", "ar": "عدم إرسال جاري الكتابة", "fa": "وضعیت تایپ ارسال نشود",
        "iw": "אל תשלח מקליד", "az": "Yazır göndərmə", "uz": "Yozmoqda holatini yubormaslik"
    },
    "EP_CustomChat": {
        "tr": "Kayıtlı Mesajları Değiştir", "ar": "استبدال الرسائل المحفوظة", "fa": "جایگزینی پیام‌های ذخیره شده",
        "iw": "החלף הודעות שמורות", "az": "Saxlanılan mesajları əvəzlə", "uz": "Saqlangan xabarlar o‘rniga chat"
    },
    "EP_CustomChat_Desc": {
        "tr": "Kayıtlı Mesajlar yerine kullanılacak sohbeti seçin.",
        "ar": "اختر المحادثة المستخدمة بدلاً من الرسائل المحفوظة.",
        "fa": "چت مورد استفاده به جای پیام‌های ذخیره شده را انتخاب کنید.",
        "iw": "בחר את הצ\'אט שישמש במקום הודעות שמורות.",
        "az": "Saxlanılan mesajların yerinə istifadə ediləcək çatı seçin.",
        "uz": "Saqlangan xabarlar o‘rniga ishlatiladigan chatni tanlang."
    },
    "EP_CustomChat_Selected_Title": {
        "tr": "yerine", "ar": "بدلاً من", "fa": "به جای",
        "iw": "במקום", "az": "əvəzinə", "uz": "o‘rniga"
    },
    "EP_DownloadSpeedBoost": {
        "tr": "Daha hızlı indirmeler", "ar": "تنزيل أسرع", "fa": "دانلود سریع‌تر",
        "iw": "הורדות מהירות יותר", "az": "Daha sürətli yükləmə", "uz": "Tezroq yuklab olish"
    },
    "EP_DownloadSpeedBoostExtreme": {
        "tr": "Ekstrem", "ar": "فائق", "fa": "فوق‌العاده",
        "iw": "קיצוני", "az": "Ekstremal", "uz": "Ekstremal"
    },
    "EP_NavigationAnimation": {
        "tr": "Gezinme animasyonu", "ar": "رسوم التنقل", "fa": "پویانمایی ناوبری",
        "iw": "הנפשת ניווט", "az": "Naviqasiya animasiyası", "uz": "Navigatsiya animatsiyasi"
    },
    "EP_NavigationAnimationBezier": {
        "tr": "Klasik", "ar": "كلاسيكي", "fa": "کلاسیک",
        "iw": "קלאסי", "az": "Klassik", "uz": "Klassik"
    },
    "EP_NavigationAnimationCrossfading": {
        "tr": "Yumuşak başlık geçişi", "ar": "انتقال سلس للعنوان", "fa": "انتقال نرم عنوان",
        "iw": "מעבר כותרת חלק", "az": "Yumşaq başlıq keçidi", "uz": "Sarlavhaning silliq almashishi"
    },
    "EP_NavigationAnimationSpring": {
        "tr": "Yaylı", "ar": "مرن", "fa": "فنری",
        "iw": "קפיצי", "az": "Yaylı", "uz": "Prujinali"
    },
    "EP_Network": {
        "tr": "Ağ", "ar": "الشبكة", "fa": "شبکه",
        "iw": "רשת", "az": "Şəbəkə", "uz": "Tarmoq"
    },
    "EP_PhotosSize": {
        "tr": "2560 piksele kadar büyük fotoğraflar", "ar": "صور كبيرة تصل إلى 2560 بكسل", "fa": "عکس‌های بزرگ تا ۲۵۶۰ پیکسل",
        "iw": "תמונות גדולות עד 2560 פיקסלים", "az": "2560 pikselə qədər böyük fotolar", "uz": "2560 pikselgacha katta rasmlar"
    },
    "EP_SlowNetworkMode": {
        "tr": "Zayıf ağ modu", "ar": "وضع الشبكة الضعيفة", "fa": "حالت شبکه ضعیف",
        "iw": "מצב רשת חלשה", "az": "Zəif şəbəkə rejimi", "uz": "Zaif tarmoq rejimi"
    },
    "Edit": {
        "tr": "Düzenle", "ar": "تعديل", "fa": "ویرایش",
        "iw": "ערוך", "az": "Düzəliş et", "uz": "Tahrirlash"
    },
    "Emoji": {
        "tr": "Emoji", "ar": "رموز تعبيرية", "fa": "اموجی",
        "iw": "אימוג\'י", "az": "Emoji", "uz": "Emoji"
    },
    "EnableSaveDeletedMessages": {
        "tr": "Silinen Mesajları Kaydet", "ar": "حفظ الرسائل المحذوفة", "fa": "ذخیره پیام‌های حذف شده",
        "iw": "שמור הודעות שנמחקו", "az": "Silinmiş mesajları saxla", "uz": "O‘chirilgan xabarlarni saqlash"
    },
    "EnableSaveEditsHistory": {
        "tr": "Düzenleme Geçmişini Kaydet", "ar": "حفظ سجل التعديلات", "fa": "ذخیره تاریخچه ویرایش‌ها",
        "iw": "שמור היסטוריית עריכות", "az": "Düzəliş tarixçəsini saxla", "uz": "Tahrirlar tarixini saqlash"
    },
    "ErrorOccurred": {
        "tr": "Bir hata oluştu.", "ar": "حدث خطأ.", "fa": "خطایی رخ داد.",
        "iw": "אירעה שגיאה.", "az": "Xəta baş verdi.", "uz": "Xatolik yuz berdi."
    },
    "EventLog": {
        "tr": "Son Eylemler", "ar": "سجل الأحداث", "fa": "اقدامات اخیر",
        "iw": "פעולות אחרונות", "az": "Son fəaliyyətlər", "uz": "Oxirgi harakatlar"
    },
    "FilterAddChats": {
        "tr": "Sohbet Ekle", "ar": "إضافة محادثات", "fa": "افزودن گفتگوها",
        "iw": "הוסף צ\'אטים", "az": "Çatlar əlavə et", "uz": "Chatlar qo‘shish"
    },
    "FilterAllChats": {
        "tr": "Tüm Sohbetler", "ar": "جميع المحادثات", "fa": "همه گفتگوها",
        "iw": "כל הצ\'אטים", "az": "Bütün çatlar", "uz": "Barcha chatlar"
    },
    "FilterBots": {
        "tr": "Botlar", "ar": "البوتات", "fa": "ربات‌ها",
        "iw": "בוטים", "az": "Botlar", "uz": "Botlar"
    },
    "FilterChannels": {
        "tr": "Kanallar", "ar": "القنوات", "fa": "کانال‌ها",
        "iw": "ערוצים", "az": "Kanallar", "uz": "Kanallar"
    },
    "FilterChats": {
        "tr": "Sohbetler", "ar": "المحادثات", "fa": "گفتگوها",
        "iw": "צ\'אטים", "az": "Çatlar", "uz": "Chatlar"
    },
    "FilterContacts": {
        "tr": "Kişiler", "ar": "جهات الاتصال", "fa": "مخاطبین",
        "iw": "אנשי קשר", "az": "Kontaktlar", "uz": "Kontaktlar"
    },
    "FilterGroups": {
        "tr": "Gruplar", "ar": "المجموعات", "fa": "گروه‌ها",
        "iw": "קבוצות", "az": "Qruplar", "uz": "Guruhlar"
    },
    "FilterNameNonMuted": {
        "tr": "Sesi Açık", "ar": "غير مكتوم", "fa": "با صدا",
        "iw": "לא מושתק", "az": "Səsli", "uz": "Ovozli"
    },
    "FilterNameUnread": {
        "tr": "Okunmamış", "ar": "غير مقروءة", "fa": "خوانده نشده",
        "iw": "שלא נקראו", "az": "Oxunmamış", "uz": "O‘qilmagan"
    },
    "FilterNonContacts": {
        "tr": "Kişi Olmayanlar", "ar": "غير جهات الاتصال", "fa": "غیرمخاطبین",
        "iw": "לא באנשי קשר", "az": "Kontakt olmayanlar", "uz": "Kontaktda bo‘lmaganlar"
    },
    "FilterStories": {
        "tr": "Hikayeler", "ar": "القصص", "fa": "داستان‌ها",
        "iw": "סטוריז", "az": "Hekayələr", "uz": "Hikoyalar"
    },
    "Forward": {
        "tr": "İlet", "ar": "تحويل", "fa": "هدایت",
        "iw": "העבר", "az": "Yönləndir", "uz": "Uzatish"
    },
    "ForwardWithoutAuthor": {
        "tr": "Yazarsız ilet", "ar": "تحويل بدون كاتب", "fa": "هدایت بدون فرستنده",
        "iw": "העבר ללא מחבר", "az": "Müəllifsiz yönləndir", "uz": "Muallifsiz uzatish"
    },
    "General": {
        "tr": "Genel", "ar": "عام", "fa": "عمومی",
        "iw": "כללי", "az": "Ümumi", "uz": "Umumiy"
    },
    "GhostEssentialsHeader": {
        "tr": "Hayalet modu", "ar": "إعدادات الشبح", "fa": "تنظیمات حالت روح",
        "iw": "הגדרות רוח", "az": "Ruh rejimi", "uz": "Kashfiyotchi rejimi"
    },
    "Info": {
        "tr": "Bilgi", "ar": "معلومات", "fa": "اطلاعات",
        "iw": "מידע", "az": "Məlumat", "uz": "Ma’lumot"
    },
    "LinkCopied": {
        "tr": "Bağlantı kopyalandı", "ar": "تم نسخ الرابط", "fa": "پیوند کپی شد",
        "iw": "הקישור הועתק", "az": "Link kopyalandı", "uz": "Havola nusxalandi"
    },
    "LiteBatteryDisabled": {
        "tr": "Kapalı", "ar": "معطل", "fa": "خاموش",
        "iw": "כבוי", "az": "Sönülü", "uz": "O‘chiq"
    },
    "LiteBatteryEnabled": {
        "tr": "Açık", "ar": "مفعل", "fa": "روشن",
        "iw": "מופעל", "az": "Aktiv", "uz": "Yoqilgan"
    },
    "LiteMode": {
        "tr": "Animasyonlar", "ar": "الرسوم المتحركة", "fa": "پویانمایی‌ها",
        "iw": "הנפשות", "az": "Animasiyalar", "uz": "Animatsiyalar"
    },
    "LocalMiscellaneousCache": {
        "tr": "Çeşitli", "ar": "متنوع", "fa": "متفرقه",
        "iw": "שונות", "az": "Müxtəlif", "uz": "Boshqa"
    },
    "MainTabsChats": {
        "tr": "Sohbetler", "ar": "المحادثات", "fa": "گفتگوها",
        "iw": "צ\'אטים", "az": "Çatlar", "uz": "Chatlar"
    },
    "MainTabsProfile": {
        "tr": "Profil", "ar": "الملف الشخصي", "fa": "نمایه",
        "iw": "פרופיל", "az": "Profil", "uz": "Profil"
    },
    "Message": {
        "tr": "Mesaj", "ar": "رسالة", "fa": "پیام",
        "iw": "הודעה", "az": "Mesaj", "uz": "Xabar"
    },
    "MessageSavingSaveMedia": {
        "tr": "Medyayı Kaydet", "ar": "حفظ الوسائط", "fa": "ذخیره رسانه‌ها",
        "iw": "שמור מדיה", "az": "Medianı saxla", "uz": "Mediani saqlash"
    },
    "MessageSavingSaveMediaHint": {
        "tr": "Daha fazlası için tıklayın.", "ar": "انقر للمزيد.", "fa": "برای اطلاعات بیشتر کلیک کنید.",
        "iw": "לחץ למידע נוסף.", "az": "Ətraflı məlumat üçün klikləyin.", "uz": "Batafsil bilish uchun bosing."
    },
    "MessageSavingSaveMediaInPrivateChannels": {
        "tr": "… özel kanallarda", "ar": "… في القنوات الخاصة", "fa": "… در کانال‌های خصوصی",
        "iw": "… בערוצים פרטיים", "az": "… gizli kanallarda", "uz": "… xususiy kanallarda"
    },
    "MessageSavingSaveMediaInPrivateChats": {
        "tr": "… özel sohbetlerde", "ar": "… في المحادثات الخاصة", "fa": "… در چت‌های خصوصی",
        "iw": "… בצ\'אטים פרטיים", "az": "… şəxsi çatlarda", "uz": "… shaxsiy chatlarda"
    },
    "MessageSavingSaveMediaInPrivateGroups": {
        "tr": "… özel gruplarda", "ar": "… في المجموعات الخاصة", "fa": "… در گروه‌های خصوصی",
        "iw": "… בקבוצות פרטיות", "az": "… gizli qruplarda", "uz": "… xususiy guruhlarda"
    },
    "MessageSavingSaveMediaInPublicChannels": {
        "tr": "… herkese açık kanallarda", "ar": "… في القنوات العامة", "fa": "… در کانال‌های عمومی",
        "iw": "… בערוצים ציבוריים", "az": "… açıq kanallarda", "uz": "… ommaviy kanallarda"
    },
    "MessageSavingSaveMediaInPublicGroups": {
        "tr": "… herkese açık gruplarda", "ar": "… في المجموعات العامة", "fa": "… در گروه‌های عمومی",
        "iw": "… בקבוצות ציבוריות", "az": "… açıq qruplarda", "uz": "… ommaviy guruhlarda"
    },
    "MessagesSettings": {
        "tr": "Mesajlar", "ar": "الرسائل", "fa": "پیام‌ها",
        "iw": "הודעות", "az": "Mesajlar", "uz": "Xabarlar"
    },
    "Music": {
        "tr": "Müzik", "ar": "الموسيقى", "fa": "موسیقی",
        "iw": "מוזיקה", "az": "Musiqi", "uz": "Musiqa"
    },
    "NM_AP_BottomTabs": {
        "tr": "Alt sekme düzenleyici", "ar": "منظم التبويبات السفلية", "fa": "سازمان‌دهنده تب‌های پایین",
        "iw": "מארגן לשוניות תחתונות", "az": "Aşağı tablar təşkilatçısı", "uz": "Pastki bo‘limlar tashkilotchisi"
    },
    "NM_AP_Folders": {
        "tr": "Klasörler", "ar": "المجلدات", "fa": "پوشه‌ها",
        "iw": "תיקיות", "az": "Qovluqlar", "uz": "Jildlar"
    },
    "NM_AvatarCorners": {
        "tr": "Avatar köşeleri", "ar": "زوايا الصور الرمزية", "fa": "گوشه‌های آواتار",
        "iw": "פינות תמונת פרופיל", "az": "Avatar küncləri", "uz": "Avatar burchaklari"
    },
    "NM_AvatarCornersCircle": {
        "tr": "Daire", "ar": "دائرة", "fa": "دایره",
        "iw": "עיגול", "az": "Dairə", "uz": "Doira"
    },
    "NM_AvatarCornersSquare": {
        "tr": "Kare", "ar": "مربع", "fa": "مربع",
        "iw": "ריבוע", "az": "Kvadrat", "uz": "Kvadrat"
    },
    "NM_AvatarCorners_Header": {
        "tr": "Avatarlar", "ar": "الصور الرمزية", "fa": "آواتارها",
        "iw": "תמונות פרופיל", "az": "Avatarlar", "uz": "Avatarlar"
    },
    "NM_BAN_AvatarBanner": {
        "tr": "Avatardan afiş", "ar": "لافتة من الصورة الرمزية", "fa": "بنر از آواتار",
        "iw": "באנר מתמונת פרופיל", "az": "Avatardan banner", "uz": "Avatardan banner"
    },
    "NM_BAN_ChangeGlobal": {
        "tr": "Genel afişi değiştir", "ar": "تغيير اللافتة العامة", "fa": "تغییر بنر عمومی",
        "iw": "שנה באנר ציבורי", "az": "Ümumi banneri dəyiş", "uz": "Umumiy bannerni o‘zgartirish"
    },
    "NM_BAN_DeleteLocal": {
        "tr": "Yerel afişi sil", "ar": "حذف اللافتة المحلية", "fa": "حذف بنر محلی",
        "iw": "מחק באנר מקומי", "az": "Lokal banneri sil", "uz": "Lokal bannerni o‘chirish"
    },
    "NM_BAN_PickLocal": {
        "tr": "Yerel afiş seç", "ar": "اختيار لافتة محلية", "fa": "انتخاب بنر محلی",
        "iw": "בחר באנר מקומי", "az": "Lokal banner seç", "uz": "Lokal bannerni tanlash"
    },
    "NM_BAN_RefreshStatus": {
        "tr": "Durumu yenile", "ar": "تحديث الحالة", "fa": "تازه‌سازی وضعیت",
        "iw": "רענן מצב", "az": "Statusu yenilə", "uz": "Statusni yangilash"
    },
    "NM_BAN_StatusLabel": {
        "tr": "Durum", "ar": "الحالة", "fa": "وضعیت",
        "iw": "מצב", "az": "Status", "uz": "Holat"
    },
    "NM_BAN_SubmitModeration": {
        "tr": "Denetime gönder", "ar": "إرسال للمراجعة", "fa": "ارسال برای بررسی",
        "iw": "שלח לבדיקה", "az": "Moderasiyaya göndər", "uz": "Tekshiruvga yuborish"
    },
    "NM_BT_EditorFooter": {
        "tr": "Açıp kapatmak için dokunun. Taşımak için basılı tutun.",
        "ar": "اضغط للتبديل. اضغط مطولاً للسحب وإعادة الترتيب.",
        "fa": "برای جابجایی ضربه بزنید. برای مرتب‌سازی نگه دارید.",
        "iw": "הקש לשינוי. לחץ לחיצה ארוכה כדי לגרור.",
        "az": "Dəyişmək üçün toxunun. Sıranı dəyişmək üçün basıb saxlayın.",
        "uz": "Yoqish/o‘chirish uchun bosing. O‘rnini o‘zgartirish uchun ushlab turing."
    },
    "NM_BT_ForceOpenChats": {
        "tr": "Her zaman sohbetlerde ara", "ar": "البحث دائمًا في المحادثات", "fa": "همیشه در چت‌ها جستجو شود",
        "iw": "חפש תמיד בצ\'אטים", "az": "Həmişə çatlarda axtar", "uz": "Doim chatlarda qidirish"
    },
    "NM_BT_LayoutHeader": {
        "tr": "Sekmeler", "ar": "علامات التبويب", "fa": "تب‌ها",
        "iw": "לשוניות", "az": "Tablar", "uz": "Bo‘limlar"
    },
    "NM_BT_MoveLeft": {
        "tr": "Sola taşı", "ar": "تحريك لليسار", "fa": "انتقال به چپ",
        "iw": "הזז שמאלה", "az": "Sola köçür", "uz": "Chapga surish"
    },
    "NM_BT_MoveRight": {
        "tr": "Sağa taşı", "ar": "تحريك لليمين", "fa": "انتقال به راست",
        "iw": "הזז ימינה", "az": "Sağa köçür", "uz": "O‘ngga surish"
    },
    "NM_BT_ShowSearchInTabs": {
        "tr": "Sekmelerde arama düğmesi", "ar": "زر البحث في التبويبات", "fa": "دکمه جستجو در تب‌ها",
        "iw": "לחצן חיפוש בלשוניות", "az": "Tablarda axtarış düyməsi", "uz": "Bo‘limlarda qidiruv tugmasi"
    },
    "NM_BT_ShowSearchInTabs_Desc": {
        "tr": "Alt sekmelerin yanında dairesel bir arama düğmesi gösterin.",
        "ar": "إظهار زر بحث دائري بجوار التبويبات السفلية.",
        "fa": "نمایش دکمه جستجوی گرد کنار ردیف تب‌ها.",
        "iw": "הצג כפתור חיפוש עגול ליד שורת הלשוניות התחתונות.",
        "az": "Aşağı tablar panelinin yanında dairəvi axtarış düyməsini göstər.",
        "uz": "Pastki bo‘limlar paneli yonida dumaloq qidiruv tugmasini ko‘rsatish."
    },
    "NM_BT_ShowTabs": {
        "tr": "Sekmeleri göster", "ar": "إظهار علامات التبويب", "fa": "نمایش تب‌ها",
        "iw": "הצג לשוניות", "az": "Tabları göstər", "uz": "Bo‘limlarni ko‘rsatish"
    },
    "NM_BT_ShowTabsTitle": {
        "tr": "Sekme başlıklarını göster", "ar": "إظهار عناوين التبويبات", "fa": "نمایش عنوان تب‌ها",
        "iw": "הצג כותרות לשוניות", "az": "Tab adlarını göstər", "uz": "Bo‘lim nomlarini ko‘rsatish"
    },
    "NM_BT_TabDisabled": {
        "tr": "Devre dışı", "ar": "معطل", "fa": "غیرفعال",
        "iw": "מושבת", "az": "Söndürülüb", "uz": "O‘chirilgan"
    },
    "NM_BT_TabEnabled": {
        "tr": "Etkin", "ar": "مفعل", "fa": "فعال",
        "iw": "פעיל", "az": "Aktivdir", "uz": "Yoqilgan"
    },
    "NM_CAM_AskCamera": {
        "tr": "Sor", "ar": "سؤال", "fa": "پرسش",
        "iw": "שאל", "az": "Soruş", "uz": "So‘rash"
    },
    "NM_CAM_ContinuousFocus": {
        "tr": "Sürekli otomatik odaklama", "ar": "تركيز تلقائي مستمر", "fa": "فوکوس خودکار مداوم",
        "iw": "מיקוד אוטומטי רציף", "az": "Davamlı avtofokus", "uz": "Doimiy avtofokus"
    },
    "NM_CAM_ContinuousFocus_Desc": {
        "tr": "Video kaydederken kamera sürekli olarak yeniden odaklanır.",
        "ar": "استمرار التركيز التلقائي أثناء تسجيل الفيديو.",
        "fa": "تداوم فوکوس دوربین در حین ضبط ویدیو.",
        "iw": "המצלמה תתמקד ברציפות בעת הקלטת וידאו.",
        "az": "Video çəkilişi zamanı kamera davamlı olaraq fokuslanacaq.",
        "uz": "Video yozish paytida kamera doimiy ravishda fokuslanadi."
    },
    "NM_CAM_FrontCamera": {
        "tr": "Ön kamera", "ar": "الكاميرا الأمامية", "fa": "دوربین جلو",
        "iw": "מצלמה קדמית", "az": "Ön kamera", "uz": "Old kamera"
    },
    "NM_CAM_Improvements": {
        "tr": "Geliştirmeler", "ar": "تحسينات", "fa": "بهبودها",
        "iw": "שיפורים", "az": "Təkmilləşdirmələr", "uz": "Yaxshilanishlar"
    },
    "NM_CAM_OpticalStabilization": {
        "tr": "Optik stabilizasyon", "ar": "تثبيت بصري", "fa": "لرزش‌گیر اپتیکال",
        "iw": "ייצוב אופטי", "az": "Optik sabitləşdirmə", "uz": "Optik barqarorlik"
    },
    "NM_CAM_OpticalStabilization_Desc": {
        "tr": "Kullanılabildiğinde optik görüntü sabitlemeyi (OIS) kullanın.",
        "ar": "استخدام التثبيت البصري للصورة عند توفره.",
        "fa": "استفاده از لرزش‌گیر اپتیکال تصویر (OIS) در صورت وجود.",
        "iw": "השתמש בייצוב תמונה אופטי (OIS) בעת זמינותו.",
        "az": "Əgər mövcuddursa, optik sabitləşdirmədən istifadə edin.",
        "uz": "Mavjud bo‘lsa, optik tasvir barqarorligidan (OIS) foydalanish."
    },
    "NM_CAM_RearCamera": {
        "tr": "Arka kamera", "ar": "الكاميرا الخلفية", "fa": "دوربین پشت",
        "iw": "מצלמה אחורית", "az": "Arxa kamera", "uz": "Orqa kamera"
    },
    "NM_CAM_RoundCamera": {
        "tr": "Video mesajlar için kamera", "ar": "كاميرا رسائل الفيديو", "fa": "دوربین پیام‌های ویدیویی",
        "iw": "מצלמה להודעות וידאו", "az": "Video mesajlar üçün kamera", "uz": "Dumaloq video xabarlar kamerasi"
    },
    "NM_CAM_RoundVideoBitrate": {
        "tr": "Video mesaj bit hızı", "ar": "معدل بت رسائل الفيديو", "fa": "بیت‌ریت پیام‌های ویدیویی",
        "iw": "קצב סיביות להודעות וידאו", "az": "Video mesajların bitreyti", "uz": "Dumaloq videolar bitreyti"
    },
    "NM_CAM_RoundVideoSize": {
        "tr": "Video mesaj boyutu", "ar": "حجم رسائل الفيديو", "fa": "اندازه پیام‌های ویدیویی",
        "iw": "גודל הודעות וידאו", "az": "Video mesajların ölçüsü", "uz": "Dumaloq videolar o‘lchami"
    },
    "NM_CAM_RoundVideoSize_Auto": {
        "tr": "Otomatik", "ar": "تلقائي", "fa": "خودکار",
        "iw": "אוטומטי", "az": "Avto", "uz": "Avto"
    },
    "NM_CAM_RoundVideoSize_HD": {
        "tr": "HD (512p)", "ar": "HD (512p)", "fa": "HD (512p)",
        "iw": "HD (512p)", "az": "HD (512p)", "uz": "HD (512p)"
    },
    "NM_CAM_RoundVideoSize_SD": {
        "tr": "SD (240p)", "ar": "SD (240p)", "fa": "SD (240p)",
        "iw": "SD (240p)", "az": "SD (240p)", "uz": "SD (240p)"
    },
    "NM_CAM_RoundVideoSize_STD": {
        "tr": "Standart (384p)", "ar": "قياسي (384p)", "fa": "استاندارد (384p)",
        "iw": "רגיל (384p)", "az": "Standart (384p)", "uz": "Standart (384p)"
    },
    "NM_CAM_VideoQuality": {
        "tr": "Video kalitesi", "ar": "جودة الفيديو", "fa": "کیفیت ویدیو",
        "iw": "איכות וידאו", "az": "Video keyfiyyəti", "uz": "Video sifati"
    },
    "NM_CARDS_AutoScroll": {
        "tr": "Otomatik kaydırma", "ar": "تمرير تلقائي", "fa": "پیمایش خودکار",
        "iw": "גלילה אוטומטית", "az": "Avtomatik sürüşdürmə", "uz": "Avtomatik varaqlash"
    },
    "NM_CARDS_Enable": {
        "tr": "Etkinleştir", "ar": "تمكين", "fa": "فعال‌سازی",
        "iw": "הפעל", "az": "Aktivləşdir", "uz": "Yoqish"
    },
    "NM_CARDS_GeneralHeader": {
        "tr": "Genel", "ar": "عام", "fa": "عمومی",
        "iw": "כללי", "az": "Ümumi", "uz": "Umumiy"
    },
    "NM_CARDS_InfiniteScroll": {
        "tr": "Sonsuz kaydırma", "ar": "تمرير لانهائي", "fa": "پیمایش نامحدود",
        "iw": "גלילה אינסופית", "az": "Sonsuz sürüşdürmə", "uz": "Cheksiz varaqlash"
    },
    "NM_CARDS_NameBitcoin": {
        "tr": "Bitcoin", "ar": "بيتكوين", "fa": "بیت‌کوین",
        "iw": "ביטקוין", "az": "Bitkoin", "uz": "Bitkoin"
    },
    "NM_CARDS_NameGram": {
        "tr": "Gram", "ar": "جرام", "fa": "گرام",
        "iw": "גראם", "az": "Qram", "uz": "Gram"
    },
    "NM_CARDS_NameProxy": {
        "tr": "Proxy", "ar": "بروكسي", "fa": "پروکسی",
        "iw": "פרוקסי", "az": "Proksi", "uz": "Proksi"
    },
    "NM_CARDS_NameStorage": {
        "tr": "Depolama", "ar": "التخزين", "fa": "ذخیره‌سازی",
        "iw": "אחסון", "az": "Yaddaş", "uz": "Xotira"
    },
    "NM_CARDS_NameUsd": {
        "tr": "ABD Doları", "ar": "دولار أمريكي", "fa": "دلار آمریکا",
        "iw": "דולר ארה\"ב", "az": "ABŞ dolları", "uz": "AQSH dollari"
    },
    "NM_CARDS_NameWeather": {
        "tr": "Hava Durumu", "ar": "الطقس", "fa": "آب و هوا",
        "iw": "מזג אוויר", "az": "Hava", "uz": "Ob-havo"
    },
    "NM_CARDS_Title": {
        "tr": "Hızlı bilgi", "ar": "معلومات سريعة", "fa": "اطلاعات سریع",
        "iw": "מידע מהיר", "az": "Məlumat vərəqləri", "uz": "Tezkor ma’lumot"
    },
    "NM_CH_RecentEmojisStickers": {
        "tr": "Son emojiler ve çıkartmalar", "ar": "الرموز والملصقات الأخيرة", "fa": "اموجی‌ها و استیکرهای اخیر",
        "iw": "אימוג\'י ומדבקות אחרונים", "az": "Son emojilər və stikerlər", "uz": "Oxirgi emojilar va stikerlar"
    },
    "NM_CH_RecentEmojisStickers_Desc": {
        "tr": "Panelde daha fazla son kullanılan emoji tutmak için artırın.",
        "ar": "زيادة للاحتفاظ بمزيد من الرموز والملصقات المستخدمة مؤخرًا.",
        "fa": "افزایش برای نگهداری اموجی‌ها و استیکرهای بیشتر در پنل.",
        "iw": "הגדל כדי לשמור יותר אימוג\'י ומדבקות בשימוש לאחרונה.",
        "az": "Paneldə daha çox son emojini saxlamaq üçün artırın.",
        "uz": "Panelda ko‘proq oxirgi emojilarni saqlash uchun oshiring."
    },
    "NM_CP_CenterTitleInChat": {
        "tr": "Ortalanmış sohbet başlığı", "ar": "عنوان الدردشة في الوسط", "fa": "عنوان چت در وسط",
        "iw": "כותרת צ\'אט ממורכזת", "az": "Çat başlığı mərkəzdə", "uz": "Chat sarlavhasi markazda"
    },
    "NM_CP_Header_ChatView": {
        "tr": "Sohbet görünümü", "ar": "مظهر الدردشة", "fa": "نمای چت",
        "iw": "מראה הצ\'אט", "az": "Çat görünüşü", "uz": "Chat ko‘rinishi"
    },
    "NM_CP_Header_Interaction": {
        "tr": "Etkileşim", "ar": "التفاعل", "fa": "تعامل",
        "iw": "אינטראקציה", "az": "Qarşılıqlı əlaqə", "uz": "O‘zaro ta’sir"
    },
    "NM_CP_Header_Media": {
        "tr": "Medya", "ar": "الوسائط", "fa": "رسانه‌ها",
        "iw": "מדיה", "az": "Media", "uz": "Media"
    },
    "NM_CP_LatexRendering": {
        "tr": "LaTeX formüllerini işle", "ar": "عرض معادلات LaTeX", "fa": "نمایش فرمول‌های LaTeX",
        "iw": "הצג נוסחאות LaTeX", "az": "LaTeX düsturlarını göstər", "uz": "LaTeX formulalarini ko‘rsatish"
    },
    "NM_CP_LatexRendering_Desc": {
        "tr": "Mesajlardaki $...$ içindeki formülleri tanıyın.",
        "ar": "التعرف على المعادلات داخل $...$ في الرسائل.",
        "fa": "شناسایی فرمول‌ها درون $...$ در پیام‌ها.",
        "iw": "זהה נוסחאות בתוך $...$ בהודעות.",
        "az": "Mesajlardakı $...$ daxilindəki düsturları tanıyın.",
        "uz": "Xabarlardagi $...$ ichidagi formulalarni aniqlash."
    },
    "NM_CP_PreReformRussian": {
        "tr": "Reform öncesi Rusça yazım", "ar": "تهجئة روسية قبل الإصلاح", "fa": "املای روسی پیش از اصلاحات",
        "iw": "איות רוסי טרום הרפורמה", "az": "İslahatdan əvvəlki rus orfoqrafiyası", "uz": "Islohotgacha bo‘lgan ruscha imlo"
    },
    "NM_CP_PreReformRussian_Desc": {
        "tr": "Giden iletiler 1918 öncesi yazımla.",
        "ar": "الرسائل الصادرة بالتهجئة السابقة لعام 1918.",
        "fa": "پیام‌های ارسالی با املای قبل از ۱۹۱۸.",
        "iw": "הודעות יוצאות באיות מלפני 1918.",
        "az": "1918-ci ildən əvvəlki orfoqrafiya ilə çıxan mesajlar.",
        "uz": "Chiquvchi xabarlar 1918-yilgacha bo‘lgan imloda."
    },
    "NM_CP_SnowflakesInChat": {
        "tr": "Sohbette kar yağışı", "ar": "ثلج في الدردشة", "fa": "برف در چت",
        "iw": "שלג בצ\'אט", "az": "Çatda qar", "uz": "Chatda qor"
    },
    "NM_CP_WeekdayNearDate": {
        "tr": "Tarihin yanında haftanın gününü göster", "ar": "إظهار يوم الأسبوع بجانب التاريخ", "fa": "نمایش روز هفته در کنار تاریخ",
        "iw": "הצג יום בשבוע ליד התאריך", "az": "Tarixin yanında həftənin gününü göstər", "uz": "Sana yonida hafta kunini ko‘rsatish"
    },
    "NM_CP_WeekdayNearDate_Desc": {
        "tr": "Sohbette tarihin yanına haftanın gününü ekler.",
        "ar": "إضافة يوم الأسبوع بجوار التاريخ في الدردشة.",
        "fa": "افزودن روز هفته به کنار تاریخ در چت.",
        "iw": "מוסיף את היום בשבוע ליד התאריך בצ\'אט.",
        "az": "Çatda tarixin yanına həftənin gününü əlavə edir.",
        "uz": "Chatda sana yoniga hafta kunini qo‘shadi."
    },
    "NM_Camera_FaceDetection": {
        "tr": "Yüz Algılama", "ar": "التعرف على الوجوه", "fa": "تشخیص چهره",
        "iw": "זיהוי פנים", "az": "Üz tanıma", "uz": "Yuzni aniqlash"
    },
    "NM_Camera_FaceDetection_Desc": {
        "tr": "Kaydederken pozlamayı ve odaklamayı yüzlere önceliklendir.",
        "ar": "إعطاء الأولوية للوجوه للتعرض والتركيز أثناء التسجيل.",
        "fa": "اولویت دادن به چهره‌ها برای نوردهی و فوکوس هنگام ضبط.",
        "iw": "תעדוף פנים לחשיפה ומיקוד בעת הקלטה.",
        "az": "Çəkiliş zamanı fokus və ekspozisiyada üzlərə üstünlük verin.",
        "uz": "Yozish paytida avtoekspozitsiya va fokusni yuzlarga yo‘naltirish."
    },
    "NM_Camera_NoiseReduction": {
        "tr": "Gürültü Azaltma", "ar": "تقليل الضوضاء", "fa": "کاهش نویز",
        "iw": "הפחתת רעש", "az": "Səs-küyün azaldılması", "uz": "Shovqinni pasaytirish"
    },
    "NM_Camera_NoiseReduction_Desc": {
        "tr": "Daha yüksek kayıt kalitesi için zamansal gürültü azaltma uygulayın.",
        "ar": "تطبيق تقليل الضوضاء لجودة تسجيل أعلى.",
        "fa": "اعمال کاهش نویز موقت برای کیفیت بالاتر ضبط.",
        "iw": "החל הפחתת רעש זמנית לאיכות הקלטה גבוהה יותר.",
        "az": "Daha yüksək çəkiliş keyfiyyəti üçün səs-küyü azaldın.",
        "uz": "Yozish sifatini oshirish uchun vaqtinchalik shovqinni pasaytirish."
    },
    "NM_Camera_UseHighRange": {
        "tr": "Yüksek FPS Aralığı", "ar": "نطاق FPS مرتفع", "fa": "محدوده FPS بالا",
        "iw": "טווח FPS גבוה", "az": "Yüksək FPS diapazonu", "uz": "Yuqori FPS diapazoni"
    },
    "NM_Camera_UseHighRange_Desc": {
        "tr": "FPS seçicide 30-60 ve 60-60 aralıklarını gösterin.",
        "ar": "عرض نطاقات 30-60 و 60-60 FPS في أداة التحديد.",
        "fa": "نمایش محدوده‌های ۳۰-۶۰ و ۶۰-۶۰ در انتخابگر FPS.",
        "iw": "הצג טווחים של 30-60 ו-60-60 בבורר FPS.",
        "az": "FPS seçicisində 30-60 və 60-60 diapazonlarını göstərin.",
        "uz": "FPS tanlagichda 30-60 va 60-60 oraliqlarini ko‘rsatish."
    },
    "NM_Chat_Section": {
        "tr": "Sohbetler", "ar": "المحادثات", "fa": "گفتگوها",
        "iw": "צ\'אטים", "az": "Çatlar", "uz": "Chatlar"
    },
    "NM_ChooseFolderIcon": {
        "tr": "Simge Seçin", "ar": "اختر أيقونة", "fa": "انتخاب یک آیکون",
        "iw": "בחר אייקון", "az": "İkon seçin", "uz": "Belgini tanlang"
    },
    "NM_Config_Error": {
        "tr": "Başarısız oldu", "ar": "فشل", "fa": "ناموفق",
        "iw": "נכשל", "az": "Uğursuz oldu", "uz": "Amalga oshmadi"
    },
    "NM_Config_Export": {
        "tr": "Ayarları dışa aktar", "ar": "تصدير الإعدادات", "fa": "خروجی گرفتن از تنظیمات",
        "iw": "ייצוא הגדרות", "az": "Tənzimləmələri ixrac et", "uz": "Sozlamalarni eksport qilish"
    },
    "NM_Config_Exported": {
        "tr": "Ayarlar dosyaya kaydedildi", "ar": "تم حفظ الإعدادات في ملف", "fa": "تنظیمات در فایل ذخیره شد",
        "iw": "ההגדרות נשמרו בקובץ", "az": "Tənzimləmələr fayla yazıldı", "uz": "Sozlamalar faylga saqlandi"
    },
    "NM_Config_Header": {
        "tr": "Yapılandırma", "ar": "التكوين", "fa": "پیکربندی",
        "iw": "תצורה", "az": "Konfiqurasiya", "uz": "Konfiguratsiya"
    },
    "NM_Config_Import": {
        "tr": "Ayarları içe aktar", "ar": "استيراد الإعدادات", "fa": "وارد کردن تنظیمات",
        "iw": "ייבוא הגדרות", "az": "Tənzimləmələri idxal et", "uz": "Sozlamalarni import qilish"
    },
    "NM_Config_ImportEmpty": {
        "tr": "Seçilen dosya bir SoundGram yapılandırması değil", "ar": "الملف المحدد ليس تكوين SoundGram",
        "fa": "فایل انتخابی پیکربندی SoundGram نیست", "iw": "הקובץ שנבחר אינו תצורת SoundGram",
        "az": "Seçilmiş fayl SoundGram konfiqurasiyası deyil", "uz": "Tanlangan fayl SoundGram konfiguratsiyasi emas"
    },
    "NM_Config_Info": {
        "tr": "Ayarları bir dosyaya kaydedin veya başka bir cihazda geri yükleyin.",
        "ar": "احفظ الإعدادات في ملف أو استعدها على جهاز آخر.",
        "fa": "تنظیمات را در یک فایل ذخیره کرده یا در دستگاه دیگری بازیابی کنید.",
        "iw": "שמור הגדרות לקובץ או שחזר אותן במכשיר אחר.",
        "az": "Tənzimləmələri faylda saxlayın və ya başqa cihazda bərpa edin.",
        "uz": "Sozlamalarni faylga saqlang yoki boshqa qurilmada tiklang."
    },
    "NM_CustomTitle": {
        "tr": "Uygulama başlığı", "ar": "عنوان التطبيق", "fa": "عنوان برنامه",
        "iw": "כותרת אפליקציה", "az": "Tətbiq başlığı", "uz": "Ilova sarlavhasi"
    },
    "NM_DBG_AudioSource": {
        "tr": "Mikrofon ses kaynağı", "ar": "مصدر صوت الميكروفون", "fa": "منبع صوتی میکروفون",
        "iw": "מקור שמע מיקרופון", "az": "Mikrofon səs mənbəyi", "uz": "Mikrofon audio manbasi"
    },
    "NM_DBG_EditTextFix": {
        "tr": "EditText önerileri düzeltmesi", "ar": "إصلاح اقتراحات EditText", "fa": "رفع مشکل پیشنهادات EditText",
        "iw": "תיקון הצעות EditText", "az": "EditText təklifləri düzəlişi", "uz": "EditText tavsiyalari tuzatmasi"
    },
    "NM_DBG_ForceCrash": {
        "tr": "Çökmeyi zorla", "ar": "فرض الإغلاق الإجباري", "fa": "توقف اجباری",
        "iw": "אלץ קריסה", "az": "Məcburi çöküş", "uz": "Majburiy to‘xtatish"
    },
    "NM_DBG_ForceCrash_Desc": {
        "tr": "Hata bildirimini test etmek için kasıtlı bir kilitlenme tetikler.",
        "ar": "يؤدي إلى تعطل متعمد لاختبار تقارير الأعطال.",
        "fa": "یک توقف عمدی برای آزمایش گزارش خطا ایجاد می‌کند.",
        "iw": "מפעיל קריסה מכוונת לבדיקת דיווח שגיאות.",
        "az": "Hesabatları yoxlamaq üçün qəsdən qəza yaradır.",
        "uz": "Xatoliklar haqida hisobotni sinash uchun qasddan to‘xtatish."
    },
    "NM_DBG_Header_Danger": {
        "tr": "Tehlike bölgesi", "ar": "منطقة الخطر", "fa": "منطقه خطر",
        "iw": "אזור סכנה", "az": "Təhlükəli zona", "uz": "Xavfli hudud"
    },
    "NM_DBG_HideVideoTimestamp": {
        "tr": "Video zaman damgasını gizle", "ar": "إخفاء الطابع الزمني للفيديو", "fa": "پنهان کردن برچسب زمان ویدیو",
        "iw": "הסתר חותמת זמן של וידאו", "az": "Video vaxt nişanını gizlət", "uz": "Video vaqt belgisini yashirish"
    },
    "NM_DBG_JacksonJSONProvider": {
        "tr": "Jackson JSON sağlayıcısını kullan", "ar": "استخدام موفر Jackson JSON", "fa": "استفاده از ارائه‌دهنده Jackson JSON",
        "iw": "השתמש בספק Jackson JSON", "az": "Jackson JSON təminatçısından istifadə et", "uz": "Jackson JSON provayderidan foydalanish"
    },
    "NM_DBG_OldTimeStyle": {
        "tr": "Sohbetlerde varsayılan zaman stili", "ar": "نمط الوقت الافتراضي في المحادثات", "fa": "سبک زمان پیش‌فرض در گفتگوها",
        "iw": "סגנון זמן ברירת מחדל בצ\'אטים", "az": "Çatlarda standart vaxt tərzi", "uz": "Chatlarda standart vaqt uslubi"
    },
    "NM_DBG_ReplacePunctuation": {
        "tr": "Noktalama işaretlerini değiştir", "ar": "استبدال علامات الترقيم", "fa": "جایگزینی علائم نگارشی",
        "iw": "החלף סימני פיסוק", "az": "Durğu işarələrini əvəz et", "uz": "Tinish belgilarini almashtirish"
    },
    "NM_DBG_SendMaxQuality": {
        "tr": "Videoları maksimum kalitede gönder", "ar": "إرسال الفيديوهات بأقصى جودة", "fa": "ارسال ویدیو با حداکثر کیفیت",
        "iw": "שלח סרטונים באיכות מרבית", "az": "Videoları maksimum keyfiyyətdə göndər", "uz": "Videolarni maksimal sifatda yuborish"
    },
    "NM_DBG_ShowAccounts": {
        "tr": "Çekmecede hesapları göster", "ar": "إظهار الحسابات في القائمة الجانبية", "fa": "نمایش حساب‌ها در منوی کشویی",
        "iw": "הצג חשבונות בתפריט הצד", "az": "Hesabları yan menyuda göstər", "uz": "Hisoblarni yon menyuda ko‘rsatish"
    },
    "NM_DBG_ShowRPCErrors": {
        "tr": "RPC hatalarını göster", "ar": "إظهار أخطاء RPC", "fa": "نمایش خطاهای RPC",
        "iw": "הצג שגיאות RPC", "az": "RPC xətalarını göstər", "uz": "RPC xatoliklarini ko‘rsatish"
    },
    "NM_DeleteAllFromSelf": {
        "tr": "Kendi mesajlarımı sil", "ar": "حذف رسائلي", "fa": "حذف پیام‌های من",
        "iw": "מחק את ההודעות שלי", "az": "Mənim mesajlarımı sil", "uz": "Mening xabarlarimni o‘chirish"
    },
    "NM_DisableSendHints": {
        "tr": "Gönderme ipuçlarını devre dışı bırak", "ar": "تعطيل تلميحات الإرسال", "fa": "غیرفعال کردن نکات ارسال",
        "iw": "בטל רמזי שליחה", "az": "Göndərmə ipucularını söndür", "uz": "Yuborish maslahatlarini o‘chirish"
    },
    "NM_DisableSendHints_Desc": {
        "tr": "Zamanlama ve sessiz gönderme ipucunu gizleyin.",
        "ar": "إخفاء تلميح الجدولة والإرسال بدون صوت.",
        "fa": "پنهان کردن نکته زمان‌بندی و ارسال بی‌صدا.",
        "iw": "הסתר רמז לתזמון ושליחה שקטה.",
        "az": "Planlaşdırma və səssiz göndərmə ipucunu gizlədin.",
        "uz": "Rejalashtirish va ovozsiz yuborish maslahatini yashirish."
    },
    "NM_DoubleTap_EditOrReact": {
        "tr": "Kendi mesajını düzenle, diğerlerine tepki ver", "ar": "تعديل رسائلك، والتفاعل مع الآخرين", "fa": "ویرایش پیام خود، واکنش به دیگران",
        "iw": "ערוך את שלי, הגב לאחרים", "az": "Özününkünü düzəlt, başqasına reaksiya ver", "uz": "O‘zinikini tahrirlash, boshqalarnikiga reaksiya"
    },
    "NM_EXP_DiscussInsteadOfMute": {
        "tr": "Kaydırmada sessize almak yerine tartış", "ar": "مناقشة بدلاً من الكتم عند التمرير", "fa": "بحث به جای بی‌صدا کردن با کشیدن",
        "iw": "דיון במקום השתקה בהחלקה", "az": "Sürüşdürərkən səssiz etmək əvəzinə müzakirə", "uz": "Siljishda ovozsiz qilish o‘rniga muhokama"
    },
    "NM_EXP_EdgeToEdge": {
        "tr": "Kenardan kenara modu", "ar": "وضع الحافة إلى الحافة", "fa": "حالت لبه به لبه",
        "iw": "מצב קצה לקצה", "az": "Kənardan kənara rejimi", "uz": "Chetdan chetgacha rejimi"
    },
    "NM_EXP_Footer": {
        "tr": "Bu seçenekler deneyseldir — hatalar olabilir.",
        "ar": "هذه الخيارات تجريبية — قد تحدث أخطاء.",
        "fa": "این گزینه‌ها آزمایشی هستند — ممکن است اشکالاتی وجود داشته باشد.",
        "iw": "אפשרויות אלו ניסיוניות — צפה לתקלות.",
        "az": "Bu seçimlər eksperimentaldır — xətalar mümkündür.",
        "uz": "Bu sozlamalar eksperimental — xatoliklar bo‘lishi mumkin."
    },
    "NM_EXP_Header_Beta": {
        "tr": "Beta özellikleri", "ar": "ميزات بيتا", "fa": "قابلیت‌های بتا",
        "iw": "מתגי בטא", "az": "Beta funksiyalar", "uz": "Beta funksiyalar"
    },
    "NM_EXP_NoRounding": {
        "tr": "Köşe yuvarlamayı devre dışı bırak", "ar": "تعطيل تدوير الزوايا", "fa": "غیرفعال کردن گردی گوشه‌ها",
        "iw": "בטל עיגול פינות", "az": "Künclərin yuvarlaqlaşdırılmasını söndür", "uz": "Burchaklarni yumaloqlashni o‘chirish"
    },
    "NM_EXP_Title": {
        "tr": "Deneysel", "ar": "تجريبي", "fa": "آزمایشی",
        "iw": "ניסיוני", "az": "Eksperimental", "uz": "Eksperimental"
    },
    "NM_Effects_Header": {
        "tr": "Efektler", "ar": "التأثيرات", "fa": "جلوه‌ها",
        "iw": "אפקטים", "az": "Effektlər", "uz": "Effektlar"
    },
    "NM_FO_FolderNameInHeader": {
        "tr": "Başlıkta klasör adı", "ar": "اسم المجلد في الرأس", "fa": "نام پوشه در سربرگ",
        "iw": "שם התיקייה בכותרת העליונה", "az": "Başlıqda qovluq adı", "uz": "Sarlavhada jild nomi"
    },
    "NM_FO_FoldersAtBottom": {
        "tr": "Klasörleri altta göster", "ar": "إظهار المجلدات في الأسفل", "fa": "نمایش پوشه‌ها در پایین",
        "iw": "הצג תיקיות למטה", "az": "Qovluqları aşağıda göstər", "uz": "Jildlarni pastda ko‘rsatish"
    },
    "NM_FO_FoldersAtBottom_Desc": {
        "tr": "Klasör sekmelerini sohbet listesinin altına taşır.",
        "ar": "نقل علامات تبويب المجلدات إلى أسفل قائمة الدردشة.",
        "fa": "تب‌های پوشه را به پایین لیست چت منتقل می‌کند.",
        "iw": "מעביר את לשוניות התיקיות לתחתית רשימת הצ\'אטים.",
        "az": "Qovluq tablarını çat siyahısının altına köçürür.",
        "uz": "Jildlar bo‘limini chatlar ro‘yxatining pastki qismiga o‘tkazadi."
    },
    "NM_FO_TabModeIcon": {
        "tr": "Yalnızca simgeler", "ar": "أيقونات فقط", "fa": "فقط آیکون‌ها",
        "iw": "סמלים בלבד", "az": "Yalnız ikonlar", "uz": "Faqat belgilar"
    },
    "NM_FO_TabModeMix": {
        "tr": "Başlıklı simgeler", "ar": "أيقونات مع عناوين", "fa": "آیکون‌ها با عنوان",
        "iw": "סמלים עם כותרות", "az": "Başlıqlı ikonlar", "uz": "Nomli belgilar"
    },
    "NM_FO_TabModeText": {
        "tr": "Yalnızca başlıklar", "ar": "عناوين فقط", "fa": "فقط عناوین",
        "iw": "כותרות בלבד", "az": "Yalnız başlıqlar", "uz": "Faqat nomlar"
    },
    "NM_FO_TabStyle": {
        "tr": "Sekme stili", "ar": "نمط التبويب", "fa": "سبک تب",
        "iw": "סגנון לשונית", "az": "Tab tərzi", "uz": "Bo‘lim uslubi"
    },
    "NM_FO_TabStyleStroke": {
        "tr": "Kenarlık ekle", "ar": "إضافة إطار", "fa": "افزودن خط دور",
        "iw": "הוסף קו תוחם", "az": "Haşiyə əlavə et", "uz": "Hoshiya qo‘shish"
    },
    "NM_FO_TabStyleStroke_Desc": {
        "tr": "Etkin sekmeyi bir anahatla vurgular.",
        "ar": "تمييز التبويب النشط بإطار خارجي.",
        "fa": "تب فعال را با یک خط دور برجسته می‌کند.",
        "iw": "מדגיש את הלשונית הפעילה באמצעות קו מתאר.",
        "az": "Aktiv tabı xətlə fərqləndirir.",
        "uz": "Faol bo‘limni hoshiya bilan ajratib ko‘rsatadi."
    },
    "NM_FO_TabsHideAllChats": {
        "tr": "«Tüm sohbetler» sekmesini gizle", "ar": "إخفاء تبويب «جميع المحادثات»", "fa": "پنهان کردن تب «همه گفتگوها»",
        "iw": "הסתר לשונית «כל הצ\'אטים»", "az": "«Bütün çatlar» tabını gizlət", "uz": "«Barcha chatlar» bo‘limini yashirish"
    },
    "NM_FO_TabsNoCounter": {
        "tr": "Sekmelerdeki sayacı gizle", "ar": "إخفاء العداد في التبويبات", "fa": "پنهان کردن شمارنده در تب‌ها",
        "iw": "הסתר מונה בלשוניות", "az": "Tablardakı sayğacı gizlət", "uz": "Bo‘limlardagi hisoblagichni yashirish"
    },
    "NM_ForceBlur": {
        "tr": "Arayüz bulanıklığı", "ar": "ضبابية الواجهة", "fa": "تاری رابط کاربری",
        "iw": "טשטוש ממשק", "az": "İnterfeys bulanıqlığı", "uz": "Interfeys xiraligi"
    },
    "NM_ForumAvatarsLikeChats": {
        "tr": "Forum avatar şekli", "ar": "شكل الصور الرمزية في المنتديات", "fa": "شکل آواتار در انجمن‌ها",
        "iw": "צורת תמונת פרופיל בפורומים", "az": "Forumlarda avatar forması", "uz": "Forumlardagi avatar shakli"
    },
    "NM_ForumAvatarsLikeChats_Desc": {
        "tr": "Sohbet avatarlarıyla aynı köşeleri kullanın.",
        "ar": "استخدام نفس زوايا الصور الرمزية في المحادثات.",
        "fa": "استفاده از همان گوشه‌های آواتار گفتگوها.",
        "iw": "השתמש באותן פינות כמו בתמונות פרופיל של צ\'אט.",
        "az": "Çat avatarları ilə eyni künclərdən istifadə edin.",
        "uz": "Chat avatarlari bilan bir xil burchaklardan foydalanish."
    },
    "NM_GEN_DeletedGifts": {
        "tr": "Silinen Yıldız hediyelerini göster", "ar": "إظهار هدايا النجوم المحذوفة", "fa": "نمایش هدایای ستاره حذف شده",
        "iw": "הצג מתנות כוכב שנמחקו", "az": "Silinmiş Ulduz hədiyyələrini göstər", "uz": "O‘chirilgan Yulduzli sovg‘alarni ko‘rsatish"
    },
    "NM_GEN_DeletedGifts_Desc": {
        "tr": "Gizli hediyeleri gönderebilmeniz için kataloğa geri ekler.",
        "ar": "إعادة إدراج الهدايا المخفية في الكتالوج لتتمكن من إرسالها.",
        "fa": "هدایای پنهان شده را برای ارسال مجدد به کاتالوگ بازمی‌گرداند.",
        "iw": "מחזיר מתנות מוסתרות לקטלוג כדי שתוכל לשלוח אותן.",
        "az": "Gizli hədiyyələri kataloqa qaytarır ki, göndərə biləsiniz.",
        "uz": "Yashirin sovg‘alarni yuborishingiz uchun katalogga qaytaradi."
    },
    "NM_GEN_LocalPremiumEmoji": {
        "tr": "Yerel premium emojiler", "ar": "رموز تعبيرية مميزة محلية", "fa": "اموجی‌های پرمیوم محلی",
        "iw": "אימוג\'י פרימיום מקומי", "az": "Lokal premium emojilər", "uz": "Lokal premium emojilar"
    },
    "NM_GEN_LocalPremiumEmoji_Desc": {
        "tr": "Premium aboneliği olmadan premium emojiler gönderin.",
        "ar": "إرسال رموز تعبيرية مميزة بدون اشتراك Premium.",
        "fa": "ارسال اموجی‌های پرمیوم بدون اشتراک تلگرام پرمیوم.",
        "iw": "שלח אימוג\'י פרימיום ללא מנוי פרימיום.",
        "az": "Premium abunəliyi olmadan premium emojilər göndərin.",
        "uz": "Premium obunasiz premium emojilarni yuborish."
    },
    "NM_GEN_MiscHeader": {
        "tr": "Çeşitli", "ar": "متنوع", "fa": "متفرقه",
        "iw": "שונות", "az": "Müxtəlif", "uz": "Boshqa"
    },
    "NM_GE_UploadSpeedBoost": {
        "tr": "Daha hızlı yüklemeler", "ar": "رفع أسرع", "fa": "آپلود سریع‌تر",
        "iw": "העלאות מהירות יותר", "az": "Daha sürətli göndərmə", "uz": "Tezroq yuklash"
    },
    "NM_HUB_ForkedFromCherrygramSub": {
        "tr": "SoundGram resmi Telegram üzerinde oluşturulmuştur; özellikler Cherrygram\'dan aktarılmıştır.",
        "ar": "تم بناء SoundGram على تطبيق تيليجرام الرسمي؛ وتم نقل الميزات من Cherrygram.",
        "fa": "برنامه SoundGram بر پایه تلگرام رسمی ساخته شده و امکانات از Cherrygram اضافه شده است.",
        "iw": "SoundGram מבוסס על טלגרם הרשמי; תכונות הועברו מ-Cherrygram.",
        "az": "SoundGram rəsmi Telegram üzərində qurulub; funksiyalar Cherrygram-dan köçürülüb.",
        "uz": "SoundGram rasmiy Telegram asosida yaratilgan; funksiyalar Cherrygramdan ko‘chirilgan."
    },
    "NM_HUB_SourceCode": {
        "tr": "Kaynak kodu", "ar": "الرمز المصدري", "fa": "کد منبع",
        "iw": "קוד מקור", "az": "Mənbə kodu", "uz": "Dastur kodi"
    },
    "NM_HideActionBarStatus": {
        "tr": "Başlık çubuğu durumunu gizle", "ar": "إخفاء الحالة في الشريط العلوي", "fa": "پنهان کردن وضعیت در نوار بالا",
        "iw": "הסתר סטטוס בכותרת", "az": "Yuxarı paneldə statusu gizlət", "uz": "Yuqori paneldagi statusni yashirish"
    },
    "NM_HideBubbleTail": {
        "tr": "Mesaj kuyruklarını gizle", "ar": "إخفاء ذيول الرسائل", "fa": "پنهان کردن دم حباب پیام",
        "iw": "הסתר זנבות הודעות", "az": "Mesaj quyruqlarını gizlət", "uz": "Xabar dumlarini yashirish"
    },
    "NM_HideBubbleTail_Desc": {
        "tr": "Mesaj kuyrukları olmadan yuvarlatılmış köşeler kullanın.",
        "ar": "استخدام زوايا مستديرة بدون ذيول للرسائل.",
        "fa": "استفاده از گوشه‌های گرد بدون دم پیام.",
        "iw": "השתמש בפינות מעוגלות ללא זנבות הודעה.",
        "az": "Mesaj quyruğu olmadan yuvarlaq künclərdən istifadə edin.",
        "uz": "Xabar dumlarisiz yumaloqlangan burchaklardan foydalanish."
    },
    "NM_IconPack_DefaultSubtitle": {
        "tr": "Standart Telegram simgeleri", "ar": "أيقونات تيليجرام الافتراضية", "fa": "آیکون‌های استاندارد تلگرام",
        "iw": "אייקוני טלגרם רגילים", "az": "Standart Telegram ikonları", "uz": "Standart Telegram belgilari"
    },
    "NM_IconPack_DefaultTitle": {
        "tr": "Varsayılan", "ar": "افتراضي", "fa": "پیش‌فرض",
        "iw": "ברירת מחדל", "az": "Standart", "uz": "Standart"
    },
    "NM_IconPack_LiquidSubtitle": {
        "tr": "Liquid Glass simge paketi", "ar": "حزمة أيقونات Liquid Glass", "fa": "بسته آیکون Liquid Glass",
        "iw": "חבילת אייקונים Liquid Glass", "az": "Liquid Glass ikon paketi", "uz": "Liquid Glass belgilar to‘plami"
    },
    "NM_IconPack_LiquidTitle": {
        "tr": "Liquid Glass", "ar": "Liquid Glass", "fa": "Liquid Glass",
        "iw": "Liquid Glass", "az": "Liquid Glass", "uz": "Liquid Glass"
    },
    "NM_IconPack_PlumpySubtitle": {
        "tr": "Plumpy simge paketi", "ar": "حزمة أيقونات Plumpy", "fa": "بسته آیکون Plumpy",
        "iw": "חבילת אייקונים Plumpy", "az": "Plumpy ikon paketi", "uz": "Plumpy belgilar to‘plami"
    },
    "NM_IconPack_PlumpyTitle": {
        "tr": "Plumpy", "ar": "Plumpy", "fa": "Plumpy",
        "iw": "Plumpy", "az": "Plumpy", "uz": "Plumpy"
    },
    "NM_IconPack_SolarSubtitle": {
        "tr": "Yerleşik Solar simge paketi", "ar": "حزمة أيقونات Solar المدمجة", "fa": "بسته آیکون پیش‌فرض Solar",
        "iw": "חבילת אייקונים מובנית Solar", "az": "Daxili Solar ikon paketi", "uz": "O‘rnatilgan Solar belgilar to‘plami"
    },
    "NM_IconPack_SolarTitle": {
        "tr": "Solar", "ar": "Solar", "fa": "Solar",
        "iw": "Solar", "az": "Solar", "uz": "Solar"
    },
    "NM_JumpToBeginning": {
        "tr": "Başa dön", "ar": "إلى البداية", "fa": "به ابتدا",
        "iw": "להתחלה", "az": "Əvvələ qayıt", "uz": "Boshiga o‘tish"
    },
    "NM_MI_ClearFromCache": {
        "tr": "Önbellekten temizle", "ar": "مسح من التخزين المؤقت", "fa": "پاک کردن از حافظه پنهان",
        "iw": "נקה מהמטמון", "az": "Keşdən təmizlə", "uz": "Keshdan tozalash"
    },
    "NM_MI_CopyPhoto": {
        "tr": "Fotoğrafı kopyala", "ar": "نسخ الصورة", "fa": "کپی عکس",
        "iw": "העתק תמונה", "az": "Fotonu kopyala", "uz": "Rasmdan nusxa olish"
    },
    "NM_MI_CopyPhotoAsSticker": {
        "tr": "Fotoğrafı çıkartma olarak kopyala", "ar": "نسخ الصورة كملصق", "fa": "کپی عکس به عنوان استیکر",
        "iw": "העתק תמונה כמדבקה", "az": "Fotonu stiker kimi kopyala", "uz": "Rasmni stiker sifatida nusxalash"
    },
    "NM_MI_Details": {
        "tr": "Ayrıntılar", "ar": "التفاصيل", "fa": "جزئیات",
        "iw": "פרטים", "az": "Təfərrüatlar", "uz": "Tafsilotlar"
    },
    "NM_MI_DownloadSticker": {
        "tr": "Çıkartmayı indir", "ar": "تنزيل الملصق", "fa": "دانلود استیکر",
        "iw": "הורד מדבקה", "az": "Stikeri endir", "uz": "Stikerni yuklab olish"
    },
    "NM_MI_ForwardWoAuthorship": {
        "tr": "Yazarsız ilet", "ar": "تحويل بدون كاتب", "fa": "هدایت بدون فرستنده",
        "iw": "העבר ללא מחבר", "az": "Müəllifsiz yönləndir", "uz": "Muallifsiz uzatish"
    },
    "NM_MI_ForwardWoCaption": {
        "tr": "Açıklamasız ilet", "ar": "تحويل بدون شرح", "fa": "هدایت بدون متن زیرنویس",
        "iw": "העבר ללא כיתוב", "az": "İmzasız yönləndir", "uz": "Izohsiz uzatish"
    },
    "NM_MI_SaveToSaved": {
        "tr": "Kayıtlı Mesajlara Kaydet", "ar": "حفظ في الرسائل المحفوظة", "fa": "ذخیره در پیام‌های ذخیره شده",
        "iw": "שמור בהודעות שמורות", "az": "Saxlanılan mesajlara yaz", "uz": "Saqlangan xabarlarga saqlash"
    },
    "NM_MM_MessageMenuItems": {
        "tr": "Mesaj menüsü öğeleri", "ar": "عناصر قائمة الرسائل", "fa": "گزینه‌های منوی پیام",
        "iw": "פריטי תפריט הודעות", "az": "Mesaj menyusu elementləri", "uz": "Xabar menyusi elementlari"
    },
    "NM_MP_CustomizeMessage": {
        "tr": "Mesajı özelleştir", "ar": "تخصيص الرسالة", "fa": "شخصی‌سازی پیام",
        "iw": "התאם אישית הודעה", "az": "Mesajı fərdiləşdir", "uz": "Xabarni sozlash"
    },
    "NM_MP_CustomizeProfile": {
        "tr": "Profili özelleştir", "ar": "تخصيص الملف الشخصي", "fa": "شخصی‌سازی نمایه",
        "iw": "התאם אישית פרופיל", "az": "Profili fərdiləşdir", "uz": "Profilni sozlash"
    },
    "NM_MP_DisablePremiumStatuses": {
        "tr": "Premium durumları devre dışı bırak", "ar": "تعطيل حالات Premium", "fa": "غیرفعال کردن وضعیت‌های پرمیوم",
        "iw": "בטל סטטוסי פרימיום", "az": "Premium statusları söndür", "uz": "Premium statuslarni o‘chirish"
    },
    "NM_MP_DisablePremiumStatuses_Desc": {
        "tr": "Yan menü dışındaki Premium emoji durumlarını gizle",
        "ar": "إخفاء حالات الرموز المميزة خارج القائمة الجانبية",
        "fa": "پنهان کردن وضعیت اموجی پرمیوم در خارج از منوی کناری",
        "iw": "הסתר סטטוסי אימוג\'י פרימיום מחוץ לתפריט הצד",
        "az": "Yan menyudan kənar Premium emoji statuslarını gizlət",
        "uz": "Yon menyudan tashqaridagi Premium emoji statuslarini yashirish"
    },
    "NM_MP_ProfileBackgroundColor": {
        "tr": "Profil arka planı", "ar": "الخلفية في الملف الشخصي", "fa": "پس‌زمینه در نمایه",
        "iw": "רקע בפרופיל", "az": "Profildə fon", "uz": "Profildagi fon"
    },
    "NM_MP_ProfileBackgroundEmoji": {
        "tr": "Profil arka planında emoji", "ar": "رمز في خلفية الملف الشخصي", "fa": "اموجی در پس‌زمینه نمایه",
        "iw": "אימוג\'י ברקע הפרופיל", "az": "Profil fonunda emoji", "uz": "Profil fonidagi emoji"
    },
    "NM_MP_ProfileBirthDatePreview": {
        "tr": "Doğum Tarihi", "ar": "تاريخ الميلاد", "fa": "تاریخ تولد",
        "iw": "תאריך לידה", "az": "Doğum tarixi", "uz": "Tug‘ilgan sana"
    },
    "NM_MP_ProfileBusinessPreview": {
        "tr": "Çalışma saatleri ve konum", "ar": "ساعات العمل والموقع", "fa": "ساعات کاری و مکان",
        "iw": "שעות פעילות ומיקום", "az": "İş saatları və məkan", "uz": "Ish soatlari va manzil"
    },
    "NM_MP_ProfileChannelPreview": {
        "tr": "Kişisel kanal", "ar": "القناة الشخصية", "fa": "کانال شخصی",
        "iw": "ערוץ אישי", "az": "Şəxsi kanal", "uz": "Shaxsiy kanal"
    },
    "NM_MP_ReplyBackground": {
        "tr": "Yanıtlarda arka plan", "ar": "الخلفية في الردود", "fa": "پس‌زمینه در پاسخ‌ها",
        "iw": "רקע בתגובات", "az": "Cavablarda fon", "uz": "Javoblardagi fon"
    },
    "NM_MP_ReplyBackgroundEmoji": {
        "tr": "Yanıtlarda ve önizlemede emoji", "ar": "رمز في الردود ومعاينة الروابط", "fa": "اموجی در پاسخ‌ها و پیش‌نمایش",
        "iw": "אימוג\'י בתגובות ובתצוגה מקדימה", "az": "Cavablarda və önbaxışda emoji", "uz": "Javoblar va oldindan ko‘rishdagi emoji"
    },
    "NM_MP_ReplyCustomColors": {
        "tr": "Yanıtlarda özel renkler", "ar": "ألوان مخصصة في الردود", "fa": "رنگ‌های سفارشی در پاسخ‌ها",
        "iw": "צבעים מותאמים אישית בתגובות", "az": "Cavablarda fərdi rənglər", "uz": "Javoblarda maxsus ranglar"
    },
    "NM_MP_ShowIdDc": {
        "tr": "Profilde ID ve DC göster", "ar": "إظهار المعرف و DC في الملف الشخصي", "fa": "نمایش شناسه و DC در نمایه",
        "iw": "הצג מזהה ו-DC בפרופיל", "az": "Profildə ID və DC göstər", "uz": "Profilda ID va DC ko‘rsatish"
    },
    "NM_MP_ShowSeconds": {
        "tr": "Zamanı saniye ile göster", "ar": "تنسيق الوقت بالثواني", "fa": "قالب زمان همراه با ثانیه",
        "iw": "פורמט זמן עם שניות", "az": "Vaxtı saniyələrlə göstər", "uz": "Vaqtni soniyalar bilan ko‘rsatish"
    },
    "NM_MP_ShowSeconds_Desc": {
        "tr": "12:34 -> 12:34:56", "ar": "12:34 -> 12:34:56", "fa": "12:34 -> 12:34:56",
        "iw": "12:34 -> 12:34:56", "az": "12:34 -> 12:34:56", "uz": "12:34 -> 12:34:56"
    },
    "NM_MSG_GifSpoilers": {
        "tr": "GIF\'leri spoiler ile gönder", "ar": "إرسال صور GIF مع حرق", "fa": "ارسال گیف با اسپویلر",
        "iw": "שלח גיפים עם ספוילר", "az": "GIF-ləri spoylerlə göndər", "uz": "GIFlarni spoyler bilan yuborish"
    },
    "NM_MSG_Header_Sending": {
        "tr": "Gönderme", "ar": "الإرسال", "fa": "ارسال",
        "iw": "שליחה", "az": "Göndərmə", "uz": "Yuborish"
    },
    "NM_MZ_LargerVoiceMessages": {
        "tr": "Uyarlanabilir sesli mesaj uzunluğu", "ar": "طول الرسالة الصوتية التكيفي", "fa": "طول تطبیقی پیام صوتی",
        "iw": "אורך הודעה קולית מסתגל", "az": "Səsli mesajların adaptiv uzunluğu", "uz": "Ovozli xabarlarning moslashuvchan uzunligi"
    },
    "NM_MZ_MediaAmplifier": {
        "tr": "Fotoğraflar ve videolar", "ar": "الصور ومقاطع الفيديو", "fa": "عکس‌ها و ویدیوها",
        "iw": "תמונות וסרטונים", "az": "Fotolar və videolar", "uz": "Rasmlar va videolar"
    },
    "NM_MZ_MediaAmplifier_Hint": {
        "tr": "Not: Medya sohbet genişliğine uyacak şekilde ölçeklenebilir.",
        "ar": "ملاحظة: قد يتم تقليص حجم الوسائط لتناسب عرض المحادثة.",
        "fa": "توجه: رسانه‌ها ممکن است متناسب با عرض چت کوچکتر شوند.",
        "iw": "הערה: ייתכן שהמדיה תוקטן כדי להתאים לרוחב הצ\'אט.",
        "az": "Qeyd: Media çat genişliyinə uyğun olaraq kiçildilə bilər.",
        "uz": "Eslatma: Media chat kengligiga mos ravishda kichraytirilishi mumkin."
    },
    "NM_MediaGlow": {
        "tr": "Medya parıltısı", "ar": "توهج الوسائط", "fa": "درخشش رسانه",
        "iw": "זוהר מדיה", "az": "Media parıltısı", "uz": "Media nurlanishi"
    },
    "NM_MediaGlow_Desc": {
        "tr": "Medya kenarlarını yumuşak bulanık bir arka planla doldurun.",
        "ar": "ملء حواف الوسائط بخلفية ضبابية ناعمة.",
        "fa": "پر کردن لبه‌های رسانه با پس‌زمینه تار و ملایم.",
        "iw": "מלא את קצוות המדיה ברקע מטושטש ועדין.",
        "az": "Media kənarlarını yumşaq bulanıq fonla doldurun.",
        "uz": "Media chetlarini yumshoq xiralashtirilgan fon bilan to‘ldirish."
    },
    "NM_Menu_Haptic": {
        "tr": "Dokunsal geri bildirim", "ar": "ردود الفعل اللمسية", "fa": "بازخورد لمسی",
        "iw": "משוב רטט", "az": "Toxunma əks-əlaqəsi", "uz": "Tebranishli javob"
    },
    "NM_Menu_Haptic_Desc": {
        "tr": "Mesaj menüsü açıldığında veya kaydırılarak kapatıldığında titretin.",
        "ar": "اهتزاز عند فتح قائمة الرسائل أو إغلاقها بالسحب.",
        "fa": "لرزش هنگام باز شدن یا بسته شدن منو با کشیدن.",
        "iw": "רטוט כאשר תפריט ההודעות נפתח או נסגר בהחלקה.",
        "az": "Menyu açıldıqda və ya sürüşdürməklə bağlandıqda titrəsin.",
        "uz": "Xabar menyusi ochilganda yoki siljib yopilganda tebranish."
    },
    "NM_Menu_Reorder": {
        "tr": "Menü öğelerini yeniden sırala", "ar": "إعادة ترتيب عناصر القائمة", "fa": "تغییر ترتیب گزینه‌های منو",
        "iw": "שנה סדר פריטי תפריט", "az": "Menyu elementlərinin sırasını dəyiş", "uz": "Menyu elementlari tartibini o‘zgartirish"
    },
    "NM_Menu_Reorder_Reset": {
        "tr": "Varsayılan sıraya sıfırla", "ar": "إعادة الترتيب الافتراضي", "fa": "بازنشانی به ترتیب پیش‌فرض",
        "iw": "אפס לסדר ברירת מחדל", "az": "Standart sıranı bərpa et", "uz": "Standart tartibni tiklash"
    },
    "NM_Menu_TelegramPlus": {
        "tr": "Modern mesaj menüsü", "ar": "قائمة رسائل حديثة", "fa": "منوی پیام مدرن",
        "iw": "תפריט הודעות מודרני", "az": "Müasir mesaj menyusu", "uz": "Zamonaviy xabarlar menyusi"
    },
    "NM_Menu_TelegramPlus_Desc": {
        "tr": "Gruplanmış eylemlerle Liquid Glass menüsü.",
        "ar": "قائمة Liquid Glass مع إجراءات مجمعة.",
        "fa": "منوی Liquid Glass با اقدامات گروهی.",
        "iw": "תפריט Liquid Glass עם פעולות מקובצות.",
        "az": "Qruplaşdırılmış hərəkətlərlə Liquid Glass menyusu.",
        "uz": "Guruhlangan amallar bilan Liquid Glass menyusi."
    },
    "NM_MsgSlideAction": {
        "tr": "Mesaj kaydırma eylemi", "ar": "الإجراء عند تمرير الرسالة", "fa": "عملیات هنگام کشیدن پیام",
        "iw": "פעולה בהחלקת הודעה", "az": "Mesajı sürüşdürmə fəaliyyəti", "uz": "Xabarni surish amali"
    },
    "NM_NM_AutoDownload": {
        "tr": "URL gönderildiğinde otomatik indir", "ar": "تنزيل تلقائي عند إرسال الرابط", "fa": "دانلود خودکار با ارسال پیوند",
        "iw": "הורדה אוטומטית בעת שליחת קישור", "az": "Link göndərildikdə avtomatik yükləmə", "uz": "Havola yuborilganda avtomatik yuklab olish"
    },
    "NM_NM_AutoDownload_Desc": {
        "tr": "Desteklenen bir bağlantı gönderildiğinde indirmeyi otomatik başlatır.",
        "ar": "إرسال رابط مدعوم يبدأ التنزيل تلقائيًا.",
        "fa": "ارسال پیوند پشتیبانی‌شده دانلود را خودکار آغاز می‌کند.",
        "iw": "שליחת הודעה המכילה קישור נתמך מתחילה את ההורדה אוטומטית.",
        "az": "Dəstəklənən link göndərildikdə yükləmə avtomatik başlayır.",
        "uz": "Qo‘llab-quvvatlanadigan havola yuborilganda yuklab olish avtomatik boshlanadi."
    },
    "NM_NM_FormatAudio": {
        "tr": "Ses", "ar": "صوت", "fa": "صدا",
        "iw": "שמע", "az": "Audio", "uz": "Audio"
    },
    "NM_NM_FormatVideo": {
        "tr": "Video", "ar": "فيديو", "fa": "ویدیو",
        "iw": "וידאו", "az": "Video", "uz": "Video"
    },
    "NM_NM_YtAsk": {
        "tr": "YouTube formatını sor", "ar": "السؤال عن تنسيق YouTube", "fa": "پرسش فرمت یوتیوب",
        "iw": "שאל על פורמט YouTube", "az": "YouTube formatını soruş", "uz": "YouTube formatini so‘rash"
    },
    "NM_NotificationReactionEmoji": {
        "tr": "Emoji", "ar": "رمز تعبيري", "fa": "اموجی",
        "iw": "אימוג\'י", "az": "Emoji", "uz": "Emoji"
    },
    "NM_NotificationReactions": {
        "tr": "Bildirimlerde tepkiler", "ar": "التفاعلات في الإشعارات", "fa": "واکنش‌ها در اعلان‌ها",
        "iw": "תגובות בהתראות", "az": "Bildirişlərdə reaksiyalar", "uz": "Bildirishnomalardagi reaksiyalar"
    },
    "NM_NotificationReactions_Desc": {
        "tr": "Mesaj bildirimlerine hızlı tepki düğmesi ekler.",
        "ar": "إضافة زر تفاعل سريع إلى إشعارات الرسائل.",
        "fa": "افزودن دکمه واکنش سریع به اعلان‌های پیام.",
        "iw": "מוסיף כפתור תגובה מהירה להתראות הודעות.",
        "az": "Mesaj bildirişlərinə sürətli reaksiya düyməsi əlavə edir.",
        "uz": "Xabar bildirishnomalariga tezkor reaksiya tugmasini qo‘shadi."
    },
    "NM_OnlineIndicatorInGroups": {
        "tr": "Gruplarda çevrim içi göstergesi", "ar": "مؤشر الاتصال في المجموعات", "fa": "نشانگر آنلاین در گروه‌ها",
        "iw": "מחוון מחובר בקבוצות", "az": "Qruplarda onlayn göstəricisi", "uz": "Guruhlardagi onlayn indikatori"
    },
    "NM_OnlineIndicatorInGroups_Desc": {
        "tr": "Grup avatarlarında çevrim içi noktası gösterin.",
        "ar": "إظهار نقطة اتصال خضراء على صور المجموعات.",
        "fa": "نمایش نقطه آنلاین روی آواتار افراد در گروه‌ها.",
        "iw": "הצג נקודה מחוברת על תמונות הפרופיל בקבוצות.",
        "az": "Qrup avatarlarında onlayn nöqtəsini göstər.",
        "uz": "Guruh avatarlarida onlayn nuqtani ko‘rsatish."
    },
    "NM_PR_AllowSystemPasscode": {
        "tr": "Cihaz ekran kilidine izin ver", "ar": "السماح بقفل شاشة الجهاز", "fa": "مجاز بودن قفل صفحه دستگاه",
        "iw": "אפשר נעילת מסך של המכשיר", "az": "Cihazın ekran kilidinə icazə ver", "uz": "Qurilma ekran blokirovkasiga ruxsat berish"
    },
    "NM_PR_AllowSystemPasscode_Desc": {
        "tr": "Biyometri yerine cihaz PIN\'ine veya desenine izin verin.",
        "ar": "السماح برمز PIN أو كلمة المرور بدلاً من القياسات الحيوية.",
        "fa": "مجاز بودن پین، رمز یا الگو به جای بیومتریک.",
        "iw": "אפשר קוד PIN, סיסמה או דפוס במקום ביומטריה.",
        "az": "Biometriya əvəzinə PIN və ya parola icazə verin.",
        "uz": "Biometriya o‘rniga PIN-kod, parol yoki grafik kalitga ruxsat berish."
    },
    "NM_PR_AskBioOpenArchive": {
        "tr": "Arşivi koru", "ar": "حماية الأرشيف", "fa": "محافظت از بایگانی",
        "iw": "הגן על הארכיון", "az": "Arxivi qoru", "uz": "Arxivni himoyalash"
    },
    "NM_PR_AskBioOpenArchive_Desc": {
        "tr": "Arşivi açmadan önce cihaz kimlik doğrulamasını zorunlu kılın.",
        "ar": "طلب مصادقة الجهاز قبل فتح الأرشيف.",
        "fa": "نیاز به احراز هویت دستگاه قبل از باز کردن بایگانی.",
        "iw": "דרוש אימות מכשיר לפני פתיחת הארכיון.",
        "az": "Arxivi açmazdan əvvəl cihaz təsdiqini tələb et.",
        "uz": "Arxivni ochishdan oldin qurilma tasdiqlashini talab qilish."
    },
    "NM_PR_AskBioOpenChats": {
        "tr": "Seçilen sohbetleri koru", "ar": "حماية المحادثات المحددة", "fa": "محافظت از گفتگوهای انتخابی",
        "iw": "הגן על צ\'אטים נבחרים", "az": "Seçilmiş çatarı qoru", "uz": "Tanlangan chatlarni himoyalash"
    },
    "NM_PR_AskBioOpenChats_Desc": {
        "tr": "Seçilen sohbetleri açmadan önce kimlik doğrulamasını zorunlu kılın.",
        "ar": "طلب مصادقة الجهاز قبل فتح المحادثات المحددة.",
        "fa": "نیاز به احراز هویت قبل از باز کردن چت‌های انتخاب شده زیر.",
        "iw": "דרוש אימות מכשיר לפני פתיחת צ\'אטים נבחרים.",
        "az": "Aşağıda seçilmiş çatarı açmazdan əvvəl təsdiq tələb et.",
        "uz": "Tanlangan chatlarni ochishdan oldin qurilma tasdiqlashini talab qilish."
    },
    "NM_PR_AskBioOpenEncrypted": {
        "tr": "Gizli sohbetleri koru", "ar": "حماية المحادثات السرية", "fa": "محافظت از گفتگوهای محرمانه",
        "iw": "הגן על צ\'אטים סודיים", "az": "Məxfi çatarı qoru", "uz": "Maxfiy chatlarni himoyalash"
    },
    "NM_PR_AskBioOpenEncrypted_Desc": {
        "tr": "Gizli bir sohbeti açmadan önce kimlik doğrulaması gerektir.",
        "ar": "طلب مصادقة الجهاز قبل فتح محادثة سرية.",
        "fa": "نیاز به احراز هویت دستگاه قبل از باز کردن چت محرمانه.",
        "iw": "דרוש אימות מכשיר לפני פתיחת צ\'אט סודי.",
        "az": "Məxfi çatı açmazdan əvvəl cihaz təsdiqini tələb et.",
        "uz": "Maxfiy chatni ochishdan oldin qurilma tasdiqlashini talab qilish."
    },
    "NM_PR_AskBioOpenSavedMessages": {
        "tr": "Kayıtlı Mesajları koru", "ar": "حماية الرسائل المحفوظة", "fa": "محافظت از پیام‌های ذخیره شده",
        "iw": "הגן על הודעות שמורות", "az": "Saxlanılan mesajları qoru", "uz": "Saqlangan xabarlarni himoyalash"
    },
    "NM_PR_AskBioOpenSavedMessages_Desc": {
        "tr": "Kayıtlı Mesajları açmadan önce biyometri veya kimlik doğrulaması isteyin.",
        "ar": "طلب المقاييس الحيوية أو المصادقة قبل فتح الرسائل المحفوظة.",
        "fa": "نیاز به بیومتریک یا احراز هویت قبل از باز کردن پیام‌های ذخیره شده.",
        "iw": "דרוש ביומטריה או אימות לפני פתיחת הודעות שמורות.",
        "az": "Saxlanılan mesajları açmazdan əvvəl biometriya və ya təsdiq tələb et.",
        "uz": "Saqlangan xabarlarni ochishdan oldin biometriya yoki tasdiqlashni talab qilish."
    },
    "NM_PR_AuthenticationRequired": {
        "tr": "Bu ayarı değiştirmek için kimlik doğrulaması gerekiyor", "ar": "المصادقة مطلوبة لتغيير هذا الإعداد", "fa": "احراز هویت برای تغییر این تنظیم الزامی است",
        "iw": "נדרש אימות כדי לשנות הגדרה זו", "az": "Bu tənzimləməni dəyişmək üçün autentifikasiya tələb olunur", "uz": "Bu sozlamani o‘zgartirish uchun autentifikatsiya talab qilinadi"
    },
    "NM_PR_AuthenticationUnavailable": {
        "tr": "Kimlik doğrulaması kullanılamıyor", "ar": "المصادقة غير متوفرة", "fa": "احراز هویت در دسترس نیست",
        "iw": "אימות אינו זמין", "az": "Autentifikasiya əlçatan deyil", "uz": "Autentifikatsiyadan foydalanib bo‘lmaydi"
    },
    "NM_PR_AuthenticationUnavailable_Desc": {
        "tr": "Kilit yöntemi yok. Bu koruma ayarı devre dışı bırakılsın mı?",
        "ar": "لا يوجد قفل متاح. هل تريد تعطيل إعداد الحماية هذا؟",
        "fa": "قفل صفحه موجود نیست. این حفاظت غیرفعال شود؟",
        "iw": "אין נעילת מסך זמינה. לבטל הגנה זו?",
        "az": "Cihaz kilidi mövcud deyil. Bu mühafizə tənzimləməsi söndürülsün?",
        "uz": "Ekran blokirovkasi mavjud emas. Bu himoya sozlamasi o‘chirilsinmi?"
    },
    "NM_PR_DeleteAccount": {
        "tr": "Telegram hesabını sil", "ar": "حذف حساب تيليجرام", "fa": "حذف حساب تلگرام",
        "iw": "מחק חשבון טלגרם", "az": "Telegram hesabını sil", "uz": "Telegram hisobini o‘chirish"
    },
    "NM_PR_Header_ChatProtection": {
        "tr": "Sohbet koruması", "ar": "حماية المحادثات", "fa": "محافظت از چت",
        "iw": "הגנת צ\'אטים", "az": "Çatların mühafizəsi", "uz": "Chatlar himoyasi"
    },
    "NM_PR_HideArchiveList": {
        "tr": "Arşivi sohbet listesinden gizle", "ar": "إخفاء الأرشيف من قائمة المحادثات", "fa": "پنهان کردن بایگانی از لیست گفتگوها",
        "iw": "הסתר ארכיון מרשימת הצ\'אטים", "az": "Arxivi çat siyahısından gizlət", "uz": "Arxivni chatlar ro‘yxatidan yashirish"
    },
    "NM_PR_HideArchiveList_Desc": {
        "tr": "Arşivi listeden kaldırın. Buradan açın veya Aramaya basılı tutun.",
        "ar": "إزالة الأرشيف من القائمة. افتحه هنا أو اضغط مطولاً على البحث.",
        "fa": "حذف بایگانی از لیست. از اینجا یا با نگه داشتن جستجو باز کنید.",
        "iw": "הסר את הארכיון. פתח כאן או החזק את כפתור החיפוש.",
        "az": "Arxivi siyahıdan silin. Buradan və ya axtarışı basıb saxlamaqla açın.",
        "uz": "Arxivni ro‘yxatdan olib tashlang. Uni bu yerdan yoki Qidiruvni bosib turib oching."
    },
    "NM_PR_HideArchivedStories": {
        "tr": "Arşivlenmiş hikayeleri gizle", "ar": "إخفاء القصص المؤرشفة", "fa": "پنهان کردن داستان‌های بایگانی شده",
        "iw": "הסתר סטוריז בארכיון", "az": "Arxivlənmiş hekayələri gizlət", "uz": "Arxivlangan hikoyalarni yashirish"
    },
    "NM_PR_HideArchivedStories_Desc": {
        "tr": "Arşivlenmiş hikayeler bölümünü profilinizden gizler.",
        "ar": "إخفاء قسم القصص المؤرشفة من ملفك الشخصي.",
        "fa": "بخش داستان‌های بایگانی شده را در نمایه شما پنهان می‌کند.",
        "iw": "מסתיר את קטע הסטוריז בארכיון מהפרופיל שלך.",
        "az": "Profilinizdəki arxivlənmiş hekayələr bölməsini gizlədir.",
        "uz": "Profilingizdagi arxivlangan hikoyalar bo‘limini yashiradi."
    },
    "NM_PR_HideProxy": {
        "tr": "Proxy sponsorunu gösterme", "ar": "عدم إظهار راعي البروكسي", "fa": "اسپانسر پروکسی نمایش داده نشود",
        "iw": "אל תציג ספונסר פרוקסי", "az": "Proksi sponsorunu göstərmə", "uz": "Proksi homiysini ko‘rsatmaslik"
    },
    "NM_PR_LockedChats": {
        "tr": "Kilitli sohbetler", "ar": "المحادثات المحمية", "fa": "گفتگوهای قفل شده",
        "iw": "צ\'אטים נעולים", "az": "Qorunan çatlar", "uz": "Himoyalangan chatlar"
    },
    "NM_PR_LockedChatsTtl": {
        "tr": "Tekrar sor", "ar": "السؤال مجددًا", "fa": "پرسش مجدد",
        "iw": "שאל שוב", "az": "Yenidən soruş", "uz": "Qayta so‘rash"
    },
    "NM_PR_LockedChatsTtl_15Min": {
        "tr": "15 dakika sonra", "ar": "بعد 15 دقيقة", "fa": "بعد از ۱۵ دقیقه",
        "iw": "לאחר 15 דקות", "az": "15 dəqiqədən sonra", "uz": "15 daqiqadan keyin"
    },
    "NM_PR_LockedChatsTtl_1Min": {
        "tr": "1 dakika sonra", "ar": "بعد دقيقة واحدة", "fa": "بعد از ۱ دقیقه",
        "iw": "לאחר דקה אחת", "az": "1 dəqiqədən sonra", "uz": "1 daqiqadan keyin"
    },
    "NM_PR_LockedChatsTtl_5Min": {
        "tr": "5 dakika sonra", "ar": "بعد 5 دقائق", "fa": "بعد از ۵ دقیقه",
        "iw": "לאחר 5 דקות", "az": "5 dəqiqədən sonra", "uz": "5 daqiqadan keyin"
    },
    "NM_PR_LockedChatsTtl_Always": {
        "tr": "Her zaman sor", "ar": "السؤال دائمًا", "fa": "همیشه بپرس",
        "iw": "שאל תמיד", "az": "Hər dəfə soruş", "uz": "Har doim so‘rash"
    },
    "NM_PR_LockedChatsTtl_UntilRestart": {
        "tr": "Yeniden başlatmaya kadar", "ar": "حتى إعادة التشغيل", "fa": "تا راه‌اندازی مجدد",
        "iw": "עד להפעלה מחדש", "az": "Yenidən başladılana qədər", "uz": "Qayta ishga tushguncha"
    },
    "NM_PR_OpenArchive": {
        "tr": "Arşivi aç", "ar": "فتح الأرشيف", "fa": "باز کردن بایگانی",
        "iw": "פתח ארכיון", "az": "Arxivi aç", "uz": "Arxivni ochish"
    },
    "NM_PR_RequireBiometricsToDelete": {
        "tr": "Silmeyi onayla", "ar": "تأكيد الحذف", "fa": "تایید حذف",
        "iw": "אשר מחיקה", "az": "Silinməni təsdiqlə", "uz": "O‘chirishni tasdiqlash"
    },
    "NM_PR_RequireBiometricsToDelete_Desc": {
        "tr": "Mesajları veya sohbetleri silmeden önce kimlik doğrulaması isteyin.",
        "ar": "طلب المصادقة قبل حذف الرسائل أو المحادثات.",
        "fa": "نیاز به احراز هویت دستگاه قبل از حذف پیام‌ها یا گفتگوها.",
        "iw": "דרוש אימות מכשיר לפני מחיקת הודעות או צ\'אטים.",
        "az": "Mesajları və ya çatarı silməzdən əvvəl təsdiq tələb et.",
        "uz": "Xabarlar yoki chatlarni o‘chirishdan oldin qurilma tasdiqlashini talab qilish."
    },
    "NM_PR_TestFingerprint": {
        "tr": "Korumayı test et", "ar": "اختبار المصادقة", "fa": "آزمایش احراز هویت",
        "iw": "בדוק אימות", "az": "Mühafizəni yoxla", "uz": "Himoyani sinash"
    },
    "NM_PR_TestFingerprint_Desc": {
        "tr": "Biyometrinin veya cihaz kilidinin kullanılabilirliğini kontrol eder.",
        "ar": "التحقق من توفر القياسات الحيوية وتحديث حالتها.",
        "fa": "بررسی دسترسی به بیومتریک و به‌روزرسانی وضعیت آن.",
        "iw": "בודק אם אימות ביומטרי זמין ומרענן את מצבו.",
        "az": "Biometriyanın mövcudluğunu yoxlayır və vəziyyətini yeniləyir.",
        "uz": "Biometriya yoki ekran blokirovkasining mavjudligini tekshiradi."
    },
    "NM_PredictiveBackAnimation": {
        "tr": "Tahmini geri hareketi", "ar": "إيماءة الرجوع التنبؤية", "fa": "ژست بازگشت پیش‌بینی‌کننده",
        "iw": "מחוות חזרה חזויה", "az": "Prediktiv geri hərəkəti", "uz": "Prediktiv orqaga jesti"
    },
    "NM_PremiumReactionRequired": {
        "tr": "Telegram Premium gereklidir", "ar": "يتطلب تيليجرام بريميوم", "fa": "نیاز به تلگرام پرمیوم",
        "iw": "נדרש טלגרם פרימיום", "az": "Telegram Premium tələb olunur", "uz": "Telegram Premium talab qilinadi"
    },
    "NM_ResidentNotification": {
        "tr": "Kalıcı bildirim", "ar": "إشعار دائم", "fa": "اعلان مداوم",
        "iw": "התראה קבועה", "az": "Daimi bildiriş", "uz": "Doimiy bildirishnoma"
    },
    "NM_Restart": {
        "tr": "Yeniden Başlat", "ar": "إعادة التشغيل", "fa": "راه‌اندازی مجدد",
        "iw": "הפעל מחדש", "az": "Yenidən başlat", "uz": "Qayta ishga tushirish"
    },
    "NM_RestartRequired": {
        "tr": "Değişiklikleri uygulamak için yeniden başlatma gerekiyor", "ar": "إعادة التشغيل مطلوبة لتطبيق التغييرات",
        "fa": "برای اعمال تغییرات نیاز به راه‌اندازی مجدد است", "iw": "נדרשת הפעלה מחדש כדי להחיל שינויים",
        "az": "Dəyişiklikləri tətbiq etmək üçün yenidən başladılmalıdır", "uz": "O‘zgarishlarni qo‘llash uchun qayta ishga tushirish kerak"
    },
    "NM_SettingsDesc_NavigationCrossfade": {
        "tr": "Ekranlar arasında geçişte karartma animasyonu", "ar": "تأثير التلاشي المتبادل عند تبديل الشاشات", "fa": "پویانمایی محو شدن هنگام تغییر صفحه",
        "iw": "הנפשת עמעום בעת מעבר מסכים", "az": "Ekranlar arası yumşaq keçid animasiyası", "uz": "Ekranlar almashganda silliq o‘tish animatsiyasi"
    },
    "NM_StickerSize": {
        "tr": "Çıkartma boyutu", "ar": "حجم الملصق", "fa": "اندازه استیکر",
        "iw": "גודל מדבקה", "az": "Stiker ölçüsü", "uz": "Stiker o‘lchami"
    },
    "NM_StickerSizeBig": {
        "tr": "Büyük", "ar": "كبير", "fa": "بزرگ",
        "iw": "גדול", "az": "Böyük", "uz": "Katta"
    },
    "NM_StickerSizePreviewReply": {
        "tr": "aya doğru 🚀", "ar": "إلى القمر 🚀", "fa": "به سوی ماه 🚀",
        "iw": "אל הירח 🚀", "az": "Marsa doğru 🚀", "uz": "Oyga qarab 🚀"
    },
    "NM_StickerSizePreviewText": {
        "tr": "hahaha 😂", "ar": "ههههه 😂", "fa": "هاهاها 😂",
        "iw": "חחחח 😂", "az": "hahaha 😂", "uz": "hahaha 😂"
    },
    "NM_StickerSizeSmall": {
        "tr": "Küçük", "ar": "صغير", "fa": "کوچک",
        "iw": "קטן", "az": "Kiçik", "uz": "Kichik"
    },
    "NM_Stickers_Header": {
        "tr": "Çıkartmalar", "ar": "الملصقات", "fa": "استیکرها",
        "iw": "מדבקות", "az": "Stikerlər", "uz": "Stikerlar"
    },
    "NM_SwitchStyle": {
        "tr": "Anahtar stili", "ar": "نمط المفاتيح", "fa": "سبک سوئیچ‌ها",
        "iw": "סגנון מתגים", "az": "Açar tərzi", "uz": "O‘chirgich uslubi"
    },
    "NM_ToSaved": {
        "tr": "Kaydet", "ar": "حفظ", "fa": "ذخیره",
        "iw": "שמור", "az": "Saxla", "uz": "Saqlash"
    },
    "NM_Vibrate_Click": {
        "tr": "Tık", "ar": "نقرة", "fa": "کلیک",
        "iw": "קליק", "az": "Klik", "uz": "Chertish"
    },
    "NM_Vibrate_Keyboard": {
        "tr": "Klavye dokunuşu", "ar": "لمس لوحة المفاتيح", "fa": "ضربه به صفحه کلید",
        "iw": "הקשת מקלדת", "az": "Klaviatura toxunuşu", "uz": "Klaviatura bosilishi"
    },
    "NM_Vibrate_Long": {
        "tr": "Uzun basma", "ar": "ضغط مطول", "fa": "فشار طولانی",
        "iw": "לחיצה ארוכה", "az": "Uzun basma", "uz": "Uzoq bosish"
    },
    "NM_Vibrate_Off": {
        "tr": "Kapalı", "ar": "معطل", "fa": "خاموش",
        "iw": "כבוי", "az": "Sönülü", "uz": "O‘chiq"
    },
    "NM_Vibrate_Wave": {
        "tr": "Dalga", "ar": "موجة", "fa": "موج",
        "iw": "גל", "az": "Dalğa", "uz": "To‘lqin"
    },
    "NM_WSB_Enable": {
        "tr": "Engellemeyi aşmayı etkinleştir", "ar": "تمكين التجاوز", "fa": "فعال‌سازی دور زدن محدودیت",
        "iw": "הפעל עקיפה", "az": "Blokdan çıxarmanı aktivləşdir", "uz": "Aylanib o‘tishni yoqish"
    },
    "NM_WSB_MiscHeader": {
        "tr": "Aramalar ve VPN", "ar": "المكالمات و VPN", "fa": "تماس‌ها و VPN",
        "iw": "שיחות ו-VPN", "az": "Zənglər və VPN", "uz": "Qo‘ng‘iroqlar va VPN"
    },
    "NM_WSB_OpenProxySettings": {
        "tr": "Telegram proxy ayarlarını aç", "ar": "فتح إعدادات بروكسي تيليجرام", "fa": "باز کردن تنظیمات پروکسی تلگرام",
        "iw": "פתח הגדרות פרוקסי בטלגרם", "az": "Telegram proksi tənzimləmələrini aç", "uz": "Telegram proksi sozlamalarini ochish"
    },
    "NM_WSB_Status_Off": {
        "tr": "Kapalı", "ar": "معطل", "fa": "خاموش",
        "iw": "כבוי", "az": "Söndürülüb", "uz": "O‘chirilgan"
    },
    "NM_WSB_SuspendOnVpn": {
        "tr": "VPN etkinken duraklat", "ar": "إيقاف مؤقت عند تفعيل VPN", "fa": "توقف هنگام فعال بودن VPN",
        "iw": "השהה בעת שימוש ב-VPN", "az": "VPN olduqda dayandır", "uz": "VPN yoqilganda to‘xtatib turish"
    },
    "NM_WSB_SuspendOnVpn_Desc": {
        "tr": "Sistem VPN\'i etkinken engellemeyi aşmayı duraklatın.",
        "ar": "إيقاف التجاوز مؤقتًا أثناء تنشيط VPN النظام.",
        "fa": "توقف دور زدن هنگام فعال بودن VPN دستگاه.",
        "iw": "השהה עקיפה כאשר VPN של המערכת פעיל.",
        "az": "Sistem VPN-i aktiv olanda blokdan çıxarmanı dayandır.",
        "uz": "Tizim VPNi faol bo‘lganda aylanib o‘tishni to‘xtatib turish."
    },
    "NM_WSB_VoIP_Enable": {
        "tr": "Aramaları tünelden geçir", "ar": "توجيه المكالمات عبر التجاوز", "fa": "هدایت تماس‌ها از طریق میانبر",
        "iw": "נתב שיחות דרך העקיפה", "az": "Zəngləri tunel vasitəsilə yönləndir", "uz": "Qo‘ng‘iroqlarni aylanib o‘tish orqali yo‘naltirish"
    },
    "NM_Without_Authorship": {
        "tr": "yazarsız", "ar": "بدون كاتب", "fa": "بدون فرستنده",
        "iw": "ללא מחבר", "az": "müəllifsiz", "uz": "muallifsiz"
    },
    "Notifications": {
        "tr": "Bildirimler", "ar": "الإشعارات", "fa": "اعلان‌ها",
        "iw": "התראות", "az": "Bildirişlər", "uz": "Bildirishnomalar"
    },
    "NotificationsService": {
        "tr": "Arka Plan Bağlantı Servisi", "ar": "خدمة الاتصال الدائم", "fa": "سرویس اتصال پس‌زمینه",
        "iw": "שירות חיבור רקע", "az": "Arxa fon əlaqə xidməti", "uz": "Fon ulanish xizmati"
    },
    "NotificationsSound": {
        "tr": "Bildirim Sesi", "ar": "صوت الإشعار", "fa": "صدای اعلان",
        "iw": "צליל התראה", "az": "Bildiriş səsi", "uz": "Bildirishnoma tovushi"
    },
    "NumberUnknown": {
        "tr": "Bilinmeyen", "ar": "مجهول", "fa": "ناشناس",
        "iw": "לא ידוע", "az": "Naməlum", "uz": "Noma’lum"
    },
    "OK": {
        "tr": "Tamam", "ar": "حسناً", "fa": "تأیید",
        "iw": "אישור", "az": "Oldu", "uz": "OK"
    },
    "Online": {
        "tr": "çevrim içi", "ar": "متصل", "fa": "آنلاین",
        "iw": "מחובר", "az": "onlayn", "uz": "tarmoqda"
    },
    "PasscodePanicCodeRemove": {
        "tr": "Panik kodunu kapat", "ar": "إيقاف رمز الطوارئ", "fa": "خاموش کردن کد اضطراری",
        "iw": "כבה קוד חירום", "az": "Təcili kodu söndür", "uz": "Favqulodda parolni o‘chirish"
    },
    "PinMessage": {
        "tr": "Sabitle", "ar": "تثبيت", "fa": "سنجاق کردن",
        "iw": "הצמד", "az": "Sancaqla", "uz": "Qadash"
    },
    "Plugins": {
        "tr": "Eklentiler", "ar": "الإضافات", "fa": "افزونه‌ها",
        "iw": "תוספים", "az": "Plaqinlər", "uz": "Plaginlar"
    },
    "PluginsEngine": {
        "tr": "Eklenti motoru", "ar": "محرك الإضافات", "fa": "موتور افزونه‌ها",
        "iw": "מנוע תוספים", "az": "Plaqin mühərriki", "uz": "Plagin dvigateli"
    },
    "PopupDisabled": {
        "tr": "Devre dışı", "ar": "معطل", "fa": "غیرفعال",
        "iw": "מושבת", "az": "Söndürülüb", "uz": "O‘chirilgan"
    },
    "ProfileBirthday": {
        "tr": "Doğum Tarihi", "ar": "تاريخ الميلاد", "fa": "تاریخ تولد",
        "iw": "תאריך לידה", "az": "Doğum tarixi", "uz": "Tug‘ilgan sana"
    },
    "Quality1440Short": {
        "tr": "2K", "ar": "2K", "fa": "2K",
        "iw": "2K", "az": "2K", "uz": "2K"
    },
    "QualityAuto": {
        "tr": "Otomatik", "ar": "تلقائي", "fa": "خودکار",
        "iw": "אוטומטי", "az": "Avto", "uz": "Avto"
    },
    "Reactions": {
        "tr": "Tepkiler", "ar": "التفاعلات", "fa": "واکنش‌ها",
        "iw": "תגובות", "az": "Reaksiyalar", "uz": "Reaksiyalar"
    },
    "Reply": {
        "tr": "Yanıtla", "ar": "رد", "fa": "پاسخ",
        "iw": "השב", "az": "Cavabla", "uz": "Javob berish"
    },
    "ReportChat": {
        "tr": "Şikayet Et", "ar": "إبلاغ", "fa": "گزارش",
        "iw": "דווח", "az": "Şikayət et", "uz": "Shikoyat qilish"
    },
    "RepostToStory": {
        "tr": "Hikayede Paylaş", "ar": "إعادة النشر في القصة", "fa": "بازنشر در داستان",
        "iw": "שתף מחדש בסטורי", "az": "Hekayədə paylaş", "uz": "Hikoyada ulashish"
    },
    "Reset": {
        "tr": "Sıfırla", "ar": "إعادة تعيين", "fa": "بازنشانی",
        "iw": "אפס", "az": "Sıfırla", "uz": "Qayta sozlash"
    },
    "Save": {
        "tr": "Kaydet", "ar": "حفظ", "fa": "ذخیره",
        "iw": "שמור", "az": "Yadda saxla", "uz": "Saqlash"
    },
    "SaveDeletedMessageForBot": {
        "tr": "Bot Sohbetlerinde Kaydet", "ar": "حفظ في محادثات البوتات", "fa": "ذخیره در چت‌های با ربات",
        "iw": "שמור בצ\'אטים של בוטים", "az": "Bot çatlarında saxla", "uz": "Bot chatlarida saqlash"
    },
    "SaveDeletedMessageForBotUser": {
        "tr": "Bot Mesajlarını Kaydet", "ar": "حفظ رسائل البوتات", "fa": "ذخیره پیام‌های ربات",
        "iw": "שמור הודעות בוטים", "az": "Bot mesajlarını saxla", "uz": "Bot xabarlarini saqlash"
    },
    "SaveForNotifications": {
        "tr": "Bildirimler için kaydet", "ar": "حفظ للإشعارات", "fa": "ذخیره برای اعلان‌ها",
        "iw": "שמור להתראות", "az": "Bildirişlər üçün saxla", "uz": "Bildirishnomalar uchun saqlash"
    },
    "SaveToDownloads": {
        "tr": "İndirilenlere kaydet", "ar": "حفظ في التنزيلات", "fa": "ذخیره در دانلودها",
        "iw": "שמור בהורדות", "az": "Yükləmələrə saxla", "uz": "Yuklanmalarga saqlash"
    },
    "SaveToGallery": {
        "tr": "Galeriye kaydet", "ar": "حفظ في المعرض", "fa": "ذخیره در گالری",
        "iw": "שמור בגלריה", "az": "Qalereyaya saxla", "uz": "Galereyaga saqlash"
    },
    "SavedMessages": {
        "tr": "Kayıtlı Mesajlar", "ar": "الرسائل المحفوظة", "fa": "پیام‌های ذخیره شده",
        "iw": "הודעות שמורות", "az": "Saxlanılan mesajlar", "uz": "Saqlangan xabarlar"
    },
    "Search": {
        "tr": "Ara", "ar": "بحث", "fa": "جستجو",
        "iw": "חיפוש", "az": "Axtar", "uz": "Qidiruv"
    },
    "Settings": {
        "tr": "Ayarlar", "ar": "الإعدادات", "fa": "تنظیمات",
        "iw": "הגדרות", "az": "Tənzimləmələr", "uz": "Sozlamalar"
    },
    "SettingsNotifications": {
        "tr": "Bildirimler", "ar": "الإشعارات", "fa": "اعلان‌ها",
        "iw": "התראות", "az": "Bildirişlər", "uz": "Bildirishnomalar"
    },
    "ShareFile": {
        "tr": "Paylaş", "ar": "مشاركة", "fa": "اشتراک‌گذاری",
        "iw": "שתף", "az": "Paylaş", "uz": "Ulashish"
    },
    "ShareLink": {
        "tr": "Bağlantıyı Paylaş", "ar": "مشاركة الرابط", "fa": "اشتراک‌گذاری پیوند",
        "iw": "שתף קישור", "az": "Linki paylaş", "uz": "Havolani ulashish"
    },
    "StatisticsAndBoosts": {
        "tr": "İstatistikler ve Yükseltmeler", "ar": "الإحصائيات والتعزيزات", "fa": "آمار و افزایش‌ها",
        "iw": "סטטיסטיקות וחיזוקים", "az": "Statistika və gücləndirmələr", "uz": "Statistika va kuchaytirishlar"
    },
    "StickersName": {
        "tr": "Çıkartmalar", "ar": "الملصقات", "fa": "استیکرها",
        "iw": "מדבקות", "az": "Stikerlər", "uz": "Stikerlar"
    },
    "TelegramPremium": {
        "tr": "Telegram Premium", "ar": "تيليجرام بريميوم", "fa": "تلگرام پرمیوم",
        "iw": "טלגרם פרימיום", "az": "Telegram Premium", "uz": "Telegram Premium"
    },
    "TosDeclineDeleteAccount": {
        "tr": "Uyarı: Bu işlem Telegram hesabınızı geri alınamaz şekilde silecektir.",
        "ar": "تحذير: سيؤدي هذا الإجراء إلى حذف حساب تيليجرام الخاص بك نهائيًا.",
        "fa": "هشدار: این عمل حساب تلگرام شما را برای همیشه حذف خواهد کرد.",
        "iw": "אזהרה: פעולה זו תמחק לצמיתות את חשבון הטלגרם שלך.",
        "az": "Xəbərdarlıq: Bu əməliyyat Telegram hesabınızı bərpa olunmaz şəkildə siləcəkdir.",
        "uz": "Ogohlantirish: bu amal Telegram hisobingizni butunlay o‘chirib yuboradi."
    },
    "TranslateMessage": {
        "tr": "Çevir", "ar": "ترجمة", "fa": "ترجمه",
        "iw": "תרגם", "az": "Tərcümə et", "uz": "Tarjima qilish"
    },
    "TranslucentDeletedMessages": {
        "tr": "Yarı Saydam Silinen Mesajlar", "ar": "رسائل محذوفة شبه شفافة", "fa": "پیام‌های حذف شده نیمه‌شفاف",
        "iw": "הודעות שנמחקו חצי שקופות", "az": "Yarımşəffaf silinmiş mesajlar", "uz": "Yarim shaffof o‘chirilgan xabarlar"
    },
    "UserColorTabProfile": {
        "tr": "Profil", "ar": "الملف الشخصي", "fa": "نمایه",
        "iw": "פרופיל", "az": "Profil", "uz": "Profil"
    },
    "VibrationDisabled": {
        "tr": "Devre dışı", "ar": "معطل", "fa": "غیرفعال",
        "iw": "מושבת", "az": "Söndürülüb", "uz": "O‘chirilgan"
    },
    "exteraAppName": {
        "tr": "SoundGram", "ar": "SoundGram", "fa": "SoundGram",
        "iw": "SoundGram", "az": "SoundGram", "uz": "SoundGram"
    }
}

langs = ["tr", "ar", "fa", "iw", "az", "uz"]
out_data = {l: {} for l in langs}

for k in sorted(en_dict.keys()):
    if k in ME_MAP:
        for l in langs:
            out_data[l][k] = ME_MAP[k].get(l, en_dict[k])
    else:
        for l in langs:
            out_data[l][k] = existing.get(l, {}).get(k, en_dict[k])

with open("tools/translations_middle_east.json", "w", encoding="utf-8") as out:
    json.dump(out_data, out, ensure_ascii=False, indent=2)

print("Generated tools/translations_middle_east.json successfully with counts:")
for l, d in out_data.items():
    print(f"  {l}: {len(d)} keys")
