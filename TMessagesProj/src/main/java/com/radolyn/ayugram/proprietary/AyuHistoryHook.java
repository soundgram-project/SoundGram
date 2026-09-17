package com.radolyn.ayugram.proprietary;

import android.text.TextUtils;
import android.util.Pair;
import android.util.SparseArray;

import androidx.collection.LongSparseArray;

import com.radolyn.ayugram.database.entities.DeletedMessageFull;
import com.radolyn.ayugram.database.entities.DeletedMessageReaction;
import com.radolyn.ayugram.messages.AyuMessagesController;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.TLRPC.MessageReplyHeader;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AyuHistoryHook {

    public static void mergeDeletedMessages(int currentAccount, long dialogId, long threadMessageId, boolean isTopic, int loadType, int max_id, int limit, boolean isEnd, ArrayList<MessageObject> objects) {
        if (dialogId == 0 || objects == null) {
            return;
        }
        long clientUserId = UserConfig.getInstance(currentAccount).clientUserId;
        AyuMessagesController ayuController = AyuMessagesController.getInstance();
        if (ayuController == null) {
            return;
        }

        int minId = Integer.MAX_VALUE;
        int maxId = Integer.MIN_VALUE;
        LongSparseArray<MessageObject> existingById = new LongSparseArray<>();
        for (int i = 0; i < objects.size(); i++) {
            MessageObject obj = objects.get(i);
            if (obj == null) continue;
            int mid = obj.getId();
            existingById.put(mid, obj);
            if (!obj.isSending()) {
                if (mid < minId) {
                    minId = mid;
                }
                if (mid > maxId) {
                    maxId = mid;
                }
            }
        }

        boolean isChannelComment = threadMessageId != 0 && !isTopic;
        List<DeletedMessageFull> deletedMessages = null;

        if (loadType == 0) {
            if (max_id == 0) {
                if (!objects.isEmpty()) {
                    long startId = isEnd ? 0 : Math.max(0, minId);
                    long endId = Integer.MAX_VALUE;
                    if (isChannelComment) {
                        deletedMessages = ayuController.getThreadMessages(clientUserId, dialogId, threadMessageId, startId, endId, limit);
                    } else if (isTopic && threadMessageId != 0) {
                        deletedMessages = ayuController.getTopicMessages(clientUserId, dialogId, threadMessageId, startId, endId, limit);
                    } else {
                        deletedMessages = ayuController.getMessages(clientUserId, dialogId, startId, endId, limit);
                    }
                } else {
                    if (isChannelComment) {
                        deletedMessages = ayuController.getThreadMessages(clientUserId, dialogId, threadMessageId, 0, Integer.MAX_VALUE, limit);
                    } else if (isTopic && threadMessageId != 0) {
                        deletedMessages = ayuController.getTopicMessages(clientUserId, dialogId, threadMessageId, 0, Integer.MAX_VALUE, limit);
                    } else {
                        deletedMessages = ayuController.getLatestMessages(clientUserId, dialogId, limit);
                    }
                }
            } else {
                if (!objects.isEmpty()) {
                    long startId = isEnd ? 0 : Math.max(0, minId);
                    long endId = Math.max(1, max_id);
                    if (isChannelComment) {
                        deletedMessages = ayuController.getThreadMessages(clientUserId, dialogId, threadMessageId, startId, endId, limit);
                    } else if (isTopic && threadMessageId != 0) {
                        deletedMessages = ayuController.getTopicMessages(clientUserId, dialogId, threadMessageId, startId, endId, limit);
                    } else {
                        deletedMessages = ayuController.getMessages(clientUserId, dialogId, startId, endId, limit);
                    }
                } else {
                    if (isChannelComment) {
                        deletedMessages = ayuController.getThreadMessages(clientUserId, dialogId, threadMessageId, 0, Math.max(1, max_id), limit);
                    } else if (isTopic && threadMessageId != 0) {
                        deletedMessages = ayuController.getTopicMessages(clientUserId, dialogId, threadMessageId, 0, Math.max(1, max_id), limit);
                    } else {
                        deletedMessages = ayuController.getOlderMessagesBefore(clientUserId, dialogId, max_id, limit);
                    }
                }
            }
        } else if (loadType == 1) {
            long startId = minId != Integer.MAX_VALUE ? Math.max(0, minId) : (max_id > 0 ? max_id : 0);
            long endId = maxId != Integer.MIN_VALUE ? maxId : Integer.MAX_VALUE;
            if (isChannelComment) {
                deletedMessages = ayuController.getThreadMessages(clientUserId, dialogId, threadMessageId, startId, endId, limit);
            } else if (isTopic && threadMessageId != 0) {
                deletedMessages = ayuController.getTopicMessages(clientUserId, dialogId, threadMessageId, startId, endId, limit);
            } else {
                deletedMessages = ayuController.getMessages(clientUserId, dialogId, startId, endId, limit);
            }
        } else {
            long startId = minId != Integer.MAX_VALUE ? Math.max(0, minId) : Math.max(0, max_id - limit / 2);
            long endId = maxId != Integer.MIN_VALUE ? maxId : (max_id + limit / 2);
            if (isChannelComment) {
                deletedMessages = ayuController.getThreadMessages(clientUserId, dialogId, threadMessageId, startId, endId, limit);
            } else if (isTopic && threadMessageId != 0) {
                deletedMessages = ayuController.getTopicMessages(clientUserId, dialogId, threadMessageId, startId, endId, limit);
            } else {
                deletedMessages = ayuController.getMessages(clientUserId, dialogId, startId, endId, limit);
            }
        }

        if (deletedMessages == null || deletedMessages.isEmpty()) {
            return;
        }

        MessagesStorage storage = MessagesStorage.getInstance(currentAccount);
        MessagesController messagesController = MessagesController.getInstance(currentAccount);
        LongSparseArray<TLRPC.TL_message> messagesById = new LongSparseArray<>();
        Set<Long> groupIds = new HashSet<>();
        Set<Integer> replyIds = new HashSet<>();
        ArrayList<Long> usersToLoad = new ArrayList<>();
        ArrayList<Long> chatsToLoad = new ArrayList<>();

        for (DeletedMessageFull deletedMessageFull : deletedMessages) {
            if (hasContent(deletedMessageFull)) {
                int mid = deletedMessageFull.message.messageId;
                if (!existingById.containsKey((long) mid)) {
                    TLRPC.TL_message mappedMessage = map(deletedMessageFull, currentAccount);
                    long groupId = mappedMessage.grouped_id;
                    if (groupId != 0) {
                        groupIds.add(groupId);
                    }
                    MessageReplyHeader replyHeader = mappedMessage.reply_to;
                    if (replyHeader != null) {
                        replyIds.add(replyHeader.reply_to_msg_id);
                    }
                    messagesById.put(mappedMessage.id, mappedMessage);
                    MessagesStorage.addUsersAndChatsFromMessage(mappedMessage, usersToLoad, chatsToLoad, null);
                }
            }
        }

        if (!groupIds.isEmpty()) {
            ArrayList<Long> uniqueGroupIds = new ArrayList<>(groupIds);
            for (DeletedMessageFull groupedMessageFull : ayuController.getMessagesGroupedIn(clientUserId, dialogId, uniqueGroupIds)) {
                if (hasContent(groupedMessageFull)) {
                    int mid = groupedMessageFull.message.messageId;
                    if (!existingById.containsKey((long) mid) && !messagesById.containsKey((long) mid)) {
                        TLRPC.TL_message mappedMessage = map(groupedMessageFull, currentAccount);
                        MessageReplyHeader replyHeader = mappedMessage.reply_to;
                        if (replyHeader != null) {
                            replyIds.add(replyHeader.reply_to_msg_id);
                        }
                        messagesById.put(mappedMessage.id, mappedMessage);
                        MessagesStorage.addUsersAndChatsFromMessage(mappedMessage, usersToLoad, chatsToLoad, null);
                    }
                }
            }
        }

        if (messagesById.isEmpty()) {
            return;
        }

        ArrayList<TLRPC.User> fetchedUsers = new ArrayList<>();
        ArrayList<TLRPC.Chat> fetchedChats = new ArrayList<>();
        try {
            if (!usersToLoad.isEmpty()) {
                ArrayList<Long> missingUserIds = new ArrayList<>();
                for (Long userId : usersToLoad) {
                    TLRPC.User user = messagesController.getUser(userId);
                    if (user != null) {
                        fetchedUsers.add(user);
                    } else {
                        missingUserIds.add(userId);
                    }
                }
                if (!missingUserIds.isEmpty()) {
                    storage.getUsersInternal(missingUserIds, fetchedUsers);
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        try {
            if (!chatsToLoad.isEmpty()) {
                ArrayList<Long> missingChatIds = new ArrayList<>();
                for (Long chatId : chatsToLoad) {
                    TLRPC.Chat chat = messagesController.getChat(chatId);
                    if (chat != null) {
                        fetchedChats.add(chat);
                    } else {
                        missingChatIds.add(chatId);
                    }
                }
                if (!missingChatIds.isEmpty()) {
                    storage.getChatsInternal(TextUtils.join(",", missingChatIds), fetchedChats);
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }

        LongSparseArray<TLRPC.User> usersById = new LongSparseArray<>();
        LongSparseArray<TLRPC.Chat> chatsById = new LongSparseArray<>();
        for (TLRPC.User user : fetchedUsers) {
            usersById.put(user.id, user);
        }
        for (TLRPC.Chat chat : fetchedChats) {
            chatsById.put(chat.id, chat);
        }

        if (!fetchedUsers.isEmpty()) {
            messagesController.putUsers(fetchedUsers, false);
        }
        if (!fetchedChats.isEmpty()) {
            messagesController.putChats(fetchedChats, false);
        }

        final ArrayList<MessageObject> deletedObjects = new ArrayList<>();
        for (int i = 0; i < messagesById.size(); i++) {
            deletedObjects.add(createMessageObject(currentAccount, messagesById.valueAt(i), usersById, chatsById, messagesController));
        }

        SparseArray<MessageObject> indexById = new SparseArray<>(objects.size() + deletedObjects.size());
        for (MessageObject msg : objects) {
            indexById.put(msg.getId(), msg);
        }
        for (MessageObject msg : deletedObjects) {
            indexById.put(msg.getId(), msg);
        }
        for (MessageObject messageObj : deletedObjects) {
            if (messageObj.messageOwner.reply_to != null) {
                int replyId = messageObj.messageOwner.reply_to.reply_to_msg_id;
                MessageObject candidate = indexById.get(replyId);
                if (candidate != null) {
                    messageObj.messageOwner.replyMessage = candidate.messageOwner;
                    messageObj.replyMessageObject = candidate;
                }
            }
        }

        objects.addAll(deletedObjects);
        objects.sort(AyuHistoryHook::doHook_compareMessages);
    }

    @Deprecated
    public static void doHookAsync(int currentAccount, long startId, long endId, long dialogId, int limit, long topicId, int loadType, boolean isChannelComment, long threadMessageId, boolean isTopic) {
        // Obsolete: deleted messages are now merged synchronously in processLoadedMessages
    }

    public static Pair<Integer, Integer> getMinAndMaxIds(ArrayList<MessageObject> messages) {
        int minId = ConnectionsManager.DEFAULT_DATACENTER_ID;
        int maxId = Integer.MIN_VALUE;
        for (MessageObject messageObj : messages) {
            if (!messageObj.isSending()) {
                int messageId = messageObj.getId();
                if (messageId < minId) {
                    minId = messageId;
                }
                if (messageId > maxId) {
                    maxId = messageId;
                }
            }
        }
        return new Pair<>(minId == ConnectionsManager.DEFAULT_DATACENTER_ID ? 0 : minId, maxId == Integer.MIN_VALUE ? 0 : maxId);
    }

    private static int doHook_compareMessages(MessageObject a, MessageObject b) {
        int aId = a.getId();
        int bId = b.getId();
        int aDate = a.messageOwner.date;
        int bDate = b.messageOwner.date;
        if (aId > 0 && bId > 0) {
            return Integer.compare(bId, aId);
        } else if (aId >= 0 || bId >= 0) {
            return Integer.compare(bDate, aDate);
        } else {
            if (aDate != bDate) {
                return Integer.compare(bDate, aDate);
            } else {
                return Integer.compare(aId, bId);
            }
        }
    }

    private static TLRPC.TL_message map(DeletedMessageFull deletedMessageFull, int accountId) {
        TLRPC.Reaction reaction;
        TLRPC.TL_message tlMessage = new TLRPC.TL_message();
        AyuMessageUtils.map(deletedMessageFull.message, tlMessage, accountId);
        List<DeletedMessageReaction> reactionsList = deletedMessageFull.reactions;
        if (reactionsList != null && !reactionsList.isEmpty()) {
            tlMessage.reactions = new TLRPC.TL_messageReactions();
            int orderIndex = 0;
            for (DeletedMessageReaction deletedMessageReaction : deletedMessageFull.reactions) {
                TLRPC.TL_reactionCount reactionCount = new TLRPC.TL_reactionCount();
                reactionCount.count = deletedMessageReaction.count;
                reactionCount.chosen = deletedMessageReaction.selfSelected;
                orderIndex++;
                reactionCount.chosen_order = orderIndex;
                if (deletedMessageReaction.isCustom) {
                    var customEmoji = new TLRPC.TL_reactionCustomEmoji();
                    customEmoji.document_id = deletedMessageReaction.documentId;
                    reaction = customEmoji;
                } else {
                    var emoji = new TLRPC.TL_reactionEmoji();
                    emoji.emoticon = deletedMessageReaction.emoticon;
                    reaction = emoji;
                }
                reactionCount.reaction = reaction;
                tlMessage.reactions.results.add(reactionCount);
            }
        }
        tlMessage.ayuDeleted = true;
        AyuMessageUtils.mapMedia(deletedMessageFull.message, tlMessage, accountId);
        return tlMessage;
    }

    private static MessageObject createMessageObject(int currentAccount, TLRPC.TL_message message, LongSparseArray<TLRPC.User> usersById, LongSparseArray<TLRPC.Chat> chatsById, MessagesController messagesController) {
        MessageObject messageObj = new MessageObject(currentAccount, message, usersById, chatsById, false, true);
        messageObj.setIsRead();
        messageObj.setContentIsRead();
        return messageObj;
    }

    private static boolean hasContent(DeletedMessageFull messageFull) {
        return messageFull != null && messageFull.message != null;
    }
}
