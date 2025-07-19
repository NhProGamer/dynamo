package fr.nhsoul.dynamo.common.util;

import com.google.protobuf.InvalidProtocolBufferException;
import fr.nhsoul.dynamo.common.model.ServerEvent;
import fr.nhsoul.dynamo.common.model.ServerInfo;
import fr.nhsoul.dynamo.common.model.proto.ServerMessagesProto;

public class ProtobufSerializer {

    public static byte[] serializeServerEvent(ServerEvent event) {
        ServerMessagesProto.ServerEvent.Builder builder = ServerMessagesProto.ServerEvent.newBuilder()
                .setType(convertEventType(event.getType()))
                .setTimestamp(event.getTimestamp());

        if (event.getServerInfo() != null) {
            builder.setServerInfo(convertToProto(event.getServerInfo()));
        }

        return builder.build().toByteArray();
    }

    public static ServerEvent deserializeServerEvent(byte[] data) throws InvalidProtocolBufferException {
        ServerMessagesProto.ServerEvent proto = ServerMessagesProto.ServerEvent.parseFrom(data);

        ServerEvent event = new ServerEvent();
        event.setType(convertEventType(proto.getType()));
        event.setTimestamp(proto.getTimestamp());

        if (proto.hasServerInfo()) {
            event.setServerInfo(convertFromProto(proto.getServerInfo()));
        }

        return event;
    }

    public static byte[] serializeServerInfo(ServerInfo serverInfo) {
        return convertToProto(serverInfo).toByteArray();
    }

    public static ServerInfo deserializeServerInfo(byte[] data) throws InvalidProtocolBufferException {
        ServerMessagesProto.ServerInfo proto = ServerMessagesProto.ServerInfo.parseFrom(data);
        return convertFromProto(proto);
    }

    private static ServerMessagesProto.ServerInfo convertToProto(ServerInfo serverInfo) {
        ServerMessagesProto.ServerInfo.Builder builder = ServerMessagesProto.ServerInfo.newBuilder()
                .setName(serverInfo.getName())
                .setHost(serverInfo.getHost())
                .setPort(serverInfo.getPort())
                .setTimestamp(serverInfo.getTimestamp())
                .setCurrentPlayers(serverInfo.getCurrentPlayers())
                .setMaxPlayers(serverInfo.getMaxPlayers());

        if (serverInfo.getGroups() != null) {
            builder.addAllGroups(serverInfo.getGroups());
        }

        return builder.build();
    }

    private static ServerInfo convertFromProto(ServerMessagesProto.ServerInfo proto) {
        ServerInfo serverInfo = new ServerInfo();
        serverInfo.setName(proto.getName());
        serverInfo.setHost(proto.getHost());
        serverInfo.setPort(proto.getPort());
        serverInfo.setTimestamp(proto.getTimestamp());
        serverInfo.setCurrentPlayers(proto.getCurrentPlayers());
        serverInfo.setMaxPlayers(proto.getMaxPlayers());

        serverInfo.setGroups(proto.getGroupsList());

        return serverInfo;
    }

    private static ServerMessagesProto.EventType convertEventType(ServerEvent.EventType type) {
        switch (type) {
            case REGISTER: return ServerMessagesProto.EventType.REGISTER;
            case HEARTBEAT: return ServerMessagesProto.EventType.HEARTBEAT;
            case UNREGISTER: return ServerMessagesProto.EventType.UNREGISTER;
            case PLAYER_JOIN: return ServerMessagesProto.EventType.PLAYER_JOIN;
            case PLAYER_LEAVE: return ServerMessagesProto.EventType.PLAYER_LEAVE;
            default: throw new IllegalArgumentException("Type d'événement non supporté: " + type);
        }
    }

    private static ServerEvent.EventType convertEventType(ServerMessagesProto.EventType type) {
        switch (type) {
            case REGISTER: return ServerEvent.EventType.REGISTER;
            case HEARTBEAT: return ServerEvent.EventType.HEARTBEAT;
            case UNREGISTER: return ServerEvent.EventType.UNREGISTER;
            case PLAYER_JOIN: return ServerEvent.EventType.PLAYER_JOIN;
            case PLAYER_LEAVE: return ServerEvent.EventType.PLAYER_LEAVE;
            default: throw new IllegalArgumentException("Type d'événement proto non supporté: " + type);
        }
    }
}