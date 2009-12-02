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

package voldemort.client;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import voldemort.client.protocol.RequestFormatType;
import voldemort.cluster.Node;
import voldemort.store.Store;
import voldemort.store.socket.SocketDestination;
import voldemort.store.socket.SocketPool;
import voldemort.store.socket.SocketStore;
import voldemort.utils.ByteArray;
import voldemort.utils.JmxUtils;
import voldemort.utils.Utils;

/**
 * A StoreClientFactory abstracts away the connection pooling, threading, and
 * bootstrapping mechanism. It can be used to create any number of
 * {@link voldemort.client.StoreClient StoreClient} instances for different
 * stores.
 * 
 * @author jay
 * 
 */
public class SocketStoreClientFactory extends AbstractStoreClientFactory {

    public static final String URL_SCHEME = "tcp";

    private final SocketPool socketPool;
    private final RoutingTier routingTier;

    public SocketStoreClientFactory(ClientConfig config) {
        super(config);
        this.routingTier = config.getRoutingTier();
        this.socketPool = new SocketPool(config.getMaxConnectionsPerNode(),
                                         config.getConnectionTimeout(TimeUnit.MILLISECONDS),
                                         config.getSocketTimeout(TimeUnit.MILLISECONDS),
                                         config.getSocketBufferSize());
        registerJmx(JmxUtils.createObjectName(SocketPool.class), socketPool);
    }

    @Override
    protected Store<ByteArray, byte[]> getStore(String storeName,
                                                String host,
                                                int port,
                                                RequestFormatType type) {
        return new SocketStore(Utils.notNull(storeName),
                               new SocketDestination(Utils.notNull(host), port, type),
                               socketPool,
                               RoutingTier.SERVER.equals(routingTier));
    }

    @Override
    protected int getPort(Node node) {
        return node.getSocketPort();
    }

    @Override
    protected void validateUrl(URI url) {
        if(!URL_SCHEME.equals(url.getScheme()))
            throw new IllegalArgumentException("Illegal scheme in bootstrap URL for SocketStoreClientFactory:"
                                               + " expected '"
                                               + URL_SCHEME
                                               + "' but found '"
                                               + url.getScheme() + "'.");
    }

    public void close() {
        this.socketPool.close();
        this.getThreadPool().shutdown();
    }

}
