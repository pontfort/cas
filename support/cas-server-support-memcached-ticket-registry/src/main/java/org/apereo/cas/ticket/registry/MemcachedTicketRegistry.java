package org.apereo.cas.ticket.registry;

import net.spy.memcached.MemcachedClientIF;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apereo.cas.memcached.MemcachedConnectionFactory;
import org.apereo.cas.ticket.Ticket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Key-value ticket registry implementation that stores tickets in memcached keyed on the ticket ID.
 *
 * @author Scott Battaglia
 * @author Marvin S. Addison
 * @since 3.3
 */
public class MemcachedTicketRegistry extends AbstractTicketRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(MemcachedTicketRegistry.class);
    /**
     * Memcached client.
     */
    private final ObjectPool<MemcachedClientIF> connectionPool;

    public MemcachedTicketRegistry(final MemcachedClientIF client) {
        this.connectionPool = new GenericObjectPool<>(new BasePooledObjectFactory<MemcachedClientIF>() {
            @Override
            public MemcachedClientIF create() throws Exception {
                return client;
            }

            @Override
            public PooledObject<MemcachedClientIF> wrap(final MemcachedClientIF memcachedClientIF) {
                return new DefaultPooledObject<>(memcachedClientIF);
            }
        });
    }

    /**
     * Creates a new instance using the given memcached client instance, which is presumably configured via
     * {@code net.spy.memcached.spring.MemcachedClientFactoryBean}.
     *
     * @param client Memcached client.
     */
    public MemcachedTicketRegistry(final ObjectPool<MemcachedClientIF> client) {
        this.connectionPool = client;
    }

    @Override
    public Ticket updateTicket(final Ticket ticketToUpdate) {
        final Ticket ticket = encodeTicket(ticketToUpdate);
        LOGGER.debug("Updating ticket [{}]", ticket);
        try {
            getClientFromPool().replace(ticket.getId(), getTimeout(ticketToUpdate), ticket);
        } catch (final Exception e) {
            LOGGER.error("Failed updating [{}]", ticket, e);
        }
        return ticket;
    }

    @Override
    public void addTicket(final Ticket ticketToAdd) {
        try {
            final Ticket ticket = encodeTicket(ticketToAdd);
            LOGGER.debug("Adding ticket [{}]", ticket);
            getClientFromPool().set(ticket.getId(), getTimeout(ticketToAdd), ticket);
        } catch (final Exception e) {
            LOGGER.error("Failed adding [{}]", ticketToAdd, e);
        }
    }

    @Override
    public long deleteAll() {
        LOGGER.debug("deleteAll() isn't supported. Returning empty list");
        return 0;
    }

    @Override
    public boolean deleteSingleTicket(final String ticketIdToDelete) {
        final String ticketId = encodeTicketId(ticketIdToDelete);
        try {
            getClientFromPool().delete(ticketId);
        } catch (final Exception e) {
            LOGGER.error("Ticket not found or is already removed. Failed deleting [{}]", ticketId, e);
        }
        return true;
    }

    @Override
    public Ticket getTicket(final String ticketIdToGet) {
        final String ticketId = encodeTicketId(ticketIdToGet);
        try {
            final Ticket t = (Ticket) getClientFromPool().get(ticketId);
            if (t != null) {
                return decodeTicket(t);
            }
        } catch (final Exception e) {
            LOGGER.error("Failed fetching [{}] ", ticketId, e);
        }
        return null;
    }

    @Override
    public Collection<Ticket> getTickets() {
        LOGGER.debug("getTickets() isn't supported. Returning empty list");
        return new ArrayList<>(0);
    }

    /**
     * Destroy the client and shut down.
     */
    @PreDestroy
    public void destroy() {
        this.connectionPool.close();
    }

    /**
     * If not time out value is specified, expire the ticket immediately.
     *
     * @param ticket the ticket
     * @return timeout in milliseconds.
     */
    private static int getTimeout(final Ticket ticket) {
        final int ttl = ticket.getExpirationPolicy().getTimeToLive().intValue();
        if (ttl == 0) {
            return 1;
        }
        return ttl;
    }

    private MemcachedClientIF getClientFromPool() throws Exception {
        return this.connectionPool.borrowObject();
    }
}
