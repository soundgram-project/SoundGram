# -*- coding: utf-8 -*-
"""
Generate complete translations for DE, FR, ES, IT, PT, NL, NO for all 460 keys.
"""
import json, os

with open("tools/missing_460_en_ru.json", "r", encoding="utf-8") as f:
    base = json.load(f)

en_dict = base["en"]
ru_dict = base["ru"]

with open("tools/existing_translations.json", "r", encoding="utf-8") as f:
    existing = json.load(f)

# Vocabulary mapping: key -> dict of {de, fr, es, it, pt, nl, no}
WESTERN_MAP = {
    "AP_CenterTitle": {
        "de": "Zentrierter Titel", "fr": "Titre centré", "es": "Título centrado",
        "it": "Titolo centrato", "pt": "Título centralizado", "nl": "Gecentreerde titel", "no": "Sentrert tittel"
    },
    "AP_DisableDividers": {
        "de": "Trennlinien ausblenden", "fr": "Masquer les séparateurs", "es": "Ocultar divisores",
        "it": "Nascondi divisori", "pt": "Ocultar divisores", "nl": "Scheidingslijnen verbergen", "no": "Skjul skillelinjer"
    },
    "AP_GlareOnElements": {
        "de": "Glanz auf Elementen", "fr": "Reflets sur les éléments", "es": "Brillo en elementos",
        "it": "Bagliore sugli elementi", "pt": "Brilho nos elementos", "nl": "Glans op elementen", "no": "Glans på elementer"
    },
    "AP_GlareOnElementsInfo": {
        "de": "Ober- und Unterlichtreflexe auf Flüssigglaselementen anzeigen.",
        "fr": "Afficher les reflets supérieurs et inférieurs sur les éléments en verre liquide.",
        "es": "Mostrar reflejos superiores e inferiores en elementos de cristal líquido.",
        "it": "Mostra i riflessi superiori e inferiori sugli elementi in vetro liquido.",
        "pt": "Mostrar reflexos superiores e inferiores em elementos de vidro líquido.",
        "nl": "Toon boven- en onderreflecties op vloeibaar glas-elementen.",
        "no": "Vis øvre og nedre lysrefleksjoner på flytende glasselementer."
    },
    "AP_Header": {
        "de": "Kopfzeile", "fr": "En-tête", "es": "Encabezado",
        "it": "Intestazione", "pt": "Cabeçalho", "nl": "Koptekst", "no": "Topptekst"
    },
    "AP_Header_Appearance": {
        "de": "Erscheinungsbild", "fr": "Apparence", "es": "Apariencia",
        "it": "Aspetto", "pt": "Aparência", "nl": "Uiterlijk", "no": "Utseende"
    },
    "AP_HideSearchBar": {
        "de": "Chat-Suchleiste ausblenden", "fr": "Masquer la barre de recherche", "es": "Ocultar barra de búsqueda de chats",
        "it": "Nascondi barra di ricerca chat", "pt": "Ocultar barra de pesquisa de chats", "nl": "Chatzoekbalk verbergen", "no": "Skjul søkefelt for samtaler"
    },
    "AP_IconReplacements": {
        "de": "Icon-Paket", "fr": "Pack d'icônes", "es": "Paquete de iconos",
        "it": "Pacchetto icone", "pt": "Pacote de ícones", "nl": "Icoonpakket", "no": "Ikonpakke"
    },
    "AP_ShowPencilIcon": {
        "de": "Bearbeitete Nachrichten markieren", "fr": "Marquer les messages modifiés", "es": "Marcar mensajes editados",
        "it": "Contrassegna messaggi modificati", "pt": "Marcar mensagens editadas", "nl": "Bewerkte berichten markeren", "no": "Merk redigerte meldinger"
    },
    "AP_SystemEmoji": {
        "de": "System-Emojis", "fr": "Émojis système", "es": "Emojis del sistema",
        "it": "Emoji di sistema", "pt": "Emojis do sistema", "nl": "Systeememoji", "no": "Systememoji"
    },
    "AP_SystemFonts": {
        "de": "Systemschriftarten", "fr": "Polices système", "es": "Fuentes del sistema",
        "it": "Caratteri di sistema", "pt": "Fontes do sistema", "nl": "Systeemlettertypen", "no": "Systemskrifttyper"
    },
    "AP_Tablet_Mode": {
        "de": "Tablet-Modus", "fr": "Mode tablette", "es": "Modo tableta",
        "it": "Modalità tablet", "pt": "Modo tablet", "nl": "Tabletmodus", "no": "Nettbrettmodus"
    },
    "AccActionPause": {
        "de": "Pause", "fr": "Pause", "es": "Pausa",
        "it": "Pausa", "pt": "Pausar", "nl": "Pauze", "no": "Pause"
    },
    "AccActionPlay": {
        "de": "Wiedergabe", "fr": "Lecture", "es": "Reproducir",
        "it": "Riproduci", "pt": "Reproduzir", "nl": "Afspelen", "no": "Spill av"
    },
    "AccDescrCustomEmoji": {
        "de": "Benutzerdefinierte Emojis", "fr": "Émojis personnalisés", "es": "Emojis personalizados",
        "it": "Emoji personalizzate", "pt": "Emojis personalizados", "nl": "Aangepaste emoji", "no": "Egendefinerte emoji"
    },
    "AccDescrGIFs": {
        "de": "GIFs", "fr": "GIFs", "es": "GIFs",
        "it": "GIF", "pt": "GIFs", "nl": "GIF\'s", "no": "GIF-er"
    },
    "AccDescrStickers": {
        "de": "Sticker", "fr": "Autocollants", "es": "Stickers",
        "it": "Sticker", "pt": "Stickers", "nl": "Stickers", "no": "Klistremerker"
    },
    "ActionsChartTitle": {
        "de": "Aktionen", "fr": "Actions", "es": "Acciones",
        "it": "Azioni", "pt": "Ações", "nl": "Acties", "no": "Handlinger"
    },
    "AppName": {
        "de": "SoundGram", "fr": "SoundGram", "es": "SoundGram",
        "it": "SoundGram", "pt": "SoundGram", "nl": "SoundGram", "no": "SoundGram"
    },
    "AreYouSure": {
        "de": "Sind Sie sicher?", "fr": "Êtes-vous sûr ?", "es": "¿Estás seguro?",
        "it": "Sei sicuro?", "pt": "Tem certeza?", "nl": "Weet u het zeker?", "no": "Er du sikker?"
    },
    "AvatarPreviewSearchMessages": {
        "de": "Nachrichten durchsuchen", "fr": "Rechercher des messages", "es": "Buscar mensajes",
        "it": "Cerca messaggi", "pt": "Pesquisar mensagens", "nl": "Berichten zoeken", "no": "Søk i meldinger"
    },
    "Back": {
        "de": "Zurück", "fr": "Retour", "es": "Atrás",
        "it": "Indietro", "pt": "Voltar", "nl": "Terug", "no": "Tilbake"
    },
    "BusinessHoursProfile": {
        "de": "Öffnungszeiten", "fr": "Horaires d'ouverture", "es": "Horario comercial",
        "it": "Orari di apertura", "pt": "Horário comercial", "nl": "Openingstijden", "no": "Åpningstider"
    },
    "BusinessHoursProfileNowOpen": {
        "de": "Geöffnet", "fr": "Ouvert", "es": "Abierto",
        "it": "Aperto", "pt": "Aberto", "nl": "Open", "no": "Åpent"
    },
    "BusinessProfileLocation": {
        "de": "Standort", "fr": "Adresse", "es": "Ubicación",
        "it": "Posizione", "pt": "Localização", "nl": "Locatie", "no": "Plassering"
    },
    "CP_AdminActions": {
        "de": "Admin-Aktionen", "fr": "Raccourcis d'administration", "es": "Acciones de administrador",
        "it": "Scorciatoie amministratore", "pt": "Ações do administrador", "nl": "Beheerderssnelkoppelingen", "no": "Administratorhandlinger"
    },
    "CP_ArchiveStories": {
        "de": "Stories archivieren", "fr": "Archiver les stories", "es": "Archivar historias",
        "it": "Archivia storie", "pt": "Arquivar stories", "nl": "Verhalen archiveren", "no": "Arkiver historier"
    },
    "CP_ArchiveStories_Desc": {
        "de": "Neue Stories automatisch ins Archiv verschieben.",
        "fr": "Déplacer automatiquement les nouvelles stories vers l'archive.",
        "es": "Mover automáticamente nuevas historias al archivo.",
        "it": "Sposta automaticamente le nuove storie nell'archivio.",
        "pt": "Mover automaticamente novos stories para o arquivo.",
        "nl": "Verplaats nieuwe verhalen automatisch naar het archief.",
        "no": "Flytt nye historier automatisk til arkivet."
    },
    "CP_AutoPauseVideo": {
        "de": "Medien automatisch anhalten", "fr": "Pause auto des médias", "es": "Pausar medios automáticamente",
        "it": "Pausa automatica media", "pt": "Pausar mídia automaticamente", "nl": "Media automatisch pauzeren", "no": "Sett medier på pause automatisk"
    },
    "CP_AutoPauseVideo_Desc": {
        "de": "Anhalten, wenn die App in den Hintergrund wechselt.",
        "fr": "Mettre en pause lorsque l'application passe en arrière-plan.",
        "es": "Pausar cuando la aplicación pasa a segundo plano.",
        "it": "Metti in pausa quando l'app passa in secondo piano.",
        "pt": "Pausar quando o aplicativo for para segundo plano.",
        "nl": "Pauzeren wanneer de app naar de achtergrond gaat.",
        "no": "Sett på pause når appen går til bakgrunnen."
    },
    "CP_AutoQuoteReplies": {
        "de": "Antworten zitieren", "fr": "Citer les réponses", "es": "Citar respuestas",
        "it": "Cita risposte", "pt": "Citar respostas", "nl": "Antwoorden citeren", "no": "Siter svar"
    },
    "CP_AutoQuoteReplies_Desc": {
        "de": "Verwende ein Zitat anstelle einer Standardantwort.",
        "fr": "Utiliser une citation au lieu d'une réponse standard.",
        "es": "Usa una cita en lugar de una respuesta estándar.",
        "it": "Usa una citazione invece di una risposta standard.",
        "pt": "Usar uma citação em vez de uma resposta padrão.",
        "nl": "Gebruik een citaat in plaats van een standaardantwoord.",
        "no": "Bruk et sitat i stedet for et standardsvar."
    },
    "CP_Camera2Desc": {
        "de": "Alle Funktionen, die bei aktiviertem Camera2 ausgeblendet sind, funktionieren nicht.",
        "fr": "Toutes les fonctions masquées lorsque Camera2 est activé ne fonctionneront pas.",
        "es": "Las funciones ocultas al activar Camera2 no funcionarán con Camera2 activado.",
        "it": "Tutte le funzioni nascoste con Camera2 attivo non funzioneranno.",
        "pt": "Todos os recursos ocultos quando a Camera2 estiver ativada não funcionarão.",
        "nl": "Functies die verborgen zijn bij Camera2 werken niet.",
        "no": "Funksjoner som skjules når Camera2 er slått på vil ikke fungere."
    },
    "CP_CameraDualCamera": {
        "de": "Duale Kamera", "fr": "Double caméra", "es": "Cámara dual",
        "it": "Doppia fotocamera", "pt": "Câmera dupla", "nl": "Dubbele camera", "no": "Dobbeltkamera"
    },
    "CP_CameraDualCamera_Desc": {
        "de": "Nimmt Videonachrichten auf beiden Kameras für schnelleres Umschalten auf.",
        "fr": "Enregistre le message vidéo sur les deux caméras pour un basculement plus rapide.",
        "es": "Graba mensajes de vídeo en ambas cámaras para alternar más rápido.",
        "it": "Registra videomessaggi su entrambe le fotocamere per un passaggio più rapido.",
        "pt": "Grava mensagens de vídeo em ambas as câmeras para alternância mais rápida.",
        "nl": "Neemt videoberichten op beide camera\'s op voor sneller schakelen.",
        "no": "Tar opp videomeldinger på begge kameraer for raskere veksling."
    },
    "CP_CameraQuality": {
        "de": "Kameraqualität", "fr": "Qualité de la caméra", "es": "Calidad de cámara",
        "it": "Qualità fotocamera", "pt": "Qualidade da câmera", "nl": "Camerakwaliteit", "no": "Kamerakvalitet"
    },
    "CP_CameraStabilisation": {
        "de": "Kamerastabilisierung", "fr": "Stabilisation de la caméra", "es": "Estabilización de cámara",
        "it": "Stabilizzazione fotocamera", "pt": "Estabilização da câmera", "nl": "Camerastabilisatie", "no": "Kamerastabilisering"
    },
    "CP_CameraType": {
        "de": "Kameratyp", "fr": "Type de caméra", "es": "Tipo de cámara",
        "it": "Tipo di fotocamera", "pt": "Tipo de câmera", "nl": "Cameratype", "no": "Kameratype"
    },
    "CP_CameraTypeSystem": {
        "de": "System", "fr": "Système", "es": "Sistema",
        "it": "Sistema", "pt": "Sistema", "nl": "Systeem", "no": "System"
    },
    "CP_CameraUW": {
        "de": "Mit Ultraweitwinkel starten", "fr": "Démarrer en ultra grand-angle", "es": "Comenzar con ultra gran angular",
        "it": "Avvia con ultra-grandangolare", "pt": "Iniciar com ultra grande-angular", "nl": "Starten vanaf ultragroothoek", "no": "Start fra ultravidvinkel"
    },
    "CP_CameraUW_Desc": {
        "de": "Die Videoaufnahme beginnt mit der Ultraweitwinkelkamera.",
        "fr": "L'enregistrement vidéo commencera avec la caméra ultra grand-angle.",
        "es": "La grabación comenzará con la cámara ultra gran angular.",
        "it": "La registrazione dei videomessaggi inizierà con la fotocamera ultra-grandangolare.",
        "pt": "A gravação começará com a câmera ultra grande-angular.",
        "nl": "Opname van videoberichten start vanaf de ultragroothoekcamera.",
        "no": "Opptak av videomeldinger starter fra ultravidvinkelkameraet."
    },
    "CP_CameraXDesc": {
        "de": "CameraX wird nur auf ausgewählten Geräten unterstützt.",
        "fr": "CameraX n'est pris en charge que sur certains appareils.",
        "es": "CameraX solo es compatible con dispositivos seleccionados.",
        "it": "CameraX è supportata solo su dispositivi selezionati.",
        "pt": "CameraX é compatível apenas em dispositivos selecionados.",
        "nl": "CameraX wordt alleen ondersteund op geselecteerde apparaten.",
        "no": "CameraX støttes bare på utvalgte enheter."
    },
    "CP_Category_Camera": {
        "de": "Kamera", "fr": "Appareil photo", "es": "Cámara",
        "it": "Fotocamera", "pt": "Câmera", "nl": "Camera", "no": "Kamera"
    },
    "CP_CenterCameraControlButtons": {
        "de": "Bedienelemente zentrieren", "fr": "Centrer les boutons de contrôle", "es": "Centrar botones de control",
        "it": "Centra i pulsanti di controllo", "pt": "Centralizar botões de controle", "nl": "Bedieningsknoppen centreren", "no": "Sentrer kontrollknapper"
    },
    "CP_CenterCameraControlButtons_Desc": {
        "de": "Tasten für Kamerawechsel und Taschenlampe zentrieren.",
        "fr": "Centrer les boutons de changement de caméra et de flash.",
        "es": "Centrar los botones de cambio de cámara y linterna.",
        "it": "Centra i pulsanti di cambio fotocamera e torcia.",
        "pt": "Centralizar botões de troca de câmera e lanterna.",
        "nl": "Centreer knoppen voor camerawissel en zaklamp.",
        "no": "Sentrer knapper for bytte av kamera og lommelykt."
    },
    "CP_ChatMenuShortcuts": {
        "de": "Chat-Menü-Aktionen", "fr": "Raccourcis du menu de discussion", "es": "Acciones del menú de chat",
        "it": "Scorciatoie menu chat", "pt": "Atalhos do menu de chat", "nl": "Chatmenusnelkoppelingen", "no": "Handlinger i chatmenyen"
    },
    "CP_CustomWallpapers": {
        "de": "Individuelle Chat-Hintergründe", "fr": "Fonds d'écran par chat", "es": "Fondos por chat",
        "it": "Sfondi per singola chat", "pt": "Papéis de parede individuais", "nl": "Achtergronden per chat", "no": "Bakgrunner per samtale"
    },
    "CP_CustomWallpapers_Desc": {
        "de": "Hintergründe für einzelne Chats anzeigen.",
        "fr": "Afficher les fonds d'écran définis pour les discussions individuelles.",
        "es": "Mostrar fondos de pantalla para chats individuales.",
        "it": "Mostra gli sfondi impostati per le singole chat.",
        "pt": "Mostrar papéis de parede definidos para chats individuais.",
        "nl": "Toon achtergronden die zijn ingesteld voor individuele chats.",
        "no": "Vis bakgrunner angitt for individuelle samtaler."
    },
    "CP_DefaultCameraDesc": {
        "de": "Verwende die integrierte Telegram-Kamera.", "fr": "Utiliser l'appareil photo intégré de Telegram.", "es": "Usa la cámara integrada de Telegram.",
        "it": "Usa la fotocamera integrata di Telegram.", "pt": "Usar a câmera integrada do Telegram.", "nl": "Gebruik de ingebouwde camera van Telegram.", "no": "Bruk det innebygde Telegram-kameraet."
    },
    "CP_DeleteForAll": {
        "de": "Standardmäßig für alle löschen", "fr": "Supprimer pour tous par défaut", "es": "Eliminar para todos por defecto",
        "it": "Elimina per tutti per impostazione predefinita", "pt": "Excluir para todos por padrão", "nl": "Standaard voor iedereen verwijderen", "no": "Slett for alle som standard"
    },
    "CP_DeleteForAll_Desc": {
        "de": "Diese Option im Löschdialog vorab auswählen.",
        "fr": "Présélectionner cette option dans la boîte de dialogue de suppression.",
        "es": "Preseleccionar esta opción en el diálogo de eliminación.",
        "it": "Preseleziona questa opzione nella finestra di eliminazione.",
        "pt": "Pré-selecionar esta opção na caixa de diálogo de exclusão.",
        "nl": "Selecteer deze optie vooraf in het verwijderdialoogvenster.",
        "no": "Forhåndsvelg dette alternativet i slettedialogen."
    },
    "CP_DisableCam": {
        "de": "Kamerataste unten", "fr": "Bouton caméra en bas", "es": "Botón de cámara abajo",
        "it": "Pulsante fotocamera in basso", "pt": "Botão da câmera embaixo", "nl": "Cameraknop onderaan", "no": "Kameraknapp nederst"
    },
    "CP_DisableCam_Desc": {
        "de": "Die Kamerataste wird neben der Galerietaste angezeigt.",
        "fr": "Le bouton de la caméra apparaîtra à côté du bouton de la galerie.",
        "es": "El botón de la cámara aparecerá junto al botón de galería.",
        "it": "Il pulsante della fotocamera apparirà accanto al pulsante della galleria.",
        "pt": "O botão da câmera aparecerá ao lado do botão da galeria.",
        "nl": "De cameraknop verschijnt naast de galerijknop.",
        "no": "Kameraknappen vises ved siden av galleriknappen."
    },
    "CP_DisablePremStickAnim": {
        "de": "Premium-Sticker-Effekte deaktivieren", "fr": "Désactiver les effets d'autocollants premium", "es": "Desactivar efectos de stickers premium",
        "it": "Disattiva effetti sticker premium", "pt": "Desativar efeitos de stickers premium", "nl": "Premium stickereffecten uitschakelen", "no": "Deaktiver premium klistremerkeeffekter"
    },
    "CP_DisablePremStickAnim_Desc": {
        "de": "Effekte beim Antippen eines Stickers nicht abspielen.",
        "fr": "Ne pas jouer les effets lors d'un appui sur un autocollant.",
        "es": "No reproducir efectos al tocar un sticker.",
        "it": "Non riprodurre gli effetti quando si tocca uno sticker.",
        "pt": "Não reproduzir efeitos ao tocar em um sticker.",
        "nl": "Speel geen effecten af bij het aantikken van een sticker.",
        "no": "Ikke spill av effekter når et klistremerke berøres."
    },
    "CP_DisablePremStickAutoPlay": {
        "de": "Automatisches Abspielen von Premium-Stickern deaktivieren",
        "fr": "Désactiver la lecture automatique des autocollants premium",
        "es": "Desactivar reproducción automática de stickers premium",
        "it": "Disattiva riproduzione automatica sticker premium",
        "pt": "Desativar reprodução automática de stickers premium",
        "nl": "Automatisch afspelen van premium stickers uitschakelen",
        "no": "Deaktiver automatisk avspilling av premium klistremerker"
    },
    "CP_DisablePremStickAutoPlay_Desc": {
        "de": "Premium-Stickeranimationen nicht automatisch abspielen.",
        "fr": "Ne pas lire automatiquement les animations d'autocollants Premium.",
        "es": "No reproducir automáticamente las animaciones de stickers Premium.",
        "it": "Non riprodurre automaticamente le animazioni degli sticker Premium.",
        "pt": "Não reproduzir animações de stickers Premium automaticamente.",
        "nl": "Premium stickeranimaties niet automatisch afspelen.",
        "no": "Ikke spill av Premium klistremerkeanimasjoner automatisk."
    },
    "CP_DisableReactionAnim": {
        "de": "Reaktionsanimationen deaktivieren", "fr": "Désactiver l'animation des réactions", "es": "Desactivar animación de reacciones",
        "it": "Disattiva animazione reazioni", "pt": "Desativar animação de reações", "nl": "Reactie-animatie uitschakelen", "no": "Deaktiver reaksjonsanimasjon"
    },
    "CP_DisableReactionAnim_Desc": {
        "de": "Reaktionen ohne große Animation anwenden.",
        "fr": "Appliquer des réactions sans la grande animation.",
        "es": "Aplicar reacciones sin la animación grande.",
        "it": "Applica reazioni senza la grande animazione.",
        "pt": "Aplicar reações sem a animação grande.",
        "nl": "Pas reacties toe zonder grote animatie.",
        "no": "Bruk reaksjoner uten den store animasjonen."
    },
    "CP_DisableReactionsOverlay": {
        "de": "Reaktionsleiste ausblenden", "fr": "Masquer le panneau de réactions", "es": "Ocultar panel de reacciones",
        "it": "Nascondi pannello reazioni", "pt": "Ocultar painel de reações", "nl": "Reactiepaneel verbergen", "no": "Skjul reaksjonspanel"
    },
    "CP_DisableReactionsOverlay_Desc": {
        "de": "Reaktionen über Nachrichten nicht anzeigen.",
        "fr": "Ne pas afficher les réactions au-dessus des messages.",
        "es": "No mostrar reacciones sobre los mensajes.",
        "it": "Non mostrare le reazioni sopra i messaggi.",
        "pt": "Não mostrar reações acima das mensagens.",
        "nl": "Toon geen reacties boven berichten.",
        "no": "Ikke vis reaksjoner over meldinger."
    },
    "CP_DisableSwipeToNext": {
        "de": "Kanal-Wischen deaktivieren", "fr": "Désactiver le glissement de canal", "es": "Desactivar deslizamiento entre canales",
        "it": "Disattiva scorrimento canali", "pt": "Desativar deslizar entre canais", "nl": "Kanaal vegen uitschakelen", "no": "Deaktiver kanalsveiping"
    },
    "CP_DisableSwipeToNext_Desc": {
        "de": "Beim Scrollen im aktuellen Kanal bleiben.",
        "fr": "Continuer à faire défiler dans le canal actuel.",
        "es": "Permanecer en el canal actual al desplazarse.",
        "it": "Continua a scorrere all'interno del canale corrente.",
        "pt": "Continuar rolando dentro do canal atual.",
        "nl": "Blijf scrollen in het huidige kanaal.",
        "no": "Fortsett å bla i den gjeldende kanalen."
    },
    "CP_DisableVibration": {
        "de": "Vibration deaktivieren", "fr": "Désactiver les vibrations", "es": "Desactivar vibración",
        "it": "Disattiva vibrazione", "pt": "Desativar vibração", "nl": "Trillen uitschakelen", "no": "Deaktiver vibrasjon"
    },
    "CP_DoubleTapAction": {
        "de": "Doppeltipp-Aktion", "fr": "Action du double appui", "es": "Acción de doble toque",
        "it": "Azione doppio tocco", "pt": "Ação de toque duplo", "nl": "Dubbeltikactie", "no": "Dobbelttrykkhandling"
    },
    "CP_Filters_Header": {
        "de": "Ordner", "fr": "Dossiers", "es": "Carpetas",
        "it": "Cartelle", "pt": "Pastas", "nl": "Mappen", "no": "Mapper"
    },
    "CP_ForwardMsgDate": {
        "de": "Datum weitergeleiteter Nachrichten anzeigen", "fr": "Afficher la date du message transféré", "es": "Mostrar fecha del mensaje reenviado",
        "it": "Mostra data del messaggio inoltrato", "pt": "Mostrar data da mensagem encaminhada", "nl": "Datum van doorgestuurd bericht tonen", "no": "Vis dato for videresendt melding"
    },
    "CP_Header_DialogList": {
        "de": "Chatliste", "fr": "Liste des discussions", "es": "Lista de chats",
        "it": "Elenco chat", "pt": "Lista de chats", "nl": "Chatlijst", "no": "Samtaleliste"
    },
    "CP_Header_Videomessages": {
        "de": "Videonachrichten", "fr": "Messages vidéo", "es": "Mensajes de vídeo",
        "it": "Videomessaggi", "pt": "Mensagens de vídeo", "nl": "Videoberichten", "no": "Videomeldinger"
    },
    "CP_HideKbdOnScroll": {
        "de": "Tastatur beim Scrollen ausblenden", "fr": "Masquer le clavier lors du défilement", "es": "Ocultar teclado al desplazarse",
        "it": "Nascondi tastiera durante lo scorrimento", "pt": "Ocultar teclado ao rolar", "nl": "Toetsenbord verbergen bij scrollen", "no": "Skjul tastatur ved rulling"
    },
    "CP_HideMuteUnmuteButton": {
        "de": "Stummschaltleiste ausblenden", "fr": "Masquer la barre de sourdine", "es": "Ocultar barra de silencio",
        "it": "Nascondi barra del volume del canale", "pt": "Ocultar barra de som do canal", "nl": "Dempbalk verbergen", "no": "Skjul dempelinje"
    },
    "CP_HideSendAsChannel": {
        "de": "„Als Kanal senden“ ausblenden", "fr": "Masquer « Envoyer en tant que canal »", "es": "Ocultar «Enviar como canal»",
        "it": "Nascondi «Invia come canale»", "pt": "Ocultar «Enviar como canal»", "nl": "„Verzenden als kanaal” verbergen", "no": "Skjul «Send som kanal»"
    },
    "CP_HideSendAsChannelDesc": {
        "de": "Das Menü bleibt durch langes Drücken der Emoji-Taste verfügbar.",
        "fr": "Le menu reste accessible en maintenant le bouton émoji.",
        "es": "El menú permanece disponible manteniendo pulsado el botón de emoji.",
        "it": "Il menu rimane disponibile tenendo premuto il pulsante emoji.",
        "pt": "O menu permanece disponível mantendo pressionado o botão de emoji.",
        "nl": "Het menu blijft beschikbaar door de emojiknop ingedrukt te houden.",
        "no": "Menyen forblir tilgjengelig ved å holde inne emojiknappen."
    },
    "CP_HideStories": {
        "de": "Stories ausblenden", "fr": "Masquer les stories", "es": "Ocultar historias",
        "it": "Nascondi storie", "pt": "Ocultar stories", "nl": "Verhalen verbergen", "no": "Skjul historier"
    },
    "CP_HideStories_Desc": {
        "de": "Stories aus Chats und Profilen entfernen.",
        "fr": "Supprimer les stories des discussions et des profils.",
        "es": "Eliminar historias de chats y perfiles.",
        "it": "Rimuovi le storie da chat e profili.",
        "pt": "Remover stories de chats e perfis.",
        "nl": "Verhalen verwijderen uit chats en profielen.",
        "no": "Fjern historier fra samtaler og profiler."
    },
    "CP_LeftBottomButtonAction": {
        "de": "Aktion der linken Taste", "fr": "Action du bouton gauche", "es": "Acción del botón izquierdo",
        "it": "Azione pulsante sinistro", "pt": "Ação do botão esquerdo", "nl": "Actie linkerknop", "no": "Handling for venstre knapp"
    },
    "CP_MainTabs_ForceOpenChats_Desc": {
        "de": "Die Suchtaste öffnet immer die Chat-Suche.",
        "fr": "Le bouton de recherche ouvre toujours la recherche de chat.",
        "es": "El botón de búsqueda siempre abre la búsqueda de chats.",
        "it": "Il pulsante di ricerca apre sempre la ricerca chat.",
        "pt": "O botão de pesquisa sempre abre a pesquisa de chats.",
        "nl": "De zoekknop opent altijd chatzoekopdracht.",
        "no": "Søk-knappen åpner alltid samtalesøk."
    },
    "CP_MainTabs_Header": {
        "de": "Untere Tabs", "fr": "Onglets du bas", "es": "Pestañas inferiores",
        "it": "Schede inferiori", "pt": "Abas inferiores", "nl": "Onderste tabbladen", "no": "Faner nederst"
    },
    "CP_MessageMenu": {
        "de": "Nachrichtenmenü", "fr": "Menu du message", "es": "Menú del mensaje",
        "it": "Menu messaggio", "pt": "Menu da mensagem", "nl": "Berichtmenu", "no": "Meldingsmeny"
    },
    "CP_MessageMenuCompactLayout": {
        "de": "Kompaktes Menü-Layout", "fr": "Disposition compacte du menu", "es": "Diseño de menú compacto",
        "it": "Layout compatto del menu", "pt": "Layout compacto do menu", "nl": "Compacte menu-indeling", "no": "Kompakt menyoppsett"
    },
    "CP_MessageMenuCompactLayout_Desc": {
        "de": "Bis zu vier Hauptaktionen als große Schaltflächen anzeigen.",
        "fr": "Afficher jusqu'à quatre actions principales sous forme de grands boutons.",
        "es": "Muestra hasta cuatro acciones principales como botones grandes.",
        "it": "Mostra fino a quattro azioni principali come pulsanti grandi.",
        "pt": "Mostra até quatro ações principais como botões grandes.",
        "nl": "Toon maximaal vier hoofdacties als grote knoppen.",
        "no": "Vis opptil fire hovedhandlinger som store knapper."
    },
    "CP_MessageMenuCompactLayout_Dot": {
        "de": "Ein Punkt kennzeichnet Aktionen bei langem Drücken.",
        "fr": "Un point marque les actions d'appui long.",
        "es": "Un punto marca las acciones de pulsación larga.",
        "it": "Un punto indica le azioni di pressione prolungata.",
        "pt": "Um ponto marca ações de pressão longa.",
        "nl": "Een stip markeert acties bij lang indrukken.",
        "no": "En prikk markerer handlinger med langt trykk."
    },
    "CP_MessageMenuItems": {
        "de": "Nachrichtenmenü-Elemente", "fr": "Éléments du menu de message", "es": "Elementos del menú de mensajes",
        "it": "Voci del menu messaggio", "pt": "Itens do menu de mensagens", "nl": "Berichtmenu-items", "no": "Meldingsmenyelementer"
    },
    "CP_Message_Filtering": {
        "de": "Nachrichtenfilterung", "fr": "Filtrage des messages", "es": "Filtrado de mensajes",
        "it": "Filtraggio messaggi", "pt": "Filtragem de mensagens", "nl": "Berichtfiltering", "no": "Meldingsfiltrering"
    },
    "CP_Message_Filtering_Collapse": {
        "de": "Nachrichten einklappen", "fr": "Réduire les messages", "es": "Colapsar mensajes",
        "it": "Comprimi messaggi", "pt": "Recolher mensagens", "nl": "Berichten samenvouwen", "no": "Skjul meldinger"
    },
    "CP_Message_Filtering_Collapse_Desc": {
        "de": "Gefilterte Nachrichten automatisch einklappen.",
        "fr": "Réduire automatiquement les messages filtrés.",
        "es": "Colapsar automáticamente mensajes filtrados.",
        "it": "Comprimi automaticamente i messaggi filtrati.",
        "pt": "Recolher mensagens filtradas automaticamente.",
        "nl": "Gefilterde berichten automatisch samenvouwen.",
        "no": "Skjul filtrerte meldinger automatisk."
    },
    "CP_Messages_Size": {
        "de": "Blasengröße", "fr": "Taille des bulles", "es": "Tamaño de burbuja",
        "it": "Dimensione bolle", "pt": "Tamanho das bolhas", "nl": "Bubbelgrootte", "no": "Boblestørrelse"
    },
    "CP_PlayVideo": {
        "de": "Video mit Lautstärketasten abspielen", "fr": "Lire la vidéo avec les boutons de volume", "es": "Reproducir vídeo con botones de volumen",
        "it": "Riproduci video con i tasti volume", "pt": "Reproduzir vídeo com botões de volume", "nl": "Video afspelen met volumeknoppen", "no": "Spill av video med volumknapper"
    },
    "CP_PlayVideo_Desc": {
        "de": "Sichtbares Video mit einer Lautstärketaste starten.",
        "fr": "Démarrer la vidéo visible avec un bouton de volume.",
        "es": "Iniciar el vídeo visible con un botón de volumen.",
        "it": "Avvia il video visibile con un pulsante del volume.",
        "pt": "Inicie o vídeo visível com um botão de volume.",
        "nl": "Start de zichtbare video met een volumeknop.",
        "no": "Start den synlige videoen med en volumknapp."
    },
    "CP_ProfileReplyBackground": {
        "de": "Nachrichten und Profile", "fr": "Messages et profils", "es": "Mensajes y perfiles",
        "it": "Messaggi e profili", "pt": "Mensagens e perfis", "nl": "Berichten en profielen", "no": "Meldinger og profiler"
    },
    "CP_SilenceNonContacts": {
        "de": "Nicht-Kontakte stummschalten", "fr": "Mettre en sourdine les non-contacts", "es": "Silenciar no contactos",
        "it": "Silenzia non-contatti", "pt": "Silenciar não-contatos", "nl": "Niet-contacten dempen", "no": "Demp ikke-kontakter"
    },
    "CP_SilenceNonContacts_Desc": {
        "de": "Benachrichtigungstöne nur für Kontakte abspielen.",
        "fr": "Émettre des sons de notification uniquement pour les contacts.",
        "es": "Reproducir sonidos de notificación solo para contactos.",
        "it": "Riproduci i suoni di notifica solo per i contatti.",
        "pt": "Tocar sons de notificação apenas para contatos.",
        "nl": "Speel meldingsgeluiden alleen af voor contacten.",
        "no": "Spill varsellyder kun for kontakter."
    },
    "CP_Slider_RecentEmojisAndStickers": {
        "de": "Zuletzt verwendete Emojis und Sticker", "fr": "Émojis et autocollants récents", "es": "Emojis y stickers recientes",
        "it": "Emoji e sticker recenti", "pt": "Emojis e stickers recentes", "nl": "Recente emoji en stickers", "no": "Nylige emoji og klistremerker"
    },
    "CP_Snowflakes_Header": {
        "de": "Schneeflocken", "fr": "Flocons de neige", "es": "Copos de nieve",
        "it": "Fiocchi di neve", "pt": "Flocos de neve", "nl": "Sneeuwvlokken", "no": "Snøflak"
    },
    "CP_SortByUnread": {
        "de": "Nach ungelesenen sortieren", "fr": "Trier par non lus", "es": "Ordenar por no leídos",
        "it": "Ordina per non letti", "pt": "Classificar por não lidas", "nl": "Sorteren op ongelezen", "no": "Sorter etter uleste"
    },
    "CP_SystemCameraDesc": {
        "de": "Direkter Zugriff auf die Systemkamera.", "fr": "Accès direct à la caméra système.", "es": "Acceso directo a la cámara del sistema.",
        "it": "Accesso diretto alla fotocamera di sistema.", "pt": "Acesso direto à câmera do sistema.", "nl": "Directe toegang tot de systeemcamera.", "no": "Direkte tilgang til systemkameraet."
    },
    "CP_TimeOnStick": {
        "de": "Zeitstempel auf Stickern ausblenden", "fr": "Masquer l'heure sur les autocollants", "es": "Ocultar marcas de tiempo en stickers",
        "it": "Nascondi data/ora sugli sticker", "pt": "Ocultar hora nos stickers", "nl": "Tijd op stickers verbergen", "no": "Skjul tidsstempel på klistremerker"
    },
    "CP_UnarchiveOnSwipe": {
        "de": "Durch Wischen dearchivieren", "fr": "Désarchiver par glissement", "es": "Desarchivar al deslizar",
        "it": "Estrai dall'archivio con scorrimento", "pt": "Desarquivar ao deslizar", "nl": "Dearchiveren bij vegen", "no": "Gjenopprett fra arkiv ved sveip"
    },
    "CP_UnreadBadgeOnBackButton": {
        "de": "Ungelesene-Zähler", "fr": "Compteur de non-lus", "es": "Contador de no leídos",
        "it": "Contatore non letti", "pt": "Contador de não lidas", "nl": "Ongelezen teller", "no": "Ulest-teller"
    },
    "CP_UnreadBadgeOnBackButton_Desc": {
        "de": "Ungelesene Nachrichten auf der Zurück-Taste anzeigen.",
        "fr": "Afficher le nombre de non lus sur le bouton Retour.",
        "es": "Mostrar el contador de no leídos en el botón Atrás.",
        "it": "Mostra il numero di non letti sul pulsante Indietro.",
        "pt": "Mostrar contagem de não lidas no botão Voltar.",
        "nl": "Toon aantal ongelezen berichten op de terugknop.",
        "no": "Vis antall uleste på Tilbake-knappen."
    },
    "CP_VibrateInChats": {
        "de": "Vibration im Chat", "fr": "Vibration dans le chat", "es": "Vibración en chats",
        "it": "Vibrazione nella chat", "pt": "Vibração nos chats", "nl": "Trillen in chats", "no": "Vibrasjon i samtaler"
    },
    "CP_VibrateInChats_Desc": {
        "de": "Vibrieren bei neuer Nachricht im geöffneten Chat.",
        "fr": "Vibrer lorsqu'un message arrive dans une discussion ouverte.",
        "es": "Vibrar cuando llega un mensaje en un chat abierto.",
        "it": "Vibra all'arrivo di un messaggio in una chat aperta.",
        "pt": "Vibrar quando uma mensagem chegar em um chat aberto.",
        "nl": "Trillen wanneer een bericht binnenkomt in een open chat.",
        "no": "Vibrer når en melding kommer i en åpen samtale."
    },
    "CP_VideoSeekDuration": {
        "de": "Video-Suchschritt", "fr": "Pas de recherche vidéo", "es": "Paso de avance de vídeo",
        "it": "Passo avanzamento video", "pt": "Passo de avanço do vídeo", "nl": "Video-zoekstap", "no": "Spoletrinn for video"
    },
    "Cancel": {
        "de": "Abbrechen", "fr": "Annuler", "es": "Cancelar",
        "it": "Annulla", "pt": "Cancelar", "nl": "Annuleren", "no": "Avbryt"
    },
    "ChangePasscode": {
        "de": "Code ändern", "fr": "Changer le code", "es": "Cambiar código",
        "it": "Cambia codice", "pt": "Alterar senha", "nl": "Toegangscode wijzigen", "no": "Endre passord"
    },
    "ChannelAdministrators": {
        "de": "Administratoren", "fr": "Administrateurs", "es": "Administradores",
        "it": "Amministratori", "pt": "Administradores", "nl": "Beheerders", "no": "Administratorer"
    },
    "ChannelMembers": {
        "de": "Mitglieder", "fr": "Membres", "es": "Miembros",
        "it": "Membri", "pt": "Membros", "nl": "Leden", "no": "Medlemmer"
    },
    "ChannelPermissions": {
        "de": "Berechtigungen", "fr": "Autorisations", "es": "Permisos",
        "it": "Permessi", "pt": "Permissões", "nl": "Machtigingen", "no": "Tillatelser"
    },
    "Clear": {
        "de": "Leeren", "fr": "Effacer", "es": "Vaciar",
        "it": "Cancella", "pt": "Limpar", "nl": "Wissen", "no": "Tøm"
    },
    "ClearMessageDatabase": {
        "de": "Nachrichtendatenbank leeren", "fr": "Effacer la base de données", "es": "Borrar base de datos de mensajes",
        "it": "Cancella database messaggi", "pt": "Limpar banco de dados de mensagens", "nl": "Berichtendatabase wissen", "no": "Tøm meldingsdatabase"
    },
    "ClearMessageDatabaseNotification": {
        "de": "Nachrichtendatenbank geleert.", "fr": "Base de données des messages effacée.", "es": "Base de datos de mensajes borrada.",
        "it": "Database dei messaggi cancellato.", "pt": "Banco de dados de mensagens limpo.", "nl": "Berichtendatabase gewist.", "no": "Meldingsdatabasen er tømt."
    },
    "Copy": {
        "de": "Kopieren", "fr": "Copier", "es": "Copiar",
        "it": "Copia", "pt": "Copiar", "nl": "Kopiëren", "no": "Kopier"
    },
    "CopyLink": {
        "de": "Link kopieren", "fr": "Copier le lien", "es": "Copiar enlace",
        "it": "Copia link", "pt": "Copiar link", "nl": "Link kopiëren", "no": "Kopier lenke"
    },
    "Deactivate": {
        "de": "JETZT LÖSCHEN", "fr": "SUPPRIMER MAINTENANT", "es": "ELIMINAR AHORA",
        "it": "ELIMINA ORA", "pt": "EXCLUIR AGORA", "nl": "NU VERWIJDEREN", "no": "SLETT NÅ"
    },
    "Default": {
        "de": "Standard", "fr": "Par défaut", "es": "Por defecto",
        "it": "Predefinito", "pt": "Padrão", "nl": "Standaard", "no": "Standard"
    },
    "Delete": {
        "de": "Löschen", "fr": "Supprimer", "es": "Eliminar",
        "it": "Elimina", "pt": "Excluir", "nl": "Verwijderen", "no": "Slett"
    },
    "DirectShare": {
        "de": "Direktes Teilen", "fr": "Partage direct", "es": "Compartir directo",
        "it": "Condivisione diretta", "pt": "Compartilhamento direto", "nl": "Direct delen", "no": "Direkte deling"
    },
    "DirectShareInfo": {
        "de": "Letzte Chats im Android-Freigabemenü anzeigen", "fr": "Afficher les discussions récentes dans le menu de partage",
        "es": "Mostrar chats recientes en el menú de compartir de Android", "it": "Mostra chat recenti nel menu di condivisione",
        "pt": "Mostrar chats recentes no menu de compartilhamento do Android", "nl": "Toon recente chats in het Android-deelmenu",
        "no": "Vis nylige samtaler i Android-delingsmenyen"
    },
    "Disable": {
        "de": "Deaktivieren", "fr": "Désactiver", "es": "Desactivar",
        "it": "Disattiva", "pt": "Desativar", "nl": "Uitschakelen", "no": "Deaktiver"
    },
    "DisablePasscodeTurnOff": {
        "de": "Ausschalten", "fr": "Désactiver", "es": "Desactivar",
        "it": "Disattiva", "pt": "Desativar", "nl": "Uitschakelen", "no": "Slå av"
    },
    "Done": {
        "de": "Fertig", "fr": "Terminé", "es": "Listo",
        "it": "Fatto", "pt": "Pronto", "nl": "Klaar", "no": "Ferdig"
    },
    "DontReadStoriesPackets": {
        "de": "Stories nicht lesen", "fr": "Ne pas lire les stories", "es": "No leer historias",
        "it": "Non leggere le storie", "pt": "Não ler stories", "nl": "Verhalen niet lezen", "no": "Ikke les historier"
    },
    "DontSendOnlinePackets": {
        "de": "Kein Online-Status senden", "fr": "Ne pas envoyer le statut en ligne", "es": "No enviar estado en línea",
        "it": "Non inviare stato online", "pt": "Não enviar status online", "nl": "Geen online status verzenden", "no": "Ikke send online-status"
    },
    "DontSendReadMessagePackets": {
        "de": "Nachrichten nicht lesen", "fr": "Ne pas lire les messages", "es": "No leer mensajes",
        "it": "Non leggere messaggi", "pt": "Não ler mensagens", "nl": "Berichten niet lezen", "no": "Ikke les meldinger"
    },
    "DontSendUploadProgress": {
        "de": "Tipp-Status nicht senden", "fr": "Ne pas envoyer l'état de saisie", "es": "No enviar escribiendo",
        "it": "Non inviare stato di scrittura", "pt": "Não enviar digitando", "nl": "Typstatus niet verzenden", "no": "Ikke send skrivestatus"
    },
    "EP_CustomChat": {
        "de": "Gespeicherte Nachrichten ersetzen", "fr": "Remplacer les messages enregistrés", "es": "Reemplazar mensajes guardados",
        "it": "Sostituisci Messaggi salvati", "pt": "Substituir Mensagens Salvas", "nl": "Opgeslagen berichten vervangen", "no": "Erstatt Lagrede meldinger"
    },
    "EP_CustomChat_Desc": {
        "de": "Wähle den Chat, der anstelle von Gespeicherten Nachrichten verwendet wird.",
        "fr": "Choisissez la discussion utilisée à la place des messages enregistrés.",
        "es": "Elige el chat utilizado en lugar de Mensajes guardados.",
        "it": "Scegli la chat utilizzata invece di Messaggi salvati.",
        "pt": "Escolha o chat usado em vez das Mensagens Salvas.",
        "nl": "Kies de chat die wordt gebruikt in plaats van Opgeslagen berichten.",
        "no": "Velg samtalen som skal brukes i stedet for Lagrede meldinger."
    },
    "EP_CustomChat_Selected_Title": {
        "de": "anstelle von", "fr": "au lieu de", "es": "en lugar de",
        "it": "invece di", "pt": "em vez de", "nl": "in plaats van", "no": "i stedet for"
    },
    "EP_DownloadSpeedBoost": {
        "de": "Schnellere Downloads", "fr": "Téléchargements plus rapides", "es": "Descargas más rápidas",
        "it": "Download più veloci", "pt": "Downloads mais rápidos", "nl": "Snellere downloads", "no": "Raskere nedlastinger"
    },
    "EP_DownloadSpeedBoostExtreme": {
        "de": "Extrem", "fr": "Extrême", "es": "Extremo",
        "it": "Estremo", "pt": "Extremo", "nl": "Extreem", "no": "Ekstrem"
    },
    "EP_NavigationAnimation": {
        "de": "Navigationsanimation", "fr": "Animation de navigation", "es": "Animación de navegación",
        "it": "Animazione navigazione", "pt": "Animação de navegação", "nl": "Navigatie-animatie", "no": "Navigasjonsanimasjon"
    },
    "EP_NavigationAnimationBezier": {
        "de": "Klassisch", "fr": "Classique", "es": "Clásica",
        "it": "Classica", "pt": "Clássica", "nl": "Klassiek", "no": "Klassisk"
    },
    "EP_NavigationAnimationCrossfading": {
        "de": "Sanfter Titelübergang", "fr": "Transition douce du titre", "es": "Transición suave del título",
        "it": "Transizione fluida del titolo", "pt": "Transição suave do título", "nl": "Vloeiende titelovergang", "no": "Jevn tittelovergang"
    },
    "EP_NavigationAnimationSpring": {
        "de": "Federnd", "fr": "Ressort", "es": "Resorte",
        "it": "Molla", "pt": "Mola", "nl": "Verend", "no": "Fjærende"
    },
    "EP_Network": {
        "de": "Netzwerk", "fr": "Réseau", "es": "Red",
        "it": "Rete", "pt": "Rede", "nl": "Netwerk", "no": "Nettverk"
    },
    "EP_PhotosSize": {
        "de": "Große Fotos bis zu 2560 px", "fr": "Grandes photos jusqu'à 2560 px", "es": "Fotos grandes de hasta 2560 px",
        "it": "Foto grandi fino a 2560 px", "pt": "Fotos grandes de até 2560 px", "nl": "Grote foto\'s tot 2560 px", "no": "Store bilder opptil 2560 px"
    },
    "EP_SlowNetworkMode": {
        "de": "Modus für schwaches Netzwerk", "fr": "Mode réseau faible", "es": "Modo de red débil",
        "it": "Modalità rete debole", "pt": "Modo de rede fraca", "nl": "Zwakke netwerkmodus", "no": "Svak nettverksmodus"
    },
    "Edit": {
        "de": "Bearbeiten", "fr": "Modifier", "es": "Editar",
        "it": "Modifica", "pt": "Editar", "nl": "Bewerken", "no": "Rediger"
    },
    "Emoji": {
        "de": "Emoji", "fr": "Émoji", "es": "Emoji",
        "it": "Emoji", "pt": "Emoji", "nl": "Emoji", "no": "Emoji"
    },
    "EnableSaveDeletedMessages": {
        "de": "Gelöschte Nachrichten speichern", "fr": "Enregistrer les messages supprimés", "es": "Guardar mensajes eliminados",
        "it": "Salva messaggi eliminati", "pt": "Salvar mensagens excluídas", "nl": "Verwijderde berichten opslaan", "no": "Lagre slettede meldinger"
    },
    "EnableSaveEditsHistory": {
        "de": "Bearbeitungsverlauf speichern", "fr": "Enregistrer l'historique des modifications", "es": "Guardar historial de ediciones",
        "it": "Salva cronologia modifiche", "pt": "Salvar histórico de edições", "nl": "Bewerkingsgeschiedenis opslaan", "no": "Lagre redigeringshistorikk"
    },
    "ErrorOccurred": {
        "de": "Ein Fehler ist aufgetreten.", "fr": "Une erreur est survenue.", "es": "Ocurrió un error.",
        "it": "Si è verificato un errore.", "pt": "Ocorreu um erro.", "nl": "Er is een fout opgetreden.", "no": "Det oppstod en feil."
    },
    "EventLog": {
        "de": "Letzte Aktionen", "fr": "Actions récentes", "es": "Acciones recientes",
        "it": "Azioni recenti", "pt": "Ações recentes", "nl": "Recente acties", "no": "Nylige handlinger"
    },
    "FilterAddChats": {
        "de": "Chats hinzufügen", "fr": "Ajouter des discussions", "es": "Añadir chats",
        "it": "Aggiungi chat", "pt": "Adicionar chats", "nl": "Chats toevoegen", "no": "Legg til samtaler"
    },
    "FilterAllChats": {
        "de": "Alle Chats", "fr": "Toutes les discussions", "es": "Todos los chats",
        "it": "Tutte le chat", "pt": "Todos os chats", "nl": "Alle chats", "no": "Alle samtaler"
    },
    "FilterBots": {
        "de": "Bots", "fr": "Bots", "es": "Bots",
        "it": "Bot", "pt": "Bots", "nl": "Bots", "no": "Boter"
    },
    "FilterChannels": {
        "de": "Kanäle", "fr": "Canaux", "es": "Canales",
        "it": "Canali", "pt": "Canais", "nl": "Kanalen", "no": "Kanaler"
    },
    "FilterChats": {
        "de": "Chats", "fr": "Discussions", "es": "Chats",
        "it": "Chat", "pt": "Chats", "nl": "Chats", "no": "Samtaler"
    },
    "FilterContacts": {
        "de": "Kontakte", "fr": "Contacts", "es": "Contactos",
        "it": "Contatti", "pt": "Contatos", "nl": "Contacten", "no": "Kontakter"
    },
    "FilterGroups": {
        "de": "Gruppen", "fr": "Groupes", "es": "Grupos",
        "it": "Gruppi", "pt": "Grupos", "nl": "Groepen", "no": "Grupper"
    },
    "FilterNameNonMuted": {
        "de": "Mit Ton", "fr": "Non muet", "es": "No silenciados",
        "it": "Non silenziate", "pt": "Não silenciados", "nl": "Niet gedempt", "no": "Ikke dempet"
    },
    "FilterNameUnread": {
        "de": "Ungelesen", "fr": "Non lus", "es": "No leídos",
        "it": "Non letti", "pt": "Não lidas", "nl": "Ongelezen", "no": "Uleste"
    },
    "FilterNonContacts": {
        "de": "Nicht in Kontakten", "fr": "Non-contacts", "es": "No contactos",
        "it": "Non contatti", "pt": "Não-contatos", "nl": "Niet-contacten", "no": "Ikke-kontakter"
    },
    "FilterStories": {
        "de": "Stories", "fr": "Stories", "es": "Historias",
        "it": "Storie", "pt": "Stories", "nl": "Verhalen", "no": "Historier"
    },
    "Forward": {
        "de": "Weiterleiten", "fr": "Transférer", "es": "Reenviar",
        "it": "Inoltra", "pt": "Encaminhar", "nl": "Doorsturen", "no": "Videresend"
    },
    "ForwardWithoutAuthor": {
        "de": "Ohne Absender weiterleiten", "fr": "Transférer sans auteur", "es": "Reenviar sin autor",
        "it": "Inoltra senza autore", "pt": "Encaminhar sem autor", "nl": "Doorsturen zonder auteur", "no": "Videresend uten forfatter"
    },
    "General": {
        "de": "Allgemein", "fr": "Général", "es": "General",
        "it": "Generale", "pt": "Geral", "nl": "Algemeen", "no": "Generelt"
    },
    "GhostEssentialsHeader": {
        "de": "Geist-Modus", "fr": "Paramètres fantôme", "es": "Ajustes fantasma",
        "it": "Impostazioni fantasma", "pt": "Ajustes fantasma", "nl": "Spookmodus", "no": "Spøkelsesmodus"
    },
    "Info": {
        "de": "Info", "fr": "Info", "es": "Info",
        "it": "Info", "pt": "Informações", "nl": "Info", "no": "Info"
    },
    "LinkCopied": {
        "de": "Link kopiert", "fr": "Lien copié", "es": "Enlace copiado",
        "it": "Link copiato", "pt": "Link copiado", "nl": "Link gekopieerd", "no": "Lenke kopiert"
    },
    "LiteBatteryDisabled": {
        "de": "Aus", "fr": "Désactivé", "es": "Desactivado",
        "it": "Spento", "pt": "Desativado", "nl": "Uit", "no": "Av"
    },
    "LiteBatteryEnabled": {
        "de": "Ein", "fr": "Activé", "es": "Activado",
        "it": "Acceso", "pt": "Ativado", "nl": "Aan", "no": "På"
    },
    "LiteMode": {
        "de": "Animationen", "fr": "Animations", "es": "Animaciones",
        "it": "Animazioni", "pt": "Animações", "nl": "Animaties", "no": "Animasjoner"
    },
    "LocalMiscellaneousCache": {
        "de": "Sonstiges", "fr": "Divers", "es": "Varios",
        "it": "Varie", "pt": "Diversos", "nl": "Diversen", "no": "Diverse"
    },
    "MainTabsChats": {
        "de": "Chats", "fr": "Discussions", "es": "Chats",
        "it": "Chat", "pt": "Chats", "nl": "Chats", "no": "Samtaler"
    },
    "MainTabsProfile": {
        "de": "Profil", "fr": "Profil", "es": "Perfil",
        "it": "Profilo", "pt": "Perfil", "nl": "Profiel", "no": "Profil"
    },
    "Message": {
        "de": "Nachricht", "fr": "Message", "es": "Mensaje",
        "it": "Messaggio", "pt": "Mensagem", "nl": "Bericht", "no": "Melding"
    },
    "MessageSavingSaveMedia": {
        "de": "Medien speichern", "fr": "Enregistrer les médias", "es": "Guardar medios",
        "it": "Salva media", "pt": "Salvar mídia", "nl": "Media opslaan", "no": "Lagre medier"
    },
    "MessageSavingSaveMediaHint": {
        "de": "Klicken für mehr.", "fr": "Cliquez pour en savoir plus.", "es": "Haz clic para más detalles.",
        "it": "Clicca per maggiori informazioni.", "pt": "Clique para saber mais.", "nl": "Klik voor meer.", "no": "Klikk for mer."
    },
    "MessageSavingSaveMediaInPrivateChannels": {
        "de": "… in privaten Kanälen", "fr": "… dans les canaux privés", "es": "… en canales privados",
        "it": "… nei canali privati", "pt": "… em canais privados", "nl": "… in privékanalen", "no": "… i private kanaler"
    },
    "MessageSavingSaveMediaInPrivateChats": {
        "de": "… in privaten Chats", "fr": "… dans les chats privés", "es": "… en chats privados",
        "it": "… nelle chat private", "pt": "… em chats privados", "nl": "… in privé-chats", "no": "… i private samtaler"
    },
    "MessageSavingSaveMediaInPrivateGroups": {
        "de": "… in privaten Gruppen", "fr": "… dans les groupes privés", "es": "… en grupos privados",
        "it": "… nei gruppi privati", "pt": "… em grupos privados", "nl": "… in privégroepen", "no": "… i private grupper"
    },
    "MessageSavingSaveMediaInPublicChannels": {
        "de": "… in öffentlichen Kanälen", "fr": "… dans les canaux publics", "es": "… en canales públicos",
        "it": "… nei canali pubblici", "pt": "… em canais públicos", "nl": "… in openbare kanalen", "no": "… i offentlige kanaler"
    },
    "MessageSavingSaveMediaInPublicGroups": {
        "de": "… in öffentlichen Gruppen", "fr": "… dans les groupes publics", "es": "… en grupos públicos",
        "it": "… nei gruppi pubblici", "pt": "… em grupos públicos", "nl": "… in openbare groepen", "no": "… i offentlige grupper"
    },
    "MessagesSettings": {
        "de": "Nachrichten", "fr": "Messages", "es": "Mensajes",
        "it": "Messaggi", "pt": "Mensagens", "nl": "Berichten", "no": "Meldinger"
    },
    "Music": {
        "de": "Musik", "fr": "Musique", "es": "Música",
        "it": "Musica", "pt": "Música", "nl": "Muziek", "no": "Musikk"
    },
    "NM_AP_BottomTabs": {
        "de": "Untere Tabs organisieren", "fr": "Organisateur d'onglets du bas", "es": "Organizador de pestañas inferiores",
        "it": "Organizzatore schede inferiori", "pt": "Organizador de abas inferiores", "nl": "Onderste tabbladen organiseren", "no": "Organiser faner nederst"
    },
    "NM_AP_Folders": {
        "de": "Ordner", "fr": "Dossiers", "es": "Carpetas",
        "it": "Cartelle", "pt": "Pastas", "nl": "Mappen", "no": "Mapper"
    },
    "NM_AvatarCorners": {
        "de": "Avatar-Ecken", "fr": "Coins des avatars", "es": "Esquinas de avatares",
        "it": "Angoli avatar", "pt": "Cantos dos avatares", "nl": "Avatarhoeken", "no": "Avatarhjørner"
    },
    "NM_AvatarCornersCircle": {
        "de": "Kreis", "fr": "Cercle", "es": "Círculo",
        "it": "Cerchio", "pt": "Círculo", "nl": "Cirkel", "no": "Sirkel"
    },
    "NM_AvatarCornersSquare": {
        "de": "Quadrat", "fr": "Carré", "es": "Cuadrado",
        "it": "Quadrato", "pt": "Quadrado", "nl": "Vierkant", "no": "Kvadrat"
    },
    "NM_AvatarCorners_Header": {
        "de": "Avatare", "fr": "Avatars", "es": "Avatares",
        "it": "Avatar", "pt": "Avatares", "nl": "Avatars", "no": "Avatater"
    },
    "NM_BAN_AvatarBanner": {
        "de": "Banner aus Avatar", "fr": "Bannière depuis l'avatar", "es": "Banner desde avatar",
        "it": "Banner da avatar", "pt": "Banner do avatar", "nl": "Banner van avatar", "no": "Banner fra avatar"
    },
    "NM_BAN_ChangeGlobal": {
        "de": "Öffentlichen Banner ändern", "fr": "Changer la bannière publique", "es": "Cambiar banner público",
        "it": "Modifica banner pubblico", "pt": "Alterar banner público", "nl": "Openbare banner wijzigen", "no": "Endre offentlig banner"
    },
    "NM_BAN_DeleteLocal": {
        "de": "Lokalen Banner löschen", "fr": "Supprimer la bannière locale", "es": "Eliminar banner local",
        "it": "Elimina banner locale", "pt": "Excluir banner local", "nl": "Lokale banner verwijderen", "no": "Slett lokalt banner"
    },
    "NM_BAN_PickLocal": {
        "de": "Lokalen Banner wählen", "fr": "Choisir une bannière locale", "es": "Elegir banner local",
        "it": "Scegli banner locale", "pt": "Escolher banner local", "nl": "Lokale banner kiezen", "no": "Velg lokalt banner"
    },
    "NM_BAN_RefreshStatus": {
        "de": "Status aktualisieren", "fr": "Actualiser le statut", "es": "Actualizar estado",
        "it": "Aggiorna stato", "pt": "Atualizar status", "nl": "Status vernieuwen", "no": "Oppdater status"
    },
    "NM_BAN_StatusLabel": {
        "de": "Status", "fr": "Statut", "es": "Estado",
        "it": "Stato", "pt": "Status", "nl": "Status", "no": "Status"
    },
    "NM_BAN_SubmitModeration": {
        "de": "Zur Moderation einreichen", "fr": "Soumettre à la modération", "es": "Enviar a moderación",
        "it": "Invia per moderazione", "pt": "Enviar para moderação", "nl": "Indienen voor moderatie", "no": "Send til moderering"
    },
    "NM_BT_EditorFooter": {
        "de": "Tab zum Umschalten antippen. Lang drücken zum Verschieben.",
        "fr": "Appuyez sur un onglet pour basculer. Appui long pour déplacer.",
        "es": "Toca una pestaña para alternar. Mantén pulsado para mover.",
        "it": "Tocca una scheda per alternare. Tieni premuto per spostare.",
        "pt": "Toque em uma aba para alternar. Pressione e segure para mover.",
        "nl": "Tik op een tabblad om te schakelen. Houd ingedrukt om te verplaatsen.",
        "no": "Trykk på en fane for å veksle. Hold inne for å flytte."
    },
    "NM_BT_ForceOpenChats": {
        "de": "Immer in Chats suchen", "fr": "Toujours chercher dans les discussions", "es": "Siempre buscar en chats",
        "it": "Cerca sempre nelle chat", "pt": "Sempre pesquisar em chats", "nl": "Altijd zoeken in chats", "no": "Søk alltid i samtaler"
    },
    "NM_BT_LayoutHeader": {
        "de": "Tabs", "fr": "Onglets", "es": "Pestañas",
        "it": "Schede", "pt": "Abas", "nl": "Tabbladen", "no": "Faner"
    },
    "NM_BT_MoveLeft": {
        "de": "Nach links verschieben", "fr": "Déplacer vers la gauche", "es": "Mover a la izquierda",
        "it": "Sposta a sinistra", "pt": "Mover para a esquerda", "nl": "Naar links verplaatsen", "no": "Flytt til venstre"
    },
    "NM_BT_MoveRight": {
        "de": "Nach rechts verschieben", "fr": "Déplacer vers la droite", "es": "Mover a la derecha",
        "it": "Sposta a destra", "pt": "Mover para a direita", "nl": "Naar rechts verplaatsen", "no": "Flytt til høyre"
    },
    "NM_BT_ShowSearchInTabs": {
        "de": "Suchschaltfläche neben Tabs", "fr": "Bouton de recherche à côté des onglets", "es": "Botón de búsqueda junto a pestañas",
        "it": "Pulsante di ricerca accanto alle schede", "pt": "Botão de pesquisa ao lado das abas", "nl": "Zoekknop naast tabbladen", "no": "Søkeknapp ved faner"
    },
    "NM_BT_ShowSearchInTabs_Desc": {
        "de": "Zeigt eine runde Suchtaste neben den unteren Tabs an.",
        "fr": "Affiche un bouton de recherche rond à côté des onglets du bas.",
        "es": "Muestra un botón de búsqueda redondo junto a las pestañas inferiores.",
        "it": "Mostra un pulsante di ricerca circolare accanto alle schede inferiori.",
        "pt": "Mostra um botão de pesquisa redondo ao lado das abas inferiores.",
        "nl": "Toon een ronde zoekknop naast de onderste tabbladen.",
        "no": "Vis en rund søkeknapp ved siden av fanene nederst."
    },
    "NM_BT_ShowTabs": {
        "de": "Tabs anzeigen", "fr": "Afficher les onglets", "es": "Mostrar pestañas",
        "it": "Mostra schede", "pt": "Mostrar abas", "nl": "Tabbladen tonen", "no": "Vis faner"
    },
    "NM_BT_ShowTabsTitle": {
        "de": "Tab-Titel anzeigen", "fr": "Afficher les titres des onglets", "es": "Mostrar títulos de pestañas",
        "it": "Mostra titoli delle schede", "pt": "Mostrar títulos das abas", "nl": "Titels van tabbladen tonen", "no": "Vis fanetitler"
    },
    "NM_BT_TabDisabled": {
        "de": "Deaktiviert", "fr": "Désactivé", "es": "Desactivado",
        "it": "Disattivato", "pt": "Desativado", "nl": "Uitgeschakeld", "no": "Deaktivert"
    },
    "NM_BT_TabEnabled": {
        "de": "Aktiviert", "fr": "Activé", "es": "Activado",
        "it": "Attivato", "pt": "Ativado", "nl": "Ingeschakeld", "no": "Aktivert"
    },
    "NM_CAM_AskCamera": {
        "de": "Nachfragen", "fr": "Demander", "es": "Preguntar",
        "it": "Chiedi", "pt": "Perguntar", "nl": "Vragen", "no": "Spør"
    },
    "NM_CAM_ContinuousFocus": {
        "de": "Kontinuierlicher Autofokus", "fr": "Autofocus continu", "es": "Enfoque automático continuo",
        "it": "Messa a fuoco automatica continua", "pt": "Foco automático contínuo", "nl": "Continue autofocus", "no": "Kontinuerlig autofokus"
    },
    "NM_CAM_ContinuousFocus_Desc": {
        "de": "Kamera fokussiert während der Videoaufnahme kontinuierlich neu.",
        "fr": "La caméra fait la mise au point en continu pendant l'enregistrement vidéo.",
        "es": "La cámara mantiene el enfoque continuo durante la grabación de vídeo.",
        "it": "La fotocamera mantiene la messa a fuoco continua durante la registrazione.",
        "pt": "A câmera mantém o foco contínuo durante a gravação do vídeo.",
        "nl": "Camera blijft continu scherpstellen tijdens video-opname.",
        "no": "Kameraet holder kontinuerlig fokus under videoopptak."
    },
    "NM_CAM_FrontCamera": {
        "de": "Frontkamera", "fr": "Caméra frontale", "es": "Cámara frontal",
        "it": "Fotocamera frontale", "pt": "Câmera frontal", "nl": "Frontcamera", "no": "Frontkamera"
    },
    "NM_CAM_Improvements": {
        "de": "Verbesserungen", "fr": "Améliorations", "es": "Mejoras",
        "it": "Miglioramenti", "pt": "Melhorias", "nl": "Verbeteringen", "no": "Forbedringer"
    },
    "NM_CAM_OpticalStabilization": {
        "de": "Optische Stabilisierung", "fr": "Stabilisation optique", "es": "Estabilización óptica",
        "it": "Stabilizzazione ottica", "pt": "Estabilização óptica", "nl": "Optische stabilisatie", "no": "Optisk stabilisering"
    },
    "NM_CAM_OpticalStabilization_Desc": {
        "de": "Optische Bildstabilisierung (OIS) verwenden, falls verfügbar.",
        "fr": "Utiliser la stabilisation optique (OIS) lorsqu'elle est disponible.",
        "es": "Utilizar estabilización óptica de imagen (OIS) cuando esté disponible.",
        "it": "Usa la stabilizzazione ottica (OIS) quando disponibile.",
        "pt": "Usar estabilização óptica de imagem (OIS) quando disponível.",
        "nl": "Gebruik optische beeldstabilisatie (OIS) indien beschikbaar.",
        "no": "Bruk optisk bildestabilisering (OIS) når tilgjengelig."
    },
    "NM_CAM_RearCamera": {
        "de": "Rückkamera", "fr": "Caméra arrière", "es": "Cámara trasera",
        "it": "Fotocamera posteriore", "pt": "Câmera traseira", "nl": "Achtercamera", "no": "Bakkamera"
    },
    "NM_CAM_RoundCamera": {
        "de": "Kamera für Videonachrichten", "fr": "Caméra pour messages vidéo", "es": "Cámara para mensajes de vídeo",
        "it": "Fotocamera per videomessaggi", "pt": "Câmera para mensagens de vídeo", "nl": "Camera voor videoberichten", "no": "Kamera for videomeldinger"
    },
    "NM_CAM_RoundVideoBitrate": {
        "de": "Bitrate für Videonachrichten", "fr": "Débit binaire des messages vidéo", "es": "Tasa de bits de mensajes de vídeo",
        "it": "Bitrate videomessaggi", "pt": "Taxa de bits de mensagens de vídeo", "nl": "Bitrate videoberichten", "no": "Bitrate for videomeldinger"
    },
    "NM_CAM_RoundVideoSize": {
        "de": "Videonachrichten-Größe", "fr": "Taille des messages vidéo", "es": "Tamaño de mensajes de vídeo",
        "it": "Dimensione videomessaggi", "pt": "Tamanho das mensagens de vídeo", "nl": "Grootte videoberichten", "no": "Størrelse på videomeldinger"
    },
    "NM_CAM_RoundVideoSize_Auto": {
        "de": "Automatisch", "fr": "Automatique", "es": "Automático",
        "it": "Automatico", "pt": "Automático", "nl": "Automatisch", "no": "Automatisk"
    },
    "NM_CAM_RoundVideoSize_HD": {
        "de": "HD (512p)", "fr": "HD (512p)", "es": "HD (512p)",
        "it": "HD (512p)", "pt": "HD (512p)", "nl": "HD (512p)", "no": "HD (512p)"
    },
    "NM_CAM_RoundVideoSize_SD": {
        "de": "SD (240p)", "fr": "SD (240p)", "es": "SD (240p)",
        "it": "SD (240p)", "pt": "SD (240p)", "nl": "SD (240p)", "no": "SD (240p)"
    },
    "NM_CAM_RoundVideoSize_STD": {
        "de": "Standard (384p)", "fr": "Standard (384p)", "es": "Estándar (384p)",
        "it": "Standard (384p)", "pt": "Padrão (384p)", "nl": "Standaard (384p)", "no": "Standard (384p)"
    },
    "NM_CAM_VideoQuality": {
        "de": "Videoqualität", "fr": "Qualité vidéo", "es": "Calidad de vídeo",
        "it": "Qualità video", "pt": "Qualidade do vídeo", "nl": "Videokwaliteit", "no": "Videokvalitet"
    },
    "NM_CARDS_AutoScroll": {
        "de": "Automatisches Scrollen", "fr": "Défilement automatique", "es": "Desplazamiento automático",
        "it": "Scorrimento automatico", "pt": "Rolagem automática", "nl": "Automatisch scrollen", "no": "Automatisk rulling"
    },
    "NM_CARDS_Enable": {
        "de": "Aktivieren", "fr": "Activer", "es": "Activar",
        "it": "Abilita", "pt": "Ativar", "nl": "Inschakelen", "no": "Aktiver"
    },
    "NM_CARDS_GeneralHeader": {
        "de": "Allgemein", "fr": "Général", "es": "General",
        "it": "Generale", "pt": "Geral", "nl": "Algemeen", "no": "Generelt"
    },
    "NM_CARDS_InfiniteScroll": {
        "de": "Endloses Scrollen", "fr": "Défilement infini", "es": "Desplazamiento infinito",
        "it": "Scorrimento infinito", "pt": "Rolagem infinita", "nl": "Oneindig scrollen", "no": "Uendelig rulling"
    },
    "NM_CARDS_NameBitcoin": {
        "de": "Bitcoin", "fr": "Bitcoin", "es": "Bitcoin",
        "it": "Bitcoin", "pt": "Bitcoin", "nl": "Bitcoin", "no": "Bitcoin"
    },
    "NM_CARDS_NameGram": {
        "de": "Gram", "fr": "Gram", "es": "Gram",
        "it": "Gram", "pt": "Gram", "nl": "Gram", "no": "Gram"
    },
    "NM_CARDS_NameProxy": {
        "de": "Proxy", "fr": "Proxy", "es": "Proxy",
        "it": "Proxy", "pt": "Proxy", "nl": "Proxy", "no": "Proxy"
    },
    "NM_CARDS_NameStorage": {
        "de": "Speicher", "fr": "Stockage", "es": "Almacenamiento",
        "it": "Archiviazione", "pt": "Armazenamento", "nl": "Opslag", "no": "Lagring"
    },
    "NM_CARDS_NameUsd": {
        "de": "US-Dollar", "fr": "Dollar américain", "es": "Dólar estadounidense",
        "it": "Dollaro USA", "pt": "Dólar americano", "nl": "Amerikaanse dollar", "no": "Amerikanske dollar"
    },
    "NM_CARDS_NameWeather": {
        "de": "Wetter", "fr": "Météo", "es": "Clima",
        "it": "Meteo", "pt": "Clima", "nl": "Weer", "no": "Vær"
    },
    "NM_CARDS_Title": {
        "de": "Schnellinfo", "fr": "Info rapide", "es": "Información rápida",
        "it": "Informazioni rapide", "pt": "Informações rápidas", "nl": "Snelle info", "no": "Hurtiginfo"
    },
    "NM_CH_RecentEmojisStickers": {
        "de": "Zuletzt verwendete Emojis und Sticker", "fr": "Émojis et autocollants récents", "es": "Emojis y stickers recientes",
        "it": "Emoji e sticker recenti", "pt": "Emojis e stickers recentes", "nl": "Recente emoji en stickers", "no": "Nylige emoji og klistremerker"
    },
    "NM_CH_RecentEmojisStickers_Desc": {
        "de": "Erhöhen, um mehr kürzlich verwendete Emojis und Sticker zu speichern.",
        "fr": "Augmentez pour conserver plus d'émojis et d'autocollants récents.",
        "es": "Aumenta para mantener más emojis y stickers recientes en el panel.",
        "it": "Aumenta per mantenere più emoji e sticker recenti.",
        "pt": "Aumente para manter mais emojis e stickers recentes no painel.",
        "nl": "Verhoog om meer recent gebruikte emoji en stickers te bewaren.",
        "no": "Øk for å beholde flere nylig brukte emoji og klistremerker."
    },
    "NM_CP_CenterTitleInChat": {
        "de": "Zentrierter Chat-Titel", "fr": "Titre du chat centré", "es": "Título de chat centrado",
        "it": "Titolo della chat centrato", "pt": "Título do chat centralizado", "nl": "Gecentreerde chattitel", "no": "Sentrert samtaleoverskrift"
    },
    "NM_CP_Header_ChatView": {
        "de": "Chat-Ansicht", "fr": "Vue de discussion", "es": "Vista de chat",
        "it": "Vista chat", "pt": "Visualização do chat", "nl": "Chatweergave", "no": "Samtalevisning"
    },
    "NM_CP_Header_Interaction": {
        "de": "Interaktion", "fr": "Interaction", "es": "Interacción",
        "it": "Interazione", "pt": "Interação", "nl": "Interactie", "no": "Interaksjon"
    },
    "NM_CP_Header_Media": {
        "de": "Medien", "fr": "Médias", "es": "Medios",
        "it": "Media", "pt": "Mídia", "nl": "Media", "no": "Medier"
    },
    "NM_CP_LatexRendering": {
        "de": "LaTeX-Formeln rendern", "fr": "Rendu des formules LaTeX", "es": "Renderizar fórmulas LaTeX",
        "it": "Renderizza formule LaTeX", "pt": "Renderizar fórmulas LaTeX", "nl": "LaTeX-formules renderen", "no": "Gjengi LaTeX-formler"
    },
    "NM_CP_LatexRendering_Desc": {
        "de": "Formeln innerhalb von $...$ in Nachrichten erkennen.",
        "fr": "Reconnaître les formules dans $...$ dans les messages.",
        "es": "Reconocer fórmulas dentro de $...$ en mensajes.",
        "it": "Riconosci formule all'interno di $...$ nei messaggi.",
        "pt": "Reconhecer fórmulas dentro de $...$ nas mensagens.",
        "nl": "Herken formules binnen $...$ in berichten.",
        "no": "Gjenkjenn formler innenfor $...$ i meldinger."
    },
    "NM_CP_PreReformRussian": {
        "de": "Vorreform-Russisch", "fr": "Russe pré-réforme", "es": "Ortografía rusa prerreforma",
        "it": "Ortografia russa pre-riforma", "pt": "Ortografia russa pré-reforma", "nl": "Pre-hervorming Russisch", "no": "Før-reforms russisk"
    },
    "NM_CP_PreReformRussian_Desc": {
        "de": "Ausgehende Nachrichten in der Rechtschreibung vor 1918.",
        "fr": "Messages sortants dans l'orthographe d'avant 1918.",
        "es": "Mensajes salientes en ortografía anterior a 1918.",
        "it": "Messaggi in uscita con l'ortografia precedente al 1918.",
        "pt": "Mensagens enviadas na ortografia anterior a 1918.",
        "nl": "Uitgaande berichten in spelling van vóór 1918.",
        "no": "Utgående meldinger i rettskriving fra før 1918."
    },
    "NM_CP_SnowflakesInChat": {
        "de": "Schnee im Chat", "fr": "Neige dans le chat", "es": "Nieve en el chat",
        "it": "Neve nella chat", "pt": "Neve no chat", "nl": "Sneeuw in de chat", "no": "Snø i samtalen"
    },
    "NM_CP_WeekdayNearDate": {
        "de": "Wochentag beim Datum anzeigen", "fr": "Afficher le jour de la semaine près de la date", "es": "Mostrar día de la semana junto a la fecha",
        "it": "Mostra giorno della settimana vicino alla data", "pt": "Mostrar dia da semana perto da data", "nl": "Weekdag bij datum tonen", "no": "Vis ukedag ved dato"
    },
    "NM_CP_WeekdayNearDate_Desc": {
        "de": "Fügt den Wochentag neben dem Datum im Chat hinzu.",
        "fr": "Ajoute le jour de la semaine à côté de la date dans la discussion.",
        "es": "Añade el día de la semana junto a la fecha en el chat.",
        "it": "Aggiunge il giorno della settimana accanto alla data nella chat.",
        "pt": "Adiciona o dia da semana ao lado da data no chat.",
        "nl": "Voegt de dag van de week toe naast de datum in de chat.",
        "no": "Legger til ukedagen ved siden av datoen i samtalen."
    },
    "NM_Camera_FaceDetection": {
        "de": "Gesichtserkennung", "fr": "Détection des visages", "es": "Detección facial",
        "it": "Rilevamento volti", "pt": "Detecção facial", "nl": "Gezichtsdetectie", "no": "Ansiktsgjenkjenning"
    },
    "NM_Camera_FaceDetection_Desc": {
        "de": "Kamera anweisen, Gesichter bei der Belichtung und Fokussierung zu priorisieren.",
        "fr": "Donner la priorité aux visages pour l'exposition et la mise au point.",
        "es": "Priorizar rostros para autoexposición y autoenfoque al grabar.",
        "it": "Dai priorità ai volti per esposizione e messa a fuoco durante la registrazione.",
        "pt": "Priorizar rostos para autoexposição e foco ao gravar.",
        "nl": "Geef prioriteit aan gezichten bij belichting en scherpstelling.",
        "no": "Prioriter ansikter for autoeksponering og autofokus under opptak."
    },
    "NM_Camera_NoiseReduction": {
        "de": "Rauschunterdrückung", "fr": "Réduction du bruit", "es": "Reducción de ruido",
        "it": "Riduzione del rumore", "pt": "Redução de ruído", "nl": "Ruisreductie", "no": "Støyreduksjon"
    },
    "NM_Camera_NoiseReduction_Desc": {
        "de": "Zeitliche Rauschunterdrückung für höhere Videoqualität anwenden.",
        "fr": "Appliquer une réduction du bruit pour une meilleure qualité d'enregistrement.",
        "es": "Aplicar reducción de ruido temporal para mayor calidad.",
        "it": "Applica riduzione del rumore per una qualità di registrazione superiore.",
        "pt": "Aplicar redução de ruído temporal para maior qualidade.",
        "nl": "Pas ruisreductie toe voor hogere opnamekwaliteit.",
        "no": "Bruk støyreduksjon for høyere opptakskvalitet."
    },
    "NM_Camera_UseHighRange": {
        "de": "Hoher FPS-Bereich", "fr": "Plage de FPS élevée", "es": "Rango de FPS alto",
        "it": "Intervallo FPS elevato", "pt": "Faixa de FPS alta", "nl": "Hoog FPS-bereik", "no": "Høyt FPS-område"
    },
    "NM_Camera_UseHighRange_Desc": {
        "de": "Bereiche 30-60 und 60-60 FPS in der FPS-Auswahl anzeigen.",
        "fr": "Afficher les plages 30-60 et 60-60 FPS dans le sélecteur.",
        "es": "Mostrar rangos de 30-60 y 60-60 FPS en el selector.",
        "it": "Mostra intervalli 30-60 e 60-60 FPS nel selettore.",
        "pt": "Mostrar faixas de 30-60 e 60-60 FPS no seletor.",
        "nl": "Toon 30-60 en 60-60 FPS bereiken in de FPS-kiezer.",
        "no": "Vis områder for 30-60 og 60-60 FPS i FPS-velgeren."
    },
    "NM_Chat_Section": {
        "de": "Chats", "fr": "Discussions", "es": "Chats",
        "it": "Chat", "pt": "Chats", "nl": "Chats", "no": "Samtaler"
    },
    "NM_ChooseFolderIcon": {
        "de": "Icon wählen", "fr": "Choisir une icône", "es": "Elegir un icono",
        "it": "Scegli un'icona", "pt": "Escolher um ícone", "nl": "Kies een pictogram", "no": "Velg et ikon"
    },
    "NM_Config_Error": {
        "de": "Fehlgeschlagen", "fr": "Échec", "es": "Error",
        "it": "Operazione non riuscita", "pt": "Falha", "nl": "Mislukt", "no": "Mislyktes"
    },
    "NM_Config_Export": {
        "de": "Einstellungen exportieren", "fr": "Exporter les paramètres", "es": "Exportar ajustes",
        "it": "Esporta impostazioni", "pt": "Exportar configurações", "nl": "Instellingen exporteren", "no": "Eksporter innstillinger"
    },
    "NM_Config_Exported": {
        "de": "Einstellungen in Datei gespeichert", "fr": "Paramètres enregistrés dans le fichier", "es": "Ajustes guardados en el archivo",
        "it": "Impostazioni salvate nel file", "pt": "Configurações salvas no arquivo", "nl": "Instellingen opgeslagen in bestand", "no": "Innstillinger lagret i fil"
    },
    "NM_Config_Header": {
        "de": "Konfiguration", "fr": "Configuration", "es": "Configuración",
        "it": "Configurazione", "pt": "Configuração", "nl": "Configuratie", "no": "Konfigurasjon"
    },
    "NM_Config_Import": {
        "de": "Einstellungen importieren", "fr": "Importer les paramètres", "es": "Importar ajustes",
        "it": "Importa impostazioni", "pt": "Importar configurações", "nl": "Instellingen importeren", "no": "Importer innstillinger"
    },
    "NM_Config_ImportEmpty": {
        "de": "Ausgewählte Datei ist keine SoundGram-Konfiguration", "fr": "Le fichier sélectionné n'est pas une configuration SoundGram",
        "es": "El archivo seleccionado no es una configuración de SoundGram", "it": "Il file selezionato non è una configurazione SoundGram",
        "pt": "O arquivo selecionado não é uma configuração do SoundGram", "nl": "Geselecteerd bestand is geen SoundGram-configuratie",
        "no": "Valgt fil er ikke en SoundGram-konfigurasjon"
    },
    "NM_Config_Info": {
        "de": "Einstellungen in Datei sichern oder auf anderem Gerät wiederherstellen.",
        "fr": "Enregistrer les paramètres dans un fichier ou les restaurer sur un autre appareil.",
        "es": "Guarda los ajustes en un archivo o restáuralos en otro dispositivo.",
        "it": "Salva le impostazioni in un file o ripristinale su un altro dispositivo.",
        "pt": "Salve as configurações em um arquivo ou restaure-as em outro dispositivo.",
        "nl": "Sla instellingen op in een bestand of herstel ze op een ander apparaat.",
        "no": "Lagre innstillinger i en fil eller gjenopprett dem på en annen enhet."
    },
    "NM_CustomTitle": {
        "de": "App-Titel", "fr": "Titre de l'application", "es": "Título de la app",
        "it": "Titolo dell'app", "pt": "Título do aplicativo", "nl": "App-titel", "no": "App-tittel"
    },
    "NM_DBG_AudioSource": {
        "de": "Mikrofon-Audioquelle", "fr": "Source audio du microphone", "es": "Fuente de audio del micrófono",
        "it": "Sorgente audio microfono", "pt": "Fonte de áudio do microfone", "nl": "Microfoonaudiobron", "no": "Mikrofonaudiokilde"
    },
    "NM_DBG_EditTextFix": {
        "de": "EditText-Vorschläge-Fix", "fr": "Correction des suggestions EditText", "es": "Corrección de sugerencias EditText",
        "it": "Correzione suggerimenti EditText", "pt": "Correção de sugestões do EditText", "nl": "EditText-suggesties corrigeren", "no": "Fiks for EditText-forslag"
    },
    "NM_DBG_ForceCrash": {
        "de": "Absturz erzwingen", "fr": "Forcer le plantage", "es": "Forzar bloqueo",
        "it": "Forza arresto anomalo", "pt": "Forçar falha", "nl": "Crash forceren", "no": "Tving krasj"
    },
    "NM_DBG_ForceCrash_Desc": {
        "de": "Löst einen gezielten Absturz aus, um Absturzberichte zu testen.",
        "fr": "Déclenche un plantage délibéré pour tester les rapports de plantage.",
        "es": "Provoca un bloqueo deliberado para probar los informes.",
        "it": "Genera un arresto anomalo per testare i rapporti.",
        "pt": "Gera uma falha deliberada para testar relatórios.",
        "nl": "Activeert een opzettelijke crash om foutrapportage te testen.",
        "no": "Utløser et krasj for å teste krasjrapportering."
    },
    "NM_DBG_Header_Danger": {
        "de": "Gefahrenbereich", "fr": "Zone dangereuse", "es": "Zona de peligro",
        "it": "Zona pericolosa", "pt": "Zona de perigo", "nl": "Gevarenzone", "no": "Faresone"
    },
    "NM_DBG_HideVideoTimestamp": {
        "de": "Video-Zeitstempel ausblenden", "fr": "Masquer l'horodatage vidéo", "es": "Ocultar marca de tiempo del vídeo",
        "it": "Nascondi timestamp video", "pt": "Ocultar timestamp do vídeo", "nl": "Videotijdstempel verbergen", "no": "Skjul videotidsstempel"
    },
    "NM_DBG_JacksonJSONProvider": {
        "de": "Jackson JSON-Provider verwenden", "fr": "Utiliser le fournisseur Jackson JSON", "es": "Usar proveedor Jackson JSON",
        "it": "Usa provider Jackson JSON", "pt": "Usar provedor Jackson JSON", "nl": "Jackson JSON-provider gebruiken", "no": "Bruk Jackson JSON-leverandør"
    },
    "NM_DBG_OldTimeStyle": {
        "de": "Standard-Zeitstil in Chats", "fr": "Style d'heure standard dans les discussions", "es": "Estilo de hora estándar en chats",
        "it": "Stile orario predefinito nelle chat", "pt": "Estilo de hora padrão nos chats", "nl": "Standaard tijdstijl in chats", "no": "Standard tidsstil i samtaler"
    },
    "NM_DBG_ReplacePunctuation": {
        "de": "Satzzeichen ersetzen", "fr": "Remplacer la ponctuation", "es": "Reemplazar signos de puntuación",
        "it": "Sostituisci segni di punteggiatura", "pt": "Substituir sinais de pontuação", "nl": "Leestekens vervangen", "no": "Erstatt tegnsetting"
    },
    "NM_DBG_SendMaxQuality": {
        "de": "Videos mit max. Qualität senden", "fr": "Envoyer des vidéos en qualité maximale", "es": "Enviar vídeos con máxima calidad",
        "it": "Invia video alla massima qualità", "pt": "Enviar vídeos na qualidade máxima", "nl": "Video\'s verzenden in maximale kwaliteit", "no": "Send videoer med maksimal kvalitet"
    },
    "NM_DBG_ShowAccounts": {
        "de": "Konten im Seitenmenü anzeigen", "fr": "Afficher les comptes dans le menu latéral", "es": "Mostrar cuentas en el menú lateral",
        "it": "Mostra account nel menu laterale", "pt": "Mostrar contas no menu lateral", "nl": "Toon accounts in het zijmenu", "no": "Vis kontoer i skuffmenyen"
    },
    "NM_DBG_ShowRPCErrors": {
        "de": "RPC-Fehler anzeigen", "fr": "Afficher les erreurs RPC", "es": "Mostrar errores RPC",
        "it": "Mostra errori RPC", "pt": "Mostrar erros RPC", "nl": "RPC-fouten tonen", "no": "Vis RPC-feil"
    },
    "NM_DeleteAllFromSelf": {
        "de": "Eigene Nachrichten löschen", "fr": "Supprimer mes messages", "es": "Eliminar mis mensajes",
        "it": "Elimina i miei messaggi", "pt": "Excluir minhas mensagens", "nl": "Eigen berichten verwijderen", "no": "Slett egne meldinger"
    },
    "NM_DisableSendHints": {
        "de": "Sendehinweise deaktivieren", "fr": "Désactiver les conseils d'envoi", "es": "Desactivar sugerencias de envío",
        "it": "Disattiva suggerimenti di invio", "pt": "Desativar dicas de envio", "nl": "Verzendtips uitschakelen", "no": "Deaktiver sendetips"
    },
    "NM_DisableSendHints_Desc": {
        "de": "Hinweise zum Planen und lautlosen Senden ausblenden.",
        "fr": "Masquer les conseils d'envoi programmé et silencieux.",
        "es": "Ocultar avisos de programación y envío silencioso.",
        "it": "Nascondi i suggerimenti per l'invio programmato e silenzioso.",
        "pt": "Ocultar dicas de agendamento e envio silencioso.",
        "nl": "Verberg hints voor plannen en stil verzenden.",
        "no": "Skjul tips om planlegging og stille sending."
    },
    "NM_DoubleTap_EditOrReact": {
        "de": "Eigene bearbeiten, auf fremde reagieren", "fr": "Modifier les miens, réagir aux autres", "es": "Editar propios, reaccionar a otros",
        "it": "Modifica i tuoi, reagisci agli altri", "pt": "Editar próprias, reagir a outras", "nl": "Eigen bewerken, reageren op anderen", "no": "Rediger egne, reager på andres"
    },
    "NM_EXP_DiscussInsteadOfMute": {
        "de": "Diskutieren statt Stummschalten beim Wischen", "fr": "Discuter au lieu de couper le son par glissement", "es": "Debatir en lugar de silenciar al deslizar",
        "it": "Discuti invece di silenziare allo scorrimento", "pt": "Discutir em vez de silenciar ao deslizar", "nl": "Discussieer in plaats van dempen bij vegen", "no": "Diskuter i stedet for å dempe ved sveip"
    },
    "NM_EXP_EdgeToEdge": {
        "de": "Edge-to-Edge-Modus", "fr": "Mode bord à bord", "es": "Modo borde a borde",
        "it": "Modalità edge-to-edge", "pt": "Modo ponta a ponta", "nl": "Rand-tot-rand modus", "no": "Kant-til-kant-modus"
    },
    "NM_EXP_Footer": {
        "de": "Diese Optionen sind experimentell — Fehler sind möglich.",
        "fr": "Ces options sont expérimentales — attendez-vous à des bugs.",
        "es": "Estas opciones son experimentales: espere errores.",
        "it": "Queste opzioni sono sperimentali: potrebbero verificarsi errori.",
        "pt": "Essas opções são experimentais — bugs podem ocorrer.",
        "nl": "Deze opties zijn experimenteel — verwacht bugs.",
        "no": "Disse alternativene er eksperimentelle — forvent feil."
    },
    "NM_EXP_Header_Beta": {
        "de": "Beta-Funktionen", "fr": "Fonctions bêta", "es": "Opciones beta",
        "it": "Funzioni beta", "pt": "Recursos beta", "nl": "Bèta-functies", "no": "Betabrytere"
    },
    "NM_EXP_NoRounding": {
        "de": "Eckenabrundung deaktivieren", "fr": "Désactiver l'arrondi des angles", "es": "Desactivar redondeo de esquinas",
        "it": "Disattiva arrotondamento angoli", "pt": "Desativar cantos arredondados", "nl": "Hoekronding uitschakelen", "no": "Deaktiver hjørneavrunding"
    },
    "NM_EXP_Title": {
        "de": "Experimentell", "fr": "Expérimental", "es": "Experimental",
        "it": "Sperimentale", "pt": "Experimental", "nl": "Experimenteel", "no": "Eksperimentelt"
    },
    "NM_Effects_Header": {
        "de": "Effekte", "fr": "Effets", "es": "Efectos",
        "it": "Effetti", "pt": "Efeitos", "nl": "Effecten", "no": "Effekter"
    },
    "NM_FO_FolderNameInHeader": {
        "de": "Ordnername im Header", "fr": "Nom du dossier dans l'en-tête", "es": "Nombre de carpeta en encabezado",
        "it": "Nome della cartella nell'intestazione", "pt": "Nome da pasta no cabeçalho", "nl": "Mapnaam in koptekst", "no": "Mappenavn i topptekst"
    },
    "NM_FO_FoldersAtBottom": {
        "de": "Ordner unten anzeigen", "fr": "Afficher les dossiers en bas", "es": "Mostrar carpetas abajo",
        "it": "Mostra le cartelle in basso", "pt": "Mostrar pastas embaixo", "nl": "Mappen onderaan tonen", "no": "Vis mapper nederst"
    },
    "NM_FO_FoldersAtBottom_Desc": {
        "de": "Verschiebt Ordner-Tabs an das Ende der Chatliste.",
        "fr": "Déplace les onglets de dossiers vers le bas de la liste des discussions.",
        "es": "Mueve las pestañas de carpetas a la parte inferior de la lista de chats.",
        "it": "Sposta le schede delle cartelle in fondo all'elenco delle chat.",
        "pt": "Move as abas de pastas para a parte inferior da lista de chats.",
        "nl": "Verplaatst maptabbladen naar de onderkant van de chatlijst.",
        "no": "Flytter mappefaner til bunnen av samtaliseten."
    },
    "NM_FO_TabModeIcon": {
        "de": "Nur Symbole", "fr": "Icônes seulement", "es": "Solo iconos",
        "it": "Solo icone", "pt": "Apenas ícones", "nl": "Alleen pictogrammen", "no": "Bare ikoner"
    },
    "NM_FO_TabModeMix": {
        "de": "Symbole mit Titeln", "fr": "Icônes avec titres", "es": "Iconos con títulos",
        "it": "Icone con titoli", "pt": "Ícones com títulos", "nl": "Pictogrammen met titels", "no": "Ikoner med titler"
    },
    "NM_FO_TabModeText": {
        "de": "Nur Titel", "fr": "Titres seulement", "es": "Solo títulos",
        "it": "Solo titoli", "pt": "Apenas títulos", "nl": "Alleen titels", "no": "Bare titler"
    },
    "NM_FO_TabStyle": {
        "de": "Tab-Stil", "fr": "Style d'onglet", "es": "Estilo de pestaña",
        "it": "Stile scheda", "pt": "Estilo da aba", "nl": "Tabstijl", "no": "Fanestil"
    },
    "NM_FO_TabStyleStroke": {
        "de": "Rahmen hinzufügen", "fr": "Ajouter une bordure", "es": "Añadir borde",
        "it": "Aggiungi bordo", "pt": "Adicionar contorno", "nl": "Omlijning toevoegen", "no": "Legg til kantlinje"
    },
    "NM_FO_TabStyleStroke_Desc": {
        "de": "Hebt den aktiven Tab mit einer Kontur hervor.",
        "fr": "Met en surbrillance l'onglet actif avec un contour.",
        "es": "Resalta la pestaña activa con un contorno.",
        "it": "Evidenzia la scheda attiva con un contorno.",
        "pt": "Destaca a aba ativa com um contorno.",
        "nl": "Markeert het actieve tabblad met een omtrek.",
        "no": "Fremhever den aktive fanen med et omriss."
    },
    "NM_FO_TabsHideAllChats": {
        "de": "„Alle Chats“-Tab ausblenden", "fr": "Masquer l'onglet « Tous les chats »", "es": "Ocultar pestaña «Todos los chats»",
        "it": "Nascondi scheda «Tutte le chat»", "pt": "Ocultar aba «Todos os chats»", "nl": "Tabblad „Alle chats” verbergen", "no": "Skjul fanen «Alle samtaler»"
    },
    "NM_FO_TabsNoCounter": {
        "de": "Zähler auf Tabs ausblenden", "fr": "Masquer le compteur sur les onglets", "es": "Ocultar contador en pestañas",
        "it": "Nascondi contatore nelle schede", "pt": "Ocultar contador nas abas", "nl": "Teller op tabbladen verbergen", "no": "Skjul teller på faner"
    },
    "NM_ForceBlur": {
        "de": "Oberflächen-Weichzeichnung", "fr": "Flou de l'interface", "es": "Desenfoque de interfaz",
        "it": "Sfocatura dell'interfaccia", "pt": "Desfoque de interface", "nl": "Interfacevervaging", "no": "Grensesnittuskarphet"
    },
    "NM_ForumAvatarsLikeChats": {
        "de": "Form der Forenavatare", "fr": "Forme des avatars de forum", "es": "Forma de avatares en foros",
        "it": "Forma degli avatar nei forum", "pt": "Formato de avatares em fóruns", "nl": "Forumavatar-vorm", "no": "Form på forumavatarer"
    },
    "NM_ForumAvatarsLikeChats_Desc": {
        "de": "Gleiche Ecken wie bei Chat-Avataren verwenden.",
        "fr": "Utiliser les mêmes angles que les avatars de discussion.",
        "es": "Usar las mismas esquinas que los avatares de chat.",
        "it": "Usa gli stessi angoli degli avatar della chat.",
        "pt": "Usar os mesmos cantos dos avatares de chat.",
        "nl": "Gebruik dezelfde hoeken als chat-avatars.",
        "no": "Bruk samme hjørner som chat-avatarer."
    },
    "NM_GEN_DeletedGifts": {
        "de": "Gelöschte Stern-Geschenke anzeigen", "fr": "Afficher les cadeaux Star supprimés", "es": "Mostrar regalos de estrellas eliminados",
        "it": "Mostra regali Star eliminati", "pt": "Mostrar presentes Star excluídos", "nl": "Verwijderde stergeschenken tonen", "no": "Vis slettede stjernegaver"
    },
    "NM_GEN_DeletedGifts_Desc": {
        "de": "Fügt versteckte Geschenke wieder in den Katalog ein.",
        "fr": "Réintègre les cadeaux masqués dans le catalogue pour pouvoir les envoyer.",
        "es": "Reincorpora regalos ocultos al catálogo para enviarlos.",
        "it": "Reintroduce i regali nascosti nel catalogo per consentirne l'invio.",
        "pt": "Reinsere presentes ocultos no catálogo para você enviá-los.",
        "nl": "Voegt verborgen geschenken opnieuw toe aan de catalogus.",
        "no": "Legger skjulte stjernegaver tilbake i katalogen."
    },
    "NM_GEN_LocalPremiumEmoji": {
        "de": "Lokale Premium-Emojis", "fr": "Émojis premium locaux", "es": "Emojis premium locales",
        "it": "Emoji premium locali", "pt": "Emojis premium locais", "nl": "Lokale premium emoji", "no": "Lokale premium-emoji"
    },
    "NM_GEN_LocalPremiumEmoji_Desc": {
        "de": "Premium-Emojis ohne Premium-Abonnement senden.",
        "fr": "Envoyez des émojis premium sans abonnement Premium.",
        "es": "Envía emojis premium sin suscripción Premium.",
        "it": "Invia emoji premium senza abbonamento Premium.",
        "pt": "Envie emojis premium sem uma assinatura Premium.",
        "nl": "Verzend premium emoji zonder Premium-abonnement.",
        "no": "Send premium-emoji uten et Premium-abonnement."
    },
    "NM_GEN_MiscHeader": {
        "de": "Verschiedenes", "fr": "Divers", "es": "Varios",
        "it": "Varie", "pt": "Diversos", "nl": "Diversen", "no": "Forskjellig"
    },
    "NM_GE_UploadSpeedBoost": {
        "de": "Schnelleres Hochladen", "fr": "Téléversements plus rapides", "es": "Subidas más rápidas",
        "it": "Caricamento più veloce", "pt": "Uploads mais rápidos", "nl": "Snellere uploads", "no": "Raskere opplasting"
    },
    "NM_HUB_ForkedFromCherrygramSub": {
        "de": "SoundGram basiert auf dem offiziellen Telegram-Client; Features stammen von Cherrygram.",
        "fr": "SoundGram est basé sur Telegram officiel ; fonctionnalités portées depuis Cherrygram.",
        "es": "SoundGram está basado en Telegram; funciones adaptadas de Cherrygram.",
        "it": "SoundGram è basato su Telegram ufficiale; funzionalità da Cherrygram.",
        "pt": "O SoundGram é baseado no Telegram oficial; recursos portados do Cherrygram.",
        "nl": "SoundGram is gebouwd op Telegram; functies overgenomen van Cherrygram.",
        "no": "SoundGram er bygget på Telegram; funksjoner portert fra Cherrygram."
    },
    "NM_HUB_SourceCode": {
        "de": "Quellcode", "fr": "Code source", "es": "Código fuente",
        "it": "Codice sorgente", "pt": "Código fonte", "nl": "Broncode", "no": "Kildekode"
    },
    "NM_HideActionBarStatus": {
        "de": "Kopfzeilenstatus ausblenden", "fr": "Masquer le statut d'en-tête", "es": "Ocultar estado en barra superior",
        "it": "Nascondi stato nell'intestazione", "pt": "Ocultar status no cabeçalho", "nl": "Koptekststatus verbergen", "no": "Skjul status i topptekst"
    },
    "NM_HideBubbleTail": {
        "de": "Nachrichtenschwänze ausblenden", "fr": "Masquer les queues de bulles", "es": "Ocultar colas de mensajes",
        "it": "Nascondi code dei messaggi", "pt": "Ocultar pontas das mensagens", "nl": "Berichtstaarten verbergen", "no": "Skjul meldingshaler"
    },
    "NM_HideBubbleTail_Desc": {
        "de": "Verwende abgerundete Ecken ohne Nachrichtenschwänze.",
        "fr": "Utiliser des coins arrondis sans queues de message.",
        "es": "Usa esquinas redondeadas sin colas de mensajes.",
        "it": "Usa angoli arrotondati senza code di messaggi.",
        "pt": "Usar cantos arredondados sem pontas de mensagem.",
        "nl": "Gebruik afgeronde hoeken zonder berichtstaarten.",
        "no": "Bruk avrundede hjørner uten meldingshaler."
    },
    "NM_IconPack_DefaultSubtitle": {
        "de": "Standard-Telegram-Icons", "fr": "Icônes Telegram standard", "es": "Iconos estándar de Telegram",
        "it": "Icone standard di Telegram", "pt": "Ícones padrão do Telegram", "nl": "Standaard Telegram-iconen", "no": "Standard Telegram-ikoner"
    },
    "NM_IconPack_DefaultTitle": {
        "de": "Standard", "fr": "Par défaut", "es": "Por defecto",
        "it": "Predefinito", "pt": "Padrão", "nl": "Standaard", "no": "Standard"
    },
    "NM_IconPack_LiquidSubtitle": {
        "de": "Liquid Glass Icon-Paket", "fr": "Pack d'icônes Liquid Glass", "es": "Paquete de iconos Liquid Glass",
        "it": "Pacchetto icone Liquid Glass", "pt": "Pacote de ícones Liquid Glass", "nl": "Liquid Glass icoonpakket", "no": "Liquid Glass ikonpakke"
    },
    "NM_IconPack_LiquidTitle": {
        "de": "Liquid Glass", "fr": "Liquid Glass", "es": "Liquid Glass",
        "it": "Liquid Glass", "pt": "Liquid Glass", "nl": "Liquid Glass", "no": "Liquid Glass"
    },
    "NM_IconPack_PlumpySubtitle": {
        "de": "Plumpy Icon-Paket", "fr": "Pack d'icônes Plumpy", "es": "Paquete de iconos Plumpy",
        "it": "Pacchetto icone Plumpy", "pt": "Pacote de ícones Plumpy", "nl": "Plumpy icoonpakket", "no": "Plumpy ikonpakke"
    },
    "NM_IconPack_PlumpyTitle": {
        "de": "Plumpy", "fr": "Plumpy", "es": "Plumpy",
        "it": "Plumpy", "pt": "Plumpy", "nl": "Plumpy", "no": "Plumpy"
    },
    "NM_IconPack_SolarSubtitle": {
        "de": "Integriertes Solar Icon-Paket", "fr": "Pack d'icônes Solar intégré", "es": "Paquete de iconos Solar integrado",
        "it": "Pacchetto icone Solar integrato", "pt": "Pacote de ícones Solar integrado", "nl": "Ingebouwd Solar icoonpakket", "no": "Innebygd Solar ikonpakke"
    },
    "NM_IconPack_SolarTitle": {
        "de": "Solar", "fr": "Solar", "es": "Solar",
        "it": "Solar", "pt": "Solar", "nl": "Solar", "no": "Solar"
    },
    "NM_JumpToBeginning": {
        "de": "Zum Anfang", "fr": "Au début", "es": "Al principio",
        "it": "All'inizio", "pt": "Para o início", "nl": "Naar het begin", "no": "Til begynnelsen"
    },
    "NM_MI_ClearFromCache": {
        "de": "Aus Cache löschen", "fr": "Effacer du cache", "es": "Borrar de caché",
        "it": "Cancella dalla cache", "pt": "Limpar do cache", "nl": "Wissen uit cache", "no": "Tøm fra hurtigbuffer"
    },
    "NM_MI_CopyPhoto": {
        "de": "Foto kopieren", "fr": "Copier la photo", "es": "Copiar foto",
        "it": "Copia foto", "pt": "Copiar foto", "nl": "Foto kopiëren", "no": "Kopier bilde"
    },
    "NM_MI_CopyPhotoAsSticker": {
        "de": "Foto als Sticker kopieren", "fr": "Copier la photo comme autocollant", "es": "Copiar foto como sticker",
        "it": "Copia foto come sticker", "pt": "Copiar foto como sticker", "nl": "Foto kopiëren als sticker", "no": "Kopier bilde som klistremerke"
    },
    "NM_MI_Details": {
        "de": "Details", "fr": "Détails", "es": "Detalles",
        "it": "Dettagli", "pt": "Detalhes", "nl": "Details", "no": "Detaljer"
    },
    "NM_MI_DownloadSticker": {
        "de": "Sticker herunterladen", "fr": "Télécharger l'autocollant", "es": "Descargar sticker",
        "it": "Scarica sticker", "pt": "Baixar sticker", "nl": "Sticker downloaden", "no": "Last ned klistremerke"
    },
    "NM_MI_ForwardWoAuthorship": {
        "de": "Ohne Urheberschaft weiterleiten", "fr": "Transférer sans auteur", "es": "Reenviar sin autoría",
        "it": "Inoltra senza autore", "pt": "Encaminhar sem autoria", "nl": "Doorsturen zonder auteur", "no": "Videresend uten forfatterskap"
    },
    "NM_MI_ForwardWoCaption": {
        "de": "Ohne Beschriftung weiterleiten", "fr": "Transférer sans légende", "es": "Reenviar sin pie de foto",
        "it": "Inoltra senza didascalia", "pt": "Encaminhar sem legenda", "nl": "Doorsturen zonder bijschrift", "no": "Videresend uten bildetekst"
    },
    "NM_MI_SaveToSaved": {
        "de": "In Gespeicherte Nachrichten sichern", "fr": "Enregistrer dans Messages enregistrés", "es": "Guardar en Mensajes guardados",
        "it": "Salva nei Messaggi salvati", "pt": "Salvar em Mensagens Salvas", "nl": "Opslaan in Opgeslagen berichten", "no": "Lagre i Lagrede meldinger"
    },
    "NM_MM_MessageMenuItems": {
        "de": "Nachrichtenmenü-Elemente", "fr": "Éléments du menu de message", "es": "Elementos del menú de mensaje",
        "it": "Voci del menu messaggio", "pt": "Itens do menu de mensagens", "nl": "Berichtmenu-items", "no": "Meldingsmenyelementer"
    },
    "NM_MP_CustomizeMessage": {
        "de": "Nachricht anpassen", "fr": "Personnaliser le message", "es": "Personalizar mensaje",
        "it": "Personalizza messaggio", "pt": "Personalizar mensagem", "nl": "Bericht aanpassen", "no": "Tilpass melding"
    },
    "NM_MP_CustomizeProfile": {
        "de": "Profil anpassen", "fr": "Personnaliser le profil", "es": "Personalizar perfil",
        "it": "Personalizza profilo", "pt": "Personalizar perfil", "nl": "Profiel aanpassen", "no": "Tilpass profil"
    },
    "NM_MP_DisablePremiumStatuses": {
        "de": "Premium-Status deaktivieren", "fr": "Désactiver les statuts premium", "es": "Desactivar estados premium",
        "it": "Disattiva stati premium", "pt": "Desativar status premium", "nl": "Premium statussen uitschakelen", "no": "Deaktiver premium-statuser"
    },
    "NM_MP_DisablePremiumStatuses_Desc": {
        "de": "Premium-Emoji-Status außerhalb des Seitenmenüs ausblenden",
        "fr": "Masquer les statuts d'émojis Premium en dehors du menu latéral",
        "es": "Ocultar estados de emojis Premium fuera del menú lateral",
        "it": "Nascondi gli stati emoji Premium fuori dal menu laterale",
        "pt": "Ocultar status de emojis Premium fora do menu lateral",
        "nl": "Verberg Premium emoji-statussen buiten het zijmenu",
        "no": "Skjul Premium emoji-statuser utenfor sidemenyen"
    },
    "NM_MP_ProfileBackgroundColor": {
        "de": "Hintergrund im Profil", "fr": "Arrière-plan dans le profil", "es": "Fondo en el perfil",
        "it": "Sfondo nel profilo", "pt": "Fundo no perfil", "nl": "Achtergrond in profiel", "no": "Bakgrunn i profil"
    },
    "NM_MP_ProfileBackgroundEmoji": {
        "de": "Emoji im Profilhintergrund", "fr": "Émoji en arrière-plan du profil", "es": "Emoji en fondo de perfil",
        "it": "Emoji nello sfondo del profilo", "pt": "Emoji no fundo do perfil", "nl": "Emoji in profielachtergrond", "no": "Emoji i profilbakgrunn"
    },
    "NM_MP_ProfileBirthDatePreview": {
        "de": "Geburtsdatum", "fr": "Date de naissance", "es": "Fecha de nacimiento",
        "it": "Data di nascita", "pt": "Data de nascimento", "nl": "Geboortedatum", "no": "Fødselsdato"
    },
    "NM_MP_ProfileBusinessPreview": {
        "de": "Öffnungszeiten und Standort", "fr": "Heures d'ouverture et adresse", "es": "Horario comercial y ubicación",
        "it": "Orari di apertura e posizione", "pt": "Horário comercial e localização", "nl": "Openingstijden en locatie", "no": "Åpningstider og plassering"
    },
    "NM_MP_ProfileChannelPreview": {
        "de": "Persönlicher Kanal", "fr": "Canal personnel", "es": "Canal personal",
        "it": "Canale personale", "pt": "Canal pessoal", "nl": "Persoonlijk kanaal", "no": "Personlig kanal"
    },
    "NM_MP_ReplyBackground": {
        "de": "Hintergrund in Antworten", "fr": "Arrière-plan dans les réponses", "es": "Fondo en respuestas",
        "it": "Sfondo nelle risposte", "pt": "Fundo nas respostas", "nl": "Achtergrond in antwoorden", "no": "Bakgrunn i svar"
    },
    "NM_MP_ReplyBackgroundEmoji": {
        "de": "Emoji in Antworten und Linkvorschau", "fr": "Émoji dans les réponses et aperçus de liens", "es": "Emoji en respuestas y vista previa",
        "it": "Emoji nelle risposte e anteprima link", "pt": "Emoji em respostas e prévias", "nl": "Emoji in antwoorden en linkvoorbeelden", "no": "Emoji i svar og forhåndsvisninger"
    },
    "NM_MP_ReplyCustomColors": {
        "de": "Eigene Farben in Antworten und Linkvorschau", "fr": "Couleurs personnalisées dans les réponses", "es": "Colores personalizados en respuestas",
        "it": "Colori personalizzati nelle risposte", "pt": "Cores personalizadas nas respostas", "nl": "Aangepaste kleuren in antwoorden", "no": "Egendefinerte farger i svar"
    },
    "NM_MP_ShowIdDc": {
        "de": "ID und DC im Profil anzeigen", "fr": "Afficher l'ID et le DC dans le profil", "es": "Mostrar ID y DC en el perfil",
        "it": "Mostra ID e DC nel profilo", "pt": "Mostrar ID e DC no perfil", "nl": "Toon ID en DC in profiel", "no": "Vis ID og DC i profil"
    },
    "NM_MP_ShowSeconds": {
        "de": "Zeit mit Sekunden formatieren", "fr": "Format d'heure avec secondes", "es": "Formatear hora con segundos",
        "it": "Formato ora con secondi", "pt": "Formatar hora com segundos", "nl": "Tijd formatteren met seconden", "no": "Formater tid med sekunder"
    },
    "NM_MP_ShowSeconds_Desc": {
        "de": "12:34 -> 12:34:56", "fr": "12:34 -> 12:34:56", "es": "12:34 -> 12:34:56",
        "it": "12:34 -> 12:34:56", "pt": "12:34 -> 12:34:56", "nl": "12:34 -> 12:34:56", "no": "12:34 -> 12:34:56"
    },
    "NM_MSG_GifSpoilers": {
        "de": "GIFs mit Spoiler senden", "fr": "Envoyer des GIF avec spoiler", "es": "Enviar GIFs con spoiler",
        "it": "Invia GIF con spoiler", "pt": "Enviar GIFs com spoiler", "nl": "GIF\'s met spoiler verzenden", "no": "Send GIF-er med spoiler"
    },
    "NM_MSG_Header_Sending": {
        "de": "Senden", "fr": "Envoi", "es": "Envío",
        "it": "Invio", "pt": "Envio", "nl": "Verzenden", "no": "Sending"
    },
    "NM_MZ_LargerVoiceMessages": {
        "de": "Adaptive Sprachnachrichtenlänge", "fr": "Longueur adaptative des messages vocaux", "es": "Longitud adaptable de notas de voz",
        "it": "Lunghezza adattiva dei messaggi vocali", "pt": "Comprimento adaptável de mensagens de voz", "nl": "Adaptieve spraakberichtlengte", "no": "Tilpasset lengde på talemeldinger"
    },
    "NM_MZ_MediaAmplifier": {
        "de": "Fotos und Videos", "fr": "Photos et vidéos", "es": "Fotos y vídeos",
        "it": "Foto e video", "pt": "Fotos e vídeos", "nl": "Foto\'s en video\'s", "no": "Bilder og videoer"
    },
    "NM_MZ_MediaAmplifier_Hint": {
        "de": "Hinweis: Medien können skaliert werden, um in die Chatbreite zu passen.",
        "fr": "Remarque : les médias peuvent être réduits pour s'adapter à la largeur du chat.",
        "es": "Nota: fotos y vídeos pueden reducirse para ajustarse al ancho del chat.",
        "it": "Nota: i media potrebbero essere ridimensionati alla larghezza della chat.",
        "pt": "Nota: fotos e vídeos podem ser reduzidos para caber na largura do chat.",
        "nl": "Opmerking: media kunnen worden verkleind om binnen de chatbreedte te passen.",
        "no": "Merk: bilder og videoer kan nedskaleres for å passe chatbredden."
    },
    "NM_MediaGlow": {
        "de": "Medienleuchten", "fr": "Lueur des médias", "es": "Resplandor de medios",
        "it": "Bagliore dei media", "pt": "Brilho de mídia", "nl": "Mediaglans", "no": "Medieglød"
    },
    "NM_MediaGlow_Desc": {
        "de": "Medienränder mit einem weichen Unschärfehintergrund füllen.",
        "fr": "Remplir les bords avec un fond flou.",
        "es": "Rellenar bordes de medios con un fondo suave desenfocado.",
        "it": "Riempi i bordi dei media con uno sfondo sfocato.",
        "pt": "Preencher bordas com um fundo suave desfocado.",
        "nl": "Vul mediaranden met een zachte onscherpe achtergrond.",
        "no": "Fyll mediekanter med en myk uskarp bakgrunn."
    },
    "NM_Menu_Haptic": {
        "de": "Haptisches Feedback", "fr": "Retour haptique", "es": "Respuesta háptica",
        "it": "Feedback aptico", "pt": "Feedback tátil", "nl": "Haptische feedback", "no": "Haptisk tilbakemelding"
    },
    "NM_Menu_Haptic_Desc": {
        "de": "Vibrieren, wenn das Menü geöffnet oder durch Wischen geschlossen wird.",
        "fr": "Vibrer à l'ouverture ou la fermeture par glissement du menu.",
        "es": "Vibrar al abrir el menú de mensajes o cerrarlo al deslizar.",
        "it": "Vibra all'apertura del menu o alla chiusura con scorrimento.",
        "pt": "Vibrar quando o menu abre ou é fechado por deslize.",
        "nl": "Trillen wanneer het menu opent of sluit door vegen.",
        "no": "Vibrer når menyen åpnes eller lukkes ved sveip."
    },
    "NM_Menu_Reorder": {
        "de": "Menüpunkte neu anordnen", "fr": "Réorganiser les éléments du menu", "es": "Reordenar elementos del menú",
        "it": "Riordina voci del menu", "pt": "Reordenar itens do menu", "nl": "Menu-items herschikken", "no": "Endre rekkefølge på menyelementer"
    },
    "NM_Menu_Reorder_Reset": {
        "de": "Standardreihenfolge wiederherstellen", "fr": "Rétablir l'ordre par défaut", "es": "Restablecer orden predeterminado",
        "it": "Ripristina ordine predefinito", "pt": "Redefinir ordem padrão", "nl": "Herstel standaardvolgorde", "no": "Tilbakestill til standard rekkefølge"
    },
    "NM_Menu_TelegramPlus": {
        "de": "Modernes Nachrichtenmenü", "fr": "Menu de message moderne", "es": "Menú de mensajes moderno",
        "it": "Menu messaggi moderno", "pt": "Menu de mensagens moderno", "nl": "Modern berichtmenu", "no": "Moderne meldingsmeny"
    },
    "NM_Menu_TelegramPlus_Desc": {
        "de": "Liquid Glass Menü mit gruppierten Aktionen.",
        "fr": "Menu Liquid Glass avec actions groupées.",
        "es": "Menú Liquid Glass con acciones agrupadas.",
        "it": "Menu Liquid Glass con azioni raggruppate.",
        "pt": "Menu Liquid Glass com ações agrupadas.",
        "nl": "Liquid Glass menu met gegroepeerde acties.",
        "no": "Liquid Glass meny med grupperte handlinger."
    },
    "NM_MsgSlideAction": {
        "de": "Aktion beim Wischen über Nachricht", "fr": "Action au glissement sur message", "es": "Acción al deslizar mensaje",
        "it": "Azione scorrimento sul messaggio", "pt": "Ação ao deslizar mensagem", "nl": "Actie bij vegen over bericht", "no": "Handling ved meldingssveip"
    },
    "NM_NM_AutoDownload": {
        "de": "Automatischer Download bei URL-Versand", "fr": "Téléchargement auto à l'envoi d'URL", "es": "Descarga automática al enviar URL",
        "it": "Download automatico all'invio di URL", "pt": "Download automático ao enviar URL", "nl": "Automatisch downloaden bij verzenden van URL", "no": "Last ned automatisk ved sending av URL"
    },
    "NM_NM_AutoDownload_Desc": {
        "de": "Senden einer unterstützten URL startet den Download automatisch.",
        "fr": "L'envoi d'un message avec une URL prise en charge déclenche le téléchargement.",
        "es": "Enviar un mensaje con un enlace compatible inicia la descarga automáticamente.",
        "it": "L'invio di un URL supportato avvia automaticamente il download.",
        "pt": "O envio de uma URL suportada aciona o download automaticamente.",
        "nl": "Het verzenden van een ondersteunde URL start de download automatisch.",
        "no": "Sending av en støttet URL utløser nedlasting automatisk."
    },
    "NM_NM_FormatAudio": {
        "de": "Audio", "fr": "Audio", "es": "Audio",
        "it": "Audio", "pt": "Áudio", "nl": "Audio", "no": "Lyd"
    },
    "NM_NM_FormatVideo": {
        "de": "Video", "fr": "Vidéo", "es": "Vídeo",
        "it": "Video", "pt": "Vídeo", "nl": "Video", "no": "Video"
    },
    "NM_NM_YtAsk": {
        "de": "Nach YouTube-Format fragen", "fr": "Demander le format YouTube", "es": "Preguntar formato de YouTube",
        "it": "Chiedi formato YouTube", "pt": "Perguntar formato do YouTube", "nl": "Vraag YouTube-formaat", "no": "Spør om YouTube-format"
    },
    "NM_NotificationReactionEmoji": {
        "de": "Emoji", "fr": "Émoji", "es": "Emoji",
        "it": "Emoji", "pt": "Emoji", "nl": "Emoji", "no": "Emoji"
    },
    "NM_NotificationReactions": {
        "de": "Reaktionen in Benachrichtigungen", "fr": "Réactions dans les notifications", "es": "Reacciones en notificaciones",
        "it": "Reazioni nelle notifiche", "pt": "Reações em notificações", "nl": "Reacties in meldingen", "no": "Reaksjoner i varsler"
    },
    "NM_NotificationReactions_Desc": {
        "de": "Fügt eine Schnelltaste für Reaktionen in Benachrichtigungen hinzu.",
        "fr": "Ajoute un bouton de réaction rapide aux notifications.",
        "es": "Añade un botón de reacción rápida a las notificaciones.",
        "it": "Aggiunge un pulsante di reazione rapida alle notifiche.",
        "pt": "Adiciona um botão de reação rápida às notificações.",
        "nl": "Voegt een snelle reactieknop toe aan meldingen.",
        "no": "Legger til en hurtigreaksjonsknapp i varsler."
    },
    "NM_OnlineIndicatorInGroups": {
        "de": "Online-Anzeige in Gruppen", "fr": "Indicateur en ligne dans les groupes", "es": "Indicador en línea en grupos",
        "it": "Indicatore online nei gruppi", "pt": "Indicador online em grupos", "nl": "Online-indicator in groepen", "no": "Online-indikator i grupper"
    },
    "NM_OnlineIndicatorInGroups_Desc": {
        "de": "Einen grünen Online-Punkt auf Gruppen-Avataren anzeigen.",
        "fr": "Afficher un point en ligne sur les avatars de groupe.",
        "es": "Muestra un punto en línea en avatares de grupo.",
        "it": "Mostra un punto online sugli avatar di gruppo.",
        "pt": "Mostra um ponto online nos avatares do grupo.",
        "nl": "Toon een online stip op groepsavatars.",
        "no": "Vis en online-prikk på gruppeavatarer."
    },
    "NM_PR_AllowSystemPasscode": {
        "de": "Gerätesperre zulassen", "fr": "Autoriser le verrouillage de l'appareil", "es": "Permitir bloqueo del dispositivo",
        "it": "Consenti blocco dello schermo", "pt": "Permitir bloqueio de tela do dispositivo", "nl": "Schermvergrendeling toestaan", "no": "Tillat enhetens skjermlås"
    },
    "NM_PR_AllowSystemPasscode_Desc": {
        "de": "PIN, Passwort oder Muster anstelle von Biometrie zulassen.",
        "fr": "Autoriser le code PIN, mot de passe ou schéma au lieu de la biométrie.",
        "es": "Permitir PIN, contraseña o patrón en lugar de biometría.",
        "it": "Consenti PIN, password o sequenza invece della biometria.",
        "pt": "Permitir PIN, senha ou padrão em vez de biometria.",
        "nl": "Pincode, wachtwoord of patroon toestaan in plaats van biometrie.",
        "no": "Tillat PIN, passord eller mønster i stedet for biometri."
    },
    "NM_PR_AskBioOpenArchive": {
        "de": "Archiv schützen", "fr": "Protéger l'archive", "es": "Proteger archivo",
        "it": "Proteggi archivio", "pt": "Proteger arquivo", "nl": "Archief beveiligen", "no": "Beskytt arkiv"
    },
    "NM_PR_AskBioOpenArchive_Desc": {
        "de": "Geräteauthentifizierung vor dem Öffnen des Archivs anfordern.",
        "fr": "Exiger l'authentification de l'appareil avant d'ouvrir l'archive.",
        "es": "Requerir autenticación antes de abrir el archivo.",
        "it": "Richiedi autenticazione prima di aprire l'archivio.",
        "pt": "Exigir autenticação antes de abrir o arquivo.",
        "nl": "Vereis authenticatie voor het openen van het archief.",
        "no": "Krev autentisering før du åpner arkivet."
    },
    "NM_PR_AskBioOpenChats": {
        "de": "Ausgewählte Chats schützen", "fr": "Protéger les discussions sélectionnées", "es": "Proteger chats seleccionados",
        "it": "Proteggi chat selezionate", "pt": "Proteger chats selecionados", "nl": "Geselecteerde chats beveiligen", "no": "Beskytt valgte samtaler"
    },
    "NM_PR_AskBioOpenChats_Desc": {
        "de": "Geräteauthentifizierung vor dem Öffnen ausgewählter Chats anfordern.",
        "fr": "Exiger l'authentification avant d'ouvrir les chats sélectionnés ci-dessous.",
        "es": "Requerir autenticación antes de abrir los chats seleccionados.",
        "it": "Richiedi autenticazione prima di aprire le chat selezionate sotto.",
        "pt": "Exigir autenticação antes de abrir chats selecionados abaixo.",
        "nl": "Vereis authenticatie voor het openen van onderstaande geselecteerde chats.",
        "no": "Krev autentisering før du åpner valgte samtaler."
    },
    "NM_PR_AskBioOpenEncrypted": {
        "de": "Geheime Chats schützen", "fr": "Protéger les échanges secrets", "es": "Proteger chats secretos",
        "it": "Proteggi chat segrete", "pt": "Proteger chats secretos", "nl": "Geheime chats beveiligen", "no": "Beskytt hemmelige samtaler"
    },
    "NM_PR_AskBioOpenEncrypted_Desc": {
        "de": "Geräteauthentifizierung vor dem Öffnen eines geheimen Chats anfordern.",
        "fr": "Exiger l'authentification avant d'ouvrir une discussion secrète.",
        "es": "Requerir autenticación antes de abrir un chat secreto.",
        "it": "Richiedi autenticazione prima di aprire una chat segreta.",
        "pt": "Exigir autenticação antes de abrir um chat secreto.",
        "nl": "Vereis authenticatie voor het openen van een geheime chat.",
        "no": "Krev autentisering før du åpner en hemmelig samtale."
    },
    "NM_PR_AskBioOpenSavedMessages": {
        "de": "Gespeicherte Nachrichten schützen", "fr": "Protéger les messages enregistrés", "es": "Proteger Mensajes guardados",
        "it": "Proteggi Messaggi salvati", "pt": "Proteger Mensagens Salvas", "nl": "Opgeslagen berichten beveiligen", "no": "Beskytt Lagrede meldinger"
    },
    "NM_PR_AskBioOpenSavedMessages_Desc": {
        "de": "Biometrie oder Geräteauthentifizierung vor dem Öffnen anfordern.",
        "fr": "Exiger la biométrie ou l'authentification avant d'ouvrir les messages enregistrés.",
        "es": "Requerir autenticación antes de abrir Mensajes guardados.",
        "it": "Richiedi autenticazione prima di aprire i Messaggi salvati.",
        "pt": "Exigir autenticação antes de abrir Mensagens Salvas.",
        "nl": "Vereis biometrie of authenticatie voor het openen van Opgeslagen berichten.",
        "no": "Krev autentisering før du åpner Lagrede meldinger."
    },
    "NM_PR_AuthenticationRequired": {
        "de": "Authentifizierung erforderlich", "fr": "Authentification requise", "es": "Autenticación requerida",
        "it": "Autenticazione richiesta", "pt": "Autenticação necessária", "nl": "Authenticatie vereist", "no": "Autentisering kreves"
    },
    "NM_PR_AuthenticationUnavailable": {
        "de": "Authentifizierung nicht verfügbar", "fr": "Authentification indisponible", "es": "Autenticación no disponible",
        "it": "Autenticazione non disponibile", "pt": "Autenticação indisponível", "nl": "Authenticatie niet beschikbaar", "no": "Autentisering utilgjengelig"
    },
    "NM_PR_AuthenticationUnavailable_Desc": {
        "de": "Keine Sperre verfügbar. Diesen Schutz deaktivieren?",
        "fr": "Aucun verrouillage disponible. Désactiver ce paramètre ?",
        "es": "No hay bloqueo disponible. ¿Desactivar esta protección?",
        "it": "Nessun blocco disponibile. Disattivare questa protezione?",
        "pt": "Nenhum bloqueio disponível. Desativar esta proteção?",
        "nl": "Geen schermvergrendeling beschikbaar. Deze beveiliging uitschakelen?",
        "no": "Ingen lås tilgjengelig. Deaktivere denne beskyttelsen?"
    },
    "NM_PR_DeleteAccount": {
        "de": "Telegram-Konto löschen", "fr": "Supprimer le compte Telegram", "es": "Eliminar cuenta de Telegram",
        "it": "Elimina account Telegram", "pt": "Excluir conta do Telegram", "nl": "Telegram-account verwijderen", "no": "Slett Telegram-konto"
    },
    "NM_PR_Header_ChatProtection": {
        "de": "Chat-Schutz", "fr": "Protection des discussions", "es": "Protección de chats",
        "it": "Protezione chat", "pt": "Proteção de chats", "nl": "Chatbeveiliging", "no": "Samtalebeskyttelse"
    },
    "NM_PR_HideArchiveList": {
        "de": "Archiv aus der Chatliste ausblenden", "fr": "Masquer l'archive de la liste des discussions", "es": "Ocultar archivo de la lista de chats",
        "it": "Nascondi archivio dall'elenco chat", "pt": "Ocultar arquivo da lista de chats", "nl": "Archief verbergen in chatlijst", "no": "Skjul arkiv fra samtalelisten"
    },
    "NM_PR_HideArchiveList_Desc": {
        "de": "Archiv aus Liste entfernen. Hier öffnen oder Suche gedrückt halten.",
        "fr": "Retirer l'archive de la liste. Ouvrez-la ici ou maintenez Recherche.",
        "es": "Quitar Archivo de la lista. Ábrelo aquí o mantén pulsado Buscar.",
        "it": "Rimuovi l'archivio dalla lista. Aprilo qui o tieni premuto Cerca.",
        "pt": "Remover Arquivo da lista. Abra aqui ou segure Pesquisar.",
        "nl": "Verwijder archief uit de lijst. Open hier of houd Zoeken ingedrukt.",
        "no": "Fjern arkiv fra listen. Åpne det her eller hold Søk inne."
    },
    "NM_PR_HideArchivedStories": {
        "de": "Archivierte Stories ausblenden", "fr": "Masquer les stories archivées", "es": "Ocultar historias archivadas",
        "it": "Nascondi storie archiviate", "pt": "Ocultar stories arquivados", "nl": "Gearchiveerde verhalen verbergen", "no": "Skjul arkiverte historier"
    },
    "NM_PR_HideArchivedStories_Desc": {
        "de": "Blendet den Bereich für archivierte Stories im Profil aus.",
        "fr": "Masque la section des stories archivées de votre profil.",
        "es": "Oculta la sección de historias archivadas de tu perfil.",
        "it": "Nasconde la sezione delle storie archiviate dal tuo profilo.",
        "pt": "Oculta a seção de stories arquivados do seu perfil.",
        "nl": "Verbergt het gedeelte voor gearchiveerde verhalen in uw profiel.",
        "no": "Skjuler delen med arkiverte historier fra profilen din."
    },
    "NM_PR_HideProxy": {
        "de": "Proxy-Sponsor nicht anzeigen", "fr": "Ne pas afficher le sponsor du proxy", "es": "No mostrar patrocinador del proxy",
        "it": "Non mostrare sponsor del proxy", "pt": "Não mostrar patrocinador do proxy", "nl": "Proxy-sponsor niet tonen", "no": "Ikke vis proxy-sponsor"
    },
    "NM_PR_LockedChats": {
        "de": "Geschützte Chats", "fr": "Discussions protégées", "es": "Chats protegidos",
        "it": "Chat protette", "pt": "Chats protegidos", "nl": "Beveiligde chats", "no": "Beskyttede samtaler"
    },
    "NM_PR_LockedChatsTtl": {
        "de": "Erneut abfragen", "fr": "Demander à nouveau", "es": "Preguntar de nuevo",
        "it": "Chiedi di nuovo", "pt": "Perguntar novamente", "nl": "Opnieuw vragen", "no": "Spør igjen"
    },
    "NM_PR_LockedChatsTtl_15Min": {
        "de": "Nach 15 Minuten", "fr": "Après 15 minutes", "es": "Después de 15 minutos",
        "it": "Dopo 15 minuti", "pt": "Após 15 minutos", "nl": "Na 15 minuten", "no": "Etter 15 minutter"
    },
    "NM_PR_LockedChatsTtl_1Min": {
        "de": "Nach 1 Minute", "fr": "Après 1 minute", "es": "Después de 1 minuto",
        "it": "Dopo 1 minuto", "pt": "Após 1 minuto", "nl": "Na 1 minuut", "no": "Etter 1 minutt"
    },
    "NM_PR_LockedChatsTtl_5Min": {
        "de": "Nach 5 Minuten", "fr": "Après 5 minutes", "es": "Después de 5 minutos",
        "it": "Dopo 5 minuti", "pt": "Após 5 minutos", "nl": "Na 5 minuten", "no": "Etter 5 minutter"
    },
    "NM_PR_LockedChatsTtl_Always": {
        "de": "Immer fragen", "fr": "Toujours demander", "es": "Siempre preguntar",
        "it": "Chiedi sempre", "pt": "Sempre perguntar", "nl": "Altijd vragen", "no": "Spør alltid"
    },
    "NM_PR_LockedChatsTtl_UntilRestart": {
        "de": "Bis zum Neustart", "fr": "Jusqu'au redémarrage", "es": "Hasta reiniciar",
        "it": "Fino al riavvio", "pt": "Até reiniciar", "nl": "Tot herstarten", "no": "Til omstart"
    },
    "NM_PR_OpenArchive": {
        "de": "Archiv öffnen", "fr": "Ouvrir l'archive", "es": "Abrir archivo",
        "it": "Apri archivio", "pt": "Abrir arquivo", "nl": "Archief openen", "no": "Åpne arkiv"
    },
    "NM_PR_RequireBiometricsToDelete": {
        "de": "Löschen bestätigen", "fr": "Confirmer la suppression", "es": "Confirmar eliminación",
        "it": "Conferma eliminazione", "pt": "Confirmar exclusão", "nl": "Verwijdering bevestigen", "no": "Bekreft sletting"
    },
    "NM_PR_RequireBiometricsToDelete_Desc": {
        "de": "Authentifizierung vor dem Löschen von Nachrichten oder Chats anfordern.",
        "fr": "Exiger l'authentification avant de supprimer des messages ou des discussions.",
        "es": "Requerir autenticación antes de eliminar mensajes o chats.",
        "it": "Richiedi autenticazione prima di eliminare messaggi o chat.",
        "pt": "Exigir autenticação antes de excluir mensagens ou chats.",
        "nl": "Vereis authenticatie voor het verwijderen van berichten of chats.",
        "no": "Krev autentisering før du sletter meldinger eller samtaler."
    },
    "NM_PR_TestFingerprint": {
        "de": "Schutz testen", "fr": "Tester la protection", "es": "Probar autenticación",
        "it": "Testa autenticazione", "pt": "Testar autenticação", "nl": "Authenticatie testen", "no": "Test autentisering"
    },
    "NM_PR_TestFingerprint_Desc": {
        "de": "Prüft, ob die Authentifizierung verfügbar ist, und aktualisiert ihren Status.",
        "fr": "Vérifie si l'authentification est disponible et actualise son état.",
        "es": "Comprueba si la autenticación está disponible y actualiza su estado.",
        "it": "Verifica se l'autenticazione è disponibile e ne aggiorna lo stato.",
        "pt": "Verifica se a autenticação está disponível e atualiza seu estado.",
        "nl": "Controleert of authenticatie beschikbaar is en vernieuwt de status.",
        "no": "Sjekker om autentisering er tilgjengelig og oppdaterer statusen."
    },
    "NM_PredictiveBackAnimation": {
        "de": "Prädiktive Zurück-Geste", "fr": "Geste retour prédictif", "es": "Gesto de atrás predictivo",
        "it": "Gesto indietro predittivo", "pt": "Gesto preditivo de voltar", "nl": "Voorspellend terug-gebaar", "no": "Forutseende tilbakebevegelse"
    },
    "NM_PremiumReactionRequired": {
        "de": "Telegram Premium erforderlich", "fr": "Telegram Premium requis", "es": "Se requiere Telegram Premium",
        "it": "Telegram Premium richiesto", "pt": "Telegram Premium necessário", "nl": "Telegram Premium vereist", "no": "Telegram Premium kreves"
    },
    "NM_ResidentNotification": {
        "de": "Dauerhafte Benachrichtigung", "fr": "Notification persistante", "es": "Notificación persistente",
        "it": "Notifica persistente", "pt": "Notificação persistente", "nl": "Permanente melding", "no": "Vedvarende varsel"
    },
    "NM_Restart": {
        "de": "Neu starten", "fr": "Redémarrer", "es": "Reiniciar",
        "it": "Riavvia", "pt": "Reiniciar", "nl": "Herstarten", "no": "Start på nytt"
    },
    "NM_RestartRequired": {
        "de": "Neustart erforderlich, um Änderungen anzuwenden", "fr": "Redémarrage requis pour appliquer les modifications", "es": "Reinicio necesario para aplicar cambios",
        "it": "Riavvio richiesto per applicare le modifiche", "pt": "Reinicialização necessária para aplicar alterações", "nl": "Herstart vereist om wijzigingen toe te passen", "no": "Omstart kreves for å bruke endringene"
    },
    "NM_SettingsDesc_NavigationCrossfade": {
        "de": "Überblendanimation beim Wechseln von Bildschirmen", "fr": "Animation de fondu enchaîné lors du changement d'écran", "es": "Animación de transición al cambiar de pantalla",
        "it": "Animazione di dissolvenza incrociata tra le schermate", "pt": "Animação de crossfade ao alternar telas", "nl": "Overvloeianimatie bij wisselen van scherm", "no": "Overtoningsanimasjon ved skjermbytte"
    },
    "NM_StickerSize": {
        "de": "Stickergröße", "fr": "Taille des autocollants", "es": "Tamaño de stickers",
        "it": "Dimensione sticker", "pt": "Tamanho dos stickers", "nl": "Stickergrootte", "no": "Klistremerkestørrelse"
    },
    "NM_StickerSizeBig": {
        "de": "Groß", "fr": "Grand", "es": "Grande",
        "it": "Grande", "pt": "Grande", "nl": "Groot", "no": "Stor"
    },
    "NM_StickerSizePreviewReply": {
        "de": "zum Mond 🚀", "fr": "vers la lune 🚀", "es": "a la luna 🚀",
        "it": "verso la luna 🚀", "pt": "para a lua 🚀", "nl": "naar de maan 🚀", "no": "til månen 🚀"
    },
    "NM_StickerSizePreviewText": {
        "de": "hahaha 😂", "fr": "hahaha 😂", "es": "jajaja 😂",
        "it": "hahaha 😂", "pt": "hahaha 😂", "nl": "hahaha 😂", "no": "hahaha 😂"
    },
    "NM_StickerSizeSmall": {
        "de": "Klein", "fr": "Petit", "es": "Pequeño",
        "it": "Piccolo", "pt": "Pequeno", "nl": "Klein", "no": "Liten"
    },
    "NM_Stickers_Header": {
        "de": "Sticker", "fr": "Autocollants", "es": "Stickers",
        "it": "Sticker", "pt": "Stickers", "nl": "Stickers", "no": "Klistremerker"
    },
    "NM_SwitchStyle": {
        "de": "Schalterstil", "fr": "Style d'interrupteur", "es": "Estilo de interruptor",
        "it": "Stile interruttore", "pt": "Estilo de chave", "nl": "Schakelaarstijl", "no": "Bryterstil"
    },
    "NM_ToSaved": {
        "de": "Speichern", "fr": "Enregistrer", "es": "Guardar",
        "it": "Salva", "pt": "Salvar", "nl": "Opslaan", "no": "Lagre"
    },
    "NM_Vibrate_Click": {
        "de": "Klick", "fr": "Clic", "es": "Clic",
        "it": "Clic", "pt": "Clique", "nl": "Klik", "no": "Klikk"
    },
    "NM_Vibrate_Keyboard": {
        "de": "Tastaturanschlag", "fr": "Touche du clavier", "es": "Toque de teclado",
        "it": "Tocco tastiera", "pt": "Toque no teclado", "nl": "Toetsenbordtik", "no": "Tastaturberøring"
    },
    "NM_Vibrate_Long": {
        "de": "Langes Drücken", "fr": "Appui long", "es": "Pulsación larga",
        "it": "Pressione prolungata", "pt": "Pressão longa", "nl": "Lang indrukken", "no": "Langt trykk"
    },
    "NM_Vibrate_Off": {
        "de": "Aus", "fr": "Désactivé", "es": "Desactivado",
        "it": "Disattivato", "pt": "Desativado", "nl": "Uit", "no": "Av"
    },
    "NM_Vibrate_Wave": {
        "de": "Welle", "fr": "Vague", "es": "Ola",
        "it": "Onda", "pt": "Onda", "nl": "Golf", "no": "Bølge"
    },
    "NM_WSB_Enable": {
        "de": "Bypass aktivieren", "fr": "Activer le contournement", "es": "Activar bypass",
        "it": "Abilita bypass", "pt": "Ativar bypass", "nl": "Bypass inschakelen", "no": "Aktiver omgåelse"
    },
    "NM_WSB_MiscHeader": {
        "de": "Anrufe und VPN", "fr": "Appels et VPN", "es": "Llamadas y VPN",
        "it": "Chiamate e VPN", "pt": "Chamadas e VPN", "nl": "Oproepen en VPN", "no": "Anrop og VPN"
    },
    "NM_WSB_OpenProxySettings": {
        "de": "Telegram-Proxy-Einstellungen öffnen", "fr": "Ouvrir les paramètres proxy de Telegram", "es": "Abrir ajustes de proxy de Telegram",
        "it": "Apri impostazioni proxy di Telegram", "pt": "Abrir configurações de proxy do Telegram", "nl": "Telegram-proxy-instellingen openen", "no": "Åpne Telegram proxy-innstillinger"
    },
    "NM_WSB_Status_Off": {
        "de": "Aus", "fr": "Désactivé", "es": "Desactivado",
        "it": "Disattivato", "pt": "Desativado", "nl": "Uit", "no": "Av"
    },
    "NM_WSB_SuspendOnVpn": {
        "de": "Bei VPN pausieren", "fr": "Suspendre sous VPN", "es": "Suspender bajo VPN",
        "it": "Sospendi sotto VPN", "pt": "Suspender sob VPN", "nl": "Onderbreken bij VPN", "no": "Sett på pause ved VPN"
    },
    "NM_WSB_SuspendOnVpn_Desc": {
        "de": "Den Bypass anhalten, während ein System-VPN aktiv ist.",
        "fr": "Suspendre le contournement lorsqu'un VPN système est actif.",
        "es": "Pausar el bypass mientras haya una VPN del sistema activa.",
        "it": "Metti in pausa il bypass mentre è attiva una VPN di sistema.",
        "pt": "Pausar o bypass enquanto uma VPN do sistema estiver ativa.",
        "nl": "Pauzeer de bypass terwijl een systeem-VPN actief is.",
        "no": "Sett omgåelse på pause mens et system-VPN er aktivt."
    },
    "NM_WSB_VoIP_Enable": {
        "de": "Anrufe über Bypass leiten", "fr": "Acheminer les appels via le contournement", "es": "Enrutar llamadas por bypass",
        "it": "Instrada chiamate attraverso il bypass", "pt": "Rotear chamadas pelo bypass", "nl": "Oproepen via bypass routeren", "no": "Rut samtaler gjennom omgåelse"
    },
    "NM_Without_Authorship": {
        "de": "ohne Urheberschaft", "fr": "sans auteur", "es": "sin autoría",
        "it": "senza autore", "pt": "sem autoria", "nl": "zonder auteur", "no": "uten forfatterskap"
    },
    "Notifications": {
        "de": "Benachrichtigungen", "fr": "Notifications", "es": "Notificaciones",
        "it": "Notifiche", "pt": "Notificações", "nl": "Meldingen", "no": "Varsler"
    },
    "NotificationsService": {
        "de": "Hintergrunddienst", "fr": "Service de maintien de connexion", "es": "Servicio en segundo plano",
        "it": "Servizio di connessione in background", "pt": "Serviço em segundo plano", "nl": "Verbindingsdienst op achtergrond", "no": "Tilkoblingstjeneste i bakgrunnen"
    },
    "NotificationsSound": {
        "de": "Benachrichtigungston", "fr": "Son de notification", "es": "Sonido de notificación",
        "it": "Suono di notifica", "pt": "Som de notificação", "nl": "Meldingsgeluid", "no": "Varsellyd"
    },
    "NumberUnknown": {
        "de": "Unbekannt", "fr": "Inconnu", "es": "Desconocido",
        "it": "Sconosciuto", "pt": "Desconhecido", "nl": "Onbekend", "no": "Ukjent"
    },
    "OK": {
        "de": "OK", "fr": "OK", "es": "OK",
        "it": "OK", "pt": "OK", "nl": "OK", "no": "OK"
    },
    "Online": {
        "de": "online", "fr": "en ligne", "es": "en línea",
        "it": "in linea", "pt": "online", "nl": "online", "no": "pålogget"
    },
    "PasscodePanicCodeRemove": {
        "de": "Panik-Code ausschalten", "fr": "Désactiver le code de panique", "es": "Desactivar código de pánico",
        "it": "Disattiva codice di panico", "pt": "Desativar código de pânico", "nl": "Paniekcode uitschakelen", "no": "Slå av panikkode"
    },
    "PinMessage": {
        "de": "Anheften", "fr": "Épingler", "es": "Fijar",
        "it": "Fissa", "pt": "Fixar", "nl": "Vastzetten", "no": "Fest"
    },
    "Plugins": {
        "de": "Plugins", "fr": "Plugins", "es": "Plugins",
        "it": "Plugin", "pt": "Plugins", "nl": "Plug-ins", "no": "Programtillegg"
    },
    "PluginsEngine": {
        "de": "Plugin-Engine", "fr": "Moteur de plugins", "es": "Motor de plugins",
        "it": "Motore dei plugin", "pt": "Mecanismo de plugins", "nl": "Plug-in engine", "no": "Programtilleggsmotor"
    },
    "PopupDisabled": {
        "de": "Deaktiviert", "fr": "Désactivé", "es": "Desactivado",
        "it": "Disattivato", "pt": "Desativado", "nl": "Uitgeschakeld", "no": "Deaktivert"
    },
    "ProfileBirthday": {
        "de": "Geburtsdatum", "fr": "Date de naissance", "es": "Fecha de nacimiento",
        "it": "Data di nascita", "pt": "Data de nascimento", "nl": "Geboortedatum", "no": "Fødselsdato"
    },
    "Quality1440Short": {
        "de": "2K", "fr": "2K", "es": "2K",
        "it": "2K", "pt": "2K", "nl": "2K", "no": "2K"
    },
    "QualityAuto": {
        "de": "Auto", "fr": "Auto", "es": "Automático",
        "it": "Auto", "pt": "Automático", "nl": "Auto", "no": "Auto"
    },
    "Reactions": {
        "de": "Reaktionen", "fr": "Réactions", "es": "Reacciones",
        "it": "Reazioni", "pt": "Reações", "nl": "Reacties", "no": "Reaksjoner"
    },
    "Reply": {
        "de": "Antworten", "fr": "Répondre", "es": "Responder",
        "it": "Rispondi", "pt": "Responder", "nl": "Beantwoorden", "no": "Svar"
    },
    "ReportChat": {
        "de": "Melden", "fr": "Signaler", "es": "Reportar",
        "it": "Segnala", "pt": "Denunciar", "nl": "Rapporteren", "no": "Rapporter"
    },
    "RepostToStory": {
        "de": "In Story teilen", "fr": "Republier en story", "es": "Republicar en historia",
        "it": "Ripubblica nella storia", "pt": "Republicar no story", "nl": "Herplaatsen in verhaal", "no": "Del i historie"
    },
    "Reset": {
        "de": "Zurücksetzen", "fr": "Réinitialiser", "es": "Restablecer",
        "it": "Ripristina", "pt": "Redefinir", "nl": "Herstellen", "no": "Tilbakestill"
    },
    "Save": {
        "de": "Speichern", "fr": "Enregistrer", "es": "Guardar",
        "it": "Salva", "pt": "Salvar", "nl": "Opslaan", "no": "Lagre"
    },
    "SaveDeletedMessageForBot": {
        "de": "In Bot-Chats speichern", "fr": "Enregistrer dans les chats de bots", "es": "Guardar en chats de bots",
        "it": "Salva nelle chat dei bot", "pt": "Salvar em chats com bots", "nl": "Opslaan in botchats", "no": "Lagre i botsamtaler"
    },
    "SaveDeletedMessageForBotUser": {
        "de": "Bot-Nachrichten speichern", "fr": "Enregistrer les messages des bots", "es": "Guardar mensajes de bots",
        "it": "Salva i messaggi dei bot", "pt": "Salvar mensagens de bots", "nl": "Berichten van bots opslaan", "no": "Lagre botmeldinger"
    },
    "SaveForNotifications": {
        "de": "Für Benachrichtigungen speichern", "fr": "Enregistrer pour les notifications", "es": "Guardar para notificaciones",
        "it": "Salva per le notifiche", "pt": "Salvar para notificações", "nl": "Opslaan voor meldingen", "no": "Lagre for varsler"
    },
    "SaveToDownloads": {
        "de": "In Downloads speichern", "fr": "Enregistrer dans Téléchargements", "es": "Guardar en Descargas",
        "it": "Salva nei download", "pt": "Salvar em Downloads", "nl": "Opslaan in Downloads", "no": "Lagre i Nedlastinger"
    },
    "SaveToGallery": {
        "de": "In Galerie speichern", "fr": "Enregistrer dans la galerie", "es": "Guardar en la galería",
        "it": "Salva nella galleria", "pt": "Salvar na galeria", "nl": "Opslaan in galerij", "no": "Lagre i galleri"
    },
    "SavedMessages": {
        "de": "Gespeicherte Nachrichten", "fr": "Messages enregistrés", "es": "Mensajes guardados",
        "it": "Messaggi salvati", "pt": "Mensagens Salvas", "nl": "Opgeslagen berichten", "no": "Lagrede meldinger"
    },
    "Search": {
        "de": "Suchen", "fr": "Recherche", "es": "Buscar",
        "it": "Cerca", "pt": "Pesquisar", "nl": "Zoeken", "no": "Søk"
    },
    "Settings": {
        "de": "Einstellungen", "fr": "Paramètres", "es": "Ajustes",
        "it": "Impostazioni", "pt": "Configurações", "nl": "Instellingen", "no": "Innstillinger"
    },
    "SettingsNotifications": {
        "de": "Benachrichtigungen", "fr": "Notifications", "es": "Notificaciones",
        "it": "Notifiche", "pt": "Notificações", "nl": "Meldingen", "no": "Varsler"
    },
    "ShareFile": {
        "de": "Teilen", "fr": "Partager", "es": "Compartir",
        "it": "Condividi", "pt": "Compartilhar", "nl": "Delen", "no": "Del"
    },
    "ShareLink": {
        "de": "Link teilen", "fr": "Partager le lien", "es": "Compartir enlace",
        "it": "Condividi link", "pt": "Compartilhar link", "nl": "Link delen", "no": "Del lenke"
    },
    "StatisticsAndBoosts": {
        "de": "Statistiken und Boosts", "fr": "Statistiques et boosts", "es": "Estadísticas y mejoras",
        "it": "Statistiche e potenziamenti", "pt": "Estatísticas e impulsos", "nl": "Statistieken en boosts", "no": "Statistikk og forsterkninger"
    },
    "StickersName": {
        "de": "Sticker", "fr": "Autocollants", "es": "Stickers",
        "it": "Sticker", "pt": "Stickers", "nl": "Stickers", "no": "Klistremerker"
    },
    "TelegramPremium": {
        "de": "Telegram Premium", "fr": "Telegram Premium", "es": "Telegram Premium",
        "it": "Telegram Premium", "pt": "Telegram Premium", "nl": "Telegram Premium", "no": "Telegram Premium"
    },
    "TosDeclineDeleteAccount": {
        "de": "Achtung: Dadurch wird Ihr Telegram-Konto unwiderruflich gelöscht.",
        "fr": "Attention : cela supprimera définitivement votre compte Telegram.",
        "es": "Advertencia: esto eliminará irreversiblemente tu cuenta de Telegram.",
        "it": "Attenzione: questo cancellerà irreversibilmente il tuo account Telegram.",
        "pt": "Aviso: isso excluirá irreversivelmente sua conta do Telegram.",
        "nl": "Waarschuwing: dit zal uw Telegram-account onherroepelijk verwijderen.",
        "no": "Advarsel: dette vil slette Telegram-kontoen din ugjenkallelig."
    },
    "TranslateMessage": {
        "de": "Übersetzen", "fr": "Traduire", "es": "Traducir",
        "it": "Traduci", "pt": "Traduzir", "nl": "Vertalen", "no": "Oversett"
    },
    "TranslucentDeletedMessages": {
        "de": "Halbtransparente gelöschte Nachrichten", "fr": "Messages supprimés translucides", "es": "Mensajes eliminados translúcidos",
        "it": "Messaggi eliminati traslucidi", "pt": "Mensagens excluídas translúcidas", "nl": "Doorschijnende verwijderde berichten", "no": "Halvgjennomsiktige slettede meldinger"
    },
    "UserColorTabProfile": {
        "de": "Profil", "fr": "Profil", "es": "Perfil",
        "it": "Profilo", "pt": "Perfil", "nl": "Profiel", "no": "Profil"
    },
    "VibrationDisabled": {
        "de": "Deaktiviert", "fr": "Désactivée", "es": "Desactivada",
        "it": "Disattivata", "pt": "Desativada", "nl": "Uitgeschakeld", "no": "Deaktivert"
    },
    "exteraAppName": {
        "de": "SoundGram", "fr": "SoundGram", "es": "SoundGram",
        "it": "SoundGram", "pt": "SoundGram", "nl": "SoundGram", "no": "SoundGram"
    }
}

langs = ["de", "fr", "es", "it", "pt", "nl", "no"]
lang_key_map = {"pt": "pt-rBR"}

out_data = {l: {} for l in langs}

for k in sorted(en_dict.keys()):
    if k in WESTERN_MAP:
        for l in langs:
            out_data[l][k] = WESTERN_MAP[k].get(l, en_dict[k])
    else:
        for l in langs:
            lookup = lang_key_map.get(l, l)
            out_data[l][k] = existing.get(lookup, {}).get(k, en_dict[k])

# Key pt as pt-rBR
result = {}
for l in langs:
    target_name = lang_key_map.get(l, l)
    result[target_name] = out_data[l]

with open("tools/translations_western.json", "w", encoding="utf-8") as out:
    json.dump(result, out, ensure_ascii=False, indent=2)

print("Generated tools/translations_western.json successfully with counts:")
for l, d in result.items():
    print(f"  {l}: {len(d)} keys")
