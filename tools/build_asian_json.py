# -*- coding: utf-8 -*-
"""
Generate complete translations for ID, JA, KO, ZH-rCN, ZH-rTW for all 460 keys.
"""
import json, os

with open("tools/missing_460_en_ru.json", "r", encoding="utf-8") as f:
    base = json.load(f)

en_dict = base["en"]
ru_dict = base["ru"]

with open("tools/existing_translations.json", "r", encoding="utf-8") as f:
    existing = json.load(f)

# Vocabulary mapping: key -> dict of {id, ja, ko, zh_cn, zh_tw}
ASIAN_MAP = {
    "AP_CenterTitle": {
        "id": "Judul di tengah", "ja": "タイトルを中央揃え", "ko": "제목 중앙 정렬",
        "zh_cn": "标题居中", "zh_tw": "標題置中"
    },
    "AP_DisableDividers": {
        "id": "Sembunyikan pemisah", "ja": "区切り線を非表示", "ko": "구분선 숨기기",
        "zh_cn": "隐藏分割线", "zh_tw": "隱藏分隔線"
    },
    "AP_GlareOnElements": {
        "id": "Silau pada elemen", "ja": "要素の反射光", "ko": "요소의 반사광",
        "zh_cn": "元素高光", "zh_tw": "元素高光"
    },
    "AP_GlareOnElementsInfo": {
        "id": "Tampilkan sorotan atas dan bawah pada elemen kaca cair.",
        "ja": "液体ガラス要素の上下のハイライトを表示します。",
        "ko": "액체 유리 요소의 상하 하이라이트를 표시합니다.",
        "zh_cn": "在液态玻璃元素上显示顶部和底部高光。",
        "zh_tw": "在液態玻璃元素上顯示頂部和底部高光。"
    },
    "AP_Header": {
        "id": "Header", "ja": "ヘッダー", "ko": "헤더",
        "zh_cn": "页眉", "zh_tw": "頁首"
    },
    "AP_Header_Appearance": {
        "id": "Tampilan", "ja": "外観", "ko": "외관",
        "zh_cn": "外观", "zh_tw": "外觀"
    },
    "AP_HideSearchBar": {
        "id": "Sembunyikan bilah pencarian obrolan", "ja": "チャット検索バーを非表示", "ko": "채팅 검색창 숨기기",
        "zh_cn": "隐藏聊天搜索栏", "zh_tw": "隱藏聊天搜尋列"
    },
    "AP_IconReplacements": {
        "id": "Paket ikon", "ja": "アイコンパック", "ko": "아이콘 팩",
        "zh_cn": "图标包", "zh_tw": "圖示包"
    },
    "AP_ShowPencilIcon": {
        "id": "Tandai pesan yang diedit", "ja": "編集されたメッセージをマーク", "ko": "수정된 메시지 표시",
        "zh_cn": "标记已编辑消息", "zh_tw": "標記已編輯訊息"
    },
    "AP_SystemEmoji": {
        "id": "Emoji sistem", "ja": "システム絵文字", "ko": "시스템 이모지",
        "zh_cn": "系统 Emoji", "zh_tw": "系統 Emoji"
    },
    "AP_SystemFonts": {
        "id": "Font sistem", "ja": "システムフォント", "ko": "시스템 글꼴",
        "zh_cn": "系统字体", "zh_tw": "系統字型"
    },
    "AP_Tablet_Mode": {
        "id": "Mode tablet", "ja": "タブレットモード", "ko": "태블릿 모드",
        "zh_cn": "平板模式", "zh_tw": "平板模式"
    },
    "AccActionPause": {
        "id": "Jeda", "ja": "一時停止", "ko": "일시 중지",
        "zh_cn": "暂停", "zh_tw": "暫停"
    },
    "AccActionPlay": {
        "id": "Putar", "ja": "再生", "ko": "재생",
        "zh_cn": "播放", "zh_tw": "播放"
    },
    "AccDescrCustomEmoji": {
        "id": "Emoji khusus", "ja": "カスタム絵文字", "ko": "사용자 지정 이모지",
        "zh_cn": "自定义 Emoji", "zh_tw": "自訂 Emoji"
    },
    "AccDescrGIFs": {
        "id": "GIF", "ja": "GIF", "ko": "GIF",
        "zh_cn": "GIF", "zh_tw": "GIF"
    },
    "AccDescrStickers": {
        "id": "Stiker", "ja": "ステッカー", "ko": "스티커",
        "zh_cn": "贴纸", "zh_tw": "貼圖"
    },
    "ActionsChartTitle": {
        "id": "Tindakan", "ja": "操作", "ko": "작업",
        "zh_cn": "操作", "zh_tw": "操作"
    },
    "AppName": {
        "id": "SoundGram", "ja": "SoundGram", "ko": "SoundGram",
        "zh_cn": "SoundGram", "zh_tw": "SoundGram"
    },
    "AreYouSure": {
        "id": "Apakah Anda yakin?", "ja": "本当によろしいですか？", "ko": "정말 진행하시겠습니까?",
        "zh_cn": "确定吗？", "zh_tw": "確定嗎？"
    },
    "AvatarPreviewSearchMessages": {
        "id": "Cari Pesan", "ja": "メッセージを検索", "ko": "메시지 검색",
        "zh_cn": "搜索消息", "zh_tw": "搜尋訊息"
    },
    "Back": {
        "id": "Kembali", "ja": "戻る", "ko": "뒤로",
        "zh_cn": "返回", "zh_tw": "返回"
    },
    "BusinessHoursProfile": {
        "id": "Jam buka", "ja": "営業時間", "ko": "영업 시간",
        "zh_cn": "营业时间", "zh_tw": "營業時間"
    },
    "BusinessHoursProfileNowOpen": {
        "id": "Buka", "ja": "営業中", "ko": "영업 중",
        "zh_cn": "营业中", "zh_tw": "營業中"
    },
    "BusinessProfileLocation": {
        "id": "Lokasi", "ja": "場所", "ko": "위치",
        "zh_cn": "位置", "zh_tw": "位置"
    },
    "CP_AdminActions": {
        "id": "Pintasan admin", "ja": "管理者ショートカット", "ko": "관리자 단축키",
        "zh_cn": "管理员操作", "zh_tw": "管理員操作"
    },
    "CP_ArchiveStories": {
        "id": "Arsipkan cerita", "ja": "ストーリーをアーカイブ", "ko": "스토리 아카이브",
        "zh_cn": "归档动态", "zh_tw": "封存限時動態"
    },
    "CP_ArchiveStories_Desc": {
        "id": "Pindahkan cerita baru ke arsip secara otomatis.",
        "ja": "新しいストーリーを自動的にアーカイブに移動します。",
        "ko": "새 스토리를 자동으로 보관함으로 이동합니다.",
        "zh_cn": "自动将新动态移动至归档。",
        "zh_tw": "自動將新限時動態移至封存。"
    },
    "CP_AutoPauseVideo": {
        "id": "Jeda media otomatis", "ja": "メディアの自動一時停止", "ko": "미디어 자동 일시 중지",
        "zh_cn": "自动暂停媒体", "zh_tw": "自動暫停媒體"
    },
    "CP_AutoPauseVideo_Desc": {
        "id": "Jeda saat aplikasi masuk ke latar belakang.",
        "ja": "アプリがバックグラウンドに移動したときに一時停止します。",
        "ko": "앱이 백그라운드로 전환될 때 일시 중지합니다.",
        "zh_cn": "应用进入后台时自动暂停。",
        "zh_tw": "應用程式進入背景時自動暫停。"
    },
    "CP_AutoQuoteReplies": {
        "id": "Kutip balasan", "ja": "返信を引用", "ko": "답장 인용",
        "zh_cn": "引用回复", "zh_tw": "引用回覆"
    },
    "CP_AutoQuoteReplies_Desc": {
        "id": "Gunakan kutipan sebagai pengganti balasan standar.",
        "ja": "通常の返信の代わりに引用を使用します。",
        "ko": "일반 답장 대신 인용구를 사용합니다.",
        "zh_cn": "使用引用代替标准回复。",
        "zh_tw": "使用引用代替標準回覆。"
    },
    "CP_Camera2Desc": {
        "id": "Semua fitur yang tersembunyi saat Camera2 aktif tidak akan berfungsi.",
        "ja": "Camera2が有効な場合、非表示の機能は動作しません。",
        "ko": "Camera2가 활성화되면 숨겨진 기능이 작동하지 않습니다.",
        "zh_cn": "启用 Camera2 时隐藏的所有功能均无法使用。",
        "zh_tw": "啟用 Camera2 時隱藏的所有功能均無法使用。"
    },
    "CP_CameraDualCamera": {
        "id": "Kamera ganda", "ja": "デュアルカメラ", "ko": "듀얼 카메라",
        "zh_cn": "双摄像头", "zh_tw": "雙鏡頭"
    },
    "CP_CameraDualCamera_Desc": {
        "id": "Merekam pesan video di kedua kamera untuk beralih lebih cepat.",
        "ja": "録画中にカメラを素早く切り替えるため両方のカメラで記録します。",
        "ko": "녹화 중 빠른 전환을 위해 양쪽 카메라로 비디오 메시지를 캡처합니다.",
        "zh_cn": "在两个摄像头上录制视频消息以便更快切换。",
        "zh_tw": "在兩個鏡頭上記錄視訊訊息以便更快切換。"
    },
    "CP_CameraQuality": {
        "id": "Kualitas Kamera", "ja": "カメラの品質", "ko": "카메라 품질",
        "zh_cn": "相机画质", "zh_tw": "相機畫質"
    },
    "CP_CameraStabilisation": {
        "id": "Stabilisasi kamera", "ja": "手ぶれ補正", "ko": "카메라 흔들림 보정",
        "zh_cn": "相机防抖", "zh_tw": "相機防手震"
    },
    "CP_CameraType": {
        "id": "Tipe Kamera", "ja": "カメラの種類", "ko": "카메라 유형",
        "zh_cn": "相机类型", "zh_tw": "相機類型"
    },
    "CP_CameraTypeSystem": {
        "id": "Sistem", "ja": "システム", "ko": "시스템",
        "zh_cn": "系统", "zh_tw": "系統"
    },
    "CP_CameraUW": {
        "id": "Mulai dari Ultra Wide", "ja": "超広角から開始", "ko": "울트라 와이드로 시작",
        "zh_cn": "从超广角镜头开始", "zh_tw": "從超廣角鏡頭開始"
    },
    "CP_CameraUW_Desc": {
        "id": "Perekaman pesan video akan dimulai dari kamera Ultra Wide.",
        "ja": "ビデオメッセージの録画を超広角カメラから開始します。",
        "ko": "비디오 메시지 녹화가 초광각 카메라에서 시작됩니다.",
        "zh_cn": "录制视频消息将从超广角镜头开始。",
        "zh_tw": "錄製視訊訊息將從超廣角鏡頭開始。"
    },
    "CP_CameraXDesc": {
        "id": "CameraX hanya didukung pada perangkat tertentu.",
        "ja": "CameraXは特定のデバイスでのみサポートされています。",
        "ko": "CameraX는 일부 지원 기기에서만 지원됩니다.",
        "zh_cn": "CameraX 仅在特定设备上受支持。",
        "zh_tw": "CameraX 僅在特定裝置上受支援。"
    },
    "CP_Category_Camera": {
        "id": "Kamera", "ja": "カメラ", "ko": "카메라",
        "zh_cn": "相机", "zh_tw": "相機"
    },
    "CP_CenterCameraControlButtons": {
        "id": "Tengahkan tombol kontrol", "ja": "操作ボタンを中央揃え", "ko": "제어 버튼 중앙 정렬",
        "zh_cn": "控制按钮居中", "zh_tw": "控制按鈕置中"
    },
    "CP_CenterCameraControlButtons_Desc": {
        "id": "Tengahkan tombol ganti kamera dan senter.",
        "ja": "カメラ切り替えとフラッシュライトのボタンを中央に配置します。",
        "ko": "카메라 전환 및 손전등 버튼을 중앙에 배치합니다.",
        "zh_cn": "居中显示切换摄像头和手电筒按钮。",
        "zh_tw": "置中顯示切換鏡頭和手電筒按鈕。"
    },
    "CP_ChatMenuShortcuts": {
        "id": "Pintasan menu obrolan", "ja": "チャットメニューのショートカット", "ko": "채팅 메뉴 바로가기",
        "zh_cn": "聊天菜单操作", "zh_tw": "聊天選單操作"
    },
    "CP_CustomWallpapers": {
        "id": "Wallpaper per-obrolan", "ja": "チャットごとの壁紙", "ko": "개별 채팅 배경화면",
        "zh_cn": "单聊独立壁纸", "zh_tw": "單聊獨立桌布"
    },
    "CP_CustomWallpapers_Desc": {
        "id": "Tampilkan wallpaper yang disetel untuk obrolan tertentu.",
        "ja": "個別のチャットに設定された壁紙を表示します。",
        "ko": "개별 채팅에 설정된 배경화면을 표시합니다.",
        "zh_cn": "显示为单个聊天设置的壁纸。",
        "zh_tw": "顯示為個別聊天設定的桌布。"
    },
    "CP_DefaultCameraDesc": {
        "id": "Gunakan kamera bawaan Telegram.", "ja": "Telegramの内蔵カメラを使用します。", "ko": "Telegram 기본 내장 카메라를 사용합니다.",
        "zh_cn": "使用 Telegram 内置相机。", "zh_tw": "使用 Telegram 內建相機。"
    },
    "CP_DeleteForAll": {
        "id": "Hapus untuk semua secara default", "ja": "デフォルトで全員に対して削除", "ko": "기본적으로 모두에게 삭제",
        "zh_cn": "默认双向删除", "zh_tw": "預設雙向刪除"
    },
    "CP_DeleteForAll_Desc": {
        "id": "Pilih otomatis opsi ini di dialog penghapusan.",
        "ja": "削除ダイアログでこのオプションを事前に選択します。",
        "ko": "삭제 확인창에서 이 옵션을 미리 선택합니다.",
        "zh_cn": "在删除确认对话框中预先勾选此项。",
        "zh_tw": "在刪除確認對話方塊中預先勾選此項。"
    },
    "CP_DisableCam": {
        "id": "Tombol kamera di bawah", "ja": "下部にカメラボタン", "ko": "하단 카메라 버튼",
        "zh_cn": "底部相机按钮", "zh_tw": "底部相機按鈕"
    },
    "CP_DisableCam_Desc": {
        "id": "Tombol kamera akan muncul di sebelah tombol galeri.",
        "ja": "カメラボタンがギャラリーボタンの横に表示されます。",
        "ko": "카메라 버튼이 갤러리 버튼 옆에 표시됩니다.",
        "zh_cn": "相机按钮将显示在图库按钮旁边。",
        "zh_tw": "相機按鈕將顯示在相簿按鈕旁邊。"
    },
    "CP_DisablePremStickAnim": {
        "id": "Nonaktifkan efek stiker premium", "ja": "プレミアムステッカー効果を無効化", "ko": "프리미엄 스티커 효과 비활성화",
        "zh_cn": "禁用高级贴纸效果", "zh_tw": "停用高級貼圖效果"
    },
    "CP_DisablePremStickAnim_Desc": {
        "id": "Jangan putar efek saat stiker diketuk.",
        "ja": "ステッカーをタップしたときに効果を再生しません。",
        "ko": "스티커를 탭할 때 효과를 재생하지 않습니다.",
        "zh_cn": "轻按贴纸时不播放大型特效。",
        "zh_tw": "輕按貼圖時不播放大型特效。"
    },
    "CP_DisablePremStickAutoPlay": {
        "id": "Nonaktifkan putar otomatis stiker premium",
        "ja": "プレミアムステッカーの自動再生を無効化",
        "ko": "프리미엄 스티커 자동 재생 비활성화",
        "zh_cn": "禁用高级贴纸自动播放",
        "zh_tw": "停用高級貼圖自動播放"
    },
    "CP_DisablePremStickAutoPlay_Desc": {
        "id": "Jangan putar otomatis animasi stiker Premium.",
        "ja": "プレミアムステッカーのアニメーションを自動再生しません。",
        "ko": "프리미엄 스티커 애니메이션을 자동으로 재생하지 않습니다.",
        "zh_cn": "不自动播放高级贴纸动画。",
        "zh_tw": "不自動播放高級貼圖動畫。"
    },
    "CP_DisableReactionAnim": {
        "id": "Nonaktifkan animasi reaksi", "ja": "リアクションアニメーションを無効化", "ko": "반응 애니메이션 비활성화",
        "zh_cn": "禁用反应动画", "zh_tw": "停用反應動畫"
    },
    "CP_DisableReactionAnim_Desc": {
        "id": "Terapkan reaksi tanpa animasi besar.",
        "ja": "大きなアニメーションなしでリアクションを適用します。",
        "ko": "큰 애니메이션 없이 반응을 적용합니다.",
        "zh_cn": "发送表情反应时不显示全屏大动画。",
        "zh_tw": "傳送表情反應時不顯示全螢幕大動畫。"
    },
    "CP_DisableReactionsOverlay": {
        "id": "Sembunyikan panel reaksi", "ja": "リアクションパネルを非表示", "ko": "반응 패널 숨기기",
        "zh_cn": "隐藏反应面板", "zh_tw": "隱藏反應面板"
    },
    "CP_DisableReactionsOverlay_Desc": {
        "id": "Jangan tampilkan reaksi di atas pesan.",
        "ja": "メッセージの上にリアクションを表示しません。",
        "ko": "메시지 위에 반응을 표시하지 않습니다.",
        "zh_cn": "长按时不显示快捷反应面板。",
        "zh_tw": "長按時不顯示快捷反應面板。"
    },
    "CP_DisableSwipeToNext": {
        "id": "Nonaktifkan geser saluran", "ja": "チャンネルのスワイプを無効化", "ko": "채널 스와이프 비활성화",
        "zh_cn": "禁用连续滑动切换频道", "zh_tw": "停用連續滑動切換頻道"
    },
    "CP_DisableSwipeToNext_Desc": {
        "id": "Tetap menggulir di dalam saluran saat ini.",
        "ja": "現在のチャンネル内でのみスクロールします。",
        "ko": "스크롤할 때 현재 채널 내에 유지합니다.",
        "zh_cn": "在当前频道滚动时不跳转到下一个频道。",
        "zh_tw": "在當前頻道滾動時不跳轉到下一個頻道。"
    },
    "CP_DisableVibration": {
        "id": "Nonaktifkan getaran", "ja": "バイブレーションを無効化", "ko": "진동 비활성화",
        "zh_cn": "禁用振动", "zh_tw": "停用震動"
    },
    "CP_DoubleTapAction": {
        "id": "Aksi ketuk ganda", "ja": "ダブルタップの操作", "ko": "더블 탭 동작",
        "zh_cn": "双击操作", "zh_tw": "連按兩下操作"
    },
    "CP_Filters_Header": {
        "id": "Folder", "ja": "フォルダー", "ko": "폴더",
        "zh_cn": "分组", "zh_tw": "分組"
    },
    "CP_ForwardMsgDate": {
        "id": "Tampilkan tanggal pesan diteruskan", "ja": "転送メッセージの日時を表示", "ko": "전달된 메시지 날짜 표시",
        "zh_cn": "显示转发消息原始日期", "zh_tw": "顯示轉傳訊息原始日期"
    },
    "CP_Header_DialogList": {
        "id": "Daftar obrolan", "ja": "チャット一覧", "ko": "대화 목록",
        "zh_cn": "聊天列表", "zh_tw": "聊天列表"
    },
    "CP_Header_Videomessages": {
        "id": "Pesan video", "ja": "ビデオメッセージ", "ko": "비디오 메시지",
        "zh_cn": "视频消息", "zh_tw": "視訊訊息"
    },
    "CP_HideKbdOnScroll": {
        "id": "Sembunyikan keyboard saat menggulir", "ja": "スクロール時にキーボードを非表示", "ko": "스크롤 시 키보드 숨기기",
        "zh_cn": "滑动时隐藏键盘", "zh_tw": "滑動時隱藏鍵盤"
    },
    "CP_HideMuteUnmuteButton": {
        "id": "Sembunyikan bilah suara saluran", "ja": "チャンネルの消音バーを非表示", "ko": "채널 음소거 표시줄 숨기기",
        "zh_cn": "隐藏频道底部静音条", "zh_tw": "隱藏頻道底部靜音列"
    },
    "CP_HideSendAsChannel": {
        "id": "Sembunyikan «Kirim sebagai saluran»", "ja": "「チャンネルとして送信」を非表示", "ko": "«채널로 보내기» 숨기기",
        "zh_cn": "隐藏“以频道身份发送”", "zh_tw": "隱藏「以頻道身分傳送」"
    },
    "CP_HideSendAsChannelDesc": {
        "id": "Menu tetap tersedia dengan menahan tombol emoji.",
        "ja": "絵文字ボタンを長押しすることでメニューは利用可能です。",
        "ko": "이모지 버튼을 길게 누르면 메뉴를 계속 사용할 수 있습니다.",
        "zh_cn": "长按表情按钮仍可打开该菜单。",
        "zh_tw": "長按表情按鈕仍可開啟該選單。"
    },
    "CP_HideStories": {
        "id": "Sembunyikan cerita", "ja": "ストーリーを非表示", "ko": "스토리 숨기기",
        "zh_cn": "隐藏动态", "zh_tw": "隱藏限時動態"
    },
    "CP_HideStories_Desc": {
        "id": "Hapus cerita dari obrolan dan profil.",
        "ja": "チャットやプロフィールからストーリーを削除します。",
        "ko": "채팅 및 프로필에서 스토리를 제거합니다.",
        "zh_cn": "从聊天和个人资料中移除动态。",
        "zh_tw": "從聊天和個人檔案中移除限時動態。"
    },
    "CP_LeftBottomButtonAction": {
        "id": "Tindakan tombol kiri", "ja": "左ボタンの操作", "ko": "왼쪽 버튼 동작",
        "zh_cn": "左侧按钮操作", "zh_tw": "左側按鈕操作"
    },
    "CP_MainTabs_ForceOpenChats_Desc": {
        "id": "Tombol pencarian selalu membuka pencarian obrolan.",
        "ja": "検索ボタンは常にチャット検索を開きます。",
        "ko": "검색 버튼이 항상 채팅 검색을 엽니다.",
        "zh_cn": "搜索按钮始终直接打开聊天搜索。",
        "zh_tw": "搜尋按鈕始終直接開啟聊天搜尋。"
    },
    "CP_MainTabs_Header": {
        "id": "Tab bawah", "ja": "下部タブ", "ko": "하단 탭",
        "zh_cn": "底部标签页", "zh_tw": "底部標籤頁"
    },
    "CP_MessageMenu": {
        "id": "Menu pesan", "ja": "メッセージメニュー", "ko": "메시지 메뉴",
        "zh_cn": "消息菜单", "zh_tw": "訊息選單"
    },
    "CP_MessageMenuCompactLayout": {
        "id": "Tata letak menu ringkas", "ja": "コンパクトなメニュー配置", "ko": "컴팩트 메뉴 레이아웃",
        "zh_cn": "紧凑菜单布局", "zh_tw": "緊湊選單版面配置"
    },
    "CP_MessageMenuCompactLayout_Desc": {
        "id": "Tampilkan hingga empat tindakan utama sebagai tombol besar.",
        "ja": "最大4つの主要なアクションを大きなボタンとして表示します。",
        "ko": "최대 4개의 주요 작업을 큰 버튼으로 표시합니다.",
        "zh_cn": "最多将四个主要操作显示为大按钮。",
        "zh_tw": "最多將四個主要操作顯示為大按鈕。"
    },
    "CP_MessageMenuCompactLayout_Dot": {
        "id": "Titik menandai tindakan tekan lama.",
        "ja": "ドットは長押し操作を示します。",
        "ko": "점 표시는 길게 누르기 동작을 나타냅니다.",
        "zh_cn": "圆点标记长按操作。",
        "zh_tw": "圓點標記長按操作。"
    },
    "CP_MessageMenuItems": {
        "id": "Item menu pesan", "ja": "メッセージメニュー項目", "ko": "메시지 메뉴 항목",
        "zh_cn": "消息菜单项目", "zh_tw": "訊息選單項目"
    },
    "CP_Message_Filtering": {
        "id": "Pemfilteran pesan", "ja": "メッセージフィルタリング", "ko": "메시지 필터링",
        "zh_cn": "消息过滤", "zh_tw": "訊息過濾"
    },
    "CP_Message_Filtering_Collapse": {
        "id": "Ciutkan pesan", "ja": "メッセージを折りたたむ", "ko": "메시지 접기",
        "zh_cn": "折叠消息", "zh_tw": "摺疊訊息"
    },
    "CP_Message_Filtering_Collapse_Desc": {
        "id": "Ciutkan pesan yang difilter secara otomatis.",
        "ja": "フィルターされたメッセージを自動的に折りたたみます。",
        "ko": "필터링된 메시지를 자동으로 접습니다.",
        "zh_cn": "自动折叠被过滤的消息。",
        "zh_tw": "自動摺疊被過濾的訊息。"
    },
    "CP_Messages_Size": {
        "id": "Ukuran balon", "ja": "フキダシのサイズ", "ko": "말풍선 크기",
        "zh_cn": "气泡大小", "zh_tw": "氣泡大小"
    },
    "CP_PlayVideo": {
        "id": "Putar video dengan tombol volume", "ja": "音量ボタンで動画を再生", "ko": "볼륨 버튼으로 비디오 재생",
        "zh_cn": "用音量键播放视频", "zh_tw": "用音量鍵播放視訊"
    },
    "CP_PlayVideo_Desc": {
        "id": "Mulai video yang terlihat dengan tombol volume.",
        "ja": "表示されているビデオを音量ボタンで再生します。",
        "ko": "화면에 보이는 비디오를 볼륨 버튼으로 시작합니다.",
        "zh_cn": "使用音量按钮开始播放当前可见的视频。",
        "zh_tw": "使用音量按鈕開始播放當前可見的視訊。"
    },
    "CP_ProfileReplyBackground": {
        "id": "Pesan dan profil", "ja": "メッセージとプロフィール", "ko": "메시지 및 프로필",
        "zh_cn": "消息与资料", "zh_tw": "訊息與個人檔案"
    },
    "CP_SilenceNonContacts": {
        "id": "Bisukan non-kontak", "ja": "連絡先以外をミュート", "ko": "연락처 외 무음 처리",
        "zh_cn": "非联系人静音", "zh_tw": "非聯絡人靜音"
    },
    "CP_SilenceNonContacts_Desc": {
        "id": "Bunyikan suara notifikasi hanya untuk kontak.",
        "ja": "連絡先からのメッセージのみ通知音を鳴らします。",
        "ko": "연락처에 등록된 사용자에게만 알림음을 재생합니다.",
        "zh_cn": "仅针对联系人播放通知声音。",
        "zh_tw": "僅針對聯絡人播放通知聲音。"
    },
    "CP_Slider_RecentEmojisAndStickers": {
        "id": "Emoji dan stiker terbaru", "ja": "最近の絵文字とステッカー", "ko": "최근 이모지 및 스티커",
        "zh_cn": "最近使用的 Emoji 和贴纸", "zh_tw": "最近使用的 Emoji 和貼圖"
    },
    "CP_Snowflakes_Header": {
        "id": "Kepingan salju", "ja": "雪の結晶", "ko": "눈송이",
        "zh_cn": "雪花效果", "zh_tw": "雪花效果"
    },
    "CP_SortByUnread": {
        "id": "Urutkan berdasarkan belum dibaca", "ja": "未読順で並べ替え", "ko": "읽지 않음 순으로 정렬",
        "zh_cn": "按未读排序", "zh_tw": "按未讀排序"
    },
    "CP_SystemCameraDesc": {
        "id": "Akses langsung kamera sistem.", "ja": "システムカメラに直接アクセスします。", "ko": "시스템 카메라에 직접 액세스합니다.",
        "zh_cn": "直接调用系统相机。", "zh_tw": "直接調用系統相機。"
    },
    "CP_TimeOnStick": {
        "id": "Sembunyikan waktu di stiker", "ja": "ステッカーの時刻を非表示", "ko": "스티커 시간 숨기기",
        "zh_cn": "隐藏贴纸时间戳", "zh_tw": "隱藏貼圖時間戳記"
    },
    "CP_UnarchiveOnSwipe": {
        "id": "Batalkan arsip saat menggeser", "ja": "スワイプでアーカイブ解除", "ko": "스와이프로 보관 취소",
        "zh_cn": "滑动解除归档", "zh_tw": "滑動解除封存"
    },
    "CP_UnreadBadgeOnBackButton": {
        "id": "Penghitung obrolan belum dibaca", "ja": "未読チャット数カウンター", "ko": "읽지 않은 채팅 카운터",
        "zh_cn": "未读计数角标", "zh_tw": "未讀計數角標"
    },
    "CP_UnreadBadgeOnBackButton_Desc": {
        "id": "Tampilkan jumlah belum dibaca pada tombol kembali.",
        "ja": "戻るボタンに未読数を表示します。",
        "ko": "뒤로 가기 버튼에 읽지 않은 개수를 표시합니다.",
        "zh_cn": "在返回按钮上显示未读聊天总数。",
        "zh_tw": "在返回按鈕上顯示未讀聊天總數。"
    },
    "CP_VibrateInChats": {
        "id": "Getaran dalam obrolan", "ja": "チャット内バイブレーション", "ko": "채팅 내 진동",
        "zh_cn": "聊天内振动", "zh_tw": "聊天內震動"
    },
    "CP_VibrateInChats_Desc": {
        "id": "Bergetar saat pesan tiba dalam obrolan yang terbuka.",
        "ja": "開いているチャットにメッセージが届いたときに振動します。",
        "ko": "열려 있는 채팅에서 메시지가 오면 진동합니다.",
        "zh_cn": "在打开的聊天中收到新消息时振动。",
        "zh_tw": "在開啟的聊天中收到新訊息時震動。"
    },
    "CP_VideoSeekDuration": {
        "id": "Langkah pemutaran video", "ja": "動画シーク間隔", "ko": "비디오 탐색 간격",
        "zh_cn": "快进/快退步长", "zh_tw": "快進/快退步長"
    },
    "Cancel": {
        "id": "Batal", "ja": "キャンセル", "ko": "취소",
        "zh_cn": "取消", "zh_tw": "取消"
    },
    "ChangePasscode": {
        "id": "Ubah Kode Sandi", "ja": "パスコードを変更", "ko": "암호 변경",
        "zh_cn": "修改锁屏密码", "zh_tw": "修改鎖定密碼"
    },
    "ChannelAdministrators": {
        "id": "Administrator", "ja": "管理者", "ko": "관리자",
        "zh_cn": "管理员", "zh_tw": "管理員"
    },
    "ChannelMembers": {
        "id": "Anggota", "ja": "メンバー", "ko": "멤버",
        "zh_cn": "成员", "zh_tw": "成員"
    },
    "ChannelPermissions": {
        "id": "Izin", "ja": "権限", "ko": "권한",
        "zh_cn": "权限", "zh_tw": "權限"
    },
    "Clear": {
        "id": "Bersihkan", "ja": "消去", "ko": "지우기",
        "zh_cn": "清除", "zh_tw": "清除"
    },
    "ClearMessageDatabase": {
        "id": "Hapus Basis Data Pesan", "ja": "メッセージデータベースを消去", "ko": "메시지 데이터베이스 지우기",
        "zh_cn": "清除消息数据库", "zh_tw": "清除訊息資料庫"
    },
    "ClearMessageDatabaseNotification": {
        "id": "Basis data pesan telah dibersihkan.", "ja": "メッセージデータベースが消去されました。", "ko": "메시지 데이터베이스가 지워졌습니다.",
        "zh_cn": "已清除消息数据库。", "zh_tw": "已清除訊息資料庫。"
    },
    "Copy": {
        "id": "Salin", "ja": "コピー", "ko": "복사",
        "zh_cn": "复制", "zh_tw": "複製"
    },
    "CopyLink": {
        "id": "Salin Tautan", "ja": "リンクをコピー", "ko": "링크 복사",
        "zh_cn": "复制链接", "zh_tw": "複製連結"
    },
    "Deactivate": {
        "id": "HAPUS SEKARANG", "ja": "今すぐ削除", "ko": "지금 삭제",
        "zh_cn": "立即注销", "zh_tw": "立即註銷"
    },
    "Default": {
        "id": "Default", "ja": "デフォルト", "ko": "기본",
        "zh_cn": "默认", "zh_tw": "預設"
    },
    "Delete": {
        "id": "Hapus", "ja": "削除", "ko": "삭제",
        "zh_cn": "删除", "zh_tw": "刪除"
    },
    "DirectShare": {
        "id": "Berbagi Langsung", "ja": "ダイレクトシェア", "ko": "다이렉트 공유",
        "zh_cn": "直接分享", "zh_tw": "直接分享"
    },
    "DirectShareInfo": {
        "id": "Tampilkan obrolan terbaru di menu berbagi Android",
        "ja": "Androidの共有メニューに最近のチャットを表示",
        "ko": "Android 공유 메뉴에 최근 대화 표시",
        "zh_cn": "在 Android 系统分享菜单中显示最近聊天",
        "zh_tw": "在 Android 系統分享選單中顯示最近聊天"
    },
    "Disable": {
        "id": "Nonaktifkan", "ja": "無効", "ko": "비활성화",
        "zh_cn": "关闭", "zh_tw": "關閉"
    },
    "DisablePasscodeTurnOff": {
        "id": "Matikan", "ja": "オフにする", "ko": "끄기",
        "zh_cn": "关闭", "zh_tw": "關閉"
    },
    "Done": {
        "id": "Selesai", "ja": "完了", "ko": "완료",
        "zh_cn": "完成", "zh_tw": "完成"
    },
    "DontReadStoriesPackets": {
        "id": "Jangan Baca Cerita", "ja": "ストーリーの既読を送信しない", "ko": "스토리 읽음 처리 안 함",
        "zh_cn": "不显示动态已读", "zh_tw": "不顯示限時動態已讀"
    },
    "DontSendOnlinePackets": {
        "id": "Jangan Kirim Status Online", "ja": "オンライン状態を送信しない", "ko": "온라인 상태 전송 안 함",
        "zh_cn": "隐身模式", "zh_tw": "隱身模式"
    },
    "DontSendReadMessagePackets": {
        "id": "Jangan Baca Pesan", "ja": "メッセージの既読を送信しない", "ko": "메시지 읽음 처리 안 함",
        "zh_cn": "不显示消息已读", "zh_tw": "不顯示訊息已讀"
    },
    "DontSendUploadProgress": {
        "id": "Jangan Kirim Sedang Mengetik", "ja": "入力中を送信しない", "ko": "입력 중 상태 전송 안 함",
        "zh_cn": "不显示正在输入", "zh_tw": "不顯示正在輸入"
    },
    "EP_CustomChat": {
        "id": "Ganti Pesan Tersimpan", "ja": "保存されたメッセージを置換", "ko": "저장한 메시지 대체",
        "zh_cn": "替换收藏夹", "zh_tw": "替換收藏夾"
    },
    "EP_CustomChat_Desc": {
        "id": "Pilih obrolan yang digunakan sebagai pengganti Pesan Tersimpan.",
        "ja": "「保存されたメッセージ」の代わりに使用するチャットを選択します。",
        "ko": "저장한 메시지 대신 사용할 대화를 선택하세요.",
        "zh_cn": "选择代替默认收藏夹使用的对话。",
        "zh_tw": "選擇代替預設收藏夾使用的對話。"
    },
    "EP_CustomChat_Selected_Title": {
        "id": "sebagai pengganti", "ja": "の代わりに", "ko": "대신",
        "zh_cn": "代替", "zh_tw": "代替"
    },
    "EP_DownloadSpeedBoost": {
        "id": "Unduhan lebih cepat", "ja": "ダウンロード高速化", "ko": "다운로드 속도 향상",
        "zh_cn": "下载加速", "zh_tw": "下載加速"
    },
    "EP_DownloadSpeedBoostExtreme": {
        "id": "Ekstrem", "ja": "エクストリーム", "ko": "최대",
        "zh_cn": "极速", "zh_tw": "極速"
    },
    "EP_NavigationAnimation": {
        "id": "Animasi navigasi", "ja": "ナビゲーションアニメーション", "ko": "탐색 애니메이션",
        "zh_cn": "页面导航动画", "zh_tw": "頁面導覽動畫"
    },
    "EP_NavigationAnimationBezier": {
        "id": "Klasik", "ja": "クラシック", "ko": "기본",
        "zh_cn": "经典", "zh_tw": "經典"
    },
    "EP_NavigationAnimationCrossfading": {
        "id": "Transisi judul yang halus", "ja": "スムーズなタイトル遷移", "ko": "부드러운 제목 전환",
        "zh_cn": "平滑标题过渡", "zh_tw": "平滑標題過渡"
    },
    "EP_NavigationAnimationSpring": {
        "id": "Pegas", "ja": "スプリング", "ko": "스프링",
        "zh_cn": "弹性", "zh_tw": "彈性"
    },
    "EP_Network": {
        "id": "Jaringan", "ja": "ネットワーク", "ko": "네트워크",
        "zh_cn": "网络", "zh_tw": "網路"
    },
    "EP_PhotosSize": {
        "id": "Foto besar hingga 2560 px", "ja": "最大2560pxの大きな写真", "ko": "최대 2560px의 큰 사진",
        "zh_cn": "高达 2560 px 的高清图片", "zh_tw": "高達 2560 px 的高畫質圖片"
    },
    "EP_SlowNetworkMode": {
        "id": "Mode jaringan lemah", "ja": "低速ネットワークモード", "ko": "저속 네트워크 모드",
        "zh_cn": "弱网模式", "zh_tw": "弱網模式"
    },
    "Edit": {
        "id": "Edit", "ja": "編集", "ko": "수정",
        "zh_cn": "编辑", "zh_tw": "編輯"
    },
    "Emoji": {
        "id": "Emoji", "ja": "絵文字", "ko": "이모지",
        "zh_cn": "Emoji", "zh_tw": "Emoji"
    },
    "EnableSaveDeletedMessages": {
        "id": "Simpan Pesan yang Dihapus", "ja": "削除済みメッセージを保存", "ko": "삭제된 메시지 저장",
        "zh_cn": "保存已删除消息", "zh_tw": "保存已刪除訊息"
    },
    "EnableSaveEditsHistory": {
        "id": "Simpan Riwayat Pengeditan", "ja": "編集履歴を保存", "ko": "수정 기록 저장",
        "zh_cn": "保存编辑历史记录", "zh_tw": "保存編輯歷程記錄"
    },
    "ErrorOccurred": {
        "id": "Terjadi kesalahan.", "ja": "エラーが発生しました。", "ko": "오류가 발생했습니다.",
        "zh_cn": "发生错误。", "zh_tw": "發生錯誤。"
    },
    "EventLog": {
        "id": "Tindakan Terbaru", "ja": "最近の操作", "ko": "최근 활동",
        "zh_cn": "近期操作", "zh_tw": "近期操作"
    },
    "FilterAddChats": {
        "id": "Tambah Obrolan", "ja": "チャットを追加", "ko": "대화 추가",
        "zh_cn": "添加对话", "zh_tw": "新增對話"
    },
    "FilterAllChats": {
        "id": "Semua Obrolan", "ja": "すべてのチャット", "ko": "모든 대화",
        "zh_cn": "全部聊天", "zh_tw": "全部聊天"
    },
    "FilterBots": {
        "id": "Bot", "ja": "ボット", "ko": "봇",
        "zh_cn": "机器人", "zh_tw": "機器人"
    },
    "FilterChannels": {
        "id": "Saluran", "ja": "チャンネル", "ko": "채널",
        "zh_cn": "频道", "zh_tw": "頻道"
    },
    "FilterChats": {
        "id": "Obrolan", "ja": "チャット", "ko": "대화",
        "zh_cn": "聊天", "zh_tw": "聊天"
    },
    "FilterContacts": {
        "id": "Kontak", "ja": "連絡先", "ko": "연락처",
        "zh_cn": "联系人", "zh_tw": "聯絡人"
    },
    "FilterGroups": {
        "id": "Grup", "ja": "グループ", "ko": "그룹",
        "zh_cn": "群组", "zh_tw": "群組"
    },
    "FilterNameNonMuted": {
        "id": "Bersuara", "ja": "ミュート解除", "ko": "소리 켜짐",
        "zh_cn": "未静音", "zh_tw": "未靜音"
    },
    "FilterNameUnread": {
        "id": "Belum Dibaca", "ja": "未読", "ko": "읽지 않음",
        "zh_cn": "未读", "zh_tw": "未讀"
    },
    "FilterNonContacts": {
        "id": "Bukan Kontak", "ja": "連絡先以外", "ko": "연락처 외",
        "zh_cn": "非联系人", "zh_tw": "非聯絡人"
    },
    "FilterStories": {
        "id": "Cerita", "ja": "ストーリー", "ko": "스토리",
        "zh_cn": "动态", "zh_tw": "限時動態"
    },
    "Forward": {
        "id": "Teruskan", "ja": "転送", "ko": "전달",
        "zh_cn": "转发", "zh_tw": "轉傳"
    },
    "ForwardWithoutAuthor": {
        "id": "Teruskan tanpa pengarang", "ja": "作成者を非表示で転送", "ko": "작성자 없이 전달",
        "zh_cn": "无来源转发", "zh_tw": "無來源轉傳"
    },
    "General": {
        "id": "Umum", "ja": "一般", "ko": "일반",
        "zh_cn": "常规", "zh_tw": "一般"
    },
    "GhostEssentialsHeader": {
        "id": "Pengaturan Hantu", "ja": "ゴースト設定", "ko": "유령 모드 설정",
        "zh_cn": "幽灵模式核心设置", "zh_tw": "幽靈模式核心設定"
    },
    "Info": {
        "id": "Info", "ja": "情報", "ko": "정보",
        "zh_cn": "信息", "zh_tw": "資訊"
    },
    "LinkCopied": {
        "id": "Tautan disalin ke papan klip", "ja": "リンクがクリップボードにコピーされました", "ko": "클립보드에 링크 복사됨",
        "zh_cn": "链接已复制到剪贴板", "zh_tw": "連結已複製到剪貼簿"
    },
    "LiteBatteryDisabled": {
        "id": "Mati", "ja": "オフ", "ko": "꺼짐",
        "zh_cn": "关闭", "zh_tw": "關閉"
    },
    "LiteBatteryEnabled": {
        "id": "Nyala", "ja": "オン", "ko": "켜짐",
        "zh_cn": "开启", "zh_tw": "開啟"
    },
    "LiteMode": {
        "id": "Animasi", "ja": "アニメーション", "ko": "애니메이션",
        "zh_cn": "动画效果", "zh_tw": "動畫效果"
    },
    "LocalMiscellaneousCache": {
        "id": "Lain-lain", "ja": "その他", "ko": "기타",
        "zh_cn": "杂项", "zh_tw": "雜項"
    },
    "MainTabsChats": {
        "id": "Obrolan", "ja": "チャット", "ko": "대화",
        "zh_cn": "聊天", "zh_tw": "聊天"
    },
    "MainTabsProfile": {
        "id": "Profil", "ja": "プロフィール", "ko": "프로필",
        "zh_cn": "个人资料", "zh_tw": "個人檔案"
    },
    "Message": {
        "id": "Pesan", "ja": "メッセージ", "ko": "메시지",
        "zh_cn": "消息", "zh_tw": "訊息"
    },
    "MessageSavingSaveMedia": {
        "id": "Simpan Media", "ja": "メディアを保存", "ko": "미디어 저장",
        "zh_cn": "保存媒体", "zh_tw": "保存媒體"
    },
    "MessageSavingSaveMediaHint": {
        "id": "Klik untuk lainnya.", "ja": "タップして詳細を表示。", "ko": "자세한 내용을 보려면 클릭하세요.",
        "zh_cn": "点击了解更多。", "zh_tw": "點擊瞭解更多。"
    },
    "MessageSavingSaveMediaInPrivateChannels": {
        "id": "… di Saluran Pribadi", "ja": "… プライベートチャンネル内", "ko": "… 비공개 채널에서",
        "zh_cn": "… 在私密频道中", "zh_tw": "… 在私密頻道中"
    },
    "MessageSavingSaveMediaInPrivateChats": {
        "id": "… di Obrolan Pribadi", "ja": "… プライベートチャット内", "ko": "… 1:1 대화에서",
        "zh_cn": "… 在私聊中", "zh_tw": "… 在私聊中"
    },
    "MessageSavingSaveMediaInPrivateGroups": {
        "id": "… di Grup Pribadi", "ja": "… プライベートグループ内", "ko": "… 비공개 그룹에서",
        "zh_cn": "… 在私密群组中", "zh_tw": "… 在私密群組中"
    },
    "MessageSavingSaveMediaInPublicChannels": {
        "id": "… di Saluran Publik", "ja": "… 公開チャンネル内", "ko": "… 공개 채널에서",
        "zh_cn": "… 在公开频道中", "zh_tw": "… 在公開頻道中"
    },
    "MessageSavingSaveMediaInPublicGroups": {
        "id": "… di Grup Publik", "ja": "… 公開グループ内", "ko": "… 공개 그룹에서",
        "zh_cn": "… 在公开群组中", "zh_tw": "… 在公開群組中"
    },
    "MessagesSettings": {
        "id": "Pesan", "ja": "メッセージ", "ko": "메시지",
        "zh_cn": "消息设置", "zh_tw": "訊息設定"
    },
    "Music": {
        "id": "Musik", "ja": "音楽", "ko": "음악",
        "zh_cn": "音乐", "zh_tw": "音樂"
    },
    "NM_AP_BottomTabs": {
        "id": "Pengatur tab bawah", "ja": "下部タブ管理", "ko": "하단 탭 관리",
        "zh_cn": "底部导航栏管理", "zh_tw": "底部導覽列管理"
    },
    "NM_AP_Folders": {
        "id": "Folder", "ja": "フォルダー", "ko": "폴더",
        "zh_cn": "分组", "zh_tw": "分組"
    },
    "NM_AvatarCorners": {
        "id": "Sudut avatar", "ja": "アバターの角", "ko": "프로필 사진 모서리",
        "zh_cn": "头像圆角", "zh_tw": "頭像圓角"
    },
    "NM_AvatarCornersCircle": {
        "id": "Lingkaran", "ja": "円形", "ko": "원형",
        "zh_cn": "圆形", "zh_tw": "圓形"
    },
    "NM_AvatarCornersSquare": {
        "id": "Persegi", "ja": "四角形", "ko": "사각형",
        "zh_cn": "方形", "zh_tw": "方形"
    },
    "NM_AvatarCorners_Header": {
        "id": "Avatar", "ja": "アバター", "ko": "프로필 사진",
        "zh_cn": "头像", "zh_tw": "頭像"
    },
    "NM_BAN_AvatarBanner": {
        "id": "Banner dari avatar", "ja": "アバターからバナー", "ko": "프로필 사진으로 배너 만들기",
        "zh_cn": "头像生成横幅", "zh_tw": "頭像產生橫幅"
    },
    "NM_BAN_ChangeGlobal": {
        "id": "Ubah banner publik", "ja": "公開バナーを変更", "ko": "공개 배너 변경",
        "zh_cn": "修改公开横幅", "zh_tw": "修改公開橫幅"
    },
    "NM_BAN_DeleteLocal": {
        "id": "Hapus banner lokal", "ja": "ローカルバナーを削除", "ko": "로컬 배너 삭제",
        "zh_cn": "删除本地横幅", "zh_tw": "刪除本機橫幅"
    },
    "NM_BAN_PickLocal": {
        "id": "Pilih banner lokal", "ja": "ローカルバナーを選択", "ko": "로컬 배너 선택",
        "zh_cn": "选择本地横幅", "zh_tw": "選擇本機橫幅"
    },
    "NM_BAN_RefreshStatus": {
        "id": "Segarkan status", "ja": "ステータスを更新", "ko": "상태 새로고침",
        "zh_cn": "刷新状态", "zh_tw": "重新整理狀態"
    },
    "NM_BAN_StatusLabel": {
        "id": "Status", "ja": "ステータス", "ko": "상태",
        "zh_cn": "状态", "zh_tw": "狀態"
    },
    "NM_BAN_SubmitModeration": {
        "id": "Kirim untuk moderasi", "ja": "審査に提出", "ko": "검토 제출",
        "zh_cn": "提交审核", "zh_tw": "提交審核"
    },
    "NM_BT_EditorFooter": {
        "id": "Ketuk untuk beralih. Tekan lama untuk memindahkan.",
        "ja": "タップして切り替え。長押しでドラッグして並べ替え。",
        "ko": "탭하여 켜거나 끄고, 길게 눌러 순서를 변경하세요.",
        "zh_cn": "轻按以切换。长按可拖动重新排序。",
        "zh_tw": "輕按以切換。長按可拖曳重新排序。"
    },
    "NM_BT_ForceOpenChats": {
        "id": "Selalu cari dalam obrolan", "ja": "常にチャット内を検索", "ko": "항상 대화에서 검색",
        "zh_cn": "搜索按钮始终搜索聊天", "zh_tw": "搜尋按鈕始終搜尋聊天"
    },
    "NM_BT_LayoutHeader": {
        "id": "Tab", "ja": "タブ", "ko": "탭",
        "zh_cn": "标签页", "zh_tw": "標籤頁"
    },
    "NM_BT_MoveLeft": {
        "id": "Pindah ke kiri", "ja": "左へ移動", "ko": "왼쪽으로 이동",
        "zh_cn": "左移", "zh_tw": "左移"
    },
    "NM_BT_MoveRight": {
        "id": "Pindah ke kanan", "ja": "右へ移動", "ko": "오른쪽으로 이동",
        "zh_cn": "右移", "zh_tw": "右移"
    },
    "NM_BT_ShowSearchInTabs": {
        "id": "Tombol pencarian di tab", "ja": "タブに検索ボタンを表示", "ko": "탭 옆 검색 버튼",
        "zh_cn": "在底部标签旁显示搜索按钮", "zh_tw": "在底部標籤旁顯示搜尋按鈕"
    },
    "NM_BT_ShowSearchInTabs_Desc": {
        "id": "Tampilkan tombol pencarian melingkar di samping tab.",
        "ja": "下部タブの横に円形の検索ボタンを表示します。",
        "ko": "하단 탭 행 옆에 원형 검색 버튼을 표시합니다.",
        "zh_cn": "在底部标签栏旁显示一个圆形的搜索按钮。",
        "zh_tw": "在底部標籤列旁顯示一個圓形的搜尋按鈕。"
    },
    "NM_BT_ShowTabs": {
        "id": "Tampilkan tab", "ja": "タブを表示", "ko": "탭 표시",
        "zh_cn": "显示底部标签栏", "zh_tw": "顯示底部標籤列"
    },
    "NM_BT_ShowTabsTitle": {
        "id": "Tampilkan judul tab", "ja": "タブのタイトルを表示", "ko": "탭 제목 표시",
        "zh_cn": "显示标签文字", "zh_tw": "顯示標籤文字"
    },
    "NM_BT_TabDisabled": {
        "id": "Nonaktif", "ja": "無効", "ko": "꺼짐",
        "zh_cn": "已停用", "zh_tw": "已停用"
    },
    "NM_BT_TabEnabled": {
        "id": "Aktif", "ja": "有効", "ko": "켜짐",
        "zh_cn": "已启用", "zh_tw": "已啟用"
    },
    "NM_CAM_AskCamera": {
        "id": "Tanya", "ja": "確認する", "ko": "항상 묻기",
        "zh_cn": "每次询问", "zh_tw": "每次詢問"
    },
    "NM_CAM_ContinuousFocus": {
        "id": "Fokus otomatis berkelanjutan", "ja": "コンティニュアスAF", "ko": "연속 자동 초점",
        "zh_cn": "持续自动对焦", "zh_tw": "持續自動對焦"
    },
    "NM_CAM_ContinuousFocus_Desc": {
        "id": "Kamera terus memfokuskan ulang saat merekam video.",
        "ja": "動画撮影中、フォーカスを固定せず常にピントを合わせ続けます。",
        "ko": "동영상 촬영 중 초점을 한 번만 맞추지 않고 지속적으로 다시 맞춥니다.",
        "zh_cn": "录制视频时持续重新对焦，而非锁定单次焦点。",
        "zh_tw": "錄製視訊時持續重新對焦，而非鎖定單次焦點。"
    },
    "NM_CAM_FrontCamera": {
        "id": "Kamera depan", "ja": "前面カメラ", "ko": "전면 카메라",
        "zh_cn": "前置摄像头", "zh_tw": "前置鏡頭"
    },
    "NM_CAM_Improvements": {
        "id": "Peningkatan", "ja": "改善点", "ko": "개선 사항",
        "zh_cn": "改进功能", "zh_tw": "改進功能"
    },
    "NM_CAM_OpticalStabilization": {
        "id": "Stabilisasi optik", "ja": "光学式手ぶれ補正", "ko": "광학식 손떨림 보정",
        "zh_cn": "光学防抖 (OIS)", "zh_tw": "光學防手震 (OIS)"
    },
    "NM_CAM_OpticalStabilization_Desc": {
        "id": "Gunakan stabilisasi gambar optik saat tersedia.",
        "ja": "利用可能な場合、レンズレベルの光学式手ぶれ補正を使用します。",
        "ko": "사용 가능한 경우 렌즈 수준의 광학식 손떨림 보정을 사용합니다.",
        "zh_cn": "在设备支持时启用镜头硬件光学防抖。",
        "zh_tw": "在裝置支援時啟用鏡頭硬體光學防手震。"
    },
    "NM_CAM_RearCamera": {
        "id": "Kamera belakang", "ja": "背面カメラ", "ko": "후면 카메라",
        "zh_cn": "后置摄像头", "zh_tw": "後置鏡頭"
    },
    "NM_CAM_RoundCamera": {
        "id": "Kamera untuk pesan video", "ja": "ビデオメッセージ用カメラ", "ko": "비디오 메시지용 카메라",
        "zh_cn": "视频消息摄像头", "zh_tw": "視訊訊息鏡頭"
    },
    "NM_CAM_RoundVideoBitrate": {
        "id": "Bitrate video bulat", "ja": "ビデオメッセージのビットレート", "ko": "비디오 메시지 비트레이트",
        "zh_cn": "圆形视频码率", "zh_tw": "圓形視訊碼率"
    },
    "NM_CAM_RoundVideoSize": {
        "id": "Ukuran video bulat", "ja": "ビデオメッセージのサイズ", "ko": "비디오 메시지 크기",
        "zh_cn": "圆形视频分辨率", "zh_tw": "圓形視訊解析度"
    },
    "NM_CAM_RoundVideoSize_Auto": {
        "id": "Otomatis", "ja": "自動", "ko": "자동",
        "zh_cn": "自动", "zh_tw": "自動"
    },
    "NM_CAM_RoundVideoSize_HD": {
        "id": "HD (512p)", "ja": "HD (512p)", "ko": "HD (512p)",
        "zh_cn": "HD (512p)", "zh_tw": "HD (512p)"
    },
    "NM_CAM_RoundVideoSize_SD": {
        "id": "SD (240p)", "ja": "SD (240p)", "ko": "SD (240p)",
        "zh_cn": "SD (240p)", "zh_tw": "SD (240p)"
    },
    "NM_CAM_RoundVideoSize_STD": {
        "id": "Standar (384p)", "ja": "標準 (384p)", "ko": "표준 (384p)",
        "zh_cn": "标准 (384p)", "zh_tw": "標準 (384p)"
    },
    "NM_CAM_VideoQuality": {
        "id": "Kualitas video", "ja": "ビデオ品質", "ko": "비디오 품질",
        "zh_cn": "视频画质", "zh_tw": "視訊畫質"
    },
    "NM_CARDS_AutoScroll": {
        "id": "Gulir otomatis", "ja": "自動スクロール", "ko": "자동 스크롤",
        "zh_cn": "自动滚动", "zh_tw": "自動捲動"
    },
    "NM_CARDS_Enable": {
        "id": "Aktifkan", "ja": "有効にする", "ko": "활성화",
        "zh_cn": "启用", "zh_tw": "啟用"
    },
    "NM_CARDS_GeneralHeader": {
        "id": "Umum", "ja": "一般", "ko": "일반",
        "zh_cn": "常规", "zh_tw": "一般"
    },
    "NM_CARDS_InfiniteScroll": {
        "id": "Gulir tak terbatas", "ja": "無限スクロール", "ko": "무한 스크롤",
        "zh_cn": "无限循环滚动", "zh_tw": "無限循環捲動"
    },
    "NM_CARDS_NameBitcoin": {
        "id": "Bitcoin", "ja": "ビットコイン", "ko": "비트코인",
        "zh_cn": "比特币", "zh_tw": "比特幣"
    },
    "NM_CARDS_NameGram": {
        "id": "Gram", "ja": "Gram", "ko": "Gram",
        "zh_cn": "Gram", "zh_tw": "Gram"
    },
    "NM_CARDS_NameProxy": {
        "id": "Proxy", "ja": "プロキシ", "ko": "프록시",
        "zh_cn": "代理", "zh_tw": "代理"
    },
    "NM_CARDS_NameStorage": {
        "id": "Penyimpanan", "ja": "ストレージ", "ko": "저장 공간",
        "zh_cn": "存储空间", "zh_tw": "儲存空間"
    },
    "NM_CARDS_NameUsd": {
        "id": "Dolar AS", "ja": "米ドル", "ko": "미국 달러",
        "zh_cn": "美元", "zh_tw": "美元"
    },
    "NM_CARDS_NameWeather": {
        "id": "Cuaca", "ja": "天気", "ko": "날씨",
        "zh_cn": "天气", "zh_tw": "天氣"
    },
    "NM_CARDS_Title": {
        "id": "Info cepat", "ja": "クイック情報", "ko": "빠른 정보 카드",
        "zh_cn": "快捷信息卡片", "zh_tw": "快捷資訊卡片"
    },
    "NM_CH_RecentEmojisStickers": {
        "id": "Emoji dan stiker terbaru", "ja": "最近の絵文字とステッカー", "ko": "최근 이모지 및 스티커",
        "zh_cn": "最近使用的表情和贴纸", "zh_tw": "最近使用的表情和貼圖"
    },
    "NM_CH_RecentEmojisStickers_Desc": {
        "id": "Tingkatkan untuk menyimpan lebih banyak emoji baru di panel.",
        "ja": "パネルに保持する最近の絵文字とステッカーの数を増やします。",
        "ko": "패널에 보관할 최근 이모지 및 스티커 개수를 늘립니다.",
        "zh_cn": "增加面板中保留的最近使用表情和贴纸数量。",
        "zh_tw": "增加面板中保留的最近使用表情和貼圖數量。"
    },
    "NM_CP_CenterTitleInChat": {
        "id": "Judul obrolan di tengah", "ja": "チャットタイトルを中央揃え", "ko": "채팅 제목 중앙 정렬",
        "zh_cn": "聊天顶部标题居中", "zh_tw": "聊天頂部標題置中"
    },
    "NM_CP_Header_ChatView": {
        "id": "Tampilan obrolan", "ja": "チャット表示", "ko": "채팅 화면",
        "zh_cn": "聊天视图", "zh_tw": "聊天檢視"
    },
    "NM_CP_Header_Interaction": {
        "id": "Interaksi", "ja": "インタラクション", "ko": "상호작용",
        "zh_cn": "交互设置", "zh_tw": "互動設定"
    },
    "NM_CP_Header_Media": {
        "id": "Media", "ja": "メディア", "ko": "미디어",
        "zh_cn": "媒体", "zh_tw": "媒體"
    },
    "NM_CP_LatexRendering": {
        "id": "Render formula LaTeX", "ja": "LaTeX数式を描画", "ko": "LaTeX 수식 렌더링",
        "zh_cn": "渲染 LaTeX 公式", "zh_tw": "渲染 LaTeX 公式"
    },
    "NM_CP_LatexRendering_Desc": {
        "id": "Kenali formula di dalam $...$ dalam pesan.",
        "ja": "メッセージ内の $...$ 内の数式を認識します。",
        "ko": "메시지의 $...$ 안에 있는 수식을 인식합니다.",
        "zh_cn": "自动识别消息中 $...$ 包含的数学公式。",
        "zh_tw": "自動辨識訊息中 $...$ 包含的數學公式。"
    },
    "NM_CP_PreReformRussian": {
        "id": "Ejaan Rusia pra-reformasi", "ja": "改革前ロシア語の正書法", "ko": "개혁 이전 러시아어 맞춤법",
        "zh_cn": "1918年前俄语拼写", "zh_tw": "1918年前俄語拼寫"
    },
    "NM_CP_PreReformRussian_Desc": {
        "id": "Pesan keluar dalam ejaan sebelum 1918.",
        "ja": "1918年以前の正書法による送信メッセージ。",
        "ko": "1918년 이전 맞춤법으로 작성된 발신 메시지입니다.",
        "zh_cn": "以 1918 年之前的拼写格式发送消息。",
        "zh_tw": "以 1918 年之前的拼寫格式傳送訊息。"
    },
    "NM_CP_SnowflakesInChat": {
        "id": "Salju di obrolan", "ja": "チャット内の雪", "ko": "채팅방 눈 내림",
        "zh_cn": "聊天内下雪特效", "zh_tw": "聊天內下雪特效"
    },
    "NM_CP_WeekdayNearDate": {
        "id": "Tampilkan hari di samping tanggal", "ja": "日付の横に曜日を表示", "ko": "날짜 옆 요일 표시",
        "zh_cn": "日期旁显示星期", "zh_tw": "日期旁顯示星期"
    },
    "NM_CP_WeekdayNearDate_Desc": {
        "id": "Menambahkan hari dalam seminggu di samping tanggal obrolan.",
        "ja": "チャット内の日付の横に曜日を追加します。",
        "ko": "채팅 내 날짜 옆에 요일을 표시합니다.",
        "zh_cn": "在聊天中的日期旁添加星期几。",
        "zh_tw": "在聊天中的日期旁新增星期幾。"
    },
    "NM_Camera_FaceDetection": {
        "id": "Deteksi Wajah", "ja": "顔認識", "ko": "얼굴 인식",
        "zh_cn": "人脸检测", "zh_tw": "人臉偵測"
    },
    "NM_Camera_FaceDetection_Desc": {
        "id": "Prioritaskan wajah untuk pencahayaan dan fokus saat merekam.",
        "ja": "録画中に顔を優先して露出とフォーカスを合わせます。",
        "ko": "녹화 중 자동 노출과 초점이 얼굴을 우선하도록 합니다.",
        "zh_cn": "录制时优先对人脸进行自动曝光和自动对焦。",
        "zh_tw": "錄製時優先對人臉進行自動曝光和自動對焦。"
    },
    "NM_Camera_NoiseReduction": {
        "id": "Pengurangan Kebisingan", "ja": "ノイズ低減", "ko": "노이즈 감소",
        "zh_cn": "降噪算法", "zh_tw": "降噪演算法"
    },
    "NM_Camera_NoiseReduction_Desc": {
        "id": "Gunakan pengurangan kebisingan untuk kualitas rekaman lebih tinggi.",
        "ja": "より高画質で記録するためにノイズリダクションを適用します。",
        "ko": "고화질 녹화를 위해 시간적 노이즈 감소를 적용합니다.",
        "zh_cn": "在录制时应用降噪算法以获得更高画质。",
        "zh_tw": "在錄製時套用降噪演算法以獲得更高畫質。"
    },
    "NM_Camera_UseHighRange": {
        "id": "Rentang FPS Tinggi", "ja": "高FPS範囲", "ko": "높은 FPS 범위",
        "zh_cn": "高帧率范围 (FPS)", "zh_tw": "高幀率範圍 (FPS)"
    },
    "NM_Camera_UseHighRange_Desc": {
        "id": "Tampilkan rentang 30-60 dan 60-60 FPS pada pemilih FPS.",
        "ja": "FPSセレクターで30-60および60-60 FPSを表示します。",
        "ko": "FPS 선택기에서 30-60 및 60-60 FPS 범위를 표시합니다.",
        "zh_cn": "在帧率选择器中显示 30-60 和 60-60 FPS 范围。",
        "zh_tw": "在幀率選擇器中顯示 30-60 和 60-60 FPS 範圍。"
    },
    "NM_Chat_Section": {
        "id": "Obrolan", "ja": "チャット", "ko": "채팅",
        "zh_cn": "聊天", "zh_tw": "聊天"
    },
    "NM_ChooseFolderIcon": {
        "id": "Pilih Ikon", "ja": "アイコンを選択", "ko": "아이콘 선택",
        "zh_cn": "选择图标", "zh_tw": "選擇圖示"
    },
    "NM_Config_Error": {
        "id": "Gagal", "ja": "失敗しました", "ko": "실패",
        "zh_cn": "失败", "zh_tw": "失敗"
    },
    "NM_Config_Export": {
        "id": "Ekspor pengaturan", "ja": "設定をエクスポート", "ko": "설정 내보내기",
        "zh_cn": "导出设置", "zh_tw": "匯出設定"
    },
    "NM_Config_Exported": {
        "id": "Pengaturan disimpan ke file", "ja": "設定がファイルに保存されました", "ko": "설정이 파일에 저장되었습니다",
        "zh_cn": "设置已保存至文件", "zh_tw": "設定已儲存至檔案"
    },
    "NM_Config_Header": {
        "id": "Konfigurasi", "ja": "設定構成", "ko": "구성",
        "zh_cn": "配置备份", "zh_tw": "設定備份"
    },
    "NM_Config_Import": {
        "id": "Impor pengaturan", "ja": "設定をインポート", "ko": "설정 가져오기",
        "zh_cn": "导入设置", "zh_tw": "匯入設定"
    },
    "NM_Config_ImportEmpty": {
        "id": "File yang dipilih bukan konfigurasi SoundGram", "ja": "選択されたファイルはSoundGram設定ではありません", "ko": "선택한 파일은 SoundGram 구성 파일이 아닙니다",
        "zh_cn": "所选文件不是 SoundGram 配置文件", "zh_tw": "所選檔案不是 SoundGram 設定檔"
    },
    "NM_Config_Info": {
        "id": "Simpan pengaturan ke file atau pulihkan di perangkat lain.",
        "ja": "設定をファイルに保存するか、別のデバイスで復元します。",
        "ko": "설정을 파일에 저장하거나 다른 기기에서 복원하세요.",
        "zh_cn": "将设置保存为文件或在其他设备上恢复。",
        "zh_tw": "將設定儲存為檔案或在其他裝置上還原。"
    },
    "NM_CustomTitle": {
        "id": "Judul aplikasi", "ja": "アプリのタイトル", "ko": "앱 제목",
        "zh_cn": "应用顶部标题", "zh_tw": "應用程式頂部標題"
    },
    "NM_DBG_AudioSource": {
        "id": "Sumber audio mikrofon", "ja": "マイクの音声入力ソース", "ko": "마이크 오디오 소스",
        "zh_cn": "麦克风音频源", "zh_tw": "麥克風音訊來源"
    },
    "NM_DBG_EditTextFix": {
        "id": "Perbaikan saran EditText", "ja": "EditText候補の修正", "ko": "EditText 추천 수정",
        "zh_cn": "输入框联想词修复", "zh_tw": "輸入框聯想詞修復"
    },
    "NM_DBG_ForceCrash": {
        "id": "Paksa mogok", "ja": "強制クラッシュ", "ko": "강제 충돌 발생",
        "zh_cn": "强制崩溃测试", "zh_tw": "強制當機測試"
    },
    "NM_DBG_ForceCrash_Desc": {
        "id": "Memicu crash yang disengaja untuk menguji laporan kesalahan.",
        "ja": "クラッシュレポートと復元をテストするために意図的にクラッシュを発生させます。",
        "ko": "오류 보고 및 복구를 테스트하기 위해 의도적으로 충돌을 일으킵니다.",
        "zh_cn": "触发故意崩溃以测试错误报告和恢复机制。",
        "zh_tw": "觸發故意當機以測試錯誤報告和復原機制。"
    },
    "NM_DBG_Header_Danger": {
        "id": "Zona berbahaya", "ja": "危険ゾーン", "ko": "위험 구역",
        "zh_cn": "危险区域", "zh_tw": "危險區域"
    },
    "NM_DBG_HideVideoTimestamp": {
        "id": "Sembunyikan cap waktu video", "ja": "動画のタイムスタンプを非表示", "ko": "동영상 타임스탬프 숨기기",
        "zh_cn": "隐藏视频时间戳", "zh_tw": "隱藏視訊時間戳記"
    },
    "NM_DBG_JacksonJSONProvider": {
        "id": "Gunakan penyedia Jackson JSON", "ja": "Jackson JSONプロバイダーを使用", "ko": "Jackson JSON 공급자 사용",
        "zh_cn": "使用 Jackson JSON 解析器", "zh_tw": "使用 Jackson JSON 解析器"
    },
    "NM_DBG_OldTimeStyle": {
        "id": "Gaya waktu default di obrolan", "ja": "チャット内の標準時間スタイル", "ko": "채팅 내 기본 시간 스타일",
        "zh_cn": "聊天内经典时间样式", "zh_tw": "聊天內經典時間樣式"
    },
    "NM_DBG_ReplacePunctuation": {
        "id": "Ganti tanda baca", "ja": "句読点を置換", "ko": "문장 부호 대체",
        "zh_cn": "替换标点符号", "zh_tw": "替換標點符號"
    },
    "NM_DBG_SendMaxQuality": {
        "id": "Kirim video dengan kualitas maksimal", "ja": "最高画質で動画を送信", "ko": "최대 화질로 비디오 보내기",
        "zh_cn": "以最高画质发送视频", "zh_tw": "以最高畫質傳送視訊"
    },
    "NM_DBG_ShowAccounts": {
        "id": "Tampilkan akun di menu samping", "ja": "サイドメニューにアカウントを表示", "ko": "서랍 메뉴에 계정 표시",
        "zh_cn": "在侧边栏显示多账号", "zh_tw": "在側邊欄顯示多帳號"
    },
    "NM_DBG_ShowRPCErrors": {
        "id": "Tampilkan kesalahan RPC", "ja": "RPCエラーを表示", "ko": "RPC 오류 표시",
        "zh_cn": "显示 RPC 错误", "zh_tw": "顯示 RPC 錯誤"
    },
    "NM_DeleteAllFromSelf": {
        "id": "Hapus pesan saya", "ja": "自分のメッセージを削除", "ko": "내 메시지 삭제",
        "zh_cn": "一键删除我的消息", "zh_tw": "一鍵刪除我的訊息"
    },
    "NM_DisableSendHints": {
        "id": "Nonaktifkan petunjuk pengiriman", "ja": "送信ヒントを無効化", "ko": "전송 힌트 비활성화",
        "zh_cn": "禁用发送提示", "zh_tw": "停用傳送提示"
    },
    "NM_DisableSendHints_Desc": {
        "id": "Sembunyikan petunjuk pengiriman terjadwal dan hening.",
        "ja": "予約送信やサイレント送信のヒントを非表示にします。",
        "ko": "예약 전송 및 무음 전송 힌트를 숨깁니다.",
        "zh_cn": "隐藏定时发送和无声发送的提示说明。",
        "zh_tw": "隱藏定時傳送和無聲傳送的提示說明。"
    },
    "NM_DoubleTap_EditOrReact": {
        "id": "Edit milik sendiri, bereaksi ke orang lain", "ja": "自分のメッセージは編集、他人のメッセージにはリアクション", "ko": "내 것은 수정, 상대 것은 반응",
        "zh_cn": "双击自己的消息编辑，双击他人消息添加反应", "zh_tw": "連按兩下自己的訊息編輯，連按兩下他人訊息新增反應"
    },
    "NM_EXP_DiscussInsteadOfMute": {
        "id": "Bahas alih-alih membisukan saat menggeser", "ja": "スワイプでミュートの代わりに議論を開く", "ko": "스와이프 시 음소거 대신 토론방 열기",
        "zh_cn": "滑动打开讨论组而非静音", "zh_tw": "滑動開啟討論群而非靜音"
    },
    "NM_EXP_EdgeToEdge": {
        "id": "Mode layar penuh edge-to-edge", "ja": "全画面 Edge-to-edge モード", "ko": "엣지 투 엣지 모드",
        "zh_cn": "全面屏 Edge-to-edge 沉浸模式", "zh_tw": "全螢幕 Edge-to-edge 沉浸模式"
    },
    "NM_EXP_Footer": {
        "id": "Pilihan ini eksperimental — bug mungkin terjadi.",
        "ja": "これらのオプションは実験的です。不具合が発生する可能性があります。",
        "ko": "이 옵션들은 실험적이며 버그가 발생할 수 있습니다.",
        "zh_cn": "这些选项为实验性功能，可能会有未预期的错误。",
        "zh_tw": "這些選項為實驗性功能，可能會有未預期的錯誤。"
    },
    "NM_EXP_Header_Beta": {
        "id": "Fitur beta", "ja": "ベータ版機能", "ko": "베타 스위치",
        "zh_cn": "Beta 实验开关", "zh_tw": "Beta 實驗開關"
    },
    "NM_EXP_NoRounding": {
        "id": "Nonaktifkan pembulatan sudut", "ja": "角丸を無効化", "ko": "모서리 둥글기 비활성화",
        "zh_cn": "禁用所有圆角效果", "zh_tw": "停用所有圓角效果"
    },
    "NM_EXP_Title": {
        "id": "Eksperimental", "ja": "実験的", "ko": "실험실",
        "zh_cn": "实验性功能", "zh_tw": "實驗性功能"
    },
    "NM_Effects_Header": {
        "id": "Efek", "ja": "エフェクト", "ko": "효과",
        "zh_cn": "视觉特效", "zh_tw": "視覺特效"
    },
    "NM_FO_FolderNameInHeader": {
        "id": "Nama folder di header", "ja": "ヘッダーにフォルダー名を表示", "ko": "헤더에 폴더 이름 표시",
        "zh_cn": "在顶部标题栏显示分组名称", "zh_tw": "在頂部標題列顯示分組名稱"
    },
    "NM_FO_FoldersAtBottom": {
        "id": "Tampilkan folder di bawah", "ja": "フォルダーを下部に表示", "ko": "폴더를 하단에 표시",
        "zh_cn": "在底部显示聊天分组", "zh_tw": "在底部顯示聊天分組"
    },
    "NM_FO_FoldersAtBottom_Desc": {
        "id": "Pindahkan tab folder ke bagian bawah daftar obrolan.",
        "ja": "フォルダータブをチャット一覧の最下部に移動します。",
        "ko": "폴더 탭을 대화 목록 하단으로 이동합니다.",
        "zh_cn": "将分组标签栏移至聊天列表的最下方。",
        "zh_tw": "將分組標籤列移至聊天列表的最下方。"
    },
    "NM_FO_TabModeIcon": {
        "id": "Hanya ikon", "ja": "アイコンのみ", "ko": "아이콘만",
        "zh_cn": "仅图标", "zh_tw": "僅圖示"
    },
    "NM_FO_TabModeMix": {
        "id": "Ikon dengan judul", "ja": "アイコンとタイトル", "ko": "아이콘 및 제목",
        "zh_cn": "图标与文字", "zh_tw": "圖示與文字"
    },
    "NM_FO_TabModeText": {
        "id": "Hanya judul", "ja": "タイトルのみ", "ko": "제목만",
        "zh_cn": "仅文字", "zh_tw": "僅文字"
    },
    "NM_FO_TabStyle": {
        "id": "Gaya tab", "ja": "タブのスタイル", "ko": "탭 스타일",
        "zh_cn": "标签页样式", "zh_tw": "標籤頁樣式"
    },
    "NM_FO_TabStyleStroke": {
        "id": "Tambahkan garis luar", "ja": "アウトラインを追加", "ko": "외곽선 추가",
        "zh_cn": "选中标签高亮描边", "zh_tw": "選中標籤醒目描邊"
    },
    "NM_FO_TabStyleStroke_Desc": {
        "id": "Sorot tab aktif dengan garis batas.",
        "ja": "アクティブなタブを輪郭で強調表示します。",
        "ko": "선택된 활성 탭을 외곽선으로 강조합니다.",
        "zh_cn": "为当前选中的标签添加外边框描边效果。",
        "zh_tw": "為當前選中的標籤新增外邊框描邊效果。"
    },
    "NM_FO_TabsHideAllChats": {
        "id": "Sembunyikan tab «Semua obrolan»", "ja": "「すべてのチャット」タブを非表示", "ko": "«모든 대화» 탭 숨기기",
        "zh_cn": "隐藏“全部聊天”标签", "zh_tw": "隱藏「全部聊天」標籤"
    },
    "NM_FO_TabsNoCounter": {
        "id": "Sembunyikan penghitung di tab", "ja": "タブの未読カウンターを非表示", "ko": "탭의 카운터 숨기기",
        "zh_cn": "隐藏标签上的未读消息计数器", "zh_tw": "隱藏標籤上的未讀訊息計數器"
    },
    "NM_ForceBlur": {
        "id": "Buram antarmuka", "ja": "インターフェースのぼかし", "ko": "인터페이스 블러",
        "zh_cn": "界面高斯模糊效果", "zh_tw": "介面高斯模糊效果"
    },
    "NM_ForumAvatarsLikeChats": {
        "id": "Bentuk avatar forum", "ja": "フォーラムアバターの形状", "ko": "포럼 프로필 사진 모양",
        "zh_cn": "话题论坛头像形状", "zh_tw": "話題論壇頭像形狀"
    },
    "NM_ForumAvatarsLikeChats_Desc": {
        "id": "Gunakan sudut yang sama dengan avatar obrolan.",
        "ja": "チャットアバターと同じ角丸形状を使用します。",
        "ko": "일반 채팅 프로필 사진과 동일한 모서리를 사용합니다.",
        "zh_cn": "使用与普通聊天头像相同的圆角形状。",
        "zh_tw": "使用與普通聊天頭像相同的圓角形狀。"
    },
    "NM_GEN_DeletedGifts": {
        "id": "Tampilkan hadiah Star yang dihapus", "ja": "削除されたスターギフトを表示", "ko": "삭제된 Star 선물 표시",
        "zh_cn": "在目录中显示已下架的星星礼物", "zh_tw": "在目錄中顯示已下架的星星禮物"
    },
    "NM_GEN_DeletedGifts_Desc": {
        "id": "Memasukkan kembali hadiah tersembunyi ke dalam katalog agar tetap dapat dikirim.",
        "ja": "非表示のギフトをカタログに再挿入して送信できるようにします。",
        "ko": "숨겨진 선물을 카탈로그에 다시 표시하여 보낼 수 있도록 합니다.",
        "zh_cn": "将已隐藏的星星礼物重新展示在礼物目录中以便发送。",
        "zh_tw": "將已隱藏的星星禮物重新展示在禮物目錄中以便傳送。"
    },
    "NM_GEN_LocalPremiumEmoji": {
        "id": "Emoji premium lokal", "ja": "ローカルプレミアム絵文字", "ko": "로컬 프리미엄 이모지",
        "zh_cn": "本地免费使用高级 Emoji", "zh_tw": "本機免費使用高級 Emoji"
    },
    "NM_GEN_LocalPremiumEmoji_Desc": {
        "id": "Kirim emoji premium tanpa langganan Premium.",
        "ja": "Premiumサブスクリプションなしでプレミアム絵文字を送信します。",
        "ko": "Premium 구독 없이 프리미엄 이모지를 전송합니다.",
        "zh_cn": "无需开通 Telegram Premium 即可发送高级专属表情。",
        "zh_tw": "無需開通 Telegram Premium 即可傳送高級專屬表情。"
    },
    "NM_GEN_MiscHeader": {
        "id": "Lain-lain", "ja": "その他", "ko": "기타",
        "zh_cn": "其他杂项", "zh_tw": "其他雜項"
    },
    "NM_GE_UploadSpeedBoost": {
        "id": "Unggahan lebih cepat", "ja": "アップロード高速化", "ko": "업로드 속도 향상",
        "zh_cn": "上传提速", "zh_tw": "上傳提速"
    },
    "NM_HUB_ForkedFromCherrygramSub": {
        "id": "SoundGram dibangun di atas Telegram resmi; fitur di-porting dari Cherrygram.",
        "ja": "SoundGramは公式Telegram上に構築され、機能はCherrygramから移植されました。",
        "ko": "SoundGram은 공식 Telegram 기반이며, 기능은 Cherrygram에서 이식되었습니다.",
        "zh_cn": "SoundGram 基于 Telegram 官方客户端构建，诸多特性移植自 Cherrygram。",
        "zh_tw": "SoundGram 基於 Telegram 官方用戶端建構，諸多特性移植自 Cherrygram。"
    },
    "NM_HUB_SourceCode": {
        "id": "Kode sumber", "ja": "ソースコード", "ko": "소스 코드",
        "zh_cn": "开放源代码", "zh_tw": "開放原始碼"
    },
    "NM_HideActionBarStatus": {
        "id": "Sembunyikan status di header", "ja": "ヘッダーのステータスを非表示", "ko": "상단 바 상태 숨기기",
        "zh_cn": "隐藏顶栏状态文本", "zh_tw": "隱藏頂欄狀態文字"
    },
    "NM_HideBubbleTail": {
        "id": "Sembunyikan ekor pesan", "ja": "メッセージのしっぽを非表示", "ko": "메시지 꼬리 숨기기",
        "zh_cn": "隐藏气泡尖角小尾巴", "zh_tw": "隱藏氣泡尖角小尾巴"
    },
    "NM_HideBubbleTail_Desc": {
        "id": "Gunakan sudut membulat tanpa ekor pesan.",
        "ja": "しっぽのない角丸フキダシを使用します。",
        "ko": "메시지 꼬리 없이 둥근 모서리를 사용합니다.",
        "zh_cn": "使用没有尖角小尾巴的圆角气泡。",
        "zh_tw": "使用沒有尖角小尾巴的圓角氣泡。"
    },
    "NM_IconPack_DefaultSubtitle": {
        "id": "Ikon standar Telegram", "ja": "標準のTelegramアイコン", "ko": "기본 Telegram 아이콘",
        "zh_cn": "Telegram 默认图标", "zh_tw": "Telegram 預設圖示"
    },
    "NM_IconPack_DefaultTitle": {
        "id": "Default", "ja": "デフォルト", "ko": "기본",
        "zh_cn": "默认", "zh_tw": "預設"
    },
    "NM_IconPack_LiquidSubtitle": {
        "id": "Paket ikon Liquid Glass", "ja": "Liquid Glass アイコンパック", "ko": "Liquid Glass 아이콘 팩",
        "zh_cn": "Liquid Glass 流体玻璃图标包", "zh_tw": "Liquid Glass 流體玻璃圖示包"
    },
    "NM_IconPack_LiquidTitle": {
        "id": "Liquid Glass", "ja": "Liquid Glass", "ko": "Liquid Glass",
        "zh_cn": "Liquid Glass", "zh_tw": "Liquid Glass"
    },
    "NM_IconPack_PlumpySubtitle": {
        "id": "Paket ikon Plumpy", "ja": "Plumpy アイコンパック", "ko": "Plumpy 아이콘 팩",
        "zh_cn": "Plumpy 饱满立体图标包", "zh_tw": "Plumpy 飽滿立體圖示包"
    },
    "NM_IconPack_PlumpyTitle": {
        "id": "Plumpy", "ja": "Plumpy", "ko": "Plumpy",
        "zh_cn": "Plumpy", "zh_tw": "Plumpy"
    },
    "NM_IconPack_SolarSubtitle": {
        "id": "Paket ikon Solar bawaan", "ja": "組み込みのSolarアイコンパック", "ko": "기본 내장 Solar 아이콘 팩",
        "zh_cn": "内置 Solar 太阳光芒图标包", "zh_tw": "內建 Solar 太陽光芒圖示包"
    },
    "NM_IconPack_SolarTitle": {
        "id": "Solar", "ja": "Solar", "ko": "Solar",
        "zh_cn": "Solar", "zh_tw": "Solar"
    },
    "NM_JumpToBeginning": {
        "id": "Ke awal", "ja": "最初へ", "ko": "처음으로",
        "zh_cn": "跳转至第一条消息", "zh_tw": "跳轉至第一則訊息"
    },
    "NM_MI_ClearFromCache": {
        "id": "Hapus dari cache", "ja": "キャッシュから削除", "ko": "캐시에서 지우기",
        "zh_cn": "从缓存中清除", "zh_tw": "從快取中清除"
    },
    "NM_MI_CopyPhoto": {
        "id": "Salin foto", "ja": "写真をコピー", "ko": "사진 복사",
        "zh_cn": "复制图片", "zh_tw": "複製圖片"
    },
    "NM_MI_CopyPhotoAsSticker": {
        "id": "Salin foto sebagai stiker", "ja": "写真をステッカーとしてコピー", "ko": "사진을 스티커로 복사",
        "zh_cn": "复制图片为贴纸", "zh_tw": "複製圖片為貼圖"
    },
    "NM_MI_Details": {
        "id": "Detail", "ja": "詳細", "ko": "세부정보",
        "zh_cn": "详细信息", "zh_tw": "詳細資訊"
    },
    "NM_MI_DownloadSticker": {
        "id": "Unduh stiker", "ja": "ステッカーをダウンロード", "ko": "스티커 다운로드",
        "zh_cn": "下载贴纸文件", "zh_tw": "下載貼圖檔案"
    },
    "NM_MI_ForwardWoAuthorship": {
        "id": "Teruskan tanpa pengarang", "ja": "作成者を非表示で転送", "ko": "작성자 없이 전달",
        "zh_cn": "无来源转发", "zh_tw": "無來源轉傳"
    },
    "NM_MI_ForwardWoCaption": {
        "id": "Teruskan tanpa keterangan", "ja": "キャプションなしで転送", "ko": "캡션 없이 전달",
        "zh_cn": "无附言转发", "zh_tw": "無附言轉傳"
    },
    "NM_MI_SaveToSaved": {
        "id": "Simpan ke Pesan Tersimpan", "ja": "保存されたメッセージに保存", "ko": "저장한 메시지에 저장",
        "zh_cn": "转存至收藏夹", "zh_tw": "轉存至收藏夾"
    },
    "NM_MM_MessageMenuItems": {
        "id": "Item menu pesan", "ja": "メッセージメニュー項目", "ko": "메시지 메뉴 항목",
        "zh_cn": "消息菜单条目", "zh_tw": "訊息選單條目"
    },
    "NM_MP_CustomizeMessage": {
        "id": "Sesuaikan pesan", "ja": "メッセージをカスタマイズ", "ko": "메시지 사용자 지정",
        "zh_cn": "自定义气泡与消息", "zh_tw": "自訂氣泡與訊息"
    },
    "NM_MP_CustomizeProfile": {
        "id": "Sesuaikan profil", "ja": "プロフィールをカスタマイズ", "ko": "프로필 사용자 지정",
        "zh_cn": "自定义个人资料卡片", "zh_tw": "自訂個人檔案卡片"
    },
    "NM_MP_DisablePremiumStatuses": {
        "id": "Nonaktifkan status premium", "ja": "プレミアムステータスを無効化", "ko": "프리미엄 상태 비활성화",
        "zh_cn": "屏蔽高级用户 Emoji 头衔", "zh_tw": "屏蔽高級使用者 Emoji 頭銜"
    },
    "NM_MP_DisablePremiumStatuses_Desc": {
        "id": "Sembunyikan status emoji Premium di luar menu samping",
        "ja": "サイドメニュー外のプレミアム絵文字ステータスを非表示にします",
        "ko": "서랍 메뉴 외부에서 프리미엄 이모지 상태를 숨깁니다",
        "zh_cn": "在侧边栏之外隐藏好友和群成员的高级 Emoji 状态",
        "zh_tw": "在側邊欄之外隱藏好友和群成員的高級 Emoji 狀態"
    },
    "NM_MP_ProfileBackgroundColor": {
        "id": "Latar belakang di profil", "ja": "プロフィールの背景色", "ko": "프로필 배경색",
        "zh_cn": "个人资料背景颜色", "zh_tw": "個人檔案背景顏色"
    },
    "NM_MP_ProfileBackgroundEmoji": {
        "id": "Emoji di latar belakang profil", "ja": "プロフィールの背景絵文字", "ko": "프로필 배경 이모지",
        "zh_cn": "个人资料背景图案 Emoji", "zh_tw": "個人檔案背景圖案 Emoji"
    },
    "NM_MP_ProfileBirthDatePreview": {
        "id": "Tanggal Lahir", "ja": "生年月日", "ko": "생년월일",
        "zh_cn": "出生日期", "zh_tw": "出生日期"
    },
    "NM_MP_ProfileBusinessPreview": {
        "id": "Jam kerja dan lokasi", "ja": "営業時間と場所", "ko": "영업 시간 및 위치",
        "zh_cn": "营业时间与地址", "zh_tw": "營業時間與地址"
    },
    "NM_MP_ProfileChannelPreview": {
        "id": "Saluran pribadi", "ja": "個人のチャンネル", "ko": "개인 채널",
        "zh_cn": "个人专属频道", "zh_tw": "個人專屬頻道"
    },
    "NM_MP_ReplyBackground": {
        "id": "Latar belakang dalam balasan", "ja": "返信の背景", "ko": "답장 배경",
        "zh_cn": "引用回复背景颜色", "zh_tw": "引用回覆背景顏色"
    },
    "NM_MP_ReplyBackgroundEmoji": {
        "id": "Emoji dalam balasan dan pratinjau", "ja": "返信とプレビューの絵文字", "ko": "답장 및 미리보기 이모지",
        "zh_cn": "引用与链接预览 Emoji", "zh_tw": "引用與連結預覽 Emoji"
    },
    "NM_MP_ReplyCustomColors": {
        "id": "Warna khusus dalam balasan", "ja": "返信のカスタムカラー", "ko": "답장 사용자 지정 색상",
        "zh_cn": "引用与链接预览自定义色彩", "zh_tw": "引用與連結預覽自訂色彩"
    },
    "NM_MP_ShowIdDc": {
        "id": "Tampilkan ID dan DC di profil", "ja": "プロフィールにIDとDCを表示", "ko": "프로필에 ID 및 DC 표시",
        "zh_cn": "在个人资料中显示 ID 与数据中心 (DC)", "zh_tw": "在個人檔案中顯示 ID 與資料中心 (DC)"
    },
    "NM_MP_ShowSeconds": {
        "id": "Format waktu dengan detik", "ja": "秒単位で時間を表示", "ko": "초 단위 시간 표시",
        "zh_cn": "消息时间精确到秒", "zh_tw": "訊息時間精確到秒"
    },
    "NM_MP_ShowSeconds_Desc": {
        "id": "12:34 -> 12:34:56", "ja": "12:34 -> 12:34:56", "ko": "12:34 -> 12:34:56",
        "zh_cn": "12:34 -> 12:34:56", "zh_tw": "12:34 -> 12:34:56"
    },
    "NM_MSG_GifSpoilers": {
        "id": "Kirim GIF dengan spoiler", "ja": "スポイラー付きでGIFを送信", "ko": "스포일러로 GIF 보내기",
        "zh_cn": "带剧透保护发送 GIF", "zh_tw": "帶劇透保護傳送 GIF"
    },
    "NM_MSG_Header_Sending": {
        "id": "Pengiriman", "ja": "送信", "ko": "보내기",
        "zh_cn": "消息发送设置", "zh_tw": "訊息傳送設定"
    },
    "NM_MZ_LargerVoiceMessages": {
        "id": "Panjang pesan suara adaptif", "ja": "音声メッセージの長さを適応", "ko": "음성 메시지 길이 적응형 조절",
        "zh_cn": "自适应语音气泡长度", "zh_tw": "自適應語音氣泡長度"
    },
    "NM_MZ_MediaAmplifier": {
        "id": "Foto dan video", "ja": "写真と動画", "ko": "사진 및 비디오",
        "zh_cn": "照片与视频展示", "zh_tw": "照片與視訊展示"
    },
    "NM_MZ_MediaAmplifier_Hint": {
        "id": "Catatan: media dapat diperkecil agar sesuai dengan lebar obrolan.",
        "ja": "注意: メディアはチャットの幅に合わせて縮小される場合があります。",
        "ko": "참고: 미디어는 대화창 너비에 맞게 축소될 수 있습니다.",
        "zh_cn": "提示：照片和视频可能会根据聊天宽度等比例缩放。",
        "zh_tw": "提示：照片和視訊可能會根據聊天寬度等比例縮放。"
    },
    "NM_MediaGlow": {
        "id": "Cahaya media", "ja": "メディアのグロー効果", "ko": "미디어 글로우 효과",
        "zh_cn": "图片与视频流光倒影", "zh_tw": "圖片與視訊流光倒影"
    },
    "NM_MediaGlow_Desc": {
        "id": "Isi tepi media dengan latar belakang buram yang lembut.",
        "ja": "メディアの端を柔らかいぼかし背景で満たします。",
        "ko": "미디어 가장자리를 부드러운 흐림 배경으로 채웁니다.",
        "zh_cn": "在图片和视频周围呈现柔和的流光色彩光晕。",
        "zh_tw": "在圖片和視訊周圍呈現柔和的流光色彩光暈。"
    },
    "NM_Menu_Haptic": {
        "id": "Umpan balik haptic", "ja": "触覚フィードバック", "ko": "햅틱 피드백",
        "zh_cn": "菜单震动触感反馈", "zh_tw": "選單震動觸感回饋"
    },
    "NM_Menu_Haptic_Desc": {
        "id": "Bergetar saat menu terbuka atau ditutup dengan menggeser.",
        "ja": "メニューが開いた時やスワイプで閉じた時に振動します。",
        "ko": "메시지 메뉴가 열리거나 스와이프로 닫힐 때 진동합니다.",
        "zh_cn": "打开或轻扫关闭消息菜单时产生触感震动反馈。",
        "zh_tw": "開啟或輕掃關閉訊息選單時產生觸感震動回饋。"
    },
    "NM_Menu_Reorder": {
        "id": "Susun ulang item menu", "ja": "メニュー項目の並べ替え", "ko": "메뉴 항목 순서 변경",
        "zh_cn": "自定义菜单项排序", "zh_tw": "自訂選單項目排序"
    },
    "NM_Menu_Reorder_Reset": {
        "id": "Atur ulang ke urutan default", "ja": "デフォルトの順序にリセット", "ko": "기본 순서로 재설정",
        "zh_cn": "恢复默认排序", "zh_tw": "恢復預設排序"
    },
    "NM_Menu_TelegramPlus": {
        "id": "Menu pesan modern", "ja": "モダンなメッセージメニュー", "ko": "현대적인 메시지 메뉴",
        "zh_cn": "现代流体消息菜单", "zh_tw": "現代流體訊息選單"
    },
    "NM_Menu_TelegramPlus_Desc": {
        "id": "Menu Liquid Glass dengan aksi yang dikelompokkan.",
        "ja": "グループ化されたアクションと滑らかな応答を備えたLiquid Glassメニュー。",
        "ko": "그룹화된 작업과 부드러운 반응을 갖춘 Liquid Glass 메뉴입니다.",
        "zh_cn": "具有弹性阻尼和分组操作的 Liquid Glass 菜单。",
        "zh_tw": "具有彈性阻尼和分組操作的 Liquid Glass 選單。"
    },
    "NM_MsgSlideAction": {
        "id": "Tindakan saat menggeser pesan", "ja": "メッセージスワイプ時のアクション", "ko": "메시지 스와이프 동작",
        "zh_cn": "消息滑动快捷操作", "zh_tw": "訊息滑動快捷操作"
    },
    "NM_NM_AutoDownload": {
        "id": "Unduh otomatis saat kirim URL", "ja": "URL送信時に自動ダウンロード", "ko": "URL 전송 시 자동 다운로드",
        "zh_cn": "发送链接自动提取下载媒体", "zh_tw": "傳送連結自動擷取下載媒體"
    },
    "NM_NM_AutoDownload_Desc": {
        "id": "Mengirim pesan yang berisi tautan yang didukung memicu unduhan otomatis.",
        "ja": "サポートされているURLのみを送信すると自動的にダウンロードが開始されます。",
        "ko": "지원되는 URL만 포함된 메시지를 보내면 자동으로 다운로드가 시작됩니다.",
        "zh_cn": "发送仅包含受支持音视频链接的消息时自动触发下载解析。",
        "zh_tw": "傳送僅包含受支援音訊視訊連結的訊息時自動觸發下載解析。"
    },
    "NM_NM_FormatAudio": {
        "id": "Audio", "ja": "音声", "ko": "오디오",
        "zh_cn": "音频", "zh_tw": "音訊"
    },
    "NM_NM_FormatVideo": {
        "id": "Video", "ja": "動画", "ko": "비디오",
        "zh_cn": "视频", "zh_tw": "視訊"
    },
    "NM_NM_YtAsk": {
        "id": "Tanyakan format YouTube", "ja": "YouTube形式を確認", "ko": "YouTube 형식 묻기",
        "zh_cn": "解析 YouTube 时询问音视频格式", "zh_tw": "解析 YouTube 時詢問音訊視訊格式"
    },
    "NM_NotificationReactionEmoji": {
        "id": "Emoji", "ja": "絵文字", "ko": "이모지",
        "zh_cn": "Emoji 表情", "zh_tw": "Emoji 表情"
    },
    "NM_NotificationReactions": {
        "id": "Reaksi dalam notifikasi", "ja": "通知内リアクション", "ko": "알림에서 반응하기",
        "zh_cn": "系统通知栏快捷表情反应", "zh_tw": "系統通知列快捷表情反應"
    },
    "NM_NotificationReactions_Desc": {
        "id": "Menambahkan tombol reaksi cepat ke notifikasi pesan.",
        "ja": "メッセージ通知にクイックリアクションボタンを追加します。",
        "ko": "메시지 알림에 빠른 반응 버튼을 추가합니다.",
        "zh_cn": "在系统消息通知栏中添加一键快捷反应按钮。",
        "zh_tw": "在系統訊息通知列中新增一鍵快捷反應按鈕。"
    },
    "NM_OnlineIndicatorInGroups": {
        "id": "Indikator online di grup", "ja": "グループ内のオンライン表示", "ko": "그룹 내 온라인 표시기",
        "zh_cn": "群组成员头像在线小绿点", "zh_tw": "群組成員頭像線上小綠點"
    },
    "NM_OnlineIndicatorInGroups_Desc": {
        "id": "Tampilkan titik online pada avatar grup.",
        "ja": "グループのアバターにオンラインの緑色の点を表示します。",
        "ko": "그룹 아바타에 온라인 상태를 나타내는 점을 표시합니다.",
        "zh_cn": "在群聊成员的头像右下角显示实时在线状态绿点。",
        "zh_tw": "在群聊成員的頭像右下角顯示即時線上狀態綠點。"
    },
    "NM_PR_AllowSystemPasscode": {
        "id": "Izinkan kunci layar perangkat", "ja": "端末の画面ロックを許可", "ko": "기기 화면 잠금 허용",
        "zh_cn": "允许使用系统锁屏密码", "zh_tw": "允許使用系統螢幕鎖定密碼"
    },
    "NM_PR_AllowSystemPasscode_Desc": {
        "id": "Izinkan PIN, kata sandi, atau pola perangkat sebagai pengganti biometrik.",
        "ja": "生体認証の代わりに端末のPIN、パスワード、パターンを許可します。",
        "ko": "생체 인증 대신 기기 PIN, 비밀번호 또는 패턴을 허용합니다.",
        "zh_cn": "在生物识别不可用时允许使用系统 PIN 码或图形锁。",
        "zh_tw": "在生物辨識不可用時允許使用系統 PIN 碼或圖形鎖。"
    },
    "NM_PR_AskBioOpenArchive": {
        "id": "Lindungi arsip", "ja": "アーカイブを保護", "ko": "보관함 보호",
        "zh_cn": "加密归档聊天列表", "zh_tw": "加密封存聊天列表"
    },
    "NM_PR_AskBioOpenArchive_Desc": {
        "id": "Wajibkan otentikasi perangkat sebelum membuka arsip.",
        "ja": "アーカイブを開く前に端末認証を要求します。",
        "ko": "보관함을 열기 전에 기기 인증을 요구합니다.",
        "zh_cn": "打开已归档聊天列表前必须验证身份。",
        "zh_tw": "開啟已封存聊天列表前必須驗證身分。"
    },
    "NM_PR_AskBioOpenChats": {
        "id": "Lindungi obrolan terpilih", "ja": "選択したチャットを保護", "ko": "선택한 채팅 보호",
        "zh_cn": "加密指定私密聊天", "zh_tw": "加密指定私密聊天"
    },
    "NM_PR_AskBioOpenChats_Desc": {
        "id": "Wajibkan otentikasi sebelum membuka obrolan yang dipilih di bawah.",
        "ja": "以下で選択したチャットを開く前に認証を要求します。",
        "ko": "아래에서 선택한 대화를 열기 전에 기기 인증을 요구합니다.",
        "zh_cn": "进入下方指定的保护聊天前必须验证身份。",
        "zh_tw": "進入下方指定的保護聊天前必須驗證身分。"
    },
    "NM_PR_AskBioOpenEncrypted": {
        "id": "Lindungi obrolan rahasia", "ja": "秘密チャットを保護", "ko": "비밀 대화 보호",
        "zh_cn": "加密端到端私密聊天", "zh_tw": "加密端對端私密聊天"
    },
    "NM_PR_AskBioOpenEncrypted_Desc": {
        "id": "Wajibkan otentikasi perangkat sebelum membuka obrolan rahasia.",
        "ja": "秘密チャットを開く前に端末認証を要求します。",
        "ko": "비밀 대화를 열기 전에 기기 인증을 요구합니다.",
        "zh_cn": "打开秘密聊天前必须验证身份。",
        "zh_tw": "開啟秘密聊天前必須驗證身分。"
    },
    "NM_PR_AskBioOpenSavedMessages": {
        "id": "Lindungi Pesan Tersimpan", "ja": "保存されたメッセージを保護", "ko": "저장한 메시지 보호",
        "zh_cn": "加密我的收藏夹", "zh_tw": "加密我的收藏夾"
    },
    "NM_PR_AskBioOpenSavedMessages_Desc": {
        "id": "Wajibkan biometrik atau otentikasi sebelum membuka Pesan Tersimpan.",
        "ja": "「保存されたメッセージ」を開く前に生体認証または端末認証を要求します。",
        "ko": "저장한 메시지를 열기 전에 생체 인증 또는 기기 인증을 요구합니다.",
        "zh_cn": "打开“收藏夹”前必须验证指纹或锁屏密码。",
        "zh_tw": "開啟「收藏夾」前必須驗證指紋或螢幕鎖定密碼。"
    },
    "NM_PR_AuthenticationRequired": {
        "id": "Otentikasi diperlukan untuk mengubah pengaturan ini", "ja": "この設定を変更するには認証が必要です", "ko": "이 설정을 변경하려면 인증이 필요합니다",
        "zh_cn": "修改此受保护的设置需要验证身份", "zh_tw": "修改此受保護的設定需要驗證身分"
    },
    "NM_PR_AuthenticationUnavailable": {
        "id": "Otentikasi tidak tersedia", "ja": "認証が利用できません", "ko": "인증을 사용할 수 없음",
        "zh_cn": "身份验证不可用", "zh_tw": "身分驗證不可用"
    },
    "NM_PR_AuthenticationUnavailable_Desc": {
        "id": "Tidak ada kunci layar. Nonaktifkan pengaturan perlindungan ini?",
        "ja": "利用可能な画面ロックがありません。この保護設定を無効にしますか？",
        "ko": "기기 화면 잠금이 설정되지 않았습니다. 이 보호 설정을 비활성화하시겠습니까?",
        "zh_cn": "设备未设置生物识别或屏幕锁。是否停用此保护设置？",
        "zh_tw": "裝置未設定生物辨識或螢幕鎖。是否停用此保護設定？"
    },
    "NM_PR_DeleteAccount": {
        "id": "Hapus akun Telegram", "ja": "Telegramアカウントを削除", "ko": "Telegram 계정 탈퇴",
        "zh_cn": "彻底注销 Telegram 账号", "zh_tw": "徹底註銷 Telegram 帳號"
    },
    "NM_PR_Header_ChatProtection": {
        "id": "Perlindungan obrolan", "ja": "チャットの保護", "ko": "채팅 보호",
        "zh_cn": "聊天安全锁", "zh_tw": "聊天安全鎖"
    },
    "NM_PR_HideArchiveList": {
        "id": "Sembunyikan arsip dari daftar obrolan", "ja": "チャット一覧からアーカイブを非表示", "ko": "대화 목록에서 보관함 숨기기",
        "zh_cn": "从聊天列表中隐藏归档文件夹", "zh_tw": "從聊天列表中隱藏封存資料夾"
    },
    "NM_PR_HideArchiveList_Desc": {
        "id": "Hapus Arsip dari daftar. Buka di sini atau tahan Cari.",
        "ja": "チャット一覧からアーカイブを削除します。ここから開くか検索を長押ししてください。",
        "ko": "대화 목록에서 보관함을 제거합니다. 여기서 열거나 검색을 길게 누르세요.",
        "zh_cn": "从主列表中隐藏。可在此处打开或长按搜索按钮呼出。",
        "zh_tw": "從主列表中隱藏。可在此處開啟或長按搜尋按鈕呼出。"
    },
    "NM_PR_HideArchivedStories": {
        "id": "Sembunyikan cerita yang diarsipkan", "ja": "アーカイブされたストーリーを非表示", "ko": "보관된 스토리 숨기기",
        "zh_cn": "隐藏归档的动态", "zh_tw": "隱藏封存的限時動態"
    },
    "NM_PR_HideArchivedStories_Desc": {
        "id": "Menyembunyikan bagian cerita yang diarsipkan dari profil Anda.",
        "ja": "プロフィールからアーカイブされたストーリーのセクションを非表示にします。",
        "ko": "프로필에서 보관된 스토리 섹션을 숨깁니다.",
        "zh_cn": "从个人资料页面中隐藏已归档的动态板块。",
        "zh_tw": "從個人檔案頁面中隱藏已封存的限時動態區塊。"
    },
    "NM_PR_HideProxy": {
        "id": "Jangan tampilkan sponsor proxy", "ja": "プロキシのスポンサーを表示しない", "ko": "프록시 스폰서 채널 숨기기",
        "zh_cn": "隐藏赞助商代理频道", "zh_tw": "隱藏贊助商代理頻道"
    },
    "NM_PR_LockedChats": {
        "id": "Obrolan yang dilindungi", "ja": "保護されたチャット", "ko": "보호된 대화",
        "zh_cn": "受密码保护的聊天", "zh_tw": "受密碼保護的聊天"
    },
    "NM_PR_LockedChatsTtl": {
        "id": "Tanyakan lagi", "ja": "再確認の間隔", "ko": "재확인 주기",
        "zh_cn": "锁屏保护超时自动锁定", "zh_tw": "鎖定保護逾時自動鎖定"
    },
    "NM_PR_LockedChatsTtl_15Min": {
        "id": "Setelah 15 menit", "ja": "15分後", "ko": "15분 후",
        "zh_cn": "15 分钟后", "zh_tw": "15 分鐘後"
    },
    "NM_PR_LockedChatsTtl_1Min": {
        "id": "Setelah 1 menit", "ja": "1分後", "ko": "1분 후",
        "zh_cn": "1 分钟后", "zh_tw": "1 分鐘後"
    },
    "NM_PR_LockedChatsTtl_5Min": {
        "id": "Setelah 5 menit", "ja": "5分後", "ko": "5분 후",
        "zh_cn": "5 分钟后", "zh_tw": "5 分鐘後"
    },
    "NM_PR_LockedChatsTtl_Always": {
        "id": "Selalu tanyakan", "ja": "毎回確認", "ko": "항상 묻기",
        "zh_cn": "每次立即锁定", "zh_tw": "每次立即鎖定"
    },
    "NM_PR_LockedChatsTtl_UntilRestart": {
        "id": "Sampai restart", "ja": "再起動まで", "ko": "다시 시작할 때까지",
        "zh_cn": "直到应用重新启动", "zh_tw": "直到應用程式重新啟動"
    },
    "NM_PR_OpenArchive": {
        "id": "Buka arsip", "ja": "アーカイブを開く", "ko": "보관함 열기",
        "zh_cn": "打开归档聊天", "zh_tw": "開啟封存聊天"
    },
    "NM_PR_RequireBiometricsToDelete": {
        "id": "Konfirmasi penghapusan", "ja": "削除時の認証", "ko": "삭제 시 인증 요구",
        "zh_cn": "删除会话或消息时需身份验证", "zh_tw": "刪除對話或訊息時需身分驗證"
    },
    "NM_PR_RequireBiometricsToDelete_Desc": {
        "id": "Wajibkan otentikasi sebelum menghapus pesan atau obrolan.",
        "ja": "メッセージやチャットを削除する前に端末認証を要求します。",
        "ko": "메시지나 대화를 삭제하기 전에 기기 인증을 요구합니다.",
        "zh_cn": "在单向或双向清空聊天记录前强制进行生物识别验证。",
        "zh_tw": "在單向或雙向清空聊天記錄前強制進行生物辨識驗證。"
    },
    "NM_PR_TestFingerprint": {
        "id": "Uji keamanan", "ja": "認証をテスト", "ko": "인증 테스트",
        "zh_cn": "测试指纹/面容身份验证", "zh_tw": "測試指紋/面容身分驗證"
    },
    "NM_PR_TestFingerprint_Desc": {
        "id": "Periksa apakah otentikasi biometrik atau perangkat tersedia.",
        "ja": "生体認証または端末ロックが利用可能かテストします。",
        "ko": "생체 인증 또는 기기 잠금 사용 가능 여부를 확인하고 상태를 새로고침합니다.",
        "zh_cn": "检查当前设备的生物识别或锁屏密码状态是否正常。",
        "zh_tw": "檢查當前裝置的生物辨識或螢幕鎖定密碼狀態是否正常。"
    },
    "NM_PredictiveBackAnimation": {
        "id": "Gestur kembali prediktif", "ja": "予測バックジェスチャー", "ko": "예측 뒤로 가기 제스처",
        "zh_cn": "预测性返回手势动画", "zh_tw": "預測性返回手勢動畫"
    },
    "NM_PremiumReactionRequired": {
        "id": "Telegram Premium diperlukan", "ja": "Telegram Premiumが必要です", "ko": "Telegram Premium 필요",
        "zh_cn": "需要开通 Telegram Premium", "zh_tw": "需要開通 Telegram Premium"
    },
    "NM_ResidentNotification": {
        "id": "Notifikasi persisten", "ja": "常駐通知", "ko": "상주 알림",
        "zh_cn": "后台常驻保活通知", "zh_tw": "後台常駐保活通知"
    },
    "NM_Restart": {
        "id": "Mulai ulang", "ja": "再起動", "ko": "다시 시작",
        "zh_cn": "重启客户端", "zh_tw": "重新啟動用戶端"
    },
    "NM_RestartRequired": {
        "id": "Perlu mulai ulang untuk menerapkan perubahan", "ja": "変更を適用するには再起動が必要です", "ko": "변경 사항을 적용하려면 앱을 다시 시작해야 합니다",
        "zh_cn": "需重启应用以应用生效", "zh_tw": "需重新啟動應用程式以套用生效"
    },
    "NM_SettingsDesc_NavigationCrossfade": {
        "id": "Animasi crossfade saat beralih layar", "ja": "画面切り替え時のクロスフェードアニメーション", "ko": "화면 전환 시 크로스페이드 애니메이션",
        "zh_cn": "切换设置页面时使用平滑淡入淡出动画", "zh_tw": "切換設定頁面時使用平滑淡入淡出動畫"
    },
    "NM_StickerSize": {
        "id": "Ukuran stiker", "ja": "ステッカーのサイズ", "ko": "스티커 크기",
        "zh_cn": "贴纸显示大小", "zh_tw": "貼圖顯示大小"
    },
    "NM_StickerSizeBig": {
        "id": "Besar", "ja": "大", "ko": "크게",
        "zh_cn": "大", "zh_tw": "大"
    },
    "NM_StickerSizePreviewReply": {
        "id": "ke bulan 🚀", "ja": "月へ行こう 🚀", "ko": "달나라로 🚀",
        "zh_cn": "飞向月球 🚀", "zh_tw": "飛向月球 🚀"
    },
    "NM_StickerSizePreviewText": {
        "id": "hahaha 😂", "ja": "あはは 😂", "ko": "아하하 😂",
        "zh_cn": "哈哈哈哈 😂", "zh_tw": "哈哈哈哈 😂"
    },
    "NM_StickerSizeSmall": {
        "id": "Kecil", "ja": "小", "ko": "작게",
        "zh_cn": "小", "zh_tw": "小"
    },
    "NM_Stickers_Header": {
        "id": "Stiker", "ja": "ステッカー", "ko": "스티커",
        "zh_cn": "贴纸", "zh_tw": "貼圖"
    },
    "NM_SwitchStyle": {
        "id": "Gaya saklar", "ja": "スイッチのスタイル", "ko": "스위치 스타일",
        "zh_cn": "开关视觉风格", "zh_tw": "開關視覺風格"
    },
    "NM_ToSaved": {
        "id": "Simpan", "ja": "保存", "ko": "저장",
        "zh_cn": "保存", "zh_tw": "儲存"
    },
    "NM_Vibrate_Click": {
        "id": "Klik", "ja": "クリック", "ko": "클릭",
        "zh_cn": "轻击", "zh_tw": "輕按"
    },
    "NM_Vibrate_Keyboard": {
        "id": "Ketukan keyboard", "ja": "キーボードタップ", "ko": "키보드 탭",
        "zh_cn": "键盘击键触感", "zh_tw": "鍵盤按鍵觸感"
    },
    "NM_Vibrate_Long": {
        "id": "Tekan lama", "ja": "長押し", "ko": "길게 누르기",
        "zh_cn": "长按振动", "zh_tw": "長按震動"
    },
    "NM_Vibrate_Off": {
        "id": "Mati", "ja": "オフ", "ko": "꺼짐",
        "zh_cn": "关闭", "zh_tw": "關閉"
    },
    "NM_Vibrate_Wave": {
        "id": "Gelombang", "ja": "波", "ko": "웨이브",
        "zh_cn": "波浪震感", "zh_tw": "波浪震感"
    },
    "NM_WSB_Enable": {
        "id": "Aktifkan bypass", "ja": "バイパスを有効化", "ko": "우회 활성화",
        "zh_cn": "启用内置免流代理穿透", "zh_tw": "啟用內建免流代理穿透"
    },
    "NM_WSB_MiscHeader": {
        "id": "Panggilan dan VPN", "ja": "通話とVPN", "ko": "통화 및 VPN",
        "zh_cn": "网络通话与 VPN 路由", "zh_tw": "網路通話與 VPN 路由"
    },
    "NM_WSB_OpenProxySettings": {
        "id": "Buka pengaturan proxy Telegram", "ja": "Telegramのプロキシ設定を開く", "ko": "Telegram 프록시 설정 열기",
        "zh_cn": "打开 Telegram 原生代理设置", "zh_tw": "開啟 Telegram 原生代理設定"
    },
    "NM_WSB_Status_Off": {
        "id": "Mati", "ja": "オフ", "ko": "꺼짐",
        "zh_cn": "已停用", "zh_tw": "已停用"
    },
    "NM_WSB_SuspendOnVpn": {
        "id": "Tangguhkan saat VPN aktif", "ja": "VPN接続時は一時停止", "ko": "VPN 사용 시 일시 중지",
        "zh_cn": "检测到系统 VPN 时暂停绕过", "zh_tw": "偵測到系統 VPN 時暫停繞過"
    },
    "NM_WSB_SuspendOnVpn_Desc": {
        "id": "Jeda bypass saat VPN sistem aktif.",
        "ja": "システムのVPNがアクティブな間はバイパスを一時停止します。",
        "ko": "기기 시스템 VPN이 활성화되어 있을 때 우회를 일시 중지합니다.",
        "zh_cn": "当系统全局 VPN 开启时自动暂停应用内穿透代理。",
        "zh_tw": "當系統全域 VPN 開啟時自動暫停應用程式內穿透代理。"
    },
    "NM_WSB_VoIP_Enable": {
        "id": "Rute panggilan melalui bypass", "ja": "通話をバイパス経由にする", "ko": "통화를 우회로 라우팅",
        "zh_cn": "语音通话走代理通道", "zh_tw": "語音通話走代理通道"
    },
    "NM_Without_Authorship": {
        "id": "tanpa pengarang", "ja": "作成者を非表示", "ko": "작성자 없이",
        "zh_cn": "无来源", "zh_tw": "無來源"
    },
    "Notifications": {
        "id": "Notifikasi", "ja": "通知", "ko": "알림",
        "zh_cn": "通知", "zh_tw": "通知"
    },
    "NotificationsService": {
        "id": "Layanan Penjaga Koneksi", "ja": "バックグラウンド接続サービス", "ko": "연결 유지 백그라운드 서비스",
        "zh_cn": "后台长连接保活服务", "zh_tw": "後台長連線保活服務"
    },
    "NotificationsSound": {
        "id": "Suara Notifikasi", "ja": "通知音", "ko": "알림음",
        "zh_cn": "通知提示音", "zh_tw": "通知提示音"
    },
    "NumberUnknown": {
        "id": "Tidak dikenal", "ja": "不明", "ko": "알 수 없음",
        "zh_cn": "未知号码", "zh_tw": "未知號碼"
    },
    "OK": {
        "id": "OK", "ja": "OK", "ko": "확인",
        "zh_cn": "确定", "zh_tw": "確定"
    },
    "Online": {
        "id": "online", "ja": "オンライン", "ko": "온라인",
        "zh_cn": "在线", "zh_tw": "線上"
    },
    "PasscodePanicCodeRemove": {
        "id": "Matikan kode panik", "ja": "パニックコードを解除", "ko": "패닉 코드 해제",
        "zh_cn": "解除应急自毁锁屏密码", "zh_tw": "解除應急自毀鎖定密碼"
    },
    "PinMessage": {
        "id": "Sematkan", "ja": "ピン留め", "ko": "고정",
        "zh_cn": "置顶消息", "zh_tw": "置頂訊息"
    },
    "Plugins": {
        "id": "Plugin", "ja": "プラグイン", "ko": "플러그인",
        "zh_cn": "扩展插件", "zh_tw": "擴充外掛"
    },
    "PluginsEngine": {
        "id": "Mesin plugin", "ja": "プラグインエンジン", "ko": "플러그인 엔진",
        "zh_cn": "插件核心引擎", "zh_tw": "外掛核心引擎"
    },
    "PopupDisabled": {
        "id": "Nonaktif", "ja": "無効", "ko": "꺼짐",
        "zh_cn": "已停用", "zh_tw": "已停用"
    },
    "ProfileBirthday": {
        "id": "Tanggal Lahir", "ja": "生年月日", "ko": "생일",
        "zh_cn": "出生日期", "zh_tw": "出生日期"
    },
    "Quality1440Short": {
        "id": "2K", "ja": "2K", "ko": "2K",
        "zh_cn": "2K", "zh_tw": "2K"
    },
    "QualityAuto": {
        "id": "Otomatis", "ja": "自動", "ko": "자동",
        "zh_cn": "自动", "zh_tw": "自動"
    },
    "Reactions": {
        "id": "Reaksi", "ja": "リアクション", "ko": "반응",
        "zh_cn": "表情反应", "zh_tw": "表情反應"
    },
    "Reply": {
        "id": "Balas", "ja": "返信", "ko": "답장",
        "zh_cn": "回复", "zh_tw": "回覆"
    },
    "ReportChat": {
        "id": "Laporkan", "ja": "報告", "ko": "신고",
        "zh_cn": "举报", "zh_tw": "檢舉"
    },
    "RepostToStory": {
        "id": "Posting Ulang ke Cerita", "ja": "ストーリーに再投稿", "ko": "스토리에 다시 게시",
        "zh_cn": "转发至动态", "zh_tw": "轉發至限時動態"
    },
    "Reset": {
        "id": "Atur Ulang", "ja": "リセット", "ko": "초기화",
        "zh_cn": "重置", "zh_tw": "重設"
    },
    "Save": {
        "id": "Simpan", "ja": "保存", "ko": "저장",
        "zh_cn": "保存", "zh_tw": "儲存"
    },
    "SaveDeletedMessageForBot": {
        "id": "Simpan di Obrolan Bot", "ja": "ボットチャットで保存", "ko": "봇 대화방에서 저장",
        "zh_cn": "保存机器人聊天中的已删除消息", "zh_tw": "保存機器人聊天中的已刪除訊息"
    },
    "SaveDeletedMessageForBotUser": {
        "id": "Simpan Pesan Bot", "ja": "ボットメッセージを保存", "ko": "봇 메시지 저장",
        "zh_cn": "保存机器人本身发送的已删除消息", "zh_tw": "保存機器人本身傳送的已刪除訊息"
    },
    "SaveForNotifications": {
        "id": "Simpan untuk notifikasi", "ja": "通知用に保存", "ko": "알림용으로 저장",
        "zh_cn": "保存为通知提示音", "zh_tw": "儲存為通知提示音"
    },
    "SaveToDownloads": {
        "id": "Simpan ke Unduhan", "ja": "ダウンロードに保存", "ko": "다운로드에 저장",
        "zh_cn": "保存到“下载”目录", "zh_tw": "儲存至「下載」目錄"
    },
    "SaveToGallery": {
        "id": "Simpan ke galeri", "ja": "ギャラリーに保存", "ko": "갤러리에 저장",
        "zh_cn": "保存到系统相册", "zh_tw": "儲存至系統相簿"
    },
    "SavedMessages": {
        "id": "Pesan Tersimpan", "ja": "保存されたメッセージ", "ko": "저장한 메시지",
        "zh_cn": "收藏夹", "zh_tw": "收藏夾"
    },
    "Search": {
        "id": "Cari", "ja": "検索", "ko": "검색",
        "zh_cn": "搜索", "zh_tw": "搜尋"
    },
    "Settings": {
        "id": "Pengaturan", "ja": "設定", "ko": "설정",
        "zh_cn": "设置", "zh_tw": "設定"
    },
    "SettingsNotifications": {
        "id": "Notifikasi", "ja": "通知", "ko": "알림",
        "zh_cn": "通知与提示音", "zh_tw": "通知與提示音"
    },
    "ShareFile": {
        "id": "Bagikan", "ja": "共有", "ko": "공유",
        "zh_cn": "分享文件", "zh_tw": "分享檔案"
    },
    "ShareLink": {
        "id": "Bagikan Tautan", "ja": "リンクを共有", "ko": "링크 공유",
        "zh_cn": "分享链接", "zh_tw": "分享連結"
    },
    "StatisticsAndBoosts": {
        "id": "Statistik dan Peningkatan", "ja": "統計とブースト", "ko": "통계 및 부스트",
        "zh_cn": "统计数据与助力", "zh_tw": "統計數據與助力"
    },
    "StickersName": {
        "id": "Stiker", "ja": "ステッカー", "ko": "스티커",
        "zh_cn": "贴纸", "zh_tw": "貼圖"
    },
    "TelegramPremium": {
        "id": "Telegram Premium", "ja": "Telegram Premium", "ko": "Telegram Premium",
        "zh_cn": "Telegram Premium", "zh_tw": "Telegram Premium"
    },
    "TosDeclineDeleteAccount": {
        "id": "Peringatan: Tindakan ini akan menghapus akun Telegram Anda secara permanen.",
        "ja": "警告：この操作を行うと、Telegramアカウントが完全に削除されます。",
        "ko": "경고: 이 작업은 Telegram 계정과 모든 클라우드 데이터를 영구적으로 삭제합니다.",
        "zh_cn": "警告：此操作将彻底注销您的 Telegram 账号并清除所有云端数据。",
        "zh_tw": "警告：此操作將徹底註銷您的 Telegram 帳號並清除所有雲端資料。"
    },
    "TranslateMessage": {
        "id": "Terjemahkan", "ja": "翻訳", "ko": "번역",
        "zh_cn": "翻译", "zh_tw": "翻譯"
    },
    "TranslucentDeletedMessages": {
        "id": "Pesan Terhapus Tembus Pandang", "ja": "半透明の削除済みメッセージ", "ko": "반투명한 삭제된 메시지",
        "zh_cn": "半透明显示已删除消息", "zh_tw": "半透明顯示已刪除訊息"
    },
    "UserColorTabProfile": {
        "id": "Profil", "ja": "プロフィール", "ko": "프로필",
        "zh_cn": "个人资料", "zh_tw": "個人檔案"
    },
    "VibrationDisabled": {
        "id": "Nonaktif", "ja": "無効", "ko": "비활성화",
        "zh_cn": "已停用", "zh_tw": "已停用"
    },
    "exteraAppName": {
        "id": "SoundGram", "ja": "SoundGram", "ko": "SoundGram",
        "zh_cn": "SoundGram", "zh_tw": "SoundGram"
    }
}

langs = ["id", "ja", "ko", "zh_cn", "zh_tw"]
lang_map = {
    "id": "id",
    "ja": "ja",
    "ko": "ko",
    "zh_cn": "zh-rCN",
    "zh_tw": "zh-rTW"
}

out_data = {lang_map[l]: {} for l in langs}

for k in sorted(en_dict.keys()):
    if k in ASIAN_MAP:
        for l in langs:
            dest = lang_map[l]
            out_data[dest][k] = ASIAN_MAP[k].get(l, en_dict[k])
    else:
        for l in langs:
            dest = lang_map[l]
            out_data[dest][k] = existing.get(dest, {}).get(k, en_dict[k])

with open("tools/translations_asian.json", "w", encoding="utf-8") as out:
    json.dump(out_data, out, ensure_ascii=False, indent=2)

print("Generated tools/translations_asian.json successfully with counts:")
for l, d in out_data.items():
    print(f"  {l}: {len(d)} keys")
