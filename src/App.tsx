import React, { useState } from 'react';
import { AppProvider, useApp } from './context/AppContext';
import { Header } from './components/Header';
import { BottomNav } from './components/BottomNav';
import { PinLockModal } from './components/PinLockModal';
import { DashboardScreen } from './screens/DashboardScreen';
import { PropertiesScreen } from './screens/PropertiesScreen';
import { TenantsScreen } from './screens/TenantsScreen';
import { PaymentsScreen } from './screens/PaymentsScreen';
import { NotificationsScreen } from './screens/NotificationsScreen';
import { SettingsScreen } from './screens/SettingsScreen';
import { ReportsScreen } from './screens/ReportsScreen';
import { ContractModal } from './components/ContractModal';
import { UnitDetailModal } from './components/UnitDetailModal';
import { PaymentModal } from './components/PaymentModal';
import { TenantModal } from './components/TenantModal';
import { TenantProfileModal } from './components/TenantProfileModal';
import { Contract, Tenant } from './types';

const MainLayout: React.FC = () => {
  const { activeTab, selectedTenantId, setSelectedTenantId } = useApp();

  // Modal states
  const [contractModalOpen, setContractModalOpen] = useState(false);
  const [contractModalUnitId, setContractModalUnitId] = useState<string | undefined>(undefined);
  const [editingContract, setEditingContract] = useState<Contract | undefined>(undefined);

  const [unitDetailId, setUnitDetailId] = useState<string | null>(null);
  const [activePaymentId, setActivePaymentId] = useState<string | null>(null);

  const [tenantModalOpen, setTenantModalOpen] = useState(false);
  const [editingTenantId, setEditingTenantId] = useState<string | null>(null);

  const handleOpenContractModal = (unitId?: string, existingContract?: Contract) => {
    setContractModalUnitId(unitId);
    setEditingContract(existingContract);
    setContractModalOpen(true);
  };

  const handleOpenUnitDetail = (unitId: string) => {
    setUnitDetailId(unitId);
  };

  const handleOpenPaymentModal = (paymentId: string) => {
    setActivePaymentId(paymentId);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh', backgroundColor: '#f8fafc' }}>
      <PinLockModal />
      <Header />

      <main style={{ flex: 1, maxWidth: '720px', width: '100%', margin: '0 auto' }}>
        {activeTab === 'dashboard' && (
          <DashboardScreen
            onOpenContractModal={handleOpenContractModal}
            onOpenPaymentModal={handleOpenPaymentModal}
            onOpenUnitDetail={handleOpenUnitDetail}
          />
        )}
        {activeTab === 'properties' && (
          <PropertiesScreen
            onOpenContractModal={handleOpenContractModal}
            onOpenUnitDetail={handleOpenUnitDetail}
          />
        )}
        {activeTab === 'tenants' && (
          <TenantsScreen
            onSelectTenant={(id) => setSelectedTenantId(id)}
            onAddTenant={() => {
              setEditingTenantId(null);
              setTenantModalOpen(true);
            }}
          />
        )}
        {activeTab === 'payments' && (
          <PaymentsScreen onOpenPaymentModal={handleOpenPaymentModal} />
        )}
        {activeTab === 'notifications' && (
          <NotificationsScreen
            onOpenContractModal={handleOpenContractModal}
            onOpenPaymentModal={handleOpenPaymentModal}
            onOpenUnitDetail={handleOpenUnitDetail}
          />
        )}
        {activeTab === 'settings' && <SettingsScreen />}
        {activeTab === 'reports' && <ReportsScreen />}
      </main>

      <BottomNav />

      {/* Contract Modal */}
      {contractModalOpen && (
        <ContractModal
          unitId={contractModalUnitId}
          contract={editingContract}
          onClose={() => {
            setContractModalOpen(false);
            setContractModalUnitId(undefined);
            setEditingContract(undefined);
          }}
        />
      )}

      {/* Unit Detail Modal */}
      {unitDetailId && (
        <UnitDetailModal
          unitId={unitDetailId}
          onClose={() => setUnitDetailId(null)}
          onEditContract={(contract) => {
            setUnitDetailId(null);
            handleOpenContractModal(contract.unitId, contract);
          }}
          onOpenPaymentModal={(paymentId) => {
            handleOpenPaymentModal(paymentId);
          }}
        />
      )}

      {/* Payment Modal */}
      {activePaymentId && (
        <PaymentModal
          paymentId={activePaymentId}
          onClose={() => setActivePaymentId(null)}
        />
      )}

      {/* Tenant Profile Modal */}
      {selectedTenantId && (
        <TenantProfileModal
          tenantId={selectedTenantId}
          onClose={() => setSelectedTenantId(null)}
          onEdit={(t: Tenant) => {
            setEditingTenantId(t.id);
            setTenantModalOpen(true);
          }}
          onOpenContract={(c: Contract) => {
            handleOpenContractModal(c.unitId, c);
          }}
        />
      )}

      {/* Tenant Add / Edit Modal */}
      {tenantModalOpen && (
        <TenantModal
          tenantId={editingTenantId}
          onClose={() => {
            setTenantModalOpen(false);
            setEditingTenantId(null);
          }}
        />
      )}
    </div>
  );
};

export const App: React.FC = () => {
  return (
    <AppProvider>
      <MainLayout />
    </AppProvider>
  );
};

export default App;
