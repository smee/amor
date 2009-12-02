/*
 * Copyright 2008-2009 LinkedIn, Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package voldemort.scheduled;

import static voldemort.TestUtils.bytesEqual;

import java.util.Date;

import junit.framework.TestCase;
import voldemort.TestUtils;
import voldemort.server.StoreRepository;
import voldemort.server.scheduler.SlopPusherJob;
import voldemort.store.FailingStore;
import voldemort.store.memory.InMemoryStorageEngine;
import voldemort.store.slop.Slop;
import voldemort.store.slop.Slop.Operation;
import voldemort.utils.ByteArray;
import voldemort.versioning.Versioned;

public class SlopPusherTest extends TestCase {

    private final static String STORE_NAME = "test";

    private SlopPusherJob pusher;
    private StoreRepository repo;
    private int failingNodeId;

    public SlopPusherTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        repo = new StoreRepository();
        repo.setSlopStore(new InMemoryStorageEngine<ByteArray, Slop>("slop"));
        repo.addNodeStore(0, new InMemoryStorageEngine<ByteArray, byte[]>(STORE_NAME));
        repo.addNodeStore(1, new InMemoryStorageEngine<ByteArray, byte[]>(STORE_NAME));
        this.failingNodeId = 2;
        repo.addNodeStore(failingNodeId, new FailingStore<ByteArray, byte[]>(STORE_NAME));
        pusher = new SlopPusherJob(repo);
    }

    private Versioned<Slop> randomSlop(String name, int nodeId) {
        return Versioned.value(new Slop(name,
                                        Operation.PUT,
                                        TestUtils.randomBytes(10),
                                        TestUtils.randomBytes(10),
                                        nodeId,
                                        new Date()));
    }

    private void pushSlop(Versioned<Slop>... slops) {
        // put all the slop in the slop store
        for(Versioned<Slop> s: slops)
            repo.getSlopStore().put(s.getValue().makeKey(), s);

        // run the pusher
        pusher.run();
    }

    private void checkPush(Versioned<Slop>[] delivered, Versioned<Slop>[] undelivered) {
        // now all the delivered slop should be gone and the various stores
        // should have
        // those items
        for(Versioned<Slop> vs: delivered) {
            // check that all the slops are in the stores
            // and no new slops have appeared
            // and the SloppyStore is now empty
            Slop slop = vs.getValue();
            assertEquals("Slop remains.", 0, repo.getSlopStore().get(slop.makeKey()).size());
            assertTrue(bytesEqual(slop.getValue(), repo.getNodeStore(STORE_NAME, slop.getNodeId())
                                                       .get(slop.makeKey())
                                                       .get(0)
                                                       .getValue()));
        }
        // check that all undelivered slop is undelivered
        for(Versioned<Slop> vs: undelivered) {
            Slop slop = vs.getValue();
            assertEquals("Slop is gone!", 1, repo.getSlopStore().get(slop.makeKey()).size());
        }
    }

    public void testPushNoSlop() {
        pusher.run();
    }

    @SuppressWarnings("unchecked")
    public void testPushSomeSlop() {
        Versioned<Slop>[] values = new Versioned[] { randomSlop(STORE_NAME, 0),
                randomSlop(STORE_NAME, 1), randomSlop(STORE_NAME, 0) };
        pushSlop(values);
        checkPush(values, new Versioned[] {});
    }

    @SuppressWarnings("unchecked")
    public void testSlopWithFailingStore() {
        Versioned<Slop> good1 = randomSlop(STORE_NAME, 0);
        Versioned<Slop> good2 = randomSlop(STORE_NAME, 1);
        Versioned<Slop> bad = randomSlop(STORE_NAME, this.failingNodeId);
        pushSlop(good1, bad, good2);
        checkPush(new Versioned[] { good1, good2 }, new Versioned[] { bad });
    }
}
