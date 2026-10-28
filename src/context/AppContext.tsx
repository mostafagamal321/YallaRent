import React, { createContext, useContext, useEffect, useState, useMemo } from 'react';
import {
  Property,
  Unit,
  Contract,
  Payment,
  AppSettings,
  InAppNotification,
  Tenant,
} from '../types';
import {
  loadInitialAppData,
  saveAppData,
  exportBackupFile,
  AppDataBundle,
} from '../services/storage';
import { DEFAULT_APARTMENT_TEMPLATE, DEFAULT_SHOPS_TEMPLATE } from '../data/defaultTemplates';
import {
  computeInAppNotifications,
  scheduleDeviceNotifications,
  requestNotificationPermissions,
} from '../services/notifications';
import { generateMonthlyPaymentDates, toISODate } from '../utils/date';

export type NavigationTab =
  | 'dashboard'
  | 'properties'
  | 'tenants'
  | 'payments'
  | 'notifications'
  | 'settings'
  | 'reports';

export interface PaymentItemizedBreakdown {
  rentAmount?: number;
  electricityAmount?: number;
  waterAmount?: number;
  stairsCleaningAmount?: number;
  previousArrearsAmount?: number;
}

interface AppContextType {
  language: 'ar' | 'en';
  setLanguage: (lang: 'ar' | 'en') => void;
  toggleLanguage: () => void;
  activeTab: NavigationTab;
  setActiveTab: (tab: NavigationTab) => void;
  searchQuery: string;
  setSearchQuery: (q: string) => void;

  // PIN lock
  isPinLocked: boolean;
  unlockWithPin: (pin: string) => boolean;

  // Data
  properties: Property[];
  units: Unit[];
  tenants: Tenant[];
  contracts: Contract[];
  payments: Payment[];
  settings: AppSettings;
  notifications: InAppNotification[];
  selectedPropertyId: string | null;
  setSelectedPropertyId: (id: string | null) => void;

  // Selected Tenant for Profile Modal
  selectedTenantId: string | null;
  setSelectedTenantId: (id: string | null) => void;

  // Actions
  addProperty: (prop: Omit<Property, 'id'>) => Promise<void>;
  updateProperty: (prop: Property) => Promise<void>;
  deleteProperty: (id: string) => Promise<void>;

  addUnit: (unit: Omit<Unit, 'id'>) => Promise<void>;
  updateUnit: (unit: Unit) => Promise<void>;
  deleteUnit: (id: string) => Promise<void>;

  addTenant: (tenant: Omit<Tenant, 'id'>) => Promise<Tenant>;
  updateTenant: (tenant: Tenant) => Promise<void>;
  deleteTenant: (id: string) => Promise<void>;

  createContract: (contract: Omit<Contract, 'id' | 'status'>) => Promise<void>;
  updateContract: (contract: Contract) => Promise<void>;
  endContract: (id: string) => Promise<void>;
  deleteContract: (id: string) => Promise<void>;
  renewContract: (oldContractId: string, newStartDate: string, newEndDate: string, newRent?: number) => Promise<void>;

  addPayment: (payment: Omit<Payment, 'id'> | Payment) => Promise<Payment>;
  updateContractRentForUnit: (unitId: string, newRent: number) => Promise<void>;

  markPaymentPaid: (
    paymentId: string,
    paidAmount: number,
    paidDate: string,
    note?: string,
    itemized?: PaymentItemizedBreakdown
  ) => Promise<void>;
  updatePayment: (payment: Payment) => Promise<void>;
  deletePayment: (id: string) => Promise<void>;
  clearAllRents: () => Promise<void>;

  updateSettings: (newSettings: Partial<AppSettings>) => Promise<void>;
  resetTemplate: (type: 'apartments' | 'shops') => Promise<void>;

  exportBackup: () => Promise<boolean>;
  importBackup: (fileContent: string) => Promise<boolean>;

  // Computed stats
  stats: {
    activeContractsCount: number;
    vacantUnitsCount: number;
    expectedMonthlyRent: number;
    rentCollectedThisMonth: number;
    totalOverdueAmount: number;
    expiringSoonCount: number;
    totalTenantsCount: number;
  };
}

const AppContext = createContext<AppContextType | undefined>(undefined);

export const AppProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [isLoaded, setIsLoaded] = useState(false);
  const [properties, setProperties] = useState<Property[]>([]);
  const [units, setUnits] = useState<Unit[]>([]);
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [contracts, setContracts] = useState<Contract[]>([]);
  const [payments, setPayments] = useState<Payment[]>([]);
  const [settings, setSettings] = useState<AppSettings>({
    language: 'ar',
    pinEnabled: false,
    pinCode: '1234',
    landlord: {
      name: 'جمال حامد عبد الوهاب',
      job: 'مهندس مدني حر',
      address: 'الفيوم – الجون – نهاية ش أبو بكر الصديق – مسجد الغفور',
      nationality: 'مصري',
    },
    reminders: {
      rentDueDaysBefore: 3,
      rentDueOnDay: true,
      contractEndingDays1: 60,
      contractEndingDays2: 30,
      overdueDaily: true,
      reminderTime: '09:00',
    },
    templates: {
      apartments: DEFAULT_APARTMENT_TEMPLATE,
      shops: DEFAULT_SHOPS_TEMPLATE,
    },
  });

  const [activeTab, setActiveTab] = useState<NavigationTab>('dashboard');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedPropertyId, setSelectedPropertyId] = useState<string | null>(null);
  const [selectedTenantId, setSelectedTenantId] = useState<string | null>(null);
  const [isPinLocked, setIsPinLocked] = useState(false);

  // Initialize data on mount
  useEffect(() => {
    async function init() {
      const data = await loadInitialAppData();
      setProperties(data.properties);
      setUnits(data.units);
      setTenants(data.tenants || []);
      setContracts(data.contracts || []);
      setPayments(data.payments || []);
      setSettings(data.settings);

      // Check PIN lock
      if (data.settings.pinEnabled && data.settings.pinCode) {
        setIsPinLocked(true);
      }

      // Sync document direction
      document.documentElement.dir = data.settings.language === 'ar' ? 'rtl' : 'ltr';
      document.documentElement.lang = data.settings.language;

      setIsLoaded(true);

      // Request notification permissions
      requestNotificationPermissions();
    }
    init();
  }, []);

  // Sync direction on language change
  const setLanguage = (lang: 'ar' | 'en') => {
    document.documentElement.dir = lang === 'ar' ? 'rtl' : 'ltr';
    document.documentElement.lang = lang;
    const updated = { ...settings, language: lang };
    setSettings(updated);
    saveAppData(properties, units, contracts, payments, updated, tenants);
  };

  const toggleLanguage = () => {
    setLanguage(settings.language === 'ar' ? 'en' : 'ar');
  };

  const unlockWithPin = (pin: string): boolean => {
    if (!settings.pinEnabled || pin === settings.pinCode) {
      setIsPinLocked(false);
      return true;
    }
    return false;
  };

  // Sync automatic overdue calculation
  const refreshedPayments = useMemo(() => {
    const today = toISODate(new Date());
    return payments.map((p) => {
      if (p.status === 'pending' && p.dueDate < today) {
        return { ...p, status: 'overdue' as const };
      }
      return p;
    });
  }, [payments]);

  // Compute In-app Notifications (linking tenant names & photos, never exposing National ID or photo in text)
  const notifications = useMemo(() => {
    return computeInAppNotifications(
      contracts,
      refreshedPayments,
      properties,
      units,
      settings.reminders,
      settings.lastBackupDate,
      settings.language
    );
  }, [contracts, refreshedPayments, properties, units, settings]);

  // Schedule device notifications whenever contracts or payments update
  useEffect(() => {
    if (isLoaded) {
      scheduleDeviceNotifications(notifications);
    }
  }, [notifications, isLoaded]);

  // Computed stats
  const stats = useMemo(() => {
    const currentMonthPrefix = toISODate(new Date()).slice(0, 7); // YYYY-MM
    const activeContracts = contracts.filter((c) => !c.isHistory && c.status === 'active');

    // Vacant units: units that do NOT have any active contract
    const activeContractUnitIds = new Set(activeContracts.map((c) => c.unitId));
    const vacantCount = units.filter((u) => !activeContractUnitIds.has(u.id)).length;

    const expectedMonthlyRent = activeContracts.reduce((sum, c) => sum + (c.monthlyRent || 0), 0);

    const rentCollectedThisMonth = refreshedPayments
      .filter((p) => p.status === 'paid' && p.paidDate && p.paidDate.startsWith(currentMonthPrefix))
      .reduce((sum, p) => sum + (p.paidAmount || p.amount || 0), 0);

    const totalOverdueAmount = refreshedPayments
      .filter((p) => p.status === 'overdue')
      .reduce((sum, p) => sum + (p.amount - (p.paidAmount || 0)), 0);

    const expiringSoonCount = contracts.filter(
      (c) => !c.isHistory && (c.status === 'expiring_soon' || c.status === 'active')
    ).length;

    return {
      activeContractsCount: activeContracts.length,
      vacantUnitsCount: vacantCount,
      expectedMonthlyRent,
      rentCollectedThisMonth,
      totalOverdueAmount,
      expiringSoonCount,
      totalTenantsCount: tenants.length,
    };
  }, [contracts, units, refreshedPayments, tenants]);

  // Actions
  const persistChanges = async (
    newProps = properties,
    newUnits = units,
    newContracts = contracts,
    newPayments = refreshedPayments,
    newSettings = settings,
    newTenants = tenants
  ) => {
    await saveAppData(newProps, newUnits, newContracts, newPayments, newSettings, newTenants);
  };

  const addProperty = async (propData: Omit<Property, 'id'>) => {
    const newProp: Property = {
      ...propData,
      id: `prop-${Date.now()}`,
    };
    const next = [...properties, newProp];
    setProperties(next);
    await persistChanges(next);
  };

  const updateProperty = async (prop: Property) => {
    const next = properties.map((p) => (p.id === prop.id ? prop : p));
    setProperties(next);
    await persistChanges(next);
  };

  const deleteProperty = async (id: string) => {
    const nextProps = properties.filter((p) => p.id !== id);
    const deletedUnitIds = new Set(units.filter((u) => u.propertyId === id).map((u) => u.id));
    const nextUnits = units.filter((u) => u.propertyId !== id);
    const nextContracts = contracts.filter((c) => !deletedUnitIds.has(c.unitId));
    const nextPayments = refreshedPayments.filter((p) => !deletedUnitIds.has(p.unitId));

    setProperties(nextProps);
    setUnits(nextUnits);
    setContracts(nextContracts);
    setPayments(nextPayments);
    await persistChanges(nextProps, nextUnits, nextContracts, nextPayments, settings, tenants);
  };

  const addUnit = async (unitData: Omit<Unit, 'id'>) => {
    const newUnit: Unit = {
      ...unitData,
      id: `unit-${Date.now()}`,
    };
    const next = [...units, newUnit];
    setUnits(next);
    await persistChanges(properties, next);
  };

  const updateUnit = async (unit: Unit) => {
    const next = units.map((u) => (u.id === unit.id ? unit : u));
    setUnits(next);
    await persistChanges(properties, next);
  };

  const deleteUnit = async (id: string) => {
    const nextUnits = units.filter((u) => u.id !== id);
    const nextContracts = contracts.filter((c) => c.unitId !== id);
    const nextPayments = refreshedPayments.filter((p) => p.unitId !== id);

    setUnits(nextUnits);
    setContracts(nextContracts);
    setPayments(nextPayments);
    await persistChanges(properties, nextUnits, nextContracts, nextPayments, settings, tenants);
  };

  // Tenant Actions
  const addTenant = async (tenantData: Omit<Tenant, 'id'>): Promise<Tenant> => {
    const newTenant: Tenant = {
      ...tenantData,
      id: `tnt-${Date.now()}`,
      nationality: tenantData.nationality || 'مصري',
    };
    const next = [...tenants, newTenant];
    setTenants(next);
    await persistChanges(properties, units, contracts, refreshedPayments, settings, next);
    return newTenant;
  };

  const updateTenant = async (tenant: Tenant) => {
    const next = tenants.map((t) => (t.id === tenant.id ? tenant : t));
    // Also sync contract tenant fields if any contract links to this tenant
    const nextContracts = contracts.map((c) => {
      if (c.tenantId === tenant.id) {
        return {
          ...c,
          tenantName: tenant.fullName,
          tenantJob: tenant.job,
          tenantAddress: tenant.address,
          tenantNationality: tenant.nationality,
          nationalId: tenant.nationalId,
        };
      }
      return c;
    });

    setTenants(next);
    setContracts(nextContracts);
    await persistChanges(properties, units, nextContracts, refreshedPayments, settings, next);
  };

  const deleteTenant = async (id: string) => {
    const nextTenants = tenants.filter((t) => t.id !== id);
    // Unlink or delete contracts of this tenant
    const nextContracts = contracts.map((c) => {
      if (c.tenantId === id) {
        return { ...c, tenantId: undefined };
      }
      return c;
    });

    setTenants(nextTenants);
    setContracts(nextContracts);
    if (selectedTenantId === id) {
      setSelectedTenantId(null);
    }
    await persistChanges(properties, units, nextContracts, refreshedPayments, settings, nextTenants);
  };

  const createContract = async (contractData: Omit<Contract, 'id' | 'status'>) => {
    const newContractId = `cnt-${Date.now()}`;
    const newContract: Contract = {
      ...contractData,
      id: newContractId,
      status: 'active',
    };

    // Generate monthly payment records
    const dueDates = generateMonthlyPaymentDates(
      newContract.startDate,
      newContract.endDate,
      newContract.rentDueDay
    );

    const newPayments: Payment[] = dueDates.map((dueDate, idx) => ({
      id: `pay-${newContractId}-${idx + 1}`,
      contractId: newContractId,
      unitId: newContract.unitId,
      dueDate,
      amount: newContract.monthlyRent,
      rentAmount: newContract.monthlyRent,
      electricityAmount: 0,
      waterAmount: 0,
      stairsCleaningAmount: 0,
      previousArrearsAmount: 0,
      status: 'pending',
    }));

    const nextContracts = [...contracts, newContract];
    const nextPayments = [...refreshedPayments, ...newPayments];

    setContracts(nextContracts);
    setPayments(nextPayments);
    await persistChanges(properties, units, nextContracts, nextPayments, settings, tenants);
  };

  const updateContract = async (contract: Contract) => {
    const next = contracts.map((c) => (c.id === contract.id ? contract : c));
    setContracts(next);

    // Update any unpaid payments for this contract with new rent amount, unit, etc.
    const updatedPayments = refreshedPayments.map((p) => {
      if (p.contractId === contract.id && p.status !== 'paid') {
        return {
          ...p,
          unitId: contract.unitId,
          amount: contract.monthlyRent,
          rentAmount: contract.monthlyRent,
        };
      }
      return p;
    });
    setPayments(updatedPayments);

    // Keep linked tenant in sync if tenantId is set
    let nextTenants = tenants;
    if (contract.tenantId) {
      nextTenants = tenants.map((t) => {
        if (t.id === contract.tenantId) {
          return {
            ...t,
            fullName: contract.tenantName || t.fullName,
            nationalId: contract.nationalId || t.nationalId,
            job: contract.tenantJob || t.job,
            address: contract.tenantAddress || t.address,
            nationality: contract.tenantNationality || t.nationality,
          };
        }
        return t;
      });
      setTenants(nextTenants);
    }

    await persistChanges(properties, units, next, updatedPayments, settings, nextTenants);
  };

  const endContract = async (id: string) => {
    const today = toISODate(new Date());
    const next = contracts.map((c) => {
      if (c.id === id) {
        return {
          ...c,
          status: 'expired' as const,
          isHistory: true,
          historyEndedDate: today,
        };
      }
      return c;
    });
    setContracts(next);
    await persistChanges(properties, units, next, refreshedPayments, settings, tenants);
  };

  const deleteContract = async (id: string) => {
    const nextContracts = contracts.filter((c) => c.id !== id);
    const nextPayments = refreshedPayments.filter((p) => p.contractId !== id);
    setContracts(nextContracts);
    setPayments(nextPayments);
    await persistChanges(properties, units, nextContracts, nextPayments, settings, tenants);
  };

  const renewContract = async (
    oldContractId: string,
    newStartDate: string,
    newEndDate: string,
    newRent?: number
  ) => {
    const old = contracts.find((c) => c.id === oldContractId);
    if (!old) return;

    // Mark old as history
    const updatedOld: Contract = {
      ...old,
      status: 'expired',
      isHistory: true,
      historyEndedDate: newStartDate,
    };

    // Create renewed contract
    const newContractId = `cnt-${Date.now()}`;
    const renewedRent = newRent !== undefined && newRent > 0 ? newRent : old.monthlyRent;
    const newContract: Contract = {
      ...old,
      id: newContractId,
      startDate: newStartDate,
      endDate: newEndDate,
      monthlyRent: renewedRent,
      status: 'active',
      isHistory: false,
      historyEndedDate: undefined,
      contractDate: toISODate(new Date()),
    };

    // Generate payments for renewed contract
    const dueDates = generateMonthlyPaymentDates(
      newContract.startDate,
      newContract.endDate,
      newContract.rentDueDay
    );

    const newPaymentList: Payment[] = dueDates.map((dueDate, idx) => ({
      id: `pay-${newContractId}-${idx + 1}`,
      contractId: newContractId,
      unitId: newContract.unitId,
      dueDate,
      amount: newContract.monthlyRent,
      rentAmount: newContract.monthlyRent,
      electricityAmount: 0,
      waterAmount: 0,
      stairsCleaningAmount: 0,
      previousArrearsAmount: 0,
      status: 'pending',
    }));

    const nextContracts = contracts.map((c) => (c.id === oldContractId ? updatedOld : c)).concat(newContract);
    const nextPayments = [...refreshedPayments, ...newPaymentList];

    setContracts(nextContracts);
    setPayments(nextPayments);
    await persistChanges(properties, units, nextContracts, nextPayments, settings, tenants);
  };

  const markPaymentPaid = async (
    paymentId: string,
    paidAmount: number,
    paidDate: string,
    note?: string,
    itemized?: PaymentItemizedBreakdown
  ) => {
    const next = refreshedPayments.map((p) => {
      if (p.id === paymentId) {
        const fullAmount = p.amount;
        const isFull = paidAmount >= fullAmount;
        return {
          ...p,
          status: isFull ? ('paid' as const) : ('pending' as const),
          paidAmount,
          paidDate,
          rentAmount: itemized?.rentAmount !== undefined ? itemized.rentAmount : (p.rentAmount ?? paidAmount),
          electricityAmount: itemized?.electricityAmount !== undefined ? itemized.electricityAmount : (p.electricityAmount ?? 0),
          waterAmount: itemized?.waterAmount !== undefined ? itemized.waterAmount : (p.waterAmount ?? 0),
          stairsCleaningAmount: itemized?.stairsCleaningAmount !== undefined ? itemized.stairsCleaningAmount : (p.stairsCleaningAmount ?? 0),
          previousArrearsAmount: itemized?.previousArrearsAmount !== undefined ? itemized.previousArrearsAmount : (p.previousArrearsAmount ?? 0),
          note: note || (isFull ? 'تم السداد بالكامل' : `سداد جزئي (${paidAmount} من ${fullAmount})`),
        };
      }
      return p;
    });

    setPayments(next);
    await persistChanges(properties, units, contracts, next, settings, tenants);
  };

  const updatePayment = async (payment: Payment) => {
    const next = refreshedPayments.map((p) => (p.id === payment.id ? payment : p));
    setPayments(next);
    await persistChanges(properties, units, contracts, next, settings, tenants);
  };

  const deletePayment = async (id: string) => {
    const next = refreshedPayments.filter((p) => p.id !== id);
    setPayments(next);
    await persistChanges(properties, units, contracts, next, settings, tenants);
  };

  const addPayment = async (paymentData: Omit<Payment, 'id'> | Payment): Promise<Payment> => {
    const id = 'id' in paymentData && paymentData.id ? paymentData.id : `pay-custom-${Date.now()}`;
    const today = toISODate(new Date());
    const isPaid = paymentData.status === 'paid';
    const finalPayment: Payment = {
      ...paymentData,
      id,
      status: isPaid ? 'paid' : (paymentData.dueDate < today ? 'overdue' : 'pending'),
      amount: paymentData.amount || 0,
    };
    const nextPayments = [finalPayment, ...refreshedPayments];
    setPayments(nextPayments);
    await persistChanges(properties, units, contracts, nextPayments, settings, tenants);
    return finalPayment;
  };

  const updateContractRentForUnit = async (unitId: string, newRent: number) => {
    const activeContract = contracts.find((c) => !c.isHistory && c.unitId === unitId);
    if (!activeContract) return;
    const updatedContract: Contract = {
      ...activeContract,
      monthlyRent: newRent,
    };
    await updateContract(updatedContract);
  };

  const clearAllRents = async () => {
    setContracts([]);
    setPayments([]);
    await persistChanges(properties, units, [], [], settings, tenants);
  };

  const updateSettings = async (newSettings: Partial<AppSettings>) => {
    const next = { ...settings, ...newSettings };
    setSettings(next);
    await persistChanges(properties, units, contracts, refreshedPayments, next, tenants);
  };

  const resetTemplate = async (type: 'apartments' | 'shops') => {
    const nextTemplates = {
      ...settings.templates,
      [type]: type === 'apartments' ? DEFAULT_APARTMENT_TEMPLATE : DEFAULT_SHOPS_TEMPLATE,
    };
    await updateSettings({ templates: nextTemplates });
  };

  const exportBackup = async (): Promise<boolean> => {
    const bundle: AppDataBundle = {
      version: 2,
      exportedAt: new Date().toISOString(),
      properties,
      units,
      tenants,
      contracts,
      payments: refreshedPayments,
      settings: { ...settings, lastBackupDate: toISODate(new Date()) },
    };
    const success = await exportBackupFile(bundle);
    if (success) {
      await updateSettings({ lastBackupDate: toISODate(new Date()) });
    }
    return success;
  };

  const importBackup = async (fileContent: string): Promise<boolean> => {
    try {
      const bundle = JSON.parse(fileContent) as AppDataBundle;
      if (!bundle || !bundle.properties || !bundle.units) {
        return false;
      }
      setProperties(bundle.properties);
      setUnits(bundle.units);
      setTenants(bundle.tenants || []);
      setContracts(bundle.contracts || []);
      setPayments(bundle.payments || []);
      if (bundle.settings) {
        setSettings(bundle.settings);
        document.documentElement.dir = bundle.settings.language === 'ar' ? 'rtl' : 'ltr';
        document.documentElement.lang = bundle.settings.language;
      }
      await persistChanges(
        bundle.properties,
        bundle.units,
        bundle.contracts || [],
        bundle.payments || [],
        bundle.settings || settings,
        bundle.tenants || []
      );
      return true;
    } catch (e) {
      console.error('Import failed:', e);
      return false;
    }
  };

  return (
    <AppContext.Provider
      value={{
        language: settings.language,
        setLanguage,
        toggleLanguage,
        activeTab,
        setActiveTab,
        searchQuery,
        setSearchQuery,
        isPinLocked,
        unlockWithPin,
        properties,
        units,
        tenants,
        contracts,
        payments: refreshedPayments,
        settings,
        notifications,
        selectedPropertyId,
        setSelectedPropertyId,
        selectedTenantId,
        setSelectedTenantId,
        addProperty,
        updateProperty,
        deleteProperty,
        addUnit,
        updateUnit,
        deleteUnit,
        addTenant,
        updateTenant,
        deleteTenant,
        createContract,
        updateContract,
        endContract,
        deleteContract,
        renewContract,
        addPayment,
        updateContractRentForUnit,
        markPaymentPaid,
        updatePayment,
        deletePayment,
        clearAllRents,
        updateSettings,
        resetTemplate,
        exportBackup,
        importBackup,
        stats,
      }}
    >
      {children}
    </AppContext.Provider>
  );
};

export const useApp = () => {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error('useApp must be used within an AppProvider');
  }
  return context;
};
