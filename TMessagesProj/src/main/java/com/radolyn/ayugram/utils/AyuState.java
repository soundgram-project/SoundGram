/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.utils;

import android.util.LongSparseArray;

import java.util.ArrayList;

import tw.nekomimi.nekogram.NekoConfig;

public class AyuState {
    private static final AyuStateVariable allowReadPacket = new AyuStateVariable();
    private static final AyuStateVariable hideSelection = new AyuStateVariable();
    private static final java.util.concurrent.ConcurrentHashMap<Long, java.util.concurrent.ConcurrentHashMap<Integer, Long>> deletePermitted = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long PERMITTED_DELETE_TIMEOUT_MS = 120_000L;

    public static void setAllowReadPacket(boolean val, int resetAfter) {
        allowReadPacket.val = val;
        allowReadPacket.resetAfter = resetAfter;
    }

    public static boolean getAllowReadPacket() {
        return NekoConfig.sendReadMessagePackets.Bool() || allowReadPacket.process();
    }

    public static void setHideSelection(boolean val, int resetAfter) {
        hideSelection.val = val;
        hideSelection.resetAfter = resetAfter;
    }

    public static boolean getHideSelection() {
        return hideSelection.process();
    }

    public static void permitDeleteMessage(long dialogId, int messageId) {
        long now = System.currentTimeMillis();
        deletePermitted.computeIfAbsent(dialogId, k -> new java.util.concurrent.ConcurrentHashMap<>()).put(messageId, now);
        deletePermitted.computeIfAbsent(0L, k -> new java.util.concurrent.ConcurrentHashMap<>()).put(messageId, now);
    }

    public static boolean isDeletePermitted(long dialogId, int messageId) {
        long now = System.currentTimeMillis();
        if (checkMap(deletePermitted.get(dialogId), messageId, now)) return true;
        if (dialogId != 0 && checkMap(deletePermitted.get(-dialogId), messageId, now)) return true;
        if (checkMap(deletePermitted.get(0L), messageId, now)) return true;
        return false;
    }

    private static boolean checkMap(java.util.concurrent.ConcurrentHashMap<Integer, Long> map, int messageId, long now) {
        if (map == null) return false;
        Long time = map.get(messageId);
        if (time != null) {
            if (now - time < PERMITTED_DELETE_TIMEOUT_MS) {
                return true;
            } else {
                map.remove(messageId);
            }
        }
        return false;
    }

    public static void messageDeleted(long dialogId, int messageId) {
        var map = deletePermitted.get(dialogId);
        if (map != null) {
            map.remove(messageId);
        }
        var map0 = deletePermitted.get(0L);
        if (map0 != null) {
            map0.remove(messageId);
        }
    }
}
