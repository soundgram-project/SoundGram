package com.radolyn.ayugram.utils;

import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;
import org.telegram.tgnet.tl.TL_stories;

import tw.nekomimi.nekogram.NekoConfig;

/**
 * Ghost Mode utilities adapted 1:1 from Phantomly.
 */
public class AyuGhostUtils {

    private static final int OFFLINE_DELAY_MS = 500;
    public static volatile boolean passRead = false;

    public static Long getDialogId(TLRPC.InputPeer peer) {
        if (peer == null) return null;
        if (peer.chat_id != 0) {
            return -peer.chat_id;
        } else if (peer.channel_id != 0) {
            return -peer.channel_id;
        } else {
            return peer.user_id;
        }
    }

    public static Long getDialogId(TLRPC.InputChannel peer) {
        if (peer == null) return null;
        return -peer.channel_id;
    }

    public static Long getDialogId(TLRPC.TL_inputEncryptedChat peer) {
        if (peer == null) {
            return null;
        }
        return (long) DialogObject.getEncryptedChatId(peer.chat_id);
    }

    public static ConnectionsManager getConnectionsManager() {
        return ConnectionsManager.getInstance(UserConfig.selectedAccount);
    }

    public static MessagesController getMessagesController() {
        return MessagesController.getInstance(UserConfig.selectedAccount);
    }

    public static MessagesStorage getMessagesStorage() {
        return MessagesStorage.getInstance(UserConfig.selectedAccount);
    }

    public static void passReadNext() {
        passRead = true;
    }

    public static void markReadOnServer(int messageId, TLRPC.InputPeer peer, boolean internal) {
        TLObject req;
        if (peer instanceof TLRPC.TL_inputPeerChannel) {
            TLRPC.TL_channels_readHistory request = new TLRPC.TL_channels_readHistory();
            request.channel = MessagesController.getInputChannel(peer);
            request.max_id = messageId;
            req = request;
        } else {
            TLRPC.TL_messages_readHistory request = new TLRPC.TL_messages_readHistory();
            request.peer = peer;
            request.max_id = messageId;
            req = request;
        }

        passRead = true;
        AyuState.setAllowReadPacket(true, 1);
        getConnectionsManager().sendRequest(req, (response, error) -> {
            if (error == null) {
                if (response instanceof TLRPC.TL_messages_affectedMessages res) {
                    getMessagesController().processNewDifferenceParams(-1, res.pts, -1, res.pts_count);
                }
                if (internal) FileLog.d("GhostMode: Read request completed.");
                if (AyuGhostPreferences.getInstantOffline() && !internal) {
                    Utilities.globalQueue.postRunnable(() -> performStatusRequest(true), OFFLINE_DELAY_MS);
                }
            }
        });
    }

    public static void markReadOnServer(MessageObject message, boolean internal) {
        if (message == null || message.messageOwner == null) return;
        int messageId = message.getId();
        long dialogId = message.getDialogId();
        TLRPC.EncryptedChat encryptedChat = getMessagesController().getEncryptedChat(DialogObject.getEncryptedChatId(dialogId));
        TLRPC.InputPeer inputPeer = getMessagesController().getInputPeer(message.messageOwner.peer_id);
        boolean readMessageContents = message.isVoice() || message.isRoundVideo();
        TLObject req;
        if (inputPeer instanceof TLRPC.TL_inputPeerChannel) {
            if (readMessageContents) {
                TLRPC.TL_channels_readMessageContents request = new TLRPC.TL_channels_readMessageContents();
                request.channel = MessagesController.getInputChannel(inputPeer);
                request.id.add(messageId);
                req = request;
            } else {
                TLRPC.TL_channels_readHistory request = new TLRPC.TL_channels_readHistory();
                request.channel = MessagesController.getInputChannel(inputPeer);
                request.max_id = messageId;
                req = request;
            }
        } else if (encryptedChat != null) {
            TLRPC.TL_messages_readEncryptedHistory request = new TLRPC.TL_messages_readEncryptedHistory();
            request.peer = new TLRPC.TL_inputEncryptedChat();
            request.peer.chat_id = encryptedChat.id;
            request.peer.access_hash = encryptedChat.access_hash;
            request.max_date = message.messageOwner.date != 0 ? message.messageOwner.date : getConnectionsManager().getCurrentTime();
            req = request;
        } else if (readMessageContents) {
            TLRPC.TL_messages_readMessageContents request = new TLRPC.TL_messages_readMessageContents();
            request.id.add(messageId);
            req = request;
        } else {
            TLRPC.TL_messages_readHistory request = new TLRPC.TL_messages_readHistory();
            request.peer = inputPeer;
            request.max_id = messageId;
            req = request;
        }

        passRead = true;
        AyuState.setAllowReadPacket(true, 1);
        getConnectionsManager().sendRequest(req, (response, error) -> {
            if (error == null) {
                if (response instanceof TLRPC.TL_messages_affectedMessages res) {
                    getMessagesController().processNewDifferenceParams(-1, res.pts, -1, res.pts_count);
                }
                if (internal) FileLog.d("GhostMode: Read request completed.");
                if (AyuGhostPreferences.getInstantOffline() && !internal) {
                    Utilities.globalQueue.postRunnable(() -> performStatusRequest(true), OFFLINE_DELAY_MS);
                }
            }
        });
    }

    public static void performStatusRequest(Boolean offline) {
        TL_account.updateStatus offlineRequest = new TL_account.updateStatus();
        offlineRequest.offline = offline;

        getConnectionsManager().sendRequest(offlineRequest, (response, error) -> FileLog.d("GhostMode: Status request completed (offline=" + offline + ")."));
    }

    public static InterceptResult interceptRequest(TLObject object, RequestDelegate onCompleteOrig) {
        // 1. Одноразовый ручной пропуск «прочитать»
        if (passRead && isReadMessageRequest(object)) {
            passRead = false;
            return InterceptResult.Proceed(onCompleteOrig);
        }

        // 2. Главный переключатель Ghost Mode (stealth)
        if (!AyuGhostPreferences.isGhostModeActive()) {
            return InterceptResult.Proceed(onCompleteOrig);
        }

        // 3. updateStatus: при instant_offline принудительно выставляем offline = true
        if (object instanceof TL_account.updateStatus updateStatus) {
            if (AyuGhostPreferences.getInstantOffline()) {
                FileLog.d("GhostMode: Forcing offline status in updateStatus request.");
                updateStatus.offline = true;
            }
            return InterceptResult.Proceed(onCompleteOrig);
        }

        Long dialogId = extractDialogId(object);
        boolean applies = dialogId == null || AyuGhostPreferences.shouldApplyGhostToChat(dialogId);

        // 4. Блокировка прочтения сообщений (block_read)
        if (applies && AyuGhostPreferences.getBlockRead() && isReadMessageRequest(object)) {
            if (!AyuState.getAllowReadPacket()) {
                FileLog.d("GhostMode: Blocking read request and sending fake response.");
                sendFakeReadResponse(onCompleteOrig);
                return InterceptResult.Blocked(onCompleteOrig);
            }
        }

        // 5. Блокировка статуса печати (block_typing)
        if (applies && AyuGhostPreferences.getBlockTyping() && isTypingRequest(object)) {
            FileLog.d("GhostMode: Blocking typing status request.");
            return InterceptResult.Blocked(onCompleteOrig);
        }

        // 6. Блокировка просмотра историй (block_stories)
        if (applies && AyuGhostPreferences.getBlockStories() && isReadStoriesRequest(object)) {
            FileLog.d("GhostMode: Blocking story read request.");
            return InterceptResult.Blocked(onCompleteOrig);
        }

        // 7. Блокировка просмотров постов каналов (block_channel_views)
        if (applies && AyuGhostPreferences.getBlockChannelViews() && isChannelViewRequest(object)) {
            FileLog.d("GhostMode: Blocking channel view count request.");
            return InterceptResult.Blocked(onCompleteOrig);
        }

        // 8. Мгновенный оффлайн после активности (_PRESENCE)
        if (AyuGhostPreferences.getInstantOffline() && isPresenceRequest(object)) {
            RequestDelegate effectiveOnComplete = (response, error) -> {
                if (onCompleteOrig != null) {
                    onCompleteOrig.run(response, error);
                }
                Utilities.globalQueue.postRunnable(() -> performStatusRequest(true), OFFLINE_DELAY_MS);
            };
            return InterceptResult.Proceed(effectiveOnComplete);
        }

        return InterceptResult.Proceed(onCompleteOrig);
    }

    public static boolean isChannelViewRequest(TLObject object) {
        return object instanceof TLRPC.TL_messages_getMessagesViews;
    }

    public static boolean isTypingRequest(TLObject object) {
        return object instanceof TLRPC.TL_messages_setTyping
                || object instanceof TLRPC.TL_messages_setEncryptedTyping;
    }

    public static boolean isReadMessageRequest(TLObject object) {
        return object instanceof TLRPC.TL_messages_readHistory
                || object instanceof TLRPC.TL_messages_readEncryptedHistory
                || object instanceof TLRPC.TL_messages_readDiscussion
                || object instanceof TLRPC.TL_channels_readHistory
                || object instanceof TLRPC.TL_messages_markDialogUnread
                || object instanceof TLRPC.TL_messages_readMessageContents
                || object instanceof TLRPC.TL_channels_readMessageContents;
    }

    public static boolean isReadStoriesRequest(TLObject object) {
        return object instanceof TL_stories.TL_stories_readStories
                || object instanceof TL_stories.TL_stories_incrementStoryViews;
    }

    public static boolean isMessageSendRequest(TLObject object) {
        return object instanceof TLRPC.TL_messages_sendMessage
                || object instanceof TLRPC.TL_messages_sendMedia
                || object instanceof TLRPC.TL_messages_sendMultiMedia
                || object instanceof TLRPC.TL_messages_sendInlineBotResult
                || object instanceof TLRPC.TL_messages_sendEncrypted
                || object instanceof TLRPC.TL_messages_sendEncryptedFile
                || object instanceof TLRPC.TL_messages_sendEncryptedMultiMedia
                || object instanceof TLRPC.TL_messages_sendEncryptedService
                || object instanceof TLRPC.TL_messages_sendReaction;
    }

    public static boolean isPresenceRequest(TLObject object) {
        if (object == null) return false;
        if (isMessageSendRequest(object)) return true;
        if (isReadMessageRequest(object)) return true;
        if (isTypingRequest(object)) return true;
        if (isReadStoriesRequest(object)) return true;
        if (object instanceof TLRPC.TL_messages_editMessage) return true;
        if (object instanceof TL_stories.TL_stories_sendStory) return true;
        String name = object.getClass().getSimpleName();
        return name.contains("Call");
    }

    private static Long extractDialogId(TLObject object) {
        if (object instanceof TLRPC.TL_messages_setTyping obj) {
            return getDialogId(obj.peer);
        } else if (object instanceof TLRPC.TL_messages_setEncryptedTyping obj) {
            return getDialogId(obj.peer);
        } else if (object instanceof TLRPC.TL_messages_readHistory obj) {
            return getDialogId(obj.peer);
        } else if (object instanceof TLRPC.TL_messages_readEncryptedHistory obj) {
            return getDialogId(obj.peer);
        } else if (object instanceof TLRPC.TL_messages_readDiscussion obj) {
            return getDialogId(obj.peer);
        } else if (object instanceof TLRPC.TL_messages_sendMessage obj) {
            return getDialogId(obj.peer);
        } else if (object instanceof TLRPC.TL_messages_sendMedia obj) {
            return getDialogId(obj.peer);
        } else if (object instanceof TLRPC.TL_messages_sendMultiMedia obj) {
            return getDialogId(obj.peer);
        } else if (object instanceof TL_stories.TL_stories_readStories obj) {
            return getDialogId(obj.peer);
        } else if (object instanceof TL_stories.TL_stories_incrementStoryViews obj) {
            return getDialogId(obj.peer);
        } else if (object instanceof TLRPC.TL_channels_readHistory obj) {
            return getDialogId(obj.channel);
        } else if (object instanceof TLRPC.TL_channels_readMessageContents obj) {
            return getDialogId(obj.channel);
        } else if (object instanceof TLRPC.TL_messages_getMessagesViews obj) {
            return getDialogId(obj.peer);
        }
        return null;
    }

    private static void sendFakeReadResponse(RequestDelegate onCompleteOrig) {
        var fakeRes = new TLRPC.TL_messages_affectedMessages();
        fakeRes.pts = -1;
        fakeRes.pts_count = 0;
        Utilities.stageQueue.postRunnable(() -> {
            try {
                if (onCompleteOrig != null) {
                    onCompleteOrig.run(fakeRes, null);
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    public static final class InterceptResult {
        public final boolean blockRequest;
        public final RequestDelegate effectiveOnComplete;

        public InterceptResult(boolean blockRequest, RequestDelegate effectiveOnComplete) {
            this.blockRequest = blockRequest;
            this.effectiveOnComplete = effectiveOnComplete;
        }

        public boolean blockRequest() {
            return blockRequest;
        }

        public RequestDelegate effectiveOnComplete() {
            return effectiveOnComplete;
        }

        public static InterceptResult Blocked(RequestDelegate originalOnComplete) {
            return new InterceptResult(true, originalOnComplete);
        }

        public static InterceptResult Proceed(RequestDelegate effectiveOnComplete) {
            return new InterceptResult(false, effectiveOnComplete);
        }
    }
}
