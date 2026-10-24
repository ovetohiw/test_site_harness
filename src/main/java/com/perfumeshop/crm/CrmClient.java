package com.perfumeshop.crm;

public interface CrmClient {

    CrmProvider provider();

    boolean isEnabled();

    CrmSyncResult syncCustomer(CrmCustomer customer);

    CrmSyncResult syncSale(CrmSale sale);

    CrmSyncResult syncBehavior(CrmBehavior behavior);
}
