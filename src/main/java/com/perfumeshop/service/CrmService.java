package com.perfumeshop.service;

import com.perfumeshop.crm.CrmBehavior;
import com.perfumeshop.crm.CrmClient;
import com.perfumeshop.crm.CrmCustomer;
import com.perfumeshop.crm.CrmEventType;
import com.perfumeshop.crm.CrmProperties;
import com.perfumeshop.crm.CrmProvider;
import com.perfumeshop.crm.CrmSale;
import com.perfumeshop.crm.CrmSyncResult;
import com.perfumeshop.domain.crm.CrmSyncEvent;
import com.perfumeshop.domain.order.ShopOrder;
import com.perfumeshop.domain.product.Product;
import com.perfumeshop.domain.user.User;
import com.perfumeshop.dto.crm.CrmEventResponse;
import com.perfumeshop.dto.crm.CrmStatusResponse;
import com.perfumeshop.repository.CrmSyncEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class CrmService {

    private static final Logger log = LoggerFactory.getLogger(CrmService.class);

    private final CrmProperties properties;
    private final List<CrmClient> clients;
    private final CrmSyncEventRepository eventRepository;

    public CrmService(
            CrmProperties properties,
            List<CrmClient> clients,
            CrmSyncEventRepository eventRepository
    ) {
        this.properties = properties;
        this.clients = clients;
        this.eventRepository = eventRepository;
    }

    public void onCustomerRegistered(User user) {
        if (!properties.isEnabled()) {
            return;
        }
        CrmCustomer customer = new CrmCustomer(user.getId(), user.getLogin());
        for (CrmClient client : enabledClients()) {
            safeSync(
                    client,
                    () -> client.syncCustomer(customer),
                    user.getLogin(),
                    String.valueOf(user.getId())
            );
        }
    }

    public void onSaleCompleted(ShopOrder order) {
        if (!properties.isEnabled()) {
            return;
        }
        List<CrmSale.Line> lines = order.getItems().stream()
                .map(item -> new CrmSale.Line(item.getProductName(), item.getQuantity(), item.getUnitPrice()))
                .toList();
        CrmSale sale = new CrmSale(order.getId(), order.getUser().getLogin(), order.getTotal(), lines);
        for (CrmClient client : enabledClients()) {
            safeSync(
                    client,
                    () -> client.syncSale(sale),
                    order.getUser().getLogin(),
                    String.valueOf(order.getId())
            );
        }
    }

    public void onFavoriteAdded(User user, Product product) {
        if (!properties.isEnabled()) {
            return;
        }
        CrmBehavior behavior = new CrmBehavior(user.getLogin(), product.getId(), product.getName());
        for (CrmClient client : enabledClients()) {
            safeSync(
                    client,
                    () -> client.syncBehavior(behavior),
                    user.getLogin(),
                    String.valueOf(product.getId())
            );
        }
    }

    @Transactional(readOnly = true)
    public CrmStatusResponse status() {
        List<CrmStatusResponse.ProviderStatus> providers = new ArrayList<>();
        for (CrmClient client : clients) {
            boolean live = isLive(client.provider());
            long success = eventRepository.countByProviderAndStatus(client.provider(), "SUCCESS");
            providers.add(new CrmStatusResponse.ProviderStatus(
                    client.provider().name(),
                    client.isEnabled(),
                    live ? "live" : "stub",
                    success
            ));
        }
        return new CrmStatusResponse(properties.isEnabled(), providers);
    }

    @Transactional(readOnly = true)
    public List<CrmEventResponse> recentEvents() {
        return eventRepository.findTop50ByOrderByCreatedAtDesc().stream()
                .map(CrmEventResponse::from)
                .toList();
    }

    private List<CrmClient> enabledClients() {
        return clients.stream().filter(CrmClient::isEnabled).toList();
    }

    private boolean isLive(CrmProvider provider) {
        return switch (provider) {
            case BITRIX24 -> properties.getBitrix24().isLive();
            case AMOCRM -> properties.getAmocrm().isLive();
        };
    }

    private void safeSync(
            CrmClient client,
            SyncCall call,
            String login,
            String referenceId
    ) {
        try {
            CrmSyncResult result = call.run();
            persist(result, login, referenceId);
        } catch (Exception ex) {
            log.warn("CRM {} sync unexpected error: {}", client.provider(), ex.getMessage());
            CrmSyncResult failed = CrmSyncResult.failed(
                    client.provider(),
                    CrmEventType.SALE_COMPLETED,
                    ex.getMessage()
            );
            persist(failed, login, referenceId);
        }
    }

    private void persist(CrmSyncResult result, String login, String referenceId) {
        eventRepository.save(new CrmSyncEvent(
                result.provider(),
                result.eventType(),
                login,
                referenceId,
                result.externalId(),
                result.status(),
                result.detail()
        ));
    }

    @FunctionalInterface
    private interface SyncCall {
        CrmSyncResult run();
    }
}
