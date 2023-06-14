package com.rashidmayes.clairvoyance;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.rashidmayes.clairvoyance.util.ClairvoyanceObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class CreateDBLocally {

    @Getter
    @RequiredArgsConstructor
    public static class UserData {

        private final String uuid;
        private final int level;

        @JsonCreator
        public static UserData create(String uuid, int level) {
            return new UserData(uuid, level);
        }

    }

    public static void main(String[] args) {
        try (var aerospikeClient = new AerospikeClient("localhost", 3000)) {
            // delete all
//            var statement = new Statement();
//            statement.setNamespace("test");
//            statement.setSetName("users");
//            var recordSet = aerospikeClient.query(null, statement);
//            for (KeyRecord keyRecord : recordSet) {
//                aerospikeClient.delete(null, keyRecord.key);
//            }

            // create users
            var users = createUsers();
            users.forEach(user -> {
                var key = new Key("test", "users", user.getUuid());
                var data = new Bin("userData", json(user));
                aerospikeClient.put(null, key, data);
            });
        }
    }

    private static List<UserData> createUsers() {
        var list = new LinkedList<UserData>();
        list.add(UserData.create(UUID.randomUUID().toString(), 1));
        list.add(UserData.create(UUID.randomUUID().toString(), 2));
        list.add(UserData.create(UUID.randomUUID().toString(), 3));
        list.add(UserData.create(UUID.randomUUID().toString(), 4));
        list.add(UserData.create(UUID.randomUUID().toString(), 5));
        list.add(UserData.create(UUID.randomUUID().toString(), 6));
        list.add(UserData.create(UUID.randomUUID().toString(), 7));
        list.add(UserData.create(UUID.randomUUID().toString(), 8));
        list.add(UserData.create(UUID.randomUUID().toString(), 9));
        list.add(UserData.create(UUID.randomUUID().toString(), 10));
        list.add(UserData.create(UUID.randomUUID().toString(), 11));
        list.add(UserData.create(UUID.randomUUID().toString(), 12));
        list.add(UserData.create(UUID.randomUUID().toString(), 13));
        list.add(UserData.create(UUID.randomUUID().toString(), 14));
        list.add(UserData.create(UUID.randomUUID().toString(), 15));
        list.add(UserData.create(UUID.randomUUID().toString(), 16));
        list.add(UserData.create(UUID.randomUUID().toString(), 17));
        list.add(UserData.create(UUID.randomUUID().toString(), 18));
        list.add(UserData.create(UUID.randomUUID().toString(), 19));
        list.add(UserData.create(UUID.randomUUID().toString(), 20));
        list.add(UserData.create(UUID.randomUUID().toString(), 21));
        return list;
    }

    @SneakyThrows
    private static String json(Object obj) {
        return ClairvoyanceObjectMapper.objectWriter.writeValueAsString(obj);
    }

}
