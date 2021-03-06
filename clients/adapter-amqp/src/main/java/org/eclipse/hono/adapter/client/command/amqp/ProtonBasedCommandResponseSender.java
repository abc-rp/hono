/**
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
 */


package org.eclipse.hono.adapter.client.command.amqp;

import java.util.Objects;

import org.apache.qpid.proton.message.Message;
import org.eclipse.hono.adapter.client.amqp.AbstractServiceClient;
import org.eclipse.hono.adapter.client.command.CommandResponse;
import org.eclipse.hono.adapter.client.command.CommandResponseSender;
import org.eclipse.hono.client.HonoConnection;
import org.eclipse.hono.client.SendMessageSampler;
import org.eclipse.hono.client.impl.CommandResponseSenderImpl;
import org.eclipse.hono.config.ProtocolAdapterProperties;
import org.eclipse.hono.util.AddressHelper;
import org.eclipse.hono.util.CommandConstants;
import org.eclipse.hono.util.MessageHelper;

import io.opentracing.SpanContext;
import io.vertx.core.Future;
import io.vertx.proton.ProtonHelper;


/**
 * A vertx-proton based client for sending command response messages downstream via the AMQP 1.0 Messaging Network.
 *
 */
public class ProtonBasedCommandResponseSender extends AbstractServiceClient implements CommandResponseSender {

    /**
     * Creates a new sender for a connection.
     *
     * @param connection The connection to the AMQP 1.0 Messaging Network.
     * @param samplerFactory The factory for creating samplers for tracing AMQP messages being sent.
     * @param adapterConfig The protocol adapter's configuration properties.
     * @throws NullPointerException if any of the parameters are {@code null}.
     */
    public ProtonBasedCommandResponseSender(
            final HonoConnection connection,
            final SendMessageSampler.Factory samplerFactory,
            final ProtocolAdapterProperties adapterConfig) {
        super(connection, samplerFactory, adapterConfig);
    }

    private Future<org.eclipse.hono.client.CommandResponseSender> getSender(final String tenantId, final String replyId) {
        return connection.executeOnContext(result -> {
            CommandResponseSenderImpl.create(
                    connection,
                    tenantId,
                    replyId,
                    samplerFactory.create(CommandConstants.COMMAND_RESPONSE_ENDPOINT),
                    onRemoteClose -> {})
            .onComplete(result);
        });
    }

    private Message createDownstreamMessage(final CommandResponse response) {
        final Message msg = ProtonHelper.message();
        MessageHelper.setCreationTime(msg);
        msg.setCorrelationId(response.getCorrelationId());
        MessageHelper.setPayload(msg, response.getContentType(), response.getPayload());
        MessageHelper.addStatus(msg, response.getStatus());
        msg.setAddress(AddressHelper.getTargetAddress(
                CommandConstants.NORTHBOUND_COMMAND_RESPONSE_ENDPOINT,
                response.getTenantId(),
                response.getReplyToId(),
                null));
        MessageHelper.addTenantId(msg, response.getTenantId());
        MessageHelper.addDeviceId(msg, response.getDeviceId());
        return msg;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Future<Void> sendCommandResponse(final CommandResponse response, final SpanContext context) {

        Objects.requireNonNull(response);
        return getSender(response.getTenantId(), response.getReplyToId())
                .compose(sender -> {
                    final Message msg = createDownstreamMessage(response);
                    return sender.sendAndWaitForOutcome(msg, context)
                            .onComplete(delivery -> sender.close(r -> {}));
                })
                .mapEmpty();
    }
}
