package com.luckgoose.goosecurios.network;

import java.lang.reflect.InvocationTargetException;

public final class ClientPacketHandlerInvoker {

    private ClientPacketHandlerInvoker() {
    }

    public static void invoke(String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Class<?> type = Class.forName("com.luckgoose.goosecurios.client.ClientPacketHandlers");
            type.getMethod(methodName, parameterTypes).invoke(null, args);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to invoke client packet handler " + methodName, e);
        }
    }
}

