package org.molkex.spring.minimalrest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import static org.junit.Assert.*;

public class MinimalStorageTest {
    static {
        MinimalStorage.ROOT_PATH = "./tmp/test/stores/";
    }

    static class TestEntity extends MinimalStorage.Entity {
        public String txt;

        @JsonIgnore
        public boolean initialized;

        public Date commited;

        TestEntity() {}

        TestEntity(String txt) {
            this.txt = txt;
        }

        @Override
        protected void init() {
            initialized = true;
        }

        @Override
        protected void commit() {
            commited = new Date();
            super.commit();
        }
    }

    @Test
    public void testMinimalStorage() throws IOException {
        Date start = new Date();

        MinimalStorage<TestEntity> store = new MinimalStorage<>("test-entity", TestEntity.class);
        store.put("xyz", new TestEntity("abc"));

        TestEntity xyz = store.get("xyz");
        assertEquals("abc", xyz.txt);
        xyz.txt = "xyz";
        xyz.commit();

        store = new MinimalStorage<>("test-entity", TestEntity.class);

        xyz = store.get("xyz");
        assertTrue(xyz.initialized);
        assertTrue(start.before(xyz.commited));
        assertEquals("xyz", xyz.txt);

        store.delete("xyz");

        store = new MinimalStorage<>("test-entity", TestEntity.class);

        assertNull(store.get("xyz"));

        FileUtils.deleteDirectory(new File("./tmp/test"));
        assertFalse(new File(MinimalStorage.ROOT_PATH).exists());
    }


    @Test
    public void testMinimalMessageBroker() {
        MinimalMessageBroker broker = new MinimalMessageBroker();

        TestEntity target = new TestEntity();

        broker.subscribe("/test/sub1", TestEntity.class, (t)-> target.txt = t.txt);

        broker.publish("/test/**", new TestEntity("hallo"));

        assertEquals("hallo", target.txt);
    }
}
