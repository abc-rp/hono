/*******************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.hono.commandrouter;

import java.net.HttpURLConnection;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import org.eclipse.hono.client.ServiceInvocationException;
import org.eclipse.hono.deviceconnection.infinispan.client.DeviceConnectionInfo;
import org.eclipse.hono.service.deviceconnection.DeviceConnectionService;
import org.eclipse.hono.util.DeviceConnectionResult;

import io.opentracing.Span;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;

/**
 * An implementation of Hono's <em>Device Connection</em> API that uses an Infinispan cache
 * for storing the device connection data.
 */
public class CacheBasedDeviceConnectionService extends AbstractVerticle implements DeviceConnectionService {

    private final DeviceConnectionInfo connectionInfoCache;

    /**
     * Creates a new service instance for a device connection info cache.
     * <p>
     * NOTE: no potential Lifecycle related start/stop invocation on the given
     * cache is done here. That is expected to be invoked outside of this class.
     *
     * @param cache The cache.
     * @throws NullPointerException if the cache is {@code null}.
     */
    public CacheBasedDeviceConnectionService(final DeviceConnectionInfo cache) {
        this.connectionInfoCache = Objects.requireNonNull(cache);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Future<DeviceConnectionResult> setLastKnownGatewayForDevice(
            final String tenantId,
            final String deviceId,
            final String gatewayId,
            final Span span) {

        return connectionInfoCache.setLastKnownGatewayForDevice(tenantId, deviceId, gatewayId, span)
                .map(ok -> DeviceConnectionResult.from(HttpURLConnection.HTTP_NO_CONTENT));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Future<DeviceConnectionResult> getLastKnownGatewayForDevice(
            final String tenantId,
            final String deviceId,
            final Span span) {

        return connectionInfoCache.getLastKnownGatewayForDevice(tenantId, deviceId, span)
                .map(json -> DeviceConnectionResult.from(HttpURLConnection.HTTP_OK, json))
                .otherwise(t -> DeviceConnectionResult.from(ServiceInvocationException.extractStatusCode(t)));
    }

    @Override
    public Future<DeviceConnectionResult> setCommandHandlingAdapterInstance(final String tenantId,
            final String deviceId, final String adapterInstanceId, final Duration lifespan,
            final Span span) {
        return connectionInfoCache.setCommandHandlingAdapterInstance(tenantId, deviceId, adapterInstanceId, lifespan, span)
                .map(v -> DeviceConnectionResult.from(HttpURLConnection.HTTP_NO_CONTENT))
                .otherwise(t -> DeviceConnectionResult.from(ServiceInvocationException.extractStatusCode(t)));
    }

    @Override
    public Future<DeviceConnectionResult> removeCommandHandlingAdapterInstance(final String tenantId, final String deviceId,
            final String adapterInstanceId, final Span span) {
        return connectionInfoCache.removeCommandHandlingAdapterInstance(tenantId, deviceId, adapterInstanceId, span)
                .map(v -> DeviceConnectionResult.from(HttpURLConnection.HTTP_NO_CONTENT))
                .otherwise(t -> DeviceConnectionResult.from(ServiceInvocationException.extractStatusCode(t)));
    }

    @Override
    public Future<DeviceConnectionResult> getCommandHandlingAdapterInstances(final String tenantId, final String deviceId,
            final List<String> viaGateways, final Span span) {
        return connectionInfoCache.getCommandHandlingAdapterInstances(tenantId, deviceId, new HashSet<>(viaGateways), span)
                .map(json -> DeviceConnectionResult.from(HttpURLConnection.HTTP_OK, json))
                .otherwise(t -> DeviceConnectionResult.from(ServiceInvocationException.extractStatusCode(t)));
    }
}
